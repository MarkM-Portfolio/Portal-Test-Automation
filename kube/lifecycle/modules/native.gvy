/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2020, 2024. All Rights Reserved. *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

import java.text.SimpleDateFormat
import groovy.transform.Field
import groovy.time.TimeCategory
import java.time.LocalDateTime 
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import groovy.json.JsonOutput

/* Loading other modules for usage */
commonConfig = load "${configDirectory}/common.gvy"
nativeConfig = load "${configDirectory}/native.gvy"

commonModule = load "${moduleDirectory}/common.gvy"
/*
 * Pipeline Module for native k8s deployment
 * Contains all necessary functions to perform a working native k8s deployment and teardown
 */

/*
 * Check Jenkins for all queued cleanup processes and delete the job for the given instance if that exists 
 */
def checkAndDeleteQueuedCleanup() {
    /* Get all queued jobs in Jenkins */
    def q = Jenkins.instance.queue
    
    /* set job and instance name to check for */
    def jobName = "${nativeConfig.NATIVE_KUBE_REMOVAL_JOB}"
    def instanceName = "${nativeConfig.INSTANCE_NAME}"
    
    println "Check for queued job ${jobName} for instance ${instanceName}"

    /* Get all queued jobs in Jenkins */
    q.items.each {
        if (it =~ /${jobName}/) {
            if (it.params =~ /INSTANCE_NAME=${instanceName}/ ) {
                println "Found queued Jenkins job"
                q.cancel(it.task)
                println "Removed job from Jenkins queue"
            }
        }
    }
}

/*
 * Install and run terraform to either create or destroy a plan
 */
def runTerraform(operation, instanceName) {
    // Map for creating the EC2 instance
    sh """
        rm -rf ${workspace}/terraform-work/config/add-route53-record/.terraform
        rm -rf ${workspace}/terraform-work/config/new-ec2-instance/.terraform
    """
    def terraformVarsEC2 = [:]
    terraformVarsEC2.tfvar_instance_name = instanceName
    terraformVarsEC2.tfvar_instance_owner = commonConfig.COMMON_INSTANCE_OWNER
    terraformVarsEC2.tfvar_aws_ec2_instance_type = nativeConfig.INSTANCE_TYPE
    terraformVarsEC2.tfvar_aws_subnet = nativeConfig.AWS_SUBNET_ID
    terraformVarsEC2.tfvar_instance_popo_schedule = nativeConfig.NATIVE_POPO_SCHEDULE
    terraformVarsEC2.tfvar_dedicated_host_id = nativeConfig.DEDICATED_HOST_ID
    terraformVarsEC2.tfvar_instance_area = nativeConfig.INSTANCE_AREA
    def terraformVarsRoute53 = [:]
    if (nativeConfig.DEPLOY_CUSTOM_CA_CERT == 'true') {
        def recordNames = ["${instanceName}${commonConfig.COMMON_DOMAIN_SUFFIX}", "customca.${instanceName}${commonConfig.COMMON_DOMAIN_SUFFIX}"]
        // Pass recod names as JSON string
        terraformVarsRoute53.tfvar_record_names = JsonOutput.toJson(recordNames)
    } else {
        terraformVarsRoute53.tfvar_record_name = "${instanceName}${commonConfig.COMMON_DOMAIN_SUFFIX}"
    }
    terraformVarsRoute53.tfvar_record_type = "A"
    terraformVarsRoute53.tfvar_hosted_zone = commonConfig.COMMON_HOSTED_ZONE

    if (operation == 'apply') {
        // Create the EC2 instance
        terraformVarsEC2 = dxTerraformCreateEc2Instance(terraformVarsEC2)
        terraformVarsEC2.tfvar_vpc_security_groups = "${commonConfig.COMMON_VPC_SECURITY_GROUPS}"
        // Create route53 entry
        terraformVarsRoute53.tfvar_ip_address = terraformVarsEC2.instance_private_ip
        dxTerraformCreateRoute53Entry(terraformVarsRoute53)
        setTTLtags(terraformVarsEC2.instance_id)
        // Return the IP address
        return terraformVarsEC2.instance_private_ip
    } else {
        dxTerraformDestroyEc2Instance(terraformVarsEC2)
        dxTerraformDestroyRoute53Entry(terraformVarsRoute53)
    }
}

// Set TTL tags for aws instance
def setTTLtags(instance_id){
        def awsInstanceProperties = [:]
        def currentTime = System.currentTimeMillis()
        currentTimeAsDate = new Date(currentTime)
        // If the delay hours is 0, we need to go far into the future, since no regular undeploy is planned
        def targetHours = commonConfig.COMMON_NEXT_JOB_DELAY_HOURS
        if (targetHours == 0.0) {
            // Let's use 5 years, that should suffice (24 * 365 * 5)
            targetHours = 43800.0
        }
        def expireTime = currentTime + ((targetHours * 60 * 60 * 1000) as long)
        expiryTimeAsDate = new Date(expireTime)

        def dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss")
        def createdDate = "${dateFormat.format(currentTimeAsDate)}"
        def expiryDate = "${dateFormat.format(expiryTimeAsDate)}"

        awsInstanceProperties.instance_id = instance_id
        awsInstanceProperties.LC_CreationDate = createdDate
        awsInstanceProperties.LC_CreatedBy = commonConfig.COMMON_INSTANCE_OWNER
        awsInstanceProperties.LC_CreatedFrom = G_JENKINS_BUILD_ENV_NAME
        awsInstanceProperties.LC_TimeToLive = expiryDate

        println "awsInstanceProperties------"
        println awsInstanceProperties
        
        dxEC2TtlSetTags(awsInstanceProperties)
}

/*
 * Function to create the control plane
 */
def createControlPlane() {
    configFileProvider([
        configFile(
            fileId: 'test-automation-deployments',
            variable: 'DEPLOY_KEY'
        )
    ]) {
        // If distributed configuration, the CP has a suffix
        def instanceName = nativeConfig.INSTANCE_NAME
        if (commonConfig.ENABLE_DISTRIBUTED_CONFIGURATION == "true") {
            instanceName = "${nativeConfig.INSTANCE_NAME}-cp"
        }
        // Create control plane
        def instanceIp = runTerraform('apply', instanceName)
        // Install kubernetes on the control plane
        installKubernetes(instanceIp)
        // Init cluster
        sh(
            """
                chmod 600 ${DEPLOY_KEY}
                cp $DEPLOY_KEY test-automation-deployments.pem
                ssh -o StrictHostKeyChecking=no -i test-automation-deployments.pem centos@${instanceIp} 'sh /home/centos/native-kube/04-configure-cp.sh ${instanceIp} ${commonConfig.COMMON_HELM_VERSION} ${commonConfig.COMMON_HELM_ARTIFACTORY_URL}'
            """
        )
        // Configure kubectl
        dxKubectlNativeKubeConfig(sshTarget: instanceIp)
        // Get join command and return it
        def joinCommand = sh(script: """
            ssh -o StrictHostKeyChecking=no -i test-automation-deployments.pem centos@${instanceIp} 'sudo kubeadm token create --print-join-command'    
        """, returnStdout: true)
        return joinCommand
    }
}

/*
 * Function to delete the control plane
 */
def deleteControlPlane() {
    // If distributed configuration, the CP has a suffix
    def instanceName = nativeConfig.INSTANCE_NAME
    if (commonConfig.ENABLE_DISTRIBUTED_CONFIGURATION == "true") {
        instanceName = "${nativeConfig.INSTANCE_NAME}-cp"
    }
    // Destroy control plane
    runTerraform('destroy', instanceName)
}

/*
 * Function to delete worker nodes
 */
def deleteWorker(workerNumber) {
    def instanceName = "${nativeConfig.INSTANCE_NAME}-node-${workerNumber}"
    // Destroy worker node
    runTerraform('destroy', instanceName)
}

/*
 * Function to untaint control plane nodes
 */
def untaintControlPlane() {
    sh """
        kubectl taint nodes --all node-role.kubernetes.io/control-plane-
    """
}

/*
 * Function to create worker nodes
 */
def createWorker(workerNumber, joinCommand) {
    def instanceName = "${nativeConfig.INSTANCE_NAME}-node-${workerNumber}"
    // Create worker node
    def instanceIp = runTerraform('apply', instanceName)
    // Install kubernetes on the worker node
    installKubernetes(instanceIp)
    // Run join command
    sh(script: """
        ssh -o StrictHostKeyChecking=no -i test-automation-deployments.pem centos@${instanceIp} 'sudo ${joinCommand}'
    """)
}

