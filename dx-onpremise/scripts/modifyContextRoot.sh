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

DX_USERNAME=$1
DX_PASSWORD=$2
PERSONALISED_HOME=$3
CONTEXT_ROOT=$4
DEFAULT_HOME=$5

/opt/HCL/wp_profile/bin/stopServer.sh WebSphere_Portal -username $DX_USERNAME -password $DX_PASSWORD

if [ "$PERSONALISED_HOME" != "" ]; then
     CTXROOTMATCH1=$(grep -ocw "WpsContextRoot=$CONTEXT_ROOT" /opt/HCL/wp_profile/ConfigEngine/properties/wkplc.properties)
     CTXROOTMATCH2=$(grep -ocw "WpsDefaultHome=$DEFAULT_HOME\|WpsPersonalizedHome=$PERSONALISED_HOME" /opt/HCL/wp_profile/ConfigEngine/properties/wkplc_comp.properties)
     CTXROOTMATCHCOUNT=$(( CTXROOTMATCH1 + CTXROOTMATCH2 ))
     if [[ $CTXROOTMATCHCOUNT -ge 3 ]] ; then
          echo "No changes in modify path."
          /opt/HCL/wp_profile/bin/startServer.sh WebSphere_Portal
     else
          echo "Run modifyContextRoot"
          sed -i -r "s/WpsContextRoot=(\w+)/WpsContextRoot=$CONTEXT_ROOT/g" /opt/HCL/wp_profile/ConfigEngine/properties/wkplc.properties
          sed -i -r "s/WpsDefaultHome=(\w+)/WpsDefaultHome=$DEFAULT_HOME/g;s/WpsPersonalizedHome=(\w+)/WpsPersonalizedHome=$PERSONALISED_HOME/g" /opt/HCL/wp_profile/ConfigEngine/properties/wkplc_comp.properties
          /opt/HCL/wp_profile/ConfigEngine/ConfigEngine.sh modify-servlet-path modify-servlet-path-portlets -DWasPassword=$DX_PASSWORD -DPortalAdminPwd=$DX_PASSWORD -DWpsContextRoot=$CONTEXT_ROOT -DWpsDefaultHome=$DEFAULT_HOME -DWpsPersonalizedHome=$PERSONALISED_HOME
     fi
else
     echo "Updating context is not initiated."
fi

if [ -d "/opt/HCL/AppServer/profiles/dmgr01/" ] 
then
    echo 'Restarting the dmgr ...'
    cd /opt/HCL/AppServer/profiles/dmgr01/bin
    sudo ./stopServer.sh dmgr -username ${DX_USERNAME} -password ${DX_PASSWORD}
    sudo ./startServer.sh dmgr
else
    echo "No dmgr found"
fi
