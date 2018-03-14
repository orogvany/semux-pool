package com.semuxpool.pool;

import com.semuxpool.client.SemuxClient;
import com.semuxpool.client.api.Block;
import com.semuxpool.client.api.SemuxException;
import com.semuxpool.pool.api.Payout;
import com.semuxpool.pool.persistence.JsonPersistence;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

/**
 * Small util to verify payments
 */
public class PoolChecker
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

        String payoutsDirectory = properties.getProperty("dataDirectory");


        String delegateAddress = properties.getProperty("delegateAddress");
        String host = properties.getProperty("apiHost");
        String user = properties.getProperty("apiUser");
        String password = properties.getProperty("apiPass");
        int port = Integer.valueOf(properties.getProperty("apiPort"));


        SemuxClient client = new SemuxClient(host, port, user, password);


        JsonPersistence persistence = new JsonPersistence(payoutsDirectory);
        List<Payout> payouts = persistence.getAllPayouts();
        Set<Long> blocksForged = new HashSet<>();
        for(Payout payout : payouts)
        {
            TreeMap<Long, String> forged = payout.getBlocksForged();
            blocksForged.addAll(forged.keySet());
        }

        //let's scan all blocks since beginning of time and see what's missing
        Long totalBlocks = (long) client.getInfo().getLatestBlockNumber();
        for(long i =0; i<= totalBlocks;i++)
        {
            Block block = client.getBlock(i);
            if(block.getCoinbase().equals(delegateAddress))
            {
                if(!blocksForged.contains(block.getNumber()))
                {
                    System.out.println("Missed block " + block.getNumber());
                }
            }
        }
    }
}
