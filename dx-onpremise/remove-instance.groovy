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
    parameters {
        string(name: 'INSTANCE_NAME', defaultValue: 'dx-onpremise', description: 'Name of the instance to be removed',  trim: false)
        string(name: 'INSTANCE_TYPE', defaultValue: 't2.large', description: 'Name of the instance type to be removed',  trim: false)
        booleanParam(name: 'CLUSTERED_ENV', defaultValue: 'false', description: 'Check this if primary node is clustered')
        booleanParam(name: 'REMOTE_SEARCH_ENV', defaultValue: 'false', description: 'Check this if remote search is installed and configured')
        string(name: 'DB2_REMOVE_JOB', defaultValue: 'hybrid/onpremise/db2-destroy', description: 'DB host removal job path',  trim: false)
        string(name: 'DB2_REGION', defaultValue: 'us-east-1', description: 'DB hosted region (Use "us-east-2" only when the DB is hosted on private subnet)',  trim: false)
        string(name: 'DB2_INSTANCE_NAME', defaultValue: '', description: 'Name of the DB instance to be removed (It is required only when the DB is hosted on private subnet)',  trim: false)
    } 
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
                    if (!env.INSTANCE_NAME){
                        env.INSTANCE_NAME = "dx-onpremise"
                    }
                    // Instance Type to be used 
                    if (!env.INSTANCE_TYPE){
                        env.INSTANCE_TYPE = "t2.large"
                    }
                    if (params.DB2_INSTANCE_NAME == ""){
                        env.DB2_INSTANCE_NAME = "${env.INSTANCE_NAME}-db2"
                    }
                    env.SECONDARY_INSTANCE_NAME = "${env.INSTANCE_NAME}-secondary"
                    env.REMOTE_SEARCH_INSTANCE_NAME = "${env.INSTANCE_NAME}-remote-search"
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
         *  We manage the lifecycle for this environment via Terraform.
         */
        stage('Delete existing EC2 Instance') {
            steps {
                script {
                    withCredentials([
                        usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID'),
                    ]) {
                        dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-remove") {
                            configFileProvider([
                                configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                            ]) {
                                // prepare terraform and execute terraform, use private key to access machine
                                sh(script: """
                                    cp $DEPLOY_KEY test-automation.pem
                                    ls -lah
                                    printenv
                                    ${workspace}/terraform init -backend-config="key=terraform-status/dx-onpremise/${INSTANCE_NAME}.key"
                                    ${workspace}/terraform destroy -auto-approve -var instance_name="${INSTANCE_NAME}" -var AWS_EC2_INSTANCE_TYPE="${INSTANCE_TYPE}"
                                """)                                
                            }
                        }
                    }
                }
            }
        }

                /*
         *  We manage the lifecycle for this environment via Terraform.
         */
        stage('Delete existing secondary EC2 Instance') {
            when { expression { params.CLUSTERED_ENV } }
            steps {
                script {
                    withCredentials([
                        usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID'),
                    ]) {
                        dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-remove") {
                            configFileProvider([
                                configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                            ]) {
                                // prepare terraform and execute terraform, use private key to access machine
                                sh(script: """
                                    cp $DEPLOY_KEY test-automation.pem
                                    ls -lah
                                    printenv
                                    ${workspace}/terraform init -backend-config="key=terraform-status/dx-onpremise/${SECONDARY_INSTANCE_NAME}.key"
                                    ${workspace}/terraform destroy -auto-approve -var instance_name="${SECONDARY_INSTANCE_NAME}" -var AWS_EC2_INSTANCE_TYPE="${INSTANCE_TYPE}"
                                """)                                
                            }
                        }
                    }
                }
            }
        }

        /*
         *  Delete remote search environment
         */
        stage('Delete existing remote search Instance') {
            when { expression { params.REMOTE_SEARCH_ENV } }
            steps {
                script {
                    withCredentials([
                        usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID'),
                    ]) {
                        dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-remove") {
                            configFileProvider([
                                configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                            ]) {
                                // prepare terraform and execute terraform, use private key to access machine
                                sh(script: """
                                    cp $DEPLOY_KEY test-automation.pem
                                    ls -lah
                                    printenv
                                    ${workspace}/terraform init -backend-config="key=terraform-status/dx-onpremise/${REMOTE_SEARCH_INSTANCE_NAME}.key"
                                    ${workspace}/terraform destroy -auto-approve -var instance_name="${REMOTE_SEARCH_INSTANCE_NAME}" -var AWS_EC2_INSTANCE_TYPE="${INSTANCE_TYPE}"
                                """)
                            }
                        }
                    }
                }
            }
        }
        
        stage('Delete existing public DB2 instance') {
            when { expression { params.DB2_INSTANCE_NAME == "" } }
            steps {
                dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-remove") {
                    build job: "${DB2_REMOVE_JOB}", parameters: [string(name: 'INSTANCE_NAME', value: "${DB2_INSTANCE_NAME}"), string(name: 'AWS_REGION', value: "${DB2_REGION}")]
                }
            }
        }

        stage('Delete existing private DB2 instance') {
            when { expression { params.DB2_INSTANCE_NAME != "" } }
            steps {
                script {
                    withCredentials([
                        usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID'),
                    ]) {
                        dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-remove") {
                            sh """
                                sh ./delete-db2.sh $DB2_INSTANCE_NAME $DB2_REGION
                            """
                        }
                    }    
                }
            }
        }  
    }
    
    post {
        cleanup {
            /* Cleanup workspace */
            /* Cleanup workspace@tmp */
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
