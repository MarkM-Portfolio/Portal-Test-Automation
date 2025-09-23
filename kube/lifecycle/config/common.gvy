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


// Import the shared library with the name configured in Jenkins
@Library("dx-shared-library") _

/*
 * Common configurations file, used for all kube flavours
 */

/* Indicates number of minutes to retry for DX core pod to be in ready state after updating */
if (env.DX_UPDATE_PROBE_RETRIES) {
    DX_UPDATE_PROBE_RETRIES = env.DX_UPDATE_PROBE_RETRIES
} else {
    DX_UPDATE_PROBE_RETRIES = "270"
}

/* Indicates number of minutes to retry for DX core pod to be in ready state for fresh install */
if (env.DX_FRESH_PROBE_RETRIES) {
    DX_FRESH_PROBE_RETRIES = env.DX_FRESH_PROBE_RETRIES
} else {
    DX_FRESH_PROBE_RETRIES = "200"
}

/* Indicates number of minutes to retry for DX core pod to be in ready state for fresh install */
if (env.DX_START_VERBOSE_MODE) {
    DX_START_VERBOSE_MODE = env.DX_START_VERBOSE_MODE
} else {
    DX_START_VERBOSE_MODE = ""
}

/*
 * Repository settings, we default to quintana non prod
 * We have specifc settings for different flavours
 * If there is a setting specified that does not match our preconfigured list,
 * We use the provided setting as the repository
 */

switch (env.IMAGE_REPOSITORY) {
    case "":
        COMMON_IMAGE_REPOSITORY = "quintana-docker"
        COMMON_IMAGE_AREA = "artifactory.cwp.pnp-hcl.com"
        println("No specific image repository provided, using fallback - ${COMMON_IMAGE_REPOSITORY}.")
        break
    case "quintana-docker":
        COMMON_IMAGE_REPOSITORY = "quintana-docker"
        COMMON_IMAGE_AREA = "artifactory.cwp.pnp-hcl.com"
        println("Using quintana-docker image repository - ${COMMON_IMAGE_REPOSITORY}.")
        break
    case "quintana-docker-prod":
        COMMON_IMAGE_REPOSITORY = "quintana-docker-prod"
        COMMON_IMAGE_AREA = "artifactory.cwp.pnp-hcl.com"
        println("Using quintana-docker-prod image repository - ${COMMON_IMAGE_REPOSITORY}.")
        break
    case "openshift":
        COMMON_IMAGE_REPOSITORY = "657641368736.dkr.ecr.us-east-2.amazonaws.com"
        COMMON_IMAGE_AREA = "657641368736.dkr.ecr.us-east-2.amazonaws.com"
        println("Using openshift image repository - ${COMMON_IMAGE_REPOSITORY}.")
        break
    case "openshiftnjdc":
        COMMON_IMAGE_REPOSITORY = "quintana-docker"
        COMMON_IMAGE_AREA = "artifactory.cwp.pnp-hcl.com"
        println("Using openshift image repository - ${COMMON_IMAGE_REPOSITORY}.")
        break
    case "openshiftnjdc-prod":
        COMMON_IMAGE_REPOSITORY = "quintana-docker-prod"
        COMMON_IMAGE_AREA = "artifactory.cwp.pnp-hcl.com"
        println("Using openshift image repository - ${COMMON_IMAGE_REPOSITORY}.")
        break
    case "google":
        COMMON_IMAGE_REPOSITORY = "us.gcr.io/hcl-gcp-l2com-sofy/dxcontainers"
        COMMON_IMAGE_AREA = "us.gcr.io/hcl-gcp-l2com-sofy/dxcontainers"
        println("Using gcr image repository - ${COMMON_IMAGE_REPOSITORY}.")
        break
    case "aws":
        COMMON_IMAGE_REPOSITORY = "657641368736.dkr.ecr.us-east-2.amazonaws.com"
        COMMON_IMAGE_AREA = "657641368736.dkr.ecr.us-east-2.amazonaws.com"
        println("Using aws ecr image repository - ${COMMON_IMAGE_REPOSITORY}.")
        break
    case "azure":
        COMMON_IMAGE_REPOSITORY = "dxcontainers2.azurecr.io"
        COMMON_IMAGE_AREA = "dxcontainers2.azurecr.io"
        println("Using azure cr image repository - ${COMMON_IMAGE_REPOSITORY}.")
        break
    case "harbor":
        COMMON_IMAGE_REPOSITORY = "hclcr.io"
        COMMON_IMAGE_AREA = "projects/20/"
        println("Using Harbor image repository - ${COMMON_IMAGE_REPOSITORY}.")
        break
    case "aws-marketplace":
        COMMON_IMAGE_REPOSITORY = "709825985650.dkr.ecr.us-east-1.amazonaws.com/hcl-america"
        COMMON_IMAGE_AREA = "709825985650.dkr.ecr.us-east-1.amazonaws.com/hcl-america"
        println("Using AWS Marketplace image repository - ${COMMON_IMAGE_REPOSITORY}.")
        break      
    default:
        COMMON_IMAGE_REPOSITORY = env.IMAGE_REPOSITORY
        COMMON_IMAGE_AREA = "quintana-docker"
        println("Using manually defined image repository - ${COMMON_IMAGE_REPOSITORY}.")
        break
}

/*
 * Harbor project name
 */
if (env.HARBOR_PROJECT) {
    COMMON_HARBOR_PROJECT = env.HARBOR_PROJECT
} else {
    COMMON_HARBOR_PROJECT = 'dx-staging'
}

/*
 * Image filter parameters used for determine the latest images
 */

/* HCL DX Core */
if (env.CORE_IMAGE_FILTER) {
    COMMON_CORE_IMAGE_FILTER = env.CORE_IMAGE_FILTER
} else {
    COMMON_CORE_IMAGE_FILTER = "develop"
}

/* Digital Asset Manager */
if (env.DAM_IMAGE_FILTER) {
    COMMON_DAM_IMAGE_FILTER = env.DAM_IMAGE_FILTER
} else {
    COMMON_DAM_IMAGE_FILTER = "develop"
}

/* applications domain */
if (env.DOMAIN) {
    DOMAIN = env.DOMAIN
} else {
    DOMAIN = "apps.dx-cluster-dev.hcl-dx-dev.net"
}

/* Digital Asset Manager Feature*/
if (env.DAM_FEATURES) {
    COMMON_DAM_FEATURES = env.DAM_FEATURES
} else {
    COMMON_DAM_FEATURES = ""
}

if (env.ENABLE_DAM_CLEAN_UP) {
    COMMON_ENABLE_DAM_CLEAN_UP = env.ENABLE_DAM_CLEAN_UP
} else {
    COMMON_ENABLE_DAM_CLEAN_UP = "false"
}

/* Heartbeats and Thresholds for Clean Up pipeline
(In this pipeline, we upload assets and break the data and verify healing with below set of parameters
which makes healing faster than default parameters) */
VALIDATION_HEARTBEAT_INTERVAL_TIME_IN_MINUTES = 5
RENDITION_OR_VERSION_HEARTBEAT_INTERVAL_TIME_IN_MINUTES = 10
CLEANUP_HEARTBEAT_INTERVAL_TIME_IN_MINUTES = 15
ORPHANDATA_AND_FILE_CLEANUP_HEARTBEAT_INTERVAL_TIME_IN_MINUTES = 25
MEDIA_CREATION_THRESHOLD_TIME_IN_MINUTES = 10
LAST_SCAN_THRESHOLD_TIME_IN_MINUTES = 10
ORPHAN_DIRECTORY_MODIFICATION_THRESHOLD_TIME_IN_MINUTES = 10
ORPHAN_MEDIA_STORAGE_CREATION_THRESHOLD_TIME_IN_MINUTES = 10
MAX_VALIDATION_PROCESSING_LIMIT = 5

/* Search Middleware Service
With CFxxx we introduce Search Middleware Service to interact between DX and OpenSearch. */
SEARCH_MIDDLEWARE_SERVICE_QUERY_NAME = "dx-search-search-middleware-query"

/* Content Composer */
if (env.CC_IMAGE_FILTER) {
    COMMON_CC_IMAGE_FILTER = env.CC_IMAGE_FILTER
} else {
    COMMON_CC_IMAGE_FILTER = "develop"
}

/* DAM Plugin Google Vision */
if (env.DAM_PLUGIN_GOOGLE_VISION_IMAGE_FILTER) {
    COMMON_DAM_PLUGIN_GOOGLE_VISION_IMAGE_FILTER = env.DAM_PLUGIN_GOOGLE_VISION_IMAGE_FILTER
} else {
    COMMON_DAM_PLUGIN_GOOGLE_VISION_IMAGE_FILTER = "develop"
}

