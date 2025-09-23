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

# Update executor-report.html with the result of the last cycle

numberOfExecutor=$1
timeLabel=$2
reportFile=$3                

sed -i "/label: \"$timeLabel\"/c\{ y: $numberOfExecutor, label: \"$timeLabel\" }," ${reportFile}
