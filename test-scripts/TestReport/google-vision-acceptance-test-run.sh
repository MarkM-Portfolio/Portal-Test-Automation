#!/bin/bash

#
 ####################################################################
 # Licensed Materials - Property of HCL                             #
 #                                                                  #
 # Copyright HCL Technologies Ltd. 2023. All Rights Reserved.       #
 #                                                                  #
 # Note to US Government Users Restricted Rights:                   #
 #                                                                  #
 # Use, duplication or disclosure restricted by GSA ADP Schedule    #
 ####################################################################
 #

 # Take the full results of this particular WTF job and add them to the
 # running combined results dashboard file

current_run_file="acceptance-test-run.xml"

cd /opt/google-vision-acceptance-test-reports
# if there is an old current run file, then remove it
if [ -f "$current_run_file" ]
then
    rm "$current_run_file"
fi

# produce xml that has combined results for this jenkins run (ends up being a single line in the output xml file)
java -jar /opt/google-vision-acceptance-test-reports/Saxon-HE-9.5.1-3.jar -o:"$current_run_file" Google_Vision_Acceptance_Test_Master.xml /opt/google-vision-acceptance-test-reports/google-vision-acceptance-test-run-to-xml.xslt pBuildName="$1"

# insert a line break needed for the read line while loop below
echo '' >> "$current_run_file"

# if there is a running WTF results file
google_vision_acceptance_test_combined_runs_file="google-vision-acceptance-test-runs.xml"
if [ -f $google_vision_acceptance_test_combined_runs_file ]
then
    # test to see if an entry for this build already exists
    if grep -Fq "$1" $google_vision_acceptance_test_combined_runs_file
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
            sed -i "/<results>/a $line1" "$google_vision_acceptance_test_combined_runs_file"
        done < "$current_run_file"
    fi

    # produce an updated WTF dashboard results HTML file that includes this run's results
    java -jar /opt/google-vision-acceptance-test-reports/Saxon-HE-9.5.1-3.jar -o:"$2"-dashboard.html "$google_vision_acceptance_test_combined_runs_file" /opt/google-vision-acceptance-test-reports/dashboard-convert-xml.xslt pWTF="$2"

    # ensure that we have a dashboard dir
    if [ ! -d "./dashboard" ] 
    then
        echo "Directory /opt/dashboard DOES NOT exist, so creating it" 
        mkdir dashboard
        cd /opt/google-vision-acceptance-test-reports
    fi

    # copy the html and xml to correct dir to get picked up for dashboard report
    cp "$2"-dashboard.html ./dashboard/
    cp "$google_vision_acceptance_test_combined_runs_file" ./dashboard/"$2"-runs.xml
    cp wtf.css ./dashboard/
fi
