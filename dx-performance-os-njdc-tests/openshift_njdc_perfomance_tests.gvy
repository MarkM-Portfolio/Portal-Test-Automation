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
                        env.VUSERS_PER_TYPE = '6'
                        env.UPLOAD_TIME_THRESHOLD_VALUE_NJDC = '7 minutes 43 seconds'
                        env.OPERATION_TIME_THRESHOLD_VALUE_NJDC = '69 minutes 30 seconds'
                        // provide threshold in seconds 
                        env.FETCHBINARYTIME_FOR_IMAGE_THRESHOLD_VALUE_NJDC = '0.170 seconds'
                        env.FETCHBINARYTIME_FOR_VIDEO_THRESHOLD_VALUE_NJDC = '0.187 seconds'
                        env.FETCHBINARYTIME_FOR_DOCUMENT_THRESHOLD_VALUE_NJDC = '0.175 seconds'
                        env.GET_API_RESPONSE_TIME_WITH_ASSETID_THRESHOLD_VALUE_NJDC = '2.360 seconds'
                        env.ANONYMOUS_FETCHBINARYTIME_FOR_IMAGE_THRESHOLD_VALUE_NJDC = '0.177 seconds'
                        env.ANONYMOUS_FETCHBINARYTIME_FOR_VIDEO_THRESHOLD_VALUE_NJDC = '0.195 seconds'
                        env.ANONYMOUS_FETCHBINARYTIME_FOR_DOCUMENT_THRESHOLD_VALUE_NJDC = '0.183 seconds'
                        
                        if (!env.TARGET_JMX_FILE){
                            env.TARGET_JMX_FILE = 'DAM_Regression_Test.jmx'
                        }
                        echo "Assigning hostname + timestamp"
                        env.ENV_HOSTNAME = "dx_dam_regression_tests_${dateFormat.format(date)}"
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
         * Install all required software on it and make sure that our settings
         * will be copied over to the target machine.
         */
        stage('Prepare instance') {
            steps {
                      configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                          sh """
                            chmod 600 ${DEPLOY_KEY}
                            ssh -o StrictHostKeyChecking=no -i $DEPLOY_KEY root@${env.JMETER_INSTANCE_IP} 'cd /home/dam_jmeter_user && mkdir -p /dam-performance-test-reports-njdc/html && mkdir -p /dam-performance-test-reports-njdc/xml'
                            scp -o StrictHostKeyChecking=no -i $DEPLOY_KEY -r ${workspace}/test-scripts/TestReport/TestReport-njdc/* root@${env.JMETER_INSTANCE_IP}:/home/dam_jmeter_user/dam-performance-test-reports-njdc
                            scp -o StrictHostKeyChecking=no -i $DEPLOY_KEY -r ${workspace}/test-scripts/convert-log-to-xml.sh root@${env.JMETER_INSTANCE_IP}:/home/dam_jmeter_user/dam-performance-test-reports-njdc
                            scp -o StrictHostKeyChecking=no -i $DEPLOY_KEY -r ${workspace}/test-scripts/TestReport/wtf.css root@${env.JMETER_INSTANCE_IP}:/home/dam_jmeter_user/dam-performance-test-reports-njdc/wtf_njdc.css 
                          """
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
                    buildParams = commonModule.createKubeParams(pipelineParameters.NAMESPACE, pipelineParameters.KUBE_FLAVOUR, "", "", "", pipelineParameters.CONTEXT_ROOT_PATH, pipelineParameters.DX_CORE_HOME_PATH, pipelineParameters.PERSONALIZED_DX_CORE_PATH, pipelineParameters.DEPLOYMENT_LEVEL, pipelineParameters.DEPLOYMENT_METHOD, pipelineParameters.DOMAIN_SUFFIX,"")
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
                                /* change key permission */
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                """

                                /* Copy scripts to njdc jump server */
                                sh """
                                    scp -v -i $DEPLOY_KEY -o StrictHostKeyChecking=no ./capture-dam-operations-time.sh mahima.badkur@bindsrvqu.quocp.nonprod.hclpnp.com:./
                                """

                                /* oc login from njdc jump server and run bash script to capture operations time */
                                sh  """
                                    ssh -i $DEPLOY_KEY -o StrictHostKeyChecking=no mahima.badkur@bindsrvqu.quocp.nonprod.hclpnp.com oc login -u mahima.badkur -p mbPassw0rd --server=https://api.quocp.nonprod.hclpnp.com:6443 --insecure-skip-tls-verify
                                    ssh -i $DEPLOY_KEY -o StrictHostKeyChecking=no mahima.badkur@bindsrvqu.quocp.nonprod.hclpnp.com "chmod +x capture-dam-operations-time.sh && sh capture-dam-operations-time.sh "${pipelineParameters.NAMESPACE}" "${pipelineParameters.KUBE_FLAVOUR}""
                                """

                                /* Copy performane results log from njdc jump server */
                                sh """
                                    scp -v -i $DEPLOY_KEY -o StrictHostKeyChecking=no mahima.badkur@bindsrvqu.quocp.nonprod.hclpnp.com:~/dam_performance_results_operation_time.log ${workspace}/dx-performance-os-njdc-tests/
                                    ssh -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no mahima.badkur@bindsrvqu.quocp.nonprod.hclpnp.com " rm ~/dam_performance_results_operation_time.log" 
                                    cat ${workspace}/dx-performance-os-njdc-tests/dam_performance_results_operation_time.log
                                """
                            } catch(Exception err) {
                                echo "Error: ${err}"
                                currentBuild.result = "FAILURE"
                                sh """
                                    printf "Failed," >>  ${workspace}/dx-performance-os-njdc-tests/dam_performance_results_operation_time.log
                                    cat ${workspace}/dx-performance-os-njdc-tests/dam_performance_results_operation_time.log
                                """
                            }
                        }
                    }
                }
            }
        }
        /*
         * Execute JMeter script for fetching binaries.  
         */ 
        stage('Run JMeter performance tests for fetching binaries') {
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
                        fetchBinaryFileName  = "test_log_dam_regression_for_fetch_binary_${nowDate.format(date)}.jtl"
                        runCommand = "/opt/apache-jmeter/bin/./jmeter -n -t /opt/apache-jmeter-5.4.3/data/DAM_Fetch_binary_API_performance_test_njdc.jmx -R ${env.JMETER_AGENT1},${env.JMETER_AGENT2} -l /home/dam_jmeter_user/${fetchBinaryFileName} -e -f -o dam_FetchBinary -GProtocol=${env.SERVER_PROTOCOL} -GHost=${env.SERVER_HOST} -GPort=${env.SERVER_PORT} -GRingAPIPort=${env.SERVER_RINGAPI_PORT} -GDAMPort=${env.SERVER_DAMAPI_PORT} -GImagePath=${env.SERVER_BINARYASSETSPATH} -GCSVPath=${env.SERVER_CSVPATH} -GTEST_DURATION=${env.TEST_DURATION} -GVUSERS_PER_TYPE=${env.VUSERS_PER_TYPE}  -Gserver.rmi.ssl.keystore.file=/opt/apache-jmeter/bin/rmi_keystore.jks"
                        validateCommand = "grep -i -e Exception -e failed ${fetchBinaryFileName} | wc -l"
                        Exception caughtException = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo "Running tests"
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i $DEPLOY_KEY root@${env.JMETER_INSTANCE_IP} 'su - dam_jmeter_user -c "${runCommand}"'
                                    scp -o StrictHostKeyChecking=no -i $DEPLOY_KEY -r root@${env.JMETER_INSTANCE_IP}:/home/dam_jmeter_user/dam_FetchBinary ${workspace}/dx-performance-os-njdc-tests
                                """
                                 jmeter_error=sh(script:"""ssh -o StrictHostKeyChecking=no -i $DEPLOY_KEY root@${env.JMETER_INSTANCE_IP} 'cd /home/dam_jmeter_user && chmod +x ${fetchBinaryFileName} && ${validateCommand}'""",returnStdout: true).trim() as Integer
                                if ("${jmeter_error}">0) {
                                    currentBuild.result = "FAILURE"
                                    echo "Errors in JMeter script results"
                                    sh """
                                        printf "Failed,Failed,Failed," >>  ${workspace}/dx-performance-os-njdc-tests/dam_performance_binary_fetch_results.log
                                    """
                                } else {
                                    // fetch average time for image, document and video 
                                    // combine the logs of dam performance results in proper format
                                    sh """
                                        echo "capture average time for image, document and video "
                                        chmod 600 ${DEPLOY_KEY}
                                        scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/kube/lifecycle/scripts/common/capture_fetch_binary_mean_time.sh root@${env.JMETER_INSTANCE_IP}:/home/dam_jmeter_user
                                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} root@${env.JMETER_INSTANCE_IP} 'sh /home/dam_jmeter_user/capture_fetch_binary_mean_time.sh "/home/dam_jmeter_user/dam_FetchBinary/content/js/dashboard.js" "${pipelineParameters.KUBE_FLAVOUR}"'
                                        scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} root@${env.JMETER_INSTANCE_IP}:/home/dam_jmeter_user/dam_performance_binary_fetch_results.log ${workspace}/dx-performance-os-njdc-tests
                                        ssh -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no root@${env.JMETER_INSTANCE_IP} "rm /home/dam_jmeter_user/dam_performance_binary_fetch_results.log"
                                        cat ${workspace}/dx-performance-os-njdc-tests/dam_performance_binary_fetch_results.log
                                    """
                                    }
                                sh """
                                    cat ${workspace}/dx-performance-os-njdc-tests/dam_performance_results_upload_asset.log >> ${workspace}/dx-performance-os-njdc-tests/dam_performance_results.log
                                    cat ${workspace}/dx-performance-os-njdc-tests/dam_performance_results_operation_time.log >> ${workspace}/dx-performance-os-njdc-tests/dam_performance_results.log
                                    echo "${env.ENV_HOSTNAME}," >> ${workspace}/dx-performance-os-njdc-tests/dam_performance_results.log
                                    cat ${workspace}/dx-performance-os-njdc-tests/dam_performance_results.log
                                    cat ${workspace}/dx-performance-os-njdc-tests/dam_performance_binary_fetch_results.log >> ${workspace}/dx-performance-os-njdc-tests/dam_performance_results.log
                                    cat ${workspace}/dx-performance-os-njdc-tests/dam_performance_results.log
                                    rm ${workspace}/dx-performance-os-njdc-tests/dam_performance_results_upload_asset.log
                                    rm ${workspace}/dx-performance-os-njdc-tests/dam_performance_results_operation_time.log
                                    rm ${workspace}/dx-performance-os-njdc-tests/dam_performance_binary_fetch_results.log   
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

        /*
         * Execute JMeter script for friendly URL.  
         */ 
        stage('Run 1 hour JMeter performance tests for Friendly URL') {
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
                        friendlyUrlFileName  = "test_log_dam_regression_for_friendly_url_${nowDate.format(date)}.jtl"
                        runCommand = "/opt/apache-jmeter/bin/./jmeter -n -t /opt/apache-jmeter-5.4.3/data/DAM_API_Performance_Tests_for_getapi_for_FriendlyUrl_opt.jmx -R ${env.JMETER_AGENT1},${env.JMETER_AGENT2} -l /home/dam_jmeter_user/${friendlyUrlFileName} -e -f -o dam_FriendlyUrl -GProtocol=${env.SERVER_PROTOCOL} -GHost=${env.SERVER_HOST} -GPort=${env.SERVER_PORT} -GRingAPIPort=${env.SERVER_RINGAPI_PORT} -GDAMPort=${env.SERVER_DAMAPI_PORT} -GImagePath=${env.SERVER_BINARYASSETSPATH} -GCSVPath=${env.SERVER_CSVPATH} -GTEST_DURATION=${env.TEST_DURATION} -GVUSERS_PER_TYPE=${env.VUSERS_PER_TYPE} -Gserver.rmi.ssl.keystore.file=/opt/apache-jmeter/bin/rmi_keystore.jks"
                        validateCommand = "grep -i -e Exception -e failed ${friendlyUrlFileName} | wc -l"
                        Exception caughtException = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo "Running tests"
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i $DEPLOY_KEY root@${env.JMETER_INSTANCE_IP} 'su - dam_jmeter_user -c "${runCommand}"'
                                    scp -o StrictHostKeyChecking=no -i $DEPLOY_KEY -r root@${env.JMETER_INSTANCE_IP}:/home/dam_jmeter_user/dam_FriendlyUrl ${workspace}/dx-performance-os-njdc-tests
                                """
                                 jmeter_error=sh(script:"""ssh -o StrictHostKeyChecking=no -i $DEPLOY_KEY root@${env.JMETER_INSTANCE_IP} 'cd /home/dam_jmeter_user && chmod +x ${friendlyUrlFileName} && ${validateCommand}'""",returnStdout: true).trim() as Integer
                                if ("${jmeter_error}">0) {
                                    currentBuild.result = "FAILURE"
                                    echo "Errors in JMeter script results"
                                    sh """
                                        printf "Failed,Failed,Failed," >>  ${workspace}/dx-performance-os-njdc-tests/dam_performance_friendly_url_results.log
                                    """
                                } else {
                                    // get api time with assetID, assetName, customURL 
                                    // combine the logs of dam performance results in proper format
                                    // retrieve values from logs to xml file and remove all log files
                                    sh """
                                        echo "capture average time for image, document and video "
                                        chmod 600 ${DEPLOY_KEY}
                                        scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/kube/lifecycle/scripts/common/capture-dam-friendly-url-mean-time.sh root@${env.JMETER_INSTANCE_IP}:/home/dam_jmeter_user/dam-performance-test-reports-njdc
                                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} root@${env.JMETER_INSTANCE_IP} 'sh /home/dam_jmeter_user/dam-performance-test-reports-njdc/capture-dam-friendly-url-mean-time.sh "/home/dam_jmeter_user/dam-performance-test-reports-njdc/dam_FriendlyUrl/content/js/dashboard.js" "${pipelineParameters.KUBE_FLAVOUR}"'
                                        scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} root@${env.JMETER_INSTANCE_IP}:/home/dam_jmeter_user/dam_performance_friendly_url_results.log ${workspace}/dx-performance-os-njdc-tests
                                        ssh -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no root@${env.JMETER_INSTANCE_IP} " cd /home/dam_jmeter_user && rm dam_performance_friendly_url_results.log"
                                        cat ${workspace}/dx-performance-os-njdc-tests/dam_performance_friendly_url_results.log
                                    """
                                    }
                                sh """
                                    cat ${workspace}/dx-performance-os-njdc-tests/dam_performance_results.log
                                    cat ${workspace}/dx-performance-os-njdc-tests/dam_performance_friendly_url_results.log >> ${workspace}/dx-performance-os-njdc-tests/dam_performance_results.log
                                    rm ${workspace}/dx-performance-os-njdc-tests/dam_performance_friendly_url_results.log                             
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

        /*
         * Execute JMeter script for DAM Anonymous fetching binaries.  
         */ 
        stage('Run JMeter performance tests for DAM Anonymous fetching binaries') {
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
                        anonymousFetchBinaryFileName  = "test_log_dam_anonymous_regression_for_fetch_binary_${nowDate.format(date)}.jtl"
                        runCommand = "/opt/apache-jmeter/bin/./jmeter -n -t /opt/apache-jmeter-5.4.3/data/DAM_Anonymous_Fetch_binary_API_performance_test.jmx -R ${env.JMETER_AGENT1},${env.JMETER_AGENT2} -l /home/dam_jmeter_user/${anonymousFetchBinaryFileName} -e -f -o dam_Anonymous_FetchBinary -GProtocol=${env.SERVER_PROTOCOL} -GHost=${env.SERVER_HOST} -GPort=${env.SERVER_PORT} -GRingAPIPort=${env.SERVER_RINGAPI_PORT} -GDAMPort=${env.SERVER_DAMAPI_PORT} -GImagePath=${env.SERVER_BINARYASSETSPATH} -GCSVPath=${env.SERVER_CSVPATH} -GTEST_DURATION=${env.TEST_DURATION} -GVUSERS_PER_TYPE=${env.VUSERS_PER_TYPE}  -Gserver.rmi.ssl.keystore.file=/opt/apache-jmeter/bin/rmi_keystore.jks"
                        validateCommand = "grep -i -e Exception -e failed ${anonymousFetchBinaryFileName} | wc -l"
                        Exception caughtException = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo "Running tests"
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i $DEPLOY_KEY root@${env.JMETER_INSTANCE_IP} 'su - dam_jmeter_user -c "${runCommand}"'
                                    scp -o StrictHostKeyChecking=no -i $DEPLOY_KEY -r root@${env.JMETER_INSTANCE_IP}:/home/dam_jmeter_user/dam_Anonymous_FetchBinary ${workspace}/dx-performance-os-njdc-tests
                                """
                                 jmeter_error=sh(script:"""ssh -o StrictHostKeyChecking=no -i $DEPLOY_KEY root@${env.JMETER_INSTANCE_IP} 'cd /home/dam_jmeter_user && chmod +x ${anonymousFetchBinaryFileName} && ${validateCommand}'""",returnStdout: true).trim() as Integer
                                if ("${jmeter_error}">0) {
                                    currentBuild.result = "FAILURE"
                                    echo "Errors in JMeter script results"
                                    sh """
                                        printf "Failed,Failed,Failed," >>  ${workspace}/dx-performance-os-njdc-tests/dam_anonymous_performance_binary_fetch_results.log
                                    """
                                } else {
                                    // fetch average time for image, document and video 
                                    // combine the logs of dam performance results in proper format
                                    sh """
                                        echo "Anonymous access to assets - capturing average response time for image, document and video "
                                        chmod 600 ${DEPLOY_KEY}
                                        scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/kube/lifecycle/scripts/common/capture_fetch_binary_mean_time.sh root@${env.JMETER_INSTANCE_IP}:/home/dam_jmeter_user
                                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} root@${env.JMETER_INSTANCE_IP} 'sh /home/dam_jmeter_user/capture_fetch_binary_mean_time.sh "/home/dam_jmeter_user/dam_Anonymous_FetchBinary/content/js/dashboard.js" "${pipelineParameters.KUBE_FLAVOUR}" "anonymous"'
                                        scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} root@${env.JMETER_INSTANCE_IP}:/home/dam_jmeter_user/dam_anonymous_performance_binary_fetch_results.log ${workspace}/dx-performance-os-njdc-tests
                                        ssh -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no root@${env.JMETER_INSTANCE_IP} "rm /home/dam_jmeter_user/dam_anonymous_performance_binary_fetch_results.log"
                                        cat ${workspace}/dx-performance-os-njdc-tests/dam_anonymous_performance_binary_fetch_results.log
                                    """
                                    sh """
                                        cat ${workspace}/dx-performance-os-njdc-tests/dam_performance_results.log
                                        cat ${workspace}/dx-performance-os-njdc-tests/dam_anonymous_performance_binary_fetch_results.log >> ${workspace}/dx-performance-os-njdc-tests/dam_performance_results.log
                                        sh ${workspace}/test-scripts/convert-log-to-xml.sh ${workspace} "${pipelineParameters.KUBE_FLAVOUR}"
                                        rm ${workspace}/dx-performance-os-njdc-tests/dam_performance_results.log 
                                        rm ${workspace}/dx-performance-os-njdc-tests/dam_anonymous_performance_binary_fetch_results.log                             
                                        cat ${workspace}/dx-performance-os-njdc-tests/Dam_Performance_Tests_Results_Njdc.xml
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
                                        echo "Generating performance test dashboard report for dam apis"
                                        // Copy previous runs file from s3 bucket
                                        // Execute script to generate report
                                        // Copy reports(html and css) to s3 bucket                                         
                                        sh """
                                                chmod 600 ${DEPLOY_KEY}                                 
                                                aws s3 cp s3://dx-testarea/performance-test-reports/dam-performance-tests-combined-runs-njdc.xml dam-performance-tests-combined-runs-njdc.xml
                                                scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/dam-performance-tests-combined-runs-njdc.xml root@${env.JMETER_INSTANCE_IP}:/home/dam_jmeter_user/dam-performance-test-reports-njdc
                                                scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/dx-performance-os-njdc-tests/testreport.xml root@${env.JMETER_INSTANCE_IP}:/home/dam_jmeter_user/dam-performance-test-reports-njdc
                                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} root@${env.JMETER_INSTANCE_IP} 'cd /home/dam_jmeter_user && chmod +x /home/dam_jmeter_user/dam-performance-test-reports-njdc/dam-performance-test-report-njdc.sh && sh /home/dam_jmeter_user/dam-performance-test-reports-njdc/dam-performance-test-report-njdc.sh snapshotDir="https://dxcore-test-results-secure.team-q-dev.com/DX-95-TestReports/${env.REPORT_BUILD_NAME}_dam-performance-test-njdc" && cat /home/dam_jmeter_user/dam-performance-test-reports-njdc/dam-jtl-Report-njdc.html'
                                                scp -o StrictHostKeyChecking=no -i $DEPLOY_KEY root@${env.JMETER_INSTANCE_IP}:/home/dam_jmeter_user/dam-performance-test-reports-njdc/dam-jtl-Report-njdc.html ${workspace}/dx-performance-os-njdc-tests/dam-jtl-Report-njdc.html
                                                scp -o StrictHostKeyChecking=no -i $DEPLOY_KEY root@${env.JMETER_INSTANCE_IP}:/home/dam_jmeter_user/dam-performance-test-reports-njdc/wtf_njdc.css ${workspace}/dx-performance-os-njdc-tests/wtf_njdc.css
                                                cd ${workspace}/dx-performance-os-njdc-tests && tar -czf ${env.REPORT_BUILD_NAME}_dam-performance-test-njdc.zip dam-jtl-Report-njdc.html wtf_njdc.css dam_FriendlyUrl/* dam_FetchBinary/* dam_Upload/* dam_Anonymous_FetchBinary/*
                                                scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/dx-performance-os-njdc-tests/Dam_Performance_Tests_Results_Njdc.xml root@${env.JMETER_INSTANCE_IP}:/home/dam_jmeter_user/dam-performance-test-reports-njdc
                                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} root@${env.JMETER_INSTANCE_IP} 'cd /home/dam_jmeter_user/dam-performance-test-reports-njdc/ && chmod +x dam_performance_test_run_njdc.sh && sh dam_performance_test_run_njdc.sh ${env.REPORT_BUILD_NAME}_dam-performance-test-njdc PerformanceNjdc "${env.UPLOAD_TIME_THRESHOLD_VALUE_NJDC}" "${env.OPERATION_TIME_THRESHOLD_VALUE_NJDC}" "${env.FETCHBINARYTIME_FOR_IMAGE_THRESHOLD_VALUE_NJDC}" "${env.FETCHBINARYTIME_FOR_VIDEO_THRESHOLD_VALUE_NJDC}" "${env.FETCHBINARYTIME_FOR_DOCUMENT_THRESHOLD_VALUE_NJDC}" "${env.GET_API_RESPONSE_TIME_WITH_ASSETID_THRESHOLD_VALUE_NJDC}" "${env.ANONYMOUS_FETCHBINARYTIME_FOR_IMAGE_THRESHOLD_VALUE_NJDC}" "${env.ANONYMOUS_FETCHBINARYTIME_FOR_VIDEO_THRESHOLD_VALUE_NJDC}" "${env.ANONYMOUS_FETCHBINARYTIME_FOR_DOCUMENT_THRESHOLD_VALUE_NJDC}"'
                                                scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} root@${env.JMETER_INSTANCE_IP}:/home/dam_jmeter_user/dam-performance-test-reports-njdc/dam-performance-tests-combined-runs-njdc.xml .
                                                scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r  root@${env.JMETER_INSTANCE_IP}:/home/dam_jmeter_user/dam-performance-test-reports-njdc/dashboard/* .
                                                aws s3 cp ${env.REPORT_BUILD_NAME}_dam-performance-test-njdc.zip s3://dx-testarea/
                                                aws s3 cp dam-performance-tests-combined-runs-njdc.xml s3://dx-testarea/performance-test-reports/
                                                cat PerformanceNjdc-dashboard.html
                                                cat wtf_njdc.css
                                                aws s3 cp PerformanceNjdc-dashboard.html s3://dx-testarea/performance-test-reports/ 
                                                aws s3 cp wtf_njdc.css s3://dx-testarea/performance-test-reports/    
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
       post {
        cleanup {
            dxWorkspaceDirectoriesCleanup()
        }
    } 
 }