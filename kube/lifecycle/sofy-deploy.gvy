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

    parameters {
        string(name: 'SOLUTION_NAME', defaultValue: 'sol-test', description: '',  trim: false)
        string(name: 'NAMESPACE', defaultValue: '', description: '',  trim: false)
        string(name: 'SOLUTION_DESCRIPTION', defaultValue: '', description: '',  trim: false)
        string(name: 'CHART_NAME', defaultValue: 'dx', description: 'Helm chart name default value is: dx')
        string(name: 'CLUSTER_NAME', defaultValue: 'dx-cluster-autonomous', description: '',  trim: false)
        string(name: 'CONTEXT_ROOT_PATH', defaultValue: 'wps', description: '',  trim: false)
        string(name: 'DX_CORE_HOME_PATH', defaultValue: 'portal', description: '',  trim: false)
        choice(name: 'IMAGE_REPO', choices: ['gcr', 'harbor'], description: 'Images should pulled from')
        choice(name: 'RELEASE_TYPE', choices: ['ga', 'preview'], description: 'Helm charts release type for harbor')
        booleanParam(name: 'SKIP_ACCEPTANCE_TESTS', defaultValue: 'false', description: 'Run acceptance tests')
    }

    stages {
        stage("Prepare Settings") {
            steps {
                script {
                    env.TARGET_IMAGE_REPOSITORY_CREDENTIALS = "sofy_gcr_login"
                    if (!env.DX_VERSION) {
                        error("DX version should not be empty")
                    }
                    if (!env.SOLUTION_NAME || env.SOLUTION_NAME.length() > 9) {
                        error("Solution name should not be empty or more than 9 characters")
                    }
                    if (!env.NAMESPACE) {
                        error("NAMESPACE should not be empty")
                    }
                    if (!env.AUTO_DELETE) {
                        env.AUTO_DELETE = 'false'
                    }
                }
            }
        }

        stage("Load modules and configuration") {
            steps {
                script {
                    // Workaround for using shared library with the nested Groovy files
                    env.THIS_BUILD_OWNER = dxJenkinsGetJobOwner()
                    commonConfig = load "${configDirectory}/common.gvy"
                    commonModule = load "${moduleDirectory}/common.gvy"
                    kubeModule = load "${moduleDirectory}/sofy.gvy"
                }
                // Install Helm in the current workspace and add it to the PATH variable
                dxHelmWorkspaceInstall()
                // Install kubectl in the current workspace and add it to the PATH variable
                dxKubectlWorkspaceInstall()
            }
        }

        stage("Create Sofy solution") {
            steps {
                withCredentials([usernamePassword(credentialsId: 'sofy_dx-dev_credentials', passwordVariable: 'DOCKER_SOFY_PASSWORD', usernameVariable: 'DOCKER_SOFY_USERNAME')]) {
                    script {
                        jenkinsIP = sh (script: "curl -s https://api.ipify.org/", returnStdout: true).trim()
                        TOKEN = sh (script: "curl --location --request GET 'https://${DOCKER_SOFY_USERNAME}:${DOCKER_SOFY_PASSWORD}@sofy-auth.products.pnpsofy.com/login'", returnStdout: true).trim()
                        dir("${workspace}/dx-sofy-helm/automation/deploy-sofy-console/") {
                        CURL_RESPONSE = sh (script: "curl \
                                                        -X POST -H 'Content-Type: application/json' \
                                                        -H 'Accept: application/json, text/plain, */*' \
                                                        -H 'Authorization: Bearer ${TOKEN}' \
                                                        -d '{ \"description\": \"${SOLUTION_DESCRIPTION}\", \"name\": \"sobud${SOLUTION_NAME}\", \"versions\": [ { \"apiGateway\": { \"accessControlEnabled\": true, \"serviceRoutes\": [ { \"disableAuthorization\": false, \"route\": \"${CHART_NAME}\", \"service\": \"${CHART_NAME}\" } ] }, \"dependencies\": [ { \"ingressMapping\": true, \"name\": \"${CHART_NAME}\", \"repository\": { \"url\": \"https://cm.products.pnpsofy.com\" }, \"version\": \"${DX_VERSION}\" } ], \"includeHomeGUI\": true, \"monitoring\": { \"dashboard\": true }, \"number\": \"1.0.0\" }]}' \
                                                        https://sol.products.pnpsofy.com/solutions", returnStdout: true).trim()
                        }
                        SOLUTION_ID = CURL_RESPONSE.split(",")[0].split(":")[1]
                        sh(script: """
                            echo "JenkinsIP: ${jenkinsIP}"
                            echo "Curl Response :${SOLUTION_ID}"
                            echo "Waiting for solution to get created"
                            sleep 30
                        """)
                    }
                }
            }
        }

        stage("Deploy solution onto GKE") {
            steps {
                script {
                    kubeModule.createEnvironmentSofy(SOLUTION_ID, NAMESPACE, TOKEN, IMAGE_REPO, RELEASE_TYPE)
                }
            }
        }

        stage("Deleting the complete deployment") {
            when { expression { env.AUTO_DELETE.toBoolean() } }
            steps {
                script {
                    kubeModule.destroySofyEnvironment(NAMESPACE)
                }
            }
        }
    }

    post {
        aborted {
            script {
                kubeModule.destroySofyEnvironment(NAMESPACE)
            }
        }
        failure {
            script {
                kubeModule.destroySofyEnvironment(NAMESPACE)
            }
        }
        cleanup {
            dxWorkspaceDirectoriesCleanup()
        }
    }
}