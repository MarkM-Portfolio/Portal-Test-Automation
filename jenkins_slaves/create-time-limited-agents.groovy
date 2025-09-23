/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2001, 2019. All Rights Reserved. *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

// Import the shared library with the name configured in Jenkins
@Library("dx-shared-library") _

JENKINS_URL="https://portal-jenkins-test.cwp.pnp-hcl.com"
CREATE_JOB_NAME="housekeeping/job/create-new-agent"  // Requires '/job' between each folder level
DECOMMISSION_JOB_NAME="housekeeping/job/decommission-agent"  // Requires '/job' between each folder level

def instanceNames
int numberOfInstances
int startInstanceNumber
int agentLongevityHours
long agentLongevitySeconds
int delayBeforeDeleteMinutes

// Set default buildserver label
if (!env.BUILD_SERVER_LABEL){
    env.BUILD_SERVER_LABEL = 'build_infra'
}

pipeline {
    /*
     * Offers the ability to run even on a different buildserver than labeled build_infra
     */
    agent {
        node {
            label "${env.BUILD_SERVER_LABEL}"
        }
    }

    /*
     * Preparing all settings and validating params
     */
    stages {
        stage('Prepare Settings') {
            steps {
                script {
                    // Convert numeric parameters
                    assert START_INSTANCE_NUMBER.isInteger() : "START_INSTANCE_NUMBER must be an integer"
                    startInstanceNumber = START_INSTANCE_NUMBER as Integer

                    assert NUMBER_OF_INSTANCES.isInteger() : "NUMBER_OF_INSTANCES must be an integer"
                    numberOfInstances = NUMBER_OF_INSTANCES as Integer

                    assert AGENT_LONGEVITY_HOURS.isInteger() : "AGENT_LONGEVITY_HOURS must be an integer"
                    agentLongevityHours = AGENT_LONGEVITY_HOURS as Integer
                    agentLongevitySeconds = agentLongevityHours * 3600

                    assert DELAY_BEFORE_DELETE_MINUTES.isInteger() : "DELAY_BEFORE_DELETE_MINUTES must be an integer"
                    delayBeforeDeleteMinutes = DELAY_BEFORE_DELETE_MINUTES as Integer

                    // Terraform log-level
                    if (!env.TF_LOG) {
                        env.TF_LOG = 'INFO'
                    }
                    // Build user
                    INSTANCE_OWNER = dxJenkinsGetJobOwner()
                    // Instances to create
                    instanceNames = new String[numberOfInstances]
                    for (int i = 0; i < numberOfInstances; i++) {
                        instanceNames[i] = String.format("%s%02d_%s", AGENT_CATEGORY, i + startInstanceNumber, AGENT_PURPOSE)
                    }
                }
            }
        }
        
        stage('Start creation and corresponding decommission jobs') {
            steps {
                script {
                    for (int i = 0; i < numberOfInstances; i++) {
                        // Request agent creation
                        def jsonObject = """
                            {
                                "parameter": [
                                    {
                                        "name":"INSTANCE_NAME",
                                        "value":"${instanceNames[i]}"
                                    },
                                    {
                                        "name":"AGENT_DESCRIPTION",
                                        "value":"${AGENT_DESCRIPTION}"
                                    },
                                    {
                                        "name":"AGENT_LABELS",
                                        "value":"${AGENT_LABELS}"
                                    },
                                    {
                                        "name":"NUMBER_OF_EXECUTORS",
                                        "value":"${NUMBER_OF_EXECUTORS}"
                                    },
                                    {
                                        "name":"JENKINS_API_CREDENTIALS_ID",
                                        "value":"${JENKINS_API_CREDENTIALS_ID}"
                                    }
                                ]
                            }
                        """
                        httpRequest(
                            url: "${JENKINS_URL}/job/${CREATE_JOB_NAME}/build",
                            contentType: 'APPLICATION_FORM',
                            httpMode: 'POST',
                            authentication: "${JENKINS_API_CREDENTIALS_ID}",
                            ignoreSslErrors: true,
                            requestBody: "json=${jsonObject}"
                        )

                        // Request agent decommissioning
                        jsonObject = """
                            {
                                "parameter": [
                                    {
                                        "name":"INSTANCE_NAME",
                                        "value":"${instanceNames[i]}"
                                    },
                                    {
                                        "name":"DELAY_BEFORE_DELETE_MINUTES",
                                        "value":"${delayBeforeDeleteMinutes}"
                                    },
                                    {
                                        "name":"JENKINS_API_CREDENTIALS_ID",
                                        "value":"${JENKINS_API_CREDENTIALS_ID}"
                                    }
                                ]
                            }
                        """
                        httpRequest(
                            url: "${JENKINS_URL}/job/${DECOMMISSION_JOB_NAME}/build?delay=${agentLongevitySeconds}sec",
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
