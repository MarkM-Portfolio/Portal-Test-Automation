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
import groovy.transform.Field
import groovy.time.TimeCategory
import java.time.LocalDateTime 
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

// Import the shared library with the name configured in Jenkins
@Library('dx-shared-library') _

// Create object to store parameters with values
def pipelineParameters = [:]

pipeline {
    agent {
        label 'build_infra'
    }

    stages {
        stage('Load parameters') {
            steps {
                dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/kube/lifecycle/native-lifetime.yaml")
            }
        }

        stage('Find instance') {
            steps {
                withAWS(credentials: 'aws_credentials', region: 'us-east-1') {
                    script {
                        if (pipelineParameters.INSTANCE_NAME == '' && pipelineParameters.INSTANCE_ID == '') {
                            error('Neither INSTANCE_NAME nor INSTANCE_ID have been provided, can not continue')
                        }
                        if (pipelineParameters.INSTANCE_NAME != '') {
                            println 'INSTANCE_NAME has been provided, ignoring any INSTANCE_ID and try to determine ID from given name'
                            def awsResult = sh(
                                script: "aws ec2 describe-instances --filters 'Name=tag:Name,Values=${pipelineParameters.INSTANCE_NAME}' --output=json --no-cli-pager",
                                returnStdout: true
                            )
                            def parsedAwsResult = readJSON text: awsResult
                            if (parsedAwsResult.Reservations.size() > 1) {
                                error('Found more than one instance matching the INSTANCE_NAME criteria')
                            }
                            if (parsedAwsResult.Reservations.size() == 0) {
                                error('Found no instance matching the INSTANCE_NAME criteria')
                            }
                            // We use the only found instance as reference
                            def currentInstance = parsedAwsResult.Reservations[0].Instances[0]
                            println "Determined instance ID to be ${currentInstance.InstanceId}"
                            pipelineParameters.INSTANCE_ID = currentInstance.InstanceId
                        }
                    }
                }
            }
        }

        stage('Adjust TTL tags') {
            steps {
                script {
                    // Get time and date
                    def dateFormat = new SimpleDateFormat('yyyyMMdd-HHmmss')
                    def currentTime = System.currentTimeMillis()
                    def currentTimeAsDate = new Date(currentTime)
                    // Get old tags
                    def existingTags = dxEC2TtlGetTags(instance_id: pipelineParameters.INSTANCE_ID)
                    println "Existing tags on instance with ID ${pipelineParameters.INSTANCE_ID}:"
                    println existingTags
                    // Check if values are empty. If they are, propagate them with correct values
                    if (!existingTags.LC_CreatedBy || existingTags.LC_CreatedBy == '') {
                        println 'LC_CreatedBy is empty, assigning current user.'
                        existingTags.LC_CreatedBy = dxJenkinsGetJobOwner()
                    }
                    if (!existingTags.LC_CreatedFrom || existingTags.LC_CreatedFrom == '') {
                        println 'LC_CreatedFrom is empty, assigning current environment'
                        existingTags.LC_CreatedFrom = G_JENKINS_BUILD_ENV_NAME
                    }
                    if (!existingTags.LC_CreationDate || existingTags.LC_CreationDate == '') {
                        println 'LC_CreationDate is empty, assigning current date'
                        existingTags.LC_CreationDate = "${dateFormat.format(currentTimeAsDate)}"
                    }
                    println 'Setting LC_TimeToLive'
                    def expireTime = currentTime + (((pipelineParameters.TIME_TO_LIVE as double) * 60 * 60 * 1000) as long)
                    def expiryTimeAsDate = new Date(expireTime)
                    existingTags.LC_TimeToLive = "${dateFormat.format(expiryTimeAsDate)}"
                    println "The following tags will be assigned to the instance with ID ${pipelineParameters.INSTANCE_ID}:"
                    println existingTags

                    // Set the tags accordingly
                    existingTags.instance_id = pipelineParameters.INSTANCE_ID
                    dxEC2TtlSetTags(existingTags)
                }
            }
        }
    }

    post {
        cleanup {
            dxWorkspaceDirectoriesCleanup()
        }
    }
}
