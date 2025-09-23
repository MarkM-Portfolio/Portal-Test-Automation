/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2023. All Rights Reserved.       *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

import java.text.SimpleDateFormat

// Use our DX shared library
@Library("dx-shared-library") _

// Define global runtime variables
def releaseBranch = ""
def releaseFresh = true
def appscanReleaseFolder = []
def appscanLastRelease = ""
def appscanCloneList = ""
def appscanWorkspace = ""

def noDbg = "{ set +x; } 2>/dev/null"

def pipelineParameters = [:]


pipeline { 
    // Runs in build_infra, since we are creating infrastructure
    agent {
        label 'build_infra'
    }

    stages {
        // Load the pipeline parameters into object
        stage('Load parameters') {
            steps {
                script {
                    appscanWorkspace = "${env.WORKSPACE}/appscan/dashboard"
                    dxParametersLoadFromFile(pipelineParameters, "${appscanWorkspace}/parameters.yaml")
                    
                    // Check Jenkins global variables needed to calculate upload target
                    // Break pipeline if not set
                    if (env.G_ARTIFACTORY_URL == null || env.G_ARTIFACTORY_URL == "") {
                        error("Jenkins global variable G_ARTIFACTORY_URL not set")
                    }
                    if (env.G_ARTIFACTORY_GENERIC_NAME == null || env.G_ARTIFACTORY_GENERIC_NAME == "") {
                        error("Jenkins global variable G_ARTIFACTORY_GENERIC_NAME not set")
                    }

                    // Break pipeline if post job is empty
                    if (pipelineParameters.PROJECT_CHECK == "") {
                        error("Optional parameter PROJECT_CHECK must not be empty")
                    }

                    // Break pipeline if post job is empty
                    if (pipelineParameters.RELEASE_TOKEN == "") {
                        error("Optional parameter RELEASE_TOKEN must not be empty")
                    }

                    // Break pipeline if post job is empty
                    if (pipelineParameters.APPSCAN_SCAN_JOB == "") {
                        error("Optional parameter APPSCAN_SCAN_JOB must not be empty")
                    }

                    // Break pipeline if post job is empty
                    if (pipelineParameters.APPSCAN_SEED_JOB == "") {
                        error("Optional parameter UPDATE_DASHBOARD_JOB must not be empty")
                    }

                    // Break pipeline if post job is empty
                    if (pipelineParameters.appscanProjectFolder == "") {
                        error("Optional parameter appscanProjectFolder must not be empty")
                    }

                    // Always add _development suffix to publish folders if not running on production Jenkins
                    if (!env.JOB_URL.contains("/portal-jenkins-staging.cwp")) {
                        pipelineParameters.appscanProjectFolder += "_development"
                        pipelineParameters.appscanS3Root += "_development"
                        println "Job is not running on PJS. Adding suffix _development where needed.\n - pipelineParameters.ARTIFACTORY_ROOT_FOLDER = ${pipelineParameters.ARTIFACTORY_ROOT_FOLDER}\n - pipelineParameters.appscanProjectFolder = ${pipelineParameters.appscanProjectFolder}\n - pipelineParameters.appscanS3Root = ${pipelineParameters.appscanS3Root}"
                    }
                    
                    env.NEW_RELEASE_SCAN_TEXT = "false"

                    // Read appscan DSL job.yaml to get SCAN_REPO list
                    println "Read appscan DSL job.yaml from ${pipelineParameters.APPSCAN_SEED_JOB} to get SCAN_REPO list"
                    withCredentials([
                        usernamePassword(credentialsId: "qbt-pullrequest-id", passwordVariable: 'GIT_TOKEN', usernameVariable: 'GIT_USER')
                    ]) {
                        def dslAppscanJobYaml = readYaml text: sh(script: "curl --silent -u ${GIT_USER}:${GIT_TOKEN} https://raw.git.cwp.pnp-hcl.com/Team-Q/dx-jenkins-jobs/${pipelineParameters.appscanSeedJobBranch}/jobs/${pipelineParameters.APPSCAN_SEED_JOB}/jobs.yaml", returnStdout: true)
                        if (dslAppscanJobYaml.jobs == null) {
                            error("Can't download Team-Q/dx-jenkins-jobs/develop/jobs/${pipelineParameters.APPSCAN_SEED_JOB}/jobs.yaml\n${dslAppscanJobYaml}")
                        } else {
                            dslAppscanJobYaml.jobs.parameters[0].each { parameter ->
                                if (parameter.name == "SCAN_REPO") {
                                    appscanCloneList = parameter.value.join(" ").replace("full ","")
                                }
                            }
                        }
                    }
                }
            }
        }
        stage('Check for new release') {
            steps {
                script {
                    echo "Check for last release branch in Git."
                    sshagent(credentials: ['jenkins']) {
                    	// Check for the latest available release branch
                    	println "Check for the latest available release branch for ${pipelineParameters.PROJECT_CHECK} (token = ${pipelineParameters.RELEASE_TOKEN})"
                        releasecheck = sh(script: """
                                          ${noDbg}
                                          git ls-remote git@git.cwp.pnp-hcl.com:${pipelineParameters.PROJECT_CHECK} |grep "${pipelineParameters.RELEASE_TOKEN}" | awk  '{print \$2}'
                                       """, returnStdout: true)
                        // Get last entry and extract release only
                        releaseBranch = releasecheck.split('\n').last().replace("refs/heads/","")
                        branchrelease = releaseBranch.replace("/","-")
                        println "Last Git release branch: ${releaseBranch} (${branchrelease})"
                    
                        // Check if all scan projects have latest available release branch
                    	appscanCloneList.split(" ").each { gitRepo ->
                           releasecheck = sh(script: """
                                             ${noDbg}
                                             git ls-remote git@git.cwp.pnp-hcl.com:"${gitRepo}".git |grep "${pipelineParameters.RELEASE_TOKEN}" | awk  '{print \$2}'
                                          """, returnStdout: true)
                                          
                           if (releaseBranch != releasecheck.split('\n').last().replace("refs/heads/","")) {
                               releaseFresh = false
                               println "${gitRepo} doesn't have release branch ${releaseBranch}"
                           }
                        }
                    }
                    
                    if (!releaseFresh) {
                    	error("Not all repos to scan have latest release branch ${releaseBranch}.")
                    }
                    
                    // Check dashboard on S3 bucket
                    println "Check for last release scan result on dashboard at\n$pipelineParameters.appscanS3Root/$pipelineParameters.appscanResultMainFolder"
                    appscanReleaseFolder = dxAwsCommand(awsCommand: "s3 ls $pipelineParameters.appscanS3Root/$pipelineParameters.appscanResultMainFolder/").split("\n").findAll { s -> s ==~ /.*release.*/ }
                    appscanLastRelease = appscanReleaseFolder.last().replace("_full/","").replace("PRE ","").trim()
                    scanrelease = appscanLastRelease.substring(0, appscanLastRelease.length() - appscanLastRelease.lastIndexOf("_"))
                    println "Last release scan: ${appscanLastRelease} (${scanrelease})"

                    if (branchrelease == scanrelease) {
                    	println "Found release branch already scanned and published on dashboard."
                    } else {
                        env.NEW_RELEASE_SCAN_TEXT = "true"
                        println "New release to scan: ${releaseBranch})"
                    }
                }
            }
        }
        stage('Run appscan release scan') {
            when {
                expression { env.NEW_RELEASE_SCAN_TEXT == "true" }
            }
            steps {
                script {
                    buildParams = []
                    
                    // Prefix Jenkins current job folder to pipelineParameters.APPSCAN_SCAN_JOB to start downstream from the same folder
                    if (env.JOB_NAME.contains("/")) {
                        pipelineParameters.APPSCAN_SCAN_JOB = "${JOB_NAME.split('/')[0..-2].join('/')}/${pipelineParameters.APPSCAN_SCAN_JOB}"
                    }

                    // Set pipeline parameters for Jenkins job to start
                    buildParams.add(string(
                        name: 'BUILD_TYPE',
                        value: "release"
                    ))
                    
                    println "Kickoff release scan for ${pipelineParameters.APPSCAN_SCAN_JOB}."
                    println "Parameters: ${buildParams}"

                    // Start appscan scan job but don't wait.
                    // Job name loaded from parameters.yaml.
                    build(
                        job: pipelineParameters.APPSCAN_SCAN_JOB, 
                        parameters: buildParams, 
                        wait: false
                    )
                }
            }
        }
    }

    post {
        cleanup {
            script {
                dxWorkspaceDirectoriesCleanup()
            }
        }
    }
}
