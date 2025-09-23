/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2022, 2023. All Rights Reserved. *
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

// Create an object to store dedicated host configuration values
def dedicatedHostParameters = [:]

def deploymentTargetBranch
def deploymentTargetRepo
def deploymentSettings
def SERVER_SOURCE
def SERVER_HOST_SOURCE
def REPORT_BUILD_NAME
DEDICATED_HOST_ID_VALUE = ''

def CUSTOM_VALUES_OVERRIDE = """
applications:
  contentComposer: false
  core: true
  damPluginGoogleVision: false
  damPluginKaltura: false
  digitalAssetManagement: true
  dxPicker: true
  haproxy: true
  imageProcessor: false
  licenseManager: false
  openLdap: false
  persistence: false
  remoteSearch: false
  ringApi: true
  runtimeController: false
resources:
  # Core resource allocation
  core:
    requests:
      cpu: "3000m"
      memory: "5000Mi"
    limits:
      cpu: "3000m"
      memory: "5000Mi"
  # Ring API resource allocation
  ringApi:
    requests:
      cpu: "500m"
      memory: "512Mi"
    limits:
      cpu: "500m"
      memory: "512Mi"
  # HAProxy resource allocation
  haproxy:
    requests:
      cpu: "700m"
      memory: "1024Mi"
    limits:
      cpu: "700m"
      memory: "1024Mi"
 # Digital asset management resource allocation
  digitalAssetManagement:
    requests:
      cpu: "1000m"
      memory: "1536Mi"
    limits:
      cpu: "1000m"
      memory: "1536Mi"
"""


// Create object to store parameters with values
def pipelineParameters = [:]

// function to look up for available dedicated hosts
def dedicatedHostsLookUp(hostCapacityMap) {
    // check for available dedicated hosts with greater available capacity
    def hostWithGreaterCapacity = ''
    def greaterCapacity = 0

    hostCapacityMap.each { key, value ->
                if (key.startsWith('availableCapacity-')) {
            def capacity = value
            if (capacity > greaterCapacity) {
                greaterCapacity = capacity
                def hostKey = key.replace('availableCapacity-', 'hostID-')
                hostWithGreaterCapacity = hostCapacityMap[hostKey]
            }
                }
    }
    // if no available capacity in hosts will return false to stop the job
    if (greaterCapacity == 0) {
        println 'Both hosts have zero available capacity'
        return false
    }
    // assign the host-id with greater capacity
    println "Host-id with greater capacity: $hostWithGreaterCapacity"
    DEDICATED_HOST_ID_VALUE = hostWithGreaterCapacity
    return true
}

