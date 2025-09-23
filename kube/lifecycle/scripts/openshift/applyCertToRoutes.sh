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
 
NAMESPACE=$1
DOMAIN=$2

CERTIFICATE="$(awk '{printf "%s\\n", $0}' $DOMAIN.cer)"
KEY="$(awk '{printf "%s\\n", $0}' $DOMAIN.key)"
CABUNDLE=$(awk '{printf "%s\\n", $0}' fullchain.cer)

ROUTES=$(oc get routes -o=jsonpath='{range .items[*]}{.metadata.name}{"|"}{.spec.tls.termination}{" "}{end}' -n $NAMESPACE)
for ROUTESPEC in $ROUTES ; do
    ROUTE=$(echo $ROUTESPEC | cut -d'|' -f1)
    ROUTETYPE=$(echo $ROUTESPEC | cut -d'|' -f2)
    if [[ "$ROUTETYPE" != "passthrough" ]]; then
        oc patch "route/$ROUTE" -n $NAMESPACE -p '{"spec":{"tls":{"certificate":"'"${CERTIFICATE}"'","key":"'"${KEY}"'","caCertificate":"'"${CABUNDLE}"'"}}}'
    fi
done