/*
 * Installs Kubernetes on the target instance
 */
def installKubernetes(instanceIp) {
    // Deal with a special case if "latest" is provided as Kube version.
    def targetKubeVersion = nativeConfig.COMMON_KUBE_VERSION
    if (nativeConfig.COMMON_KUBE_VERSION == 'latest') {
        targetKubeVersion = ''
    }
     // Copy scripts to target environment and execute setup
    dir("${env.WORKSPACE}/kube/lifecycle/scripts/native/deploy") {
        // Wrap in config file for SSH access to remote machine
        configFileProvider([
            configFile(
                fileId: 'test-automation-deployments',
                variable: 'DEPLOY_KEY'
            )
        ]) {
            // Using instanceIp as the target host
            sh """
                chmod 600 ${DEPLOY_KEY}
                cp $DEPLOY_KEY test-automation-deployments.pem
                chmod 0600 test-automation-deployments.pem
                scp -i test-automation-deployments.pem ./test-automation-deployments.pem centos@${instanceIp}:/home/centos/
                ssh -o StrictHostKeyChecking=no -i test-automation-deployments.pem centos@${instanceIp} 'mkdir -p /home/centos/native-kube/'
                scp -o StrictHostKeyChecking=no -i test-automation-deployments.pem -r ./* centos@${instanceIp}:/home/centos/native-kube
                ssh -o StrictHostKeyChecking=no -i test-automation-deployments.pem centos@${instanceIp} 'chmod +x /home/centos/native-kube/*.sh && sudo sh /home/centos/native-kube/01-setup-prereqs-kube.sh'
                ssh -o StrictHostKeyChecking=no -i test-automation-deployments.pem centos@${instanceIp} 'sh /home/centos/native-kube/02-install-container-runtime.sh && sudo sh /home/centos/native-kube/03-install-kube.sh ${targetKubeVersion}'
            """
            // Transfer SSH key to the target instance
            transferSSHKey(instanceIp)
        }
    }
}

/*
 * Function to perform deployment
 */
def deployEnvironment() {
    // Create the deployment namespace
    println 'Create namespace for deployment'
    commonModule.createNamespace()
    // Install TLS Certificate before deployment
    println 'Install TLS certificate'
    commonModule.fetchAndInstallTLSCertificate(commonConfig.COMMON_DOMAIN_SUFFIX);
    // We have to differentiate between harbor and normal deployments
    // If we are running a harbor deployment, perform specific actions
    def deploymentParameters = [:]

    if (commonConfig.COMMON_IMAGE_REPOSITORY == 'hclcr.io') {
        // Create the image pull secret on the target instance, so we can successfully load harbor images
        commonModule.createHarborImagePullSecret()
        // Harbor does not require us to lookup images beforehand, since they are already defined in the helm chart itself
        commonModule.loginToHarborRepo()
    } else {
        if (commonConfig.COMMON_IMAGE_REPOSITORY != '709825985650.dkr.ecr.us-east-1.amazonaws.com/hcl-america'){
            // Setup deployment parameters, adding image tags
            deploymentParameters = commonModule.determineImageTags()
            // Determine Helm Chart URL
            deploymentParameters.HELM_CHARTS_URL = commonModule.determineHelmChartsURL()
        }
    }
    if (commonConfig.ENABLE_DISTRIBUTED_CONFIGURATION == "true") {
        // Distributed deployments require a different storage class
        deploymentParameters.cloudStgclass = 'nfs-rwo'
        deploymentParameters.rwxStgClass = 'nfs-rwx'
        // Configure Node that will be used for the haproxy,
        // This is the same node that will be used for the IP of metalLB
        def nodeName = sh(
            script: "kubectl get nodes -o wide | grep -v \"control-plane\" | tail -n +2 | head -n 1 | awk -v OFS='\\t\\t' '{print \$1}'",
            returnStdout: true
        ).trim()
        deploymentParameters.haproxyNode = nodeName
    } else {
        // Storage class for native kube environments is always "manual" for now
        deploymentParameters.cloudStgclass = 'manual'
        // Override storage class for RWX
        deploymentParameters.rwxStgClass = 'manual'
    }
    // Configure deployment host
    deploymentParameters.DX_DEPLOYMENT_HOST = "${nativeConfig.INSTANCE_NAME}${commonConfig.COMMON_DOMAIN_SUFFIX}"

    deploymentParameters.DX_INSTANCE_NAME = nativeConfig.INSTANCE_NAME
    // Install Ingress
    if (commonConfig.COMMON_ENABLE_INGRESS == 'true' || nativeConfig.DEPLOY_CUSTOM_CA_CERT == 'true') {
        deployIngress()
    }

    // Call self-signed root CA and cert
    if (nativeConfig.DEPLOY_CUSTOM_CA_CERT == 'true') {
        createRootCA()
    }

    // Deploy DX if enabled
    if (commonConfig.COMMON_DEPLOY_DX == "true") {
        println 'Perform DX Helm install'
        commonModule.applyHelmAction(deploymentParameters, 'install')

        // Trigger DB and LDAP configuration
        println 'Running DB and LDAP configuration.'
        commonModule.configureDB_LDAP()
    }

    // Deploy Leap if enabled
    if (commonConfig.COMMON_DEPLOY_LEAP == "true") {
        println 'Perform Leap Helm install'
        deployLeap(deploymentParameters)
    }

    // Deploy Keycloak if enabled
    if (commonConfig.COMMON_DEPLOY_KEYCLOAK == "true") {
        println 'Perform Keycloak Helm install'
        deployKeycloak("install")
    }

    // Deploy People Service if enabled
    if (commonConfig.COMMON_DEPLOY_PEOPLESERVICE == "true") {
        println 'Perform People Service Helm install'
        deployPeopleService("install")
    }

    // Deploy Liberty Portlet Container if enabled
    if (commonConfig.COMMON_DEPLOY_LPC == "true") {
        println 'Perform Liberty Portlet Container Helm install'
        deployLibertyPortletContainer(deploymentParameters)
    }

    def openSearchStorageClass = 'manual'
    if (commonConfig.ENABLE_DISTRIBUTED_CONFIGURATION == "true") {
        openSearchStorageClass = 'nfs-rwo'
    }

    // Installs opensource OpenSearch
    if (commonConfig.COMMON_DEPLOY_OPENSEARCH == "true" ){
        commonModule.installOpenSearch(
            env.NAMESPACE,
            1,
            openSearchStorageClass,
            commonConfig.COMMON_OPENSEARCH_VERSION,
            nativeConfig.INSTANCE_NAME,
            commonConfig.COMMON_DOMAIN_SUFFIX,
            commonConfig.COMMON_USE_OPENSOURCE_OPENSEARCH
        )
    }

    // Configure route53 entry in clustered configuration
    if (commonConfig.ENABLE_DISTRIBUTED_CONFIGURATION == "true") {
        createDeploymentRoute53Record()
    }
}

/*
 * Deploy LEAP
 */
