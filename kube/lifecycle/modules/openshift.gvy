/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2020, 2023. All Rights Reserved. *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

/* Loading other modules for usage */
/* Required configs */
commonConfig = load "${configDirectory}/common.gvy"
openshiftConfig = load "${configDirectory}/openshift.gvy"
/* Modules with functions */
commonModule = load "${moduleDirectory}/common.gvy"

/*
 * Pipeline Module for openshift k8s deployment
 * Contains all necessary functions to perform a working openshift k8s deployment and teardown
 * Will work with different openshift targets like our AWS and NJDC instances
 */

/*
 * create the openshift environment
 */
def createEnvironment() {
    // Configure kubectl to use the target openshift environment
    loginOpenshift()

    // Load openshift default variables, setting the skipPVCreation flag to "false"
    openshiftEnvVars=generateOpenShiftDeploymentVariables(false)

    // Create a namespace in the target cluster
    commonModule.createNamespace()
    commonModule.createHAProxyOpenshiftRoute()
    
    // Update passthrough host to use dx domain and create network policies
    if (commonConfig.COMMON_KUBE_FLAVOUR == 'openshift') {
        sh """
           oc patch route dx-deployment-passthrough -n "${NAMESPACE}" --type=json -p='[{"op": "replace", "path": "/spec/host", "value":"dx-deployment-passthrough-${NAMESPACE}${commonConfig.COMMON_DOMAIN_SUFFIX}"}]'
           
           sed -i "s|NAMESPACE_PLACEHOLDER|${NAMESPACE}|g" ${env.WORKSPACE}/kube/lifecycle/scripts/openshift/networkPolicies.yaml
           oc apply -f ${env.WORKSPACE}/kube/lifecycle/scripts/openshift/networkPolicies.yaml -n "${NAMESPACE}"
        """
    }

    echo 'Deploying with helm'
    // commonModule.generateTLSCertificate()
    echo "Fetch TLS Certificate for ${commonConfig.COMMON_DOMAIN_SUFFIX}"
    commonModule.fetchAndInstallTLSCertificate(commonConfig.COMMON_DOMAIN_SUFFIX);
    commonModule.applyHelmAction(openshiftEnvVars, 'install')
    
    // Configure user that kicked of the deployment to be an admin for it
    sh "oc policy add-role-to-user admin ${commonConfig.COMMON_INSTANCE_OWNER_SHORT} -n ${NAMESPACE}"

    // Run the LDAP/DB configuration task
    commonModule.configureDB_LDAP()

    EXTERNAL_IP = sh (script: """(kubectl get routes -n ${NAMESPACE} | grep dx-deployment-passthrough-${NAMESPACE} |awk '{print \$2}') """ ,   returnStdout: true).trim()
    
    // Determine if acceptance test execution or data setup is required
    executeAcceptance = !("${commonConfig.COMMON_SKIP_ACCEPTANCE_TESTS}".toBoolean())
    executeDataSetup = !("${commonConfig.COMMON_SKIP_DATA_SETUP_VERIFY}".toBoolean())

    // Run the AT and verify preparation
    if (executeDataSetup || executeAcceptance){
        echo 'Preparing data setup and acceptance test execution.'
        // Trying every 30s, for max 120 times, giving 60s settling period after first successful check
        dxPodsCheckReadiness(
            namespace: NAMESPACE,
            lookupInterval: 30,
            lookupTries: 120,
            pendingLimit: 15,
            containerCreateLimit: 15,
            safetyInterval: 60
        )
        jobParams = commonModule.createAcceptanceAndSetupVerifJobParams(openshiftEnvVars.CORE_IMAGE_TAG, NAMESPACE, commonConfig.COMMON_DOMAIN_SUFFIX)
        // Execute acceptance test remote job
        if (executeAcceptance) {
            echo 'Triggering acceptance tests.'
            commonModule.isAcceptanceTestsSuccess(
                openshiftEnvVars.CORE_IMAGE_TAG,
                EXTERNAL_IP,
                true
            )
        }
        // Execute setup and verify remote job
        if (executeDataSetup) {
            echo 'Triggering data verify.'
            commonModule.isSetupAndVerifyDataSuccess(
                openshiftEnvVars.CORE_IMAGE_TAG,
                jobParams,
                'setup'
            )
        }
    }

    // Execute run RTRM remote job
    if ("${commonConfig.COMMON_DAM_RTRM_TESTS}".toBoolean()) {
        echo 'Running DAM RTRM test.'
        commonModule.isRtrmJMeterTestsSuccess(
            openshiftEnvVars.CORE_IMAGE_TAG,
            EXTERNAL_IP,
            false
        )
        commonModule.isRtrmMigrationAcceptanceTestsSuccess(
            openshiftEnvVars.CORE_IMAGE_TAG,
            EXTERNAL_IP,
            false
        )
    }
}

