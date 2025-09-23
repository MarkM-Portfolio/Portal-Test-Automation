# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2022. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

# Creating DMGR profile in primary node
host_name=$1
was_username=$2
was_password=$3
domain_suffix=$4
dmgr_username=wpsadmin
dmgr_password=wpsadmin

yum install -y psmisc

/opt/IBM/WebSphere/AppServer/bin/manageprofiles.sh  -create -hostName ${host_name}${domain_suffix} -adminUserName $was_username -adminPassword $was_password -enableAdminSecurity true -cellName dmgrCell01 -nodeName dmgrNode01 -profileName dmgr01 -templatePath /opt/IBM/WebSphere/AppServer/profileTemplates/management -profilePath /opt/IBM/WebSphere/AppServer/profiles/dmgr01
/opt/IBM/WebSphere/AppServer/profiles/dmgr01/bin/startManager.sh
/opt/IBM/WebSphere/AppServer/bin/manageprofiles.sh  -augment -templatePath /opt/IBM/WebSphere/PortalServer/profileTemplates/management.portal.augment -profileName dmgr01
/opt/IBM/WebSphere/AppServer/profiles/dmgr01/bin/stopManager.sh  -username $dmgr_username -password $dmgr_password
/opt/IBM/WebSphere/AppServer/profiles/dmgr01/bin/startManager.sh
