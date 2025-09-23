/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2021. All Rights Reserved. *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */
ARTIFACT_SERVER_URL_PREFIX = "https://artifactory.cwp.pnp-hcl.com/artifactory/"
commonConfig = load "./autonomous-deployments/config/common.gvy"

def retrieveMasterImageCreationDate(CURRENT_BUILD_FILTER,CURRENT_BUILD_NUMBER) {

    CURRENT_BUILD_IMAGE_TAG_CORE  = retrieveImageTag(commonConfig.COMMON_CORE_IMAGE_PATH, CURRENT_BUILD_FILTER);
    CURRENT_BUILD_IMAGE_TAG_RS  = retrieveImageTag(commonConfig.COMMON_RS_IMAGE_PATH, CURRENT_BUILD_FILTER);

    CURRENT_BUILD_DATE = CURRENT_BUILD_IMAGE_TAG_CORE.split("-")[0].split("_")[2]

    def imageTagMap = [:]
    imageTagMap.putAt(commonConfig.COMMON_CORE_IMAGE_PATH, CURRENT_BUILD_IMAGE_TAG_CORE)
    imageTagMap.putAt(commonConfig.COMMON_RS_IMAGE_PATH, CURRENT_BUILD_IMAGE_TAG_RS)

    if (CURRENT_BUILD_NUMBER <= 199) {
        CURRENT_BUILD_IMAGE_TAG_CORE_OPERATOR  = retrieveImageTag(commonConfig.COMMON_CORE_OPERATOR_IMAGE_PATH, CURRENT_BUILD_FILTER);
        CURRENT_BUILD_IMAGE_TAG_DAM_OPERATOR  = retrieveImageTag(commonConfig.COMMON_DAM_OPERATOR_IMAGE_PATH, CURRENT_BUILD_FILTER);
        imageTagMap.putAt(commonConfig.COMMON_CORE_OPERATOR_IMAGE_PATH, CURRENT_BUILD_IMAGE_TAG_CORE_OPERATOR)
        imageTagMap.putAt(commonConfig.COMMON_DAM_OPERATOR_IMAGE_PATH, CURRENT_BUILD_IMAGE_TAG_DAM_OPERATOR)
    }
    imageTagMap.each { imageTag ->
        echo "${imageTag.key}: ${imageTag.value}"
        if ((imageTag.value).split("-")[0].split("_")[2] < CURRENT_BUILD_DATE) {
            CURRENT_BUILD_DATE = (imageTag.value).split("-")[0].split("_")[2]
        }
    }
    return CURRENT_BUILD_DATE;
}


def retrieveOpenLDAPMasterImageTag(IMAGE_TAG, IMAGE_PATH, UPDATE_RELEASE_LEVEL) {
    CURRENT_BUILD_NUMBER = IMAGE_TAG.split("-")[0].split("_")[1].split("CF")[1].toInteger()
    CURRENT_BUILD_LABEL_CF = "v95_CF${CURRENT_BUILD_NUMBER}"
    CURRENT_BUILD_FILTER = "${CURRENT_BUILD_LABEL_CF}.*.rivendell_master"

    CURRENT_BUILD_IMAGE_TAG_CORE  = retrieveImageTag(commonConfig.COMMON_CORE_IMAGE_PATH, CURRENT_BUILD_FILTER);
    CURRENT_BUILD_IMAGE_TAG_CORE_OPERATOR  = retrieveImageTag(commonConfig.COMMON_CORE_OPERATOR_IMAGE_PATH, CURRENT_BUILD_FILTER);

    CORE_CF_IMAGE_DATE = CURRENT_BUILD_IMAGE_TAG_CORE.split("-")[0].split("_")[2]
    CORE_OPERATOR_CF_IMAGE_DATE = CURRENT_BUILD_IMAGE_TAG_CORE_OPERATOR.split("-")[0].split("_")[2]

    CURRENT_BUILD_IMAGE_TAG_OPENLDAP = retrieveImageTagLDAP(commonConfig.COMMON_LDAP_IMAGE_PATH, "master_${CORE_CF_IMAGE_DATE}");
    LATEST_BUILD_IMAGE_TAG_OPENLDAP = CURRENT_BUILD_IMAGE_TAG_OPENLDAP

    CURRENT_BUILD_IMAGE_TAG_OPENLDAP = retrieveImageTagLDAP(commonConfig.COMMON_LDAP_IMAGE_PATH, "master_${CORE_OPERATOR_CF_IMAGE_DATE}");

    if(LATEST_BUILD_IMAGE_TAG_OPENLDAP.length() < 1 && CURRENT_BUILD_IMAGE_TAG_OPENLDAP.length() >= 1){
            LATEST_BUILD_IMAGE_TAG_OPENLDAP = CURRENT_BUILD_IMAGE_TAG_OPENLDAP
    }

    if(LATEST_BUILD_IMAGE_TAG_OPENLDAP.length() >= 1 && CURRENT_BUILD_IMAGE_TAG_OPENLDAP.length() >= 1){
        if(CURRENT_BUILD_IMAGE_TAG_OPENLDAP.split("-")[1].split("_")[1] > LATEST_BUILD_IMAGE_TAG_OPENLDAP.split("-")[1].split("_")[1]){
            LATEST_BUILD_IMAGE_TAG_OPENLDAP = CURRENT_BUILD_IMAGE_TAG_OPENLDAP
        }
    }

    if(CURRENT_BUILD_IMAGE_TAG_OPENLDAP.length() < 1 && LATEST_BUILD_IMAGE_TAG_OPENLDAP.length() < 1){
        error("Could not find matching tag for image path ${commonConfig.COMMON_LDAP_IMAGE_PATH} and filter master_${CORE_OPERATOR_CF_IMAGE_DATE} or master_${CORE_OPERATOR_CF_IMAGE_DATE}")
    }

    echo "LATEST_BUILD_IMAGE_TAG_OPENLDAP:- ${LATEST_BUILD_IMAGE_TAG_OPENLDAP}"
    return LATEST_BUILD_IMAGE_TAG_OPENLDAP;
}

/*
 * Get the URL to the helm charts to be used in deployment
 */
def retrievePrevHelmChartImageTag(DEPLOYMENT_LEVEL, FILTER_TEXT_ADDONS) {
    HELM_CHARTS_AREA="${commonConfig.COMMON_HELM_CHARTS_AREA}"
    if (DEPLOYMENT_LEVEL.matches("(.*)rivendell(.*)|(.*)master(.*)|(.*)release(.*)")){
        HELM_CHARTS_AREA="${commonConfig.COMMON_HELM_CHARTS_AREA}-prod"  
    }
    /* Get correct artifactory path  */
    artifactoryChartsListURL = "${ARTIFACT_SERVER_URL_PREFIX}list/${HELM_CHARTS_AREA}"

    IMAGE_FILTER = "${FILTER_TEXT_ADDONS}.*.${DEPLOYMENT_LEVEL}"

    echo "Chart Filter : ${IMAGE_FILTER}"
    chartsFilename= retrieveHelmChartImageTag(commonConfig.COMMON_HELM_CHARTS_PATH, IMAGE_FILTER, artifactoryChartsListURL);

    if (chartsFilename.length() < 1) {
        error("Could not find a Helm charts package")
    }
    echo "chartsFilename : ${chartsFilename}"
    return chartsFilename;
}

def retrievePrevCFMasterImageTag(IMAGE_TAG, IMAGE_PATH, UPDATE_RELEASE_LEVEL) {
    CURRENT_BUILD_NUMBER = IMAGE_TAG.split("-")[0].split("_")[1].split("CF")[1].toInteger()
    CURRENT_BUILD_LABEL_CF = "v95_CF${CURRENT_BUILD_NUMBER - UPDATE_RELEASE_LEVEL.toInteger()}"
    if(IMAGE_PATH == commonConfig.COMMON_CORE_OPERATOR_IMAGE_PATH && CURRENT_BUILD_LABEL_CF.startsWith("v95_CF194")){
        return 'v95_CF194_20210416-0233_rivendell_master'
    }
    CURRENT_BUILD_FILTER = "${CURRENT_BUILD_LABEL_CF}.*.rivendell_master"
    CURRENT_BUILD_IMAGE_TAG  = retrieveImageTag(IMAGE_PATH, CURRENT_BUILD_FILTER);
    return CURRENT_BUILD_IMAGE_TAG
}

def retrievePrevCFMasterImageTagNew(CURRENT_RELEASE, IMAGE_PATH, UPDATE_RELEASE_LEVEL) {
    PREVIOUS_BUILD_LABEL_CF = "v95_CF${CURRENT_RELEASE.toInteger() - UPDATE_RELEASE_LEVEL.toInteger()}"
    if(IMAGE_PATH == commonConfig.COMMON_CORE_OPERATOR_IMAGE_PATH) {
        if (PREVIOUS_BUILD_LABEL_CF.startsWith("v95_CF194")) {
            return 'v95_CF194_20210416-0233_rivendell_master'
        }
        if (PREVIOUS_BUILD_LABEL_CF.startsWith("v95_CF2")) {
            return 'v95_CF199_20211118-0034_rivendell_master'
        }
    }
    if(IMAGE_PATH == commonConfig.COMMON_DAM_OPERATOR_IMAGE_PATH && PREVIOUS_BUILD_LABEL_CF.startsWith("v95_CF2")){
        return 'v95_CF199_20211029-1342_rivendell_master'
    }
    PREVIOUS_BUILD_FILTER = "${PREVIOUS_BUILD_LABEL_CF}.*.rivendell_master"
    PREVIOUS_BUILD_IMAGE_TAG  = retrieveImageTag(IMAGE_PATH, PREVIOUS_BUILD_FILTER);
    return PREVIOUS_BUILD_IMAGE_TAG
}

def retrievePrevAddOnMasterImagesDate(IMAGE_TAG, UPDATE_RELEASE_LEVEL) {
    CURRENT_BUILD_NUMBER = IMAGE_TAG.split("-")[0].split("_")[1].split("CF")[1].toInteger()
    prevBuildNumber = CURRENT_BUILD_NUMBER - UPDATE_RELEASE_LEVEL.toInteger()
    CURRENT_BUILD_LABEL_FOR_ADD_ONS = "v95_CF${prevBuildNumber}"
    CURRENT_BUILD_FILTER = "${CURRENT_BUILD_LABEL_FOR_ADD_ONS}.*.rivendell_master"
    CURRENT_BUILD_DATE = retrieveMasterImageCreationDate(CURRENT_BUILD_FILTER,CURRENT_BUILD_NUMBER)
    // Backport from new automation, we remove the day part of the returned date
    // 20210922 => 202109
    // This will allow for master image lookups, if there has been no build run of all images on one day
    return CURRENT_BUILD_DATE.substring(0,6);
}

def retrieveAddOnMasterImagesDate(IMAGE_TAG) {
    CURRENT_BUILD_DATE = IMAGE_TAG.split("-")[0].split("_")[2]
    // Backport from new automation, we remove the day part of the returned date
    // 20210922 => 202109
    // This will allow for master image lookups, if there has been no build run of all images on one day
    return CURRENT_BUILD_DATE.substring(0,6);
}

