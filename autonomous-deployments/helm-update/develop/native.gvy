/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2021. All Rights Reserved.       *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

pipeline {

    agent {
        label 'build_infra'
    }

    parameters {
      string(name: 'DEPLOYMENT_LEVEL', defaultValue: 'master', description: 'Deploying latest master image')
      string(name: 'UPDATE_DEPLOYMENT_LEVEL', defaultValue: 'develop', description: 'Updating to the latest develop images')
      string(name: 'DEPLOYMENT_METHOD', defaultValue: 'helm', description: 'Deployment method')
      string(name: 'KUBE_FLAVOUR', defaultValue: 'native',description: 'Deploying a native kube environment.')
      string(name: 'KUBE_UNDEPLOY_JOB', defaultValue: 'CI/kube-deploy/native-kube-remove', description: 'Job which removes the native kube environment',  trim: false)
      string(name: 'KUBE_DEPLOY_JOB', defaultValue: 'CI/kube-deploy/native-kube-next-deploy', description: 'Job which creates the native kube environment',  trim: false)
      string(name: 'KUBE_UPDATE_JOB', defaultValue: 'CI/kube-deploy/native-kube-next-update', description: 'Job which updates the native kube environment',  trim: false)
      string(name: 'UPDATE_RELEASE_LEVEL', defaultValue: '0', description: 'Update from n to n+UPDATE_RELEASE_LEVEL',  trim: false)
      string(name: 'CONTEXT_ROOT_PATH', defaultValue: 'wps', description: 'Context root')
      string(name: 'DX_CORE_HOME_PATH', defaultValue: 'portal', description: 'Home path')
      string(name: 'PERSONALIZED_DX_CORE_PATH', defaultValue: 'myportal', description: 'Personalized path')
      string(name: 'DOMAIN_SUFFIX', defaultValue: '.team-q-dev.com', description: 'Kube flavour domain suffix')
    }

    stages {
        stage("Load modules and configuration") {
            steps {
                script {
                    commonModule = load "./autonomous-deployments/modules/common.gvy"
                    commonConfig = load "./autonomous-deployments/config/common.gvy"
                    env.INSTANCE_NAME = "native-kube-${DEPLOYMENT_METHOD}-n${UPDATE_RELEASE_LEVEL}-dev"
                    if (!env.MS_TEAMS_URL){
                        env.MS_TEAMS_URL = 'https://outlook.office.com/webhook/8a6712b0-0629-4fbb-9e35-641ae6c7f577@189de737-c93a-4f5a-8b68-6f4ca9941912/JenkinsCI/a1fa77efc3b545a0aba82ab2bf0ddd4f/e012756a-5de7-490a-9a92-8b5b2c116578'
                    }
                }
            }
        }

        stage('Undeploying the application in k8 environment') {
            steps {
                script {
                    buildParams = []
                    buildParams.add(
                        [$class: 'StringParameterValue',
                            name: 'INSTANCE_NAME',
                            value: "${INSTANCE_NAME}"])
                    build(job: "${params.KUBE_UNDEPLOY_JOB}", 
                        parameters: buildParams, 
                        propagate: true,
                        wait: true)
                }
            }
        }

        stage('Deploying the application in k8 environment') {
            steps {
                script {
                    buildParams = commonModule.createKubeDeployParamsForMaster(env.INSTANCE_NAME, params.KUBE_FLAVOUR, "", "", "", params.CONTEXT_ROOT_PATH, params.DX_CORE_HOME_PATH, params.PERSONALIZED_DX_CORE_PATH, params.UPDATE_RELEASE_LEVEL, params.DEPLOYMENT_LEVEL, params.DEPLOYMENT_METHOD, params.DOMAIN_SUFFIX)
                    build(job: "${params.KUBE_DEPLOY_JOB}", 
                        parameters: buildParams, 
                        propagate: true,
                        wait: true)
                }
            }
        }

        stage('Updating the application in k8 environment') {
            steps {
                script {
                    buildParams = commonModule.createKubeParams(env.INSTANCE_NAME, params.KUBE_FLAVOUR, "", "", "", params.CONTEXT_ROOT_PATH, params.DX_CORE_HOME_PATH, params.PERSONALIZED_DX_CORE_PATH, params.UPDATE_DEPLOYMENT_LEVEL, params.DEPLOYMENT_METHOD, params.DOMAIN_SUFFIX)
                    build(job: "${params.KUBE_UPDATE_JOB}", 
                        parameters: buildParams, 
                        propagate: true,
                        wait: true)
                }
            }
        }

        stage('Acceptance Tests') {
            steps {
                script {
                    buildParams = commonModule.createAcceptanceTestJobParams(env.INSTANCE_NAME, params.KUBE_FLAVOUR, params.DEPLOYMENT_LEVEL, "kube", "", params.CONTEXT_ROOT_PATH, params.DX_CORE_HOME_PATH)

                    catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                        build(job: commonConfig.ACCEPTANCE_TEST_JOB, 
                          parameters: buildParams, 
                          propagate: true, 
                          wait: true)
                    }
                }

            }
        }
    }

    post {
        aborted {
            script {
                office365ConnectorSend message: "Aborted ${env.JOB_NAME} committed by @${user} [View on Jenkins] ", status: "Aborted", webhookUrl: "${env.MS_TEAMS_URL}"
            }
        }

        failure {
            script {
                office365ConnectorSend message: "Build Failed ${env.JOB_NAME} committed by @${user} [View on Jenkins] ", status: "Build Failed", webhookUrl: "${env.MS_TEAMS_URL}"
            }
        }

        success {
            script {
                office365ConnectorSend message: "Build Success ${env.JOB_NAME} committed by @${user} [View on Jenkins] https://${env.INSTANCE_NAME}.team-q-dev.com/wps/portal ", status: "Build Success", webhookUrl: "${env.MS_TEAMS_URL}"
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
