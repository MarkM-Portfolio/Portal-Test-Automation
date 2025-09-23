#/*
#********************************************************************
#* Licensed Materials - Property of HCL                             *
#*                                                                  *
#* Copyright HCL Technologies Ltd. 2024. All Rights Reserved.       *
#*                                                                  *
#* Note to US Government Users Restricted Rights:                   *
#*                                                                  *
#* Use, duplication or disclosure restricted by GSA ADP Schedule    *
#********************************************************************
#*/

# Add users to the WebSphere Portal server
# This script will add the following users:
# - wpsadmin1
# - wpsadmin2
# - dxpentestuser1
# - dxpentestuser2
# - Add the users to the wpsadmins group
# - Save the changes

def addUser(name, password):
    print AdminTask.createUser('[-uid ' + name + ' -password ' + password + ' -confirmPassword ' + password + ' -cn ' + name + ' -sn ' + name + ']')
    print AdminTask.addMemberToGroup('[-memberUniqueName uid=' + name + ',o=defaultWIMFileBasedRealm -groupUniqueName cn=wpsadmins,o=defaultWIMFileBasedRealm]')

# Add users
addUser('wpsadmin1', 'P3nTest4!DXadmin1')
addUser('wpsadmin2', 'P3nTest4!DXadmin2')
addUser('dxpentestuser1', 'P3nTest4!User1')
addUser('dxpentestuser2', 'P3nTest4!User2')

print AdminConfig.save()