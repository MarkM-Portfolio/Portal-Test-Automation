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

commonConfig = load "./autonomous-deployments/config/common.gvy"

def createAcceptanceTestJobParams(NAMESPACE, CONTEXT_ROOT_PATH, DX_CORE_HOME_PATH, HAS_SM = false) {
    echo "GEO MACHINE NAMESPACE: ${NAMESPACE}"
    sleep 1000
    branch="develop"
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
        [$class: 'StringParameterValue',
         name: 'TARGET_BRANCH',
         value: branch])

    FULL_CONTEXT_ROOT_PATH = "/${CONTEXT_ROOT_PATH}/${DX_CORE_HOME_PATH}"

    if(!CONTEXT_ROOT_PATH) {
        FULL_CONTEXT_ROOT_PATH = "wps/portal"
    }
    // For Site Manager, Content Composer and Portal
    buildParams.add(
        [$class: 'StringParameterValue',
            name: 'PORTAL_HOST',
            value: "http://${NAMESPACE}:10039/${FULL_CONTEXT_ROOT_PATH}"])
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'EXP_API',
         value: "http://${NAMESPACE}:4000/dx/api/core/v1"]) 
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'WCMREST',
         value: "http://${NAMESPACE}:10039/wps"]) 
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'APP_ENDPOINT',
         value: "http://${NAMESPACE}:3000/dx/ui/dam"]) 
    buildParams.add(
        [$class: 'StringParameterValue',
         name: 'DXCONNECT_HOST',
         value: "https://${NAMESPACE}:10202"]) // add port and slash

    return buildParams
}

/* Mandatory return statement on EOF */
return this
