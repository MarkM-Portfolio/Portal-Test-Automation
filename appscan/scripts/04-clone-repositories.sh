#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2023. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
 
#
# The script determines the latest docker images to use, and pulls them
# Clones the given repos and creates the Appscan scan script cli_script.txt
#

if [ $# -ne 5 ] ; then
  echo "Usage: $0 keyfile passfile branch scanlist scanrepo"
  exit 1
fi

branchName=$3
scanList=$4
scanRepo=$5

containerRootDir="/Apps"
hostAppscanDir="$HOME/appscan"
repoDir="$hostAppscanDir/repos"
reportsDir="reports"
scriptFile="cli_script.txt"

eval $(ssh-agent)
pass=$(cat $2)

expect << EOF
  spawn ssh-add $1
  expect "Enter passphrase"
  send "$pass\r"
  expect eof
EOF

mkdir -p $repoDir
cd $repoDir

# Clone repos to scan
for repo in $scanList; do
    git clone --single-branch --branch $branchName git@git.cwp.pnp-hcl.com:${repo}.git
done

# Get scan PAF list
if [ "$scanRepo" == "full" ]; then
    pafList=$(find $repoDir -name "*.paf")
else
    pafList=$(find $repoDir/$scanRepo -name "*.paf")
fi

# Create cli_script.txt from found PAF files starting with login
echo "login" > $scriptFile
echo " " >> $scriptFile

# Add scan code for all found PAF files
# 1. strip host root directory to get relative path usable inside container
# 2. get repo name from PAF file
# 3. add code lines for repo
for paf in $pafList; do
   appName=$(cat $paf |grep "<Application " | awk '{print $2}')
   # returns the value of a key=value pair
   appName=${appName##*=}
   # removes all '"'
   appName=${appName//\"/}
   paf=${paf/$repoDir\//}
   echo "openapplication $containerRootDir/${paf}" >> $scriptFile
   echo "scan $containerRootDir/$reportsDir" >> $scriptFile
   echo "report \"Findings by File\" pdf-detailed $containerRootDir/$reportsDir/${appName}.pdf -includeTrace:definitive" >> $scriptFile
   echo "report \"Findings by File\" zip $containerRootDir/$reportsDir/${appName}.zip -includeTrace:definitive" >> $scriptFile
   echo " " >> $scriptFile
done

# Finish cli_script.txt with logout
echo " " >> $scriptFile
echo "logout" >> $scriptFile
echo " " >> $scriptFile

echo "AppScan script file:"
cat $scriptFile

mkdir $reportsDir

echo "done cloning repositories"
