/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2023, 2024. All Rights Reserved. *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

import groovy.time.TimeCategory
import groovy.time.TimeDuration
import java.text.SimpleDateFormat

@Library('dx-shared-library') _
// test map for creating a new EC2 instance

// Using SimpleDateFormat for versioning the name of executions
import java.text.SimpleDateFormat

def pipelineParameters = [:]
def terraformVarsEC2 = [:]

def deploymentTargetBranch
def deploymentTargetRepo
def deploymentSettings
def SERVER_HOST_PUBLISHER
def SERVER_HOST_SUBSCRIBER
def uploadStartTime
def deleteStartTime
def updateStartTime

pipeline {
    agent {
        label 'build_infra'
    }

    stages {
        stage('Load modules and configuration') {
            steps {
                script {
                    // load params from yaml file
                    dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/dx-dam-staging-tests/parameters-short-run.yaml")
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
                        terraformVarsEC2.tfvar_instance_name = "DAM-Staging-short-run_${dateFormat.format(date)}"
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
                        println("BUILD_URL > ${env.BUILD_URL} <.")

                        // Adjust display name of current run
                        def currentDate = "${dateFormat.format(date)}"
                        def removeHTTPFromDisplayName = currentBuild.displayName.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)","")
                        def tempReportBuildName = removeHTTPFromDisplayName.split('/')
                        tempReportBuildName = tempReportBuildName[0].replace(':','')
                        if (removeHTTPFromDisplayName.contains('/')) {
                            tempReportBuildName = "${tempReportBuildName}_${currentDate}"
                            tempReportBuildName = tempReportBuildName.replace(':','')
                        }
                        pipelineParameters.reportBuildname = tempReportBuildName
                        println("Report build name is > ${pipelineParameters.reportBuildname} <.")
                    }
                }
            }
        }

        stage('Undeploying the publisher application in k8 environment') {
            steps {
                script {
                    buildParameters = []
                    buildParameters.add(string(name: 'NAMESPACE', value: "${pipelineParameters.NAMESPACE}-sync-publisher"))
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
                    buildParameters.add(string(name: 'NAMESPACE', value: "${pipelineParameters.NAMESPACE}-sync-subscriber" ))
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
                        scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/dx-dam-staging-tests/helpers/01-setup-prereqs.sh centos@${terraformVarsEC2.instance_private_ip}:/tmp
                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'chmod +x /tmp/01-setup-prereqs.sh && sh /tmp/01-setup-prereqs.sh'
                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sudo mkdir -p /opt/dam-staging-acceptance-test-reports && sudo chown centos: /opt/dam-staging-acceptance-test-reports'
                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'mkdir -p /opt/dam-staging-acceptance-test-reports/html && mkdir -p /opt/dam-staging-acceptance-test-reports/xml && mkdir -p /opt/dam-staging-acceptance-test-reports/dam_staging_report'
                        scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r ${workspace}/test-scripts/TestReport/* centos@${terraformVarsEC2.instance_private_ip}:/opt/dam-staging-acceptance-test-reports
                        scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r ${workspace}/dx-core-tests/dx-test-rest/helpers/Saxon-HE-9.5.1-3.jar centos@${terraformVarsEC2.instance_private_ip}:/opt/dam-staging-acceptance-test-reports/Saxon-HE-9.5.1-3.jar
                    """
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

                    buildParameters.add(booleanParam(name: 'PERFORMANCE_RUN', value: true))
                    buildParameters.add(booleanParam(name: 'ENABLE_DB_CONFIG', value: false))
                    buildParameters.add(booleanParam(name: 'DISABLE_DESIGN_STUDIO', value: deploymentSettings.DISABLE_DESIGN_STUDIO))
                    buildParameters.add(booleanParam(name: 'DISABLE_REMOTESEARCH', value: deploymentSettings.DISABLE_REMOTE_SEARCH))
                    buildParameters.add(booleanParam(name: 'DISABLE_CONTENTCOMPOSER', value: deploymentSettings.DISABLE_CONTENT_COMPOSER))
                    buildParameters.add(booleanParam(name: 'DISABLE_DAM', value: deploymentSettings.DISABLE_DIGITAL_ASSET_MANAGEMENT))
                    buildParameters.add(booleanParam(name: 'DISABLE_KALTURA_PLUGIN', value: deploymentSettings.DISABLE_KALTURA_PLUGIN))
                    buildParameters.add(booleanParam(name: 'DISABLE_RINGAPI', value: deploymentSettings.DISABLE_RING_API))
                    buildParameters.add(booleanParam(name: 'DISABLE_PERSISTENCE', value: deploymentSettings.DISABLE_PERSISTENCE))
                    buildParameters.add(booleanParam(name: 'DISABLE_PLUGIN_GOOGLE_VISION', value: deploymentSettings.DISABLE_PLUGIN_GOOGLE_VISION))
                    buildParameters.add(booleanParam(name: 'PERFORMANCE_RUN', value: deploymentSettings.PERFORMANCE_RUN))
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

                    buildParameters.add(booleanParam(name: 'PERFORMANCE_RUN', value: true))
                    buildParameters.add(booleanParam(name: 'ENABLE_DB_CONFIG', value: false))
                    buildParameters.add(booleanParam(name: 'DISABLE_DESIGN_STUDIO', value: deploymentSettings.DISABLE_DESIGN_STUDIO))
                    buildParameters.add(booleanParam(name: 'DISABLE_REMOTESEARCH', value: deploymentSettings.DISABLE_REMOTE_SEARCH))
                    buildParameters.add(booleanParam(name: 'DISABLE_CONTENTCOMPOSER', value: deploymentSettings.DISABLE_CONTENT_COMPOSER))
                    buildParameters.add(booleanParam(name: 'DISABLE_DAM', value: deploymentSettings.DISABLE_DIGITAL_ASSET_MANAGEMENT))
                    buildParameters.add(booleanParam(name: 'DISABLE_KALTURA_PLUGIN', value: deploymentSettings.DISABLE_KALTURA_PLUGIN))
                    buildParameters.add(booleanParam(name: 'DISABLE_RINGAPI', value: deploymentSettings.DISABLE_RING_API))
                    buildParameters.add(booleanParam(name: 'DISABLE_PERSISTENCE', value: deploymentSettings.DISABLE_PERSISTENCE))
                    buildParameters.add(booleanParam(name: 'DISABLE_PLUGIN_GOOGLE_VISION', value: deploymentSettings.DISABLE_PLUGIN_GOOGLE_VISION))
                    buildParameters.add(booleanParam(name: 'PERFORMANCE_RUN', value: deploymentSettings.PERFORMANCE_RUN))
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
         * Once subscriber is created, we need to run the JMeter scripts to upload assets in publisher environment.
         */
        stage('Run JMeter tests to upload assets') {
            options {
                timeout(time: 90, unit: 'MINUTES') 
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                        dxRemoteInstanceCheckHttpStatus(url: "https://${SERVER_HOST_PUBLISHER}:443/dx/api/dam/v1", lookupInterval: 30, lookupTries: 50)
                        dxRemoteInstanceCheckHttpStatus(url: "https://${SERVER_HOST_PUBLISHER}:443/dx/api/core/v1", lookupInterval: 30, lookupTries: 60)
                        }
                        def nowDate = new SimpleDateFormat("M-d-Y-HHmmss")
                        def date = new Date()
                        Exception caughtException = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo "Running tests"
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && chmod 400 ./keys/Performance-Test-Keypair.pem'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && scp -r -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem jmeter/DAM_Staging_to_Production_Tests/* centos@performance-test-jmeter-master.team-q-dev.com:~/data/dam/staging-to-prod'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \'env JVM_ARGS="-Dnashorn.args=--no-deprecation-warning" jmeter -JProtocol=${pipelineParameters.SERVER_PROTOCOL} -JHost=${SERVER_HOST_PUBLISHER} -JPort=${pipelineParameters.SERVER_PORT} -JRingAPIPort='' -JDAMPort='' -JDocumentsPath=/home/centos/data/dam/DAMRegression/Dataset/documents -JImagesPath=/home/centos/data/dam/DAMRegression/Dataset/images -JVideosPath=/home/centos/data/dam/DAMRegression/Dataset/videos -JCSVPath=/home/centos/data/dam/DAMRegression/Dataset/dam_users.csv -j jmeter.save.saveservice.output_format=xml -n -t /home/centos/data/dam/DAM_Staging_to_Production_Tests/${pipelineParameters.UPLOAD_JMX_FILE} -l /home/centos/test_logs/test_log_dam_staging_upload_assets_${nowDate.format(date)}.jtl\''
                                """
                                jmeter_error=sh(script:"ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \'grep -o -i Exception /home/centos/test_logs/test_log_dam_staging_upload_assets_${nowDate.format(date)}.jtl | wc -l\''",returnStdout: true).trim() as Integer
                                if ("${jmeter_error}">0) {
                                    sh "exit 1"
                                }
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

        /*
         * Once JMeter scripts are executed then we will run the below stage to capture operations time for uploaded assets.
         */
        stage('Run scripts to capture operations time in publisher') {
            steps {
                // Capture the DAM operations time
                dir("${workspace}/kube/lifecycle/scripts/common") {
                    configFileProvider([
                        configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                    ]) {
                        script {
                            uploadStartTime = new Date()
                            try {
                                    /* Extract PEM file */
                                sh """
                                    cp $DEPLOY_KEY test-automation-deployments.pem
                                    chmod 0600 test-automation-deployments.pem
                                """

                                /* Copy scripts to EC2 instance */
                                sh """
                                    scp -v -i test-automation-deployments.pem -o StrictHostKeyChecking=no ./capture-dam-operations-time.sh centos@${SERVER_HOST_PUBLISHER}:/home/centos/native-kube/
                                """

                                /* Run bash script to capture operations time */
                                sh  """
                                    ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${SERVER_HOST_PUBLISHER} \
                                    '(sh /home/centos/native-kube/capture-dam-operations-time.sh "${pipelineParameters.NAMESPACE}" "" ${pipelineParameters.OPERATIONS_EMPTY_CHECK_RETRIES})'
                                """

                                 /* Copy performane results log from EC2 instance */
                                sh """
                                    scp -v -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${SERVER_HOST_PUBLISHER}:/home/centos/native-kube/dam_performance_results.log ${workspace}/dx-dam-staging-tests/
                                    cat ${workspace}/dx-dam-staging-tests/dam_performance_results.log
                                """
                            } catch(Exception err) {
                                echo "Error: ${err}"
                                currentBuild.result = "FAILURE"
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    cp $DEPLOY_KEY test-automation-deployments.pem
                                    chmod 0600 test-automation-deployments.pem
                                    ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${SERVER_HOST_PUBLISHER} 'printf "Failed," >> /home/centos/native-kube/dam_performance_results.log'
                                """
                            }
                        }
                    }
                }
            }
        }

        /*
         * Once scripts are copied to EC2 instance, we need to run the JMeter scripts to create subscriber. 
         */ 
        stage('Run JMeter tests to create subscriber') {
            options {
                timeout(time: 90, unit: 'MINUTES') 
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        def nowDate = new SimpleDateFormat("M-d-Y-HHmmss")
                        def date = new Date()
                        Exception caughtException = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo "Running tests"
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && chmod 400 ./keys/Performance-Test-Keypair.pem'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && scp -r -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem jmeter/DAM_Staging_to_Production_Tests/* centos@performance-test-jmeter-master.team-q-dev.com:~/data/dam/staging-to-prod'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \'env JVM_ARGS="-Dnashorn.args=--no-deprecation-warning" jmeter -JProtocol=${pipelineParameters.SERVER_PROTOCOL} -JHost=${SERVER_HOST_PUBLISHER} -JPort=${pipelineParameters.SERVER_PORT} -JRingAPIPort='' -JDAMPort='' -JCSVPath=${pipelineParameters.SERVER_CSVPATH} -JCSVPath=${pipelineParameters.SERVER_CSVPATH} -JTarget=${SERVER_HOST_SUBSCRIBER} -j jmeter.save.saveservice.output_format=xml -n -t /home/centos/data/dam/staging-to-prod/${pipelineParameters.SUBSCRIBER_JMX_FILE} -l /home/centos/test_logs/test_log_dam_staging_create_subscriber_${nowDate.format(date)}.jtl\''
                                """
                                jmeter_error=sh(script:"ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \'grep -o -i Exception /home/centos/test_logs/test_log_dam_staging_create_subscriber_${nowDate.format(date)}.jtl | wc -l\''",returnStdout: true).trim() as Integer
                                if ("${jmeter_error}">0) {
                                    sh "exit 1"
                                }

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

        stage('Install dxclient and trigger staging sync') {
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) 
                {
                script {
                    dir("${WORKSPACE}") {
                        withCredentials([
                          [$class: 'UsernamePasswordMultiBinding', credentialsId: "${pipelineParameters.TOOL_CREDENTIALS_ID}", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']
              ]) {
                
                      sh """
                                chmod 600 ${DEPLOY_KEY}
                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /home/centos/ && curl -s -u${USERNAME}:${PASSWORD} ${pipelineParameters.TOOL_PACKAGE_URL} --output dxclient.zip'
                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'yes | unzip dxclient.zip && ls'
                                echo load the dxclient docker image
                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /home/centos/dxclient && docker load < dxclient.tar.gz'
                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /home/centos/dxclient && ./bin/dxclient -h'
                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /home/centos/dxclient && ./bin/dxclient  manage-dam-staging register-dam-subscriber -dxProtocol ${pipelineParameters.DX_PROTOCOL} -hostname ${SERVER_HOST_PUBLISHER} -dxPort ${pipelineParameters.DX_PORT} -dxUsername ${pipelineParameters.PUBLISHER_USERNAME} -dxPassword {pipelineParameters.PUBLISHER_PASSWORD} -dxWASUsername ${pipelineParameters.WAS_USERNAME} -dxWASPassword ${pipelineParameters.WAS_PASSWORD}  -targetServerUsername ${pipelineParameters.SUBSCRIBER_USERNAME} -targetServerPassword ${pipelineParameters.SUBSCRIBER_PASSWORD} -damAPIPort ${pipelineParameters.DAM_API_PORT} -ringAPIPort ${pipelineParameters.RING_API_PORT} -damAPIVersion v1 -ringAPIVersion v1 -targetHostname ${SERVER_HOST_SUBSCRIBER} -interval 2'
                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /home/centos/dxclient && ./bin/dxclient  manage-dam-staging trigger-staging -dxProtocol https -hostname ${SERVER_HOST_PUBLISHER} -dxPort ${pipelineParameters.DX_PORT} -dxUsername ${pipelineParameters.PUBLISHER_USERNAME} -dxPassword ${pipelineParameters.PUBLISHER_PASSWORD} -damAPIPort ${pipelineParameters.DAM_API_PORT} -ringAPIPort ${pipelineParameters.RING_API_PORT} -damAPIVersion v1 -ringAPIVersion v1 -targetHostname ${SERVER_HOST_SUBSCRIBER}'
                        """
              }
                    }
                }
                }
            }
        }

         /*
         * Once JMeter scripts are executed then we will run the below stage to capture operations time for uploaded assets.
         */
        stage('Run scripts to capture operations time in subscriber') {
            steps {
                // Capture the DAM operations time
                dir("${workspace}/kube/lifecycle/scripts/common") {
                    configFileProvider([
                        configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                    ]) {
                        script {
                            // subscriber registered just before this stage - waiting for events to send to subscriber
                            sleep 600
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
                                    '(sh /home/centos/native-kube/capture-dam-operations-time.sh "${pipelineParameters.NAMESPACE}" "" ${pipelineParameters.OPERATIONS_EMPTY_CHECK_RETRIES})'
                                """

                                 /* Copy performane results log from EC2 instance */
                                sh """
                                    scp -v -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${SERVER_HOST_SUBSCRIBER}:/home/centos/native-kube/dam_performance_results.log ${workspace}/dx-dam-staging-tests/
                                    cat ${workspace}/dx-dam-staging-tests/dam_performance_results.log
                                """
                            } catch(Exception err) {
                                echo "Error: ${err}"
                                currentBuild.result = "FAILURE"
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

        stage('Run scripts to capture total dam staging upload time') {
            steps {
                script{
                    def uploadEndTime = new Date()
                    TimeDuration uploadTimeDuration = TimeCategory.minus( uploadEndTime, uploadStartTime )
                    println("DAM Staging upload time duration in HH:mm:ss - " + new Date(uploadTimeDuration.toMilliseconds()).format("HH:mm:ss"))
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
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \'env JVM_ARGS="-Dnashorn.args=--no-deprecation-warning" jmeter -JProtocol=${pipelineParameters.SERVER_PROTOCOL} -JHost=${SERVER_HOST_SUBSCRIBER} -JPort=${pipelineParameters.SERVER_PORT} -JRingAPIPort='' -JDAMPort='' -JImagePath=${pipelineParameters.SERVER_IMAGEPATH} -JCSVPath=${pipelineParameters.SERVER_CSVPATH} -j jmeter.save.saveservice.output_format=xml -n -t /home/centos/data/dam/staging-to-prod/${pipelineParameters.VERIFY_JMX_FILE} -l /home/centos/test_logs/test_log_dam_staging_validate_asset_${nowDate.format(date)}.jtl\' -e -f -o /home/centos/test_logs/dam_staging_report_${nowDate.format(date)}'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && scp -r -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com:~/test_logs/dam_staging_report_${nowDate.format(date)}/statistics.json /opt/dam-staging-acceptance-test-reports/'
                                """

                                echo 'executing script to capture to total samples and error count'
                                /* generate xml file for current run */
                                sh """
                                    cp $DEPLOY_KEY test-automation-deployments.pem
                                    chmod 0600 test-automation-deployments.pem
                                    scp -v -i test-automation-deployments.pem -o StrictHostKeyChecking=no ${workspace}/test-scripts/get_staging_count.sh centos@${terraformVarsEC2.instance_private_ip}:/opt/dam-staging-acceptance-test-reports/
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sh /opt/dam-staging-acceptance-test-reports/get_staging_count.sh "/opt/dam-staging-acceptance-test-reports/statistics.json" ${pipelineParameters.reportBuildname} ${env.BUILD_URL}'
                                """
                                
                                jmeter_error = sh(script:"ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \'grep -o -i Exception /home/centos/test_logs/test_log_dam_staging_validate_asset_${nowDate.format(date)}.jtl | wc -l\''", returnStdout: true).trim() as Integer
                                
                                if ("${jmeter_error}" > 0) {
                                    currentBuild.result = 'UNSTABLE'
                                    echo 'Errors in DAM Staging Short Run JMeter script execution results'
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

        stage('Generate Test Report') {
            steps {
                withCredentials([
                    usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID'),
                ]) {
                    dir("${workspace}") {
                        configFileProvider([
                                configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                        ]) {
                            script {
                                Exception caughtException = null;
                                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                                    try {
                                        echo "Generating test dam-staging-acceptance-dashboard report"
                                        // Copy previous runs file from s3 bucket
                                        // Copy current run and previous run file to EC2 instance
                                        // Execute script to generate report
                                        // Copy reports(html and css) to s3 bucket  
                                        sh """
                                            chmod 600 ${DEPLOY_KEY}
                                            aws s3 cp s3://dx-testarea/dam-staging-acceptance-test-reports/dam-staging-acceptance-test-runs.xml dam-staging-acceptance-test-runs.xml
                                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/dam-staging-acceptance-test-runs.xml centos@${terraformVarsEC2.instance_private_ip}:/opt/dam-staging-acceptance-test-reports/
                                            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/dam-staging-acceptance-test-reports/ && chmod +x dam_staging_acceptance_test_run.sh && sh dam_staging_acceptance_test_run.sh ${pipelineParameters.reportBuildname} Dam-Staging'
                                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r centos@${terraformVarsEC2.instance_private_ip}:/opt/dam-staging-acceptance-test-reports/dam-staging-acceptance-test-runs.xml .
                                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r centos@${terraformVarsEC2.instance_private_ip}:/opt/dam-staging-acceptance-test-reports/dashboard/* .
                                            aws s3 cp dam-staging-acceptance-test-runs.xml s3://dx-testarea/dam-staging-acceptance-test-reports/dam-staging-acceptance-test-runs.xml
                                            aws s3 cp Dam-Staging-dashboard.html s3://dx-testarea/dam-staging-acceptance-test-reports/Dam-Staging-dashboard.html
                                            aws s3 cp wtf.css s3://dx-testarea/dam-staging-acceptance-test-reports/wtf.css
                                       """
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
