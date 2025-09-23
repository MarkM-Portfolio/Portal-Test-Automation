/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2023. All Rights Reserved.       *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
  */

// Import the shared library with the name configured in Jenkins
@Library("dx-shared-library") _

// Using SimpleDateFormat for versioning the name of executions
import java.text.SimpleDateFormat

// Create object to store parameters with values
def pipelineParameters = [:]

// EC2 instance variables
def terraformVarsEC2 = [:]
terraformVarsEC2.ec2_ssh_user = 'centos'

// Check if mapString is map syntax
// e.g. [key1: "value1", key2: "value2"]
def isMapStringOK(mapString) {
    def mapOK = false
    // Check brackets
    if (mapString.startsWith('[') && mapString.endsWith(']')) {
        // Remove all blanks and brackets, then split
        mapArray = mapString.replace(" ", "").replace("[", "").replace("]", "").split(",")
        keysOK = 0
        mapArray.each {
            // Check for correct key:value pair syntax
            if (it.contains(':"') && it.endsWith('"')) {
                ++keysOK
            }
        }
        // Check if only valid key:value pairs found
        if (mapArray.size() == keysOK) {
            mapOK = true
        }
    }
    return mapOK
}

// Get value for a passed key in passed mapString
// The map can define a default value which is returned if the requested key has no individual definition
def getValueFromMapString(mapKey, mapString) {
    def returnValue = ""
    def defaultValue = "" 
    if (mapString.contains("default:")) {
        defaultValue = mapString.split("default:")[1]
        defaultValue = defaultValue.split('"')[1]
    }
    if (mapString.contains("${mapKey}:")) {
        returnValue = mapString.split("${mapKey}:")[1]
        returnValue = returnValue.split('"')[1]
    } else {
        returnValue = defaultValue
    }
    return returnValue
}

