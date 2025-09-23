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

def SERVER_HOST_PUBLISHER
def SERVER_HOST_SUBSCRIBER

def deploymentSettings

pipeline {
  // Runs in build_infra, since we are creating infrastructure
   agent {
        label 'build_infra'
   }
   
   stages {
      // Load the pipeline parameters, common modules and configuration
        stage('Load parameters and configuration') {
            steps {
                dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/dx-dam-staging-resync-tests/parameters.yaml")
                script {
                    SERVER_HOST_PUBLISHER = "${pipelineParameters.NAMESPACE}-staging-resync-pub${pipelineParameters.DOMAIN_SUFFIX}"
                    SERVER_HOST_SUBSCRIBER = "${pipelineParameters.NAMESPACE}-staging-resync-sub${pipelineParameters.DOMAIN_SUFFIX}"

                    env.EXP_API_PUB = "https://${SERVER_HOST_PUBLISHER}/dx/api/core/v1/"
                    env.DAM_API_PUB = "https://${SERVER_HOST_PUBLISHER}/dx/api/dam/v1/"

                    env.EXP_API_SUB = "https://${SERVER_HOST_SUBSCRIBER}/dx/api/core/v1/"
                    env.DAM_API_SUB = "https://${SERVER_HOST_SUBSCRIBER}/dx/api/dam/v1/"

                    pipelineParameters.buildUser = dxJenkinsGetJobOwner()
                    println("Instance owner will be > ${pipelineParameters.buildUser} <.")

                    
                    
                    
                    

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
                    currentBuild.displayName = "${pipelineParameters.AWS_INSTANCE_NAME}_${currentDate}"
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

        // Terraform install
        stage('Install Terraform') {
            steps {
                script {
                    terraformVarsEC2.tfvar_instance_name = pipelineParameters.AWS_INSTANCE_NAME
                    terraformVarsEC2.tfvar_aws_ec2_instance_type = pipelineParameters.AWS_INSTANCE_TYPE
                    terraformVarsEC2.tfvar_instance_owner = dxJenkinsGetJobOwner()
                    terraformVarsEC2.tfvar_EXPIRATION_STAMP = pipelineParameters.ttl
                    terraformVarsEC2.instance_adm_user = "centos"
                }
                dxTerraformInstall (platform: "alma")
                echo "Terraform Installation done"
            }
        }

         /*
         * Run terraform to create an EC2 instance based on the terraform scripting and add an route53 record for it.
         */
        stage('Create EC2 Instance') {
            steps {
                script {
                    dxTerraformCustomConfig (source: 'dx-dam-staging-resync-tests/terraform/ec2-launch-alma')
                    try {
                        terraformVarsEC2 = dxTerraformCreateEc2Instance(terraformVarsEC2)
                    } catch (err) {
                        error("Creating EC2 instance failed.")
                    }
                    echo "Instance ${terraformVarsEC2.instance_private_ip} running on ${terraformVarsEC2.instance_id}."
                    env.INSTANCE_IP = terraformVarsEC2.instance_private_ip
                    println "terraformVarsEC2 = " + terraformVarsEC2
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
                        scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/dx-acceptance-tests/scripts/install-prereqs.sh centos@${terraformVarsEC2.instance_private_ip}:/tmp
                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'chmod +x /tmp/install-prereqs.sh && sh /tmp/install-prereqs.sh'
                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sudo mkdir -p /opt/dam-staging-resync-acceptance-test-reports && sudo chown centos: /opt/dam-staging-resync-acceptance-test-reports'
                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'mkdir -p /opt/dam-staging-resync-acceptance-test-reports/html && mkdir -p /opt/dam-staging-resync-acceptance-test-reports/xml'
                        scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r ${workspace}/test-scripts/TestReport/* centos@${terraformVarsEC2.instance_private_ip}:/opt/dam-staging-resync-acceptance-test-reports
                        scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r ${workspace}/dx-core-tests/dx-test-rest/helpers/Saxon-HE-9.5.1-3.jar centos@${terraformVarsEC2.instance_private_ip}:/opt/dam-staging-resync-acceptance-test-reports/Saxon-HE-9.5.1-3.jar
                    """
                }
            }
        }

        stage('Deploying the publisher application in k8 environment') {
            steps {
                script {
                    dir("${WORKSPACE}/dx-dam-staging-tests") {
                        deploymentSettings = readYaml file: "${workspace}/dx-dam-staging-resync-tests/deployment-settings.yaml"
                        echo "deploymentSettings values: ${deploymentSettings}"
                    }
                    buildParameters = []
                    buildParameters.add(string(name: 'INSTANCE_NAME', value: "${pipelineParameters.NAMESPACE}-staging-resync-pub"))
                    buildParameters.add(string(name: 'NAMESPACE', value: pipelineParameters.NAMESPACE))
                    buildParameters.add(string(name: 'DOMAIN_SUFFIX', value: pipelineParameters.DOMAIN_SUFFIX))
                    buildParameters.add(string(name: 'HOSTED_ZONE', value: pipelineParameters.HOSTED_ZONE))
                    buildParameters.add(string(name: 'BUILD_USER_ID', value: terraformVarsEC2.tfvar_instance_owner))
                    buildParameters.add(string(name: 'NEXT_JOB_DELAY_HOURS', value: '0'))
                    buildParameters.add(booleanParam(name: 'ENABLE_DAM_CLEAN_UP', value: pipelineParameters.ENABLE_DAM_CLEAN_UP))

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

        stage('Deploying the subscriber application in k8 environment') {
            steps {
                script {
                    dir("${WORKSPACE}/dx-dam-staging-tests") {
                        deploymentSettings = readYaml file: "${workspace}/dx-dam-staging-resync-tests/deployment-settings.yaml"
                        echo "deploymentSettings values: ${deploymentSettings}"
                    }
                    buildParameters = []
                    buildParameters.add(string(name: 'INSTANCE_NAME', value: "${pipelineParameters.NAMESPACE}-staging-resync-sub"))
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
         * Once scripts are copied to EC2 instance, we need to run the JMeter scripts to register subscriber. 
         */ 
        stage('Register subscriber in publisher') {
            steps {
                dir("${workspace}/dx-dam-staging-resync-tests/scripts") {
                    configFileProvider([
                        configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                    ]) {
                        script {
                            try {
                                sleep 30
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ./01-register-subscriber.sh centos@${env.INSTANCE_IP}:/tmp
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} \
                                    '(SERVER_PROTOCOL="${pipelineParameters.SERVER_PROTOCOL}" SERVER_HOST_PUBLISHER="${SERVER_HOST_PUBLISHER}" SERVER_HOST_SUBSCRIBER="${SERVER_HOST_SUBSCRIBER}" sh /tmp/01-register-subscriber.sh)'
                                """
                                echo "subscriber registerd in publisher"
                            } catch(Exception err) {
                                echo "Error: ${err}"
                                currentBuild.result = "FAILURE"
                            }
                        }
                    }
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
                                git clone -b ${pipelineParameters.TARGET_BRANCH} git@git.cwp.pnp-hcl.com:websphere-portal-incubator/media-library.git ${workspace}/media-library
                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sudo mkdir -p /opt/media-library && sudo chown centos: /opt/media-library'
                                scp -r -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/media-library centos@${terraformVarsEC2.instance_private_ip}:/opt
                            """
                        }
                    }
                }
            }
        }

        /*
         * Upload data in publisher env 
         */
        stage('Run data setup in publisher') {
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        Exception caughtException = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo "Running publisher tests"
                                echo "DAM API will be: ${env.DAM_API_PUB}"
                                echo "EXP API will be: ${env.EXP_API_PUB}"
                                echo "INSTANCE_IP will be: ${terraformVarsEC2.instance_private_ip}"
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/media-library && make scope && make install && make buildServer'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/media-library/packages/server-v1 && make install && make build && make ${pipelineParameters.TEST_COMMAND_PUBLISHER} ring_api=${env.EXP_API_PUB} dam_api=${env.DAM_API_PUB} insecure=${pipelineParameters.SSL_ENABLED}'
                                """
                                // wait till sync completed 
                                echo 'sleeping for 20 min for sync before breaking the asset in subsrciber'
                                sleep 1200
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
         * Modify data in subscriber env 
         */
        stage('Run data setup in subscriber') {
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        Exception caughtException = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                // tests
                                echo "Running subscriber tests"
                                echo "DAM API will be: ${env.DAM_API_SUB}"
                                echo "EXP API will be: ${env.EXP_API_SUB}"
                                echo "INSTANCE_IP will be: ${terraformVarsEC2.instance_private_ip}"
                                
                                // script
                                echo "subscriber name ${SERVER_HOST_SUBSCRIBER}"
                                    /* Extract PEM file */
                                sh """
                                    cp $DEPLOY_KEY test-automation-deployments.pem
                                    chmod 0600 test-automation-deployments.pem
                                """
                                echo "Copy script to native kube instance "
                                /* Copy scripts to EC2 instance */
                                sh """
                                    scp -v -i test-automation-deployments.pem -o StrictHostKeyChecking=no ${workspace}/dx-dam-staging-resync-tests/scripts/break-assets-subscriber.sh centos@${SERVER_HOST_SUBSCRIBER}:/home/centos/native-kube/
                                """
                                echo "Execute script on native kube instance "
                                /* Run bash script to capture operations time */
                                sh  """
                                    ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${SERVER_HOST_SUBSCRIBER} \
                                    '(sh /home/centos/native-kube/./break-assets-subscriber.sh ${env.NAMESPACE})'
                                """
                                echo "Execution of script completed "
                                /* Run acceptance tests in subscriber env */
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/media-library && make scope && make install && make buildServer'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/media-library/packages/server-v1 && make install && make build && make ${pipelineParameters.TEST_COMMAND_SUBSCRIBER} ring_api=${env.EXP_API_SUB} dam_api=${env.DAM_API_SUB} insecure=${pipelineParameters.SSL_ENABLED}'
                                """
                                sleep 120
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
         * Trigger find mismatch for subscriber 
         */
        stage('Trigger find mismatch for subscriber') {
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        Exception caughtException = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo "Triggering find mismatch start"
                                echo "DAM API will be: ${env.DAM_API_PUB}"
                                echo "EXP API will be: ${env.EXP_API_PUB}"
                                echo "INSTANCE_IP will be: ${terraformVarsEC2.instance_private_ip}"
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/media-library && make scope && make install && make buildServer'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/media-library/packages/server-v1 && make install && make build && make ${pipelineParameters.TEST_COMMAND_FIND_MISMATCH_START} ring_api=${env.EXP_API_PUB} dam_api=${env.DAM_API_PUB} insecure=${pipelineParameters.SSL_ENABLED}'
                                """
                                sleep 300
                                echo 'After Triggering find mismatch start'
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
         * Verify mismatch logs 
         */
        stage('Run scripts to verify mismatch logs') {
            steps {
                dir("${workspace}/dx-dam-staging-resync-tests/scripts") {
                    configFileProvider([
                        configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                    ]) {
                        script {
                            Exception caughtException = null;
                            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                                try {
                                    echo "Namespace is : ${env.NAMESPACE}"
                                        /* Extract PEM file */
                                    sh """
                                        cp $DEPLOY_KEY test-automation-deployments.pem
                                        chmod 0600 test-automation-deployments.pem
                                    """
                                    echo "Copy script to native kube instance "
                                    /* Copy scripts to EC2 instance */
                                    sh """
                                        scp -v -i test-automation-deployments.pem -o StrictHostKeyChecking=no ./test-mismatch-logs.sh centos@${SERVER_HOST_PUBLISHER}:/home/centos/native-kube/
                                    """
                                    echo "Execute script on native kube instance "
                                    /* Run bash script to verify mismatch logs */
                                    sh  """
                                        ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${SERVER_HOST_PUBLISHER} \
                                        '(sh /home/centos/native-kube/test-mismatch-logs.sh ${env.NAMESPACE})'
                                    """
                                    echo "Execution of script completed "
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

        /*
         * Start resync for subscriber 
         */
        stage('Start resync for subscriber') {
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        Exception caughtException = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo "Start resync"
                                echo "DAM API will be: ${env.DAM_API_PUB}"
                                echo "EXP API will be: ${env.EXP_API_PUB}"
                                echo "INSTANCE_IP will be: ${terraformVarsEC2.instance_private_ip}"
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/media-library && make scope && make install && make buildServer'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/media-library/packages/server-v1 && make install && make build && make ${pipelineParameters.TEST_COMMAND_START_RESYNC} ring_api=${env.EXP_API_PUB} dam_api=${env.DAM_API_PUB} insecure=${pipelineParameters.SSL_ENABLED}'
                                """
                                sleep 300
                                echo 'After resync start'
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
         * Trigger find mismatch for subscriber to verify resync
         */
        stage('Trigger find mismatch for subscriber to verify resync') {
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        Exception caughtException = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo "Triggering find mismatch start to verify resync"
                                echo "DAM API will be: ${env.DAM_API_PUB}"
                                echo "EXP API will be: ${env.EXP_API_PUB}"
                                echo "INSTANCE_IP will be: ${terraformVarsEC2.instance_private_ip}"
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/media-library && make scope && make install && make buildServer'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/media-library/packages/server-v1 && make install && make build && make ${pipelineParameters.TEST_COMMAND_FIND_MISMATCH_START} ring_api=${env.EXP_API_PUB} dam_api=${env.DAM_API_PUB} insecure=${pipelineParameters.SSL_ENABLED}'
                                """
                                sleep 300
                                echo 'After Triggering find mismatch start'
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
         * Verify resync 
         */
        stage('verify resync') {
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        Exception caughtException = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo "verify resync"
                                echo "DAM API will be: ${env.DAM_API_PUB}"
                                echo "EXP API will be: ${env.EXP_API_PUB}"
                                echo "INSTANCE_IP will be: ${terraformVarsEC2.instance_private_ip}"
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/media-library && make scope && make install && make buildServer'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/media-library/packages/server-v1 && make install && make build && make ${pipelineParameters.TEST_COMMAND_VERIFY_RESYNC} ring_api=${env.EXP_API_PUB} dam_api=${env.DAM_API_PUB} insecure=${pipelineParameters.SSL_ENABLED}'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cp /opt/media-library/packages/server-v1/test-report/STAGING_RESYNC.html /opt/dam-staging-resync-acceptance-test-reports/html/ 2>/dev/null || : && cp /opt/media-library/packages/server-v1/test-report/STAGING_RESYNC.xml /opt/dam-staging-resync-acceptance-test-reports/xml/ 2>/dev/null || :'
                                """
                                echo 'Verified resync'
                            } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
                                error "TIMEOUT ${e.toString()}"
                            } catch (Throwable e) {
                                caughtException = e;
                            }
                            if (caughtException) {
                                sh """
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cp /opt/media-library/packages/server-v1/test-report/STAGING_RESYNC.html /opt/dam-staging-resync-acceptance-test-reports/html/ 2>/dev/null || : && cp /opt/media-library/packages/server-v1/test-report/STAGING_RESYNC.xml /opt/dam-staging-resync-acceptance-test-reports/xml/ 2>/dev/null || :'
                                """
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
                                        echo "Generating test dam-staging-resync-acceptance-dashboard report"
                                        sh """
                                            chmod 600 ${DEPLOY_KEY}
                                            aws s3 cp s3://dx-testarea/dam-staging-resync-acceptance-test-reports/dam-staging-resync-acceptance-test-runs.xml dam-staging-resync-acceptance-test-runs.xml
                                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/dam-staging-resync-acceptance-test-runs.xml centos@${terraformVarsEC2.instance_private_ip}:/opt/dam-staging-resync-acceptance-test-reports/
                                            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/dam-staging-resync-acceptance-test-reports && source ~/.bash_profile && chmod +x /opt/dam-staging-resync-acceptance-test-reports/dam-staging-resync-acceptance-test-master-report.sh && sh /opt/dam-staging-resync-acceptance-test-reports/dam-staging-resync-acceptance-test-master-report.sh snapshotDir="https://dxcore-test-results-secure.team-q-dev.com/DX-95-TestReports/${pipelineParameters.reportBuildname}_acceptance-test/html" && tar -czf ${pipelineParameters.reportBuildname}_acceptance-test.zip Master-Report.html wtf.css html/*'
                                            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/dam-staging-resync-acceptance-test-reports/ && chmod +x dam-staging-resync-acceptance-test-run.sh && sh dam-staging-resync-acceptance-test-run.sh ${pipelineParameters.reportBuildname}_acceptance-test Dam-Staging-Resync-Acceptance'
                                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r centos@${terraformVarsEC2.instance_private_ip}:/opt/dam-staging-resync-acceptance-test-reports/dam-staging-resync-acceptance-test-runs.xml .
                                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r centos@${terraformVarsEC2.instance_private_ip}:/opt/dam-staging-resync-acceptance-test-reports/dashboard/* .
                                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r centos@${terraformVarsEC2.instance_private_ip}:/opt/dam-staging-resync-acceptance-test-reports/${pipelineParameters.reportBuildname}_acceptance-test.zip .
                                            aws s3 cp ${pipelineParameters.reportBuildname}_acceptance-test.zip s3://dx-testarea/
                                            aws s3 cp dam-staging-resync-acceptance-test-runs.xml s3://dx-testarea/dam-staging-resync-acceptance-test-reports/dam-staging-resync-acceptance-test-runs.xml
                                            aws s3 cp Dam-Staging-Resync-Acceptance-dashboard.html s3://dx-testarea/dam-staging-resync-acceptance-test-reports/Dam-Staging-Resync-Acceptance-dashboard.html
                                            aws s3 cp wtf.css s3://dx-testarea/dam-staging-resync-acceptance-test-reports/wtf.css
                                        """
                                    } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
                                        error "TIMEOUT ${e.toString()}"
                                    } catch (Throwable e) {
                                        caughtException = e;
                                    }
                                    if (caughtException) {

                                         sh """
                                            chmod 600 ${DEPLOY_KEY}
                                            aws s3 cp s3://dx-testarea/dam-staging-resync-acceptance-test-reports/dam-staging-resync-acceptance-test-runs.xml dam-staging-resync-acceptance-test-runs.xml
                                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/dam-staging-resync-acceptance-test-runs.xml centos@${terraformVarsEC2.instance_private_ip}:/opt/dam-staging-resync-acceptance-test-reports/
                                            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/dam-staging-resync-acceptance-test-reports/ && chmod +x dam-staging-resync-acceptance-test-failure-xml.sh && sh dam-staging-resync-acceptance-test-failure-xml.sh ${pipelineParameters.reportBuildname}_acceptance-test ${env.BUILD_URL}'
                                            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/dam-staging-resync-acceptance-test-reports/ && chmod +x dam-staging-resync-acceptance-failure-test-run.sh && sh dam-staging-resync-acceptance-failure-test-run.sh ${pipelineParameters.reportBuildname}_acceptance-test Dam-Staging-Resync-Acceptance'
                                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r centos@${terraformVarsEC2.instance_private_ip}:/opt/dam-staging-resync-acceptance-test-reports/dam-staging-resync-acceptance-test-runs.xml .
                                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r centos@${terraformVarsEC2.instance_private_ip}:/opt/dam-staging-resync-acceptance-test-reports/dashboard/* .
                                            aws s3 cp dam-staging-resync-acceptance-test-runs.xml s3://dx-testarea/dam-staging-resync-acceptance-test-reports/dam-staging-resync-acceptance-test-runs.xml
                                            aws s3 cp Dam-Staging-Resync-Acceptance-dashboard.html s3://dx-testarea/dam-staging-resync-acceptance-test-reports/Dam-Staging-Resync-Acceptance-dashboard.html
                                            aws s3 cp wtf.css s3://dx-testarea/dam-staging-resync-acceptance-test-reports/wtf.css
                                        """
                                        error caughtException.message
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        stage('Undeploy the staging publisher application in k8 environment') {
            steps {
                script {
                    buildParams = []
                    buildParams.add(
                        [$class: 'StringParameterValue',
                            name: 'INSTANCE_NAME',
                            value: "${pipelineParameters.NAMESPACE}-staging-resync-pub"])
                    build(job: "${pipelineParameters.KUBE_UNDEPLOY_JOB}", 
                          parameters: buildParams, 
                          propagate: true,
                          wait: true)
                }
            }
        }

        stage('Undeploy the staging subscriber application in k8 environment') {
            steps {
                script {
                    buildParams = []
                     buildParams.add(
                        [$class: 'StringParameterValue',
                            name: 'INSTANCE_NAME',
                            value: "${pipelineParameters.NAMESPACE}-staging-resync-sub"])
                    build(job: "${pipelineParameters.KUBE_UNDEPLOY_JOB}", 
                          parameters: buildParams, 
                          propagate: true,
                          wait: true)
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