if(env.PERFORMANCE_RUN){
    COMMON_DAM_PLUGIN_GOOGLE_VISION_STUB_MODE = env.PERFORMANCE_RUN
} else {
    COMMON_DAM_PLUGIN_GOOGLE_VISION_STUB_MODE = "false"
}

/* Ring-API */
if (env.RINGAPI_IMAGE_FILTER) {
    COMMON_RINGAPI_IMAGE_FILTER = env.RINGAPI_IMAGE_FILTER
} else {
    COMMON_RINGAPI_IMAGE_FILTER = "develop"
}

/* Image processor */
if (env.IMGPROC_IMAGE_FILTER) {
    COMMON_IMGPROC_IMAGE_FILTER = env.IMGPROC_IMAGE_FILTER
} else {
    COMMON_IMGPROC_IMAGE_FILTER = "develop"
}

/* DAM KALTURA PLUGIN */
if (env.DAM_KALTURA_PLUGIN_IMAGE_FILTER) {
    COMMON_DAM_KALTURA_PLUGIN_IMAGE_FILTER = env.DAM_KALTURA_PLUGIN_IMAGE_FILTER
} else {
    COMMON_DAM_KALTURA_PLUGIN_IMAGE_FILTER = "develop"
}

/* Persistence Connection Pool */
if (env.PERSISTENCE_CONNECTION_POOL_IMAGE_FILTER) {
    COMMON_PERSISTENCE_CONNECTION_POOL_IMAGE_FILTER = env.PERSISTENCE_CONNECTION_POOL_IMAGE_FILTER
} else {
    COMMON_PERSISTENCE_CONNECTION_POOL_IMAGE_FILTER = "develop"
}

/* Persistence Node*/
if (env.PERSISTENCE_NODE_IMAGE_FILTER) {
    COMMON_PERSISTENCE_NODE_IMAGE_FILTER = env.PERSISTENCE_NODE_IMAGE_FILTER
} else {
    COMMON_PERSISTENCE_NODE_IMAGE_FILTER = "develop"
}

/* DX runtime controller */
if (env.RUNTIME_CONTROLLER_IMAGE_FILTER) {
    COMMON_RUNTIME_CONTROLLER_IMAGE_FILTER = env.RUNTIME_CONTROLLER_IMAGE_FILTER
} else {
    COMMON_RUNTIME_CONTROLLER_IMAGE_FILTER = "develop"
}

/* HAProxy */
if (env.HAPROXY_IMAGE_FILTER) {
    COMMON_HAPROXY_IMAGE_FILTER = env.HAPROXY_IMAGE_FILTER
} else {
    COMMON_HAPROXY_IMAGE_FILTER = "develop"
}

/* License-Manager */
if (env.LICENSE_MANAGER_IMAGE_FILTER) {
    COMMON_LICENSE_MANAGER_IMAGE_FILTER = env.LICENSE_MANAGER_IMAGE_FILTER
} else {
    COMMON_LICENSE_MANAGER_IMAGE_FILTER = "develop"
}

if (env.IS_PRODUCTION_ENV) {
    COMMON_IS_PRODUCTION_ENV = env.IS_PRODUCTION_ENV
} else {
    COMMON_IS_PRODUCTION_ENV = "false"
}

if (env.LICENSE_SERVER_ID) {
    COMMON_LICENSE_SERVER_ID = env.LICENSE_SERVER_ID
} else {
    COMMON_LICENSE_SERVER_ID = "Q8A5YCZ3A4GH"
}

if (env.LICENSE_SERVER_URI) {
    COMMON_LICENSE_SERVER_URI = env.LICENSE_SERVER_URI
} else {
    COMMON_LICENSE_SERVER_URI = "https://hclsoftware-uat.compliance.flexnetoperations.com"
}

if (env.LICENSE_SERVER_FEATURE_WITH_VERSION) {
    COMMON_LICENSE_SERVER_FEATURE_WITH_VERSION = env.LICENSE_SERVER_FEATURE_WITH_VERSION
} else {
    COMMON_LICENSE_SERVER_FEATURE_WITH_VERSION = "DXPN_CloudNative_Tier1_500K@9.5,DXPN_CloudNative_Tier2_2M@9.5,DXPN_CloudNative_Tier3_6M@9.5,DXPN_CloudNative_Tier4_12M@9.5,DXPN_CloudNative_Tier5_24M@9.5,DXPN_CloudNative_Tier6_60M@9.5,DXPN_CloudNative_Tier7_120M@9.5"
}

if (env.LICENSE_USERNAME) {
    COMMON_LICENSE_USERNAME = env.LICENSE_USERNAME
} else {
    COMMON_LICENSE_USERNAME = "admin"
}

if (env.LICENSE_PASSWORD) {
    COMMON_LICENSE_PASSWORD = env.LICENSE_PASSWORD
} else {
    COMMON_LICENSE_PASSWORD = ""
}

/* Logging Sidecar */
if (env.LOGGING_SIDECAR_IMAGE_FILTER) {
    COMMON_LOGGING_SIDECAR_DIFF_IMAGE_FILTER = env.LOGGING_SIDECAR_IMAGE_FILTER
} else {
    COMMON_LOGGING_SIDECAR_DIFF_IMAGE_FILTER = "develop"
}

/* Additional images of componentens that are optional for deployment */

/* Open LDAP */
if (env.LDAP_IMAGE_FILTER) {
    COMMON_LDAP_IMAGE_FILTER = env.LDAP_IMAGE_FILTER
} else {
    COMMON_LDAP_IMAGE_FILTER = "develop"
}

/* Remote Search */
if (env.RS_IMAGE_FILTER) {
    COMMON_RS_IMAGE_FILTER = env.RS_IMAGE_FILTER
} else {
    COMMON_RS_IMAGE_FILTER = "develop"
}

/* Remote Search Auto Configuration */
if (env.ENABLE_RS_AUTOCONFIG) {
    COMMON_ENABLE_RS_AUTOCONFIG = env.ENABLE_RS_AUTOCONFIG
} else {
    COMMON_ENABLE_RS_AUTOCONFIG = "false"
}

/* Prereqs Checker */
if (env.PREREQS_CHECKER_IMAGE_FILTER) {
    COMMON_PREREQS_CHECKER_IMAGE_FILTER = env.PREREQS_CHECKER_IMAGE_FILTER
} else {
    COMMON_PREREQS_CHECKER_IMAGE_FILTER = "develop"
}

/*
 * Image paths used to lookup the images from the corresponding repository
 * Overridable, having a default matching the new build output paths
 */

/* HCL DX Core */
if (env.CORE_IMAGE_PATH) {
    COMMON_CORE_IMAGE_PATH = env.CORE_IMAGE_PATH
} else {
    COMMON_CORE_IMAGE_PATH = "dx-build-output/core/dxen"
}

/* Digital Asset Manager */
if (env.DAM_IMAGE_PATH) {
    COMMON_DAM_IMAGE_PATH = env.DAM_IMAGE_PATH
} else {
    COMMON_DAM_IMAGE_PATH = "dx-build-output/core-addon/media-library"
}

/* Content Composer */
if (env.CC_IMAGE_PATH) {
    COMMON_CC_IMAGE_PATH = env.CC_IMAGE_PATH
} else {
    COMMON_CC_IMAGE_PATH = "dx-build-output/core-addon/content-ui"
}

/* DAM PLUGIN GOOGLE VISION */
if (env.DAM_PLUGIN_GOOGLE_VISION_IMAGE_PATH) {
    COMMON_DAM_PLUGIN_GOOGLE_VISION_IMAGE_PATH = env.DAM_PLUGIN_GOOGLE_VISION_IMAGE_PATH
} else {
    COMMON_DAM_PLUGIN_GOOGLE_VISION_IMAGE_PATH = "dx-build-output/core-addon/api/dam-plugin-google-vision"
}

/* Ring-API */
if (env.RINGAPI_IMAGE_PATH) {
    COMMON_RINGAPI_IMAGE_PATH = env.RINGAPI_IMAGE_PATH
} else {
    COMMON_RINGAPI_IMAGE_PATH = "dx-build-output/core-addon/api/ringapi"
}

/* Image processor */
if (env.IMGPROC_IMAGE_PATH) {
    COMMON_IMGPROC_IMAGE_PATH = env.IMGPROC_IMAGE_PATH
} else {
    COMMON_IMGPROC_IMAGE_PATH = "dx-build-output/core-addon/image-processor"
}

/* DAM KALTURA PLUGIN */
if (env.DAM_KALTURA_PLUGIN_IMAGE_PATH) {
    COMMON_DAM_KALTURA_PLUGIN_IMAGE_PATH = env.DAM_KALTURA_PLUGIN_IMAGE_PATH
} else {
    COMMON_DAM_KALTURA_PLUGIN_IMAGE_PATH = "dx-build-output/core-addon/api/dam-plugin-kaltura"
}

