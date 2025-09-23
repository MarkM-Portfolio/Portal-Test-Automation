
#!/bin/sh
#
####################################################################
# Licensed Materials - Property of HCL                             #
#                                                                  #
# Copyright HCL Technologies Ltd. 2024. All Rights Reserved. #
#                                                                  #
# Note to US Government Users Restricted Rights:                   #
#                                                                  #
# Use, duplication or disclosure restricted by GSA ADP Schedule    #
####################################################################
#

DX_USERNAME=$1
DX_PASSWORD=$2

echo "Add users to Portal"
/opt/HCL/wp_profile/bin/wsadmin.sh -username ${DX_USERNAME} -password ${DX_PASSWORD} -lang jython -f /home/dx_user/pen-test-config/add-users.py

echo "Enable WAS config settings"
/opt/HCL/wp_profile/bin/wsadmin.sh -username ${DX_USERNAME} -password ${DX_PASSWORD} -lang jython -f /home/dx_user/pen-test-config/enable-was-settings.py