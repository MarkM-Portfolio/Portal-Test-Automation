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
      string(name: 'DEPLOYMENT_LEVEL', defaultValue: 'develop',description: 'Deploying develop images')
      string(name: 'DEPLOYMENT_METHOD', defaultValue: 'helm', description: 'Type of Kube deployment, can be either dxctl or helm.')
      string(name: 'KUBE_FLAVOUR', defaultValue: 'native',description: 'Deploying a native kube hybrid environment.')
      string(name: 'HYBRID_UNDEPLOY_JOB', defaultValue: 'hybrid/hybrid-native-destroy', description: 'Job which undeploys the native kube hybrid environment',  trim: false)
      string(name: 'HYBRID_DEPLOY_JOB', defaultValue: 'hybrid/hybrid-native-deploy', description: 'Job which deploys the native kube hybrid environment',  trim: false)
      string(name: 'AWS_ZONE_ID', defaultValue: 'Z3OEC7SLEHQ2P3', description: 'Zone Id of the Route 53 record')
      string(name: 'DOMAIN_SUFFIX', defaultValue: '.team-q-dev.com', description: 'Domain Suffix of the Route 53 record')
      string(name: 'KUBE_DOMAIN_SUFFIX', defaultValue: '.team-q-dev.com', description: 'Kube deployment domain suffix')
      string(name: 'AWS_REGION', defaultValue: 'us-east-1', description: 'Region in AWS.',  trim: false)
      string(name: 'AWS_SUBNET', defaultValue: 'subnet-09f521dfcea461588', description: 'Select the AWS subnet.')
      string(name: 'VPC_SECURITY_GROUPS', defaultValue: '["sg-0b1faa6b777393d69","sg-01cd0516fd7094663","sg-0021c8bdd484deaea"]',description: 'Provide the security groups.')
      string(name: 'CONTEXT_ROOT_PATH', defaultValue: 'wps', description: 'Context root')
      string(name: 'DX_CORE_HOME_PATH', defaultValue: 'portal', description: 'Home path')
      string(name: 'PERSONALIZED_DX_CORE_PATH', defaultValue: 'myportal', description: 'Personalized path')
    }

    stages {
        stage("Load modules and configuration") {
            steps {
                script {
                    commonModule = load "./autonomous-deployments/modules/common.gvy"
                    commonConfig = load "./autonomous-deployments/config/common.gvy"
                    env.NAMESPACE = "native-stdhyb-dev"
                    if (!env.MS_TEAMS_URL){
                        env.MS_TEAMS_URL = 'https://outlook.office.com/webhook/8a6712b0-0629-4fbb-9e35-641ae6c7f577@189de737-c93a-4f5a-8b68-6f4ca9941912/JenkinsCI/a1fa77efc3b545a0aba82ab2bf0ddd4f/e012756a-5de7-490a-9a92-8b5b2c116578'
                    }
                }
            }
        }

        stage('Undeploying the native kube hybrid environment') {
            steps {
                script {
                    buildParams = commonModule.createHybridKubeUnDeployParams(env.NAMESPACE, params.KUBE_FLAVOUR, "false", "", "", "")
                    build(job: "${params.HYBRID_UNDEPLOY_JOB}", 
                          parameters: buildParams, 
                          propagate: true,
                          wait: true)
                }
            }
        }

        stage('Deploying the native kube hybrid environment') {
            steps {
                script {
                    buildParams = commonModule.createHybridFreshDeployParams(env.NAMESPACE, params.KUBE_FLAVOUR, params.DEPLOYMENT_LEVEL, DOMAIN_SUFFIX, 0, "false", "false", 0, "", "", "", params.CONTEXT_ROOT_PATH, params.DX_CORE_HOME_PATH, params.PERSONALIZED_DX_CORE_PATH)
                    build(job: "${params.HYBRID_DEPLOY_JOB}", 
                          parameters: buildParams, 
                          propagate: true,
                          wait: true)
                }
            }
        }

        stage("Retrieve on-premise recordset details for running acceptance tests") {
            steps {
                withAWS(credentials: 'aws_credentials', region: "${params.AWS_REGION}") { 
                    script {
                        def route53RecordSet = sh(script: "aws route53 list-resource-record-sets --hosted-zone-id ${params.AWS_ZONE_ID} --query 'ResourceRecordSets[?Name == `${NAMESPACE}-onprem${DOMAIN_SUFFIX}.`]'", returnStdout: true).trim()
                        echo "route53RecordSet= ${route53RecordSet}"
                        route53RecordSet = route53RecordSet.replace("\n","")
                        def jsonRoute53RecordSet = readJSON text: route53RecordSet
                        env.ONPREMISE_HOST_IP = jsonRoute53RecordSet.ResourceRecords[0].Value[0]
                        echo "ONPREMISE HOST IP= ${env.ONPREMISE_HOST_IP}"
                    }                    
                }
            }
        }

        stage('Run acceptance test on native kube environment') {
            steps {
                script {
                    buildParams = commonModule.createAcceptanceTestJobParams(env.NAMESPACE, params.KUBE_FLAVOUR, params.DEPLOYMENT_LEVEL, "hybrid", env.ONPREMISE_HOST_IP, params.CONTEXT_ROOT_PATH, params.DX_CORE_HOME_PATH)

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
                office365ConnectorSend message: "Build Success ${env.JOB_NAME} commited by @${user} [View on Jenkins] https://${env.NAMESPACE}${DOMAIN_SUFFIX}/wps/portal ", status: "Build Success. Acceptance test result is quite unstable", webhookUrl: "${env.MS_TEAMS_URL}"
            }
        }

        success {
            script {
                office365ConnectorSend message: "Build Success ${env.JOB_NAME} commited by @${user} [View on Jenkins] https://${env.NAMESPACE}${DOMAIN_SUFFIX}/wps/portal ", status: "Build Success", webhookUrl: "${env.MS_TEAMS_URL}"
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