def deployLeap(deploymentParameters) {
    dir('kube/lifecycle/scripts/native/deploy/leap') {
        // Download the leap helm chart
        def leapUrl = commonModule.determineLeapHelmChartsURL()
        sh """
            curl -s ${leapUrl} --output helm.tgz
            tar zvxf helm.tgz
        """
        // For non artifactory repos, this is sufficient
        def imageRepository = commonConfig.COMMON_IMAGE_REPOSITORY
        // For artifactory repos, we have to build the appropriate repository string
        if (commonConfig.COMMON_IMAGE_REPOSITORY != commonConfig.COMMON_IMAGE_AREA) {
            imageRepository = "${commonConfig.COMMON_IMAGE_REPOSITORY}.${commonConfig.COMMON_IMAGE_AREA}"
        }
        // Replace deployment placeholders
        sh """
            sed -i.bck "s'LEAP_IMAGE_NAME'${commonConfig.COMMON_LEAP_IMAGE_PATH}'g" deploy-values.yaml
            sed -i.bck "s'LEAP_IMAGE_TAG'${deploymentParameters.LEAP_IMAGE_TAG}'g" deploy-values.yaml
            sed -i.bck "s'REPOSITORY_NAME'${imageRepository}'g" deploy-values.yaml
        """
        // Configure service for leap depending on if leap is deployed alone
        if (commonConfig.COMMON_DEPLOY_LEAP == 'true' && commonConfig.COMMON_DEPLOY_DX != 'true') {
            // If leap is deployed stand alone, it will use a loadbalancer service to expose the application
            sh """
                sed -i.bck "s'LEAP_SERVICE_TYPE'LoadBalancer'g" deploy-values.yaml
            """
        } else {
            // If leap is deployed together with DX, it will use ClusterIP
            sh """
                sed -i.bck "s'LEAP_SERVICE_TYPE'ClusterIP'g" deploy-values.yaml
            """
        }

        // Perform helm installation
        sh """

            helm install -n ${env.NAMESPACE} leap-deployment ./hcl-leap-deployment -f ./deploy-values.yaml ${commonModule.getCustomValuesOverride()}
        """

        // Get the deployed custom values from the actual deployment
        sh """
            helm get values leap-deployment -n ${env.NAMESPACE} -o yaml > ./merged-leap-deploy-values.yaml
        """
        // Wrap in config file for SSH access to remote machine
        configFileProvider([
            configFile(
                fileId: 'test-automation-deployments',
                variable: 'DEPLOY_KEY'
            )
        ]) {
            // Using deploymentParameters.DX_DEPLOYMENT_HOST as the target host
            sh """
                chmod 600 ${DEPLOY_KEY}
                scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ./merged-leap-deploy-values.yaml centos@${deploymentParameters.DX_DEPLOYMENT_HOST}:/home/centos/native-kube/leap-deploy-values.yaml
                scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r ./hcl-leap-deployment centos@${deploymentParameters.DX_DEPLOYMENT_HOST}:/home/centos/native-kube/hcl-leap-deployment
            """
        }
    }
}

/*
 * Deploy Liberty Portlet Container
 */
def deployLibertyPortletContainer(deploymentParameters) {
    dir('kube/lifecycle/scripts/native/deploy/lpc') {
        // Download the LPC helm chart
        def lpcUrl = commonModule.determineLPCHelmChartsURL()
        sh """
            curl -s ${lpcUrl} --output helm.tgz
            tar zvxf helm.tgz
        """

        // For non artifactory repos, this is sufficient
        def imageRepository = commonConfig.COMMON_IMAGE_REPOSITORY

        // For artifactory repos, we have to build the appropriate repository string
        if (commonConfig.COMMON_IMAGE_REPOSITORY != commonConfig.COMMON_IMAGE_AREA) {
            imageRepository = "${commonConfig.COMMON_IMAGE_REPOSITORY}.${commonConfig.COMMON_IMAGE_AREA}"
        }

        // Replace deployment placeholders
        sh """
            sed -i.bck "s'LPC_IMAGE_NAME'${commonConfig.COMMON_LPC_IMAGE_PATH}'g" deploy-values.yaml
            sed -i.bck "s'LPC_IMAGE_TAG'${deploymentParameters.LPC_IMAGE_TAG}'g" deploy-values.yaml
            sed -i.bck "s'REPOSITORY_NAME'${imageRepository}'g" deploy-values.yaml
        """

        // Perform helm installation
        sh """
            helm install -n ${env.NAMESPACE} lpc-deployment ./hcl-lpc-deployment -f ./deploy-values.yaml
        """

        // Get the deployed custom values from the actual deployment
        sh """
            helm get values lpc-deployment -n ${env.NAMESPACE} -o yaml > ./merged-lpc-deploy-values.yaml
        """

        // Wrap in config file for SSH access to remote machine
        configFileProvider([
            configFile(
                fileId: 'test-automation-deployments',
                variable: 'DEPLOY_KEY'
            )
        ]) {
            // Using deploymentParameters.DX_DEPLOYMENT_HOST as the target host
            sh """
                chmod 600 ${DEPLOY_KEY}
                scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ./merged-lpc-deploy-values.yaml centos@${deploymentParameters.DX_DEPLOYMENT_HOST}:/home/centos/native-kube/lpc-deploy-values.yaml
                scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r ./hcl-lpc-deployment centos@${deploymentParameters.DX_DEPLOYMENT_HOST}:/home/centos/native-kube/hcl-lpc-deployment
            """

            // If configured to deploy test applications, wait for pod to be up and then do so
            if (commonConfig.COMMON_DEPLOY_LPC_TEST_APPS == "true") {
                println 'Deploying Liberty Portlet Container test applications'

                println "Checking if LPC is up and running before preparing scripts"
                dxPodsCheckReadiness(
                    namespace: commonConfig.NAMESPACE,
                    lookupInterval: 30,
                    lookupTries: 120,
                    pendingLimit: 30,
                    containerCreateLimit: 60,
                    safetyInterval: 60,
                    podFilter: 'lpc-0'
                )

                // Install test applications from Artifactory into running LPC pod
                sshagent(credentials: ['jenkins-git']) {
                    sh """
                        git clone git@git.cwp.pnp-hcl.com:websphere-portal/liberty-portlet-container.git
                    """
                }
                sh """
                    chmod +x liberty-portlet-container/wsrp-tests/deployWSRPTestWARs.sh
                    ./liberty-portlet-container/wsrp-tests/deployWSRPTestWARs.sh
                """

                // Configure DX Core WSRP consumer to use test applications
                sh """
                    kubectl cp ./liberty-portlet-container/wsrp-tests/DeployWSRPTestPages.xml dx-deployment-core-0:/opt/HCL/wp_profile/ -n ${commonConfig.NAMESPACE}
                    kubectl exec -n ${commonConfig.NAMESPACE} dx-deployment-core-0 -- bash -c '/opt/HCL/wp_profile/PortalServer/bin/xmlaccess.sh -user ${commonConfig.COMMON_CUSTOM_WPSADMIN_USER} -password ${commonConfig.COMMON_CUSTOM_WPSADMIN_PASSWORD} -url localhost:10039/${commonConfig.COMMON_CONTEXT_ROOT_PATH}/config -in /opt/HCL/wp_profile/DeployWSRPTestPages.xml -out /opt/HCL/wp_profile/DeployWSRPResults.xml'
                    kubectl exec -n ${commonConfig.NAMESPACE} dx-deployment-core-0 -- bash -c 'cat /opt/HCL/wp_profile/DeployWSRPResults.xml'
                """
            }
        }
    }
}

/*
 * Deploy Keycloak using Helm chart
 */
