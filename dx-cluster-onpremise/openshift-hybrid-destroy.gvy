/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2022. All Rights Reserved.       *
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

    stages {
        /*
         * Validate the supplied parameters
         */
        stage('Validate parameters') {
            steps {
                script {
                    if (!params.ON_PREM_INSTANCE_NAME){
                        error("On-premise instance name should not be empty")
                    }
                    if (!params.NAMESPACE){
                        error("Kube namespace should not be empty")
                    }
                    if (!params.ON_PREM_JOB){
                        error("On-premise destroy job name should not be empty")
                    }
                    if (!params.KUBE_JOB){
                        error("Kube destroy job name should not be empty")
                    }
                }
            }
        }

        /*
         * Destroy the on-prem component
         */
        stage('Destroy on-prem') {
            steps {
                script {
                    buildParams = []
                    buildParams.add(
                        [$class: 'StringParameterValue',
                         name: 'INSTANCE_NAME',
                         value: "${ON_PREM_INSTANCE_NAME}"])
                    buildParams.add(
                        [$class: 'BooleanParameterValue',
                         name: 'CLUSTERED_ENV',
                         value: env.CLUSTERED_ENV])
                    buildParams.add(
                        [$class: 'BooleanParameterValue',
                        name: 'REMOTE_SEARCH_ENV',
                        value: env.REMOTE_SEARCH_ENV])
                    build(job: "${ON_PREM_JOB}", 
                          parameters: buildParams, 
                          propagate: true, 
                          wait: true)
                }
            }
        }

        /*
         * Destroy the kube component
         */
        stage('Destroy kube') {
            steps {
                script {
                    buildParams = []
                    if (params.KUBE_FLAVOUR == "native") {
                        buildParams.add(
                            [$class: 'StringParameterValue',
                            name: 'INSTANCE_NAME',
                            value: "${params.NAMESPACE}"])
                    } else {
                        buildParams.add(
                            [$class: 'StringParameterValue',
                            name: 'NAMESPACE',
                            value: "${NAMESPACE}"])
                        buildParams.add(
                            [$class: 'StringParameterValue',
                            name: 'CLUSTER_NAME',
                            value: "${CLUSTER_NAME}"])
                        buildParams.add(
                            [$class: 'StringParameterValue',
                            name: 'CLUSTER_REGION',
                            value: "${CLUSTER_REGION}"])
                        buildParams.add(
                            [$class: 'StringParameterValue',
                            name: 'RESOURCE_GROP',
                            value: "${RESOURCE_GROP}"])
                        buildParams.add(
                            [$class: 'StringParameterValue',
                            name: 'NFS_SERVER',
                            value: "${NFS_SERVER}"])
                    }
                    buildParams.add(
                        [$class: 'StringParameterValue',
                         name: 'KUBE_FLAVOUR',
                         value: "${KUBE_FLAVOUR}"])
                    build(job: "${KUBE_JOB}", 
                          parameters: buildParams,
                          propagate: true, 
                          wait: true)
                }
            }
        }
    }
}
