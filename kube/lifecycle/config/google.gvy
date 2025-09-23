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
 * GKE configuration file used to provide specific
 * configuration parameters and defaults
 */

/* ID of credentials (in Jenkins store) with which to perform GKE operations */
if (env.GOOGLE_SERVICE_ACCOUNT) {
    GOOGLE_SERVICE_ACCOUNT = env.GOOGLE_SERVICE_ACCOUNT
} else {
    GOOGLE_SERVICE_ACCOUNT = "gke_credentials"
}

/*CORE Storage class */
if (env.CORE_STORAGECLASS) {
    CORE_STORAGECLASS = env.CORE_STORAGECLASS
} else {
    CORE_STORAGECLASS = "dx-deploy-stg"
}

/* DAM Storage class */
if (env.DAM_STORAGECLASS) {
    DAM_STORAGECLASS = env.DAM_STORAGECLASS
} else {
    DAM_STORAGECLASS = "dx-deploy-stg"
}

/* remote search Storage class */
if (env.RS_STORAGECLASS) {
    RS_STORAGECLASS = env.RS_STORAGECLASS
} else {
    RS_STORAGECLASS = "standard"
}

/* DAM persistent volume  default will be create new persistent volume  */
DAM_PV_NAME=env.DAM_PV_NAME

/* CORE persistent volume  default will be create new persistent volume  */
CORE_PV_NAME=env.CORE_PV_NAME

/* Remote Search persistent volume  default will be create new persistent volume  */
RS_PV_NAME=env.RS_PV_NAME

/* NFS server */
NFS_SERVER=env.NFS_SERVER

/* CLUSTER NAME */
if (env.CLUSTER_NAME) {
    CLUSTER_NAME = env.CLUSTER_NAME
} else {
    CLUSTER_NAME = "dx-jenkins-cluster"
}

/* CLUSTER REGION */
if (env.CLUSTER_REGION) {
    CLUSTER_REGION = env.CLUSTER_REGION
} else {
    CLUSTER_REGION = "us-east4"
}

/* Mandatory return statement on EOF */
return this
