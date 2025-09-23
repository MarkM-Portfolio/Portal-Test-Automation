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

import groovy.time.TimeCategory
import java.text.SimpleDateFormat

@Library('dx-shared-library') _
// test map for creating a new EC2 instance

def pipelineParameters = [:]
def terraformVarsEC2 = [:]

def deploymentTargetBranch
def deploymentTargetRepo
def deploymentSettings
def SERVER_HOST_PUBLISHER
def SERVER_HOST_SUBSCRIBER

pipeline {
    agent {
        label 'build_infra'
    }

    stages {
        stage('Load modules and configuration') {
            steps {
                script {
                    // load params from yaml file
                    dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/dx-dam-staging-tests/two-way-staging-parameters.yaml")
                }
            }
        }

        stage('Prepare Settings') {
            steps {
                /*
                 * Assigning hostname using Performace-test-automation + the timestap
                 */
                dir("${WORKSPACE}/dx-dam-staging-tests") {
                    script {
                        // determine build version and label current job accordingly
                        def dateFormat = new SimpleDateFormat('yyyyMMdd-HHmm')
                        def date = new Date()

                        // using namespace to differentiate publisher and subscriber environments
                        SERVER_HOST_PUBLISHER = "${pipelineParameters.NAMESPACE}-sync-publisher${pipelineParameters.DOMAIN_SUFFIX}"
                        SERVER_HOST_SUBSCRIBER = "${pipelineParameters.NAMESPACE}-sync-subscriber${pipelineParameters.DOMAIN_SUFFIX}"

                        echo 'Assigning hostname + timestamp'
                        terraformVarsEC2.tfvar_instance_name = "DAM-Staging-long-run_${dateFormat.format(date)}"
                        echo "New instance will be: ${terraformVarsEC2.tfvar_instance_name}"

                        INSTANCE_OWNER = dxJenkinsGetJobOwner()
                        terraformVarsEC2.tfvar_instance_owner = "${INSTANCE_OWNER}"

                        // Display name includes the INSTANCE_NAME and a timestamp
                        currentBuild.displayName = terraformVarsEC2.tfvar_instance_name

                        // get target repo for artifactory
                        if (pipelineParameters.TARGET_BRANCH.contains('release')) {
                            deploymentTargetBranch = 'release'
                            deploymentTargetRepo = 'quintana-docker-prod'
                        } else {
                            deploymentTargetBranch = 'develop'
                            deploymentTargetRepo = 'quintana-docker'
                        }
                    }
                }
            }
        }

          stage('Undeploying the publisher application in k8 environment') {
            steps {
                script {
                    buildParameters = []
                    buildParameters.add(string(name: 'INSTANCE_NAME', value: "${pipelineParameters.NAMESPACE}-sync-publisher"))
                    buildParameters.add(string(name: 'KUBE_FLAVOUR', value: pipelineParameters.KUBE_FLAVOUR))
                    build(job: "${pipelineParameters.KUBE_UNDEPLOY_JOB}",
                          parameters: buildParameters,
                          propagate: true,
                          wait: true)
                }
            }
        }

        stage('Undeploying the subscriber application in k8 environment') {
            steps {
                script {
                    buildParameters = []
                    buildParameters.add(string(name: 'INSTANCE_NAME', value: "${pipelineParameters.NAMESPACE}-sync-subscriber"))
                    buildParameters.add(string(name: 'KUBE_FLAVOUR', value: pipelineParameters.KUBE_FLAVOUR))
                    build(job: "${pipelineParameters.KUBE_UNDEPLOY_JOB}",
                          parameters: buildParameters,
                          propagate: true,
                          wait: true)
                }
            }
        }

        /*
         *  Preparing terraform to run in the current workspace. Terraform abstracts the AWS access and handles resource lifecycles and deployments
         */
        stage('Install Terraform') {
            steps {
                script {
                    dxTerraformInstall()
                }
            }
        }

         /*
         * create an EC2 instance based on the terraform scripting .
         */
        stage('Create EC2 Instance') {
            steps {
                script {
                    try {
                        terraformVarsEC2 = dxTerraformCreateEc2Instance(terraformVarsEC2)
                        echo "Instance ${terraformVarsEC2.instance_private_ip} running on ${terraformVarsEC2.instance_id}."
                    } catch (err) {
                          error('ERROR: Creating EC2 instance failed.')
                      }
                    println 'terraformVarsEC2 = ' + terraformVarsEC2
                    println 'Test OK'
                }
            }
        }

        stage('Deploying the publisher application in k8 environment') {
            steps {
                script {
                    dir("${WORKSPACE}/dx-dam-staging-tests") {
                        deploymentSettings = readYaml file: "${workspace}/dx-dam-staging-tests/deployment-settings.yaml"
                        echo "deploymentSettings values: ${deploymentSettings}"
                    }
                    buildParameters = []
                    buildParameters.add(string(name: 'INSTANCE_NAME', value: "${pipelineParameters.NAMESPACE}-sync-publisher"))
                    buildParameters.add(string(name: 'NAMESPACE', value: pipelineParameters.NAMESPACE))
                    buildParameters.add(string(name: 'DOMAIN_SUFFIX', value: pipelineParameters.DOMAIN_SUFFIX))
                    buildParameters.add(string(name: 'HOSTED_ZONE', value: pipelineParameters.HOSTED_ZONE))
                    buildParameters.add(string(name: 'BUILD_USER_ID', value: terraformVarsEC2.tfvar_instance_owner))
                    buildParameters.add(string(name: 'NEXT_JOB_DELAY_HOURS', value: '0'))

                    buildParameters.add(string(name: 'IMAGE_REPOSITORY', value: deploymentTargetRepo))
                    buildParameters.add(string(name: 'CORE_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'DAM_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'CC_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'DESIGN_STUDIO_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'DAM_PLUGIN_GOOGLE_VISION_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'RINGAPI_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'IMGPROC_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'DAM_KALTURA_PLUGIN_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'RS_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'RUNTIME_CONTROLLER_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'PERSISTENCE_NODE_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'PERSISTENCE_CONNECTION_POOL_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'HAPROXY_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'LICENSE_MANAGER_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'LOGGING_SIDECAR_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'PERSISTENCE_METRICS_EXPORTER_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'LDAP_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'HELM_CHARTS_FILTER', value: deploymentTargetBranch))

                    buildParameters.add(booleanParam(name: 'PERFORMANCE_RUN', value: pipelineParameters.PERFORMANCE_RUN))
                    buildParameters.add(booleanParam(name: 'ENABLE_DB_CONFIG', value: pipelineParameters.ENABLE_DB_CONFIG))
                    buildParameters.add(booleanParam(name: 'DISABLE_DESIGN_STUDIO', value: deploymentSettings.DISABLE_DESIGN_STUDIO))
                    buildParameters.add(booleanParam(name: 'DISABLE_REMOTESEARCH', value: deploymentSettings.DISABLE_REMOTE_SEARCH))
                    buildParameters.add(booleanParam(name: 'DISABLE_CONTENTCOMPOSER', value: deploymentSettings.DISABLE_CONTENT_COMPOSER))
                    buildParameters.add(booleanParam(name: 'DISABLE_DAM', value: deploymentSettings.DISABLE_DIGITAL_ASSET_MANAGEMENT))
                    buildParameters.add(booleanParam(name: 'DISABLE_KALTURA_PLUGIN', value: deploymentSettings.DISABLE_KALTURA_PLUGIN))
                    buildParameters.add(booleanParam(name: 'DISABLE_RINGAPI', value: deploymentSettings.DISABLE_RING_API))
                    buildParameters.add(booleanParam(name: 'DISABLE_PERSISTENCE', value: deploymentSettings.DISABLE_PERSISTENCE))
                    buildParameters.add(booleanParam(name: 'DISABLE_PLUGIN_GOOGLE_VISION', value: deploymentSettings.DISABLE_PLUGIN_GOOGLE_VISION))
                    buildParameters.add(booleanParam(name: 'DISABLE_IMAGEPROCESSOR', value: deploymentSettings.DISABLE_IMAGEPROCESSOR))
                    buildParameters.add(booleanParam(name: 'DISABLE_AMBASSADOR', value: deploymentSettings.DISABLE_AMBASSADOR))
                    buildParameters.add(booleanParam(name: 'DISABLE_RUNTIME_CONTROLLER', value: deploymentSettings.DISABLE_RUNTIME_CONTROLLER))
                    buildParameters.add(booleanParam(name: 'DISABLE_OPENLDAP', value: deploymentSettings.DISABLE_OPENLDAP))

                    // There is no schedule required for these machines
                    buildParameters.add(string(name: 'NATIVE_POPO_SCHEDULE', value: 'n/a'))

                    build(
                        job: "${pipelineParameters.KUBE_DEPLOY_JOB}",
                        parameters: buildParameters,
                        propagate: true,
                        wait: true
                    )
                }
            }
        }

        stage('Deploying the subscriber application in k8 environment') {
            steps {
                script {
                    dir("${WORKSPACE}/dx-dam-staging-tests") {
                        deploymentSettings = readYaml file: "${workspace}/dx-dam-staging-tests/deployment-settings.yaml"
                        echo "deploymentSettings values: ${deploymentSettings}"
                    }
                    buildParameters = []
                    buildParameters.add(string(name: 'INSTANCE_NAME', value: "${pipelineParameters.NAMESPACE}-sync-subscriber"))
                    buildParameters.add(string(name: 'NAMESPACE', value: pipelineParameters.NAMESPACE))
                    buildParameters.add(string(name: 'DOMAIN_SUFFIX', value: pipelineParameters.DOMAIN_SUFFIX))
                    buildParameters.add(string(name: 'HOSTED_ZONE', value: pipelineParameters.HOSTED_ZONE))
                    buildParameters.add(string(name: 'BUILD_USER_ID', value: terraformVarsEC2.tfvar_instance_owner))
                    buildParameters.add(string(name: 'NEXT_JOB_DELAY_HOURS', value: '0'))

                    buildParameters.add(string(name: 'IMAGE_REPOSITORY', value: deploymentTargetRepo))
                    buildParameters.add(string(name: 'CORE_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'DAM_IMAGE_FILTER', value: pipelineParameters.DAM_IMAGE_FILTER))
                    buildParameters.add(string(name: 'CC_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'DESIGN_STUDIO_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'DAM_PLUGIN_GOOGLE_VISION_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'RINGAPI_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'IMGPROC_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'DAM_KALTURA_PLUGIN_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'RS_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'RUNTIME_CONTROLLER_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'PERSISTENCE_NODE_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'PERSISTENCE_CONNECTION_POOL_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'HAPROXY_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'LICENSE_MANAGER_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'LOGGING_SIDECAR_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'PERSISTENCE_METRICS_EXPORTER_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'LDAP_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'HELM_CHARTS_FILTER', value: deploymentTargetBranch))

                    buildParameters.add(booleanParam(name: 'PERFORMANCE_RUN', value: pipelineParameters.PERFORMANCE_RUN))
                    buildParameters.add(booleanParam(name: 'ENABLE_DB_CONFIG', value: pipelineParameters.ENABLE_DB_CONFIG))
                    buildParameters.add(booleanParam(name: 'DISABLE_DESIGN_STUDIO', value: deploymentSettings.DISABLE_DESIGN_STUDIO))
                    buildParameters.add(booleanParam(name: 'DISABLE_REMOTESEARCH', value: deploymentSettings.DISABLE_REMOTE_SEARCH))
                    buildParameters.add(booleanParam(name: 'DISABLE_CONTENTCOMPOSER', value: deploymentSettings.DISABLE_CONTENT_COMPOSER))
                    buildParameters.add(booleanParam(name: 'DISABLE_DAM', value: deploymentSettings.DISABLE_DIGITAL_ASSET_MANAGEMENT))
                    buildParameters.add(booleanParam(name: 'DISABLE_KALTURA_PLUGIN', value: deploymentSettings.DISABLE_KALTURA_PLUGIN))
                    buildParameters.add(booleanParam(name: 'DISABLE_RINGAPI', value: deploymentSettings.DISABLE_RING_API))
                    buildParameters.add(booleanParam(name: 'DISABLE_PERSISTENCE', value: deploymentSettings.DISABLE_PERSISTENCE))
                    buildParameters.add(booleanParam(name: 'DISABLE_PLUGIN_GOOGLE_VISION', value: deploymentSettings.DISABLE_PLUGIN_GOOGLE_VISION))
                    buildParameters.add(booleanParam(name: 'DISABLE_IMAGEPROCESSOR', value: deploymentSettings.DISABLE_IMAGEPROCESSOR))
                    buildParameters.add(booleanParam(name: 'DISABLE_AMBASSADOR', value: deploymentSettings.DISABLE_AMBASSADOR))
                    buildParameters.add(booleanParam(name: 'DISABLE_RUNTIME_CONTROLLER', value: deploymentSettings.DISABLE_RUNTIME_CONTROLLER))
                    buildParameters.add(booleanParam(name: 'DISABLE_OPENLDAP', value: deploymentSettings.DISABLE_OPENLDAP))

                    // There is no schedule required for these machines
                    buildParameters.add(string(name: 'NATIVE_POPO_SCHEDULE', value: 'n/a'))

                    build(
                        job: "${pipelineParameters.KUBE_DEPLOY_JOB}",
                        parameters: buildParameters,
                        propagate: true,
                        wait: true
                    )
                }
            }
        }

        /*
         * Once prerequisites are installed, we pull the repositories needed for the tests in the workspace, and scp transfer it to the EC2 instance.
         */
        stage('Pull DAM staging jmeter tests') {
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    sshagent(credentials: ['jenkins-git']) {
                        script {
                            sh """
                                echo ${workspace}
                                chmod 600 ${DEPLOY_KEY}
                                git clone -b ${pipelineParameters.JMETER_BRANCH} git@git.cwp.pnp-hcl.com:Team-Q/Portal-Performance-Tests.git ${workspace}/Portal-Performance-Tests
                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sudo mkdir -p /opt/Portal-Performance-Tests && sudo chown centos: /opt/Portal-Performance-Tests'
                                scp -r -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/Portal-Performance-Tests centos@${terraformVarsEC2.instance_private_ip}:/opt
                            """
                        }
                    }
                }
            }
        }

         /*
         * Once scripts are copied to EC2 instance, we need to run the JMeter scripts to register subscriber.
         */
        stage('Run JMeter tests to register subscriber') {
            options {
                timeout(time: 90, unit: 'MINUTES')
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        def nowDate = new SimpleDateFormat('M-d-Y-HHmmss')
                        def date = new Date()
                        Exception caughtException = null
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo 'Running tests'
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && chmod 400 ./keys/Performance-Test-Keypair.pem'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && scp -r -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem jmeter/DAM_Staging_to_Production_Tests/* centos@performance-test-jmeter-master.team-q-dev.com:~/data/dam/staging-to-prod'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \'jmeter -JProtocol=${pipelineParameters.SERVER_PROTOCOL} -JHost=${SERVER_HOST_PUBLISHER} -JPort=${pipelineParameters.SERVER_PORT} -JRingAPIPort='' -JDAMPort='' -JCSVPath=${pipelineParameters.SERVER_CSVPATH} -JCSVPath=${pipelineParameters.SERVER_CSVPATH} -JTarget=${SERVER_HOST_SUBSCRIBER} -j jmeter.save.saveservice.output_format=xml -n -t /home/centos/data/dam/staging-to-prod/${pipelineParameters.SUBSCRIBER_JMX_FILE} -l /home/centos/test_logs/test_log_dam_staging_create_subscriber_${nowDate.format(date)}.jtl\''
                                """
                                jmeter_error = sh(script:"ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \'grep -o -i Exception /home/centos/test_logs/test_log_dam_staging_create_subscriber_${nowDate.format(date)}.jtl | wc -l\''", returnStdout: true).trim() as Integer
                                if ("${jmeter_error}" > 0) {
                                    sh 'exit 1'
                                }
                            } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
                                  error "TIMEOUT ${e.toString()}"
                              } catch (Throwable e) {
                                    caughtException = e
                                }
                            if (caughtException) {
                                error caughtException.message
                            }
                        }
                    }    
                }
            }
        }

        /*
         * Once scripts are copied to EC2 instance, we need to run the JMeter scripts to register publisher.
         */
        stage('Run JMeter tests to register publisher') {
            options {
                timeout(time: 90, unit: 'MINUTES')
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        def nowDate = new SimpleDateFormat('M-d-Y-HHmmss')
                        def date = new Date()
                        Exception caughtException = null
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo 'Running tests'
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && chmod 400 ./keys/Performance-Test-Keypair.pem'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && scp -r -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem jmeter/DAM_Staging_to_Production_Tests/* centos@performance-test-jmeter-master.team-q-dev.com:~/data/dam/staging-to-prod'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \'jmeter -JProtocol=${pipelineParameters.SERVER_PROTOCOL} -JHost=${SERVER_HOST_SUBSCRIBER} -JPort=${pipelineParameters.SERVER_PORT} -JRingAPIPort='' -JDAMPort='' -JCSVPath=${pipelineParameters.SERVER_CSVPATH} -JCSVPath=${pipelineParameters.SERVER_CSVPATH} -JTarget=${SERVER_HOST_PUBLISHER} -j jmeter.save.saveservice.output_format=xml -n -t /home/centos/data/dam/staging-to-prod/${pipelineParameters.SUBSCRIBER_JMX_FILE} -l /home/centos/test_logs/test_log_dam_staging_create_subscriber_${nowDate.format(date)}.jtl\''
                                """
                                jmeter_error = sh(script:"ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \'grep -o -i Exception /home/centos/test_logs/test_log_dam_staging_create_subscriber_${nowDate.format(date)}.jtl | wc -l\''", returnStdout: true).trim() as Integer
                                if ("${jmeter_error}" > 0) {
                                    sh 'exit 1'
                                }
                            } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
                                  error "TIMEOUT ${e.toString()}"
                              } catch (Throwable e) {
                                    caughtException = e
                                }
                            if (caughtException) {
                                error caughtException.message
                            }
                        }
                    }    
                }
            }
        }

        /*
         * Once subscriber is created, we need to run the JMeter scripts to upload assets in publisher environment.
         */
        stage('Run JMeter tests to upload assets') {
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                            dxRemoteInstanceCheckHttpStatus(url: "https://${SERVER_HOST_PUBLISHER}:443/dx/api/dam/v1", lookupInterval: 30, lookupTries: 50)
                            dxRemoteInstanceCheckHttpStatus(url: "https://${SERVER_HOST_PUBLISHER}:443/dx/api/core/v1", lookupInterval: 30, lookupTries: 60)
                        }
                        def nowDate = new SimpleDateFormat('M-d-Y-HHmmss')
                        def date = new Date()
                        Exception caughtException = null
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo 'Running DAM continous syndications tests for 48 hours with sleep of 2 hours with upload of 1k assets in DAM publisher'

                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && chmod 400 ./keys/Performance-Test-Keypair.pem'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && scp -r -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem jmeter/DAM_Staging_to_Production_Tests/* centos@performance-test-jmeter-master.team-q-dev.com:~/data/dam/staging-to-prod'
                                """

                                // DAM uploads to check on hourly basis
                                echo('DAM Staging continous syndication long run to do uploads on hourly basis')

                                Date startDate = new Date()
                                println("startDate: $startDate")

                                int longRunDuration = pipelineParameters.LONG_RUN_DURATION_IN_HOURS.toString().toInteger()
                                println("longRunDuration in : $longRunDuration hours")

                                // upload assets in publisher and also in subscriber to do two-way sync for longRunDuration 
                                for(int i=0; i<longRunDuration; i++){
                                      sh """
                                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \'env JVM_ARGS="-Dnashorn.args=--no-deprecation-warning" jmeter -JProtocol=${pipelineParameters.SERVER_PROTOCOL} -JHost=${SERVER_HOST_PUBLISHER} -JPort=${pipelineParameters.SERVER_PORT} -JRingAPIPort='' -JDAMPort='' -JDocumentsPath=/home/centos/data/dam/DAMRegression/Dataset/documents -JImagesPath=/home/centos/data/dam/DAMRegression/Dataset/images -JVideosPath=/home/centos/data/dam/DAMRegression/Dataset/videos -JCSVPath=/home/centos/data/dam/DAMRegression/Dataset/dam_users.csv -j jmeter.save.saveservice.output_format=xml -n -t /home/centos/data/dam/staging-to-prod/${pipelineParameters.UPLOAD_JMX_FILE} -l /home/centos/test_logs/test_log_dam_staging_upload_assets_${nowDate.format(date)}.jtl\''
                                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \'env JVM_ARGS="-Dnashorn.args=--no-deprecation-warning" jmeter -JProtocol=${pipelineParameters.SERVER_PROTOCOL} -JHost=${SERVER_HOST_SUBSCRIBER} -JPort=${pipelineParameters.SERVER_PORT} -JRingAPIPort='' -JDAMPort='' -JDocumentsPath=/home/centos/data/dam/DAMRegression/Dataset/documents -JImagesPath=/home/centos/data/dam/DAMRegression/Dataset/images -JVideosPath=/home/centos/data/dam/DAMRegression/Dataset/videos -JCSVPath=/home/centos/data/dam/DAMRegression/Dataset/dam_users.csv -j jmeter.save.saveservice.output_format=xml -n -t /home/centos/data/dam/staging-to-prod/${pipelineParameters.UPLOAD_JMX_FILE} -l /home/centos/test_logs/test_log_dam_staging_upload_assets1_${nowDate.format(date)}.jtl\''
                                      """
                                      echo 'sleeping for 1 hour'
                                      sleep 3600
                                }

                                jmeter_error = sh(script:"ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \'grep -o -i Exception /home/centos/test_logs/test_log_dam_staging_upload_assets_${nowDate.format(date)}.jtl | wc -l\''", returnStdout: true).trim() as Integer
                                if ("${jmeter_error}" > 0) {
                                    sh 'exit 1'
                                }
                            } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
                                  error "TIMEOUT ${e.toString()}"
                              } catch (Throwable e) {
                                    caughtException = e
                                }
                            if (caughtException) {
                                error caughtException.message
                            }
                        }
                    }
                }
            }
        }

        /*
         * Once JMeter scripts are executed then we will run the below stage to capture operations time for syndicated assets in subscriber.
         */
        stage('Run scripts to capture operations time in subscriber environment') {
            steps {
                // Capture the DAM operations time
                dir("${workspace}/kube/lifecycle/scripts/common") {
                    configFileProvider([
                        configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                    ]) {
                        script {
                            try {
                                    /* Extract PEM file */
                                sh """
                                    cp $DEPLOY_KEY test-automation-deployments.pem
                                    chmod 0600 test-automation-deployments.pem
                                """

                                /* Copy scripts to EC2 instance */
                                sh """
                                    scp -v -i test-automation-deployments.pem -o StrictHostKeyChecking=no ./capture-dam-operations-time.sh centos@${SERVER_HOST_SUBSCRIBER}:/home/centos/native-kube/
                                """

                                /* Run bash script to capture operations time */
                                sh  """
                                    ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${SERVER_HOST_SUBSCRIBER} \
                                    '(sh /home/centos/native-kube/capture-dam-operations-time.sh "${pipelineParameters.NAMESPACE}" "" 50)'
                                """

                                 /* Copy performane results log from EC2 instance */
                                sh """
                                    scp -v -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${SERVER_HOST_SUBSCRIBER}:/home/centos/native-kube/dam_performance_results.log ${workspace}/dx-dam-staging-tests/
                                    cat ${workspace}/dx-dam-staging-tests/dam_performance_results.log
                                """
                            } catch (Exception err) {
                                echo "Error: ${err}"
                                currentBuild.result = 'FAILURE'
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    cp $DEPLOY_KEY test-automation-deployments.pem
                                    chmod 0600 test-automation-deployments.pem
                                    ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${SERVER_HOST_SUBSCRIBER} 'printf "Failed," >> /home/centos/native-kube/dam_performance_results.log'
                                """
                            }
                        }
                    }
                }
            }
        }

        /*
         * Once assets are uploaded in publisher environment, we need to run the JMeter scripts to find subscriber syncstatus.
         */
        stage('Run JMeter tests to find subscriber sync status') {
            options {
                timeout(time: 90, unit: 'MINUTES')
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        def nowDate = new SimpleDateFormat('M-d-Y-HHmmss')
                        def date = new Date()
                        Exception caughtException = null
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo 'Running tests'
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && chmod 400 ./keys/Performance-Test-Keypair.pem'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && scp -r -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem jmeter/DAM_Staging_to_Production_Tests/* centos@performance-test-jmeter-master.team-q-dev.com:~/data/dam/staging-to-prod'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \'jmeter -JProtocol=${pipelineParameters.SERVER_PROTOCOL} -JHost=${SERVER_HOST_PUBLISHER} -JPort=${pipelineParameters.SERVER_PORT} -JRingAPIPort='' -JDAMPort='' -JImagePath=${pipelineParameters.SERVER_IMAGEPATH} -JCSVPath=${pipelineParameters.SERVER_CSVPATH} -JTargetHost=${SERVER_HOST_SUBSCRIBER} -j jmeter.save.saveservice.output_format=xml -n -t /home/centos/data/dam/staging-to-prod/${pipelineParameters.FINDSTATUS_JMX_FILE} -l /home/centos/test_logs/test_log_dam_staging_syncstatus_${nowDate.format(date)}.jtl\''
                                """
                                jmeter_error = sh(script:"ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \'grep -o -i Exception /home/centos/test_logs/test_log_dam_staging_syncstatus_${nowDate.format(date)}.jtl | wc -l\''", returnStdout: true).trim() as Integer
                                if ("${jmeter_error}" > 0) {
                                    sh 'exit 1'
                                }
                            } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
                                  error "TIMEOUT ${e.toString()}"
                              } catch (Throwable e) {
                                    caughtException = e
                                }
                            if (caughtException) {
                                error caughtException.message
                            }
                        }
                    }
                }
            }
        }

        /*
         * Once subcriber sync status is successfull, we need to run the JMeter scripts to validate assets in subscriber environment.
         */
        stage('Run JMeter tests to validate assets in subscriber environment') {
            options {
                timeout(time: 90, unit: 'MINUTES')
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        def nowDate = new SimpleDateFormat('M-d-Y-HHmmss')
                        def date = new Date()
                        Exception caughtException = null
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo 'Running tests'
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && chmod 400 ./keys/Performance-Test-Keypair.pem'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && scp -r -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem jmeter/DAM_Staging_to_Production_Tests/* centos@performance-test-jmeter-master.team-q-dev.com:~/data/dam/staging-to-prod'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \'jmeter -JProtocol=${pipelineParameters.SERVER_PROTOCOL} -JHost=${SERVER_HOST_SUBSCRIBER} -JPort=${pipelineParameters.SERVER_PORT} -JRingAPIPort='' -JDAMPort='' -JImagePath=${pipelineParameters.SERVER_IMAGEPATH} -JCSVPath=${pipelineParameters.SERVER_CSVPATH} -j jmeter.save.saveservice.output_format=xml -n -t /home/centos/data/dam/staging-to-prod/${pipelineParameters.VERIFY_JMX_FILE} -l /home/centos/test_logs/test_log_dam_staging_validate_asset_${nowDate.format(date)}.jtl\''
                                """
                                jmeter_error = sh(script:"ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \'grep -o -i Exception /home/centos/test_logs/test_log_dam_staging_validate_asset_${nowDate.format(date)}.jtl | wc -l\''", returnStdout: true).trim() as Integer
                                if ("${jmeter_error}" > 0) {
                                    sh 'exit 1'
                                }
                            } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
                                  error "TIMEOUT ${e.toString()}"
                              } catch (Throwable e) {
                                    caughtException = e
                                }
                            if (caughtException) {
                                error caughtException.message
                            }
                        }
                    }
                }
            }
        }

         /*
         * Once assets are uploaded in publisher environment, we need to run the JMeter scripts to find subscriber syncstatus.
         */
        stage('Run JMeter tests to find publisher sync status') {
            options {
                timeout(time: 90, unit: 'MINUTES')
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        def nowDate = new SimpleDateFormat('M-d-Y-HHmmss')
                        def date = new Date()
                        Exception caughtException = null
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo 'Running tests'
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && chmod 400 ./keys/Performance-Test-Keypair.pem'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && scp -r -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem jmeter/DAM_Staging_to_Production_Tests/* centos@performance-test-jmeter-master.team-q-dev.com:~/data/dam/staging-to-prod'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \'jmeter -JProtocol=${pipelineParameters.SERVER_PROTOCOL} -JHost=${SERVER_HOST_SUBSCRIBER} -JPort=${pipelineParameters.SERVER_PORT} -JRingAPIPort='' -JDAMPort='' -JImagePath=${pipelineParameters.SERVER_IMAGEPATH} -JCSVPath=${pipelineParameters.SERVER_CSVPATH} -JTargetHost=${SERVER_HOST_PUBLISHER} -j jmeter.save.saveservice.output_format=xml -n -t /home/centos/data/dam/staging-to-prod/${pipelineParameters.FINDSTATUS_JMX_FILE} -l /home/centos/test_logs/test_log_dam_staging_syncstatus_${nowDate.format(date)}.jtl\''
                                """
                                jmeter_error = sh(script:"ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \'grep -o -i Exception /home/centos/test_logs/test_log_dam_staging_syncstatus_${nowDate.format(date)}.jtl | wc -l\''", returnStdout: true).trim() as Integer
                                if ("${jmeter_error}" > 0) {
                                    sh 'exit 1'
                                }
                            } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
                                  error "TIMEOUT ${e.toString()}"
                              } catch (Throwable e) {
                                    caughtException = e
                                }
                            if (caughtException) {
                                error caughtException.message
                            }
                        }
                    }
                }
            }
        }
        /*
         * Once publisher sync status is successfull, we need to run the JMeter scripts to validate assets in publisher environment.
         */
        stage('Run JMeter tests to validate assets in publisher environment') {
            options {
                timeout(time: 90, unit: 'MINUTES')
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        def nowDate = new SimpleDateFormat('M-d-Y-HHmmss')
                        def date = new Date()
                        Exception caughtException = null
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo 'Running tests'
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && chmod 400 ./keys/Performance-Test-Keypair.pem'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && scp -r -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem jmeter/DAM_Staging_to_Production_Tests/* centos@performance-test-jmeter-master.team-q-dev.com:~/data/dam/staging-to-prod'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \'jmeter -JProtocol=${pipelineParameters.SERVER_PROTOCOL} -JHost=${SERVER_HOST_PUBLISHER} -JPort=${pipelineParameters.SERVER_PORT} -JRingAPIPort='' -JDAMPort='' -JImagePath=${pipelineParameters.SERVER_IMAGEPATH} -JCSVPath=${pipelineParameters.SERVER_CSVPATH} -j jmeter.save.saveservice.output_format=xml -n -t /home/centos/data/dam/staging-to-prod/${pipelineParameters.VERIFY_JMX_FILE} -l /home/centos/test_logs/test_log_dam_staging_validate_asset_${nowDate.format(date)}.jtl\''
                                """
                                jmeter_error = sh(script:"ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \'grep -o -i Exception /home/centos/test_logs/test_log_dam_staging_validate_asset_${nowDate.format(date)}.jtl | wc -l\''", returnStdout: true).trim() as Integer
                                if ("${jmeter_error}" > 0) {
                                    sh 'exit 1'
                                }
                            } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
                                  error "TIMEOUT ${e.toString()}"
                              } catch (Throwable e) {
                                    caughtException = e
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

     /*
     * Perform proper cleanup to leave a healthy jenkins agent. On build unstable/failure/aborted/success we clean up the EC2 instance.
     */
    post {
        cleanup {
            script {
                // Destroy the EC2 instance incl. storage
                dxTerraformDestroyEc2Instance(terraformVarsEC2)
                dxWorkspaceDirectoriesCleanup()
            }
        }
    }
}
