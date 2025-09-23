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

echo "EC2 Instance Name: $1"
TMP_INSTANCE_ID=$(aws ec2 --region $2 describe-instances --filters Name=tag:Name,Values=$1 | grep InstanceId | awk -F: '{print $2;exit}' | sed 's/"//g; s/,//g')
echo "Removing $TMP_INSTANCE_ID"
aws ec2 --region $2 terminate-instances --instance-id $TMP_INSTANCE_ID
echo "Sleeping for 5 seconds ..." && sleep 5
echo "Trying to get the instance ID after the deletion"
TMP_INSTANCE_ID=$(aws ec2 --region $2 describe-instances --filters Name=tag:Name,Values=$1 | grep InstanceId | awk -F: '{print $2;exit}' | sed 's/"//g; s/,//g')
echo "InstanceID is ( $TMP_INSTANCE_ID )"