pipeline {
    agent {
        label 'build_infra'
    }

    stages {
        // Load the pipeline parameters into object
        stage('Load parameters') {
            steps {
                dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/dx-dam-regression/parameters.yaml")
            }
        }

        // Prepare settings
        stage('Prepare Settings') {
            steps {
                /*
                 * Assigning hostname using Performace-test-automation + the timestap
                 */
                dir("${WORKSPACE}/dx-dam-regression") {
                    script {
                        // determine build version and label current job accordingly
                        def dateFormat = new SimpleDateFormat('yyyyMMdd-HHmmss')
                        def date = new Date()

                        // short and long strings append to host names based on render duration from params
                        if (pipelineParameters.RENDER_TEST_DURATION == '3600') {
                            SERVER_SOURCE = "${pipelineParameters.INSTANCE_NAME}-short-${pipelineParameters.TARGET_BRANCH}"
                        } else if (pipelineParameters.RENDER_TEST_DURATION == '172800') {
                            SERVER_SOURCE = "${pipelineParameters.INSTANCE_NAME}-long-${pipelineParameters.TARGET_BRANCH}"
                        } else {
                            SERVER_SOURCE = "${pipelineParameters.INSTANCE_NAME}-${pipelineParameters.TARGET_BRANCH}"
                        }

                        SERVER_HOST_SOURCE = "${SERVER_SOURCE}${pipelineParameters.DOMAIN_SUFFIX}"

                        if ("${SERVER_HOST_SOURCE}") {
                            def currentDate = "${dateFormat.format(date)}"
                            currentBuild.displayName = "${SERVER_HOST_SOURCE}_${currentDate}"
                            def removeHTTPFromDisplayName = currentBuild.displayName.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)", '')
                            def tempReportBuildName = removeHTTPFromDisplayName.split('/')
                            def removeSpecialCharacter = tempReportBuildName[0].replace(':', '')
                            if (removeHTTPFromDisplayName.contains('/')) {
                                tempReportBuildName = "${tempReportBuildName[0]}_${currentDate}"
                                removeSpecialCharacter = tempReportBuildName.replace(':', '')
                            }
                            REPORT_BUILD_NAME = "${removeSpecialCharacter}"
                            echo "REPORT_BUILD_NAME ${REPORT_BUILD_NAME}"
                        }

                        // instance type
                        terraformVarsEC2.tfvar_aws_ec2_instance_type = pipelineParameters.EC2_INSTANCE_TYPE

                        // instance area = PERF
                        terraformVarsEC2.tfvar_instance_area = pipelineParameters.INSTANCE_AREA

                        // Determine owner of EC2 instance
                        INSTANCE_OWNER = dxJenkinsGetJobOwner()
                        terraformVarsEC2.tfvar_instance_owner = "${INSTANCE_OWNER}"

                        terraformVarsEC2.tfvar_instance_name = 'dx-dam-regression'
                        echo 'Assigning hostname + timestamp'
                        terraformVarsEC2.tfvar_instance_name = "${terraformVarsEC2.tfvar_instance_name}_${dateFormat.format(date)}"
                        echo "New instance will be: ${terraformVarsEC2.tfvar_instance_name}"

                        // subnet id
                        terraformVarsEC2.tfvar_aws_subnet = pipelineParameters.AWS_SUBNET_ID
                        echo "AWS Subnet name : ${pipelineParameters.AWS_SUBNET_NAME}"
                        echo "AWS Subnet ID : ${terraformVarsEC2.tfvar_aws_subnet}"

                        // get target repo for artifactory
                        if (pipelineParameters.TARGET_BRANCH.contains('release')) {
                            deploymentTargetBranch = pipelineParameters.TARGET_BRANCH
                            deploymentTargetRepo = 'quintana-docker-prod'
                        } else {
                            deploymentTargetBranch = 'develop'
                            deploymentTargetRepo = 'quintana-docker'
                        }
                        // check if CUSTOM_VALUES_OVERRIDE provided in params or else use small config helm values to override for performance tests
                        if(pipelineParameters.CUSTOM_VALUES_OVERRIDE == ""){
                            pipelineParameters.CUSTOM_VALUES_OVERRIDE = CUSTOM_VALUES_OVERRIDE;
                        }
                    }
                }
            }
        }

        // Terraform install
        stage('Install Terraform') {
            steps {
                script {
                    dxTerraformInstall()
                    echo 'Terraform Installation done'
                }
            }
        }

        stage('Look up for available dedicated hosts') {
            when {
                expression { pipelineParameters.USE_NON_DEDICATED_INSTANCE == "false" }
            }
            steps {
                script {
                    try {
                        dedicatedHostParameters.tfvar_dedicated_host_owner = dxJenkinsGetJobOwner(defaultOwner: 'philipp.milich@hcl.com')
                        dedicatedHostParameters.tfvar_dedicated_host_availability_zone = pipelineParameters.AVAILABILITY_ZONE
                        dedicatedHostParameters.tfvar_aws_region = pipelineParameters.AWS_REGION
                        dedicatedHostParameters.tfvar_aws_dedicated_host_instance_family =  pipelineParameters.INSTANCE_FAMILY
                        dedicatedHostParameters.tfvar_aws_ec2_instance_type  =  pipelineParameters.NATIVE_KUBE_INSTANCE_TYPE
                        def result = dxDedicatedHostsLookUp(dedicatedHostParameters)
                        boolean hostLookup = dedicatedHostsLookUp(result)
                        if (hostLookup) {
                               // adding dedicated host-id for jmeter instance
                            println "Dedicated host-id: $DEDICATED_HOST_ID_VALUE"
                            terraformVarsEC2.tfvar_dedicated_host_id = DEDICATED_HOST_ID_VALUE
                            println "Dedicated host-id: $DEDICATED_HOST_ID_VALUE"
                        } else {
                            throw new Exception('Dedicated Hosts not available!')
                        }
                   } catch (Exception err) {
                        echo "Error: ${err}"
                        currentBuild.result = 'FAILURE'
                    }
                }
            }
        }

        // Launch the EC2 instance with our target parameters
        stage('Create EC2 Instance') {
            steps {
                script {
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
                withCredentials([
                    usernamePassword(credentialsId: 'artifactory', passwordVariable: 'ARTIFACTORY_PASSWORD', usernameVariable: 'ARTIFACTORY_USER')
                  ]) {
                    configFileProvider([
                          configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                      ]) {
                        sh """
                            chmod 600 ${DEPLOY_KEY}
                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/dx-dam-regression/scripts/install-prereqs.sh centos@${terraformVarsEC2.instance_private_ip}:/tmp
                            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'chmod +x /tmp/install-prereqs.sh  && sh /tmp/install-prereqs.sh ${pipelineParameters.JMETER_BINARY_VERSION}'
                            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sudo mkdir -p /opt/dam-performance-test-reports && sudo chown centos: /opt/dam-performance-test-reports'
                            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sudo mkdir -p /home/centos/test_logs && sudo chown centos: /home/centos/test_logs'
                            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sudo mkdir -p /home/centos/dam_AnonymousRendering && sudo chown centos: /home/centos/dam_AnonymousRendering'
                            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'mkdir -p /opt/dam-performance-test-reports/html && mkdir -p /opt/dam-performance-test-reports/xml'
                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r ${workspace}/test-scripts/TestReport/* centos@${terraformVarsEC2.instance_private_ip}:/opt/dam-performance-test-reports
                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r ${workspace}/dx-core-tests/dx-test-rest/helpers/Saxon-HE-9.5.1-3.jar centos@${terraformVarsEC2.instance_private_ip}:/opt/dam-performance-test-reports/Saxon-HE-9.5.1-3.jar
                          """
                      }
                  }
            }
        }

        // Undeploy if same namespace exists
        stage('Undeploying the application in k8 environment for regression test') {
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

        // Deploy native-kube environment
        stage('Deploying the application in k8 environment for regression test') {
            steps {
                script {
                    dir("${WORKSPACE}/dx-dam-regression") {
                        deploymentSettings = readYaml file: "${workspace}/dx-dam-regression/deployment-settings.yaml"
                        echo "deploymentSettings values: ${deploymentSettings}"
                    }
                    buildParameters = []
                    buildParameters.add(string(name: 'INSTANCE_NAME', value: "${SERVER_SOURCE}"))
                    buildParameters.add(string(name: 'NAMESPACE', value: 'dxns'))
                    buildParameters.add(string(name: 'DOMAIN_SUFFIX', value: pipelineParameters.DOMAIN_SUFFIX))
                    buildParameters.add(string(name: 'HOSTED_ZONE', value: pipelineParameters.HOSTED_ZONE))
                    buildParameters.add(string(name: 'BUILD_USER_ID', value: terraformVarsEC2.tfvar_instance_owner))
                    buildParameters.add(string(name: 'NEXT_JOB_DELAY_HOURS', value: pipelineParameters.NEXT_JOB_DELAY_HOURS))
                    buildParameters.add(string(name: 'AWS_SUBNET_NAME', value: pipelineParameters.AWS_SUBNET_NAME))

                    buildParameters.add(string(name: 'IMAGE_REPOSITORY', value: deploymentTargetRepo))
                    buildParameters.add(string(name: 'CORE_IMAGE_FILTER', value: pipelineParameters.CORE_IMAGE_FILTER))
                    buildParameters.add(string(name: 'DAM_IMAGE_FILTER', value: pipelineParameters.DAM_IMAGE_FILTER))
                    buildParameters.add(string(name: 'CC_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'DESIGN_STUDIO_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'DAM_PLUGIN_GOOGLE_VISION_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'RINGAPI_IMAGE_FILTER', value: pipelineParameters.RING_API_IMAGE_FILTER))
                    buildParameters.add(string(name: 'IMGPROC_IMAGE_FILTER', value: pipelineParameters.IMAGE_PROCESSOR_FILTER))
                    buildParameters.add(string(name: 'DAM_KALTURA_PLUGIN_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'RS_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'RUNTIME_CONTROLLER_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'PERSISTENCE_NODE_IMAGE_FILTER', value: pipelineParameters.POSTGRES_IMAGE_FILTER))
                    buildParameters.add(string(name: 'PERSISTENCE_CONNECTION_POOL_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'HAPROXY_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'LICENSE_MANAGER_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'LOGGING_SIDECAR_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'PERSISTENCE_METRICS_EXPORTER_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'LDAP_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'HELM_CHARTS_FILTER', value: pipelineParameters.HELM_IMAGE_FILTER))
                    buildParameters.add(string(name: 'PREREQS_CHECKER_IMAGE_FILTER', value: deploymentTargetBranch))

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
                    buildParameters.add(string(name: 'INSTANCE_AREA', value: pipelineParameters.INSTANCE_AREA))
                    buildParameters.add(string(name: 'CUSTOM_VALUES_OVERRIDE', value: pipelineParameters.CUSTOM_VALUES_OVERRIDE))
                    buildParameters.add(booleanParam(name: 'ENABLE_LOGSTASH_SETUP', value: pipelineParameters.ENABLE_LOGSTASH_SETUP))
                    
                    // use-non-dedicated is false then instance will use dedicated host-id
                    if(pipelineParameters.USE_NON_DEDICATED_INSTANCE == "false")
                    {
                        buildParameters.add(string(name: 'DEDICATED_HOST_ID', value: "${DEDICATED_HOST_ID_VALUE}"))
                    }

                    // There is no schedule required for these machines
                    buildParameters.add(string(name: 'NATIVE_POPO_SCHEDULE', value: pipelineParameters.NATIVE_POPO_SCHEDULE))

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
        stage('Pull Performace tests') {
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    sshagent(credentials: ['jenkins-git']) {
                        script {
                            sh """
                                chmod 600 ${DEPLOY_KEY}
                                git clone -b ${pipelineParameters.JMETER_BRANCH} git@git.cwp.pnp-hcl.com:Team-Q/Portal-Performance-Tests.git ${workspace}/Portal-Performance-Tests
                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sudo mkdir -p /opt/Portal-Performance-Tests && sudo chown centos: /opt/Portal-Performance-Tests 
                                sudo rm -rf /opt/Portal-Performance-Tests/.git'
                                scp -r -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/Portal-Performance-Tests centos@${terraformVarsEC2.instance_private_ip}:/opt
                            """
                        }
                    }
                }
            }
        }

        /*
         * Once scripts are copied to EC2 instance, we need to run the JMeter scripts for upload assets.
         */
        stage('Run JMeter performance tests for upload assets') {
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
                        fileName  = "test_log_dam_regression_for_upload_assets_${nowDate.format(date)}.jtl"
                        Exception caughtException = null
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo 'Running tests'
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'env JVM_ARGS="-Dnashorn.args=--no-deprecation-warning" /home/centos/${pipelineParameters.JMETER_BINARY_VERSION}/bin/./jmeter.sh  -JProtocol=${pipelineParameters.SERVER_PROTOCOL} -JHost=${SERVER_HOST_SOURCE} -JPort=${pipelineParameters.SERVER_PORT} -JRingAPIPort='' -JDAMPort='' -JDocumentsPath=${pipelineParameters.SERVER_DOCUMENTSPATH} -JImagesPath=${pipelineParameters.SERVER_IMAGESPATH} -JVideosPath=${pipelineParameters.SERVER_VIDEOSPATH} -JCSVPath=${pipelineParameters.SERVER_CSVPATH} -j jmeter.save.saveservice.output_format=xml -n -t /opt/Portal-Performance-Tests/jmeter/DAM_Performance_Test/DAMRegression/${pipelineParameters.UPLOAD_JMX_FILE} -JVUSERS_PER_TYPE=${pipelineParameters.VUSERS_PER_TYPE} -JTEST_AUTHENTICATED_VUSER_RAMP=${pipelineParameters.TEST_AUTHENTICATED_VUSER_RAMP} -JLOOP_COUNT=${pipelineParameters.LOOP_COUNT} -l /home/centos/test_logs/${fileName} -e -f -o /opt/dam-performance-test-reports/dam_Upload'
                                """
                                jmeter_error = sh(script:"ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'grep -i -e Exception -e Conflict /home/centos/test_logs/${fileName} | wc -l'", returnStdout: true).trim() as Integer
                                if ("${jmeter_error}" > 0) {
                                    echo 'Errors in DAM upload assets JMeter script execution results'
                                    currentBuild.result = 'UNSTABLE'
                                    sh """
                                        chmod 600 ${DEPLOY_KEY}
                                        cp $DEPLOY_KEY test-automation-deployments.pem
                                        chmod 0600 test-automation-deployments.pem
                                    """
                                }
                                // copy jtl file to native-kube instance and capture the upload time from jtl file
                                sh """
                                      echo "capture upload time from jtl file"
                                      chmod 600 ${DEPLOY_KEY}
                                      cp $DEPLOY_KEY test-automation-deployments.pem
                                      chmod 0600 test-automation-deployments.pem
                                      scp -v -i test-automation-deployments.pem -o StrictHostKeyChecking=no ${workspace}/kube/lifecycle/scripts/common/capture_api_response_time.sh centos@${SERVER_SOURCE}${pipelineParameters.DOMAIN_SUFFIX}:/home/centos/native-kube/
                                      scp -r -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no centos@${terraformVarsEC2.instance_private_ip}:~/test_logs/${fileName} .
                                      scp -v -i test-automation-deployments.pem -o StrictHostKeyChecking=no ./${fileName} centos@${SERVER_SOURCE}${pipelineParameters.DOMAIN_SUFFIX}:/home/centos/native-kube/
                                      ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${SERVER_SOURCE}${pipelineParameters.DOMAIN_SUFFIX}   '(sh /home/centos/native-kube/capture_api_response_time.sh ${fileName})'
                                      rm ${fileName}
                                """
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

        /*
         * Once JMeter scripts are executed then we will run the below stage to capture operations time for uploaded assets.
         */
        stage('Run scripts to capture operations time') {
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
                                    scp -v -i test-automation-deployments.pem -o StrictHostKeyChecking=no ./capture-dam-operations-time.sh centos@${SERVER_SOURCE}${pipelineParameters.DOMAIN_SUFFIX}:/home/centos/native-kube/
                                """

                                /* Run bash script to capture operations time */
                                sh  """
                                    ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${SERVER_SOURCE}${pipelineParameters.DOMAIN_SUFFIX} \
                                    '(sh /home/centos/native-kube/capture-dam-operations-time.sh)'
                                """

                                 /* Copy performane results log from EC2 instance */
                                sh """
                                    scp -v -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${SERVER_SOURCE}${pipelineParameters.DOMAIN_SUFFIX}:/home/centos/native-kube/dam_performance_results.log ${workspace}/dx-dam-regression/
                                """
                            } catch (Exception err) {
                                echo "Error: ${err}"
                                currentBuild.result = 'UNSTABLE'
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    cp $DEPLOY_KEY test-automation-deployments.pem
                                    chmod 0600 test-automation-deployments.pem
                                    ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${SERVER_SOURCE}${pipelineParameters.DOMAIN_SUFFIX} 'printf "0," >> /home/centos/native-kube/dam_performance_results.log'
                                    scp -v -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${SERVER_SOURCE}${pipelineParameters.DOMAIN_SUFFIX}:/home/centos/native-kube/dam_performance_results.log ${workspace}/dx-dam-regression/
                                """
                            }
                        }
                    }
                }
            }
        }

        /*
        * Import Logstash script to fetch pod logs and start logstash.
        */
        stage('Import Logstash script to fetch pod logs and start logstash') {
            steps {
               configFileProvider([
                   configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        try {
                           
                          /* Copy logstash script to start and stop logstash running, fetching pod logs */
                          sh """
                              chmod 600 ${DEPLOY_KEY}

                              scp -v -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no ${workspace}/test-scripts/logstash-scripts/start_log_collection.sh centos@${SERVER_HOST_SOURCE}:/home/centos 
                              scp -v -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no ${workspace}/test-scripts/logstash-scripts/start_logstash.sh centos@${SERVER_HOST_SOURCE}:/home/centos
                              scp -v -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no ${workspace}/test-scripts/logstash-scripts/stop_log_collection.sh centos@${SERVER_HOST_SOURCE}:/home/centos
                              scp -v -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no ${workspace}/test-scripts/logstash-scripts/stop_logstash.sh centos@${SERVER_HOST_SOURCE}:/home/centos
                              scp -v -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no ${workspace}/test-scripts/logstash-scripts/print_collected_pod_logfiles.sh centos@${SERVER_HOST_SOURCE}:/home/centos
                           """
                        } catch (Exception err) {
                            echo "Error: ${err}"
                            currentBuild.result = 'UNSTABLE'
                        }
                    }
                }
            }
        }

         /*
         * Execute JMeter script for DAM Anonymous fetching binaries.
         */
        stage('Run JMeter performance tests for DAM Anonymous rendering assets') {
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        def nowDate = new SimpleDateFormat('M-d-Y-HHmmss')
                        def date = new Date()
                        jtlFileforloadanonymousrendering  = "test_log_dam_load_anonymous_rendering_${nowDate.format(date)}.jtl"
                        jtlFileforanonymousrendering  = "test_log_dam_anonymous_rendering_${nowDate.format(date)}.jtl"
                        Exception caughtException = null
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo 'Running tests'
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'env JVM_ARGS="-Dnashorn.args=--no-deprecation-warning" /home/centos/${pipelineParameters.JMETER_BINARY_VERSION}/bin/./jmeter.sh -JProtocol=${pipelineParameters.SERVER_PROTOCOL} -JHost=${SERVER_HOST_SOURCE} -JPort=${pipelineParameters.SERVER_PORT} -JRingAPIPort='' -JDAMPort='' -JImagePath=${pipelineParameters.SERVER_IMAGEPATH} -JCSVPath=${pipelineParameters.SERVER_CSVPATH} -j jmeter.save.saveservice.output_format=xml -n -t /opt/Portal-Performance-Tests/jmeter/DAM_Performance_Test/DAMRegression/${pipelineParameters.TARGET_JMX_FILE_FOR_Load_Anonymous_Rendering} -l /home/centos/test_logs/${jtlFileforloadanonymousrendering}'
                                """
                                // wait time to have renditions ready for uploaded assets to render in next script
                                def WAIT_TIME_FOR_RENDITIONS = pipelineParameters.WAIT_TIME_FOR_RENDITIONS
                                sleep WAIT_TIME_FOR_RENDITIONS.toInteger()

                                /* Run bash script to collect rendering pod logs  */
                                sh  """
                                    ssh -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no centos@${SERVER_HOST_SOURCE} 'chmod +x /home/centos/start_log_collection.sh && sh /home/centos/start_log_collection.sh ${pipelineParameters.ENABLE_LOGSTASH_SETUP} &'                             
                                """
                                /* Executing script rendering */

                                sh """
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'env JVM_ARGS="-Dnashorn.args=--no-deprecation-warning" /home/centos/${pipelineParameters.JMETER_BINARY_VERSION}/bin/./jmeter.sh -JProtocol=${pipelineParameters.SERVER_PROTOCOL} -JHost=${SERVER_HOST_SOURCE} -JPort=${pipelineParameters.SERVER_PORT} -JRingAPIPort='' -JDAMPort='' -JImagePath=${pipelineParameters.SERVER_IMAGEPATH} -JCSVPath=${pipelineParameters.SERVER_CSVPATH} -j jmeter.save.saveservice.output_format=xml -n -t /opt/Portal-Performance-Tests/jmeter/DAM_Performance_Test/DAMRegression/${pipelineParameters.TARGET_JMX_FILE_FOR_Anonymous_Rendering} -JVUSERS_PER_TYPE=${pipelineParameters.RENDER_VUSERS_PER_TYPE} -JTEST_AUTHENTICATED_VUSER_RAMP=${pipelineParameters.RENDER_TEST_AUTHENTICATED_VUSER_RAMP} -JTEST_DURATION=${pipelineParameters.RENDER_TEST_DURATION} -l /home/centos/test_logs/${jtlFileforanonymousrendering} -e -f -o /opt/dam-performance-test-reports/dam_AnonymousRendering'
                                """
                                /* Run bash script to print collected pod logs stop collecting rendering pod logs and start logstash running  */
                                sh  """
                                    chmod 600 ${DEPLOY_KEY} 
                                    ssh -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no centos@${SERVER_HOST_SOURCE} 'chmod +x /home/centos/print_collected_pod_logfiles.sh && sh /home/centos/print_collected_pod_logfiles.sh ${pipelineParameters.ENABLE_LOGSTASH_SETUP}'
                                    ssh -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no centos@${SERVER_HOST_SOURCE} 'chmod +x /home/centos/stop_log_collection.sh && sh /home/centos/stop_log_collection.sh'
                                    ssh -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no centos@${SERVER_HOST_SOURCE} 'chmod +x /home/centos/start_logstash.sh && sh /home/centos/start_logstash.sh ${pipelineParameters.ENABLE_LOGSTASH_SETUP}' 
                                """
                                // for jmeter report to ready for checking errors details in results
                                sleep WAIT_TIME_FOR_RENDITIONS.toInteger()

                                // check if errors count in statistics.json and copy the parse error details to workspace
                                sh """
                                    scp -v -i test-automation-deployments.pem -o StrictHostKeyChecking=no ${workspace}/test-scripts/get_jtl_errors.sh centos@${terraformVarsEC2.instance_private_ip}:/opt/dam-performance-test-reports/dam_AnonymousRendering
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sh /opt/dam-performance-test-reports/dam_AnonymousRendering/get_jtl_errors.sh "/opt/dam-performance-test-reports/dam_AnonymousRendering/content/js/dashboard.js" "/opt/dam-performance-test-reports/dam_AnonymousRendering/statistics.json"'
                                    scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip}:/home/centos/jtl_errors.json ${workspace}/dx-dam-regression
                                """
      

                                jmeter_error = sh(script:"ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'grep -i -e Exception -e Conflict /home/centos/test_logs/${jtlFileforanonymousrendering} | wc -l'", returnStdout: true).trim() as Integer
                                if ("${jmeter_error}" > 0) {
                                    currentBuild.result = 'UNSTABLE'
                                    echo 'Errors in DAM Anonymous rendering assets JMeter script execution results'
                                }

                                // fetch average time for image, document and video
                                // combine the logs of dam performance results in proper format
                                sh """
                                    echo "Anonymous access to assets - capturing average response time for image, document and video "
                                    chmod 600 ${DEPLOY_KEY}
                                    scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/kube/lifecycle/scripts/common/capture_fetch_binary_mean_time.sh centos@${terraformVarsEC2.instance_private_ip}:/opt/Portal-Performance-Tests
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && sh capture_fetch_binary_mean_time.sh "/opt/dam-performance-test-reports/dam_AnonymousRendering/content/js/dashboard.js" "${pipelineParameters.KUBE_FLAVOUR}" "anonymous"'
                                    scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip}:/home/centos/dam_AnonymousRendering/dam_anonymous_performance_binary_fetch_results.log ${workspace}/dx-dam-regression
                                    cat ${workspace}/dx-dam-regression/dam_anonymous_performance_binary_fetch_results.log
                                """

                                // Prometheus results to opensearch dashboard
                                sh """
                                    scp -v -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no ${workspace}/test-scripts/container_config.txt centos@${SERVER_HOST_SOURCE}:/home/centos 
                                    scp -v -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no ${workspace}/test-scripts/installation-of-jq.sh centos@${SERVER_HOST_SOURCE}:/home/centos 
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${SERVER_HOST_SOURCE} 'sh /home/centos/installation-of-jq.sh ${SERVER_HOST_SOURCE}'
                                    scp -v -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no ${workspace}/test-scripts/get_prometheus_results.sh centos@${SERVER_HOST_SOURCE}:/home/centos
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${SERVER_HOST_SOURCE} 'sh /home/centos/get_prometheus_results.sh ${SERVER_HOST_SOURCE} http 32001 60'
                                    scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${SERVER_HOST_SOURCE}:/home/centos/prometheus_cpu_results.json ${workspace}/dx-dam-regression
                                    scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${SERVER_HOST_SOURCE}:/home/centos/prometheus_memory_results.json ${workspace}/dx-dam-regression
                                """

                                // Add to dashboard
                                sh """
                                        echo "${currentBuild.displayName}," >> ${workspace}/dx-dam-regression/dam_performance_results.log
                                        cat ${workspace}/dx-dam-regression/dam_performance_results.log
                                        cat ${workspace}/dx-dam-regression/dam_anonymous_performance_binary_fetch_results.log >> ${workspace}/dx-dam-regression/dam_performance_results.log
                                        sh ${workspace}/test-scripts/convert-log-to-xml.sh ${workspace} "${pipelineParameters.KUBE_FLAVOUR}"
                                        rm ${workspace}/dx-dam-regression/dam_anonymous_performance_binary_fetch_results.log
                                        cat ${workspace}/dx-dam-regression/Dam_Performance_Tests_Results.xml
                                        cat ${workspace}/dx-dam-regression/dam_performance_results.log
                                    """
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
        /*
         * Extract DAM performance results from log file  and use JMeter script to push results to OpenSearch Dashboard
         */
        stage('Post extracted results from JMeter report to OpenSearch Dashboard') {
            options {
                timeout(time: 90, unit: 'MINUTES')
            }
            steps {
                withCredentials([
                    usernamePassword(credentialsId: 'cwp-opensearch', passwordVariable: 'OPENSEARCH_PASSWORD', usernameVariable: 'OPENSEARCH_USERNAME'),
               ]) {
                    dir("${workspace}") {
                        configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                            script {
                                def nowDate = new SimpleDateFormat('M-d-Y-HHmmss')
                                def date = new Date()

                                def build_Time = new Date().format("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", TimeZone.getTimeZone('GMT+5:30'))
                                echo "DAM Regression Build_Time: - $build_Time"
                                Exception caughtException = null
                                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                                    try {
                                        echo 'Running tests'
                                        sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    sh ${workspace}/test-scripts/dam-results-to-opensearch.sh ${workspace} "${pipelineParameters.OS_PROTOCOL}" "${pipelineParameters.OS_HOSTNAME}" "${pipelineParameters.OS_INDEX_NAME}" "${pipelineParameters.OS_USER_NAME}" "${OPENSEARCH_PASSWORD}" ${build_Time}
                                """
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
        }}}

        /*
         * Combine above all stage results and upload to S3 bucket
         */

        stage('Generate Test Report') {
            steps {
                withCredentials([
                   usernamePassword(credentialsId: 'aws_credentials', passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID'),
              ]) {
                    dir("${workspace}") {
                        configFileProvider([
                               configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                       ]) {
                            script {
                                Exception caughtException = null
                                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                                    try {
                                        echo 'Generating performance test dashboard report for dam apis'
                                        // Copy previous runs file from s3 bucket
                                        // Copy current run and previous run file to EC2 instance
                                        // Execute script to generate report
                                        // Copy reports(html and css) to s3 bucket
                                        sh """
                                           chmod 600 ${DEPLOY_KEY}
                                           aws s3 cp s3://dx-testarea/performance-test-reports/dam-performance-tests-combined-runs.xml dam-performance-tests-combined-runs.xml
                                           scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/dam-performance-tests-combined-runs.xml centos@${terraformVarsEC2.instance_private_ip}:/opt/dam-performance-test-reports/
                                           scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/dx-dam-regression/testreport.xml centos@${terraformVarsEC2.instance_private_ip}:/opt/dam-performance-test-reports/
                                           ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/dam-performance-test-reports && chmod +x /opt/dam-performance-test-reports/dam-performance-test-report.sh && sh /opt/dam-performance-test-reports/dam-performance-test-report.sh snapshotDir="https://dxcore-test-results-secure.team-q-dev.com/DX-95-TestReports/${REPORT_BUILD_NAME}_dam-performance-test" && tar -czf ${REPORT_BUILD_NAME}_dam-performance-test.zip dam-jtl-Report.html wtf.css dam_Upload/* dam_AnonymousRendering/*'
                                           scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/dx-dam-regression/Dam_Performance_Tests_Results.xml centos@${terraformVarsEC2.instance_private_ip}:/opt/dam-performance-test-reports/
                                           ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/dam-performance-test-reports/ && chmod +x dam_performance_test_run.sh && sh dam_performance_test_run.sh ${REPORT_BUILD_NAME}_dam-performance-test Performance'
                                           scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r  centos@${terraformVarsEC2.instance_private_ip}:/opt/dam-performance-test-reports/dam-performance-tests-combined-runs.xml .
                                           scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r  centos@${terraformVarsEC2.instance_private_ip}:/opt/dam-performance-test-reports/dashboard/* .
                                           scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r  centos@${terraformVarsEC2.instance_private_ip}:/opt/dam-performance-test-reports/${REPORT_BUILD_NAME}_dam-performance-test.zip .
                                           aws s3 cp ${REPORT_BUILD_NAME}_dam-performance-test.zip s3://dx-testarea/
                                           aws s3 cp dam-performance-tests-combined-runs.xml s3://dx-testarea/performance-test-reports/
                                           cat Performance-dashboard.html
                                           cat wtf.css
                                           aws s3 cp Performance-dashboard.html s3://dx-testarea/performance-test-reports/
                                           aws s3 cp wtf.css s3://dx-testarea/performance-test-reports/

                                       """
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
            }
        }
            }
     /*
     * Perform proper cleanup to leave a healthy jenkins agent.
     */
    post {
        cleanup {
            //Destroy the EC2 instance incl. storage
            dxTerraformDestroyEc2Instance(terraformVarsEC2)
            //Cleanup workspace
            dxWorkspaceDirectoriesCleanup()
        }
    }
}
