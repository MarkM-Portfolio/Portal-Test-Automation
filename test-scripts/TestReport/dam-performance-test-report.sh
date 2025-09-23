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
cd /opt/dam-performance-test-reports/

# produce one master report html file
java -jar /opt/dam-performance-test-reports/Saxon-HE-9.5.1-3.jar -o:dam-jtl-Report.html testreport.xml /opt/dam-performance-test-reports/dam-test-convert-xml.xslt $1