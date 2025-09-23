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
import com.cloudbees.plugins.credentials.impl.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.jenkins.plugins.sshcredentials.impl.*
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl
import hudson.util.Secret


def addToCredStore(String credsfile) {
   def inFile = new File(credsfile)
   def credType = ""
   def credId = ""
   def credDescription = ""
   def credUser = ""
   def credPass = ""
   def lne = 0

   inFile.eachLine { inline ->
      if (inline.startsWith("<cred-def ")) {
         credType = inline.substring(10)
         println "Adding ${credType}"
         lne = 1
      }
      if (inline == "/>") {
         if (credType == "BasicSSHUserPrivateKey") {
            def source = new BasicSSHUserPrivateKey.DirectEntryPrivateKeySource(credPass)
            Credentials c = (Credentials) new BasicSSHUserPrivateKey(CredentialsScope.GLOBAL, credId, credUser, source, "", credDescription)
            SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), c)
         }
         if (credType == "UsernamePasswordCredentialsImpl") {
            Credentials c = (Credentials) new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, credId, credDescription, credUser, credPass)
            SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), c)
         }
         if (credType == "StringCredentialsImpl") {
            Credentials c = (Credentials) new StringCredentialsImpl(CredentialsScope.GLOBAL, credId, credDescription, Secret.fromString(credPass))
            SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), c)
         }
         
         lne = 0
         credPass = ""
      }
      if (lne == 2) {
         credId = inline
      }
      if (lne == 3) {
         credDescription = inline
      }
      if (lne > 3) {
         if (credType == "BasicSSHUserPrivateKey") {
            if (lne == 4) {
               credUser = inline
            }
            if (lne > 4) {
               credPass = credPass + inline + "\n"
            }
         }
         if (credType == "UsernamePasswordCredentialsImpl") {
            if (lne == 4) {
               credUser = inline
            }
            if (lne == 5) {
               credPass = inline
            }
         }
         if (credType == "StringCredentialsImpl") {
            if (lne == 4) {
               credPass = inline
            }
         }
      }
      lne += 1   
   }
}


String default_credsfile = "/tmp/install_jenkins/default-creds.txt"
String migrate_credsfile = "/tmp/install_jenkins/migrate-creds.txt"

addToCredStore(default_credsfile)
addToCredStore(migrate_credsfile)

def instance = Jenkins.getInstance()
def hudsonRealm = new HudsonPrivateSecurityRealm(false)
hudsonRealm.createAccount("reguser","regUser")
hudsonRealm.createAccount("privuser","privUser")
hudsonRealm.createAccount("adminuser","adminUser")
instance.setSecurityRealm(hudsonRealm)

def strategy = new GlobalMatrixAuthorizationStrategy()

strategy.add(Jenkins.ADMINISTER, "admin")
strategy.add(hudson.model.Hudson.READ,'anonymous')
strategy.add(hudson.model.Item.DISCOVER,'anonymous')

strategy.add(hudson.model.Hudson.READ,'authenticated')
strategy.add(hudson.model.Item.BUILD,'authenticated')
strategy.add(hudson.model.Item.CANCEL,'authenticated')
strategy.add(hudson.model.Item.CONFIGURE,'authenticated')
strategy.add(hudson.model.Item.CREATE,'authenticated')
strategy.add(hudson.model.Item.READ,'authenticated')
strategy.add(hudson.model.Run.DELETE,'authenticated')
strategy.add(hudson.model.Run.UPDATE,'authenticated')
strategy.add(hudson.model.View.READ,'authenticated')
strategy.add(hudson.scm.SCM.TAG,'authenticated')
/* not working as expected yet, needs more investigation
strategy.add(org.jenkins.plugins.lockableresources.LockableResourcesManager.VIEW,'authenticated')
*/

strategy.add(hudson.model.Hudson.READ,'privUser')
strategy.add(com.cloudbees.plugins.credentials.CredentialsProvider.CREATE,'privUser')
strategy.add(com.cloudbees.plugins.credentials.CredentialsProvider.VIEW,'privUser')
strategy.add(hudson.model.Item.BUILD,'privUser')
strategy.add(hudson.model.Item.CANCEL,'privUser')
strategy.add(hudson.model.Item.CONFIGURE,'privUser')
strategy.add(hudson.model.Item.CREATE,'privUser')
strategy.add(hudson.model.Item.READ,'privUser')
strategy.add(hudson.model.Run.DELETE,'privUser')
strategy.add(hudson.model.Run.UPDATE,'privUser')
strategy.add(hudson.model.View.READ,'privUser')
strategy.add(hudson.scm.SCM.TAG,'privUser')

strategy.add(hudson.model.Hudson.READ,'adminUser')
strategy.add(com.cloudbees.plugins.credentials.CredentialsProvider.CREATE,'adminUser')
strategy.add(com.cloudbees.plugins.credentials.CredentialsProvider.VIEW,'adminUser')
strategy.add(hudson.model.Computer.BUILD,'adminUser')
strategy.add(hudson.model.Computer.CONFIGURE,'adminUser')
strategy.add(com.cloudbees.plugins.credentials.CredentialsProvider.CREATE,'adminUser')
strategy.add(com.cloudbees.plugins.credentials.CredentialsProvider.DELETE,'adminUser')
strategy.add(com.cloudbees.plugins.credentials.CredentialsProvider.UPDATE,'adminUser')
strategy.add(com.cloudbees.plugins.credentials.CredentialsProvider.VIEW,'adminUser')
strategy.add(hudson.model.Item.BUILD,'adminUser')
strategy.add(hudson.model.Item.CANCEL,'adminUser')
strategy.add(hudson.model.Item.CONFIGURE,'adminUser')
strategy.add(hudson.model.Item.CREATE,'adminUser')
strategy.add(hudson.model.Item.READ,'adminUser')
strategy.add(hudson.model.Run.DELETE,'adminUser')
strategy.add(hudson.model.Run.UPDATE,'adminUser')
strategy.add(hudson.model.View.READ,'adminUser')
strategy.add(hudson.scm.SCM.TAG,'adminUser')

instance.setAuthorizationStrategy(strategy)
instance.save()
