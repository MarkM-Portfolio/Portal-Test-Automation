/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2021-2022. All Rights Reserved.  *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */


// Import the shared library with the name configured in Jenkins
@Library("dx-shared-library") _

import java.text.SimpleDateFormat

// Create object to store parameters with values
def pipelineParameters = [:]

// EC2 instance variables
def terraformVarsEC2 = [:]
// Use a small EC2 instance, save some money (using 2C/4GB).
terraformVarsEC2.tfvar_aws_ec2_instance_type = 't3a.medium'

pipeline {
    agent {
        label 'build_infra'
    }

    stages {
        stage('Prepare Settings') {
            steps {
                /*
                 * Assigning hostname using Acceptance-test-automation + the timestap
                 */
                dir("${WORKSPACE}/dx-acceptance-tests") {
                    script {
                        // Load parameters via yaml file
                        dxParametersLoadFromFile(pipelineParameters, 'data-verify.yaml')

                        // determine build version and label current job accordingly
                        def dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss")
                        def date = new Date()
                        echo "Assigning hostname + timestamp"
                        pipelineParameters.hostname = "DX_Data_Verify_${dateFormat.format(date)}"
                        echo "New hostname will be: ${pipelineParameters.hostname}"

                        // Display name includes the initiating job and a timestamp
                        currentBuild.displayName = "${pipelineParameters.hostname}"
                        currentBuild.description = "${pipelineParameters.PORTAL_HOST} ${dateFormat.format(date)}"

                        // Calculate expiration timestamp
                        pipelineParameters.ttl = (System.currentTimeMillis() + (pipelineParameters.RESOURCES_TTL.toString().toInteger() * 60 * 60 * 1000))

                        pipelineParameters.buildUser = dxJenkinsGetJobOwner()
                        println("Instance owner will be > ${pipelineParameters.buildUser} <.")
                    }
                }
            }
        }

        /*
         * Prepare terraform with custom config
         */
        stage('Prepare Terraform') {
            steps {
                dxTerraformInstall()
                dxTerraformCustomConfig(source: 'dx-acceptance-tests/terraform/ec2-launch')
            }
        }

        /*
         * Create EC2 instance
         */
        stage('Create EC2 Instance') {
            // Setting the TF variables to overwrite specific EC2 instance settings
            steps {
                script {
                    terraformVarsEC2.tfvar_EXPIRATION_STAMP = "${pipelineParameters.ttl}"
                    terraformVarsEC2.tfvar_instance_owner = "${pipelineParameters.buildUser}"
                    terraformVarsEC2.tfvar_instance_name = "${pipelineParameters.hostname}"
                    terraformVarsEC2 = dxTerraformCreateEc2Instance(terraformVarsEC2)
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
                    configFile(
                        fileId: 'test-automation-deployments',
                        variable: 'DEPLOY_KEY'
                )
                ]) {
                    sh """
                        chmod 600 ${DEPLOY_KEY}
                        scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/dx-acceptance-tests/scripts/install-prereqs.sh centos@${terraformVarsEC2.instance_private_ip}:/tmp
                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'chmod +x /tmp/install-prereqs.sh && sh /tmp/install-prereqs.sh'
                    """
                }
            }
        }

        /*
         * Once prerequisites are installed, we pull the repositories needed for the tests in the workspace, and scp transfer it to the EC2 instance.
         */
        stage('Pull tests') {
            when {
                expression { 
                    pipelineParameters.VERIFY_CC == true 
                }
            }
            steps {
                configFileProvider([
                    configFile(
                        fileId: 'test-automation-deployments',
                        variable: 'DEPLOY_KEY'
                    )
                ]) {
                    sshagent(
                        credentials: ['jenkins-git']
                    ) {
                        sh """
                            chmod 600 ${DEPLOY_KEY}
                            git clone -b ${pipelineParameters.TARGET_BRANCH} git@git.cwp.pnp-hcl.com:websphere-portal-incubator/content-ui.git ${workspace}/content-ui
                            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sudo mkdir -p /opt/content-ui && sudo chown centos: /opt/content-ui'
                            scp -r -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/content-ui centos@${terraformVarsEC2.instance_private_ip}:/opt
                        """
                    }
                }
            }
        }

        stage('Run CC Data Verify') {
            options {
                timeout(time: 60, unit: 'MINUTES')
            }
            when {
                expression { 
                    pipelineParameters.VERIFY_CC == true 
                }
            }
            steps {
                configFileProvider([
                    configFile(
                        fileId: 'test-automation-deployments',
                        variable: 'DEPLOY_KEY'
                    )
                ]) {
                    script {
                        Exception caughtException = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo "Running tests"
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/content-ui && make install'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/content-ui/packages/ui && make data-verify-endpoint dx_core=${pipelineParameters.PORTAL_HOST} ring_api=${pipelineParameters.EXP_API} insecure=${pipelineParameters.SSL_ENABLED}'
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
        always {
            script {
                try {
                    terraformVarsEC2.tfvar_EXPIRATION_STAMP = "${pipelineParameters.ttl}"
                    terraformVarsEC2.tfvar_instance_owner = "${pipelineParameters.buildUser}"
                    terraformVarsEC2.tfvar_instance_name = "${pipelineParameters.hostname}"
                    dxTerraformDestroyEc2Instance(terraformVarsEC2)
                } catch (Throwable e) {
                    println('Unable to destroy EC2 instance!')
                }
            }
        }
        cleanup {
            script {
                /* remove internal instance from known-hosts */
                if (terraformVarsEC2.instance_private_ip) {
                    sh(script: """
                        ssh-keygen -R ${terraformVarsEC2.instance_private_ip} | true
                        rm -f ~/.ssh/known_hosts.old
                    """)
                }
                dxWorkspaceDirectoriesCleanup()
            }
        }
    }
}