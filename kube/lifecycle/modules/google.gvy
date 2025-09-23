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

/* Loading other modules for usage */
commonConfig = load "${configDirectory}/common.gvy"
gkeConfig = load "${configDirectory}/google.gvy"
commonModule = load "${moduleDirectory}/common.gvy"

/*
 * Pipeline Module for gke k8s deployment
 * Contains all necessary functions to perform a working gke k8s deployment and teardown
 */

/*
 * create the gke environment
 */
def createEnvironment() {
    loginGKE();
    withEnv(["PATH=$PATH:${workspace}/gke-client","KUBECONFIG=${workspace}/gke-${NAMESPACE}-kubeconfig.yaml"]){
        gkeEnvVars=envVarsGKE(false);
        commonModule.createNamespace();
        echo "Fetch TLS Certificate"
        commonModule.fetchAndInstallTLSCertificate(gkeEnvVars.DX_DEPLOYMENT_DOMAIN);
        echo "Deploy DX CORE,Installing HCL Digital Experience components : Experience API, Content Composer and Digital Asset Management."
        echo "Deploying with helm"
        commonModule.applyHelmAction(gkeEnvVars, 'install')
        echo "Triggering DB Transfer."
        commonModule.configureDB_LDAP();
        EXTERNAL_IP= sh (script: """(kubectl get services -n ${NAMESPACE} | grep  LoadBalancer | awk '{print \$4}' )""" ,   returnStdout: true).trim();
        echo "EXTERNAL_IP: ${EXTERNAL_IP}"
        commonModule.createRoute53Record(EXTERNAL_IP,NAMESPACE,commonConfig.COMMON_HOSTED_ZONE,workspace)

        executeAcceptance = !("${commonConfig.COMMON_SKIP_ACCEPTANCE_TESTS}".toBoolean())
        executeDataSetup = !("${commonConfig.COMMON_SKIP_DATA_SETUP_VERIFY}".toBoolean())
        if(executeDataSetup || executeAcceptance){
            commonModule.executeBuffer();
            echo "======= INSTANCE NAME: ${NAMESPACE} ======="
            jobParams = commonModule.createAcceptanceAndSetupVerifJobParams(gkeEnvVars.CORE_IMAGE_TAG, NAMESPACE, commonConfig.COMMON_DOMAIN_SUFFIX)
        }
        if(executeAcceptance){
            commonModule.isAcceptanceTestsSuccess(gkeEnvVars.CORE_IMAGE_TAG,EXTERNAL_IP,true)
        }
        if(executeDataSetup){
            commonModule.isSetupAndVerifyDataSuccess(gkeEnvVars.CORE_IMAGE_TAG,jobParams,'setup')
        }
        echo "======= DAM rtrm  tests : ${commonConfig.COMMON_DAM_RTRM_TESTS} ======="
        if("${commonConfig.COMMON_DAM_RTRM_TESTS}".toBoolean()){
            commonModule.isRtrmJMeterTestsSuccess(gkeEnvVars.CORE_IMAGE_TAG,EXTERNAL_IP,false)
            commonModule.isRtrmMigrationAcceptanceTestsSuccess(gkeEnvVars.CORE_IMAGE_TAG,EXTERNAL_IP,false)
        }
         echo "======= DAM rtrm perf tests : ${commonConfig.COMMON_DAM_RTRM_PERFORMANCE} ======="
        if("${commonConfig.COMMON_DAM_RTRM_PERFORMANCE}".toBoolean()){
                commonModule.isRtrmJMeterTestsSuccess(gkeEnvVars.CORE_IMAGE_TAG,EXTERNAL_IP,false, true)
        }
    }
}

/*
 * create the hybrid gke environment
 */
def createHybridEnvironment() {
    loginGKE();
    withEnv(["PATH=$PATH:${workspace}/gke-client","KUBECONFIG=${workspace}/gke-${NAMESPACE}-kubeconfig.yaml"]){
        gkeEnvVars=envVarsGKE(false);
        commonModule.createNamespace();
        echo "Fetch TLS Certificate"
        commonModule.fetchAndInstallTLSCertificate(gkeEnvVars.DX_DEPLOYMENT_DOMAIN);
        echo "Deploy DX CORE,Installing HCL Digital Experience components : Experience API, Content Composer and Digital Asset Management."
        echo "Deploying with helm"
        commonModule.applyHelmAction(gkeEnvVars, 'install')
        EXTERNAL_IP= sh (script: """(kubectl get services -n ${NAMESPACE} | grep  LoadBalancer | awk '{print \$4}' )""" ,   returnStdout: true).trim();
        commonModule.createRoute53Record(EXTERNAL_IP,"${NAMESPACE}.apps",commonConfig.COMMON_HOSTED_ZONE,workspace)

        if(!("${commonConfig.COMMON_SKIP_ACCEPTANCE_TESTS}".toBoolean())){
            commonModule.isAcceptanceTestsSuccess(gkeEnvVars.CORE_IMAGE_TAG, gkeEnvVars.DX_DEPLOYMENT_HOST, true)
        }
    }
}

