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

/*** Utility functions and values for on-prem autonomous deployments ***/

/*** Utility values ***/

/*** Utility functions ***/

/**
    Loads all parameters from the parameters defintion file
    Might be refactored into a shared library function, based on outcomes from the architecture community
*/
def loadParameters(Map pipelineParameters, String parameterFile) {
    // Load the parameter file
    def parameterYaml = readYaml file: parameterFile
    // Iterate over all defined parameters
    for (parameter in parameterYaml.parameters) {
        if (parameter.optional) {
            if (!params[parameter.name]) {
                println("No value passed in for optional parameter ${parameter.name}, using default ${parameter.default}")
                pipelineParameters[parameter.name] = parameter.default
            } else {
                pipelineParameters[parameter.name] = params[parameter.name]
            }
        } else {
            // Fail if no value is handed in for a mandatory parameter
            if (!params[parameter.name]) {
                error("ERROR: No parameter value defined mandatory for ${parameter.name}")
            } else {
                pipelineParameters[parameter.name] = params[parameter.name]
            }
        }
        println("Assigned value ${pipelineParameters[parameter.name]} to parameter ${parameter.name}")
    }
}

/**
    Creates the job Parameters for binary AMI creation
*/
def createBinaryAMIjobParameters(Map pipelineParameters) {
    def jobParameters = []
    // Pass in the instance name
    jobParameters.add(string(name: 'SECONDARY_INSTANCE_NAME' , value: "binary-ami-generator"))
    // Pass the instance type
    jobParameters.add(string(name: 'INSTANCE_TYPE', value: "t2.large"))
    // Pass the Core build version
    jobParameters.add(string(name: 'DX_CORE_BUILD_VERSION', value: "${pipelineParameters.DX_CORE_BUILD}"))
    // Pass the Core build number
    jobParameters.add(string(name: 'DXBuildNumber_NAME', value: "${pipelineParameters.DX_CORE_BUILD}"))
    
    println("Job Parameters: ${jobParameters}")

    // Return complete set of jobParameters
    return jobParameters
}

/**
    Creates the job Parameters for cluster deployment
*/
def createClusterDeploymentjobParameters(Map pipelineParameters) {
    def jobParameters = []
    // Pass in the instance name
    jobParameters.add(string(name: 'INSTANCE_NAME' , value: "${pipelineParameters.INSTANCE_NAME}"))
    // Pass the instance type
    jobParameters.add(string(name: 'INSTANCE_TYPE', value: "t2.large"))
    // Pass the Core build version
    jobParameters.add(string(name: 'DX_CORE_BUILD_VERSION', value: "${pipelineParameters.DX_CORE_BUILD}"))
    // INSTANCE_POPO_SCHEDULE for cluster deployment job
    jobParameters.add(string(name: 'INSTANCE_POPO_SCHEDULE', value: "${pipelineParameters.INSTANCE_POPO_SCHEDULE}")) 
    // Pass the additional node
    jobParameters.add(booleanParam(name: 'ADD_ADDITIONAL_NODE', value: true))
    // Pass the database type
    jobParameters.add(string(name: 'DBTYPE', value: "db2"))
    
    println("Job Parameters: ${jobParameters}")

    // Return complete set of jobParameters
    return jobParameters
}

/**
    Creates the job Parameters for cluster undeploy and remove the instances
*/
def createClusterUndeployjobParameters(Map pipelineParameters) {
    def jobParameters = []
    // Pass in the instance name
    jobParameters.add(string(name: 'INSTANCE_NAME' , value: "${pipelineParameters.INSTANCE_NAME}"))
    // Pass the instance type
    jobParameters.add(string(name: 'INSTANCE_TYPE', value: "t2.large"))
    // Pass the cluster environment
    jobParameters.add(booleanParam(name: 'CLUSTERED_ENV', value: true))
    
    println("Job Parameters: ${jobParameters}")

    // Return complete set of jobParameters
    return jobParameters
}

/**
    Creates the job Parameters for hybrid on-prem core undeploy and remove the instances
*/
def createHybridUndeployjobParameters(Map pipelineParameters) {
    def jobParameters = []
    // Pass in the instance name
    jobParameters.add(string(name: 'INSTANCE_NAME' , value: "${pipelineParameters.ON_PREM_INSTANCE_NAME}"))
    // Pass the instance type
    jobParameters.add(string(name: 'INSTANCE_TYPE', value: "${pipelineParameters.ON_PREM_INSTANCE_TYPE}"))
    // Pass the cluster environment
    jobParameters.add(booleanParam(name: 'CLUSTERED_ENV', value: false))
    
    println("Job Parameters: ${jobParameters}")

    // Return complete set of jobParameters
    return jobParameters
}

