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
                    dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/dx-cluster-onpremise/hybrid-autonomous-deployment-parameters.yaml")
                    // Create utility object 
                    utility = load "${env.WORKSPACE}/dx-cluster-onpremise/utility.groovy"
                    // Add DX Code build into pipeline parameter as loadParameter() will not load run-parameter.
                    pipelineParameters["DX_CORE_BUILD"] = DX_CORE_BUILD_VERSION_NAME
                }
            }
        }

        stage('Undeploy previous hybrid on-prem deployment & cleanup all the instances') {
            steps {
                script {
                    // Add job type parameter to pipeline parameter, this parameter decide that which job has to be trigger.
                    pipelineParameters["JOB_TYPE"] = "cleanupHybridOnPremInstances"

                    // Create job parameter
                    jobParams = []
                    jobParams = utility.createHybridUndeployjobParameters(pipelineParameters)

                    // Trigger remote job
                    utility.triggerJob(jobParams, pipelineParameters)
                }
            }
        }

        stage('Undeploy previous hybrid native kube deployment & cleanup all the instances') {
            steps {
                script {
                    // Add job type parameter to pipeline parameter, this parameter decide that which job has to be trigger.
                    pipelineParameters["JOB_TYPE"] = "cleanupHybridKubeInstances"

                    // Create job parameter
                    jobParams = []
                    jobParams = utility.createHybridKubeUndeployjobParameters(pipelineParameters)

                    // Trigger remote job
                    utility.triggerJob(jobParams, pipelineParameters)
                }
            }
        }

        stage('Create on-prem hybrid core deployment') {
            steps {
                script {
                    // Add job type parameter to pipeline parameter, this parameter decide that which job has to be trigger.
                    pipelineParameters["JOB_TYPE"] = "createOnPremHybridCoreDeployment"

                    // Create job parameter
                    jobParams = []
                    jobParams = utility.createOnPremHybridCoreDeploymentjobParameters(pipelineParameters)

                    // Trigger remote job
                    utility.triggerJob(jobParams, pipelineParameters)
                }
            }
        }

        stage('Create native kube deployment') {
            steps {
                script {
                    // Add job type parameter to pipeline parameter, this parameter decide that which job has to be trigger.
                    pipelineParameters["JOB_TYPE"] = "createNativeKubeDeployment"

                    // Create job parameter
                    jobParams = []
                    jobParams = utility.createNativeKubeDeploymentjobParameters(pipelineParameters)

                    // Trigger remote job
                    utility.triggerJob(jobParams, pipelineParameters)
                }
            }
        }

        stage('Run acceptance test') {
            when {
                expression { pipelineParameters.FRESH_DEPLOYMENT_RUN_ACCEPTANCE_TESTS }
            }
            steps {
                script {
                    // Create job parameter
                    jobParams = []
                    jobParams = utility.createHybridAcceptanceTestRunjobParameters(pipelineParameters)

                    catchError {
                        build(job: pipelineParameters.ACCEPTANCE_TEST_JOB, 
                            parameters: jobParams, 
                            propagate: true,
                            wait: true)
                    }
                }
            }
        }
    }

    post {
        cleanup {
            /* Cleanup workspace */
            dxWorkspaceDirectoriesCleanup()
        }
    }
}
