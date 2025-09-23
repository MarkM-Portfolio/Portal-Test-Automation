/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2001, 2022. All Rights Reserved. *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

/*
 * Native kubernetes configuration file used to provide specific
 * configuration parameters and defaults
 */

/* ID of credentials (in Jenkins store) with which to perform AWS operations */
if (env.AWS_CREDENTIALS_ID) {
    AWS_CREDENTIALS_ID = env.AWS_CREDENTIALS_ID
} else {
    AWS_CREDENTIALS_ID = "aws_credentials"
}

/* EC2 instance type to create */
if (env.INSTANCE_TYPE) {
    INSTANCE_TYPE = env.INSTANCE_TYPE
} else {
    INSTANCE_TYPE = "c5.4xlarge"
}

/* Name of EC2 instance to create */
if (env.INSTANCE_NAME) {
    INSTANCE_NAME = env.INSTANCE_NAME
} else {
    INSTANCE_NAME = "native-kube"
}

/* DB2 details if performing DB transfer */
if (env.DB2_PASSWORD) {
    DB2_PASSWORD = env.DB2_PASSWORD
} else {
    DB2_PASSWORD = "diet4coke"
}

if (env.DB2_IMAGE_PATH) {
    DB2_IMAGE_PATH = env.DB2_IMAGE_PATH
} else {
    DB2_IMAGE_PATH = "dx-db2"
}

if (env.DB2_IMAGE_TAG) {
    DB2_IMAGE_TAG = env.DB2_IMAGE_TAG
} else {
    DB2_IMAGE_TAG = "v11.5"
}

/* Subnet to which to deploy EC2 instance */
if (env.AWS_SUBNET_NAME) {
    AWS_SUBNET_NAME = env.AWS_SUBNET_NAME
} else {
    AWS_SUBNET_NAME = "Build01"
}

if (env.AWS_SUBNET_ID) {
    AWS_SUBNET_ID = env.AWS_SUBNET_ID
} else {
    switch (AWS_SUBNET_NAME) {
        case "Dev01":
            AWS_SUBNET_ID = "subnet-02a350a23b3e39a43"
            break
        case "Dev02":
            AWS_SUBNET_ID = "subnet-033035ecf3a0e7ff4"
            break
        case "Build02":
            AWS_SUBNET_ID = "subnet-00153ed57f803609e"
            break
        case "Build01":
            AWS_SUBNET_ID = "subnet-014047f30974086c8"
            break
        case "Support01":
            AWS_SUBNET_ID = "subnet-0d8701e99946ea834"
            break
        case "Support02":
            AWS_SUBNET_ID = "subnet-07df4340bd57769e3"
            break
        default:
            AWS_SUBNET_ID = "subnet-014047f30974086c8"
            break
    }
}

/* Set job name for native-kube removal*/
if (env.NATIVE_KUBE_REMOVAL_JOB) {
    NATIVE_KUBE_REMOVAL_JOB = env.NATIVE_KUBE_REMOVAL_JOB
} else {
    NATIVE_KUBE_REMOVAL_JOB = "native-kube-remove"
}

/* Set IS NJDC Deployment parameter */
if (env.IS_NJDC_DEPLOYMENT) {
    IS_NJDC_DEPLOYMENT = env.IS_NJDC_DEPLOYMENT
} else {
    IS_NJDC_DEPLOYMENT = "false"
}

/* Set IP if it is NJDC Deployment */
if (env.NJDC_INSTANCE_IP) {
    NJDC_INSTANCE_IP = env.NJDC_INSTANCE_IP
} else {
    NJDC_INSTANCE_IP = ""
}

/* Configure default POPO Schedule */
if (env.NATIVE_POPO_SCHEDULE) {
    NATIVE_POPO_SCHEDULE = env.NATIVE_POPO_SCHEDULE
} else {
    NATIVE_POPO_SCHEDULE = 'EU-workdays-uptime-8am-8pm'
}

if (env.PUBLIC_KEY) {
    PUBLIC_KEY = env.PUBLIC_KEY.trim()
} else {
    PUBLIC_KEY = ''
}

/* Dedicated host ID for EC2 instances */
if (env.DEDICATED_HOST_ID) {
    DEDICATED_HOST_ID = env.DEDICATED_HOST_ID
} else {
    DEDICATED_HOST_ID = ''
}

/* Set IS NJDC Deployment parameter */
if (env.DEPLOY_CUSTOM_CA_CERT) {
    DEPLOY_CUSTOM_CA_CERT = env.DEPLOY_CUSTOM_CA_CERT
} else {
    DEPLOY_CUSTOM_CA_CERT = "false"
}
/* Area Tag instances */
if (env.INSTANCE_AREA) {
    INSTANCE_AREA = env.INSTANCE_AREA
} else {
    INSTANCE_AREA = ''
}

/* Mandatory return statement on EOF */
return this