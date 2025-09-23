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

@Library("dx-shared-library") _

// Used to store parameters
def pipelineParameters = [:]

pipeline { 

    agent {
        label 'build_infra'
    }

    /*
     * Preparing all settings we might need, using defaults if no override happens through jenkins params
     */
    stages {
        stage('Validate and load parameter') {
            steps {
                script {
                    // Load parameter
                    dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/dx-cluster-onpremise/parameters.yaml")
                    // Create utility object 
                    utility = load "${env.WORKSPACE}/dx-cluster-onpremise/utility.groovy"
                    // Add DX Code build into pipeline parameter as loadParameter() will not load run-parameter.
                    pipelineParameters["DX_CORE_BUILD"] = DX_CORE_BUILD_VERSION_NAME
                }
            }
        }

        stage('Undeploy previous deployment & cleanup all the instances') {
            steps {
                script {
                    // Add job type parameter to pipeline parameter, this parameter decide that which job has to be trigger.
                    pipelineParameters["JOB_TYPE"] = "cleanupInstances"

                    // Create job parameter
                    jobParams = []
                    jobParams = utility.createClusterUndeployjobParameters(pipelineParameters)

                    // Trigger remote job
                    utility.triggerJob(jobParams, pipelineParameters)
                }
            }
        }

        stage('Create binary AMI') {
            steps {
                script {
                    // Add job type parameter to pipeline parameter, this parameter decide that which job has to be trigger.
                    pipelineParameters["JOB_TYPE"] = "binaryCreation"

                    // Create job parameter
                    jobParams = []
                    jobParams = utility.createBinaryAMIjobParameters(pipelineParameters)

                    // Trigger remote job
                    utility.triggerJob(jobParams, pipelineParameters)
                }
            }
        }

        stage('Deploy cluster with additional node') {
            steps {
                script {
                    // Add job type parameter to pipeline parameter, this parameter decide that which job has to be trigger.
                    pipelineParameters["JOB_TYPE"] = "clusterDeploy"

                    // Create job parameter
                    jobParams = []
                    jobParams = utility.createClusterDeploymentjobParameters(pipelineParameters)

                    // Trigger remote job
                    utility.triggerJob(jobParams, pipelineParameters)
                }
            }
        }
    }

    post {
        cleanup {
            /* Cleanup workspace */
            /* Cleanup workspace@tmp */
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
