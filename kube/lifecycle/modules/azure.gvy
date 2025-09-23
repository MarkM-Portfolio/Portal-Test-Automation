/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2021, 2024. All Rights Reserved.       *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

/* Loading other modules for usage */
commonConfig = load "${configDirectory}/common.gvy"
aksConfig = load "${configDirectory}/azure.gvy"
commonModule = load "${moduleDirectory}/common.gvy"

/*
 * Pipeline Module for aks k8s deployment
 * Contains all necessary functions to perform a working aks k8s deployment and teardown
 */

/*
 * create the aks environment
 */
def createEnvironment() {
    loginAKS();
    withEnv(["PATH=$PATH:${workspace}/aks-client", "KUBECONFIG=${workspace}/aks-${NAMESPACE}-kubeconfig.yaml"]){
        aksEnvVars=envVarsAKS("CREATE");
        commonModule.createNamespace();
        echo "Fetch TLS Certificate"
        commonModule.fetchAndInstallTLSCertificate(aksEnvVars.DX_DEPLOYMENT_DOMAIN);
        echo "Deploy DX CORE, Installing HCL Digital Experience components: Experience API, Content Composer, and Digital Asset Management."
        echo "Deploying with helm"
        commonModule.applyHelmAction(aksEnvVars, 'install')
        commonModule.configureDB_LDAP();
        EXTERNAL_IP= sh (script: """(kubectl get services -n ${NAMESPACE} | grep  LoadBalancer | awk '{print \$4}' )""" ,   returnStdout: true).trim();
        commonModule.createRoute53Record(EXTERNAL_IP,NAMESPACE,commonConfig.COMMON_HOSTED_ZONE,workspace)

        executeAcceptance = !("${commonConfig.COMMON_SKIP_ACCEPTANCE_TESTS}".toBoolean())
        executeDataSetup = !("${commonConfig.COMMON_SKIP_DATA_SETUP_VERIFY}".toBoolean())
        configurePenTestEnv = "${commonConfig.COMMON_PEN_TEST_CONFIG_ENABLED}".toBoolean()

        if(executeDataSetup || executeAcceptance){
            commonModule.executeBuffer();
            echo "======= INSTANCE NAME: ${NAMESPACE} ======="
            jobParams = commonModule.createAcceptanceAndSetupVerifJobParams(aksEnvVars.CORE_IMAGE_TAG, NAMESPACE, commonConfig.COMMON_DOMAIN_SUFFIX)
        }

        if(executeAcceptance){
            commonModule.isAcceptanceTestsSuccess(aksEnvVars.CORE_IMAGE_TAG,EXTERNAL_IP,true)
        }
        if(executeDataSetup){
            commonModule.isSetupAndVerifyDataSuccess(aksEnvVars.CORE_IMAGE_TAG,jobParams,'setup')
        }
        if("${commonConfig.COMMON_DAM_RTRM_TESTS}".toBoolean()){
            commonModule.isRtrmJMeterTestsSuccess(aksEnvVars.CORE_IMAGE_TAG,EXTERNAL_IP,false)
            commonModule.isRtrmMigrationAcceptanceTestsSuccess(aksEnvVars.CORE_IMAGE_TAG,EXTERNAL_IP,false)
        }
        println "Execute pen test environment: ${configurePenTestEnv}"
        if(configurePenTestEnv){
            commonModule.configurePenTestEnv(aksEnvVars)
        }
    }
}

/*
 * Create an environment for connection to a separate on-prem core installation
 */
 def createHybridEnvironment() {
    loginAKS();
    withEnv(["PATH=$PATH:${workspace}/aks-client", "KUBECONFIG=${workspace}/aks-${NAMESPACE}-kubeconfig.yaml"]){
        aksEnvVars=envVarsAKS("CREATE");
        commonModule.createNamespace();
        echo "Fetch TLS Certificate"
        commonModule.fetchAndInstallTLSCertificate(aksEnvVars.DX_DEPLOYMENT_DOMAIN);
        echo "Deploy DX CORE,Installing HCL Digital Experience components : Experience API, Content Composer and Digital Asset Management."
        echo "Deploying with helm"
        commonModule.applyHelmAction(aksEnvVars, 'install');
        EXTERNAL_IP= sh (script: """(kubectl get services -n ${NAMESPACE} | grep  LoadBalancer | awk '{print \$4}' )""" ,   returnStdout: true).trim();
        echo "EXTERNAL_IP = ${EXTERNAL_IP}"
        commonModule.createRoute53Record(EXTERNAL_IP,"${NAMESPACE}.apps",commonConfig.COMMON_HOSTED_ZONE,workspace)

        if(!("${commonConfig.COMMON_SKIP_ACCEPTANCE_TESTS}".toBoolean())){
            commonModule.isAcceptanceTestsSuccess(aksEnvVars.CORE_IMAGE_TAG, aksEnvVars.DX_DEPLOYMENT_HOST, true)
        }
    }
}

