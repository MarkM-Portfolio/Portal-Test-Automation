/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2001, 2023. All Rights Reserved. *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

// Import the shared library with the name configured in Jenkins
@Library("dx-shared-library") _

import java.text.SimpleDateFormat

/* Global definitions for using common Jenkins library */
def commonJenkinsLib
def commonLibFile = "./jenkins_common/common-lib.gvy"

def TERRAFORM_DOWNLOAD="dx-build-prereqs/terraform/terraform_0.12.20_linux_amd64.zip"

pipeline {
    parameters {
        string(name: 'SECONDARY_INSTANCE_NAME', defaultValue: '', description: 'Name of the secondary instance to be created.',  trim: false)
        string(name: 'INSTANCE_TYPE', defaultValue: 't2.large', description: 'Type of the EC2 instance to be created',  trim: false)
        string(name: 'DX_CORE_BUILD_VERSION', defaultValue: '', description: 'Specify DX on-premise version to be installed in created EC2 instance Ex: DX_Core_20201202-055535_rohan_develop',  trim: false)
        string(name: 'CF_VERSION', defaultValue: '', description: 'This is required if DX_CORE_BUILD_VERSION is master',  trim: false)
        string(name: 'CONTEXT_ROOT_PATH', defaultValue: 'wps', description: 'Eg: wps',  trim: false)
        string(name: 'DX_CORE_HOME_PATH', defaultValue: 'portal', description: 'Eg: portal',  trim: false)
        string(name: 'PERSONALIZED_DX_CORE_PATH', defaultValue: 'myportal', description: 'Eg: myportal',  trim: false)
        string(name: 'DX_USERNAME', defaultValue: 'wpsadmin', description: 'DX portal username',  trim: false)
        string(name: 'DX_PASSWORD', defaultValue: 'wpsadmin', description: 'DX portal password',  trim: false)
        string(name: 'AWS_REGION', defaultValue: 'us-east-1', description: 'AWS_REGION.',  trim: false)
        string(name: 'AWS_SUBNET', defaultValue: 'subnet-02a350a23b3e39a43', description: 'AWS_SUBNET',  trim: false)
        string(name: 'DOMAIN_SUFFIX', defaultValue: '.team-q-dev.com', description: 'DOMAIN_SUFFIX',  trim: false)
        string(name: 'USE_PUBLIC_IP', defaultValue: 'false', description: 'USE_PUBLIC_IP',  trim: false)
        string(name: 'HOSTED_ZONE', defaultValue: 'Z3OEC7SLEHQ2P3', description: 'HOSTED_ZONE',  trim: false)
        string(name: 'vpcSecGroupsParamater', defaultValue: '\'vpc_security_groups=["sg-0ddaf1862f39be2be"]\'', description: 'vpcSecGroupsParamater',  trim: false)
        string(name: 'DXBuildNumber_NAME', defaultValue: '', description: 'Name of the AMI to build portal off of. For eg. dx-update-cf-v95_20220621-0817',  trim: false)
    } 

    agent {
        label 'build_infra'    
    }

    stages {
        stage('Prepare EC2 instance settings') {
            steps {
                script {
                    // Load common Jenkins library
                    commonJenkinsLib = load "${commonLibFile}" 
                    
                    dir("${workspace}/dx-cluster-onpremise/terraform/ec2-dx-onpremise-launch-ami-ami") {
                        //EC2 Instance settings
                        /*
                            For the dx-core image build we use a t2.large with 4vCPU and 8GB RAM
                            C Instances have high IPC CPU performance, which is beneficial
                            when having workloads with not many threads such as a setup.
                            Testing showed that this setup does not benefit from a NVMe drive
                            for docker, so we ommit this and do not use c5d.xlarge.
                        */
                        if (!params.SECONDARY_INSTANCE_NAME){
                            error("Instance name should not be empty")
                        }
                        if (!params.INSTANCE_TYPE){
                            error("Instance type should not be empty")
                        }

                        // version prefix for the displayed build-version
                        if (!env.VERSION){
                            env.VERSION = 'dx-binary-cf-v95'
                        }

                       /*
                        * Determine the default user for tagging of our environments
                        * This user will be handled as the owner, so environments are relatable
                        */
                        INSTANCE_OWNER = dxJenkinsGetJobOwner()

                        env.retrieveFilesFrom = "retrieveFTPFilesForSecondary.sh"
                        //Create variable for the updated CF AMI ID
                        // env.TF_VAR_CF_AMI_ID = "dx-update-cf-v95_secondary_20220708-0223"
                        env.DMGR_HOST_DEFAULT = "ci-linuxstal-39sht7rx.rtp.raleigh.ibm.com"

                        CURRENT_BUILD_IMAGE_TAG_CORE = env.DX_CORE_BUILD_VERSION
                        CF_BUILD_NUMBER = '19'
                        if(CURRENT_BUILD_IMAGE_TAG_CORE == "master") {
                            env.CF_VERSION = env.CF_VERSION.toUpperCase()
                            CF_BUILD_NUMBER = CF_VERSION.split("CF")[1].toInteger()
                            CURRENT_BUILD_IMAGE_TAG_CORE = "${CURRENT_BUILD_IMAGE_TAG_CORE}_${env.CF_VERSION}"
                            env.retrieveFilesFrom = "retrieveReleaseFTPFiles.sh"
                            if(CF_BUILD_NUMBER <= 19) {
                                CURRENT_BUILD_IMAGE_TAG_CORE  = sh (script: "${workspace}/dx-cluster-onpremise/scripts/get_latest_image.sh ${env.CF_VERSION}", returnStdout: true)
                                echo "${CURRENT_BUILD_IMAGE_TAG_CORE}"
                            }
                        }

                        // Defines the time to live in hours for all resources created (AMI, EC2 instances and DNS entries)
                        if (!env.RESOURCES_TTL){
                            env.RESOURCES_TTL = '24'
                        }

                        // determine build version and label current job accordingly
                        def dateFormat = new SimpleDateFormat("yyyyMMdd-HHmm")
                        def date = new Date()
                        // DXBuildNumber_NAME is being inherited by the upstream job that triggers this job
                        currentBuild.description = "${DXBuildNumber_NAME}"
                        env.TF_VAR_BUILD_LABEL = "${DXBuildNumber_NAME}"
                        // Description includes the version prefix and a timestamp
                        currentBuild.displayName = "${env.VERSION}_${dateFormat.format(date)}"
                        // Create variable for Terraform to determine current test run
                        env.TF_VAR_TEST_RUN_ID = "${env.VERSION}_${dateFormat.format(date)}"

                        // Calculate expiration timestamp
                        def ttl_stamp = (System.currentTimeMillis() + (env.RESOURCES_TTL.toString().toInteger() * 60 * 60 * 1000))
                        env.TF_VAR_EXPIRATION_STAMP = ttl_stamp

                        echo "AWS_REGION: ${env.AWS_REGION}"
                        echo "USE_PUBLIC_IP: ${env.USE_PUBLIC_IP}"
                        echo "AWS_SUBNET: ${env.AWS_SUBNET}"
                        echo "vpcSecGroupsParamater: ${env.vpcSecGroupsParamater}"
                        echo "DOMAIN_SUFFIX: ${env.DOMAIN_SUFFIX}"
                        echo "HOSTED_ZONE: ${env.HOSTED_ZONE}"
                        echo "Primary Node INSTANCE_IP: ${env.INSTANCE_IP}"
                        echo "INSTANCE_OWNER: ${INSTANCE_OWNER}"
                        echo "DX_CORE_BUILD_VERSION: ${env.DX_CORE_BUILD_VERSION}"
                        echo "DMGR_HOST_DEFAULT ${env.DMGR_HOST_DEFAULT}"
                        echo "EXPIRATION_STAMP ${ttl_stamp}"
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
                    curl -LJO "https://${G_ARTIFACTORY_HOST}/artifactory/${G_ARTIFACTORY_GENERIC_NAME}/${TERRAFORM_DOWNLOAD}"
                    unzip -o terraform_0.12.20_linux_amd64.zip
                    chmod +x terraform
                    ./terraform --help
                """
            }
        }

        stage('Create new Secondary node EC2 Instance and copy files') {
            steps {
                script {
                    withCredentials([
                        usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')
                    ]) {
                        // Before creating the new EC2 instance delete any existing Route53 record
                        commonJenkinsLib.checkDeleteRoute53_Record("id:${HOSTED_ZONE}", "${SECONDARY_INSTANCE_NAME}${DOMAIN_SUFFIX}")
                        dir("${workspace}/dx-cluster-onpremise/terraform/ec2-dx-onpremise-launch-ami") {
                            configFileProvider([
                                configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                            ]) {
                                // replace placeholder in the variables.tf to fit the execution
                                sh(script: """
                                    cp $DEPLOY_KEY test-automation-deployments.pem
                                    chmod 0400 test-automation-deployments.pem
                                    ${workspace}/terraform init -backend-config="key=terraform-status/dx-onpremise/${env.SECONDARY_INSTANCE_NAME}.key"
                                    ${workspace}/terraform apply --auto-approve -var instance_name="${env.SECONDARY_INSTANCE_NAME}" -var instance_owner="${INSTANCE_OWNER}" -var AWS_EC2_INSTANCE_TYPE="${INSTANCE_TYPE}" -var aws_region="${env.AWS_REGION}" -var aws_subnet="${env.AWS_SUBNET}" -var domain_suffix="${env.DOMAIN_SUFFIX}" -var ${env.vpcSecGroupsParamater} -var HOSTED_ZONE="${env.HOSTED_ZONE}" -var use_public_ip=${env.USE_PUBLIC_IP}
                                """)
                                // use terraform show to get all information about the instance for later use
                                def instanceInformation = sh(script: """
                                    ${workspace}/terraform show -json
                                """, returnStdout: true).trim()
                                echo "${instanceInformation}"
                                def instanceJsonInformation = readJSON text: instanceInformation
                                // extract ip, dns and id of created instance
                                if (env.USE_PUBLIC_IP.toBoolean()) {
                                    instanceIp = instanceJsonInformation['values']['root_module']['resources'][0]['values']['public_ip']
                                } else {
                                    instanceIp = instanceJsonInformation['values']['root_module']['resources'][0]['values']['private_ip']
                                }
                                env.SECONDARY_PRIVATE_IP = instanceJsonInformation['values']['root_module']['resources'][0]['values']['private_ip']
                                def instanceId = instanceJsonInformation['values']['root_module']['resources'][0]['values']['id']
                                echo "Instance ${instanceId} running on ${instanceIp}."
                                // test connect to environment via ssh, timeout if not successful
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
                                // set instanceIp, instanceId as variable for later use
                                env.SECONDARY_INSTANCE_IP = instanceIp
                                env.SECONDARY_INSTANCE_ID = instanceId
                                env.SECONDARY_INSTANCE_HOSTNAME = "${env.SECONDARY_INSTANCE_IP}".replace(".", "-")
                                
                            }
                        }
                    }
                }
            }
        }

        /*
            Install OS level based prereqs via yum using 01-setup-prereqs.sh
        */
        stage('Install prereqs for secondary') {
            steps {
                dir("${workspace}/dx-cluster-onpremise/terraform/ec2-dx-onpremise-launch-ami") {
                    sh """
                        ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no \
                        centos@${env.SECONDARY_INSTANCE_IP} '(cd /tmp/dx-onpremise/scripts && sudo sh 01-setup-prereqs.sh ${env.SECONDARY_PRIVATE_IP} ${env.SECONDARY_INSTANCE_NAME}${env.DOMAIN_SUFFIX})'
                    """
                }
            }
        }

        /*
            Preparing all response files and the params.txt on the EC2 instance with the correct
            credentials and settings to access repositories properly.
            All credentials are being taken from Jenkins and forwarded to 03-prepare-dx-setup.sh.
        */
        stage('Prepare dx-core setup for secondary') {
            steps {
                withCredentials([
                    usernamePassword(credentialsId: 'artifactory', passwordVariable: 'ARTIFACTORY_PASSWORD', usernameVariable: 'ARTIFACTORY_USER')
                ]) {
                    dir("${workspace}/dx-cluster-onpremise/terraform/ec2-dx-onpremise-launch-ami") {
                        sh """
                            chmod 744 ${workspace}/dx-cluster-onpremise/scripts/${env.retrieveFilesFrom}
                            DX_CORE_BUILD_VERSION="${env.DX_CORE_BUILD_VERSION}" DX_IMAGE="${CURRENT_BUILD_IMAGE_TAG_CORE}" CF_VERSION="${CF_BUILD_NUMBER}" ${workspace}/dx-cluster-onpremise/scripts/${env.retrieveFilesFrom}
                            scp -r -i test-automation-deployments.pem -o StrictHostKeyChecking=no ftp/msa centos@${env.SECONDARY_INSTANCE_IP}:/tmp
                            ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${env.SECONDARY_INSTANCE_IP} \
                            '(cd /tmp/dx-onpremise/scripts && chmod -R 777 /tmp/msa && ARTIFACTORY_HOST="${env.ARTIFACTORY_HOST}" \
                            DX_CORE_BUILD_VERSION="${CURRENT_BUILD_IMAGE_TAG_CORE}" \
                            sh 02-prepare-dx-setup.sh)'
                        """
                    }
                }
            }
        }

        /*
            Install portal base using 03-install-dx-portal85base-binary.sh
        */
        stage('Install dx-core Portal85Base for secondary') {
            steps {
                dir("${workspace}/dx-cluster-onpremise/terraform/ec2-dx-onpremise-launch-ami") {
                    sh """
                        ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${env.SECONDARY_INSTANCE_IP} \
                        '(cd /tmp/dx-onpremise/scripts && sudo sh 03-install-dx-portal85base-binary.sh Binary)'
                        echo "Portal is running on https://"${env.SECONDARY_INSTANCE_IP}
                    """
                }
            }
        }

        /*
            Apply CF using 04-install-dx-tool-imcl.sh
        */
        stage('Install dx-core imcl for secondary') {
            steps {
                dir("${workspace}/dx-cluster-onpremise/terraform/ec2-dx-onpremise-launch-ami") {
                    sh """
                        ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${env.SECONDARY_INSTANCE_IP} \
                        '(cd /tmp/dx-onpremise/scripts && sudo sh 04-install-dx-tool-imcl.sh)'
                        ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${env.SECONDARY_INSTANCE_IP} \
                        '(cd /tmp/ && rm -rf /tmp/dx-onpremise/)'
                        echo "Portal is running on https://"${env.SECONDARY_INSTANCE_IP}
                    """
                }
            }
        }

        /*
         *  This stage creates an AMI of the updated EC2 instance via terraform
         */
        stage('Create new AMI image for testing.') {
            steps {
                script {
                    withCredentials([
                        usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID'),
                    ]) {
                        dir("${workspace}/dx-cluster-onpremise/terraform/ami-creation") {
                            env.TF_VAR_TEST_EC2_ID = env.SECONDARY_INSTANCE_ID
                            // replace placeholder in the variables.tf to fit the current test-run
                            sh(script: """
                                sed -i 's/dx-update-cf-v95-local/${env.TF_VAR_TEST_RUN_ID}/g' variables.tf
                                ${workspace}/terraform init
                                ${workspace}/terraform apply -auto-approve
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
                withCredentials([
                    usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')
                ]) {
                    dir("${workspace}/dx-cluster-onpremise/terraform/ec2-dx-onpremise-launch-ami") {
                        sh(script: """
                            ${workspace}/terraform init
                            ${workspace}/terraform destroy -auto-approve -var instance_name="${env.SECONDARY_INSTANCE_NAME}" -var AWS_EC2_INSTANCE_TYPE="${env.INSTANCE_TYPE}"
                        """)
                    }
                    
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
}
