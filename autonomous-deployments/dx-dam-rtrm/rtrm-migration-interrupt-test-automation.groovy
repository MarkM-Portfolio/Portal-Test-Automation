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

def CLUSTER_REGION="us-east4"
def RESOURCE_GROP=""

pipeline {

    agent {
        label 'build_infra'
    }

    parameters {
      booleanParam(name: 'ENABLE_INTERRUPT_MIGRATION', defaultValue: true, description: 'Required for testing environments with migration.') 
      string(name: 'DEPLOYMENT_LEVEL', defaultValue: 'master',description: 'Deploying latest master image')
      string(name: 'UPDATE_DEPLOYMENT_LEVEL', defaultValue: 'develop',description: 'Updating to the latest develop images')
      string(name: 'KUBE_FLAVOUR', defaultValue: 'google',description: 'Deploying to the aws environment.')
      string(name: 'KUBE_UNDEPLOY_JOB', defaultValue: 'kube/cloud-undeploy', description: 'Job which undeploys the image in google gke',  trim: false)
      string(name: 'KUBE_DEPLOY_JOB', defaultValue: 'kube/cloud-deploy', description: 'Job which deploys the image in aws',  trim: false)
      string(name: 'KUBE_UPDATE_JOB', defaultValue: 'kube/cloud-update-deploy', description: 'Job which updates the image on google gke',  trim: false)
      string(name: 'UPDATE_RELEASE_LEVEL', defaultValue: '1', description: 'Update from n to n+UPDATE_RELEASE_LEVEL',  trim: false)
      choice(name: 'DEPLOYMENT_METHOD', choices: ['dxctl', 'helm'] ,  description: 'Select deployment method')
      string(name: 'CLUSTER_NAME', defaultValue: 'dx-cluster-05', description: 'Cluster name',  trim: false)
      string(name: 'CONTEXT_ROOT_PATH', defaultValue: 'wps', description: 'Context root')
      string(name: 'DX_CORE_HOME_PATH', defaultValue: 'portal', description: 'Home path')
      string(name: 'PERSONALIZED_DX_CORE_PATH', defaultValue: 'myportal', description: 'Personalized path')
      string(name: 'NEXT_JOB_DELAY_HOURS', defaultValue: '0', description: 'Delay in hours to trigger undeploy job')
    }

    stages {
        stage("Load modules and configuration for RTRM") {
            steps {
                script {
                    commonModule = load "./autonomous-deployments/modules/common.gvy"
                    commonConfig = load "./autonomous-deployments/config/common.gvy"
                    env.NAMESPACE = "rtrm-${DEPLOYMENT_METHOD}-n${UPDATE_RELEASE_LEVEL}-migration-interrupt-dev"
                }
            }
        }

        stage('Undeploying the application in k8 environment for RTRM') {
            steps {
                script {
                    buildParams = []
                    buildParams = commonModule.createKubeUnDeployParams(env.NAMESPACE, params.KUBE_FLAVOUR, params.CLUSTER_NAME, CLUSTER_REGION, RESOURCE_GROP)
                          build(job: "${params.KUBE_UNDEPLOY_JOB}", 
                          parameters: buildParams, 
                          propagate: true,
                          wait: true)
                }
            }
        }

        stage('Deploying the application in k8 environment for RTRM') {
            steps {
                script {
                    buildParams = commonModule.createKubeDeployParamsForMaster(env.NAMESPACE, params.KUBE_FLAVOUR, params.CLUSTER_NAME, CLUSTER_REGION, RESOURCE_GROP, params.CONTEXT_ROOT_PATH, params.DX_CORE_HOME_PATH, params.PERSONALIZED_DX_CORE_PATH, UPDATE_RELEASE_LEVEL, params.DEPLOYMENT_LEVEL, params.DEPLOYMENT_METHOD)
                          build(job: "${params.KUBE_DEPLOY_JOB}", 
                          parameters: buildParams, 
                          propagate: true,
                          wait: true)
                }
            }
        }

        stage('Updating the application in k8 environment for RTRM') {
            steps {
                script {
                    buildParams = commonModule.createKubeParams(env.NAMESPACE, params.KUBE_FLAVOUR, params.CLUSTER_NAME, CLUSTER_REGION, RESOURCE_GROP, params.CONTEXT_ROOT_PATH, params.DX_CORE_HOME_PATH, params.PERSONALIZED_DX_CORE_PATH, params.UPDATE_DEPLOYMENT_LEVEL, params.DEPLOYMENT_METHOD)
                    buildParams.add(
                        [$class: 'BooleanParameterValue',
                        name: 'ENABLE_INTERRUPT_MIGRATION',
                        value: "${params.ENABLE_INTERRUPT_MIGRATION}"])
                          build(job: "${params.KUBE_UPDATE_JOB}", 
                          parameters: buildParams, 
                          propagate: true,
                          wait: true)
                }
            }
        }
    }

    post {
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
