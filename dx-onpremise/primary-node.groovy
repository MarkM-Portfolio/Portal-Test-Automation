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
        string(name: 'INSTANCE_IP', defaultValue: '', description: 'IP of the primary node of the cluster.',  trim: false)
        string(name: 'INSTANCE_NAME', defaultValue: '', description: 'Name of the primary instance to be created.',  trim: false)
        string(name: 'INSTANCE_TYPE', defaultValue: 't2.large', description: 'Type of the EC2 instance to be created',  trim: false)
        string(name: 'DX_CORE_BUILD_VERSION', defaultValue: '', description: 'Specify DX on-premise version to be installed in created EC2 instance Ex: DX_Core_20201202-055535_rohan_develop',  trim: false)
        string(name: 'CF_VERSION', defaultValue: '', description: 'This is required if DX_CORE_BUILD_VERSION is master',  trim: false)
        string(name: 'DX_USERNAME', defaultValue: 'wpsadmin', description: 'DX portal username',  trim: false)
        string(name: 'DX_PASSWORD', defaultValue: 'wpsadmin', description: 'DX portal password',  trim: false)
        string(name: 'AWS_REGION', defaultValue: '', description: 'AWS_REGION.',  trim: false)
        string(name: 'AWS_SUBNET', defaultValue: '', description: 'AWS_SUBNET',  trim: false)
        string(name: 'DOMAIN_SUFFIX', defaultValue: '', description: 'DOMAIN_SUFFIX',  trim: false)
        booleanParam(name: 'USE_PUBLIC_IP', defaultValue: true, description: 'USE_PUBLIC_IP')
        string(name: 'HOSTED_ZONE', defaultValue: '', description: 'HOSTED_ZONE',  trim: false)
        string(name: 'vpcSecGroupsParamater', defaultValue: '', description: 'vpcSecGroupsParamater',  trim: false)
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
                        if (!params.INSTANCE_NAME){
                            error("Instance name should not be empty")
                        }
                        if (!params.INSTANCE_TYPE){
                            error("Instance type should not be empty")
                        }

                       /*
                        * Determine the default user for tagging of our environments
                        * This user will be handled as the owner, so environments are relatable
                        */
                        INSTANCE_OWNER = dxJenkinsGetJobOwner()

                        env.retrieveFilesFrom = "retrieveFTPFiles.sh"

                        CURRENT_BUILD_IMAGE_TAG_CORE = env.DX_CORE_BUILD_VERSION
                        CF_BUILD_NUMBER = '19'
                        if(env.DX_CORE_BUILD_VERSION == "master") {
                            env.CF_VERSION = env.CF_VERSION.toUpperCase()
                            CF_BUILD_NUMBER = CF_VERSION.split("CF")[1].toInteger()
                            CURRENT_BUILD_IMAGE_TAG_CORE = "${CURRENT_BUILD_IMAGE_TAG_CORE}_${env.CF_VERSION}"
                            env.retrieveFilesFrom = "retrieveReleaseFTPFiles.sh"
                            if(CF_BUILD_NUMBER <= 19) {
                                CURRENT_BUILD_IMAGE_TAG_CORE  = sh (script: "${workspace}/dx-onpremise/scripts/get_latest_image.sh ${env.CF_VERSION}", returnStdout: true)
                                echo "${CURRENT_BUILD_IMAGE_TAG_CORE}"
                            }
                        }

                        echo "DX_CORE_BUILD_VERSION: ${env.DX_CORE_BUILD_VERSION}"
                        echo "AWS_REGION: ${env.AWS_REGION}"
                        echo "USE_PUBLIC_IP: ${env.USE_PUBLIC_IP}"
                        echo "AWS_SUBNET: ${env.AWS_SUBNET}"
                        echo "vpcSecGroupsParamater: ${env.vpcSecGroupsParamater}"
                        echo "DOMAIN_SUFFIX: ${env.DOMAIN_SUFFIX}"
                        echo "HOSTED_ZONE: ${env.HOSTED_ZONE}"
                        echo "Retrieve path: ${env.retrieveFilesFrom}"
                        echo "INSTANCE_OWNER: ${INSTANCE_OWNER}"
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

        /*
         *  We will create a new EC2 instance to run the build - copy over all files via terraform 
         */
        stage('Create primary EC2 Instance and copy files') {
            steps {
                script {
                    withCredentials([
                        usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')
                    ]) {
                        // Before creating the new EC2 instance delete any existing Route53 record
                        commonJenkinsLib.checkDeleteRoute53_Record("id:${HOSTED_ZONE}", "${INSTANCE_NAME}${DOMAIN_SUFFIX}")
                        dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-launch") {
                            configFileProvider([
                                configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                            ]) {
                                // replace placeholder in the variables.tf to fit the execution
                                sh(script: """
                                    cp $DEPLOY_KEY test-automation-deployments.pem
                                    chmod 0400 test-automation-deployments.pem
                                    ${workspace}/terraform init -backend-config="key=terraform-status/dx-onpremise/${INSTANCE_NAME}.key"
                                    ${workspace}/terraform apply --auto-approve -var instance_name="${INSTANCE_NAME}" -var instance_owner="${INSTANCE_OWNER}" -var AWS_EC2_INSTANCE_TYPE="${INSTANCE_TYPE}" -var aws_region="${env.AWS_REGION}" -var aws_subnet="${env.AWS_SUBNET}" -var domain_suffix="${env.DOMAIN_SUFFIX}" -var ${env.vpcSecGroupsParamater} -var HOSTED_ZONE="${env.HOSTED_ZONE}" -var use_public_ip=${env.USE_PUBLIC_IP}
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
                                env.PRIVATE_IP = instanceJsonInformation['values']['root_module']['resources'][0]['values']['private_ip']
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
                                env.INSTANCE_IP = instanceIp
                                env.INSTANCE_ID = instanceId
                                env.DMGR_HOSTNAME = "${env.INSTANCE_IP}".replace(".", "-")
                            }
                        }
                    }
                }
            }
        }
        
        stage('Install wildcard certificate from dx-truststore') {
            steps {
                script {
                    // Cert settings
                    env.CERT_HOME = "${workspace}/dx-onpremise/certs"
                    env.ARTIFACTORY_URL = "https://$G_ARTIFACTORY_HOST/artifactory/$G_ARTIFACTORY_GENERIC_NAME/dx-truststore"
                    env.CERT_DOMAIN = "${env.DOMAIN_SUFFIX}".replaceFirst(/^./, "")
                    env.ACCESS_KEY = "${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-launch/test-automation-deployments.pem"
                    withCredentials([
                        usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')
                    ]) {
                        dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-launch") {
                            // Check if wildcard cert exists for domain
                            // If no wildcard cert exist get new cert
                            def ca_cert = sh (script: "curl ${env.ARTIFACTORY_URL}/${env.CERT_DOMAIN}/ca.cer", returnStdout: true)
                            if (ca_cert.contains(": 404,")) {
                                echo "No wildcard domain cert found for ${env.CERT_DOMAIN}. Get new cert."
                                sh """
                                    ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${env.INSTANCE_IP} \
                                    '(chmod 744 /tmp/dx-onpremise/scripts/letsencrypt/common-scripts/* &&
                                    sudo sh /tmp/dx-onpremise/scripts/letsencrypt/common-scripts/C1-add-acmesh.sh  &&
                                    sudo sh /tmp/dx-onpremise/scripts/letsencrypt/common-scripts/C2-get-ca_cert.sh "${env.INSTANCE_NAME}${env.DOMAIN_SUFFIX}" "$AWS_ACCESS_KEY_ID" "$AWS_SECRET_ACCESS_KEY")'
                                """
                            } else {
                                echo "Use wildcard domain cert found for ${env.CERT_DOMAIN}."
                                sh """
                                    sh ${workspace}/letsencrypt/le-wildcard-scripts/L2-download-lewc_cert.sh ${env.ARTIFACTORY_URL} ${env.CERT_DOMAIN} ${env.CERT_HOME}
                                    sh ${workspace}/letsencrypt/le-wildcard-scripts/X2-copy-cert-on-target.sh ${env.INSTANCE_IP} ${env.CERT_HOME}/${env.CERT_DOMAIN} "all" ${env.ACCESS_KEY}
                                    ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${env.INSTANCE_IP} \
                                    '(chmod 744 /tmp/dx-onpremise/scripts/letsencrypt/common-scripts/* &&
                                    sudo sh /tmp/dx-onpremise/scripts/letsencrypt/common-scripts/C1-add-acmesh.sh  &&
                                    sudo sh /tmp/dx-onpremise/scripts/letsencrypt/common-scripts/C3-move-ca_cert.sh "${env.CERT_DOMAIN}")'
                                """
                            }
                        }
                    }
                }
            }
        }

        /*
            Install OS level based prereqs via yum using 01-setup-prereqs.sh
        */
        stage('Install prereqs') {
            steps {
                dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-launch") {
                    sh """
                        ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no \
                        centos@${env.INSTANCE_IP} '(cd /tmp/dx-onpremise/scripts && sudo sh 01-setup-prereqs.sh ${env.PRIVATE_IP} ${env.INSTANCE_NAME}${env.DOMAIN_SUFFIX})'
                    """
                }
            }
        }

        /*
            Preparing all response files and the params.txt on the EC2 instance with the correct
            credentials and settings to access repositories properly.
            All credentials are being taken from Jenkins and forwarded to 03-prepare-dx-setup.sh.
        */
        stage('Prepare dx-core setup') {
            steps {
                withCredentials([
                    usernamePassword(credentialsId: 'artifactory', passwordVariable: 'ARTIFACTORY_PASSWORD', usernameVariable: 'ARTIFACTORY_USER')
                ]) {
                    dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-launch") {
                        sh """
                            chmod 744 ${workspace}/dx-onpremise/scripts/${env.retrieveFilesFrom}
                            DX_CORE_BUILD_VERSION="${env.DX_CORE_BUILD_VERSION}" DX_IMAGE="${CURRENT_BUILD_IMAGE_TAG_CORE}" CF_VERSION="${CF_BUILD_NUMBER}" ${workspace}/dx-onpremise/scripts/${env.retrieveFilesFrom}
                            scp -r -i test-automation-deployments.pem -o StrictHostKeyChecking=no ftp/msa centos@${env.INSTANCE_IP}:/tmp
                            ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${env.INSTANCE_IP} \
                            '(cd /tmp/dx-onpremise/scripts && chmod -R 777 /tmp/msa && ARTIFACTORY_HOST="${env.ARTIFACTORY_HOST}" \
                            DX_CORE_BUILD_VERSION="${CURRENT_BUILD_IMAGE_TAG_CORE}" \
                            sh 02-prepare-dx-setup.sh)'
                        """
                    }
                }
            }
        }

        /*
            Install portal base using 03-install-dx-portal85base.sh
        */
        stage('Install dx-core Portal85Base') {
            steps {
                dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-launch") {
                    sh """
                        ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${env.INSTANCE_IP} \
                        '(cd /tmp/dx-onpremise/scripts && sudo sh 03-install-dx-portal85base.sh ${env.INSTANCE_NAME}${env.DOMAIN_SUFFIX} ip-${env.DMGR_HOSTNAME}Node)'
                        echo "Portal is running on https://"${env.INSTANCE_IP}
                    """
                }
            }
        }

        /*
            Apply CF using 04-install-dx-applycf.sh
        */
        stage('Install dx-core ApplyCF') {
            steps {
                dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-launch") {
                    sh """
                        ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${env.INSTANCE_IP} \
                        '(cd /tmp/dx-onpremise/scripts && sudo sh 04-install-dx-applycf.sh)'
                        echo "Portal is running on https://"${env.INSTANCE_IP}
                    """
                }
            }
        }

        /*
            Run required ConfigEngine using 05-install-dx-configengine.sh
        */
        stage('Install dx-core ConfigEngine') {
            steps {
                dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-launch") {
                    sh """
                        ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${env.INSTANCE_IP} \
                        '(cd /tmp/dx-onpremise/scripts && sudo sh 05-install-dx-configengine.sh)'
                    """
                }
            }
        }

        /*
            Start portal server using 06-start-server.sh
        */
        stage('Start server') {
            steps {
                dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-launch") {
                    sh """
                        ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${env.INSTANCE_IP} \
                        '(cd /tmp/dx-onpremise/scripts && sh 06-start-server.sh)'
                        echo "Portal is running on http://${env.INSTANCE_NAME}${env.DOMAIN_SUFFIX}:10039/wps/portal"
                        echo "WAS is running on https://${env.INSTANCE_NAME}${env.DOMAIN_SUFFIX}:10041/ibm/console"
                    """
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
