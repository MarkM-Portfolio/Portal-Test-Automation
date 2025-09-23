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

/*
 * Image paths used to lookup the images from the corresponding repository
 * Overridable, having a default matching the new build output paths
 */

COMMON_IMAGE_AREA = "https://artifactory.cwp.pnp-hcl.com/artifactory/list/quintana-docker-prod"

/* HCL DX Core */
COMMON_CORE_IMAGE_PATH = "dxen"

/* Digital Asset Manager */
COMMON_DAM_IMAGE_PATH = "portal/media-library"


/* Content Composer */
COMMON_CC_IMAGE_PATH = "portal/content-ui"

/* DAM Plugin Google Vision */
COMMON_DAM_PLUGIN_GOOGLE_VISION_IMAGE_PATH = "portal/api/dam-plugin-google-vision"

/* Ring-API */
COMMON_RINGAPI_IMAGE_PATH = "portal/api/ringapi"


/* Image processor */
COMMON_IMGPROC_IMAGE_PATH = "portal/image-processor"


/* PostgreSQL */
COMMON_PERSISTENCE_IMAGE_PATH = "portal/persistence/postgres"


/* HCL DX Core Operator */
COMMON_CORE_OPERATOR_IMAGE_PATH = "hcldx-cloud-operator"




/* HCL DAM Operator */
COMMON_DAM_OPERATOR_IMAGE_PATH = "hcl-medialibrary-operator"


/* Those images are special, they may need to be handled differently, because they are externally build */

/* Ambassador */
COMMON_AMBASSADOR_IMAGE_PATH = "ambassador"


/* Ambassador REDIS */
COMMON_REDIS_IMAGE_PATH = "redis"


/* Additional images of componentens that are optional for deployment */

/* Open LDAP */
COMMON_LDAP_IMAGE_PATH = "dx-openldap"

/* Remote Search */
COMMON_RS_IMAGE_PATH = "dx-build-output/core/dxrs"

ACCEPTANCE_TEST_JOB = "CI/Automated_Tests/Acceptance_Tests"

/* Constant parameters for autonomous jobs */
COMMON_CLUSTERED_ENV = "true"
COMMON_ADDITIONAL_NODE = "true"
COMMON_ENABLE_RS = "true"
COMMON_DB2_TYPE = "db2"

COMMON_ENABLE_LDAP = "true"

COMMON_ENABLE_DB_CONFIG = "true"

COMMON_PUSH_IMAGES = "true"

COMMON_SKIP_ACCEPTANCE_TESTS = "true"
COMMON_IS_SCHEDULED = "true"
COMMON_SKIP_DATA_SETUP_VERIFY = "true"
COMMON_DATA_SETUP_VERIFY_BRANCH = ""

/* DAM RTRM tests */
COMMON_DAM_RTRM_TESTS = "true"

/* DBHA persistence path */
COMMON_PERSISTENCE_CONNECTION_POOL_IMAGE_PATH = "portal/persistence/pgpool"
COMMON_PERSISTENCE_NODE_IMAGE_PATH = "portal/persistence/postgresrepmgr"

COMMON_PERSISTENCE_METRICS_EXPORTER_IMAGE_PATH = "dx-build-output/core-addon/persistence/postgres-metrics-exporter"

/* Design studio */
COMMON_DESIGN_STUDIO_IMAGE_PATH = "dx-build-output/core-addon/site-manager"

/* Runtime Controller */
COMMON_RUNTIME_CONTROLLER_IMAGE_PATH = "dx-build-output/operator/hcldx-runtime-controller"

/* HAProxy Image */
COMMON_HAPROXY_IMAGE_PATH = "dx-build-output/common/haproxy"

/* Sidecar Image */
COMMON_SIDECAR_IMAGE_PATH = "dx-build-output/common/dxubi"
// Helm chart default path
COMMON_HELM_CHARTS_PATH = "hcl-dx-deployment"

COMMON_HELM_CHARTS_AREA = "quintana-helm"

/* Logging Sidecar */
COMMON_LOGGING_SIDECAR_IMAGE_PATH = "dx-build-output/common/logging-sidecar"

/* Prereqs Checker */
COMMON_PREREQS_CHECKER_IMAGE_PATH = "dx-build-output/common/prereqs-checker"

/* DAM Kaltura Plugin */
COMMON_DAM_KALTURA_PLUGIN_IMAGE_PATH = "portal/api/dam-plugin-kaltura"

/* Mandatory return statement on EOF */
return this
