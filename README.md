# semux-pool
Semux Pool Management 

# What is this?
This is pool software for [SEMUX](https://www.semux.org/), a distributed java-based blockchain.

This program allows you to pay out voters for your delegate/validator a share of your pool forgings.

Note:  There is no guarantees implied with this software.  It is purely use at your own risk.  The authors of this code cannot be held responsible for any bugs resulting in over payments or other issues.

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