def deployKeycloak(actionType = "install") {
    def realmInfo = commonModule.getRealmInfo()
    def parameters = [:]
    parameters.IMAGE_PATH = commonConfig.COMMON_HCLDS_KEYCLOAK_IMAGE_PATH
    parameters.IMAGE_TAG_NAME = commonModule.getImageTag(
        commonConfig.COMMON_HCLDS_KEYCLOAK_IMAGE_PATH, 
        commonConfig.COMMON_HCLDS_KEYCLOAK_IMAGE_FILTER
    )
    parameters.HELM_CHART_URL = commonModule.determineKeycloakHelmChartsURL()
    parameters.RELEASE_NAME = "hclds-keycloak"
    parameters.HOST_NAME = "${nativeConfig.INSTANCE_NAME}${commonConfig.COMMON_DOMAIN_SUFFIX}"
    parameters.KEYCLOAK_DB_NAME = "keycloak_db"

    // DX specific parameters
    parameters.DX_OIDC_CLIENT_ID = "${realmInfo.REALM_NAME}-dx-oidc-client"
    parameters.DX_OIDC_CLIENT_SECRET = "${parameters.DX_OIDC_CLIENT_ID}-secret"
    parameters.DX_REDIRECT_URL_1 = "https://${parameters.HOST_NAME}:443/oidcclient/${realmInfo.REALM_NAME}"
    parameters.DX_REDIRECT_URL_2 = "https://${parameters.HOST_NAME}/${commonConfig.COMMON_CONTEXT_ROOT_PATH}/${commonConfig.PERSONALIZED_DX_CORE_PATH}"
    parameters.DX_POST_LOGOUT_REDIRECT_URIS = "https://${parameters.HOST_NAME}/${commonConfig.COMMON_CONTEXT_ROOT_PATH}/${commonConfig.COMMON_DX_CORE_HOME_PATH}"
    parameters.DX_FRONTCHANNEL_LOGOUT_URL = ""

    // People Service specific parameters
    parameters.PEOPLE_SERVICE_OIDC_CLIENT_ID = "${realmInfo.REALM_NAME}-people-service-oidc-client"
    parameters.PEOPLE_SERVICE_OIDC_CLIENT_SECRET = "${parameters.PEOPLE_SERVICE_OIDC_CLIENT_ID}-secret"
    parameters.PEOPLE_SERVICE_REDIRECT_URI_1 = "https://${parameters.HOST_NAME}/dx/api/people/v1/auth/login"
    parameters.PEOPLE_SERVICE_REDIRECT_URI_2 = "https://${parameters.HOST_NAME}/dx/ui/people"
    parameters.PEOPLE_SERVICE_POST_LOGOUT_REDIRECT_URIS = ""
    parameters.PEOPLE_SERVICE_FRONTCHANNEL_LOGOUT_URL = "https://${parameters.HOST_NAME}/dx/api/people/v1/auth/logout"

    // Create database
    commonModule.createPostgreSQLDB(parameters.KEYCLOAK_DB_NAME)

    // Download the Keycloak Helm chart
    sh """
        curl -s ${parameters.HELM_CHART_URL} --output ${parameters.RELEASE_NAME}.tgz
        tar zvxf ${parameters.RELEASE_NAME}.tgz
    """

    // For non artifactory repos, this is sufficient
    def imageRepository = commonConfig.COMMON_IMAGE_REPOSITORY
    // For artifactory repos, we have to build the appropriate repository string
    if (commonConfig.COMMON_IMAGE_REPOSITORY != commonConfig.COMMON_IMAGE_AREA) {
        imageRepository = "${commonConfig.COMMON_IMAGE_REPOSITORY}.${commonConfig.COMMON_IMAGE_AREA}"
    }

    // Replace deployment placeholders
    def customValuesFileName = "keycloak-dx-deploy-values.yaml"
    sh """
        cd ${parameters.RELEASE_NAME}
        cp ${workspace}/kube/lifecycle/scripts/native/deploy/oidc/${customValuesFileName} ./
        
        sed -i "
            s|IMAGE_REGISTRY_NAME|${imageRepository}|g;
            s|IMAGE_REPOSITORY_NAME|${parameters.IMAGE_PATH}|g;
            s|IMAGE_TAG_NAME|${parameters.IMAGE_TAG_NAME}|g;
            s|REALM_NAME|${realmInfo.REALM_NAME}|g;
            s|HOST_NAME|${parameters.HOST_NAME}|g;
            s|DX_OIDC_CLIENT_ID|${parameters.DX_OIDC_CLIENT_ID}|g;
            s|DX_OIDC_CLIENT_SECRET|${parameters.DX_OIDC_CLIENT_SECRET}|g;
            s|DX_REDIRECT_URL_1|${parameters.DX_REDIRECT_URL_1}|g;
            s|DX_REDIRECT_URL_2|${parameters.DX_REDIRECT_URL_2}|g;
            s|DX_POST_LOGOUT_REDIRECT_URIS|${parameters.DX_POST_LOGOUT_REDIRECT_URIS}|g;
            s|DX_FRONTCHANNEL_LOGOUT_URL|${parameters.DX_FRONTCHANNEL_LOGOUT_URL}|g;
            s|PEOPLE_SERVICE_OIDC_CLIENT_ID|${parameters.PEOPLE_SERVICE_OIDC_CLIENT_ID}|g;
            s|PEOPLE_SERVICE_OIDC_CLIENT_SECRET|${parameters.PEOPLE_SERVICE_OIDC_CLIENT_SECRET}|g;
            s|PEOPLE_SERVICE_REDIRECT_URI_1|${parameters.PEOPLE_SERVICE_REDIRECT_URI_1}|g;
            s|PEOPLE_SERVICE_REDIRECT_URI_2|${parameters.PEOPLE_SERVICE_REDIRECT_URI_2}|g;
            s|PEOPLE_SERVICE_POST_LOGOUT_REDIRECT_URIS|${parameters.PEOPLE_SERVICE_POST_LOGOUT_REDIRECT_URIS}|g;
            s|PEOPLE_SERVICE_FRONTCHANNEL_LOGOUT_URL|${parameters.PEOPLE_SERVICE_FRONTCHANNEL_LOGOUT_URL}|g;
            s|POSTGRESQL_DB_HOST|dx-deployment-persistence|g;
            s|POSTGRESQL_DB_NAME|${parameters.KEYCLOAK_DB_NAME}|g;
            s|POSTGRESQL_EXISTING_SECRET_NAME|dx-deployment-persistence-user|g;
            s|POSTGRESQL_EXISTING_SECRET_USER_KEY|username|g;
            s|POSTGRESQL_EXISTING_SECRET_PASSWORD_KEY|password|g;
            s|OPENLDAP_CONNECTION_URL|${realmInfo.USER_FEDERATION_LDAP_CONNECTION_URL}|g;
            s|OPEN_LDAP_BIND_CREDENTIAL|${realmInfo.USER_FEDERATION_LDAP_BIND_CREDENTIAL}|g;
        " ./${customValuesFileName}
    """

    // Perform helm installation
    sh """
        cd ${parameters.RELEASE_NAME}
        helm ${actionType} -n ${commonConfig.NAMESPACE} ${parameters.RELEASE_NAME} . -f ./${customValuesFileName}
    """

    // copy the custom values yaml to the remote instance, so developers can reference it for native kube
    // Wrap in config file for SSH access to remote machine
    configFileProvider([
        configFile(
            fileId: 'test-automation-deployments',
            variable: 'DEPLOY_KEY'
        )
    ]) {
        // Get the deployed custom values from the actual deployment
        sh """
            cd ${parameters.RELEASE_NAME}
            helm get values ${parameters.RELEASE_NAME} -n ${commonConfig.NAMESPACE} -o yaml > ./merged-hclds-keycloak-deploy-values.yaml
            cat ./merged-hclds-keycloak-deploy-values.yaml
        """

        // Using parameters.HOST_NAME as the target host
        sh """
            cd ${parameters.RELEASE_NAME}
            chmod 600 ${DEPLOY_KEY}
            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ./merged-hclds-keycloak-deploy-values.yaml centos@${parameters.HOST_NAME}:/home/centos/native-kube/${actionType}-hclds-keycloak-deploy-values.yaml
            rm ./keycloak-dx-deploy-values.yaml
            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r ./ centos@${parameters.HOST_NAME}:/home/centos/native-kube/${actionType}-hclds-keycloak
        """
    }

    // Wait for Keycloak to be ready
    dxPodsCheckReadiness(
        namespace: commonConfig.NAMESPACE,
        lookupInterval: 30,
        lookupTries: 180,
        pendingLimit: 30,
        containerCreateLimit: 60,
        safetyInterval: 60,
        podFilter: parameters.RELEASE_NAME
    )
}

