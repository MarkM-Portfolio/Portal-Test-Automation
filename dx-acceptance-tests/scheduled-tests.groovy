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

env.ACCEPTANCE_TEST_JOB = "CI/Automated_Tests/Acceptance_Tests"
env.TARGET_ENVIRONMENT_SUFFIX = "dev"

applicationsToTest = [
    string(name: 'TEST_DX_CORE', value: "true"),
    string(name: 'TEST_RING', value: "true"),
    string(name: 'TEST_CC', value: "true"),
    string(name: 'TEST_DAM', value: "true"),
    string(name: 'TEST_DAM_SERVER', value: "true"),
    string(name: 'TEST_DXCLIENT', value: "true"),
    string(name: 'TEST_URL_LOCALE', value: "false"),
    string(name: 'TEST_THEME_EDITOR', value: "true"),
    string(name: 'TEST_CR', value: "true"),
    string(name: 'TEST_PICKER', value: "true"),
    string(name: 'TEST_PEOPLE_SERVICE', value: "true"),
]

if(env.KUBE_LIST) {
    env.IMAGE_REPO = "quintana-docker-prod"
    if(params.TARGET_ENVIRONMENT == "master") {
        env.TARGET_BRANCH = "master"
        env.TARGET_ENVIRONMENT_SUFFIX = "mastr"
        env.MEDIA_LIBRARY_PERSISTENCE_IMAGE_FILTER = "rivendell_master"
        env.ARTIFACTORY_IMAGE_BASE_URL = "https://artifactory.cwp.pnp-hcl.com/artifactory/list/quintana-docker-prod"
    } else if(params.TARGET_ENVIRONMENT == "release") {
        env.TARGET_BRANCH = "${params.TARGET_BRANCH}"
        env.TARGET_ENVIRONMENT_SUFFIX = "rel"
        env.MEDIA_LIBRARY_PERSISTENCE_IMAGE_FILTER = "rivendell_release"
        env.ARTIFACTORY_IMAGE_BASE_URL = "https://artifactory.cwp.pnp-hcl.com/artifactory/list/quintana-docker-prod"
    } else {
        env.IMAGE_REPO = "quintana-docker"
        env.TARGET_BRANCH = "develop"
        env.TARGET_ENVIRONMENT_SUFFIX = "dev"
        env.MEDIA_LIBRARY_PERSISTENCE_IMAGE_FILTER = ""
        env.ARTIFACTORY_IMAGE_BASE_URL = "https://artifactory.cwp.pnp-hcl.com/artifactory/list/quintana-docker"
    }

    echo "Scheduling autonomous deployments for target Environment: ${env.TARGET_ENVIRONMENT}, Branch: ${env.TARGET_BRANCH}, Suffix: ${env.TARGET_ENVIRONMENT_SUFFIX}"

    KUBE_PARAM_LIST = env.KUBE_LIST.tokenize(",")

    echo "Kube List: ${KUBE_PARAM_LIST}"

    for (String kube_item : KUBE_PARAM_LIST) {
        CONTEXT_ROOT_PATH = '/wps'
        DX_CORE_HOME_PATH = '/portal'
        if(params.TARGET_ENVIRONMENT == "master") {
            if(kube_item == "gke" || kube_item == "os"){
                env.SOURCE_ENVIRONMENT_LEVEL = "n${env.ALT_UPDATE_LEVEL_FOR_MASTER}"
            }
            if(kube_item == "aks" || kube_item == "eks"){
                env.SOURCE_ENVIRONMENT_LEVEL = "n${env.DEFAULT_UPDATE_LEVEL_FOR_MASTER}"
            }
            if(kube_item == "aks" && env.DEPLOYMENT_METHOD == 'helm') {
                CONTEXT_ROOT_PATH = '/hcl'
                DX_CORE_HOME_PATH = '/dx'
            }
            if(kube_item == "gke" && env.DEPLOYMENT_METHOD == 'helm'){
                CONTEXT_ROOT_PATH = ''
                DX_CORE_HOME_PATH = ''
            }
        }

        if(params.TARGET_ENVIRONMENT == "release") {
            if(kube_item == "gke" || kube_item == "os"){
                env.SOURCE_ENVIRONMENT_LEVEL = "n${env.DEFAULT_UPDATE_LEVEL_FOR_RELEASE}"
            }
            if(kube_item == "aks" || kube_item == "eks"){
                env.SOURCE_ENVIRONMENT_LEVEL = "n${env.ALT_UPDATE_LEVEL_FOR_RELEASE}"
            }
            if(kube_item == "gke" && env.DEPLOYMENT_METHOD == 'helm') {
                CONTEXT_ROOT_PATH = '/hcl'
                DX_CORE_HOME_PATH = '/dx'
            }
            if((kube_item == "os" || kube_item == "eks") && env.DEPLOYMENT_METHOD == 'helm'){
                CONTEXT_ROOT_PATH = ''
                DX_CORE_HOME_PATH = ''
            }
        }

        if(params.TARGET_ENVIRONMENT == "develop") {
            if(kube_item == "gke" || kube_item == "os"){
                env.SOURCE_ENVIRONMENT_LEVEL = "n${env.ALT_UPDATE_LEVEL_FOR_DEV}"
            }
            if(kube_item == "aks" || kube_item == "eks"){
                env.SOURCE_ENVIRONMENT_LEVEL = "n${env.DEFAULT_UPDATE_LEVEL_FOR_DEV}"
            }
            if((kube_item == "os" || kube_item == "eks") && env.DEPLOYMENT_METHOD == 'helm') {
                CONTEXT_ROOT_PATH = '/hcl'
                DX_CORE_HOME_PATH = '/dx'
            }
            if(kube_item == "aks" && env.DEPLOYMENT_METHOD == 'helm'){
                CONTEXT_ROOT_PATH = ''
                DX_CORE_HOME_PATH = ''
            }
        }

        /* For CF198 we do not have n-1 helm jobs configured, so we force the maximum depth of jobs will be forced to 0 */
        if ("${env.TARGET_BRANCH}".contains("CF198") && env.DEPLOYMENT_METHOD == 'helm') {
            echo "Running for CF198 - forcing helm deployments to go for n-0 at max."
            env.SOURCE_ENVIRONMENT_LEVEL = "n0"
        }

        env.HOSTNAME = "${kube_item}-${env.DEPLOYMENT_METHOD}-${env.SOURCE_ENVIRONMENT_LEVEL}-${env.TARGET_ENVIRONMENT_SUFFIX}"
        echo "KUBE DEPLOYMENT ENVIRONMENT: ${kube_item}, HOST NAME: ${env.HOSTNAME}"
        echo "PORTAL_HOST: https://${env.HOSTNAME}.hcl-dx-dev.net${CONTEXT_ROOT_PATH}${DX_CORE_HOME_PATH}"
        kubeUpdateTestParams = [
            string(name: 'PORTAL_HOST', value: "https://${env.HOSTNAME}.hcl-dx-dev.net${CONTEXT_ROOT_PATH}${DX_CORE_HOME_PATH}"),
            string(name: 'EXP_API', value: "https://${env.HOSTNAME}.hcl-dx-dev.net/dx/api/core/v1"),
            string(name: 'APP_ENDPOINT', value: "https://${env.HOSTNAME}.hcl-dx-dev.net/dx/ui/dam"),
            string(name: 'DXCONNECT_HOST', value: "https://${env.HOSTNAME}.hcl-dx-dev.net"),
            string(name: 'WCMREST', value: "https://${env.HOSTNAME}.hcl-dx-dev.net${CONTEXT_ROOT_PATH}"),
            string(name: 'TARGET_BRANCH', value: "${env.TARGET_BRANCH}"),
            string(name: 'SSL_ENABLED', value: "true"),
            string(name: 'ARTIFACTORY_HOST', value: "${env.IMAGE_REPO}.artifactory.cwp.pnp-hcl.com"),
            string(name: 'ARTIFACTORY_IMAGE_BASE_URL', value: env.ARTIFACTORY_IMAGE_BASE_URL),
            string(name: 'MEDIA_LIBRARY_PERSISTENCE_IMAGE_FILTER', value: env.MEDIA_LIBRARY_PERSISTENCE_IMAGE_FILTER),
            string(name: 'DAM_API', value: "https://${env.HOSTNAME}.hcl-dx-dev.net/dx/api/dam/v1"),
            string(name: 'IMAGE_PROCESSOR_API', value: "https://${env.HOSTNAME}.hcl-dx-dev.net/dx/api/image-processor/v1")
        ].plus(applicationsToTest)

        if(kube_item == "gke"){
            kubeUpdateGKE = kubeUpdateTestParams
        }

        if(kube_item == "eks"){
            kubeUpdateEKS = kubeUpdateTestParams
        }

        if(kube_item == "aks"){
            kubeUpdateAKS = kubeUpdateTestParams
        }

        if(kube_item == "os"){
            OS_DOMAIN_SUFFIX= ".apps.dx-cluster-dev.hcl-dx-dev.net"
            OS_SERVICE = "passthrough"
            OS_HOST_NAME = "${env.HOSTNAME}"
            if(env.DEPLOYMENT_METHOD != 'helm') {
                OS_SERVICE = "service"
                OS_HOST_NAME = "${env.HOSTNAME}-${env.HOSTNAME}"
            }
            echo "PORTAL_HOST: https://dx-deployment-${OS_SERVICE}-${OS_HOST_NAME}${OS_DOMAIN_SUFFIX}${CONTEXT_ROOT_PATH}${DX_CORE_HOME_PATH}"
            kubeUpdateOS = [
                string(name: 'PORTAL_HOST', value: "https://dx-deployment-${OS_SERVICE}-${OS_HOST_NAME}${OS_DOMAIN_SUFFIX}${CONTEXT_ROOT_PATH}${DX_CORE_HOME_PATH}"),
                string(name: 'EXP_API', value: "https://dx-deployment-${OS_SERVICE}-${OS_HOST_NAME}${OS_DOMAIN_SUFFIX}/dx/api/core/v1"),
                string(name: 'APP_ENDPOINT', value: "https://dx-deployment-${OS_SERVICE}-${OS_HOST_NAME}${OS_DOMAIN_SUFFIX}/dx/ui/dam"),
                string(name: 'DXCONNECT_HOST', value: "https://dx-deployment-${OS_SERVICE}-dxconnect-${env.HOSTNAME}${OS_DOMAIN_SUFFIX}"),
                string(name: 'WCMREST', value: "https://dx-deployment-${OS_SERVICE}-${env.HOSTNAME}${OS_DOMAIN_SUFFIX}${CONTEXT_ROOT_PATH}"),
                string(name: 'TARGET_BRANCH', value: "${env.TARGET_BRANCH}"),
                string(name: 'SSL_ENABLED', value: "true"),
                string(name: 'ARTIFACTORY_HOST', value: "${env.IMAGE_REPO}.artifactory.cwp.pnp-hcl.com"),
                string(name: 'ARTIFACTORY_IMAGE_BASE_URL', value: env.ARTIFACTORY_IMAGE_BASE_URL),
                string(name: 'MEDIA_LIBRARY_PERSISTENCE_IMAGE_FILTER', value: env.MEDIA_LIBRARY_PERSISTENCE_IMAGE_FILTER),
                string(name: 'DAM_API', value: "https://dx-deployment-${OS_SERVICE}-${OS_HOST_NAME}${OS_DOMAIN_SUFFIX}/dx/api/dam/v1"),
                string(name: 'IMAGE_PROCESSOR_API', value: "https://dx-deployment-${OS_SERVICE}-${OS_HOST_NAME}${OS_DOMAIN_SUFFIX}/dx/api/image-processor/v1")
            ].plus(applicationsToTest)
        }

    }
}

