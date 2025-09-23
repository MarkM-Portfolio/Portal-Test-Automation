/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2022. All Rights Reserved. *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

// Import the shared library with the name configured in Jenkins
@Library("dx-shared-library") _

import java.text.SimpleDateFormat

def TERRAFORM_DOWNLOAD="dx-build-prereqs/terraform/terraform_0.12.20_linux_amd64.zip"

pipeline {
    parameters {
        string(name: 'INSTANCE_NAME', defaultValue: '', description: 'Name of the primary instance to be upgraded.',  trim: false)
        string(name: 'DX_BUILD_VERSION', defaultValue: '', description: 'DX build version to roll back.',  trim: false)
        booleanParam(name: 'CLUSTERED_ENV', defaultValue: 'true', description: 'Check this to make primary node clustered')
        booleanParam(name: 'ADD_ADDITIONAL_NODE', defaultValue: 'true', description: 'Check this to create additional node in clustered environment. Secondary instance name created will be {INSTANCE_NAME}-secondary')
        string(name: 'DX_USERNAME', defaultValue: 'wpsadmin', description: 'DX portal username',  trim: false)
        string(name: 'DX_PASSWORD', defaultValue: 'wpsadmin', description: 'DX portal password',  trim: false)
        choice(name: 'DOMAIN_SUFFIX', choices: ['.team-q-dev.com','.apps.dx-cluster-dev.hcl-dx-dev.net'],description: 'Select the domain for the host. Use the latter for openshift hybrid')
        string(name: 'KUBE_FLAVOUR', defaultValue: '', description: 'Provide the hybrid kube flavour',  trim: false)
    }

    agent {
        label 'build_infra'    
    }

    stages {
        stage('Prepare onpremise rollback settings') {
            steps {
                withAWS(credentials: 'aws_credentials', region: 'us-east-1') {
                    script {
                        dir("${workspace}/dx-cluster-onpremise/terraform/ec2-dx-onpremise-launch") {
                            if (!params.INSTANCE_NAME){
                                error("Instance name should not be empty")
                            }
                            
                           /*
                            * Determine the default user for tagging of our environments
                            * This user will be handled as the owner, so environments are relatable
                            */
                            INSTANCE_OWNER = dxJenkinsGetJobOwner()

                            //The instance name to roll back
                            if (!env.INSTANCE_NAME){
                                env.INSTANCE_NAME = 'dx-onpremise-update'
                            }

                            AWS_ZONE_ID = "Z06696141PM4GFM2MX2HR"

                            PRIMARY_HOST_NAME = "${params.INSTANCE_NAME}${DOMAIN_SUFFIX}"
                            SECONDARY_HOST_NAME = "${params.INSTANCE_NAME}-secondary${DOMAIN_SUFFIX}"
                            REMOTE_SEARCH_HOST_NAME = "${params.INSTANCE_NAME}-remote-search${DOMAIN_SUFFIX}"


                            echo "PRIMARY_HOST_NAME: ${PRIMARY_HOST_NAME}"
                            echo "SECONDARY_HOST_NAME: ${SECONDARY_HOST_NAME}"
                            echo "REMOTE_SEARCH_HOST_NAME: ${REMOTE_SEARCH_HOST_NAME}"
                            echo "AWS_REGION: ${env.AWS_REGION}"
                            echo "USE_PUBLIC_IP: ${env.USE_PUBLIC_IP}"
                            echo "AWS_SUBNET: ${env.AWS_SUBNET}"
                            echo "vpcSecGroupsParamater: ${env.vpcSecGroupsParamater}"
                            echo "DOMAIN_SUFFIX: ${env.DOMAIN_SUFFIX}"
                            echo "HOSTED_ZONE: ${env.HOSTED_ZONE}"
                            echo "INSTANCE_OWNER: ${INSTANCE_OWNER}"
                        }
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

        stage('Start preparing on-premise instance for rollback') {	
            steps {	
                script {	
                    withCredentials([	
                        usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')	
                    ]) {	
                        dir("${workspace}/dx-cluster-onpremise/terraform/ec2-dx-onpremise-launch") {	
                            configFileProvider([	
                                configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')	
                            ]) {	
                                // replace placeholder in the variables.tf to fit the execution	
                                sh(script: """	
                                    cp $DEPLOY_KEY test-automation-deployments.pem	
                                    chmod 0400 test-automation-deployments.pem	
                                """)
                                sh(script: """	
                                    target=${PRIMARY_HOST_NAME}
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
                            }	
                        }	
                    }	
                }	
            }	
        }


        stage('Stop WCM Syndication') {
            steps {
                dir("${workspace}/dx-cluster-onpremise/terraform/ec2-dx-onpremise-launch") {
                    echo "Stopping wcm automatic syndication"

                    sh  """
                        ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${PRIMARY_HOST_NAME} \
                        '(sudo sh /opt/HCL/wp_profile/ConfigEngine/ConfigEngine.sh disable-syndication-auto-scheduler -DWasPassword=${DX_PASSWORD} -DPortalAdminPwd=${DX_PASSWORD})'
                    """
                }
            }
        }

        stage('Disable automatic synchronization and Stop nodeAgents') {
            when { expression { params.CLUSTERED_ENV } }
            steps {
                dir("${workspace}/dx-cluster-onpremise/terraform/ec2-dx-onpremise-launch") {
                    echo "Disabling automatic node synchronization and stopping the nodeAgents"

                    sh  """
                        ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${PRIMARY_HOST_NAME} \
                        '(sudo /opt/HCL/AppServer/bin/wsadmin.sh -username ${DX_USERNAME} -password ${DX_PASSWORD} -lang jython -f /tmp/dx-onpremise/scripts/python/nodesync_disable_enable.py false 0 && \
                          sudo /opt/HCL/AppServer/bin/wsadmin.sh -username ${DX_USERNAME} -password ${DX_PASSWORD} -lang jython -f /tmp/dx-onpremise/scripts/python/nodeagent_stop.py)'
                    """
                }
            }
        }

        //Rollback the primary node
        stage('Rolling back the primary node') {
            steps {
                    dir("${workspace}/dx-cluster-onpremise/terraform/ec2-dx-onpremise-launch") {
                        echo "Rolling back the primary node"


                        sh  """
                            ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${PRIMARY_HOST_NAME} \
                            '(sudo sh /tmp/dx-onpremise/scripts/cf-rollback/01-rollback-primary-node.sh ${DX_USERNAME} ${DX_PASSWORD})'
                        """
                    }
            }
        }

        //Rollback the second node
        stage('Rolling back the Secondary node') {
            when { expression { params.ADD_ADDITIONAL_NODE } }
            steps {
                    dir("${workspace}/dx-cluster-onpremise/terraform/ec2-dx-onpremise-launch") {
                        echo "Rolling back the secondary node"

                        sh  """
                            ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${SECONDARY_HOST_NAME} \
                            '(sudo sh /tmp/dx-onpremise/scripts/cf-rollback/02-rollback-secondary-node.sh ${DX_USERNAME} ${DX_PASSWORD})'
                        """
                    }
            }
        }

        stage('Enable automatic synchronization') {
            when { expression { params.CLUSTERED_ENV } }
            steps {
                dir("${workspace}/dx-cluster-onpremise/terraform/ec2-dx-onpremise-launch") {
                    echo "Enabling automatic node synchronization"

                    sh  """
                        ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${PRIMARY_HOST_NAME} \
                        '(sudo /opt/HCL/AppServer/bin/wsadmin.sh -username ${DX_USERNAME} -password ${DX_PASSWORD} -lang jython -f /tmp/dx-onpremise/scripts/python/nodesync_disable_enable.py true 1)'
                    """
                }
            }
        }

        stage('Start Webserver') {
            steps {
                dir("${workspace}/dx-cluster-onpremise/terraform/ec2-dx-onpremise-launch") {
                    echo "Start Webserver"

                    sh  """
                        ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${PRIMARY_HOST_NAME} \
                        '(sudo /opt/HCL/HTTPServer/bin/apachectl start)'
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
