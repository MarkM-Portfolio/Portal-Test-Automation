# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2001, 2020. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

# Setup Jenkins Agents

## Get cmd parameter
JENKINS_URL=$1
AGENTS_LIST=$2
URL_DOMAIN_NAME=$3
WPBUILD_CREDENTIALS_ID=$4
USER_ADMIN=$5
DIRECTORY_LIST=$6

## Set agents config file
AGENT_CONFIG="helpers/agents.txt"

## Set JSON files
JSON_CONFIG="helpers/node.json"
JSON_TEMPLATE="helpers/node.json.template"

## Create API token for admin
COOKIE_FILE="helpers/node.json"
CRUMB=$(curl $JENKINS_URL/crumbIssuer/api/xml?xpath=concat\(//crumbRequestField,%22:%22,//crumb\) --silent --user "$USER_ADMIN:$USER_ADMIN" -c $COOKIE_FILE)
TOKEN=$(curl "$JENKINS_URL/user/admin/descriptorByName/jenkins.security.ApiTokenProperty/generateNewToken" --silent --data "newTokenName=AdminApiToken" --user "$USER_ADMIN:$USER_ADMIN" -H "$CRUMB" -b $COOKIE_FILE)
TOKEN=${TOKEN##*\":\"}
TOKEN=${TOKEN%\"*}
JENKINS_PASS="$USER_ADMIN:$TOKEN"

echo "Register agents"

while IFS= read -r line; do
   line=${line:1:${#line}-2}
   SLAVE_NAME=${line%%\",\"*}
   echo $SLAVE_NAME
   if [[ ",$AGENTS_LIST," == *"$SLAVE_NAME"* ]]; then
      echo "  add agent"
      line=${line#*\",\"}
      NO_EXECUTORS=${line%%\",\"*}
      line=${line#*\",\"}
      AGENT_LABELS=${line%%\",\"*}
      line=${line#*\",\"}
      DESCRIPTION=${line%%\",\"*}
      cp $JSON_TEMPLATE $JSON_CONFIG
      sed -i "s/\${INSTANCE_SLAVE_NAME}/$SLAVE_NAME/g" $JSON_CONFIG
      sed -i "s/\${AGENT_DESCRIPTION}/$DESCRIPTION/g" $JSON_CONFIG
      sed -i "s/\${NUMBER_OF_EXECUTORS}/$NO_EXECUTORS/g" $JSON_CONFIG
      sed -i "s/\${AGENT_LABELS}/$AGENT_LABELS/g" $JSON_CONFIG
      sed -i "s/\${URL_DOMAIN_NAME}/$URL_DOMAIN_NAME/g" $JSON_CONFIG
      sed -i "s/\${WPBUILD_CREDENTIALS_ID}/$WPBUILD_CREDENTIALS_ID/g" $JSON_CONFIG
      curl -L -s -o /dev/null -w "%{http_code}" -u $JENKINS_PASS \
        -H "Content-Type:application/x-www-form-urlencoded" -X POST \
        -d "json=$(cat $JSON_CONFIG)" \
        "$JENKINS_URL/computer/doCreateItem?name=$SLAVE_NAME&type=hudson.slaves.DumbSlave"; \
        echo
   else
      echo "  not in passed agent list"
   fi
done < "$AGENT_CONFIG"

echo "Set master offline"
curl --silent "$JENKINS_URL/computer/(master)/toggleOffline" --request "POST" --data "offlineMessage=Do not use master node" -u $JENKINS_PASS

for FOLDER_NAME in $DIRECTORY_LIST; do
   echo $FOLDER_NAME
   JSON_STRING="{\"name\":\"$FOLDER_NAME\",\"mode\":\"com.cloudbees.hudson.plugins.folder.Folder\",\"from\":\"\",\"Submit\":\"OK\"}&Submit=OK"
   curl -k -s -o /dev/null -X POST -u $JENKINS_PASS \
     -H "Content-Type:application/x-www-form-urlencoded" \
     "$JENKINS_URL/createItem?name=$FOLDER_NAME&mode=com.cloudbees.hudson.plugins.folder.Folder&from=&json=$JSON_STRING"
done