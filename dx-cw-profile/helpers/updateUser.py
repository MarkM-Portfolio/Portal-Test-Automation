#/*
#********************************************************************
#* Licensed Materials - Property of HCL                             *
#*                                                                  *
#* Copyright HCL Technologies Ltd. 2023. All Rights Reserved.       *
#*                                                                  *
#* Note to US Government Users Restricted Rights:                   *
#*                                                                  *
#* Use, duplication or disclosure restricted by GSA ADP Schedule    *
#********************************************************************
#*/

import sys
import os
scriptName = 'updateUser'

#-------------------------------------------------------------------------------
# Name: process()
# Role: Perform the specified action on the applications listed in the file
#-------------------------------------------------------------------------------
def process():

    AdminTask.updateUser ('[-uniqueName uid=wpsadmin,o=defaultWIMFileBasedRealm -password testpassword]')
    AdminConfig.save()
    return 0

#-------------------------------------------------------------------------------
# Name: anonymous
# Role: Script entry point - verify script was executed, not imported
#-------------------------------------------------------------------------------
if __name__ in [ '__main__', 'main' ] :
    #---------------------------------------------------------------------------
    # How many arguments did the user specify on the command line?
    #---------------------------------------------------------------------------
    argc = len( sys.argv )
    returnCode = process()
    os._exit(returnCode)

else :
    print '\nError: Script should be executed, not imported.'
    sys.exit()