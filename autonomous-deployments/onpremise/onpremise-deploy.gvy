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
      string(name: 'DEPLOYMENT_LEVEL', defaultValue: 'develop',description: 'Deploying rivendell master image')
      string(name: 'ONPREMISE_UNDEPLOY_JOB', defaultValue: 'hybrid/onpremise/dx-onpremise-remove', description: 'Job which undeploys the onpremise environment',  trim: false)
      string(name: 'ONPREMISE_DEPLOY_JOB', defaultValue: 'hybrid/onpremise/dx-onpremise-deployment', description: 'Job which deploys the onpremise environment',  trim: false)
    }

    stages {
        stage("Load modules and configuration") {
            steps {
                script {
                    commonModule = load "./autonomous-deployments/modules/common.gvy"
                    commonConfig = load "./autonomous-deployments/config/common.gvy"
                    env.NAMESPACE = "dx-onpremise-fresh"
                }
            }
        }

        stage('Undeploying the application from onpremise environment') {
            steps {
                script {
                    buildParams = []
                    if (!env.MS_TEAMS_URL){
                        env.MS_TEAMS_URL = 'https://outlook.office.com/webhook/8a6712b0-0629-4fbb-9e35-641ae6c7f577@189de737-c93a-4f5a-8b68-6f4ca9941912/JenkinsCI/a1fa77efc3b545a0aba82ab2bf0ddd4f/e012756a-5de7-490a-9a92-8b5b2c116578'
                    }
                    buildParams = commonModule.createOnpremiseUnDeployParams(env.NAMESPACE, commonConfig.COMMON_CLUSTERED_ENV)
                    imagesPushResult = build(job: "${params.ONPREMISE_UNDEPLOY_JOB}", 
                          parameters: buildParams, 
                          propagate: true,
                          wait: true)
                }
            }
        }

        stage('Deploying the application in onpremise environment') {
            steps {
                script {
                    buildParams = commonModule.createOnpremiseDeployParams(env.NAMESPACE, commonConfig.COMMON_CLUSTERED_ENV, commonConfig.COMMON_ADDITIONAL_NODE, params.DEPLOYMENT_LEVEL)
                    imagesPushResult = build(job: "${params.ONPREMISE_DEPLOY_JOB}", 
                          parameters: buildParams, 
                          propagate: true,
                          wait: true)
                }
            }
        }

        stage("Retrieve on-premise recordset details") {
            steps {
                withAWS(credentials: 'aws_credentials', region: 'us-east-1') { 
                    script {
                        def route53RecordSet = sh(script: "aws route53 list-resource-record-sets --hosted-zone-id Z3OEC7SLEHQ2P3 --query 'ResourceRecordSets[?Name == `${NAMESPACE}.team-q-dev.com.`]'", returnStdout: true).trim()
                        echo "route53RecordSet= ${route53RecordSet}"
                        route53RecordSet = route53RecordSet.replace("\n","")
                        def jsonRoute53RecordSet = readJSON text: route53RecordSet
                        env.ONPREMISE_HOST_IP = jsonRoute53RecordSet.ResourceRecords[0].Value[0]
                        echo "ONPREMISE HOST IP= ${env.ONPREMISE_HOST_IP}"
                    }                    
                }
            }
        }

        stage('Run acceptance test on DX Core') {
            steps {
                script {
                    buildParams = []
                    buildParams.add(
                        [$class: 'BooleanParameterValue',
                        name: 'TEST_DX_CORE',
                        value: true])
                    buildParams.add(
                        [$class: 'BooleanParameterValue',
                        name: 'SSL_ENABLED',
                        value: true])
                    buildParams.add(
                        [$class: 'StringParameterValue',
                        name: 'TARGET_BRANCH',
                        value: "develop"])
                    buildParams.add(
                        [$class: 'StringParameterValue',
                        name: 'HOST_IP_ADDRESS',
                        value: "${env.ONPREMISE_HOST_IP}"])
                    buildParams.add(
                        [$class: 'StringParameterValue',
                        name: 'HOSTNAME',
                        value: "${NAMESPACE}.team-q-dev.com"])
                    buildParams.add(
                                [$class: 'StringParameterValue',
                                name: 'PORTAL_HOST',
                                value: "https://${NAMESPACE}.team-q-dev.com/wps/portal"])

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

        unstable {
            script {
                office365ConnectorSend message: "Build Success ${env.JOB_NAME} commited by @${user} [View on Jenkins] https://${env.NAMESPACE}.apps.dx-cluster-dev.hcl-dx-dev.net/wps/portal ", status: "Build Success. Acceptance test result is quite unstable", webhookUrl: "${env.MS_TEAMS_URL}"
            }
        }

        success {
            script {
                office365ConnectorSend message: "Build Success ${env.JOB_NAME} commited by @${user} [View on Jenkins] https://${env.NAMESPACE}.apps.dx-cluster-dev.hcl-dx-dev.net/wps/portal ", status: "Build Success", webhookUrl: "${env.MS_TEAMS_URL}"
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
