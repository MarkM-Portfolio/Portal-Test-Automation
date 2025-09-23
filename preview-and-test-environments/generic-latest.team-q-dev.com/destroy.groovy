/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2001, 2020. All Rights Reserved. *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

pipeline { 
    agent {
        label 'build_infra'    
    }

    stages {
        /*
         * We have to get the current branch name in order to determine hostnames and artifact filters.
         * When checking code out from git, jenkins provides an environment variable named GIT_BRANCH.
         * This variable can be leverage and must be cleaned up, to ensure proper formatting.
         * If the parameter for a custom hostname is empty, we use a generated one.
         */
        stage('Prepare Settings') {
            steps {
                script {
                    echo "Determine branch, will be used for filtering the artifacts to pull."
                    env.ESCAPED_GIT_BRANCH_NAME = "${GIT_BRANCH}".replace("origin/", "").replace("/", "_")
                    echo "Full name is ${env.GIT_BRANCH} - escaped and reduced to ${ESCAPED_GIT_BRANCH_NAME}."
                    echo "Replacing hostname for created environment in all config files."
                    if (!env.ENV_HOSTNAME){
                        env.ENV_HOSTNAME = "${env.ESCAPED_GIT_BRANCH_NAME}-latest.team-q-dev.com"
                    }   
                    echo "New hostname will be: ${env.ENV_HOSTNAME}"
                    if (!env.PUBLISH_EXTERNAL){
                      env.PUBLISH_EXTERNAL = "false"
                    }
                    env.TF_VAR_BUILD_LABEL = "${env.ENV_HOSTNAME}"
                    env.TF_VAR_TEST_RUN_ID = "${env.ENV_HOSTNAME}"
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
         * Run terraform destroy to remove the existing instance including the route53 record
         */
        stage('Destroy internal EC2 instance') {
            when {
                allOf {
                    not {environment name: 'PUBLISH_EXTERNAL', value: ''}
                    environment name: 'PUBLISH_EXTERNAL', value: 'false'
                }
            }
            steps {
                script {
                    /*
                     * We need the AWS credentials for terraform and the deploy key to have proper SSH access to instances we created.
                     */ 
                    withCredentials([
                        usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'TF_VAR_aws_secret_key', usernameVariable: 'TF_VAR_aws_access_key')
                    ]) {
                        configFileProvider([
                          configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                        ]) {
                            dir("${workspace}/preview-and-test-environments/generic-latest.team-q-dev.com/terraform/ec2-launch") {
                                // replace placeholder in the variables.tf to fit the current instance
                                sh(script: """
                                    sed -i 's/ENVIRONMENT_HOSTNAME/${env.ENV_HOSTNAME}/g' variables.tf
                                    ${workspace}/terraform init
                                    ${workspace}/terraform destroy -auto-approve
                                """)
                            }
                        }
                    }
                }             
            }
        }
        stage('Destroy external EC2 instance') {
            when {
                allOf {
                    not {environment name: 'PUBLISH_EXTERNAL', value: ''}
                    environment name: 'PUBLISH_EXTERNAL', value: 'true'
                }
            }
            steps {
                script {
                    /*
                     * We need the AWS credentials for terraform and the deploy key to have proper SSH access to instances we created.
                     */ 
                    withCredentials([
                        usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'TF_VAR_aws_secret_key', usernameVariable: 'TF_VAR_aws_access_key')
                    ]) {
                        configFileProvider([
                          configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                        ]) {
                            dir("${workspace}/preview-and-test-environments/generic-latest.team-q-dev.com/terraform/external-ec2-launch") {
                                // replace placeholder in the variables.tf to fit the current instance
                                sh(script: """
                                    sed -i 's/ENVIRONMENT_HOSTNAME/${env.ENV_HOSTNAME}/g' variables.tf
                                    ${workspace}/terraform init
                                    ${workspace}/terraform destroy -auto-approve
                                """)
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
