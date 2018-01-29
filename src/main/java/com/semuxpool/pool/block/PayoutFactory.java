package com.semuxpool.pool.block;

import com.semuxpool.pool.api.BlockResult;
import com.semuxpool.pool.api.Payout;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Construct Payouts from BlockResults
 */
public class PayoutFactory
{
    private Map<String, String> delegateNameMap = new HashMap<>();
    //the address for pool profits to be paid out to.
    private final String payoutAddress;
    private final long fee;

    public PayoutFactory(Map<String, String> delegateNameMap, String payoutAddress,  long fee)
    {
        this.delegateNameMap = delegateNameMap;
        this.payoutAddress = payoutAddress;
        this.fee = fee;
    }

    /**
     * Construct a Payout object from a collection of forged block results.
     *
     * @param blocks       blocks
     * @param startBlockId startBlockId
     * @return Payout
     */
    public synchronized Payout getPayoutForBlockResults(List<BlockResult> blocks, Long startBlockId)
    {
        Payout payout = new Payout();
        if(blocks.isEmpty() )
        {
            return null;
        }


        long totalPoolFees = 0l;
        TreeMap<Long, String> blocksForged = new TreeMap<>();
        for (BlockResult blockResult : blocks)
        {
            blocksForged.put(blockResult.getBlockId(), delegateNameMap.get(blockResult.getDelegate()));

            //add up pool fees
            Long poolProfit = blockResult.getPayouts().get(payoutAddress);
            if(poolProfit != null)
            {
                totalPoolFees += poolProfit;
            }
        }
        payout.setBlocksForged(blocksForged);

        payout.setStartBlock(startBlockId);

        payout.setEndBlock(blocks.get(blocks.size() - 1).getBlockId());

        payout.setPayouts(getPayouts(blocks));
        payout.setPoolProfits(totalPoolFees);
        payout.setFee(fee);
        payout.setDate(new Date());

        return payout;
    }

    private synchronized Map<String, Long> getPayouts(List<BlockResult> blockResults)
    {
        Map<String, Long> unPaidPayouts = new HashMap<>();
        for (BlockResult result : blockResults)
        {
            for (Map.Entry<String, Long> payout : result.getPayouts().entrySet())
            {
                Long val = unPaidPayouts.get(payout.getKey());
                if (val == null)
                {
                    val = 0l;
                }
                val += payout.getValue();
                unPaidPayouts.put(payout.getKey(), val);
            }
        }

        return unPaidPayouts;
    }
}
