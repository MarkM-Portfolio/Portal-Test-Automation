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

source ~/.bash_profile

docker_build_tools_id=$(docker ps | grep quintana-docker.artifactory.cwp.pnp-hcl.com/portal-build:* | awk '{ print $1 }')

echo "Setting up CTF"
cd /opt/git
docker cp ctf.care.full.install.zip dx_core:/home/dx_user
docker exec -i dx_core /bin/sh -c "
cd /home/dx_user;
mkdir shared;
cd shared;
unzip ../ctf.care.full.install.zip;
curl -O https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-build-output/ctf/CTF_BUILD_LABEL.zip;
cd care;
unzip ../CTF_BUILD_LABEL.zip;
mkdir -p /home/dx_user/shared/care/ctf/lib;
mkdir -p /home/dx_user/shared/care/ctf/prereq;
chmod 755 -R /opt/HCL/ConfigEngine/;
cd /home/dx_user/shared/care/rt/ref;
. ./copysetup.sh;
cd /home/dx_user/shared/care;
sed -i \"s/WebSphere/HCL/g\" setupenv.properties;
. ./setupenv.sh;
cd /home/dx_user/shared/care/rt;
ant -Ddeploy=true care.deploy.ctf"

echo "Updating hostname and port values in ctf.properties"
docker exec -i dx_core /bin/sh -c "sed -i 's/@hostname@/INSTANCE_IP/g' /opt/HCL/ctf.properties"
docker exec -i $(docker ps | grep quintana-docker.artifactory.cwp.pnp-hcl.com/portal-build:* | awk '{ print $1 }') sh -c "sed -i 's/172.17.0.2/INSTANCE_IP/g' /root/ctf.properties"
docker exec -i $(docker ps | grep quintana-docker.artifactory.cwp.pnp-hcl.com/portal-build:* | awk '{ print $1 }') sh -c "sed -i 's/30015/10039/g' /root/ctf.properties"

echo "Building pluto.ext.ide.testrunner"
cd /opt/git/git-dx-clone/wcm.ext/wp/code/pluto.ext.ide.testrunner/test/src/runner/wcm/suite/
sed '/WcmIUTSuite_Services\|WcmIUTSuite_API\|WcmIUTSuite_Domain\|WcmIUTSuite_Syndication/d' -i WcmIUTSuite.java
cd /opt/git/git-dx-clone/wcm.ext/wp/code/pluto.ext.ide.testrunner/ && wsbld

echo "Copy servlet.war"
docker cp /opt/git/pluto.ext.ide.testrunner.servlet.war dx_core:/opt/HCL/

echo "Deploy pluto_ext_ide_testrunner_servlet_war"
docker exec -i dx_core /bin/sh -c "
cd /opt/HCL/AppServer/bin;
./wsadmin.sh  -lang jython -c \"AdminApp.install('/opt/HCL/pluto.ext.ide.testrunner.servlet.war', '[ -MapWebModToVH [[ .* .* default_host ]]   -appname pluto_ext_ide_testrunner_servlet_war]')\" -user wpsadmin -password wpsadmin"

echo "Setting the context_root and security role mapping"
echo $'options = []
options.append("-CtxRootForWebMod")
options.append([[".*", ".*", "/iutrunner"]])
AdminApp.edit("pluto_ext_ide_testrunner_servlet_war", options)
AdminConfig.save()' >> /tmp/context_root.py
echo $'AdminApp.edit("pluto_ext_ide_testrunner_servlet_war", \'[-MapRolesToUsers [["All Role" Yes No "" ""]]\')
AdminConfig.save()' >> /tmp/security_mapping.py

echo "Deploying and starting the application"
docker cp /tmp/context_root.py dx_core:/opt/HCL/
docker cp /tmp/security_mapping.py dx_core:/opt/HCL/
docker exec -i dx_core /bin/sh -c "
cd /opt/HCL/AppServer/bin;
./wsadmin.sh -user wpsadmin -password wpsadmin -lang jython -f /opt/HCL/context_root.py;
./wsadmin.sh -user wpsadmin -password wpsadmin -lang jython -f /opt/HCL/security_mapping.py;
./wsadmin.sh -c \"AdminControl.invoke(AdminControl.queryNames('type=ApplicationManager,*'),'startApplication','pluto_ext_ide_testrunner_servlet_war')\" -lang jython -user wpsadmin -password wpsadmin"

