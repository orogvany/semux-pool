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
import java.util.Properties;

/**
 * loads configuration and starts pool
 */
public class PoolRunner
{
    public static void main(String[] args) throws IOException, SemuxException
    {
        //
        // Load properties
        Properties properties = new Properties();
        if (args.length > 0)
        {
            properties.load(new FileInputStream(new File("./config/" + args[0])));
        }
        else
        {
            properties.load(new FileInputStream(new File("./config/semuxpool.properties")));
        }

        //
        // Read configuration
        String delegateAddress = properties.getProperty("delegateAddress");
        String host = properties.getProperty("apiHost");
        String payoutsDirectory = properties.getProperty("dataDirectory");
        String user = properties.getProperty("apiUser");
        String password = properties.getProperty("apiPass");
        int port = Integer.valueOf(properties.getProperty("apiPort"));
        float poolPayoutPercent = Math.max(0, Float.valueOf(properties.getProperty("poolFeePercent")));
        String note = properties.getProperty("paymentNote");
        float donationPercent = Math.max(0, Float.valueOf(properties.getProperty("developerDonationPercent")));
        Integer payoutEveryBlock = Integer.valueOf(properties.getProperty("payoutEveryNBlocks"));
        Integer loggingInterval = Integer.valueOf(properties.getProperty("loggingIntervalMs"));
        String payoutTimeString = properties.getProperty("payoutTime");
        boolean debugMode = Boolean.valueOf(properties.getProperty("debugMode"));
        String poolProfitsAddress = properties.getProperty("poolProfitsAddress");
        Boolean submitToAggregationSite = Boolean.valueOf(properties.getProperty("submitToAggregationSite"));
        long startBlock = Long.valueOf(properties.getProperty("startProcessingAtBlock"));
        float minPayoutSem = Float.valueOf(properties.getProperty("minPayoutSem"));
        long minPayout = (long) (minPayoutSem * Constants.SEM);

        LocalTime payoutTime = null;
        if (payoutTimeString != null)
        {
            payoutEveryBlock = null;
            payoutTime = LocalTime.parse(payoutTimeString);
        }


        //
        //client
        SemuxClient client = new SemuxClient(host, port, user, password);
        client.setMockPayments(debugMode);

        //
        // Try to look up block reward, else use defaults
        long blockReward = 3 * Constants.SEM;
        long fee = 5_000_000l;

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

        //
        //persistence
        Persistence persistence = new JsonPersistence(payoutsDirectory);
        BlockResultFactory blockResultFactory = new BlockResultFactory(client, poolPayoutPercent, donationPercent, blockReward, poolProfitsAddress);

        //
        //payer
        PoolPayer payer = new PoolPayer(client, delegateAddress, persistence, fee, minPayout, note);

        Pool pool = new Pool(
            client, persistence, delegateAddress, payoutEveryBlock, blockResultFactory,
            fee, payer, startBlock, payoutTime, loggingInterval);
        pool.run();
    }
}
