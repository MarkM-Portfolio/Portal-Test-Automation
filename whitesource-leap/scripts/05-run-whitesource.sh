#!/bin/sh -ex
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2019, 2022. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

cd ~/whitesource-leap
echo "In whitesource-leap folder"
echo "Showing contents of current folder"
ls -latr
echo "Completed showing contents of current folder"
echo "Not going to unzip the wss-unified-agent.jar"
# unzip -l wss-unified-agent.jar
echo "Did not run unzip of the wss-unified-agent.jar"
echo "Showing contents of config folder"
ls -latr ./config
echo "Completed showing contents of config folder"

echo "Show the java level"
java -version
echo "Java level shown above"

mkdir logs

# Leap Docker image scans

echo "Going to run the whitesource scan via the wss-unified-agent.jar against the LEAP image"
java -d64 -Xms16g -Xmx32g -jar wss-unified-agent.jar -c ./config/wss-leap-image.config &

wait
echo "LEAP Container image scans done"
echo "Done with Leap Whitesource 05-run-whitesource"
