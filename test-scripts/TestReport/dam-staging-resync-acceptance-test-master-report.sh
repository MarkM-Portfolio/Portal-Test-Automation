#!/bin/bash

#
 ####################################################################
 # Licensed Materials - Property of HCL                             #
 #                                                                  #
 # Copyright HCL Technologies Ltd. 2023. All Rights Reserved. #
 #                                                                  #
 # Note to US Government Users Restricted Rights:                   #
 #                                                                  #
 # Use, duplication or disclosure restricted by GSA ADP Schedule    #
 ####################################################################
 #
cd /opt/dam-staging-resync-acceptance-test-reports/
# if there is an old Dam_Staging_Resync_Acceptance_Test_Master.xml, then remove it
if [ -f "Dam_Staging_Resync_Acceptance_Test_Master.xml" ]
then
    rm Dam_Staging_Resync_Acceptance_Test_Master.xml
fi

echo '<results>' >> Dam_Staging_Resync_Acceptance_Test_Master.xml
for files in /opt/dam-staging-resync-acceptance-test-reports/xml/*.xml; do
    echo $files
    # get the data from each .xml file and create one Acceptance_Test_Master xml
    python xml-combine.py $files >> Dam_Staging_Resync_Acceptance_Test_Master.xml
done
echo '</results>' >> Dam_Staging_Resync_Acceptance_Test_Master.xml

# produce one master report html file
java -jar /opt/dam-staging-resync-acceptance-test-reports/Saxon-HE-9.5.1-3.jar -o:Master-Report.html Dam_Staging_Resync_Acceptance_Test_Master.xml /opt/dam-staging-resync-acceptance-test-reports/dam-staging-resync-acceptance-test-convert-xml.xslt $1