/*
 * Deploy People Service
 */
 def deployPeopleService(actionType = "install") {
    if (actionType != "install" && actionType != "upgrade") {
        error "Provided invalid actionType > ${actionType} < to Helm."
    }

    def isDXInstalled = commonModule.isHelmChartInstalled("dx-deployment", commonConfig.NAMESPACE)
    
    // Populate necessary parameters
    def parameters = [:]
    parameters.PEOPLE_SERVICE_IMAGE_PATH = commonConfig.COMMON_PEOPLE_SERVICE_IMAGE_PATH
    parameters.PEOPLE_SERVICE_IMAGE_TAG = commonModule.getImageTag(
        commonConfig.COMMON_PEOPLE_SERVICE_IMAGE_PATH, 
        commonConfig.COMMON_PEOPLE_SERVICE_IMAGE_FILTER
    )
    parameters.PEOPLE_SERVICE_HELM_CHART_URL = commonModule.determinePeopleServiceHelmChartsURL()
    parameters.PEOPLE_SERVICE_RELEASE_NAME = "hcl-people-service"
    parameters.HOST_NAME = "${nativeConfig.INSTANCE_NAME}${commonConfig.COMMON_DOMAIN_SUFFIX}"
    parameters.PEOPLE_SERVICE_DB_NAME = "peopledb"
    parameters.PEOPLE_SERVICE_POSTGRESQL_ENABLED = "true"
    parameters.PEOPLE_SERVICE_TYPE = "NodePort"
    parameters.PEOPLE_SERVICE_HOST = "${parameters.HOST_NAME}:30000"
    parameters.PEOPLE_SERVICE_API_CONTEXT_ROOT = isDXInstalled ? "/dx/api/people/v1" : "/people/api/v1"
    parameters.PEOPLE_SERVICE_CLIENT_CONTEXT_ROOT = isDXInstalled ? "/dx/ui/people" : "/people/ui"
    parameters.PEOPLE_SERVICE_INGRESS_ENABLED = "false"
    parameters.PEOPLE_SERVICE_DATA_VOLUME_NAME = getPersistentVolumeName(
        "hcl-people-service-data", 
        commonConfig.NAMESPACE
    )
    parameters.PEOPLE_SERVICE_DATABASE_VOLUME_NAME = getPersistentVolumeName(
        "data-hcl-people-service-postgresql-0", 
        commonConfig.NAMESPACE
    )

    // use external postgresql db if enabled which is used for DX
    if (commonConfig.COMMON_DEPLOY_DX == "true" && commonConfig.COMMON_DISABLE_PERSISTENCE == "false") {
        parameters.PEOPLE_SERVICE_POSTGRESQL_ENABLED = "false"
    }

    if (commonConfig.COMMON_ENABLE_INGRESS == "true") {
        parameters.PEOPLE_SERVICE_TYPE = "ClusterIP"
        parameters.PEOPLE_SERVICE_HOST = "${parameters.HOST_NAME}"
        parameters.PEOPLE_SERVICE_INGRESS_ENABLED = "true"
    }

    def realmInfo = commonModule.getRealmInfo()

    parameters.PEOPLE_SERVICE_AUTHENTICATION_ENABLED = (commonConfig.COMMON_DEPLOY_KEYCLOAK == "true" ? "true" : "false")
    parameters.PEOPLE_SERVICE_OIDC_ISSUER_URL = realmInfo.REALM_URL
    parameters.PEOPLE_SERVICE_OIDC_CLIENT_ID = "${realmInfo.REALM_NAME}-people-service-oidc-client"
    parameters.PEOPLE_SERVICE_OIDC_CLIENT_SECRET = "${parameters.PEOPLE_SERVICE_OIDC_CLIENT_ID}-secret"
    parameters.PEOPLE_SERVICE_REDIRECT_URI = "https://${parameters.PEOPLE_SERVICE_HOST}${parameters.PEOPLE_SERVICE_API_CONTEXT_ROOT}/auth/login"
    parameters.PEOPLE_SERVICE_JWT_SECRET = "${parameters.PEOPLE_SERVICE_OIDC_CLIENT_ID}-jwt-secret"
    
    parameters.PEOPLE_SERVICE_LDAP_ENABLED = (commonConfig.COMMON_DISABLE_OPENLDAP == "false" ? "true" : "false")
    parameters.PEOPLE_SERVICE_LDAP_HOST_URL = realmInfo.USER_FEDERATION_LDAP_CONNECTION_URL
    parameters.PEOPLE_SERVICE_LDAP_BIND_DN = commonConfig.COMMON_LDAP_CONFIG_BIND_DN
    parameters.PEOPLE_SERVICE_LDAP_BIND_PASSWORD = realmInfo.USER_FEDERATION_LDAP_BIND_CREDENTIAL

    // Download the People Service Helm chart
    sh """
        curl -s ${parameters.PEOPLE_SERVICE_HELM_CHART_URL} --output ${parameters.PEOPLE_SERVICE_RELEASE_NAME}.tgz
        tar zvxf ${parameters.PEOPLE_SERVICE_RELEASE_NAME}.tgz
    """
    // For non artifactory repos, this is sufficient
    def imageRepository = commonConfig.COMMON_IMAGE_REPOSITORY
    // For artifactory repos, we have to build the appropriate repository string
    if (commonConfig.COMMON_IMAGE_REPOSITORY != commonConfig.COMMON_IMAGE_AREA) {
        imageRepository = "${commonConfig.COMMON_IMAGE_REPOSITORY}.${commonConfig.COMMON_IMAGE_AREA}"
    }

    dir("${parameters.PEOPLE_SERVICE_RELEASE_NAME}") {
        // Replace deployment placeholders
        def customValuesFileName = "people-service-kube-values.yaml"
        sh """
            cp ${workspace}/kube/lifecycle/scripts/native/deploy/people/${customValuesFileName} ./
            
            sed -i "
                s|PEOPLE_SERVICE_IMAGE_REPOSITORY_NAME|${imageRepository}|g;
                s|PEOPLE_SERVICE_IMAGE_TAG|${parameters.PEOPLE_SERVICE_IMAGE_TAG}|g;
                s|PEOPLE_SERVICE_IMAGE_PATH|${parameters.PEOPLE_SERVICE_IMAGE_PATH}|g;
                s|PEOPLE_SERVICE_TYPE|${parameters.PEOPLE_SERVICE_TYPE}|g;
                s|PEOPLE_SERVICE_POSTGRESQL_ENABLED|${parameters.PEOPLE_SERVICE_POSTGRESQL_ENABLED}|g;
                s|PEOPLE_SERVICE_POSTGRESQL_DB_HOST|dx-deployment-persistence-headless-svc|g;
                s|PEOPLE_SERVICE_POSTGRESQL_DB_NAME|${parameters.PEOPLE_SERVICE_DB_NAME}|g;
                s|PEOPLE_SERVICE_POSTGRESQL_EXISTING_SECRET_NAME|dx-deployment-persistence-user|g;
                s|PEOPLE_SERVICE_POSTGRESQL_EXISTING_SECRET_USER_KEY|username|g;
                s|PEOPLE_SERVICE_POSTGRESQL_EXISTING_SECRET_PASSWORD_KEY|password|g;
                s|PEOPLE_SERVICE_HOST|${parameters.PEOPLE_SERVICE_HOST}|g;
                s|PEOPLE_SERVICE_API_CONTEXT_ROOT|${parameters.PEOPLE_SERVICE_API_CONTEXT_ROOT}|g;
                s|PEOPLE_SERVICE_CLIENT_CONTEXT_ROOT|${parameters.PEOPLE_SERVICE_CLIENT_CONTEXT_ROOT}|g;
                s|PEOPLE_SERVICE_INGRESS_ENABLED|${parameters.PEOPLE_SERVICE_INGRESS_ENABLED}|g;
                s|PEOPLE_SERVICE_DATA_VOLUME_NAME|${parameters.PEOPLE_SERVICE_DATA_VOLUME_NAME}|g;
                s|PEOPLE_SERVICE_DATABASE_VOLUME_NAME|${parameters.PEOPLE_SERVICE_DATABASE_VOLUME_NAME}|g;
                s|PEOPLE_SERVICE_AUTHENTICATION_ENABLED|${parameters.PEOPLE_SERVICE_AUTHENTICATION_ENABLED}|g;
                s|PEOPLE_SERVICE_OIDC_ISSUER_URL|${parameters.PEOPLE_SERVICE_OIDC_ISSUER_URL}|g;
                s|PEOPLE_SERVICE_OIDC_CLIENT_ID|${parameters.PEOPLE_SERVICE_OIDC_CLIENT_ID}|g;
                s|PEOPLE_SERVICE_OIDC_CLIENT_SECRET|${parameters.PEOPLE_SERVICE_OIDC_CLIENT_SECRET}|g;
                s|PEOPLE_SERVICE_REDIRECT_URI|${parameters.PEOPLE_SERVICE_REDIRECT_URI}|g;
                s|PEOPLE_SERVICE_JWT_SECRET|${parameters.PEOPLE_SERVICE_JWT_SECRET}|g;
                s|PEOPLE_SERVICE_LDAP_ENABLED|${parameters.PEOPLE_SERVICE_LDAP_ENABLED}|g;
                s|PEOPLE_SERVICE_LDAP_HOST_URL|${parameters.PEOPLE_SERVICE_LDAP_HOST_URL}|g;
                s|PEOPLE_SERVICE_LDAP_BIND_DN|${parameters.PEOPLE_SERVICE_LDAP_BIND_DN}|g;
                s|PEOPLE_SERVICE_LDAP_BIND_PASSWORD|${parameters.PEOPLE_SERVICE_LDAP_BIND_PASSWORD}|g;
            " ./${customValuesFileName}
        """

        // Check for action type if it is install or upgrade
        println "Preparing action > ${actionType} < with Helm."
        sh """
            cat ./${customValuesFileName}
            helm ${actionType} -n ${commonConfig.NAMESPACE} ${parameters.PEOPLE_SERVICE_RELEASE_NAME} . -f ./${customValuesFileName} ${commonModule.getCustomValuesOverride()}
        """

        // copy the custom values yaml to the remote instance, so developers can reference it for native kube
        // Wrap in config file for SSH access to remote machine
        configFileProvider([
            configFile(
                fileId: 'test-automation-deployments',
                variable: 'DEPLOY_KEY'
            )
        ]) {
            // Get the deployed custom values from the actual deployment
            sh """
                helm get values ${parameters.PEOPLE_SERVICE_RELEASE_NAME} -n ${commonConfig.NAMESPACE} -o yaml > ./merged-people-service-deploy-values.yaml
                cat ./merged-people-service-deploy-values.yaml
            """
            // Using parameters.HOST_NAME as the target host
            sh """
                chmod 600 ${DEPLOY_KEY}
                scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ./merged-people-service-deploy-values.yaml centos@${parameters.HOST_NAME}:/home/centos/native-kube/${actionType}-people-service-deploy-values.yaml
                rm ./${customValuesFileName}
                scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r ./ centos@${parameters.HOST_NAME}:/home/centos/native-kube/${actionType}-people-service
            """
        }
    }

    // Wait for People service to be ready
    dxPodsCheckReadiness(
        namespace: commonConfig.NAMESPACE,
        lookupInterval: 30,
        lookupTries: 180,
        pendingLimit: 30,
        containerCreateLimit: 60,
        safetyInterval: 60,
        podFilter: parameters.PEOPLE_SERVICE_RELEASE_NAME
    )
}

