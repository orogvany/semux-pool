# semux-pool
Semux Pool Management 

# What is this?
This is pool software for [SEMUX](https://www.semux.org/), a distributed java-based blockchain.

This program allows you to pay out voters for your delegate/validator a share of your pool forgings.

Note:  There is no guarantees implied with this software.  It is purely use at your own risk.  The authors of this code cannot be held responsible for any bugs resulting in over payments or other issues.

# How Voting Works
SEMUX is a proof of stake blockchain.  Users burn (destroy) 1000 SEM, and become a delegate.  If they get enough votes (from themselves or others) to be in the top 100 delegates, they become a validator.  Validators take turns forging (mining) blocks, and get the block reward (3SEM) plus any transaction fees in that block.

Most delegates/validators do not share the SEM from the blocks they forge.  If you vote for random validators, you are not entitled to any of the block reward, so at present, most of the validators just vote for themselves, and will continue to grow.  This does not promote a healthy ecosystem!  Only the top 100 get all the SEM ever produced!  So a pool is required.

A pool allows a group of people who do not have enough SEM to have enough voting power to forge SEM, and grow with the network.  One person burns 1000 SEM, and then offers to share a % of their block rewards in exchange for votes.  This software makes it easy for those people to operate a pool and automate payouts.  It runs alongside the SEMUX client.  The client forges blocks, and this software calculates and distributes pool earnings based on how many votes each voter has made.

For example, if person A votes 100 and person B votes 800, and the pool operates with a 10% fee, person A is entitled to 10%, person B 80%, and the pool operator keeps 10% of each block reward.  

A validator should forge about 600 SEM per week, so a 10% fee would mean the pool operator would need to run for 17 weeks to break even from the initial 1000SEM startup cost.  After that, they could choose to lower their fee to remain competetive. 


# Contribute

If you find a bug, please submit it to [issues](https://github.com/orogvany/semux-pool/issues).

# Getting started
Download the [release](https://github.com/orogvany/semux-pool/releases).

Enable the REST API for your semux client, and note the user/pass/port

*** Before running, please make sure your delegate account does not contain non-pool funds!  You should make sure to protect yourself against bugs.  Nothing in this code even calls unlock/vote functionality, so locked funds should be safe, but best bet is to move them off ***

Update the semuxpool.properties file with the appropriate values for

```
delegateAddress
poolProfitsAddress
apiHost
apiUser
apiPass
```
and set ``debugMode`` to ``true``
Debug mode allows you to run without actually making payments while you configure and sanity check.

run ``semux-pool.sh`` (linux/OSX) or ``semux-pool.bat`` (windows) files to start it.

You will see json objects created in ``./payouts``  These are logs of your pool's paid transactions.  If you are running in debug mode check that these values match what you expect.

After you are satisfied, set ``debugMode`` to ``false``, delete the payouts directory and start it up.
