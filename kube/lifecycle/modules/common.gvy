/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2021, 2024. All Rights Reserved. *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

ARTIFACT_SERVER_URL_PREFIX = "https://artifactory.cwp.pnp-hcl.com/artifactory/"
commonConfig = load "${configDirectory}/common.gvy"

/*
 * Pipeline Module for all kubernetes flavours
 * Contains functions that affect all deployments
 */

/*
 * Retrieves an image tag for a target image, based on image path and filter
 * Target repository will be determined by the commonConfig settings
 */
def getImageTag(imagePath, imageFilter) {
    def foundImage = ""
    // For some repositories (non-artifactory) the COMMON_IMAGE_REPOSITORY and COMMON_IMAGE_AREA are the same
    // We call the lookup function differently in that case
    if (commonConfig.COMMON_IMAGE_REPOSITORY == commonConfig.COMMON_IMAGE_AREA) {
        // Handle for all non artifactory repos
        foundImage = dxLatestImageTagExtract(
            imageArea: commonConfig.COMMON_IMAGE_AREA, 
            imagePath: imagePath, 
            imageFilter: imageFilter
        )
    } else {
        // Handle for artifactory based repositories
        foundImage = dxLatestImageTagExtract(
            imageArea: commonConfig.COMMON_IMAGE_AREA,
            repositoryProject: commonConfig.COMMON_IMAGE_REPOSITORY,
            imagePath: imagePath,
            imageFilter: imageFilter
        )
    }
    if (foundImage == "" || foundImage == null) {
        println "Unable to determine a matching image tag for ${imagePath} - ${imageFilter}"
    }
    return foundImage
}

/*
 * Determines image tags using the shared library function
 */
def determineImageTags() {
    def tags = [:]
    // For non master level deployments, we use the individual image filters to determine the image tags
    if (commonConfig.COMMON_MASTER_DEPLOYMENT_LEVEL == 'NA') {
        tags.CORE_IMAGE_TAG = getImageTag(
            commonConfig.COMMON_CORE_IMAGE_PATH,
            commonConfig.COMMON_CORE_IMAGE_FILTER
        )
        tags.DAM_PLUGIN_GOOGLE_VISION_IMAGE_TAG = getImageTag(
            commonConfig.COMMON_DAM_PLUGIN_GOOGLE_VISION_IMAGE_PATH,
            commonConfig.COMMON_DAM_PLUGIN_GOOGLE_VISION_IMAGE_FILTER
        )
        tags.RINGAPI_IMAGE_TAG = getImageTag(
            commonConfig.COMMON_RINGAPI_IMAGE_PATH,
            commonConfig.COMMON_RINGAPI_IMAGE_FILTER
        )
        tags.CC_IMAGE_TAG = getImageTag(
            commonConfig.COMMON_CC_IMAGE_PATH,
            commonConfig.COMMON_CC_IMAGE_FILTER
        )
        tags.DAM_IMAGE_TAG = getImageTag(
            commonConfig.COMMON_DAM_IMAGE_PATH,
            commonConfig.COMMON_DAM_IMAGE_FILTER
        )
        tags.IMGPROC_IMAGE_TAG = getImageTag(
            commonConfig.COMMON_IMGPROC_IMAGE_PATH,
            commonConfig.COMMON_IMGPROC_IMAGE_FILTER
        )
        tags.DAM_KALTURA_PLUGIN_IMAGE_TAG = getImageTag(
            commonConfig.COMMON_DAM_KALTURA_PLUGIN_IMAGE_PATH,
            commonConfig.COMMON_DAM_KALTURA_PLUGIN_IMAGE_FILTER
        )
        tags.LDAP_IMAGE_TAG = getImageTag(
            commonConfig.COMMON_LDAP_IMAGE_PATH,
            commonConfig.COMMON_LDAP_IMAGE_FILTER
        )
        tags.RS_IMAGE_TAG = getImageTag(
            commonConfig.COMMON_RS_IMAGE_PATH,
            commonConfig.COMMON_RS_IMAGE_FILTER,
        )
        tags.RUNTIME_CONTROLLER_IMAGE_TAG = getImageTag(
            commonConfig.COMMON_RUNTIME_CONTROLLER_IMAGE_PATH,
            commonConfig.COMMON_RUNTIME_CONTROLLER_IMAGE_FILTER
        )
        tags.PERSISTENCE_CONNECTION_POOL_IMAGE_TAG = getImageTag(
            commonConfig.COMMON_PERSISTENCE_CONNECTION_POOL_IMAGE_PATH,
            commonConfig.COMMON_PERSISTENCE_CONNECTION_POOL_IMAGE_FILTER
        )
        tags.PERSISTENCE_NODE_IMAGE_TAG = getImageTag(
            commonConfig.COMMON_PERSISTENCE_NODE_IMAGE_PATH,
            commonConfig.COMMON_PERSISTENCE_NODE_IMAGE_FILTER
        )
        tags.PERSISTENCE_METRICS_EXPORTER_IMAGE_TAG = getImageTag(
            commonConfig.COMMON_PERSISTENCE_METRICS_EXPORTER_IMAGE_PATH,
            commonConfig.COMMON_PERSISTENCE_METRICS_EXPORTER_IMAGE_FILTER
        )
        tags.HAPROXY_IMAGE_TAG = getImageTag(
            commonConfig.COMMON_HAPROXY_IMAGE_PATH,
            commonConfig.COMMON_HAPROXY_IMAGE_FILTER
        )
        tags.LOGGING_SIDECAR_DIFF_IMAGE_TAG = getImageTag(
            commonConfig.COMMON_LOGGING_SIDECAR_DIFF_IMAGE_PATH,
            commonConfig.COMMON_LOGGING_SIDECAR_DIFF_IMAGE_FILTER
        )
        tags.LICENSE_MANAGER_IMAGE_TAG = getImageTag(
            commonConfig.COMMON_LICENSE_MANAGER_IMAGE_PATH,
            commonConfig.COMMON_LICENSE_MANAGER_IMAGE_FILTER
        )
        tags.PREREQS_CHECKER_IMAGE_TAG = getImageTag(
            commonConfig.COMMON_PREREQS_CHECKER_IMAGE_PATH,
            commonConfig.COMMON_PREREQS_CHECKER_IMAGE_FILTER
        )

        if (commonConfig.COMMON_KUBE_FLAVOUR == 'native' && commonConfig.COMMON_DEPLOY_LEAP == "true") {
            tags.LEAP_IMAGE_TAG = getImageTag(
                commonConfig.COMMON_LEAP_IMAGE_PATH,
                commonConfig.COMMON_LEAP_IMAGE_FILTER
            )
        }

        if (commonConfig.COMMON_KUBE_FLAVOUR == 'native' && commonConfig.COMMON_DEPLOY_LPC == "true") {
            tags.LPC_IMAGE_TAG = getImageTag(
                commonConfig.COMMON_LPC_IMAGE_PATH,
                commonConfig.COMMON_LPC_IMAGE_FILTER
            )
        }
    } else {
        // For master level deployments, we use a dedicated lookup logic
        tags = determineMasterImageTags()
    }

    println tags
    return tags
}


/**
Returns the image build date of a core image, based on an imageTag that has been passed in
Input string could look like this: v95_CF200_20211118-200044_rohan_develop_6196d9c4
Output will be 20211118
*/
def getCoreImageBuildDate(String imageTag) {
    // First we split the imageTag between date and time the timestamp and take everything of the first group
    // v95_CF200_20211118
    // Then we split this remaining string by underscores and take the third item, being the creation date
    // 20211118
    // We remove the last two digits, since we do not care about the days (allows for some room between builds)
    // 202111
    return imageTag.split('-')[0].split('_')[2].substring(0,6)
}

/*
 * Get the URL to the helm charts to be used in deployment
 */
def determineHelmChartsURL() {
    echo "HELM CHART FILTER : ${commonConfig.COMMON_HELM_CHARTS_FILTER}"
    def chartFilter = commonConfig.COMMON_HELM_CHARTS_FILTER
    // If we use a master deployment level, we can derive the helm chart filter from that
    if (commonConfig.COMMON_MASTER_DEPLOYMENT_LEVEL != 'NA') {
        chartFilter = determineMasterImageFilter()
    }
    /* Get correct artifactory path  */
    artifactoryChartsListURL = "${ARTIFACT_SERVER_URL_PREFIX}list/${commonConfig.COMMON_HELM_CHARTS_AREA}"
    HELM_CHARTS_AREA = "${commonConfig.COMMON_HELM_CHARTS_AREA}"
    if (chartFilter.matches("(.*)rivendell(.*)|(.*)master(.*)|(.*)release(.*)")){
        HELM_CHARTS_AREA="${commonConfig.COMMON_HELM_CHARTS_AREA}-prod"
        artifactoryChartsListURL = "${ARTIFACT_SERVER_URL_PREFIX}list/${HELM_CHARTS_AREA}"
    }
    if (chartFilter.contains(".tgz")){
        // Log returned chart version
        println "HELM CHART VERSION : ${chartFilter}"
        return "${ARTIFACT_SERVER_URL_PREFIX}${HELM_CHARTS_AREA}/${commonConfig.COMMON_HELM_CHARTS_PATH}/${chartFilter}"
    }
    else {
        dir("${workspace}/kube/lifecycle/scripts/common") {
            chartsFilename = sh (script: "./get_latest_image_wrapper.sh ${commonConfig.COMMON_HELM_CHARTS_PATH} ${chartFilter} ${artifactoryChartsListURL}", returnStdout: true)
            if (chartsFilename.length() < 1) {
                error("Could not find a Helm charts package")
            }
            // Log returned chart version
            println "HELM CHART VERSION : ${chartsFilename}"
            return "${ARTIFACT_SERVER_URL_PREFIX}${HELM_CHARTS_AREA}/${commonConfig.COMMON_HELM_CHARTS_PATH}/${chartsFilename}"
        }
    }
}

def determineHelmChartsURLSearch() {
    echo "HELM CHART FILTER : ${commonConfig.COMMON_OPENSEARCH_HELM_CHARTS_FILTER}"
    def chartFilter = commonConfig.COMMON_OPENSEARCH_HELM_CHARTS_FILTER
    // If we use a master deployment level, we can derive the helm chart filter from that
    if (commonConfig.COMMON_MASTER_DEPLOYMENT_LEVEL != 'NA') {
        chartFilter = determineMasterImageFilter()
    }
    /* Get correct artifactory path  */
    artifactoryChartsListURL = "${ARTIFACT_SERVER_URL_PREFIX}list/${commonConfig.COMMON_OPENSEARCH_HELM_CHARTS_AREA}"
    HELM_CHARTS_AREA = "${commonConfig.COMMON_OPENSEARCH_HELM_CHARTS_AREA}"
    if (chartFilter.matches("(.*)rivendell(.*)|(.*)master(.*)|(.*)release(.*)")){
        HELM_CHARTS_AREA="${commonConfig.COMMON_OPENSEARCH_HELM_CHARTS_AREA}-prod"
        artifactoryChartsListURL = "${ARTIFACT_SERVER_URL_PREFIX}list/${HELM_CHARTS_AREA}"
    }
    if (chartFilter.contains(".tgz")){
        // Log returned chart version
        println "HELM CHART VERSION : ${chartFilter}"
        return "${ARTIFACT_SERVER_URL_PREFIX}${HELM_CHARTS_AREA}/${commonConfig.COMMON_OPENSEARCH_HELM_CHARTS_PATH}/${chartFilter}"
    }
    else {
        dir("${workspace}/kube/lifecycle/scripts/common") {
            chartsFilename = sh (script: "./get_latest_image_wrapper.sh ${commonConfig.COMMON_OPENSEARCH_HELM_CHARTS_PATH} ${chartFilter} ${artifactoryChartsListURL}", returnStdout: true)
            if (chartsFilename.length() < 1) {
                error("Could not find a Helm charts package")
            }
            // Log returned chart version
            println "HELM CHART VERSION : ${chartsFilename}"
            return "${ARTIFACT_SERVER_URL_PREFIX}${HELM_CHARTS_AREA}/${commonConfig.COMMON_OPENSEARCH_HELM_CHARTS_PATH}/${chartsFilename}"
        }
    }
}

/*
 * Get the URL to the leap helm charts to be used in deployment
 */
def determineLeapHelmChartsURL() {
    echo "LEAP HELM CHART FILTER : ${commonConfig.COMMON_LEAP_HELM_CHARTS_FILTER}"
    def chartFilter = commonConfig.COMMON_LEAP_HELM_CHARTS_FILTER
    /* Get correct artifactory path  */
    artifactoryChartsListURL = "${ARTIFACT_SERVER_URL_PREFIX}list/${commonConfig.COMMON_LEAP_HELM_CHARTS_AREA}"
    LEAP_HELM_CHARTS_AREA="${commonConfig.COMMON_HELM_CHARTS_AREA}"
    if (chartFilter.contains(".tgz")){
        // Log returned chart version
        println "LEAP_HELM CHART VERSION : ${chartFilter}"
        return "${ARTIFACT_SERVER_URL_PREFIX}${LEAP_HELM_CHARTS_AREA}/${commonConfig.COMMON_LEAP_HELM_CHARTS_PATH}/${chartFilter}"
    }
    else {
        dir("${workspace}/kube/lifecycle/scripts/common") {
            chartsFilename = sh (script: "./get_latest_image_wrapper.sh ${commonConfig.COMMON_LEAP_HELM_CHARTS_PATH} ${chartFilter} ${artifactoryChartsListURL}", returnStdout: true)
            if (chartsFilename.length() < 1) {
                error("Could not find a Helm charts package")
            }
            // Log returned chart version
            println "LEAP HELM CHART VERSION : ${chartsFilename}"
            return "${ARTIFACT_SERVER_URL_PREFIX}${LEAP_HELM_CHARTS_AREA}/${commonConfig.COMMON_LEAP_HELM_CHARTS_PATH}/${chartsFilename}"
        }
    }

}

/*
 * Get the URL to the HCL DS Keycloak helm charts to be used in deployment
 */
def determineKeycloakHelmChartsURL() {
    echo "HCL DS KEYCLOAK HELM CHART FILTER : ${commonConfig.COMMON_HCLDS_KEYCLOAK_HELM_CHARTS_FILTER}"
    def chartFilter = commonConfig.COMMON_HCLDS_KEYCLOAK_HELM_CHARTS_FILTER
    /* Get correct artifactory path  */
    artifactoryChartsListURL = "${ARTIFACT_SERVER_URL_PREFIX}list/${commonConfig.COMMON_HCLDS_KEYCLOAK_HELM_CHARTS_AREA}"
    HCLDS_KEYCLOAK_HELM_CHARTS_AREA="${commonConfig.COMMON_HELM_CHARTS_AREA}"
    if (chartFilter.contains(".tgz")){
        // Log returned chart version
        println "HCL DS KEYCLOAK HELM CHART VERSION : ${chartFilter}"
        return "${ARTIFACT_SERVER_URL_PREFIX}${HCLDS_KEYCLOAK_HELM_CHARTS_AREA}/${commonConfig.COMMON_HCLDS_KEYCLOAK_HELM_CHARTS_PATH}/${chartFilter}"
    }
    else {
        dir("${workspace}/kube/lifecycle/scripts/common") {
            chartsFilename = sh (script: "./get_latest_image_wrapper.sh ${commonConfig.COMMON_HCLDS_KEYCLOAK_HELM_CHARTS_PATH} ${chartFilter} ${artifactoryChartsListURL}", returnStdout: true)
            if (chartsFilename.length() < 1) {
                error("Could not find a Helm charts package")
            }
            // Log returned chart version
            println "HCL DS KEYCLOAK HELM CHART VERSION : ${chartsFilename}"
            return "${ARTIFACT_SERVER_URL_PREFIX}${HCLDS_KEYCLOAK_HELM_CHARTS_AREA}/${commonConfig.COMMON_HCLDS_KEYCLOAK_HELM_CHARTS_PATH}/${chartsFilename}"
        }
    }
}

/*
 * Get the URL to the HCL PEOPLE SERVICE helm charts to be used in deployment
 */
def determinePeopleServiceHelmChartsURL() {
    echo "HCL PEOPLE SERVICE HELM CHART FILTER : ${commonConfig.COMMON_PEOPLE_SERVICE_HELM_CHARTS_FILTER}"
    def chartFilter = commonConfig.COMMON_PEOPLE_SERVICE_HELM_CHARTS_FILTER
    /* Get correct artifactory path  */
    artifactoryChartsListURL = "${ARTIFACT_SERVER_URL_PREFIX}list/${commonConfig.COMMON_PEOPLE_SERVICE_HELM_CHARTS_AREA}"
    PEOPLE_SERVICE_HELM_CHARTS_AREA="${commonConfig.COMMON_HELM_CHARTS_AREA}"
    if (chartFilter.contains(".tgz")){
        // Log returned chart version
        println "PEOPLE SERVICE HELM CHART VERSION : ${chartFilter}"
        return "${ARTIFACT_SERVER_URL_PREFIX}${PEOPLE_SERVICE_HELM_CHARTS_AREA}/${commonConfig.COMMON_PEOPLE_SERVICE_HELM_CHARTS_PATH}/${chartFilter}"
    }
    else {
        dir("${workspace}/kube/lifecycle/scripts/common") {
            chartsFilename = sh (script: "./get_latest_image_wrapper.sh ${commonConfig.COMMON_PEOPLE_SERVICE_HELM_CHARTS_PATH} ${chartFilter} ${artifactoryChartsListURL}", returnStdout: true)
            if (chartsFilename.length() < 1) {
                error("Could not find a People Service Helm charts package")
            }
            // Log returned chart version
            println "PEOPLE SERVICE HELM CHART VERSION : ${chartsFilename}"
            return "${ARTIFACT_SERVER_URL_PREFIX}${PEOPLE_SERVICE_HELM_CHARTS_AREA}/${commonConfig.COMMON_PEOPLE_SERVICE_HELM_CHARTS_PATH}/${chartsFilename}"
        }
    }
}

/*    
 * Get the URL to the Liberty Portlet Container helm charts to be used in deployment
 */
def determineLPCHelmChartsURL() {
    echo "LIBERTY PORTLET CONTAINER HELM CHART FILTER : ${commonConfig.COMMON_LPC_HELM_CHARTS_FILTER}"
    def chartFilter = commonConfig.COMMON_LPC_HELM_CHARTS_FILTER
    /* Get correct artifactory path  */
    artifactoryChartsListURL = "${ARTIFACT_SERVER_URL_PREFIX}list/${commonConfig.COMMON_LPC_HELM_CHARTS_AREA}"
    LPC_HELM_CHARTS_AREA="${commonConfig.COMMON_LPC_HELM_CHARTS_AREA}"
    if (chartFilter.contains(".tgz")){
        // Log returned chart version
        println "LPC_HELM CHART VERSION : ${chartFilter}"
        return "${ARTIFACT_SERVER_URL_PREFIX}${LPC_HELM_CHARTS_AREA}/${commonConfig.COMMON_LPC_HELM_CHARTS_PATH}/${chartFilter}"
    }
    else {
        dir("${workspace}/kube/lifecycle/scripts/common") {
            chartsFilename = sh (script: "./get_latest_image_wrapper.sh ${commonConfig.COMMON_LPC_HELM_CHARTS_PATH} ${chartFilter} ${artifactoryChartsListURL}", returnStdout: true)
            if (chartsFilename.length() < 1) {
                error("Could not find a Helm charts package")
            }
            // Log returned chart version
            println "LPC HELM CHART VERSION : ${chartsFilename}"
            return "${ARTIFACT_SERVER_URL_PREFIX}${LPC_HELM_CHARTS_AREA}/${commonConfig.COMMON_LPC_HELM_CHARTS_PATH}/${chartsFilename}"
        }
    }
}

/*
 * Login to the Harbor repository.
 * This is required to execute the OCI commands for helm chart.
 */
def loginToHarborRepo() {
    withCredentials([usernamePassword(credentialsId: 'sofy-harbor-universal', passwordVariable: 'SOFY_PASSWORD', usernameVariable: 'SOFY_USERNAME')]) {
        println "Login to the Harbor repository to execute OCI pull/push commands."
        sh(script: """
            helm version
            helm registry login -u ${SOFY_USERNAME} -p ${SOFY_PASSWORD} https://hclcr.io/
        """)
    }
}

/*
 * Schedule a follow-on job if requested
 */
def scheduleNextJob() {
    def scheduleNextJobReuse = { jobName, paramList, delayHours ->
        // Only schedule if there is a delay defined for the next Job and if there is a Job reference
        if (delayHours && (delayHours > 0) && jobName) {
            // Calculate the time in seconds until the next job should be triggered
            int quietPeriod = (delayHours * 3600) as Integer

            // Parse the provided parameters
            def buildParameters = []
            paramList.each {
                // Get value of parameter from current Job execution
                def parameterValue = evaluate("env.$it")
                buildParameters.add(
                    string(name: it, value: parameterValue)
                )
            }

            // Trigger the target job build and populate the parameters
            println "Trigger ${jobName}\nParameters: ${buildParameters}"
            build(
                job: jobName,
                propagate: false,
                wait: false,
                parameters: buildParameters,
                quietPeriod: quietPeriod
            )
        }
    }

    
   // regular next job 
    scheduleNextJobReuse(commonConfig.COMMON_NEXT_JOB_NAME, commonConfig.COMMON_NEXT_JOB_PARAM_LIST, commonConfig.COMMON_NEXT_JOB_DELAY_HOURS)
   // EKS cluster destroy job
    scheduleNextJobReuse(commonConfig.COMMON_NEXT_CLUSTER_JOB_NAME, commonConfig.COMMON_NEXT_CLUSTER_JOB_PARAM_LIST, commonConfig.COMMON_NEXT_CLUSTER_JOB_DELAY_HOURS)
}