def retrieveHelmChartImageTag(imagePath, imageFilter, imageArea) {
    dir("${workspace}/autonomous-deployments/scripts/") {
        tag = sh (script: "./get_latest_image_wrapper.sh ${imagePath} ${imageFilter} ${imageArea}", returnStdout: true)
        if (tag.length() < 1) {
            error("Could not find matching tag for image path ${imagePath} and filter ${imageFilter}")
        }
        return tag;
    }
}

def retrieveImageTag(imagePath, imageFilter) {
    dir("${workspace}/autonomous-deployments/scripts/") {
        tag = sh (script: "./get_latest_image_wrapper.sh ${imagePath} ${imageFilter} ${commonConfig.COMMON_IMAGE_AREA}", returnStdout: true)
        if (tag.length() < 1) {
            error("Could not find matching tag for image path ${imagePath} and filter ${imageFilter}")
        }
        return tag;
    }
}

def retrieveImageTagLDAP(imagePath, imageFilter) {
    dir("${workspace}/autonomous-deployments/scripts/") {
        tag = sh (script: "./get_latest_image_wrapper.sh ${imagePath} ${imageFilter} ${commonConfig.COMMON_IMAGE_AREA} true", returnStdout: true)
        return tag;
    }
}

def createAcceptanceTestJobParams(NAMESPACE, KUBE_FLAVOUR, DEPLOYMENT_LEVEL, DEPLOYMENT_TYPE, ONPREMISE_HOST_IP, CONTEXT_ROOT_PATH, DX_CORE_HOME_PATH) {
    echo "AUTONOMUS DEPLOYMENT NAMESPACE: ${NAMESPACE}"
    sleep 1000
    HOSTNAME = "${NAMESPACE}${DOMAIN_SUFFIX}"
    INTERNAL_SERVICE = "${NAMESPACE}${DOMAIN_SUFFIX}"
    DXCONNECT_HOST = "${NAMESPACE}${DOMAIN_SUFFIX}"
    if (KUBE_FLAVOUR == "native") {
        HOSTNAME = "${NAMESPACE}${DOMAIN_SUFFIX}"
        INTERNAL_SERVICE = "${NAMESPACE}${DOMAIN_SUFFIX}"
        DXCONNECT_HOST = "${NAMESPACE}${DOMAIN_SUFFIX}"
    }
    if (KUBE_FLAVOUR == "openshift") {
        // For Helm we use a passthrough route, which goes to Ambassador, so the name is different
        // Endpoints for internal services as well as for DXConnect is the same
        if (DEPLOYMENT_METHOD == "helm") {
            HOSTNAME = "${NAMESPACE}${DOMAIN_SUFFIX}"
            INTERNAL_SERVICE = "dx-deployment-passthrough-${HOSTNAME}"
            DXCONNECT_HOST = "dx-deployment-passthrough-${HOSTNAME}"
        } else {
            HOSTNAME = "${NAMESPACE}${DOMAIN_SUFFIX}"
            INTERNAL_SERVICE = "dx-deployment-service-${NAMESPACE}-${NAMESPACE}${DOMAIN_SUFFIX}"
            DXCONNECT_HOST = "dx-deployment-service-dxconnect-${NAMESPACE}${DOMAIN_SUFFIX}"
        }
    }
    if(DEPLOYMENT_TYPE == "hybrid" && KUBE_FLAVOUR != "openshift") {
        if (KUBE_FLAVOUR == "native") {
            KUBE_DOMAIN_SUFFIX = "${KUBE_DOMAIN_SUFFIX}"
            DX_DOMAIN_SUFFIX = "${DOMAIN_SUFFIX}"
            HOSTNAME = "${NAMESPACE}-onprem${DX_DOMAIN_SUFFIX}"
            DXCONNECT_HOST = "${NAMESPACE}-onprem${DX_DOMAIN_SUFFIX}"
        } else {
            KUBE_DOMAIN_SUFFIX = "${KUBE_DOMAIN_SUFFIX}"
            DX_DOMAIN_SUFFIX = "${DOMAIN_SUFFIX}"
            HOSTNAME = "${NAMESPACE}${DX_DOMAIN_SUFFIX}"
        }
        INTERNAL_SERVICE = "${NAMESPACE}${KUBE_DOMAIN_SUFFIX}"
    }

    persistence_img_filter="rivendell_${DEPLOYMENT_LEVEL}"
    image_repo = "quintana-docker-prod"
    if(DEPLOYMENT_LEVEL == "master") {
        branch= "master"
    } else if(DEPLOYMENT_LEVEL == "release") {
        coreTag = retrieveImageTag(commonConfig.COMMON_CORE_IMAGE_PATH, 'rivendell_release');
        coreTag= coreTag - 'v'
        version=coreTag.split("_",3)
        branch= "release/${version[0]}_${version[1]}"
    } else {
        branch="develop"
        persistence_img_filter=""
        image_repo = "quintana-docker"
    }

    artifactory_img_base_url="https://artifactory.cwp.pnp-hcl.com/artifactory/list/${image_repo}"

    echo "TARGET BRANCH:- ${branch}"

    buildParams = []
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'TEST_DX_CORE',
         value: true])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'TEST_RING',
         value: true])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'TEST_CC',
         value: true])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'TEST_DAM',
         value: true])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'TEST_DXCLIENT',
         value: true])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'SSL_ENABLED',
         value: true])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'TEST_URL_LOCALE',
         value: false])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'TEST_THEME_EDITOR',
         value: true])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'TEST_DAM_SERVER',
         value: true])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'TARGET_BRANCH',
         value: branch])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'TEST_CR',
         value: true])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'TEST_PICKER',
         value: true])

    if(DEPLOYMENT_TYPE == "hybrid") {
        DXCONNECT_HOST = "${HOSTNAME}:10202"
        buildParams.add(
            [$class: 'StringParameterValue',
         name: 'HOST_IP_ADDRESS',
         value: "${ONPREMISE_HOST_IP}"])
        buildParams.add(
            [$class: 'StringParameterValue',
         name: 'HOSTNAME',
         value: "${HOSTNAME}"])
    }

    FULL_CONTEXT_ROOT_PATH = "/${CONTEXT_ROOT_PATH}/${DX_CORE_HOME_PATH}"
    WCM_REST_CONTEXT_ROOT = "/${CONTEXT_ROOT_PATH}"

    if(!CONTEXT_ROOT_PATH) {
        FULL_CONTEXT_ROOT_PATH = ""
        WCM_REST_CONTEXT_ROOT=""
    }

    if(KUBE_FLAVOUR == "openshift" && DEPLOYMENT_TYPE == "kube") {
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

    if(DEPLOYMENT_TYPE == "hybrid" && KUBE_FLAVOUR != "openshift") {
        buildParams.add(
        [$class: 'StringParameterValue',
         name: 'WCMREST',
         value: "https://${HOSTNAME}${WCM_REST_CONTEXT_ROOT}"])
    } else {
        buildParams.add(
        [$class: 'StringParameterValue',
         name: 'WCMREST',
         value: "https://${INTERNAL_SERVICE}${WCM_REST_CONTEXT_ROOT}"])
    }

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

def retrieveCurrentCFVersion(CURRENT_MASTER_VERSION) {
    echo "CURRENT_MASTER_VERSION: ${CURRENT_MASTER_VERSION}"
    CF_BUILD_NUMBER = CURRENT_MASTER_VERSION.split("CF")[1].toInteger()
    return CF_BUILD_NUMBER;
}

def prepareK8SpecificParams(IS_HYBRID, DEPLOYMENT_LEVEL, KUBE_FLAVOUR) {
        buildParams = []
        if(DEPLOYMENT_LEVEL == "develop") {
            env.CORE_IMAGE_PATH = "dx-build-output/core/dxen"
            env.DAM_IMAGE_PATH = "dx-build-output/core-addon/media-library"
            env.CC_IMAGE_PATH = "dx-build-output/core-addon/content-ui"
            env.DAM_PLUGIN_GOOGLE_VISION_IMAGE_PATH = "dx-build-output/core-addon/api/dam-plugin-google-vision"
            env.RINGAPI_IMAGE_PATH = "dx-build-output/core-addon/api/ringapi"
            env.IMGPROC_IMAGE_PATH = "dx-build-output/core-addon/image-processor"
            env.PERSISTENCE_IMAGE_PATH = "dx-build-output/core-addon/persistence/postgres"
            env.PERSISTENCE_CONNECTION_POOL_IMAGE_PATH = "dx-build-output/core-addon/persistence/pgpool"
            env.PERSISTENCE_NODE_IMAGE_PATH = "dx-build-output/core-addon/persistence/postgresrepmgr"
            env.PERSISTENCE_METRICS_EXPORTER_IMAGE_PATH = "dx-build-output/core-addon/persistence/postgres-metrics-exporter"
            env.DESIGN_STUDIO_IMAGE_PATH = "dx-build-output/core-addon/site-manager"
            env.RUNTIME_CONTROLLER_IMAGE_PATH = "dx-build-output/operator/hcldx-runtime-controller"
            env.CORE_OPERATOR_IMAGE_PATH = "dx-build-output/operator/hcldx-cloud-operator"
            env.DAM_OPERATOR_IMAGE_PATH = "dx-build-output/operator/hcl-medialibrary-operator"
            env.LDAP_IMAGE_PATH = "dx-build-output/core-addon/dx-openldap"
            env.SIDECAR_IMAGE_PATH = "dx-build-output/common/dxubi"
            env.DAM_KALTURA_PLUIGN_IMAGE_PATH = "dx-build-output/core-addon/api/dam-plugin-kaltura"
        } else {
            env.CORE_IMAGE_PATH = "dxen"
            env.DAM_IMAGE_PATH = "portal/media-library"
            env.CC_IMAGE_PATH = "portal/content-ui"
            env.DAM_PLUGIN_GOOGLE_VISION_IMAGE_PATH = "portal/api/dam-plugin-google-vision"
            env.RINGAPI_IMAGE_PATH = "portal/api/ringapi"
            env.IMGPROC_IMAGE_PATH = "portal/image-processor"
            env.PERSISTENCE_IMAGE_PATH = "portal/persistence/postgres"
            env.PERSISTENCE_CONNECTION_POOL_IMAGE_PATH = "dx-build-output/core-addon/persistence/pgpool"
            env.PERSISTENCE_NODE_IMAGE_PATH = "dx-build-output/core-addon/persistence/postgresrepmgr"
            env.PERSISTENCE_METRICS_EXPORTER_IMAGE_PATH = "dx-build-output/core-addon/persistence/postgres-metrics-exporter"
            env.DESIGN_STUDIO_IMAGE_PATH = "dx-build-output/core-addon/site-manager"
            env.RUNTIME_CONTROLLER_IMAGE_PATH = "dx-build-output/operator/hcldx-runtime-controller"
            env.CORE_OPERATOR_IMAGE_PATH = "hcldx-cloud-operator"
            env.DAM_OPERATOR_IMAGE_PATH = "hcl-medialibrary-operator"
            env.LDAP_IMAGE_PATH = "dx-openldap"
            env.SIDECAR_IMAGE_PATH = "dx-build-output/common/dxubi"
            env.DAM_KALTURA_PLUIGN_IMAGE_PATH = "dx-build-output/core-addon/api/dam-plugin-kaltura"
        }
        env.AMBASSADOR_IMAGE_PATH = "dx-build-output/common/ambassador"
        env.HAPROXY_IMAGE_PATH = "dx-build-output/common/haproxy"
        env.REDIS_IMAGE_PATH = "dx-build-output/common/redis"
        env.LOGGING_SIDECAR_IMAGE_PATH = "dx-build-output/common/logging-sidecar"
        env.PREREQS_CHECKER_IMAGE_PATH = "dx-build-output/common/prereqs-checker"

        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'CORE_IMAGE_PATH',
            value: "${env.CORE_IMAGE_PATH}"])
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'DAM_IMAGE_PATH',
            value: "${env.DAM_IMAGE_PATH}"])
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'CC_IMAGE_PATH',
            value: "${env.CC_IMAGE_PATH}"])
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'DAM_PLUGIN_GOOGLE_VISION_IMAGE_PATH',
            value: "${env.DAM_PLUGIN_GOOGLE_VISION_IMAGE_PATH}"])
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'RINGAPI_IMAGE_PATH',
            value: "${env.RINGAPI_IMAGE_PATH}"])
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'IMGPROC_IMAGE_PATH',
            value: "${env.IMGPROC_IMAGE_PATH}"])
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'DAM_KALTURA_PLUIGN_IMAGE_PATH',
            value: "${env.DAM_KALTURA_PLUIGN_IMAGE_PATH}"])
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'PERSISTENCE_IMAGE_PATH',
            value: "${env.PERSISTENCE_IMAGE_PATH}"])
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'PERSISTENCE_NODE_IMAGE_PATH',
            value: "${env.PERSISTENCE_NODE_IMAGE_PATH}"])
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'PERSISTENCE_METRICS_EXPORTER_IMAGE_PATH',
            value: "${env.PERSISTENCE_METRICS_EXPORTER_IMAGE_PATH}"])
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'PERSISTENCE_CONNECTION_POOL_IMAGE_PATH',
            value: "${env.PERSISTENCE_CONNECTION_POOL_IMAGE_PATH}"])
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'DESIGN_STUDIO_IMAGE_PATH',
            value: "${env.DESIGN_STUDIO_IMAGE_PATH}"])
        if ((KUBE_FLAVOUR != "native") || (DEPLOYMENT_METHOD == "helm")) {
            buildParams.add(
                [$class: 'StringParameterValue',
                name: 'RUNTIME_CONTROLLER_IMAGE_PATH',
                value: "${env.RUNTIME_CONTROLLER_IMAGE_PATH}"])
        }
        if ((KUBE_FLAVOUR != "native") || (DEPLOYMENT_METHOD != "helm")) {
            buildParams.add(
                [$class: 'StringParameterValue',
                name: 'CORE_OPERATOR_IMAGE_PATH',
                value: "${env.CORE_OPERATOR_IMAGE_PATH}"])
            buildParams.add(
                [$class: 'StringParameterValue',
                name: 'DAM_OPERATOR_IMAGE_PATH',
                value: "${env.DAM_OPERATOR_IMAGE_PATH}"])
        }
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'AMBASSADOR_IMAGE_PATH',
            value: "${env.AMBASSADOR_IMAGE_PATH}"])
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'HAPROXY_IMAGE_PATH',
            value: "${env.HAPROXY_IMAGE_PATH}"])
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'REDIS_IMAGE_PATH',
            value: "${env.REDIS_IMAGE_PATH}"])
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'LDAP_IMAGE_PATH',
            value: "${env.LDAP_IMAGE_PATH}"])
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'SIDECAR_IMAGE_PATH',
            value: "${env.SIDECAR_IMAGE_PATH}"])
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'LOGGING_SIDECAR_IMAGE_PATH',
            value: "${env.LOGGING_SIDECAR_IMAGE_PATH}"])
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'PREREQS_CHECKER_IMAGE_PATH',
            value: "${env.PREREQS_CHECKER_IMAGE_PATH}"])

        if(!IS_HYBRID) {
         
            if(KUBE_FLAVOUR == "openshift") {
                buildParams.add(
                    [$class: 'BooleanParameterValue',
                    name: 'IS_SCHEDULED',
                    value: "false"])
            } else if (KUBE_FLAVOUR != "native") {
                buildParams.add(
                    [$class: 'BooleanParameterValue',
                    name: 'IS_SCHEDULED',
                    value: "${commonConfig.COMMON_IS_SCHEDULED}"])
            }

            buildParams.add(
                [$class: 'BooleanParameterValue',
                name: 'ENABLE_LDAP_CONFIG',
                value: "${env.ENABLE_LDAP_CONFIG}"])
            buildParams.add(
                [$class: 'BooleanParameterValue',
                name: 'ENABLE_DB_CONFIG',
                value: "${env.ENABLE_DB_CONFIG}"])
        }
        return buildParams;
}