/*
 * Get Persistent Volume name for specified pvc name and namespace
 */
def getPersistentVolumeName(pvcName, namespace) {
    def pvName = ""
    try {
        pvName = sh(
            script: "kubectl get pvc ${pvcName} -n ${namespace} -o jsonpath='{.spec.volumeName}'", 
            returnStdout: true
        ).trim()
    } catch (Exception e) {
        println "Error while getting Persistent Volume name for PVC ${pvcName} in namespace ${namespace}"
    }
    return pvName
}

/*
 * Create Kubernetes Cluster
 */
def createCluster() {
    // Prepare terraform
    /* Install terraform in the current workspace. Terraform abstracts the AWS access and handles resource lifecycles and deployments */
    dxTerraformInstall()
    // Create control plane node
    def joinCommand = createControlPlane()
    // Create worker nodes
    if (commonConfig.ENABLE_DISTRIBUTED_CONFIGURATION == "true") {
        for (int i = 0; i < commonConfig.DISTRIBUTED_NODE_COUNT.toInteger(); i++) {
            createWorker(i, joinCommand)
        }
    } else {
        // Just make the control plane node a worker node as well
        untaintControlPlane()
    }
    // Configure kubernetes cluster networking
    sh """
        kubectl apply -f ${workspace}/kube/lifecycle/scripts/native/deploy/artifacts/calico.yaml
    """
    // Check the readiness of the nodes 
    sh """
        kubectl get nodes
        sh ${workspace}/kube/lifecycle/scripts/native/deploy/05-nodecheck.sh
    """
    // Deploy metallb
    sh """
        kubectl apply -f ${workspace}/kube/lifecycle/scripts/native/deploy/artifacts/metallb.yaml
    """
    // Wait for metallb to be ready
    dxPodsCheckReadiness(namespace: 'metallb-system', lookupTries: 100, lookupInterval: 10)
    // Build metallb config based off the environents nodes
    if (commonConfig.ENABLE_DISTRIBUTED_CONFIGURATION == "true") {
        // Chose the first node in the list that is not the control-plane
        sh """
            # Define the yaml file
            yaml_file="${workspace}/kube/lifecycle/scripts/native/deploy/artifacts/metallb-config.yaml"

            # Execute the command and read its output line by line
            kubectl get nodes -o wide | grep -v \"control-plane\" | tail -n +2 | head -n 1 | awk -v OFS='\\t\\t' '{print \$6}' | while read -r line
            do
                # Use sed to insert the line into the yaml file at the desired location
                insertValue="\\ \\ -\\ \$line-\$line"
                sed -i "/addresses:/a \$insertValue" "\$yaml_file"
            done
        """
    } else {
        // It's only one node, so just use the first one
        sh """
            # Define the yaml file
            yaml_file="${workspace}/kube/lifecycle/scripts/native/deploy/artifacts/metallb-config.yaml"

            # Execute the command and read its output line by line
            kubectl get nodes -o wide | tail -n +2 | head -n 1 | awk -v OFS=' \\t\\t' '{print \$6}' | while read -r line
            do
                # Use sed to insert the line into the yaml file at the desired location
                insertValue="\\ \\ -\\ \$line-\$line"
                sed -i "/addresses:/a \$insertValue" "\$yaml_file"
            done        
        """
    }
    // Apply the metallb config
    sh """
        echo "Metallb config:"
        cat ${workspace}/kube/lifecycle/scripts/native/deploy/artifacts/metallb-config.yaml
        kubectl apply -f ${workspace}/kube/lifecycle/scripts/native/deploy/artifacts/metallb-config.yaml
    """
    // Print all pods in the cluster
    sh """
        kubectl get pods --all-namespaces
    """
    // Install the metrics server
    sh """
        curl -L -o metrics.yaml https://github.com/kubernetes-sigs/metrics-server/releases/download/v0.7.1/components.yaml
        sed -i.bck "/kubelet-use-node-status-port/i \\ \\ \\ \\ \\ \\ \\ \\ - --kubelet-insecure-tls" metrics.yaml
        kubectl apply -f metrics.yaml
    """
    // Configure persistent volumes
    if (commonConfig.ENABLE_DISTRIBUTED_CONFIGURATION == "true") {
        configureNFS()
    } else {
        configureLocalPV()
    }
}

/*
 * Configure the NFS Server and deploy NFS provisioner
 * We use the control plane node as NFS server
 * Not the most performant solution, but it works for now
 */
def configureNFS() {
    println 'Configure NFS persistent volumes'
    // Configure NFS Server on the CP
    configFileProvider([
        configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
    ]) {
        sh """
            cp $DEPLOY_KEY test-automation-deployments.pem
            chmod 0600 test-automation-deployments.pem

            scp -i test-automation-deployments.pem -o StrictHostKeyChecking=no ${workspace}/kube/lifecycle/scripts/native/deploy/06-configure-nfs-server.sh centos@${nativeConfig.INSTANCE_NAME}-cp${commonConfig.COMMON_DOMAIN_SUFFIX}:~/
            ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${nativeConfig.INSTANCE_NAME}-cp${commonConfig.COMMON_DOMAIN_SUFFIX} 'chmod +x ~/06-configure-nfs-server.sh && sudo ~/06-configure-nfs-server.sh'
        """
    }
    def nfsValuesRwxFilePath = "${workspace}/kube/lifecycle/scripts/native/deploy/artifacts/nfs-provisioner-rwx-values.yaml"
    def nfsValuesRwoFilePath = "${workspace}/kube/lifecycle/scripts/native/deploy/artifacts/nfs-provisioner-rwo-values.yaml"
    // Deploy NFS provisioner with Helm
    sh """
        kubectl create namespace nfs
        helm repo add nfs-subdir-external-provisioner https://kubernetes-sigs.github.io/nfs-subdir-external-provisioner/
        helm install nfs-rwo -n nfs nfs-subdir-external-provisioner/nfs-subdir-external-provisioner -f ${nfsValuesRwoFilePath} --set nfs.server=${nativeConfig.INSTANCE_NAME}-cp${commonConfig.COMMON_DOMAIN_SUFFIX}
        helm install nfs-rwx -n nfs nfs-subdir-external-provisioner/nfs-subdir-external-provisioner -f ${nfsValuesRwxFilePath} --set nfs.server=${nativeConfig.INSTANCE_NAME}-cp${commonConfig.COMMON_DOMAIN_SUFFIX}
    """
}

/*
 * Configure local node persistent volumes
 * Used in non-distributed configurations
 */
