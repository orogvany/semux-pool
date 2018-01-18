# semux-pool
Semux Pool Management 

This program allows you to pay out voters a share of your pool forgings on a specified schedule.

Note:  There is no guarantees implied with this software.  It is purely use at your own risk.  The authors of this code cannot be held responsible for any bugs resulting in over payments or other issues.

# Getting started

Download the source code and compile it with maven (mvn clean install).
Note: You will also need to download the semux-java-client https://github.com/orogvany/semux-java-client

Enable the REST API for your semux client, and note the user/pass/port

Update the semuxpool.properties file with your delegateAddress, poolProfitsAddress, apiHost, apiUser, apiPass, and set debugMode to 'true'
debug mode allows you to run without actually making payments while you configure and sanity check.

run the provided .sh or bat files to start it.  An automated build is forthcoming.

You will see json objects created in ./payouts  These are logs of your pool's paid transactions.  If you are running in debug mode check that these values match what you expect.

After you are satisfied, set debugMode to false, delete the payouts directory and start it up.

TODO - more docs are coming.
