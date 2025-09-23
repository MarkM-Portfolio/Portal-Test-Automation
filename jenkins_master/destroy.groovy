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

// Variable to store deployment settings
def deploymentProps

pipeline { 

    agent {
        label 'build_infra'
    }
    
    /*
     * This section automatically creates the configurable parameters in Jenkins
     */
    parameters {
        choice(name: 'environment', choices: ['pjt', 'pjd', 'pjs'], description: 'Select the environment that you want to destroy.')
    }

    /*
     * Preparing all settings we might need, using defaults if no override happens through jenkins params
     */
    stages {   
        /*
         * Read the property files for the corresponding environment
         */
        stage('Read stage properties') {
            steps {
                print "Going to deploy Jenkins using configuration for [${params.environment}]"
                script {
                    // Loading deployment properties
                    deploymentProps = readYaml file: "${env.WORKSPACE}/jenkins_master/configurations/${params.environment}.yaml"
                    
                    // General Job configuration that is independent of Jenkins
                    // Log level of terraform, will default to WARN
                    if (!env.TF_LOG) {
                        env.TF_LOG = 'WARN'
                    }
                    // Version of terraform to be used
                    if (!env.TERRAFORM_ZIP) {
                        env.TERRAFORM_ZIP = "terraform_0.12.20_linux_amd64.zip"
                    }
                    // Path to terraform binaries in artifactory
                    env.TERRAFORM_DOWNLOAD = "dx-build-prereqs/terraform/${env.TERRAFORM_ZIP}"
                    // Build user
                }
            }
        }

        /*
         *  Preparing terraform to run in the current workspace. Terraform abstracts the AWS access and handles resource lifecycles and deployments
         */
        stage('Prepare Terraform') {
            steps {
                sh """
                    if [ ! -e "${env.TERRAFORM_ZIP}" ]; then
                       curl -LJO "https://${G_ARTIFACTORY_HOST}/artifactory/${G_ARTIFACTORY_GENERIC_NAME}/${env.TERRAFORM_DOWNLOAD}"
                       unzip "${env.TERRAFORM_ZIP}"
                       chmod +x terraform
                    fi
                    ./terraform --help
                """
            }
        }

        /*
         *  We will create the environment via Terraform and use it to call the scripts to configure the Jenkins agent prereqs
         */
        stage('Destroy master and agents') {
            steps {
                script {
                    withCredentials([
                        usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')
                    ]) {
                        configFileProvider([
                            configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                        ]) {
                            dir("${workspace}/jenkins_master/terraform") {
                                sh(script: """
                                    cp $DEPLOY_KEY test-automation.pem
                                    chmod 0400 test-automation.pem
                                    ${workspace}/terraform init -backend-config="key=terraform-status/jenkinsmaster/${deploymentProps.general.subDomain}.key"
                                    ${workspace}/terraform destroy --auto-approve
                                """)
                            }
                            def agents = deploymentProps.jenkins.agentDefinition
                            dir("${workspace}/jenkins_slaves/terraform") {
                                sh(script: """
                                    cp $DEPLOY_KEY test-automation.pem
                                    chmod 0400 test-automation.pem
                                """)
                                for (agent in agents) {
                                    sh(script: """
                                        ${workspace}/terraform init -backend-config="key=terraform-status/jenkinsagent/${agent.name}.key"
                                        ${workspace}/terraform destroy --auto-approve
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