/*
 * Create an environment for connection to a separate on-prem core installation
 */
def createHybridEnvironment() {
    // Configure kubectl to use the target openshift environment
    loginOpenshift()

    // Load openshift default variables, setting the skipPVCreation flag to "false"
    openshiftEnvVars=generateOpenShiftDeploymentVariables(false)

    // Create a namespace in the target cluster
    commonModule.createNamespace()

    // Create TLS Certificate
    // commonModule.generateTLSCertificate()
    echo "Fetch TLS Certificate for ${commonConfig.COMMON_DOMAIN_SUFFIX}"
    commonModule.fetchAndInstallTLSCertificate(commonConfig.COMMON_DOMAIN_SUFFIX);

    // Perform Helm install
    commonModule.applyHelmAction(openshiftEnvVars, 'install')

    // Configure user that kicked of the deployment to be an admin for it
    sh "oc policy add-role-to-user admin ${commonConfig.COMMON_INSTANCE_OWNER_SHORT} -n ${NAMESPACE}"

    // Determine the external IP of the deployment
    EXTERNAL_IP = sh(script: "(kubectl get routes -n ${NAMESPACE} | grep dx-deployment-passthrough-${NAMESPACE} |awk '{print \$2}')",   returnStdout: true).trim()
    echo "EXTERNAL_IP: ${EXTERNAL_IP}"

    // Run acceptance tests
    if(!("${commonConfig.COMMON_SKIP_ACCEPTANCE_TESTS}".toBoolean())){
        // Trying every 30s, for max 120 times, giving 60s settling period after first successful check
        dxPodsCheckReadiness(
            namespace: NAMESPACE,
            lookupInterval: 30,
            lookupTries: 120,
            pendingLimit: 15,
            containerCreateLimit: 15,
            safetyInterval: 60
        )
        commonModule.isAcceptanceTestsSuccess(openshiftEnvVars.CORE_IMAGE_TAG, openshiftEnvVars.DX_DEPLOYMENT_HOST, true)
    }
 }

/*
 * destroy the environment
 */
def destroyEnvironment() {
    // Configure kubectl/oc to use the target cluster
    loginOpenshift()

    // Destroy target environment, pass in required NFS variables
    commonModule.destroy(openshiftConfig.NFS_ACCESS_KEY, openshiftConfig.NFS_SERVER, openshiftConfig.NFS_PATH)

    // If we are running in the AWS cluster, we destroy the loadbalancer
    if (commonConfig.COMMON_KUBE_FLAVOUR == 'openshift') {
        destroyLoadBalancer()
    }
}

/*
 * destroy LoadBalancer
 */
