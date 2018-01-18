package com.semuxpool.pool.api;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 */
public class BlockResultTest
{

    @Test
    public void testPayoutPercentage()
    {
        BlockResult result = new BlockResult();
        Map<String, Long> votes = new HashMap<>();
        votes.put("foo", 100l);
        votes.put("bar", 100l);
        votes.put("pool", 100l);
        result.setVotes(votes);
        result.setTotalVotes(300l);
        result.setBlockReward(1000000l);

        Map<String, Long> payouts = result.getPayouts();

        System.out.println(payouts.get("foo"));
        System.out.println(payouts.get("bar"));
        System.out.println(payouts.get("pool"));


    }
}
