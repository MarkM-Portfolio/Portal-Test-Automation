/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2019, 2021. All Rights Reserved. *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

// Import the shared library with the name configured in Jenkins
@Library("dx-shared-library") _

OFFLINE_MESSAGE="Temporary agent which will be deleted shortly"

int delayBeforeDeleteMinutes
long delayBeforeDeleteSeconds

pipeline { 
    agent {
        label 'build_infra'
    }

    /*
     * Preparing all settings and validating params
     */
    stages {
        stage('Prepare Settings') {
            steps {
                script {
                    // Convert numeric parameters
                    assert DELAY_BEFORE_DELETE_MINUTES.isInteger() : "DELAY_BEFORE_DELETE_MINUTES must be an integer"
                    delayBeforeDeleteMinutes = DELAY_BEFORE_DELETE_MINUTES as Integer
                    delayBeforeDeleteSeconds = delayBeforeDeleteMinutes * 60

                    // Terraform log-level
                    if (!env.TF_LOG) {
                        env.TF_LOG = 'INFO'
                    }

                    // Default parameters
                    if (!env.JENKINS_URL) {
                        env.JENKINS_URL="https://portal-jenkins-test.cwp.pnp-hcl.com"
                    }

                    if (!env.DELETE_JOB_NAME) {
                        env.DELETE_JOB_NAME="housekeeping/job/delete-agent"  // Requires '/job' between each folder level
                    }

                    // Build user
                    INSTANCE_OWNER = dxJenkinsGetJobOwner()
                }
            }
        }
        
        stage('Mark agent as temporarily offline') {
            steps {
                script {
                    // Mark as offline
                    def jsonObject = """
                        {
                            "offlineMessage": "${OFFLINE_MESSAGE}"
                        }
                    """
                    httpRequest(
                        url: "${JENKINS_URL}/computer/${INSTANCE_NAME}/toggleOffline",
                        contentType: 'APPLICATION_FORM',
                        httpMode: 'POST',
                        authentication: "${JENKINS_API_CREDENTIALS_ID}",
                        ignoreSslErrors: true,
                        requestBody: "json=${jsonObject}"
                    )
                }
            }
        }
        
        stage('Schedule deletion job') {
            steps {
                script {
                    // Request agent delete
                    def jsonObject = """
                        {
                            "parameter": [
                                {
                                    "name":"INSTANCE_NAME",
                                    "value":"${INSTANCE_NAME}"
                                },
                                {
                                    "name":"JENKINS_API_CREDENTIALS_ID",
                                    "value":"${JENKINS_API_CREDENTIALS_ID}"
                                }
                            ]
                        }
                    """
                    httpRequest(
                        url: "${JENKINS_URL}/job/${DELETE_JOB_NAME}/build?delay=${delayBeforeDeleteSeconds}sec",
                        contentType: 'APPLICATION_FORM',
                        httpMode: 'POST',
                        authentication: "${JENKINS_API_CREDENTIALS_ID}",
                        ignoreSslErrors: true,
                        requestBody: "json=${jsonObject}"
                    )
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
