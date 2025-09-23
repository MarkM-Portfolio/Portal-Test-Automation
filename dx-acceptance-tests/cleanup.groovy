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
 
pipeline {
    agent {
        label 'build_infra'    
    }

    stages {
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
        stage('Remove EC2 Instancce') {
            steps {
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