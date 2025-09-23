#!/bin/bash

# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2023. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

# This script executes ldapadd cmd to add users to OpenLDAP and restart portal to reflect all changes

# Change directory to /home/centos/
cd /home/centos/open-ldap-setup

# Perform the operations

echo "copy the ldif file into open ldap pod"
kubectl cp test_users.ldif dx-deployment-open-ldap-0:/home/dx_user/ -n dxns
kubectl cp test_cn_users.ldif dx-deployment-open-ldap-0:/home/dx_user/ -n dxns
kubectl cp test_mail_users.ldif dx-deployment-open-ldap-0:/home/dx_user/ -n dxns
kubectl cp accented_user.ldif dx-deployment-open-ldap-0:/home/dx_user/ -n dxns

echo "copy the wkplc.properties file into core pod"
kubectl cp wkplc.properties dx-deployment-core-0:/opt/HCL/wp_profile/ConfigEngine/properties/ -n dxns

echo "execute ldapadd in open ldap"
kubectl exec -n dxns pod/dx-deployment-open-ldap-0 -- bash -c '/var/dx-openldap/bin/./ldapadd -h dx-deployment-open-ldap-0 -p 1389 -f /home/dx_user/test_users.ldif -x -D "cn=dx_user,dc=dx,dc=com" -w "p0rtal4u" -v > /dev/null 2>&1 &'

echo "execute ldapadd in open ldap, for additional (cn, mail based) users"
kubectl exec -n dxns pod/dx-deployment-open-ldap-0 -- bash -c '/var/dx-openldap/bin/./ldapadd -h dx-deployment-open-ldap-0 -p 1389 -f /home/dx_user/test_cn_users.ldif -x -D "cn=dx_user,dc=dx,dc=com" -w "p0rtal4u" -v > /dev/null 2>&1 &'
kubectl exec -n dxns pod/dx-deployment-open-ldap-0 -- bash -c '/var/dx-openldap/bin/./ldapadd -h dx-deployment-open-ldap-0 -p 1389 -f /home/dx_user/test_mail_users.ldif -x -D "cn=dx_user,dc=dx,dc=com" -w "p0rtal4u" -v > /dev/null 2>&1 &'

echo "execute ldapadd in open ldap, to add accented users"
kubectl exec -n dxns pod/dx-deployment-open-ldap-0 -- bash -c '/var/dx-openldap/bin/./ldapadd -h dx-deployment-open-ldap-0 -p 1389 -f /home/dx_user/accented_user.ldif -x -D "cn=dx_user,dc=dx,dc=com" -w "p0rtal4u" -v > /dev/null 2>&1 &'

if [ -f "/home/centos/open-ldap-setup/ldap-data/ldifs/people-service-users.ldif" ]; then
    echo "copy the people-service-users.ldif file into open ldap pod"
    kubectl cp /home/centos/open-ldap-setup/ldap-data/ldifs/people-service-users.ldif dx-deployment-open-ldap-0:/home/dx_user/ -n dxns
    echo "execute ldapadd for people-service users in open ldap"
    kubectl exec -n dxns pod/dx-deployment-open-ldap-0 -- bash -c '/var/dx-openldap/bin/./ldapadd -h dx-deployment-open-ldap-0 -p 1389 -f /home/dx_user/people-service-users.ldif -x -D "cn=dx_user,dc=dx,dc=com" -w "p0rtal4u" -v > /dev/null 2>&1 &'
fi

echo "execute ConfigEngine validate-federated-ldap"
kubectl exec -n dxns pod/dx-deployment-core-0 -- bash -c "/opt/HCL/wp_profile/ConfigEngine/./ConfigEngine.sh validate-federated-ldap"

echo "execute ConfigEngine wp-create-ldap"
kubectl exec -n dxns pod/dx-deployment-core-0 -- bash -c "/opt/HCL/wp_profile/ConfigEngine/./ConfigEngine.sh wp-create-ldap"

echo "execute ConfigEngine reregister-scheduler-tasks"
kubectl exec -n dxns pod/dx-deployment-core-0 -- bash -c "/opt/HCL/wp_profile/ConfigEngine/./ConfigEngine.sh reregister-scheduler-tasks"

echo "execute ConfigEngine wp-set-entitytypes"
kubectl exec -n dxns pod/dx-deployment-core-0 -- bash -c "/opt/HCL/wp_profile/ConfigEngine/./ConfigEngine.sh wp-set-entitytypes"

echo "execute ConfigEngine wp-update-federated-ldap-attribute-config"
kubectl exec -n dxns pod/dx-deployment-core-0 -- bash -c "/opt/HCL/wp_profile/ConfigEngine/./ConfigEngine.sh wp-update-federated-ldap-attribute-config"

# update use.db.cache.invalidation.table to false for wcm rendering
echo "set false on use.db.cache.invalidation.table"
kubectl exec -n dxns pod/dx-deployment-core-0 -- bash -c "sed -i 's/name=\"use.db.cache.invalidation.table\" value=\"true\"/name=\"use.db.cache.invalidation.table\" value=\"false\"/' /opt/HCL/wp_profile/config/cells/dockerCell/nodes/dockerNode/servers/WebSphere_Portal/resources.xml"

echo "execute ConfigEngine stop-portal-server"
kubectl exec -n dxns pod/dx-deployment-core-0 -- bash -c "/opt/HCL/wp_profile/ConfigEngine/./ConfigEngine.sh stop-portal-server"

echo "execute ConfigEngine start-portal-server"
kubectl exec -n dxns pod/dx-deployment-core-0 -- bash -c "/opt/HCL/wp_profile/ConfigEngine/./ConfigEngine.sh start-portal-server -username wpsadmin -password wpsadmin"