def configureLocalPV() {
    println 'Configure local persistent volumes'
    configFileProvider([
        configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
    ]) {
        sh """
            cp $DEPLOY_KEY test-automation-deployments.pem
            chmod 0600 test-automation-deployments.pem

            ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${nativeConfig.INSTANCE_NAME}${commonConfig.COMMON_DOMAIN_SUFFIX} 'chmod +x ~/native-kube/06-configure-local-pvs.sh && ~/native-kube/06-configure-local-pvs.sh'
        """
    }
}

/*
 * Creates a route53 record through which the deployment will be accessible
 * Uses the loadbalancer IP, since that one is our entrypont
 * We cannot use a fixed node IP, since we have multiple nodes
 */
def createDeploymentRoute53Record() {
    sh """
        rm -rf ${workspace}/terraform-work/config/add-route53-record/.terraform
    """
    // Get the external_ip of the LoadBalancer Service
    def externalIp = sh(
        script: "kubectl get svc -n dxns | grep LoadBalancer | awk '{print \$4}'",
        returnStdout: true
    ).trim()
    // Create the Route53 record
    def terraformVarsRoute53 = [:]
    terraformVarsRoute53.tfvar_record_name = "${nativeConfig.INSTANCE_NAME}${commonConfig.COMMON_DOMAIN_SUFFIX}"
    terraformVarsRoute53.tfvar_record_type = "A"
    terraformVarsRoute53.tfvar_hosted_zone = commonConfig.COMMON_HOSTED_ZONE
    terraformVarsRoute53.tfvar_ip_address = externalIp
    dxTerraformCreateRoute53Entry(terraformVarsRoute53)
}

/*
 * Deletes the Route53 record
 */
def deleteDeploymentRoute53Record() {
    sh """
        rm -rf ${workspace}/terraform-work/config/add-route53-record/.terraform
    """
    // Delete the Route53 record
    def terraformVarsRoute53 = [:]
    terraformVarsRoute53.tfvar_record_name = "${nativeConfig.INSTANCE_NAME}${commonConfig.COMMON_DOMAIN_SUFFIX}"
    terraformVarsRoute53.tfvar_record_type = "A"
    terraformVarsRoute53.tfvar_hosted_zone = commonConfig.COMMON_HOSTED_ZONE
    dxTerraformDestroyRoute53Entry(terraformVarsRoute53)
}

/*
 * Deletes the whole cluster, works for distributed and non-distributed configurations
 */
def deleteCluster() {
    // Prepare terraform
    /* Install terraform in the current workspace. Terraform abstracts the AWS access and handles resource lifecycles and deployments */
    dxTerraformInstall()
    // Destroy the cluster
    deleteControlPlane()
    // Destroy worker nodes
    if (commonConfig.ENABLE_DISTRIBUTED_CONFIGURATION == "true") {
        // Cast the DISTRIBUTED_NODE_COUNT of worker nodes to an integer
        for (int i = 0; i < commonConfig.DISTRIBUTED_NODE_COUNT.toInteger(); i++) {
            deleteWorker(i)
        }
        deleteDeploymentRoute53Record()
    }
}

/*
 * Create the native kube environment
 */
def createEnvironment() {
    createCluster();

    if (commonConfig.PLAIN_KUBERNETES == "false") {
        deployEnvironment()
    }

   // checking if native kube has all pods up and running
    else {
        dxPodsCheckReadiness(namespace: 'kube-system', lookupTries: 100, lookupInterval: 10)
        dxPodsCheckReadiness(namespace: 'metallb-system', lookupTries: 100, lookupInterval: 10)
    }
}





/*
 * Create the native kube hybrid environment
 */
def createHybridEnvironment() {
    createCluster()
    deployEnvironment()

    /* Run acceptance tests unless disabled */
    if (!("${commonConfig.COMMON_SKIP_ACCEPTANCE_TESTS}".toBoolean())){
        /* Give DX time to start before running tests */
        sleep(600)
        if ((commonConfig.COMMON_CONTEXT_ROOT_PATH != "wps") ||
            (commonConfig.COMMON_PERSONALIZED_DX_CORE_PATH != "myportal") ||
            (commonConfig.COMMON_DX_CORE_HOME_PATH != "portal")) {
            /* Give extra time if a context root change needs to be configured */
            sleep(900)
        }
        commonModule.isAcceptanceTestsSuccess(tags.CORE_IMAGE_TAG, "${nativeConfig.INSTANCE_NAME}${commonConfig.COMMON_DOMAIN_SUFFIX}", true)
    }
}

/*
 * Destroy the native kube environment
 */
def destroyEnvironment() {
    checkAndDeleteQueuedCleanup()
    if (("${nativeConfig.IS_NJDC_DEPLOYMENT}".toBoolean())){
        echo "Destroying environment on existing NJDC system ${nativeConfig.NJDC_INSTANCE_IP}"
        removeNativeKubeFromNJDC()
    } else {
        deleteCluster()
    }
}

/*
 * Routine to cleanup in case of an error
 */
