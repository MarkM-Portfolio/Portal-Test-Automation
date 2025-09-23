#!/bin/bash

#
 ####################################################################
 # Licensed Materials - Property of HCL                             #
 #                                                                  #
 # Copyright HCL Technologies Ltd. 2022. All Rights Reserved.       #
 #                                                                  #
 # Note to US Government Users Restricted Rights:                   #
 #                                                                  #
 # Use, duplication or disclosure restricted by GSA ADP Schedule    #
 ####################################################################
 #

 # Take the full results of current run and add them to the
 # running combined results dashboard file

current_run_file="Dam_Performance_Current_Tests_njdc.xml"

cd /home/dam_jmeter_user/dam-performance-test-reports-njdc

if [ ! -f "Dam_Performance_Tests_Results_Njdc.xml" ]
then
    exit 1
fi
# if there is an old current run file, then remove it
if [ -f "$current_run_file" ]
then
    rm "$current_run_file"
fi

cat Dam_Performance_Tests_Results_Njdc.xml

# produce xml that has combined results for this jenkins run (ends up being a single line in the output xml file)
java -jar /home/dam_jmeter_user/dam-performance-test-reports-njdc/Saxon-HE-9.5.1-3.jar -o:"$current_run_file" Dam_Performance_Tests_Results_Njdc.xml /home/dam_jmeter_user/dam-performance-test-reports-njdc/dam_performance_test_run_to_xml_njdc.xslt pBuildName="$1"


# insert a line break needed for the read line while loop below
echo '' >> "$current_run_file"

# if there is a running WTF results file
dam_performance_test_combined_runs_file="dam-performance-tests-combined-runs-njdc.xml"
if [ -f $dam_performance_test_combined_runs_file ]
then
    # test to see if an entry for this build already exists
    if grep -Fq "$1" $dam_performance_test_combined_runs_file
    then
        echo "An entry already exists in the acceptance-test combined runs file, so no need to insert current run results"
    else
        # then insert our line of xml from this run as the first entry
        echo "reading xml line from acceptance-test run file ---------"
        while IFS= read -r line1
        do
            line1=${line1%$'\n'}   # Remove trailing newline if needed
            echo "$line1"
            # insert this entire line at the top
            sed -i "/<results>/a $line1" "$dam_performance_test_combined_runs_file"
        done < "$current_run_file"
    fi

    # produce an updated dam performance dashboard results HTML file that includes this run's results
    java -jar /home/dam_jmeter_user/dam-performance-test-reports-njdc/Saxon-HE-9.5.1-3.jar -o:"$2"-dashboard.html "$dam_performance_test_combined_runs_file" /home/dam_jmeter_user/dam-performance-test-reports-njdc/dam_performance_test_convert_dashboard_njdc.xslt pWTF="$2" pUploadThresholdValue="$3" pOperationThresholdValue="$4" pFetchBinaryImageThresholdValue="$5" pFetchBinaryVideoThresholdValue="$6" pFetchBinaryDocumentThresholdValue="$7"

    # ensure that we have a dashboard dir
    if [ ! -d "./dashboard" ] 
    then
        echo "Directory dashboard DOES NOT exist, so creating it" 
        mkdir dashboard
        cd /home/dam_jmeter_user/dam-performance-test-reports-njdc
    fi
    # copy the html and xml to correct dir to get picked up for dashboard report
    cp "$2"-dashboard.html ./dashboard/
    cp "$dam_performance_test_combined_runs_file" ./dashboard/"$2"-combined-runs.xml
    cp wtf_njdc.css ./dashboard/wtf_njdc.css
fi