/**
    Creates the job Parameters for hybrid native kube undeploy and remove the instances
*/
def createHybridKubeUndeployjobParameters(Map pipelineParameters) {
    def jobParameters = []
    // Pass in the instance name
    jobParameters.add(string(name: 'INSTANCE_NAME' , value: "${pipelineParameters.NATIVE_KUBE_INSTANCE_NAME}"))
    // Pass the instance type
    jobParameters.add(string(name: 'INSTANCE_TYPE', value: "${pipelineParameters.NATIVE_KUBE_INSTANCE_TYPE}"))
    // Domain suffix
    jobParameters.add(string(name: 'DOMAIN_SUFFIX', value: "${pipelineParameters.NATIVE_KUBE_DOMAIN_SUFFIX}"))
    
    println("Job Parameters: ${jobParameters}")

    // Return complete set of jobParameters
    return jobParameters
}

/**
    Creates the job Parameters for on-prem hybrid core deployment
*/
def createOnPremHybridCoreDeploymentjobParameters(Map pipelineParameters) {
    def jobParameters = []
    // Pass in the instance name
    jobParameters.add(string(name: 'INSTANCE_NAME' , value: "${pipelineParameters.ON_PREM_INSTANCE_NAME}"))
    // DOMAIN_SUFFIX for for the on-prem hybrid deployment
    jobParameters.add(string(name: 'DOMAIN_SUFFIX' , value: "${pipelineParameters.ON_PREM_DOMAIN_SUFFIX}"))
    // Pass the instance type
    jobParameters.add(string(name: 'INSTANCE_TYPE', value: "${pipelineParameters.ON_PREM_INSTANCE_TYPE}"))
    // Pass the Core build version
    jobParameters.add(string(name: 'DX_CORE_BUILD_VERSION', value: "${pipelineParameters.DX_CORE_BUILD}"))
    // INSTANCE_POPO_SCHEDULE for hybrid core deployment job
    jobParameters.add(string(name: 'INSTANCE_POPO_SCHEDULE', value: "${pipelineParameters.INSTANCE_POPO_SCHEDULE}")) 
    // CLUSTERED_ENV for the on-prem hybrid deployment
    jobParameters.add(booleanParam(name: 'CLUSTERED_ENV', value: false))
    // Pass the additional node
    jobParameters.add(booleanParam(name: 'ADD_ADDITIONAL_NODE', value: false))
    // Pass the database type
    jobParameters.add(string(name: 'DBTYPE', value: "db2"))
    // CONFIGURE_HYBRID for the on-prem hybrid deployment
    jobParameters.add(booleanParam(name: 'CONFIGURE_HYBRID', value: true))
    // HYBRID_KUBE_HOST for the on-prem hybrid deployment
    jobParameters.add(string(name: 'HYBRID_KUBE_HOST', value: "${pipelineParameters.NATIVE_KUBE_INSTANCE_NAME}${pipelineParameters.NATIVE_KUBE_DOMAIN_SUFFIX}"))

    println("Job Parameters: ${jobParameters}")

    // Return complete set of jobParameters
    return jobParameters
}

