#! /bin/bash

buildName=$1
buildUrl=$2

echo "Script begin"

TOTAL_COUNT=0
FAILED_COUNT=0
# write to xml
# Check if Google_Vision_Tests_Results.xml exists and remove it
if [ -e "/opt/google-vision-acceptance-test-reports/Google_Vision_Tests_Results.xml" ]; then
    rm "/opt/google-vision-acceptance-test-reports/Google_Vision_Tests_Results.xml"
    echo "Existing Google_Vision_Tests_Results.xml removed."
fi


echo " <gv_acceptance_test_run buildName="\"${buildName}\"" buildUrl="\"${buildUrl}\"" tests="\"${TOTAL_COUNT}\"" failures="\"${FAILED_COUNT}\"" errors="\"${TOTAL_COUNT}\"" skipped="\"${TOTAL_COUNT}\""/> ">> /opt/google-vision-acceptance-test-reports/Google_Vision_Tests_Results.xml

echo "Script ends"
