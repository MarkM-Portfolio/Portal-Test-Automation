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
def xmlFileToImport = ''
DEDICATED_HOST_ID_VALUE = ''
// Create an object to store dedicated host configuration values
def dedicatedHostParameters = [:]

def deploymentTargetBranch
def deploymentTargetRepo

def newDeployment = "false"
def serverHostSource = ""

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
  openLdap: true
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
"""

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
        stage('Load modules and configuration') {
            steps {
                script {
                    // load params from yaml file
                    dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/dx-wcm-pages-perf-tests/parameters.yaml")
                }
            }
        }

        stage('Prepare Settings') {
            steps {
                /*
                 * Assigning hostname using Performace-test-automation + the timestap
                 */
                dir("${WORKSPACE}/dx-wcm-pages-perf-tests") {
                    script {
                        // determine build version and label current job accordingly
                        def dateFormat = new SimpleDateFormat('yyyyMMdd-HHmmss')
                        def date = new Date()

                        INSTANCE_OWNER = dxJenkinsGetJobOwner()
                        terraformVarsEC2.tfvar_instance_owner = "${INSTANCE_OWNER}"
                        terraformVarsEC2.tfvar_instance_name = 'wcm_automation_pages_test'
                         // instance area = PERF
                        terraformVarsEC2.tfvar_instance_area = pipelineParameters.INSTANCE_AREA

                        // We are doing DAM bulk upload EXIM performance tests we are using large ec2 instance
                        terraformVarsEC2.tfvar_aws_ec2_instance_type = pipelineParameters.EC2_INSTANCE_TYPE

                        echo 'Assigning hostname + timestamp'
                        terraformVarsEC2.tfvar_instance_name = "${terraformVarsEC2.tfvar_instance_name}_${dateFormat.format(date)}"
                        echo "New instance will be: ${terraformVarsEC2.tfvar_instance_name}"

                        //select pages to import based on CONFIGURATION in params
                        def config = pipelineParameters.DX_CONFIGURATION
                        if(config == "medium"){
                            xmlFileToImport = "wcmpagesToImport_medium.xml"
                        } else if(config == "large"){
                            xmlFileToImport = "wcmpagesToImport_large.xml"
                        } else {
                             xmlFileToImport = "PerfTestServerPages.xml"
                        }
                        echo "pages xml to import: ${xmlFileToImport}"

                        //target instance host name for authoring
                        if(pipelineParameters.PORTAL_HOST == "")
                        {
                            newDeployment = "true"
                            serverHostSource = "${pipelineParameters.INSTANCE_NAME}-short-${pipelineParameters.TARGET_BRANCH}"

                            // short and long strings append to host names based on render duration from params
                            if (pipelineParameters.RENDER_TEST_DURATION == '3600') {
                                pipelineParameters.PORTAL_HOST = "${pipelineParameters.INSTANCE_NAME}-short-${pipelineParameters.TARGET_BRANCH}${pipelineParameters.DOMAIN_SUFFIX}"
                                serverHostSource = "${pipelineParameters.INSTANCE_NAME}-short-${pipelineParameters.TARGET_BRANCH}"
                            } else if (pipelineParameters.RENDER_TEST_DURATION == '172800') {
                                pipelineParameters.PORTAL_HOST = "${pipelineParameters.INSTANCE_NAME}-long-${pipelineParameters.TARGET_BRANCH}${pipelineParameters.DOMAIN_SUFFIX}"
                                serverHostSource = "${pipelineParameters.INSTANCE_NAME}-long-${pipelineParameters.TARGET_BRANCH}"
                            } else {
                                pipelineParameters.PORTAL_HOST = "${pipelineParameters.INSTANCE_NAME}-${pipelineParameters.TARGET_BRANCH}${pipelineParameters.DOMAIN_SUFFIX}"
                                serverHostSource = "${pipelineParameters.INSTANCE_NAME}-${pipelineParameters.TARGET_BRANCH}"
                            }

                        } else {
                            serverHostSource = "${pipelineParameters.INSTANCE_NAME}"
                        }

                         // Display name includes the INSTANCE_NAME and a timestamp
                        currentBuild.displayName = "${serverHostSource}_${dateFormat.format(date)}"

                        // check if CUSTOM_VALUES_OVERRIDE provided in params or else use small config helm values to override for performance tests
                        if(pipelineParameters.CUSTOM_VALUES_OVERRIDE == ""){
                            pipelineParameters.CUSTOM_VALUES_OVERRIDE = CUSTOM_VALUES_OVERRIDE;
                        }

                        // get target repo for artifactory
                        if (pipelineParameters.TARGET_BRANCH.contains('release')) {
                            deploymentTargetBranch = pipelineParameters.TARGET_BRANCH
                            deploymentTargetRepo = 'quintana-docker-prod'
                        } else {
                            deploymentTargetBranch = 'develop'
                            deploymentTargetRepo = 'quintana-docker'
                        }
                         
                    }
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


        stage('Look up for available dedicated hosts') {
            when {
                expression { pipelineParameters.USE_NON_DEDICATED_INSTANCE == "false" }
            }
            steps {
                script {
                    try {
                        dedicatedHostParameters.tfvar_dedicated_host_owner = dxJenkinsGetJobOwner(defaultOwner: 'pm.gayathri@hcl.com')
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
                        scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/dx-wcm-pages-perf-tests/helpers/01-setup-prereqs.sh centos@${terraformVarsEC2.instance_private_ip}:/tmp
                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'chmod +x /tmp/01-setup-prereqs.sh && sh /tmp/01-setup-prereqs.sh ${pipelineParameters.JMETER_BINARY_VERSION}'
                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sudo mkdir -p /opt/wcm-performance-test-reports && sudo chown centos: /opt/wcm-performance-test-reports'
                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sudo mkdir -p /home/centos/test_logs && sudo chown centos: /home/centos/test_logs'
                    
                    """
                }
            }
        }

         // Undeploy if same namespace exists
        stage('Undeploying the application in k8 environment for regression test') {
            when { expression { newDeployment == "true" } }
            steps {
                script {
                    buildParameters = []
                    buildParameters.add(string(name: 'INSTANCE_NAME', value: "${serverHostSource}"))
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
            when { expression { newDeployment == "true" } }
            steps {
                script {
                    dir("${WORKSPACE}/dx-wcm-pages-perf-tests") {
                        deploymentSettings = readYaml file: "${workspace}/dx-wcm-pages-perf-tests/deployment-settings.yaml"
                        echo "deploymentSettings values: ${deploymentSettings}"
                    }
                    buildParameters = []
                    buildParameters.add(string(name: 'INSTANCE_NAME', value: "${serverHostSource}"))
                    buildParameters.add(string(name: 'NAMESPACE', value: 'dxns'))
                    buildParameters.add(string(name: 'DOMAIN_SUFFIX', value: pipelineParameters.DOMAIN_SUFFIX))
                    buildParameters.add(string(name: 'HOSTED_ZONE', value: pipelineParameters.HOSTED_ZONE))
                    buildParameters.add(string(name: 'BUILD_USER_ID', value: terraformVarsEC2.tfvar_instance_owner))
                    buildParameters.add(string(name: 'NEXT_JOB_DELAY_HOURS', value: pipelineParameters.NEXT_JOB_DELAY_HOURS))
                    buildParameters.add(string(name: 'AWS_SUBNET_NAME', value: pipelineParameters.AWS_SUBNET_NAME))

                    buildParameters.add(string(name: 'IMAGE_REPOSITORY', value: deploymentTargetRepo))
                    buildParameters.add(string(name: 'CORE_IMAGE_FILTER', value: pipelineParameters.CORE_IMAGE_FILTER))
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
                    buildParameters.add(string(name: 'ENABLE_OPENLDAP_SETUP', value: pipelineParameters.ENABLE_OPENLDAP_SETUP))
                    buildParameters.add(string(name: 'USERS_COUNT_OPENLDAP', value: pipelineParameters.USERS_COUNT_OPENLDAP))
                    buildParameters.add(booleanParam(name: 'ENABLE_LOGSTASH_SETUP', value: pipelineParameters.ENABLE_LOGSTASH_SETUP))
                    
                    // use-non-dedicated is false then instance will use dedicated host-id
                    if(pipelineParameters.USE_NON_DEDICATED_INSTANCE == "false")
                    {
                        buildParameters.add(string(name: 'DEDICATED_HOST_ID', value: "${DEDICATED_HOST_ID_VALUE}"))
                    }

                    // There is no schedule required for these machines
                    buildParameters.add(string(name: 'NATIVE_POPO_SCHEDULE', value: pipelineParameters.NATIVE_POPO_SCHEDULE))
                    buildParameters.add(string(name: 'CUSTOM_VALUES_OVERRIDE', value: pipelineParameters.CUSTOM_VALUES_OVERRIDE))

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
         * Install dxclient, configure and import wcm libraries and pages.
         */
        stage('Install dxclient, configure and import libraries and pages ') {
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        dir("${workspace}") {
                            withCredentials([
                                [$class: 'UsernamePasswordMultiBinding', credentialsId: "${pipelineParameters.TOOL_CREDENTIALS_ID}", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']
                            ]) {
                                try {
                                    sh """
                                        chmod 600 ${DEPLOY_KEY}
                                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /home/centos/ && curl -s -u${USERNAME}:${PASSWORD} ${pipelineParameters.TOOL_PACKAGE_URL} --output dxclient.zip'
                                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'yes | unzip dxclient.zip && ls'
                                        echo load the dxclient docker image
                                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /home/centos/dxclient && docker load < dxclient.tar.gz'
                                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /home/centos/dxclient && chmod -R 777 bin && ./bin/dxclient -h'
 
                                        scp -r -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/dx-wcm-pages-perf-tests/helpers/${xmlFileToImport} centos@${terraformVarsEC2.instance_private_ip}:/tmp
                                        sleep 300
                                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /home/centos/dxclient && chmod -R 777 bin && ./bin/dxclient xmlaccess -xmlFile /tmp/${xmlFileToImport} -dxProtocol ${pipelineParameters.DX_PROTOCOL} -hostname ${pipelineParameters.PORTAL_HOST} -dxPort ${pipelineParameters.DX_PORT} -dxUsername ${pipelineParameters.USERNAME} -dxPassword ${pipelineParameters.PASSWORD}'
                                     """
                                } catch (Exception err) {
                                    echo "Error: ${err}"
                                    currentBuild.result = 'UNSTABLE'
                                }
                            }
                        }
                    }
                }
            }
        }

          /*
          * Import WCM and Script app libraries.
          */
        stage('Import WCM and Script app libraries') {
            steps {
                dir("${workspace}/dx-wcm-pages-perf-tests") {
                    configFileProvider([
                        configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                    ]) {
                        script {
                            dir("${workspace}") {
                                withCredentials([
                                    [$class: 'UsernamePasswordMultiBinding', credentialsId: "${pipelineParameters.TOOL_CREDENTIALS_ID}", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']
                                ]) {
                                    try {
                                          /* Extract PEM file */
                                        sh """
                                            chmod 600 ${DEPLOY_KEY}
                                         """
 
                                         /* Copy libs and scripts to EC2 instance */
                                        sh """
                                           scp -r -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip}:/home/centos/PerfTestLibs.zip ${workspace}/dx-wcm-pages-perf-tests
                                           scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/dx-wcm-pages-perf-tests/PerfTestLibs.zip centos@${pipelineParameters.PORTAL_HOST}:/home/centos/
                                           scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/dx-wcm-pages-perf-tests/helpers/importPerfTestLibs.sh centos@${pipelineParameters.PORTAL_HOST}:/home/centos/
                                         """
 
                                         /* Run bash script to import libs*/
                                        sh  """
                                             ssh -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no centos@${pipelineParameters.PORTAL_HOST} \
                                            '(sh /home/centos/importPerfTestLibs.sh)'
                                            
                                         """
                                    } catch (Exception err) {
                                        echo "Error: ${err}"
                                        currentBuild.result = 'UNSTABLE'
                                    }
                                }
                            }
                    }   }
                }
            }
        }

        stage('Pull Performace tests') {
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    sshagent(credentials: ['jenkins-git']) {
                        script {
                            sh """
                                chmod 600 ${DEPLOY_KEY}
                                rm -rf ${workspace}/Portal-Performance-Tests
                                git clone -b ${pipelineParameters.JMETER_BRANCH} git@git.cwp.pnp-hcl.com:Team-Q/Portal-Performance-Tests.git ${workspace}/Portal-Performance-Tests
                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sudo mkdir -p /opt/Portal-Performance-Tests && sudo chown centos: /opt/Portal-Performance-Tests && sudo rm -rf /opt/Portal-Performance-Tests/.git'
                                scp -r -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/Portal-Performance-Tests centos@${terraformVarsEC2.instance_private_ip}:/opt
                            """
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
                           
                          /* Copy logstash script to start and stop logstash, fetching pod logs */
                          sh """
                              chmod 600 ${DEPLOY_KEY}

                              scp -v -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no ${workspace}/test-scripts/logstash-scripts/start_log_collection.sh centos@${pipelineParameters.PORTAL_HOST}:/home/centos 
                              scp -v -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no ${workspace}/test-scripts/logstash-scripts/start_logstash.sh centos@${pipelineParameters.PORTAL_HOST}:/home/centos
                              scp -v -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no ${workspace}/test-scripts/logstash-scripts/stop_log_collection.sh centos@${pipelineParameters.PORTAL_HOST}:/home/centos
                              scp -v -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no ${workspace}/test-scripts/logstash-scripts/stop_logstash.sh centos@${pipelineParameters.PORTAL_HOST}:/home/centos
                              scp -v -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no ${workspace}/test-scripts/logstash-scripts/print_collected_pod_logfiles.sh centos@${pipelineParameters.PORTAL_HOST}:/home/centos
                           """
                           /* Run bash script to collect rendering pod logs */
                           sh  """
                               ssh -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no centos@${pipelineParameters.PORTAL_HOST} 'chmod +x /home/centos/start_log_collection.sh && sh /home/centos/start_log_collection.sh ${pipelineParameters.ENABLE_LOGSTASH_SETUP} &'  
                               
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
         * Run JMeter performance tests for wcm performance tests rendering
         */
        stage('Run JMeter performance tests for wcm performance tests rendering') {
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        def nowDate = new SimpleDateFormat('M-d-Y-HHmmss')
                        def date = new Date()
                        jtlFileRendering  = "test_log_wcm_rendering_${nowDate.format(date)}.jtl"
                        Exception caughtException = null
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo 'Running tests wcm rendering tests'

                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'env JVM_ARGS="-Dnashorn.args=--no-deprecation-warning" /home/centos/${pipelineParameters.JMETER_BINARY_VERSION}/bin/./jmeter.sh -JDX_Protocol=${pipelineParameters.SERVER_PROTOCOL} -JDX_HOST=${pipelineParameters.PORTAL_HOST}  -j jmeter.save.saveservice.output_format=xml -n -t /opt/Portal-Performance-Tests/jmeter/Portal_WCM_Read_Scenario/data/${pipelineParameters.TARGET_JMX_FILE_FOR_Rendering} -JVUSERS_PER_TYPE=${pipelineParameters.RENDER_VUSERS_PER_TYPE} -JTEST_AUTHENTICATED_VUSER_RAMP=${pipelineParameters.RENDER_TEST_AUTHENTICATED_VUSER_RAMP} -JTEST_DURATION=${pipelineParameters.RENDER_TEST_DURATION} -JDX_RENDER_TIMEOUT=${pipelineParameters.DX_RENDER_TIMEOUT} -l /home/centos/test_logs/${jtlFileRendering} -e -f -o /opt/wcm-performance-test-reports/wcm_Rendering'
                                """
                                
                                /* Run bash script to display collected pod logs,stop collecting rendering pod logs and start logstash */
                                sh  """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no centos@${pipelineParameters.PORTAL_HOST} 'chmod +x /home/centos/print_collected_pod_logfiles.sh && sh /home/centos/print_collected_pod_logfiles.sh ${pipelineParameters.ENABLE_LOGSTASH_SETUP}'
                                    ssh -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no centos@${pipelineParameters.PORTAL_HOST} 'chmod +x /home/centos/stop_log_collection.sh && sh /home/centos/stop_log_collection.sh'
                                    ssh -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no centos@${pipelineParameters.PORTAL_HOST} 'chmod +x /home/centos/start_logstash.sh && sh /home/centos/start_logstash.sh ${pipelineParameters.ENABLE_LOGSTASH_SETUP}'
                                """
                                // for jmeter report to ready for checking errors details in results
                                sleep 300

                                // check if errors count in statistics.json and copy the parse error details to workspace
                                sh """
                                    scp -v -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no ${workspace}/test-scripts/get_jtl_errors.sh centos@${terraformVarsEC2.instance_private_ip}:/opt/wcm-performance-test-reports/wcm_Rendering
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sh /opt/wcm-performance-test-reports/wcm_Rendering/get_jtl_errors.sh "/opt/wcm-performance-test-reports/wcm_Rendering/content/js/dashboard.js" "/opt/wcm-performance-test-reports/wcm_Rendering/statistics.json"'
                                    scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip}:/home/centos/jtl_errors.json ${workspace}/dx-wcm-pages-perf-tests
                                """

                                jmeter_error = sh(script:"ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'grep -i -e Exception /home/centos/test_logs/${jtlFileRendering} | wc -l'", returnStdout: true).trim() as Integer
                                if ("${jmeter_error}" > 0) {
                                    currentBuild.result = 'UNSTABLE'
                                    echo 'Errors in wcm rendering assets JMeter script execution results'
                                }

                                // fetch average response and throughput from jmeter report
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/dx-wcm-pages-perf-tests/helpers/get_response_time.sh centos@${terraformVarsEC2.instance_private_ip}:/opt/Portal-Performance-Tests
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/Portal-Performance-Tests && sh get_response_time.sh "/opt/wcm-performance-test-reports/wcm_Rendering/content/js/dashboard.js"'
                                    scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip}:/home/centos/wcm_rendering_performance_results.log ${workspace}/dx-wcm-pages-perf-tests
                                    echo "${currentBuild.displayName}," >> ${workspace}/dx-wcm-pages-perf-tests/wcm_rendering_performance_results.log
                                    cat ${workspace}/dx-wcm-pages-perf-tests/wcm_rendering_performance_results.log
                                """

                                // Prometheus results to opensearch dashboard
                                sh """
                                    scp -v -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no ${workspace}/test-scripts/container_config.txt centos@${pipelineParameters.PORTAL_HOST}:/home/centos 
                                    scp -v -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no ${workspace}/test-scripts/installation-of-jq.sh centos@${pipelineParameters.PORTAL_HOST}:/home/centos 
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${pipelineParameters.PORTAL_HOST} 'sh /home/centos/installation-of-jq.sh ${pipelineParameters.PORTAL_HOST}'
                                    scp -v -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no ${workspace}/test-scripts/get_prometheus_results.sh centos@${pipelineParameters.PORTAL_HOST}:/home/centos
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${pipelineParameters.PORTAL_HOST} 'sh /home/centos/get_prometheus_results.sh ${pipelineParameters.PORTAL_HOST} http 32001 60'
                                    scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${pipelineParameters.PORTAL_HOST}:/home/centos/prometheus_cpu_results.json ${workspace}/dx-wcm-pages-perf-tests
                                    scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${pipelineParameters.PORTAL_HOST}:/home/centos/prometheus_memory_results.json ${workspace}/dx-wcm-pages-perf-tests
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
                                echo "WCM rendering Regression Build_Time: - $build_Time"
                                Exception caughtException = null
                                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                                    try {
                                        echo 'Running tests'
                                        sh """
                                            chmod 600 ${DEPLOY_KEY}
                                            sh ${workspace}/test-scripts/wcm-results-to-opensearch.sh ${workspace} "${pipelineParameters.OS_PROTOCOL}" "${pipelineParameters.OS_HOSTNAME}" "${pipelineParameters.OS_INDEX_NAME}" "${pipelineParameters.OS_USER_NAME}" "${OPENSEARCH_PASSWORD}" ${build_Time}
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
        }}  }
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