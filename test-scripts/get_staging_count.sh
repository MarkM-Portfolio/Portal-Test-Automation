# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2024. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

#!/usr/bin/env bash

json_file=$1
buildName=$2
buildUrl=$3


# Check if a JSON file is provided as a parameter
if [ -z $json_file ]; then
    echo "Please provide a JSON file as the first parameter."
    exit 1
fi


if [ $# -lt 3 ]; then
    echo "Please provide a JSON file, build name, and build URL as parameters."
    exit 1
fi


# Read the JSON file
json_data=$(cat "$json_file")


# Extract sampleCount and errorCount from Total object
sample_count=$(echo "$json_data" | jq -r '.Total.sampleCount')
# sample_count=$(expr $sample_count + 0)  # Convert sample_count from string to number
error_count=$(echo "$json_data" | jq -r '.Total.errorCount')
# error_count=$(expr $error_count + 0)  # Convert error_count from string to number

# Print the extracted values
echo "Sample Count: $sample_count"
echo "Error Count: $error_count"

# write to xml
# Check if Dam_Staging_Tests_Results.xml exists and remove it
if [ -e "/opt/dam-staging-acceptance-test-reports/Dam_Staging_Tests_Results.xml" ]; then
    rm "/opt/dam-staging-acceptance-test-reports/Dam_Staging_Tests_Results.xml"
    echo "Existing Dam_Staging_Tests_Results.xml removed."
fi


echo " <dam_staging_test buildName="\"${buildName}\"" buildUrl="\"${buildUrl}\"" tests="\"${sample_count}\"" failures="\"${error_count}\""/> ">> /opt/dam-staging-acceptance-test-reports/Dam_Staging_Tests_Results.xml

echo "Dam_Staging_Tests_Results.xml created at "${buildName}"/opt/dam-staging-acceptance-test-reports"