package com.semuxpool.pool.pay;

import com.semuxpool.client.SemuxClient;
import com.semuxpool.client.api.Account;
import com.semuxpool.client.api.SemuxException;
import com.semuxpool.pool.api.Payment;
import com.semuxpool.pool.api.Payout;
import com.semuxpool.pool.persistence.Persistence;
import com.semuxpool.pool.state.PoolState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

/**
 * Handle sending payments
 */
public class PoolPayer
{
    private static final Logger logger = LoggerFactory.getLogger(PoolPayer.class);
    private final SemuxClient client;
    private final String delegateAddress;
    private final Persistence persistence;
    private final long fee;
    private final long minPayout;
    private final String note;

    public PoolPayer(SemuxClient client, String delegateAddress, Persistence persistence, long fee, long minPayout, String note)
    {
        this.client = client;
        this.delegateAddress = delegateAddress;
        this.persistence = persistence;
        this.fee = fee;
        this.minPayout = minPayout;
        this.note = note;
    }

    public void pay(Payout payout, PoolState poolState) throws IOException, SemuxException
    {
        //save this payout
        persistence.persistPayout(payout);

        //get the current balance
        Account account = client.getAccount(delegateAddress);
        Long payoutAddressBalance = account.getAvailable();

        for (Map.Entry<String, Long> pay : payout.getPayouts().entrySet())
        {
            boolean includesDeferred = false;
            long amountToSend = pay.getValue() - fee;
            //look for any owed money and add it to it;
            Long unpaid = poolState.getUnpaidBalances().get(pay.getKey());
            if (unpaid != null)
            {
                amountToSend += unpaid;
                includesDeferred = true;
            }

            try
            {
                //make sure we haven't already sent it
                if (payout.getPaidPayouts().get(pay.getKey()) != null)
                {
                    continue;
                }
                //make sure its enough to pay out
                if (amountToSend < minPayout)
                {
                    continue;
                }
                //make sure we have enough balance
                if (payoutAddressBalance < amountToSend)
                {
                    logger.warn("Pool has insufficient balance to pay!  Deferring payment.");
                    continue;
                }

                if (unpaid != null && unpaid > 0)
                {
                    logger.debug("Paying " + pay.getKey() + " " + unpaid + " deferred payment");
                }
                //pay them
                String confirmation;

                //if we voted for our own pool, just mark it as paid
                if (delegateAddress.contains(pay.getKey()))
                {
                    confirmation = "SELF";
                }
                else
                {
                    confirmation = client.transfer(amountToSend, delegateAddress, pay.getKey(), fee, note.getBytes());
                }

                payoutAddressBalance -= (amountToSend + fee);

                Payment payment = new Payment();
                payment.setDate(new Date());
                payment.setAmount(amountToSend);
                payment.setHash(confirmation);
                payment.setTo(pay.getKey());
                payment.setIncludesPriorOwed(includesDeferred);
                payout.getPaidPayouts().put(pay.getKey(), payment);
                //update immediately in case of crash
                persistence.update(payout);
            }
            catch (SemuxException e)
            {
                logger.error("Unable to send payment to " + delegateAddress, e);
            }
        }

        //update again just to be safe.
        persistence.update(payout);
    }
}
