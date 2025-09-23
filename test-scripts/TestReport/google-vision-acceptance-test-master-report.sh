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
cd /opt/google-vision-acceptance-test-reports/
# if there is an old Google_Vision_Acceptance_Test_Master.xml, then remove it
if [ -f "Google_Vision_Acceptance_Test_Master.xml" ]
then
    rm Google_Vision_Acceptance_Test_Master.xml
fi

echo '<results>' >> Google_Vision_Acceptance_Test_Master.xml
for files in /opt/google-vision-acceptance-test-reports/xml/*.xml; do
    echo $files
    # get the data from each .xml file and create one Acceptance_Test_Master xml
    python xml-combine.py $files >> Google_Vision_Acceptance_Test_Master.xml
done
echo '</results>' >> Google_Vision_Acceptance_Test_Master.xml

# produce one master report html file
java -jar /opt/google-vision-acceptance-test-reports/Saxon-HE-9.5.1-3.jar -o:Master-Report.html Google_Vision_Acceptance_Test_Master.xml /opt/google-vision-acceptance-test-reports/google-vision-acceptance-test-convert-xml.xslt $1