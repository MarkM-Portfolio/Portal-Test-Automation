#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2019, 2023. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
 
# Install Whitesource

cd ~/whitesource-leap
curl -LJO https://github.com/whitesource/unified-agent-distribution/releases/download/v24.1.1/wss-unified-agent.jar
#curl -LJO https://unified-agent.s3.amazonaws.com/wss-unified-agent.jar

echo "Done with Leap Whitesource 03-install-whitesource"