def applyHelmAction(deploymentParameters, actionType = "install") {
    if (actionType != 'install' && actionType != 'upgrade') {
        error "Provided invalid actionType > ${actionType} < to Helm."
    }
    println "Preparing action >  ${actionType} < with Helm."

    // For harbor we can pull the helm chart differently, without relying on the HELM_CHARTS_URL
    def helmChartVersion = ''
    if (commonConfig.COMMON_IMAGE_REPOSITORY == 'hclcr.io') {
        // We use helm pull to retrieve the helm chart from harbor and then unpack it
        println "Pulling helm chart from ${commonConfig.COMMON_HARBOR_PROJECT} Harbor project with ${commonConfig.COMMON_HELM_CHARTS_FILTER} version."
        sh """
            if helm pull oci://hclcr.io/${commonConfig.COMMON_HARBOR_PROJECT}/hcl-dx-deployment --version ${commonConfig.COMMON_HELM_CHARTS_FILTER}; then
                tar zxvf hcl-dx-deployment-${commonConfig.COMMON_HELM_CHARTS_FILTER}.tgz
            else
                echo "Error in pulling helm chart from Harbor. Make sure that helm chart version ${commonConfig.COMMON_HELM_CHARTS_FILTER} exists on the ${commonConfig.COMMON_HARBOR_PROJECT} Harbor project."
                exit 1
            fi
        """
    }else if (commonConfig.COMMON_IMAGE_REPOSITORY == '709825985650.dkr.ecr.us-east-1.amazonaws.com/hcl-america'){
        if (!env.HELM_CHARTS_FILTER){
            commonConfig.COMMON_HELM_CHARTS_FILTER = "2.26.0-alpha.5"
        }
        if (env.KUBE_FLAVOUR == "aws"){
            /*
            * Configure Service Account and Helm chart for AWS Marketplace DX EKS flavor
            * This is adapted from the AWS Marketplace Helm fullfillment launch instructions
            */
            def account_id = ""
            def oidc_provider = ""
            def namespace = ""
            def service_account = ""
            // We use the AWS Credentials to execute the aws cli commands but then use the tokens we get as a response for the actual deployment
            withCredentials([
            string(credentialsId: 'aws-marketplace-deploy-token', variable: 'AWSMP_TOKEN'),
            usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')
            ]) {
                sh """
                    export namespace=dxns
                    export service_account=hcl-digital-experience-service-account
                    
                    kubectl create serviceaccount hcl-digital-experience-service-account --namespace \${namespace}

                    account_id=\$(aws sts get-caller-identity --query "Account" --output text)
                    oidc_provider=\$(aws eks describe-cluster --name eks_cluster_auto --region us-east-1 --query "cluster.identity.oidc.issuer" --output text | sed -e "s/^https:\\/\\///")

                    aws iam attach-role-policy --policy-arn arn:aws:iam::aws:policy/AWSMarketplaceMeteringRegisterUsage --role-name eks-hcl-dx-marketplace-role
                    aws iam attach-role-policy --policy-arn arn:aws:iam::aws:policy/service-role/AWSLicenseManagerConsumptionPolicy --role-name eks-hcl-dx-marketplace-role
                    kubectl annotate serviceaccount -n \${namespace} \${service_account} eks.amazonaws.com/role-arn=arn:aws:iam::\${account_id}:role/eks-hcl-dx-marketplace-role
                    
                    export HELM_EXPERIMENTAL_OCI=1

                    aws ecr get-login-password \
                        --region us-east-1 | helm registry login \
                        --username AWS \
                        --password-stdin 709825985650.dkr.ecr.us-east-1.amazonaws.com 

                    if helm pull oci://${commonConfig.COMMON_IMAGE_REPOSITORY}/hcl-dx-deployment --version ${commonConfig.COMMON_HELM_CHARTS_FILTER}; then
                            tar zxvf hcl-dx-deployment-${commonConfig.COMMON_HELM_CHARTS_FILTER}.tgz
                    else
                        echo "Error in pulling helm chart from AWS Marketplace. Make sure that helm chart version ${commonConfig.COMMON_HELM_CHARTS_FILTER} exists on the ${commonConfig.COMMON_IMAGE_REPOSITORY} ECR folder."
                        exit 1
                    fi
                """
            }
        } else {
            /*
            * Configure Service Account and Helm chart for AWS Marketplace DX Native Kube flavor
            * This is adapted from the AWS Marketplace Helm fullfillment launch instructions 
            */
            def AWSMP_ROLE_ARN = ""
            def AWSMP_ACCESS_TOKEN = ""
            def AWSMP_ROLE_CREDENTIALS = ""
            def AWS_ACCESS_KEY_ID = ""
            def AWS_SECRET_ACCESS_KEY = ""
            def AWS_SESSION_TOKEN = ""
            // We use the AWS Credentials to execute the aws cli commands but then use the tokens we get as a response for the actual deployment
            withCredentials([
            string(credentialsId: 'aws-marketplace-deploy-token', variable: 'AWSMP_TOKEN'),
            usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')
            ]) {
                sh """
                    kubectl create serviceaccount hcl-digital-experience-service-account --namespace dxns

                    AWSMP_ROLE_ARN="arn:aws:iam::657641368736:role/service-role/AWSMarketplaceLicenseTokenConsumptionRole"


                    kubectl create secret generic awsmp-license-token-secret \
                                        --from-literal=license_token=${AWSMP_TOKEN} \
                                        --from-literal=iam_role=\${AWSMP_ROLE_ARN} \
                                        --namespace dxns

                    AWSMP_ACCESS_TOKEN="\$(aws license-manager get-access-token \
                                            --output text --query "*" --token ${AWSMP_TOKEN} --region us-east-1)"

                    AWSMP_ROLE_CREDENTIALS="\$(aws sts assume-role-with-web-identity \
                                    --region 'us-east-1' \
                                    --role-arn \${AWSMP_ROLE_ARN} \
                                    --role-session-name 'AWSMP-guided-deployment-session' \
                                    --web-identity-token \${AWSMP_ACCESS_TOKEN} \
                                    --query 'Credentials' \
                                    --output text)"
                                    
                    export AWS_ACCESS_KEY_ID="\$(echo \${AWSMP_ROLE_CREDENTIALS} | awk '{print \$1}' | xargs)"
                    export AWS_SECRET_ACCESS_KEY="\$(echo \${AWSMP_ROLE_CREDENTIALS} | awk '{print \$3}' | xargs)"
                    export AWS_SESSION_TOKEN="\$(echo \${AWSMP_ROLE_CREDENTIALS} | awk '{print \$4}' | xargs)"

                    kubectl create secret docker-registry awsmp-image-pull-secret \
                    --docker-server=709825985650.dkr.ecr.us-east-1.amazonaws.com \
                    --docker-username=AWS \
                    --docker-password=\$(aws ecr get-login-password --region us-east-1) \
                    --namespace dxns

                    kubectl patch serviceaccount hcl-digital-experience-service-account \
                    --namespace dxns \
                    -p '{"imagePullSecrets": [{"name": "awsmp-image-pull-secret"}]}'

                    export HELM_EXPERIMENTAL_OCI=1

                    aws ecr get-login-password \
                        --region us-east-1 | helm registry login \
                        --username AWS \
                        --password-stdin 709825985650.dkr.ecr.us-east-1.amazonaws.com

                    if helm pull oci://${commonConfig.COMMON_IMAGE_REPOSITORY}/hcl-dx-deployment --version ${commonConfig.COMMON_HELM_CHARTS_FILTER}; then
                            tar zxvf hcl-dx-deployment-${commonConfig.COMMON_HELM_CHARTS_FILTER}.tgz
                    else
                        echo "Error in pulling helm chart from AWS Marketplace. Make sure that helm chart version ${commonConfig.COMMON_HELM_CHARTS_FILTER} exists on the ${commonConfig.COMMON_IMAGE_REPOSITORY} ECR folder."
                        exit 1
                    fi
                """
            }
        }
        
    } else {
        sh """
            curl -s ${deploymentParameters.HELM_CHARTS_URL} --output dxHelmChart.tgz
            tar zxvf dxHelmChart.tgz
        """
    }

    // Determine CF level from helm chart
    helmChartVersion = getHelmChartVersion()

    // Retrieve PV Names if we are performing an upgrade
    if (actionType == 'upgrade') {
        List<String> pvcList = [];
        pvcList = sh(
            script: "kubectl get pvc -n ${NAMESPACE} | tail -n +2 |  awk '{print \$3 \" \" \$1}' ",
            returnStdout: true
        ).trim().tokenize("\n");
        // Iterate through list
        if(!pvcList.isEmpty()){
            for (pvc in pvcList) {
                println "${pvc}"
                // Split result line by space, leaving us with the PVC name in position 1 and the PV name in position 0
                def splitPVC = pvc.split(' ')
                if (splitPVC[1].contains("core-profile")) {
                    deploymentParameters.CORE_PV_NAME=splitPVC[0]
                }
                if (splitPVC[1].contains("digital-asset-management")) {
                    deploymentParameters.DAM_PV_NAME=splitPVC[0]
                }
            }
        }
    }

    // Prepare PV configuration
    preparePersistenceValues(deploymentParameters)

    // Configure parameters based on deployment type (hybrid or non hybrid)
    if (commonConfig.COMMON_HYBRID == 'true') {
        println "This is a HYBRID deployment."
        deploymentParameters.CORE_HOST = commonConfig.COMMON_HYBRID_HOST
        deploymentParameters.OTHER_HOST = deploymentParameters.DX_DEPLOYMENT_HOST
        deploymentParameters.CORE_PORT = commonConfig.COMMON_HYBRID_PORT
        deploymentParameters.OTHER_PORT = '443'
        deploymentParameters.ENABLE_CORE = 'false'
        if (!deploymentParameters.DX_HYBRID_HOST_CORS || deploymentParameters.DX_HYBRID_HOST_CORS == '') {
            if (commonConfig.COMMON_HYBRID_CORE_SSL_ENABLED == "true") {
                deploymentParameters.DX_HYBRID_HOST_CORS = "https://${deploymentParameters.CORE_HOST},https://${deploymentParameters.CORE_HOST}:${deploymentParameters.CORE_PORT}"
            } else {
                deploymentParameters.DX_HYBRID_HOST_CORS = "http://${deploymentParameters.CORE_HOST},http://${deploymentParameters.CORE_HOST}:${deploymentParameters.CORE_PORT}"
            }
        }
        // For hybrid we inherit DX_HYBRID_HOST_CORS from the calling function
    } else {
        println "This is NOT a HYBRID deployment."
        deploymentParameters.CORE_HOST = deploymentParameters.DX_DEPLOYMENT_HOST
        deploymentParameters.OTHER_HOST = deploymentParameters.DX_DEPLOYMENT_HOST
        deploymentParameters.CORE_PORT = '443'
        deploymentParameters.OTHER_PORT = '443'
        deploymentParameters.DX_HYBRID_HOST_CORS = ''
        deploymentParameters.ENABLE_CORE = !commonConfig.COMMON_DISABLE_DX_CORE.toBoolean()
    }

    // For non artifactory repos, this is sufficient
    def imageRepository = commonConfig.COMMON_IMAGE_REPOSITORY
    // For artifactory repos, we have to build the appropriate repository string
    if (commonConfig.COMMON_IMAGE_REPOSITORY != commonConfig.COMMON_IMAGE_AREA) {
        imageRepository = "${commonConfig.COMMON_IMAGE_REPOSITORY}.${commonConfig.COMMON_IMAGE_AREA}"
    }

    // Enable SSL and use LoadBalancer Service by default
    def enableSSL = "true"
    def serviceType = "LoadBalancer"
    // Disable SSL and use ClusterIP when Ingress is deployed
    if (commonConfig.COMMON_ENABLE_INGRESS != "false") {
      enableSSL = "false"
      serviceType = "ClusterIP"
    }
    
    //Check if content AI provider is XAI_INTERNAL
    def isXAIInternal
    if (commonConfig.COMMON_ENABLE_AI == "true" && commonConfig.COMMON_IS_XAI_INTERNAL_AI == "true") {
        isXAIInternal = "true"
    } else {
        isXAIInternal = "false"
    }

    // Preparation for values yaml
    sh """
        cd hcl-dx-deployment
        cp ${workspace}/kube/lifecycle/scripts/common/cf${helmChartVersion}-deploy-values.yaml ./
        ##sed to values.yaml
        sed -i "
            s|REPOSITORY_NAME|${imageRepository}|g;
            s|ENABLE_SSL|${enableSSL}|g;
            s|SERVICE_TYPE|${serviceType}|g;
            s|CORE_IMAGE_NAME|${commonConfig.COMMON_CORE_IMAGE_PATH}|g;
            s|DAM_PLUGIN_GOOGLE_VISION_IMAGE_NAME|${commonConfig.COMMON_DAM_PLUGIN_GOOGLE_VISION_IMAGE_PATH}|g;
            s|RING_API_IMAGE_NAME|${commonConfig.COMMON_RINGAPI_IMAGE_PATH}|g;
            s|HAPROXY_IMAGE_NAME|${commonConfig.COMMON_HAPROXY_IMAGE_PATH}|g;
            s|LOGGING_SIDECAR_DIFF_IMAGE_NAME|${commonConfig.COMMON_LOGGING_SIDECAR_DIFF_IMAGE_PATH}|g;
            s|PREREQS_CHECKER_IMAGE_NAME|${commonConfig.COMMON_PREREQS_CHECKER_IMAGE_PATH}|g;
            s|LICENSE_MANAGER_IMAGE_NAME|${commonConfig.COMMON_LICENSE_MANAGER_IMAGE_PATH}|g;
            s|IMGPROC_IMAGE_NAME|${commonConfig.COMMON_IMGPROC_IMAGE_PATH}|g;
            s|DAM_KALTURA_PLUGIN_IMAGE_NAME|${commonConfig.COMMON_DAM_KALTURA_PLUGIN_IMAGE_PATH}|g;
            s|CC_IMAGE_NAME|${commonConfig.COMMON_CC_IMAGE_PATH}|g;
            s|DAM_IMAGE_NAME|${commonConfig.COMMON_DAM_IMAGE_PATH}|g;
            s|RUNTIME_CONTROLLER_IMAGE_NAME|${commonConfig.COMMON_RUNTIME_CONTROLLER_IMAGE_PATH}|g;
            s|CORE_IMAGE_TAG|${deploymentParameters.CORE_IMAGE_TAG}|g;
            s|DAM_PLUGIN_GOOGLE_VISION_IMAGE_TAG|${deploymentParameters.DAM_PLUGIN_GOOGLE_VISION_IMAGE_TAG}|g;
            s|RINGAPI_IMAGE_TAG|${deploymentParameters.RINGAPI_IMAGE_TAG}|g;
            s|HAPROXY_IMAGE_TAG|${deploymentParameters.HAPROXY_IMAGE_TAG}|g;   
            s|LOGGING_SIDECAR_DIFF_IMAGE_TAG|${deploymentParameters.LOGGING_SIDECAR_DIFF_IMAGE_TAG}|g;
            s|PREREQS_CHECKER_IMAGE_TAG|${deploymentParameters.PREREQS_CHECKER_IMAGE_TAG}|g;
            s|LICENSE_MANAGER_IMAGE_TAG|${deploymentParameters.LICENSE_MANAGER_IMAGE_TAG}|g;
            s|IMGPROC_IMAGE_TAG|${deploymentParameters.IMGPROC_IMAGE_TAG}|g;
            s|DAM_KALTURA_PLUGIN_IMAGE_TAG|${deploymentParameters.DAM_KALTURA_PLUGIN_IMAGE_TAG}|g;
            s|CC_IMAGE_TAG|${deploymentParameters.CC_IMAGE_TAG}|g;
            s|DAM_IMAGE_TAG|${deploymentParameters.DAM_IMAGE_TAG}|g;
            s|RUNTIME_CONTROLLER_IMAGE_TAG|${deploymentParameters.RUNTIME_CONTROLLER_IMAGE_TAG}|g;
            s|DAM_KALTURA_PLUGIN_API_KEY|${commonConfig.COMMON_DAM_KALTURA_PLUGIN_API_KEY}|g;
            s|ENABLE_DAM_NEW_UI|${!commonConfig.COMMON_DISABLE_DAM_NEW_UI.toBoolean()}|g;
            s|DAM_PLUGIN_GOOGLE_VISION_API_KEY|${commonConfig.COMMON_DAM_PLUGIN_GOOGLE_VISION_API_KEY}|g;
            s|ENABLE_PLUGIN_GOOGLE_VISION|${!commonConfig.COMMON_DISABLE_PLUGIN_GOOGLE_VISION.toBoolean()}|g;
            s|DAM_PLUGIN_GOOGLE_VISION_STUB_MODE|${commonConfig.COMMON_DAM_PLUGIN_GOOGLE_VISION_STUB_MODE.toBoolean()}|g;
            s|ENABLE_CLEANUP_RENDITION_VERSION_HEARTBEAT|${commonConfig.COMMON_ENABLE_DAM_CLEAN_UP.toBoolean()}|g;
            s|ENABLE_RINGAPI|${!commonConfig.COMMON_DISABLE_RINGAPI.toBoolean()}|g;
            s|ENABLE_CONTENTCOMPOSER|${!commonConfig.COMMON_DISABLE_CONTENTCOMPOSER.toBoolean()}|g;
            s|ENABLE_DAM|${!commonConfig.COMMON_DISABLE_DAM.toBoolean()}|g;
            s|ENABLE_KALTURA_PLUGIN|${!commonConfig.COMMON_DISABLE_KALTURA_PLUGIN.toBoolean()}|g;
            s|ENABLE_REMOTESEARCH| ${!commonConfig.COMMON_DISABLE_REMOTESEARCH.toBoolean()}|g;
            s|ENABLE_RS_AUTOCONFIG| ${commonConfig.COMMON_ENABLE_RS_AUTOCONFIG.toBoolean()}|g;
            s|DX_CORE_REPLICAS|${commonConfig.DX_CORE_REPLICAS}|g;
            s|DX_DAM_REPLICAS|${commonConfig.DX_DAM_REPLICAS}|g;
            s|DX_CORE_HOME_PATH|${commonConfig.COMMON_DX_CORE_HOME_PATH}|g;
            s|CONTEXT_ROOT_PATH|${commonConfig.COMMON_CONTEXT_ROOT_PATH}|g;
            s|PERSONALIZED_DX_CORE_PATH|${commonConfig.COMMON_PERSONALIZED_DX_CORE_PATH}|g;
            s|ENABLE_OPENLDAP|${!commonConfig.COMMON_DISABLE_OPENLDAP.toBoolean()}|g;
            s|ENABLE_HAPROXY|${!commonConfig.COMMON_DISABLE_HAPROXY.toBoolean()}|g; 
            s|ENABLE_PREREQS_CHECKER|${!commonConfig.COMMON_DISABLE_PREREQS_CHECKER.toBoolean()}|g;
            s|ENABLE_LICENSE_MANAGER|${!commonConfig.COMMON_DISABLE_LICENSE_MANAGER.toBoolean()}|g;
            s|IS_PRODUCTION_ENV|${commonConfig.COMMON_IS_PRODUCTION_ENV.toBoolean()}|g;
            s|LICENSE_SERVER_ID|${commonConfig.COMMON_LICENSE_SERVER_ID}|g;
            s|LICENSE_SERVER_URI|${commonConfig.COMMON_LICENSE_SERVER_URI}|g;
            s|LICENSE_SERVER_FEATURE_WITH_VERSION|${commonConfig.COMMON_LICENSE_SERVER_FEATURE_WITH_VERSION}|g;
            s|LICENSE_USERNAME|${commonConfig.COMMON_LICENSE_USERNAME}|g;
            s|LICENSE_PASSWORD|${commonConfig.COMMON_LICENSE_PASSWORD}|g;
            s|LDAP_IMAGE_NAME|${commonConfig.COMMON_LDAP_IMAGE_PATH}|g
            s|LDAP_IMAGE_TAG|${deploymentParameters.LDAP_IMAGE_TAG}|g
            s|REMOTESEARCH_IMAGE_NAME|${commonConfig.COMMON_RS_IMAGE_PATH}|g
            s|REMOTESEARCH_IMAGE_TAG|${deploymentParameters.RS_IMAGE_TAG}|g
            s|PERSISTENCE_CONNECTION_POOL_IMAGE_NAME|${commonConfig.COMMON_PERSISTENCE_CONNECTION_POOL_IMAGE_PATH}|g
            s|PERSISTENCE_NODE_IMAGE_NAME|${commonConfig.COMMON_PERSISTENCE_NODE_IMAGE_PATH}|g
            s|PERSISTENCE_CONNECTION_POOL_IMAGE_TAG|${deploymentParameters.PERSISTENCE_CONNECTION_POOL_IMAGE_TAG}|g
            s|PERSISTENCE_NODE_IMAGE_TAG|${deploymentParameters.PERSISTENCE_NODE_IMAGE_TAG}|g
            s|PERSISTENCE_METRICS_EXPORTER_IMAGE_NAME|${commonConfig.COMMON_PERSISTENCE_METRICS_EXPORTER_IMAGE_PATH}|g;
            s|PERSISTENCE_METRICS_EXPORTER_IMAGE_TAG|${deploymentParameters.PERSISTENCE_METRICS_EXPORTER_IMAGE_TAG}|g;
            s|METRICS_ENABLED|${commonConfig.COMMON_METRICS_ENABLED.toBoolean()}|g
            s|AUTHORING_ENABLED|${commonConfig.COMMON_AUTHORING_ENABLED.toBoolean()}|g
            s|CORE_WAS_CREDENTIALS_SECRET_NAME|${commonConfig.CORE_WAS_CUSTOM_SECRET}|g
            s|CORE_WPS_CREDENTIALS_SECRET_NAME|${commonConfig.CORE_WPS_CUSTOM_SECRET}|g
            s|CONFIG_WIZARD_CREDENTIALS_SECRET_NAME|${commonConfig.CONFIG_WIZARD_CUSTOM_SECRET}|g
            s|CORE_AI_CREDENTIALS_SECRET_NAME|${commonConfig.CORE_AI_CUSTOM_SECRET}|g
            s|RS_WAS_CREDENTIALS_SECRET_NAME|${commonConfig.RS_WAS_CUSTOM_SECRET}|g
            s|DAM_DB_CREDENTIALS_SECRET_NAME|${commonConfig.DAM_DB_CUSTOM_SECRET}|g
            s|DAM_DB_REPLICATION_CREDENTIALS_SECRET_NAME|${commonConfig.DAM_REPLICATION_CUSTOM_SECRET}|g
            s|DAM_CREDENTIALS_SECRET_NAME|${commonConfig.DAM_CUSTOM_SECRET}|g
            s|IMAGE_PROCESSOR_CREDENTIALS_SECRET_NAME|${commonConfig.IMAGE_PROCESSOR_CUSTOM_SECRET}|g
            s|DAM_KALTURA_KEY_SECRET|${commonConfig.DAM_KULTURA_CUSTOM_SECRET}|g
            s|PERSISTENT_CONNECTION_POOL_SECRET|${commonConfig.PERSISTENT_CONNECTION_POOL_CUSTOM_SECRET}|g
            s|DAM_PLUGIN_GOOGLE_VISION_SECRET_NAME|${commonConfig.DAM_GOOGLE_VISION_CUSTOM_SECRET}|g
            s|LICENSE_MANAGER_CREDENTIALS_SECRET_NAME|${commonConfig.LICENSE_MANAGER_CUSTOM_SECRET}|g
            s|OPEN_LDAP_CREDENTIALS_SECRET_NAME|${commonConfig.OPEN_LDAP_CUSTOM_SECRET}|g
            s|CORE_LDAP_CREDENTIALS_SECRET_NAME|${commonConfig.CORE_LDAP_CUSTOM_SECRET}|g
            s|CORE_LTPA_CREDENTIALS_SECRET_NAME|${commonConfig.CORE_LTPA_CUSTOM_SECRET}|g
            s|ENABLE_CONTENT_REPORTING|${commonConfig.COMMON_ENABLE_CONTENT_REPORTING.toBoolean()}|g;
            s|ENABLE_AI|${commonConfig.COMMON_ENABLE_AI.toBoolean()}|g;
            s|CORE_HOST|${deploymentParameters.CORE_HOST}|g;
            s|CORE_PORT|${deploymentParameters.CORE_PORT}|g;
            s|HYBRID_CORE_SSL_ENABLED|${commonConfig.COMMON_HYBRID_CORE_SSL_ENABLED}|g;
            s|OTHER_HOST|${deploymentParameters.OTHER_HOST}|g;
            s|OTHER_PORT|${deploymentParameters.OTHER_PORT}|g;
            s|DX_HYBRID_HOST_CORS|${deploymentParameters.DX_HYBRID_HOST_CORS}|g;
            s|ENABLE_CORE|${deploymentParameters.ENABLE_CORE}|g;
            s|CONTENT_AI_PROVIDER|${commonConfig.COMMON_CONTENT_AI_PROVIDER}|g;
            s|CUSTOM_AI_CLASSNAME|${commonConfig.COMMON_CUSTOM_AI_CLASSNAME}|g;
            s|IS_XAI_INTERNAL|${isXAIInternal}|g;
            s|ENABLE_DX_PICKER|${commonConfig.COMMON_ENABLE_DX_PICKER.toBoolean()}|g;
            s|ENABLE_PRESENTATION_DESIGNER|${commonConfig.COMMON_ENABLE_PRESENTATION_DESIGNER.toBoolean()}|g;
        " ./cf${helmChartVersion}-deploy-values.yaml
        
        if [[ "${commonConfig.COMMON_DEPLOY_OPENSEARCH}" == "true" ]]
        then
            echo "Search Middleware Service enabled"
            sed -i "
                s|SEARCH_MIDDLEWARE_SERVICE_QUERY_NAME|${commonConfig.SEARCH_MIDDLEWARE_SERVICE_QUERY_NAME}|g;
            " ./cf${helmChartVersion}-deploy-values.yaml
        fi

        echo "deploymentParameters.DX_INSTANCE_NAME is ${deploymentParameters.DX_INSTANCE_NAME}"
        if [[ "${deploymentParameters.DX_INSTANCE_NAME}" == "toblerone-latest" ]]
        then
            echo "Enable ACL Traversal"
            sed -i "
                s|ENABLE_ACL_TRAVERSAL|true|g;
            " ./cf${helmChartVersion}-deploy-values.yaml
        else
            echo "Remove ACL Traversal"
            sed -i "
                /aclTraversal/d;
            " ./cf${helmChartVersion}-deploy-values.yaml    
        fi

        if [[ "${commonConfig.COMMON_ENABLE_DAM_CLEAN_UP}" == "true" ]]
        then
            echo "ENABLE_DAM_CLEAN_UP enabled"
            sed -i "     
                s|VALIDATION_HEARTBEAT|${commonConfig.VALIDATION_HEARTBEAT_INTERVAL_TIME_IN_MINUTES}|g;
                s|RENDITION_OR_VERSION_HEARTBEAT|${commonConfig.RENDITION_OR_VERSION_HEARTBEAT_INTERVAL_TIME_IN_MINUTES}|g;
                s|CLEAN_UP_HEARTBEAT|${commonConfig.CLEANUP_HEARTBEAT_INTERVAL_TIME_IN_MINUTES}|g;
                s|ORPHAN_DATA_FILE_CLEANUP_HEARTBEAT|${commonConfig.ORPHANDATA_AND_FILE_CLEANUP_HEARTBEAT_INTERVAL_TIME_IN_MINUTES}|g;
                s|MEDIA_CREATION_THRESHOLD|${commonConfig.MEDIA_CREATION_THRESHOLD_TIME_IN_MINUTES}|g;
                s|LAST_SCAN_THRESHOLD|${commonConfig.LAST_SCAN_THRESHOLD_TIME_IN_MINUTES}|g;
                s|ORPHAN_DIRECTORY_MODIFICATION_THRESHOLD|${commonConfig.ORPHAN_DIRECTORY_MODIFICATION_THRESHOLD_TIME_IN_MINUTES}|g;
                s|ORPHAN_MEDIA_STORAGE_CREATION_THRESHOLD|${commonConfig.ORPHAN_MEDIA_STORAGE_CREATION_THRESHOLD_TIME_IN_MINUTES}|g;
                s|MAX_VALIDATION_PROCESSING_LIMIT|${commonConfig.MAX_VALIDATION_PROCESSING_LIMIT}|g;
            " ./cf${helmChartVersion}-deploy-values.yaml
        else
            echo "ENABLE_DAM_CLEAN_UP disabled"
            sed -i "     
                /validationHeartbeatIntervalTimeInMinutes/d;
                /renditionOrVersionHeartbeatIntervalTimeInMinutes/d;
                /cleanUpHeartbeatIntervalTimeInMinutes/d;
                /orphanDataAndFileCleanupHeartbeatIntervalTimeInMinutes/d;
                /mediaCreationThresholdTimeInMinutes/d;
                /lastScanThresholdTimeInMinutes/d;
                /orphanDirectoryModificationThresholdTimeInMinutes/d;
                /orphanMediaStorageCreationThresholdTimeInMinutes/d;
                /maxValidationProcessingLimit/d;
            " ./cf${helmChartVersion}-deploy-values.yaml
        fi

        if [[ "${commonConfig.COMMON_PEN_TEST_CONFIG_ENABLED}" == "true" ]]
        then
            echo "Pen test config enabled"
            sed -i "
                s|DX_CORE_COOKIE_SAME_SITE_ATTRIBUTE|Strict|g;
                s|DX_CORE_CSP_FRAME_ANCESTORS_ENABLED|true|g;
                s|DX_CORE_CSP_FRAME_ALLOWED_URLS|[https://${deploymentParameters.CORE_HOST}]|g;
                s|DX_CC_CSP_FRAME_ANCESTORS_ENABLED|true|g;
                s|DX_CC_CSP_FRAME_ALLOWED_URLS|[https://${deploymentParameters.CORE_HOST}]|g;
                s|DX_DAM_CSP_FRAME_ANCESTORS_ENABLED|true|g;
                s|DX_DAM_CSP_FRAME_ALLOWED_URLS|[https://${deploymentParameters.CORE_HOST}]|g;
                s|DX_IMGP_CSP_FRAME_ANCESTORS_ENABLED|true|g;
                s|DX_IMGP_CSP_FRAME_ALLOWED_URLS|[https://${deploymentParameters.CORE_HOST}]|g;
                s|DX_RING_CSP_FRAME_ANCESTORS_ENABLED|true|g;
                s|DX_RING_CSP_FRAME_ALLOWED_URLS|[https://${deploymentParameters.CORE_HOST}]|g;
            " ./cf${helmChartVersion}-deploy-values.yaml
        else
            echo "Pen test config disabled"
            sed -i "
                s|DX_CORE_COOKIE_SAME_SITE_ATTRIBUTE|""|g;
                s|DX_CORE_CSP_FRAME_ANCESTORS_ENABLED|false|g;
                s|DX_CORE_CSP_FRAME_ALLOWED_URLS|[]|g;
                s|DX_CC_CSP_FRAME_ANCESTORS_ENABLED|false|g;
                s|DX_CC_CSP_FRAME_ALLOWED_URLS|[]|g;
                s|DX_DAM_CSP_FRAME_ANCESTORS_ENABLED|false|g;
                s|DX_DAM_CSP_FRAME_ALLOWED_URLS|[]|g;
                s|DX_IMGP_CSP_FRAME_ANCESTORS_ENABLED|false|g;
                s|DX_IMGP_CSP_FRAME_ALLOWED_URLS|[]|g;
                s|DX_RING_CSP_FRAME_ANCESTORS_ENABLED|false|g;
                s|DX_RING_CSP_FRAME_ALLOWED_URLS|[]|g;
            " ./cf${helmChartVersion}-deploy-values.yaml
        fi

        echo
        cat ./cf${helmChartVersion}-deploy-values.yaml
    """

    if (deploymentParameters.haproxyNode) {
        println "Setting haproxy node selector to ${deploymentParameters.haproxyNode}"
        // Replace the NODE_NAME placeholder in the haproxy-nodeSelector.yaml
        sh """
            sed -i "s/NODE_NAME/${deploymentParameters.haproxyNode}/g" ${workspace}/kube/lifecycle/scripts/common/haproxy-nodeSelector.yaml
        """
        haproxyNodeSelector = "-f ${workspace}/kube/lifecycle/scripts/common/haproxy-nodeSelector.yaml"
    } else {
        haproxyNodeSelector = ""
    }

    // Create a secret for admin credentials
    generateSecrets()

    // Different deployment strategy for harbor vs. non-harbor
    // In harbor we can completely remove the image names and tags from our custom values yaml
    // Both are provided by the helm chart itself, thus not requiring us to add anything
    dir('hcl-dx-deployment') {
        if (commonConfig.COMMON_IMAGE_REPOSITORY == 'hclcr.io') {
            def intermediateYaml = readYaml(file: "cf${helmChartVersion}-deploy-values.yaml")
            // Create a secret map object that will be used as the imagePullSecret
            def secretMap = [:]
            secretMap.name = 'dx-harbor'
            // Remove tags and names from images
            intermediateYaml.images.remove('tags')
            intermediateYaml.images.remove('names')
            // Set repository to hclcr.io
            intermediateYaml.images.repository = 'hclcr.io'
            // Add the imagepullsecret to the list of secrets
            intermediateYaml.images.imagePullSecrets = [secretMap]
            writeYaml(file: "cf${helmChartVersion}-deploy-values.yaml", overwrite: true, data: intermediateYaml)
            println "Stripped custom-values yaml for harbor from image tags and names"
            sh "cat cf${helmChartVersion}-deploy-values.yaml"
            println "Performing ${actionType} of harbor based helm chart"
            sh """
                helm ${actionType} dx-deployment -n ${commonConfig.NAMESPACE} . -f ./cf${helmChartVersion}-deploy-values.yaml -f ../persistence-values.yaml ${getCustomValuesOverride()}
            """
        } else if (commonConfig.COMMON_IMAGE_REPOSITORY == '709825985650.dkr.ecr.us-east-1.amazonaws.com/hcl-america'){
            def intermediateYaml = readYaml(file: "cf${helmChartVersion}-deploy-values.yaml")
            // Remove tags and names from images
            intermediateYaml.images.remove('tags')
            intermediateYaml.images.remove('names')
            // Set repository to aws marketplace ecr
            intermediateYaml.images.repository = '709825985650.dkr.ecr.us-east-1.amazonaws.com/hcl-america'
            // Add the imagepullsecret to the list of secrets
            writeYaml(file: "cf${helmChartVersion}-deploy-values.yaml", overwrite: true, data: intermediateYaml)
            println "Stripped custom-values yaml for AWS Marketplace from image tags and names"
            sh "cat cf${helmChartVersion}-deploy-values.yaml"
            println "Performing ${actionType} of AWS Marketplace based helm chart"
            if (env.KUBE_FLAVOUR == "aws"){
                // Set Service Account Name for AWS Marketplace DX EKS flavor
                sh """
                    helm ${actionType} dx-deployment -n ${commonConfig.NAMESPACE} . -f ./cf${helmChartVersion}-deploy-values.yaml -f ../persistence-values.yaml ${getCustomValuesOverride()} --set configuration.licenseManager.serviceAccountName=hcl-digital-experience-service-account
                """
            } else {
                // Set Service Account Name and License Token Name for AWS Marketplace DX Native Kube flavor 
                sh """
                    helm ${actionType} dx-deployment -n ${commonConfig.NAMESPACE} . -f ./cf${helmChartVersion}-deploy-values.yaml -f ../persistence-values.yaml ${getCustomValuesOverride()} --set configuration.licenseManager.serviceAccountName=hcl-digital-experience-service-account --set configuration.licenseManager.licenseConfigSecret=awsmp-license-token-secret
                """
            }  
        } else {
            println "Performing ${actionType} of artifactory based helm chart"
            sh """
                helm ${actionType} dx-deployment -n ${commonConfig.NAMESPACE} . -f ./cf${helmChartVersion}-deploy-values.yaml -f ../persistence-values.yaml ${getCustomValuesOverride()} ${haproxyNodeSelector}
            """
        }

        // copy the custom values yaml to the remote instance, so developers can reference it for native kube
        if (commonConfig.COMMON_KUBE_FLAVOUR == 'native') {
            // Wrap in config file for SSH access to remote machine
            configFileProvider([
                configFile(
                    fileId: 'test-automation-deployments',
                    variable: 'DEPLOY_KEY'
                )
            ]) {
                // Get the deployed custom values from the actual deployment
                sh """
                    helm get values dx-deployment -n ${commonConfig.NAMESPACE} -o yaml > ../merged-deploy-values.yaml
                """

                def targetHost = deploymentParameters.DX_DEPLOYMENT_HOST
                // For clustered native kube environments we copy the content to the control plane
                if (commonConfig.ENABLE_DISTRIBUTED_CONFIGURATION == 'true') {
                    targetHost = "${env.INSTANCE_NAME}-cp${commonConfig.COMMON_DOMAIN_SUFFIX}"
                }
                // Using deploymentParameters.DX_DEPLOYMENT_HOST as the target host
                sh """
                    chmod 600 ${DEPLOY_KEY}
                    scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ../merged-deploy-values.yaml centos@${targetHost}:/home/centos/native-kube/${actionType}-deploy-values.yaml
                    rm ./cf${helmChartVersion}-deploy-values.yaml
                    scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r ./ centos@${targetHost}:/home/centos/native-kube/${actionType}-hcl-dx-deployment
                """
            }
        }
    }

    // Use fitting retry count
    def retryCount = commonConfig.DX_FRESH_PROBE_RETRIES
    if (actionType == 'upgrade') {
        retryCount = commonConfig.DX_UPDATE_PROBE_RETRIES
    }

    if (commonConfig.COMMON_HYBRID == 'false') {
        dxPodsCheckReadiness(
            namespace: commonConfig.NAMESPACE,
            lookupInterval: 30,
            lookupTries: retryCount.toInteger(),
            pendingLimit: 30,
            containerCreateLimit: 60,
            safetyInterval: 120,
            podFilter: 'core-0'
        )
    }
}

