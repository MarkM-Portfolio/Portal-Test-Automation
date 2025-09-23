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
                        env.PERFORMANCE_RUN_FLAG = 'true'
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
                    buildParams = commonModule.createKubeParams(pipelineParameters.NAMESPACE, pipelineParameters.KUBE_FLAVOUR, "", "", "", pipelineParameters.CONTEXT_ROOT_PATH, pipelineParameters.DX_CORE_HOME_PATH, pipelineParameters.PERSONALIZED_DX_CORE_PATH, pipelineParameters.DEPLOYMENT_LEVEL, pipelineParameters.DEPLOYMENT_METHOD, pipelineParameters.DOMAIN_SUFFIX, env.PERFORMANCE_RUN_FLAG)
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
        stage('Run JMeter tests to upload 15k assets') {
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
                        fileName  = "test_log_dam_regression_for_upload_assets_${nowDate.format(date)}.jtl"
                        runCommand = "/opt/apache-jmeter/bin/./jmeter -n -t /opt/apache-jmeter-5.4.3/data/DAM_Regression_Test.jmx -R ${env.JMETER_AGENT1},${env.JMETER_AGENT2} -l /home/dam_jmeter_user/${fileName} -e -f -o dam_Upload -GProtocol=${env.SERVER_PROTOCOL} -GHost=${env.SERVER_HOST} -GPort=${env.SERVER_PORT} -GRingAPIPort=${env.SERVER_RINGAPI_PORT} -GDAMPort=${env.SERVER_DAMAPI_PORT} -GDocumentsPath=${env.SERVER_DOCUMENTSPATH} -GImagesPath=${env.SERVER_IMAGESPATH} -GVideosPath=${env.SERVER_VIDEOSPATH} -GCSVPath=${env.SERVER_CSVPATH}  -Gserver.rmi.ssl.keystore.file=/opt/apache-jmeter/bin/rmi_keystore.jks"
                        validateCommand = "grep -i -e Exception -e failed ${fileName}| wc -l"
                        Exception caughtException = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo "Running tests"
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i $DEPLOY_KEY root@${env.JMETER_INSTANCE_IP} 'su - dam_jmeter_user -c "${runCommand}"'
                                    scp -o StrictHostKeyChecking=no -i $DEPLOY_KEY -r root@${env.JMETER_INSTANCE_IP}:/home/dam_jmeter_user/dam_Upload ${workspace}/dx-performance-os-njdc-tests
                                """
                                 jmeter_error=sh(script:"""ssh -o StrictHostKeyChecking=no -i $DEPLOY_KEY root@${env.JMETER_INSTANCE_IP} 'cd /home/dam_jmeter_user && chmod +x ${fileName} && ${validateCommand}'""",returnStdout: true).trim() as Integer
                                if ("${jmeter_error}">0) {
                                    echo "Errors in JMeter script results"
                                    currentBuild.result = "FAILURE"
                                    sh """
                                        chmod 600 ${DEPLOY_KEY}
                                           printf "Failed," >>  ${workspace}/dx-performance-os-njdc-tests/dam_performance_results_upload_asset.log
                                    """
                                }
                                else {
                                    // copy jtl file and capture the upload time from jtl file
                                        sh """
                                            echo "capture upload time from jtl file"
                                            chmod 600 ${DEPLOY_KEY}
                                            scp -v -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no ${workspace}/kube/lifecycle/scripts/common/capture_api_response_time.sh root@${env.JMETER_INSTANCE_IP}:/home/dam_jmeter_user/
                                            ssh -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no root@${env.JMETER_INSTANCE_IP} '(sh /home/dam_jmeter_user/capture_api_response_time.sh ${fileName} "${pipelineParameters.KUBE_FLAVOUR}")'
                                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} root@${env.JMETER_INSTANCE_IP}:/home/dam_jmeter_user/dam_performance_results_upload_asset.log ${workspace}/dx-performance-os-njdc-tests
                                            ssh -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no root@${env.JMETER_INSTANCE_IP} "rm /home/dam_jmeter_user/dam_performance_results_upload_asset.log"
                                            cat ${workspace}/dx-performance-os-njdc-tests/dam_performance_results_upload_asset.log
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
         * Run the Search Keyword JMeter scripts. 
         */ 
        stage('Run JMeter tests for Search Keyword in DAM') {
            options {
                timeout(time: 150, unit: 'MINUTES') 
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        // wait for all 15k asset's keywords generation to complete using stub mode
                        sleep 900
                        def nowDate = new SimpleDateFormat("M-d-Y-HHmmss")
                        def date = new Date()
                        fileName  = "log_dam_keyword_${nowDate.format(date)}.jtl"
                        runCommand = "/opt/apache-jmeter/bin/./jmeter -n -t /opt/apache-jmeter-5.4.3/data/DAM_Performance_Regression_DAM_Search_Keyword_tests.jmx -R ${env.JMETER_AGENT1},${env.JMETER_AGENT2} -l /home/dam_jmeter_user/${fileName} -e -f -o dam_Search_Keywords -GProtocol=https -GHost=${env.SERVER_HOST} -GCSVPath=/opt/apache-jmeter-5.4.3/data/files/dam_users.csv -GKeywordCSVPath=/opt/apache-jmeter-5.4.3/data/files/keyword.csv"
                        validateCommand = "grep -i -e Exception -e failed ${fileName}| wc -l"
                        Exception caughtException = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo "Running tests"
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i $DEPLOY_KEY root@${env.JMETER_INSTANCE_IP} 'su - dam_jmeter_user -c "${runCommand}"'
                                """
                                 jmeter_error=sh(script:"""ssh -o StrictHostKeyChecking=no -i $DEPLOY_KEY root@${env.JMETER_INSTANCE_IP} 'cd /home/dam_jmeter_user && chmod +x ${fileName} && ${validateCommand}'""",returnStdout: true).trim() as Integer
                                if ("${jmeter_error}">0) {
                                    echo "Errors in JMeter script results"
                                    currentBuild.result = "FAILURE"
                                } else {
                                    // fetch average response time for searching asset keywords from jtl file
                                    
                                    sh """
                                        echo "capture average response time for searching asset keywords"
                                        chmod 600 ${DEPLOY_KEY}
                                        scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/kube/lifecycle/scripts/common/capture_fetch_binary_keyword_mean_time.sh root@${env.JMETER_INSTANCE_IP}:/home/dam_jmeter_user
                                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} root@${env.JMETER_INSTANCE_IP} 'sh /home/dam_jmeter_user/capture_fetch_binary_keyword_mean_time.sh "/home/dam_jmeter_user/dam_Search_Keywords/content/js/dashboard.js" "${pipelineParameters.KUBE_FLAVOUR}"'
                                        scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} root@${env.JMETER_INSTANCE_IP}:/home/dam_jmeter_user/dam_performance_search_keywords.log ${workspace}/dx-performance-os-njdc-tests
                                        ssh -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no root@${env.JMETER_INSTANCE_IP} "rm /home/dam_jmeter_user/dam_performance_search_keywords.log"
                                        cat ${workspace}/dx-performance-os-njdc-tests/dam_performance_search_keywords.log
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
       post {
        cleanup {
            dxWorkspaceDirectoriesCleanup()
        }
    } 
 }