#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2019, 2022. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
 
# Install Whitesource

cd ~/whitesource
curl -LJO https://github.com/whitesource/unified-agent-distribution/releases/download/v23.8.2/wss-unified-agent.jar