def destroyLoadBalancer(){
    withCredentials([usernamePassword(
        credentialsId: 'aws_credentials',
        passwordVariable: 'AWS_SECRET_ACCESS_KEY',
        usernameVariable: 'AWS_ACCESS_KEY_ID'
    )]) {
        script {
            List<String> loadBalancerList = []
            // Retrieve list of AWS loadbalancers
            loadBalancerList = sh(script: """(aws elb describe-load-balancers --query 'LoadBalancerDescriptions[].LoadBalancerName' --output text --region us-east-2  | xargs -n20 aws elb describe-tags --load-balancer-names --region us-east-2 --query "TagDescriptions[].[(Tags[? Key=='kubernetes.io/service-name' && (starts_with(Value, '$NAMESPACE')) && contains(Value, '$NAMESPACE/' )].Value)[0],LoadBalancerName]" --output text  | grep -v None ) || echo ''""", returnStdout: true).trim().tokenize("\n")
            for (loadBalancer in loadBalancerList) {
                sh """
                    echo "$loadBalancer" |  awk  '{print "Deleting LoadBalancer for "\$1"-"\$2 "in $NAMESPACE." }'
                    aws elb delete-load-balancer --load-balancer-name \$(echo "$loadBalancer" |  awk  '{print \$2}') --region us-east-2
                """
            }
        }
    }
}

/*
 * Routine to cleanup in case of an error
 */
def destroyAfterError() {
    // Configure kubectl/oc to use the target cluster
    loginOpenshift()
    def checkSuccess = fileExists "${workspace}/success.properties"
    if (checkSuccess){
        echo "DX pod is running. Retainining deployment after error."
    } else {
        commonModule.collectData()
        // The destroy method called will remove the PV as well as the OS resources
        commonModule.destroy(openshiftConfig.NFS_ACCESS_KEY, openshiftConfig.NFS_SERVER, openshiftConfig.NFS_PATH)
        // If we are running in the AWS cluster, we destroy the loadbalancer
        if (commonConfig.COMMON_KUBE_FLAVOUR == 'openshift') {
            destroyLoadBalancer()
        }
    }
}

/*
 * Updating the openshift deployment.
 */
def updateEnvironment() {
    // Configure kubectl/oc to use the target cluster
    loginOpenshift()
    // Create the default set of deployment configuration, skipping the PV creation
    openshiftEnvVars = generateOpenShiftDeploymentVariables(true)
    echo "Updating environment ${NAMESPACE} using Helm"
    // Determine URL for Helm chart download
    openshiftEnvVars.HELM_CHARTS_URL = commonModule.determineHelmChartsURL()
    // Select default storageclass for persistent volumes
    openshiftEnvVars.cloudStgclass = openshiftConfig.DEFAULT_STORAGECLASS
    // Perform actual update using Helm
    commonModule.applyHelmAction(openshiftEnvVars, upgrade)
    // Determine the dx passthrough route
    EXTERNAL_IP = sh(
        script: "(kubectl get routes -n ${NAMESPACE} | grep dx-deployment-passthrough-${NAMESPACE} |awk '{print \$2}')" ,
        returnStdout: true
    ).trim()

    // Perform acceptance tests, if the skip parameter is not set to true
    if (commonConfig.COMMON_SKIP_ACCEPTANCE_TESTS != 'true') {
        // Trigger acceptance test run, pass in the core image tag as version reference
        // Add the EXTERNAL_IP as a testing endpoint
        // Set dx-core test execution to true
        commonModule.isAcceptanceTestsSuccess(
            openshiftEnvVars.CORE_IMAGE_TAG,
            EXTERNAL_IP,
            true
        )
    }

    // Perform DAM RTRM testing if parameter is set to true
    if (commonConfig.COMMON_DAM_RTRM_TESTS == 'true') {
        // Trigger the RTRM test cycle, pass in the core image as version reference
        // Add the EXTERNAL_IP as the testing endpoint
        // Set the update-deployment parameter to true
        commonModule.isRtrmJMeterTestsSuccess(
            openshiftEnvVars.CORE_IMAGE_TAG,
            EXTERNAL_IP,
            true
        )
        // Trying every 30s, for max 120 times, giving 60s settling period after first successful check
        dxPodsCheckReadiness(
            namespace: NAMESPACE,
            lookupInterval: 30,
            lookupTries: 120,
            pendingLimit: 15,
            containerCreateLimit: 15,
            safetyInterval: 60
        )
        // Run RTRM Pre Migration and Post migration Tests, pass in the core image as version reference
        // Add the EXTERNAL_IP as the testing endpoint
        // Set the update-deployment parameter to true
        commonModule.isRtrmMigrationAcceptanceTestsSuccess(
            openshiftEnvVars.CORE_IMAGE_TAG,
            EXTERNAL_IP,
            true
        )
    }

    // Execute data verify tests, if skip parameter is not true
    if (commonConfig.COMMON_SKIP_DATA_SETUP_VERIFY != 'true')  {
        // Trying every 30s, for max 120 times, giving 60s settling period after first successful check
        dxPodsCheckReadiness(
            namespace: NAMESPACE,
            lookupInterval: 30,
            lookupTries: 120,
            pendingLimit: 15,
            containerCreateLimit: 15,
            safetyInterval: 60
        )
        // Create Job parameters for the data verify tests
        jobParams = commonModule.createAcceptanceAndSetupVerifJobParams(
            openshiftEnvVars.CORE_IMAGE_TAG,
            NAMESPACE,
            commonConfig.COMMON_DOMAIN_SUFFIX
        )
        // Execute data verify tests
        commonModule.isSetupAndVerifyDataSuccess(
            openshiftEnvVars.CORE_IMAGE_TAG,
            jobParams,
            'verify'
        )
    }
}