echo "Running the IUT tests"
cd /opt/git/git-dx-clone/wcm.ext/wp/code/pluto.ext.ide.testrunner/test && wsbld do-run-tests

mkdir -p /opt/git/iut_xml_reports

echo "Copying REST report"
cp /opt/git/git-dx-clone/wcm.ext/wp/code/pluto.ext.ide.testrunner/test/TEST-runner.wcm.suite.WcmIUTSuite.xml /opt/git/iut_xml_reports/Result-REST.xml

echo "Updating IUT runner to run WCM API"
cp -f /opt/git/WcmIUTSuite.sh /opt/git/git-dx-clone/wcm.ext/wp/code/pluto.ext.ide.testrunner/test/src/runner/wcm/suite/WcmIUTSuite.java
sed '29 i com.ibm.workplace.wcm.wcmiutsuite.WcmIUTSuite_API.class,com.ibm.workplace.wcm.wcmiutsuite.WcmIUTSuite_Domain.class,' -i /opt/git/git-dx-clone/wcm.ext/wp/code/pluto.ext.ide.testrunner/test/src/runner/wcm/suite/WcmIUTSuite.java
cd /opt/git/git-dx-clone/wcm.ext/wp/code/pluto.ext.ide.testrunner/ && wsbld

echo "Running WCM API IUT"
chmod +x /tmp/setup_single_iut.sh && sh /tmp/setup_single_iut.sh

echo "Copying WCM API report"
cp /opt/git/git-dx-clone/wcm.ext/wp/code/pluto.ext.ide.testrunner/test/TEST-runner.wcm.suite.WcmIUTSuite.xml /opt/git/iut_xml_reports/Result-API.xml

echo "Updating IUT runner to run WCM Services"
cp -f /opt/git/WcmIUTSuite.sh /opt/git/git-dx-clone/wcm.ext/wp/code/pluto.ext.ide.testrunner/test/src/runner/wcm/suite/WcmIUTSuite.java
sed '29 i com.ibm.workplace.wcm.wcmiutsuite.WcmIUTSuite_Services.class,' -i /opt/git/git-dx-clone/wcm.ext/wp/code/pluto.ext.ide.testrunner/test/src/runner/wcm/suite/WcmIUTSuite.java
cd /opt/git/git-dx-clone/wcm.ext/wp/code/pluto.ext.ide.testrunner/ && wsbld

echo "Running WCM Services IUT"
chmod +x /tmp/setup_single_iut.sh && sh /tmp/setup_single_iut.sh

echo "Copying WCM Services report"
cp /opt/git/git-dx-clone/wcm.ext/wp/code/pluto.ext.ide.testrunner/test/TEST-runner.wcm.suite.WcmIUTSuite.xml /opt/git/iut_xml_reports/Result-Services.xml

echo "Update IUT runner to run WCM Syndication"
cp -f /opt/git/WcmIUTSuite.sh /opt/git/git-dx-clone/wcm.ext/wp/code/pluto.ext.ide.testrunner/test/src/runner/wcm/suite/WcmIUTSuite.java
sed '29 i com.ibm.workplace.wcm.wcmiutsuite.WcmIUTSuite_Syndication.class,' -i /opt/git/git-dx-clone/wcm.ext/wp/code/pluto.ext.ide.testrunner/test/src/runner/wcm/suite/WcmIUTSuite.java
cd /opt/git/git-dx-clone/wcm.ext/wp/code/pluto.ext.ide.testrunner/ && wsbld

echo "Running WCM Syndication"
chmod +x /tmp/setup_single_iut.sh && sh /tmp/setup_single_iut.sh

echo "Copying WCM Syndication report"
cp /opt/git/git-dx-clone/wcm.ext/wp/code/pluto.ext.ide.testrunner/test/TEST-runner.wcm.suite.WcmIUTSuite.xml /opt/git/iut_xml_reports/Result-Syndication.xml

mkdir -p /opt/git/report

cd /opt/git
docker cp dx_core:/opt/HCL/wp_profile/logs/WebSphere_Portal/SystemOut.log .
docker cp dx_core:/opt/HCL/wp_profile/logs/WebSphere_Portal/SystemErr.log .