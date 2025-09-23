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

 COMMON_IMAGE_PUSH_PARAMS = [
    string(name: 'AMBASSADOR_IMAGE_PATH', value: "dx-build-output/common/ambassador"),
    string(name: 'REDIS_IMAGE_PATH', value: "dx-build-output/common/redis"),
    string(name: 'RUNTIME_CONTROLLER_IMAGE_PATH', value: "dx-build-output/operator/hcldx-runtime-controller"),
    string(name: 'PERSISTENCE_CONNECTION_POOL_IMAGE_PATH', value: "dx-build-output/core-addon/persistence/pgpool"),
    string(name: 'PERSISTENCE_NODE_IMAGE_PATH', value: "dx-build-output/core-addon/persistence/postgresrepmgr"),
]

def pushImages(){

    if(params.TARGET_CLOUD_PLATFORM_LIST && params.IMAGE_TYPE_LIST) {

        TARGET_CLOUD_PLATFORMS = env.TARGET_CLOUD_PLATFORM_LIST.tokenize(",")
        echo "Target Cloud Platforms to which images will be pushed: ${TARGET_CLOUD_PLATFORMS}"

        IMAGE_TYPES = env.IMAGE_TYPE_LIST.tokenize(",")
        echo "Type of images that will be pushed: ${IMAGE_TYPES}"

        for (String CLOUD_PLATFORM : TARGET_CLOUD_PLATFORMS) {

            if(CLOUD_PLATFORM == "google") {
                env.TARGET_REGISTRY_ENVIRONMENT = "GCR"
            } 
            if(CLOUD_PLATFORM == "aws") {
                env.TARGET_REGISTRY_ENVIRONMENT = "ECR/OCR"
            } 
            if(CLOUD_PLATFORM == "azure") {
                env.TARGET_REGISTRY_ENVIRONMENT = "ACR"
            }

            for (String IMAGE_TYPE : IMAGE_TYPES) {

                if(IMAGE_TYPE == "develop") {
                    env.IMAGE_REPOSITORY = "quintana-docker"
                    env.CORE_IMAGE_PATH = "dx-build-output/core/dxen"
                    env.CORE_OPERATOR_IMAGE_PATH = "dx-build-output/operator/hcldx-cloud-operator"
                    env.LDAP_IMAGE_PATH = "dx-build-output/core-addon/dx-openldap"
                } else {
                    env.IMAGE_REPOSITORY = "quintana-docker-prod"
                    env.CORE_IMAGE_PATH = "dxen"
                    env.CORE_OPERATOR_IMAGE_PATH = "hcldx-cloud-operator"
                    env.LDAP_IMAGE_PATH = "dx-openldap"
                }

                imagePushParams = [
                    string(name: 'IMAGE_REPOSITORY', value: "${env.IMAGE_REPOSITORY}"),
                    string(name: 'TARGET_REGISTRY_ENVIRONMENT', value: "${env.TARGET_REGISTRY_ENVIRONMENT}"),
                    string(name: 'CORE_IMAGE_FILTER', value: "${IMAGE_TYPE}"),
                    string(name: 'RS_IMAGE_FILTER', value: "${IMAGE_TYPE}"),
                    string(name: 'DAM_IMAGE_FILTER', value: "${IMAGE_TYPE}"),
                    string(name: 'CC_IMAGE_FILTER', value: "${IMAGE_TYPE}"),
                    string(name: 'IMGPROC_IMAGE_FILTER', value: "${IMAGE_TYPE}"),
                    string(name: 'DAM_KALTURA_PLUGIN_IMAGE_FILTER', value: "${IMAGE_TYPE}"),
                    string(name: 'PERSISTENCE_IMAGE_FILTER', value: "${IMAGE_TYPE}"),
                    string(name: 'PERSISTENCE_CONNECTION_POOL_IMAGE_FILTER', value: "${IMAGE_TYPE}"),
                    string(name: 'PERSISTENCE_NODE_IMAGE_FILTER', value: "${IMAGE_TYPE}"),
                    string(name: 'DAM_PLUGIN_GOOGLE_VISION_IMAGE_FILTER', value: "${IMAGE_TYPE}"),
                    string(name: 'RINGAPI_IMAGE_FILTER', value: "${IMAGE_TYPE}"),
                    string(name: 'PERSISTENCE_METRICS_EXPORTER_IMAGE_FILTER', value: "${IMAGE_TYPE}"),
                    string(name: 'SIDECAR_IMAGE_FILTER', value: "${IMAGE_TYPE}"),
                    string(name: 'HAPROXY_IMAGE_FILTER', value: "${IMAGE_TYPE}"),
                    string(name: 'LOGGING_SIDECAR_IMAGE_FILTER', value: "${IMAGE_TYPE}"),
                    string(name: 'PREREQS_CHECKER_IMAGE_FILTER', value: "${IMAGE_TYPE}"),
                    string(name: 'DESIGN_STUDIO_IMAGE_FILTER', value: "${IMAGE_TYPE}"),
                    string(name: 'RUNTIME_CONTROLLER_IMAGE_FILTER', value: "${IMAGE_TYPE}"),
                    string(name: 'CORE_OPERATOR_IMAGE_FILTER', value: "${IMAGE_TYPE}"),
                    string(name: 'DAM_OPERATOR_IMAGE_FILTER', value: "${IMAGE_TYPE}"),
                    string(name: 'LDAP_IMAGE_FILTER', value: "${IMAGE_TYPE}"),
                    string(name: 'CORE_IMAGE_PATH', value: "${env.CORE_IMAGE_PATH}"),
                    string(name: 'CORE_OPERATOR_IMAGE_PATH', value: "${env.CORE_OPERATOR_IMAGE_PATH}"),
                    string(name: 'LDAP_IMAGE_PATH', value: "${env.LDAP_IMAGE_PATH}"),
                ].plus(COMMON_IMAGE_PUSH_PARAMS)

                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    echo "Pushing images to TARGET_REGISTRY_ENVIRONMENT: ${env.TARGET_REGISTRY_ENVIRONMENT}, Image Type: ${IMAGE_TYPE}"
                    build job: "${params.PUSH_IMAGE_TO_REGISTRY_JOB}", parameters: imagePushParams
                }
            }
        }
    }
}

pipeline {

    agent {
        label 'build_infra'
    }

    stages {
        stage('Pushing the images to Cloud registries') {
            steps {
                script {
                    if (!env.MS_TEAMS_URL){
                        env.MS_TEAMS_URL = 'https://outlook.office.com/webhook/8a6712b0-0629-4fbb-9e35-641ae6c7f577@189de737-c93a-4f5a-8b68-6f4ca9941912/JenkinsCI/a1fa77efc3b545a0aba82ab2bf0ddd4f/e012756a-5de7-490a-9a92-8b5b2c116578'
                    }
                    pushImages()
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
                office365ConnectorSend message: "Build Success ${env.JOB_NAME} commited by @${user} [View on Jenkins] Image Push to cloud registries is successful", status: "Build Success", webhookUrl: "${env.MS_TEAMS_URL}"
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