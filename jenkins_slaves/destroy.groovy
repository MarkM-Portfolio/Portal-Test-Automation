/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2019, 2023. All Rights Reserved. *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

// Import the shared library with the name configured in Jenkins
@Library("dx-shared-library") _

TERRAFORM_DOWNLOAD="dx-build-prereqs/terraform/terraform_0.12.20_linux_amd64.zip"

pipeline { 
    agent {
        label 'build_infra'
    }

    /*
     * Preparing all settings we might need, using defaults if no override happens through jenkins params
     */
    stages {
        stage('Prepare Settings') {
            steps {
                script {
                    // Terraform log-level
                    if (!env.TF_LOG) {
                        env.TF_LOG = 'INFO'
                    }
                    // Default parameters
                    if (!env.JENKINS_URL) {
                        env.JENKINS_URL="https://portal-jenkins-test.cwp.pnp-hcl.com"
                    }

                    // Build user
                    INSTANCE_OWNER = dxJenkinsGetJobOwner()
                }
            }
        }
        
        /*
         *  Preparing terraform to run in the current workspace. Terraform abstracts the AWS access and handles resource lifecycles and deployments
         */
        stage('Prepare Terraform') {
            steps {
                sh """
                    curl -LJO "https://${G_ARTIFACTORY_HOST}/artifactory/${G_ARTIFACTORY_GENERIC_NAME}/${TERRAFORM_DOWNLOAD}"
                    unzip -o terraform_0.12.20_linux_amd64.zip
                    chmod +x terraform
                    ./terraform --help
                """
            }
        }

        /*
         *  Deregister the agent before destruction using the Jenkins REST API
         */
        stage('Deregister agent with Jenkins') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: "${JENKINS_API_CREDENTIALS_ID}",
                        passwordVariable: 'JENKINS_PASSWORD',
                        usernameVariable: 'JENKINS_USER'),
                ]) {
                    sh """
                        curl -k -u "${JENKINS_USER}:${JENKINS_PASSWORD}" -X POST "${JENKINS_URL}/computer/${INSTANCE_NAME}/doDelete"
                    """
                }
            }
        }

        /*
         *  We will destroy the environment via Terraform
         */
        stage('Destroy instance') {
            steps {
                script {
                    withCredentials([
                        usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID'),
                    ]) {
                        dir("${workspace}/jenkins_slaves/terraform") {
                            configFileProvider([
                                configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                            ]) {
                                // prepare terraform and execute terraform, use private key to access machine
                                sh(script: """
                                    cp $DEPLOY_KEY test-automation.pem
                                    ls -lah
                                    printenv
                                    ${workspace}/terraform init -backend-config="key=terraform-status/jenkinsagent/${INSTANCE_NAME}.key"
                                    ${workspace}/terraform destroy -auto-approve -var instance_name="${INSTANCE_NAME}"
                                """)                                
                            }
                        }
                    }
                }
            }
        }
    }
    
    post {
        cleanup {
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
