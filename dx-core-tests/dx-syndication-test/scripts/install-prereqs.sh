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
n=1
while [[ "$(curl -L -s -o /dev/null -w ''%{http_code}'' http://INSTANCE_IP:10039/wps/portal)" != "200" ]]
do
        echo "Container not running yet, waiting another 30s"
        sleep 30
        n=$(( n+1 ))
        if [ $n -eq 20 ]; then
          echo "Container failed to run within alotted time"
          exit 1
        fi
done
echo "Container is running"
}

# Install jq
sudo yum install epel-release -y
sudo yum install jq -y
jq -Version

# Install Docker CE
sudo yum install -y yum-utils device-mapper-persistent-data lvm2
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install -y docker-ce docker-ce-cli containerd.io

# Set experimental features on
sudo mkdir -p /etc/docker
echo '{"experimental": true}' > sudo /etc/docker/daemon.json

# Start Docker CE
sudo systemctl enable docker
sudo systemctl start docker

# Add user to docker group
sudo usermod -aG docker centos

sudo chmod 666 /var/run/docker.sock

sudo mkdir -p /opt/git/git-dx-clone
cd /opt/git/git-dx-clone

sudo chmod 777 /opt/git/git-dx-clone

#sudo yum install git -y
sudo yum install unzip -y

# Get Java
sudo mkdir -p /opt/jdk/ && cd /opt/jdk/
sudo curl -JLO https://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/wasdev/downloads/wlp/21.0.0.2/wlp-webProfile8-java8-linux-x86_64-21.0.0.2.zip
sudo unzip -qq wlp-webProfile8-java8-linux-x86_64-21.0.0.2.zip 
sudo rm -f wlp-webProfile8-java8-linux-x86_64-21.0.0.2.zip
sudo chown -R centos:centos wlp/

# Update bash_profile
echo "JAVA_HOME=/opt/jdk/wlp/java/java
export PATH=/opt/jdk/wlp/java/java/bin:$PATH" >> ~/.bash_profile

source ~/.bash_profile

cd /opt/
sudo chown -R centos:centos git/

cd /opt/git
source ~/.bash_profile 

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
docker run -d -u 0 -p 10039:10039 -p 10041:10041 -p 10200:10200 -p 10203:10203 -p  10202:10202 -p 10025:10025 -p 2809:2809 --name dx_core quintana-docker.artifactory.cwp.pnp-hcl.com/dxen:$latest

do_wait_for_container

docker exec -i dx_core sh -c "/opt/HCL/wp_profile/ConfigEngine/ConfigEngine.sh enable-content-sites -DWasPassword=wpsadmin -DPortalAdminPwd=wpsadmin -Dcontentsites.static.ui.url=http://INSTANCE_IP:5500/dx/ui/site-manager/static"

sleep 30