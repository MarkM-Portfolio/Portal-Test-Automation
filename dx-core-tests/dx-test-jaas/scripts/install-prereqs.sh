#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2024. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

# Prepare for delta branch source download
DELTA_BRANCH_FELIST=$1

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

TOKEN="a6e1b4891cf73029a9f3b692635e06d3a4d08261"
HOSTNAME="git.cwp.pnp-hcl.com"
ORGANISATION="websphere-portal"

ssh-keyscan git.cwp.pnp-hcl.com >> ~/.ssh/known_hosts

curl -s https://$TOKEN:@$HOSTNAME/api/v3/orgs/$ORGANISATION/repos\?per_page\=200 | grep -wF "name" | grep -v "{/name}" | awk -F '"' '{print $4}' > names.txt

curl -s https://$TOKEN:@$HOSTNAME/api/v3/orgs/$ORGANISATION/repos\?per_page\=200 | grep svn_url | awk -F '"' '{print $4"/archive/develop.zip"}' > repo.txt 

# Replace with delta branch if necessary
if [ "$DELTA_BRANCH_FELIST" != "" ]; then
   # Split into branch and FE list
   DELTA_BRANCH=${DELTA_BRANCH_FELIST%%:*}
   DELTA_FELIST=${DELTA_BRANCH_FELIST#*:}
   for single_fe in $(echo $DELTA_FELIST); do
      sed -i -e "s,${single_fe}/archive/develop.zip,${single_fe}/archive/${DELTA_BRANCH}.zip,g" repo.txt
   done
fi

while IFS= read -r line1 && IFS= read -r line2 <&3; do
  if [[ " $DELTA_FELIST " == *" $line2 "* ]]; then
     fe_branch=$DELTA_BRANCH
  else
     fe_branch="develop"
  fi
  curl --header 'Authorization: token a6e1b4891cf73029a9f3b692635e06d3a4d08261' --header 'Accept: application/vnd.github.v3.raw' --remote-name --location $line1 && unzip -qq ${fe_branch}.zip && sudo mv $line2-${fe_branch}  $line2  &&   rm -rf ${fe_branch}.zip
  echo "File 1: $line1"
  echo "File 2: $line2"
done < repo.txt 3< names.txt

# Get Java
sudo mkdir -p /opt/jdk/ && cd /opt/jdk/
sudo curl -JLO https://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/wasdev/downloads/wlp/21.0.0.2/wlp-webProfile8-java8-linux-x86_64-21.0.0.2.zip
sudo unzip -qq wlp-webProfile8-java8-linux-x86_64-21.0.0.2.zip 
sudo rm -f wlp-webProfile8-java8-linux-x86_64-21.0.0.2.zip
sudo chown -R centos:centos wlp/

# Update bash_profile
echo "JAVA_HOME=/opt/jdk/wlp/java/java
export PATH=/opt/jdk/wlp/java/java/bin:/opt/git/Portal-Developer-Tools/local-build-tools/bin:$PATH
export PORTAL_SOURCE_BASE_DIR=/opt/git/git-dx-clone
export PORTAL_LOCAL_DIR=/opt/git/git-dx-clone/prereq_fes
export PORTAL_PREREQS_DIR=/Volumes/Quintana/prereqs" >> ~/.bash_profile

source ~/.bash_profile

# Get the missing lwo
cd /opt/git/git-dx-clone/prereq_fes
rm -rf lwo
curl -JLO ftp://wpbuild:p0rtal4u@merry.team-q-dev.com/msa/rtpmsa/projects/b/build.portal/prereqs/thomas/lwo.zip
jar -xvf lwo.zip
rm -rf lwo.zip

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