usLatest = [
    string(name: 'PORTAL_HOST', value: "http://us-latest.team-q-dev.com:10039/wps/portal"),
    string(name: 'EXP_API', value: "http://us-latest.team-q-dev.com:4000/dx/api/core/v1"),
    string(name: 'APP_ENDPOINT', value: "http://us-latest.team-q-dev.com:3000/dx/ui/dam"),
    string(name: 'DXCONNECT_HOST', value: "https://us-latest.team-q-dev.com:10202"),
    string(name: 'WCMREST', value: "http://us-latest.team-q-dev.com:10039/wps"),
    string(name: 'DAM_API', value: "http://us-latest.team-q-dev.com:3000/dx/api/dam/v1"),
    string(name: 'IMAGE_PROCESSOR_API', value: "http://us-latest.team-q-dev.com:8080/dx/api/image-processor/v1")
].plus(applicationsToTest)

kubeNative = [
    string(name: 'PORTAL_HOST', value: "https://native-kube-latest.team-q-dev.com/wps/portal"),
    string(name: 'EXP_API', value: "https://native-kube-latest.team-q-dev.com/dx/api/core/v1"),
    string(name: 'APP_ENDPOINT', value: "https://native-kube-latest.team-q-dev.com/dx/ui/dam"),
    string(name: 'DXCONNECT_HOST', value: "https://native-kube-latest.team-q-dev.com"),
    string(name: 'WCMREST', value: "http://native-kube-latest.team-q-dev.com/wps"),
    string(name: 'SSL_ENABLED', value: "true"),
    string(name: 'DAM_API', value: "https://native-kube-latest.team-q-dev.com/dx/api/dam/v1"),
    string(name: 'IMAGE_PROCESSOR_API', value: "https://native-kube-latest.team-q-dev.com/dx/api/image-processor/v1")
].plus(applicationsToTest)

