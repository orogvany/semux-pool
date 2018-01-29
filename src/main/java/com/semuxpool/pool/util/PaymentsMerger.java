package com.semuxpool.pool.util;

import com.semuxpool.pool.api.Payment;
import com.semuxpool.pool.api.Payout;
import com.semuxpool.pool.persistence.JsonPersistence;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A mostly useless class
 * If you find that payments were double paid
 * Mostly due to developer error
 * you can combine sets
 */
public class PaymentsMerger
{
    public static void main(String[] args) throws IOException
    {
        JsonPersistence jsonPersistence = new JsonPersistence("./fixme");
        List<Payout> payouts = jsonPersistence.getAllPayouts();
        Payout merged = new Payout();
        Map<String, Payment> mergedPaid = new HashMap<>();
        for (Payout payout : payouts)
        {
            Map<String, Payment> paid = payout.getPaidPayouts();
            for (Map.Entry<String, Payment> entry : paid.entrySet())
            {
                Payment mergedPayment = mergedPaid.get(entry.getKey());
                Payment toMerge = entry.getValue();
                if (mergedPayment == null)
                {
                    mergedPayment = new Payment();
                    mergedPayment.setDate(toMerge.getDate());
                    mergedPayment.setAmount(0l);
                    mergedPayment.setHash("MERGED:");
                    mergedPayment.setIncludesPriorOwed(toMerge.isIncludesPriorOwed());
                    mergedPayment.setTo(toMerge.getTo());
                    mergedPaid.put(entry.getKey(), mergedPayment);
                }
                //update with new
                if (mergedPayment.getAmount() > 0)
                {
                    //consolidate the fees, since we only accounted for 1
                    mergedPayment.setAmount(mergedPayment.getAmount() + payout.getFee());
                }
                mergedPayment.setAmount(mergedPayment.getAmount() + toMerge.getAmount());

                mergedPayment.setHash(mergedPayment.getHash() + toMerge.getHash() + ",");
                mergedPayment.setIncludesPriorOwed(mergedPayment.isIncludesPriorOwed() || toMerge.isIncludesPriorOwed());
            }
        }
        merged.setPaidPayouts(mergedPaid);
        jsonPersistence.persistPayout(merged);
    }
}
