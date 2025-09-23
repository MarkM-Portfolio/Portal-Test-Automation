#!/bin/sh
#
 ####################################################################
 # Licensed Materials - Property of HCL                              #
 #                                                                  #
 # Copyright HCL Technologies Ltd. 2001, 2019. All Rights Reserved. #
 #                                                                  #
 # Note to US Government Users Restricted Rights:                   #
 #                                                                  #
 # Use, duplication or disclosure restricted by GSA ADP Schedule    #
 ####################################################################
 #

if [ XXX$1 != "XXX" ]; then
    NEW_IP=$1
    DNS=$2
    echo "NEW IP : $NEW_IP"
    echo "DNS : $DNS"
fi

cat > update-record-set.json << EOF

{
    "Comment": "Adding latest ip",
    "Changes": [{
        "Action": "UPSERT",
        "ResourceRecordSet": {
            "ResourceRecords":[{ "Value": "$NEW_IP" }],
            "Name": "$DNS",
            "Type": "A",
            "TTL": 300
        }
    }]
}

EOF