def createHybridFreshDeployParams(NAMESPACE, KUBE_FLAVOUR, DEPLOYMENT_LEVEL, DOMAIN_SUFFIX, UPDATE_RELEASE_LEVEL, CLUSTERED_ENV, ADD_ADDITIONAL_NODE, UPDATE_CORE_RELEASE_LEVEL, CLUSTER_NAME, CLUSTER_REGION, RESOURCE_GROP, CONTEXT_ROOT_PATH, DX_CORE_HOME_PATH, PERSONALIZED_DX_CORE_PATH) {

    CORE_VERSIONS = sh (script: "${workspace}/autonomous-deployments/scripts/get_latest_cf.sh true", returnStdout: true)
    CORE_VERSIONS = CORE_VERSIONS.split('CF')
    CURRENT_CORE_CF_NUMBER = CORE_VERSIONS[CORE_VERSIONS.length - 1]
    CURRENT_CORE_CF_NUMBER = CURRENT_CORE_CF_NUMBER.toInteger()
    CURRENT_CORE_CF_VERSION = "CF${CURRENT_CORE_CF_NUMBER}"
    echo "CURRENT_CORE_CF_VERSION = ${CURRENT_CORE_CF_VERSION}"

    ENABLE_REMOTE_SEARCH = true

    buildParams = prepareK8SpecificParams(true, DEPLOYMENT_LEVEL, KUBE_FLAVOUR)

    buildParams.add(
        [$class: 'StringParameterValue',
        name: 'DX_CORE_BUILD_VERSION',
        value: "${DEPLOYMENT_LEVEL}"])
    if(DEPLOYMENT_LEVEL != "develop") {
        DEPLOYMENT_LEVEL = "rivendell_${DEPLOYMENT_LEVEL}"
        if(CURRENT_CORE_CF_NUMBER <= 19){
            ENABLE_REMOTE_SEARCH = false
        }
    }

    buildParams.add(
        [$class: 'StringParameterValue',
        name: 'NAMESPACE',
        value: "${NAMESPACE}"])

    if (KUBE_FLAVOUR == "native") {
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'ON_PREM_INSTANCE_NAME',
            value: "${NAMESPACE}-onprem"])
        if (DEPLOYMENT_LEVEL != "develop") {
            buildParams.add(
                [$class: 'StringParameterValue',
                name: 'IMAGE_REPOSITORY',
                value: "quintana-docker-prod"])
        }
    } else {
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'ON_PREM_INSTANCE_NAME',
            value: "${NAMESPACE}"])
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'IMAGE_REPOSITORY',
            value: "${KUBE_FLAVOUR}"])
        buildParams.add(
            [$class: 'BooleanParameterValue',
            name: 'PUSH_IMAGE_TO_REGISTRY',
            value: "${commonConfig.COMMON_PUSH_IMAGES}"])
        buildParams.add(
            [$class: 'StringParameterValue',
                name: 'CLUSTER_NAME',
                value: "${CLUSTER_NAME}"])
        buildParams.add(
            [$class: 'StringParameterValue',
                name: 'CLUSTER_REGION',
                value: "${CLUSTER_REGION}"])
        buildParams.add(
            [$class: 'StringParameterValue',
                name: 'RESOURCE_GROP',
                value: "${RESOURCE_GROP}"])
        buildParams.add(
            [$class: 'StringParameterValue',
                name: 'AWS_SUBNET',
                value: "${AWS_SUBNET}"])
        buildParams.add(
            [$class: 'StringParameterValue',
                name: 'VPC_SECURITY_GROUPS',
                value: "${VPC_SECURITY_GROUPS}"])
        buildParams.add(
            [$class: 'StringParameterValue',
                name: 'AWS_HOSTED_ZONE',
                value: "${AWS_ZONE_ID}"])
        buildParams.add(
            [$class: 'StringParameterValue',
                name: 'AWS_REGION',
                value: "${AWS_REGION}"])
    }
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'KUBE_FLAVOUR',
         value: "${KUBE_FLAVOUR}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'CORE_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'RS_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'DAM_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'CC_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'DAM_PLUGIN_GOOGLE_VISION_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'RINGAPI_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'IMGPROC_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
     buildParams.add(
        [$class: 'StringParameterValue',
         name: 'DAM_KALTURA_PLUIGN_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'PERSISTENCE_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'PERSISTENCE_NODE_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'PERSISTENCE_METRICS_EXPORTER_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'PERSISTENCE_CONNECTION_POOL_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'CORE_OPERATOR_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'HAPROXY_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'LOGGING_SIDECAR_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'PREREQS_CHECKER_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'DAM_OPERATOR_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'LDAP_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'DESIGN_STUDIO_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'DBTYPE',
         value: "${commonConfig.COMMON_DB2_TYPE}"])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'ENABLE_LDAP_CONFIG',
         value: "${commonConfig.COMMON_ENABLE_LDAP}"])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'ENABLE_DB_CONFIG',
         value: "${commonConfig.COMMON_ENABLE_DB_CONFIG}"])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'REMOTE_SEARCH_ENV',
         value: "${ENABLE_REMOTE_SEARCH}"])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'CLUSTERED_ENV',
         value: "${CLUSTERED_ENV}"])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'ADD_ADDITIONAL_NODE',
         value: "${ADD_ADDITIONAL_NODE}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'CF_VERSION',
         value: "${CURRENT_CORE_CF_VERSION}"])
    buildParams.add(
            [$class: 'StringParameterValue',
             name: 'DOMAIN_SUFFIX',
             value: "${DOMAIN_SUFFIX}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'KUBE_DOMAIN_SUFFIX',
         value: "${KUBE_DOMAIN_SUFFIX}"])
    buildParams.add(
        [$class: 'BooleanParameterValue',
            name: 'SKIP_ACCEPTANCE_TESTS',
            value: "${commonConfig.COMMON_SKIP_ACCEPTANCE_TESTS}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'RUNTIME_CONTROLLER_IMAGE_FILTER',
            value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'HELM_CHARTS_FILTER',
            value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'CONTEXT_ROOT_PATH',
         value: "${CONTEXT_ROOT_PATH}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'DX_CORE_HOME_PATH',
         value: "${DX_CORE_HOME_PATH}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'PERSONALIZED_DX_CORE_PATH',
         value: "${PERSONALIZED_DX_CORE_PATH}"])

    return buildParams;
}

