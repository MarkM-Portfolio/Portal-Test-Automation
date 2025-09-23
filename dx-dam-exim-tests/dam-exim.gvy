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

pipeline {
    agent {
        label 'build_infra'
    }
    parameters {
        string(name: 'STAGING_ACCEPTANCE_TEST_JOB', defaultValue: 'CI/DAM-Staging/staging_acceptance_tests', description: 'Job which runs acceptance tests for staging',  trim: false)
        string(name: 'TARGET_BRANCH', defaultValue: 'develop', description: 'Target branch')
        booleanParam(name: 'SSL_ENABLED', defaultValue: true, description: 'Required for testing environments with https/self-signed certificates like native.kube.')
        string(name: 'EXP_API', defaultValue: 'https://halo-halo-latest.team-q-dev.com:443/dx/api/core/v1', description: 'Required for CC and DAM tests. Examples: https://halo-halo-latest.team-q-dev.com:443/dx/api/dam/v1 -- default context')
        string(name: 'DAM_API', defaultValue: 'https://halo-halo-latest.team-q-dev.com:443/dx/api/dam/v1', description: 'Required for CC and DAM tests. Examples: https://halo-halo-latest.team-q-dev.com:443/dx/api/dam/v1 -- default context')
    }
    stages {
        stage('Load modules and configuration') {
            steps {
                script {
                    commonModule = load './autonomous-deployments/modules/common.gvy'
                }
            }
        }

        stage('Running dam-exim acceptance tests in k8 environment') {
            steps {
                script {
                    buildparams = commonModule.createStagingParams()
                    build(job: "${params.STAGING_ACCEPTANCE_TEST_JOB}",
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
                office365ConnectorSend message: "Aborted ${params.STAGING_ACCEPTANCE_TEST_JOB} commited by @${user} [View on Jenkins] ", status: 'Aborted', webhookUrl: "${env.MS_TEAMS_URL}"
            }
        }

        failure {
            script {
                office365ConnectorSend message: "Build Failed ${params.STAGING_ACCEPTANCE_TEST_JOB} commited by @${user} [View on Jenkins] ", status: 'Build Failed', webhookUrl: "${env.MS_TEAMS_URL}"
            }
        }

        success {
            script {
                office365ConnectorSend message: "Build Success ${params.STAGING_ACCEPTANCE_TEST_JOB} commited by @${user} [View on Jenkins] ", status: 'Build Success', webhookUrl: "${env.MS_TEAMS_URL}"
            }
        }
    }
}
