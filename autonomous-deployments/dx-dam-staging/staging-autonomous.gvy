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
      string(name: 'NAMESPACE', defaultValue: 'staging',description: 'name space')
      string(name: 'DEPLOYMENT_LEVEL', defaultValue: 'develop',description: 'Deploying develop images')
      string(name: 'KUBE_FLAVOUR', defaultValue: 'native', description: 'Deploying a native kube environment.')
      string(name: 'KUBE_UNDEPLOY_JOB', defaultValue: 'CI/kube-deploy/native-kube-remove', description: 'Job which undeploys the environment',  trim: false)
      string(name: 'KUBE_DEPLOY_JOB', defaultValue: 'CI/kube-deploy/native-kube-next-deploy', description: 'Job which deploys the environment',  trim: false)
      string(name: 'DEPLOYMENT_METHOD', defaultValue: 'helm', description: 'Deployment method')
      string(name: 'CONTEXT_ROOT_PATH', defaultValue: 'wps', description: 'Context root')
      string(name: 'DX_CORE_HOME_PATH', defaultValue: 'portal', description: 'Home path')
      string(name: 'PERSONALIZED_DX_CORE_PATH', defaultValue: 'myportal', description: 'Personalized path')
      string(name: 'JMETER_BRANCH', defaultValue: 'develop', description: 'Jmeter script branch')
      string(name: 'STAGING_AUTOMATION_DEPLOY_JOB', defaultValue: 'CI/DAM-Staging/DAM_Staging', description: 'Job which deploys the staging automation tests',  trim: false)
      string(name: 'DOMAIN_SUFFIX', defaultValue: '.team-q-dev.com', description: 'Kube flavour domain suffix')
      string(name: 'CLUSTER_NAME', defaultValue: '', description: 'Cluster name where the deployment should be deployed to')
      string(name: 'CLUSTER_REGION', defaultValue: '', description: 'Region of the cluster')
      string(name: 'STAGING_ACCEPTANCE_TEST_JOB', defaultValue: 'CI/DAM-Staging/staging_acceptance_tests', description: 'Job which runs acceptance tests for staging',  trim: false)
      string(name: 'EXP_API', defaultValue: 'https://halo-halo-latest.team-q-dev.com:443/dx/api/core/v1', description: 'Required for CC and DAM tests. Examples: https://halo-halo-latest.team-q-dev.com:443/dx/api/dam/v1 -- default context')
      string(name: 'DAM_API', defaultValue: 'https://halo-halo-latest.team-q-dev.com:443/dx/api/dam/v1', description: 'Required for CC and DAM tests. Examples: https://halo-halo-latest.team-q-dev.com:443/dx/api/dam/v1 -- default context')
      string(name: 'TARGET_BRANCH', defaultValue: 'develop', description: 'Target branch')
      booleanParam(name: 'SSL_ENABLED', defaultValue: true, description: 'Required for testing environments with https/self-signed certificates like native.kube.')
    }

    stages {
        stage("Load modules and configuration") {
            steps {
                script {
                    commonModule = load "./autonomous-deployments/modules/common.gvy"
                    env.NAMESPACE = "native-kube-dev-${DEPLOYMENT_METHOD}-${env.NAMESPACE}-fresh"
                }
            }
        }

        stage('Deploying and running staging automation tests in k8 environment') {
            steps {
                script {
                    buildparams = commonModule.createStagingParams()
                    build(job: "${params.STAGING_AUTOMATION_DEPLOY_JOB}", 
                          parameters: buildParams, 
                          propagate: true,
                          wait: true)
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
                office365ConnectorSend message: "Build Success ${env.JOB_NAME} commited by @${user} [View on Jenkins] https://${env.NAMESPACE}${params.DOMAIN_SUFFIX}/${env.CONTEXT_ROOT_PATH}/${env.DX_CORE_HOME_PATH} ", status: "Build Success", webhookUrl: "${env.MS_TEAMS_URL}"
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