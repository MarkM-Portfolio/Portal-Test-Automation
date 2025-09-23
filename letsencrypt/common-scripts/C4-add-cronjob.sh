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

## Get parameters
SCRIPT_NAME=$(basename "${BASH_SOURCE[0]}")
RENEWAL_SCRIPT="$1"

if [ "$RENEWAL_SCRIPT" == "" ]; then
   echo "ERROR: Parameters missing!"
   echo "Syntax: $SCRIPT_NAME RENEWAL_SCRIPT"
   echo "        RENEWAL_SCRIPT - Script to add as cronjob (full qualified path)"
   exit 1
fi

if ! [ -f "$RENEWAL_SCRIPT" ]; then
  echo "Can not install cronjob, $RENEWAL_SCRIPT not found."
  exit 1
fi

if crontab -l | grep "$RENEWAL_SCRIPT"; then
  echo "Cronjob already installed"
else
  echo "Installing cron job"
  (crontab -l 2>/dev/null; echo "# Try to renew certs once a day") | crontab -
  (crontab -l 2>/dev/null; echo "27 0 * * * \"$RENEWAL_SCRIPT\"") | crontab -
fi

if [ "$?" != "0" ]; then
  echo "Install cron job failed. You need to manually renew your certs."
  echo "Or you can add cronjob by yourself adding the following line to crontab:"
  echo "27 0 * * * \"$RENEWAL_SCRIPT\""
  exit 1
fi
