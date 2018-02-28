package com.semuxpool.pool.pay;

import org.junit.Assert;
import org.junit.Test;

/**
 */
public class PoolProfitAddressesTest
{

    @Test
    public void testOneAddress()
    {
        PoolProfitAddresses add = PoolProfitAddresses.fromString("0x123");
        Assert.assertEquals(1, add.getAddresses().size());
        Assert.assertEquals(new Float(1.0f), add.getPercent(add.getAddresses().iterator().next()));
    }

    @Test
    public void testPercentsNot100()
    {
        try
        {
            PoolProfitAddresses add = PoolProfitAddresses.fromString("0x123:0.9,0x456:0.2");
            Assert.fail();
        }
        catch (InternalError e)
        {
            //expected
        }
    }

    @Test
    public void testMultipleAddresses()
    {
        PoolProfitAddresses add = PoolProfitAddresses.fromString("0x123:.10, 0x456:.80,0x789:0.1");
        Assert.assertEquals(3, add.getAddresses().size());

        Assert.assertEquals(new Float(0.10f), add.getPercent("0x123"));
        Assert.assertEquals(new Float(0.80f), add.getPercent("0x456"));
        Assert.assertEquals(new Float(0.10f), add.getPercent("0x789"));
    }
}
