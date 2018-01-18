package com.semuxpool.pool.api;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * Payout results for a single given forged block.
 */
public class BlockResult
{
    private Long blockId;
    private Long totalVotes;
    private Map<String, Long> votes;
    private Long blockReward;
    private String delegate;

    public Long getTotalVotes()
    {
        return totalVotes;
    }

    public void setTotalVotes(Long totalVotes)
    {
        this.totalVotes = totalVotes;
    }

    public Map<String, Long> getVotes()
    {
        return votes;
    }

    public void setVotes(Map<String, Long> votes)
    {
        this.votes = votes;
    }

    public Long getBlockReward()
    {
        return blockReward;
    }

    public void setBlockReward(Long blockReward)
    {
        this.blockReward = blockReward;
    }

    @JsonIgnore
    public Map<String, Long> getPayouts()
    {
        Map<String, Long> payout = new HashMap<>();
        for (Map.Entry<String, Long> voter : votes.entrySet())
        {
            payout.put(voter.getKey(), getPartialReward(voter.getValue()));
        }
        return payout;
    }

    /**
     * Get's the amount for the block that is due given a certain amount of votes
     * @param value value
     * @return partial reward
     */
    private Long getPartialReward(Long value)
    {
        BigInteger results = BigInteger.valueOf(getBlockReward()).multiply(BigInteger.valueOf(value)).divide(BigInteger.valueOf(totalVotes));
        return results.longValue();
    }

    public void setDelegate(String delegate)
    {
        this.delegate = delegate;
    }

    public String getDelegate()
    {
        return delegate;
    }

    public Long getBlockId()
    {
        return blockId;
    }

    public void setBlockId(Long blockId)
    {
        this.blockId = blockId;
    }
}
