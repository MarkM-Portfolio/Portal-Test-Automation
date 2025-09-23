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
 
TMP_INSTANCE_ID=$(aws --region $AWS_REGION ec2 describe-instances --filters Name=tag:Name,Values=$INSTANCE_NAME | grep InstanceId | awk -F: '{print $2;exit}' | sed 's/"//g; s/,//g')
echo "Temporary instance id: $TMP_INSTANCE_ID"

if [ -z $TMP_INSTANCE_ID ]
then
  echo "Nothing to remove"
else
  echo "Removing $TMP_INSTANCE_ID"
  aws ec2 create-tags --resources $TMP_INSTANCE_ID --tags "Key=Name,Value=$TMP_INSTANCE_ID"
  aws ec2 terminate-instances --instance-id $TMP_INSTANCE_ID
  echo "Sleeping for 5 seconds ..." && sleep 5
fi 
