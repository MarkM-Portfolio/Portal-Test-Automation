#! /bin/bash

buildName=$1
buildUrl=$2

echo "Script begin"

TOTAL_COUNT=0
FAILED_COUNT=0
# write to xml
# Check if Dam_Cleanup_Tests_Results.xml exists and remove it
if [ -e "/opt/dam-cleanup-test-reports/Dam_Cleanup_Tests_Results.xml" ]; then
    rm "/opt/dam-cleanup-test-reports/Dam_Cleanup_Tests_Results.xml"
    echo "Existing Dam_Cleanup_Tests_Results.xml removed."
fi


echo " <dam_cleanup_test buildName="\"${buildName}\"" buildUrl="\"${buildUrl}\"" tests="\"${TOTAL_COUNT}\"" failures="\"${FAILED_COUNT}\""/> ">> /opt/dam-cleanup-test-reports/Dam_Cleanup_Tests_Results.xml

echo "Script ends"