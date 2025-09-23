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

 /*
 * AKS configuration file used to provide specific
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
    RS_STORAGECLASS = "managed-premium"
}

/* DAM persistent volume  default will create new persistent volume  */
DAM_PV_NAME=env.DAM_PV_NAME

/* CORE persistent volume  default will create new persistent volume  */
CORE_PV_NAME=env.CORE_PV_NAME

/* Remote Search persistent volume  default will create new persistent volume  */
RS_PV_NAME=env.RS_PV_NAME

/* NFS server */
if(env.NFS_SERVER) {
    NFS_SERVER=env.NFS_SERVER
} else {
    NFS_SERVER='azureuser@4.236.156.251'
}

/* CLUSTER NAME */
if (env.CLUSTER_NAME) {
    CLUSTER_NAME = env.CLUSTER_NAME
} else {
    CLUSTER_NAME = "dx-automation-cluster"
}

/* RESOURCE GROP */
if (env.RESOURCE_GROP) {
    RESOURCE_GROP = env.RESOURCE_GROP
} else {
    RESOURCE_GROP = "MC_HCLSW_QNTNA_AKS_RG_AZR_QNTNA_eastus"
}

/* AZURE SUBSCRIPTION */
if (env.SUBSCRIPTION) {
    SUBSCRIPTION = env.SUBSCRIPTION
} else {
    SUBSCRIPTION = "1ed2ec66-0820-4445-aa5d-2be8402c1bb6"
}

/* AZURE TENANT */
if (env.TENANT) {
    TENANT = env.TENANT
} else {
    TENANT = "dc207805-8a5c-4370-a00a-2573016a71a7"
}

/* Mandatory return statement on EOF */
return this
