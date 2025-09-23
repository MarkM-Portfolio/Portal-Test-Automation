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
                            ssh -o StrictHostKeyChecking=no -i $DEPLOY_KEY root@${env.JMETER_INSTANCE_IP} 'cd /home/jmeter_user && mkdir pnp-perfomance-regression-reports'
                            scp -o StrictHostKeyChecking=no -i $DEPLOY_KEY -r ${workspace}/test-scripts/TestReport/TestReport-njdc/* root@${env.JMETER_INSTANCE_IP}:/home/jmeter_user/pnp-perfomance-regression-reports
                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r ${workspace}/dx-core-tests/dx-test-rest/helpers/Saxon-HE-9.5.1-3.jar root@${env.JMETER_INSTANCE_IP}:/home/jmeter_user/pnp-perfomance-regression-reports/Saxon-HE-9.5.1-3.jar
                            scp -o StrictHostKeyChecking=no -i $DEPLOY_KEY -r ${workspace}/test-scripts/TestReport/wtf.css root@${env.JMETER_INSTANCE_IP}:/home/jmeter_user/pnp-perfomance-regression-reports/wtf.css
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
                    buildParams = commonModule.createKubeParams(pipelineParameters.NAMESPACE, pipelineParameters.KUBE_FLAVOUR, "", "", "", pipelineParameters.CONTEXT_ROOT_PATH, pipelineParameters.DX_CORE_HOME_PATH, pipelineParameters.PERSONALIZED_DX_CORE_PATH, pipelineParameters.DEPLOYMENT_LEVEL, pipelineParameters.DEPLOYMENT_METHOD, pipelineParameters.DOMAIN_SUFFIX,"false")
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
         * Automate importing XMLAccess 
         */
         stage('Automating Import of XMLAccess') {
            steps {
                // Executing XMLAccess shell script
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
                                    scp -r -i $DEPLOY_KEY -o StrictHostKeyChecking=no ./XMLAccessFolder  srinath.tr@bindsrvqu.quocp.nonprod.hclpnp.com:./
                                """

                                /* logging in oc from jump server and executing XMLAccess shell script */
                                sh  """
                                    ssh -i $DEPLOY_KEY -o StrictHostKeyChecking=no srinath.tr@bindsrvqu.quocp.nonprod.hclpnp.com oc login -u srinath.tr -p SR11**at --server=https://api.quocp.nonprod.hclpnp.com:6443 --insecure-skip-tls-verify
                                    ssh -i $DEPLOY_KEY -o StrictHostKeyChecking=no srinath.tr@bindsrvqu.quocp.nonprod.hclpnp.com "cd XMLAccessFolder && chmod +x automate-import-XMLAccess.sh && sh automate-import-XMLAccess.sh ${env.SERVER_HOST} ${pipelineParameters.NAMESPACE}"
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
         * Run the JMeter scripts. 
         */ 
        stage('Run JMeter tests for Page and Portlet Rendering') {
            options {
                timeout(time: 150, unit: 'MINUTES') 
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        sleep 900
                        def nowDate = new SimpleDateFormat("M-d-Y-HHmmss")
                        def date = new Date()
                        fileName  = "test_log_pageportlet_${nowDate.format(date)}.jtl"
                        runCommand = "/opt/apache-jmeter/bin/./jmeter -n -t /opt/apache-jmeter-5.4.3/data/Page_and_Portlet_Rendering.jmx -R ${env.JMETER_AGENT1},${env.JMETER_AGENT2} -l /home/jmeter_user/${fileName} -e -f -o pandp_test_reports -GDX_PROTOCOL=${env.SERVER_PROTOCOL} -GTEST_DURATION=${env.TEST_DURATION} -GCSVPath=/opt/apache-jmeter-5.4.3/data/files/dam_users.csv -GHost=${env.SERVER_HOST} -Gserver.rmi.ssl.keystore.file=/opt/apache-jmeter/bin/rmi_keystore.jks"
                        validateCommand = "grep -i -e Exception -e failed ${fileName}| wc -l"
                        Exception caughtException = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo "Running tests"
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i $DEPLOY_KEY root@${env.JMETER_INSTANCE_IP} 'su - jmeter_user -c "${runCommand}"'
                                    scp -o StrictHostKeyChecking=no -i $DEPLOY_KEY -r root@${env.JMETER_INSTANCE_IP}:/home/jmeter_user/pandp_test_reports ${workspace}/dx-performance-os-njdc-tests

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
                                                aws s3 cp s3://dx-testarea/pandp_regression-reports/pandp-regression-test-combined-runs_njdc.xml pandp-regression-test-combined-runs_njdc.xml
                                                scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/pandp-regression-test-combined-runs_njdc.xml root@${env.JMETER_INSTANCE_IP}:/home/jmeter_user/pnp-perfomance-regression-reports
                                        
                                                cd ${workspace}/dx-performance-os-njdc-tests && tar -czf ${env.REPORT_BUILD_NAME}_pnp-performance-test-njdc.zip pandp_test_reports/*
                                            
                                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} root@${env.JMETER_INSTANCE_IP} 'cd /home/jmeter_user/pnp-perfomance-regression-reports/ && chmod +x pageportlet-regression-test-dashboard.sh && sh pageportlet-regression-test-dashboard.sh ${env.REPORT_BUILD_NAME}_pnp-performance-test-njdc pnpPerformanceNjdc'
                                                scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} root@${env.JMETER_INSTANCE_IP}:/home/jmeter_user/pnp-perfomance-regression-reports/pandp-regression-test-combined-runs_njdc.xml .
                                                scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r  root@${env.JMETER_INSTANCE_IP}:/home/jmeter_user/pnp-perfomance-regression-reports/dashboard/* .
                                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} root@${env.JMETER_INSTANCE_IP} 'rm -rf /home/jmeter_user/pnp-perfomance-regression-reports'
                                                aws s3 cp ${env.REPORT_BUILD_NAME}_pnp-performance-test-njdc.zip s3://dx-testarea/
                                                aws s3 cp pandp-regression-test-combined-runs_njdc.xml s3://dx-testarea/pandp_regression-reports/
                                                cat pnpPerformanceNjdc-dashboard.html
                                                cat wtf.css
                                                aws s3 cp pnpPerformanceNjdc-dashboard.html s3://dx-testarea/pandp_regression-reports/ 
                                                aws s3 cp wtf.css s3://dx-testarea/pandp_regression-reports/    
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