/* Persistence Connection Pool */
if (env.PERSISTENCE_CONNECTION_POOL_IMAGE_PATH) {
    COMMON_PERSISTENCE_CONNECTION_POOL_IMAGE_PATH = env.PERSISTENCE_CONNECTION_POOL_IMAGE_PATH
} else {
    COMMON_PERSISTENCE_CONNECTION_POOL_IMAGE_PATH = "dx-build-output/core-addon/persistence/pgpool"
}

/* Persistence Node */
if (env.PERSISTENCE_NODE_IMAGE_PATH) {
    COMMON_PERSISTENCE_NODE_IMAGE_PATH = env.PERSISTENCE_NODE_IMAGE_PATH
} else {
    COMMON_PERSISTENCE_NODE_IMAGE_PATH = "dx-build-output/core-addon/persistence/postgresrepmgr"
}

/* Persistence Metrics exporter */
if (env.PERSISTENCE_METRICS_EXPORTER_IMAGE_PATH) {
    COMMON_PERSISTENCE_METRICS_EXPORTER_IMAGE_PATH = env.PERSISTENCE_METRICS_EXPORTER_IMAGE_PATH
} else {
    COMMON_PERSISTENCE_METRICS_EXPORTER_IMAGE_PATH = "dx-build-output/core-addon/persistence/postgres-metrics-exporter"
}

/* DX runtime controller */
if (env.RUNTIME_CONTROLLER_IMAGE_PATH) {
    COMMON_RUNTIME_CONTROLLER_IMAGE_PATH = env.RUNTIME_CONTROLLER_IMAGE_PATH
} else {
    COMMON_RUNTIME_CONTROLLER_IMAGE_PATH = "dx-build-output/operator/hcldx-runtime-controller"
}

/* HAProxy */
if (env.HAPROXY_IMAGE_PATH) {
    COMMON_HAPROXY_IMAGE_PATH = env.HAPROXY_IMAGE_PATH
} else {
    COMMON_HAPROXY_IMAGE_PATH = "dx-build-output/common/haproxy"
}

/* License-Manager */
if (env.LICENSE_MANAGER_IMAGE_PATH) {
    COMMON_LICENSE_MANAGER_IMAGE_PATH = env.LICENSE_MANAGER_IMAGE_PATH
} else {
    COMMON_LICENSE_MANAGER_IMAGE_PATH = "dx-build-output/common/license-manager"
}

/* Logging Sidecar */
if (env.LOGGING_SIDECAR_IMAGE_PATH) {
    COMMON_LOGGING_SIDECAR_DIFF_IMAGE_PATH = env.LOGGING_SIDECAR_IMAGE_PATH
} else {
    COMMON_LOGGING_SIDECAR_DIFF_IMAGE_PATH = "dx-build-output/common/logging-sidecar"
}

/* Prereqs Checker */
if (env.PREREQS_CHECKER_IMAGE_PATH) {
    COMMON_PREREQS_CHECKER_IMAGE_PATH = env.PREREQS_CHECKER_IMAGE_PATH
} else {
    COMMON_PREREQS_CHECKER_IMAGE_PATH = "dx-build-output/common/prereqs-checker"
}

/* Additional images of componentens that are optional for deployment */

/* Open LDAP */
if (env.LDAP_IMAGE_PATH) {
    COMMON_LDAP_IMAGE_PATH = env.LDAP_IMAGE_PATH
} else {
    COMMON_LDAP_IMAGE_PATH = "dx-build-output/core-addon/dx-openldap"
}

/* Remote Search */
if (env.RS_IMAGE_PATH) {
    COMMON_RS_IMAGE_PATH = env.RS_IMAGE_PATH
} else {
    COMMON_RS_IMAGE_PATH = "dx-build-output/core/dxrs"
}

/* Helm Chart location */
if (env.HELM_CHARTS_FILTER) {
    COMMON_HELM_CHARTS_FILTER = env.HELM_CHARTS_FILTER
} else {
    COMMON_HELM_CHARTS_FILTER = "develop"
}

if (env.HELM_CHARTS_PATH) {
    COMMON_HELM_CHARTS_PATH = env.HELM_CHARTS_PATH
} else {
    COMMON_HELM_CHARTS_PATH = "hcl-dx-deployment"
}

if (env.HELM_CHARTS_AREA) {
    COMMON_HELM_CHARTS_AREA = env.HELM_CHARTS_AREA
} else {
    COMMON_HELM_CHARTS_AREA = "quintana-helm"
}

/* Leap Helm Chart location */
if (env.LEAP_HELM_CHARTS_FILTER) {
    COMMON_LEAP_HELM_CHARTS_FILTER = env.LEAP_HELM_CHARTS_FILTER
} else {
    COMMON_LEAP_HELM_CHARTS_FILTER = "develop"
}

if (env.LEAP_HELM_CHARTS_PATH) {
    COMMON_LEAP_HELM_CHARTS_PATH = env.LEAP_HELM_CHARTS_PATH
} else {
    COMMON_LEAP_HELM_CHARTS_PATH = "hcl-leap-deployment"
}

if (env.LEAP_HELM_CHARTS_AREA) {
    COMMON_LEAP_HELM_CHARTS_AREA = env.LEAP_HELM_CHARTS_AREA
} else {
    COMMON_LEAP_HELM_CHARTS_AREA = "quintana-helm"
}

/* Liberty Portlet Container Helm Chart location */
if (env.LPC_HELM_CHARTS_FILTER) {
    COMMON_LPC_HELM_CHARTS_FILTER = env.LPC_HELM_CHARTS_FILTER
} else {
    COMMON_LPC_HELM_CHARTS_FILTER = "develop"
}

if (env.LPC_HELM_CHARTS_PATH) {
    COMMON_LPC_HELM_CHARTS_PATH = env.LPC_HELM_CHARTS_PATH
} else {
    COMMON_LPC_HELM_CHARTS_PATH = "hcl-lpc-deployment"
}

if (env.LPC_HELM_CHARTS_AREA) {
    COMMON_LPC_HELM_CHARTS_AREA = env.LPC_HELM_CHARTS_AREA
} else {
    COMMON_LPC_HELM_CHARTS_AREA = "quintana-helm"
}

/* HCL DS Keycloak Chart location */
if (env.HCLDS_KEYCLOAK_HELM_CHARTS_FILTER) {
    COMMON_HCLDS_KEYCLOAK_HELM_CHARTS_FILTER = env.HCLDS_KEYCLOAK_HELM_CHARTS_FILTER
} else {
    COMMON_HCLDS_KEYCLOAK_HELM_CHARTS_FILTER = "hclds-keycloak-22.0.1-16.0.5_"
}

if (env.HCLDS_KEYCLOAK_HELM_CHARTS_PATH) {
    COMMON_HCLDS_KEYCLOAK_HELM_CHARTS_PATH = env.HCLDS_KEYCLOAK_HELM_CHARTS_PATH
} else {
    COMMON_HCLDS_KEYCLOAK_HELM_CHARTS_PATH = "hclds-keycloak"
}

if (env.HCLDS_KEYCLOAK_HELM_CHARTS_AREA) {
    COMMON_HCLDS_KEYCLOAK_HELM_CHARTS_AREA = env.HCLDS_KEYCLOAK_HELM_CHARTS_AREA
} else {
    COMMON_HCLDS_KEYCLOAK_HELM_CHARTS_AREA = "quintana-helm"
}

/* HCL People Service Chart location */
if (env.PEOPLE_SERVICE_HELM_CHARTS_FILTER) {
    COMMON_PEOPLE_SERVICE_HELM_CHARTS_FILTER = env.PEOPLE_SERVICE_HELM_CHARTS_FILTER
} else {
    COMMON_PEOPLE_SERVICE_HELM_CHARTS_FILTER = "develop"
}

if (env.PEOPLE_SERVICE_HELM_CHARTS_PATH) {
    COMMON_PEOPLE_SERVICE_HELM_CHARTS_PATH = env.PEOPLE_SERVICE_HELM_CHARTS_PATH
} else {
    COMMON_PEOPLE_SERVICE_HELM_CHARTS_PATH = "hcl-people-service"
}

if (env.PEOPLE_SERVICE_HELM_CHARTS_AREA) {
    COMMON_PEOPLE_SERVICE_HELM_CHARTS_AREA = env.PEOPLE_SERVICE_HELM_CHARTS_AREA
} else {
    COMMON_PEOPLE_SERVICE_HELM_CHARTS_AREA = "quintana-helm"
}

/* Opensearch Helm Chart location */
if (env.OPENSEARCH_HELM_CHARTS_FILTER) {
    COMMON_OPENSEARCH_HELM_CHARTS_FILTER = env.OPENSEARCH_HELM_CHARTS_FILTER
} else {
    COMMON_OPENSEARCH_HELM_CHARTS_FILTER = "develop"
}