def destroyAfterError() {
    dir ("${workspace}") {
        env.FILESEP = "_"
        env.KUBE_FLAVOUR = "${commonConfig.COMMON_KUBE_FLAVOUR}"
        withCredentials([
        usernamePassword(credentialsId: "${nativeConfig.AWS_CREDENTIALS_ID}", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID'),
        ]) {
            configFileProvider([
                configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
            ]) {
                sh """
                    if [[ -z \$(ls | grep AWS_TMP.TXT) ]]; then \$(aws sts get-session-token --duration-seconds 900 > AWS_TMP.TXT); fi
                    export AWS_ACCESS_KEY_ID=\$(cat AWS_TMP.TXT | grep AccessKeyId | awk -F: '{print \$2}' | sed 's/^ //g; s/\"//g; s/,//g')
                    export AWS_SECRET_ACCESS_KEY=\$(cat AWS_TMP.TXT | grep SecretAccessKey | awk -F: '{print \$2}' | sed 's/^ //g; s/\"//g; s/,//g')

                    cp $DEPLOY_KEY test-automation-deployments.pem
                    chmod 0600 test-automation-deployments.pem

                    mkdir -p ./data
                    echo export KUBE_FLAVOUR="${commonConfig.COMMON_KUBE_FLAVOUR}" > sshenv
                    echo export NATIVE_KUBE_TYPE="${nativeConfig.NATIVE_KUBE_TYPE}" >> sshenv
                    echo export JOB_BASE_NAME="${JOB_BASE_NAME}" >> sshenv
                    echo export BUILD_ID="${BUILD_ID}" >> sshenv
                    echo export NAMESPACE="dxns" >> sshenv
                    echo export WORKSPACE="." >> sshenv
                    echo export AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID}" >> sshenv
                    echo export AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY}" >> sshenv

                    echo Native Kube Type is $NATIVE_KUBE_TYPE

                    scp -i test-automation-deployments.pem -o StrictHostKeyChecking=no sshenv centos@${INSTANCE_NAME}${commonConfig.COMMON_DOMAIN_SUFFIX}:~/.ssh/environment

                    scp -i test-automation-deployments.pem -o StrictHostKeyChecking=no ${workspace}/kube/lifecycle/scripts/common/collect-failed-deployment-data.sh ${workspace}/kube/lifecycle/scripts/common/upload-failed-deployment-data.sh centos@${INSTANCE_NAME}${commonConfig.COMMON_DOMAIN_SUFFIX}:~/

                    ssh -tt -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${INSTANCE_NAME}${commonConfig.COMMON_DOMAIN_SUFFIX} 'sudo yum install -y zip; . ~/.ssh/environment; chmod +x ~/collect-failed-deployment-data.sh; ~/./collect-failed-deployment-data.sh'

                    ssh -tt -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${INSTANCE_NAME}${commonConfig.COMMON_DOMAIN_SUFFIX} 'if [[ -f ~/data/${JOB_BASE_NAME}${FILESEP}${BUILD_ID}${FILESEP}${KUBE_FLAVOUR}.zip ]]; then . ~/.ssh/environment; chmod +x ~/upload-failed-deployment-data.sh; ~/./upload-failed-deployment-data.sh;fi'

                    """
                }
         }
    }
    // We remove the EC2 instance and all resources that have been created with it
    installAndRunTerraform('destroy')
}

/*
 * Update the environment
 */
def updateEnvironment() {
    // Connect to the remote instance via kubectl
    dir(env.WORKSPACE) {
        dxKubectlNativeKubeConfig(sshTarget: "${nativeConfig.INSTANCE_NAME}${commonConfig.COMMON_DOMAIN_SUFFIX}")
    }

    // Install Ingress
    if (commonConfig.COMMON_ENABLE_INGRESS == 'true' || nativeConfig.DEPLOY_CUSTOM_CA_CERT == 'true') {
        deployIngress()
    }

    def deploymentParameters = [:]
    // Setup deployment parameters, adding image tags
    deploymentParameters = commonModule.determineImageTags()
    // Determine Helm Chart URL
    deploymentParameters.HELM_CHARTS_URL = commonModule.determineHelmChartsURL() 

    // Determine host
    deploymentParameters.DX_DEPLOYMENT_HOST = "${nativeConfig.INSTANCE_NAME}${commonConfig.COMMON_DOMAIN_SUFFIX}"

    // Perform update action on DX if enabled
    if (commonConfig.COMMON_DEPLOY_DX == "true" ) {
        // Perform update action
        commonModule.applyHelmAction(deploymentParameters, "upgrade")
    }

    // Upgrade Keycloak if enabled
    if (commonConfig.COMMON_DEPLOY_KEYCLOAK == "true") {
        println 'Perform HCLDS Keycloak Helm upgrade'
        // Check if Keycloak is already deployed or not. If not we should install it.
        def isKeycloakHelmChartInstalled = commonModule.isHelmChartInstalled("hclds-keycloak", commonConfig.NAMESPACE)
        deployKeycloak(isKeycloakHelmChartInstalled ? "upgrade" : "install")
    }

     // Upgrade People Service if enabled
     // Check if People Service is already deployed or not. If not we should install it.
    if (commonConfig.COMMON_DEPLOY_PEOPLESERVICE == "true") {
        println 'Perform People Service Helm upgrade'
        def isPeopleServiceHelmChartInstalled = commonModule.isHelmChartInstalled("hcl-people-service", commonConfig.NAMESPACE)
        deployPeopleService(isPeopleServiceHelmChartInstalled ? "upgrade" : "install")
    }

    /*
    * Executing data verify.
    */
    executeDataVerify = !("${commonConfig.COMMON_SKIP_DATA_SETUP_VERIFY}".toBoolean())
    if (executeDataVerify){
        commonModule.executeBuffer(7200);
        jobParams = commonModule.createAcceptanceAndSetupVerifJobParams(deploymentParameters.CORE_IMAGE_TAG, nativeConfig.INSTANCE_NAME, commonConfig.COMMON_DOMAIN_SUFFIX)
        commonModule.isSetupAndVerifyDataSuccess(deploymentParameters.CORE_IMAGE_TAG, jobParams, 'verify')
    }

    /* Run acceptance tests unless disabled */
    if (!("${commonConfig.COMMON_SKIP_ACCEPTANCE_TESTS}".toBoolean())){
        /* Give DX time to update and restart before running tests */
        commonModule.executeBuffer(2400);
        commonModule.isAcceptanceTestsSuccess(deploymentParameters.CORE_IMAGE_TAG, "${nativeConfig.INSTANCE_NAME}${commonConfig.COMMON_DOMAIN_SUFFIX}", true)
    }
}

/*
 * Update a hybrid environment (mostly just call the usual method, logic is in the shell scripts)
 */
def updateHybridEnvironment() {
    nativeConfig.INSTANCE_NAME = env.NAMESPACE
    updateEnvironment()
}

/*
 * Remove and clean up native kube from NJDC
 */
def removeNativeKubeFromNJDC() {
    dir("${workspace}/kube/lifecycle/scripts/native/backup") {
    configFileProvider([
            configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
    ]) {
        /* Extract PEM file */
        sh """
            cp $DEPLOY_KEY test-automation-deployments.pem
            chmod 0600 test-automation-deployments.pem
        """

        /* Execute kube remove and clean up commands */
        sh """
            ssh -o StrictHostKeyChecking=no -i test-automation-deployments.pem centos@${nativeConfig.NJDC_INSTANCE_IP} 'sudo kubeadm reset -f && rm -rf ~/.kube/ && if [[ \$(docker images -q) != "" ]]; then docker rmi `docker images -q`; fi'
        """            
        }
    }
}

/*
 * Validate an SSH key
 */
def validateSSHKey() {
    if (nativeConfig.PUBLIC_KEY && nativeConfig.PUBLIC_KEY != '') {
        // Put the content of the PUBLIC_KEY variable into a file for validation
        sh """
            echo ${nativeConfig.PUBLIC_KEY} > this.pub
        """
        // Use the ssh-keygen tool to verify that the PUBLIC_KEY is valid. If not, exit.
        keyValidation = sh(
            script: 'ssh-keygen -l -f ./this.pub',
            returnStdout: true
        ).trim()
        // Check command output to get the validation result
        if (keyValidation.contains('is not a public key file')) {
            error('The public key you have provided is NOT valid. Exiting.')
        }
    }
}

/*
 * Transfer the SSH key to the remote native kube instance
 */
def transferSSHKey(targetInstance) {
    if (nativeConfig.PUBLIC_KEY && nativeConfig.PUBLIC_KEY != '') {
        configFileProvider([
            configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
        ]) {
            // Add the PUBLIC_KEY to the list of authorized keys in the target environment using SSH
            sh """
                chmod 600 ${DEPLOY_KEY}
                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} \
                centos@${targetInstance} \
                'echo ${nativeConfig.PUBLIC_KEY} >> ~/.ssh/authorized_keys'
            """
        }
    }
}

/*
 * Deploy NGINX Ingress
 */
def deployIngress() {
    println "Deploying Nginx Ingress"
    def yamlPath = "${workspace}/kube/lifecycle/scripts/native/deploy/artifacts/ingress"
    // Add the helm repo for nginx ingress
    sh """
        helm upgrade --install ingress-nginx ingress-nginx \
        --repo https://kubernetes.github.io/ingress-nginx \
        --namespace ingress-nginx --create-namespace
    """

    sh "kubectl delete -A ValidatingWebhookConfiguration ingress-nginx-admission"

    sh "sed -i.bck \"s'HOST_PLACEHOLDER'${nativeConfig.INSTANCE_NAME}${commonConfig.COMMON_DOMAIN_SUFFIX}'g\" ${yamlPath}/nginx-ingress-dx-and-leap-route.yaml"
    sh "kubectl apply -f ${yamlPath}/nginx-ingress-dx-and-leap-route.yaml -n ${commonConfig.NAMESPACE}"
}

/*
 * Create self-signed root CA and cert
 */
def createRootCA() {
    println "Creating Root CA and self signed server certificate"
    // Add the helm repo for nginx ingress
    sh """
      openssl req -nodes -newkey rsa:2048 -keyout customCACert.key -out customCACert.csr -subj "/C=GB/ST=London/L=London/O=Global Security/OU=IT Department/CN=example.com"
      openssl x509 -req -sha256 -days 365 -in customCACert.csr -signkey customCACert.key -out customCACert.pem
      openssl ecparam -out customServer.key -name prime256v1 -genkey
      openssl req -new -sha256 -key customServer.key -out customServer.csr -subj "/C=GB/ST=London/L=London/O=Global Security/OU=IT Department/CN=customca.${nativeConfig.INSTANCE_NAME}${commonConfig.COMMON_DOMAIN_SUFFIX}"
      openssl x509 -req -in customServer.csr -CA  customCACert.pem -CAkey customCACert.key -CAcreateserial -out customServer.crt -days 365 -sha256
      openssl x509 -in customServer.crt -text -noout
      kubectl -n ${NAMESPACE} create secret generic custom-ca-cert --from-file=./customCACert.pem
      kubectl -n ${NAMESPACE} create secret tls custom-signed-server-cert --cert=./customServer.crt --key=./customServer.key
    """

    def yamlPath = "${workspace}/kube/lifecycle/scripts/native/deploy/artifacts/ingress"
    sh "sed -i.bck \"s'HOST_PLACEHOLDER'customca.${nativeConfig.INSTANCE_NAME}${commonConfig.COMMON_DOMAIN_SUFFIX}'g\" ${yamlPath}/custom-ca-ingress.yaml"
    sh "kubectl apply -f ${yamlPath}/custom-ca-ingress.yaml -n ${commonConfig.NAMESPACE}"
}

/* Mandatory return statement on EOF */
return this
