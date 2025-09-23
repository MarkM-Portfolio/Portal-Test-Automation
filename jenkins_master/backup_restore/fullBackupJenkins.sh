#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2021, 2024. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

FILE_PREFIX="$FILE_PREFIX"
GENERIC_URL="$GENERIC_URL"
UPLOAD_PATH="$UPLOAD_PATH"
UPLOAD_CREDS="$CREDS"
MAX_KEEP_BKP="$MAX_BKP"
JENKINS_SYSCONFIG="/etc/sysconfig/jenkins"

#
# function to get location of jenkins.war
# Location maybe defined in Jenkins service script
#
getJenkinsWarLocation() {
   jenkinsWar=""
   jenkinsWarDefault=""
   # Get Jenkins service script
   jenkinsService=$(systemctl status jenkins.service |grep "Loaded: loaded" |cut -d'(' -f2 |cut -d';' -f1)
   # Get number of detected JENKINS_WAR= statements
   JenkinsWarCtr=$(cat $jenkinsService |grep -c "JENKINS_WAR=")
   if [ "$JenkinsWarCtr" == "0" ]; then
      # If not found try some common places
      if [ -e "/usr/lib/jenkins/jenkins.war" ]; then
         jenkinsWarDefault="/usr/lib/jenkins/jenkins.war"
      else
         if [ -e "/usr/share/java/jenkins.war" ]; then
            jenkinsWarDefault="/usr/share/java/jenkins.war"
         fi
      fi
   elif [ "$JenkinsWarCtr" == "1" ]; then
      # If found only once strip it out
      jenkinsWar=$(cat $jenkinsService |grep "JENKINS_WAR=" |sed 's/\"//g')
      jenkinsWar=${jenkinsWar##*JENKINS_WAR=}
   else
      jenkinsWarFound=($(cat $jenkinsService |grep "JENKINS_WAR=" |sed 's/\"//g'))
      for loc in "${jenkinsWarFound[@]}"; do
         if [[ "$loc" != "#"* ]]; then
            # take latest definition from service script
            jenkinsWar=${loc##*JENKINS_WAR=}
         else
            if [[ "$loc" == *"Environment=\"JENKINS_WAR"* ]]; then
               # use commented environment as default
               jenkinsWarDefault=${loc##*JENKINS_WAR=}
            fi
         fi
      done
   fi
   if [ "$jenkinsWar" == "" ]; then
      jenkinsWar=$jenkinsWarDefault
   fi
   echo $jenkinsWar
}

JENKINS_WAR=$(getJenkinsWarLocation)
JENKINS_HOME=$(cat $JENKINS_SYSCONFIG |grep "JENKINS_HOME=" |awk '{split($0,a,"\""); print a[2]}')

TIME_STAMP=$(date +%Y%m%d_%H%M%S)
echo "[$TIME_STAMP] Create backup file"
d=$TIME_STAMP
UPLOAD_FILE="${FILE_PREFIX}-bkp_$d.tgz"
tar -czf $UPLOAD_FILE --exclude=$JENKINS_HOME/workspace --exclude=$JENKINS_HOME/caches --exclude=$JENKINS_HOME/logs $JENKINS_HOME/* $JENKINS_SYSCONFIG $JENKINS_WAR

TIME_STAMP=$(date +%Y%m%d_%H%M%S)
echo "[$TIME_STAMP] Upload backup file"
ret=$(curl -u "$UPLOAD_CREDS" -X PUT "${GENERIC_URL}${UPLOAD_PATH}${UPLOAD_FILE}" -T $UPLOAD_FILE)
echo "$ret"
rm -f $UPLOAD_FILE

TIME_STAMP=$(date +%Y%m%d_%H%M%S)
echo "[$TIME_STAMP] Cleanup for maximum backups"
GENERIC_LIST_URL=$(echo ${GENERIC_URL} |awk '{split($0,a,"/",sep); print a[1],sep[1],sep[2],a[3],sep[3],a[4],sep[4],"list",sep[4],a[5]}')
GENERIC_LIST_URL=${GENERIC_LIST_URL// /}
BKP_CHECK="${FILE_PREFIX}-bkp_"
BKP_STORED=$(curl -s "${GENERIC_LIST_URL}${UPLOAD_PATH}" |grep -c "${BKP_CHECK}")
while [ "$BKP_STORED" -gt "$MAX_KEEP_BKP" ]; do
    BKP_LIST=$(curl -s "${GENERIC_LIST_URL}${UPLOAD_PATH}" |grep "${BKP_CHECK}")
    d=$(echo $BKP_LIST |awk '{split($0,a,"\""); print a[2]}')
    echo "[$TIME_STAMP] Delete ${GENERIC_LIST_URL}${UPLOAD_PATH}$d"
    curl -u "$UPLOAD_CREDS" -X DELETE "${GENERIC_LIST_URL}${UPLOAD_PATH}$d"
    BKP_STORED=$(curl -s "${GENERIC_LIST_URL}${UPLOAD_PATH}" |grep -c "${BKP_CHECK}")
done
