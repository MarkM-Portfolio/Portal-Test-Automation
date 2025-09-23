/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2021. All Rights Reserved.       *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */
 
/* *******************************
 * THIS FILE IS NOT USED YET !!! *
 * *******************************
 */

import hudson.node_monitors.*
import hudson.slaves.*
import java.util.concurrent.*

jenkins = Jenkins.instance

import javax.mail.internet.*;
import javax.mail.*
import javax.activation.*


def sendMail (agent, cause) {

 message = agent + " agent is down. Check http://JENKINS_HOSTNAME:JENKINS_PORT/computer/" + agent + "\nBecause " + cause
 subject = agent + " agent is offline"
 toAddress = "JENKINS_ADMIN@YOUR_DOMAIN"
 fromAddress = "JENKINS@YOUR_DOMAIN"
 host = "SMTP_SERVER"
 port = "SMTP_PORT"

 Properties mprops = new Properties();
 mprops.setProperty("mail.transport.protocol","smtp");
 mprops.setProperty("mail.host",host);
 mprops.setProperty("mail.smtp.port",port);

 Session lSession = Session.getDefaultInstance(mprops,null);
 MimeMessage msg = new MimeMessage(lSession);


 //tokenize out the recipients in case they came in as a list
 StringTokenizer tok = new StringTokenizer(toAddress,";");
 ArrayList emailTos = new ArrayList();
 while(tok.hasMoreElements()) {
   emailTos.add(new InternetAddress(tok.nextElement().toString()));
 }
 InternetAddress[] to = new InternetAddress[emailTos.size()];
 to = (InternetAddress[]) emailTos.toArray(to);
 msg.setRecipients(MimeMessage.RecipientType.TO,to);
 InternetAddress fromAddr = new InternetAddress(fromAddress);
 msg.setFrom(fromAddr);
 msg.setFrom(new InternetAddress(fromAddress));
 msg.setSubject(subject);
 msg.setText(message)

 Transport transporter = lSession.getTransport("smtp");
 transporter.connect();
 transporter.send(msg);
}


def getEnviron(computer) {
   def env
   def thread = Thread.start("Getting env from ${computer.name}", { env = computer.environment })
   thread.join(2000)
   if (thread.isAlive()) thread.interrupt()
   env
}

def agentAccessible(computer) {
    getEnviron(computer)?.get('PATH') != null
}

def numberOfflineNodes = 0
def numberNodes = 0
for (agent in jenkins.getNodes()) {
   def computer = agent.computer
   numberNodes ++
   println ""
   println "Checking computer ${computer.name}:"
   def isOK = (agentAccessible(computer) && !computer.offline)
   if (isOK) {
     println "\t\tOK, got PATH back from agent ${computer.name}."
     println('\tcomputer.isOffline: ' + computer.isOffline());
     println('\tcomputer.isTemporarilyOffline: ' + computer.isTemporarilyOffline());
     println('\tcomputer.getOfflineCause: ' + computer.getOfflineCause());
     println('\tcomputer.offline: ' + computer.offline);
   } else {
     numberOfflineNodes ++
     println "  ERROR: can't get PATH from agent ${computer.name}."
     println('\tcomputer.isOffline: ' + computer.isOffline());
     println('\tcomputer.isTemporarilyOffline: ' + computer.isTemporarilyOffline());
     println('\tcomputer.getOfflineCause: ' + computer.getOfflineCause());
     println('\tcomputer.offline: ' + computer.offline);
     sendMail(computer.name, computer.getOfflineCause().toString())
     if (computer.isTemporarilyOffline()) {
       if (!computer.getOfflineCause().toString().contains("Disconnected by")) {
         computer.setTemporarilyOffline(false, agent.getComputer().getOfflineCause())
       }
     } else {
         computer.connect(true)
     }
   }
 }
println ("Number of Offline Nodes: " + numberOfflineNodes)
println ("Number of Nodes: " + numberNodes)