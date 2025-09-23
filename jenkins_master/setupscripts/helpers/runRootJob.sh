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
# *                                                                  *
# * This job is considered to be used as cron job for user root.     *
# * It will get the JENKINS_HOME directory from the Jenkins system   *
# * configuration and then scans there the jobs directory for the    *
# * trigger file 'runAsRoot.job'. This trigger file if found has an  *
# * entry ROOT_JOB=xxxxx which passes the script to run under root   *
# * authority.                                                       *
# *                                                                  *
# * Before starting the script it will be moved along with the job   *
# * trigger to the workdirectory of the cronjob. After the script    *
# * finished the script and trigger file will be deleted from the    *
# * working directory.                                               *
# *                                                                  *
# * This process will be used for the Jenkins full backup process    *
# * since this needs root access to some of the file being part of   *
# * the backup. The backup script itself is part of the housekeeping *
# * of the Portal-Server-Build-Tools Git repo.                       *
# ********************************************************************

WORKDIR=$(dirname $0)
cd $WORKDIR

#JENKINS_SYSCONFIG="/etc/sysconfig/jenkins"
#JENKINS_HOME=$(cat $JENKINS_SYSCONFIG |grep "JENKINS_HOME=" |awk '{split($0,a,"\""); print a[2]}')
## Get JENKINS_HOME
JENKINS_HOME=$(cat /lib/systemd/system/jenkins.service | grep "JENKINS_HOME=")
JENKINS_HOME=${JENKINS_HOME//Environment=/}
JENKINS_HOME=${JENKINS_HOME//\"/}
JENKINS_HOME=${JENKINS_HOME//JENKINS_HOME=/}
JOB_TRIGGER="runAsRoot.job"
JOB_LOG="runRootJob.log"

FOUND_TRIGGER=$(find $JENKINS_HOME/jobs -name $JOB_TRIGGER -type f)
if [ "$FOUND_TRIGGER" == "" ]; then
    exit
fi

ROOT_JOB=$(cat $FOUND_TRIGGER |grep "ROOT_JOB=" |awk '{split($0,a,"="); print a[2]}')
ROOT_JOB=${ROOT_JOB/\$JENKINS_HOME/$JENKINS_HOME}
LOCAL_JOB=$(basename $ROOT_JOB)

STILL_RUNNING=$(ps -ef |grep "${LOCAL_JOB}" |grep -v " grep ")
if [ "$STILL_RUNNING" != "" ]; then
    exit
fi

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
echo "[$TIMESTAMP] ====================== Run root job ======================" >> $JOB_LOG

if [ -f $LOCAL_JOB ]; then
    echo "[$TIMESTAMP] Found script from previous run"
    echo "[$TIMESTAMP] Delete old $LOCAL_JOB"
    rm -f $LOCAL_JOB
fi
if [ -f $JOB_TRIGGER ]; then
    echo "[$TIMESTAMP] Found job trigger from previous run"
    echo "[$TIMESTAMP] Delete old job trigger"
    rm -f $JOB_TRIGGER
fi

mv $FOUND_TRIGGER .
mv $ROOT_JOB .

echo "[$TIMESTAMP] Start $LOCAL_JOB" >> $JOB_LOG
sh $LOCAL_JOB >> $JOB_LOG
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
echo "[$TIMESTAMP] Finished $LOCAL_JOB" >> $JOB_LOG
rm -f $LOCAL_JOB
rm -f $JOB_TRIGGER