def createNamespace() {
    // Creation of a namespace, in Openshift we require different settings for UID and GID allowance
    if (commonConfig.COMMON_KUBE_FLAVOUR == "openshift" || commonConfig.COMMON_KUBE_FLAVOUR == "openshiftnjdc") {
        // Use namespace template for openshift containing correct annotations
        sh """
            sed -i "s|NAMESPACE_PLACEHOLDER|${commonConfig.NAMESPACE}|g" ${env.WORKSPACE}/kube/lifecycle/scripts/openshift/namespace.yaml
            kubectl apply -f ${env.WORKSPACE}/kube/lifecycle/scripts/openshift/namespace.yaml
        """
    } else {
        // Use default namespace creation
        sh """
            kubectl create namespace ${commonConfig.NAMESPACE}
        """
    }
    // Creation of metadata labels for owner, jenkinsBuildNumber, and deployMethod
    sh """
        kubectl patch ns ${commonConfig.NAMESPACE} -p '{"metadata":{"labels": { "owner": "${commonConfig.COMMON_INSTANCE_OWNER_SHORT}" ,"jenkinsBuildNumber": "${currentBuild.number}" }}}'
    """
}

def createHAProxyOpenshiftRoute() {
    // Create HAProxy Route for Openshift passthrough
    sh """
        kubectl apply -n ${commonConfig.NAMESPACE} -f ${env.WORKSPACE}/kube/lifecycle/scripts/openshift/haproxyRoute.yaml
    """
}

/*
 * Selects the right persistence-values.yaml and replaces value placeholders
 */
def preparePersistenceValues(cloudEnvVars) {
    // If there is no RWX class provided, we use our default
    if (!cloudEnvVars.rwxStgClass || (cloudEnvVars.rwxStgClass == '')) {
        cloudEnvVars.rwxStgClass = 'dx-deploy-stg'
    }

    // Prepare PV configuration
    if (commonConfig.COMMON_KUBE_FLAVOUR == 'native' && commonConfig.ENABLE_DISTRIBUTED_CONFIGURATION != 'true') {
        // For native kube non clustered we use a different PVC matching method (via labels)
        // Therefore we leverage a different persistence-values.yaml
        sh "cp ${workspace}/kube/lifecycle/scripts/native/persistence-values.yaml ."
        println "Using native kube persistence-values.yaml."
    } else if (commonConfig.COMMON_KUBE_FLAVOUR == 'native' && commonConfig.ENABLE_DISTRIBUTED_CONFIGURATION == 'true') {
        // For native kube clustered we use NFS auto provisioning
        sh "cp ${workspace}/kube/lifecycle/scripts/native/persistence-values-nfs.yaml persistence-values.yaml"
        println "Using native kube persistence-values-nfs.yaml."
    } else {
        // For all other kube flavours, we simply go with storageclass + type assignments
        sh "cp ${workspace}/kube/lifecycle/scripts/common/persistence-values.yaml ."
        sh """
            sed -i "
                s|CLOUD_STG_CLASS|${cloudEnvVars.cloudStgclass}|g;
                s|RWX_STG_CLASS|${cloudEnvVars.rwxStgClass}|g
                s|CORE_PV_NAME|${cloudEnvVars.CORE_PV_NAME}|g
                s|DAM_PV_NAME|${cloudEnvVars.DAM_PV_NAME}|g
            " ./persistence-values.yaml
        """
        println "Using cloud provider persistence-values.yaml"
    }
    sh "cat ./persistence-values.yaml"
}

