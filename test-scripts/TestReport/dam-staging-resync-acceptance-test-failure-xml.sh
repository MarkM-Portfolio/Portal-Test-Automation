#! /bin/bash

buildName=$1
buildUrl=$2

echo "Script begin"

TOTAL_COUNT=0
FAILED_COUNT=0
# write to xml
# Check if Dam_Staging_Resync_Tests_Results.xml exists and remove it
if [ -e "/opt/dam-staging-resync-acceptance-test-reports/Dam_Staging_Resync_Tests_Results.xml" ]; then
    rm "/opt/dam-staging-resync-acceptance-test-reports/Dam_Staging_Resync_Tests_Results.xml"
    echo "Existing Dam_Staging_Resync_Tests_Results.xml removed."
fi


echo " <dam_staging_resync_test_run buildName="\"${buildName}\"" buildUrl="\"${buildUrl}\"" tests="\"${TOTAL_COUNT}\"" failures="\"${FAILED_COUNT}\"" errors="\"${TOTAL_COUNT}\"" skipped="\"${TOTAL_COUNT}\""/> ">> /opt/dam-staging-resync-acceptance-test-reports/Dam_Staging_Resync_Tests_Results.xml

echo "Script ends"