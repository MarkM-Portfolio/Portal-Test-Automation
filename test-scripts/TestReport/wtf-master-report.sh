#!/bin/bash

#
 ####################################################################
 # Licensed Materials - Property of HCL                             #
 #                                                                  #
 # Copyright HCL Technologies Ltd. 2001, 2021. All Rights Reserved. #
 #                                                                  #
 # Note to US Government Users Restricted Rights:                   #
 #                                                                  #
 # Use, duplication or disclosure restricted by GSA ADP Schedule    #
 ####################################################################
 #
cd /
# if there is an old combined.xml, then remove it
if [ -f "combined.xml" ]
then
    rm combined.xml
fi

echo '<results>' >> combined.xml
for files in /opt/wtf/test.function/target/surefire-reports/*.xml; do
    echo $files
    # get the data from each .xml file and create one combined xml
    python xml-combine.py $files >> combined.xml
done
echo '</results>' >> combined.xml

# produce one master report html file
java -jar /html_conversion_jar/Saxon-HE-9.5.1-3.jar -o:Master-Report.html combined.xml /master-convert-xml.xslt

# if there is an open defects file, then add links to the defects for each applicable test result
defects_file="open-defects.txt"
if [ -f $defects_file ]
then
    echo "reading lines from open defects file ---------"
    # each pair of lines should be in this format:
    #    line1 - full classname of the test
    #    line2 - JIRA ticket number
    #
    # example:
    # com.ibm.portal.test.function.wcm.authoring.workflow.workflowstage.WorkflowStageDependency
    # DXQ-15380
    #
    while IFS= read -r line1
    do
        line1=${line1%$'\n'}   # Remove trailing newline if needed
        echo "$line1"
        read -r line2
        line2=${line2%$'\n'}   # Remove trailing newline if needed
        echo "$line2"
        # insert a link in the same table td (inline) as the test classname
        sed -i "s/>$line1<\/a>/& <a class=\"open-defects\" target=\"_blank\" href=\"https:\/\/jira.cwp.pnp-hcl.com\/browse\/$line2\">$line2<\/a>/" Master-Report.html
    done < "$defects_file"
fi

# copy the html and css to correct dir to get picked up for dashboard report
cp Master-Report.html /opt/wtf/test.function/target/
cp wtf.css /opt/wtf/test.function/target/

# ensure that we have a snapshots dir (if no test errors, then it won't exist)
if [ ! -d "/opt/wtf/test.function/target/snapshots" ] 
then
    echo "Directory /opt/wtf/test.function/target/snapshots DOES NOT exist, so creating it" 
    cd /opt/wtf/test.function/target
    mkdir snapshots
    cd /
fi