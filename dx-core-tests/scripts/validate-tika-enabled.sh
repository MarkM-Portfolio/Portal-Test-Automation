#!/bin/sh

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

echo "wp_profile path is $1"

# Verify tika jars exists in PortalServer/lwo/prereq.odc/shared/app
cd $1
cd ../PortalServer/lwo/prereq.odc/shared/app
find convertors.jar | xargs -I {} bash -c 'if grep -Fq "com/ibm/wps/odc/convert/tika/TikaExport.class" {}; then echo TikaExport was found in {}; else echo "Error: TikaExport.class was not found"; exit 1; fi'

# Verify convetor.xml is using TikaExport after the update
cd $1
cd PortalServer/dcs/
find convertors.xml | xargs -I {} bash -c 'if grep -Fq "com.ibm.wps.odc.convert.tika.TikaExport" {} ; then echo Verified Tika is being used now in {}; else echo "Error: com.ibm.wps.odc.convert.tika.TikaExport was not found in convertors.xml"; exit 1 ; fi'


# Verify convetor.xml is using Stellent
# cd $1
# cd PortalServer/dcs/
# find convertors.xml | xargs -I {} bash -c 'if grep -Fq "com.ibm.wps.odc.convert.stellent.OutsideInExport" {} ; then echo Verified Tika is being used now in {}; else echo "Error: Tika not found"; exit 1 ; fi'