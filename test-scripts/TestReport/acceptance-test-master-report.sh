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
cd /opt/acceptance-test-reports/
# if there is an old Acceptance_Test_Master.xml, then remove it
if [ -f "Acceptance_Test_Master.xml" ]
then
    rm Acceptance_Test_Master.xml
fi

echo '<results>' >> Acceptance_Test_Master.xml
for files in /opt/acceptance-test-reports/xml/*.xml; do
    echo $files
    # get the data from each .xml file and create one Acceptance_Test_Master xml
    python xml-combine.py $files >> Acceptance_Test_Master.xml
done
echo '</results>' >> Acceptance_Test_Master.xml

# produce one master report html file
java -jar /opt/acceptance-test-reports/Saxon-HE-9.5.1-3.jar -o:Master-Report.html Acceptance_Test_Master.xml /opt/acceptance-test-reports/acceptance-test-convert-xml.xslt $1