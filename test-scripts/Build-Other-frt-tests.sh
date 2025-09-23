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
    USER=$1
    PASSWORD=$2
    DXBuildNumber=$3
    DXBuildName=$4
    echo "FTP User : "$USER
    echo "FTP Password : "$PASSWORD
    echo "DXBuildNumber : "$DXBuildNumber
    echo "DXBuildNumber : "$DXBuildName
fi
build_label=$(</opt/zips/cf17/currentBuildLabel.txt)
echo "Current Build Label is " $build_label

#wget -r  --user=$USER --password=$PASSWORD ftp://merry.team-q-dev.com/msa/rtpmsa/projects/b/build.portal/builds/DX_Core/${build_label}/buildartifacts/test.* -P /

#yes | cp -rf merry.team-q-dev.com/msa/rtpmsa/projects/b/build.portal/builds/DX_Core/${build_label}/buildartifacts/test.*/*  /shared/care/

#Run set up scripts for CTF
echo "Running Set ups for Test Environment..."
cd /shared/care/rt/ref
. ./copysetup.sh

cd /shared/care
. ./setupenv.sh

#Copy to all test case reports into one folder for that cretaing folder
echo "Creating Folder.."
cd /root
mkdir reports

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
echo "Starting to run the following tests for Other FRT CTF test components..."
#unit test begin
#base
echo "in base..."
ant ctf.run -Dctf.input=/test.cp.tagging/regression.tsf
cp -avr /shared/care/ctf/build/reports/base/test.cp.tagging /root/reports
ant ctf.run -Dctf.input=/test.spa.impl/regression.tsf
cp -avr /shared/care/ctf/build/reports/base/test.spa.impl /root/reports
ant ctf.run -Dctf.input=/test.resolver.friendly/regression.tsf
cp -avr /shared/care/ctf/build/reports/base/test.resolver.friendly /root/reports
ant ctf.run -Dctf.input=/test.resolver.impl/regression.tsf
cp -avr /shared/care/ctf/build/reports/base/test.resolver.impl /root/reports
ant ctf.run -Dctf.input=/test.dxconnect/regression.tsf
cp -avr /shared/care/ctf/build/reports/base/test.dxconnect /root/reports

#pzn.ext
echo "in pzn.ext..."
ant ctf.run -Dctf.input=/test.pzn.wcm.plr/regression.tsf
cp -avr /shared/care/ctf/build/reports/pzn.ext/test.pzn.wcm.plr /root/reports
ant ctf.run -Dctf.input=/test.pzn.wcm.ecm/regression.tsf
cp -avr /shared/care/ctf/build/reports/pzn.ext/test.pzn.wcm.ecm /root/reports


#Converting reports to Zip file and copying to Dashboard...
echo "Converting reports to Zip file and copying to Dashboard..."
cd /opt/zips
if [ $DXBuildName == "DX_9_CF17_TEST_CTF_UNIT_" ]; then
    echo "build Num=$DXBuildNumber and buildname=$DXBuildName"
    tar czf ${DXBuildNumber}_linux_db2_cf17_other_frt_ctf.zip /root/reports
    aws s3 cp ${DXBuildNumber}_linux_db2_cf17_other_frt_ctf.zip s3://dx-testarea
else
    tar czf ${DXBuildNumber}_linux_oracle_95_other_frt_ctf.zip /root/reports
    aws s3 cp ${DXBuildNumber}_linux_oracle_95_other_frt_ctf.zip s3://dx-testarea

fi
