package com.semuxpool.pool.api;

import java.util.Date;

/**
 * A payment made to a voter
 */
public class Payment {
    private String to;
    private Date date;
    private Long amount;
    private String hash;
    private boolean includesPriorOwed;

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    /**
     * If this payment includes back-due payments for payment periods
     * that were less than minimum payments
     */
    public boolean isIncludesPriorOwed() {
        return includesPriorOwed;
    }

    public void setIncludesPriorOwed(boolean includesPriorOwed) {
        this.includesPriorOwed = includesPriorOwed;
    }
}
