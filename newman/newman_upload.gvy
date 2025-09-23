/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2024. All Rights Reserved.       *
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

// test map for creating a new EC2 instance
def terraformVarsEC2 = [:]

// Create object to store parameters with values
def pipelineParameters = [:]
def deploymentTargetBranch
def deploymentTargetRepo
def REPORT_BUILD_NAME

pipeline {
  // Runs in build_infra, since we are creating infrastructure
   agent {
        label 'build_infra'
   }
   
   stages {
      // Load the pipeline parameters, common modules and configuration
        stage('Load parameters and configuration') {
            steps {
                dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/newman/parameters.yaml")
                script {
                    env.EXP_API = "https://${pipelineParameters.INSTANCE_NAME}${pipelineParameters.DOMAIN_SUFFIX}/dx/api/core/v1/"
                    env.DAM_API = "https://${pipelineParameters.INSTANCE_NAME}${pipelineParameters.DOMAIN_SUFFIX}/dx/api/dam/v1/"

                    pipelineParameters.buildUser = dxJenkinsGetJobOwner()
                    println("Instance owner will be > ${pipelineParameters.buildUser} <.")
                    println("BUILD_URL > ${env.BUILD_URL} <.")

                    terraformVarsEC2.tfvar_instance_name = pipelineParameters.AWS_INSTANCE_NAME
                    terraformVarsEC2.tfvar_aws_ec2_instance_type = pipelineParameters.AWS_INSTANCE_TYPE
                    terraformVarsEC2.tfvar_instance_owner = pipelineParameters.buildUser
                    terraformVarsEC2.tfvar_instance_popo_schedule = pipelineParameters.POPO_SCHEDULE

                    if (pipelineParameters.DEPLOYMENT_TARGET_BRANCH.contains('release') ) {
                        deploymentTargetBranch = 'release'
                        deploymentTargetRepo = 'quintana-docker-prod'
                    } else {
                        deploymentTargetBranch = 'develop'
                        deploymentTargetRepo = 'quintana-docker'
                    }

                    // determine build version and label current job accordingly
                    def dateFormat = new SimpleDateFormat('yyyyMMdd-HHmmssSSS')
                    def date = new Date()

                    // Adjust display name of current run
                    def currentDate = "${dateFormat.format(date)}"
                    currentBuild.displayName = "${pipelineParameters.INSTANCE_NAME}_${currentDate}"
                    def removeHTTPFromDisplayName = currentBuild.displayName.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)","")
                    def tempReportBuildName = removeHTTPFromDisplayName.split('/')
                    tempReportBuildName = tempReportBuildName[0].replace(':','')
                    if (removeHTTPFromDisplayName.contains('/')) {
                        tempReportBuildName = "${tempReportBuildName}_${currentDate}"
                        tempReportBuildName = tempReportBuildName.replace(':','')
                    }
                    REPORT_BUILD_NAME = tempReportBuildName
                    println("Report build name is > ${REPORT_BUILD_NAME} <.")

                }
            }
        }

        // Terraform install
        stage('Install Terraform') {
            steps {
                script {
                    dxTerraformInstall()
                    echo "Terraform Installation done"
                }
            }
        }

        // Launch the EC2 instance with our target parameters
        stage('Create EC2 Instance') {
            steps {
                script {
                    try {
                        terraformVarsEC2 = dxTerraformCreateEc2Instance(terraformVarsEC2)
                        // extract private ip, dns and id of created instance
                        def instanceIp = terraformVarsEC2.instance_private_ip
                        def instanceId = terraformVarsEC2.instance_id
                        echo "Instance ${instanceId} running on ${instanceIp}."
                        env.INSTANCE_IP = instanceIp
                        env.INSTANCE_ID = instanceId
                    } catch (err) {
                        error('ERROR: Creating EC2 instance failed.')
                    }
                        println 'terraformVarsEC2 = ' + terraformVarsEC2
                        println 'Test OK'
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
                        scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/newman/scripts/install-prereqs.sh centos@${terraformVarsEC2.instance_private_ip}:/tmp
                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'chmod +x /tmp/install-prereqs.sh && sh /tmp/install-prereqs.sh'
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
                            sh """
                                chmod 600 ${DEPLOY_KEY}
                                git clone -b "develop" git@git.cwp.pnp-hcl.com:Team-Q/Portal-Test-Automation.git ${workspace}/Portal-Test-Automation
                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sudo mkdir -p /opt/Portal-Test-Automation && sudo chown centos: /opt/Portal-Test-Automation'
                                scp -r -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/Portal-Test-Automation centos@${terraformVarsEC2.instance_private_ip}:/opt
                            """
                        }
                    }
                }
            }
        }
        stage('Upload DAM Data through newman testing') {
                options {
                    timeout(time: 30, unit: 'MINUTES') 
                }
                steps {
                    configFileProvider([
                        configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                    ]) {
                        script {
                            Exception caughtException = null;
                            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                                try {
                                    echo "Running tests"
                                    echo "DAM API will be: ${env.DAM_API}"
                                    echo "EXIM API will be: ${env.EXP_API}"
                                    echo "INSTANCE_IP will be: ${terraformVarsEC2.instance_private_ip}"
                                    
                                    sh """
                                        chmod 600 ${DEPLOY_KEY}
                                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Test-Automation/newman && sed -i "s|http://localhost:10039|https://${pipelineParameters.INSTANCE_NAME}${pipelineParameters.DOMAIN_SUFFIX}|g" /opt/Portal-Test-Automation/newman/localhost_DAM.postman_environment.json && npx newman run DAM_Upload.postman_collection.json -e localhost_DAM.postman_environment.json --export-environment exported_environment.json'
                                    """
                                    sh """
                                        scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip}:/opt/Portal-Test-Automation/newman/exported_environment.json ${env.WORKSPACE}/exported_environment.json
                                    """
                                    /* Stash the env.json file */
                                    stash includes: 'exported_environment.json', name: 'jsonStash'
                                    // Archiving the file as an artifact and making it accessible for download
                                    archiveArtifacts artifacts: 'exported_environment.json', allowEmptyArchive: true
                                    // archiveArtifacts 'exported_environment.json'
                                } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
                                    error "TIMEOUT ${e.toString()}"
                                } catch (Throwable e) {
                                    caughtException = e;
                                }
                                if (caughtException) {
                                    error caughtException.message
                                }
                            }
                        }
                    }
                }
            }   
        stage('Upload CC/WCM Data through newman testing') {
                options {
                    timeout(time: 30, unit: 'MINUTES') 
                }
                steps {
                    configFileProvider([
                        configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                    ]) {
                        script {
                            Exception caughtException = null;
                            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                                try {
                                    echo "Running tests for the new stage"
                                    echo "DAM API will be: ${env.NEW_DAM_API}"
                                    echo "EXIM API will be: ${env.NEW_EXP_API}"
                                    echo "INSTANCE_IP will be: ${terraformVarsEC2.instance_private_ip}"
                                    
                                    sh """
                                        chmod 600 ${DEPLOY_KEY}
                                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} "cd /opt/Portal-Test-Automation/newman && sed -i -e 's|\\b\\(wps\\)\\b|${pipelineParameters.CONTEXT_ROOT_PATH}|g' -e 's|http://localhost:10039|https://${pipelineParameters.INSTANCE_NAME}${pipelineParameters.DOMAIN_SUFFIX}|g' -e 's|portal|${pipelineParameters.DX_CORE_HOME_PATH}|g' localhost_API.postman_environment.json && npx newman run wcmrest_categories_upload.postman_collection.json -e localhost_API.postman_environment.json --export-environment exported_WCM_environment.json"
                                    """
                                    sh """
                                        scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip}:/opt/Portal-Test-Automation/newman/exported_WCM_environment.json ${env.WORKSPACE}/exported_WCM_environment.json
                                    """
                                    stash includes: 'exported_WCM_environment.json', name: 'jsonStash'
                                    // Archiving the file as an artifact and making it accessible for download
                                    archiveArtifacts artifacts: 'exported_WCM_environment.json', allowEmptyArchive: true
                                    // archiveArtifacts 'exported_WCM_environment.json'
                                } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
                                    error "TIMEOUT ${e.toString()}"
                                } catch (Throwable e) {
                                    caughtException = e;
                                }
                                if (caughtException) {
                                    error caughtException.message
                                }
                            }
                        }
                    }
                }
            }

        stage('Upload PZN Data through newman testing') {
                options {
                    timeout(time: 30, unit: 'MINUTES') 
                }
                steps {
                    configFileProvider([
                        configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                    ]) {
                        script {
                            Exception caughtException = null;
                            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                                try {
                                    echo "Running tests for the new stage"
                                    echo "DAM API will be: ${env.NEW_DAM_API}"
                                    echo "EXIM API will be: ${env.NEW_EXP_API}"
                                    echo "INSTANCE_IP will be: ${terraformVarsEC2.instance_private_ip}"
                                    
                                    sh """
                                        chmod 600 ${DEPLOY_KEY}
                                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} "cd /opt/Portal-Test-Automation/newman && sed -i -e 's|\\b\\(wps\\)\\b|${pipelineParameters.CONTEXT_ROOT_PATH}|g' -e 's|http://localhost:10039|https://${pipelineParameters.INSTANCE_NAME}${pipelineParameters.DOMAIN_SUFFIX}|g' -e 's|portal|${pipelineParameters.DX_CORE_HOME_PATH}|g' localhost_API.postman_environment.json && npx newman run PZN_upload.postman_collection.json -e localhost_API.postman_environment.json --export-environment exported_PZN_environment.json"
                                    """
                                    sh """
                                        scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip}:/opt/Portal-Test-Automation/newman/exported_PZN_environment.json ${env.WORKSPACE}/exported_PZN_environment.json
                                    """
                                    stash includes: 'exported_PZN_environment.json', name: 'jsonStash'
                                    // Archiving the file as an artifact and making it accessible for download
                                    archiveArtifacts artifacts: 'exported_PZN_environment.json', allowEmptyArchive: true
                                    // archiveArtifacts 'exported_PZN_environment.json'
                                } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
                                    error "TIMEOUT ${e.toString()}"
                                } catch (Throwable e) {
                                    caughtException = e;
                                }
                                if (caughtException) {
                                    error caughtException.message
                                }
                            }
                        }
                    }
                }
            }    
   }

    post {
        cleanup {
            //Cleanup workspace
            dxTerraformDestroyEc2Instance(terraformVarsEC2)
            dxWorkspaceDirectoriesCleanup()
        }
    }
}