def createHybridKubeUpdateParams(NAMESPACE, KUBE_FLAVOUR, DEPLOYMENT_LEVEL, DOMAIN_SUFFIX, UPDATE_RELEASE_LEVEL, CLUSTERED_ENV, ADD_ADDITIONAL_NODE, UPDATE_CORE_RELEASE_LEVEL) {

    CORE_VERSIONS = sh (script: "${workspace}/autonomous-deployments/scripts/get_latest_cf.sh", returnStdout: true)
    echo "${CORE_VERSIONS}"
    CORE_VERSIONS = CORE_VERSIONS.split('CF')
    CURRENT_CORE_CF_NUMBER = CORE_VERSIONS[CORE_VERSIONS.length - 1]
    CURRENT_CORE_CF_VERSION  = "CF${CURRENT_CORE_CF_NUMBER}"
    echo "CURRENT_CORE_CF_VERSION = ${CURRENT_CORE_CF_VERSION}"
    echo "CURRENT_CORE_CF_NUMBER = ${CURRENT_CORE_CF_NUMBER}"
    UPDATE_CORE_RELEASE_LEVEL = UPDATE_CORE_RELEASE_LEVEL.toInteger()
    VERSION_INDEX = CORE_VERSIONS.length - UPDATE_CORE_RELEASE_LEVEL - 1
    PREVIOUS_CORE_CF_NUMBER = CORE_VERSIONS[VERSION_INDEX]
    PREVIOUS_CORE_CF_NUMBER = PREVIOUS_CORE_CF_NUMBER.toInteger()
    PREVIOUS_CORE_CF_VERSION = "CF${PREVIOUS_CORE_CF_NUMBER}"
    echo "PREVIOUS_CORE_CF_VERSION = ${PREVIOUS_CORE_CF_VERSION}"
    ENABLE_REMOTE_SEARCH = true
    if(PREVIOUS_CORE_CF_NUMBER <= 19){
        ENABLE_REMOTE_SEARCH = false
    }
    CORE_UPDATE = true
    if(UPDATE_CORE_RELEASE_LEVEL == 0 && DEPLOYMENT_LEVEL == 'master'){
        CORE_UPDATE = false
    }
    buildParams = prepareK8SpecificParams(true, DEPLOYMENT_LEVEL, KUBE_FLAVOUR)
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'DX_CORE_BUILD_VERSION',
         value: "${DEPLOYMENT_LEVEL}"])
    if(DEPLOYMENT_LEVEL != "develop") {
        DEPLOYMENT_LEVEL = "rivendell_${DEPLOYMENT_LEVEL}"
    }
    /* Kube update parameters */
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'NAMESPACE',
         value: "${NAMESPACE}"])

    if (KUBE_FLAVOUR == "native") {
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'INSTANCE_NAME',
            value: "${NAMESPACE}-onprem"])
        if (DEPLOYMENT_LEVEL != "develop") {
            buildParams.add(
                [$class: 'StringParameterValue',
                name: 'IMAGE_REPOSITORY',
                value: "quintana-docker-prod"])
        }
    } else {
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'INSTANCE_NAME',
            value: "${NAMESPACE}"])
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'IMAGE_REPOSITORY',
            value: "${KUBE_FLAVOUR}"])
        buildParams.add(
            [$class: 'StringParameterValue',
                name: 'CLUSTER_NAME',
                value: "${CLUSTER_NAME}"])
        buildParams.add(
            [$class: 'StringParameterValue',
                name: 'CLUSTER_REGION',
                value: "${CLUSTER_REGION}"])
        buildParams.add(
            [$class: 'StringParameterValue',
                name: 'RESOURCE_GROP',
                value: "${RESOURCE_GROP}"])
    }

    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'KUBE_FLAVOUR',
         value: "${KUBE_FLAVOUR}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'CORE_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'RS_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'DAM_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'CC_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'DAM_PLUGIN_GOOGLE_VISION_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'RINGAPI_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'IMGPROC_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
       buildParams.add(
        [$class: 'StringParameterValue',
         name: 'DAM_KALTURA_PLUIGN_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'HAPROXY_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'PERSISTENCE_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'PERSISTENCE_NODE_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'PERSISTENCE_METRICS_EXPORTER_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'PERSISTENCE_CONNECTION_POOL_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'CORE_OPERATOR_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'DAM_OPERATOR_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'LDAP_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'DESIGN_STUDIO_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'LOGGING_SIDECAR_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'PREREQS_CHECKER_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'DBTYPE',
         value: "${commonConfig.COMMON_DB2_TYPE}"])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'ENABLE_LDAP_CONFIG',
         value: "${commonConfig.COMMON_ENABLE_LDAP}"])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'ENABLE_DB_CONFIG',
         value: "${commonConfig.COMMON_ENABLE_DB_CONFIG}"])

    /* Core update parameters */
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'UPDATE_ONPREMISE',
         value: "${CORE_UPDATE}"])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'REMOTE_SEARCH_ENV',
         value: "${ENABLE_REMOTE_SEARCH}"])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'CLUSTERED_ENV',
         value: "${CLUSTERED_ENV}"])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'ADD_ADDITIONAL_NODE',
         value: "${ADD_ADDITIONAL_NODE}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'CF_VERSION',
         value: "${CURRENT_CORE_CF_VERSION}"])

    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'AWS_SUBNET',
         value: "${AWS_SUBNET}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'VPC_SECURITY_GROUPS',
            value: "${VPC_SECURITY_GROUPS}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'AWS_HOSTED_ZONE',
            value: "${AWS_ZONE_ID}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'AWS_REGION',
            value: "${AWS_REGION}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'DOMAIN_SUFFIX',
            value: "${DOMAIN_SUFFIX}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'KUBE_DOMAIN_SUFFIX',
         value: "${KUBE_DOMAIN_SUFFIX}"])
    buildParams.add(
        [$class: 'BooleanParameterValue',
            name: 'SKIP_ACCEPTANCE_TESTS',
            value: "${commonConfig.COMMON_SKIP_ACCEPTANCE_TESTS}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'RUNTIME_CONTROLLER_IMAGE_FILTER',
            value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'HELM_CHARTS_FILTER',
            value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'CONTEXT_ROOT_PATH',
         value: "${CONTEXT_ROOT_PATH}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'DX_CORE_HOME_PATH',
         value: "${DX_CORE_HOME_PATH}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'PERSONALIZED_DX_CORE_PATH',
         value: "${PERSONALIZED_DX_CORE_PATH}"])

    return buildParams;
}

