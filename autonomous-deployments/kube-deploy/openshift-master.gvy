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

import java.text.SimpleDateFormat

def TERRAFORM_DOWNLOAD="dx-build-prereqs/terraform/terraform_0.12.20_linux_amd64.zip"

pipeline {

    agent {
        label 'build_infra'
    }

    parameters {
      string(name: 'DEPLOYMENT_LEVEL', defaultValue: 'master',description: 'Deploying rivendell master images')
      string(name: 'KUBE_FLAVOUR', defaultValue: 'openshift',description: 'Deploying to the openshift environment.')
      string(name: 'KUBE_UNDEPLOY_JOB', defaultValue: 'kube/cloud-undeploy', description: 'Job which undeploys the image in openshift',  trim: false)
      string(name: 'KUBE_DEPLOY_JOB', defaultValue: 'kube/cloud-deploy', description: 'Job which deploys the image in openshift',  trim: false)
      string(name: 'DEPLOYMENT_METHOD', defaultValue: 'dxctl', description: 'Deployment method')
      string(name: 'CONTEXT_ROOT_PATH', defaultValue: 'wps', description: 'Context root')
      string(name: 'DX_CORE_HOME_PATH', defaultValue: 'portal', description: 'Home path')
      string(name: 'PERSONALIZED_DX_CORE_PATH', defaultValue: 'myportal', description: 'Personalized path')
      string(name: 'DOMAIN_SUFFIX', defaultValue: '.apps.dx-cluster-dev.hcl-dx-dev.net', description: 'Kube flavour domain suffix')
      string(name: 'NEXT_JOB_DELAY_HOURS', defaultValue: '15', description: 'Delay in hours to trigger undeploy job when job is not scheduled')
      string(name: 'NEXT_CLUSTER_JOB_DELAY_HOURS', defaultValue: '15', description: 'Delay in hours to trigger cluster destroy job when job is not scheduled')  
    }

    stages {
        stage("Load modules and configuration") {
            steps {
                script {
                    commonModule = load "./autonomous-deployments/modules/common.gvy"
                    commonConfig = load "./autonomous-deployments/config/common.gvy"
                    env.NAMESPACE = "os-mstr-${DEPLOYMENT_METHOD}-fresh"
                }
            }
        }

        stage('Undeploying the application in k8 environment') {
            steps {
                script {
                    buildParams = []
                    if (!env.MS_TEAMS_URL){
                        env.MS_TEAMS_URL = 'https://outlook.office.com/webhook/8a6712b0-0629-4fbb-9e35-641ae6c7f577@189de737-c93a-4f5a-8b68-6f4ca9941912/JenkinsCI/a1fa77efc3b545a0aba82ab2bf0ddd4f/e012756a-5de7-490a-9a92-8b5b2c116578'
                    }
                    buildParams = commonModule.createKubeUnDeployParams(env.NAMESPACE, params.KUBE_FLAVOUR, "", "", "")
                    imagesPushResult = build(job: "${params.KUBE_UNDEPLOY_JOB}", 
                          parameters: buildParams, 
                          propagate: true,
                          wait: true)
                }
            }
        }

        stage('Deploying the application in k8 environment') {
            steps {
                script {
                    buildParams = commonModule.createKubeParams(env.NAMESPACE, params.KUBE_FLAVOUR, "", "", "", params.CONTEXT_ROOT_PATH, params.DX_CORE_HOME_PATH, params.PERSONALIZED_DX_CORE_PATH, params.DEPLOYMENT_LEVEL, params.DEPLOYMENT_METHOD, params.DOMAIN_SUFFIX)
                    imagesPushResult = build(job: "${params.KUBE_DEPLOY_JOB}", 
                          parameters: buildParams, 
                          propagate: true,
                          wait: true)
                }
            }
        }

        stage('Run acceptance test for Openshift master deployment') {
            steps {
                script {
                    buildParams = commonModule.createAcceptanceTestJobParams(env.NAMESPACE, params.KUBE_FLAVOUR, params.DEPLOYMENT_LEVEL,"kube","", params.CONTEXT_ROOT_PATH, params.DX_CORE_HOME_PATH)

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
                office365ConnectorSend message: "Aborted ${env.JOB_NAME} commited by @${user} [View on Jenkins] ", status: "Aborted", webhookUrl: "${env.MS_TEAMS_URL}"
            }
        }

        failure {
            script {
                office365ConnectorSend message: "Build Failed ${env.JOB_NAME} commited by @${user} [View on Jenkins] ", status: "Build Failed", webhookUrl: "${env.MS_TEAMS_URL}"
            }
        }

        success {
            script {
                office365ConnectorSend message: "Build Success ${env.JOB_NAME} commited by @${user} [View on Jenkins] https://dx-deployment-service-${env.NAMESPACE}-${env.NAMESPACE}.apps.dx-cluster-dev.hcl-dx-dev.net/wps/portal", status: "Build Success", webhookUrl: "${env.MS_TEAMS_URL}"
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
