package com.semuxpool.pool;

import com.semuxpool.client.SemuxClient;
import com.semuxpool.client.api.SemuxException;
import com.semuxpool.client.api.TransactionLimits;
import com.semuxpool.pool.block.BlockResultFactory;
import com.semuxpool.pool.pay.PoolPayer;
import com.semuxpool.pool.pay.PoolProfitAddresses;
import com.semuxpool.pool.persistence.JsonPersistence;
import com.semuxpool.pool.persistence.Persistence;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * loads configuration and starts pool
 */
public class PoolRunner {
    public static void main(String[] args) throws IOException, SemuxException {
        //
        // Load properties
        Properties properties = new Properties();
        if (args.length > 0) {
            properties.load(new FileInputStream(new File("./config/" + args[0])));
        } else {
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
        boolean signLocal = Boolean.valueOf(properties.getProperty("signLocal", "false"));
        String delegatePrivateKey = properties.getProperty("delegatePrivateKey", null);
        float poolPayoutPercent = Math.max(0, Float.valueOf(properties.getProperty("poolFeePercent")));
        String note = properties.getProperty("paymentNote", "semuxpool.com");
        float developerBeerFundPercent = Math.max(0, Float.valueOf(properties.getProperty("developerBeerFundPercent", "0.05")));
        Set<String> poolAddresses = new HashSet<>();
        poolAddresses.add(delegateAddress);
        Integer loggingInterval = Integer.valueOf(properties.getProperty("loggingIntervalMs", "3600000"));
        String payoutTimeString = properties.getProperty("payoutTime", "13:00");
        boolean debugMode = Boolean.valueOf(properties.getProperty("debugMode", "false"));
        //handle pool quitters
        boolean dontPayPoolQuitters = Boolean.valueOf(properties.getProperty("dontPayPoolQuitters", "false"));
        String payQuitterAddress = properties.getProperty("poolQuitterAddress", "");
        if (!payQuitterAddress.isEmpty()) {
            poolAddresses.add(payQuitterAddress);
        }
        Integer minimumVoteAgeBeforeCounting = Integer.valueOf(properties.getProperty("minimumVoteAgeBeforeCounting", "200"));
        PoolProfitAddresses poolProfitsAddress = PoolProfitAddresses.fromString(properties.getProperty("poolProfitsAddress"));
        Set<String> voterWhitelist = new HashSet<>();
        String[] voterWhitelistString = properties.getProperty("voterWhiteList", "").split(",");
        for (String whitelist : voterWhitelistString) {
            if (!whitelist.trim().isEmpty()) {
                voterWhitelist.add(whitelist.trim());
            }
        }
        Set<String> voterBlacklist = new HashSet<>();
        String[] voterBlacklistString = properties.getProperty("voterBlackList", "").split(",");
        for (String blacklist : voterBlacklistString) {
            if (!blacklist.trim().isEmpty()) {
                voterBlacklist.add(blacklist.trim());
            }
        }

        poolAddresses.addAll(poolProfitsAddress.getAddresses());
        boolean submitToAggregationSite = Boolean.valueOf(properties.getProperty("submitToAggregationSite", "false"));
        long startBlock = Long.valueOf(properties.getProperty("startProcessingAtBlock", "0"));
        float minPayoutSem = Float.valueOf(properties.getProperty("minPayoutSem", "0.05"));
        long minPayout = (long) (minPayoutSem * Constants.SEM);

        LocalTime payoutTime = null;
        if (payoutTimeString != null) {
            payoutTime = LocalTime.parse(payoutTimeString);
        }

        //
        //client
        SemuxClient client = new SemuxClient(host, port, user, password);
        client.setMockPayments(debugMode);

        //
        // Try to look up block reward, else use defaults
        long fee = 5_000_000l;

        try {
            TransactionLimits transactionLimits = client.getTransactionLimits("TRANSFER");
            fee = transactionLimits.getMinTransactionFee();
        } catch (Exception e) {
            //old API in use, just use defaults
        }

        //
        //persistence
        Persistence persistence = new JsonPersistence(payoutsDirectory);
        BlockResultFactory blockResultFactory = new BlockResultFactory(
                client, poolPayoutPercent, developerBeerFundPercent, poolProfitsAddress, minimumVoteAgeBeforeCounting, voterWhitelist, voterBlacklist);

        //
        //payer
        PoolPayer payer = new PoolPayer(signLocal, delegatePrivateKey, client, delegateAddress, persistence, fee, minPayout, note, dontPayPoolQuitters,
                payQuitterAddress, poolAddresses);

        Pool pool = new Pool(
                client, persistence, delegateAddress, blockResultFactory,
                fee, payer, startBlock, payoutTime, loggingInterval);
        pool.run();
    }
}