/*
 * Updating the openshift hybrid deployment.
 */
def updateHybridEnvironment() {
    loginOpenshift()
    // Login to container image registry
    loginOpenshiftRegistry()
    script {
        openshiftEnvVars = commonModule.determineImageTags()
    }

    // Determine the amount of replicas for core, coming from the openshift config 

    // Determine Helm Chart URL
    openshiftEnvVars.HELM_CHARTS_URL = commonModule.determineHelmChartsURL()
    // Determine deployment host based on the passthrough route
    openshiftEnvVars.DX_DEPLOYMENT_HOST="dx-deployment-passthrough-${NAMESPACE}.${openshiftConfig.DOMAIN}"
    // Select default storageclass for persistent volumes
    openshiftEnvVars.cloudStgclass = openshiftConfig.DEFAULT_STORAGECLASS
    // Perform actual Helm upgrade
    commonModule.applyHelmAction(openshiftEnvVars, 'upgrade')

    // Perform acceptance tests, if the skip parameter is not set to true
    if (commonConfig.COMMON_SKIP_ACCEPTANCE_TESTS != 'true') {
        // Trigger acceptance test run, pass in the core image tag as version reference
        // Add the DX_DEPLOYMENT_HOST as a testing endpoint
        // Set dx-core test execution to true
        commonModule.isAcceptanceTestsSuccess(
            openshiftEnvVars.CORE_IMAGE_TAG,
            openshiftEnvVars.DX_DEPLOYMENT_HOST,
            true
        )
    }

    // Execute data verify tests, if skip parameter is not true
    if (commonConfig.COMMON_SKIP_DATA_SETUP_VERIFY != 'true')  {
        // Trying every 30s, for max 120 times, giving 60s settling period after first successful check
        dxPodsCheckReadiness(
            namespace: NAMESPACE,
            lookupInterval: 30,
            lookupTries: 120,
            pendingLimit: 15,
            containerCreateLimit: 15,
            safetyInterval: 60
        )
        // Create Job parameters for the data verify tests
        jobParams = commonModule.createAcceptanceAndSetupVerifJobParams(
            openshiftEnvVars.CORE_IMAGE_TAG,
            nativeConfig.INSTANCE_NAME,
            commonConfig.COMMON_DOMAIN_SUFFIX
        )
        // Execute data verify tests
        commonModule.isSetupAndVerifyDataSuccess(
            openshiftEnvVars.CORE_IMAGE_TAG,
            jobParams,
            'verify'
        )
    }
}

