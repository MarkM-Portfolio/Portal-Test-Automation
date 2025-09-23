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

current_run_file="pnp-regression-run.xml"

cd /home/jmeter_user/pnp-perfomance-regression-reports/
# if there is an old current run file, then remove it
if [ -f "$current_run_file" ]
then
    rm "$current_run_file"
fi

echo '<wtf_run buildName="'"$1"'"/>' >> "$current_run_file"

# produce xml that has combined results for this jenkins run (ends up being a single line in the output xml file)
# java -jar /opt/pnp-perfomance-regression-reports/Saxon-HE-9.5.1-3.jar -o:"$current_run_file" Test_IUT_Master.xml /opt/pnp-perfomance-regression-reports/wtf-run-to-xml.xslt pBuildName="$1"

# insert a line break needed for the read line while loop below
echo '' >> "$current_run_file"

# if there is a running WTF results file
iut_combined_runs_file="pandp-regression-test-combined-runs_njdc.xml"
if [ -f $iut_combined_runs_file ]
then
    # test to see if an entry for this build already exists
    if grep -Fq "$1" $iut_combined_runs_file
    then
        echo "An entry already exists in the wtf combined runs file, so no need to insert current run results"
    else
        # then insert our line of xml from this run as the first entry
        echo "reading xml line from wtf run file ---------"
        while IFS= read -r line1
        do
            line1=${line1%$'\n'}   # Remove trailing newline if needed
            echo "$line1"
            # insert this entire line at the top
            sed -i "/<results>/a $line1" "$iut_combined_runs_file"
        done < "$current_run_file"
    fi

    # produce an updated WTF dashboard results HTML file that includes this run's results
    java -jar /home/jmeter_user/pnp-perfomance-regression-reports/Saxon-HE-9.5.1-3.jar -o:"$2"-dashboard.html "$iut_combined_runs_file" /home/jmeter_user/pnp-perfomance-regression-reports/pageportlet-dashboard-convert-xml.xslt pWTF="$2"

    # ensure that we have a dashboard dir
    if [ ! -d "/home/jmeter_user/pnp-perfomance-regression-reports/dashboard" ] 
    then
        echo "Directory /home/jmeter_user/pnp-perfomance-regression-reports/dashboard DOES NOT exist, so creating it" 
        cd /home/jmeter_user/pnp-perfomance-regression-reports/
        mkdir dashboard
        cd /home/jmeter_user/pnp-perfomance-regression-reports
    fi

    # copy the html and xml to correct dir to get picked up for dashboard report
    cp "$2"-dashboard.html /home/jmeter_user/pnp-perfomance-regression-reports/dashboard/
    cp "$iut_combined_runs_file" /home/jmeter_user/pnp-perfomance-regression-reports/dashboard/"$2"-combined-runs.xml
    cp wtf.css ./dashboard
fi
