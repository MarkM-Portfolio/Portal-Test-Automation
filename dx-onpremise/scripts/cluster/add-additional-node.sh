# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2021. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

host=$1
ip=$2
secondary_host=$3
was_username=$4
was_password=$5
secondary_host_name=$6
domain_suffix=$7
PERSONALISED_HOME=$8
CONTEXT_ROOT=$9
DEFAULT_HOME=${10}

yum install -y psmisc

/opt/HCL/AppServer/bin/manageprofiles.sh -create -templatePath /opt/HCL/PortalServer/profileTemplates/managed.portal -profileName wp_profile -profilePath /opt/HCL/wp_profile -cellName ip-${secondary_host}Cell -nodeName ip-${secondary_host}Node -hostName ${secondary_host_name}${domain_suffix} -startingPort 10012

tar -zxf /tmp/dx-onpremise/db2home.tar.gz --directory /opt/HCL/wp_profile

/opt/HCL/wp_profile/ConfigEngine/ConfigEngine.sh  -DSaveParentProperties=true -DparentProperties=/tmp/dx-onpremise/scripts/cluster/properties/additional-node/FederateNodeRemote.properties -DWasPassword=$was_password -DDmgrNodeName=dmgrNode01 -DDmgrCellName=dmgrCell01 -DdmgrProfilePath=/opt/HCL/AppServer/profiles/dmgr01 -DisRemoteDmgr=true add-node -DWasPassword=$was_password

if [ $? -eq 0 ] 
then 
  echo "Successfully federated" 
else 
  echo "First federation failed. Wait and try again."
  sleep 30m
  /opt/HCL/wp_profile/ConfigEngine/ConfigEngine.sh  -DSaveParentProperties=true -DparentProperties=/tmp/dx-onpremise/scripts/cluster/properties/additional-node/FederateNodeRemote.properties -DWasPassword=$was_password -DDmgrNodeName=dmgrNode01 -DDmgrCellName=dmgrCell01 -DdmgrProfilePath=/opt/HCL/AppServer/profiles/dmgr01 -DisRemoteDmgr=true add-node -DWasPassword=$was_password  
fi

if [ "$PERSONALISED_HOME" != "" ]; then
     CTXROOTMATCH1=$(grep -ocw "WpsContextRoot=$CONTEXT_ROOT" /opt/HCL/wp_profile/ConfigEngine/properties/wkplc.properties)
     CTXROOTMATCH2=$(grep -ocw "WpsDefaultHome=$DEFAULT_HOME\|WpsPersonalizedHome=$PERSONALISED_HOME" /opt/HCL/wp_profile/ConfigEngine/properties/wkplc_comp.properties)
     CTXROOTMATCHCOUNT=$(( CTXROOTMATCH1 + CTXROOTMATCH2 ))
     if [[ $CTXROOTMATCHCOUNT -ge 3 ]] ; then
          echo "No changes in modify path."
     else
          echo "Run modifyContextRoot"
          sed -i -r "s/WpsContextRoot=(\w+)/WpsContextRoot=$CONTEXT_ROOT/g" /opt/HCL/wp_profile/ConfigEngine/properties/wkplc.properties
          sed -i -r "s/WpsDefaultHome=(\w+)/WpsDefaultHome=$DEFAULT_HOME/g;s/WpsPersonalizedHome=(\w+)/WpsPersonalizedHome=$PERSONALISED_HOME/g" /opt/HCL/wp_profile/ConfigEngine/properties/wkplc_comp.properties
     fi
else
     echo "Updating context is not initiated."
fi

/opt/HCL/wp_profile/ConfigEngine/ConfigEngine.sh  -DSaveParentProperties=true -DparentProperties=/tmp/dx-onpremise/scripts/cluster/properties/additional-node/AddSecondaryNode.properties -DWasUserid=$was_username -DWasPassword=$was_password cluster-node-config-cluster-setup-additional -DWasPassword=$was_password

/opt/HCL/wp_profile/ConfigEngine/ConfigEngine.sh  -DWasPassword=$was_password start-portal-server -DWasPassword=$was_password