if (env.OPENSEARCH_HELM_CHARTS_PATH) {
    COMMON_OPENSEARCH_HELM_CHARTS_PATH = env.OPENSEARCH_HELM_CHARTS_PATH
} else {
    COMMON_OPENSEARCH_HELM_CHARTS_PATH = "hcl-dx-search"
}

if (env.OPENSEARCH_HELM_CHARTS_AREA) {
    COMMON_OPENSEARCH_HELM_CHARTS_AREA = env.OPENSEARCH_HELM_CHARTS_AREA
} else {
    COMMON_OPENSEARCH_HELM_CHARTS_AREA = "quintana-helm"
}    

/* Set the helm artifactory for the installation */
if (env.HELM_ARTIFACTORY_URL) {
    COMMON_HELM_ARTIFACTORY_URL = env.HELM_ARTIFACTORY_URL
} else {
    COMMON_HELM_ARTIFACTORY_URL = "https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/helm"
}

/* Set the helm version for the installation */
if (env.HELM_VERSION) {
    COMMON_HELM_VERSION = env.HELM_VERSION
} else {
    COMMON_HELM_VERSION = "v3.11.2"
}

/* Context root path */
if (env.CONTEXT_ROOT_PATH) {
    COMMON_CONTEXT_ROOT_PATH = env.CONTEXT_ROOT_PATH
} else {
    COMMON_CONTEXT_ROOT_PATH = "wps"
}

/* Persistence Exporter*/
/* Moved down due to dependency on HELM CHART FILTER */
if (env.PERSISTENCE_METRICS_EXPORTER_IMAGE_FILTER) {
    COMMON_PERSISTENCE_METRICS_EXPORTER_IMAGE_FILTER = env.PERSISTENCE_METRICS_EXPORTER_IMAGE_FILTER
    println("Using pipeline configured (PERSISTENCE_METRICS_EXPORTER_IMAGE_FILTER) COMMON_PERSISTENCE_METRICS_EXPORTER_IMAGE_FILTER - ${COMMON_PERSISTENCE_METRICS_EXPORTER_IMAGE_FILTER}")
} else {
    /* Hack to set COMMON_PERSISTENCE_METRICS_EXPORTER_IMAGE_FILTER default to release_95_CF199 */
    /* if HELM_CHARTS_FILTER value contains rivendell_                                          */ 
    if (COMMON_HELM_CHARTS_FILTER.contains("rivendell_")) {
        COMMON_PERSISTENCE_METRICS_EXPORTER_IMAGE_FILTER = "release_95_CF199"
    } else {
        COMMON_PERSISTENCE_METRICS_EXPORTER_IMAGE_FILTER = "develop"
    }
    println("Using pre-configured COMMON_PERSISTENCE_METRICS_EXPORTER_IMAGE_FILTER - ${COMMON_PERSISTENCE_METRICS_EXPORTER_IMAGE_FILTER}")
}

/* Personalized DX Core path */
if (env.PERSONALIZED_DX_CORE_PATH) {
    COMMON_PERSONALIZED_DX_CORE_PATH = env.PERSONALIZED_DX_CORE_PATH
} else {
    COMMON_PERSONALIZED_DX_CORE_PATH = ""
}

/* DX Core home path */
if (env.DX_CORE_HOME_PATH) {
    COMMON_DX_CORE_HOME_PATH = env.DX_CORE_HOME_PATH
} else {
    COMMON_DX_CORE_HOME_PATH = ""
}

/* DX replicas default is 1 */
if (env.DX_CORE_REPLICAS) {
    DX_CORE_REPLICAS = env.DX_CORE_REPLICAS
} else {
    DX_CORE_REPLICAS = '1'
}

/* DX replicas default is 1 */
if (env.DX_DAM_REPLICAS) {
    DX_DAM_REPLICAS = env.DX_DAM_REPLICAS
} else {
    DX_DAM_REPLICAS = '1'
}

if (env.IMAGE_AREA) {
    COMMON_IMAGE_AREA = env.IMAGE_AREA
    println("Using manually defined image area - ${COMMON_IMAGE_AREA}")
} else {
    println("Using pre-configured image area - ${COMMON_IMAGE_AREA}")
}

/* Namespace for the deployment */
if (env.NAMESPACE) {
    NAMESPACE = env.NAMESPACE
} else {
    NAMESPACE = "dxns"
}


/* Version of kube to deploy */
if (env.KUBE_VERSION) {
    COMMON_KUBE_VERSION = env.KUBE_VERSION
} else {
    COMMON_KUBE_VERSION = "1.21.13"
}

/* Disable  DX component  open ldap */
if (env.DISABLE_OPENLDAP) {
    COMMON_DISABLE_OPENLDAP = env.DISABLE_OPENLDAP
} else {
    COMMON_DISABLE_OPENLDAP = "false"
}

/* Disable  DX-core */
if (env.DISABLE_DX_CORE) {
    COMMON_DISABLE_DX_CORE = env.DISABLE_DX_CORE
} else {
    COMMON_DISABLE_DX_CORE = "false"
}

/* Disable  DX component remote search */
if (env.DISABLE_REMOTESEARCH) {
    COMMON_DISABLE_REMOTESEARCH = env.DISABLE_REMOTESEARCH
} else {
    COMMON_DISABLE_REMOTESEARCH = "false"
}

/* Disable DX component Content Composer */
if (env.DISABLE_CONTENTCOMPOSER) {
    COMMON_DISABLE_CONTENTCOMPOSER = env.DISABLE_CONTENTCOMPOSER
} else {
    COMMON_DISABLE_CONTENTCOMPOSER = "false"
}

/* Disable  DX component DAM */
if (env.DISABLE_DAM) {
    COMMON_DISABLE_DAM = env.DISABLE_DAM
} else {
    COMMON_DISABLE_DAM = "false"
}

/* Enable  Opensearch deployment */
echo "env.ENABLE_OPENSEARCH : ${env.ENABLE_OPENSEARCH}"
if (env.ENABLE_OPENSEARCH) {
    COMMON_DEPLOY_OPENSEARCH = env.ENABLE_OPENSEARCH
} else {
    COMMON_DEPLOY_OPENSEARCH = "false"
}

/* Opensearch version */
if (env.OPENSEARCH_VERSION) {
    COMMON_OPENSEARCH_VERSION = env.OPENSEARCH_VERSION
} else {
    COMMON_OPENSEARCH_VERSION = ""
}

/* Opensource Opensearch */
if (env.USE_OPENSOURCE_OPENSEARCH) {
    COMMON_USE_OPENSOURCE_OPENSEARCH = env.USE_OPENSOURCE_OPENSEARCH
} else {
    COMMON_USE_OPENSOURCE_OPENSEARCH = "false"
}

/* Disable  DX component DAM PLUGIN GOOGLE VISION */
if (env.DISABLE_PLUGIN_GOOGLE_VISION) {
    COMMON_DISABLE_PLUGIN_GOOGLE_VISION = env.DISABLE_PLUGIN_GOOGLE_VISION
} else {
    COMMON_DISABLE_PLUGIN_GOOGLE_VISION = "false"
}

/* Disable DAM NEW UI */
if (env.DISABLE_DAM_NEW_UI) {
    COMMON_DISABLE_DAM_NEW_UI = env.DISABLE_DAM_NEW_UI
} else {
    COMMON_DISABLE_DAM_NEW_UI = "false"
}

/* KALTURA API KEY */
if (env.DAM_KALTURA_PLUGIN_CREDENTIALS) {
    COMMON_DAM_KALTURA_PLUGIN_API_KEY = env.DAM_KALTURA_PLUGIN_CREDENTIALS
} else {
    COMMON_DAM_KALTURA_PLUGIN_API_KEY = ""
}

/* GOOGLE VISION API KEY */
if (env.DAM_PLUGIN_GOOGLE_VISION_CREDENTIALS) {
    COMMON_DAM_PLUGIN_GOOGLE_VISION_API_KEY = env.DAM_PLUGIN_GOOGLE_VISION_CREDENTIALS
} else {
    COMMON_DAM_PLUGIN_GOOGLE_VISION_API_KEY = ""
}

/* Check if AI is enabled*/
if (env.ENABLE_AI) {
    COMMON_ENABLE_AI = env.ENABLE_AI
} else {
    COMMON_ENABLE_AI = "false"
}

/* Check if IS_XAI_INTERNAL_AI */
if (env.IS_XAI_INTERNAL) {
    COMMON_IS_XAI_INTERNAL_AI = env.IS_XAI_INTERNAL
} else {
    COMMON_IS_XAI_INTERNAL_AI = "true"
}

