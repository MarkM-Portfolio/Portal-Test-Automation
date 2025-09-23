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
cd /opt/git/report/
# if there is an old Test_IUT_Master.xml, then remove it
if [ -f "Test_IUT_Master.xml" ]
then
    rm Test_IUT_Master.xml
fi

echo '<results>' >> Test_IUT_Master.xml
for files in /opt/git/iut_xml_reports/*.xml; do
    echo $files
    # get the data from each .xml file and create one Test_IUT_Master xml
    python xml-combine.py $files >> Test_IUT_Master.xml
done
echo '</results>' >> Test_IUT_Master.xml

# produce one master report html file
java -jar /opt/git/Saxon-HE-9.5.1-3.jar -o:Master-Report.html Test_IUT_Master.xml /opt/git/report/iut-convert-xml.xslt