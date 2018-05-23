package com.semuxpool.pool;

/**
 */
public class Constants {
    public static final long SEM = 1_000_000_000;

    public static String getInSEM(long amount) {
        return String.format("%.8f", amount / (float) Constants.SEM);
    }
}