/* Content AI Provider */
if (env.CONTENT_AI_PROVIDER) {
    COMMON_CONTENT_AI_PROVIDER = env.CONTENT_AI_PROVIDER
} else {
    COMMON_CONTENT_AI_PROVIDER = "OPEN_AI"
}

/* Custom Content AI Provider */
if (env.CUSTOM_AI_CLASSNAME) {
    COMMON_CUSTOM_AI_CLASSNAME = env.CUSTOM_AI_CLASSNAME
} else {
    COMMON_CUSTOM_AI_CLASSNAME = "com.ai.sample.CustomerAI"
}

/* AI API KEY for xai */
if (env.XAI_API_CREDENTIALS) {
    COMMON_XAI_API_KEY = env.XAI_API_CREDENTIALS
} else {
    COMMON_XAI_API_KEY = "AIzaSyBl0BJKT95mYGdl5HwubVQYnIDkUK23o6A"
}

/* AI API KEY for xai_internal */
if (env.XAI_INTERNAL_API_CREDENTIALS) {
    COMMON_XAI_INTERNAL_API_KEY = env.XAI_INTERNAL_API_CREDENTIALS
} else {
    COMMON_XAI_INTERNAL_API_KEY = "AIzaSyCoRqzdIJR87RKIFiTTlU5d12la9o-4AlE"
}

/* AI API KEY for chatgpt */
if (env.CHATGPT_API_CREDENTIALS) {
    COMMON_CHATGPT_API_KEY = env.CHATGPT_API_CREDENTIALS
} else {
    COMMON_CHATGPT_API_KEY = "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"
}

/* API KEY for custom AI Provider */
if (env.CUSTOM_AI_API_CREDENTIALS) {
    COMMON_CUSTOM_AI_API_CREDENTIALS = env.CUSTOM_AI_API_CREDENTIALS
} else {
    COMMON_CUSTOM_AI_API_CREDENTIALS = "AIzaSyCoRqzdIJR87RKIFiTTlU5d12la9o-4AlE"
}

/* Disable  DX component Ring API */
if (env.DISABLE_RINGAPI) {
    COMMON_DISABLE_RINGAPI = env.DISABLE_RINGAPI
} else {
    COMMON_DISABLE_RINGAPI = "false"
}

/* Disable Persistence */
if (env.DISABLE_PERSISTENCE) {
    COMMON_DISABLE_PERSISTENCE = env.DISABLE_PERSISTENCE
} else {
    COMMON_DISABLE_PERSISTENCE = "false"
}

/* Disable  DX component Image processor */
if (env.DISABLE_IMAGEPROCESSOR) {
    COMMON_DISABLE_IMAGEPROCESSOR = env.DISABLE_IMAGEPROCESSOR
} else {
    COMMON_DISABLE_IMAGEPROCESSOR = "false"
}

/* Disable  DX component kaltura pluign */
if (env.DISABLE_KALTURA_PLUGIN) {
    COMMON_DISABLE_KALTURA_PLUGIN = env.DISABLE_KALTURA_PLUGIN
} else {
    COMMON_DISABLE_KALTURA_PLUGIN = "false"
}

/* Disable HAProxy */
if (env.DISABLE_HAPROXY) {
    COMMON_DISABLE_HAPROXY = env.DISABLE_HAPROXY
} else {
    COMMON_DISABLE_HAPROXY = "false"
}

/* Disable License Manager */
if (env.DISABLE_LICENSE_MANAGER) {
    COMMON_DISABLE_LICENSE_MANAGER = env.DISABLE_LICENSE_MANAGER
} else {
    COMMON_DISABLE_LICENSE_MANAGER = "false"
}

/* Disable Logging Sidecar */ 
if (env.DISABLE_LOGGING_SIDECAR_DIFF) {
    COMMON_DISABLE_LOGGING_SIDECAR_DIFF = env.DISABLE_LOGGING_SIDECAR_DIFF
} else {
    COMMON_DISABLE_LOGGING_SIDECAR_DIFF = "false"
}

/* Disable Prereqs Checker */ 
if (env.DISABLE_PREREQS_CHECKER) {
    COMMON_DISABLE_PREREQS_CHECKER = env.DISABLE_PREREQS_CHECKER
} else {
    COMMON_DISABLE_PREREQS_CHECKER = "false"
}

/* Enable ingress */
if (env.ENABLE_INGRESS) {
    COMMON_ENABLE_INGRESS = env.ENABLE_INGRESS
} else {
    COMMON_ENABLE_INGRESS = "false"
}

/* Disable  DX component Runtime Controller */
if (env.DISABLE_RUNTIME_CONTROLLER) {
    COMMON_DISABLE_RUNTIME_CONTROLLER = env.DISABLE_RUNTIME_CONTROLLER
} else {
    COMMON_DISABLE_RUNTIME_CONTROLLER = "false"
}

/* Disable DX deployement and have plain kube cluster */
if (env.PLAIN_KUBERNETES) {
    PLAIN_KUBERNETES = env.PLAIN_KUBERNETES
} else {
    PLAIN_KUBERNETES = "false"
}

/* Enable external core database */
echo "env.ENABLE_DB_CONFIG : ${env.ENABLE_DB_CONFIG}"
if (env.ENABLE_DB_CONFIG) {
    COMMON_ENABLE_DB_CONFIG = env.ENABLE_DB_CONFIG
} else {
    COMMON_ENABLE_DB_CONFIG = "false"
}

/* DB type to use */
if (env.DB_TYPE) {
    COMMON_DB_TYPE = env.DB_TYPE
} else {
    COMMON_DB_TYPE = 'db2'
}

/* DB host to use */
if (env.DB_HOST) {
    COMMON_DB_HOST = env.DB_HOST
} else if(env.KUBE_FLAVOUR == "openshiftnjdc") {
    COMMON_DB_HOST = '10.190.75.30'
} else {
    COMMON_DB_HOST=""
}

/* Enable LDAP */
if (env.ENABLE_LDAP_CONFIG) {
    COMMON_ENABLE_LDAP_CONFIG = env.ENABLE_LDAP_CONFIG
} else {
    COMMON_ENABLE_LDAP_CONFIG = "false"
}

/* DX Core profile username to configure DB,LDAP  */
if (env.DX_USERNAME) {
    COMMON_DX_USERNAME = env.DX_USERNAME
} else {
    COMMON_DX_USERNAME = "wpsadmin"
}

/* DX Core profile password to configure DB,LDAP  */
if (env.DX_PASSWORD) {
    COMMON_DX_PASSWORD = env.DX_PASSWORD
} else {
    COMMON_DX_PASSWORD = "wpsadmin"
}

/* Custom WPSADMIN USERNAME  */
if (env.CUSTOM_WPSADMIN_USER) {
    COMMON_CUSTOM_WPSADMIN_USER = env.CUSTOM_WPSADMIN_USER
} else {
    COMMON_CUSTOM_WPSADMIN_USER = "wpsadmin"
}
/* Custom WPSADMIN PASSWORD  */
if (env.CUSTOM_WPSADMIN_PASSWORD) {
    COMMON_CUSTOM_WPSADMIN_PASSWORD = env.CUSTOM_WPSADMIN_PASSWORD
} else {
    COMMON_CUSTOM_WPSADMIN_PASSWORD = "wpsadmin"
}

/* LDAP Config HOST */
if (env.LDAP_CONFIG_HOST) {
    COMMON_LDAP_CONFIG_HOST = env.LDAP_CONFIG_HOST
} else {
    COMMON_LDAP_CONFIG_HOST = "dx-deployment-open-ldap"
}

/* LDAP Config PORT */
if (env.LDAP_CONFIG_PORT) {
    COMMON_LDAP_CONFIG_PORT = env.LDAP_CONFIG_PORT
} else {
    COMMON_LDAP_CONFIG_PORT = "1389"
}

/* LDAP Config Bind User Distinguished Name */
if (env.LDAP_CONFIG_BIND_DN) {
    COMMON_LDAP_CONFIG_BIND_DN = env.LDAP_CONFIG_BIND_DN
} else {
    COMMON_LDAP_CONFIG_BIND_DN = "cn=dx_user,dc=dx,dc=com"
}

/* LDAP Config Bind User Password */
if (env.LDAP_CONFIG_BIND_PASSWORD) {
    COMMON_LDAP_CONFIG_BIND_PASSWORD = env.LDAP_CONFIG_BIND_PASSWORD
} else {
    COMMON_LDAP_CONFIG_BIND_PASSWORD = "p0rtal4u"
}

/* LDAP Config LDAP Server Type */
if (env.LDAP_CONFIG_SERVER_TYPE) {
    COMMON_LDAP_CONFIG_SERVER_TYPE = env.LDAP_CONFIG_SERVER_TYPE
} else {
    COMMON_LDAP_CONFIG_SERVER_TYPE = "custom"
}