/*
 * Update the hybrid gke environment
 */
def updateHybridEnvironment() {
    loginGKE();
    withEnv(["PATH=$PATH:${workspace}/gke-client","KUBECONFIG=${workspace}/gke-${NAMESPACE}-kubeconfig.yaml"]){
        withCredentials([file(credentialsId: 'gcr_credentials', variable: 'LOGIN_KEY')]) {
            script {
                sh(script: """docker login -u _json_key --password-stdin ${commonConfig.COMMON_IMAGE_REPOSITORY} < ${LOGIN_KEY} """)
                gkeEnvVars = commonModule.determineImageTags();
            }
        }
        gkeEnvVars.HELM_CHARTS_URL = commonModule.determineHelmChartsURL();
        envVarsGKE.DX_DEPLOYMENT_HOST="${NAMESPACE}${commonConfig.COMMON_DOMAIN_SUFFIX}"
        gkeEnvVars.cloudStgclass='standard'
        echo "Update DX CORE,Installing HCL Digital Experience components : Experience API, Content Composer and Digital Asset Management."
        echo "Updating with helm"
        commonModule.applyHelmAction(gkeEnvVars, 'upgrade')
        EXTERNAL_IP= sh (script: """(kubectl get services -n ${NAMESPACE} | grep  LoadBalancer | awk '{print \$4}' )""" ,   returnStdout: true).trim();
        commonModule.createRoute53Record(EXTERNAL_IP,"${NAMESPACE}.apps",commonConfig.COMMON_HOSTED_ZONE,workspace)

        if(!("${commonConfig.COMMON_SKIP_ACCEPTANCE_TESTS}".toBoolean())){
            commonModule.isAcceptanceTestsSuccess(gkeEnvVars.CORE_IMAGE_TAG, gkeEnvVars.DX_DEPLOYMENT_HOST, true)
        }
    }
}

/*
 * destroy the environment
 */
def destroyEnvironment() {
    loginGKE();
    withEnv(["PATH=$PATH:${workspace}/gke-client","KUBECONFIG=${workspace}/gke-${NAMESPACE}-kubeconfig.yaml"]){
        commonModule.destroy('gke_nfs_access_key',gkeConfig.NFS_SERVER?gkeConfig.NFS_SERVER:"$G_GKE_NFS_SERVER",'/nfs/jenkinsnfs')
    }
}

/*
 * Routine to cleanup in case of an error
 */
def destroyAfterError() {
    loginGKE();
    withEnv(["PATH=$PATH:${workspace}/gke-client","KUBECONFIG=${workspace}/gke-${NAMESPACE}-kubeconfig.yaml"]){
        def checkSuccess = fileExists "${workspace}/success.properties"
        if (checkSuccess){
            echo "DX pod is running.  Retainining deployment after error."
        } else {
            commonModule.collectData()
            // The destroy method called will remove the PV as well as the gke resources
            commonModule.destroy('gke_nfs_access_key',gkeConfig.NFS_SERVER?gkeConfig.NFS_SERVER:"$G_GKE_NFS_SERVER",'/nfs/jenkinsnfs')
        }
    }
}

/*
 * Updating the GKE deployment.
 */
def updateEnvironment() {
    // Login to GKE to make sure, our Helm and Kubectl commands work
    loginGKE();
    // We use the default env vars for GKE as our starting point
    // We set the parameter skipCreatePV to true, since we don't need new PVs for an update
    def envVars = envVarsGKE(true)
    // Set correct context for kubectl and Helm using the GKE target kube config
    withEnv(["PATH=$PATH:${workspace}/gke-client","KUBECONFIG=${workspace}/gke-${NAMESPACE}-kubeconfig.yaml"]){
        echo "Updating with helm"
        // Trigger common Helm update function
        commonModule.applyHelmAction(envVars, 'upgrade')
        // Determine external IP for remote jobs to talk to
        EXTERNAL_IP= sh (script: """(kubectl get services -n ${NAMESPACE} | grep  LoadBalancer | awk '{print \$4}' )""" ,   returnStdout: true).trim();
        // Perform the update for the route53 entry after an update, since the host might have changed
        commonModule.createRoute53Record(EXTERNAL_IP,NAMESPACE,commonConfig.COMMON_HOSTED_ZONE,workspace)
        // Trigger acceptance test if not skipped
        if(!("${commonConfig.COMMON_SKIP_ACCEPTANCE_TESTS}".toBoolean())){
            commonModule.isAcceptanceTestsSuccess(envVars.CORE_IMAGE_TAG,EXTERNAL_IP,true)
        }
        // Trigger DAM RTRM tests if enabled
        if("${commonConfig.COMMON_DAM_RTRM_TESTS}".toBoolean()){
            commonModule.isRtrmJMeterTestsSuccess(envVars.CORE_IMAGE_TAG,EXTERNAL_IP,true)
            commonModule.isRtrmMigrationAcceptanceTestsSuccess(envVars.CORE_IMAGE_TAG,EXTERNAL_IP,true)
        }

        /*
        * Executing data verify.
        */
        executeDataVerify = !("${commonConfig.COMMON_SKIP_DATA_SETUP_VERIFY}".toBoolean())
        if (executeDataVerify){
            commonModule.executeBuffer(7200);
            jobParams = commonModule.createAcceptanceAndSetupVerifJobParams(envVarsGKE.CORE_IMAGE_TAG, NAMESPACE, commonConfig.COMMON_DOMAIN_SUFFIX)
            commonModule.isSetupAndVerifyDataSuccess(envVarsGKE.CORE_IMAGE_TAG,jobParams, 'verify')
        }
    }
}

