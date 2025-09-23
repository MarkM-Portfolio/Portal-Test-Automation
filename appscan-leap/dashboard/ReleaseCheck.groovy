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
def releaseToken = "/release/95_"
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
                    appscanWorkspace = "${env.WORKSPACE}/appscan-leap/dashboard"
                    dxParametersLoadFromFile(pipelineParameters, "${appscanWorkspace}/parameters.yaml")
                    
                    env.NEW_RELEASE_SCAN_TEXT = "false"

                    // Read appscan-leap DSL job.yaml to get SCAN_REPO list
                    withCredentials([
                        usernamePassword(credentialsId: "qbt-pullrequest-id", passwordVariable: 'GIT_TOKEN', usernameVariable: 'GIT_USER')
                    ]) {
                        def dslAppscanJobYaml = readYaml text: sh(script: "curl --silent -u ${GIT_USER}:${GIT_TOKEN} https://raw.git.cwp.pnp-hcl.com/Team-Q/dx-jenkins-jobs/develop/jobs/appscan-leap/jobs.yaml", returnStdout: true)
                        dslAppscanJobYaml.jobs.parameters[0].each { parameter ->
                            if (parameter.name == "SCAN_REPO") {
                                appscanCloneList = parameter.value.join(" ").replace("full ","")
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
                    	// Check for the latest available release branch from base
                        releasecheck = sh(script: """
                                          ${noDbg}
                                          git ls-remote git@git.cwp.pnp-hcl.com:websphere-portal/base.git |grep "${releaseToken}" | awk  '{print \$2}'
                                       """, returnStdout: true)
                        // Get last entry and extract release only
                        releaseBranch = releasecheck.split('\n').last().replace("refs/heads/","")
                        branchrelease = releaseBranch.replace("/","-")
                        println "Last Git release branch: ${releaseBranch} (${branchrelease})"
                    
                        // Check if all scan projects have latest available release branch
                    	appscanCloneList.split(" ").each { gitRepo ->
                           releasecheck = sh(script: """
                                             ${noDbg}
                                             git ls-remote git@git.cwp.pnp-hcl.com:"${gitRepo}".git |grep "${releaseToken}" | awk  '{print \$2}'
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
                    echo "Check for last release scan result on dashboard."
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
        stage('Run appscan-leap release scan') {
            when {
                expression { env.NEW_RELEASE_SCAN_TEXT == "true" }
            }
            steps {
                script {
                    buildParams = []

                    // Set APPSCAN_REPORT to report just uploaded to Artifactory.
                    buildParams.add(string(
                        name: 'BUILD_TYPE',
                        value: "release"
                    ))
                    
                    println "Kickoff release scan for ${pipelineParameters.APPSCAN_SCAN_JOB}."
                    println "Parameters: ${buildParams}"
                    
                    // Start appscan-leap scan job but don't wait.
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
