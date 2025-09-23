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

if test -f "/tmp/dx-onpremise/properties/wkplc_dbtype.properties"; then
    sed -i "s/^derby.DbLibrary=.*/derby.DbLibrary=\/opt\/IBM\/WebSphere\/AppServer\/derby\/lib\/derby.jar/" /tmp/dx-onpremise/properties/wkplc_dbtype.properties
    sed -i "s/^db2.DbLibrary=.*/db2.DbLibrary=\/opt\/IBM\/WebSphere\/wp_profile\/db2home\/dbjars\/db2jcc4.jar:\/opt\/IBM\/WebSphere\/wp_profile\/db2home\/dbjars\/db2jcc_license_cu.jar/" /tmp/dx-onpremise/properties/wkplc_dbtype.properties
else
    echo "Error: Property file ('wkplc_dbtype.properties') not found for DB2"
    exit 1
fi
