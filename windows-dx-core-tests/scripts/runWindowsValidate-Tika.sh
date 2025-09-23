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

set -e

# Verify tika jars exists in PortalServer/lwo/prereq.odc/shared/app/convertors.jar

find convertors.jar | xargs -I {} bash -c 'if grep -Fq "com/ibm/wps/odc/convert/tika/TikaExport.class" {}; then echo TikaExport was found in {}; else echo "Error: TikaExport.class was not found"; exit 1; fi'

# Verify convertors.xml is using TikaExport after the update

find convertors.xml | xargs -I {} bash -c 'if grep -Fq "com.ibm.wps.odc.convert.tika.TikaExport" {} ; then echo Verified Tika is being used now in {}; else echo "Error: com.ibm.wps.odc.convert.tika.TikaExport was not found in convertors.xml"; exit 1 ; fi'

