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

// Import the shared library with the name configured in Jenkins
@Library("dx-shared-library") _

import java.text.SimpleDateFormat

/* Global definitions for using common Jenkins library */
def commonJenkinsLib
def commonLibFile = "./jenkins_common/common-lib.gvy"

def TERRAFORM_DOWNLOAD="dx-build-prereqs/terraform/terraform_0.12.20_linux_amd64.zip"

pipeline {
    parameters {
        string(name: 'REMOTE_SEARCH_INSTANCE_NAME', defaultValue: '', description: 'Name of the remote search instance to be created.',  trim: false)
        string(name: 'INSTANCE_TYPE', defaultValue: 't2.large', description: 'Type of the EC2 instance to be created',  trim: false)
        string(name: 'DX_CORE_BUILD_VERSION', defaultValue: '', description: 'Specify DX on-premise version to be installed in created EC2 instance Ex: DX_Core_20201202-055535_rohan_develop',  trim: false)
        string(name: 'CF_VERSION', defaultValue: '', description: 'This is required if DX_CORE_BUILD_VERSION is master',  trim: false)
        string(name: 'DX_USERNAME', defaultValue: 'wpsadmin', description: 'DX portal username',  trim: false)
        string(name: 'DX_PASSWORD', defaultValue: 'wpsadmin', description: 'DX portal password',  trim: false)
        string(name: 'INSTANCE_IP', defaultValue: '', description: 'IP of the primary node of the cluster.',  trim: false)
        choice(name: 'DOMAIN_SUFFIX', choices: ['.team-q-dev.com','.apps.dx-cluster-dev.hcl-dx-dev.net','.dx.hcl-dx-dev.net'],description: 'Select the domain for the host. Use the former for openshift hybrid')
        string(name: 'AWS_REGION', defaultValue: '', description: 'AWS_REGION.',  trim: false)
        string(name: 'AWS_SUBNET', defaultValue: '', description: 'AWS_SUBNET',  trim: false)
        string(name: 'USE_PUBLIC_IP', defaultValue: '', description: 'USE_PUBLIC_IP',  trim: false)
        string(name: 'HOSTED_ZONE', defaultValue: '', description: 'HOSTED_ZONE',  trim: false)
        string(name: 'vpcSecGroupsParamater', defaultValue: '', description: 'vpcSecGroupsParamater',  trim: false)
        string(name: 'CONTEXT_ROOT_PATH', defaultValue: '', description: 'Eg: wps',  trim: false)
        string(name: 'DX_CORE_HOME_PATH', defaultValue: '', description: 'Eg: portal',  trim: false)
        booleanParam(name: 'CLEANUP_ON_FAILURE', defaultValue: true, description: 'Cleanup on failure - default true.')
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
                    
                    dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-launch") {
                        //EC2 Instance settings
                        /*
                            For the dx-core image build we use a t2.large with 4vCPU and 8GB RAM
                            C Instances have high IPC CPU performance, which is beneficial
                            when having workloads with not many threads such as a setup.
                            Testing showed that this setup does not benefit from a NVMe drive
                            for docker, so we ommit this and do not use c5d.xlarge.
                        */
                        if (!params.REMOTE_SEARCH_INSTANCE_NAME){
                            error("Instance name should not be empty")
                        }
                        if (!params.INSTANCE_TYPE){
                            error("Instance type should not be empty")
                        }
                        if (!params.CONTEXT_ROOT_PATH){
                            env.CONTEXT_ROOT_PATH = "wps"
                        }
                        if (!params.DX_CORE_HOME_PATH){
                            env.DX_CORE_HOME_PATH = "portal"
                        }
                        
                       /*
                        * Determine the default user for tagging of our environments
                        * This user will be handled as the owner, so environments are relatable
                        */
                        INSTANCE_OWNER = dxJenkinsGetJobOwner()

                        env.retrieveFilesFrom = "retrieveFTPFiles.sh"
                        CURRENT_BUILD_IMAGE_TAG_CORE = env.DX_CORE_BUILD_VERSION
                        CF_BUILD_NUMBER = '19'
                        if(CURRENT_BUILD_IMAGE_TAG_CORE == "master") {
                            env.CF_VERSION = env.CF_VERSION.toUpperCase()
                            CF_BUILD_NUMBER = CF_VERSION.split("CF")[1].toInteger()
                            CURRENT_BUILD_IMAGE_TAG_CORE = "${CURRENT_BUILD_IMAGE_TAG_CORE}_${env.CF_VERSION}"
                            env.retrieveFilesFrom = "retrieveReleaseFTPFiles.sh"
                            if(CF_BUILD_NUMBER <= 19) {
                                CURRENT_BUILD_IMAGE_TAG_CORE  = sh (script: "${workspace}/dx-onpremise/scripts/get_latest_image.sh ${env.CF_VERSION}", returnStdout: true)
                                echo "${CURRENT_BUILD_IMAGE_TAG_CORE}"
                            }
                        }

                        echo "AWS_REGION: ${env.AWS_REGION}"
                        echo "USE_PUBLIC_IP: ${env.USE_PUBLIC_IP}"
                        echo "AWS_SUBNET: ${env.AWS_SUBNET}"
                        echo "DOMAIN_SUFFIX: ${env.DOMAIN_SUFFIX}"
                        echo "vpcSecGroupsParamater: ${env.vpcSecGroupsParamater}"
                        echo "HOSTED_ZONE: ${env.HOSTED_ZONE}"
                        echo "Primary Node INSTANCE_IP: ${env.INSTANCE_IP}"
                        echo "INSTANCE_OWNER: ${INSTANCE_OWNER}"
                        echo "DX_CORE_BUILD_VERSION: ${env.DX_CORE_BUILD_VERSION}"
                        echo "CONTEXT_ROOT_PATH: ${env.CONTEXT_ROOT_PATH}"
                        echo "DX_CORE_HOME_PATH: ${env.DX_CORE_HOME_PATH}"
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

        stage('Create new EC2 Instance and copy files for Remote Search') {
            steps {
                script {
                    withCredentials([
                        usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')
                    ]) {
                        // Before creating the new EC2 instance delete any existing Route53 record
                        commonJenkinsLib.checkDeleteRoute53_Record("id:${HOSTED_ZONE}", "${REMOTE_SEARCH_INSTANCE_NAME}${DOMAIN_SUFFIX}")
                        dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-launch") {
                            configFileProvider([
                                configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                            ]) {
                                // replace placeholder in the variables.tf to fit the execution
                                sh(script: """
                                    cp $DEPLOY_KEY test-automation-deployments.pem
                                    chmod 0400 test-automation-deployments.pem
                                    ${workspace}/terraform init -backend-config="key=terraform-status/dx-onpremise/${REMOTE_SEARCH_INSTANCE_NAME}.key"
                                    ${workspace}/terraform apply --auto-approve -var instance_name="${REMOTE_SEARCH_INSTANCE_NAME}" -var instance_owner="${INSTANCE_OWNER}" -var AWS_EC2_INSTANCE_TYPE="${INSTANCE_TYPE}" -var aws_region="${env.AWS_REGION}" -var aws_subnet="${env.AWS_SUBNET}" -var domain_suffix="${env.DOMAIN_SUFFIX}" -var ${env.vpcSecGroupsParamater} -var HOSTED_ZONE="${env.HOSTED_ZONE}" -var use_public_ip=${env.USE_PUBLIC_IP}
                                """)
                                // use terraform show to get all information about the instance for later use
                                def instanceInformation = sh(script: """
                                    ${workspace}/terraform show -json
                                """, returnStdout: true).trim()
                                def instanceJsonInformation = readJSON text: instanceInformation
                                // extract private/public ip, dns and id of created instance
                                if (env.USE_PUBLIC_IP.toBoolean()) {
                                    instanceIp = instanceJsonInformation['values']['root_module']['resources'][0]['values']['public_ip']
                                } else {
                                    instanceIp = instanceJsonInformation['values']['root_module']['resources'][0]['values']['private_ip']
                                }
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
                                env.REMOTE_SEARCH_INSTANCE_IP = instanceIp
                                env.REMOTE_SEARCH_INSTANCE_ID = instanceId
                                env.REMOTE_SEARCH_INSTANCE_HOSTNAME = "${env.REMOTE_SEARCH_INSTANCE_IP}".replace(".", "-")
                            }
                        }
                    }
                }
            }
        }

        /*
            Install OS level based prereqs via yum using 01-setup-prereqs.sh
        */
        stage('Install prereqs for remote search') {
            steps {
                dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-launch") {
                    sh """
                        ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no \
                        centos@${env.REMOTE_SEARCH_INSTANCE_IP} '(cd /tmp/dx-onpremise/scripts && sudo sh 01-setup-prereqs.sh)'
                    """
                }
            }
        }

        /*
            Preparing all response files and the params.txt on the EC2 instance with the correct
            credentials and settings to access repositories properly.
            All credentials are being taken from Jenkins and forwarded to 03-prepare-dx-setup.sh.
        */
        stage('Prepare for remote search') {
            steps {
                withCredentials([
                    usernamePassword(credentialsId: 'artifactory', passwordVariable: 'ARTIFACTORY_PASSWORD', usernameVariable: 'ARTIFACTORY_USER')
                ]) {
                    dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-launch") {
                        sh """
                            chmod 744 ${workspace}/dx-onpremise/scripts/${env.retrieveFilesFrom}
                            DX_CORE_BUILD_VERSION="${env.DX_CORE_BUILD_VERSION}" DX_IMAGE="${CURRENT_BUILD_IMAGE_TAG_CORE}" CF_VERSION="${CF_BUILD_NUMBER}" ${workspace}/dx-onpremise/scripts/${env.retrieveFilesFrom}
                            scp -r -i test-automation-deployments.pem -o StrictHostKeyChecking=no ftp/msa centos@${env.REMOTE_SEARCH_INSTANCE_IP}:/tmp
                            ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${env.REMOTE_SEARCH_INSTANCE_IP} \
                            '(cd /tmp/dx-onpremise/scripts && chmod -R 777 /tmp/msa && ARTIFACTORY_HOST="${env.ARTIFACTORY_HOST}" \
                            REMOTE_SEARCH_ENV=true \
                            DX_CORE_BUILD_VERSION="${CURRENT_BUILD_IMAGE_TAG_CORE}" \
                            sh 02-prepare-dx-setup.sh)'
                        """
                    }
                }
            }
        }

        stage('Download remote search packages') {
            steps {
                withCredentials([
                    usernamePassword(credentialsId: 'artifactory', passwordVariable: 'ARTIFACTORY_PASSWORD', usernameVariable: 'ARTIFACTORY_USER')
                ]) {
                    dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-launch") {
                        sh """
                            sh ${workspace}/dx-onpremise/scripts/08-download-remote-search-packages.sh ${env.REMOTE_SEARCH_INSTANCE_IP}
                        """
                    }
                }
            }
        }

        /*
            Install remote search base with CFXX using 07-install-dx-portal85remotesearch.sh
        */
        stage('Install remote search 85CF') {
            steps {
                dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-launch") {
                    sh """
                        ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${env.REMOTE_SEARCH_INSTANCE_IP} \
                        '(cd /tmp/dx-onpremise/scripts && sudo sh 07-install-dx-portal85remotesearch.sh)'
                        echo "Remote Search installation completed"
                    """
                }
            }
        }

        stage('Configure remote search') {
            steps {
                withCredentials([
                    usernamePassword(credentialsId: 'artifactory', passwordVariable: 'ARTIFACTORY_PASSWORD', usernameVariable: 'ARTIFACTORY_USER')
                ]) {
                    dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-launch") {
                        sh """
                            ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${env.INSTANCE_IP} \
                            '(cd /tmp/dx-onpremise/scripts && REMOTE_SEARCH_INSTANCE_IP="${env.REMOTE_SEARCH_INSTANCE_IP}" \
                            REMOTE_SEARCH_ENV=true \
                            sh 08-portal-remote-search-configengine.sh)'
                        """

                        sh """
                        ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${env.INSTANCE_IP} \
                        '(sudo cp /root/LTPAKeyExported /home/centos)'
                        """

                        sh """
                             scp -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${env.INSTANCE_IP}:/home/centos/LTPAKeyExported .
                             scp -i test-automation-deployments.pem -o StrictHostKeyChecking=no LTPAKeyExported centos@${env.REMOTE_SEARCH_INSTANCE_IP}:/tmp/dx-onpremise/
                        """
                        
                        sh """
                            ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${env.REMOTE_SEARCH_INSTANCE_IP} \
                            '(cd /tmp/dx-onpremise/scripts && REMOTE_SEARCH_INSTANCE_IP="${env.REMOTE_SEARCH_INSTANCE_IP}" \
                            INSTANCE_IP="${env.INSTANCE_IP}" \
                            sh 08-remote-search-configengine.sh)'
                        """
                        
                    }
                }
            }
        }

        stage ('Restart the servers') {
            steps {
                dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-launch") {

                    echo "Restart the remote search server"

                    sh  """
                        ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${env.REMOTE_SEARCH_INSTANCE_IP} \
                        '(sudo sh /tmp/dx-onpremise/scripts/reStartRemoteSearch.sh ${DX_USERNAME} ${DX_PASSWORD})'
                    """

                    echo "Restart the portal server"

                    sh  """
                        ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${env.INSTANCE_IP} \
                        '(sudo sh /tmp/dx-onpremise/scripts/reStartPortal.sh ${DX_USERNAME} ${DX_PASSWORD})'
                    """
                }
            }
        }

        stage('Add Remote Search services') {
            steps {
                withCredentials([
                    usernamePassword(credentialsId: 'artifactory', passwordVariable: 'ARTIFACTORY_PASSWORD', usernameVariable: 'ARTIFACTORY_USER')
                ]) {
                    dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-launch") {
                        sh """
                            ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${env.INSTANCE_IP} \
                            '(cd /tmp/dx-onpremise/scripts && REMOTE_SEARCH_INSTANCE_IP="${env.REMOTE_SEARCH_INSTANCE_IP}" \
                            INSTANCE_IP="${env.INSTANCE_IP}" DX_CORE_HOME_PATH="${env.DX_CORE_HOME_PATH}" \
                            CONTEXT_ROOT_PATH="${env.CONTEXT_ROOT_PATH}" REMOTE_SEARCH_ENV=true \
                            sh 09-remote-search-config-APIs.sh)'
                        """
                    }
                }
            }
        }
    }

    post {  
        failure {
            script {
                withCredentials([
                    usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID'),
                ]) {
                    dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-launch") {
                        configFileProvider([
                            configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                        ]) {
                            if (env.REMOTE_SEARCH_INSTANCE_NAME && env.CLEANUP_ON_FAILURE) {
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
