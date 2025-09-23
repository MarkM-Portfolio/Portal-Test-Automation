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

#Rollback binaries
cd /opt/IBM/InstallationManager/eclipse/tools/
./imcl rollback com.ibm.websphere.PORTAL.SERVER.v85 -installationDirectory /opt/IBM/WebSphere/PortalServer/

#Rollback CF
cd /opt/IBM/WebSphere/wp_profile/PortalServer/bin
./rollbackCF.sh -DPortalAdminPwd=wpsadmin -DWasPassword=wpsadmin