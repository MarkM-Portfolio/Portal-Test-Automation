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

    stages {
        stage('Prepare Settings') {
            steps {
                /*
                 * Assigning hostname using Performace-test-automation + the timestap
                 */
                dir("${workspace}/dx-performance-tests/dx-wcm-rendering-regression") {
                    script {
                        // determine build version and label current job accordingly
                        def dateFormat = new SimpleDateFormat("yyyyMMdd-HHmm")
                        def date = new Date()
                        
                        if (!env.VERSION){
                            env.VERSION = 'dx-wcm-rendering-regression'
                        }

                        if (!env.TARGET_JMX_FILE){
                            env.TARGET_JMX_FILE = 'Portal_WCM_Performance_Test_Scenario.jmx'
                        }

                        // Defines the CF Version to be deployed
                        if (!env.CF_VERSION){
                            env.CF_VERSION = 'CF207'
                        }

                        echo "Assigning hostname + timestamp"
                        env.ENV_HOSTNAME = "JMeter_Tests_WCM_${dateFormat.format(date)}"

                        // Display name includes the ENV_HOSTNAME and a timestamp
                        currentBuild.displayName = "${env.ENV_HOSTNAME}"

                        // Defines the time to live in hours for all resources created (AMI, EC2 instances and DNS entries)
                        if (!env.RESOURCES_TTL){
                            env.RESOURCES_TTL = '10'
                        }

                        if (!env.INSTANCE_IP){
                            env.INSTANCE_IP = '10.190.75.36'
                        }

                        if (!env.GTEST_DURATION){
                            env.GTEST_DURATION = '3600'
                        }

                        currentBuild.description = "${DXBuildNumber_NAME}"
                        env.TF_VAR_BUILD_LABEL = "${DXBuildNumber_NAME}"

                        // Calculate expiration timestamp
                        def ttl_stamp = (System.currentTimeMillis() + (env.RESOURCES_TTL.toString().toInteger() * 60 * 60 * 1000))
                        env.TF_VAR_EXPIRATION_STAMP = ttl_stamp

                        echo "INSTANCE_IP: ${env.INSTANCE_IP}"
                        echo "TF_VAR_BUILD_LABEL: ${env.DXBuildNumber_NAME}"
                        echo "GTEST_DURATION: ${env.GTEST_DURATION}"
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
                            ssh -o StrictHostKeyChecking=no -i $DEPLOY_KEY root@${env.JMETER_INSTANCE_IP} 'cd /home/jmeter_user && mkdir wcm-perfomance-regression-reports'
                            scp -o StrictHostKeyChecking=no -i $DEPLOY_KEY -r ${workspace}/test-scripts/TestReport/TestReport-njdc/* root@${env.JMETER_INSTANCE_IP}:/home/jmeter_user/wcm-perfomance-regression-reports
                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r ${workspace}/dx-core-tests/dx-test-rest/helpers/Saxon-HE-9.5.1-3.jar root@${env.JMETER_INSTANCE_IP}:/home/jmeter_user/wcm-perfomance-regression-reports/Saxon-HE-9.5.1-3.jar
                            scp -o StrictHostKeyChecking=no -i $DEPLOY_KEY -r ${workspace}/test-scripts/TestReport/wtf.css root@${env.JMETER_INSTANCE_IP}:/home/jmeter_user/wcm-perfomance-regression-reports/wtf.css
                          """
                      }
            }
        }
        

         /*
         *  Perform the actual CF update on the NJDC machine
         */
        stage('Run applyCF to a given DXBuildNumber') {
            steps {
                withCredentials([
                    sshUserPrivateKey(credentialsId: "dx-core-tests-base-image-key", keyFileVariable: 'connectKey'),
                    usernamePassword(credentialsId: "old_ftp_credentials", passwordVariable: 'FTP_PASSWORD', usernameVariable: 'FTP_USER')
                ]) {
                    dir("${workspace}/dx-core-tests/scripts") {
                        sh """
                            scp -i ${connectKey} -o StrictHostKeyChecking=no -r portal-update-cf-njdc.sh root@${env.INSTANCE_IP}:/portal-update-cf-njdc.sh
                            sh run-portal-cf-update-njdc.sh root ${env.INSTANCE_IP} ${G_AWS_SHARE_FTP_HOST} ${FTP_USER} ${FTP_PASSWORD} ${CF_VERSION} ${TF_VAR_BUILD_LABEL} ${connectKey}
                        """
                    }
                }
            }
         }
        
        /*
         * Run the JMeter scripts. 
         */ 
        stage('Run JMeter performance tests') {
            options {
                timeout(time: 150, unit: 'MINUTES') 
            }
            steps {
                withCredentials([
                    sshUserPrivateKey(credentialsId: "dx-core-tests-base-image-key", keyFileVariable: 'connectKey')
                ]) {
                    script {
                        fileName  = "test_log_${CF_VERSION}_${TF_VAR_BUILD_LABEL}.jtl"
                        runCommand = "/opt/apache-jmeter/bin/jmeter -n -t /home/jmeter_user/jmeter_tests/Portal_WCM_Performance_Test_Scenario.jmx -R ${env.JMETER_AGENT1},${env.JMETER_AGENT2} -GDX_HOST=${env.INSTANCE_IP} -GDX_PROTOCOL=${env.SERVER_PROTOCOL} -GDX_PORT=80 -GTEST_DURATION=${env.GTEST_DURATION} -GVUSERS_PER_TYPE=${env.GVUSERS_PER_TYPE} -l /home/jmeter_user/jmeter_logs/${fileName} -e -f -o wcm_test_report_njdc"
                        validateCommand = "grep -i -e Exception -e failed /home/jmeter_user/jmeter_logs/${fileName}| wc -l"
                        Exception caughtException = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo "Running tests"
                                sh """
                                    ssh -o StrictHostKeyChecking=no -i ${connectKey} root@${env.JMETER_INSTANCE_IP} 'su - jmeter_user -c "${runCommand}"'
                                    scp -o StrictHostKeyChecking=no -i ${connectKey} -r root@${env.JMETER_INSTANCE_IP}:/home/jmeter_user/wcm_test_report_njdc ${workspace}/dx-performance-os-njdc-tests
                                """
                                jmeter_error=sh(script:"""ssh -o StrictHostKeyChecking=no -i ${connectKey} root@${env.JMETER_INSTANCE_IP} 'su - jmeter_user -c "${validateCommand}"'""",returnStdout: true).trim() as Integer
                                if ("${jmeter_error}">0) {
                                    echo "Errors in JMeter script results"
                                    sh "exit 1"
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
    // }

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
                                        echo "Generating performance test dashboard report for WCM apis"
                                        // Copy previous runs file from s3 bucket
                                        // Copy current run and previous run file to EC2 instance
                                        // Execute script to generate report
                                        // Copy reports(html and css) to s3 bucket 
                                        sh """
                                            chmod 600 ${DEPLOY_KEY}
                                            aws s3 cp s3://dx-testarea/wcm-regression-reports/onprem_wcm_performance_tests_results.xml onprem_wcm_performance_tests_results.xml
                                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/onprem_wcm_performance_tests_results.xml root@${env.JMETER_INSTANCE_IP}:/home/jmeter_user/wcm-perfomance-regression-reports/

                                            cd ${workspace}/dx-performance-os-njdc-tests && tar -czf ${env.DXBuildNumber_NAME}_wcm_performance_test.zip wcm_test_report_njdc/*

                                            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} root@${env.JMETER_INSTANCE_IP} 'cd /home/jmeter_user/wcm-perfomance-regression-reports/ && chmod +x wcm-regression-test-dashboard.sh && sh wcm-regression-test-dashboard.sh ${env.DXBuildNumber_NAME}_wcm_performance_test WCM-Performance'
                                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r  root@${env.JMETER_INSTANCE_IP}:/home/jmeter_user/wcm-perfomance-regression-reports/onprem_wcm_performance_tests_results.xml .
                                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r  root@${env.JMETER_INSTANCE_IP}:/home/jmeter_user/wcm-perfomance-regression-reports/dashboard/* .
                                            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} root@${env.JMETER_INSTANCE_IP} 'rm -rf /home/jmeter_user/wcm-perfomance-regression-reports'

                                            aws s3 cp ${env.DXBuildNumber_NAME}_wcm_performance_test.zip s3://dx-testarea/
                                            aws s3 cp onprem_wcm_performance_tests_results.xml s3://dx-testarea/wcm-regression-reports/
                                            cat WCM-Performance-dashboard.html
                                            cat wtf.css
                                            aws s3 cp WCM-Performance-dashboard.html s3://dx-testarea/wcm-regression-reports/ 
                                            aws s3 cp wtf.css s3://dx-testarea/wcm-regression-reports/
                                            
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

    /*
     * Perform proper cleanup to leave a healthy jenkins agent.
     */ 
    post {
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
                
            }
        }
    }
 }
