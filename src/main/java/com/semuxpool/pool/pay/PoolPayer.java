package com.semuxpool.pool.pay;

import com.semuxpool.client.SemuxClient;
import com.semuxpool.client.api.Account;
import com.semuxpool.client.api.SemuxException;
import com.semuxpool.pool.api.Payment;
import com.semuxpool.pool.api.Payout;
import com.semuxpool.pool.block.BlockResultFactory;
import com.semuxpool.pool.persistence.Persistence;
import com.semuxpool.pool.state.PoolState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Set;

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
    //whether to count votes from voter's who left in the middle of the cycle
    private final boolean dontPayPoolQuitters;
    private final String payQuitterAddress;
    private final Set<String> poolAddresses;

    public PoolPayer(SemuxClient client, String delegateAddress, Persistence persistence, long fee,
        long minPayout, String note, boolean dontPayPoolQuitters, String payQuitterAddress, Set<String> poolAddresses)
    {
        this.client = client;
        this.delegateAddress = delegateAddress;
        this.persistence = persistence;
        this.fee = fee;
        this.minPayout = minPayout;
        this.note = note;
        this.dontPayPoolQuitters = dontPayPoolQuitters;
        this.payQuitterAddress = payQuitterAddress;
        this.poolAddresses = poolAddresses;
        //if donation amount set, add it to our known addresses set.
        //if no donation amount set, this doesn't do anything.
        poolAddresses.add(BlockResultFactory.DONATION_ADDRESS);
    }

    public void pay(Payout payout, PoolState poolState) throws IOException, SemuxException
    {
        //save this payout
        persistence.persistPayout(payout);

        //get the current balance
        Account account = client.getAccount(delegateAddress);
        Long payoutAddressBalance = account.getAvailable();

        Map<String, Long> currentVotes = client.getVotes(delegateAddress);

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
                else if (dontPayPoolQuitters && !isCurrentVoter(pay.getKey(), currentVotes))
                {
                    //if someone has unvoted for pool, this hurts the pool, so their last day's payout is void
                    //if they leave some in the pool, they'll be paid out as normal.
                    //this is only enabled if the pool operator has configured it to do so (default off)
                    //Requested feature by a pool operator.
                    client.transfer(amountToSend, delegateAddress, payQuitterAddress, fee, note.getBytes());
                    confirmation = "VOID";
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

    private boolean isCurrentVoter(String address, Map<String, Long> currentVotes)
    {
        //this is a pool operation address, it gets a pass
        if (poolAddresses.contains(address))
        {
            return true;
        }
        Long current = currentVotes.get(address);
        return current != null && current > 0;
    }
}
