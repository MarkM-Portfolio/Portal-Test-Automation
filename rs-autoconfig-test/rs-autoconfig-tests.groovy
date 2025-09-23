/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2023. All Rights Reserved.       *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

// Import the shared library with the name configured in Jenkins
@Library("dx-shared-library") _

// Create object to store parameters with values
def pipelineParameters = [:]

pipeline {
    agent {
        label 'build_infra'
    }
    stages {
        stage('Load parameters') {
            steps {
                dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/rs-autoconfig-test/parameters.yaml")
            }
        }
        stage("Check default Search Service when RS not configured") {
            steps {
                script {
                    CURL_RESPONSE = sh (script: "curl -kL -u '${pipelineParameters.USERNAME}:${pipelineParameters.PASSWORD}' ${pipelineParameters.PORTAL_HOST}/mycontenthandler/!ut/p/searchadmin/service/Default+Search+Service", returnStdout: true).trim()
                    println "Curl Response :${CURL_RESPONSE}"
                    if (!pipelineParameters.RS_AUTO_CONFIG && CURL_RESPONSE == '{}') {
                        error("Default Search Service doesn't exist when RS not configured ")
                    }
                }
            }
        }
        stage("Check RS service when RS is not configured") {
            steps {
                script {
                    CURL_RESPONSE = sh (script: "curl -kL -u '${pipelineParameters.USERNAME}:${pipelineParameters.PASSWORD}' ${pipelineParameters.PORTAL_HOST}/mycontenthandler/!ut/p/searchadmin/service/Kubernetes+Remote+Search", returnStdout: true).trim()
                    println "Curl Response :${CURL_RESPONSE}"
                    if (!pipelineParameters.RS_AUTO_CONFIG && CURL_RESPONSE != '{}') {
                        error("RS service doesn't exist when RS is not configured")
                    }
                }
            }
        }
        stage("Check the default Search Service when RS configured") {
            steps {
                script {
                    CURL_RESPONSE = sh (script: "curl -kL -u '${pipelineParameters.USERNAME}:${pipelineParameters.PASSWORD}' ${pipelineParameters.PORTAL_HOST}/mycontenthandler/!ut/p/searchadmin/service/Default+Search+Service", returnStdout: true).trim()
                    println "Curl Response :${CURL_RESPONSE}"
                    if (pipelineParameters.RS_AUTO_CONFIG && CURL_RESPONSE != '{}') {
                        error("Default Search Service doesn't exist when RS configured")
                    }
                }
            }
        }
        stage("Check the RS service when RS configured") {
            steps {
                script {
                    CURL_RESPONSE = sh (script: "curl -kL -u '${pipelineParameters.USERNAME}:${pipelineParameters.PASSWORD}' ${pipelineParameters.PORTAL_HOST}/mycontenthandler/!ut/p/searchadmin/service/Kubernetes+Remote+Search", returnStdout: true).trim()
                    println "Curl Response :${CURL_RESPONSE}"
                    if (pipelineParameters.RS_AUTO_CONFIG && CURL_RESPONSE == '{}') {
                        error("RS service doesn't exist when RS configured")
                    }
                }
            }
        }
        stage("Check RS collections (PORTAL) when RS configured") {
            steps {
                script {
                    CURL_RESPONSE = sh (script: "curl -kL -u '${pipelineParameters.USERNAME}:${pipelineParameters.PASSWORD}' ${pipelineParameters.PORTAL_HOST}/mycontenthandler/!ut/p/searchadmin/service/Kubernetes+Remote+Search/collection/Portal+Search+Collection/provider/Portal+Content+Source", returnStdout: true).trim()
                    println "Curl Response :${CURL_RESPONSE}"
                    if (pipelineParameters.RS_AUTO_CONFIG && CURL_RESPONSE == '{}') {
                        error("RS Portal Collection: Portal Provider is not found.")
                    }
                    CURL_RESPONSE = sh (script: "curl -kL -u '${pipelineParameters.USERNAME}:${pipelineParameters.PASSWORD}' ${pipelineParameters.PORTAL_HOST}/mycontenthandler/!ut/p/searchadmin/service/Kubernetes+Remote+Search/collection/Portal+Search+Collection/provider/WCM+Content+Source", returnStdout: true).trim()
                    println "Curl Response :${CURL_RESPONSE}"
                    if (pipelineParameters.RS_AUTO_CONFIG && CURL_RESPONSE == '{}') {
                        error("RS Portal Collection: WCM Provider is not found.")
                    }
                }
            }
        }
        stage("Check RS collections (JCR) when RS configured") {
            steps {
                script {
                    CURL_RESPONSE = sh (script: "curl -kL -u '${pipelineParameters.USERNAME}:${pipelineParameters.PASSWORD}' ${pipelineParameters.PORTAL_HOST}/mycontenthandler/!ut/p/searchadmin/service/Kubernetes+Remote+Search/collection/JCRCollection1/provider/JCR+Content+Source", returnStdout: true).trim()
                    println "Curl Response :${CURL_RESPONSE}"
                    if (pipelineParameters.RS_AUTO_CONFIG && CURL_RESPONSE == '{}') {
                        error("RS JCR Collection: JCR Provider is not found.")
                    }
                }
            }
        }
        stage("Trigger RS collections (PORTAL) crawlers when RS configured") {
            steps {
                script {
                    CURL_RESPONSE = sh (script: "curl -kL -H 'Content-Type: application/json' -d '{}' -X POST -u '${pipelineParameters.USERNAME}:${pipelineParameters.PASSWORD}' ${pipelineParameters.PORTAL_HOST}/mycontenthandler/!ut/p/searchadmin/service/Kubernetes+Remote+Search/collection/Portal+Search+Collection/provider/Portal+Content+Source/crawl", returnStdout: true).trim()
                    println "Curl Response :${CURL_RESPONSE}"
                    if (pipelineParameters.RS_AUTO_CONFIG && CURL_RESPONSE.indexOf('"crawl": "started"') != -1) {
                        error("RS Portal Collection: Portal Provider crawler failed.")
                    }
                    CURL_RESPONSE = sh (script: "curl -kL -H 'Content-Type: application/json' -d '{}' -X POST -u '${pipelineParameters.USERNAME}:${pipelineParameters.PASSWORD}' ${pipelineParameters.PORTAL_HOST}/mycontenthandler/!ut/p/searchadmin/service/Kubernetes+Remote+Search/collection/Portal+Search+Collection/provider/WCM+Content+Source/crawl", returnStdout: true).trim()
                    println "Curl Response :${CURL_RESPONSE}"
                    if (pipelineParameters.RS_AUTO_CONFIG && CURL_RESPONSE.indexOf('"crawl": "started"') != -1) {
                        error("RS Portal Collection: WCM Provider crawler failed.")
                    }
                }
            }
        }
        stage("Trigger RS collections (JCR) crawlers when RS configured") {
            steps {
                script {
                    CURL_RESPONSE = sh (script: "curl -kL -H 'Content-Type: application/json' -d '{}' -X POST -u '${pipelineParameters.USERNAME}:${pipelineParameters.PASSWORD}' ${pipelineParameters.PORTAL_HOST}/mycontenthandler/!ut/p/searchadmin/service/Kubernetes+Remote+Search/collection/JCRCollection1/provider/JCR+Content+Source/crawl", returnStdout: true).trim()
                    println "Curl Response :${CURL_RESPONSE}"
                    if (pipelineParameters.RS_AUTO_CONFIG && CURL_RESPONSE.indexOf('"crawl": "started"') != -1) {
                        error("RS JCR Collection: JCR Provider crawler failed.")
                    }
                }
            }
        }
    }
    post {
        cleanup {
            dxWorkspaceDirectoriesCleanup()
        }
    }
}
