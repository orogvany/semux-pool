package com.semuxpool.pool.block;

import com.semuxpool.client.SemuxClient;
import com.semuxpool.client.api.Block;
import com.semuxpool.client.api.SemuxException;
import com.semuxpool.client.api.Transaction;
import com.semuxpool.pool.api.BlockResult;
import com.semuxpool.pool.pay.PoolProfitAddresses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Take a block and convert to a BlockResult
 */
public class BlockResultFactory
{
    private static final Logger logger = LoggerFactory.getLogger(BlockResultFactory.class);

    //optional donation address
    public static final String DONATION_ADDRESS = "0x5a10cd29917253f3b4a98552a5e258ceb6c0775f";
    private final SemuxClient client;
    private final float poolPayoutPercent;
    private final float donationPercent;
    private final PoolProfitAddresses poolProfitsAddress;
    private final Integer minimumVoteAgeBeforeCounting;
    private final Long blockReward;
    private Set<String> voterWhitelist = new HashSet<>();

    public BlockResultFactory(SemuxClient client, float poolPayoutPercent, float donationPercent, Long blockReward, PoolProfitAddresses poolProfitsAddress, Integer minimumVoteAgeBeforeCounting, Set<String> voterWhitelist)
    {
        this.client = client;
        this.poolPayoutPercent = poolPayoutPercent;
        this.blockReward = blockReward;
        this.donationPercent = donationPercent;
        this.poolProfitsAddress = poolProfitsAddress;
        this.minimumVoteAgeBeforeCounting = minimumVoteAgeBeforeCounting;
        this.voterWhitelist = voterWhitelist;
    }

    public BlockResult getBlockResult(Block block) throws IOException, SemuxException
    {
        //get the current voters active at that block, either for that delegate or for all
        Map<String, Long> votes = getAgedVotesForBlock(block);

        // no donation if just single voter
        if (donationPercent > 0 && votes.size() > 1)
        {
            Long totalVotes = getTotalVotes(votes);
            // this 'vote' amount should be that % of total
            Long poolVotes = (long) (totalVotes / (1.0f - donationPercent) - totalVotes);

            Long currentPoolVotes = votes.get(DONATION_ADDRESS);
            if (currentPoolVotes == null)
            {
                currentPoolVotes = 0l;
            }
            currentPoolVotes += poolVotes;
            votes.put(DONATION_ADDRESS, currentPoolVotes);
        }

        //add in our payout address
        if (poolPayoutPercent > 0)
        {
            Long totalVotes = getTotalVotes(votes);
            //if we're running manual payouts, clear existing gerbage
            Long poolVotes;

            if (poolPayoutPercent > 0.99f)
            {
                votes.clear();
                //just choose a number that is big enough to divide out evenish
                poolVotes = 1000000l;
            }
            else
            {
                poolVotes = (long) (totalVotes / (1.0f - poolPayoutPercent) - totalVotes);
            }
            // this 'vote' amount should be that % of total
            for (String address : poolProfitsAddress.getAddresses())
            {
                Long currentPoolVotes = votes.get(address);
                if (currentPoolVotes == null || currentPoolVotes < 0)
                {
                    currentPoolVotes = 0l;
                }
                currentPoolVotes += (long) (poolVotes * poolProfitsAddress.getPercent(address));
                votes.put(address, currentPoolVotes);
            }
        }

        //recalculate totalVotes
        Long totalVotes = getTotalVotes(votes);

        BlockResult result = new BlockResult();
        result.setTotalVotes(totalVotes);
        result.setVotes(votes);
        result.setDelegate(block.getCoinbase());
        result.setBlockReward(getReward(block));
        result.setBlockId(block.getNumber());

        //let's double check the payouts
        Long totalPaid = 0l;
        for (Long payout : result.getPayouts().values())
        {
            totalPaid += payout;
        }

        //
        // Debug statistics
        //

        //get payout for pool

        Long poolPayout = 0l;
        for (String profitAddress : poolProfitsAddress.getAddresses())
        {
            poolPayout += result.getPayouts().get(profitAddress);
        }
        logger.trace("Pool profits: " + poolPayout);
        float totalPercent = 0;
        float totalVotesPercent = 0;
        for (Map.Entry<String, Long> payout : result.getPayouts().entrySet())
        {
            float percent = (float) payout.getValue() / (float) totalPaid;
            totalPercent += percent;
            float votesPercent = (float) votes.get(payout.getKey()) / totalVotes;
            totalVotesPercent += votesPercent;
            logger.trace(payout.getKey() + "\t" + payout.getValue() + "\t " + percent + "\t" + votesPercent);
        }
        logger.trace("total percent " + totalPercent);
        logger.trace("total votes percent " + totalVotesPercent);
        return result;
    }

    private Map<String, Long> getAgedVotesForBlock(Block block) throws IOException, SemuxException
    {
        String delegate = block.getCoinbase();
        List transactions = client.getAccountTransactions(delegate, 0, 2147483647);
        HashMap<String, Long> votes = new HashMap<>();
        Iterator iterator = transactions.iterator();
        Long oldestBlockDate = client.getBlock(Math.max(1, block.getNumber() - minimumVoteAgeBeforeCounting)).getTimestamp();

        while (iterator.hasNext())
        {
            Transaction transaction = (Transaction) iterator.next();
            if (transaction.getTimestamp() > block.getTimestamp())
            {
                break;
            }

            long valueToAdd = 0L;
            //only count votes that are older than threshold
            // new version will have block on transaction, til then, we keep track of dates

            //if we have a voter whitelist, votes only count if they're in whitelist
            if (!voterWhitelist.isEmpty() && !voterWhitelist.contains(transaction.getFrom()))
            {
                if (transaction.getType().equals("VOTE"))
                {
                    logger.warn("Not counting votes from " + transaction.getFrom() + " - " + transaction.getType());
                }

                continue;
            }

            if (transaction.getType().equals("VOTE"))
            {
                //votes only count when aged.
                if (transaction.getTimestamp() <= oldestBlockDate)
                {
                    valueToAdd = transaction.getValue();
                }
                else
                {
                    logger.debug("Vote not yet vested");
                }
            }
            else if (transaction.getType().equals("UNVOTE"))
            {
                //unvotes count immediately
                valueToAdd = 0L - transaction.getValue();
            }

            if (valueToAdd != 0L)
            {
                Long currentVal = votes.get(transaction.getFrom());
                if (currentVal == null)
                {
                    currentVal = 0L;
                }

                currentVal = currentVal + valueToAdd;
                if (currentVal < 0L)
                {
                    logger.info("Negative vote amount from " + transaction.getFrom() + ", votes = " + currentVal + ". This is normal when votes not yet vested are unvoted.");
                    currentVal = 0l;
                }

                if (currentVal > 0)
                {
                    votes.put(transaction.getFrom(), currentVal);
                }
                else
                {
                    votes.remove(transaction.getFrom());
                }
            }
        }

        return votes;
    }

    private Long getReward(Block block)
    {
        Long total = blockReward;
        for (Transaction transaction : block.getTransactions())
        {
            total += transaction.getFee();
        }
        return total;
    }

    private Long getTotalVotes(Map<String, Long> votes)
    {
        long total = 0;
        for (Long vote : votes.values())
        {
            //negative votes don't count
            if (vote > 0)
            {
                total += vote;
            }
        }
        return total;
    }

    public PoolProfitAddresses getPoolProfitsAddress()
    {
        return poolProfitsAddress;
    }
}
