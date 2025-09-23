/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2023. All Rights Reserved.       *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

// This pipeline is to create EC2 instance with optional params DX_SQUAD using shared library and test.

// Import the shared library with the name configured in Jenkins
@Library("dx-shared-library") _

// test map for creating a new EC2 instance
def terraformVarsEC2 = [:]
terraformVarsEC2.tfvar_instance_name = "dx_docker_compose_test"
terraformVarsEC2.tfvar_instance_owner = "kumar-manish@hcl.com"

/* Common paths - must be here always */
moduleDirectory = "./kube/lifecycle/modules"
configDirectory = "./kube/lifecycle/config"
scriptDirectory = "./kube/lifecycle/scripts"
// Create object to store parameters with values
def pipelineParameters = [:]

pipeline {

    agent {
        label 'build_infra'
    }


    parameters {
      string(name: 'DEPLOYMENT_LEVEL', defaultValue: 'develop', description: 'Deploying develop images')
      string(name: 'KUBE_FLAVOUR', defaultValue: 'native', description: 'Deploying a native kube environment.')
      string(name: 'DEPLOYMENT_METHOD', defaultValue: 'helm', description: 'Deployment method')
      string(name: 'CONTEXT_ROOT_PATH', defaultValue: 'wps', description: 'Context root')
      string(name: 'DX_CORE_HOME_PATH', defaultValue: 'portal', description: 'Home path')
      string(name: 'PERSONALIZED_DX_CORE_PATH', defaultValue: 'myportal', description: 'Personalized path')
      string(name: 'DESTROY_DEPLOYMENT', defaultValue: 'true', description: 'Destroy deployment')

    }

   
    stages {

        /*
         * Load modules and configuration from the different flavours using "load"
         */
        stage("Load modules and configuration") {
            steps {
                script {
                    env.THIS_BUILD_OWNER = dxJenkinsGetJobOwner()
                    env.IMAGE_REPOSITORY="quintana-docker-prod"
                    commonConfig = load "${configDirectory}/common.gvy"
                    commonModule = load "${moduleDirectory}/common.gvy"
                    kubeModule = load "${moduleDirectory}/${commonConfig.COMMON_KUBE_FLAVOUR}.gvy"
                }
            }
        }

        stage('Install Terraform') {
            steps {
                script {
                    dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/dx-docker-compose-tests/parameters-test.yaml")
                    dxTerraformInstall()
                }
            }
        }
        stage('Create EC2 Instance') {
            steps {
                script {
                    terraformVarsEC2.tfvar_instance_DX_SQUAD = pipelineParameters.DX_SQUAD
                    terraformVarsEC2.tfvar_aws_ec2_instance_type = pipelineParameters.INSTANCE_TYPE
                    terraformVarsEC2.tfvar_dedicated_host_id = pipelineParameters.DEDICATED_HOST_ID
                    try {
                        terraformVarsEC2 = dxTerraformCreateEc2Instance(terraformVarsEC2)
                    } catch (err) {
                        error("ERROR: Creating EC2 instance failed.")
                    }
                    println "terraformVarsEC2 = " + terraformVarsEC2
                    println "Test OK"
                }
            }
        }
        /*
         * After a successful creation of the EC2 instance, we install all required software on it and make sure that our settings
         * will be copied over to the target machine.
         */
        stage('Prepare EC2 instance') {
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    sh """
                        chmod 600 ${DEPLOY_KEY}
                        scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/dx-docker-compose-tests/scripts/install-prereqs.sh centos@${terraformVarsEC2.instance_private_ip}:/tmp
                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'chmod +x /tmp/install-prereqs.sh && sh /tmp/install-prereqs.sh'
                    """
                }
            }
        }

        /*
         * Once prerequisites are installed, we pull the repositories needed for the tests in the workspace, and scp transfer it to the EC2 instance. 
         */ 
        stage('Run docker compose') {
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    sshagent(credentials: ['jenkins-git']) {
                        script {
                            tags = commonModule.determineImageTags();
                            println("CORE_IMAGE_TAG====>:\n${tags.CORE_IMAGE_TAG}")

                            sh """
                                chmod 600 ${DEPLOY_KEY}
                                git clone https://github.com/HCL-TECH-SOFTWARE/dx-docker-compose.git ${workspace}/dx-docker-compose
                                sed -i -r 's|^DX_DOCKER_IMAGE_CONTENT_COMPOSER=.*|DX_DOCKER_IMAGE_CONTENT_COMPOSER=${commonConfig.COMMON_IMAGE_REPOSITORY}.artifactory.cwp.pnp-hcl.com/portal/content-ui:${tags.CC_IMAGE_TAG}|' ${workspace}/dx-docker-compose/dx.properties
                                sed -i -r 's|^DX_DOCKER_IMAGE_CORE=.*|DX_DOCKER_IMAGE_CORE=${commonConfig.COMMON_IMAGE_REPOSITORY}.artifactory.cwp.pnp-hcl.com/dxen:${tags.CORE_IMAGE_TAG}|' ${workspace}/dx-docker-compose/dx.properties
                                sed -i -r 's|^DX_DOCKER_IMAGE_DIGITAL_ASSET_MANAGER=.*|DX_DOCKER_IMAGE_DIGITAL_ASSET_MANAGER=${commonConfig.COMMON_IMAGE_REPOSITORY}.artifactory.cwp.pnp-hcl.com/portal/media-library:${tags.DAM_IMAGE_TAG}|' ${workspace}/dx-docker-compose/dx.properties
                                sed -i -r 's|^DX_DOCKER_IMAGE_DATABASE_CONNECTION_POOL_DIGITAL_ASSET_MANAGER=.*|DX_DOCKER_IMAGE_DATABASE_CONNECTION_POOL_DIGITAL_ASSET_MANAGER=${commonConfig.COMMON_IMAGE_REPOSITORY}.artifactory.cwp.pnp-hcl.com/portal/persistence/pgpool:${tags.PERSISTENCE_CONNECTION_POOL_IMAGE_TAG}|' ${workspace}/dx-docker-compose/dx.properties
                                sed -i -r 's|^DX_DOCKER_IMAGE_IMAGE_PROCESSOR=.*|DX_DOCKER_IMAGE_IMAGE_PROCESSOR=${commonConfig.COMMON_IMAGE_REPOSITORY}.artifactory.cwp.pnp-hcl.com/portal/image-processor:${tags.IMGPROC_IMAGE_TAG}|' ${workspace}/dx-docker-compose/dx.properties
                                sed -i -r 's|^DX_DOCKER_IMAGE_RING_API=.*|DX_DOCKER_IMAGE_RING_API=${commonConfig.COMMON_IMAGE_REPOSITORY}.artifactory.cwp.pnp-hcl.com/portal/api/ringapi:${tags.RINGAPI_IMAGE_TAG}|' ${workspace}/dx-docker-compose/dx.properties
                                sed -i -r 's|^DX_DOCKER_IMAGE_HAPROXY=.*|DX_DOCKER_IMAGE_HAPROXY=${commonConfig.COMMON_IMAGE_REPOSITORY}.artifactory.cwp.pnp-hcl.com/dx-build-output/common/haproxy:${tags.HAPROXY_IMAGE_TAG}|' ${workspace}/dx-docker-compose/dx.properties
                                sed -i -r 's|^DX_DOCKER_IMAGE_DATABASE_NODE_DIGITAL_ASSET_MANAGER=.*|DX_DOCKER_IMAGE_DATABASE_NODE_DIGITAL_ASSET_MANAGER=${commonConfig.COMMON_IMAGE_REPOSITORY}.artifactory.cwp.pnp-hcl.com/portal/persistence/postgresrepmgr:${tags.PERSISTENCE_NODE_IMAGE_TAG}|' ${workspace}/dx-docker-compose/dx.properties
                                sed -i -r 's|^DX_DOCKER_IMAGE_PREREQS_CHECKER=.*|DX_DOCKER_IMAGE_PREREQS_CHECKER=${commonConfig.COMMON_IMAGE_REPOSITORY}.artifactory.cwp.pnp-hcl.com/dx-build-output/common/prereqs-checker:${tags.PREREQS_CHECKER_IMAGE_TAG}|' ${workspace}/dx-docker-compose/dx.properties

                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sudo mkdir -p /opt/dx-docker-compose && sudo mkdir -p /opt/dx-docker-compose/volumes/core/wp_profile && sudo mkdir -p /opt/dx-docker-compose/volumes/dam/db && sudo chown centos: /opt/dx-docker-compose'
                                
                                scp -r -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/dx-docker-compose centos@${terraformVarsEC2.instance_private_ip}:/opt
                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/dx-docker-compose && sudo chmod -R 777 ./ && source ./set.sh && docker-compose up -d && source ./installApps.sh'

                            """
                        }
                    }
                }
            } 
        }

        stage('Verify all services are up and running') {
            steps {
                script {
                    final String ringApi = sh(script: "curl -o /dev/null -s -w %{http_code} http://${terraformVarsEC2.instance_private_ip}/dx/api/core/v1/explorer/index.html", returnStdout: true).trim()
                    echo "Ring Api response code: ${ringApi}"
                    if ("${ringApi}" != "200") {
                        error("Failed to verify the RingApi with response code ${ringApi}.")
                    }
                     final String dam = sh(script: "curl -o /dev/null -s -w %{http_code} http://${terraformVarsEC2.instance_private_ip}/dx/api/dam/v1/explorer/index.html", returnStdout: true).trim()
                    echo "DAM response code: ${dam}"
                    if ("${dam}" != "200") {
                        error("Failed to verify the DAM with response code ${dam}.")
                    }
                     final String imageProcessor = sh(script: "curl -o /dev/null -s -w %{http_code} http://${terraformVarsEC2.instance_private_ip}/dx/api/image-processor/v1/explorer/index.html", returnStdout: true).trim()
                    echo "Image processor Response code: ${imageProcessor}"
                    if ("${imageProcessor}" != "302") {
                        error("Failed to verify the imageProcessor with response code ${imageProcessor}.")
                    }
                    final String content = sh(script: "curl -o /dev/null -s -w %{http_code} http://${terraformVarsEC2.instance_private_ip}/dx/ui/content/", returnStdout: true).trim()
                    echo "Response code: ${content}"
                    if ("${content}" != "200") {
                        error("Failed to verify the content with response code ${content}.")
                    }
                    final String portal = sh(script: "curl -o /dev/null -s -w %{http_code} http://${terraformVarsEC2.instance_private_ip}/wps/portal", returnStdout: true).trim()
                    echo "Portal Response code: ${portal}"
                    if ("${portal}" != "302") {
                        error("Failed to verify the portal with response code ${portal}.")
                    }
                }
            }
        }

    }

    post {
        cleanup {
            script {
            if (params.DESTROY_DEPLOYMENT=="true") {
              dxTerraformDestroyEc2Instance(terraformVarsEC2)
            }
             //Cleanup workspace
            dxWorkspaceDirectoriesCleanup()
            }
           
        }
    }
}
