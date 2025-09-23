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

/* Global definitions for using common Jenkins library */
def commonJenkinsLib
def commonLibFile = "./jenkins_common/common-lib.gvy"

TERRAFORM_DOWNLOAD_PATH="dx-build-prereqs/terraform"
TERRAFORM_DOWNLOAD_FILE="terraform_1.1.9_linux_amd64.zip"

pipeline { 
    parameters {
        string(name: 'INSTANCE_NAME', defaultValue: '', description: 'Name of the instance to be created',  trim: false)
        string(name: 'AWS_REGION', defaultValue: 'us-east-1', description: 'AWS region',  trim: false)
        string(name: 'AWS_SUBNET', defaultValue: 'subnet-09f521dfcea461588', description: 'AWS subnet',  trim: false)
        string(name: 'DOMAIN_SUFFIX', defaultValue: '.team-q-dev.com', description: 'Enter the domain suffix',  trim: false)
        string(name: 'HOSTED_ZONE', defaultValue: 'Z3OEC7SLEHQ2P3', description: 'Enter the AWS hosted zone',  trim: false)
        string(name: 'VPC_SECURITY_GROUPS', defaultValue: '', description: 'Enter the AWS security group(s)',  trim: false)
        booleanParam(name: 'USE_PUBLIC_IP', defaultValue: true, description: 'Needs to be set for public subnet for Jenkins to have access')
        choice(name: 'INSTANCE_POPO_SCHEDULE', choices: ['EST-nightly-shutoff-at-8pm','India-nightly-shutoff-at-8pm','EU-nightly-shutoff-at-8pm','UK-nightly-shutoff-at-8pm','PST-nightly-shutoff-at-8pm','EST-nightly-shutoff-at-1159pm','India-nightly-shutoff-at-1159pm','PST-nightly-shutoff-at-1159pm','EST-workdays-uptime-8am-8pm','India-workdays-uptime-8am-8pm','EU-workdays-uptime-8am-8pm','UK-workdays-uptime-8am-8pm','PST-workdays-uptime-8am-8pm','n/a'], description: 'Schedule for shutdown/startup')
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
                    // Load common Jenkins library
                    commonJenkinsLib = load "${commonLibFile}" 
                    
                    // Validate parameters
                    if (!params.INSTANCE_NAME){
                        error("Instance name should not be empty")
                    }
                    // Default parameters

                    if (!env.INSTANCE_TYPE){
                        env.INSTANCE_TYPE = "t3a.large"
                    }

                    // Terraform log-level
                    if (!env.TF_LOG) {
                        env.TF_LOG = 'INFO'
                    }

                    /*
                    * Determine the default user for tagging of our environments
                    * This user will be handled as the owner, so environments are relatable
                    */
                    INSTANCE_OWNER = dxJenkinsGetJobOwner()
                    if (INSTANCE_OWNER.endsWith("@pnp-hcl.com")) {
                        INSTANCE_OWNER = INSTANCE_OWNER.replace('@pnp-hcl.com', '@hcl.com');
                    }

                    // DB2 settings
                    if (!env.DB2_TAG) {
                        env.DB2_TAG="v11.5"
                    }

                    if (!env.DB2_PASSWORD) {
                        env.DB2_PASSWORD="diet4coke"
                    }

                    if(!env.VPC_SECURITY_GROUPS) {
                        vpcSecGroupsParamater = """'vpc_security_groups=["sg-0b1faa6b777393d69","sg-01cd0516fd7094663"]'"""
                    } else {
                        vpcSecGroupsParamater = env.VPC_SECURITY_GROUPS
                        if (!vpcSecGroupsParamater.startsWith("vpc_security_groups=")) {
                            vpcSecGroupsParamater = """'vpc_security_groups=${env.VPC_SECURITY_GROUPS}'"""
                        }
                    }
 
                    if (!env.IMAGE_REPOSITORY) {
                        env.IMAGE_REPOSITORY="quintana-docker.artifactory.cwp.pnp-hcl.com"
                    }

                    if (!env.DOCKER_IMAGE_NAME) {
                        env.DOCKER_IMAGE_NAME="dx-db2"
                    }

                    // Log values
                    echo "INSTANCE_NAME: ${INSTANCE_NAME}"
                    echo "INSTANCE_TYPE: ${INSTANCE_TYPE}"
                    echo "AWS_REGION: ${AWS_REGION}"
                    echo "AWS_SUBNET: ${AWS_SUBNET}"
                    echo "vpcSecGroupsParamater: ${vpcSecGroupsParamater}"
                    echo "DOMAIN_SUFFIX: ${DOMAIN_SUFFIX}"
                    echo "HOSTED_ZONE: ${HOSTED_ZONE}"
                    echo "INSTANCE_OWNER: ${INSTANCE_OWNER}"
                    echo "DB2_TAG: ${DB2_TAG}"
                    echo "IMAGE_REPOSITORY: ${IMAGE_REPOSITORY}"
                    echo "DOCKER_IMAGE_NAME: ${DOCKER_IMAGE_NAME}"
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
         *  Download docker image from artifactory as created machine may not have access
         */
        stage('Download image') {
            steps {
                sh """
                    docker pull ${IMAGE_REPOSITORY}/${DOCKER_IMAGE_NAME}:${DB2_TAG}
                    docker save -o db2-image.docker ${IMAGE_REPOSITORY}/${DOCKER_IMAGE_NAME}:${DB2_TAG}
                """
            }
        }

        /*
         *  We will create the environment via Terraform and use it to call the scripts to configure it
         */
        stage('Create instance, deploy software and other prereqs') {
            steps {
                script {
                    withCredentials([
                        usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID'),
                    ]) {
                        // Before creating the new EC2 instance delete any existing Route53 record
                        commonJenkinsLib.checkDeleteRoute53_Record("id:${HOSTED_ZONE}", "${INSTANCE_NAME}${DOMAIN_SUFFIX}")
                        dir("${workspace}/db2/terraform") {
                            configFileProvider([
                                configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                            ]) {
                                // Prepare terraform and execute terraform
                                sh(script: """
                                    sed -i "
                                        s|DB2_PASSWORD_PLACEHOLDER|${DB2_PASSWORD}|g
                                        s|IMAGE_NAME_PLACEHOLDER|${IMAGE_REPOSITORY}/${DOCKER_IMAGE_NAME}:${DB2_TAG}|g
                                    " ../scripts/02-launch-db2-container.sh
                                    cp $DEPLOY_KEY test-automation-deployments.pem
                                    chmod 0400 test-automation-deployments.pem
                                    ${workspace}/terraform init -backend-config="key=terraform-status/jenkinsagent/${INSTANCE_NAME}.key"
                                    ${workspace}/terraform apply -auto-approve -var instance_name="${INSTANCE_NAME}" -var instance_owner="${INSTANCE_OWNER}" -var aws_ec2_instance_type="${INSTANCE_TYPE}" -var aws_region="${AWS_REGION}" -var aws_subnet="${AWS_SUBNET}" -var domain_suffix="${DOMAIN_SUFFIX}" -var ${vpcSecGroupsParamater} -var hosted_zone="${HOSTED_ZONE}" -var use_public_ip=${USE_PUBLIC_IP} -var popo_schedule=${env.INSTANCE_POPO_SCHEDULE}
                                """)                                
                                // Use terraform show to get all information about the instance and extract some for later use
                                def instanceInformation = sh(script: """
                                    ${workspace}/terraform show -json
                                """, returnStdout: true).trim()
                                echo "${instanceInformation}"
                                instanceJsonInformation = readJSON text: instanceInformation
                                if (env.USE_PUBLIC_IP.toBoolean()) {
                                    instanceIp = instanceJsonInformation['values']['root_module']['resources'][4]['values']['public_ip']
                                } else {
                                    instanceIp = instanceJsonInformation['values']['root_module']['resources'][4]['values']['private_ip']
                                }
                                // Wait for environment to be ready
                                sh(script: """
                                    target=${instanceIp}
                                    n=0
                                    while ! ssh -o StrictHostKeyChecking=no -i test-automation-deployments.pem centos@\$target
                                    do
                                        echo "Machine ssh not available. Retrying in 10s."
                                        sleep 10
                                        n=\$(( n+1 ))
                                        if [ \$n -eq 20 ]; then
                                            echo "Machine failed to run within alotted time"
                                            exit 1
                                        fi
                                    done
                                """)
                                env.PRIVATE_IP = instanceJsonInformation['values']['root_module']['resources'][4]['values']['private_ip']
                                env.INSTANCE_IP = instanceIp
                                env.INSTANCE_ID = instanceJsonInformation['values']['root_module']['resources'][4]['values']['id']
                                echo "Instance ${env.INSTANCE_ID} running on ${instanceIp}"

                                sh """
                                    ssh -i test-automation-deployments.pem centos@${instanceIp} 'mkdir -p /home/centos/scripts'
                                    scp -r -i test-automation-deployments.pem ${workspace}/db2/scripts/* centos@${instanceIp}:/home/centos/scripts
                                    scp -r -i test-automation-deployments.pem ${workspace}/db2-image.docker centos@${instanceIp}:/tmp/db2-image.docker
                                    ssh -i test-automation-deployments.pem centos@${instanceIp} 'sudo sh ~/scripts/01-install-docker.sh'
                                """

                                println "Starting DB2"

                                sh """
                                    ssh -i test-automation-deployments.pem centos@${instanceIp} 'sudo sh ~/scripts/02-launch-db2-container.sh ${INSTANCE_NAME}${DOMAIN_SUFFIX} ${instanceIp}'
                                """
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
