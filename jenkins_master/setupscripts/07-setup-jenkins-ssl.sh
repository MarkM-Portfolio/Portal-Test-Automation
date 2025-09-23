# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2001, 2020. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

## Get parameters
JENKINS_MASTER=$1
# Sysconfig is obsolete
JENKINS_SYSCONFIG="/etc/sysconfig/jenkins"

## Get JENKINS_HOME
JENKINS_HOME=$(cat /lib/systemd/system/jenkins.service | grep "JENKINS_HOME=")
JENKINS_HOME=${JENKINS_HOME//Environment=/}
JENKINS_HOME=${JENKINS_HOME//\"/}
JENKINS_HOME=${JENKINS_HOME//JENKINS_HOME=/}
echo "JENKINS_HOME = $JENKINS_HOME"

echo "Set Jenkins access to SSL for $JENKINS_MASTER"
systemctl stop jenkins

## Change Jenkins config from HTTP to HTTPS
#sed -i 's|JENKINS_PORT="8080"|JENKINS_PORT="-1"|g' $JENKINS_SYSCONFIG
echo "Environment=\"JENKINS_PORT=-1\"" >> /etc/systemd/system/jenkins.service.d/override.conf
#sed -i 's|JENKINS_HTTPS_PORT=""|JENKINS_HTTPS_PORT="8443"|g' $JENKINS_SYSCONFIG
echo "Environment=\"JENKINS_HTTPS_PORT=8443\"" >> /etc/systemd/system/jenkins.service.d/override.conf
#sed -i 's|JENKINS_HTTPS_KEYSTORE=""|JENKINS_HTTPS_KEYSTORE="/var/lib/jenkins/jenkins.jks"|g' $JENKINS_SYSCONFIG
echo "Environment=\"JENKINS_HTTPS_KEYSTORE=/var/lib/jenkins/jenkins.jks\"" >> /etc/systemd/system/jenkins.service.d/override.conf
#sed -i 's|JENKINS_HTTPS_KEYSTORE_PASSWORD=""|JENKINS_HTTPS_KEYSTORE_PASSWORD="admin123"|g' $JENKINS_SYSCONFIG
echo "Environment=\"JENKINS_HTTPS_KEYSTORE_PASSWORD=admin123\"" >> /etc/systemd/system/jenkins.service.d/override.conf
#sed -i 's|JENKINS_HTTPS_LISTEN_ADDRESS=""|JENKINS_HTTPS_LISTEN_ADDRESS="0.0.0.0"|g' $JENKINS_SYSCONFIG
echo "Environment=\"JENKINS_HTTPS_LISTEN_ADDRESS=0.0.0.0\"" >> /etc/systemd/system/jenkins.service.d/override.conf

systemctl reload-daemon

## Navigate to location of JENKINS_HOME and generate required keystore
cd $JENKINS_HOME
openssl req -nodes -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365 -subj /CN=$JENKINS_MASTER
openssl pkcs12 -export -out jenkins.p12 -passout 'pass:admin123' -inkey key.pem -in cert.pem -name $JENKINS_MASTER
keytool -importkeystore -srckeystore jenkins.p12 -srcstorepass 'admin123' -srcstoretype PKCS12 -srcalias $JENKINS_MASTER -deststoretype JKS -destkeystore jenkins.jks -deststorepass 'admin123' -destalias $JENKINS_MASTER
chown jenkins:jenkins jenkins.jks
chmod 644 jenkins.jks

## Start jenkins service
## Start and connect to Jenkins, retry max 6 times
echo "Restart Jenkins"
systemctl start jenkins
max_loop=18
while [ "$max_loop" != "0" ]; do
   echo "   Continuing in 10.0 Seconds...."
   sleep 10s
   jenkins_up=$(curl -s https://$JENKINS_MASTER:8443 --insecure)
   if [[ $jenkins_up == *"Dashboard"* ]]; then
      break;
   fi
   ((max_loop--))
done

## Fail pipeline if Jenkins didn't start
if [ "$max_loop" == "0" ]; then
   echo "ERROR: Jenkins did not start as expected."
   exit 1
fi

echo "Jenkins is up and running."