package com.semuxpool.pool.api;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Defines a logged payment over a given block range
 */
public class Payout
{
    private Map<String, Long> payouts = new HashMap<>();
    //address to payment confirmation address
    private Map<String, com.semuxpool.pool.api.Payment> paidPayouts = new HashMap<>();
    private Long startBlock;
    private Long endBlock;
    private long poolProfits;
    private long totalPayouts;
    private TreeMap<Long, String> blocksForged;
    private Long fee;
    //unique identifier for payout set
    private String id;
    private Date date;

    public Map<String, Long> getPayouts()
    {
        return payouts;
    }

    public void setPayouts(Map<String, Long> payouts)
    {
        this.payouts = payouts;
        long total = 0l;
        for (Long payout : payouts.values())
        {
            total += payout;
        }
        setTotalPayouts(total);
    }

    public Long getStartBlock()
    {
        return startBlock;
    }

    public void setStartBlock(Long startBlock)
    {
        this.startBlock = startBlock;
    }

    public Long getEndBlock()
    {
        return endBlock;
    }

    public void setEndBlock(Long endBlock)
    {
        this.endBlock = endBlock;
    }

    public long getPoolProfits()
    {
        return poolProfits;
    }

    public void setPoolProfits(long poolProfits)
    {
        this.poolProfits = poolProfits;
    }

    public Map<String, com.semuxpool.pool.api.Payment> getPaidPayouts()
    {
        return paidPayouts;
    }

    public void setPaidPayouts(Map<String, com.semuxpool.pool.api.Payment> paidPayouts)
    {
        this.paidPayouts = paidPayouts;
    }

    public long getTotalPayouts()
    {
        return totalPayouts;
    }

    public void setTotalPayouts(long totalPayouts)
    {
        this.totalPayouts = totalPayouts;
    }

    public TreeMap<Long, String> getBlocksForged()
    {
        return blocksForged;
    }

    public void setBlocksForged(TreeMap<Long, String> blocksForged)
    {
        this.blocksForged = blocksForged;
    }

    public Long getFee()
    {
        return fee;
    }

    public void setFee(Long fee)
    {
        this.fee = fee;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public Date getDate()
    {
        return date;
    }

    public void setDate(Date date)
    {
        this.date = date;
    }
}
