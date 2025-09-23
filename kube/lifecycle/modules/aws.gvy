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
awsConfig = load "${configDirectory}/aws.gvy"
commonModule = load "${moduleDirectory}/common.gvy"

/*
 * Pipeline Module for aws k8s deployment
 * Contains all necessary functions to perform a working aws k8s deployment and teardown
 */

/*
 * create the aws environment
 */
def createEnvironment() {
    loginEKS();
    withCredentials([usernamePassword(credentialsId: 'aws_credentials', passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')]) {
        withEnv(["PATH=$PATH:${workspace}/eks-client", "KUBECONFIG=${workspace}/aws-${NAMESPACE}-kubeconfig.yaml"]){
            awsEnvVars=envVarsEKS("CREATE");
            commonModule.createNamespace();
            echo "Fetch TLS Certificate"
            commonModule.fetchAndInstallTLSCertificate(awsEnvVars.DX_DEPLOYMENT_DOMAIN);
            echo "Deploy DX CORE,Installing HCL Digital Experience components : Experience API, Content Composer and Digital Asset Management."
            /* Check the DX deployment method for the installation */
            commonModule.applyHelmAction(awsEnvVars, 'install')
            commonModule.configureDB_LDAP();
            EXTERNAL_IP= sh (script: """(kubectl get services -n ${NAMESPACE} | grep  LoadBalancer | awk '{print \$4}' )""" ,   returnStdout: true).trim();
            commonModule.createRoute53Record(EXTERNAL_IP,NAMESPACE,commonConfig.COMMON_HOSTED_ZONE,workspace,'CNAME')
             
            executeAcceptance = !("${commonConfig.COMMON_SKIP_ACCEPTANCE_TESTS}".toBoolean())
            executeDataSetup = !("${commonConfig.COMMON_SKIP_DATA_SETUP_VERIFY}".toBoolean())
            if(executeDataSetup || executeAcceptance){
                commonModule.executeBuffer();
                echo "======= INSTANCE NAME: ${NAMESPACE} ======="
                jobParams = commonModule.createAcceptanceAndSetupVerifJobParams(awsEnvVars.CORE_IMAGE_TAG, NAMESPACE, commonConfig.COMMON_DOMAIN_SUFFIX)
            }
            if(executeAcceptance){
                commonModule.isAcceptanceTestsSuccess(awsEnvVars.CORE_IMAGE_TAG,EXTERNAL_IP,true)
            }
            if(executeDataSetup){
                commonModule.isSetupAndVerifyDataSuccess(awsEnvVars.CORE_IMAGE_TAG,jobParams,'setup')
            }
            if("${commonConfig.COMMON_DAM_RTRM_TESTS}".toBoolean()){
                commonModule.isRtrmJMeterTestsSuccess(awsEnvVars.CORE_IMAGE_TAG,EXTERNAL_IP,false)
                commonModule.isRtrmMigrationAcceptanceTestsSuccess(awsEnvVars.CORE_IMAGE_TAG,EXTERNAL_IP,false)
            }
        }
    }
}

/*
 * Create an environment for connection to a separate on-prem core installation
 */
 def createHybridEnvironment() {
    loginEKS();
    withCredentials([usernamePassword(credentialsId: 'aws_credentials', passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')]) {
        withEnv(["PATH=$PATH:${workspace}/eks-client", "KUBECONFIG=${workspace}/aws-${NAMESPACE}-kubeconfig.yaml"]){
            awsEnvVars=envVarsEKS("CREATE");
            commonModule.createNamespace();
            echo "Fetch TLS Certificate"
            commonModule.fetchAndInstallTLSCertificate(awsEnvVars.DX_DEPLOYMENT_DOMAIN);
            echo "Deploy DX CORE,Installing HCL Digital Experience components : Experience API, Content Composer and Digital Asset Management."
            commonModule.applyHelmAction(awsEnvVars, 'install');
            EXTERNAL_IP= sh (script: """(kubectl get services -n ${NAMESPACE} | grep  LoadBalancer | awk '{print \$4}' )""" ,   returnStdout: true).trim();
            echo "EXTERNAL_IP = ${EXTERNAL_IP}"
            commonModule.createRoute53Record(EXTERNAL_IP,"${NAMESPACE}.apps",commonConfig.COMMON_HOSTED_ZONE,workspace,'CNAME')

            if(!("${commonConfig.COMMON_SKIP_ACCEPTANCE_TESTS}".toBoolean())){
                commonModule.isAcceptanceTestsSuccess(awsEnvVars.CORE_IMAGE_TAG, awsEnvVars.DX_DEPLOYMENT_HOST, true)
            }
        }
    }
 }

 /*
 * Update an environment for connection to a separate on-prem core installation
 */
 def updateHybridEnvironment() {
    loginEKS();
    withCredentials([usernamePassword(credentialsId: 'aws_credentials', passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')]) {
        withEnv(["PATH=$PATH:${workspace}/eks-client", "KUBECONFIG=${workspace}/aws-${NAMESPACE}-kubeconfig.yaml"]){
            awsEnvVars = commonModule.determineImageTags();
            awsEnvVars.HELM_CHARTS_URL = commonModule.determineHelmChartsURL();
            awsEnvVars.DX_DEPLOYMENT_HOST="${NAMESPACE}${commonConfig.COMMON_DOMAIN_SUFFIX}"
            awsEnvVars.cloudStgclass='gp2'
            echo "Update DX CORE,Installing HCL Digital Experience components : Experience API, Content Composer and Digital Asset Management."
            echo "Updating with helm"
            commonModule.applyHelmAction(awsEnvVars, 'upgrade');
            EXTERNAL_IP= sh (script: """(kubectl get services -n ${NAMESPACE} | grep  LoadBalancer | awk '{print \$4}' )""" ,   returnStdout: true).trim();
            echo "EXTERNAL_IP = ${EXTERNAL_IP}"
            commonModule.createRoute53Record(EXTERNAL_IP,"${NAMESPACE}.apps",commonConfig.COMMON_HOSTED_ZONE,workspace,'CNAME')

            if(!("${commonConfig.COMMON_SKIP_ACCEPTANCE_TESTS}".toBoolean())){
                commonModule.isAcceptanceTestsSuccess(awsEnvVars.CORE_IMAGE_TAG, awsEnvVars.DX_DEPLOYMENT_HOST, true)
            }
        }
    }
 }


/*
 * destroy the environment
 */
def destroyEnvironment() {
    loginEKS();
    withCredentials([usernamePassword(credentialsId: 'aws_credentials', passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')]) {
        withEnv(["PATH=$PATH:${workspace}/eks-client", "KUBECONFIG=${workspace}/aws-${NAMESPACE}-kubeconfig.yaml"]){
            // The destroy method called will remove the PV as well as the aws resources
            commonModule.destroy(awsConfig.NFS_ACCESS_KEY,awsConfig.NFS_SERVER,'/mnt/storage')
        }
    }
}

/*
 * destroy the environment with all namespaces
 */
def destroyEnvironmentAllNamespaces() {
    loginEKS();
    withCredentials([usernamePassword(credentialsId: 'aws_credentials', passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')]) {
        withEnv(["PATH=$PATH:${workspace}/eks-client", "KUBECONFIG=${workspace}/aws-${NAMESPACE}-kubeconfig.yaml"]){
            // The destroy method called will remove the PV as well as the aws resources
            commonModule.destroyAllNamespaces(awsConfig.NFS_ACCESS_KEY,awsConfig.NFS_SERVER,'/mnt/storage')
        }
    }
}

/*
 * Routine to cleanup in case of an error
 */
def destroyAfterError() {
    loginEKS();
    withCredentials([usernamePassword(credentialsId: 'aws_credentials', passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')]) {
        withEnv(["PATH=$PATH:${workspace}/eks-client", "KUBECONFIG=${workspace}/aws-${NAMESPACE}-kubeconfig.yaml"]){
            def checkSuccess = fileExists "${workspace}/success.properties"
            if (checkSuccess) {
                echo "DX pod is running.  Retainining deployment after error."
            } else {
                commonModule.collectData()
                // The destroy method called will remove the PV as well as the aws resources
                commonModule.destroy(awsConfig.NFS_ACCESS_KEY,awsConfig.NFS_SERVER,'/mnt/storage')
            }
        }
    }
}

/*
 * Stub for updating the environment
 */
def updateEnvironment() {
    loginEKS();
    withCredentials([usernamePassword(credentialsId: 'aws_credentials', passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')]) {
        withEnv(["PATH=$PATH:${workspace}/eks-client", "KUBECONFIG=${workspace}/aws-${NAMESPACE}-kubeconfig.yaml"]){
            awsEnvVars = envVarsEKS("UPDATE");
            /* Check the DX deployment method for the installation */
            echo "Updating environment $NAMESPACE using Helm"
            awsEnvVars.HELM_CHARTS_URL = commonModule.determineHelmChartsURL();
            print "Calling aws helm update"
            commonModule.applyHelmAction(awsEnvVars, 'upgrade');
            EXTERNAL_IP= sh (script: """(kubectl get services -n ${NAMESPACE} | grep  LoadBalancer | awk '{print \$4}' )""" ,   returnStdout: true).trim();
            // Perform the update for the route53 entry after an update, since the host might have changed
            commonModule.createRoute53Record(EXTERNAL_IP,NAMESPACE,commonConfig.COMMON_HOSTED_ZONE,workspace,'CNAME')
            if(!("${commonConfig.COMMON_SKIP_ACCEPTANCE_TESTS}".toBoolean())){
                commonModule.isAcceptanceTestsSuccess(awsEnvVars.CORE_IMAGE_TAG,EXTERNAL_IP,true)
            }
            if("${commonConfig.COMMON_DAM_RTRM_TESTS}".toBoolean()){
                commonModule.isRtrmJMeterTestsSuccess(awsEnvVars.CORE_IMAGE_TAG,EXTERNAL_IP,true)
                commonModule.isRtrmMigrationAcceptanceTestsSuccess(awsEnvVars.CORE_IMAGE_TAG,EXTERNAL_IP,true)
            }

            /*
            * Executing data verify.
            */
            executeDataVerify = !("${commonConfig.COMMON_SKIP_DATA_SETUP_VERIFY}".toBoolean())
            if (executeDataVerify){
                commonModule.executeBuffer(7200);
                jobParams = commonModule.createAcceptanceAndSetupVerifJobParams(awsEnvVars.CORE_IMAGE_TAG, NAMESPACE, commonConfig.COMMON_DOMAIN_SUFFIX)
                commonModule.isSetupAndVerifyDataSuccess(awsEnvVars.CORE_IMAGE_TAG,jobParams, 'verify')
            }
        }
    }
}

/*
 * login to aws environment
 */
def loginEKS() {
    echo "Configure aws env,Check connection to EKS"
    withCredentials([usernamePassword(credentialsId: 'aws_credentials', passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')]) {
        script {
            sh """
                mkdir -p eks-client && cd eks-client
                curl -LO https://storage.googleapis.com/kubernetes-release/release/v${awsConfig.COMMON_KUBE_VERSION}/bin/linux/amd64/kubectl > /dev/null 2>&1
                export PATH="$PATH:${workspace}/eks-client"
                cd ${workspace}
                chmod -R 755 ./
                KUBECONFIG="aws-${NAMESPACE}-kubeconfig.yaml" aws eks update-kubeconfig --name ${awsConfig.CLUSTER_NAME} --region ${awsConfig.CLUSTER_REGION}
                export KUBECONFIG="aws-${NAMESPACE}-kubeconfig.yaml"
                kubectl config current-context
            """
        }
    }
}

/*
 * login to AWS environment
 */
def envVarsEKS(deploymentType) {
    def envVarsEKS = [:];
        withCredentials([usernamePassword(credentialsId: 'aws_credentials', passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')]) {
            script {
                /* We only determine image tags and do an ecr login if we are not deploying from AWS Marketplace */
                if (commonConfig.COMMON_IMAGE_REPOSITORY != '709825985650.dkr.ecr.us-east-1.amazonaws.com/hcl-america'){
                    sh(script: """aws ecr get-login-password --region us-east-2 | docker login --username AWS --password-stdin '${commonConfig.COMMON_IMAGE_REPOSITORY}' """)
                    envVarsEKS = commonModule.determineImageTags();
                }
            }
        }
        /* We only determine Helm chart URL if we are not deploying from AWS Marketplace */
        if (commonConfig.COMMON_IMAGE_REPOSITORY != '709825985650.dkr.ecr.us-east-1.amazonaws.com/hcl-america'){
            envVarsEKS.HELM_CHARTS_URL = commonModule.determineHelmChartsURL();
            envVarsEKS.IMAGE_REPOSITORY="657641368736.dkr.ecr.us-east-2.amazonaws.com"
        } else {
             /* We update repository to AWS Marketplace ECR */
            envVarsEKS.IMAGE_REPOSITORY="709825985650.dkr.ecr.us-east-1.amazonaws.com/hcl-america"
            envVarsEKS.CORE_IMAGE_TAG=""
        }
        envVarsEKS.cloudStgclass='gp2'
        envVarsEKS.pvCreationTime = new Date().format("yyMMdd-HHmmss");
        envVarsEKS.CORE_STORAGECLASS=awsConfig.CORE_STORAGECLASS
        envVarsEKS.DAM_STORAGECLASS=awsConfig.DAM_STORAGECLASS
        envVarsEKS.RS_STORAGECLASS=awsConfig.RS_STORAGECLASS
        envVarsEKS.NFS_SERVER=awsConfig.NFS_SERVER
        envVarsEKS.dxLoggingStgclass='gp2'
        envVarsEKS.dxTranloggingStgclass='gp2'
        /* We create new PV only if deployment type is CREATE. */
        if(deploymentType == "CREATE") {
            if (!"${commonConfig.COMMON_DISABLE_DAM}".toBoolean()) {
                envVarsEKS.DAM_PV_NAME=awsConfig.DAM_PV_NAME?awsConfig.DAM_PV_NAME:commonModule.createPV(awsConfig.NFS_ACCESS_KEY,envVarsEKS.NFS_SERVER,'/mnt/storage',"${NAMESPACE}-dam-${envVarsEKS.pvCreationTime}")
            }
            envVarsEKS.CORE_PV_NAME=awsConfig.CORE_PV_NAME?awsConfig.CORE_PV_NAME:commonModule.createPV(awsConfig.NFS_ACCESS_KEY,envVarsEKS.NFS_SERVER,'/mnt/storage',"${NAMESPACE}-core-${envVarsEKS.pvCreationTime}")
        }
        envVarsEKS.RS_PV_NAME=''
        envVarsEKS.DX_DEPLOYMENT_HOST="${NAMESPACE}${commonConfig.COMMON_DOMAIN_SUFFIX}"
        envVarsEKS.DX_DEPLOYMENT_DOMAIN="${commonConfig.COMMON_DOMAIN_SUFFIX}"
        echo "aws eks environemnt varibles  ${envVarsEKS}"
    return envVarsEKS;
}

/* Mandatory return statement on EOF */
return this