/**
    Creates the job Parameters for hybrid native kube deployment
*/
def createNativeKubeDeploymentjobParameters(Map pipelineParameters) {
    def jobParameters = []
    // Pass in the instance name
    jobParameters.add(string(name: 'INSTANCE_NAME' , value: "${pipelineParameters.NATIVE_KUBE_INSTANCE_NAME}"))
    // DOMAIN_SUFFIX for native kube deployment job
    jobParameters.add(string(name: 'DOMAIN_SUFFIX' , value: "${pipelineParameters.NATIVE_KUBE_DOMAIN_SUFFIX}"))
    // Pass the instance type
    jobParameters.add(string(name: 'INSTANCE_TYPE', value: "${pipelineParameters.NATIVE_KUBE_INSTANCE_TYPE}"))
    // NATIVE_POPO_SCHEDULE for native kube deployment job
    jobParameters.add(string(name: 'NATIVE_POPO_SCHEDULE', value: "${pipelineParameters.INSTANCE_POPO_SCHEDULE}")) 
    // NEXT_JOB_DELAY_HOURS for native kube deployment job
    jobParameters.add(string(name: 'NEXT_JOB_DELAY_HOURS', value: "0"))
    // HYBRID for native kube deployment job
    jobParameters.add(booleanParam(name: 'HYBRID', value: true))
    // HYBRID_HOST for native kube deployment job
    jobParameters.add(string(name: 'HYBRID_HOST', value: "${pipelineParameters.ON_PREM_INSTANCE_NAME}${pipelineParameters.ON_PREM_DOMAIN_SUFFIX}"))
    // HYBRID_PORT for native kube deployment job
    jobParameters.add(string(name: 'HYBRID_PORT', value: "443"))
    // HYBRID_CORE_SSL for native kube deployment job
    jobParameters.add(booleanParam(name: 'HYBRID_CORE_SSL', value: true))

    println("Job Parameters: ${jobParameters}")

    // Return complete set of jobParameters
    return jobParameters
}

/**
    Retrieves the IP Address behind an AWS Route53 entry
    Requires the AWS hosting zone and the instance name
*/
def getInstanceID(String awsZone, String instanceName) {
    println("Retrieving IP for instance ${instanceName} in AWS zone ${awsZone}")
    def instanceIP = sh(script: "aws --region ${awsZone} ec2 describe-instances --filters \"Name=instance-state-name,Values=running\" \"Name=tag:Name,Values=${instanceName}\" --query 'Reservations[*].Instances[*].[PrivateIpAddress]' --output text", returnStdout: true).trim()
    println("Retrieved IP ${instanceIP} for instance ${instanceName} in AWS zone ${awsZone}")
    return instanceIP 
}

