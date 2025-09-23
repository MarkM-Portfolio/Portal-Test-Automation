/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2020, 2022. All Rights Reserved. *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

 /*
 * Openshift configuration file used to provide specific
 * configuration parameters and defaults
 */

/* Determine if NJDC or NOT */
OS_TARGET_ENVIRONMENT = 'openshift'
// This will only be run if we are in openshift, so taking over that parameter is safe
// This can either be openshift or openshiftnjdc
if (env.KUBE_FLAVOUR) {
    println("Kube flavour parameter has been set - using '${env.KUBE_FLAVOUR}'")
    OS_TARGET_ENVIRONMENT = env.KUBE_FLAVOUR
}


/* openshift server url */
if (env.OC_SERVER_URL) {
    OC_SERVER_URL = env.OC_SERVER_URL
} else {
    // Determine if AWS or NJDC openshift is in use, since they are different target servers for kubectl/oc
    switch (OS_TARGET_ENVIRONMENT) {
        case 'openshiftnjdc':
            OC_SERVER_URL = 'https://api.quocp.nonprod.hclpnp.com:6443'
            break
        case 'openshift':
            OC_SERVER_URL = 'https://api.dx-cluster-dev.hcl-dx-dev.net:6443'
            break
        default:
            // Don't assume a default, if it does not match our expectations, we break!
            error('No valid OS_TARGET_ENVIRONMENT has been provided!')
    }
}

/* openshift clint url */
if (env.OC_CLIENT_URL) {
    OC_CLIENT_URL = env.OC_CLIENT_URL
} else {
    OC_CLIENT_URL = 'https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/openshift-origin-client-tools/openshift-origin-client-tools-v3.11.0-0cbc58b-linux-64bit.tar.gz'
}


/* ID of credentials (in Jenkins store) with which to perform openshift operations */
if (env.OPENSHIFT_CREDENTIALS_ID) {
    OPENSHIFT_CREDENTIALS_ID = env.OPENSHIFT_CREDENTIALS_ID
} else {
    // Determine if AWS or NJDC openshift is in use
    switch (OS_TARGET_ENVIRONMENT) {
        case 'openshiftnjdc':
            OPENSHIFT_CREDENTIALS_ID = 'njdc_openshift_credentials'
            break
        case 'openshift':
            OPENSHIFT_CREDENTIALS_ID = 'openshift_rosa_credentials'
            break
        default:
            // Don't assume a default, if it does not match our expectations, we break!
            error('No valid OS_TARGET_ENVIRONMENT has been provided!')
    }
}

/* openshift CORE Storage class */
if (env.CORE_STORAGECLASS) {
    CORE_STORAGECLASS = env.CORE_STORAGECLASS
} else {
    // The core storageclass is the same for NJDC and AWS
    CORE_STORAGECLASS = 'dx-deploy-stg'
}

/* openshift DAM Storage class */
if (env.DAM_STORAGECLASS) {
    DAM_STORAGECLASS = env.DAM_STORAGECLASS
} else {
    // The DAM storageclass is the same for NJDC and AWS
    DAM_STORAGECLASS = 'dx-deploy-stg'
}

/* remote search Storage class */
if (env.RS_STORAGECLASS) {
    RS_STORAGECLASS = env.RS_STORAGECLASS
} else {
    // Determine if AWS or NJDC openshift is in use, since they have a different storageClass for RWO
    switch (OS_TARGET_ENVIRONMENT) {
        case 'openshiftnjdc':
            RS_STORAGECLASS = 'thin'
            break
        case 'openshift':
            RS_STORAGECLASS = 'gp3'
            break
        default:
            // Don't assume a default, if it does not match our expectations, we break!
            error('No valid OS_TARGET_ENVIRONMENT has been provided!')
    }
}

/* default Storage class */
if (env.DEFAULT_STORAGECLASS) {
    DEFAULT_STORAGECLASS = env.DEFAULT_STORAGECLASS
} else {
    // Determine if AWS or NJDC openshift is in use, since they have a different storageClass for RWO
    switch (OS_TARGET_ENVIRONMENT) {
        case 'openshiftnjdc':
            DEFAULT_STORAGECLASS = 'thin'
            break
        case 'openshift':
            DEFAULT_STORAGECLASS = 'gp3'
            break
        default:
            // Don't assume a default, if it does not match our expectations, we break!
            error('No valid OS_TARGET_ENVIRONMENT has been provided!')
    }
}

/* DAM persistent volume  default will be create new persistent volume  */
DAM_PV_NAME=env.DAM_PV_NAME

/* CORE persistent volume  default will be create new persistent volume  */
CORE_PV_NAME=env.CORE_PV_NAME

/* NFS server */
if (env.NFS_SERVER) {
    NFS_SERVER = env.NFS_SERVER
} else {
    // Determine if AWS or NJDC openshift is in use, since they have a different NFS Server for RWX
    switch (OS_TARGET_ENVIRONMENT) {
        case 'openshiftnjdc':
            NFS_SERVER = 'mary.dooley@10.190.75.13'
            break
        case 'openshift':
            NFS_SERVER = 'ubuntu@18.218.47.60'
            break
        default:
            // Don't assume a default, if it does not match our expectations, we break!
            error('No valid OS_TARGET_ENVIRONMENT has been provided!')
    }
}

/* NFS Directory path */
if (env.NFS_PATH) {
    NFS_PATH = env.NFS_PATH
} else {
    // Determine if AWS or NJDC openshift is in use, since they have a different NFS Server for RWX
    switch (OS_TARGET_ENVIRONMENT) {
        case 'openshiftnjdc':
            NFS_PATH = '/ocpqu'
            break
        case 'openshift':
            NFS_PATH = '/nfs/jenkinsnfs'
            break
        default:
            // Don't assume a default, if it does not match our expectations, we break!
            error('No valid OS_TARGET_ENVIRONMENT has been provided!')
    }
}

/* NFS access key for PV management */
if (env.NFS_ACCESS_KEY) {
    NFS_ACCESS_KEY = env.NFS_ACCESS_KEY
} else {
    // Determine if AWS or NJDC openshift is in use, since they have a different NFS Server for RWX
    switch (OS_TARGET_ENVIRONMENT) {
        case 'openshiftnjdc':
            NFS_ACCESS_KEY = 'njdc_nfs_access_key'
            break
        case 'openshift':
            NFS_ACCESS_KEY = 'openshift_rosa_nfs'
            break
        default:
            // Don't assume a default, if it does not match our expectations, we break!
            error('No valid OS_TARGET_ENVIRONMENT has been provided!')
    }
}

/* Openshift applications domain */
if (env.DOMAIN) {
    DOMAIN = env.DOMAIN
} else {
    // Determine if AWS or NJDC openshift is in use, since they have a different DNS Zone
    switch (OS_TARGET_ENVIRONMENT) {
        case 'openshiftnjdc':
            DOMAIN = 'apps.quocp.nonprod.hclpnp.com'
            break
        case 'openshift':
            DOMAIN = 'apps.dx-cluster-dev.hcl-dx-dev.net'
            break
        default:
            // Don't assume a default, if it does not match our expectations, we break!
            error('No valid OS_TARGET_ENVIRONMENT has been provided!')
    }
}

/* Openshift Namespace length */
OS_NAMESPACE_LENGTH = 20

/* Mandatory return statement on EOF */
return this