/*
 * Deploys a DB2 as a container inside your current kubernetes namespace
 */
def createDB2Server() {
    println "Creating DB2 inside Kubernetes"
    def db2Image = ""
    // Create cmap with DB reference
    sh """
        kubectl create configmap db-transfer --from-literal dbType=db2 --from-literal instanceName=local-db2 -n ${commonConfig.NAMESPACE}
    """
    println "Created dbtransfer reference configmap."
    switch(commonConfig.COMMON_KUBE_FLAVOUR) {
        case "openshift":
            db2Image = "657641368736.dkr.ecr.us-east-2.amazonaws.com/dx-db2:v11.5"
            break
        case "google":
            db2Image = "us.gcr.io/hcl-gcp-l2com-sofy/dxcontainers/dx-db2:v11.5"
            break
        case "azure":
            db2Image = "dxcontainers.azurecr.io/dx-db2:v11.5"
            break
        case "aws":
            db2Image = "657641368736.dkr.ecr.us-east-2.amazonaws.com/dx-db2:v11.5"
            break
        default: 
            db2Image = "quintana-docker.artifactory.cwp.pnp-hcl.com/dx-db2:v11.5"
    }
    println "Determined DB2 container image to be > ${db2Image} <"
    if (commonConfig.COMMON_KUBE_FLAVOUR == 'openshift') {
        println "Performing SCC, SA and DB2 deployment."
        sh """
            cp ${env.WORKSPACE}/kube/db2/os.yaml .
            sed -i 's|DB2IMAGE|${db2Image}|g' os.yaml
            kubectl apply -f os.yaml -n ${commonConfig.NAMESPACE}
        """
    } else if (commonConfig.COMMON_KUBE_FLAVOUR == 'native') {
        println "Performing native DB2 deployment."
        def storageClassName = "manual"
        if (commonConfig.ENABLE_DISTRIBUTED_CONFIGURATION == 'true') {
        sh """
            cp ${env.WORKSPACE}/kube/db2/native-nfs.yaml .
            sed -i 's|DB2IMAGE|${db2Image}|g' native-nfs.yaml
            kubectl apply -f native-nfs.yaml -n ${commonConfig.NAMESPACE}
        """
        } else {
            sh """
                cp ${env.WORKSPACE}/kube/db2/native.yaml .
                sed -i 's|DB2IMAGE|${db2Image}|g' native.yaml
                kubectl apply -f native.yaml -n ${commonConfig.NAMESPACE}
            """
        }

    } else {
        println "Performing DB2 deployment."
        sh """
            cp ${env.WORKSPACE}/kube/db2/non-os.yaml .
            sed -i 's|DB2IMAGE|${db2Image}|g' non-os.yaml
            kubectl apply -f non-os.yaml -n ${commonConfig.NAMESPACE}
        """
    }

    // Wait for the statefulset to be completely ready. Wait longer for OpenShift because typically a new worker is needed.
    def db2Timeout = 600
    if (commonConfig.COMMON_KUBE_FLAVOUR == "openshift") {
        db2Timeout = 1500
    }
    def deploymentState = sh(script: "kubectl rollout status --watch --timeout=${db2Timeout}s statefulset/local-db2 -n ${commonConfig.NAMESPACE}", returnStatus: true)        
    if (deploymentState != 0) {
        error("DB2 deployment failed, statefulset of DB2 did not become ready.")
    }
    println "DB2 has been deployed."

    if (commonConfig.COMMON_KUBE_FLAVOUR != 'native') {
        sh """
            kubectl exec -n ${commonConfig.NAMESPACE} local-db2-0 -- /bin/bash -c 'su db2inst1 -c "export DB2INSTANCE=db2inst1 && /home/db2inst1/sqllib/bin/db2 attach to db2inst1 &&  /home/db2inst1/sqllib/bin/db2 update dbm cfg using instance_memory 1875000"'
            kubectl exec -n ${commonConfig.NAMESPACE} local-db2-0 -- /bin/bash -c 'su db2inst1 -c "export DB2INSTANCE=db2inst1 && /home/db2inst1/sqllib/adm/db2pd -dbptnmem"'
        """
        println "Reconfigured DB2 memory management to fit in a limited Pod size"
    }
    return 'local-db2'
}

/*
 * Configure DB & LDAP
 */
def configureDB_LDAP() {
    // Working directory for storing temporary files
    def workDirectory = 'dbLdapWorkdir'
    sh "mkdir -p ${workDirectory}"
    // Default core container setup
    def deploymentName = 'dx-deployment-core-0'
    // Flag variables, determined by global configuration
    def dbEnabled = commonConfig.COMMON_ENABLE_DB_CONFIG.toBoolean()
    def ldapEnabled = commonConfig.COMMON_ENABLE_LDAP_CONFIG.toBoolean()
    def createDb = false
    
    // DB host, empty per default
    def dbHost = ''

    if (dbEnabled || ldapEnabled) {
        println "Checking if DX is up and running before preparing scripts"
        dxPodsCheckReadiness(
            namespace: commonConfig.NAMESPACE,
            lookupInterval: 30,
            lookupTries: 120,
            pendingLimit: 30,
            containerCreateLimit: 60,
            safetyInterval: 120,
            podFilter: 'core-0'
        )

        println "Preparing scripts"
        sh """
            mkdir -p ${workDirectory}/scripts
            cp ./kube/lifecycle/scripts/common/configure-DB-LDAP/* ${workDirectory}/scripts
            kubectl cp ./${workDirectory} ${deploymentName}:/opt/HCL/wp_profile -n ${commonConfig.NAMESPACE}
            kubectl exec -n ${commonConfig.NAMESPACE} ${deploymentName} -- bash -c 'chmod -R +x /opt/HCL/wp_profile/${workDirectory}/'
        """

        // Wait for Core to be up and running before configuring LDAP
        dxPodsCheckReadiness(
            namespace: commonConfig.NAMESPACE,
            lookupInterval: 30,
            lookupTries: 120,
            pendingLimit: 30,
            containerCreateLimit: 60,
            safetyInterval: 120,
            podFilter: 'core-0'
        )
        // adding semaphore as core might become unavailable during configuration
        println "Prepare semaphore file for config"
        sh """
            kubectl exec -n ${commonConfig.NAMESPACE} ${deploymentName} -- bash -c 'touch /opt/app/configInProgress'
        """
    } else {
        println "Neither DB nor LDAP are being configured"
        return
    }

    // DB Configuration
    if (dbEnabled) {
        // Determine if DB creation is required
        if (commonConfig.COMMON_DB_HOST == '' && commonConfig.COMMON_DB_TYPE == 'db2') {
            createDb = true
        }
        println "DB Host provided to this run: ${commonConfig.COMMON_DB_HOST}"
        println "DB Type provided to this run: ${commonConfig.COMMON_DB_TYPE}"
        println "DB should be created in this run: ${createDb}"

        // If necessary, create a DB2 instance
        if (createDb) {
            // Launch the DB2 Server creation
            dbHost = createDB2Server()
        } else {
            // Use the provided DB host coming from the common configuration
            // Check if it is empty, because that will cause DB transfer to fail
            if (commonConfig.COMMON_DB_HOST == '') {
                error("The provided DB host was empty, can not continue with DB transfer.")
            }
            dbHost = commonConfig.COMMON_DB_HOST
        }

        // Retrieve and unpack the DB transfer file
        // They will land in a subdirectory of the current working directory and will contain both JARs and property files
        sh """
            mkdir dx-db-transfer
            cd dx-db-transfer
            curl -O https://$G_ARTIFACTORY_HOST/artifactory/$G_ARTIFACTORY_GENERIC_NAME/dx-db-transfer/${commonConfig.COMMON_DB2_TRANSFER_FILE}
            unzip ${commonConfig.COMMON_DB2_TRANSFER_FILE}
            cd ..
            mkdir -p ./${workDirectory}/${commonConfig.COMMON_DB_TYPE}home
            cp -r ./dx-db-transfer/${commonConfig.COMMON_DB_TYPE}/dbjars ./${workDirectory}/${commonConfig.COMMON_DB_TYPE}home/
            cp -r ./dx-db-transfer/${commonConfig.COMMON_DB_TYPE}/properties ./${workDirectory}/
        """
        
        // Prepare the wkplc_dbdomain file for anything but openshift NJDC
        // Insert the Hostname of the either created or passed in DB host
        // Outside of openshift NJDC, we need to perform some extra adjustments
        // Since the commonConfig.COMMON_KUBE_FLAVOUR is openshift, but we need to check specifically for openshiftnjdc, we need to directly access the parameter to figure out if we are really using NJDC or not
        if (env.KUBE_FLAVOUR != 'openshiftnjdc') {
            sh """
                sed -i "s|DB_HOST|${dbHost}|g;" ./kube/lifecycle/scripts/common/updateConfig/wkplc_dbdomain_${commonConfig.COMMON_DB_TYPE}.properties
                cp ./kube/lifecycle/scripts/common/updateConfig/wkplc_dbdomain_${commonConfig.COMMON_DB_TYPE}.properties ./${workDirectory}/properties/wkplc_dbdomain.properties
            """
        }

        println "Transferring Database by running transferDB.sh"
        sh """
            kubectl cp ./${workDirectory} ${deploymentName}:/opt/HCL/wp_profile -n ${commonConfig.NAMESPACE}
            kubectl exec -n ${commonConfig.NAMESPACE} ${deploymentName} -- bash -c 'sh /opt/HCL/wp_profile/${workDirectory}/scripts/transferDB.sh ${workDirectory} ${commonConfig.COMMON_DB_TYPE}home ${commonConfig.COMMON_DX_PASSWORD}' |& tee db_transfer_log.txt
        """

        // Check if the build has failed or not
        def isBUILDFAILED = sh(
            returnStdout: true,
            script: "grep -o -i 'BUILD FAILED'  db_transfer_log.txt | wc -l"
        ).trim()
        // Throw an error if the DB transfer has failed
        if (Integer.parseInt("${isBUILDFAILED}") > 0) {
            println "DB Transfer Failed"
            currentBuild.result = "FAILURE"
            error("Throw to stop pipeline")
        }

        println "DB Transfer SUCCESSFUL"
    }

    // LDAP Configuration
    if (ldapEnabled) {
        println "LDAP Host provided to this run: ${commonConfig.COMMON_LDAP_CONFIG_HOST}"
        println "LDAP Port provided to this run: ${commonConfig.COMMON_LDAP_CONFIG_PORT}"

        println "Copying files to LDAP"
        sh """
            sed -i "
                    s|LDAP_CONFIG_HOST|${commonConfig.COMMON_LDAP_CONFIG_HOST}|g;s|LDAP_CONFIG_PORT|${commonConfig.COMMON_LDAP_CONFIG_PORT}|g;
                    s|LDAP_CONFIG_BIND_DN|${commonConfig.COMMON_LDAP_CONFIG_BIND_DN}|g;s|LDAP_CONFIG_BIND_PASSWORD|${commonConfig.COMMON_LDAP_CONFIG_BIND_PASSWORD}|g;
                    s|LDAP_CONFIG_SERVER_TYPE|${commonConfig.COMMON_LDAP_CONFIG_SERVER_TYPE}|g;s|LDAP_CONFIG_BASE_ENTRY|${commonConfig.COMMON_LDAP_CONFIG_BASE_ENTRY}|g;
                "  ./kube/lifecycle/scripts/common/updateConfig/wim/config/wimconfig.xml
            cd ./kube/lifecycle/scripts/common/updateConfig/ && tar cvzf wim.tar.gz wim && cd ${workspace}
            kubectl cp -n ${commonConfig.NAMESPACE} ./kube/lifecycle/scripts/common/updateConfig/wim.tar.gz ${deploymentName}:/opt/HCL/wp_profile/${workDirectory}/
        """

        println "Configure LDAP to the server: execute configLDAP.sh"
        sh  """
            kubectl exec -n ${commonConfig.NAMESPACE} ${deploymentName} -- bash -c 'chmod +x /opt/HCL/wp_profile/dbLdapWorkdir/scripts/configLDAP.sh; /opt/HCL/wp_profile/${workDirectory}/scripts/configLDAP.sh ${workDirectory}'
        """
    }

    // Restart logic for DX Core
    if(dbEnabled || ldapEnabled){
        println "Restart DX"
        sh  """
            kubectl exec -n ${commonConfig.NAMESPACE} ${deploymentName} -- bash -c 'cd /opt/HCL/wp_profile/${workDirectory}/scripts; sh ./reStartPortal.sh ${commonConfig.COMMON_DX_USERNAME} ${commonConfig.COMMON_DX_PASSWORD}'
        """

        // removing semaphore added earlier after db2 transfer is done successfully
        sh """
            kubectl exec -n ${commonConfig.NAMESPACE} ${deploymentName} -- bash -c 'rm -f /opt/app/configInProgress'
        """
        println "Removed semaphore file, configuration is done."

        println "Waiting for core to be ready"
        dxPodsCheckReadiness(
            namespace: commonConfig.NAMESPACE,
            lookupInterval: 30,
            lookupTries: 120,
            pendingLimit: 30,
            containerCreateLimit: 60,
            safetyInterval: 120,
            podFilter: 'core-0'
        )    }
}

/*
 * Returns a file append for helm, including a reference to the custom values override
 * Will return nothing if the values are either not valid yaml
 * OR
 * no values provided
 */
def getCustomValuesOverride() {
    def customValuesOverride = ''
    if (commonConfig.COMMON_CUSTOM_VALUES_OVERRIDE != '') {
        println "Values were provided through CUSTOM_VALUES_OVERRIDE"
        try {
            def parsedYaml = readYaml(text: commonConfig.COMMON_CUSTOM_VALUES_OVERRIDE)
            println "Successfully parsed custom values override."
            println "${commonConfig.COMMON_CUSTOM_VALUES_OVERRIDE}"
            writeYaml(file: 'custom-override.yaml', data: parsedYaml, overwrite: true)
            customValuesOverride = '-f ./custom-override.yaml'
        } catch (parsingError) {
            println "Parsing of custom values override was not successful."
            println parsingError
        }
    }
    return customValuesOverride
}

/*
 * destroy the Persistent Volume and k8s resources
 */
def destroy(NFS_CREDENTIALS_ID,NFS_HOST,NFS_PATH) {
    echo "Destroying the environment ${NAMESPACE}"
    /*
     * Deleting the DB2 instance.
     */
    isDBConfigAvilable=sh (script: """(kubectl get configmap -n  ${NAMESPACE} |  grep -q db-transfer && echo true || echo false) """ ,   returnStdout: true).trim().toBoolean();
    if(isDBConfigAvilable){
        dBType=sh (script: """kubectl get configmap db-transfer -n ${NAMESPACE} -o=jsonpath='{.data.dbType}' """ ,   returnStdout: true).trim();
        instanceName=sh (script: """kubectl get configmap db-transfer -n ${NAMESPACE} -o=jsonpath='{.data.instanceName}' """ ,   returnStdout: true).trim();
    }
    List<String> pvList = [];
    pvList=sh (script: """(kubectl get pv | grep -v 'pvc-' | grep "${NAMESPACE}/" | grep "^${NAMESPACE}" | awk  '{print \$1}') """ ,   returnStdout: true).trim().tokenize("\n");
    /*
     * Deleting the namespace will cause all resources in that namespace to be removed.
     * We also trigger deletion of the deployment and services
     * We wait until the PV is released, before we go ahead and remove the PV
     */
    sh """
        (helm uninstall  dx-deployment -n ${NAMESPACE}) || echo "${NAMESPACE} is not deployed with helm"
        ( (timeout 30s kubectl delete ns ${NAMESPACE} ) && ( kubectl delete deployment -n ${NAMESPACE}) ) || echo "${NAMESPACE} hanging in Terminating State"
        (kubectl delete pod --all --force --grace-period=0 -n ${NAMESPACE}) || echo "${NAMESPACE} PODS hanging in Terminating State"
        ( kubectl patch services \$( kubectl get services -n ${NAMESPACE} | grep -v "NAME" | awk  '{print \$1}' ) -p '{"metadata":{"finalizers":null}}' -n ${NAMESPACE} ) || echo "No services to delete."
        ${workspace}/kube/lifecycle/scripts/common/namespace-check.sh ${commonConfig.NAMESPACE}
        ./kube/lifecycle/scripts/common/start-dxcore-pv-released.sh \$(kubectl get pv | grep -v 'pvc-' | grep '${NAMESPACE}/' | grep "^${NAMESPACE}" | awk  '{print \$1}')
    """
    echo "Persistent Volumes : $pvList "
    if(!pvList.isEmpty()){
        isNFSPVexist = false
        for (PV in pvList) {
            withCredentials([sshUserPrivateKey(credentialsId: "${NFS_CREDENTIALS_ID}", keyFileVariable: 'NFS_KEY_FILE', passphraseVariable: '', usernameVariable: '')]) {
                IP_ADDRESS = NFS_HOST.split("@")[1]
                // Check if the NFS Server is accessible via SSH
                isNFSdown = sh(script: "ssh -q -i ${NFS_KEY_FILE} -o StrictHostKeyChecking=no ${NFS_HOST} exit" , returnStatus: true)
                // Fail completely if NFS Server can't be reached via SSH
                if (isNFSdown != 0) {
                    error("NFS System down: ${PV} - ${IP_ADDRESS}")
                } else {
                    isNFSPVexist=sh (script: """(ssh -i ${NFS_KEY_FILE} -o StrictHostKeyChecking=no ${NFS_HOST} "[[ -d ${NFS_PATH}/$PV ]] && echo 'true'|| echo 'false'"  ) """ ,   returnStdout: true).trim().toBoolean();
                    if(isNFSPVexist){
                        echo "Destroying Persistent Volumes "
                        sh """
                            kubectl delete pv $PV
                            scp -i ${NFS_KEY_FILE} -o StrictHostKeyChecking=no ./kube/lifecycle/scripts/common/pvOperations/nfs-pv-delete.sh ${NFS_HOST}:/tmp
                            ssh -i ${NFS_KEY_FILE} -o StrictHostKeyChecking=no ${NFS_HOST} "bash /tmp/nfs-pv-delete.sh $PV ${NFS_PATH} ${COMMON_KUBE_FLAVOUR} ${commonConfig.DX_START_VERBOSE_MODE}"
                        """
                    }
                }
            }
        }
        if(!isNFSPVexist){
            echo "can not be deleted from nfs,PV deletion excluded from script.Releasing the PV for the future use."
            sh """
                ./kube/lifecycle/scripts/common/start-dxcore-pv-released.sh \$(kubectl get pv | grep -v 'pvc-' | grep '${NAMESPACE}/' | grep "^${NAMESPACE}" | awk  '{print \$1}')
                (kubectl patch pv \$(kubectl get pv | grep -v 'pvc-' | grep '${NAMESPACE}/' | grep "^${NAMESPACE}" | awk  '{print \$1}') -p '{"spec":{"claimRef": null}}' ) || echo "PV Status Changed Available,un used PV with TAG PVC- ignored"
            """
        }
    }
}

/*
 * destroyAllNamespaces the Persistent Volume and k8s resources
 */
