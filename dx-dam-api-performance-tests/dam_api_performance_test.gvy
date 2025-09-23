/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2021. All Rights Reserved.       *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

import java.text.SimpleDateFormat

def cleanupInstances() {
     withCredentials([
                    usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')
                ]) {
                    dir("${workspace}/dx-dam-regression/terraform/ec2-launch") {
                        sh(script: """
                            sed -i 's/ENVIRONMENT_HOSTNAME/${env.ENV_HOSTNAME}/g' variables.tf
                            ${workspace}/terraform init
                            ${workspace}/terraform destroy -auto-approve
                        """)
                    }
                }
}

/*
* Sets the default branch to pull for running the tests. If no branch is provided branch will be set to develop.
*/
if (!env.TARGET_BRANCH) {
    env.TARGET_BRANCH = 'develop'
}

pipeline {

    agent {
        label 'build_infra'
    }
    parameters {
      string(name: 'DEPLOYMENT_LEVEL', defaultValue: 'develop',description: 'Deploying latest images')
      string(name: 'NAMESPACE', defaultValue: 'dam_api_performance_test',description: 'name space')
      string(name: 'KUBE_FLAVOUR', defaultValue: 'native', description: 'Deploying a native kube environment.')
      string(name: 'KUBE_UNDEPLOY_JOB', defaultValue: 'CI/kube-deploy/native-kube-remove', description: 'Job which undeploys the environment',  trim: false)
      string(name: 'KUBE_DEPLOY_JOB', defaultValue: 'CI/kube-deploy/native-kube-next-deploy', description: 'Job which deploys the environment',  trim: false)
      choice(name: 'DEPLOYMENT_METHOD', choices: ['helm', 'dxctl'],  description: 'Select deployment method')
      string(name: 'CLUSTER_NAME', defaultValue: '', description: 'Cluster name where the deployment should be deployed to')
      string(name: 'CLUSTER_REGION', defaultValue: '', description: 'Region of the cluster')
      string(name: 'CONTEXT_ROOT_PATH', defaultValue: 'wps', description: 'Context root')
      string(name: 'DX_CORE_HOME_PATH', defaultValue: 'portal', description: 'Home path')
      string(name: 'PERSONALIZED_DX_CORE_PATH', defaultValue: 'myportal', description: 'Personalized path')
      string(name: 'DOMAIN_SUFFIX', defaultValue: '.team-q-dev.com', description: ' ')
      booleanParam(name: 'SSL_ENABLED', defaultValue: true, description: 'Required for testing environments with https/self-signed certificates like native.kube.')
    }

    stages {
        stage("Load modules and configuration") {
            steps {
                script {
                    commonModule = load "./autonomous-deployments/modules/common.gvy"
                    commonConfig = load "./autonomous-deployments/config/common.gvy"
                    env.NAMESPACE = "dam-api-perf-${DEPLOYMENT_METHOD}-dev"
                }
            }
        }

        stage('Prepare Settings') {
            steps {
                /*
                 * Assigning hostname using Performace-test-automation + the timestap
                 */
                dir("${WORKSPACE}/dx-dam-api-performance-tests") {
                    script {
                        // determine build version and label current job accordingly
                        def dateFormat = new SimpleDateFormat("yyyyMMdd-HHmm")
                        def date = new Date()
                        env.SERVER_PROTOCOL = 'https'
                        env.SERVER_HOST = "${env.NAMESPACE}${DOMAIN_SUFFIX}"
                        env.SERVER_PORT = ''
                        env.SERVER_RINGAPI_PORT = ''
                        env.SERVER_DAMAPI_PORT = ''
                        env.SERVER_IMAGEPATH = '/home/centos/data/dam/DAMRegression/Dataset'
                        env.SERVER_CSVPATH = '/home/centos/data/dam/DAMRegression/Dataset/dam_users.csv'
                        if (!env.TARGET_JMX_FILE){
                            env.TARGET_JMX_FILE = 'DAM_Regression_Test.jmx'
                        }
                        if (!env.TARGET_JMX_FILE_FOR_DAM_GET_API){
                            env.TARGET_JMX_FILE_FOR_DAM_GET_API = 'DAM_API_Performance_Tests_for_getapi.jmx'
                        }
                        echo "Assigning hostname + timestamp"
                        env.ENV_HOSTNAME = "dx_dam_api_performance_tests_${dateFormat.format(date)}"
                        echo "New hostname will be: ${env.ENV_HOSTNAME}"

                        // Display name includes the ENV_HOSTNAME and a timestamp
                        currentBuild.displayName = "${env.ENV_HOSTNAME}"

                        // Defines the time to live in hours for all resources created (AMI, EC2 instances and DNS entries)
                        if (!env.RESOURCES_TTL){
                            env.RESOURCES_TTL = '10'
                        }
                        // Calculate expiration timestamp
                        def ttl_stamp = (System.currentTimeMillis() + (env.RESOURCES_TTL.toString().toInteger() * 60 * 60 * 1000))
                        env.TF_VAR_EXPIRATION_STAMP = ttl_stamp
                    }
                }

            }
        }

         /*
         *  Preparing terraform to run in the current workspace. Terraform abstracts the AWS access and handles resource lifecycles and deployments
         */
        stage('Prepare Terraform') {
            steps {
                sh """
                    curl -LJO https://releases.hashicorp.com/terraform/0.12.20/terraform_0.12.20_linux_amd64.zip
                    unzip terraform_0.12.20_linux_amd64.zip
                    chmod +x terraform
                    ./terraform --help
                """
            }
        }

         /*
         * Run terraform to create an EC2 instance based on the terraform scripting and add an route53 record for it.
         */
        stage('Create EC2 instance') {
            steps {
                script {
                    /*
                     * We need the AWS credentials for terraform and the deploy key to have proper SSH access to instances we created.
                     */ 
                    withCredentials([
                        usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')
                    ]) {
                        configFileProvider([
                          configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                        ]) {
                            dir("${workspace}/dx-dam-api-performance-tests/terraform/ec2-launch") {
                                // replace placeholder in the variables.tf to fit the current environment
                                sh(script: """
                                    sed -i 's/ENVIRONMENT_HOSTNAME/${env.ENV_HOSTNAME}/g' variables.tf
                                    sed -i 's/TAG_NAME/${env.ENV_HOSTNAME}/g' main.tf
                                    ${workspace}/terraform init
                                    ${workspace}/terraform apply -auto-approve
                                """)
                                def instanceInformation = sh(script: """
                                    ${workspace}/terraform show -json
                                """, returnStdout: true).trim()
                                def instanceJsonInformation = readJSON text: instanceInformation
                                // extract private ip, dns and id of created instance
                                def instanceIp = instanceJsonInformation['values']['root_module']['resources'][0]['values']['private_ip']
                                def instanceDns = instanceJsonInformation['values']['root_module']['resources'][0]['values']['private_dns']
                                def instanceId = instanceJsonInformation['values']['root_module']['resources'][0]['values']['id']
                                echo "Instance ${instanceId} running on ${instanceIp}."
                                // set instanceIp, instanceDns and instanceId as variable for later use
                                env.INSTANCE_IP = instanceIp
                                env.INSTANCE_DNS = instanceDns
                                env.INSTANCE_ID = instanceId
                                // test connection to instance via ssh
                                sh(script: """
                                    chmod 600 ${DEPLOY_KEY}
                                    export TARGET_IP=${INSTANCE_IP}
                            	    sh ${workspace}/dx-dam-api-performance-tests/scripts/wait_for_instance.sh
                                """)
                            }
                        }
                    }
                }             
            }
        }

        stage('Undeploying the application in k8 environment for regression test') {
             steps {
                script {
                    buildParams = []
                    buildParams.add(
                        [$class: 'StringParameterValue',
                            name: 'INSTANCE_NAME',
                            value: "${env.NAMESPACE}"])
                    build(job: "${params.KUBE_UNDEPLOY_JOB}", 
                          parameters: buildParams, 
                          propagate: true,
                          wait: true)
                }
            }
        }

        stage('Deploying the application in k8 environment for regression test') {
            steps {
                script {
                    buildParams = commonModule.createKubeParams(env.NAMESPACE, params.KUBE_FLAVOUR, params.CLUSTER_NAME, params.CLUSTER_REGION, "", params.CONTEXT_ROOT_PATH, params.DX_CORE_HOME_PATH, params.PERSONALIZED_DX_CORE_PATH, params.DEPLOYMENT_LEVEL, params.DEPLOYMENT_METHOD, params.DOMAIN_SUFFIX)
                     build(job: "${params.KUBE_DEPLOY_JOB}", 
                          parameters: buildParams, 
                          propagate: true,
                          wait: true)
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
                                git clone -b ${env.TARGET_BRANCH} git@git.cwp.pnp-hcl.com:Team-Q/Portal-Performance-Tests.git ${workspace}/Portal-Performance-Tests
                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'sudo mkdir -p /opt/Portal-Performance-Tests && sudo chown centos: /opt/Portal-Performance-Tests'
                                scp -r -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/Portal-Performance-Tests centos@${env.INSTANCE_IP}:/opt
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
            options {
                timeout(time: 90, unit: 'MINUTES') 
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        sleep 800
                        def nowDate = new SimpleDateFormat("M-d-Y-HHmmss")
                        def date = new Date()
                        fileName  = "test_log_dam_api_performance_tests_for_upload_assets_${nowDate.format(date)}.jtl"
                        Exception caughtException = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo "Running tests"
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && chmod 400 ./keys/Performance-Test-Keypair.pem'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && scp -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem -r jmeter/DAM_Performance_Test/DAMRegression centos@performance-test-jmeter-master.team-q-dev.com:~/data/dam'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \' env JVM_ARGS="-Dnashorn.args=--no-deprecation-warning" jmeter -JProtocol=${env.SERVER_PROTOCOL} -JHost=${env.SERVER_HOST} -JPort=${env.SERVER_PORT} -JRingAPIPort='' -JDAMPort='' -JImagePath=${env.SERVER_IMAGEPATH} -JCSVPath=${env.SERVER_CSVPATH} -j jmeter.save.saveservice.output_format=xml -n -t /home/centos/data/dam/DAMRegression/${TARGET_JMX_FILE} -l /home/centos/test_logs/${fileName}\''
                                """
                                 jmeter_error=sh(script:"ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \'grep -o -i Exception /home/centos/test_logs/${fileName} | wc -l\''",returnStdout: true).trim() as Integer
                                if ("${jmeter_error}">0) {
                                    sh "exit 1"
                                } else {
                                    // copy jtl file and capture the upload time from jtl file
                                    sh """
                                    echo "capture upload time from jtl file"
                                    chmod 600 ${DEPLOY_KEY}
                                    cp $DEPLOY_KEY test-automation-deployments.pem
                                    chmod 0600 test-automation-deployments.pem
                                    scp -v -i test-automation-deployments.pem -o StrictHostKeyChecking=no ${workspace}/kube/lifecycle/scripts/common/capture_api_response_time.sh centos@${env.NAMESPACE}${params.DOMAIN_SUFFIX}:/home/centos/native-kube/
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && scp -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com:~/test_logs/${fileName} .'
                                    scp -r -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no centos@${env.INSTANCE_IP}:/opt/Portal-Performance-Tests/${fileName} .
                                    scp -v -i test-automation-deployments.pem -o StrictHostKeyChecking=no ./${fileName} centos@${env.NAMESPACE}${params.DOMAIN_SUFFIX}:/home/centos/native-kube/
                                    ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${env.NAMESPACE}${params.DOMAIN_SUFFIX}   '(sh /home/centos/native-kube/capture_api_response_time.sh ${fileName})'
                                    rm ${fileName}
                                    """
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
                                    scp -v -i test-automation-deployments.pem -o StrictHostKeyChecking=no ./capture-dam-operations-time.sh centos@${env.NAMESPACE}${params.DOMAIN_SUFFIX}:/home/centos/native-kube/
                                """

                                /* Run bash script to capture operations time */
                                sh  """
                                    ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${env.NAMESPACE}${params.DOMAIN_SUFFIX} \
                                    '(sh /home/centos/native-kube/capture-dam-operations-time.sh)'
                                """
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
         * Execute JMeter script for DAM GET calls. 
         */ 
        stage('Run JMeter performance tests DAM GET apis') {
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
                        fileName  = "test_log_dam_get_api_${nowDate.format(date)}.jtl"
                        Exception caughtException = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo "Running tests"
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && chmod 400 ./keys/Performance-Test-Keypair.pem'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && scp -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem -r jmeter/DAM_Performance_Test/DAMRegression centos@performance-test-jmeter-master.team-q-dev.com:~/data/dam'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \' env JVM_ARGS="-Dnashorn.args=--no-deprecation-warning" jmeter -JProtocol=${env.SERVER_PROTOCOL} -JHost=${env.SERVER_HOST} -JPort=${env.SERVER_PORT} -JRingAPIPort='' -JDAMPort='' -JImagePath=${env.SERVER_IMAGEPATH} -JCSVPath=${env.SERVER_CSVPATH} -j jmeter.save.saveservice.output_format=xml -n -t /home/centos/data/dam/DAMRegression/${TARGET_JMX_FILE_FOR_DAM_GET_API} -l /home/centos/test_logs/${fileName}\''
                                """
                                 jmeter_error=sh(script:"ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \'grep -o -i Exception /home/centos/test_logs/${fileName} | wc -l\''",returnStdout: true).trim() as Integer
                                if ("${jmeter_error}">0) {
                                    sh "exit 1"
                                }else {
                                    // fetch average time for dam get apis 
                                    sh """
                                    echo "capture average time DAM GET API "
                                    chmod 600 ${DEPLOY_KEY}
                                    scp -r -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/kube/lifecycle/scripts/common/capture_dam_get_api_mean_time.sh centos@${env.INSTANCE_IP}:/opt/Portal-Performance-Tests
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && scp -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem ./capture_dam_get_api_mean_time.sh centos@performance-test-jmeter-master.team-q-dev.com:~/test_logs'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \' env JVM_ARGS="-Dnashorn.args=--no-deprecation-warning" jmeter -g /home/centos/test_logs/${fileName} -o /home/centos/test_logs/test_log_dam_get_apis \''
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com sh /home/centos/test_logs/capture_dam_get_api_mean_time.sh "/home/centos/test_logs/test_log_dam_get_apis/content/js/dashboard.js"'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com 'rm -rf /home/centos/test_logs/test_log_dam_get_apis''
                                    """
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
    }
     /*
     * Perform proper cleanup to leave a healthy jenkins agent. On build unstable/failure/aborted/success we clean up the EC2 instance.
     */ 
    post {
         unstable {
            script {
                cleanupInstances()
            }
        }
        failure {
            script {
               cleanupInstances()
            }
        }
        aborted {
            script {
                cleanupInstances()
            }
        }
        success {
            script {
               cleanupInstances()
            }
        }
        cleanup {
            script {
                /* Cleanup workspace */
                dir("${workspace}") {
                    deleteDir()
                }
                
                /* Cleanup workspace@tmp */
                dir("${workspace}@tmp") {
                    deleteDir()
                }
                
                /* remove internal instance from known-hosts */
                if (env.INSTANCE_IP) {
                    sh(script: """
                        ssh-keygen -R ${env.INSTANCE_IP} | true
                        rm -f ~/.ssh/known_hosts.old
                    """)
                }
            }
        }
    }
}