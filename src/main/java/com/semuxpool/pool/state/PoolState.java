package com.semuxpool.pool.state;

import com.semuxpool.pool.Constants;
import com.semuxpool.pool.api.Payment;
import com.semuxpool.pool.api.Payout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 */
public class PoolState {
    private static final Logger logger = LoggerFactory.getLogger(PoolState.class);
    private final Map<String, Long> unpaidBalances = new HashMap<>();
    private final Map<String, Long> paidBalances = new HashMap<>();

    //stats
    private long totalPoolProfits = 0;
    private long totalPaidOut = 0;
    private long totalUnpaid = 0;
    private long totalFeesPaid = 0;
    private long blocksForged = 0;

    private Set<String> delegates;
    private long currentBlock;
    private Date lastPayoutDate;


    public long getTotalPoolProfits() {
        return totalPoolProfits;
    }

    public void setTotalPoolProfits(long totalPoolProfits) {
        this.totalPoolProfits = totalPoolProfits;
    }

    public long getTotalPaidOut() {
        return totalPaidOut;
    }

    public void setTotalPaidOut(long totalPaidOut) {
        this.totalPaidOut = totalPaidOut;
    }

    public long getTotalUnpaid() {
        return totalUnpaid;
    }

    public long getTotalFeesPaid() {
        return totalFeesPaid;
    }

    public Set<String> getDelegates() {
        return delegates;
    }

    public void setDelegates(Set<String> delegates) {
        this.delegates = delegates;
    }

    public long getCurrentBlock() {
        return currentBlock;
    }

    public void setCurrentBlock(long currentBlock) {
        this.currentBlock = currentBlock;
    }

    public Map<String, Long> getUnpaidBalances() {
        return unpaidBalances;
    }

    public Map<String, Long> getPaidBalances() {
        return paidBalances;
    }

    public long getBlocksForged() {
        return blocksForged;
    }

    public synchronized void addPayout(Payout payouts) {
        totalPoolProfits += payouts.getPoolProfits();
        //for each set of blocks we owe, first add what we owe
        long subtotalUnpaid = 0;
        for (Map.Entry<String, Long> payout : payouts.getPayouts().entrySet()) {
            Long amount = add(payout.getValue(), unpaidBalances.get(payout.getKey()));
            unpaidBalances.put(payout.getKey(), amount);
            subtotalUnpaid += payout.getValue();
        }
        logger.info("SubtotalUnpaid:" + Constants.getInSEM(subtotalUnpaid));
        if (subtotalUnpaid != payouts.getTotalPayouts()) {
            logger.error("Calculated " + subtotalUnpaid + " payout but was " + payouts.getTotalPayouts());
        }
        totalUnpaid += subtotalUnpaid;

        //then subtract what we paid.
        long subtotalPaid = 0;
        for (Map.Entry<String, Payment> paid : payouts.getPaidPayouts().entrySet()) {
            Long paymentTotalWithFees = paid.getValue().getAmount() + payouts.getFee();
            Long amount = add(-paymentTotalWithFees, unpaidBalances.get(paid.getKey()));
            subtotalPaid += paymentTotalWithFees;
            totalPaidOut += paid.getValue().getAmount();
            totalFeesPaid += payouts.getFee();

            Long paidAmount = add(paid.getValue().getAmount(), paidBalances.get(paid.getKey()));

            paidBalances.put(paid.getKey(), paidAmount);

            if (amount != 0) {
                if (amount < 0) {
                    //this occurs in bugs in early versions.  We'll just carry negative balance forward
                    logger.error("Found negative balance for " + paid.getKey() + " : " + amount);
                }

                unpaidBalances.put(paid.getKey(), amount);
            } else {
                unpaidBalances.remove(paid.getKey());
            }
        }
        logger.info("SubtotalPaid:" + Constants.getInSEM(subtotalPaid));
        totalUnpaid -= subtotalPaid;

        //now unpaid balances include everything still owed.
        blocksForged += payouts.getBlocksForged().size();
    }

    /**
     * Null safe add
     *
     * @param a a
     * @param b b
     * @return sum
     */
    private Long add(Long a, Long b) {
        if (a == null) {
            a = 0l;
        }
        if (b == null) {
            b = 0l;
        }
        return a + b;
    }

    public Date getLastPayoutDate() {
        return lastPayoutDate;
    }

    public void setLastPayoutDate(Date lastPayoutDate) {
        this.lastPayoutDate = lastPayoutDate;
    }
}
