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

TERRAFORM_DOWNLOAD_PATH="dx-build-prereqs/terraform"
TERRAFORM_DOWNLOAD_FILE="terraform_1.1.9_linux_amd64.zip"

pipeline { 
    parameters {
        string(name: 'INSTANCE_NAME', defaultValue: '', description: 'Name of the instance to be removed',  trim: false)
        string(name: 'AWS_REGION', defaultValue: 'us-east-1', description: 'DB hosted region',  trim: false)
        booleanParam(name: 'IS_PRIVATE', defaultValue: false, description: 'Click this to destroy private db2 instance')
    } 

    agent {
        label 'build_infra'
    }

    stages {
        /*
        * Preparing all settings we might need, using defaults if no override happens through jenkins params
        */
        stage('Prepare Settings') {
            steps {
                script {
                    // Validate parameters
                    if (!params.INSTANCE_NAME){
                        error("Instance name should not be empty")
                    }
                    // Terraform log-level
                    if (!env.TF_LOG) {
                        env.TF_LOG = 'INFO'
                    }

                    // Log values
                    echo "INSTANCE_NAME: ${INSTANCE_NAME}"
                }
            }
        }
        
        /*
         *  Preparing terraform to run in the current workspace. Terraform abstracts the AWS access and handles resource lifecycles and deployments
         */
        stage('Prepare Terraform') {
            steps {
                sh """
                    curl -LJO "https://${G_ARTIFACTORY_HOST}/artifactory/${G_ARTIFACTORY_GENERIC_NAME}/${TERRAFORM_DOWNLOAD_PATH}/${TERRAFORM_DOWNLOAD_FILE}"
                    unzip -o ${TERRAFORM_DOWNLOAD_FILE}
                    chmod +x terraform
                    ./terraform --help
                """
            }
        }

        /*
         *  Destroy the environment via Terraform
         */
        stage('Destroy instance') {
            steps {
                script {
                    withCredentials([
                        usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID'),
                    ]) {
                        dir("${workspace}/db2/terraform") {
                            configFileProvider([
                                configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                            ]) {
                                // Prepare terraform and execute terraform
                                sh(script: """
                                    cp $DEPLOY_KEY test-automation-deployments.pem
                                    chmod 0400 test-automation-deployments.pem
                                """)      
                                if (env.IS_PRIVATE && env.IS_PRIVATE.toBoolean()) {
                                    sh(script: """
                                        sh ${workspace}/db2/scripts/destroy-db2.sh ${INSTANCE_NAME} ${AWS_REGION}
                                    """) 
                                } else {
                                    sh(script: """
                                        ${workspace}/terraform init -backend-config="key=terraform-status/jenkinsagent/${INSTANCE_NAME}.key"
                                        ${workspace}/terraform destroy -refresh=false -auto-approve -var instance_name="${INSTANCE_NAME}"
                                    """) 
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