/*
 * login to gke environment
 */
def loginGKE() {
    echo "Configure gke env,Check connection to GKE"
    /***
    once the activate-service-account is created enable below section.
    withCredentials([file(credentialsId: 'gke_credentials', variable: 'GOOGLE_SERVICE_ACCOUNT_KEY')]) {
        gcloud auth activate-service-account --key-file=${GOOGLE_SERVICE_ACCOUNT_KEY}
    }
    **/
    sh """
        mkdir -p gke-client && cd gke-client
        curl -LO https://storage.googleapis.com/kubernetes-release/release/v${gkeConfig.COMMON_KUBE_VERSION}/bin/linux/amd64/kubectl > /dev/null 2>&1
        export PATH="$PATH:${workspace}/gke-client"
        cd ${workspace}
        chmod -R 755 ./
        KUBECONFIG="gke-${NAMESPACE}-kubeconfig.yaml" gcloud container clusters get-credentials ${gkeConfig.CLUSTER_NAME} --region ${gkeConfig.CLUSTER_REGION} --project hcl-gcp-l2com-sofy
        export KUBECONFIG="gke-${NAMESPACE}-kubeconfig.yaml"
        kubectl config current-context
    """
}


/*
 * Prepare the default variables for a GKE deployment
 */
def envVarsGKE(skipCreatePV) {
        // Determine the image tags we use for the GKE deplyoment
        withCredentials([file(credentialsId: 'gcr_credentials', variable: 'LOGIN_KEY')]) {
            script {
                sh(script: """docker login -u _json_key --password-stdin ${commonConfig.COMMON_IMAGE_REPOSITORY} < ${LOGIN_KEY} """)
                envVarsGKE = commonModule.determineImageTags();
            }
        }
        envVarsGKE.HELM_CHARTS_URL = commonModule.determineHelmChartsURL();
        // Some fitting defaults for a GKE deployment
        envVarsGKE.cloudStgclass='standard'
        envVarsGKE.IMAGE_REPOSITORY="us.gcr.io/hcl-gcp-l2com-sofy/dxcontainers"
        envVarsGKE.CORE_STORAGECLASS=gkeConfig.CORE_STORAGECLASS
        envVarsGKE.DAM_STORAGECLASS=gkeConfig.DAM_STORAGECLASS
        envVarsGKE.RS_STORAGECLASS=gkeConfig.RS_STORAGECLASS
        envVarsGKE.NFS_SERVER=gkeConfig.NFS_SERVER?gkeConfig.NFS_SERVER:"$G_GKE_NFS_SERVER"
        envVarsGKE.dxLoggingStgclass='standard'
        envVarsGKE.dxTranloggingStgclass='standard'
        /*
            If we run updates, we do not need PVs to be created, since they already exist
            So we skip this step off creation entirely, as the variables are not required for updates
        */
        if (skipCreatePV) {
            println 'Not creating PVs for GKE this time.'
        } else {
            // define PV creation timestamp
            envVarsGKE.pvCreationTime = new Date().format("yyMMdd-HHmmss") ;
            // Create GKE PV for DAM if required
            if (!"${commonConfig.COMMON_DISABLE_DAM}".toBoolean()) {
                envVarsGKE.DAM_PV_NAME=gkeConfig.DAM_PV_NAME?gkeConfig.DAM_PV_NAME:commonModule.createPV('gke_nfs_access_key',envVarsGKE.NFS_SERVER,'/nfs/jenkinsnfs',"${NAMESPACE}-dam-${envVarsGKE.pvCreationTime}")
            }
            // Create GKE PV for Core
            envVarsGKE.CORE_PV_NAME=gkeConfig.CORE_PV_NAME?gkeConfig.CORE_PV_NAME:commonModule.createPV('gke_nfs_access_key',envVarsGKE.NFS_SERVER,'/nfs/jenkinsnfs',"${NAMESPACE}-core-${envVarsGKE.pvCreationTime}")
            // We don't use a specific PV for RS
            envVarsGKE.RS_PV_NAME=''
        }
        // Extract host settings from the common config
        envVarsGKE.DX_DEPLOYMENT_DOMAIN="${commonConfig.COMMON_DOMAIN_SUFFIX}"
        envVarsGKE.DX_DEPLOYMENT_HOST="${NAMESPACE}${commonConfig.COMMON_DOMAIN_SUFFIX}"
        echo "GKE environment variables: ${envVarsGKE}"
    return envVarsGKE;
}

/* Mandatory return statement on EOF */
return this
