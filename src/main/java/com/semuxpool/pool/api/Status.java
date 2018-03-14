package com.semuxpool.pool.api;

import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * Pools current status
 *
 * TODO - WIP
 */
public class Status
{
    private Long currentBlock;
    private Long poolPaid;
    private Map<String, com.semuxpool.pool.api.Payment> payments;
    private Set<com.semuxpool.pool.api.DelegateInfo> delegates;
    private Date updated;

    public Status()
    {
    }

    public Long getCurrentBlock()
    {
        return currentBlock;
    }

    public void setCurrentBlock(Long currentBlock)
    {
        this.currentBlock = currentBlock;
    }

    public Long getPoolPaid()
    {
        return poolPaid;
    }

    public void setPoolPaid(Long poolPaid)
    {
        this.poolPaid = poolPaid;
    }

    public Map<String, com.semuxpool.pool.api.Payment> getPayments()
    {
        return payments;
    }

    public void setPayments(Map<String, com.semuxpool.pool.api.Payment> payments)
    {
        this.payments = payments;
    }

    public Set<com.semuxpool.pool.api.DelegateInfo> getDelegates()
    {
        return delegates;
    }

    public void setDelegates(Set<com.semuxpool.pool.api.DelegateInfo> delegates)
    {
        this.delegates = delegates;
    }

    public Date getUpdated()
    {
        return updated;
    }

    public void setUpdated(Date updated)
    {
        this.updated = updated;
    }
}
