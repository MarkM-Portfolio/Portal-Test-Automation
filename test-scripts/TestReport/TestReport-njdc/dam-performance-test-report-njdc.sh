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
cd  /home/dam_jmeter_user/dam-performance-test-reports-njdc/

# produce one master report html file
java -jar /home/dam_jmeter_user/dam-performance-test-reports-njdc/Saxon-HE-9.5.1-3.jar -o:dam-jtl-Report-njdc.html testreport.xml /home/dam_jmeter_user/dam-performance-test-reports-njdc/dam-test-convert-xml-njdc.xslt $1