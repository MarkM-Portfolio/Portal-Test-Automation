#!groovy
/**
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2001, 2020. All Rights Reserved. *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

import jenkins.model.*
import jenkins.install.*
import hudson.util.*
import hudson.security.*

def userName = 'a-d-m-i-n'
def instance = Jenkins.getInstance()

println "--> creating local user '" + userName + "'"

def hudsonRealm = new HudsonPrivateSecurityRealm(false)
hudsonRealm.createAccount(userName,userName)
instance.setSecurityRealm(hudsonRealm)

def strategy = new FullControlOnceLoggedInAuthorizationStrategy()

instance.setAuthorizationStrategy(strategy)

instance.setNumExecutors(0)

instance.setInstallState(InstallState.INITIAL_SETUP_COMPLETED)

instance.save()
