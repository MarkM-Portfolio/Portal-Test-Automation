/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2001, 2021. All Rights Reserved. *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

import java.text.SimpleDateFormat

pipeline {
    agent {
        label 'build_infra'    
    }
    parameters {
        string(name: 'PORTAL_HOST', defaultValue: 'https://halo-halo-latest.team-q-dev.com/wps/portal', description: 'Required')

        string(name: 'APP_ENDPOINT', defaultValue: 'https://halo-halo-latest.team-q-dev.com:5500/dx/ui/site-manager', description: 'Required')

        string(name: 'WCMREST', defaultValue: 'https://halo-halo-latest.team-q-dev.com/wps', description: 'Required')

        booleanParam(name: 'SSL_ENABLED', defaultValue: false, description: 'Required for testing environments with https/self-signed certificates like native.kube.') 

        booleanParam(name: 'BROWSER_HEADLESS', defaultValue: true, description: 'Required false for drag and drop testing') 

        string(name: 'TARGET_BRANCH', defaultValue: 'develop', description: 'Target branch of the applications. For release branches please include complete branch name. for example: release/95_CF192')

        string(name: 'USERNAME', defaultValue: 'wpsadmin')

        string(name: 'PASSWORD', defaultValue: 'wpsadmin')
    }
    stages {
        stage('Prepare Settings') {
            steps {
                /*
                 * Assigning hostname using Acceptance-test-automation + the timestap
                 */
                dir("${WORKSPACE}/dx-acceptance-tests") {
                    script {
                        // determine build version and label current job accordingly
                        def dateFormat = new SimpleDateFormat("yyyyMMdd-HHmm")
                        def date = new Date()
                        echo "Assigning hostname + timestamp"
                        env.ENV_HOSTNAME = "DX_Acceptance_Tests_${dateFormat.format(date)}"
                        echo "New hostname will be: ${env.ENV_HOSTNAME}"

                        // Display name includes the ENV_HOSTNAME and a timestamp
                        currentBuild.displayName = "${env.ENV_HOSTNAME}"

                        if (!env.VERSION){
                            env.VERSION = 'dx-acceptance-tests'
                        }

                        // Defines the time to live in hours for all resources created (AMI, EC2 instances and DNS entries)
                        if (!env.RESOURCES_TTL){
                            env.RESOURCES_TTL = '24'
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
                            dir("${workspace}/dx-acceptance-tests/terraform/ec2-launch") {
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
                            	    sh ${workspace}/dx-acceptance-tests/scripts/wait_for_instance.sh
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
                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/dx-acceptance-tests/scripts/install-prereqs.sh centos@${env.INSTANCE_IP}:/tmp
                            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'chmod +x /tmp/install-prereqs.sh && sh /tmp/install-prereqs.sh'
                          """
                      } 
                }
            }
        }

        /*
         * Once prerequisites are installed, we pull the repositories needed for the tests in the workspace, and scp transfer it to the EC2 instance. 
         */ 
        stage('Pull tests') {
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    sshagent(credentials: ['jenkins-git']) {
                        script {
                            script {
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    git clone -b ${env.TARGET_BRANCH} git@git.cwp.pnp-hcl.com:websphere-portal-incubator/site-manager.git ${workspace}/site-manager
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'sudo mkdir -p /opt/site-manager && sudo chown centos: /opt/site-manager'
                                    scp -r -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/site-manager centos@${env.INSTANCE_IP}:/opt
                                """
                            }
                        }
                    }
                }
            } 
        }


        /*
         * Each application would then install its dependencies and run the acceptance tests. Timeout is currently set at 30 mins per application stage.
         * Timeout is also treated as a failure, and is caught using org.jenkinsci.pligins.workflow.steps.FlowInterruptedException. Otherwise timeouts
         * are going to be registered as ABORTED in jenkins status report. The other catch is for any other errors produced by the test. 
         */ 
        stage('Run Site Manager acceptance tests') {
            options {
                timeout(time: 60, unit: 'MINUTES') 
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        Exception caughtException = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo "Running tests"
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/site-manager && make install'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/site-manager && make build-dx-sm-components'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'cd /opt/site-manager/packages/ui && export DISPLAY=:99 && make test-acceptance-endpoint dx_core=${env.PORTAL_HOST} app=${env.APP_ENDPOINT} wcmrest=${env.WCMREST} insecure=${env.SSL_ENABLED} headless=${env.BROWSER_HEADLESS}'
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

    /*
     * Perform proper cleanup to leave a healthy jenkins agent. On build success we clean up the EC2 instance. On fails/unstable EC2 is left up and
     * to be terminated manually.
     */ 
    post {
        success {
            script {
                withCredentials([
                    usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')
                ]) {
                    dir("${workspace}/dx-acceptance-tests/terraform/ec2-launch") {
                        sh(script: """
                            sed -i 's/ENVIRONMENT_HOSTNAME/${env.ENV_HOSTNAME}/g' variables.tf
                            ${workspace}/terraform init
                            ${workspace}/terraform destroy -auto-approve
                        """)
                    }
                }
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