def destroyAllNamespaces(NFS_CREDENTIALS_ID,NFS_HOST,NFS_PATH) {
    def namespaces = sh(script: 'kubectl get namespaces -o=jsonpath=\'{.items[*].metadata.name}\'', returnStdout: true).trim().tokenize()
    // Exclude default and kube-system namespaces
    namespaces = namespaces.findAll { it != 'default' && it != 'kube-system' && it != 'kube-node-lease' && it != 'kube-public' }

    namespaces.each { NAMESPACE ->
        echo "Destroying the environment ${NAMESPACE}"

        // Deleting the DB2 instance
        def isDBConfigAvailable = sh(script: """kubectl get configmap -n ${NAMESPACE} | grep -q db-transfer && echo true || echo false""", returnStdout: true).trim().toBoolean()
        def dBType = null
        def instanceName = null
        if (isDBConfigAvailable) {
            dBType = sh(script: """kubectl get configmap db-transfer -n ${NAMESPACE} -o=jsonpath='{.data.dbType}'""", returnStdout: true).trim()
            instanceName = sh(script: """kubectl get configmap db-transfer -n ${NAMESPACE} -o=jsonpath='{.data.instanceName}'""", returnStdout: true).trim()
        }

        // Deleting namespace and related resources
        sh """
            (helm uninstall dx-deployment -n ${NAMESPACE}) || echo "${NAMESPACE} is not deployed with helm"
            ( (timeout 30s kubectl delete ns ${NAMESPACE}) && (kubectl delete deployment -n ${NAMESPACE}) ) || echo "${NAMESPACE} hanging in Terminating State"
            (kubectl delete pod --all --force --grace-period=0 -n ${NAMESPACE}) || echo "${NAMESPACE} PODS hanging in Terminating State"
            (kubectl patch services \$(kubectl get services -n ${NAMESPACE} | grep -v "NAME" | awk '{print \$1}') -p '{"metadata":{"finalizers":null}}' -n ${NAMESPACE}) || echo "No services to delete."
            ${workspace}/kube/lifecycle/scripts/common/namespace-check.sh ${NAMESPACE}
            ./kube/lifecycle/scripts/common/start-dxcore-pv-released.sh \$(kubectl get pv | grep -v 'pvc-' | grep '${NAMESPACE}/' | grep "^${NAMESPACE}" | awk '{print \$1}')
        """

        // Deleting Persistent Volumes
        def pvList = sh(script: """kubectl get pv | grep -v 'pvc-' | grep "${NAMESPACE}/" | grep "^${NAMESPACE}" | awk '{print \$1}'""", returnStdout: true).trim().tokenize("\n")
        echo "Persistent Volumes : $pvList"
        pvList.each { PV ->
            withCredentials([sshUserPrivateKey(credentialsId: "${NFS_CREDENTIALS_ID}", keyFileVariable: 'NFS_KEY_FILE', passphraseVariable: '', usernameVariable: '')]) {
                def IP_ADDRESS = NFS_HOST.split("@")[1]
                // Check if the NFS Server is accessible via SSH
                def isNFSdown = sh(script: "ssh -q -i ${NFS_KEY_FILE} -o StrictHostKeyChecking=no ${NFS_HOST} exit", returnStatus: true)
                // Fail completely if NFS Server can't be reached via SSH
                if (isNFSdown != 0) {
                    error("NFS System down: ${PV} - ${IP_ADDRESS}")
                } else {
                    def isNFSPVexist = sh(script: """ssh -i ${NFS_KEY_FILE} -o StrictHostKeyChecking=no ${NFS_HOST} "[[ -d /mnt/storage/$PV ]] && echo 'true'|| echo 'false'"  """, returnStdout: true).trim().toBoolean()
                    if (isNFSPVexist) {
                        echo "Destroying Persistent Volumes "
                        sh """
                            kubectl delete pv $PV
                            scp -i ${NFS_KEY_FILE} -o StrictHostKeyChecking=no ./kube/lifecycle/scripts/common/pvOperations/nfs-pv-delete.sh ${NFS_HOST}:/tmp
                            ssh -i ${NFS_KEY_FILE} -o StrictHostKeyChecking=no ${NFS_HOST} "bash /tmp/nfs-pv-delete.sh $PV /mnt/storage aws ''"
                        """
                    }
                }
            }
        }
        if (pvList.isEmpty()) {
            echo "can not be deleted from nfs, PV deletion excluded from script. Releasing the PV for future use."
            sh """
            ./kube/lifecycle/scripts/common/start-dxcore-pv-released.sh \$(kubectl get pv | grep -v 'pvc-' | grep '${NAMESPACE}/' | grep "^${NAMESPACE}" | awk '{print \$1}')
            (kubectl patch pv \$(kubectl get pv | grep -v 'pvc-' | grep '${NAMESPACE}/' | grep "^${NAMESPACE}" | awk '{print \$1}') -p '{"spec":{"claimRef": null}}') || echo "PV Status Changed Available, un-used PV with TAG PVC- ignored"
            """
        }
    }
}

/*
 * create the Persistent Volumes
 */
def createPV(NFS_CREDENTIALS_ID,NFS_HOST,NFS_PATH,pvName){
    withCredentials([sshUserPrivateKey(credentialsId: "${NFS_CREDENTIALS_ID}", keyFileVariable: 'NFS_KEY_FILE', passphraseVariable: '', usernameVariable: '')]) {
        isNFSPVexist=sh (script: """(ssh -i ${NFS_KEY_FILE} -o StrictHostKeyChecking=no ${NFS_HOST} "[[ -d ${NFS_PATH}/$pvName ]] && echo 'true'|| echo 'false'"  ) """ ,   returnStdout: true).trim().toBoolean();
        nfsPrivateHostName=sh (script: """ssh -i ${NFS_KEY_FILE} -o StrictHostKeyChecking=no ${NFS_HOST} "hostname -I | awk '{print \$1}'" """ ,   returnStdout: true).trim();
        if(isNFSPVexist){
            echo "Destroying Persistent Volumes "
            sh """
                (kubectl delete pv $pvName ) || (echo "Destroying Persistent Volumes ")
                scp -i ${NFS_KEY_FILE} -o StrictHostKeyChecking=no ./kube/lifecycle/scripts/common/pvOperations/nfs-pv-delete.sh ${NFS_HOST}:/tmp
                ssh -i ${NFS_KEY_FILE} -o StrictHostKeyChecking=no ${NFS_HOST} "bash /tmp/nfs-pv-delete.sh $pvName ${NFS_PATH} ${COMMON_KUBE_FLAVOUR} ${commonConfig.DX_START_VERBOSE_MODE}"
            """
        }

        /* NJDC NFS server is returning two IP addresses for nfsPrivateHostName */
        if ("$nfsPrivateHostName".contains(" ")){
            print "Adjusting NFS Private Host Name."
            nfsPrivateHostName = "$nfsPrivateHostName".tokenize(" ")[0]
            print "$nfsPrivateHostName"
        }

        /* Create PV */
        sh """
            scp -i ${NFS_KEY_FILE} -o StrictHostKeyChecking=no ./kube/lifecycle/scripts/common/pvOperations/nfs-pv-create.sh ${NFS_HOST}:/tmp
            ssh -i ${NFS_KEY_FILE} -o StrictHostKeyChecking=no ${NFS_HOST} "bash /tmp/nfs-pv-create.sh $pvName ${NFS_PATH} ${COMMON_KUBE_FLAVOUR} ${commonConfig.DX_START_VERBOSE_MODE}"
            sed -e " s|NFS_PATH|${NFS_PATH}|g; s|NFS_PRIVATE_HOST_NAME|${nfsPrivateHostName}|g; s|PV|${pvName}|g; " ./kube/lifecycle/scripts/common/pvOperations/persistentVolume.yaml > ./kube/lifecycle/scripts/common/pvOperations/persistentVolume-${pvName}.yaml
            kubectl apply -f ./kube/lifecycle/scripts/common/pvOperations/persistentVolume-${pvName}.yaml
        """
        echo "PV : ${pvName} is created"
    }
    return pvName
}

/*
 * Generate credentials secret
 */
def generateSecrets() {
    tmp_license_username = "${commonConfig.COMMON_LICENSE_USERNAME}"
    tmp_license_password = "${commonConfig.COMMON_LICENSE_PASSWORD}"
    if (tmp_license_username == "") {
        tmp_license_username = "admin"
    }
    if (tmp_license_password == "") {
        tmp_license_password = "licenseManagerPassword"
    }

    tmp_kaltura_plugin_secret_key = "${commonConfig.COMMON_DAM_KALTURA_PLUGIN_API_KEY}"
    if (tmp_kaltura_plugin_secret_key == "") {
        tmp_kaltura_plugin_secret_key = "ffd97b4815279ae1f0aeaffc7835335d"
    }

    tmp_google_vision_plugin_api_key = "${commonConfig.COMMON_DAM_PLUGIN_GOOGLE_VISION_API_KEY}"
    if (tmp_google_vision_plugin_api_key == "") {
        tmp_google_vision_plugin_api_key = "AIzaSyDNduM2wApY9Vg-LyBGkSpsB2OOZWQvn5k"
    }

    if (commonConfig.COMMON_IS_XAI_INTERNAL_AI == 'true') {
        tmp_ai_api_key = "${commonConfig.COMMON_XAI_INTERNAL_API_KEY}"
    } else {
        ai_provider = "${commonConfig.COMMON_CONTENT_AI_PROVIDER}"
        if(ai_provider == 'OPEN_AI') {
            tmp_ai_api_key = "${commonConfig.COMMON_CHATGPT_API_KEY}"
        } else if(ai_provider == 'XAI') {
            tmp_ai_api_key = "${commonConfig.COMMON_XAI_API_KEY}"
        } else {
            tmp_ai_api_key = "${commonConfig.COMMON_CUSTOM_AI_API_CREDENTIALS}"
        }
    }
    
    // Deleting the secret first to avoid failure scenario when the secret already exists.
    sh """
        kubectl delete secret ${commonConfig.CORE_WAS_CUSTOM_SECRET} --namespace=${NAMESPACE} --ignore-not-found
        kubectl create secret generic ${commonConfig.CORE_WAS_CUSTOM_SECRET} --from-literal=username=${commonConfig.COMMON_CUSTOM_WPSADMIN_USER} --from-literal=password=${commonConfig.COMMON_CUSTOM_WPSADMIN_PASSWORD} --namespace=${NAMESPACE}

        kubectl delete secret ${commonConfig.CORE_WPS_CUSTOM_SECRET} --namespace=${NAMESPACE} --ignore-not-found
        kubectl create secret generic ${commonConfig.CORE_WPS_CUSTOM_SECRET} --from-literal=username=${commonConfig.COMMON_CUSTOM_WPSADMIN_USER} --from-literal=password=${commonConfig.COMMON_CUSTOM_WPSADMIN_PASSWORD} --namespace=${NAMESPACE}

        kubectl delete secret ${commonConfig.CONFIG_WIZARD_CUSTOM_SECRET} --namespace=${NAMESPACE} --ignore-not-found
        kubectl create secret generic ${commonConfig.CONFIG_WIZARD_CUSTOM_SECRET} --from-literal=username=wpsadmin --from-literal=password=wpsadmin --namespace=${NAMESPACE}

        kubectl delete secret ${commonConfig.RS_WAS_CUSTOM_SECRET} --namespace=${NAMESPACE} --ignore-not-found
        kubectl create secret generic ${commonConfig.RS_WAS_CUSTOM_SECRET} --from-literal=username=wpsadmin --from-literal=password=wpsadmin --namespace=${NAMESPACE}

        kubectl delete secret ${commonConfig.CORE_AI_CUSTOM_SECRET} --namespace=${NAMESPACE} --ignore-not-found
        kubectl create secret generic ${commonConfig.CORE_AI_CUSTOM_SECRET} --from-literal=apiKey=${tmp_ai_api_key} --namespace=${NAMESPACE}

        kubectl delete secret ${commonConfig.CORE_LDAP_CUSTOM_SECRET} --namespace=${NAMESPACE} --ignore-not-found
        kubectl create secret generic ${commonConfig.CORE_LDAP_CUSTOM_SECRET} --from-literal=bindUser= --from-literal=bindPassword= --namespace=${NAMESPACE}

        kubectl delete secret ${commonConfig.CORE_LTPA_CUSTOM_SECRET} --namespace=${NAMESPACE} --ignore-not-found
        kubectl create secret generic ${commonConfig.CORE_LTPA_CUSTOM_SECRET} --from-literal=ltpa.version= --from-literal=ltpa.realm= --from-literal=ltpa.desKey= --from-literal=ltpa.privateKey= --from-literal=ltpa.publicKey= --from-literal=ltpa.password= --namespace=${NAMESPACE}

        kubectl delete secret ${commonConfig.PERSISTENT_CONNECTION_POOL_CUSTOM_SECRET} --namespace=${NAMESPACE} --ignore-not-found
        kubectl create secret generic ${commonConfig.PERSISTENT_CONNECTION_POOL_CUSTOM_SECRET} --from-literal=username=admin --from-literal=password=adminpassword --namespace=${NAMESPACE}

        kubectl delete secret ${commonConfig.DAM_DB_CUSTOM_SECRET} --namespace=${NAMESPACE} --ignore-not-found
        kubectl create secret generic ${commonConfig.DAM_DB_CUSTOM_SECRET} --from-literal=username=dxuser --from-literal=password=d1gitalExperience --namespace=${NAMESPACE}

        kubectl delete secret ${commonConfig.DAM_REPLICATION_CUSTOM_SECRET} --namespace=${NAMESPACE} --ignore-not-found
        kubectl create secret generic ${commonConfig.DAM_REPLICATION_CUSTOM_SECRET} --from-literal=username=repdxuser --from-literal=password=d1gitalExperience --namespace=${NAMESPACE}

        kubectl delete secret ${commonConfig.DAM_CUSTOM_SECRET} --namespace=${NAMESPACE} --ignore-not-found
        kubectl create secret generic ${commonConfig.DAM_CUSTOM_SECRET} --from-literal=username=damuser --from-literal=password=1234 --namespace=${NAMESPACE}

        kubectl delete secret ${commonConfig.IMAGE_PROCESSOR_CUSTOM_SECRET} --namespace=${NAMESPACE} --ignore-not-found
        kubectl create secret generic ${commonConfig.IMAGE_PROCESSOR_CUSTOM_SECRET} --from-literal=authenticationKey=PluginSecretAuthKey --namespace=${NAMESPACE}

        kubectl delete secret ${commonConfig.OPEN_LDAP_CUSTOM_SECRET} --namespace=${NAMESPACE} --ignore-not-found
        kubectl create secret generic ${commonConfig.OPEN_LDAP_CUSTOM_SECRET} --from-literal=username=dx_user --from-literal=password=p0rtal4u --namespace=${NAMESPACE}

        kubectl delete secret ${commonConfig.LICENSE_MANAGER_CUSTOM_SECRET} --namespace=${NAMESPACE} --ignore-not-found
        kubectl create secret generic ${commonConfig.LICENSE_MANAGER_CUSTOM_SECRET} --from-literal=username=${tmp_license_username} --from-literal=password=${tmp_license_password} --namespace=${NAMESPACE}

        kubectl delete secret ${commonConfig.DAM_KULTURA_CUSTOM_SECRET} --namespace=${NAMESPACE} --ignore-not-found
        kubectl create secret generic ${commonConfig.DAM_KULTURA_CUSTOM_SECRET} --from-literal=authenticationKey=kalturaPluginSecretAuthKey --from-literal=secretKey=${tmp_kaltura_plugin_secret_key} --namespace=${NAMESPACE}

        kubectl delete secret ${commonConfig.DAM_GOOGLE_VISION_CUSTOM_SECRET} --namespace=${NAMESPACE} --ignore-not-found
        kubectl create secret generic ${commonConfig.DAM_GOOGLE_VISION_CUSTOM_SECRET} --from-literal=authenticationKey=PluginSecretAuthKey --from-literal=apiKey=${tmp_google_vision_plugin_api_key} --namespace=${NAMESPACE}

        kubectl delete secret ${commonConfig.SEARCH_ADMIN_CUSTOM_SECRET} --namespace=${NAMESPACE} --ignore-not-found
        kubectl create secret generic ${commonConfig.SEARCH_ADMIN_CUSTOM_SECRET} --from-literal=username=${commonConfig.COMMON_CUSTOM_SEARCH_ADMIN_USER} --from-literal=password=${commonConfig.COMMON_CUSTOM_SEARCH_ADMIN_PASSWORD} --namespace=${NAMESPACE}

        kubectl delete secret ${commonConfig.SEARCH_PUSH_CUSTOM_SECRET} --namespace=${NAMESPACE} --ignore-not-found
        kubectl create secret generic ${commonConfig.SEARCH_PUSH_CUSTOM_SECRET} --from-literal=username=${commonConfig.COMMON_CUSTOM_SEARCH_PUSH_USER} --from-literal=password=${commonConfig.COMMON_CUSTOM_SEARCH_PUSH_PASSWORD} --namespace=${NAMESPACE}
    """
}

/*
 * Generate TLS Certificate
 */
def generateTLSCertificate() {
    sh """
        openssl genrsa -out my-key.pem 2048
        openssl req -x509 -key my-key.pem -out my-cert.pem -days 365 -subj '/CN=my-cert'
        kubectl create secret tls dx-tls-cert --cert=my-cert.pem --key=my-key.pem -n ${NAMESPACE}
    """
}

/*
 * Fetch and installl TLS Certificate from Artifactory
 */
def fetchAndInstallTLSCertificate(passedCertDomain) {
    if (passedCertDomain) {
        if (passedCertDomain.startsWith(".")) {
            certDomain = passedCertDomain.substring(1)
        } else {
            certDomain = passedCertDomain
        }
        sh """
            curl -L ${COMMON_ARTIFACTORY_TRUSTSTOREURL}/${certDomain}/fullchain.cer -o fullchain.cer
            curl -L ${COMMON_ARTIFACTORY_TRUSTSTOREURL}/${certDomain}/${certDomain}.key -o ${certDomain}.key
            kubectl create secret tls dx-tls-cert --cert=fullchain.cer --key=${certDomain}.key -n ${NAMESPACE}
        """
    } else {
        generateTLSCertificate()
    }
}

def executeBuffer(runtime=600){
    // Interval in seconds to check for readiness
    def interval = 60
    // Calculate the amount of tries
    def tries = runtime / interval
    println "Waiting for a maximum of ${runtime} seconds, checking every ${interval} seconds for readiness."
    dxPodsCheckReadiness(
        namespace: commonConfig.NAMESPACE,
        lookupInterval: interval,
        lookupTries: tries,
        pendingLimit: 30,
        containerCreateLimit: 60,
        safetyInterval: 120
    )
    println "Waiting done, execution continues."
}

/* Creates common used params for Acceptance and Setup, Verify Jobs */
// Instance name can also be the namespace...
def createAcceptanceAndSetupVerifJobParams(coreTag, instanceName, domainSuffix){
    echo "Create Acceptance And Setup Verify JobParams started."
    echo "domainSuffix= ${domainSuffix}"

    switch(commonConfig.COMMON_KUBE_FLAVOUR){
        case "native":
            HOSTNAME = "${instanceName}${domainSuffix}"
            INTERNAL_SERVICE = "${instanceName}${domainSuffix}"
            DXCONNECT_HOST = "${instanceName}${domainSuffix}"
        break;
        case "openshift":
            HOSTNAME = "${instanceName}.apps${domainSuffix}"
            INTERNAL_SERVICE = "dx-deployment-passthrough-${HOSTNAME}"
            DXCONNECT_HOST = "dx-deployment-passthrough-${HOSTNAME}"
        break;
        default:
            HOSTNAME = "${instanceName}${domainSuffix}"
            INTERNAL_SERVICE = "${instanceName}${domainSuffix}"
            DXCONNECT_HOST = "${instanceName}${domainSuffix}"
        break;

        echo "HOSTNAME=${HOSTNAME}"
        echo "INTERNAL_SERVICE=${INTERNAL_SERVICE}"
        echo "DXCONNECT_HOST=${DXCONNECT_HOST}"
    }

    if(coreTag.contains("master")){
        branch="master"
        persistence_img_filter="rivendell_${branch}"
        image_repo = "quintana-docker-prod"
    }else if(coreTag.contains("release")){
        coreTag= coreTag - 'v'
        version=coreTag.split("_",3)
        branch= "release/${version[0]}_${version[1]}"
        persistence_img_filter="rivendell_release"
        image_repo = "quintana-docker-prod"
    }else{
        branch="develop"
        persistence_img_filter=""
        image_repo = "quintana-docker"
    }

    if(commonConfig.COMMON_DATA_SETUP_VERIFY_BRANCH){
        branch = commonConfig.COMMON_DATA_SETUP_VERIFY_BRANCH;
    }

    artifactory_img_base_url="https://artifactory.cwp.pnp-hcl.com/artifactory/list/${image_repo}"
    FULL_CONTEXT_ROOT_PATH = "/${commonConfig.COMMON_CONTEXT_ROOT_PATH}/${commonConfig.COMMON_DX_CORE_HOME_PATH}"
    WCM_REST_CONTEXT_ROOT = "/${commonConfig.COMMON_CONTEXT_ROOT_PATH}"
    buildParams = []
    if(commonConfig.COMMON_KUBE_FLAVOUR == "openshift" || commonConfig.COMMON_KUBE_FLAVOUR == "openshiftnjdc" && !(commonConfig.COMMON_HYBRID.toBoolean())) {
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'PORTAL_HOST',
            value: "https://${INTERNAL_SERVICE}${FULL_CONTEXT_ROOT_PATH}"])
    } else {
        buildParams.add(
            [$class: 'StringParameterValue',
                name: 'PORTAL_HOST',
                value: "https://${HOSTNAME}${FULL_CONTEXT_ROOT_PATH}"])
    }

    buildParams.add(
        [$class: 'StringParameterValue',
        name: 'TARGET_BRANCH',
        value: branch])
    buildParams.add(
        [$class: 'BooleanParameterValue',
        name: 'SSL_ENABLED',
        value: true])

    buildParams.add(
        [$class: 'StringParameterValue',
        name: 'EXP_API',
        value: "https://${INTERNAL_SERVICE}/dx/api/core/v1"])
    buildParams.add(
        [$class: 'StringParameterValue',
        name: 'APP_ENDPOINT',
        value: "https://${INTERNAL_SERVICE}/dx/ui/dam"])
    buildParams.add(
        [$class: 'StringParameterValue',
        name: 'DXCONNECT_HOST',
        value: "https://${DXCONNECT_HOST}"])
    buildParams.add(
        [$class: 'StringParameterValue',
        name: 'WCMREST',
        value: "https://${INTERNAL_SERVICE}${WCM_REST_CONTEXT_ROOT}"])
    buildParams.add(
        [$class: 'StringParameterValue',
        name: 'ARTIFACTORY_HOST',
        value: "${image_repo}.artifactory.cwp.pnp-hcl.com"])
    buildParams.add(
        [$class: 'StringParameterValue',
        name: 'ARTIFACTORY_IMAGE_BASE_URL',
        value: artifactory_img_base_url])
    buildParams.add(
        [$class: 'StringParameterValue',
        name: 'MEDIA_LIBRARY_PERSISTENCE_IMAGE_FILTER',
        value: persistence_img_filter])
    buildParams.add(
        [$class: 'StringParameterValue',
        name: 'DAM_API',
        value: "https://${INTERNAL_SERVICE}/dx/api/dam/v1"])
    buildParams.add(
        [$class: 'StringParameterValue',
        name: 'IMAGE_PROCESSOR_API',
        value: "https://${INTERNAL_SERVICE}/dx/api/image-processor/v1"])

    return buildParams
}

