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

commonConfig = load "./dx-dam-staging-tests/config/common.gvy"

 def retrieveMasterImageCreationDate(CURRENT_BUILD_FILTER) {

    CURRENT_BUILD_IMAGE_TAG_CORE  = retrieveImageTag(commonConfig.COMMON_CORE_IMAGE_PATH, CURRENT_BUILD_FILTER);
    CURRENT_BUILD_IMAGE_TAG_CORE_OPERATOR  = retrieveImageTag(commonConfig.COMMON_CORE_OPERATOR_IMAGE_PATH, CURRENT_BUILD_FILTER);
    CURRENT_BUILD_IMAGE_TAG_DAM_OPERATOR  = retrieveImageTag(commonConfig.COMMON_DAM_OPERATOR_IMAGE_PATH, CURRENT_BUILD_FILTER);
    CURRENT_BUILD_IMAGE_TAG_RS  = retrieveImageTag(commonConfig.COMMON_RS_IMAGE_PATH, CURRENT_BUILD_FILTER);

    CURRENT_BUILD_DATE = CURRENT_BUILD_IMAGE_TAG_CORE.split("-")[0].split("_")[2]

    def imageTagMap = [:]
    imageTagMap.putAt(commonConfig.COMMON_CORE_IMAGE_PATH, CURRENT_BUILD_IMAGE_TAG_CORE)
    imageTagMap.putAt(commonConfig.COMMON_CORE_OPERATOR_IMAGE_PATH, CURRENT_BUILD_IMAGE_TAG_CORE_OPERATOR)
    imageTagMap.putAt(commonConfig.COMMON_DAM_OPERATOR_IMAGE_PATH, CURRENT_BUILD_IMAGE_TAG_DAM_OPERATOR)
    imageTagMap.putAt(commonConfig.COMMON_RS_IMAGE_PATH, CURRENT_BUILD_IMAGE_TAG_RS)

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

def retrievePrevAddOnMasterImagesDate(IMAGE_TAG, UPDATE_RELEASE_LEVEL) {
    CURRENT_BUILD_NUMBER = IMAGE_TAG.split("-")[0].split("_")[1].split("CF")[1].toInteger()
    prevBuildNumber = CURRENT_BUILD_NUMBER - UPDATE_RELEASE_LEVEL.toInteger()
    CURRENT_BUILD_LABEL_FOR_ADD_ONS = "v95_CF${prevBuildNumber}"
    CURRENT_BUILD_FILTER = "${CURRENT_BUILD_LABEL_FOR_ADD_ONS}.*.rivendell_master"
    CURRENT_BUILD_DATE = retrieveMasterImageCreationDate(CURRENT_BUILD_FILTER)
    return CURRENT_BUILD_DATE;
}

def retrieveImageTag(imagePath, imageFilter) {
    dir("${workspace}/dx-dam-staging-tests/scripts/") {
        tag = sh (script: "./get_latest_image_wrapper.sh ${imagePath} ${imageFilter} ${commonConfig.COMMON_IMAGE_AREA}", returnStdout: true)
        if (tag.length() < 1) {
            error("Could not find matching tag for image path ${imagePath} and filter ${imageFilter}")
        }
        return tag;
    }
}

def retrieveImageTagLDAP(imagePath, imageFilter) {
    dir("${workspace}/dx-dam-staging-tests/scripts/") {
        tag = sh (script: "./get_latest_image_wrapper.sh ${imagePath} ${imageFilter} ${commonConfig.COMMON_IMAGE_AREA} true", returnStdout: true)
        return tag;
    }
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
            env.RINGAPI_IMAGE_PATH = "dx-build-output/core-addon/api/ringapi"
            env.IMGPROC_IMAGE_PATH = "dx-build-output/core-addon/image-processor"
            env.PERSISTENCE_IMAGE_PATH = "dx-build-output/core-addon/persistence/postgres"
            env.PERSISTENCE_CONNECTION_POOL_IMAGE_PATH = "dx-build-output/core-addon/persistence/pgpool"
            env.PERSISTENCE_NODE_IMAGE_PATH = "dx-build-output/core-addon/persistence/postgresrepmgr"
            env.DESIGN_STUDIO_IMAGE_PATH = "dx-build-output/core-addon/site-manager"
            env.RUNTIME_CONTROLLER_IMAGE_PATH = "dx-build-output/operator/hcldx-runtime-controller"
            env.CORE_OPERATOR_IMAGE_PATH = "dx-build-output/operator/hcldx-cloud-operator"
            env.DAM_OPERATOR_IMAGE_PATH = "dx-build-output/operator/hcl-medialibrary-operator"
            env.LDAP_IMAGE_PATH = "dx-build-output/core-addon/dx-openldap"
            env.RS_IMAGE_PATH = "dx-build-output/core/dxrs"
        } else {
            env.CORE_IMAGE_PATH = "dxen"
            env.DAM_IMAGE_PATH = "portal/media-library"
            env.CC_IMAGE_PATH = "portal/content-ui"
            env.RINGAPI_IMAGE_PATH = "portal/api/ringapi"
            env.IMGPROC_IMAGE_PATH = "portal/image-processor"
            env.PERSISTENCE_IMAGE_PATH = "portal/persistence/postgres"
            env.PERSISTENCE_CONNECTION_POOL_IMAGE_PATH = "dx-build-output/core-addon/persistence/pgpool"
            env.PERSISTENCE_NODE_IMAGE_PATH = "dx-build-output/core-addon/persistence/postgresrepmgr"
            env.DESIGN_STUDIO_IMAGE_PATH = "dx-build-output/core-addon/site-manager"
            env.RUNTIME_CONTROLLER_IMAGE_PATH = "dx-build-output/operator/hcldx-runtime-controller"
            env.CORE_OPERATOR_IMAGE_PATH = "hcldx-cloud-operator"
            env.DAM_OPERATOR_IMAGE_PATH = "hcl-medialibrary-operator"
            env.LDAP_IMAGE_PATH = "dx-openldap"
            env.RS_IMAGE_PATH = "dxrs"
        }
        env.AMBASSADOR_IMAGE_PATH = "ambassador"
        env.REDIS_IMAGE_PATH = "redis"

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
            name: 'RINGAPI_IMAGE_PATH',
            value: "${env.RINGAPI_IMAGE_PATH}"])
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'IMGPROC_IMAGE_PATH',
            value: "${env.IMGPROC_IMAGE_PATH}"])
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
            name: 'REDIS_IMAGE_PATH',
            value: "${env.REDIS_IMAGE_PATH}"])
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'LDAP_IMAGE_PATH',
            value: "${env.LDAP_IMAGE_PATH}"])
        buildParams.add(
            [$class: 'StringParameterValue',
            name: 'RS_IMAGE_PATH',
            value: "${env.RS_IMAGE_PATH}"])

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

def createKubeParams(NAMESPACE, KUBE_FLAVOUR, CLUSTER_NAME, CLUSTER_REGION, RESOURCE_GROP, CONTEXT_ROOT_PATH, DX_CORE_HOME_PATH, PERSONALIZED_DX_CORE_PATH, DEPLOYMENT_LEVEL, DEPLOYMENT_METHOD, DOMAIN_SUFFIX) {
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
         name: 'RINGAPI_IMAGE_FILTER',
         value: "${DEPLOYMENT_LEVEL}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'IMGPROC_IMAGE_FILTER',
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
        [$class: 'StringParameterValue',
         name: 'REDIS_IMAGE_FILTER',
         value: "${commonConfig.COMMON_REDIS_IMAGE_FILTER}"])
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

    /* DAM RTRM test to run only during update deployments not for fresh deployments */
    if(env.UPDATE_DEPLOYMENT_LEVEL){
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

/* Mandatory return statement on EOF */
return this