def createHybridKubeDeployParams(NAMESPACE, KUBE_FLAVOUR, DEPLOYMENT_LEVEL, DOMAIN_SUFFIX, UPDATE_RELEASE_LEVEL, CLUSTERED_ENV, ADD_ADDITIONAL_NODE, UPDATE_CORE_RELEASE_LEVEL) {

    CORE_VERSIONS = sh (script: "${workspace}/autonomous-deployments/scripts/get_latest_cf.sh true", returnStdout: true)
    CORE_VERSIONS = CORE_VERSIONS.split('CF')
    CURRENT_CORE_CF_NUMBER = CORE_VERSIONS[CORE_VERSIONS.length - 1]
    CURRENT_CORE_CF_VERSION  = "CF${CURRENT_CORE_CF_NUMBER}"
    echo "CURRENT_CORE_CF_VERSION = ${CURRENT_CORE_CF_VERSION}"
    echo "CURRENT_CORE_CF_NUMBER = ${CURRENT_CORE_CF_NUMBER}"
    UPDATE_CORE_RELEASE_LEVEL = UPDATE_CORE_RELEASE_LEVEL.toInteger()
    VERSION_INDEX = CORE_VERSIONS.length - UPDATE_CORE_RELEASE_LEVEL - 1
    echo "VERSION_INDEX = ${VERSION_INDEX}"
    PREVIOUS_CORE_CF_NUMBER = CORE_VERSIONS[VERSION_INDEX]
    PREVIOUS_CORE_CF_NUMBER = PREVIOUS_CORE_CF_NUMBER.toInteger()
    PREVIOUS_CORE_CF_VERSION = "CF${PREVIOUS_CORE_CF_NUMBER}"
    echo "PREVIOUS_CORE_CF_VERSION = ${PREVIOUS_CORE_CF_VERSION}"
    ENABLE_REMOTE_SEARCH = true
    if(PREVIOUS_CORE_CF_NUMBER <= 19){
        ENABLE_REMOTE_SEARCH = false
    }

    buildParams = prepareK8SpecificParams(true, DEPLOYMENT_LEVEL, KUBE_FLAVOUR)
    CURRENT_BUILD_IMAGE_TAG_CORE  = retrieveImageTag(commonConfig.COMMON_CORE_IMAGE_PATH, 'rivendell_master');
    CURRENT_BUILD_IMAGE_TAG_RS  = retrieveImageTag(commonConfig.COMMON_RS_IMAGE_PATH, 'rivendell_master');

    RELEASE_NUMBER = retrieveCurrentCFVersion(CURRENT_BUILD_IMAGE_TAG_CORE.split("_")[1]).toInteger()

    echo "Release Number:- ${RELEASE_NUMBER} "
    if(RELEASE_NUMBER <= 199) {
        echo "Pulling CORE & DAM Operator Tags."
        CURRENT_BUILD_IMAGE_TAG_CORE_OPERATOR  = retrieveImageTag(commonConfig.COMMON_CORE_OPERATOR_IMAGE_PATH, 'rivendell_master');
        CURRENT_BUILD_IMAGE_TAG_DAM_OPERATOR  = retrieveImageTag(commonConfig.COMMON_DAM_OPERATOR_IMAGE_PATH, 'rivendell_master');
        CURRENT_CORE_OPERATOR_IMAGE_PATH = retrievePrevCFMasterImageTag(CURRENT_BUILD_IMAGE_TAG_CORE_OPERATOR, commonConfig.COMMON_CORE_OPERATOR_IMAGE_PATH, env.UPDATE_RELEASE_LEVEL)
        CURRENT_DAM_OPERATOR_IMAGE_PATH = retrievePrevCFMasterImageTag(CURRENT_BUILD_IMAGE_TAG_DAM_OPERATOR, commonConfig.COMMON_DAM_OPERATOR_IMAGE_PATH, env.UPDATE_RELEASE_LEVEL)
    }
    CURRENT_CORE_IMAGE_TAG = retrievePrevCFMasterImageTag(CURRENT_BUILD_IMAGE_TAG_CORE, commonConfig.COMMON_CORE_IMAGE_PATH, env.UPDATE_RELEASE_LEVEL)
    CURRENT_RS_IMAGE_PATH = retrievePrevCFMasterImageTag(CURRENT_BUILD_IMAGE_TAG_RS, commonConfig.COMMON_RS_IMAGE_PATH, env.UPDATE_RELEASE_LEVEL)
    
    echo "Getting PrevAddOnMasterImagesDate"
    
    FILTER_TEXT_ADDONS = retrievePrevAddOnMasterImagesDate(CURRENT_BUILD_IMAGE_TAG_CORE, env.UPDATE_RELEASE_LEVEL)
    CURRENT_IMAGE_FILTER = "${FILTER_TEXT_ADDONS}.*.rivendell_master"
    echo "Filter for deploying previous addon masterimages: ${CURRENT_IMAGE_FILTER}"

    CURRENT_DAM_IMAGE_PATH = retrieveImageTag(commonConfig.COMMON_DAM_IMAGE_PATH, CURRENT_IMAGE_FILTER);
    CURRENT_CC_IMAGE_PATH = retrieveImageTag(commonConfig.COMMON_CC_IMAGE_PATH, CURRENT_IMAGE_FILTER);
    CURRENT_DAM_PLUGIN_GOOGLE_VISION_IMAGE_PATH = retrieveImageTag(commonConfig.COMMON_DAM_PLUGIN_GOOGLE_VISION_IMAGE_PATH, CURRENT_IMAGE_FILTER);
    CURRENT_RINGAPI_IMAGE_PATH = retrieveImageTag(commonConfig.COMMON_RINGAPI_IMAGE_PATH, CURRENT_IMAGE_FILTER);
    CURRENT_IMGPROC_IMAGE_PATH = retrieveImageTag(commonConfig.COMMON_IMGPROC_IMAGE_PATH, CURRENT_IMAGE_FILTER);
    CURRENT_DAM_KALTURA_PLUGIN_IMAGE_PATH = retrieveImageTag(commonConfig.COMMON_DAM_KALTURA_PLUGIN_IMAGE_PATH, CURRENT_IMAGE_FILTER);
    CURRENT_PERSISTENCE_IMAGE_PATH = retrieveImageTag(commonConfig.COMMON_PERSISTENCE_IMAGE_PATH, CURRENT_IMAGE_FILTER);

    echo "BUILDING RELEASE NUMBER:- ${RELEASE_NUMBER-UPDATE_RELEASE_LEVEL.toInteger()}"

    echo "UPDATE_RELEASE_LEVEL: ${UPDATE_RELEASE_LEVEL}"
    echo "CF_VERSION: ${PREVIOUS_CORE_CF_VERSION}"
    echo "CURRENT_CORE_IMAGE_TAG:- ${CURRENT_CORE_IMAGE_TAG}"
    echo "CURRENT_RS_IMAGE_PATH:- ${CURRENT_RS_IMAGE_PATH}"
    echo "CURRENT_DAM_IMAGE_PATH:- ${CURRENT_DAM_IMAGE_PATH}"
    echo "CURRENT_DAM_KALTURA_PLUGIN_IMAGE_PATH:- ${CURRENT_DAM_KALTURA_PLUGIN_IMAGE_PATH}"
    echo "CURRENT_CC_IMAGE_PATH:- ${CURRENT_CC_IMAGE_PATH}"
    echo "CURRENT_DAM_PLUGIN_GOOGLE_VISION_IMAGE_PATH:- ${CURRENT_DAM_PLUGIN_GOOGLE_VISION_IMAGE_PATH}"
    echo "CURRENT_RINGAPI_IMAGE_PATH:- ${CURRENT_RINGAPI_IMAGE_PATH}"
    echo "CURRENT_IMGPROC_IMAGE_PATH:- ${CURRENT_IMGPROC_IMAGE_PATH}"
    echo "CURRENT_PERSISTENCE_IMAGE_PATH:- ${CURRENT_PERSISTENCE_IMAGE_PATH}"
    
    if (RELEASE_NUMBER <= 199) {
        echo "CURRENT_CORE_OPERATOR_IMAGE_PATH:- ${CURRENT_CORE_OPERATOR_IMAGE_PATH}"
        echo "CURRENT_DAM_OPERATOR_IMAGE_PATH:- ${CURRENT_DAM_OPERATOR_IMAGE_PATH}"
     }    
    /* Site Manager available from 196 */
    echo "RELEASE_NUMBER:- ${RELEASE_NUMBER}"
    if(RELEASE_NUMBER-UPDATE_RELEASE_LEVEL.toInteger() >= 196) {
        CURRENT_DESIGN_STUDIO_IMAGE_TAG = retrieveImageTag(commonConfig.COMMON_DESIGN_STUDIO_IMAGE_PATH, CURRENT_IMAGE_FILTER)
        CURRENT_LDAP_IMAGE_PATH = retrieveImageTag(commonConfig.COMMON_LDAP_IMAGE_PATH, CURRENT_IMAGE_FILTER)
        echo "CURRENT_DESIGN_STUDIO_IMAGE_TAG:- ${CURRENT_DESIGN_STUDIO_IMAGE_TAG}"
        buildParams.add(
        [$class: 'StringParameterValue',
         name: 'DESIGN_STUDIO_IMAGE_FILTER',
         value: "${CURRENT_DESIGN_STUDIO_IMAGE_TAG}"])
    } else {
        CURRENT_LDAP_IMAGE_PATH = retrieveOpenLDAPMasterImageTag(CURRENT_BUILD_IMAGE_TAG_CORE, commonConfig.COMMON_CORE_IMAGE_PATH, env.UPDATE_RELEASE_LEVEL)
        buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'DISABLE_DESIGN_STUDIO',
         value: true])
    }
    echo "CURRENT_LDAP_IMAGE_PATH:- ${CURRENT_LDAP_IMAGE_PATH}"

    if((RELEASE_NUMBER-UPDATE_RELEASE_LEVEL.toInteger() >= 197) && (DEPLOYMENT_METHOD == "helm")){
        RUNTIME_CONTROLLER_IMAGE_FILTER = retrieveImageTag(commonConfig.COMMON_RUNTIME_CONTROLLER_IMAGE_PATH, CURRENT_IMAGE_FILTER)
        echo "RUNTIME_CONTROLLER_IMAGE_FILTER:- ${RUNTIME_CONTROLLER_IMAGE_FILTER}"
        echo "HELM_CHARTS_FILTER:- ${CURRENT_IMAGE_FILTER}"
        buildParams.add(
        [$class: 'StringParameterValue',
            name: 'RUNTIME_CONTROLLER_IMAGE_FILTER',
            value: "${RUNTIME_CONTROLLER_IMAGE_FILTER}"])
        buildParams.add(
        [$class: 'StringParameterValue',
            name: 'HELM_CHARTS_FILTER',
            value: "${CURRENT_IMAGE_FILTER}"])
        
    }

    if((RELEASE_NUMBER-UPDATE_RELEASE_LEVEL.toInteger() >= 198) && (DEPLOYMENT_METHOD == "helm")){
        echo "Deploying with DAM Persistence HA."
        CURRENT_PERSISTENCE_CONNECTION_POOL_IMAGE_FILTER = retrieveImageTag(commonConfig.COMMON_PERSISTENCE_CONNECTION_POOL_IMAGE_PATH, CURRENT_IMAGE_FILTER);
        CURRENT_PERSISTENCE_NODE_IMAGE_FILTER = retrieveImageTag(commonConfig.COMMON_PERSISTENCE_NODE_IMAGE_PATH, CURRENT_IMAGE_FILTER);

        buildParams.add(
        [$class: 'StringParameterValue',
            name: 'PERSISTENCE_CONNECTION_POOL_IMAGE_FILTER',
            value: "${CURRENT_PERSISTENCE_CONNECTION_POOL_IMAGE_FILTER}"])
        buildParams.add(
        [$class: 'StringParameterValue',
            name: 'PERSISTENCE_NODE_IMAGE_FILTER',
            value: "${CURRENT_PERSISTENCE_NODE_IMAGE_FILTER}"])
    }

    buildParams.add(
    [$class: 'StringParameterValue',
        name: 'PERSISTENCE_METRICS_EXPORTER_IMAGE_FILTER',
        value: "${CURRENT_IMAGE_FILTER}"])

    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'NAMESPACE',
            value: "${NAMESPACE}"])

    if (KUBE_FLAVOUR == "native") {
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'ON_PREM_INSTANCE_NAME',
            value: "${NAMESPACE}-onprem"])
        if (DEPLOYMENT_LEVEL != "develop") {
            buildParams.add(
                [$class: 'StringParameterValue',
                name: 'IMAGE_REPOSITORY',
                value: "quintana-docker-prod"])
        }
    } else {
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'ON_PREM_INSTANCE_NAME',
            value: "${NAMESPACE}"])
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'IMAGE_REPOSITORY',
            value: "${params.KUBE_FLAVOUR}"])
        buildParams.add(
            [$class: 'BooleanParameterValue',
            name: 'PUSH_IMAGE_TO_REGISTRY',
            value: "${commonConfig.COMMON_PUSH_IMAGES}"])
        buildParams.add(
            [$class: 'StringParameterValue',
                name: 'CLUSTER_NAME',
                value: "${CLUSTER_NAME}"])
        buildParams.add(
            [$class: 'StringParameterValue',
                name: 'CLUSTER_REGION',
                value: "${CLUSTER_REGION}"])
        buildParams.add(
            [$class: 'StringParameterValue',
                name: 'RESOURCE_GROP',
                value: "${RESOURCE_GROP}"])
    }

    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'KUBE_FLAVOUR',
            value: "${KUBE_FLAVOUR}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'CF_VERSION',
            value: "${PREVIOUS_CORE_CF_VERSION}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'CORE_IMAGE_FILTER',
            value: "${CURRENT_CORE_IMAGE_TAG}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'RS_IMAGE_FILTER',
            value: "${CURRENT_RS_IMAGE_PATH}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'DAM_IMAGE_FILTER',
            value: "${CURRENT_DAM_IMAGE_PATH}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'CC_IMAGE_FILTER',
            value: "${CURRENT_CC_IMAGE_PATH}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'DAM_PLUGIN_GOOGLE_VISION_IMAGE_FILTER',
            value: "${CURRENT_DAM_PLUGIN_GOOGLE_VISION_IMAGE_PATH}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'RINGAPI_IMAGE_FILTER',
            value: "${CURRENT_RINGAPI_IMAGE_PATH}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'IMGPROC_IMAGE_FILTER',
            value: "${CURRENT_IMGPROC_IMAGE_PATH}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'DAM_KALTURA_PLUGIN_IMAGE_FILTER',
            value: "${CURRENT_DAM_KALTURA_PLUGIN_IMAGE_PATH}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'HAPROXY_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'LOGGING_SIDECAR_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'PREREQS_CHECKER_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'PERSISTENCE_IMAGE_FILTER',
            value: "${CURRENT_PERSISTENCE_IMAGE_PATH}"])
    if (RELEASE_NUMBER <= 199) {
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'CORE_OPERATOR_IMAGE_FILTER',
            value: "${CURRENT_CORE_OPERATOR_IMAGE_PATH}"])
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'DAM_OPERATOR_IMAGE_FILTER',
            value: "${CURRENT_DAM_OPERATOR_IMAGE_PATH}"])
     }
  
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'LDAP_IMAGE_FILTER',
            value: "${CURRENT_LDAP_IMAGE_PATH}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'DESIGN_STUDIO_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'DX_CORE_BUILD_VERSION',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'REMOTE_SEARCH_ENV',
         value: "${ENABLE_REMOTE_SEARCH}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'DBTYPE',
         value: "${commonConfig.COMMON_DB2_TYPE}"])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'ENABLE_LDAP_CONFIG',
         value: "${commonConfig.COMMON_ENABLE_LDAP}"])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'ENABLE_DB_CONFIG',
         value: "${commonConfig.COMMON_ENABLE_DB_CONFIG}"])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'CLUSTERED_ENV',
         value: "${CLUSTERED_ENV}"])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'ADD_ADDITIONAL_NODE',
         value: "${ADD_ADDITIONAL_NODE}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'AWS_SUBNET',
         value: "${AWS_SUBNET}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'VPC_SECURITY_GROUPS',
            value: "${VPC_SECURITY_GROUPS}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'AWS_HOSTED_ZONE',
            value: "${AWS_ZONE_ID}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'AWS_REGION',
            value: "${AWS_REGION}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'DOMAIN_SUFFIX',
            value: "${DOMAIN_SUFFIX}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'KUBE_DOMAIN_SUFFIX',
         value: "${KUBE_DOMAIN_SUFFIX}"])
    buildParams.add(
        [$class: 'BooleanParameterValue',
            name: 'SKIP_ACCEPTANCE_TESTS',
            value: "${commonConfig.COMMON_SKIP_ACCEPTANCE_TESTS}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'CONTEXT_ROOT_PATH',
         value: "${CONTEXT_ROOT_PATH}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'DX_CORE_HOME_PATH',
         value: "${DX_CORE_HOME_PATH}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'PERSONALIZED_DX_CORE_PATH',
         value: "${PERSONALIZED_DX_CORE_PATH}"])
    return buildParams;
}

