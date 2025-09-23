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

def cleanImages(){

    if(params.TARGET_CLOUD_PLATFORM_LIST && params.IMAGE_REPOSITORY_LIST) {

        TARGET_CLOUD_PLATFORMS = env.TARGET_CLOUD_PLATFORM_LIST.tokenize(",")
        echo "Target Cloud Platforms from which images will be deleted: ${TARGET_CLOUD_PLATFORMS}"

        IMAGE_REPOSITORIES = env.IMAGE_REPOSITORY_LIST.tokenize(",")
        echo "Type of images that will be deleted: ${IMAGE_REPOSITORIES}"

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

            for (String IMAGE_REPOSITORY : IMAGE_REPOSITORIES) {
                imageDeleteParams = [
                    string(name: 'IMAGE_REPOSITORY', value: "${IMAGE_REPOSITORY}"),
                    string(name: 'TARGET_REGISTRY_ENVIRONMENT', value: "${env.TARGET_REGISTRY_ENVIRONMENT}"),
                    string(name: 'DAYS_TO_DELETE', value: "${env.DAYS_TO_DELETE}"),
                    string(name: 'DELETE_MASTER_EXCEPT', value: "${env.DELETE_MASTER_EXCEPT}"),
                ]
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    echo "Cleaning images from TARGET_REGISTRY_ENVIRONMENT: ${env.TARGET_REGISTRY_ENVIRONMENT}, Image Repo Type: ${IMAGE_REPOSITORY}"
                    build job: "${params.CLEAN_IMAGE_FROM_REGISTRY_JOB}", parameters: imageDeleteParams
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
        stage('Cleaning the images from Cloud registries') {
            steps {
                script {
                    cleanImages()
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