package com.semuxpool.pool.block;

import com.semuxpool.client.SemuxClient;
import com.semuxpool.client.api.Block;
import com.semuxpool.client.api.SemuxException;
import com.semuxpool.client.api.Transaction;
import com.semuxpool.pool.api.BlockResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * Take a block and convert to a BlockResult
 */
public class BlockResultFactory
{
    private static final Logger logger = LoggerFactory.getLogger(BlockResultFactory.class);

    //optional donation address
    public static final String DONATION_ADDRESS = "0x5a10cd29917253f3b4a98552a5e258ceb6c0775f";
    private SemuxClient client;
    private float poolPayoutPercent;
    private float donationPercent;
    private String poolProfitsAddress;
    private Long blockReward;

    public BlockResultFactory(SemuxClient client, float poolPayoutPercent, float donationPercent, Long blockReward, String poolProfitsAddress)
    {
        this.client = client;
        this.poolPayoutPercent = poolPayoutPercent;
        this.blockReward = blockReward;
        this.donationPercent = donationPercent;
        this.poolProfitsAddress = poolProfitsAddress;
    }

    public BlockResult getBlockResult(Block block) throws IOException, SemuxException
    {
        //get the current voters active at that block, either for that delegate or for all
        Map<String, Long> votes;
        votes = client.getVotesForBlock(block.getCoinbase(), block.getNumber());

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

        //add in our payout address & donation votes
        if (poolPayoutPercent > 0)
        {
            Long totalVotes = getTotalVotes(votes);
            // this 'vote' amount should be that % of total
            Long poolVotes = (long) (totalVotes / (1.0f - poolPayoutPercent) - totalVotes);

            Long currentPoolVotes = votes.get(poolProfitsAddress);
            if (currentPoolVotes == null)
            {
                currentPoolVotes = 0l;
            }
            currentPoolVotes += poolVotes;
            votes.put(poolProfitsAddress, currentPoolVotes);
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
        Long poolPayout = result.getPayouts().get(poolProfitsAddress);
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
            total += vote;
        }
        return total;
    }
}
