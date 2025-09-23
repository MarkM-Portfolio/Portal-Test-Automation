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
echo "in jcr..."
ant ctf.run -Dctf.input=/test.wp.content.repository/regression.tsf
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
tar czf ${BUILD_LABEL}_linux_db2_${CF_VERSION}_bvt_v95_ctf.zip /root/reports
aws s3 cp ${BUILD_LABEL}_linux_db2_${CF_VERSION}_bvt_v95_ctf.zip s3://dx-testarea