/*
 * login to OpenShift environment and configure kubectl
 */
def loginOpenshift() {
    echo "Configure openshift env, Check connection to openshift"
    withCredentials([[
        $class: 'UsernamePasswordMultiBinding',
        credentialsId: "$OPENSHIFT_CREDENTIALS_ID",
        usernameVariable: 'USERNAME', 
        passwordVariable: 'PASSWORD'
    ]]) {
        sh """
            mkdir -p oc-client
            (curl -L ${openshiftConfig.OC_CLIENT_URL} > ./oc-client/openshift-oc.tar.gz) > /dev/null 2>&1
            cd oc-client && tar -xvf openshift-oc.tar.gz --strip=1 > /dev/null 2>&1
            export PATH="$PATH:${workspace}/oc-client"
            cd ${workspace}
            chmod -R 755 ./
            KUBECONFIG="openshift-${NAMESPACE}-kubeconfig.yaml" oc login -u=$USERNAME -p=$PASSWORD --server=${openshiftConfig.OC_SERVER_URL} --insecure-skip-tls-verify
        """

        // Adjust the env variable globally, so that the openshift client and kubernetes config are easily accessible
        env.PATH = "$PATH:${workspace}/oc-client"
        env.KUBECONFIG = "${workspace}/openshift-${NAMESPACE}-kubeconfig.yaml"

        // Perform quick test
        sh 'oc status'
    }
}

/*
 * Prepare OpenShift variables for deployment
 */
def generateOpenShiftDeploymentVariables(skipCreatePV) {
    script {
        // Login to CR for image lookup
        loginOpenshiftRegistry()
        // Determine image tags
        openshiftEnvVars = commonModule.determineImageTags()
        // Determine Helm Chart URL
        openshiftEnvVars.HELM_CHARTS_URL = commonModule.determineHelmChartsURL()
        // Select default storageclass for persistent volumes
        openshiftEnvVars.cloudStgclass = openshiftConfig.DEFAULT_STORAGECLASS
    }
    // Creation timestamp for the Persistent Volumes
    openshiftEnvVars.pvCreationTime = new Date().format("yyMMdd-HHmmss")
    // Assign storage classes
    openshiftEnvVars.CORE_STORAGECLASS = openshiftConfig.CORE_STORAGECLASS
    openshiftEnvVars.DAM_STORAGECLASS = openshiftConfig.DAM_STORAGECLASS
    openshiftEnvVars.RS_STORAGECLASS = openshiftConfig.RS_STORAGECLASS
    openshiftEnvVars.dxLoggingStgclass = openshiftConfig.DEFAULT_STORAGECLASS
    openshiftEnvVars.dxTranloggingStgclass = openshiftConfig.DEFAULT_STORAGECLASS
    // Only perform PV creation if asked to do so, update deployments e.g. do not need new PVs
    if (!skipCreatePV) {
        // Create a PV if DAM disable is not true
        if (commonConfig.COMMON_DISABLE_DAM != 'true') {
            // Check if PV name is provided externally, if not we create one
            if (openshiftConfig.DAM_PV_NAME) {
                // Assign given value
                openshiftEnvVars.DAM_PV_NAME = openshiftConfig.DAM_PV_NAME
            } else {
                // Create a new PV and assign result value to environment variables
                openshiftEnvVars.DAM_PV_NAME = commonModule.createPV(
                    openshiftConfig.NFS_ACCESS_KEY,
                    openshiftConfig.NFS_SERVER,
                    openshiftConfig.NFS_PATH,
                    "${NAMESPACE}-dam-${openshiftEnvVars.pvCreationTime}"
                )
            }
        }
        // Create PV for Core if we are not in hybrid mode
        if (!commonConfig.COMMON_HYBRID.toBoolean()) {
            // Check if a PV name is provided externally, if not we create one
            if (openshiftConfig.CORE_PV_NAME) {
                openshiftEnvVars.CORE_PV_NAME = openshiftConfig.CORE_PV_NAME
            } else {
                // Create a new PV and assign result value to environment variables
                openshiftEnvVars.CORE_PV_NAME = commonModule.createPV(
                    openshiftConfig.NFS_ACCESS_KEY,
                    openshiftConfig.NFS_SERVER,
                    openshiftConfig.NFS_PATH,
                    "${NAMESPACE}-core-${openshiftEnvVars.pvCreationTime}"
                )
            }
        }
    }
    // Set the remote search PV name as empty for the deployment, since we don't use a specific one
    openshiftEnvVars.RS_PV_NAME=''
    // Perform special configuration for helm deployments and their corresponding passthrough setups
    openshiftEnvVars.DX_DEPLOYMENT_HOST = "dx-deployment-passthrough-${NAMESPACE}${commonConfig.COMMON_DOMAIN_SUFFIX}"
    // Wait longer for ROSA OpenShift
    if (commonConfig.COMMON_KUBE_FLAVOUR == 'openshift') {
        commonConfig.DX_FRESH_PROBE_RETRIES = '300'
    }    
    echo "Openshift environment variables ${openshiftEnvVars}"
    return openshiftEnvVars
}