pipeline {
    agent {
        label 'build_infra'
    }
    stages {
        stage('Load parameters and settings') {
            steps {
                // This example uses the file called dxParametersLoadFromFile.yaml
                dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/leap-acceptance-tests/leap-acceptance-test-automation.yaml")
                // Determine the job Display name 
                script {
                    if (!env.SSH_USER){
                        env.SSH_USER = 'centos'
                    }
                    // determine build version and label current job accordingly
                    def dateFormat = new SimpleDateFormat('yyyyMMdd-HHmmssSSS')
                    def date = new Date()

                    // Determine hostname for EC2 instance
                    pipelineParameters.environmentHostname = "Leap_Acceptance_Tests_${dateFormat.format(date)}"
                    println("Hostname of EC2 instance that will perform the test execution: > ${pipelineParameters.environmentHostname} <.")

                    // Adjust display name of current run
                    def currentDate = "${dateFormat.format(date)}"
                    currentBuild.displayName = "${pipelineParameters.LEAP_HOST}_${currentDate}"
                    def removeHTTPFromDisplayName = currentBuild.displayName.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)","")
                    def tempReportBuildName = removeHTTPFromDisplayName.split('/')
                    tempReportBuildName = tempReportBuildName[0].replace(':','')
                    if (removeHTTPFromDisplayName.contains('/')) {
                        tempReportBuildName = "${tempReportBuildName}_${currentDate}"
                        tempReportBuildName = tempReportBuildName.replace(':','')
                    }
                    pipelineParameters.reportBuildname = tempReportBuildName
                    println("Report build name is > ${pipelineParameters.reportBuildname} <.")

                    // Determine owner of EC2 instance
                    pipelineParameters.buildUser = dxJenkinsGetJobOwner()
                    println("Instance owner will be > ${pipelineParameters.buildUser} <.")

                    // Check TARGET_BRANCH if passed as map and then create TARGET_BRANCH_MAP
                    if (pipelineParameters.TARGET_BRANCH.startsWith("[")) {
                        pipelineParameters.TARGET_BRANCH_MAP = pipelineParameters.TARGET_BRANCH.replace(" ", "")
                        if (isMapStringOK(pipelineParameters.TARGET_BRANCH_MAP)) {
                            if (getValueFromMapString("default", pipelineParameters.TARGET_BRANCH_MAP) == "") {
                                error("TARGET_BRANCH has been passed as map but doesn't have the mandatory default entry.\nSyntax: [ default: \"develop\", TEST_CC: \"feature/DXQ-12345\" ]")
                            }
                        } else {
                            error("TARGET_BRANCH has been passed as map but the syntax is not correct.\nExample: [ default: \"develop\", key1: \"value1\", key2: \"value2\", ... ]")
                        }
                    }
                }
            }
        }

        /*
         * Basic health check for the environment under test, allows us to easily stop execution before running actual tests.
         */
        stage('Environment healthcheck') {
            steps {
                println("Checking health for environment > ${pipelineParameters.LEAP_HOST} <.")
                script {
                    // Try to check if the instance is up, return error if it fails
                    try {
                        sh "curl -s -L -k -i -f ${pipelineParameters.LEAP_HOST} > ${env.WORKSPACE}/healthcheck-response.txt"
                    } catch (Throwable e) {
                        def failReason = ''
                        if (e.message.contains('exit code 6')) {
                            failReason = 'Could not resolve host.'
                        }
                        if (e.message.contains('exit code 7')) {
                            failReason = 'Failed to connect() to host or proxy.'
                        }
                        if (e.message.contains('exit code 22')) {
                            failReason = 'HTTP server returns an error code that is >= 400.'
                        }
                        if (e.message.contains('exit code 35')) {
                            failReason = 'HTTP does not properly talk HTTPS.'
                        }
                        // echo the HTTP response, since we have an error and it might appear interesting
                        sh "cat ${env.WORKSPACE}/healthcheck-response.txt"
                        error("Environment > ${pipelineParameters.LEAP_HOST} < is not healthy, reason: ${failReason}")
                    }
                }
            }
        }

       

        /*
         * Prepare terraform with custom config
         */
        stage('Prepare Terraform') {
            steps {
                dxTerraformInstall()
                dxTerraformCustomConfig(source: 'leap-acceptance-tests/terraform/ec2-launch')
            }
        }

        /*
         * Create EC2 instance
         */
        stage('Create EC2 Instance') {
            // Setting the TF variables to overwrite specific EC2 instance settings
            environment {
                // Expiration stamp taken from the TTL value
                TF_VAR_EXPIRATION_STAMP = "${pipelineParameters.ttl}"
                // Environment name passed into TF files
                TF_VAR_INSTANCE_NAME = "${pipelineParameters.environmentHostname}"
                // Environment owner passed into TF files
                TF_VAR_INSTANCE_OWNER = "${pipelineParameters.buildUser}"
            }
            steps {
                script {
                    terraformVarsEC2.tfvar_instance_name = pipelineParameters.environmentHostname
                    terraformVarsEC2.tfvar_instance_owner = pipelineParameters.buildUser
                    terraformVarsEC2.tfvar_EXPIRATION_STAMP = pipelineParameters.ttl
                    terraformVarsEC2 = dxTerraformCreateEc2Instance(terraformVarsEC2)
                }
            }
        }

        /*
         * After a successful creation of the EC2 instance, we install all required software on it and make sure that our settings
         * will be copied over to the target machine.
         */
        stage('Prepare EC2 instance') {
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    sh """
                        chmod 600 ${DEPLOY_KEY}
                        scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/leap-acceptance-tests/scripts/install-prereqs.sh centos@${terraformVarsEC2.instance_private_ip}:/tmp
                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'chmod +x /tmp/install-prereqs.sh && sh /tmp/install-prereqs.sh'
                        scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/leap-acceptance-tests/scripts/install-docker.sh centos@${terraformVarsEC2.instance_private_ip}:/tmp
                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'chmod +x /tmp/install-docker.sh && sh /tmp/install-docker.sh'
                    """
                }
            }
        }

        /*
         * Once prerequisites are installed, we pull the repositories needed for the tests in the workspace, and scp transfer it to the EC2 instance.
         */
        stage('Pull tests') {
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    sshagent(credentials: ['jenkins-git']) {
                        script {
                            sh "chmod 600 ${DEPLOY_KEY}"
                            sh """
                                git clone -b ${pipelineParameters.TARGET_BRANCH} git@git.cwp.pnp-hcl.com:low-code/nitro-automation.git ${workspace}/nitro-automation
                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sudo mkdir -p /opt/nitro-automation && sudo chown centos: /opt/nitro-automation'
                                scp -r -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/nitro-automation centos@${terraformVarsEC2.instance_private_ip}:/opt
                            """
                        }
                    }
                }
            }
        }

         // Sample Run of Dummy Docker container 
        stage('Run Docker Container Tests') {
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    sh """
                        chmod 600 ${DEPLOY_KEY}
                        scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/leap-acceptance-tests/scripts/run-docker-container.sh centos@${terraformVarsEC2.instance_private_ip}:/tmp
                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'chmod +x /tmp/run-docker-container.sh && sh /tmp/run-docker-container.sh'
                  """
              }
            }
        }

        /*
         * Each application would then install its dependencies and run the acceptance tests. Timeout is currently set at 30 mins per application stage.
         * Timeout is also treated as a failure, and is caught using org.jenkinsci.pligins.workflow.steps.FlowInterruptedException. Otherwise timeouts
         * are going to be registered as ABORTED in jenkins status report. The other catch is for any other errors produced by the test.
         */
        stage('Run Leap acceptance tests') {
            options {
                timeout(time: 30, unit: 'MINUTES')
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        String caughtExceptionMessage = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo "Running tests"
                            } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
                                caughtExceptionMessage = "TIMEOUT ${e.toString()}"
                            } catch (Throwable e) {
                                caughtExceptionMessage = e.message;
                            }
                            if (caughtExceptionMessage) {
                                echo "Caught Exxception"
                            }
                        }
                    }
                }
            }
        }

    }

    /*
     * Perform proper cleanup to leave a healthy jenkins agent. On build success we clean up the EC2 instance. On fails/unstable EC2 is left up and
     * to be terminated manually.
     */
    post {
        cleanup {
            script {
                try {
                    // Expiration stamp taken from the TTL value
                    env.TF_VAR_EXPIRATION_STAMP = "${pipelineParameters.ttl}"
                    // Environment name passed into TF files
                    env.TF_VAR_INSTANCE_NAME = "${pipelineParameters.environmentHostname}"
                    // Environment owner passed into TF files
                    env.TF_VAR_instance_owner = "${pipelineParameters.buildUser}"
                    // terraformVarsEC2.tfvar_instance_name = pipelineParameters.environmentHostname
                    dxTerraformDestroyEc2Instance(terraformVarsEC2)
                } catch (Throwable e) {
                    println('Unable to destroy EC2 instance!')
                }

                /* remove internal instance from known-hosts */
                if (terraformVarsEC2.instance_private_ip) {
                    sh(script: """
                        ssh-keygen -R ${terraformVarsEC2.instance_private_ip} | true
                        rm -f ~/.ssh/known_hosts.old
                    """)
                }

                dxWorkspaceDirectoriesCleanup()
            }
        }
    }
}