def createHybridKubeUnDeployParams(NAMESPACE, KUBE_FLAVOUR, CLUSTERED_ENV, CLUSTER_NAME, CLUSTER_REGION, RESOURCE_GROP) {
    buildParams = []
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'NAMESPACE',
            value: "${NAMESPACE}"])
    if (KUBE_FLAVOUR == "native") {
        buildParams.add(
            [$class: 'StringParameterValue',
                name: 'ON_PREM_INSTANCE_NAME',
                value: "${NAMESPACE}-onprem"])
    } else {
        buildParams.add(
            [$class: 'StringParameterValue',
                name: 'ON_PREM_INSTANCE_NAME',
                value: "${NAMESPACE}"])
        buildParams.add(
            [$class: 'StringParameterValue',
                name: 'CLUSTER_NAME',
                value: "${CLUSTER_NAME}"])
        buildParams.add(
            [$class: 'StringParameterValue',
                name: 'CLUSTER_REGION',
                value: "${CLUSTER_REGION}"])
        buildParams.add(
            [$class: 'StringParameterValue',
                name: 'RESOURCE_GROP',
                value: "${RESOURCE_GROP}"])
    }
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'KUBE_FLAVOUR',
            value: "${KUBE_FLAVOUR}"])
    buildParams.add(
        [$class: 'BooleanParameterValue',
            name: 'CLUSTERED_ENV',
            value: "${CLUSTERED_ENV}"])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'REMOTE_SEARCH_ENV',
         value: "true"])
    return buildParams;
}

def createKubeUnDeployParams(NAMESPACE, KUBE_FLAVOUR, CLUSTER_NAME, CLUSTER_REGION, RESOURCE_GROP) {
    buildParams = []
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'NAMESPACE',
            value: "${NAMESPACE}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'KUBE_FLAVOUR',
            value: "${KUBE_FLAVOUR}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'CLUSTER_NAME',
            value: "${CLUSTER_NAME}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'CLUSTER_REGION',
            value: "${CLUSTER_REGION}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'RESOURCE_GROP',
            value: "${RESOURCE_GROP}"])
    return buildParams;
}

