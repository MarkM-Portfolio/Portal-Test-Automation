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

    stages {
        /*
         * Deploy the on-prem component
         */
        stage('Deploy on-prem') {
            steps {
                script {
                    env.HYBRID_KUBE_HOST = "${NAMESPACE}${KUBE_DOMAIN_SUFFIX}"
                    if(params.KUBE_FLAVOUR == "openshift") {
                        env.HYBRID_KUBE_HOST = "dx-deployment-service-${NAMESPACE}-${NAMESPACE}${KUBE_DOMAIN_SUFFIX}"
                        if(params.DEPLOYMENT_METHOD == 'helm') {
                            env.HYBRID_KUBE_HOST = "dx-deployment-passthrough-${NAMESPACE}${KUBE_DOMAIN_SUFFIX}"
                        }
                    }
                    buildParams = createJobParams(ON_PREM_PARAM_LIST)
                    buildParams.add(
                        [$class: 'StringParameterValue',
                         name: 'INSTANCE_NAME',
                         value: "${ON_PREM_INSTANCE_NAME}"])
                    buildParams.add(
                        [$class: 'StringParameterValue',
                         name: 'DOMAIN_SUFFIX',
                         value: "${DOMAIN_SUFFIX}"])
                    buildParams.add(
                        [$class: 'BooleanParameterValue',
                         name: 'CONFIGURE_HYBRID',
                         value: true])
                    buildParams.add(
                        [$class: 'BooleanParameterValue',
                         name: 'REMOTE_SEARCH_ENV',
                         value: "${REMOTE_SEARCH_ENV}"])
                    buildParams.add(
                        [$class: 'BooleanParameterValue',
                         name: 'USE_PUBLIC_IP',
                         value: true])
                    buildParams.add(
                        [$class: 'StringParameterValue',
                         name: 'HYBRID_KUBE_HOST',
                         value: "${env.HYBRID_KUBE_HOST}"])
                    buildParams.add(
                        [$class: 'BooleanParameterValue',
                         name: 'CLEANUP_ON_FAILURE',
                         value: "${CLEANUP_ON_FAILURE}"])
                    build(job: "${ON_PREM_JOB}", 
                          parameters: buildParams, 
                          propagate: true, 
                          wait: true)
                }
            }
        }

        /*
         * Deploy the kube component
         */
        stage('Deploy kube') {
            steps {
                script {
                    buildParams = createJobParams(KUBE_PARAM_LIST)
                    buildParams.add(
                        [$class: 'BooleanParameterValue',
                         name: 'HYBRID',
                         value: true])
                    buildParams.add(
                        [$class: 'StringParameterValue',
                         name: 'HYBRID_HOST',
                         value: "${ON_PREM_INSTANCE_NAME}${DOMAIN_SUFFIX}"])
                    buildParams.add(
                        [$class: 'StringParameterValue',
                         name: 'HYBRID_PORT',
                         value: "${ON_PREM_HTTPS_PORT}"])
                    buildParams.add(
                        [$class: 'StringParameterValue',
                         name: 'DEPLOYMENT_METHOD',
                         value: "${params.DEPLOYMENT_METHOD}"])
                    buildParams.add(
                        [$class: 'BooleanParameterValue',
                         name: 'SKIP_ACCEPTANCE_TESTS',
                         value: "${params.SKIP_ACCEPTANCE_TESTS}"])
                    buildParams.add(
                        [$class: 'BooleanParameterValue',
                         name: 'CLEANUP_ON_FAILURE',
                         value: "${CLEANUP_ON_FAILURE}"])
                    if (params.KUBE_FLAVOUR == "native") {
                        buildParams.add(
                            [$class: 'StringParameterValue',
                            name: 'INSTANCE_NAME',
                            value: "${params.NAMESPACE}"])
                    }
                    build(job: "${KUBE_JOB}", 
                          parameters: buildParams,
                          propagate: true, 
                          wait: true)
                }
            }
        }
    }
}

def createJobParams(copyParamListString) {
    copyParamList = copyParamListString.split(',').collect{it as String}
    buildParams = []
    params.each { key, value ->
        if (copyParamList.contains(key)) {
            buildParams.add(
                [$class: 'StringParameterValue',
                    name: key,
                    value: "${value}"])
        }
    }
    return buildParams
}