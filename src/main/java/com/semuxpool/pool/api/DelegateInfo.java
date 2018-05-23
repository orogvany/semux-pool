package com.semuxpool.pool.api;

/**
 * Information about a delegate
 */
public class DelegateInfo {
    private String name;
    private String address;
    private Long blocksForged;
    private Float votesSem;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Long getBlocksForged() {
        return blocksForged;
    }

    public void setBlocksForged(Long blocksForged) {
        this.blocksForged = blocksForged;
    }

    public Float getVotesSem() {
        return votesSem;
    }

    public void setVotesSem(Float votesSem) {
        this.votesSem = votesSem;
    }
}
