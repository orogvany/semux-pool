# semux-pool
Semux Pool Management 

This program allows you to pay out voters a share of your pool forgings on a specified schedule.

Note:  There is no guarentees implied with this software.  It is purely use at your own risk.  The authors of this code cannot be held responsible for any bugs resulting in over payments or other issues.

# Getting started

Download the source code and compile it.

Enable the REST API for your semux client, and note the user/pass/port

Update the semuxpool.properties file with your delegateAddress, apiHost, apiUser, apiPass, and set debugMode to 'true'
debug mode allows you to run without actually making payments while you configure and sanity check.

run PoolRunner.main() to start it.  An automated build is forthcoming.

You will see json objects created in ./payouts  These are logs of your pool's paid transactions.  Do not delete them.

TODO - more docs are coming.
