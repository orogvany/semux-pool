package com.semuxpool.pool.pay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains information about multiple payouts
 */
public class PoolProfitAddresses
{
    private static final Logger logger = LoggerFactory.getLogger(PoolProfitAddresses.class);

    private final Map<String, Float> addressToPercent = new HashMap<>();

    public static PoolProfitAddresses fromString(String addresses)
    {
        String[] addressBlocks = addresses.split(",");

        PoolProfitAddresses ret = new PoolProfitAddresses();
        Float totalPercent = 0f;
        for (String address : addressBlocks)
        {
            String[] namePercent = address.split(":");
            Float percent = 1.0f;
            if (namePercent.length > 1)
            {
                percent = Float.valueOf(namePercent[1].trim());
            }
            totalPercent += percent;
            ret.addressToPercent.put(namePercent[0].trim(), percent);
        }
        if (totalPercent < 0.999f || totalPercent > 1.001f)
        {
            throw new InternalError("Total percent for pool addresses must equal 1.0");
        }

        return ret;
    }

    public Collection<? extends String> getAddresses()
    {
        return addressToPercent.keySet();
    }

    public Float getPercent(String address)
    {
        return addressToPercent.get(address);
    }
}