/*
 * Run Acceptance Tests
 */
def isAcceptanceTestsSuccess(coreTag,externalIP,enableDXCoreTests){
    branch="develop"
    dxCoreHost="https://$externalIP"
    if(coreTag.contains("master")){
        branch= "master"
    }
    if(coreTag.contains("release")){
        coreTag= coreTag - 'v'
        version=coreTag.split("_",3)
        branch= "release/${version[0]}_${version[1]}"
    }
    if(commonConfig.COMMON_CONTEXT_ROOT_PATH) {
        dxCoreHost="https://$externalIP/$commonConfig.COMMON_CONTEXT_ROOT_PATH/$commonConfig.COMMON_DX_CORE_HOME_PATH"
    }
    if(commonConfig.COMMON_DX_CORE_HOME_PATH == "\\\" \\\"" && commonConfig.COMMON_CONTEXT_ROOT_PATH == "\\\" \\\"" ) {
        dxCoreHost="https://$externalIP/"
    }

    DXCONNECT_IP = externalIP
    if("${commonConfig.COMMON_KUBE_FLAVOUR}" ==  "openshift"){
        DXCONNECT_IP = "dx-deployment-service-dxconnect-${commonConfig.NAMESPACE}.apps.dx-cluster-dev.hcl-dx-dev.net"
    }
    if(commonConfig.COMMON_HYBRID.toBoolean()){
        HYBRID_DOMAIN_SUFFIX = "${DOMAIN_SUFFIX}"
        hybridHostName = "${commonConfig.NAMESPACE}${HYBRID_DOMAIN_SUFFIX}/$commonConfig.COMMON_CONTEXT_ROOT_PATH/$commonConfig.COMMON_DX_CORE_HOME_PATH"
        if((commonConfig.COMMON_DX_CORE_HOME_PATH == "\\\" \\\"" || commonConfig.COMMON_DX_CORE_HOME_PATH == "") && (commonConfig.COMMON_CONTEXT_ROOT_PATH == "\\\" \\\"" || commonConfig.COMMON_DX_CORE_HOME_PATH == "") ) {
            hybridHostName = "${commonConfig.NAMESPACE}${HYBRID_DOMAIN_SUFFIX}"
        }
        dxCoreHost = "https://${hybridHostName}"
        DXCONNECT_IP = "${commonConfig.NAMESPACE}${HYBRID_DOMAIN_SUFFIX}:10202"
    }
    buildParams = [        
        string(name: 'EXP_API',value: "https://$externalIP/dx/api/core/v1"),
        string(name: 'DAM_API',value: "https://$externalIP/dx/api/dam/v1"),
        string(name: 'IMAGE_PROCESSOR_API',value: "https://$externalIP/dx/api/image-processor/v1"),
        string(name: 'APP_ENDPOINT',value: "https://$externalIP/dx/ui/dam"),
        string(name: 'WCMREST',value: "https://$externalIP/${commonConfig.COMMON_CONTEXT_ROOT_PATH}"),
        string(name: 'TARGET_BRANCH',value: branch),
        string(name: 'KUBERNETES_FLAVOUR',value: "${commonConfig.COMMON_KUBE_FLAVOUR}"),
        string(name: 'KUBERNETES_NAMESPACE',value: "${NAMESPACE}"),         
        booleanParam(name: 'SSL_ENABLED',value:'true'),
        string(name: 'DXCONNECT_HOST',value: "https://$DXCONNECT_IP")
    ];
    if (commonConfig.COMMON_DEPLOY_PEOPLESERVICE == "true" && commonConfig.COMMON_DEPLOY_DX == "false") {
        buildParams.add(booleanParam(name: 'TEST_DX_CORE',value: 'false'))
        buildParams.add(booleanParam(name: 'TEST_CC',value: 'false'))
        buildParams.add(booleanParam(name: 'TEST_DAM',value: 'false'))
        buildParams.add(booleanParam(name: 'TEST_RING',value: 'false'))
        buildParams.add(booleanParam(name: 'TEST_DXCLIENT',value: 'false'))
        buildParams.add(booleanParam(name: 'TEST_DAM_SERVER',value:'false'))
        buildParams.add(booleanParam(name: 'TEST_THEME_EDITOR',value:'false'))
    } else {
        buildParams.add(booleanParam(name: 'TEST_DX_CORE',value: enableDXCoreTests))
        buildParams.add(booleanParam(name: 'TEST_CC',value:   "${!commonConfig.COMMON_DISABLE_CONTENTCOMPOSER.toBoolean()}"))
        buildParams.add(booleanParam(name: 'TEST_DAM',value:  "${!commonConfig.COMMON_DISABLE_DAM.toBoolean()}"))
        buildParams.add(booleanParam(name: 'TEST_RING',value: "${!commonConfig.COMMON_DISABLE_RINGAPI.toBoolean()}"))
        buildParams.add(booleanParam(name: 'TEST_DXCLIENT',value: enableDXCoreTests))
        buildParams.add(booleanParam(name: 'TEST_DAM_SERVER',value:'true'))
        buildParams.add(booleanParam(name: 'TEST_THEME_EDITOR',value:'true'))
    }
    
    if (commonConfig.COMMON_DEPLOY_PEOPLESERVICE == "true") {
        buildParams.add(booleanParam(name: 'TEST_PEOPLE_SERVICE',value:'true'))
        def TEST_PEOPLE_SERVICE_URL = "http://$externalIP:30000/people/ui/#/"
        if (commonConfig.COMMON_ENABLE_INGRESS == "true") {
            TEST_PEOPLE_SERVICE_URL = "https://$externalIP/people/ui/#/"
        }
        buildParams.add(string(name: 'TEST_PEOPLE_SERVICE_URL',value: TEST_PEOPLE_SERVICE_URL))
        // if DEPLOY_DX is false, then we need to use the people service url as PORTAL_HOST
        if (commonConfig.COMMON_DEPLOY_DX == "false") {
            dxCoreHost = TEST_PEOPLE_SERVICE_URL
        }
    }    
    buildParams.add(string(name: 'PORTAL_HOST',value: dxCoreHost))

    //Enable license manager acceptance only for toblerone release
    if("${NAMESPACE}" == "toblerone-release-latest") {
        buildParams.add(booleanParam(name: 'TEST_LICENSE_MANAGER',value:'true'))
    } else {
        buildParams.add(booleanParam(name: 'TEST_LICENSE_MANAGER',value:'false'))
    }

    if(commonConfig.COMMON_HYBRID.toBoolean() && commonConfig.COMMON_KUBE_FLAVOUR == "openshift"){
        withAWS(credentials: 'aws_credentials', region: "${AWS_REGION}") {
            def route53RecordSet = sh(script: "aws route53 list-resource-record-sets --hosted-zone-id ${AWS_ZONE_ID} --query 'ResourceRecordSets[?Name == `${NAMESPACE}${HYBRID_DOMAIN_SUFFIX}.`]'", returnStdout: true).trim()
            echo "route53RecordSet= ${route53RecordSet}"
            route53RecordSet = route53RecordSet.replace("\n","")
            def jsonRoute53RecordSet = readJSON text: route53RecordSet
            env.ONPREMISE_HOST_IP = jsonRoute53RecordSet.ResourceRecords[0].Value[0]
            echo "ONPREMISE HOST IP= ${env.ONPREMISE_HOST_IP}"
        }
        buildParams.add(string(name: 'HOST_IP_ADDRESS',value: "${env.ONPREMISE_HOST_IP}"))
        buildParams.add(string(name: 'HOSTNAME',value: "${NAMESPACE}${HYBRID_DOMAIN_SUFFIX}"))
    }

    if (commonConfig.COMMON_ENABLE_OIDC_CONFIGURATION == "true" && commonConfig.COMMON_DEPLOY_DX == "true" ){
        def TEST_DX_PORTAL_URL = "https://$externalIP/${commonConfig.COMMON_CONTEXT_ROOT_PATH}/${commonConfig.COMMON_DX_CORE_HOME_PATH}"
        def TEST_DX_PERSONALIZED_PORTAL_URL = "https://$externalIP/${commonConfig.COMMON_CONTEXT_ROOT_PATH}/${commonConfig.COMMON_PERSONALIZED_DX_CORE_PATH}"
        def TEST_DX_PORTAL_ADMIN_URL = "https://$externalIP/${commonConfig.COMMON_CONTEXT_ROOT_PATH}/${commonConfig.COMMON_DX_CORE_HOME_PATH}/admin"
        def TEST_LDAP_USERNAME = "tuser1"
        def TEST_LDAP_PASSWORD = "passw0rd"
        def TEST_ADMIN_USERNAME = commonConfig.CUSTOM_WPSADMIN_USER
        def TEST_ADMIN_PASSWORD = commonConfig.CUSTOM_WPSADMIN_PASSWORD
        def TEST_OIDC_ADMIN_URL = "https://$externalIP/auth/admin/master/console"
        def TEST_OIDC_REALMS_URL = "https://$externalIP/auth/realms"

        buildParams.add(booleanParam(name: 'TEST_OIDC',value:'true'))
        buildParams.add(string(name: 'TEST_DX_PORTAL_URL',value: TEST_DX_PORTAL_URL))
        buildParams.add(string(name: 'TEST_DX_PERSONALIZED_PORTAL_URL',value: TEST_DX_PERSONALIZED_PORTAL_URL))
        buildParams.add(string(name: 'TEST_DX_PORTAL_ADMIN_URL',value: TEST_DX_PORTAL_ADMIN_URL))
        buildParams.add(string(name: 'TEST_LDAP_USERNAME',value: TEST_LDAP_USERNAME))
        buildParams.add(string(name: 'TEST_LDAP_PASSWORD',value: TEST_LDAP_PASSWORD))
        buildParams.add(string(name: 'TEST_ADMIN_USERNAME',value: TEST_ADMIN_USERNAME))
        buildParams.add(string(name: 'TEST_ADMIN_PASSWORD',value: TEST_ADMIN_PASSWORD))
        buildParams.add(string(name: 'TEST_OIDC_ADMIN_URL',value: TEST_OIDC_ADMIN_URL))
        buildParams.add(string(name: 'TEST_OIDC_REALMS_URL',value: TEST_OIDC_REALMS_URL))
    }

    acceptanceTest = build( 
        job: commonConfig.ACCEPTANCE_TEST_JOB,
        propagate: false,
        parameters: buildParams,
        wait: true)
    echo "The Acceptance Test cases for ${acceptanceTest.getDisplayName()} job build number ${acceptanceTest. getId()} is ${acceptanceTest.getResult()}"
    return acceptanceTest.getResult();
}

def isSetupAndVerifyDataSuccess(coreTag,jobParams,job){
    // job param: setup and verify
    jobName = commonConfig.DATA_VERIFY_JOB
    prefix = 'VERIFY'

    if(job == 'setup'){
        jobName = commonConfig.DATA_SETUP_JOB
        prefix = 'RUN'
    }
    /* Commenting this for the meantime. Branch currently being determined on creation of params
        branch="develop"
        if(coreTag.contains("master")){
            branch= "master"
        }
        if(coreTag.contains("release")){
            coreTag= coreTag - 'v'
            version=coreTag.split("_",3)
            branch= "release/${version[0]}_${version[1]}"
        }
    */
    echo "Setup And Verify Data Job Target Branch: ${branch}"
    jobParams.add(
        [$class: 'BooleanParameterValue',
        name: "${prefix}_CC",
        value: !commonConfig.COMMON_DISABLE_CONTENTCOMPOSER.toBoolean()])

    jobParams.add(
        [$class: 'StringParameterValue',
        name: "TARGET_BRANCH",
        value: branch])

    dataSetupTest = build( job: jobName,
            propagate: false,
            parameters: jobParams,
            wait: true );

    println "${jobParams}"
    echo "The Data Setup cases for ${dataSetupTest.getDisplayName()} job build number ${dataSetupTest.getId()} is ${dataSetupTest.getResult()}";
    return dataSetupTest.getResult();
}

/*
 * Run DAM RTRM Pre Migration and Post migration Tests
 */
def isRtrmMigrationAcceptanceTestsSuccess(coreTag,externalIP,isUpdateDeploy){
    branch="develop"
    if(coreTag.contains("release")){
        coreTag= coreTag - 'v'
        version=coreTag.split("_",3)
        branch="release/${version[0]}_${version[1]}"
    }
    if(coreTag.contains("master")){
        branch= "master"
    }
    echo "RTRM Migration Acceptance Test Job Target Branch: ${branch}"
    testCommnad="pre-migration-test-acceptance-endpoint"
    responseFilterString="Pre Migration Acceptance Test cases"
    if(isUpdateDeploy){
        testCommnad="post-migration-test-acceptance-endpoint"
        responseFilterString="Post Migration Acceptance, Unit and Integration Test cases"
    }
    rtrmMigrationAcceptanceTest = build(
        job: commonConfig.DAM_RTRM_MIGRATION_ACCEPTANCE_TEST_JOB,
        propagate: false,
        parameters:[
            string(name: 'DAM_API',value: "https://$externalIP/dx/api/dam/v1"),
            string(name: 'EXP_API',value: "https://$externalIP/dx/api/core/v1"),
            string(name: 'TARGET_BRANCH',value: branch),
            string(name: 'TEST_COMMAND',value:testCommnad),
            booleanParam(name: 'SSL_ENABLED',value:'true'),
        ],
        wait: true)
    if(rtrmMigrationAcceptanceTest.getResult() == 'SUCCESS') {
        echo "The DAM RTRM ${responseFilterString} for ${rtrmMigrationAcceptanceTest.getDisplayName()} job build number ${rtrmMigrationAcceptanceTest.getId()} is ${rtrmMigrationAcceptanceTest.getResult()}"
        return rtrmMigrationAcceptanceTest.getResult();
    }
    error("The DAM RTRM ${responseFilterString} for ${rtrmMigrationAcceptanceTest.getDisplayName()} job build number ${rtrmMigrationAcceptanceTest.getId()} is ${rtrmMigrationAcceptanceTest.getResult()}")
}

/*
 * Run DAM RTRM JMeter upload and validation Tests
 */
def isRtrmJMeterTestsSuccess(coreTag,externalIP,isUpdateDeploy, isPerformanceTest=false){
    branch="develop"
    targetJmxFile="DAM_RTRM_Upload_Assets.jmx"
    if(coreTag.contains("master")){
        branch= "master"
    }
    if(coreTag.contains("release")){
        coreTag= coreTag - 'v'
        version=coreTag.split("_",3)
        branch="release/${version[0]}_${version[1]}"
    }
    echo "RTRM JMeter Tests Job Target Branch: ${branch}"
    responseFilterString="Uploading Assets"
    if(isUpdateDeploy){
        targetJmxFile="DAM_RTRM_Validation_Assets.jmx"
        responseFilterString="Validating Assets"
    }
    // RTRM performance tests jmx file
    if(isPerformanceTest){
        // branch set to develop - currently the DAM RTRM performance JMX files are not in master yet
        branch= "develop"
        echo "## DAM RTRM performance test enabled ##"
        targetJmxFile="DAM_RTRM_Upload_Images_For_Performance.jmx"
    }
    rtrmJMeterTest = build( 
        job: commonConfig.DAM_RTRM_JMETER_TEST_JOB,
        propagate: false,
        parameters:[
            string(name: 'SERVER_HOST',value: "$externalIP"),
            string(name: 'SERVER_PORT',value: ""),
            string(name: 'SERVER_RINGAPI_PORT',value: ""),
            string(name: 'SERVER_DAMAPI_PORT',value: ""),
            string(name: 'SERVER_PROTOCOL',value: "https"),
            string(name: 'TARGET_BRANCH',value: branch),
            string(name: 'TARGET_JMX_FILE',value:targetJmxFile),
        ],
        wait: true)
    if(rtrmJMeterTest.getResult() == 'SUCCESS') {
        echo "The DAM RTRM JMeter ${responseFilterString} Test cases for ${rtrmJMeterTest.getDisplayName()} job build number ${rtrmJMeterTest.getId()} is ${rtrmJMeterTest.getResult()}"
        return rtrmJMeterTest.getResult();
    }
    error("The DAM RTRM JMeter ${responseFilterString} Test cases for ${rtrmJMeterTest.getDisplayName()} job build number ${rtrmJMeterTest.getId()} is ${rtrmJMeterTest.getResult()}")
}

/*
 * Create Route53 Record
 */
def createRoute53Record(EXTERNAL_IP,DOMAIN_NAME,HOSTED_ZONE,workspace,RECORD_TYPE='A'){
    dir("${workspace}") {
        sh """
            curl -LJO https://releases.hashicorp.com/terraform/0.12.20/terraform_0.12.20_linux_amd64.zip
            unzip -o terraform_0.12.20_linux_amd64.zip
            chmod +x terraform
            ./terraform --help
        """
    }
        withCredentials([
        usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')
    ]) {
            dir("${workspace}/kube/lifecycle/scripts/native/terraform/add-route53-record") {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    sh(script: """
                    cp $DEPLOY_KEY test-automation.pem
                    ls -lah
                    printenv
                    ${workspace}/terraform init -backend-config="key=terraform-status/kube/${DOMAIN_NAME}.key"
                    ${workspace}/terraform apply --auto-approve -var domain_name="${DOMAIN_NAME}" -var record_type="${RECORD_TYPE}" -var ip_address="${EXTERNAL_IP}" -var hosted_zone="${HOSTED_ZONE}"
                    """)
                }
            }
        }
}

/*
* Collect Data for Failed Deployment
*/
def collectData(){
    withAWS(credentials: 'aws_credentials', region: 'us-east-2') {
        dir ("${workspace}") {
        echo "Collecting Environment Data for Analysis Before Destroy"
        //execute script to collect data
        script {
            env.FILESEP="_"
            env.KUBE_FLAVOUR="${commonConfig.COMMON_KUBE_FLAVOUR}"
            sh """
                chmod +x ./kube/lifecycle/scripts/common/collect-failed-deployment-data.sh
                ./kube/lifecycle/scripts/common/collect-failed-deployment-data.sh
            """
                def exists = fileExists "${workspace}/data/${JOB_BASE_NAME}${FILESEP}${BUILD_ID}${FILESEP}${KUBE_FLAVOUR}.zip"
                if (exists){
                    sh(script: "aws s3 cp '${workspace}/data/${JOB_BASE_NAME}${FILESEP}${BUILD_ID}${FILESEP}${KUBE_FLAVOUR}.zip' s3://dx-failed-kube-deployment-logs")
                } else {
                    echo "No data collected for upload."
                }
            }
        }
    }
}

/*
Fetch the helm chart app version
*/
def getHelmChartVersion(){
    // Get Helm chart appVersion
    def helmChartVersion= sh (script: """
        cd hcl-dx-deployment
        grep 'appVersion:' Chart.yaml | awk '{print \$2}'
    """ , returnStdout: true).trim();

    print "HELM_CHART_APP_VERSION: $helmChartVersion"
    def releaseVersion = helmChartVersion.replaceFirst(/.*CF(.*)/, '$1').toInteger()
    return releaseVersion
}

/*
Fetch the search helm chart app version
*/
def getSearchHelmChartVersion(){
    // Get Helm chart appVersion
    def helmChartVersion= sh (script: """
        cd hcl-dx-search
        grep 'appVersion:' Chart.yaml | awk '{print \$2}'
    """ , returnStdout: true).trim();

    print "HELM_CHART_APP_VERSION: $helmChartVersion"
    def releaseVersion = helmChartVersion.replaceFirst(/.*CF(.*)/, '$1').toInteger()
    return releaseVersion
}

/*
 * Configures an imagePullSecret for Harbor access inside the target namespace
 * secret will be called dx-harbor
 */
def createHarborImagePullSecret() {
    withCredentials([usernamePassword(
        credentialsId: 'sofy-harbor-universal',
        passwordVariable: 'HARBOR_PASSWORD',
        usernameVariable: 'HARBOR_USERNAME'
    )]) {
        sh """
            # E-Mail and username are your harbor login, the password is your harbor CLI secret
            kubectl create secret -n ${commonConfig.NAMESPACE} docker-registry dx-harbor --docker-server="hclcr.io" \
            --docker-email='${HARBOR_USERNAME}' \
            --docker-username='${HARBOR_USERNAME}' \
            --docker-password='${HARBOR_PASSWORD}'
        """
    }
}

