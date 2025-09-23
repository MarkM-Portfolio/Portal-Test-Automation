# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2022. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

#!/bin/bash

if [ $1xxx == "xxx" ]; then
	INPUTXML="Export.xml"
else
	INPUTXML=$1;
fi

VIRTUALPORTAL=$2
SERVER_HOST=$3
echo "SE"	
OUTXML=$HOME"/XMLAccess/"$INPUTXML".out"
PORTALADMINID="wpsadmin"
PORTALADMINPW="wpsadmin"

PROFILEROOT="/opt/HCL/wp_profile/"
EXPORTCMD=$PROFILEROOT"PortalServer/bin/xmlaccess.sh"
XMLSAMPLESDIR="/opt/HCL/PortalServer/doc/xml-samples"
HOSTURL="https://$SERVER_HOST/wps/config"

if [ -z "$2" -a "${2+xxx}" == "xxx" ]; then
	HOSTURL=$HOSTURL
else
	HOSTURL=$HOSTURL/$VIRTUALPORTAL
fi

# if the XML task requested exists in the local dir, use that. Otherwise use $XMLSAMPLEDIR as the source
COMPLETEFILE=$XMLSAMPLESDIR/$INPUTXML
if [ -e $INPUTXML ]; then
	echo "Input file will be "$PWD"/"$INPUTXML
else
	INPUTXML=$COMPLETEFILE
	echo "Input file will be" $INPUTFILE
fi

echo "Using for following for the host URL:" $HOSTURL
echo "Hit ENTER if this is OK"
read NOTHING

echo "Attempting command: " $EXPORTCMD -user $PORTALADMINID -password $PORTALADMINPW -url $HOSTURL -in $INPUTXML -out $OUTXML
$EXPORTCMD -user $PORTALADMINID -password $PORTALADMINPW -url $HOSTURL -in $INPUTXML -out $OUTXML
