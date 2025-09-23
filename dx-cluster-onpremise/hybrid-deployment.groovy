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

// This is the main pipeline script for hybrid deployment
// This script will trigger the following jobs in sequence
// 1. cleanupHybridOnPremInstances
// 2. cleanupHybridKubeInstances
// 3. createOnPremHybridCoreDeployment
// 4. createNativeKubeDeployment
// 5. Run acceptance test

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
                    dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/dx-cluster-onpremise/hybrid-deployment-parameters.yaml")
                    // Create utility object 
                    utility = load "${env.WORKSPACE}/dx-cluster-onpremise/utility.groovy"
                    if (env.INSTANCE_NAME) {
                        pipelineParameters["ON_PREM_INSTANCE_NAME"] = env.INSTANCE_NAME
                        pipelineParameters["NATIVE_KUBE_INSTANCE_NAME"] = env.INSTANCE_NAME + "-native-kube"
                    } else {
                        error("Instance name should not be empty")
                    }
                    if (env.DX_CORE_BUILD_VERSION) {
                        pipelineParameters["DX_CORE_BUILD"] = env.DX_CORE_BUILD_VERSION
                    } else {
                        error("DX Core build version should not be empty")
                    }
                    // Add DX Code build into pipeline parameter as loadParameter() will not load run-parameter.
                    pipelineParameters["ON_PREM_DOMAIN_SUFFIX"] = DOMAIN_SUFFIX
                    pipelineParameters["NATIVE_KUBE_DOMAIN_SUFFIX"] = DOMAIN_SUFFIX

                    println "Final pipeline praameters: ${pipelineParameters}"
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
