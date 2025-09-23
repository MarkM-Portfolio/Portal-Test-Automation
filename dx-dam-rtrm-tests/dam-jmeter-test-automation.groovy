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

/*
* In failure scenarios of  DAM RTRM JMeter test the jtl files are uploaded to s3 bucket for future investigation
*/
def damRtrmJmeterResultsBackupForInvestigation(DEPLOY_KEY) {
    echo "Perform the dam rtrm jmeter reports jtl file backup"
    withCredentials([usernamePassword(credentialsId: 'aws_credentials', passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')]) {
        script {
            echo "jtl - filename: ${fileName}"
            sh """
            mkdir -p damJmeterResultsBackup && cd damJmeterResultsBackup
            chmod 600 ${DEPLOY_KEY}
            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && scp -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com:~/test_logs/${fileName} ./keys'
            scp -r -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no centos@${env.INSTANCE_IP}:/opt/Portal-Performance-Tests/keys ./ 
            echo "Upload the dam rtrm jtl file to s3 bucket - ${fileName}"
            aws s3 cp ./keys/${fileName} s3://dx-dam-automation-jmeter-logs/
            echo "Removing the backup file from jenkins damJmeterResultsBackup directory"
            rm ./keys/${fileName}
            echo "File uploaded to S3 bucket dx-dam-automation-jmeter-logs."
            """
        }
    }
}

def cleanupInstances() {
    withCredentials([
        usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')
            ]) {
                dir("${workspace}/dx-dam-rtrm-tests/terraform/ec2-launch") {
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

    stages {
        stage('Prepare Settings') {
            steps {
                /*
                 * Assigning hostname using Performace-test-automation + the timestap
                 */
                dir("${WORKSPACE}/dx-dam-rtrm-tests") {
                    script {
                        // determine build version and label current job accordingly
                        def dateFormat = new SimpleDateFormat("yyyyMMdd-HHmm")
                        def date = new Date()
                        
                        if (!env.VERSION){
                            env.VERSION = 'dx-dam-rtrm-tests'
                        }

                        if (!env.TARGET_JMX_FILE){
                            env.TARGET_JMX_FILE = 'DAM_RTRM_Upload_Assets.jmx'
                        }

                        echo "Assigning hostname + timestamp"
                        env.ENV_HOSTNAME = "JMeter_Tests_${dateFormat.format(date)}"

                        if (env.TARGET_JMX_FILE == 'DAM_RTRM_Upload_Assets.jmx'){
                            env.ENV_HOSTNAME = "JMeter_Tests_Uploading_Assets_${dateFormat.format(date)}"
                        }
                        if (env.TARGET_JMX_FILE == 'DAM_RTRM_Validation_Assets.jmx'){
                            env.ENV_HOSTNAME = "JMeter_Tests_Validating_Assets_${dateFormat.format(date)}"
                        }
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
                            dir("${workspace}/dx-dam-rtrm-tests/terraform/ec2-launch") {
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
                            	    sh ${workspace}/dx-dam-rtrm-tests/scripts/wait_for_instance.sh
                                """)
                            }
                        }
                    }
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
         * Once scripts are copied to EC2 instance, we need to run the JMeter scripts. 
         */ 
        stage('Run JMeter performance tests') {
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
                        fileName  = "test_log_dam_rtrm_${nowDate.format(date)}.jtl"
                        Exception caughtException = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo "Running tests"
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && chmod 400 ./keys/Performance-Test-Keypair.pem'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com ls -al ~/data/'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && scp -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem jmeter/DAM_RTRM_Tests/${TARGET_JMX_FILE} centos@performance-test-jmeter-master.team-q-dev.com:~/data/${TARGET_JMX_FILE}'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && scp -r -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem jmeter/DAM_RTRM_Tests/Dataset/* centos@performance-test-jmeter-master.team-q-dev.com:~/data/dam/rtrm'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \' env JVM_ARGS="-Dnashorn.args=--no-deprecation-warning" jmeter -JProtocol=${env.SERVER_PROTOCOL} -JHost=${env.SERVER_HOST} -JPort=${env.SERVER_PORT} -JRingAPIPort=${env.SERVER_RINGAPI_PORT} -JDAMPort=${env.SERVER_DAMAPI_PORT} -j jmeter.save.saveservice.output_format=xml -n -t /home/centos/data/${TARGET_JMX_FILE} -l /home/centos/test_logs/${fileName}\''
                                """
                                jmeter_error=sh(script:"ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/Portal-Performance-Tests && ssh -o StrictHostKeyChecking=no -i ./keys/Performance-Test-Keypair.pem centos@performance-test-jmeter-master.team-q-dev.com \'grep -i -e Exception -e failed /home/centos/test_logs/${fileName}| wc -l\''",returnStdout: true).trim() as Integer
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
    }

    /*
     * Perform proper cleanup to leave a healthy jenkins agent.
     */ 
    post {
        unstable {
           configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
            script {
                damRtrmJmeterResultsBackupForInvestigation("${DEPLOY_KEY}")
                cleanupInstances()
                    }
                }
        }
        failure {
           configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
            script {
                damRtrmJmeterResultsBackupForInvestigation("${DEPLOY_KEY}")
                cleanupInstances()
                    }
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
