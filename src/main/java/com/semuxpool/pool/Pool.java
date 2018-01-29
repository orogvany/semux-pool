package com.semuxpool.pool;

import com.semuxpool.client.SemuxClient;
import com.semuxpool.client.api.Block;
import com.semuxpool.client.api.Delegate;
import com.semuxpool.client.api.SemuxException;
import com.semuxpool.pool.api.BlockResult;
import com.semuxpool.pool.api.Payout;
import com.semuxpool.pool.block.BlockResultFactory;
import com.semuxpool.pool.block.PayoutFactory;
import com.semuxpool.pool.pay.PoolPayer;
import com.semuxpool.pool.persistence.Persistence;
import com.semuxpool.pool.state.PoolState;
import com.semuxpool.pool.status.StatusLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Polls for new blocks and manages state
 */
public class Pool implements Runnable
{
    private static final Logger logger = LoggerFactory.getLogger(Pool.class);
    public static final int LOGGING_INTERVAL = 30000;
    private final SemuxClient client;
    private final Persistence persistence;
    private final Set<String> delegates;
    private final StatusLogger statusLogger = new StatusLogger();
    private final Integer payOutNBlocks;
    private final BlockResultFactory blockResultFactory;
    private final long fee;
    private final PoolPayer payer;
    private final String payoutAddress;
    private final Long startBlock;
    private final LocalTime payoutTime;

    private PoolState poolState;
    private PayoutFactory payoutFactory;

    public Pool(SemuxClient client, Persistence persistence, Set<String> delegates, Integer payOutNBlocks, BlockResultFactory blockResultFactory, long fee, PoolPayer payer, String payoutAddress, Long startBlock, LocalTime payoutTime)
    {
        this.client = client;
        this.persistence = persistence;
        this.delegates = delegates;
        this.payOutNBlocks = payOutNBlocks;
        this.blockResultFactory = blockResultFactory;
        this.fee = fee;
        this.payer = payer;
        this.payoutAddress = payoutAddress;
        this.startBlock = startBlock;
        this.payoutTime = payoutTime;
    }

    @Override
    public void run()
    {
        initializePoolState();

        long lastLog = 0;
        long currentBlock = poolState.getCurrentBlock();
        List<BlockResult> blockResults = new ArrayList<>();
        //once we're up to a block we
        boolean isSynced = false;
        //noinspection InfiniteLoopStatement
        while (true)
        {
            if (System.currentTimeMillis() > (lastLog + LOGGING_INTERVAL))
            {
                statusLogger.logState(poolState);
                lastLog = System.currentTimeMillis();
            }
            try
            {
                if (currentBlock % 10000 == 0)
                {
                    logger.info("Processed up to " + currentBlock);
                }
                Block block = getBlock(currentBlock);
                if (block != null)
                {
                    if (ourBlock(block))
                    {
                        logger.info("Forged block " + block.getNumber());
                        blockResults.add(blockResultFactory.getBlockResult(block));
                    }

                    if (shouldPay(currentBlock, isSynced))
                    {
                        logger.info("Calculating payments...");
                        Payout payout = payoutFactory.getPayoutForBlockResults(blockResults, poolState.getCurrentBlock());
                        if (payout != null && !payout.getPayouts().isEmpty())
                        {
                            //pay out
                            payer.pay(payout, poolState);
                            //update running state
                            poolState.addPayout(payout);
                            //clear existing block results so we don't pay again.
                            blockResults.clear();
                            //set the current paid up to block
                            poolState.setLastPayoutDate(payout.getDate());
                        }
                        else
                        {
                            poolState.setLastPayoutDate(new Date());
                        }
                        poolState.setCurrentBlock(currentBlock + 1);
                    }

                    //next loop will add new block
                    currentBlock++;
                }
                else if (!isSynced)
                {
                    logger.info("All caught up.");
                    //when we get a null block, we're up to date
                    isSynced = true;
                }
            }
            catch (IOException e)
            {
                logger.error("Error connecting to API", e);
            }
            catch (SemuxException e)
            {
                logger.error("Error from API", e);
            }

            try
            {
                if (isSynced)
                {
                    Thread.sleep(10000);
                }
            }
            catch (InterruptedException e)
            {
                //ignore
            }
        }
    }

    private Block getBlock(long currentBlock)
    {
        try
        {
            return client.getBlock(currentBlock);
        }
        catch (IOException | SemuxException e)
        {
            //most likely block not found
        }
        return null;
    }

    /**
     * Decide if it is time to pay
     *
     * @param currentBlock currentBlockId
     * @param isSynced     if we're synced to latest block
     * @return if it is time to flush payments
     */
    private boolean shouldPay(long currentBlock, boolean isSynced)
    {
        if (payoutTime != null)
        {
            LocalDateTime currentTime = LocalDateTime.now();

            Date lastPayout = poolState.getLastPayoutDate();
            if (lastPayout != null)
            {
                LocalDateTime lastPaymentDate = LocalDateTime.ofInstant(lastPayout.toInstant(), ZoneId.systemDefault());

                LocalDate dateOfPayment = lastPaymentDate.toLocalDate();
                dateOfPayment = dateOfPayment.plusDays(1);

                LocalDateTime targetDate = LocalDateTime.of(dateOfPayment, payoutTime);
                if (currentTime.compareTo(targetDate) > 0)
                {
                    return isSynced;
                }
            }
            else
            {
                //is it past that hour today
                if (currentTime.toLocalTime().compareTo(payoutTime) > 0)
                {
                    return isSynced;
                }
            }
            return false;
        }
        else
        {
            return (currentBlock > (poolState.getCurrentBlock() + payOutNBlocks)) && isSynced;
        }
    }

    /**
     * Check if this is a block we forged.
     *
     * @param block block
     * @return true if we forged
     */
    private boolean ourBlock(Block block)
    {
        return delegates.contains(block.getCoinbase());
    }

    private void initializePoolState()
    {
        try
        {
            poolState = persistence.loadPoolState();
            // also create the delegateName map
            Map<String, String> delegateNameMap = new HashMap<>();
            for (String delegateAddress : delegates)
            {
                Delegate delegate = client.getDelegate(delegateAddress);
                delegateNameMap.put(delegateAddress, delegate.getName());
            }
            //create the PayoutFactory
            payoutFactory = new PayoutFactory(delegateNameMap, payoutAddress, fee);

            if (startBlock > poolState.getCurrentBlock())
            {
                poolState.setCurrentBlock(startBlock);
            }
        }
        catch (IOException e)
        {
            logger.error("Unable to load pool state", e);
            System.exit(-1);
        }
        catch (SemuxException e)
        {
            logger.error("Unable to connect to API", e);
            System.exit(-1);
        }
    }
}
