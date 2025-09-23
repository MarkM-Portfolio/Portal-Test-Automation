/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2022, 2023. All Rights Reserved. *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

import java.text.SimpleDateFormat

@Library('dx-shared-library') _


def pipelineParameters = [:]

pipeline {
    agent {
        label 'build_infra'
    }

    stages {
        // Load the pipeline parameters from the YAML file
        stage('Load parameters') {
            steps {
                script {
                    dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/dx-opensearch-logs/destroy-logs.yaml")
                }
            }
        }

        stage('Delete Old Logs') {
            steps {
               withCredentials([
                   usernamePassword(credentialsId: 'cwp-opensearch', passwordVariable: 'OPENSEARCH_PASSWORD', usernameVariable: 'OPENSEARCH_USERNAME'),
                ]) {
                    script {
                       // Execute the shell script with provided parameters
                 
                        sh "chmod +x ${workspace}/test-scripts/delete-logs-opensearch.sh && sh ${workspace}/test-scripts/delete-logs-opensearch.sh ${pipelineParameters.PROTOCOL} ${pipelineParameters.HOSTNAME} ${pipelineParameters.INDEX_NAME} ${pipelineParameters.USER_NAME} ${OPENSEARCH_PASSWORD}"
                
                    }
                }
            }
        }    
    }
   
    /*
     * Perform proper cleanup to leave a healthy Jenkins agent.
     */
    post {
        cleanup {
            // Cleanup workspace
            dxWorkspaceDirectoriesCleanup()
        }
    }
}
