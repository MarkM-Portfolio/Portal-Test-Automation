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

import hudson.model.Node
import hudson.model.Slave
import jenkins.model.Jenkins

def setNewExecutors = false
def checkLabel = "build_infra"   // label on agent to check for
def minFreeExecutors = 0         // minimum free executors before creating new ones
def maxAdjustExecutors = 60      // maximum excutors which can be created
def diffExecutors = 5            // number of executors to add or remove from actual setting
def freeExecutors = 0            // currently free executors on agent
def defaultExecutors = 20        // default minimum executors for agent
def targetExecutors = 20         // new number of executors to set for adjustment
def maxExecutors = 0             // current maximum if executors defined
def busyExecutors = 0            // current number of executors busy with jobs

pipeline {

    agent {
        label 'sys_util'
    }

    stages {
        /*
         * Check and adjust executors on agent
         */
        stage('Check adjustment') {
            steps {
                script {
                    /* Change default agent label */
                    if (env.AGENT_LABEL) {
                       checkLabel = env.AGENT_LABEL
                    }
                    
                    /* Change minimum free executors before adding more */
                    if (env.MIN_FREE_Executors) {
                       minFreeExecutors = env.MIN_FREE_Executors.toInteger()
                    }
                    
                    /* Change default number of executors for the given label */
                    if (env.DEFAULT_Executors) {
                       defaultExecutors = env.DEFAULT_Executors.toInteger()
                    }
                    
                    /* calculate the number of oversized executors */
                    /* if we see more free executors than this the actual number is reduced again */
                    def oversizedExecutors = minFreeExecutors + diffExecutors

                    /* get node names having the label */
                    def nodes = jenkins.model.Jenkins.get().computers.findAll{ it.node.labelString.contains(checkLabel) }.collect{ it.node.selfLabel.name }
                    def j = Jenkins.getInstanceOrNull();
                    
                    /* get node instance */
                    def aSlave = (Slave) j.getNode(nodes[0])  // here cast is needed

                    maxExecutors = aSlave.getNumExecutors()
                    busyExecutors = aSlave.getComputer().countBusy()

                    /* only check if agent is online */
                    if (!aSlave.getComputer().isOffline()) {
                       freeExecutors = maxExecutors - busyExecutors
                       /* increase number of executors if limit has reached */ 
                       if (freeExecutors <= minFreeExecutors) {
                          /* don't extend agent with more than maxAdjustExecutors */
                          if (maxExecutors < maxAdjustExecutors) {
                             targetExecutors = maxExecutors + diffExecutors
                             setNewExecutors = true
                          }
                       }
                       if (!setNewExecutors) {
                          /* reduce executors if we see wasted capacity */
                          if (maxExecutors > defaultExecutors) {
                             if (freeExecutors > oversizedExecutors) {
                                targetExecutors = maxExecutors - diffExecutors
                                setNewExecutors = true
                             }
                          }
                       }
                       /* set new number of executors and apply if necessary */
                       if (setNewExecutors) {
                          aSlave.setNumExecutors(targetExecutors)
                          aSlave.save()
                          j.reload()
                       }
                    }
                }
            }
        }

        /*
         * Print report
         */
        stage('Report') {
            steps {
                script {
                    println "Status executors for label $checkLabel"
                    println "============================================="
                    println "Current max executors:  $maxExecutors"
                    println "Current busy executors: $busyExecutors"
                    println "Results free executors: $freeExecutors"
                    println "Minimum free executors: $minFreeExecutors"
                    if (setNewExecutors) {
                       println "Adjustment needed"
                       println "Set new max executors:  $targetExecutors"
                    } else {
                       println "Nothing to adjust"
                    }
                    reportFile = "executor_report.html"
                    def now = new Date()
                    timestamp = now.format("HH:mm")
                    def lastBuildID = currentBuild.previousBuild.id
                    def lastReport = "cat /var/lib/jenkins/jobs/housekeeping/jobs/adjust_agent_executors/builds/$lastBuildID/htmlreports/Executor_20usage_20build_5finfra/executor_report.html".execute().text
                    def reportData = ""
                    if (lastReport != "") {
                       reportData = lastReport.split('dataPoints:\n')[1]
                       reportData = reportData.split('\n]\n')[0]
                    }
                    dir("${workspace}/jenkins_slaves/reports") {
                       if (lastReport != "") {
                          sh """
                             set +x
                             cp report_header.html "$reportFile"
                             echo '$reportData' >> "$reportFile"
                             cat report_footer.html >> "$reportFile"
                          """
                       }
                       sh """
                          sh ./upd-executor_report.sh "$busyExecutors" "$timestamp" "$reportFile"
                       """
                       publishHTML (target : [allowMissing: false,
                                              alwaysLinkToLastBuild: true,
                                              keepAll: true,
                                              reportDir: ".",
                                              reportFiles: "$reportFile",
                                              reportName: "Executor usage $checkLabel"]
                                   )
                       /* publish report once a day into Artifactory */
                       if (timestamp == "09:00") {
                          datestamp = now.format("yyyy-MM-dd")
                          upl_reportFile = datestamp + "_" + reportFile
                          env.REPORT_ARTIFACTORY_PATH = "jenkins/reports"
                          env.ARTIFACTORY_CREDENTIALS = 'artifactory'
                          println "Upload $upl_reportFile"
                          withCredentials([
                              usernamePassword(credentialsId: "${ARTIFACTORY_CREDENTIALS}", passwordVariable: 'ARTIFACTORY_PASSWORD', usernameVariable: 'ARTIFACTORY_USER')
                          ]) {
                              sh """
                                 curl -s -u$ARTIFACTORY_USER:$ARTIFACTORY_PASSWORD -T $reportFile \
                                 https://$G_ARTIFACTORY_HOST/artifactory/$G_ARTIFACTORY_GENERIC_NAME/$REPORT_ARTIFACTORY_PATH/$upl_reportFile
                              """
                          }
                       }
                    }
                }
            }
        }
    }
    
    post {
        cleanup {
            /* Cleanup workspace */
            dir("${workspace}") {
                deleteDir()
            }

            /* Cleanup workspace@tmp */
            dir("${workspace}@tmp") {
                deleteDir()
            }
        }
    }
}