/*
 * Retrieve the wildcard certificate from artifactory and deploy to each route
 */
def updateRouteKeys() {
    // Only perform the changes in non NJDC OpenShift
    if (OS_TARGET_ENVIRONMENT != 'openshiftnjdc') {
        sh """
            cd ./kube/lifecycle/scripts/openshift
            curl -L ${COMMON_ARTIFACTORY_TRUSTSTOREURL}/${DOMAIN}/fullchain.cer -o fullchain.cer
            curl -L ${COMMON_ARTIFACTORY_TRUSTSTOREURL}/${DOMAIN}/${DOMAIN}.cer -o ${DOMAIN}.cer
            curl -L ${COMMON_ARTIFACTORY_TRUSTSTOREURL}/${DOMAIN}/${DOMAIN}.key -o ${DOMAIN}.key
            ./applyCertToRoutes.sh ${NAMESPACE} ${DOMAIN}
        """
    }
}

/*
 * Log in to the fitting openshift container image registry
 */
def loginOpenshiftRegistry() {
    if (OS_TARGET_ENVIRONMENT == 'openshiftnjdc') {
        // Login to Openshift NJDC container image registry
        withCredentials([usernamePassword(
            credentialsId: 'njdc_artifactory_credentials',
            passwordVariable: 'password',
            usernameVariable: 'username'
        )]) {
            sh " docker login -u ${username} -p ${password} '${commonConfig.COMMON_IMAGE_REPOSITORY}' "
        }
    } else {
        // Login to AWS ECR
        withCredentials([usernamePassword(
            credentialsId: 'aws_credentials',
            passwordVariable: 'AWS_SECRET_ACCESS_KEY',
            usernameVariable: 'AWS_ACCESS_KEY_ID'
        )]) {
            // Create $HOME/.aws/credentials on each run
            // By this we don't need to run an "aws configure" locally on the agent 
            // and we can change the access creds in Jenkins at any time.
            sh """
                set +x
                mkdir --p $HOME/.aws
                echo "[default]" > $HOME/.aws/credentials
                echo "aws_access_key_id = $AWS_ACCESS_KEY_ID" >> $HOME/.aws/credentials
                echo "aws_secret_access_key = $AWS_SECRET_ACCESS_KEY" >> $HOME/.aws/credentials
                chmod 600 $HOME/.aws/credentials
            """
            sh "aws ecr get-login-password --region us-east-2 | docker login --username AWS --password-stdin '${commonConfig.COMMON_IMAGE_REPOSITORY}'"
        }
    }
}

/* Mandatory return statement on EOF */
return this
