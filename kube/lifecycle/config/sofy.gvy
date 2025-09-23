/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2022. All Rights Reserved.       *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

//env.TARGET_GITHUB_FORK_REPOSITORY - Source GitHub repository from which we pull the code. (SSH)
if (env.TARGET_GITHUB_FORK_REPOSITORY) {
    COMMON_TARGET_GITHUB_FORK_REPOSITORY = env.TARGET_GITHUB_FORK_REPOSITORY
} else {
    COMMON_TARGET_GITHUB_FORK_REPOSITORY = "git@github01.hclpnp.com:hcl-dx-shared/sofy-catalog-content.git"
}
// env.TARGET_GITHUB_UPSTREAM_REPOSITORY - Target GitHub repository that we will open a PR against from our fork. (SSH)
if (env.TARGET_GITHUB_UPSTREAM_REPOSITORY) {
    COMMON_TARGET_GITHUB_UPSTREAM_REPOSITORY = env.TARGET_GITHUB_UPSTREAM_REPOSITORY
} else {
    COMMON_TARGET_GITHUB_UPSTREAM_REPOSITORY = "git@github01.hclpnp.com:kubernetes/sofy-catalog-content.git"
} 
//env.TARGET_GITHUB_CREDENTIALS - GitHub credentials used to pull code. (Jenkins Credential ID)
if (env.TARGET_GITHUB_CREDENTIALS) {
    COMMON_TARGET_GITHUB_CREDENTIALS = env.TARGET_GITHUB_CREDENTIALS
} else {
    COMMON_TARGET_GITHUB_CREDENTIALS = "sofy_git_credentials"
}
//env.TARGET_GITHUB_BASE_DIRECTORY - The directory where we put our code inside
if (env.TARGET_GITHUB_BASE_DIRECTORY) {
    COMMON_TARGET_GITHUB_BASE_DIRECTORY = env.TARGET_GITHUB_BASE_DIRECTORY
} else {
    COMMON_TARGET_GITHUB_BASE_DIRECTORY = "GA"
}
//env.HELM_CHART_DIRECTORY - The helm chart directory that should be copied over, currently we have dx and dx-debug
if (env.HELM_CHART_DIRECTORY) {
    COMMON_HELM_CHART_DIRECTORY = env.HELM_CHART_DIRECTORY
} else {
    COMMON_HELM_CHART_DIRECTORY = "dx"
}

/* Mandatory return statement on EOF */
return this