smLatest = [
    string(name: 'PORTAL_HOST', value: "http://site-manager-latest.team-q-dev.com:10039/wps/portal"),
    string(name: 'EXP_API', value: "http://site-manager-latest.team-q-dev.com:4000/dx/api/core/v1"),
    string(name: 'APP_ENDPOINT', value: "http://site-manager-latest.team-q-dev.com:3000/dx/ui/dam"),
    string(name: 'WCMREST', value: "http://site-manager-latest.team-q-dev.com:10039/wps"),
    string(name: 'DXCONNECT_HOST', value: "https://site-manager-latest.team-q-dev.com:10202"),
    string(name: 'DAM_API', value: "http://site-manager-latest.team-q-dev.com:3000/dx/api/dam/v1"),
    string(name: 'IMAGE_PROCESSOR_API', value: "http://site-manager-latest.team-q-dev.com:8080/dx/api/image-processor/v1")
].plus(applicationsToTest)

pipeline {
    agent {
        label 'build_infra'
    }
    stages {
        stage('Test Autonomous Kube Updates for Google GKE') {
            when { expression { params.KUBE_LIST && KUBE_LIST.contains("gke") } }
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    build job: "${env.ACCEPTANCE_TEST_JOB}", parameters: kubeUpdateGKE
                }
            }
        }
        stage('Test Autonomous Kube Updates for AWS EKS') {
            when { expression { params.KUBE_LIST && KUBE_LIST.contains("eks") } }
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    build job: "${env.ACCEPTANCE_TEST_JOB}", parameters: kubeUpdateEKS
                }
            }
        }
        stage('Test Autonomous Kube Updates for Azure AKS') {
            when { expression { params.KUBE_LIST && KUBE_LIST.contains("aks") } }
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    build job: "${env.ACCEPTANCE_TEST_JOB}", parameters: kubeUpdateAKS
                }
            }
        }
        stage('Test Autonomous Kube Updates for Openshift') {
            when { expression { params.KUBE_LIST && KUBE_LIST.contains("os") } }
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    build job: "${env.ACCEPTANCE_TEST_JOB}", parameters: kubeUpdateOS
                }
            }
        }
        stage('Test US Latest') {
            when { expression { !params.KUBE_LIST } }
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    build job: "${env.ACCEPTANCE_TEST_JOB}", parameters: usLatest
                }
            }
        }
        stage('Test Native Kube') {
            when { expression { !params.KUBE_LIST } }
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    build job: "${env.ACCEPTANCE_TEST_JOB}", parameters: kubeNative
                }
            }
        }
        stage('Test SM Latest') {
            when { expression { !params.KUBE_LIST } }
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    build job: "${env.ACCEPTANCE_TEST_JOB}", parameters: smLatest
                }
            }
        }
    }
    post {
        cleanup {
            script {
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
}