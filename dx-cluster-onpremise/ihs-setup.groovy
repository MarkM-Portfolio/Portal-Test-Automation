/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2001, 2022. All Rights Reserved. *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

import java.text.SimpleDateFormat

def TERRAFORM_DOWNLOAD="dx-build-prereqs/terraform/terraform_0.12.20_linux_amd64.zip"

pipeline {
    parameters {
        string(name: 'INSTANCE_IP', defaultValue: '', description: 'IP of the primary node of the cluster.',  trim: false)
        string(name: 'INSTANCE_NAME', defaultValue: 'dx-onpremise', description: 'Name of the instance to be created. In cluster environment, this is the name of primary instance',  trim: false)
        string(name: 'SECONDARY_INSTANCE_IP', defaultValue: '', description: 'IP of the secondary node of the cluster.',  trim: false)
        string(name: 'DX_USERNAME', defaultValue: 'wpsadmin', description: 'DX portal username',  trim: false)
        string(name: 'DX_PASSWORD', defaultValue: 'wpsadmin', description: 'DX portal password',  trim: false)
        choice(name: 'DOMAIN_SUFFIX', choices: ['.team-q-dev.com','.apps.dx-cluster-dev.hcl-dx-dev.net','.hcl-dx-dev.net'],description: 'Select the domain for the host. Use the former for openshift hybrid')
        string(name: 'DMGR_HOSTNAME', defaultValue: '', description: 'DMGR_HOSTNAME',  trim: false)
        booleanParam(name: 'CLUSTERED_ENV', defaultValue: 'true', description: 'Check this to make primary node clustered')
        booleanParam(name: 'ADD_ADDITIONAL_NODE', defaultValue: 'false', description: 'Check this to create additional node in clustered environment. Secondary instance name created will be {INSTANCE_NAME}-secondary')
        string(name: 'AWS_REGION', defaultValue: '', description: 'AWS_REGION.',  trim: false)
    } 

    agent {
        label 'build_infra'    
    }

    stages {
        stage('Prepare EC2 instance settings') {
            steps {
                script {
                    echo "AWS_REGION: ${env.AWS_REGION}"
                    echo "DOMAIN_SUFFIX: ${env.DOMAIN_SUFFIX}"
                    echo "Primary Node INSTANCE_IP: ${env.INSTANCE_IP}"

                    env.AWS_REGION = "us-east-1"
                    env.DMGR_HOST_DEFAULT = "ci-linuxstal-39sht7rx"
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
            Create IHS
        */
        stage('Create IHS') {
            steps {
                withCredentials([
                    usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID'),
                    sshUserPrivateKey(credentialsId: "dx-core-tests-base-image-key", keyFileVariable: 'connectKey')
                ]) {
                    dir("${workspace}/dx-cluster-onpremise/terraform/ec2-dx-onpremise-launch") {
                        configFileProvider([
                                configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                            ]) {                              // replace placeholder in the variables.tf to fit the execution
                                sh(script: """
                                    cp ${connectKey} test-automation-deployments.pem
                                    chmod 0400 test-automation-deployments.pem
                                    cp ${DEPLOY_KEY} test-automation-deployments-secondary.pem
                                    chmod 0400 test-automation-deployments-secondary.pem
                                """)
                            sh """
                                mkdir ${workspace}/dx-cluster-onpremise/ihs
                                curl https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-build-prereqs/image_prereqs/was.repo.9000.ihs.zip -o ${workspace}/dx-cluster-onpremise/ihs/was.repo.9000.ihs.zip --create-dirs
                                curl https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-build-prereqs/image_prereqs/was.repo.9000.plugins.zip -o ${workspace}/dx-cluster-onpremise/ihs/was.repo.9000.plugins.zip --create-dirs
                                curl https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-build-prereqs/image_prereqs/was.repo.9000.wct.zip -o ${workspace}/dx-cluster-onpremise/ihs/was.repo.9000.wct.zip --create-dirs
                                ls ${workspace}/dx-cluster-onpremise/ihs
                            """
                            sh """
                                ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no root@${env.INSTANCE_IP} \
                                '(mkdir -p /tmp/dx-onpremise/ihs)'
                            """
                            sh """
                                scp -i test-automation-deployments.pem -o StrictHostKeyChecking=no -r ${workspace}/dx-cluster-onpremise/ihs/* root@${env.INSTANCE_IP}:/tmp/dx-onpremise/ihs
                                ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no root@${env.INSTANCE_IP} \
                                '(cd /tmp/dx-onpremise/scripts && sudo sh cluster/install-ihs-dependencies.sh)'
                            """
                        }
                    }
                }
            }
        }

        /*
            Configure IHS for clustered environment
        */
        stage('Configure IHS For Cluster') {
            when { expression { params.CLUSTERED_ENV } }
            steps {
                dir("${workspace}/dx-cluster-onpremise/terraform/ec2-dx-onpremise-launch") {
                    sh """
                        ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no root@${env.INSTANCE_IP} \
                        '(sudo sh /opt/IBM/WebSphere/ihsToolbox/WCT/wctcmd.sh -tool pct -defLocPathname /opt/IBM/WebSphere/ihsPlugins -defLocName DefaultDefinition -createDefinition -response /tmp/dx-onpremise/scripts/helpers/http-server/pct_responsefile.txt)'
                    """

                    sh """
                        ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no root@${env.INSTANCE_IP} \
                        '(cd /tmp/dx-onpremise/scripts && sudo sh cluster/ihs-setup.sh ${env.DMGR_HOST_DEFAULT}Node ${params.INSTANCE_NAME}${env.DOMAIN_SUFFIX} ${env.DX_USERNAME} ${env.DX_PASSWORD})'
                    """
                }
            }
        }

        /*
            Configure IHS for standalone environment
        */
        stage('Configure IHS for standalone') {
            when { expression { !params.CLUSTERED_ENV } }
            steps {
                dir("${workspace}/dx-cluster-onpremise/terraform/ec2-dx-onpremise-launch") {
                    sh """
                        ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no root@${env.INSTANCE_IP} \
                        '(sudo sh /opt/IBM/WebSphere/ihsToolbox/WCT/wctcmd.sh -tool pct -defLocPathname /opt/IBM/WebSphere/ihsPlugins -defLocName DefaultDefinition -createDefinition -response /tmp/dx-onpremise/scripts/helpers/http-server/pct_responsefile.txt)'
                    """

                    sh """
                        ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no root@${env.INSTANCE_IP} \
                        '(cd /tmp/dx-onpremise/scripts && sudo sh ihs-setup-for-standalone.sh ${env.DMGR_HOST_DEFAULT}Node ${params.INSTANCE_NAME}${env.DOMAIN_SUFFIX} ${env.DX_USERNAME} ${env.DX_PASSWORD})'
                    """
                }
            }
        }

        stage('Enable SSL for IHS') {
            steps {
                dir("${workspace}/dx-cluster-onpremise/terraform/ec2-dx-onpremise-launch") {

                    sh """
                        ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no root@${env.INSTANCE_IP} \
                        '(sudo sh /tmp/dx-onpremise/scripts/enable-ssl-for-ihs.sh ${env.DOMAIN_SUFFIX} ${env.DMGR_HOST_DEFAULT}Node ${env.DX_USERNAME} ${env.DX_PASSWORD} ${params.CLUSTERED_ENV})'
                    """
                    
                    script {
                        if(params.CLUSTERED_ENV){
                            sh """
                            ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no root@${env.INSTANCE_IP} \
                            '(sudo sh /tmp/dx-onpremise/scripts/cluster/restart-dmgr.sh &&
                              sudo sh /tmp/dx-onpremise/scripts/cluster/restart-node.sh)'
                            """
                        } else {
                            sh """
                            ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no root@${env.INSTANCE_IP} \
                            '(sudo /opt/IBM/WebSphere/HTTPServer/bin/apachectl start)'
                            """
                        }
                        if(params.ADD_ADDITIONAL_NODE){
                            sh """
                            ssh -i test-automation-deployments-secondary.pem -o StrictHostKeyChecking=no centos@${env.SECONDARY_INSTANCE_IP} \
                            '(sudo sh /tmp/dx-onpremise/scripts/cluster/restart-node.sh &&
                            sudo sh /tmp/dx-onpremise/scripts/reStartPortal.sh ${env.DX_USERNAME} ${env.DX_PASSWORD})'
                        """
                        }
                    }

                    sh  """
                        ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no root@${env.INSTANCE_IP} \
                        '(sudo sh /tmp/dx-onpremise/scripts/reStartPortal.sh ${env.DX_USERNAME} ${env.DX_PASSWORD})'
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