/* LDAP Config LDAP Base Entry */
if (env.LDAP_CONFIG_BASE_ENTRY) {
    COMMON_LDAP_CONFIG_BASE_ENTRY = env.LDAP_CONFIG_BASE_ENTRY
} else {
    COMMON_LDAP_CONFIG_BASE_ENTRY = "ou=users,dc=dx,dc=com"
}

/*
 * Determine the default user for tagging of our environments
 * This user will be handled as the owner, so environments are relatable
 */
COMMON_INSTANCE_OWNER = env.THIS_BUILD_OWNER
COMMON_INSTANCE_OWNER_SHORT = COMMON_INSTANCE_OWNER.split('@')[0]

/*
 * Default is native kubernetes, can be overwritten via a parameter
 */
COMMON_KUBE_FLAVOUR = "native"
if (env.KUBE_FLAVOUR) {
    // For openshift flavours, no matter if NJDC or AWS, we will force the use of the openshift module
    // Openshift config will use the env.KUBE_FLAVOUR variable itself to determine which OS target it is
    if (env.KUBE_FLAVOUR.startsWith('openshift')) {
        COMMON_KUBE_FLAVOUR = 'openshift'
    } else {
        COMMON_KUBE_FLAVOUR = env.KUBE_FLAVOUR
    }
    println("Kube flavour parameter has been set - using '${COMMON_KUBE_FLAVOUR}' (extracted from input parameter '${env.KUBE_FLAVOUR}')")
}

/* Determine if, when and where to automatically run the next Jenkins job related to this deployment (e.g. to delete the environment) */
if (env.NEXT_JOB_DELAY_HOURS) {
    /* This parameter must have a value that can be converted to a number */
    assert env.NEXT_JOB_DELAY_HOURS.isFloat() : "NEXT_JOB_DELAY_HOURS must be a number"
    COMMON_NEXT_JOB_DELAY_HOURS = env.NEXT_JOB_DELAY_HOURS as Float
} else {
    COMMON_NEXT_JOB_DELAY_HOURS = 0.0
}

if (env.NEXT_JOB_NAME) {
    COMMON_NEXT_JOB_NAME = env.NEXT_JOB_NAME
} else {
    COMMON_NEXT_JOB_NAME = ""
}

/* Determine if, when and where to automatically run the next Jenkins job related to this deployment (e.g. to delete the cluster) */
if (env.NEXT_CLUSTER_JOB_DELAY_HOURS) {
    /* This parameter must have a value that can be converted to a number */
    assert env.NEXT_CLUSTER_JOB_DELAY_HOURS.isFloat() : "NEXT_CLUSTER_JOB_DELAY_HOURS must be a number"
    COMMON_NEXT_CLUSTER_JOB_DELAY_HOURS = env.NEXT_CLUSTER_JOB_DELAY_HOURS as Float
} else {
    COMMON_NEXT_CLUSTER_JOB_DELAY_HOURS = 0.0
}

if (env.NEXT_CLUSTER_JOB_NAME) {
    COMMON_NEXT_CLUSTER_JOB_NAME = env.NEXT_CLUSTER_JOB_NAME
} else {
    COMMON_NEXT_CLUSTER_JOB_NAME = ""
}

if (env.JENKINS_API_CREDENTIALS_ID) {
    COMMON_JENKINS_API_CREDENTIALS_ID = env.JENKINS_API_CREDENTIALS_ID
} else {
    COMMON_JENKINS_API_CREDENTIALS_ID = "GrahamHarperJenkins"
}

/* Note that JENKINS_URL will likely be provided by the system, with a trailing slash */
if (env.JENKINS_URL) {
    COMMON_JENKINS_URL = env.JENKINS_URL
} else {
    COMMON_JENKINS_URL = "https://portal-jenkins-test.cwp.pnp-hcl.com/"
}

if (env.NEXT_JOB_PARAM_LIST) {
    /* This parameter should be a comma-separated list of parameter names */
    COMMON_NEXT_JOB_PARAM_LIST = env.NEXT_JOB_PARAM_LIST.split(',')
} else {
    COMMON_NEXT_JOB_PARAM_LIST = []
}

if (env.NEXT_CLUSTER_JOB_PARAM_LIST) {
    /* This parameter should be a comma-separated list of parameter names */
    COMMON_NEXT_CLUSTER_JOB_PARAM_LIST = env.NEXT_CLUSTER_JOB_PARAM_LIST.split(',')
} else {
    COMMON_NEXT_CLUSTER_JOB_PARAM_LIST = []
}

/* Hybrid-specific parameters */
if (env.HYBRID) {
    COMMON_HYBRID = env.HYBRID
} else {
    COMMON_HYBRID = "false"
}

if (env.HYBRID_HOST) {
    COMMON_HYBRID_HOST = env.HYBRID_HOST
} else {
    COMMON_HYBRID_HOST = ""
}

if (env.HYBRID_PORT) {
    COMMON_HYBRID_PORT = env.HYBRID_PORT
} else {
    COMMON_HYBRID_PORT = "443"
}

if (env.HYBRID_CORE_SSL) {
    COMMON_HYBRID_CORE_SSL_ENABLED = env.HYBRID_CORE_SSL
} else {
    COMMON_HYBRID_CORE_SSL_ENABLED = "true"
}

/* skip AcceptanceTests  */
if (env.SKIP_ACCEPTANCE_TESTS) {
    COMMON_SKIP_ACCEPTANCE_TESTS = env.SKIP_ACCEPTANCE_TESTS
} else {
    COMMON_SKIP_ACCEPTANCE_TESTS = "false"
}

/* skip data setup and verify  */
if (env.SKIP_DATA_SETUP_VERIFY) {
    COMMON_SKIP_DATA_SETUP_VERIFY = env.SKIP_DATA_SETUP_VERIFY
} else {
    COMMON_SKIP_DATA_SETUP_VERIFY = "true"
}

/*  data setup and verify  branch */
if (env.DATA_SETUP_VERIFY_BRANCH) {
    COMMON_DATA_SETUP_VERIFY_BRANCH = env.DATA_SETUP_VERIFY_BRANCH
} else {
    COMMON_DATA_SETUP_VERIFY_BRANCH = ""
}

/* enable RTRM Tests */
if (env.DAM_RTRM_TESTS) {
    COMMON_DAM_RTRM_TESTS = env.DAM_RTRM_TESTS
} else {
    COMMON_DAM_RTRM_TESTS = "false"
}

/* enable RTRM Performance Tests */
if (env.DAM_RTRM_PERFORMANCE) {
    COMMON_DAM_RTRM_PERFORMANCE = env.DAM_RTRM_PERFORMANCE
} else {
    COMMON_DAM_RTRM_PERFORMANCE= "false"
}

/* enable interruption on DAM Database schema migration */
if (env.ENABLE_INTERRUPT_MIGRATION) {
    COMMON_ENABLE_INTERRUPT_MIGRATION = env.ENABLE_INTERRUPT_MIGRATION
} else {
    COMMON_ENABLE_INTERRUPT_MIGRATION = "false"
}

/* AWS settings */
if (env.AWS_REGION) {
    COMMON_AWS_REGION = env.AWS_REGION
} else {
    COMMON_AWS_REGION = "us-east-1"
}

if (env.AWS_SUBNET) {
    COMMON_AWS_SUBNET = env.AWS_SUBNET
} else {
    COMMON_AWS_SUBNET = "subnet-09f521dfcea461588"
}

if (env.HOSTED_ZONE) {
    COMMON_HOSTED_ZONE = env.HOSTED_ZONE
} else {
    COMMON_HOSTED_ZONE = "Z2GWCKJIP1K8IK"
}

if (env.DOMAIN_SUFFIX) {
    COMMON_DOMAIN_SUFFIX = env.DOMAIN_SUFFIX
} else {
    COMMON_DOMAIN_SUFFIX = ".hcl-dx-dev.net"
}

if (env.VPC_SECURITY_GROUPS) {
    COMMON_VPC_SECURITY_GROUPS = env.VPC_SECURITY_GROUPS
} else {
    COMMON_VPC_SECURITY_GROUPS = '["sg-0ddaf1862f39be2be"]'
}

/* Artifactory certificate store URL */
if (env.ARTIFACTORY_TRUSTSTOREURL) {
    COMMON_ARTIFACTORY_TRUSTSTOREURL = env.ARTIFACTORY_TRUSTSTOREURL
} else {
    COMMON_ARTIFACTORY_TRUSTSTOREURL = "https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-truststore"
}

/* Tune core for authoring or rendering? (Authoring is default ) */
if (env.AUTHORING_ENVIRONMENT) {
    COMMON_AUTHORING_ENABLED = env.AUTHORING_ENVIRONMENT
} else {
    COMMON_AUTHORING_ENABLED = "true"
}

