#!/bin/bash

#
 ####################################################################
 # Licensed Materials - Property of HCL                              #
 #                                                                  #
 # Copyright HCL Technologies Ltd. 2001, 2021. All Rights Reserved. #
 #                                                                  #
 # Note to US Government Users Restricted Rights:                   #
 #                                                                  #
 # Use, duplication or disclosure restricted by GSA ADP Schedule    #
 ####################################################################
 #
# navigate to folder which has all the test reports (contains report in .txt and .xml formats)
cd /opt/wtf/test.function/target/surefire-reports
# create a directory to store the converted html report
mkdir HTMLReports
cd HTMLReports

# determine the snapshot dir (variable time stamp like '2021.02.04_14.42.41')
# this will be needed for constructing links from the tests to error snapshots
for dir in /opt/wtf/test.function/target/snapshots/*/ ; do
    snapshotDir=$(basename "$dir")
done
echo $snapshotDir


# loop through the xml files and create corresponding HTML files 
for files in /opt/wtf/test.function/target/surefire-reports/*.xml; do
    echo $files
    #gets the .xml file
    xmlFile=$(basename "$files")
    echo $xmlFile
    #removes the .xml extension from the file
    HTMLfile="${xmlFile%.*}"
    echo $HTMLfile
    # Saxon-HE-9.5.1-3.jar is placed inside the new ami instance /html_conversion_jar/Saxon-HE-9.5.1-3.jar
    java -jar /html_conversion_jar/Saxon-HE-9.5.1-3.jar -o:$HTMLfile.html $files /convert-xml.xslt snapshotDir="$snapshotDir"
done

# get the CSS file used with all the HTML results
cp /wtf.css /opt/wtf/test.function/target/surefire-reports/HTMLReports
