/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2022. All Rights Reserved.       *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

import java.text.SimpleDateFormat

@Library('dx-shared-library') _
// test map for creating a new EC2 instance
def terraformVarsEC2 = [:]
terraformVarsEC2.tfvar_instance_name = 'exim_test'
terraformVarsEC2.tfvar_instance_owner = 'utkarshsr@hcl.com'
terraformVarsEC2.tfvar_aws_subnet = 'subnet-07df4340bd57769e3'
pipeline {
    agent {
        label 'build_infra'
    }
    parameters {
        choice(name: 'OPTION', choices: ['create', 'destroy', 'addroute53', 'delroute53', 'test0', 'test1', 'test2', 'test3', 'test4', 'test5'], description: 'Option which stage to run. Install terraform stage will always run')
        string(name: 'TARGET_BRANCH', defaultValue: 'develop', description: 'Target branch')
        string(name: 'DEPLOYMENT_LEVEL', defaultValue: 'develop', description: 'Deploying latest images')
        string(name: 'NAMESPACE', defaultValue: 'exim', description: 'name space')
        string(name: 'KUBE_FLAVOUR', defaultValue: 'native', description: 'Deploying a native kube environment.')
        string(name: 'KUBE_UNDEPLOY_JOB', defaultValue: 'CI/kube-deploy/native-kube-remove', description: 'Job which undeploys the environment',  trim: false)
        string(name: 'KUBE_DEPLOY_JOB', defaultValue: 'CI/kube-deploy/native-kube-next-deploy', description: 'Job which deploys the environment',  trim: false)
        choice(name: 'DEPLOYMENT_METHOD', choices:'helm',  description: 'deployment method')
        string(name: 'CLUSTER_NAME', defaultValue: '', description: 'Cluster name where the deployment should be deployed to')
        string(name: 'CLUSTER_REGION', defaultValue: '', description: 'Region of the cluster')
        string(name: 'CONTEXT_ROOT_PATH', defaultValue: 'wps', description: 'Context root')
        string(name: 'DX_CORE_HOME_PATH', defaultValue: 'portal', description: 'Home path')
        string(name: 'PERSONALIZED_DX_CORE_PATH', defaultValue: 'myportal', description: 'Personalized path')
        string(name: 'DOMAIN_SUFFIX', defaultValue: '.hcl-dx-dev.net', description: ' Kube flavour domain suffix')
        booleanParam(name: 'SSL_ENABLED', defaultValue: true, description: 'Required for testing environments with https/self-signed certificates like native.kube.')
        string(name: 'JMETER_BRANCH', defaultValue: 'develop', description: 'Jmeter script branch')
        string(name: 'TOOL_PACKAGE_URL', defaultValue: 'https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dxclient-new/hcl-dxclient-image-v95_CF200_20220211-2040_rohan_develop.zip', description: 'URL from which to download dxclient zip',  trim: false)
        string(name: 'TOOL_CREDENTIALS_ID', defaultValue: 'artifactory', description: 'ID in Jenkins store to user name / password credentials needed to access tool package URL',  trim: false)
        string(name: 'STAGING_ACCEPTANCE_TEST_JOB', defaultValue: 'CI/DAM-Staging/staging_acceptance_tests', description: 'Job which runs acceptance tests for staging',  trim: false)
        string(name: 'DX_PROTOCOL', defaultValue: 'https', description: 'Protocol to connect to DX server',  trim: false)
        string(name: 'DX_PORT', defaultValue: '443', description: 'Port to connect to DX server (main profile)',  trim: false)
        string(name: 'USERNAME', defaultValue: 'wpsadmin', description: 'username of target',  trim: false)
        string(name: 'PASSWORD', defaultValue: 'wpsadmin', description: 'password of target',  trim: false)
        string(name: 'DAM_API_PORT', defaultValue: '443', description: 'port for dam api',  trim: false)
        string(name: 'RING_API_PORT', defaultValue: '443', description: 'port for ring api',  trim: false)
        booleanParam(name: 'EXPORT_BINARY', defaultValue: true, description: 'binary flag')
    }

    stages {
        stage('Load modules and configuration') {
            steps {
                script {
                    commonModule = load './autonomous-deployments/modules/common.gvy'
                    commonConfig = load './autonomous-deployments/config/common.gvy'
                    env.NAMESPACE = 'exim'
                    env.NAMESPACE_EXIM_SOURCE = "${env.NAMESPACE}-source"
                    env.NAMESPACE_EXIM_TARGET = "${env.NAMESPACE}-target"
                    env.EXPORT_PATH = '/dxclient/store/outputFiles/dam-export-assets'
                }
            }
        }
        stage('Prepare Settings') {
            steps {
                /*
                 * Assigning hostname using Performace-test-automation + the timestap
                 */
                dir("${WORKSPACE}/dx-dam-exim-tests") {
                    script {
                        // determine build version and label current job accordingly
                        def dateFormat = new SimpleDateFormat('yyyyMMdd-HHmm')
                        def date = new Date()
                        env.SERVER_PROTOCOL = 'https'
                        env.SERVER_HOST_SOURCE = "${env.NAMESPACE_EXIM_SOURCE}${DOMAIN_SUFFIX}"
                        env.SERVER_HOST_TARGET = "${env.NAMESPACE_EXIM_TARGET}${DOMAIN_SUFFIX}"
                        env.SERVER_PORT = '10039'
                        env.SERVER_RINGAPI_PORT = ''
                        env.SERVER_DAMAPI_PORT = ''
                        env.SERVER_IMAGEPATH = '/home/centos/data/dam/DAM_EXIM_Tests/assets'
                        env.SERVER_CSVPATH = '/home/centos/data/dam/DAM_EXIM_Tests/assets/users.csv'
                        if (!env.UPLOAD_JMX_FILE) {
                            env.UPLOAD_JMX_FILE = 'DAM_Assets_Upload.jmx'
                        }
                        if (!env.VERIFY_JMX_FILE) {
                            env.VERIFY_JMX_FILE = 'DAM_Assets_Verification.jmx'
                        }

                        echo 'Assigning hostname + timestamp'
                        terraformVarsEC2.tfvar_instance_name= "${terraformVarsEC2.tfvar_instance_name}_${dateFormat.format(date)}"
                        echo "New instance will be: ${terraformVarsEC2.tfvar_instance_name}"

                        // Display name includes the env.INSTANCE_NAME and a timestamp
                        currentBuild.displayName = "${terraformVarsEC2.tfvar_instance_name}"
                    }
                }
            }
        }
        stage('Undeploying the stale application in k8 source environment for exim test') {
            steps {
                script {
                    buildParams = []
                    buildParams.add(
                    string(name: 'INSTANCE_NAME', value: "${env.NAMESPACE_EXIM_SOURCE}")
                                   )
                    build(job: "${params.KUBE_UNDEPLOY_JOB}",
                          parameters: buildParams,
                          propagate: true,
                          wait: true)
                }
            }
        }
        stage('Undeploying the stale application in k8 target environment for exim test') {
            steps {
                script {
                    buildParams = []
                    buildParams.add(
                    string(name: 'INSTANCE_NAME', value: "${env.NAMESPACE_EXIM_TARGET}")
                                   )
                    build(job: "${params.KUBE_UNDEPLOY_JOB}",
                          parameters: buildParams,
                          propagate: true,
                          wait: true)
                }
            }
        }
        stage('Deploying the application in k8 source environment for exim test') {
            steps {
                script {
                    buildParams = commonModule.createKubeParams(env.NAMESPACE_EXIM_SOURCE, params.KUBE_FLAVOUR, params.CLUSTER_NAME, params.CLUSTER_REGION, '', params.CONTEXT_ROOT_PATH, params.DX_CORE_HOME_PATH, params.PERSONALIZED_DX_CORE_PATH, params.DEPLOYMENT_LEVEL, params.DEPLOYMENT_METHOD, params.DOMAIN_SUFFIX)
                    build(job: "${params.KUBE_DEPLOY_JOB}",
                          parameters: buildParams,
                          propagate: true,
                          wait: true)
                }
            }
        }
        stage('Deploying the application in k8 target environment for exim test') {
            steps {
                script {
                    buildParams = commonModule.createKubeParams(env.NAMESPACE_EXIM_TARGET, params.KUBE_FLAVOUR, params.CLUSTER_NAME, params.CLUSTER_REGION, '', params.CONTEXT_ROOT_PATH, params.DX_CORE_HOME_PATH, params.PERSONALIZED_DX_CORE_PATH, params.DEPLOYMENT_LEVEL, params.DEPLOYMENT_METHOD, params.DOMAIN_SUFFIX)
                    build(job: "${params.KUBE_DEPLOY_JOB}",
                          parameters: buildParams,
                          propagate: true,
                          wait: true)
                }
            }
        }
        stage('Running dam-exim acceptance tests in k8 source environment') {
            steps {
                script {
                    catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                    dxRemoteInstanceCheckHttpStatus(url: "https://${env.SERVER_HOST_SOURCE}:443/dx/api/dam/v1", lookupInterval: 20, lookupTries: 60)
                    dxRemoteInstanceCheckHttpStatus(url: "https://${env.SERVER_HOST_SOURCE}:443/dx/api/core/v1", lookupInterval: 20, lookupTries: 60)
                }
                    buildParams = []
                    buildParams.add(
                        string(name: 'DAM_API', value: "https://${env.SERVER_HOST_SOURCE}:443/dx/api/dam/v1")
                    )
                    buildParams.add(
                        string(name: 'EXP_API', value: "https://${env.SERVER_HOST_SOURCE}:443/dx/api/core/v1")
                    )
                    build(job: "${params.STAGING_ACCEPTANCE_TEST_JOB}",
                          parameters: buildParams,
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
                    println '************************************'
                    println "***  Running TEST: ${env.OPTION}"
                    println '************************************'
                    dxTerraformInstall()
                }
            }
        }
         /*
         * create an EC2 instance based on the terraform scripting .
         */
        stage('Create EC2 Instance') {
            when {
                expression { env.OPTION == 'create' }
            }
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
                        println 'SL_TERRAFORM_EC2_SETTINGS_KEYS = ' + env.SL_TERRAFORM_EC2_SETTINGS_KEYS
                        println 'Test OK'
                }
            }
        }
        /*
         * Once prerequisites are installed, we pull the repositories needed for the tests in the workspace, and scp transfer it to the EC2 instance.
         */
        stage('Pull exim Performace tests') {
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    sshagent(credentials: ['jenkins-git']) {
                            sh """
                                echo ${workspace}
                                chmod 600 ${DEPLOY_KEY}
                                git clone -b ${env.TARGET_BRANCH} git@git.cwp.pnp-hcl.com:Team-Q/Portal-Performance-Tests.git ${workspace}/Portal-Performance-Tests
                                rm -rf ${workspace}/Portal-Performance-Tests/.git
                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'sudo mkdir -p /opt/Portal-Performance-Tests && sudo chown centos: /opt/Portal-Performance-Tests'
                                scp -r -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/Portal-Performance-Tests centos@${env.INSTANCE_IP}:/opt
                               """
                    }
                }
            }
        }
        /*
         * Once scripts are copied to EC2 instance, we need to run the JMeter scripts for upload assets.
         */
        stage('Run JMeter performance tests for upload assets') {
            options {
                timeout(time: 90, unit: 'MINUTES')
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                       // sleep 1200
                        def nowDate = new SimpleDateFormat('M-d-Y-HHmmss')
                        def date = new Date()
                        Exception caughtException = null
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo 'Running tests'
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && chmod 400 ./keys/Performance-Test-Keypair.pem'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && scp -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem -r jmeter/DAM_EXIM_Tests centos@performance-test-jmeter-master.team-q-dev.com:~/data/dam/DAM_EXIM_Tests'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \'jmeter -JProtocol=${env.SERVER_PROTOCOL} -JHost=${env.SERVER_HOST_SOURCE} -JPort=${env.SERVER_PORT} -JRingAPIPort='' -JDAMPort='' -JImagePath=${env.SERVER_IMAGEPATH} -JCSVPath=${env.SERVER_CSVPATH} -j jmeter.save.saveservice.output_format=xml -n -t /home/centos/data/dam/DAM_EXIM_Tests/${env.UPLOAD_JMX_FILE} -l /home/centos/test_logs/test_log_dam_staging_upload_assets_${nowDate.format(date)}.jtl\''
                                """
                                jmeter_error = sh(script:"ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \'grep -o -i Exception /home/centos/test_logs/test_log_dam_staging_upload_assets_${nowDate.format(date)}.jtl | wc -l\''", returnStdout: true).trim() as Integer

                                if ("${jmeter_error}" > 0) {
                                    echo "The error is ${jmeter_error}"
                                    sh 'exit 1'
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
        stage('Install dxclient and export assets from source to filesystem, validate exported assets,import assets to target k8') {
            steps {
                script {
                    dir("${WORKSPACE}") {
                        withCredentials([
                          [$class: 'UsernamePasswordMultiBinding', credentialsId: "${TOOL_CREDENTIALS_ID}", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']
              ]) {
                                sh "curl -s -u${USERNAME}:${PASSWORD} ${TOOL_PACKAGE_URL} --output dxclient.zip"
                                sh 'yes | unzip dxclient.zip'
                                sh 'ls'
              }
                    }

                    dir("${WORKSPACE}/dxclient") {
                            // pull the dxclient docker image from the given artifactory details
                            sh """
                                echo load the dxclient docker image
                                docker load < dxclient.tar.gz
                            ./bin/dxclient -h
                                ./bin/dxclient manage-dam-assets export-assets -dxProtocol ${DX_PROTOCOL} -hostname ${env.SERVER_HOST_SOURCE}  -dxPort ${DX_PORT} -dxUsername ${USERNAME} -dxPassword ${PASSWORD} -damAPIPort ${DAM_API_PORT} -ringAPIPort ${RING_API_PORT} -exportBinary ${EXPORT_BINARY}
                                ./bin/dxclient manage-dam-assets validate-assets -exportPath ${EXPORT_PATH}
                                ./bin/dxclient manage-dam-assets import-assets -dxProtocol ${DX_PROTOCOL} -hostname ${env.SERVER_HOST_TARGET} -dxPort ${DX_PORT} -dxUsername ${USERNAME} -dxPassword ${PASSWORD} -damAPIPort ${DAM_API_PORT} -ringAPIPort ${RING_API_PORT}
                            """
                    }
                }
            }
        }
         /*
         * Once subcriber sync status is successfull, we need to run the JMeter scripts to validate assets in subscriber environment.
         */
        stage('Run JMeter tests to verify assets in target environment') {
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
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && chmod 400 ./keys/Performance-Test-Keypair.pem'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && scp  -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem -r jmeter/DAM_EXIM_Tests centos@performance-test-jmeter-master.team-q-dev.com:~/data/dam/DAM_EXIM_Tests'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \'jmeter -JProtocol=${env.SERVER_PROTOCOL} -JHost=${env.SERVER_HOST_TARGET} -JPort=${env.SERVER_PORT} -JRingAPIPort='' -JDAMPort='' -JImagePath=${env.SERVER_IMAGEPATH} -JCSVPath=${env.SERVER_CSVPATH} -j jmeter.save.saveservice.output_format=xml -n -t /home/centos/data/dam/DAM_EXIM_Tests/${env.VERIFY_JMX_FILE} -l /home/centos/test_logs/test_log_dam_staging_validate_asset_${nowDate.format(date)}.jtl\''
                                """
                                jmeter_error = sh(script:"ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \'grep -o -i Exception /home/centos/test_logs/test_log_dam_staging_validate_asset_${nowDate.format(date)}.jtl | wc -l\''", returnStdout: true).trim() as Integer
                                if ("${jmeter_error}" > 0) {
                                    sh 'exit 1'
                                }
                            } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
                                error "TIMEOUT ${e}"
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
        stage('Undeploying the application in k8 source environment for exim test') {
            steps {
                script {
                    buildParams = []
                    buildParams.add(
                    string(name: 'INSTANCE_NAME', value: "${env.NAMESPACE_EXIM_SOURCE}")
                                   )
                    build(job: "${params.KUBE_UNDEPLOY_JOB}",
                          parameters: buildParams,
                          propagate: true,
                          wait: true)
                }
            }
        }
        stage('Undeploying the application in k8 target environment for exim test') {
            steps {
                script {
                    buildParams = []
                    buildParams.add(
                    string(name: 'INSTANCE_NAME', value: "${env.NAMESPACE_EXIM_TARGET}")
                                   )
                    build(job: "${params.KUBE_UNDEPLOY_JOB}",
                          parameters: buildParams,
                          propagate: true,
                          wait: true)
                }
            }
        }
    }

     /*
     * Perform proper cleanup to leave a healthy jenkins agent. On build unstable/failure/aborted/success we clean up the EC2 instance.
     */
    post {
        cleanup {
            // Destroy the EC2 instance incl. storage
            dxTerraformDestroyEc2Instance(terraformVarsEC2)
            dxWorkspaceDirectoriesCleanup()
        }
    }
}
