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
 * AWS configuration file used to provide specific
 * configuration parameters and defaults
 */


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
    RS_STORAGECLASS = "gp2"
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
    CLUSTER_NAME = "eks-cluster-dx01"
}

/* CLUSTER REGION */
if (env.CLUSTER_REGION) {
    CLUSTER_REGION = env.CLUSTER_REGION
} else {
    CLUSTER_REGION = "us-east-1"
}

/* NFS server */
if (env.NFS_SERVER) {
    NFS_SERVER = env.NFS_SERVER
} else {
    NFS_SERVER = "centos@dx-automation-nfs.team-q-dev.com"
}

/* NFS access key */
if (env.NFS_ACCESS_KEY) {
    NFS_ACCESS_KEY = env.NFS_ACCESS_KEY
} else {
    NFS_ACCESS_KEY = "test-automation-deployments"
}

/* Mandatory return statement on EOF */
return this
