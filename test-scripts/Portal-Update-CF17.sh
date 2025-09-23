#!/bin/sh

#
 ####################################################################
 # Licensed Materials - Property of HCL                              #
 #                                                                  #
 # Copyright HCL Technologies Ltd. 2001, 2019. All Rights Reserved. #
 #                                                                  #
 # Note to US Government Users Restricted Rights:                   #
 #                                                                  #
 # Use, duplication or disclosure restricted by GSA ADP Schedule    #
 ####################################################################
 #


#AWS SMB Mount
if [ XXX$1 != "XXX" ]; then
    echo "If mount needed, will mount as "$1
    MOUNTUSER=$1
    PASSWORD=$2
    PORTAL_ADMIN_PWD=$3
fi

if [ -d /msa ]; then
    echo "mount exists, no mounting required"
else
    echo "Mount not found, mounting now as" $MOUNTUSER
    #mkdir /msa
    #sudo mount -t cifs  //10.134.211.188/aws-hcl-unzip-playground/msa /msa -o username=$MOUNTUSER,password=$PASSWORD
fi

#copy the latest build output and unzip it
if [ ! -d "/opt/zips" ]; then
cd /opt/
mkdir zips
fi
cd /opt/zips/
mkdir cf17
#cp /msa/rtpmsa/projects/b/build.portal/builds/DX_Core/currentBuildLabel.txt /opt/zips/
cd /opt/zips/cf17
curl ftp://merry.team-q-dev.com/msa/rtpmsa/projects/b/build.portal/builds/DX_Core/currentBuildLabel.txt -u $MOUNTUSER:$PASSWORD -o currentBuildLabel.txt
build_label=$(</opt/zips/cf17/currentBuildLabel.txt)
echo "Current Build Label is " $build_label

#cp /msa/rtpmsa/projects/b/build.portal/builds/DX_Core/${build_label}/buildartifacts/iim.server/WP8500CF19_Server.zip /opt/zips/
cd /opt/zips/cf17
curl ftp://merry.team-q-dev.com/msa/rtpmsa/projects/b/build.portal/builds/DX_Core/${build_label}/buildartifacts/iim.server/WP8500CF19_Server.zip -u $MOUNTUSER:$PASSWORD -o WP8500CF19_Server.zip
yes | unzip WP8500CF19_Server.zip 
echo "Unzipped latest build..."

#Run Installation Manager
echo "Starting to Install Portal..."
cd /opt/IBM/InstallationManager/eclipse/tools/
./imcl install com.ibm.websphere.PORTAL.SERVER.v85 -repositories /opt/zips/cf17/repository.config -installationDirectory /opt/IBM/WebSphere/PortalServer/ -acceptLicense


#Apply CF
echo "Starting to Apply CF..."
cd /opt/IBM/WebSphere/wp_profile/PortalServer/bin
./applyCF.sh -DPortalAdminPwd=$PORTAL_ADMIN_PWD -DWasPassword=$PORTAL_ADMIN_PWD


#Copy test zips from MSA into the instance
#yes | cp -rf /msa/rtpmsa/projects/b/build.portal/builds/DX_Core/${build_label}/buildartifacts/test.*/*  /shared/care/
wget -r  --user=$MOUNTUSER --password=$PASSWORD ftp://merry.team-q-dev.com/msa/rtpmsa/projects/b/build.portal/builds/DX_Core/${build_label}/buildartifacts/test.* -P /
yes | cp -rf merry.team-q-dev.com/msa/rtpmsa/projects/b/build.portal/builds/DX_Core/${build_label}/buildartifacts/test.*/*  /shared/care/

if grep -q fixlevel=CF19 /opt/IBM/WebSphere/PortalServer/wps.properties && grep -q fixlevel=CF19 /opt/IBM/WebSphere/wp_profile/PortalServer/wps.properties
then 
echo "CF19 Installation Successful !"
else
echo "CF19 Installation Failed !"
exit 1
fi 

#unmount msa
#umount /msa
#echo "Unmounted MSA!"

#capturing running ec2 instance id
curl http://169.254.169.254/latest/meta-data/instance-id >> /instance-id.txt
