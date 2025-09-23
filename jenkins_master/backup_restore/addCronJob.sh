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

ROOT_JOB=$1

user=$(whoami)
if [ "$user" != "root" ]; then
   echo "ERROR:  This script can only run as root"
   echo "SYNTAX: addCronJob jobname"
   echo "        jobname = path and name of script to add"
   echo "        This script will add the given script as new cronjob for root."
   echo "        The new cronjob will run every 5 minutes."
   exit 1
fi

if [ "$ROOT_JOB" == "" ]; then
   echo "ERROR:  No cron job given"
   echo "SYNTAX: addCronJob jobname"
   echo "        jobname = path and name of script to add"
   echo "        This script will add the given script as new cronjob for root."
   echo "        The new cronjob will run every 5 minutes."
   exit 1
else
   ROOT_CRONJOB=$(basename $ROOT_JOB)
   if [ -e /root/$ROOT_CRONJOB ]; then
      echo "Cron job $ROOT_CRONJOB already in /root" 
   else
      echo "Copy cron job"
      cp $ROOT_JOB /root/$ROOT_CRONJOB
      chown root:root /root/$ROOT_CRONJOB
      chmod 700 /root/$ROOT_CRONJOB
   fi
   CRONT_TEST=$(crontab -l |grep "/root/$ROOT_CRONJOB")
   if [ "$CRONT_TEST" == "" ]; then
      echo "Installing cron job"
      (crontab -l 2>/dev/null; echo "# Check Jenkins every 5 minutes for root job") | crontab -
      (crontab -l 2>/dev/null; echo "*/5 * * * * \"/root/$ROOT_CRONJOB\"") | crontab -
   else
      echo "Cron job already installed for root"
   fi
fi