def createKubeDeployParamsForMaster(NAMESPACE, KUBE_FLAVOUR, CLUSTER_NAME, CLUSTER_REGION, RESOURCE_GROP, CONTEXT_ROOT_PATH, DX_CORE_HOME_PATH, PERSONALIZED_DX_CORE_PATH, UPDATE_RELEASE_LEVEL, DEPLOYMENT_LEVEL, DEPLOYMENT_METHOD, DOMAIN_SUFFIX) {
    // take CORE tag instead of CORE_OPERATOR to get actual release
    CURRENT_RELEASE_IMAGE_TAG_CORE  = retrieveImageTag(commonConfig.COMMON_CORE_IMAGE_PATH, 'rivendell_release');
    echo "CURRENT_RELEASE_IMAGE_TAG_CORE: ${CURRENT_RELEASE_IMAGE_TAG_CORE}"
    CURRENT_RELEASE_NUMBER = CURRENT_RELEASE_IMAGE_TAG_CORE.split("-")[0].split("_")[1].split("CF")[1].toInteger()
    BASE_RELEASE_NUMBER = CURRENT_RELEASE_NUMBER.toInteger() - UPDATE_RELEASE_LEVEL.toInteger()
    println "CURRENT_RELEASE_NUMBER: ${CURRENT_RELEASE_NUMBER}\nBASE_RELEASE_NUMBER: ${BASE_RELEASE_NUMBER}"
    
    CURRENT_BUILD_IMAGE_TAG_CORE  = retrieveImageTag(commonConfig.COMMON_CORE_IMAGE_PATH, 'rivendell_master');
    CURRENT_BUILD_IMAGE_TAG_CORE_OPERATOR  = retrieveImageTag(commonConfig.COMMON_CORE_OPERATOR_IMAGE_PATH, 'rivendell_master');
    CURRENT_BUILD_IMAGE_TAG_DAM_OPERATOR  = retrieveImageTag(commonConfig.COMMON_DAM_OPERATOR_IMAGE_PATH, 'rivendell_master');
    CURRENT_BUILD_IMAGE_TAG_RS  = retrieveImageTag(commonConfig.COMMON_RS_IMAGE_PATH, 'rivendell_master');

    CURRENT_CORE_IMAGE_TAG = retrievePrevCFMasterImageTagNew(CURRENT_RELEASE_NUMBER, commonConfig.COMMON_CORE_IMAGE_PATH, env.UPDATE_RELEASE_LEVEL)
    CURRENT_CORE_OPERATOR_IMAGE_PATH = retrievePrevCFMasterImageTagNew(CURRENT_RELEASE_NUMBER, commonConfig.COMMON_CORE_OPERATOR_IMAGE_PATH, env.UPDATE_RELEASE_LEVEL)
    CURRENT_DAM_OPERATOR_IMAGE_PATH = retrievePrevCFMasterImageTagNew(CURRENT_RELEASE_NUMBER, commonConfig.COMMON_DAM_OPERATOR_IMAGE_PATH, env.UPDATE_RELEASE_LEVEL)
    CURRENT_RS_IMAGE_PATH = retrievePrevCFMasterImageTagNew(CURRENT_RELEASE_NUMBER, commonConfig.COMMON_RS_IMAGE_PATH, env.UPDATE_RELEASE_LEVEL)

    FILTER_TEXT_ADDONS = retrieveAddOnMasterImagesDate(CURRENT_CORE_IMAGE_TAG)
    CURRENT_IMAGE_FILTER = "${FILTER_TEXT_ADDONS}.*.rivendell_master"
    echo "Filter for deploying previous addon masterimages: ${CURRENT_IMAGE_FILTER}"

    CURRENT_DAM_IMAGE_PATH = retrieveImageTag(commonConfig.COMMON_DAM_IMAGE_PATH, CURRENT_IMAGE_FILTER);
    CURRENT_CC_IMAGE_PATH = retrieveImageTag(commonConfig.COMMON_CC_IMAGE_PATH, CURRENT_IMAGE_FILTER);
    CURRENT_DAM_PLUGIN_GOOGLE_VISION_IMAGE_PATH = retrieveImageTag(commonConfig.COMMON_DAM_PLUGIN_GOOGLE_VISION_IMAGE_PATH, CURRENT_IMAGE_FILTER);
    CURRENT_RINGAPI_IMAGE_PATH = retrieveImageTag(commonConfig.COMMON_RINGAPI_IMAGE_PATH, CURRENT_IMAGE_FILTER);
    CURRENT_IMGPROC_IMAGE_PATH = retrieveImageTag(commonConfig.COMMON_IMGPROC_IMAGE_PATH, CURRENT_IMAGE_FILTER);
    CURRENT_DAM_KALTURA_PLUGIN_IMAGE_PATH = retrieveImageTag(commonConfig.COMMON_DAM_KALTURA_PLUGIN_IMAGE_PATH, CURRENT_IMAGE_FILTER);
    CURRENT_PERSISTENCE_IMAGE_PATH = retrieveImageTag(commonConfig.COMMON_PERSISTENCE_IMAGE_PATH, CURRENT_IMAGE_FILTER);

    CURRENT_HELM_CHART_FILTER = retrievePrevHelmChartImageTag(DEPLOYMENT_LEVEL, FILTER_TEXT_ADDONS);

    echo "DAM_RTRM_PERFORMANCE: ${env.DAM_RTRM_PERFORMANCE}"
    echo "UPDATE_RELEASE_LEVEL: ${UPDATE_RELEASE_LEVEL}"
    echo "NAMESPACE: ${NAMESPACE}"
    echo "BASE_CORE_IMAGE_TAG:- ${CURRENT_CORE_IMAGE_TAG}"
    echo "BASE_CORE_OPERATOR_IMAGE_PATH:- ${CURRENT_CORE_OPERATOR_IMAGE_PATH}"
    echo "BASE_DAM_OPERATOR_IMAGE_PATH:- ${CURRENT_DAM_OPERATOR_IMAGE_PATH}"
    echo "BASE_RS_IMAGE_PATH:- ${CURRENT_RS_IMAGE_PATH}"
    echo "BASE_DAM_IMAGE_PATH:- ${CURRENT_DAM_IMAGE_PATH}"
    echo "BASE_CC_IMAGE_PATH:- ${CURRENT_CC_IMAGE_PATH}"
    echo "BASE_DAM_PLUGIN_GOOGLE_VISION_IMAGE_PATH:- ${CURRENT_DAM_PLUGIN_GOOGLE_VISION_IMAGE_PATH}"
    echo "BASE_RINGAPI_IMAGE_PATH:- ${CURRENT_RINGAPI_IMAGE_PATH}"
    echo "BASE_IMGPROC_IMAGE_PATH:- ${CURRENT_IMGPROC_IMAGE_PATH}"
    echo "BASE_DAM_KALTURA_PLUGIN_IMAGE_PATH:- ${CURRENT_DAM_KALTURA_PLUGIN_IMAGE_PATH}"
    echo "BASE_PERSISTENCE_IMAGE_PATH:- ${CURRENT_PERSISTENCE_IMAGE_PATH}"
    echo "BASE_HELM_CHART_FILTER: ${CURRENT_HELM_CHART_FILTER}"

    buildParams = prepareK8SpecificParams(false, DEPLOYMENT_LEVEL, KUBE_FLAVOUR);
    /* Site Manager available from 196 */
    echo "BASE_RELEASE_NUMBER:- ${BASE_RELEASE_NUMBER}"
    if(BASE_RELEASE_NUMBER.toInteger() >= 196) {
        CURRENT_DESIGN_STUDIO_IMAGE_TAG = retrieveImageTag(commonConfig.COMMON_DESIGN_STUDIO_IMAGE_PATH, CURRENT_IMAGE_FILTER);
        CURRENT_LDAP_IMAGE_PATH = retrieveImageTag(commonConfig.COMMON_LDAP_IMAGE_PATH, CURRENT_IMAGE_FILTER)
        echo "CURRENT_DESIGN_STUDIO_IMAGE_TAG:- ${CURRENT_DESIGN_STUDIO_IMAGE_TAG}"
        buildParams.add(
        [$class: 'StringParameterValue',
         name: 'DESIGN_STUDIO_IMAGE_FILTER',
         value: "${CURRENT_DESIGN_STUDIO_IMAGE_TAG}"])
    } else {
        CURRENT_LDAP_IMAGE_PATH = retrieveOpenLDAPMasterImageTag(CURRENT_BUILD_IMAGE_TAG_CORE, commonConfig.COMMON_CORE_IMAGE_PATH, env.UPDATE_RELEASE_LEVEL)
        buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'DISABLE_DESIGN_STUDIO',
         value: true])
    }
    echo "BASE_LDAP_IMAGE_PATH:- ${CURRENT_LDAP_IMAGE_PATH}"

    echo "BUILDING RELEASE NUMBER:- ${BASE_RELEASE_NUMBER}"
    if((BASE_RELEASE_NUMBER.toInteger() >= 198) && (DEPLOYMENT_METHOD == "helm")){

        echo "Deploying with DAM Persistence HA."
        CURRENT_PERSISTENCE_CONNECTION_POOL_IMAGE_FILTER = retrieveImageTag(commonConfig.COMMON_PERSISTENCE_CONNECTION_POOL_IMAGE_PATH, CURRENT_IMAGE_FILTER);
        CURRENT_PERSISTENCE_NODE_IMAGE_FILTER = retrieveImageTag(commonConfig.COMMON_PERSISTENCE_NODE_IMAGE_PATH, CURRENT_IMAGE_FILTER);
        buildParams.add(
        [$class: 'StringParameterValue',
            name: 'PERSISTENCE_CONNECTION_POOL_IMAGE_FILTER',
            value: "${CURRENT_PERSISTENCE_CONNECTION_POOL_IMAGE_FILTER}"])
        buildParams.add(
        [$class: 'StringParameterValue',
            name: 'PERSISTENCE_NODE_IMAGE_FILTER',
            value: "${CURRENT_PERSISTENCE_NODE_IMAGE_FILTER}"])
    }

    buildParams.add(
    [$class: 'StringParameterValue',
        name: 'PERSISTENCE_METRICS_EXPORTER_IMAGE_FILTER',
        value: "${CURRENT_IMAGE_FILTER}"])

    if (KUBE_FLAVOUR == "native") {
        buildParams.add(
            [$class: 'StringParameterValue',
             name: 'INSTANCE_NAME',
             value: "${NAMESPACE}"])
        /* The default for NEXT_JOB_DELAY_HOURS is 24 hours. */
        /* This interferes with the test schedule so setting to 15 */
        buildParams.add(
            [$class: 'StringParameterValue',
             name: 'NEXT_JOB_DELAY_HOURS',
             value: "15"])
        if (DEPLOYMENT_LEVEL != "develop") {
            buildParams.add(
                [$class: 'StringParameterValue',
                name: 'IMAGE_REPOSITORY',
                value: "quintana-docker-prod"])
        }
    } else {
        buildParams.add(
            [$class: 'StringParameterValue',
                name: 'NAMESPACE',
                value: "${NAMESPACE}"])
        buildParams.add(
            [$class: 'StringParameterValue',
                name: 'IMAGE_REPOSITORY',
                value: "${params.KUBE_FLAVOUR}"])
        buildParams.add(
            [$class: 'StringParameterValue',
                name: 'CLUSTER_NAME',
                value: "${CLUSTER_NAME}"])
        buildParams.add(
            [$class: 'StringParameterValue',
                name: 'CLUSTER_REGION',
                value: "${CLUSTER_REGION}"])
        buildParams.add(
            [$class: 'StringParameterValue',
                name: 'RESOURCE_GROP',
                value: "${RESOURCE_GROP}"])
        buildParams.add(
            [$class: 'BooleanParameterValue',
            name: 'PUSH_IMAGE_TO_REGISTRY',
            value: "${commonConfig.COMMON_PUSH_IMAGES}"])
        buildParams.add(
            [$class: 'BooleanParameterValue',
            name: 'ENABLE_DB_CONFIG',
            value: "${commonConfig.COMMON_ENABLE_DB_CONFIG}"])
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'DB_TYPE',
            value: "${commonConfig.COMMON_DB2_TYPE}"])
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'NEXT_JOB_DELAY_HOURS',
            value: "${NEXT_JOB_DELAY_HOURS}"])
    }
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'KUBE_FLAVOUR',
            value: "${KUBE_FLAVOUR}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'CONTEXT_ROOT_PATH',
            value: "${CONTEXT_ROOT_PATH}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'DX_CORE_HOME_PATH',
            value: "${DX_CORE_HOME_PATH}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'PERSONALIZED_DX_CORE_PATH',
            value: "${PERSONALIZED_DX_CORE_PATH}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'CORE_IMAGE_FILTER',
            value: "${CURRENT_CORE_IMAGE_TAG}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'RS_IMAGE_FILTER',
            value: "${CURRENT_RS_IMAGE_PATH}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'DAM_IMAGE_FILTER',
            value: "${CURRENT_DAM_IMAGE_PATH}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'CC_IMAGE_FILTER',
            value: "${CURRENT_CC_IMAGE_PATH}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'DAM_PLUGIN_GOOGLE_VISION_IMAGE_FILTER',
            value: "${CURRENT_DAM_PLUGIN_GOOGLE_VISION_IMAGE_PATH}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'RINGAPI_IMAGE_FILTER',
            value: "${CURRENT_RINGAPI_IMAGE_PATH}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'IMGPROC_IMAGE_FILTER',
            value: "${CURRENT_IMGPROC_IMAGE_PATH}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'DAM_KALTURA_PLUGIN_IMAGE_FILTER',
            value: "${CURRENT_DAM_KALTURA_PLUGIN_IMAGE_PATH}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'HAPROXY_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'LOGGING_SIDECAR_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'PREREQS_CHECKER_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'PERSISTENCE_IMAGE_FILTER',
            value: "${CURRENT_PERSISTENCE_IMAGE_PATH}"])
    if ((KUBE_FLAVOUR != "native") || (DEPLOYMENT_METHOD == "helm")) {
        buildParams.add(
            [$class: 'StringParameterValue',
                name: 'RUNTIME_CONTROLLER_IMAGE_FILTER',
                value: "${DEPLOYMENT_LEVEL}"])
        buildParams.add(
            [$class: 'StringParameterValue',
                name: 'HELM_CHARTS_FILTER',
                value: "${CURRENT_HELM_CHART_FILTER}"])
    }
    if ((KUBE_FLAVOUR != "native") || (DEPLOYMENT_METHOD != "helm")) {
        buildParams.add(
            [$class: 'StringParameterValue',
                name: 'CORE_OPERATOR_IMAGE_FILTER',
                value: "${CURRENT_CORE_OPERATOR_IMAGE_PATH}"])
        buildParams.add(
            [$class: 'StringParameterValue',
                name: 'DAM_OPERATOR_IMAGE_FILTER',
                value: "${CURRENT_DAM_OPERATOR_IMAGE_PATH}"])
    }
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'LDAP_IMAGE_FILTER',
            value: "${CURRENT_LDAP_IMAGE_PATH}"])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'SKIP_ACCEPTANCE_TESTS',
         value: "${commonConfig.COMMON_SKIP_ACCEPTANCE_TESTS}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'DOMAIN_SUFFIX',
         value: "${DOMAIN_SUFFIX}"]) 
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'DEPLOYMENT_METHOD',
         value: "${DEPLOYMENT_METHOD}"])
  

    /* DAM RTRM Performance runs weekly once in GKE during that time skipping daily DAM RTRM jobs
       Also addded condition to daily DAM RTRM jobs to run only for EKS and native
    */
    if("${env.DAM_RTRM_PERFORMANCE}".toBoolean() && KUBE_FLAVOUR == "google"){
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'DAM_RTRM_PERFORMANCE',
         value: "true"])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'DAM_RTRM_TESTS',
         value: "false"])
    } else if(KUBE_FLAVOUR == "aws" || KUBE_FLAVOUR == "native") {
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'DAM_RTRM_TESTS',
         value: "${commonConfig.COMMON_DAM_RTRM_TESTS}"])
    }
  
    buildParams.add(
        [$class: 'BooleanParameterValue',
            name: 'SKIP_DATA_SETUP_VERIFY',
            value: "false"])

    /* Determines what branch to pull for setup */
    echo "UPDATE_DEPLOYMENT_LEVEL: ${env.UPDATE_DEPLOYMENT_LEVEL}"
    data_setup_verify_branch =  ""
    if(env.UPDATE_DEPLOYMENT_LEVEL){
        switch(env.UPDATE_DEPLOYMENT_LEVEL){
            case "release":
                coreTag = retrieveImageTag(commonConfig.COMMON_CORE_IMAGE_PATH, 'rivendell_release');
                coreTag= coreTag - 'v'
                version=coreTag.split("_",3)
                data_setup_verify_branch= "release/${version[0]}_${version[1]}"
            break;
            default:
                data_setup_verify_branch=env.UPDATE_DEPLOYMENT_LEVEL
            break;
        }
    }
    echo "DATA SETUP VERIFY BRANCH-- ${data_setup_verify_branch}"

    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'DATA_SETUP_VERIFY_BRANCH',
            value: data_setup_verify_branch])
    return buildParams;
}