/**
    Creates the job Parameters for acceptance test run for hybrid deployment
*/
def createHybridAcceptanceTestRunjobParameters(Map pipelineParameters) {

    def jobParameters = []

    // Per default, we will try to get test images from the prod artifactory
    def testImageBranch = 'develop'
    def testImageRepository = 'quintana-docker'

    // Determine the target hostname that will be used for the tests
    // Per default, all hosts are the same, since we go through the same ingress
    def coreHost = "${pipelineParameters.ON_PREM_INSTANCE_NAME}${pipelineParameters.ON_PREM_DOMAIN_SUFFIX}"
    def dxConnectHost = "${pipelineParameters.ON_PREM_INSTANCE_NAME}${pipelineParameters.ON_PREM_DOMAIN_SUFFIX}"
    def nativeKubeHost = "${pipelineParameters.NATIVE_KUBE_INSTANCE_NAME}${pipelineParameters.NATIVE_KUBE_DOMAIN_SUFFIX}"

    // Pass in the target branch
    jobParameters.add(string(name: 'TARGET_BRANCH' , value: testImageBranch))
    // Pass the core host
    jobParameters.add(string(name: 'PORTAL_HOST', value: "https://${coreHost}/wps/portal"))
    // Pass the Ring API endpoint
    jobParameters.add(string(name: 'EXP_API', value: "https://${nativeKubeHost}/dx/api/core/v1"))
    // Pass the DAM UI endpoint
    jobParameters.add(string(name: 'APP_ENDPOINT', value: "https://${nativeKubeHost}/dx/ui/dam"))
    // Pass the DAM API endpoint
    jobParameters.add(string(name: 'DAM_API', value: "https://${nativeKubeHost}/dx/api/dam/v1"))
    // Pass the Image Processor API endpoint
    jobParameters.add(string(name: 'IMAGE_PROCESSOR_API', value: "https://${nativeKubeHost}/dx/api/image-processor/v1"))
    // Pass the dxConnect host
    jobParameters.add(string(name: 'DXCONNECT_HOST', value: "https://${dxConnectHost}"))
    // Pass the WCM Rest endpoints
    jobParameters.add(string(name: 'WCMREST', value: "https://${coreHost}/wps"))

    // Pass the artifactory host
    jobParameters.add(string(name: 'ARTIFACTORY_HOST', value: "${testImageRepository}.artifactory.cwp.pnp-hcl.com"))
    // Pass the artifactory image lookup url
    jobParameters.add(string(name: 'ARTIFACTORY_IMAGE_BASE_URL', value: "https://artifactory.cwp.pnp-hcl.com/artifactory/list/${testImageRepository}"))

    // Configure individual testing stages
    jobParameters.add(booleanParam(name: 'TEST_DX_CORE', value: pipelineParameters.ACCEPTANCE_TEST_DX_CORE))
    jobParameters.add(booleanParam(name: 'TEST_RING', value: pipelineParameters.ACCEPTANCE_TEST_RING_API))
    jobParameters.add(booleanParam(name: 'TEST_CC', value: pipelineParameters.ACCEPTANCE_TEST_CONTENT_COMPOSER))
    jobParameters.add(booleanParam(name: 'TEST_DAM', value: pipelineParameters.ACCEPTANCE_TEST_DAM))
    jobParameters.add(booleanParam(name: 'TEST_DXCLIENT', value: pipelineParameters.ACCEPTANCE_TEST_DXCLIENT))
    jobParameters.add(booleanParam(name: 'TEST_URL_LOCALE', value: pipelineParameters.ACCEPTANCE_TEST_URL_LOCALE))
    jobParameters.add(booleanParam(name: 'TEST_THEME_EDITOR', value: pipelineParameters.ACCEPTANCE_TEST_THEME_EDITOR))
    jobParameters.add(booleanParam(name: 'TEST_DAM_SERVER', value: pipelineParameters.ACCEPTANCE_TEST_DAM_SERVER))
    jobParameters.add(booleanParam(name: 'TEST_LICENSE_MANAGER', value: pipelineParameters.ACCEPTANCE_TEST_LICENSE_MANAGER))
    jobParameters.add(booleanParam(name: 'TEST_CR', value: pipelineParameters.ACCEPTANCE_TEST_CONTENT_REPORTING))
    jobParameters.add(booleanParam(name: 'TEST_PICKER', value: pipelineParameters.ACCEPTANCE_TEST_PICKER))

    // If RS autoconfig has been enabled either on the fresh or update deploy, we will test for
    if (pipelineParameters.UPDATE_ENABLE_RS_AUTOCONFIG || pipelineParameters.FRESH_ENABLE_RS_AUTOCONFIG) {
        jobParameters.add(booleanParam(name: 'RS_AUTO_CONFIG', value: true))
    } else {
        jobParameters.add(booleanParam(name: 'RS_AUTO_CONFIG', value: false))
    }

    // SSL Configuration
    jobParameters.add(booleanParam(name: 'SSL_ENABLED', value: true))

    // We get the on-premise IP by looking up the route53 entry of the coreHost
    jobParameters.add(string(name: 'HOST_IP_ADDRESS', value: getInstanceID("us-east-1", "${pipelineParameters.ON_PREM_INSTANCE_NAME}")))

    // Perform an output of all acceptance test parameters used
    println('Determined acceptance test parameters.')
    println("Job Parameters: ${jobParameters}")

    // Return complete set of jobParameters
    return jobParameters
}

/**
    Triggers a binary AMI creation job
*/
def triggerJob(jobParameters, pipelineParameters) {
    def remoteJob
    switch(pipelineParameters.JOB_TYPE) {
        case 'binaryCreation':
            remoteJob = pipelineParameters.BINARY_AMI_CREATION_JOB
            break
        case 'clusterDeploy':
            remoteJob = pipelineParameters.CLUSTER_DEPLOYMENT_JOB
            break
        case 'cleanupInstances':
            remoteJob = pipelineParameters.CLUSTER_UNDEPLOY_JOB
            break
        case 'createNativeKubeDeployment':
            remoteJob = pipelineParameters.NATIVE_KUBE_DEPLOYMENT_JOB
            break
        case 'createOnPremHybridCoreDeployment':
            remoteJob = pipelineParameters.ON_PREM_CORE_DEPLOYMENT_JOB
            break
        case 'cleanupHybridOnPremInstances':
            remoteJob = pipelineParameters.ON_PREM_CORE_UNDEPLOY_JOB
            break
        case 'cleanupHybridKubeInstances':
            remoteJob = pipelineParameters.NATIVE_KUBE_UNDEPLOY_JOB
            break
        default:
            remoteJob = ""
            break
    }

    if (remoteJob != "") {
        jobResult = build(job: remoteJob, 
        parameters: jobParameters, 
        propagate: true,
        wait: true)

        echo "Job Result: ${jobResult}"
    } else {
        error("ERROR: Remote job not specified. Make sure that value assign to pipelineParameters.JOB_TYPE")
    }
}

/* Mandatory return statement on EOF */
return this
