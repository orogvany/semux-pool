package com.semuxpool.pool.persistence;

import com.semuxpool.pool.api.Payout;
import com.semuxpool.pool.state.PoolState;

import java.io.IOException;

/**
 * Persistence for pool stats
 */
public interface Persistence
{
    String persistPayout(Payout payout);

    PoolState loadPoolState() throws IOException;

    void update(Payout payout);
}
