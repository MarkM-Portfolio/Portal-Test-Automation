/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2021, 2022. All Rights Reserved. *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

// Import the shared library with the name configured in Jenkins
@Library("dx-shared-library") _

/* Module and Config variables */
def commonConfig
def kubeModule

/* Common paths - must be here always */
moduleDirectory = "./kube/lifecycle/modules"
scriptDirectory = "./kube/lifecycle/scripts"
configDirectory = "./kube/lifecycle/config"

pipeline {
    agent {
        label 'build_infra'
    }

    stages {
        /*
         * Load modules and configuration from the different flavours using "load"
         * Use one module and load the fitting flavour
         */
        stage("Load modules and configuration") {
            steps {
                script {
                    // Workaround for using shared library with the nested Groovy files
                    env.THIS_BUILD_OWNER = dxJenkinsGetJobOwner()
                    commonConfig = load "${configDirectory}/common.gvy"
                    commonModule = load "${moduleDirectory}/common.gvy"
                    kubeModule = load "${moduleDirectory}/${commonConfig.COMMON_KUBE_FLAVOUR}.gvy"
                }
                // Install Helm in the current workspace and add it to the PATH variable
                dxHelmWorkspaceInstall()
                // Install kubectl in the current workspace and add it to the PATH variable
                dxKubectlWorkspaceInstall()
            }
        }

        stage('Push images to registry') {
            when { expression { env.PUSH_IMAGE_TO_REGISTRY == "true"} }
            steps {
                script {
                    echo "PUSH_IMAGE_TO_REGISTRY: ${env.PUSH_IMAGE_TO_REGISTRY}"
                    buildParams = commonModule.createJobParams(env.PUSH_IMAGE_TO_REGISTRY_JOB_PARAMS)
                    if (env.IMAGE_REPOSITORY == "openshift" || env.IMAGE_REPOSITORY == "aws") {
                        buildParams.add(
                        [$class: 'StringParameterValue',
                         name: 'TARGET_REGISTRY_ENVIRONMENT',
                         value: "ECR/OCR"])
                        env.TARGET_REGISTRY_ENVIRONMENT = "ECR/OCR"
                    } else if (env.IMAGE_REPOSITORY == "google") {
                        buildParams.add(
                        [$class: 'StringParameterValue',
                         name: 'TARGET_REGISTRY_ENVIRONMENT',
                         value: "GCR"])
                    } else if (env.IMAGE_REPOSITORY == "azure") {
                        buildParams.add(
                        [$class: 'StringParameterValue',
                         name: 'TARGET_REGISTRY_ENVIRONMENT',
                         value: "ACR"])
                    }
                    if (env.CORE_IMAGE_FILTER.contains('master')) {
                        buildParams.add(
                        [$class: 'StringParameterValue',
                         name: 'SOURCE_IMAGE_STAGE',
                         value: "master"])
                    } else if (env.CORE_IMAGE_FILTER.contains('release')) {
                        buildParams.add(
                        [$class: 'StringParameterValue',
                         name: 'SOURCE_IMAGE_STAGE',
                         value: "release"])
                    } else {
                        buildParams.add(
                        [$class: 'StringParameterValue',
                         name: 'SOURCE_IMAGE_STAGE',
                         value: "develop"])
                    }
                    build(job: commonModule.PUSH_IMAGE_TO_REGISTRY_JOB, 
                          parameters: buildParams, 
                          propagate: true, 
                          wait: true)
                }
            }
        }

        stage('Update on-premise Environment') {
            when { expression { env.UPDATE_ONPREMISE == "true"} }
            steps {
                script {
                    buildParams = commonModule.createJobParams(env.UPDATE_ON_PREMISE_ENV_JOB_PARAMS)
                    build(job: env.UPDATE_ON_PREMISE_ENV_JOB, 
                          parameters: buildParams,
                          propagate: true, 
                          wait: true)
                }
            }
        }

        /*
         * Update the environment of the selected flavour
         */
        stage("Update Environment") {
            steps {
                script {
                    if (commonConfig.COMMON_HYBRID.toBoolean()) {
                        kubeModule.updateHybridEnvironment()
                    } else {
                        kubeModule.updateEnvironment()
                    }
                }
            }
        }

        /*
         * Enable open LDAP users set up
         */
        stage('OpenLDAP users set up') {
            when { expression { commonConfig.COMMON_ENABLE_OPENLDAP_SET_UP == "true"} }
            steps {
                script {
                    host_name = "${env.INSTANCE_NAME}${env.DOMAIN_SUFFIX}"
                    commonModule.openLDAPUserSetUp(host_name)
                }
            }
        }

        /*
        * Run config engine task to enable OIDC configuration
        */
        stage('Enable OIDC ConfigEngine Task') {
            when { expression { commonConfig.COMMON_ENABLE_OIDC_CONFIGURATION == "true" && commonConfig.COMMON_DEPLOY_DX == "true" } }
            steps {
                script {                    
                    commonModule.configureOIDC()
                }
            }
        }

        stage("Run Health Checks") {
            steps {
                script {
                    kubeConfig = load "${configDirectory}/${commonConfig.COMMON_KUBE_FLAVOUR}.gvy"
                    commonModule.runHealthCheck(kubeConfig)  
                }
            }
        }
    }

    post {
        cleanup {
            dxWorkspaceDirectoriesCleanup()
        }
    }
}