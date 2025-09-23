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
configDirectory = "./kube/lifecycle/config"

pipeline {
    agent {
        label 'build_infra'
    }

    parameters {
      string(name: 'SOBUD_IMAGE_VERSION', defaultValue: '0.1.4',description: 'Version of sobud docker image to be pulled from Sofy GCR')
      string(name: 'DEPLOY_WAIT', defaultValue: '120', description: 'Deploy wait time')
      string(name: 'PODS_WAIT', defaultValue: '60', description: 'Pods wait time')
      string(name: 'WAIT', defaultValue: '120', description: 'Wait time')
      string(name: 'AUTO_DELETE', defaultValue: 'false', description: 'Delete Sandbox automatically after deployment validation')
      choice(name: 'CHART_NAME', choices: ['dx', 'hcl-dx-deployment'], description: 'Helm chart name for operator and helm based. Default value is for operator based: dx')
      string(name: 'SOLUTION_NAME', defaultValue: 'test-sol', description: 'Name of DX solution')
      string(name: 'CONTEXT_ROOT_PATH', defaultValue: 'wps', description: '',  trim: false)
      string(name: 'DX_CORE_HOME_PATH', defaultValue: 'portal', description: '',  trim: false)
      booleanParam(name: 'SKIP_ACCEPTANCE_TESTS', defaultValue: 'false', description: 'Run acceptance tests')
    }

    stages {
        stage("Load modules and configuration") {
            steps {
                script {
                    // Workaround for using shared library with the nested Groovy files
                    env.THIS_BUILD_OWNER = dxJenkinsGetJobOwner()
                    commonConfig = load "${configDirectory}/common.gvy"
                    sofyConfig = load "${configDirectory}/sofy.gvy"
                    commonModule = load "${moduleDirectory}/common.gvy"
                }
                // Install Helm in the current workspace and add it to the PATH variable
                dxHelmWorkspaceInstall()
                // Install kubectl in the current workspace and add it to the PATH variable
                dxKubectlWorkspaceInstall()
            }
        }

        stage("Prepare sofy connection and other configuration settings") {
            steps {
                script {
                    // Sofy GCR image repo credentials
                    if (!env.TARGET_IMAGE_REPOSITORY_CREDENTIALS){
                        env.TARGET_IMAGE_REPOSITORY_CREDENTIALS = "sofy_gcr_login"
                    }

                    // Sofy Docker image credentials
                    if (!env.SOFY_DOCKER_IMAGE_CREDENTIALS){
                        env.SOFY_DOCKER_IMAGE_CREDENTIALS = 'sofy_dx-dev_credentials'
                    }

                    if (!env.SOFY_SOBUD_IMAGE){
                        env.SOFY_SOBUD_IMAGE = "gcr.io/blackjack-209019/test/sobud:${params.SOBUD_IMAGE_VERSION}"
                    }

                    if (!params.SOLUTION_NAME || params.SOLUTION_NAME.length() > 9) {
                        error("Solution name should not be empty or more than 9 characters")
                    }
                    if (!params.AUTO_DELETE) {
                        params.AUTO_DELETE = 'false'
                    }
                    /* extract directory name where git checkouts to */
                    env.TARGET_GITHUB_FORK_REPOSITORY_NAME = (sofyConfig.COMMON_TARGET_GITHUB_FORK_REPOSITORY =~ /(?<=\/)(.*?)(?=.git)/)[0][1]
                }
            }
        }

        /*
         * We checkout the remote fork repository in a local directory using the configured credentials.
         */
        stage("Checkout fork repository") {
            steps {
                sshagent(credentials: ["${sofyConfig.COMMON_TARGET_GITHUB_CREDENTIALS}"]) {
                    sh """
                        git clone ${sofyConfig.COMMON_TARGET_GITHUB_FORK_REPOSITORY}
                        cd ${env.TARGET_GITHUB_FORK_REPOSITORY_NAME}
                        git remote add upstream ${sofyConfig.COMMON_TARGET_GITHUB_UPSTREAM_REPOSITORY}
                        git remote -v
                    """
                }
            }
        }

        /*
         * We update our fork with the latest changes from the upstream, to make sure we are up-to-date
         */
        stage("Update fork to upstream") {
            steps {
                sshagent(credentials: ["${sofyConfig.COMMON_TARGET_GITHUB_CREDENTIALS}"]) {
                    sh """
                        cd ${WORKSPACE}/${env.TARGET_GITHUB_FORK_REPOSITORY_NAME}
                        git fetch upstream
                        git checkout master
                        git merge upstream/master
                        git push
                    """
                }           
            }
        }

        stage("Fetch DX Chart Version from fork repository") {
            steps {
                script {
                    /* Get properties of target chart.yaml */
                    if (!env.DX_VERSION) {
                        targetChartProps =  readYaml file: "${workspace}/${env.TARGET_GITHUB_FORK_REPOSITORY_NAME}/${sofyConfig.COMMON_TARGET_GITHUB_BASE_DIRECTORY}/${sofyConfig.COMMON_HELM_CHART_DIRECTORY}/Chart.yaml"
                        env.DX_VERSION = "${targetChartProps.version}"
                        echo "DX Helm Chart Version in Sofy: ${env.DX_VERSION}"
                    }
                    
                }           
            }
        }

        /*
         * We login to the target image repository using the credentials stored in Jenkins
         */
        stage("Login to sofy and run sobud docker image") {
            steps {
                withCredentials([file(credentialsId: "${env.TARGET_IMAGE_REPOSITORY_CREDENTIALS}", variable: 'LOGIN_KEY')]) {
                    withCredentials([usernamePassword(credentialsId: "${env.SOFY_DOCKER_IMAGE_CREDENTIALS}", passwordVariable: 'DOCKER_SOFY_PASSWORD', usernameVariable: 'DOCKER_SOFY_USERNAME')]) {
                        script {
                            jenkinsIP = sh (script: "curl -s https://api.ipify.org/", returnStdout: true).trim()
                            sh(script: """
                                echo "JenkinsIP address :${jenkinsIP}"
                                docker login -u _json_key --password-stdin https://gcr.io < ${LOGIN_KEY}
                                docker pull ${env.SOFY_SOBUD_IMAGE}
                            """)
                            SOBUD_RESPONSE = sh(returnStdout: true, script: "docker run --env SERVICES=${params.CHART_NAME}:${env.DX_VERSION} --env SOLUTION_NAME=${params.SOLUTION_NAME} --env USER=${DOCKER_SOFY_USERNAME} --env PASSWORD='${DOCKER_SOFY_PASSWORD}' --env DEPLOY_WAIT=${params.DEPLOY_WAIT} --env PODS_WAIT=${params.PODS_WAIT} --env WAIT=${params.WAIT} --env AUTO_DELETE=${params.AUTO_DELETE} ${env.SOFY_SOBUD_IMAGE}").trim()
                            println("SOBUD RESPONSE: - ${SOBUD_RESPONSE}.")
                            def reg1 = ~/^Sobud Response: /
                            def reg2 = ~/PASS/
                            String SOBUD_RESPONSE_STR1 = SOBUD_RESPONSE - reg1
                            String SOBUD_RESPONSE_STR2 = SOBUD_RESPONSE_STR1 - reg2
                            def sobudJsonInformation = readJSON text: SOBUD_RESPONSE_STR2
                            def domain_url= "dx.${sobudJsonInformation['SandboxResponse']['domain']}"
                            println("DOMAIIN URL:- ${domain_url}")
                            if(!("${commonConfig.COMMON_SKIP_ACCEPTANCE_TESTS}".toBoolean())){
                                commonModule.isAcceptanceTestsSuccess("develop", domain_url, true)
                            }
                        }
                    }
                }
            }
        }
        
    }

    /*
     * Cleanup all temporary files and docker images
     */
    post {
        cleanup {
            dxWorkspaceDirectoriesCleanup()
        }
    }
}
