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

import java.text.SimpleDateFormat

@Library('dx-shared-library') _
// test map for creating a new EC2 instance
def terraformVarsEC2 = [:]
def pipelineParameters = [:]

def deploymentTargetBranch
def deploymentTargetRepo
def deploymentSettings
def SERVER_SOURCE
def SERVER_TARGET
def SERVER_HOST_SOURCE
def SERVER_HOST_TARGET

pipeline {
    agent {
        label 'build_infra'
    }

    stages {
        stage('Load modules and configuration') {
            steps {
                script {
                    // load params from yaml file
                    dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/dx-dam-exim-tests/parameters.yaml")
                    SERVER_SOURCE = "${pipelineParameters.INSTANCE_NAME}-source"
                    SERVER_TARGET = "${pipelineParameters.INSTANCE_NAME}-target"
                    SERVER_HOST_SOURCE = "${SERVER_SOURCE}${DOMAIN_SUFFIX}"
                    SERVER_HOST_TARGET = "${SERVER_TARGET}${DOMAIN_SUFFIX}"
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
                     
                        INSTANCE_OWNER = dxJenkinsGetJobOwner()
                        terraformVarsEC2.tfvar_instance_owner = "${INSTANCE_OWNER}"
                        terraformVarsEC2.tfvar_instance_name = 'exim_test'

                        // We are doing DAM bulk upload EXIM performance tests we are using large ec2 instance
                        terraformVarsEC2.tfvar_aws_ec2_instance_type = pipelineParameters.EC2_INSTANCE_TYPE

                        echo 'Assigning hostname + timestamp'
                        terraformVarsEC2.tfvar_instance_name= "${terraformVarsEC2.tfvar_instance_name}_${dateFormat.format(date)}"
                        echo "New instance will be: ${terraformVarsEC2.tfvar_instance_name}"

                        // Display name includes the env.INSTANCE_NAME and a timestamp
                        currentBuild.displayName = "${terraformVarsEC2.tfvar_instance_name}"

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

        stage('Undeploying the DAM EXIM Source application in k8 environment') {
            steps {
                script {
                    buildParameters = []
                    buildParameters.add(string(name: 'INSTANCE_NAME', value: "${SERVER_SOURCE}"))
                    buildParameters.add(string(name: 'KUBE_FLAVOUR', value: pipelineParameters.KUBE_FLAVOUR))
                    build(job: "${pipelineParameters.KUBE_UNDEPLOY_JOB}",
                          parameters: buildParameters,
                          propagate: true,
                          wait: true)
                }
            }
        }

        stage('Undeploying the DAM EXIM Target application in k8 environment') {
            steps {
                script {
                    buildParameters = []
                    buildParameters.add(string(name: 'INSTANCE_NAME', value: "${SERVER_TARGET}"))
                    buildParameters.add(string(name: 'KUBE_FLAVOUR', value: pipelineParameters.KUBE_FLAVOUR))
                    build(job: "${pipelineParameters.KUBE_UNDEPLOY_JOB}",
                          parameters: buildParameters,
                          propagate: true,
                          wait: true)
                }
            }
        }

        stage('Deploying the DAM EXIM Source application in k8 environment') {
            steps {
                script {
                    dir("${WORKSPACE}/dx-dam-staging-tests") {
                        deploymentSettings = readYaml file: "${workspace}/dx-dam-exim-tests/deployment-settings.yaml"
                        echo "deploymentSettings values: ${deploymentSettings}"
                    }
                    buildParameters = []
                    buildParameters.add(string(name: 'INSTANCE_NAME', value: "${SERVER_SOURCE}"))
                    buildParameters.add(string(name: 'NAMESPACE', value: "dxns"))
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
                    buildParameters.add(string(name: 'HELM_CHARTS_FILTER', value: pipelineParameters.HELM_IMAGE_FILTER))

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

        stage('Deploying the DAM EXIM Target application in k8 environment') {
            steps {
                script {
                    dir("${WORKSPACE}/dx-dam-staging-tests") {
                        deploymentSettings = readYaml file: "${workspace}/dx-dam-staging-tests/deployment-settings.yaml"
                        echo "deploymentSettings values: ${deploymentSettings}"
                    }
                    buildParameters = []
                    buildParameters.add(string(name: 'INSTANCE_NAME', value: "${SERVER_TARGET}"))
                    buildParameters.add(string(name: 'NAMESPACE', value: "dxns"))
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
                    buildParameters.add(string(name: 'HELM_CHARTS_FILTER', value: pipelineParameters.HELM_IMAGE_FILTER))

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
                        scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/dx-dam-exim-tests/helpers/01-setup-prereqs.sh centos@${terraformVarsEC2.instance_private_ip}:/tmp
                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'chmod +x /tmp/01-setup-prereqs.sh && sh /tmp/01-setup-prereqs.sh'
                    """
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
                                git clone -b ${pipelineParameters.JMETER_BRANCH} git@git.cwp.pnp-hcl.com:Team-Q/Portal-Performance-Tests.git ${workspace}/Portal-Performance-Tests
                                rm -rf ${workspace}/Portal-Performance-Tests/.git
                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sudo mkdir -p /opt/Portal-Performance-Tests && sudo chown centos: /opt/Portal-Performance-Tests'
                                scp -r -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/Portal-Performance-Tests centos@${terraformVarsEC2.instance_private_ip}:/opt
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
                          catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                            dxRemoteInstanceCheckHttpStatus(url: "https://${SERVER_HOST_SOURCE}:443/dx/api/dam/v1", lookupInterval: 30, lookupTries: 50)
                            dxRemoteInstanceCheckHttpStatus(url: "https://${SERVER_HOST_SOURCE}:443/dx/api/core/v1", lookupInterval: 30, lookupTries: 60)
                          }
                        def nowDate = new SimpleDateFormat('M-d-Y-HHmmss')
                        def date = new Date()
                        Exception caughtException = null
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo 'Running tests'
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && chmod 400 ./keys/Performance-Test-Keypair.pem'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && scp -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem -r jmeter/DAM_EXIM_Tests centos@performance-test-jmeter-master.team-q-dev.com:~/data/dam/'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \'env JVM_ARGS="-Dnashorn.args=--no-deprecation-warning" jmeter -JProtocol=${pipelineParameters.SERVER_PROTOCOL} -JHost=${SERVER_HOST_SOURCE} -JPort=${pipelineParameters.SERVER_PORT} -JRingAPIPort='' -JDAMPort='' -JDocumentsPath=/home/centos/data/dam/DAMRegression/Dataset/documents -JImagesPath=/home/centos/data/dam/DAMRegression/Dataset/images -JVideosPath=/home/centos/data/dam/DAMRegression/Dataset/videos -JCSVPath=/home/centos/data/dam/DAMRegression/Dataset/dam_users.csv -j jmeter.save.saveservice.output_format=xml -n -t /home/centos/data/dam/DAM_EXIM_Tests/${pipelineParameters.UPLOAD_JMX_FILE} -l /home/centos/test_logs/test_log_dam_exim_upload_assets_${nowDate.format(date)}.jtl -JVUSERS_PER_TYPE=${pipelineParameters.VUSERS_PER_TYPE} -JTEST_AUTHENTICATED_VUSER_RAMP=${pipelineParameters.TEST_AUTHENTICATED_VUSER_RAMP} -JLOOP_COUNT=${pipelineParameters.LOOP_COUNT}\''
                                """
                                jmeter_error = sh(script:"ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \'grep -o -i Exception /home/centos/test_logs/test_log_dam_exim_upload_assets_${nowDate.format(date)}.jtl | wc -l\''", returnStdout: true).trim() as Integer

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

        /*
         * Once JMeter scripts are executed then we will run the below stage to capture operations time for uploaded assets. 
         */ 
        stage('Run scripts to capture operations time in source environment') {
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
                                    scp -v -i test-automation-deployments.pem -o StrictHostKeyChecking=no ./capture-dam-operations-time.sh centos@${SERVER_HOST_SOURCE}:/home/centos/native-kube/
                                """

                                /* Run bash script to capture operations time */
                                sh  """
                                    ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${SERVER_HOST_SOURCE} \
                                    '(sh /home/centos/native-kube/capture-dam-operations-time.sh "" "" 600)'
                                """

                                 /* Copy performane results log from EC2 instance */
                                sh """
                                    scp -v -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${SERVER_HOST_SOURCE}:/home/centos/native-kube/dam_performance_results.log ${workspace}/dx-dam-exim-tests/
                                    cat ${workspace}/dx-dam-exim-tests/dam_performance_results.log
                                """
                            } catch(Exception err) {
                                echo "Error: ${err}"
                                currentBuild.result = "FAILURE"
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    cp $DEPLOY_KEY test-automation-deployments.pem
                                    chmod 0600 test-automation-deployments.pem
                                    ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${SERVER_HOST_SOURCE} 'printf "Failed," >> /home/centos/native-kube/dam_performance_results.log'
                                """
                            }
                        }
                    }
                }
            }
        }

        stage('Install dxclient and export assets from source to filesystem, validate exported assets,import assets to target k8') {
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

                                EXPORT_START_TIME=\$(date +%s)
                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /home/centos/dxclient && ./bin/dxclient manage-dam-assets export-assets -dxProtocol ${pipelineParameters.DX_PROTOCOL} -hostname ${SERVER_HOST_SOURCE}  -dxPort ${pipelineParameters.DX_PORT} -dxUsername ${pipelineParameters.USERNAME} -dxPassword ${pipelineParameters.PASSWORD} -damAPIPort ${pipelineParameters.DAM_API_PORT} -ringAPIPort ${pipelineParameters.RING_API_PORT} -exportBinary ${pipelineParameters.EXPORT_BINARY}'
                                EXPORT_END_TIME=\$(date +%s)
                                TOTAL_EXPORT_TIME_IN_SECS=\$((\$EXPORT_END_TIME - \$EXPORT_START_TIME))
                                printf 'Total Export Time -  %dh:%dm:%ds\t' \$((\$TOTAL_EXPORT_TIME_IN_SECS/3600)) \$((\$TOTAL_EXPORT_TIME_IN_SECS%3600/60)) \$((\$TOTAL_EXPORT_TIME_IN_SECS%60))
                              
                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /home/centos/dxclient && ./bin/dxclient manage-dam-assets validate-assets -exportPath ${pipelineParameters.EXPORT_PATH}'

                                IMPORT_START_TIME=\$(date +%s)
                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /home/centos/dxclient && ./bin/dxclient manage-dam-assets import-assets -dxProtocol ${pipelineParameters.DX_PROTOCOL} -hostname ${SERVER_HOST_TARGET} -dxPort ${pipelineParameters.DX_PORT} -dxUsername ${pipelineParameters.USERNAME} -dxPassword ${pipelineParameters.PASSWORD} -damAPIPort ${pipelineParameters.DAM_API_PORT} -ringAPIPort ${pipelineParameters.RING_API_PORT}'
                                IMPORT_END_TIME=\$(date +%s)
                                TOTAL_IMPORT_TIME_IN_SECS=\$((\$IMPORT_END_TIME - \$IMPORT_START_TIME))
                                printf ' Total Import Time - %dh:%dm:%ds\t' \$((\$TOTAL_IMPORT_TIME_IN_SECS/3600)) \$((\$TOTAL_IMPORT_TIME_IN_SECS%3600/60)) \$((\$TOTAL_IMPORT_TIME_IN_SECS%60))
                        """
              }
                    }
                }
                }
            }
        }

        /*
         * Once JMeter scripts are executed then we will run the below stage to check DAM operations for synced assets in target. 
         */ 
        stage('Run scripts to capture operations time in target environment') {
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
                                    scp -v -i test-automation-deployments.pem -o StrictHostKeyChecking=no ./capture-dam-operations-time.sh centos@${SERVER_HOST_TARGET}:/home/centos/native-kube/
                                """

                                /* Run bash script to capture operations time */
                                sh  """
                                    ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${SERVER_HOST_TARGET} \
                                    '(sh /home/centos/native-kube/capture-dam-operations-time.sh)'
                                """

                                 /* Copy performane results log from EC2 instance */
                                sh """
                                    scp -v -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${SERVER_HOST_TARGET}:/home/centos/native-kube/dam_performance_results.log ${workspace}/dx-dam-exim-tests/
                                    cat ${workspace}/dx-dam-exim-tests/dam_performance_results.log
                                """
                            } catch(Exception err) {
                                echo "Error: ${err}"
                                currentBuild.result = "FAILURE"
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    cp $DEPLOY_KEY test-automation-deployments.pem
                                    chmod 0600 test-automation-deployments.pem
                                    ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${SERVER_HOST_TARGET} 'printf "Failed," >> /home/centos/native-kube/dam_performance_results.log'
                                """
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
            // Destroy the EC2 instance incl. storage
            dxTerraformDestroyEc2Instance(terraformVarsEC2)
            dxWorkspaceDirectoriesCleanup()
        }
    }
}