/*
 * Update the hybrid aks environment
 */
def updateHybridEnvironment() {
    /* create the aks environment using helm : Deploy DX CORE, Installing HCL Digital Experience components : Experience API, Content Composer and Digital Asset Management.*/
    loginAKS();
    withEnv(["PATH=$PATH:${workspace}/aks-client","KUBECONFIG=${workspace}/aks-${NAMESPACE}-kubeconfig.yaml"]){
        withCredentials([usernamePassword(credentialsId: 'acr_credentials_HCLSW_AZRNP', passwordVariable: 'ACR_PASSWORD', usernameVariable: 'ACR_USERNAME')]) {
            script {
                sh(script: """echo "${ACR_PASSWORD}" | docker login -u ${ACR_USERNAME} ${commonConfig.COMMON_IMAGE_REPOSITORY} --password-stdin """)
                aksEnvVars = commonModule.determineImageTags();
            }
        }
        aksEnvVars.HELM_CHARTS_URL = commonModule.determineHelmChartsURL();
        aksEnvVars.DX_DEPLOYMENT_HOST="${NAMESPACE}.apps${commonConfig.COMMON_DOMAIN_SUFFIX}"
        aksEnvVars.cloudStgclass='managed-premium'
        echo "Update DX CORE,Installing HCL Digital Experience components : Experience API, Content Composer and Digital Asset Management."
        echo "Updating with helm"
        commonModule.applyHelmAction(aksEnvVars, 'upgrade')
        EXTERNAL_IP= sh (script: """(kubectl get services -n ${NAMESPACE} | grep  LoadBalancer | awk '{print \$4}' )""" ,   returnStdout: true).trim();
        commonModule.createRoute53Record(EXTERNAL_IP,"${NAMESPACE}.apps",commonConfig.COMMON_HOSTED_ZONE,workspace)

        if(!("${commonConfig.COMMON_SKIP_ACCEPTANCE_TESTS}".toBoolean())){
            commonModule.isAcceptanceTestsSuccess(aksEnvVars.CORE_IMAGE_TAG, aksEnvVars.DX_DEPLOYMENT_HOST, true)
        }
    }
}


/*
 * destroy the environment
 */
def destroyEnvironment() {
    loginAKS();
    withEnv(["PATH=$PATH:${workspace}/aks-client", "KUBECONFIG=${workspace}/aks-${NAMESPACE}-kubeconfig.yaml"]){
        // The destroy method called will remove the PV as well as the aks resources
        commonModule.destroy('aks_nfs_server3',aksConfig.NFS_SERVER,'/nfs/jenkinsnfs')
    }
}

/*
 * Routine cleanup in case of an error
 */
def destroyAfterError() {
    loginAKS();
    withEnv(["PATH=$PATH:${workspace}/aks-client", "KUBECONFIG=${workspace}/aks-${NAMESPACE}-kubeconfig.yaml"]){
        def checkSuccess = fileExists "${workspace}/success.properties"
        if (checkSuccess){
            echo "DX pod is running.  Retainining deployment after error."
        } else {
            commonModule.collectData()
            // The destroy method called will remove the PV as well as the aks resources
            commonModule.destroy('aks_nfs_server3',aksConfig.NFS_SERVER,'/nfs/jenkinsnfs')
        }
    }
}

/*
 * Stub for updating the environment
 */
def updateEnvironment() {
    loginAKS();
    withEnv(["PATH=$PATH:${workspace}/aks-client", "KUBECONFIG=${workspace}/aks-${NAMESPACE}-kubeconfig.yaml"]){
        aksEnvVars=envVarsAKS("UPDATE");
        echo "Updating DX deployment"
        aksEnvVars.HELM_CHARTS_URL = commonModule.determineHelmChartsURL();
        echo "Updating environment ${NAMESPACE} using Helm"
        commonModule.applyHelmAction(aksEnvVars, 'upgrade')
        EXTERNAL_IP= sh (script: """(kubectl get services -n ${NAMESPACE} | grep  LoadBalancer | awk '{print \$4}' )""" ,   returnStdout: true).trim();
        // Perform the update for the route53 entry after an update, since the host might have changed
        commonModule.createRoute53Record(EXTERNAL_IP,NAMESPACE,commonConfig.COMMON_HOSTED_ZONE,workspace)
        if(!("${commonConfig.COMMON_SKIP_ACCEPTANCE_TESTS}".toBoolean())){
            commonModule.isAcceptanceTestsSuccess(aksEnvVars.CORE_IMAGE_TAG,EXTERNAL_IP,true)
        }
        if("${commonConfig.COMMON_DAM_RTRM_TESTS}".toBoolean()){
            commonModule.isRtrmJMeterTestsSuccess(aksEnvVars.CORE_IMAGE_TAG,EXTERNAL_IP,true)
            commonModule.isRtrmMigrationAcceptanceTestsSuccess(aksEnvVars.CORE_IMAGE_TAG,EXTERNAL_IP,true)
        }

        /*
        * Executing data verify.
        */
        executeDataVerify = !("${commonConfig.COMMON_SKIP_DATA_SETUP_VERIFY}".toBoolean())
        if (executeDataVerify){
            commonModule.executeBuffer(7200);
            jobParams = commonModule.createAcceptanceAndSetupVerifJobParams(aksEnvVars.CORE_IMAGE_TAG, NAMESPACE, commonConfig.COMMON_DOMAIN_SUFFIX)
            commonModule.isSetupAndVerifyDataSuccess(aksEnvVars.CORE_IMAGE_TAG,jobParams, 'verify')
        }
    }
}

