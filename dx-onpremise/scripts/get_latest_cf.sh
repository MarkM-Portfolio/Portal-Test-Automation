# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2021. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

#!/bin/bash


#defaults
artifactoryUrl="https://artifactory.cwp.pnp-hcl.com/artifactory/list/quintana-generic-prod/portal/packaging/production/"

linearray=($(curl --silent ${artifactoryUrl} | grep 'CF' |tr -s '[:blank:]' ',' | cut -d ',' -f 2 | sed 's/.*">//' | sed 's/\/.*>//' | grep '^CF'))

newestCF=""

for i in "${linearray[@]}"
do
    newestCF=$i
done

echo -n $newestCF