/* enable metrics scraping for all services (metrics enabled as default) */
if (env.ENABLE_METRICS) {
    COMMON_METRICS_ENABLED = env.ENABLE_METRICS
} else {
    COMMON_METRICS_ENABLED = "true"
}

/* Bucket to keep the DAM persistence backup */
if (env.BUCKET_PATH) {
    COMMON_BUCKET_PATH = env.BUCKET_PATH
} else {
    COMMON_BUCKET_PATH = "s3://dx-dam-persist-backup"
}
if(env.BACKUP_FILE_NAME){
    COMMON_BACKUP_FILE_NAME = env.BACKUP_FILE_NAME
}

/* Are we deploying to the WCM Perfomance Test environment */
if (env.WCM_PERFORMANCE) {
    COMMON_WCM_PERFORMANCE = env.WCM_PERFORMANCE
} else {
    COMMON_WCM_PERFORMANCE = 'false'
}

/* Determine the DB transfer file by using target environment */
if (env.DB2_TRANSFER_FILE) {
    COMMON_DB2_TRANSFER_FILE = env.DB2_TRANSFER_FILE
} else {
    // For njdc openshift we use a different set of transfer files
    // Since the commonConfig.COMMON_KUBE_FLAVOUR is openshift, but we need to check specifically for openshiftnjdc, we need to directly access the parameter to figure out if we are really using NJDC or not
    if (env.KUBE_FLAVOUR == 'openshiftnjdc') {
        // We have different transfer files for WCM performance testing
        if (COMMON_WCM_PERFORMANCE == 'true') {
            COMMON_DB2_TRANSFER_FILE = 'dx-db-transfer.njdc.perf.zip'
        } else {
            COMMON_DB2_TRANSFER_FILE = 'dx-db-transfer.njdc.zip'
        }
    } else {
        COMMON_DB2_TRANSFER_FILE = 'dx-db-transfer.zip'
    }
}

/* Master deployment level, by default it will be NA(not applicable) */
if (env.MASTER_DEPLOYMENT_LEVEL) {
    COMMON_MASTER_DEPLOYMENT_LEVEL = env.MASTER_DEPLOYMENT_LEVEL
} else {
    COMMON_MASTER_DEPLOYMENT_LEVEL = 'NA'
}

if (env.DAM_RTRM_MIGRATION_ACCEPTANCE_TEST_JOB) {
    DAM_RTRM_MIGRATION_ACCEPTANCE_TEST_JOB = env.DAM_RTRM_MIGRATION_ACCEPTANCE_TEST_JOB
} else {
    DAM_RTRM_MIGRATION_ACCEPTANCE_TEST_JOB = "/CI/DAM_RTRM_Automation_Tests/DAM_Migration_Tests"
}

if (env.DAM_RTRM_JMETER_TEST_JOB) {
    DAM_RTRM_JMETER_TEST_JOB = env.DAM_RTRM_JMETER_TEST_JOB
} else {
    DAM_RTRM_JMETER_TEST_JOB = "/CI/DAM_RTRM_Automation_Tests/DAM_JMeter_Tests"
}

if (env.ACCEPTANCE_TEST_JOB) {
    ACCEPTANCE_TEST_JOB = env.ACCEPTANCE_TEST_JOB
} else {
    ACCEPTANCE_TEST_JOB = "/CI/Automated_Tests/Acceptance_Tests"
}

if (env.DATA_SETUP_JOB) {
    DATA_SETUP_JOB = env.DATA_SETUP_JOB
} else {
    DATA_SETUP_JOB = "/CI/Automated_Tests/DataSetup"
}

if (env.DATA_VERIFY_JOB) {
    DATA_VERIFY_JOB = env.DATA_VERIFY_JOB
} else {
    DATA_VERIFY_JOB = "/CI/Automated_Tests/DataVerify"
}

if (env.DEPLOY_DX) {
    COMMON_DEPLOY_DX = env.DEPLOY_DX
} else {
    COMMON_DEPLOY_DX = "true"
}

if (env.DEPLOY_LEAP) {
    COMMON_DEPLOY_LEAP = env.DEPLOY_LEAP
} else {
    COMMON_DEPLOY_LEAP = "false"
}

if (env.DEPLOY_LPC) {
    COMMON_DEPLOY_LPC = env.DEPLOY_LPC
} else {
    COMMON_DEPLOY_LPC = "false"
}

if (env.DEPLOY_LPC_TEST_APPS) {
    COMMON_DEPLOY_LPC_TEST_APPS = env.DEPLOY_LPC_TEST_APPS
} else {
    COMMON_DEPLOY_LPC_TEST_APPS = "false"
}

if (env.DEPLOY_PEOPLESERVICE) {
    COMMON_DEPLOY_PEOPLESERVICE = env.DEPLOY_PEOPLESERVICE
} else {
    COMMON_DEPLOY_PEOPLESERVICE = "false"
}

if (env.DEPLOY_KEYCLOAK) {
    COMMON_DEPLOY_KEYCLOAK = env.DEPLOY_KEYCLOAK
} else {
    COMMON_DEPLOY_KEYCLOAK = "false"
}

/* HCL Leap */
if (env.LEAP_IMAGE_FILTER) {
    COMMON_LEAP_IMAGE_FILTER = env.LEAP_IMAGE_FILTER
} else {
    COMMON_LEAP_IMAGE_FILTER = "develop"
}

if (env.LEAP_IMAGE_PATH) {
    COMMON_LEAP_IMAGE_PATH = env.LEAP_IMAGE_PATH
} else {
    COMMON_LEAP_IMAGE_PATH = "dx-build-output/leap"
}

/* HCL DS Keycloak */
if (env.HCLDS_KEYCLOAK_IMAGE_FILTER) {
    COMMON_HCLDS_KEYCLOAK_IMAGE_FILTER = env.HCLDS_KEYCLOAK_IMAGE_FILTER
} else {
    COMMON_HCLDS_KEYCLOAK_IMAGE_FILTER = "hclds-keycloak-22.0.1_"
}

if (env.HCLDS_KEYCLOAK_IMAGE_PATH) {
    COMMON_HCLDS_KEYCLOAK_IMAGE_PATH = env.HCLDS_KEYCLOAK_IMAGE_PATH
} else {
    COMMON_HCLDS_KEYCLOAK_IMAGE_PATH = "dx-build-output/hclds-keycloak"
}

/* HCL People Service */
if (env.PEOPLE_SERVICE_IMAGE_FILTER) {
    COMMON_PEOPLE_SERVICE_IMAGE_FILTER = env.PEOPLE_SERVICE_IMAGE_FILTER
} else {
    COMMON_PEOPLE_SERVICE_IMAGE_FILTER = "develop"
}

if (env.PEOPLE_SERVICE_IMAGE_PATH) {
    COMMON_PEOPLE_SERVICE_IMAGE_PATH = env.PEOPLE_SERVICE_IMAGE_PATH
} else {
    COMMON_PEOPLE_SERVICE_IMAGE_PATH = "dx-build-output/people-service/people-service"
}

/* Liberty Portlet Container */
if (env.LPC_IMAGE_FILTER) {
    COMMON_LPC_IMAGE_FILTER = env.LPC_IMAGE_FILTER
} else {
    COMMON_LPC_IMAGE_FILTER = "develop"
}

if (env.LPC_IMAGE_PATH) {
    COMMON_LPC_IMAGE_PATH = env.LPC_IMAGE_PATH
} else {
    COMMON_LPC_IMAGE_PATH = "dx-build-output/portlet-container-liberty/hcldx-portlet-container-liberty"
}

/* Opensearch */
if (env.OPENSEARCH_IMAGE_FILTER) {
    COMMON_OPENSEARCH_IMAGE_FILTER = env.OPENSEARCH_IMAGE_FILTER
} else {
    COMMON_OPENSEARCH_IMAGE_FILTER = "develop"
}

if (env.OPENSEARCH_IMAGE_PATH) {
    COMMON_OPENSEARCH_IMAGE_PATH = env.OPENSEARCH_IMAGE_PATH
} else {
    COMMON_OPENSEARCH_IMAGE_PATH = "dx-build-output/search/dx-opensearch"
}

/* Opensearch Search Admin  Custom credentials*/
if (env.CUSTOM_SEARCH_ADMIN_USER) {
    COMMON_CUSTOM_SEARCH_ADMIN_USER = env.CUSTOM_SEARCH_ADMIN_USER
} else {
    COMMON_CUSTOM_SEARCH_ADMIN_USER = "searchadmin"
}

if (env.CUSTOM_SEARCH_ADMIN_PASSWORD) {
    COMMON_CUSTOM_SEARCH_ADMIN_PASSWORD = env.CUSTOM_SEARCH_ADMIN_PASSWORD
} else {
    COMMON_CUSTOM_SEARCH_ADMIN_PASSWORD = "adminsearch"
}

