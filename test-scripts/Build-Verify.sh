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
echo "Starting to run the following ARTs..."
#base
echo "in base..."
ant ctf.run -Dctf.input=/test.ac.rest/regression.tsf
cp -avr /shared/care/ctf/build/reports/base/test.ac.rest /root/reports
ant ctf.run -Dctf.input=/test.content.templating/regression.tsf
cp -avr /shared/care/ctf/build/reports/base/test.content.templating /root/reports
ant ctf.run -Dctf.input=/test.db/regression.tsf
cp -avr /shared/care/ctf/build/reports/base/test.db /root/reports
ant ctf.run -Dctf.input=/test.engine/regression.tsf
cp -avr /shared/care/ctf/build/reports/base/test.engine /root/reports
ant ctf.run -Dctf.input=/test.resolver.aggregation/regression.tsf
cp -avr /shared/care/ctf/build/reports/base/test.resolver.aggregation /root/reports
ant ctf.run -Dctf.input=/test.user/regression.tsf
cp -avr /shared/care/ctf/build/reports/base/test.user /root/reports
ant ctf.run -Dctf.input=/test.xml/regression.tsf
cp -avr /shared/care/ctf/build/reports/base/test.xml /root/reports
#jcr
#echo "in jcr..."
ant ctf.run -Dctf.input=/test.wp.content.repository/regression.tsf
cp -avr /shared/care/ctf/build/reports/jcr/test.wp.content.repository /root/reports
#search
echo "in search..."
ant ctf.run -Dctf.input=/test.search.rcss/regression.tsf
cp -avr /shared/care/ctf/build/reports/search/test.search.rcss /root/reports
ant ctf.run -Dctf.input=/test.searchengine.index/regression.tsf
cp -avr /shared/care/ctf/build/reports/search/test.searchengine.index /root/reports
ant ctf.run -Dctf.input=/test.searchengine.query/regression.tsf
cp -avr /shared/care/ctf/build/reports/search/test.searchengine.query /root/reports
#toolbar
echo "in toolbar..."
ant ctf.run -Dctf.input=/test.portlet.changelayout/regression.tsf
cp -avr /shared/care/ctf/build/reports/toolbar/test.portlet.changelayout /root/reports
ant ctf.run -Dctf.input=/test.portlet.contenttabs/regression.tsf
cp -avr /shared/care/ctf/build/reports/toolbar/test.portlet.contenttabs /root/reports
ant ctf.run -Dctf.input=/test.toolbar.content.api/regression.tsf
cp -avr /shared/care/ctf/build/reports/toolbar/test.toolbar.content.api /root/reports
#pzn.ext
echo "in pzn.ext..."
ant ctf.run -Dctf.input=/test.oob/regression.tsf
cp -avr /shared/care/ctf/build/reports/pzn.ext/test.oob /root/reports
#Defect for investigating the managed pages tests causing subsequent tests to fail can be found here:  https://jira.cwp.pnp-hcl.com/browse/DXQ-4702
#ant ctf.run -Dctf.input=/test.pzn.ext.managedpages/regression.tsf
#cp -avr /shared/care/ctf/build/reports/pzn.ext/test.pzn.ext.managedpages /root/reports
ant ctf.run -Dctf.input=/test.wcm.rendering/regression.tsf
cp -avr /shared/care/ctf/build/reports/pzn.ext/test.wcm.rendering /root/reports
ant ctf.run -Dctf.input=/test.wp.templating.wcm/regression.tsf
cp -avr /shared/care/ctf/build/reports/pzn.ext/test.wp.templating.wcm /root/reports
ant ctf.run -Dctf.input=/test.wp.wcm.sync/regression.tsf
cp -avr /shared/care/ctf/build/reports/pzn.ext/test.wp.wcm.sync /root/reports

#Converting reports to Zip file and copying to Dashboard...
echo "Converting reports to Zip file and copying to Dashboard..."
cd /opt/zips
if [ $DXBuildName == "DX_9_CF17_BVT_" ]; then
    echo "build Num=$DXBuildNumber and buildname=$DXBuildName"
    tar czf ${DXBuildNumber}_linux_db2_cf17_bvt_ctf.zip /root/reports
    aws s3 cp ${DXBuildNumber}_linux_db2_cf17_bvt_ctf.zip s3://dx-testarea
elif [ $DXBuildName == "DX_95_BVT_" ]; then
    tar czf ${DXBuildNumber}_linux_db2_95_bvt_ctf.zip /root/reports
    aws s3 cp ${DXBuildNumber}_linux_db2_95_bvt_ctf.zip s3://dx-testarea
else
    tar czf ${DXBuildNumber}_linux_oracle_95_bvt_ctf.zip /root/reports
    aws s3 cp ${DXBuildNumber}_linux_oracle_95_bvt_ctf.zip s3://dx-testarea

fi
