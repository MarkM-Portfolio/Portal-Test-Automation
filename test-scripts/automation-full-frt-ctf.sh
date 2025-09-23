
#!/bin/sh

####################################################################
# Licensed Materials - Property of HCL                              #
#                                                                  #
# Copyright HCL Technologies Ltd. 2001, 2021. All Rights Reserved. #
#                                                                  #
# Note to US Government Users Restricted Rights:                   #
#                                                                  #
# Use, duplication or disclosure restricted by GSA ADP Schedule    #
####################################################################


if [ XXX$1 != "XXX" ]; then
    USER=$1
    PASSWORD=$2
    echo "FTP User : "$USER
    echo "FTP Password : "$PASSWORD
fi
build_label=$(</opt/zips/cf17/currentBuildLabel.txt)
echo "Current Build Label is " $build_label

#wget -r  --user=$USER --password=$PASSWORD ftp://merry.team-q-dev.com/msa/rtpmsa/projects/b/build.portal/builds/DXCore_Tests/${build_label}/buildartifacts/test.* -P /

#yes | cp -rf merry.team-q-dev.com/msa/rtpmsa/projects/b/build.portal/builds/DX_Core/${build_label}/buildartifacts/test.*/*  /shared/care/

#Run set up scripts for CTF
echo "Running Set ups for Test Environment..."
cd /shared/care/rt/ref
. ./copysetup.sh

cd /shared/care
. ./setupenv.sh

#Run CTF Deploy 
cd /shared/care/rt
ant -Ddeploy=true care.deploy.ctf

#set up command for unit tests
ant care.prepare.setup

#Import nodetypes needed by JCR tests
/opt/IBM/WebSphere/wp_profile/ConfigEngine/ConfigEngine.sh import-nodetypes -Dwp.content.repository.input.dir="/test.nodetypes"

#Start the portal server
echo "Starting Portal Application..."
/opt/IBM/WebSphere/wp_profile/bin/startServer.sh WebSphere_Portal

#Copy db2 drivers to the ctf lib folder to ensure the db2 driver and classes are found.
cp /opt/IBM/WebSphere/AppServer/deploytool/itp/plugins/com.ibm.datatools.db2_2.2.200.v20150728_2354/driver/*.jar /shared/care/ctf/build/lib/

#Run the unit tests 
echo "Starting to run the following FRTs..."

ant frt -logfile frt.out.log