/*
 * Function used to determine image tags based on the COMMON_MASTER_DEPLOYMENT_LEVEL
 */
def determineMasterImageTags() {
    def tags = [:]
    def masterImageFilter = determineMasterImageFilter()
    tags.CORE_IMAGE_TAG = getImageTag(
        commonConfig.COMMON_CORE_IMAGE_PATH,
        masterImageFilter
    )
    tags.DAM_PLUGIN_GOOGLE_VISION_IMAGE_TAG = getImageTag(
        commonConfig.COMMON_DAM_PLUGIN_GOOGLE_VISION_IMAGE_PATH,
        masterImageFilter
    )
    tags.RINGAPI_IMAGE_TAG = getImageTag(
        commonConfig.COMMON_RINGAPI_IMAGE_PATH,
        masterImageFilter
    )
    tags.CC_IMAGE_TAG = getImageTag(
        commonConfig.COMMON_CC_IMAGE_PATH,
        masterImageFilter
    )
    tags.DAM_IMAGE_TAG = getImageTag(
        commonConfig.COMMON_DAM_IMAGE_PATH,
        masterImageFilter
    )
    tags.IMGPROC_IMAGE_TAG = getImageTag(
        commonConfig.COMMON_IMGPROC_IMAGE_PATH,
        masterImageFilter
    )
    tags.DAM_KALTURA_PLUGIN_IMAGE_TAG = getImageTag(
        commonConfig.COMMON_DAM_KALTURA_PLUGIN_IMAGE_PATH,
        masterImageFilter
    )
    tags.LDAP_IMAGE_TAG = getImageTag(
        commonConfig.COMMON_LDAP_IMAGE_PATH,
        masterImageFilter
    )
    tags.RS_IMAGE_TAG = getImageTag(
        commonConfig.COMMON_RS_IMAGE_PATH,
        masterImageFilter
    )
    tags.PERSISTENCE_METRICS_EXPORTER_IMAGE_TAG = getImageTag(
        commonConfig.COMMON_PERSISTENCE_METRICS_EXPORTER_IMAGE_PATH,
        masterImageFilter
    )
    tags.HAPROXY_IMAGE_TAG = getImageTag(
        commonConfig.COMMON_HAPROXY_IMAGE_PATH,
        masterImageFilter
    )
    tags.LOGGING_SIDECAR_DIFF_IMAGE_TAG = getImageTag(
        commonConfig.COMMON_LOGGING_SIDECAR_DIFF_IMAGE_PATH,
        masterImageFilter
    )
    tags.PREREQS_CHECKER_IMAGE_TAG = getImageTag(
        commonConfig.COMMON_PREREQS_CHECKER_IMAGE_PATH,
        masterImageFilter
    )
    tags.LICENSE_MANAGER_IMAGE_TAG = getImageTag(
        commonConfig.COMMON_LICENSE_MANAGER_IMAGE_PATH,
        masterImageFilter
    )
    tags.RUNTIME_CONTROLLER_IMAGE_TAG = getImageTag(
        commonConfig.COMMON_RUNTIME_CONTROLLER_IMAGE_PATH,
        masterImageFilter
    )
    tags.PERSISTENCE_CONNECTION_POOL_IMAGE_TAG = getImageTag(
        commonConfig.COMMON_PERSISTENCE_CONNECTION_POOL_IMAGE_PATH,
        masterImageFilter
    )
    tags.PERSISTENCE_NODE_IMAGE_TAG = getImageTag(
        commonConfig.COMMON_PERSISTENCE_NODE_IMAGE_PATH,
        masterImageFilter
    )
    return tags
}

def determineMasterImageFilter() {
    // If the image filter contains a reference to the master branch
    // We will have to figure out which images belong to the target CF level of DX Core
    // Therefore we need to identify the Core image that will be used matching our filter
    def latestMatchingCoreImageTag = dxLatestImageTagExtract(repositoryProject: "quintana-docker-prod", imagePath: commonConfig.COMMON_CORE_IMAGE_PATH, imageFilter: 'rivendell_master', imageArea: 'artifactory.cwp.pnp-hcl.com')
    CURRENT_BUILD_NUMBER = latestMatchingCoreImageTag.split("-")[0].split("_")[1].split("CF")[1].toInteger()
    CURRENT_BUILD_NUMBER = CURRENT_BUILD_NUMBER - commonConfig.COMMON_MASTER_DEPLOYMENT_LEVEL.toInteger()
    DEPLOY_BUILD_FILTER = "v95_CF${CURRENT_BUILD_NUMBER}.*.rivendell_master"
    latestMatchingCoreImageTag = dxLatestImageTagExtract(repositoryProject: "quintana-docker-prod", imagePath: commonConfig.COMMON_CORE_IMAGE_PATH, imageFilter: DEPLOY_BUILD_FILTER, imageArea: 'artifactory.cwp.pnp-hcl.com')
    // We extract the day of build for the Core image from master that matched our filter
    def masterBuildDate = getCoreImageBuildDate(latestMatchingCoreImageTag)
    println("Determined master image filter ${masterBuildDate}.*.rivendell_master")
    return "${masterBuildDate}.*.rivendell_master"
}

def runHealthCheck(kubeConfig) {
    // execute config commands as per flavour of deployments
    dir(env.WORKSPACE) {
        switch(commonConfig.COMMON_KUBE_FLAVOUR) {
            // Openshift login procedure
            case 'openshift':
                dxKubectlOpenshiftConfig(openshiftCredentialId: kubeConfig.OPENSHIFT_CREDENTIALS_ID,openshiftServerUrl: kubeConfig.OC_SERVER_URL)
                break
            case 'openshiftnjdc':
                dxKubectlOpenshiftConfig(openshiftCredentialId: kubeConfig.OPENSHIFT_CREDENTIALS_ID,openshiftServerUrl: kubeConfig.OC_SERVER_URL)
                break
            // AWS (EKS) login procedure
            case 'aws':
                dxKubectlAwsConfig(awsClusterName:kubeConfig.CLUSTER_NAME)
                break
            // Azure (AKS) login procedure
            case 'azure':
                dxKubectlAzureConfig(azureClusterName:kubeConfig.CLUSTER_NAME)
                break
            // Google (GKE) login procedure
            case 'google':
                dxKubectlGoogleConfig(gkeClusterName:kubeConfig.CLUSTER_NAME)
                break
            // native kube login procedure
            case 'native':
                if(("${nativeConfig.IS_NJDC_DEPLOYMENT}".toBoolean())) {
                    dxKubectlNativeKubeConfig(sshTarget: nativeConfig.NJDC_INSTANCE_IP)
                    break
                } else {
                    // Different configuration for distributed deployment
                    if (commonConfig.ENABLE_DISTRIBUTED_CONFIGURATION.toBoolean()) {
                        dxKubectlNativeKubeConfig(sshTarget: "${kubeConfig.INSTANCE_NAME}-cp${commonConfig.COMMON_DOMAIN_SUFFIX}")
                    } else {
                        dxKubectlNativeKubeConfig(sshTarget: "${kubeConfig.INSTANCE_NAME}${commonConfig.COMMON_DOMAIN_SUFFIX}")
                    }
                    break
                }
            default:
                error("Flavour is not set.")
        }   
    }
    // check pods for readiness
    dxPodsCheckReadiness(
        namespace: commonConfig.NAMESPACE,
        lookupInterval: 90,
        lookupTries: 180,
        pendingLimit: 15,
        containerCreateLimit: 15,
        safetyInterval: 60
    )
}

def createJobParams(copyParamListString) {
    copyParamList = copyParamListString.split(',').collect{it as String}
    buildParams = []
    params.each { key, value ->
        if (copyParamList.contains(key)) {
            buildParams.add(
                [$class: 'StringParameterValue',
                    name: key,
                    value: "${value}"])
        }
    }
    return buildParams
}


def logstashSetup(SERVER_HOST) {
    echo "Host - $SERVER_HOST"

    // opensearch server username and password 
    withCredentials([
            usernamePassword(credentialsId: 'cwp-opensearch', passwordVariable: 'OPENSEARCH_PASSWORD', usernameVariable: 'OPENSEARCH_USERNAME'),
        ]) {
        // Copy the logstash script for logstash setup and configuration 
        dir("${WORKSPACE}") {
            configFileProvider([
                configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
            ]) {
                try {
                    /* Extract PEM file */
                    sh """
                        cp $DEPLOY_KEY test-automation-deployments.pem
                        chmod 0600 test-automation-deployments.pem
                    """

                    /* Copy scripts to EC2 instance */
                    sh """
                        # Correcting permissions for the private key
                        chmod 0400 test-automation-deployments.pem

                        scp -v -i test-automation-deployments.pem -o StrictHostKeyChecking=no ${WORKSPACE}/kube/lifecycle/scripts/common/Logstash-setup/logstash-setup-configuration.sh centos@${SERVER_HOST}:/home/centos
                        ssh -i test-automation-deployments.pem centos@${SERVER_HOST} 'chmod +x /home/centos/logstash-setup-configuration.sh && sh /home/centos/logstash-setup-configuration.sh "${OS_PROTOCOL}" "${OS_HOSTNAME}" "${OS_INDEX_NAME}" "${OS_USERNAME}" "${OPENSEARCH_PASSWORD}" "${LOGSTASH_VERSION}" '
                    """
                } catch (Exception err) { 
                    echo "Error: ${err}"
                }
            }
        }
    }
}


def installPrometheusAndGrafana(SERVER_HOST, DOMAIN_SUFFIX) {
    echo "Host - $SERVER_HOST"
    echo "Domain-Suffix- $DOMAIN_SUFFIX"

    dir("${workspace}/kube/lifecycle/scripts/common") {
        configFileProvider([
            configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
        ]) {
            try {
                /* Extract PEM file */
                sh """
                    cp $DEPLOY_KEY test-automation-deployments.pem
                    chmod 0600 test-automation-deployments.pem
                """

                def targetHost = "${SERVER_HOST}${DOMAIN_SUFFIX}"
                // For clustered native kube environments we copy the content to the control plane
                if (commonConfig.ENABLE_DISTRIBUTED_CONFIGURATION == 'true') {
                    targetHost = "${env.INSTANCE_NAME}-cp${commonConfig.COMMON_DOMAIN_SUFFIX}"
                }

                /* Copy scripts to EC2 instance */
                sh """
                    scp -r -i test-automation-deployments.pem -o StrictHostKeyChecking=no ./install-prometheus-grafana centos@${targetHost}:/home/centos/native-kube/
                """

                /* Run bash script to install Prometheus and Grafana */
                sh  """
                    ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${targetHost} \
                    '(sh /home/centos/native-kube/install-prometheus-grafana/install-prometheus-grafana.sh)'
                """
                } catch (Exception err) { 
                echo "Error: ${err}"
            }
        }
    }
}