/*
 * login to aks environment
 */
def loginAKS() {
    echo "Configure aks env,Check connection to AKS"
    withCredentials([usernamePassword(
        credentialsId: 'aks_service_principal',
        usernameVariable: 'username',
        passwordVariable: 'password'
    )]) {
        sh """
            mkdir -p aks-client && cd aks-client
            curl -LO https://storage.googleapis.com/kubernetes-release/release/v${aksConfig.COMMON_KUBE_VERSION}/bin/linux/amd64/kubectl > /dev/null 2>&1
            export PATH="$PATH:${workspace}/aks-client"
            cd ${workspace}
            chmod -R 755 ./
            az login --service-principal --tenant ${aksConfig.TENANT} --username ${username} --password ${password}
            az account set --subscription ${SUBSCRIPTION}
            az aks get-credentials --resource-group ${RESOURCE_GROP} --name  ${aksConfig.CLUSTER_NAME} --file aks-${NAMESPACE}-kubeconfig.yaml
            KUBECONFIG="aks-${NAMESPACE}-kubeconfig.yaml"
            export KUBECONFIG
            kubectl config current-context
        """
    }
}

/*
 * login to AKS environment
 */
def envVarsAKS(deploymentType) {
        envVarsAKS = commonModule.determineImageTags();
        envVarsAKS.HELM_CHARTS_URL = commonModule.determineHelmChartsURL();
        envVarsAKS.cloudStgclass='managed-premium'
        envVarsAKS.pvCreationTime = new Date().format("yyMMdd-HHmmss") ;
        envVarsAKS.IMAGE_REPOSITORY="dxcontainers2.azurecr.io"
        envVarsAKS.CORE_STORAGECLASS=aksConfig.CORE_STORAGECLASS
        envVarsAKS.DAM_STORAGECLASS=aksConfig.DAM_STORAGECLASS
        envVarsAKS.RS_STORAGECLASS=aksConfig.RS_STORAGECLASS
        envVarsAKS.NFS_SERVER=aksConfig.NFS_SERVER
        envVarsAKS.dxLoggingStgclass='managed-premium'
        envVarsAKS.dxTranloggingStgclass='managed-premium'
        /* We create new PV only if deployment type is CREATE. */
        if(deploymentType == "CREATE") {
            if (!"${commonConfig.COMMON_DISABLE_DAM}".toBoolean()) {
                envVarsAKS.DAM_PV_NAME=aksConfig.DAM_PV_NAME?aksConfig.DAM_PV_NAME:commonModule.createPV('aks_nfs_server3',envVarsAKS.NFS_SERVER,'/nfs/jenkinsnfs',"${NAMESPACE}-dam-${envVarsAKS.pvCreationTime}")
            }
            envVarsAKS.CORE_PV_NAME=aksConfig.CORE_PV_NAME?aksConfig.CORE_PV_NAME:commonModule.createPV('aks_nfs_server3',envVarsAKS.NFS_SERVER,'/nfs/jenkinsnfs',"${NAMESPACE}-core-${envVarsAKS.pvCreationTime}")
        }
        envVarsAKS.RS_PV_NAME=''
        envVarsAKS.DX_DEPLOYMENT_DOMAIN="${commonConfig.COMMON_DOMAIN_SUFFIX}"
        envVarsAKS.DX_DEPLOYMENT_HOST="${NAMESPACE}${commonConfig.COMMON_DOMAIN_SUFFIX}"
        echo "aks eks environemnt varibles  ${envVarsAKS}"
    return envVarsAKS;
}

/* Mandatory return statement on EOF */
return this
