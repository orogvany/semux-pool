# semux-pool
Semux Pool Management 

This program allows you to pay out voters a share of your pool forgings on a specified schedule.

Note:  There is no guarantees implied with this software.  It is purely use at your own risk.  The authors of this code cannot be held responsible for any bugs resulting in over payments or other issues.

# Getting started
Download the release.

Enable the REST API for your semux client, and note the user/pass/port

*** Before running, please make sure your delegate account does not contain non-pool funds!  You should make sure to protect yourself against bugs.  Nothing in this code even calls unlock/vote functionality, so locked funds should be safe, but best bet is to move them off ***

Update the semuxpool.properties file with your delegateAddress, poolProfitsAddress, apiHost, apiUser, apiPass, and set debugMode to 'true'
debug mode allows you to run without actually making payments while you configure and sanity check.

run semux-pool.sh (linux/OSX) or semux-pool.bat (windows) files to start it.

You will see json objects created in ./payouts  These are logs of your pool's paid transactions.  If you are running in debug mode check that these values match what you expect.

After you are satisfied, set debugMode to 'false', delete the payouts directory and start it up.

TODO - more docs are coming.
