/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2021. All Rights Reserved. *
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
        string(name: 'DX_CORE_BUILD_VERSION', defaultValue: '', description: 'DX build version to update.',  trim: false)
        string(name: 'CF_VERSION', defaultValue: '', description: 'This is required if DX_CORE_BUILD_VERSION is master',  trim: false)
        booleanParam(name: 'CLUSTERED_ENV', defaultValue: 'true', description: 'Check this to make primary node clustered')
        booleanParam(name: 'ADD_ADDITIONAL_NODE', defaultValue: 'true', description: 'Check this to create additional node in clustered environment. Secondary instance name created will be {INSTANCE_NAME}-secondary')
        booleanParam(name: 'REMOTE_SEARCH_ENV', defaultValue: 'true', description: 'Check this to install and configure remote search')
        string(name: 'DX_USERNAME', defaultValue: 'wpsadmin', description: 'DX portal username',  trim: false)
        string(name: 'DX_PASSWORD', defaultValue: 'wpsadmin', description: 'DX portal password',  trim: false)
        choice(name: 'DOMAIN_SUFFIX', choices: ['.team-q-dev.com','.apps.dx-cluster-dev.hcl-dx-dev.net','.dx.hcl-dx-dev.net'],description: 'Select the domain for the host. Use the latter for openshift hybrid')
        string(name: 'KUBE_FLAVOUR', defaultValue: '', description: 'Provide the hybrid kube flavour',  trim: false)
    }

    agent {
        label 'build_infra'    
    }

    stages {
        stage('Prepare onpremise upgrade settings') {
            steps {
                withAWS(credentials: 'aws_credentials', region: 'us-east-1') {
                    script {
                        dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-launch") {
                            if (!params.INSTANCE_NAME){
                                error("Instance name should not be empty")
                            }
                            if (!params.DX_CORE_BUILD_VERSION){
                                error("DX core build version should not be empty")
                            }
                            
                           /*
                            * Determine the default user for tagging of our environments
                            * This user will be handled as the owner, so environments are relatable
                            */
                            INSTANCE_OWNER = dxJenkinsGetJobOwner()

                            env.retrieveFilesFrom = "retrieveFTPFiles.sh"

                            SELECTED_CORE_IMAGE_TAG = env.DX_CORE_BUILD_VERSION

                            CORE_MASTER_VERSION = params.CF_VERSION
                            CF_BUILD_NUMBER = '19'
                            if(SELECTED_CORE_IMAGE_TAG == 'develop' || SELECTED_CORE_IMAGE_TAG == 'release') {
                                url = "https://artifactory.cwp.pnp-hcl.com/artifactory/list/quintana-generic/dx-build-output"
                                tarFile  = sh (script: "${workspace}/dx-onpremise/scripts/get_latest_image.sh builds rohan_${SELECTED_CORE_IMAGE_TAG} ${url}", returnStdout: true)
                                SELECTED_CORE_IMAGE_TAG = tarFile.split('.tar')[0]
                                echo "${env.CORE_BUILD_TAG}"
                            } else if(SELECTED_CORE_IMAGE_TAG == 'master') {
                                if(CORE_MASTER_VERSION == '') {
                                    CORE_MASTER_VERSION  = sh (script: "${workspace}/dx-onpremise/scripts/get_latest_cf.sh", returnStdout: true)
                                    echo "${CORE_MASTER_VERSION}"
                                }
                                env.retrieveFilesFrom = "retrieveReleaseFTPFiles.sh"
                                CORE_MASTER_VERSION = CORE_MASTER_VERSION.toUpperCase()
                                CF_BUILD_NUMBER = CORE_MASTER_VERSION.split("CF")[1].toInteger()
                                SELECTED_CORE_IMAGE_TAG = "${SELECTED_CORE_IMAGE_TAG}_${CORE_MASTER_VERSION}"
                                env.retrieveFilesFrom = "retrieveReleaseFTPFiles.sh"
                                if(CF_BUILD_NUMBER <= 19) {
                                    SELECTED_CORE_IMAGE_TAG  = sh (script: "${workspace}/dx-onpremise/scripts/get_latest_image.sh ${CORE_MASTER_VERSION}", returnStdout: true)
                                }
                                echo "SELECTED_CORE_IMAGE_TAG: ${SELECTED_CORE_IMAGE_TAG}"
                                echo "CF_VERSION: ${CORE_MASTER_VERSION}"
                            }

                            AWS_ZONE_ID = "Z015304021BTM5FF7UP77"

                            env.BUILD_PATH = "DX_Core/${SELECTED_CORE_IMAGE_TAG}"
                            PRIMARY_HOST_NAME = "${params.INSTANCE_NAME}${DOMAIN_SUFFIX}"
                            SECONDARY_HOST_NAME = "${params.INSTANCE_NAME}-secondary${DOMAIN_SUFFIX}"
                            REMOTE_SEARCH_HOST_NAME = "${params.INSTANCE_NAME}-remote-search${DOMAIN_SUFFIX}"

                            if(params.KUBE_FLAVOUR == "openshift") {
                                def route53RecordSet = sh(script: "aws route53 list-resource-record-sets --hosted-zone-id ${AWS_ZONE_ID} --query 'ResourceRecordSets[?Name == `${PRIMARY_HOST_NAME}.`]'", returnStdout: true).trim()
                                echo "route53RecordSet= ${route53RecordSet}"
                                route53RecordSet = route53RecordSet.replace("\n","")
                                def jsonRoute53RecordSet = readJSON text: route53RecordSet
                                PRIMARY_HOST_NAME = jsonRoute53RecordSet.ResourceRecords[0].Value[0]
                                echo "PRIMARY_IP: ${PRIMARY_HOST_NAME}"
                            }

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
                            echo "DX_CORE_BUILD_VERSION: ${SELECTED_CORE_IMAGE_TAG}"
                        }
                    }
                }
            }
        }

        stage('Retrive IP for remote search') {
            when { expression { env.KUBE_FLAVOUR == "openshift" && params.REMOTE_SEARCH_ENV} }
            steps {
                withAWS(credentials: 'aws_credentials', region: 'us-east-1') {
                    script {
                            route53RecordSet = sh(script: "aws route53 list-resource-record-sets --hosted-zone-id ${AWS_ZONE_ID} --query 'ResourceRecordSets[?Name == `${REMOTE_SEARCH_HOST_NAME}.`]'", returnStdout: true).trim()
                            echo "route53RecordSet= ${route53RecordSet}"
                            route53RecordSet = route53RecordSet.replace("\n","")
                            jsonRoute53RecordSet = readJSON text: route53RecordSet
                            REMOTE_SEARCH_HOST_NAME = jsonRoute53RecordSet.ResourceRecords[0].Value[0]
                            echo "REMOTESEARCH_IP: ${REMOTE_SEARCH_HOST_NAME}"
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

        stage('Start preparing on-premise instance for upgrade') {	
            steps {	
                script {	
                    withCredentials([	
                        usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')	
                    ]) {	
                        dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-launch") {	
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

        stage('Update wkplc property file before upgrade') {
            steps {
                dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-launch") {
                    echo "Updating wkplc property file before upgrade"

                    sh  """
                        ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${PRIMARY_HOST_NAME} \
                        '(sudo sh -c "echo 'PWordDelete=false' >> /opt/HCL/wp_profile/ConfigEngine/properties/wkplc.properties")'
                    """
                }
            }
        }

        stage('Stop WCM Syndication') {
            steps {
                dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-launch") {
                    echo "Stopping wcm automatic syndication"

                    sh  """
                        ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${PRIMARY_HOST_NAME} \
                        '(sudo sh /opt/HCL/wp_profile/ConfigEngine/ConfigEngine.sh disable-syndication-auto-scheduler -DWasPassword=${DX_PASSWORD} -DPortalAdminPwd=${DX_PASSWORD})'
                    """
                }
            }
        }

        stage('Download DX Tar file') {
            steps {
                    dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-launch") {
                        echo "Download DX Tar file"

                        sh  """
                            chmod 744 ${workspace}/dx-onpremise/scripts/${env.retrieveFilesFrom}
                            DX_CORE_BUILD_VERSION="${SELECTED_CORE_IMAGE_TAG}" DX_IMAGE="${SELECTED_CORE_IMAGE_TAG}" CF_VERSION="${CF_BUILD_NUMBER}" ${workspace}/dx-onpremise/scripts/${env.retrieveFilesFrom}
                        """
                    }
            }
        }

        stage('Upgrading the remote search') {
            when { expression { params.REMOTE_SEARCH_ENV } }
            steps {
                withCredentials([
                    usernamePassword(credentialsId: 'artifactory', passwordVariable: 'ARTIFACTORY_PASSWORD', usernameVariable: 'ARTIFACTORY_USER')
                ]) {
                    dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-launch") {
                        echo "Remote search update to the selected version"

                        sh  """
                            scp -r -i test-automation-deployments.pem -o StrictHostKeyChecking=no ftp/msa/rtpmsa/projects/b/build.portal/builds/DX_Core/${SELECTED_CORE_IMAGE_TAG} centos@${REMOTE_SEARCH_HOST_NAME}:/tmp/msa/rtpmsa/projects/b/build.portal/builds/DX_Core
                            scp -i test-automation-deployments.pem -o StrictHostKeyChecking=no -r ${workspace}/dx-onpremise/scripts/helpers/UpdateRemoteSearch.xml centos@${REMOTE_SEARCH_HOST_NAME}:/tmp/dx-onpremise/scripts/helpers/UpdateRemoteSearch.xml
                        """

                        sh  """
                            ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${REMOTE_SEARCH_HOST_NAME} \
                            '(sed -i "s|CFDIR|${env.BUILD_PATH}|g;" /tmp/dx-onpremise/scripts/helpers/UpdateRemoteSearch.xml && \
                            sudo sh /tmp/dx-onpremise/scripts/cf-upgrade/03-upgrade-remote-search.sh ${DX_USERNAME} ${DX_PASSWORD})'
                        """

                        sh  """
                            ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${REMOTE_SEARCH_HOST_NAME} \
                            '(sudo sh /tmp/dx-onpremise/scripts/reStartRemoteSearch.sh ${DX_USERNAME} ${DX_PASSWORD})'
                        """
                    }
                }
            }
        }

        stage('Disable automatic synchronization and Stop nodeAgents') {
            when { expression { params.CLUSTERED_ENV } }
            steps {
                dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-launch") {
                    echo "Disabling automatic node synchronization and stopping the nodeAgents"

                    sh  """
                        ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${PRIMARY_HOST_NAME} \
                        '(sudo /opt/HCL/AppServer/bin/wsadmin.sh -username ${DX_USERNAME} -password ${DX_PASSWORD} -lang jython -f /tmp/dx-onpremise/scripts/python/nodesync_disable_enable.py false 0 && \
                          sudo /opt/HCL/AppServer/bin/wsadmin.sh -username ${DX_USERNAME} -password ${DX_PASSWORD} -lang jython -f /tmp/dx-onpremise/scripts/python/nodeagent_stop.py)'
                    """
                }
            }
        }

        stage('Upgrading the primary node') {
            steps {
                    dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-launch") {
                        echo "Upgrading the primary node"

                        sh  """
                            ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${PRIMARY_HOST_NAME} \
                            '(sudo rm -rf /tmp/upgrade)'
                            scp -r -i test-automation-deployments.pem -o StrictHostKeyChecking=no ftp/msa centos@${PRIMARY_HOST_NAME}:/tmp/upgrade/
                        """

                        sh  """
                            ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${PRIMARY_HOST_NAME} \
                            '(sudo sh /tmp/dx-onpremise/scripts/cf-upgrade/01-upgrade-dx-core.sh ${SELECTED_CORE_IMAGE_TAG} ${DX_USERNAME} ${DX_PASSWORD})'
                        """
                    }
            }
        }

        stage('Retrive IP for secondary node') {
            when { expression { env.KUBE_FLAVOUR == "openshift" && params.ADD_ADDITIONAL_NODE} }
            steps {
                withAWS(credentials: 'aws_credentials', region: 'us-east-1') {
                    script {
                            route53RecordSet = sh(script: "aws route53 list-resource-record-sets --hosted-zone-id ${AWS_ZONE_ID} --query 'ResourceRecordSets[?Name == `${SECONDARY_HOST_NAME}.`]'", returnStdout: true).trim()
                            echo "route53RecordSet= ${route53RecordSet}"
                            route53RecordSet = route53RecordSet.replace("\n","")
                            jsonRoute53RecordSet = readJSON text: route53RecordSet
                            SECONDARY_HOST_NAME = jsonRoute53RecordSet.ResourceRecords[0].Value[0]
                            echo "SECONDARYIP_IP: ${SECONDARY_HOST_NAME}"
                    }                    
                }
            }
        }

        stage('Upgrading the Secondary node') {
            when { expression { params.ADD_ADDITIONAL_NODE } }
            steps {
                    dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-launch") {
                        echo "Upgrading the secondary node"

                        sh  """
                            ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${SECONDARY_HOST_NAME} \
                            '(sudo rm -rf /tmp/upgrade)'
                            scp -r -i test-automation-deployments.pem -o StrictHostKeyChecking=no ftp/msa centos@${SECONDARY_HOST_NAME}:/tmp/upgrade/
                        """

                        sh  """
                            ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${SECONDARY_HOST_NAME} \
                            '(sudo sh /tmp/dx-onpremise/scripts/cf-upgrade/02-upgrade-dx-core.sh ${SELECTED_CORE_IMAGE_TAG} ${DX_USERNAME} ${DX_PASSWORD})'
                        """
                    }
            }
        }

        stage('Enable automatic synchronization') {
            when { expression { params.CLUSTERED_ENV } }
            steps {
                dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-launch") {
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
                dir("${workspace}/dx-onpremise/terraform/ec2-dx-onpremise-launch") {
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
