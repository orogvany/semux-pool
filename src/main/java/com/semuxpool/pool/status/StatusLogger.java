package com.semuxpool.pool.status;

import com.semuxpool.pool.Constants;
import com.semuxpool.pool.state.PoolState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Logging for current state of pool payouts
 */
public class StatusLogger
{
    private static final Logger logger = LoggerFactory.getLogger(StatusLogger.class);

    public void logState(PoolState poolState)
    {
        logger.info("=====================");
        logger.info("Block: " + poolState.getCurrentBlock());
        logger.info("Pool profit: " + Constants.getInSEM(poolState.getTotalPoolProfits()));
        logger.info("Pool paid: " + Constants.getInSEM(poolState.getTotalPaidOut()));
        logger.info("Pool unpaid: " + Constants.getInSEM(poolState.getTotalUnpaid()));
        logger.info("Pool fees paid: " + Constants.getInSEM(poolState.getTotalFeesPaid()));
        long totalAllTime = poolState.getTotalPaidOut() + poolState.getTotalFeesPaid() + poolState.getTotalUnpaid();
        logger.info("Pool forged: " + Constants.getInSEM(totalAllTime));
        logger.info("Blocks forged: " + poolState.getBlocksForged());
        float poolFee = (float) poolState.getTotalPoolProfits() / (float) totalAllTime;
        logger.info("Effective Pool Earnings %: " + poolFee * 100.0f + "%");
        logger.info("Total Unpaid Addresses: " + poolState.getUnpaidBalances().size());
        logger.info("=====================");
        logger.info("Current unpaid balances");
        long calculatedUnpaid = 0;
        long calculatedOverpaid = 0;
        for (Map.Entry<String, Long> unpaid : poolState.getUnpaidBalances().entrySet())
        {
            if (unpaid.getValue() > 0)
            {
                calculatedUnpaid += unpaid.getValue();
            }
            else
            {
                calculatedOverpaid -= unpaid.getValue();
            }
            logger.info(unpaid.getKey() + " : " + Constants.getInSEM(unpaid.getValue()));
        }
        logger.info("Pool unpaid (sanity): " + Constants.getInSEM(calculatedUnpaid));
        logger.info("Pool overpaid (sanity): " + Constants.getInSEM(calculatedOverpaid));
        logger.info("Accounted for: " + (float) (calculatedUnpaid - calculatedOverpaid) / (float) poolState.getTotalUnpaid() * 100 + "%");
        logger.info("=====================");
        logger.info("Current paid balances");
        long calculatedPaid = 0;
        for (Map.Entry<String, Long> paid : poolState.getPaidBalances().entrySet())
        {
            calculatedPaid += paid.getValue();
            logger.info(paid.getKey() + " : " + Constants.getInSEM(paid.getValue()));
        }
        logger.info("Pool paid (sanity): " + Constants.getInSEM(calculatedPaid));
        logger.info("Accounted for: " + (float) calculatedPaid / (float) poolState.getTotalPaidOut() * 100 + "%");
        logger.info("=====================");
    }

}