def openLDAPUserSetUp(SERVER_HOST) {
    // If we running in a clustered environment, we need to copy the script to the control plane
    if (commonConfig.ENABLE_DISTRIBUTED_CONFIGURATION == 'true') {
        SERVER_HOST = "${env.INSTANCE_NAME}-cp${commonConfig.COMMON_DOMAIN_SUFFIX}"
    }
    echo "Host - $SERVER_HOST"
    echo "generating users ldif file using script ..."
    // copy the LDAP script from performance repo for generating users ldif file
    dir("${workspace}") {
        configFileProvider([
            configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
        ]) { sshagent(credentials: ['jenkins-git']) {
                script{
                        try{
                            userCount = commonConfig.COMMON_USERS_OPENLDAP.toInteger()
                            echo "User count to add in OpenLDAP: $userCount"
                            /* create user.ldif file from script and copy to native-kube instance */
                            sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    git clone -b develop git@git.cwp.pnp-hcl.com:Team-Q/Portal-Performance-Tests.git ${workspace}/Portal-Performance-Tests
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${SERVER_HOST} 'sudo mkdir -p /opt/Portal-Performance-Tests && sudo chown centos: /opt/Portal-Performance-Tests'
                                    scp -r -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/Portal-Performance-Tests centos@${SERVER_HOST}:/opt
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${SERVER_HOST} 'sudo mkdir -p /opt/Portal-Performance-Tests/ldap/output && sudo chown centos: /opt/Portal-Performance-Tests/ldap/output'
                                    scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/kube/lifecycle/scripts/common/install-node.sh centos@${SERVER_HOST}:/home/centos
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${SERVER_HOST} 'sh /home/centos/install-node.sh'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${SERVER_HOST} 'cd /opt/Portal-Performance-Tests/ldap && npm install'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${SERVER_HOST} 'cd /opt/Portal-Performance-Tests/ldap && node . --userCount $userCount --userName User --groupCount 1 --groupName Group --orgUnitCount 1 --orgUnitName dx --mappedGroups 1 --baseDN com --skipUserCSV --ldifType OpenLDAP --splitSize -1'
                                """
                                /* Copy scripts to EC2 instance */
                                sh """
                                    scp -r -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no ${workspace}/kube/lifecycle/scripts/common/open-ldap-setup centos@${SERVER_HOST}:/home/centos
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${SERVER_HOST} 'cd /opt/Portal-Performance-Tests/ldap/output && cp -R users.0.ldif test_users.ldif && mv test_users.ldif /home/centos/open-ldap-setup'
                                """
                                /* Generate CN DN based user's ldif file */
                                sh """
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${SERVER_HOST} 'cd /opt/Portal-Performance-Tests/ldap && node . --userCount $userCount --userName User --groupCount 1 --groupName Group --orgUnitCount 1 --orgUnitName dx --mappedGroups 1 --baseDN com --skipUserCSV --ldifType OpenLDAPWith_CN_DN --splitSize -1'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${SERVER_HOST} 'cd /opt/Portal-Performance-Tests/ldap/output && cp -R users.0.ldif test_cn_users.ldif && mv test_cn_users.ldif /home/centos/open-ldap-setup'
                                """
                                /* Generate MAIL DN based user's ldif file */
                                sh """
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${SERVER_HOST} 'cd /opt/Portal-Performance-Tests/ldap && node . --userCount $userCount --userName User --groupCount 1 --groupName Group --orgUnitCount 1 --orgUnitName dx --mappedGroups 1 --baseDN com --skipUserCSV --ldifType OpenLDAPWith_MAIL_DN --splitSize -1'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${SERVER_HOST} 'cd /opt/Portal-Performance-Tests/ldap/output && cp -R users.0.ldif test_mail_users.ldif && mv test_mail_users.ldif /home/centos/open-ldap-setup'
                                """
                                /* Generate user with accented characters */
                                def accentedUsers = ["Téa", "Váquéz", "Zöe", "Eleña", "Glück", "Königin", "O\\'Neil", "रविकिशन", "מיכאל_Michael", "欣怡_Xīnyí"]
                                for (tmpUserName in accentedUsers) {
                                    sh """
                                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${SERVER_HOST} "cd /opt/Portal-Performance-Tests/ldap && node . --userCount 1 --userName ${tmpUserName} --groupCount 1 --groupName Group --orgUnitCount 1 --orgUnitName dx --mappedGroups 1 --baseDN com --skipUserCSV --ldifType OpenLDAPSingleUserWithSpecialChar --splitSize -1"
                                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${SERVER_HOST} 'cd /opt/Portal-Performance-Tests/ldap/output && cat users.0.ldif >> accented_user.ldif'
                                    """
                                }
                                sh """
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${SERVER_HOST} 'cd /opt/Portal-Performance-Tests/ldap/output && mv accented_user.ldif /home/centos/open-ldap-setup'
                                """
                                
                                /* Import people-service openldap users */
                                if(commonConfig.COMMON_DEPLOY_PEOPLESERVICE.toBoolean() && !COMMON_DISABLE_OPENLDAP.toBoolean()) {
                                    try {
                                        echo "Importing people-service openldap users"
                                        sh """
                                            chmod 600 ${DEPLOY_KEY}
                                            git clone -b develop git@git.cwp.pnp-hcl.com:hclds/people-service.git ${workspace}/people-service-repo
                                            echo "Copying ldap-data to open-ldap-setup"
                                            scp -r -i ${DEPLOY_KEY} ${workspace}/people-service-repo/docker/ldap-data/ centos@${SERVER_HOST}:/home/centos/open-ldap-setup
                                        """
                                    } catch (Exception err) {
                                        echo "Error: ${err}"
                                    }
                                }

                                /* Run bash script to copy ldif user file and also wkplc properties file */
                                sh  """
                                    ssh -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no centos@${SERVER_HOST} \
                                    '(sh /home/centos/open-ldap-setup/add-openldap-users.sh)'
                                """
                                // check core pod for readiness
                                dxPodsCheckReadiness(
                                    namespace: commonConfig.NAMESPACE,
                                    lookupInterval: 30,
                                    lookupTries: 120,
                                    pendingLimit: 30,
                                    containerCreateLimit: 60,
                                    safetyInterval: 120,
                                    podFilter: 'core-0'
                                )
                                echo "Completed openldap users set up .."
                        } catch (Exception err) { 
                        echo "Error: ${err}"
                        }   
                }

         }
        }
    }
}

def configurePenTestEnv(deploymentParameters) {
    echo "Configuring Penetration Testing Environment"
    // Copy the script to the target instance
    dir("${workspace}") {
        configFileProvider([
            configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
        ]) {
            try {
                /* Extract PEM file */
                sh """
                    cp $DEPLOY_KEY test-automation-deployments.pem
                    chmod 0600 test-automation-deployments.pem
                """

                // Copy script to core pods
                def dxCoreDeploymentName = "dx-deployment-core-0"
                def scriptFiles = "${workspace}/kube/lifecycle/scripts/common/pen-test-config"
                println "Copying ${scriptFiles} to ${dxCoreDeploymentName} in ${NAMESPACE}"
                sh """
                    kubectl cp -n ${namespace} ${scriptFiles} ${dxCoreDeploymentName}:/home/dx_user/
                    kubectl -n ${namespace} exec pod/${dxCoreDeploymentName} -- /bin/bash -c 'bash /home/dx_user/pen-test-config/execute-admin-task.sh wpsadmin P3nTest4!Tunate'
                """

            } catch (Exception err) { 
                echo "Error: ${err}"
            }
        }
    }
}

/*
 * Get realm information for Identity Provider (Keycloak)
 */
def getRealmInfo() {
    def parameters = [:]
    def httpRelativePath = "auth"
    parameters.REALM_NAME = "hcl"
    parameters.REALM_URL = "https://${env.INSTANCE_NAME}${env.DOMAIN_SUFFIX}/${httpRelativePath}/realms/${parameters.REALM_NAME}"
    parameters.USER_FEDERATION_LDAP_CONNECTION_URL = "ldap://dx-deployment-open-ldap:1389"
    parameters.USER_FEDERATION_LDAP_BIND_CREDENTIAL = "p0rtal4u"
    parameters.OIDC_DISCOVERY_ENDPOINT = "${parameters.REALM_URL}/.well-known/openid-configuration"
    return parameters
}

/*
 * Create postgresql database
 */
def createPostgreSQLDB(DB_NAME) {
    try {
        println "Create postgresql database ${DB_NAME}"
        sh """
            kubectl -n ${commonConfig.NAMESPACE} exec pod/dx-deployment-persistence-node-0 -c persistence-node -- psql -c "CREATE DATABASE ${DB_NAME};"
        """
    } catch (Exception err) {
        echo "Database ${DB_NAME} exists so skipping creation"
    }
}

/*
 * Create postgresql database
 */
def dropPostgreSQLDB(DB_NAME) {
    try {
        println "Drop postgresql database ${DB_NAME}"
        sh """
            kubectl -n ${commonConfig.NAMESPACE} exec pod/dx-deployment-persistence-node-0 -c persistence-node -- psql -c "DROP DATABASE ${DB_NAME};"
        """
    } catch (Exception err) {
        echo "Database ${DB_NAME} does not exists so skipping dropping it"
    }
}

def configureOIDC() {
    // Configure OIDC for DX
    if (commonConfig.COMMON_DEPLOY_DX == "true") {
        configureOIDCForDX()
    }
}

/*
 * Configure OIDC for DX
 */
def configureOIDCForDX() {
    def realmInfo = getRealmInfo()
    def configFileName = "oidc-config.properties"
    def hostName = "${env.INSTANCE_NAME}${env.DOMAIN_SUFFIX}"
    def coreHomeURL = "https://${hostName}/${commonConfig.COMMON_CONTEXT_ROOT_PATH}/${commonConfig.COMMON_DX_CORE_HOME_PATH}"
    def redirectUrl = "https://${hostName}/${commonConfig.COMMON_CONTEXT_ROOT_PATH}/${commonConfig.PERSONALIZED_DX_CORE_PATH}"
    def namespace = commonConfig.NAMESPACE
    def clientId = "${realmInfo.REALM_NAME}-dx-oidc-client"
    def clientSecret = "${clientId}-secret"
    def postLogoutUrl = "${realmInfo.REALM_URL}/protocol/openid-connect/logout?post_logout_redirect_uri=${coreHomeURL}\\&client_id=${clientId}"

    // Replace deployment placeholders within oidc-config.properties
    sh """
        cp ${workspace}/kube/lifecycle/scripts/native/deploy/oidc/${configFileName} ./
        sed -i "
            s|REALM_NAME|${realmInfo.REALM_NAME}|g;
            s|DX_OIDC_CLIENT_ID|${clientId}|g;
            s|DX_OIDC_CLIENT_SECRET|${clientSecret}|g;
            s|DX_HOST|${hostName}|g;
            s|DX_REDIRECT_URL|${redirectUrl}|g;
            s|DX_POST_LOGOUT_URL|${postLogoutUrl}|g;
            s|OIDC_DISCOVERY_ENDPOINT|${realmInfo.OIDC_DISCOVERY_ENDPOINT}|g;
            s|WAS_PASSWORD|${commonConfig.COMMON_DX_PASSWORD}|g;
            s|PORTAL_ADMIN_PASSWORD|${commonConfig.COMMON_CUSTOM_WPSADMIN_PASSWORD}|g;
        " ./${configFileName}
    """

    sh """
        cat ${configFileName}
    """
    
    // Copy oidc-config.properties to dx-core
    def dxCoreDeploymentName = "dx-deployment-core-0"
    println "Copying ${configFileName} to ${dxCoreDeploymentName}"
    
    sh """
        kubectl cp -n ${namespace} ${configFileName} ${dxCoreDeploymentName}:/opt/HCL/wp_profile/ConfigEngine/properties/
    """

    def command = "/opt/HCL/wp_profile/ConfigEngine/./ConfigEngine.sh -DSaveParentProperties=true -DparentProperties=\"/opt/HCL/wp_profile/ConfigEngine/properties/${configFileName}\" enable-oidc-configuration"
    sh """
        kubectl -n ${namespace} exec pod/${dxCoreDeploymentName} -- bash -c "${command}"
    """

    // check pods for readiness
    dxPodsCheckReadiness(
        namespace: commonConfig.NAMESPACE,
        lookupInterval: 90,
        lookupTries: 180,
        pendingLimit: 15,
        containerCreateLimit: 15,
        safetyInterval: 60
    )

    echo "Finished configuring OIDC for DX"
}

/*
 * Installs OpenSearch
 */
def installOpenSearch(NAMESPACE, REPLICAS, STORAGE_CLASS, OPENSEARCH_VERSION, INSTANCE_NAME, DOMAIN_SUFFIX, USE_OPENSOURCE_OPENSEARCH) {
    echo "Install opensource OpenSearch"
    echo "NAMESPACE - $NAMESPACE"
    echo "REPLICAS - $REPLICAS"
    echo "STORAGE_CLASS - $STORAGE_CLASS"
    echo "OPENSEARCH_VERSION - $OPENSEARCH_VERSION"
    echo "INSTANCE_NAME - $INSTANCE_NAME"
    echo "DOMAIN_SUFFIX - $DOMAIN_SUFFIX"
    echo "USE_OPENSOURCE_OPENSEARCH - $USE_OPENSOURCE_OPENSEARCH"

    if (commonConfig.COMMON_KUBE_FLAVOUR == 'native') {
        configFileProvider([
            configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
        ]) {
            try {

                /* Extract PEM file */
                sh  """
                        cp $DEPLOY_KEY test-automation-deployments.pem
                        chmod 0600 test-automation-deployments.pem
                    """
                /* Setting the maximum heap size for the opensearch installation */
                sh  """
                        ssh -tt -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${INSTANCE_NAME}${DOMAIN_SUFFIX} 'sudo -- sh -c -e "echo 'vm.max_map_count=262144' >> /etc/sysctl.conf"'
                        ssh -tt -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@${INSTANCE_NAME}${DOMAIN_SUFFIX} 'sudo sysctl -p'
                    """
            } catch (Exception err) {
                echo "Error: ${err}"
            }  
        }
    }

    OS_VERSION=''
    if (OPENSEARCH_VERSION) {
        OS_VERSION = "--version=$OPENSEARCH_VERSION"
    }
    echo OS_VERSION

    // if USE_OPENSOURCE_OPENSEARCH is enabled opensource opensearch repo & helm chart will be used for deployment
    // if USE_OPENSOURCE_OPENSEARCH is disabled hcl-dx-search helm chart will be used for deployment
    if (USE_OPENSOURCE_OPENSEARCH == "true") {
        sh  """
            echo "Installing OpenSearch..."
            helm repo add opensearch "https://opensearch-project.github.io/helm-charts/"
            helm search repo opensearch
            helm show values opensearch/opensearch > openSearch.yaml
            helm install --set replicas=${REPLICAS} --set persistence.storageClass=${STORAGE_CLASS} -n ${NAMESPACE}  -f openSearch.yaml os-master opensearch/opensearch ${OS_VERSION}
            sleep 1m
            helm install --set nodeGroup=data --set replicas=${REPLICAS} --set persistence.storageClass=${STORAGE_CLASS} -n ${NAMESPACE} -f openSearch.yaml os-data opensearch/opensearch ${OS_VERSION}
            sleep 1m
            helm install --set nodeGroup=client --set replicas=${REPLICAS} --set persistence.storageClass=${STORAGE_CLASS} -n ${NAMESPACE} -f openSearch.yaml os-client opensearch/opensearch ${OS_VERSION}
        """
    } else {
        def deploymentParametersSearch = [:]
        def OPENSEARCH_IMAGE = ""
        def SEARCH_MIDDLEWARE_IMAGE = ""
        def OPENSEARCH_HELM_CHART = ""
        deploymentParametersSearch.OPENSEARCH_IMAGE = getImageTag(commonConfig.COMMON_OPENSEARCH_IMAGE_PATH, commonConfig.COMMON_OPENSEARCH_IMAGE_FILTER)
        deploymentParametersSearch.SEARCH_MIDDLEWARE_IMAGE = getImageTag(commonConfig.COMMON_SEARCH_MIDDLEWARE_IMAGE_PATH, commonConfig.COMMON_SEARCH_MIDDLEWARE_IMAGE_FILTER)
        deploymentParametersSearch.OPENSEARCH_HELM_CHART = getImageTag(commonConfig.COMMON_OPENSEARCH_HELM_CHARTS_PATH, commonConfig.COMMON_OPENSEARCH_HELM_CHARTS_FILTER)
        deploymentParametersSearch.OPENSEARCH_VERSION = OPENSEARCH_VERSION
        deploymentParametersSearch.DX_DEPLOYMENT_HOST = "${INSTANCE_NAME}${DOMAIN_SUFFIX}"
        deploymentParametersSearch.DX_CONTENT_SOURCE_HOST = "dx-deployment-core:10042"
        deploymentParametersSearch.HELM_CHARTS_URL = commonModule.determineHelmChartsURLSearch()
        
        applyHelmActionSearch(deploymentParametersSearch, 'install')

        /*
        configure the contentSource for WCM which will crawl the deployed instance and will run every 15 minutes
        */
        if (commonConfig.COMMON_CONFIGURE_OPENSEARCH == 'true') {
            dir("${workspace}/kube/lifecycle/scripts/common/") {
                
                def SEARCH_HOST = "https://${deploymentParametersSearch.DX_DEPLOYMENT_HOST}"
                def CONTENT_SOURCE_HOST = "https://${deploymentParametersSearch.DX_CONTENT_SOURCE_HOST}"
                println "SEARCH_HOST :- ${SEARCH_HOST}"
                println "CONTENT_SOURCE_HOST :- ${CONTENT_SOURCE_HOST}"
                CURL_RESPONSE = sh (script: "./opensearch_authentication.sh ${SEARCH_HOST} ${commonConfig.COMMON_CUSTOM_SEARCH_ADMIN_USER} ${commonConfig.COMMON_CUSTOM_SEARCH_ADMIN_PASSWORD}", returnStdout: true)
                println "Authentication curl Response :- ${CURL_RESPONSE}"
                CURL_RESPONSE = CURL_RESPONSE.replace("\n","")
                def JSON_CURL_RESPONSE = readJSON text: CURL_RESPONSE
                TOKEN = JSON_CURL_RESPONSE.jwt
                println "JWT TOKEN :- ${TOKEN}"

                CURL_RESPONSE = sh (script: "./opensearch_contentsources.sh ${SEARCH_HOST} ${TOKEN} ${CONTENT_SOURCE_HOST}", returnStdout: true)
                println "Contentsources curl Response :${CURL_RESPONSE}"
                CURL_RESPONSE = CURL_RESPONSE.replace("\n","")
                def JSON_CURL_RESPONSE_CONTENTSOURCES = readJSON text: CURL_RESPONSE
                CONTENT_SOURCE_ID = JSON_CURL_RESPONSE_CONTENTSOURCES.id
                println "CONTENT_SOURCE_ID :- ${CONTENT_SOURCE_ID}"

                CURL_RESPONSE = sh (script: "./opensearch_crawlers.sh ${SEARCH_HOST} ${TOKEN} ${CONTENT_SOURCE_ID} ${CONTENT_SOURCE_HOST}", returnStdout: true)
                println "Crawler curl Response :${CURL_RESPONSE}"
                CURL_RESPONSE = CURL_RESPONSE.replace("\n","")
                def JSON_CURL_RESPONSE_CRAWLERS = readJSON text: CURL_RESPONSE
                CRAWLER_ID = JSON_CURL_RESPONSE_CRAWLERS.id
                println "CRAWLER_ID :- ${CRAWLER_ID}"

                CURL_RESPONSE = sh (script: "./opensearch_crawlers_trigger.sh ${SEARCH_HOST} ${TOKEN} ${CRAWLER_ID}", returnStdout: true)
                println "Crawlers trigger Response :${CURL_RESPONSE}"

                sleep 120
                CURL_RESPONSE = sh (script: "./opensearch_search.sh ${SEARCH_HOST} ${TOKEN} ${CONTENT_SOURCE_ID}", returnStdout: true)
                CURL_RESPONSE = CURL_RESPONSE.replace("\n","")
                def JSON_CURL_RESPONSE_SEARCH = readJSON text: CURL_RESPONSE
                TOTAL_ITEMS = JSON_CURL_RESPONSE_SEARCH.hits.total.value
                println "TOTAL_ITEMS :- ${TOTAL_ITEMS}"
            }
        }
    }
    
}

def applyHelmActionSearch(deploymentParametersSearch, actionType = "install") {
    if (actionType != 'install' && actionType != 'upgrade') {
        error "Provided invalid actionType > ${actionType} < to Helm."
    }
    println "Preparing action >  ${actionType} < with Helm."

    // For harbor we can pull the helm chart differently, without relying on the HELM_CHARTS_URL
    def helmChartVersion = ''
    if (commonConfig.COMMON_IMAGE_REPOSITORY == 'hclcr.io') {
        // We use helm pull to retrieve the helm chart from harbor and then unpack it
        sh """
            helm pull harbor-dx/hcl-dx-deployment --version ${commonConfig.COMMON_HELM_CHARTS_FILTER}
            tar zxvf hcl-dx-deployment-${commonConfig.COMMON_HELM_CHARTS_FILTER}.tgz
        """
    } else {
        sh """
            curl -s ${deploymentParametersSearch.HELM_CHARTS_URL} --output dxHelmChart.tgz
            tar zxvf dxHelmChart.tgz
        """
    }

    // Determine CF level from helm chart
    helmChartVersion = getSearchHelmChartVersion()

    // Prepare PV configuration
    preparePersistenceValues(deploymentParametersSearch)

    // Configure parameters based on deployment type (hybrid or non hybrid)
    if (commonConfig.COMMON_HYBRID == 'true') {
        println "This is a HYBRID deployment."
        deploymentParametersSearch.CORE_HOST = commonConfig.COMMON_HYBRID_HOST
        deploymentParametersSearch.OTHER_HOST = deploymentParametersSearch.DX_DEPLOYMENT_HOST
        deploymentParametersSearch.CORE_PORT = commonConfig.COMMON_HYBRID_PORT
        deploymentParametersSearch.OTHER_PORT = '443'
        deploymentParametersSearch.ENABLE_CORE = 'false'
        if (!deploymentParametersSearch.DX_HYBRID_HOST_CORS || deploymentParametersSearch.DX_HYBRID_HOST_CORS == '') {
            deploymentParametersSearch.DX_HYBRID_HOST_CORS = "https://${deploymentParametersSearch.CORE_HOST},https://${deploymentParametersSearch.CORE_HOST}:${deploymentParametersSearch.CORE_PORT}"
        }
        // For hybrid we inherit DX_HYBRID_HOST_CORS from the calling function
    } else {
        println "This is NOT a HYBRID deployment."
        deploymentParametersSearch.CORE_HOST = deploymentParametersSearch.DX_DEPLOYMENT_HOST
        deploymentParametersSearch.OTHER_HOST = deploymentParametersSearch.DX_DEPLOYMENT_HOST
        deploymentParametersSearch.CORE_PORT = '443'
        deploymentParametersSearch.OTHER_PORT = '443'
        deploymentParametersSearch.DX_HYBRID_HOST_CORS = ''
        deploymentParametersSearch.ENABLE_CORE = !commonConfig.COMMON_DISABLE_DX_CORE.toBoolean()
    }

    // For non artifactory repos, this is sufficient
    def imageRepository = commonConfig.COMMON_IMAGE_REPOSITORY
    // For artifactory repos, we have to build the appropriate repository string
    if (commonConfig.COMMON_IMAGE_REPOSITORY != commonConfig.COMMON_IMAGE_AREA) {
        imageRepository = "${commonConfig.COMMON_IMAGE_REPOSITORY}.${commonConfig.COMMON_IMAGE_AREA}"
    }

    // Enable SSL and use LoadBalancer Service by default
    def enableSSL = "true"
    def serviceType = "LoadBalancer"
    // Disable SSL and use ClusterIP when Ingress is deployed
    if (commonConfig.COMMON_ENABLE_INGRESS != "false") {
      enableSSL = "false"
      serviceType = "ClusterIP"
    }
    //Check if content AI provider is XAI_INTERNAL
    def isXAIInternal
    if (commonConfig.COMMON_ENABLE_AI == "true" && commonConfig.COMMON_IS_XAI_INTERNAL_AI == "true") {
        isXAIInternal = "true"
    } else {
        isXAIInternal = "false"
    }

    // Preparation for values yaml
    sh """
        cd hcl-dx-search
        cp ${workspace}/kube/lifecycle/scripts/common/cf${helmChartVersion}-deploy-search-values.yaml ./
        ##sed to values.yaml
        sed -i "
            s|REPOSITORY_NAME|${imageRepository}|g;
            s|OPENSEARCH_IMAGE_NAME|${commonConfig.COMMON_OPENSEARCH_IMAGE_PATH}|g;
            s|SEARCH_MIDDLEWARE_IMAGE_NAME|${commonConfig.COMMON_SEARCH_MIDDLEWARE_IMAGE_PATH}|g;
            s|OPENSEARCH_IMAGE_TAG|${deploymentParametersSearch.OPENSEARCH_IMAGE}|g;   
            s|SEARCH_MIDDLEWARE_IMAGE_TAG|${deploymentParametersSearch.SEARCH_MIDDLEWARE_IMAGE}|g;   
            s|SEARCH_ADMIN_CREDENTIALS_SECRET_NAME|${commonConfig.SEARCH_ADMIN_CUSTOM_SECRET}|g;   
            s|SEARCH_PUSH_ADMIN_CREDENTIALS_SECRET_NAME|${commonConfig.SEARCH_PUSH_CUSTOM_SECRET}|g;   
        " ./cf${helmChartVersion}-deploy-search-values.yaml
    """

    // Create a secrets for openSearch authentication
    sh """
        # Root CA
        openssl genrsa -out root-ca-key.pem 2048
        openssl req -new -x509 -sha256 -key root-ca-key.pem -subj "/C=US/O=ORG/OU=UNIT/CN=opensearch" -out root-ca.pem -days 730

        # Admin cert
        openssl genrsa -out admin-key-temp.pem 2048
        openssl pkcs8 -inform PEM -outform PEM -in admin-key-temp.pem -topk8 -nocrypt -v1 PBE-SHA1-3DES -out admin-key.pem
        openssl req -new -key admin-key.pem -subj "/C=US/O=ORG/OU=UNIT/CN=A" -out admin.csr
        openssl x509 -req -in admin.csr -CA root-ca.pem -CAkey root-ca-key.pem -CAcreateserial -sha256 -out admin.pem -days 730

        # Node cert
        openssl genrsa -out node-key-temp.pem 2048
        openssl pkcs8 -inform PEM -outform PEM -in node-key-temp.pem -topk8 -nocrypt -v1 PBE-SHA1-3DES -out node-key.pem
        openssl req -new -key node-key.pem -subj "/C=US/O=ORG/OU=UNIT/CN=opensearch-node" -out node.csr
        openssl x509 -req -in node.csr -CA root-ca.pem -CAkey root-ca-key.pem -CAcreateserial -sha256 -out node.pem -days 730

        # Client cert
        openssl genrsa -out client-key-temp.pem 2048
        openssl pkcs8 -inform PEM -outform PEM -in client-key-temp.pem -topk8 -nocrypt -v1 PBE-SHA1-3DES -out client-key.pem
        openssl req -new -key client-key.pem -subj "/C=US/O=ORG/OU=UNIT/CN=opensearch-client" -out client.csr
        openssl x509 -req -in client.csr -CA root-ca.pem -CAkey root-ca-key.pem -CAcreateserial -sha256 -out client.pem -days 730

        # Delete old secrets
        kubectl delete secret search-admin-cert -n ${commonConfig.NAMESPACE} || true
        kubectl delete secret search-node-cert -n ${commonConfig.NAMESPACE} || true
        kubectl delete secret search-client-cert -n ${commonConfig.NAMESPACE} || true

        # Create kubernetes secrets
        kubectl create secret generic search-admin-cert --from-file=admin.pem --from-file=admin-key.pem --from-file=root-ca.pem -n ${commonConfig.NAMESPACE}
        kubectl create secret generic search-node-cert --from-file=node.pem --from-file=node-key.pem --from-file=root-ca.pem -n ${commonConfig.NAMESPACE}
        kubectl create secret generic search-client-cert --from-file=client.pem --from-file=client-key.pem --from-file=root-ca.pem -n ${commonConfig.NAMESPACE}
    """

    // Different deployment strategy for harbor vs. non-harbor
    // In harbor we can completely remove the image names and tags from our custom values yaml
    // Both are provided by the helm chart itself, thus not requiring us to add anything
    dir('hcl-dx-search') {
        println "Performing ${actionType} of artifactory based helm chart"
            sh """
                helm ${actionType} dx-search -n ${commonConfig.NAMESPACE} . -f ./cf${helmChartVersion}-deploy-search-values.yaml ${getCustomValuesOverride()}
            """
        // copy the custom values yaml to the remote instance, so developers can reference it for native kube
        if (commonConfig.COMMON_KUBE_FLAVOUR == 'native') {
            // Wrap in config file for SSH access to remote machine
            configFileProvider([
                configFile(
                    fileId: 'test-automation-deployments',
                    variable: 'DEPLOY_KEY'
                )
            ]) {
                // Get the deployed custom values from the actual deployment
                sh """
                    helm get values dx-search -n ${commonConfig.NAMESPACE} -o yaml > ../merged-deploy-values.yaml
                """

                def targetHost = deploymentParametersSearch.DX_DEPLOYMENT_HOST
                // For clustered native kube environments we copy the content to the control plane
                if (commonConfig.ENABLE_DISTRIBUTED_CONFIGURATION == 'true') {
                    targetHost = "${env.INSTANCE_NAME}-cp${commonConfig.COMMON_DOMAIN_SUFFIX}"
                }
                // Using deploymentParametersSearch.DX_DEPLOYMENT_HOST as the target host
                sh """
                    chmod 600 ${DEPLOY_KEY}
                    scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ../merged-deploy-values.yaml centos@${targetHost}:/home/centos/native-kube/${actionType}-deploy-search-values.yaml
                    rm ./cf${helmChartVersion}-deploy-search-values.yaml
                    scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r ./ centos@${targetHost}:/home/centos/native-kube/${actionType}-hcl-dx-search
                """
            }
        }
    }

    // Use fitting retry count
    def retryCount = commonConfig.DX_FRESH_PROBE_RETRIES
    if (actionType == 'upgrade') {
        retryCount = commonConfig.DX_UPDATE_PROBE_RETRIES
    }

    dxPodsCheckReadiness(
        namespace: commonConfig.NAMESPACE,
        lookupInterval: 30,
        lookupTries: retryCount.toInteger(),
        pendingLimit: 30,
        containerCreateLimit: 60,
        safetyInterval: 120
    )
}

def isHelmChartInstalled(releaseName, namespace = commonConfig.NAMESPACE) {
    def helmList = ""
    try {
        helmList = sh(
            script: "helm list -n ${namespace} | grep ${releaseName}", 
            returnStdout: true
        ).trim()
    } catch (Exception err) {
        echo "Error while getting helm list for ${releaseName} in namespace ${namespace}"
        return false
    }
    return helmList != ""
}

/* Mandatory return statement on EOF */
return this
