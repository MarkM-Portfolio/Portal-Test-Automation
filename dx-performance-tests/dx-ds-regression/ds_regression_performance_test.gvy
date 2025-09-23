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

def cleanupInstances() {
     withCredentials([
                    usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')
                ]) {
                    dir("${workspace}/dx-performance-tests/dx-ds-regression/terraform/ec2-launch") {
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
      string(name: 'NAMESPACE', defaultValue: 'ds_regression_test',description: 'name space')
      string(name: 'KUBE_FLAVOUR', defaultValue: 'native', description: 'Deploying a native kube environment.')
      string(name: 'KUBE_UNDEPLOY_JOB', defaultValue: 'CI/kube-deploy/native-kube-remove', description: 'Job which undeploys the environment',  trim: false)
      string(name: 'KUBE_DEPLOY_JOB', defaultValue: 'CI/kube-deploy/native-kube-next-deploy', description: 'Job which deploys the environment',  trim: false)
      choice(name: 'DEPLOYMENT_METHOD', choices: ['helm', 'dxctl'],  description: 'Select deployment method')
      string(name: 'CLUSTER_NAME', defaultValue: '', description: 'Cluster name where the deployment should be deployed to')
      string(name: 'CLUSTER_REGION', defaultValue: '', description: 'Region of the cluster')
      string(name: 'CONTEXT_ROOT_PATH', defaultValue: 'wps', description: 'Context root')
      string(name: 'DX_CORE_HOME_PATH', defaultValue: 'portal', description: 'Home path')
      string(name: 'PERSONALIZED_DX_CORE_PATH', defaultValue: 'myportal', description: 'Personalized path')
      string(name: 'DOMAIN_SUFFIX', defaultValue: '.hcl-dx-dev.net', description: ' ')
      booleanParam(name: 'SSL_ENABLED', defaultValue: true, description: 'Required for testing environments with https/self-signed certificates like native.kube.')
    }

    stages {
        stage("Load modules and configuration") {
            steps {
                script {
                    commonModule = load "./autonomous-deployments/modules/common.gvy"
                    commonConfig = load "./autonomous-deployments/config/common.gvy"
                    env.NAMESPACE = "regression-ds-${DEPLOYMENT_METHOD}-${DEPLOYMENT_LEVEL}"
                }
            }
        }

        stage('Prepare Settings') {
            steps {
                /*
                 * Assigning hostname using Performace-test-automation + the timestap
                 */
                dir("${workspace}/dx-performance-tests/dx-ds-regression") {
                    script {
                        // determine build version and label current job accordingly
                        def dateFormat = new SimpleDateFormat("yyyyMMdd-HHmm")
                        def date = new Date()
                        env.SERVER_PROTOCOL = 'https'
                        env.SERVER_HOST = "${env.NAMESPACE}${DOMAIN_SUFFIX}"
                        env.SERVER_PORT = ''
                        env.SERVER_RINGAPI_PORT = ''
                        env.SERVER_DSAPI_PORT = ''
                        env.SERVER_IMAGEPATH = '/home/centos/data/ds/DSRegression/Dataset'
                        env.SERVER_CSVPATH = '/home/centos/data/ds/DSRegression/Dataset/ds_users.csv'
                        env.SERVER_DOCUMENTSPATH = '/home/centos/data/ds/DSRegression/Dataset/documents'
                        env.SERVER_IMAGESPATH = '/home/centos/data/ds/DSRegression/Dataset/images'
                        env.SERVER_VIDEOSPATH = '/home/centos/data/ds/DSRegression/Dataset/videos'
                        if (!env.TARGET_JMX_FILE){
                            env.TARGET_JMX_FILE = 'Baseline-V2-SM-SitesandPages.jmx'
                        }
                      
                        echo "Assigning hostname + timestamp"
                        env.ENV_HOSTNAME = "dx_ds_regression_tests_${dateFormat.format(date)}"
                        echo "New hostname will be: ${env.ENV_HOSTNAME}"

                        // Display name includes the ENV_HOSTNAME and a timestamp
                        currentBuild.displayName = "${env.ENV_HOSTNAME}"

                        if (env.SERVER_HOST) {
                            def currentDate = "${dateFormat.format(date)}"
                            currentBuild.displayName = "${env.SERVER_HOST}_${currentDate}"
                            def removeHTTPFromDisplayName = currentBuild.displayName.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)","")
                            def tempReportBuildName = removeHTTPFromDisplayName.split('/')
                            def removeSpecialCharacter = tempReportBuildName[0].replace(':','')
                            if (removeHTTPFromDisplayName.contains('/')) {
                                tempReportBuildName = "${tempReportBuildName[0]}_${currentDate}"
                                removeSpecialCharacter = tempReportBuildName.replace(':','')
                            }
                            env.REPORT_BUILD_NAME = "${removeSpecialCharacter}"

                            echo "REPORT_BUILD_NAME ${env.REPORT_BUILD_NAME}"
                        }


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
                            dir("${workspace}/dx-performance-tests/dx-ds-regression/terraform/ec2-launch") {
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
                            	    sh ${workspace}/dx-performance-tests/dx-ds-regression/scripts/wait_for_instance.sh
                                """)
                            }
                        }
                    }
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
                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/dx-performance-tests/dx-ds-regression/scripts/install-prereqs.sh centos@${env.INSTANCE_IP}:/tmp
                            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'chmod +x /tmp/install-prereqs.sh && sh /tmp/install-prereqs.sh ${env.HOST_IP_ADDRESS} ${env.HOSTNAME}'
                            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'sudo mkdir -p /opt/ds-perfomance-regression-reports && sudo chown centos: /opt/ds-perfomance-regression-reports'
                            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'mkdir -p /opt/ds-perfomance-regression-reports/html && mkdir -p /opt/ds-perfomance-regression-reports/xml'
                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r ${workspace}/test-scripts/TestReport/* centos@${env.INSTANCE_IP}:/opt/ds-perfomance-regression-reports
                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r ${workspace}/dx-core-tests/dx-test-rest/helpers/Saxon-HE-9.5.1-3.jar centos@${env.INSTANCE_IP}:/opt/ds-perfomance-regression-reports/Saxon-HE-9.5.1-3.jar
                          """
                      }
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
         * Once scripts are copied to EC2 instance, we need to run the JMeter scripts for sites and pages creation 
         */ 
        stage('Run JMeter performance tests for sites and pages creation') {
            options {
                timeout(time: 90, unit: 'MINUTES') 
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        sleep 900
                        def nowDate = new SimpleDateFormat("M-d-Y-HHmmss")
                        def date = new Date()
                        fileName  = "test_log_ds_regression_${nowDate.format(date)}.jtl"
                        Exception caughtException = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo "Running tests"
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && chmod 400 ./keys/Performance-Test-Keypair.pem'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && scp -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem jmeter/WCMV1VersusV2REST/${TARGET_JMX_FILE} centos@performance-test-jmeter-master.team-q-dev.com:~/data/${TARGET_JMX_FILE}'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \' env JVM_ARGS="-Dnashorn.args=--no-deprecation-warning" jmeter -JProtocol=${env.SERVER_PROTOCOL} -JHost=${env.SERVER_HOST} -j jmeter.save.saveservice.output_format=xml -n -t /home/centos/data/${TARGET_JMX_FILE} -l /home/centos/test_logs/${fileName} -e -f -o ds-regression-report\''
                                """
                                 jmeter_error=sh(script:"ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \'grep -o -i Exception /home/centos/test_logs/${fileName} | wc -l\''",returnStdout: true).trim() as Integer
                                if ("${jmeter_error}">0) {
                                    currentBuild.result = "FAILURE"
                                    echo "Errors in JMeter script results"
                                } 
                                        sh """
                                        echo "capture upload time from jtl file"
                                        chmod 600 ${DEPLOY_KEY}
                                        cp $DEPLOY_KEY test-automation-deployments.pem
                                        chmod 0600 test-automation-deployments.pem
                                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \'tar -czf ${env.REPORT_BUILD_NAME}_ds_performance_test.zip ./ds-regression-report/*\''
                                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && scp -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com:~/${env.REPORT_BUILD_NAME}_ds_performance_test.zip .'
                                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \'rm ${env.REPORT_BUILD_NAME}_ds_performance_test.zip\''
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
                                        echo "Generating performance test dashboard report for ds apis"
                                        // Copy previous runs file from s3 bucket
                                        // Copy current run and previous run file to EC2 instance
                                        // Execute script to generate report
                                        // Copy reports(html and css) to s3 bucket 
                                        sh """
                                            chmod 600 ${DEPLOY_KEY}
                                            aws s3 cp s3://dx-testarea/ds-regression-reports/ds-regression-test-combined-runs.xml ds-regression-test-combined-runs.xml
                                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/ds-regression-test-combined-runs.xml centos@${env.INSTANCE_IP}:/opt/ds-perfomance-regression-reports/
                                            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/ds-perfomance-regression-reports/ && chmod +x ds-regression-test-dashboard.sh && sh ds-regression-test-dashboard.sh ${env.REPORT_BUILD_NAME}_ds_performance_test DS-Performance'
                                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r  centos@${env.INSTANCE_IP}:/opt/ds-perfomance-regression-reports/ds-regression-test-combined-runs.xml .
                                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r  centos@${env.INSTANCE_IP}:/opt/ds-perfomance-regression-reports/dashboard/* .
                                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r  centos@${env.INSTANCE_IP}:/opt/Portal-Performance-Tests/${env.REPORT_BUILD_NAME}_ds_performance_test.zip .
                                            aws s3 cp ds-regression-test-combined-runs.xml s3://dx-testarea/ds-regression-reports/
                                            aws s3 cp DS-Performance-dashboard.html s3://dx-testarea/ds-regression-reports/ 
                                            aws s3 cp wtf.css s3://dx-testarea/ds-regression-reports/
                                            aws s3 cp ${env.REPORT_BUILD_NAME}_ds_performance_test.zip s3://dx-testarea/
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