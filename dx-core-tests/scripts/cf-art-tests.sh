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

if [ XXX$1 != "XXX" ]; then
    FTP_USER=$1
    FTP_PASSWORD=$2
    CF_VERSION=$3
    BUILD_LABEL=$4
    echo "FTP User : "$FTP_USER
    echo "FTP Password : "$FTP_PASSWORD
    echo "CF Version : "$CF_VERSION
    echo "BUILD LABEL : "$BUILD_LABEL
fi

#Run set up scripts for CTF
echo "Running Set ups for Test Environment..."
cd /shared/care/rt/ref
. ./copysetup.sh

cd /shared/care
. ./setupenv.sh

#Copy to all test case reports into one folder for that creating folder
echo "Creating Folder.."
#cd /root
#mkdir /root/reports

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

#Run the unit tests 
echo "Starting to run the Full Rgression Test ART..."
ant art -logfile regression.out.log
# cp -avr /shared/care/ctf/build/reports /root/reports/shared/care/ctf/build

#Converting reports to Zip file and copying to Dashboard...
echo "Converting reports to Zip file and copying to Dashboard..."
cd /opt/zips
tar czf ${BUILD_LABEL}_linux_oracle_${CF_VERSION}_art_tests.zip /shared/care/ctf/build/reports
aws s3 cp ${BUILD_LABEL}_linux_oracle_${CF_VERSION}_art_tests.zip s3://dx-testarea
