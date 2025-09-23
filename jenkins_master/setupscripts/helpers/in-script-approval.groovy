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
import org.jenkinsci.plugins.scriptsecurity.scripts.*;


String credsfile = "/tmp/install_jenkins/migrate-in-script.txt"
def inFile = new File(credsfile)
def scriptApproval = org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval.get()

inFile.eachLine { inline ->
   scriptApproval.approveSignature(inline)
}

scriptApproval.save()