/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2024. All Rights Reserved.       *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

import groovy.json.JsonSlurper

// Use our DX shared library
@Library("dx-shared-library") _

// Create object to store parameters with values
def pipelineParameters = [:]

def noDbg = "{ set +x; } 2>/dev/null"

pipeline {
    // Runs in build_infra, since we are creating infrastructure
    agent {
        label 'build_infra'
    }

    stages {
        // Load the pipeline parameters into object
        stage('Load parameters') {
            steps {
                dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/dx-opensearch-eap/new-wcm-crawler/parametersDelete.yaml")
                // Change script files to executables
                script {
                    dir("${env.WORKSPACE}/dx-opensearch-eap/new-wcm-crawler/helpers") {
                        sh "chmod 755 * "
                    }
                    if (pipelineParameters.INDEX == "") {
                        error("No management index to clean.")
                    }
                    pipelineParameters.URL_INDEX = pipelineParameters.INDEX.toLowerCase()
                }
            }
        }

        // Get authentication bearer token
        stage("Search authorization") {
            steps {
                script {
                    dir("${env.WORKSPACE}/dx-opensearch-eap/new-wcm-crawler/helpers") {
                        def authResult = sh(script: """
                                            ${noDbg}
                                            ./01-search-authorization.sh ${pipelineParameters.SEARCH_HOST} ${pipelineParameters.SEARCH_ADMIN} ${pipelineParameters.SEARCH_ADMIN_PASSWORD}
                                        """, returnStdout: true)
                        if (authResult.contains("\"jwt\":")) {
                            pipelineParameters.bearerToken = authResult.split(':')[1].split('"')[1]
                            println "Bearer: ${pipelineParameters.bearerToken}"
                        } else {
                            error("Authorization failed.")
                        }
                    }
                }
            }
        }

        // Delete all content/crawler
        stage("Delete") {
            steps {
                script {
                    dir("${env.WORKSPACE}/dx-opensearch-eap/new-wcm-crawler/helpers") {
                        def indexResult = "{ }"
                        while (!indexResult.contains("\"${pipelineParameters.INDEX}\": []")) {
                            indexResult = sh(script: """
                                            ${noDbg}
                                            ./X1-get-mgmt-index.sh ${pipelineParameters.bearerToken} ${pipelineParameters.SEARCH_HOST} ${pipelineParameters.URL_INDEX}
                                        """, returnStdout: true)
                            if (indexResult.contains("\"id\":")) {
                                pipelineParameters.indexId = indexResult.split("\"id\":")[1].split('"')[1]
                                println "Delete ${pipelineParameters.indexId} from ${pipelineParameters.INDEX}"
                                sh(script: """
                                        ${noDbg}
                                        ./X2-del-index-element.sh ${pipelineParameters.bearerToken} ${pipelineParameters.SEARCH_HOST} ${pipelineParameters.URL_INDEX} ${pipelineParameters.indexId}
                                    """, returnStdout: true)
                            }
                        }
                        println indexResult
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
