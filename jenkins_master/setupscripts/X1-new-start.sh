#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2001, 2020. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
 
curl http://127.0.0.1:8080

## Start Jenkins w/o admin wizard and run basic-security.groovy
systemctl stop jenkins

## Reset saved JENKINS_ARGS
sed -i "/JENKINS_ARGS=/c\JENKINS_ARGS=\"$JENKINS_ARGS\"" /etc/sysconfig/jenkins

## Deactivate initial boot script directory
mv $JENKINS_HOME/init.groovy.d $JENKINS_HOME/used.init.groovy.d

## Restart Jenkins 
systemctl start jenkins
systemctl status jenkins

