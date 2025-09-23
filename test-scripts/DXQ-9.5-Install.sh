#!/bin/sh

#
 ####################################################################
 # Licensed Materials - Property of HCL                              #
 #                                                                  #
 # Copyright HCL Technologies Ltd. 2001, 2021. All Rights Reserved. #
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
    ADMINPASSWORD=$3
fi


#copy the latest build output and unzip it
cd /opt/zips/
mkdir Portal95InstallZip

cd /opt/zips/Portal95InstallZip
curl ftp://merry.team-q-dev.com/msa/rtpmsa/projects/b/build.portal/builds/DX_Core/currentBuildLabel.txt -u $MOUNTUSER:$PASSWORD -o currentBuildLabel.txt
build_label=$(</opt/zips/Portal95InstallZip/currentBuildLabel.txt)
echo "Current Build Label is " $build_label

cd /opt/zips/Portal95InstallZip
curl ftp://merry.team-q-dev.com/msa/rtpmsa/projects/b/build.portal/builds/DX_Core/${build_label}/buildartifacts/iim.server/WP8500CF19_Server.zip -u $MOUNTUSER:$PASSWORD -o WP8500CF19_Server.zip
unzip WP8500CF19_Server.zip 
echo "Unzipped latest build..."

curl ftp://merry.team-q-dev.com/msa/rtpmsa/projects/b/build.portal/builds/DX_Core/${build_label}/buildartifacts/iim.portal95/WP95_Server_REPOS.zip -u $MOUNTUSER:$PASSWORD -o WP95_Server_REPOS.zip
unzip WP95_Server_REPOS.zip 
echo "Unzipped WP95_Server_REPOS ..."

curl ftp://merry.team-q-dev.com/msa/rtpmsa/projects/b/build.portal/builds/DX_Core/${build_label}/buildartifacts/iim.portal95/WP95_Enable_REPOS.zip -u $MOUNTUSER:$PASSWORD -o WP95_Enable_REPOS.zip
unzip WP95_Enable_REPOS.zip 
echo "Unzipped WP95_Enable_REPOS ..."

#getting the latest build number and appending to Response file

cd /opt/zips/Portal95InstallZip/WP95_Enable/Offerings
file=''
file=$(echo `ls | grep "com.ibm.websphere.PORTAL.ENABLE.v95_.*"`)
echo $file
WP95_Enable_buildNumber=$(awk -F PORTAL.ENABLE '{print $2}' <<<"$file")
echo $WP95_Enable_buildNumber
WP95_Enable_buildNumberSecondIndex=$(awk -F _ '{print $2}' <<<"$WP95_Enable_buildNumber")
WP95_Enable_buildNumberThirdIndex=$(awk -F _ '{print $3}' <<<"$WP95_Enable_buildNumber")
WP95_Enable_buildNumberFourthIndex=$(echo "$WP95_Enable_buildNumberSecondIndex"_"$WP95_Enable_buildNumberThirdIndex")
WP95_Enable_currentBuildLabel=$(awk -F .jar '{print $1}' <<<"$WP95_Enable_buildNumberFourthIndex")
echo WP95_Enable_currentBuildLabel=$WP95_Enable_currentBuildLabel

cd /opt/zips/Portal95InstallZip/WP95_Server/Offerings
file=''
file=$(echo `ls | grep "com.ibm.websphere.PORTAL.SERVER.v95_.*"`)
echo $file
ServerBuildNumber=$(awk -F PORTAL.SERVER '{print $2}' <<<"$file")
echo $ServerBuildNumber
ServerBuildNumberSecondIndex=$(awk -F _ '{print $2}' <<<"$ServerBuildNumber")
ServerBuildNumberThirdIndex=$(awk -F _ '{print $3}' <<<"$ServerBuildNumber")
ServerBuildNumberFourthIndex=$(echo "$ServerBuildNumberSecondIndex"_"$ServerBuildNumberThirdIndex")
Server_CurrentBuildLabel=$(awk -F .jar '{print $1}' <<<"$ServerBuildNumberFourthIndex")
echo Server_CurrentBuildLabel=$Server_CurrentBuildLabel


cd /opt/zips

#for extracting latest build number for  PORTAL.SERVER9.5

Server_PreviousBuildLabel=$(xmllint --xpath 'string(/agent-input/install/offering[1]/@version)' responseFile.xml)
echo Server_PreviousBuildLabel=$Server_PreviousBuildLabel
sed "s/$Server_PreviousBuildLabel/$Server_CurrentBuildLabel/g" responseFile.xml >responseFileCopy.xml

#for extracting latest build number for  PORTAL.ENABLE9.5

WP95_Enable_PreviousBuildLabel=$(xmllint --xpath 'string(/agent-input/install/offering[2]/@version)' responseFile.xml)
echo WP95_Enable_PreviousBuildLabel=$WP95_Enable_PreviousBuildLabel
sed "s/$WP95_Enable_PreviousBuildLabel/$WP95_Enable_currentBuildLabel/g" responseFileCopy.xml > newResponseFile.xml
cat newResponseFile.xml

#removing the respostories

cd /opt/IBM/InstallationManager/eclipse/tools/

./imcl -acceptLicense input /opt/zips/resetRepository.xml

#Installing the Porta9.5
./imcl -acceptLicense input /opt/zips/newResponseFile.xml -log /opt/zips/Portal95InstallZip/v95install.log -showProgress

#Run Config Engine to enable Woodburn and Practioner's Studio
/opt/IBM/WebSphere/wp_profile/ConfigEngine/ConfigEngine.sh enable-v95-UI-features -DWasPassword=$ADMINPASSWORD -DPortalAdminPwd=$ADMINPASSWORD

#Removing data
cd /opt/zips/
rm -rf {cf19,Portal95InstallZip}

#Check if 9.5 installation is successful
if grep -q Portal95=true /opt/IBM/WebSphere/PortalServer/wps.properties && grep -q Portal95=true /opt/IBM/WebSphere/wp_profile/PortalServer/wps.properties
then 
echo "DX 9.5 Installation Successful !"
else
echo "DX 9.5 Installation Failed !"
exit 1
fi 

