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
@Library("dx-shared-library") _
/*
 * Pipeline Module for njdc openshift k8s deployment for DX performance tests
 * Contains all necessary functions to perform a working openshift k8s deployment and teardown
 */

def pipelineParameters = [:]

/* Common paths - must be here always */
commonModuleDirectory = "./autonomous-deployments/modules"
commonConfigDirectory = "./autonomous-deployments/config"


 pipeline {

    agent {
        label 'build_infra'
    }

    stages {
        stage("Load modules and configuration") {
            steps {
                script {
                    dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/dx-performance-os-njdc-tests/parameters.yaml")
                    commonModule = load "${commonModuleDirectory}/common.gvy"
                    commonConfig = load "${commonConfigDirectory}/common.gvy"
                }
            }
        }

    stage('Prepare Settings') {
            steps {
                /*
                 * Assigning hostname using Performace-test-automation + the timestap
                 */
                dir("${WORKSPACE}/dx-performance-os-njdc-tests") {
                    script {
                        // determine build version and label current job accordingly
                        def dateFormat = new SimpleDateFormat("yyyyMMdd-HHmm")
                        def date = new Date()
                        env.SERVER_PROTOCOL = 'https' 
                        env.SERVER_HOST = "dx-deployment-passthrough-${pipelineParameters.NAMESPACE}.apps.quocp.nonprod.hclpnp.com"                        
                        env.SERVER_PORT = ''
                        env.SERVER_RINGAPI_PORT = ''
                        env.SERVER_DAMAPI_PORT = ''
                        env.SERVER_CSVPATH = '/opt/apache-jmeter-5.4.3/data/files/dam_users.csv'
                        env.SERVER_DOCUMENTSPATH = '/opt/apache-jmeter-5.4.3/data/files/documents'
                        env.SERVER_IMAGESPATH = '/opt/apache-jmeter-5.4.3/data/files/images'
                        env.SERVER_VIDEOSPATH = '/opt/apache-jmeter-5.4.3/data/files/videos'
                        env.SERVER_BINARYASSETSPATH = '/opt/apache-jmeter-5.4.3/data/files'
                        env.JMETER_AGENT1 = '10.190.75.29'
                        env.JMETER_AGENT2 = '10.190.75.31'
                        env.JMETER_INSTANCE_IP = '10.190.75.28'
                        env.TEST_DURATION = '3600'
                        
                        if (!env.TARGET_JMX_FILE){
                            env.TARGET_JMX_FILE = 'DAM_Regression_Test.jmx'
                        }
                        if (!env.TEST_DURATION){
                            env.TEST_DURATION = '3600'
                        }
                        echo "Assigning hostname + timestamp"
                        env.ENV_HOSTNAME = "dx_dam_regression_tests_${dateFormat.format(date)}"
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

        stage('Undeploying the application in k8 environment') {
            steps {
                script {
                    buildParams = []
                    buildParams = commonModule.createKubeUnDeployParams(pipelineParameters.NAMESPACE, pipelineParameters.KUBE_FLAVOUR, "", "", "")
                    build(job: "${pipelineParameters.KUBE_UNDEPLOY_JOB}", 
                          parameters: buildParams, 
                          propagate: true,
                          wait: true)
                }
            }
        }

        stage('Deploying the application in k8 environment') {
            steps {
                script {
                    buildParams = []
                    buildParams = commonModule.createKubeParams(pipelineParameters.NAMESPACE, pipelineParameters.KUBE_FLAVOUR, "", "", "", pipelineParameters.CONTEXT_ROOT_PATH, pipelineParameters.DX_CORE_HOME_PATH, pipelineParameters.PERSONALIZED_DX_CORE_PATH, pipelineParameters.DEPLOYMENT_LEVEL, pipelineParameters.DEPLOYMENT_METHOD, pipelineParameters.DOMAIN_SUFFIX)
                    buildParams.add(string(name: 'IMAGE_REPOSITORY', value: pipelineParameters.IMAGE_REPOSITORY))
                    buildParams.add(string(name: 'ENABLE_LDAP_CONFIG', value: "false"))
                    buildParams.add(string(name: 'ENABLE_DB_CONFIG', value: "false"))
                    buildParams.add(string(name: 'IS_SCHEDULED', value: "false"))
                    build(job: "${pipelineParameters.KUBE_DEPLOY_JOB}", 
                          parameters: buildParams, 
                          propagate: true,
                          wait: true)
                }
            }
        }
        /*
         * Run the JMeter scripts. 
         */ 
        stage('Run JMeter tests for DesignStudio Rendering') {
            options {
                timeout(time: 150, unit: 'MINUTES') 
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        def nowDate = new SimpleDateFormat("M-d-Y-HHmmss")
                        def date = new Date()
                        fileName  = "test_log_designStudio_${nowDate.format(date)}.jtl"
                        runCommand = "/opt/apache-jmeter/bin/./jmeter -n -t /opt/apache-jmeter-5.4.3/data/DesignStudioRendering.jmx -l /home/jmeter_user/${fileName} -GTEST_DURATION=${env.TEST_DURATION} -GHost=${env.SERVER_HOST} -Gserver.rmi.ssl.keystore.file=/opt/apache-jmeter/bin/rmi_keystore.jks"
                        validateCommand = "grep -i -e Exception -e failed ${fileName}| wc -l"
                        Exception caughtException = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo "Running tests"
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i $DEPLOY_KEY root@${env.JMETER_INSTANCE_IP} 'su - jmeter_user -c "${runCommand}"'
                                """
                                 jmeter_error=sh(script:"""ssh -o StrictHostKeyChecking=no -i $DEPLOY_KEY root@${env.JMETER_INSTANCE_IP} 'cd /home/jmeter_user && chmod +x ${fileName} && ${validateCommand}'""",returnStdout: true).trim() as Integer
                                if ("${jmeter_error}">0) {
                                    echo "Errors in JMeter script results"
                                    currentBuild.result = "FAILURE"
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
       post {
        cleanup {
            dxWorkspaceDirectoriesCleanup()
        }
    } 
 }