def createKubeParams(NAMESPACE, KUBE_FLAVOUR, CLUSTER_NAME, CLUSTER_REGION, RESOURCE_GROP, CONTEXT_ROOT_PATH, DX_CORE_HOME_PATH, PERSONALIZED_DX_CORE_PATH, DEPLOYMENT_LEVEL, DEPLOYMENT_METHOD, DOMAIN_SUFFIX, PERFORMANCE_RUN_FLAG) {
    buildParams = prepareK8SpecificParams(false, DEPLOYMENT_LEVEL, KUBE_FLAVOUR);
    if(DEPLOYMENT_LEVEL != "develop") {
         DEPLOYMENT_LEVEL = "rivendell_${DEPLOYMENT_LEVEL}"
    }
    /* Kube update parameters */
    if (KUBE_FLAVOUR == "native") {
        buildParams.add(
            [$class: 'StringParameterValue',
             name: 'INSTANCE_NAME',
             value: "${NAMESPACE}"])
        /* The default for NEXT_JOB_DELAY_HOURS is 24 hours. */
        /* This interferes with the test schedule so setting to 15 */
        buildParams.add(
            [$class: 'StringParameterValue',
             name: 'NEXT_JOB_DELAY_HOURS',
             value: "15"])
        if (DEPLOYMENT_LEVEL != "develop") {
            buildParams.add(
                [$class: 'StringParameterValue',
                name: 'IMAGE_REPOSITORY',
                value: "quintana-docker-prod"])
        }
    } else {
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'NAMESPACE',
            value: "${NAMESPACE}"])
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'IMAGE_REPOSITORY',
            value: "${KUBE_FLAVOUR}"])
        buildParams.add(
            [$class: 'StringParameterValue',
             name: 'CLUSTER_NAME',
             value: "${CLUSTER_NAME}"])
        buildParams.add(
            [$class: 'StringParameterValue',
             name: 'CLUSTER_REGION',
             value: "${CLUSTER_REGION}"])
        buildParams.add(
            [$class: 'StringParameterValue',
             name: 'RESOURCE_GROP',
             value: "${RESOURCE_GROP}"])
        buildParams.add(
            [$class: 'BooleanParameterValue',
             name: 'PUSH_IMAGE_TO_REGISTRY',
             value: "${commonConfig.COMMON_PUSH_IMAGES}"])
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'DB_TYPE',
            value: "${commonConfig.COMMON_DB2_TYPE}"])
        buildParams.add(
            [$class: 'BooleanParameterValue',
            name: 'ENABLE_DB_CONFIG',
            value: "${commonConfig.COMMON_ENABLE_DB_CONFIG}"])
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'NEXT_JOB_DELAY_HOURS',
            value: "${NEXT_JOB_DELAY_HOURS}"])
    }
    if(PERFORMANCE_RUN_FLAG == "true"){
         buildParams.add(
        [$class: 'BooleanParameterValue',
            name: 'PERFORMANCE_RUN',
            value: "true"])
         buildParams.add(
            [$class: 'BooleanParameterValue',
            name: 'ENABLE_DB_CONFIG',
            value: "${commonConfig.COMMON_ENABLE_DB_CONFIG}"])
    }
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'DEPLOYMENT_METHOD',
         value: "${DEPLOYMENT_METHOD}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'KUBE_FLAVOUR',
         value: "${KUBE_FLAVOUR}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'CONTEXT_ROOT_PATH',
         value: "${CONTEXT_ROOT_PATH}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'DX_CORE_HOME_PATH',
         value: "${DX_CORE_HOME_PATH}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'PERSONALIZED_DX_CORE_PATH',
         value: "${PERSONALIZED_DX_CORE_PATH}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'DESIGN_STUDIO_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'CORE_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'RS_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'DAM_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'CC_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'DAM_PLUGIN_GOOGLE_VISION_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'RINGAPI_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'IMGPROC_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
   buildParams.add(
        [$class: 'StringParameterValue',
         name: 'DAM_KALTURA_PLUGIN_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'HAPROXY_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'LOGGING_SIDECAR_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'PREREQS_CHECKER_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'PERSISTENCE_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'PERSISTENCE_NODE_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'PERSISTENCE_METRICS_EXPORTER_IMAGE_FILTER',
            value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'PERSISTENCE_CONNECTION_POOL_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    if ((KUBE_FLAVOUR != "native") || (DEPLOYMENT_METHOD == "helm")) {
        buildParams.add(
            [$class: 'StringParameterValue',
             name: 'RUNTIME_CONTROLLER_IMAGE_FILTER',
             value: "${DEPLOYMENT_LEVEL}"])
        buildParams.add(
            [$class: 'StringParameterValue',
             name: 'HELM_CHARTS_FILTER',
             value: "${DEPLOYMENT_LEVEL}"])
    }
    if ((KUBE_FLAVOUR != "native") || (DEPLOYMENT_METHOD != "helm")) {
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'CORE_OPERATOR_IMAGE_FILTER',
            value: "${DEPLOYMENT_LEVEL}"])
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'DAM_OPERATOR_IMAGE_FILTER',
            value: "${DEPLOYMENT_LEVEL}"])
    }
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'LDAP_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'SKIP_ACCEPTANCE_TESTS',
         value: "${commonConfig.COMMON_SKIP_ACCEPTANCE_TESTS}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'DOMAIN_SUFFIX',
         value: "${DOMAIN_SUFFIX}"]) 

    skip_exec_setup_verify = "true";
    data_setup_verify_branch = "";

    /* DAM RTRM test to run only during update deployments not for fresh deployments and to run only in EKS and native kube */
    if(env.UPDATE_DEPLOYMENT_LEVEL && (KUBE_FLAVOUR == "native" || KUBE_FLAVOUR == "aws")){
        buildParams.add(
        [$class: 'BooleanParameterValue',
        name: 'DAM_RTRM_TESTS',
        value: "${commonConfig.COMMON_DAM_RTRM_TESTS}"])

        skip_exec_setup_verify = "false";
        switch(env.UPDATE_DEPLOYMENT_LEVEL){
            case "release":
                coreTag = retrieveImageTag(commonConfig.COMMON_CORE_IMAGE_PATH, 'rivendell_release');
                coreTag= coreTag - 'v'
                version=coreTag.split("_",3)
                data_setup_verify_branch= "release/${version[0]}_${version[1]}"
            break;
            default:
                data_setup_verify_branch=env.UPDATE_DEPLOYMENT_LEVEL
            break;
        }
    }

    echo "DATA SETUP VERIFY BRANCH-- ${data_setup_verify_branch}"

    buildParams.add(
            [$class: 'BooleanParameterValue',
            name: 'SKIP_DATA_SETUP_VERIFY',
            value: skip_exec_setup_verify])
    buildParams.add(
            [$class: 'StringParameterValue',
            name: 'DATA_SETUP_VERIFY_BRANCH',
        value: data_setup_verify_branch])
    return buildParams;
}

def createOnpremiseUnDeployParams(NAMESPACE, CLUSTERED_ENV) {
    buildParams = []
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'INSTANCE_NAME',
            value: "${NAMESPACE}"])
    buildParams.add(
        [$class: 'BooleanParameterValue',
            name: 'CLUSTERED_ENV',
            value: "${CLUSTERED_ENV}"])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'REMOTE_SEARCH_ENV',
         value: "true"])
    return buildParams;
}


def createOnpremiseDeployParams(NAMESPACE, CLUSTERED_ENV, ADD_ADDITIONAL_NODE, DEPLOYMENT_LEVEL) {

    buildParams = []
    if(DEPLOYMENT_LEVEL == "master") {
        CORE_VERSIONS = sh (script: "${workspace}/autonomous-deployments/scripts/get_latest_cf.sh true", returnStdout: true)
        CORE_VERSIONS = CORE_VERSIONS.split('CF')
        echo "CORE_VERSIONS = ${CORE_VERSIONS}"
        CURRENT_CORE_CF_NUMBER = CORE_VERSIONS[CORE_VERSIONS.length - 1]
        CURRENT_CORE_CF_NUMBER = CURRENT_CORE_CF_NUMBER.toInteger()
        CURRENT_CORE_CF_VERSION = "CF${CURRENT_CORE_CF_NUMBER}"

        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'CF_VERSION',
            value: "${CURRENT_CORE_CF_VERSION}"])

        echo "CURRENT_CORE_CF_VERSION = ${CURRENT_CORE_CF_VERSION}"
    }

    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'INSTANCE_NAME',
         value: "${NAMESPACE}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'DBTYPE',
         value: "${commonConfig.COMMON_DB2_TYPE}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'LDAP_CONFIG_HOST',
         value: "3.21.231.178"])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'CLUSTERED_ENV',
         value: "${CLUSTERED_ENV}"])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'ADD_ADDITIONAL_NODE',
         value: "${ADD_ADDITIONAL_NODE}"])

    buildParams.add(
            [$class: 'StringParameterValue',
             name: 'DOMAIN_SUFFIX',
             value: ".team-q-dev.com"])
    if(DEPLOYMENT_LEVEL == "develop") {
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'DX_CORE_BUILD_VERSION',
            value: "develop"])
    }else {
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'DX_CORE_BUILD_VERSION',
            value: "${DEPLOYMENT_LEVEL}"])
    }
    buildParams.add(
            [$class: 'BooleanParameterValue',
            name: 'REMOTE_SEARCH_ENV',
            value: "false"])
    return buildParams;
}

def createOnpremiseUpgradeParams(NAMESPACE, CLUSTERED_ENV, ADD_ADDITIONAL_NODE) {
    buildParams = []
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'INSTANCE_NAME',
         value: "${NAMESPACE}"])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'CLUSTERED_ENV',
         value: "${CLUSTERED_ENV}"])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'ADD_ADDITIONAL_NODE',
         value: "${ADD_ADDITIONAL_NODE}"])
    buildParams.add(
            [$class: 'StringParameterValue',
             name: 'DOMAIN_SUFFIX',
             value: ".team-q-dev.com"])
    buildParams.add(
            [$class: 'StringParameterValue',
            name: 'DX_CORE_BUILD_VERSION',
            value: "develop"])
    buildParams.add(
        [$class: 'BooleanParameterValue',
         name: 'REMOTE_SEARCH_ENV',
         value: "false"])
    return buildParams;
}

def createStagingParams(){
    buildParams = []
    buildParams.add(
            [$class: 'StringParameterValue',
            name: 'NAMESPACE',
            value: "${env.NAMESPACE}"])
    buildParams.add(
            [$class: 'StringParameterValue',
            name: 'DEPLOYMENT_LEVEL',
            value: "${env.DEPLOYMENT_LEVEL}"])
    buildParams.add(
            [$class: 'StringParameterValue',
            name: 'KUBE_FLAVOUR',
            value: "${env.KUBE_FLAVOUR}"])
    buildParams.add(
            [$class: 'StringParameterValue',
            name: 'DEPLOYMENT_METHOD',
            value: "${env.DEPLOYMENT_METHOD}"])
    buildParams.add(
            [$class: 'StringParameterValue',
            name: 'CONTEXT_ROOT_PATH',
            value: "${env.CONTEXT_ROOT_PATH}"])
    buildParams.add(
            [$class: 'StringParameterValue',
            name: 'DX_CORE_HOME_PATH',
            value: "${env.DX_CORE_HOME_PATH}"])
    buildParams.add(
            [$class: 'StringParameterValue',
            name: 'PERSONALIZED_DX_CORE_PATH',
            value: "${env.PERSONALIZED_DX_CORE_PATH}"])
    buildParams.add(
            [$class: 'StringParameterValue',
            name: 'DOMAIN_SUFFIX',
            value: "${env.DOMAIN_SUFFIX}"])
    buildParams.add(
            [$class: 'StringParameterValue',
            name: 'KUBE_DEPLOY_JOB',
            value: "${env.KUBE_DEPLOY_JOB}"])
    buildParams.add(
            [$class: 'StringParameterValue',
            name: 'KUBE_UNDEPLOY_JOB',
            value: "${env.KUBE_UNDEPLOY_JOB}"])
    buildParams.add(
            [$class: 'StringParameterValue',
            name: 'JMETER_BRANCH',
            value: "${env.JMETER_BRANCH}"])
    buildParams.add(
            [$class: 'StringParameterValue',
                name: 'CLUSTER_NAME',
                value: "${env.CLUSTER_NAME}"])
    buildParams.add(
            [$class: 'StringParameterValue',
                name: 'CLUSTER_REGION',
                value: "${env.CLUSTER_REGION}"])
    buildParams.add(
            [$class: 'StringParameterValue',
                name: 'EXP_API',
                value: "${env.EXP_API}"])
    buildParams.add(
            [$class: 'StringParameterValue',
                name: 'DAM_API',
                value: "${env.DAM_API}"])
    buildParams.add(
            [$class: 'StringParameterValue',
                name: 'TARGET_BRANCH',
                value: "${env.TARGET_BRANCH}"])
    buildParams.add(
            [$class: 'BooleanParameterValue',
                name: 'SSL_ENABLED',
                value: "${env.SSL_ENABLED}"])
    return buildParams;                
}

/* Mandatory return statement on EOF */
return this
