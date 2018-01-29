package com.semuxpool.pool;

import com.semuxpool.client.SemuxClient;
import com.semuxpool.client.api.SemuxException;
import com.semuxpool.client.api.TransactionLimits;
import com.semuxpool.pool.block.BlockResultFactory;
import com.semuxpool.pool.pay.PoolPayer;
import com.semuxpool.pool.persistence.JsonPersistence;
import com.semuxpool.pool.persistence.Persistence;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * loads configuration and starts pool
 */
public class PoolRunner
{
    public static void main(String[] args) throws IOException, SemuxException
    {
        Properties properties = new Properties();
        if (args.length > 0)
        {
            properties.load(new FileInputStream(new File("./config/" + args[0])));
        }
        else
        {
            properties.load(new FileInputStream(new File("./config/semuxpool.properties")));
        }

        Set<String> delegates = new HashSet<>();

        //will eventually support multiple pools per payer
        String[] delegatesAr = properties.getProperty("delegateAddress").split(",");
        Collections.addAll(delegates, delegatesAr);

        String host = properties.getProperty("apiHost");
        String payoutsDirectory = properties.getProperty("dataDirectory");
        String user = properties.getProperty("apiUser");
        String password = properties.getProperty("apiPass");
        int port = Integer.valueOf(properties.getProperty("apiPort"));
        float poolPayoutPercent = Math.max(0, Float.valueOf(properties.getProperty("poolFeePercent")));
        String note = properties.getProperty("paymentNote");
        float donationPercent = Math.max(0, Float.valueOf(properties.getProperty("developerDonationPercent")));
        Integer payoutEveryBlock = Integer.valueOf(properties.getProperty("payoutEveryNBlocks"));
        String payoutTimeString = properties.getProperty("payoutTime");
        LocalTime payoutTime = null;
        if (payoutTimeString != null)
        {
            payoutEveryBlock = null;
            payoutTime = LocalTime.parse(payoutTimeString);
        }
        boolean debugMode = Boolean.valueOf(properties.getProperty("debugMode"));
        String poolProfitsAddress = properties.getProperty("poolProfitsAddress");

        int minPayoutMultiplier = Integer.valueOf(properties.getProperty("minPayoutMultiplier"));
        //client
        SemuxClient client = new SemuxClient(host, port, user, password);
        client.setMockPayments(debugMode);

        long blockReward = 1 * Constants.SEM;
        long fee = 50_000_000l;

        long startBlock = Long.valueOf(properties.getProperty("startProcessingAtBlock"));

        try
        {
            TransactionLimits transactionLimits = client.getTransactionLimits("TRANSFER");
            blockReward = 3 * Constants.SEM;
            fee = transactionLimits.getMinTransactionFee();
        }
        catch (Exception e)
        {
            //old API in use, just use defaults
        }
        long minPayout = fee * minPayoutMultiplier;

        //persistence
        Persistence persistence = new JsonPersistence(payoutsDirectory);
        BlockResultFactory blockResultFactory = new BlockResultFactory(client, poolPayoutPercent, donationPercent, blockReward, poolProfitsAddress);

        //payer
        PoolPayer payer = new PoolPayer(client, delegates, delegates.iterator().next(), persistence, fee, minPayout, note);

        Pool pool = new Pool(
            client, persistence, delegates, payoutEveryBlock, blockResultFactory,
            fee, payer, poolProfitsAddress, startBlock, payoutTime);
        pool.run();
    }
}
