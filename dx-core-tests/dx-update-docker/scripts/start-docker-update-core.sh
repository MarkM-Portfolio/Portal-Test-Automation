#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2021. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
 
do_wait_for_container(){
#wait for the container using curl, netstat is not accurate 
# Note: The login URL was altered below due a defect in the redeployment of the Woodburn theme
n=1
while [[ "$(curl -L -s -o /dev/null -w ''%{http_code}'' http://INSTANCE_IP:10039/this/works/Home)" != "200" ]]
do
        echo "Container not running yet, waiting another 300s"
        sleep 300
        n=$(( n+1 ))
        if [ $n -eq 15 ]; then
          echo "Container failed to install and run within the alotted time"
          exit 1
        fi
done
echo "Container is running"
}


# Start Docker CE 
 sudo systemctl enable docker
 sudo systemctl start docker

ARTIFACTORY_HOST="https://artifactory.cwp.pnp-hcl.com/artifactory/list/quintana-docker"
echo "ARTIFACTORY_HOST: $ARTIFACTORY_HOST"

# retrieve the dxen index page, listing all image tags
curl $ARTIFACTORY_HOST/dxen/ -o dxen.list

# remove HTML from the output
sed -e 's/<[^>]*>//g' dxen.list > dxen-clean.list

# remove all non v95 content
grep "develop" dxen-clean.list > dxen-v95.list

# create a csv out of it
cat dxen-v95.list | tr -s '[:blank:]' ',' > dxen-v95.csv

# transform all dates to a sortable format
while read line; do
        var1=$(echo "$line" | cut -d ',' -f 1)
        var2=$(echo "$line" | cut -d ',' -f 2)
        var3=$(echo "$line" | cut -d ',' -f 3)
        var2=$(date --date="$var2 $var3" +"%s")
        echo "$var1,$var2" >> dxen-transformed.csv
done < dxen-v95.csv

# get the latest image tag (first line of csv)
latest=$(sort -t, -k2,2 -nr dxen-transformed.csv | head -n 1 | cut -d ',' -f 1)
latest=${latest%/}
echo "Going to pull the following image: $latest"

# tidy up
rm dxen.list dxen-clean.list dxen-v95.list dxen-v95.csv dxen-transformed.csv

docker pull quintana-docker.artifactory.cwp.pnp-hcl.com/dxen:$latest

echo "Starting DX Portal"
echo "docker run -d -u 0 -e WAS_ADMIN=wpsadmin -e WAS_PASSWORD=password -e DX_ADMIN=wpsadmin -e DX_PASSWORD=password -p 10039:10039 -p 10041:10041 -p 10200:10200 -p 10203:10203 -p  10202:10202 -p 10025:10025 -p 2809:2809 -v /opt/wp_profile:/opt/HCL/wp_profile --name dx_core quintana-docker.artifactory.cwp.pnp-hcl.com/dxen:$latest
"
docker run -d -u 0 -e WAS_ADMIN=wpsadmin -e WAS_PASSWORD=password -e DX_ADMIN=wpsadmin -e DX_PASSWORD=password -p 10039:10039 -p 10041:10041 -p 10200:10200 -p 10203:10203 -p  10202:10202 -p 10025:10025 -p 2809:2809 -v /opt/wp_profile:/opt/HCL/wp_profile --name dx_core quintana-docker.artifactory.cwp.pnp-hcl.com/dxen:$latest

do_wait_for_container

sleep 30