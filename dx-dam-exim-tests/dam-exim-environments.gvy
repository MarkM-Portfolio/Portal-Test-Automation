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

// Import the shared library with the name configured in Jenkins
@Library("dx-shared-library") _

import java.text.SimpleDateFormat


def cleanupInstances() {
     withCredentials([
                    usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')
                ]) {
                    dir("${workspace}/dx-dam-exim-tests/terraform/ec2-launch") {
                        sh(script: """
                            sed -i 's/ENVIRONMENT_HOSTNAME/${env.ENV_HOSTNAME}/g' variables.tf
                            ${workspace}/terraform init
                            ${workspace}/terraform destroy -auto-approve
                        """)
                    }
                }
}

pipeline {

    agent {
        label 'build_infra'
    }

    parameters {
      string(name: 'NAMESPACE', defaultValue: 'exim',description: 'name space')
      string(name: 'DEPLOYMENT_LEVEL', defaultValue: 'develop',description: 'Deploying develop images')
      string(name: 'KUBE_FLAVOUR', defaultValue: 'native', description: 'Deploying a native kube environment.')
      string(name: 'KUBE_UNDEPLOY_JOB', defaultValue: 'CI/kube-deploy/native-kube-remove', description: 'Job which undeploys the environment',  trim: false)
      string(name: 'KUBE_DEPLOY_JOB', defaultValue: 'CI/kube-deploy/native-kube-next-deploy', description: 'Job which deploys the environment',  trim: false)
      string(name: 'DEPLOYMENT_METHOD', defaultValue: 'helm', description: 'Deployment method')
      string(name: 'CONTEXT_ROOT_PATH', defaultValue: 'wps', description: 'Context root')
      string(name: 'DX_CORE_HOME_PATH', defaultValue: 'portal', description: 'Home path')
      string(name: 'PERSONALIZED_DX_CORE_PATH', defaultValue: 'myportal', description: 'Personalized path')
      string(name: 'DOMAIN_SUFFIX', defaultValue: '.team-q-dev.com', description: 'Kube flavour domain suffix')
      string(name: 'CLUSTER_NAME', defaultValue: '', description: 'Cluster name where the deployment should be deployed to')
      string(name: 'CLUSTER_REGION', defaultValue: '', description: 'Region of the cluster')
    }

    stages {
        stage("Load modules and configuration") {
            steps {
                script {
                    commonConfig = load "./autonomous-deployments/config/common.gvy"
                    commonModule = load "./autonomous-deployments/modules/common.gvy"
                    env.NAMESPACE_EXIM_SOURCE = "${env.NAMESPACE}-source"
                    env.NAMESPACE_EXIM_TARGET = "${env.NAMESPACE}-target"
                    /*
                    * Determine the default user for tagging of our environments
                    * This user will be handled as the owner, so environments are relatable
                    */
                    INSTANCE_OWNER = dxJenkinsGetJobOwner()
                    println("INSTANCE_OWNER: ${INSTANCE_OWNER}.")
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
         * Run terraform to create an EC2 instance based on the terraform scripting.
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
                            dir("${workspace}/dx-dam-exim-tests/terraform/ec2-launch") {
                                // replace placeholder in the variables.tf to fit the current environment
                                sh(script: """
                                    sed -i 's/ENVIRONMENT_HOSTNAME/${env.ENV_HOSTNAME}/g' variables.tf
                                    sed -i 's/TAG_NAME/${env.ENV_HOSTNAME}/g' main.tf
                                    ${workspace}/terraform init 
                                    ${workspace}/terraform apply -auto-approve -var instance_owner="${INSTANCE_OWNER}"
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
                            	    sh ${workspace}/dx-dam-exim-tests/scripts/wait_for_instance.sh
                                """)
                            }
                        }
                    }
                }             
            }
        }

        stage('Creating the dam source environment in k8') {
            steps {
                script {
                    buildParams = commonModule.createKubeParams(env.NAMESPACE_EXIM_SOURCE, params.KUBE_FLAVOUR, params.CLUSTER_NAME, params.CLUSTER_REGION, "", params.CONTEXT_ROOT_PATH, params.DX_CORE_HOME_PATH, params.PERSONALIZED_DX_CORE_PATH, params.DEPLOYMENT_LEVEL, params.DEPLOYMENT_METHOD, params.DOMAIN_SUFFIX)
                     build(job: "${params.KUBE_DEPLOY_JOB}", 
                          parameters: buildParams, 
                          propagate: true,
                          wait: true)
                }
            }
        }

        stage('Creating the dam target environment in k8') {
            steps {
                script {
                    buildParams = commonModule.createKubeParams(env.NAMESPACE_EXIM_TARGET, params.KUBE_FLAVOUR, params.CLUSTER_NAME, params.CLUSTER_REGION, "", params.CONTEXT_ROOT_PATH, params.DX_CORE_HOME_PATH, params.PERSONALIZED_DX_CORE_PATH, params.DEPLOYMENT_LEVEL, params.DEPLOYMENT_METHOD, params.DOMAIN_SUFFIX)
                     build(job: "${params.KUBE_DEPLOY_JOB}", 
                          parameters: buildParams, 
                          propagate: true,
                          wait: true)
                }
            }
        }

        stage('Destroying the dam source environment in k8') {
            steps {
                script {
                    buildParams = []
                    buildParams.add(
                        [$class: 'StringParameterValue',
                            name: 'INSTANCE_NAME',
                            value: "${env.NAMESPACE_EXIM_SOURCE}"])
                    build(job: "${params.KUBE_UNDEPLOY_JOB}", 
                          parameters: buildParams, 
                          propagate: true,
                          wait: true)
                }
            }
        }

         stage('Destroying the dam target environment in k8') {
            steps {
                script {
                    buildParams = []
                     buildParams.add(
                        [$class: 'StringParameterValue',
                            name: 'INSTANCE_NAME',
                            value: "${env.NAMESPACE_EXIM_TARGET}"])
                    build(job: "${params.KUBE_UNDEPLOY_JOB}", 
                          parameters: buildParams, 
                          propagate: true,
                          wait: true)
                }
            }
        }
    }

     /*
     * Perform proper cleanup to leave a healthy jenkins agent. On build unstable/failure/aborted/success we clean up the EC2 instance.
     */ 
    post {
         unstable {
            script {
                cleanupInstances()
            }
        }
        failure {
            script {
               cleanupInstances()
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