/* Opensearch Push Admin  Custom credentials*/
if (env.CUSTOM_SEARCH_PUSH_USER) {
    COMMON_CUSTOM_SEARCH_PUSH_USER = env.CUSTOM_SEARCH_PUSH_USER
} else {
    COMMON_CUSTOM_SEARCH_PUSH_USER = "pushadmin"
}

if (env.CUSTOM_SEARCH_PUSH_PASSWORD) {
    COMMON_CUSTOM_SEARCH_PUSH_PASSWORD = env.CUSTOM_SEARCH_PUSH_PASSWORD
} else {
    COMMON_CUSTOM_SEARCH_PUSH_PASSWORD = "adminpush"
}
/* Configure content sources for WCM with the new search */
if (env.CONFIGURE_OPENSEARCH) {
    COMMON_CONFIGURE_OPENSEARCH = env.CONFIGURE_OPENSEARCH
} else {
    COMMON_CONFIGURE_OPENSEARCH = "false"
}

/* Search Middleware */
if (env.SEARCH_MIDDLEWARE_IMAGE_FILTER) {
    COMMON_SEARCH_MIDDLEWARE_IMAGE_FILTER = env.SEARCH_MIDDLEWARE_IMAGE_FILTER
} else {
    COMMON_SEARCH_MIDDLEWARE_IMAGE_FILTER = "develop"
}

if (env.SEARCH_MIDDLEWARE_IMAGE_PATH) {
    COMMON_SEARCH_MIDDLEWARE_IMAGE_PATH = env.SEARCH_MIDDLEWARE_IMAGE_PATH
} else {
    COMMON_SEARCH_MIDDLEWARE_IMAGE_PATH = "dx-build-output/search/dx-search-middleware"
}

/* enable metrics monitoring using prometheus and grafana*/
if (env.ENABLE_METRICS_MONITORING) {
    COMMON_METRICS_MONITORING_ENABLED = env.ENABLE_METRICS_MONITORING
} else {
    if (env.KUBE_FLAVOUR == 'openshift') {
        // Disable monitoring since ROSA already has its own
        COMMON_METRICS_MONITORING_ENABLED = "false"
    } else {
        COMMON_METRICS_MONITORING_ENABLED = "true"
    }
}

/* Content Reporting (Content Reporting enable by default) */
if (env.ENABLE_CONTENT_REPORTING) {
    COMMON_ENABLE_CONTENT_REPORTING = env.ENABLE_CONTENT_REPORTING
} else {
    COMMON_ENABLE_CONTENT_REPORTING = "true"
}

/* DX Picker (DX Picker enable by default) */
if (env.ENABLE_DX_PICKER) {
    COMMON_ENABLE_DX_PICKER = env.ENABLE_DX_PICKER
} else {
    COMMON_ENABLE_DX_PICKER = "true"
}

/* DX Picker (DX Picker enable by default) */
if (env.ENABLE_PRESENTATION_DESIGNER) {
    COMMON_ENABLE_PRESENTATION_DESIGNER = env.ENABLE_PRESENTATION_DESIGNER
} else {
    COMMON_ENABLE_PRESENTATION_DESIGNER = "false"
}

if (env.CUSTOM_VALUES_OVERRIDE) {
    COMMON_CUSTOM_VALUES_OVERRIDE = env.CUSTOM_VALUES_OVERRIDE
} else {
    COMMON_CUSTOM_VALUES_OVERRIDE = ''
}

/* Enable option to add users to OpenLDAP */
if (env.ENABLE_OPENLDAP_SETUP) {
    COMMON_ENABLE_OPENLDAP_SET_UP = env.ENABLE_OPENLDAP_SETUP
} else {
    COMMON_ENABLE_OPENLDAP_SET_UP = "false"
}

/* No of users to add in OpenLDAP */
if (env.USERS_COUNT_OPENLDAP) {
    COMMON_USERS_OPENLDAP = env.USERS_COUNT_OPENLDAP
} else {
    COMMON_USERS_OPENLDAP = "10"
}

/* Enable OIDC configuration */
if (env.ENABLE_OIDC_CONFIGURATION) {
    COMMON_ENABLE_OIDC_CONFIGURATION = env.ENABLE_OIDC_CONFIGURATION
} else {
    COMMON_ENABLE_OIDC_CONFIGURATION = "false"
}

/* Enable Distributed Environment */
if (env.ENABLE_DISTRIBUTED_CONFIGURATION) {
    ENABLE_DISTRIBUTED_CONFIGURATION = env.ENABLE_DISTRIBUTED_CONFIGURATION
} else {
    ENABLE_DISTRIBUTED_CONFIGURATION = "false"
}

/* Enable Distributed Environment */
if (env.DISTRIBUTED_NODE_COUNT) {
    DISTRIBUTED_NODE_COUNT = env.DISTRIBUTED_NODE_COUNT
} else {
    DISTRIBUTED_NODE_COUNT = "1"
}

/* Set the Logstash version for the installation */
if (env.LOGSTASH_VERSION) {
    LOGSTASH_VERSION = env.LOGSTASH_VERSION 
} else {
    LOGSTASH_VERSION = "8.9.0"
}

/* Set the opensearch server host */
if (env.OS_HOSTNAME) {
    OS_HOSTNAME = env.OS_HOSTNAME 
} else {
    OS_HOSTNAME = "search01.cwp.pnp-hcl.com"
}

/* Set the opensearch server protocol */
if (env.OS_PROTOCOL) {
    OS_PROTOCOL = env.OS_PROTOCOL
} else {
    OS_PROTOCOL = "https"
}

/* Set the opensearch server index name */
if (env.OS_INDEX_NAME) {
    OS_INDEX_NAME = env.OS_INDEX_NAME 
} else {
    OS_INDEX_NAME = "dx-logstash-logs"
}

/* Set the opensearch server username */
if (env.OS_USERNAME) {
    OS_USERNAME = env.OS_USERNAME  
} else {
    OS_USERNAME = "dx.internal"
}

/* Enable Logstash SetUp */
if (env.ENABLE_LOGSTASH_SETUP) {
    ENABLE_LOGSTASH_SETUP = env.ENABLE_LOGSTASH_SETUP
} else {
   ENABLE_LOGSTASH_SETUP = "false"
}

/* Enable pen test configuration */
if (env.PEN_TEST_CONFIG_ENABLED) {
    COMMON_PEN_TEST_CONFIG_ENABLED = env.PEN_TEST_CONFIG_ENABLED
} else {
    COMMON_PEN_TEST_CONFIG_ENABLED = false
}
echo "Pen test config enabled : ${COMMON_PEN_TEST_CONFIG_ENABLED}"
if (COMMON_PEN_TEST_CONFIG_ENABLED == 'true') {
    COMMON_DX_PASSWORD = "P3nTest4!Tunate"
    COMMON_CUSTOM_WPSADMIN_PASSWORD = "P3nTest4!Tunate"
}

/* 
Replace the credentials value in helm chart. Use the credentials from the secret instead of pre-defined value.
*/
CORE_WAS_CUSTOM_SECRET="custom-credentials-core-was-admin"
CORE_WPS_CUSTOM_SECRET="custom-credentials-core-wps-admin"
CONFIG_WIZARD_CUSTOM_SECRET="custom-credentials-config-wizard-admin"
CORE_AI_CUSTOM_SECRET="custom-credentials-core-ai-secret"
RS_WAS_CUSTOM_SECRET="custom-credentials-remote-search-was-admin"
DAM_DB_CUSTOM_SECRET="custom-credentials-dam-db"
DAM_REPLICATION_CUSTOM_SECRET="custom-credentials-dam-replication"
DAM_CUSTOM_SECRET="custom-credentials-dam"
IMAGE_PROCESSOR_CUSTOM_SECRET="custom-credentials-image-processor"
DAM_KULTURA_CUSTOM_SECRET="custom-credentials-dam-kultura"
PERSISTENT_CONNECTION_POOL_CUSTOM_SECRET="custom-credentials-persistent-connection-pool"
DAM_GOOGLE_VISION_CUSTOM_SECRET="custom-credentials-dam-google-vision"
LICENSE_MANAGER_CUSTOM_SECRET="custom-credentials-license-manager"
OPEN_LDAP_CUSTOM_SECRET="custom-credentials-open-ldap"
CORE_LDAP_CUSTOM_SECRET="custom-credentials-core-ldap"
CORE_LTPA_CUSTOM_SECRET="custom-credentials-core-ltpa"
SEARCH_ADMIN_CUSTOM_SECRET="custom-credentials-search-admin-search"
SEARCH_PUSH_CUSTOM_SECRET="custom-credentials-search-admin-push"

/* Mandatory return statement on EOF */
return this
