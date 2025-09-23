/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2023. All Rights Reserved.   *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

// Import the shared library with the name configured in Jenkins
@Library("dx-shared-library") _

// Create object to store parameters with values
def pipelineParameters = [:]

/*
    !!! IMPORTANT !!!
    This pipeline has a mandatory parameter called with EC2_INSTANCE_ID of type String Parameter
    Make sure to configure it in your Jenkins Job, otherwise the Job will obviously not run ;)
*/

 pipeline { 
    agent {
        label 'build_infra'
    }

    stages {
        stage('Load Parameters') {
            steps {
                // Load parameters defined in the job yaml file
                dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/aws-housekeeping/change-ec2-instance-state.yaml")
            }
        }
        stage('Change instance state') {
            steps {
                withAWS(credentials: 'aws_credentials', region: 'us-east-1') {
                    script {
                        // Wait time and maximum cycles to wait for instance is running again
                        def maxCycle = 100
                        def waitTime = 5
                        def maxWait = maxCycle * waitTime
                        def awsInstanceId = pipelineParameters.EC2_INSTANCE_ID
                        // Get instance ID for stopped instance with name given in pipelineParameters.EC2_INSTANCE_ID
                        if(pipelineParameters.EC2_INSTANCE_ID == ""){
                            error("Parameters provided to execute this job are not enough. Please check parameters and run again!!")
                        }
                        if(pipelineParameters.ACTION == 'start'){
                            echo "instance id is ${awsInstanceId} "
                             if (awsInstanceId != "") {
                                println "Starting EC2 instance with id: instance ID = ${awsInstanceId}"
                                def awsResult = sh(script: "aws ec2 start-instances --instance-ids ${awsInstanceId}",returnStdout: true).trim()
                                while (maxCycle > 0) {
                                    awsResult = sh(script: "aws ec2 describe-instances" +
                                        " --instance-ids ${awsInstanceId}" +
                                        " --query 'Reservations[*].Instances[*].[State]'" +
                                         " --output text", 
                                        returnStdout: true).trim()      
                                    println awsResult
                                    if (awsResult.contains("running")) {
                                        println "Instance started."
                                        maxCycle = 0
                                    } else {
                                        maxCycle--
                                        if (maxCycle > 0) {
                                            println "Instance not running yet. Remaining retries: ${maxCycle}"
                                            sleep waitTime
                                        } else {
                                            println "Instance not restarted within ${maxWait} seconds."
                                            currentBuild.result = 'UNSTABLE'
                                        }
                                    }
                                }
                            } else {
                                error( "No stopped EC2 instance found with id: ${pipelineParameters.EC2_INSTANCE_ID}")
                            }
                        }
                        if(pipelineParameters.ACTION == 'stop'){
                                awsInstanceId = pipelineParameters.EC2_INSTANCE_ID
                                echo "If stop action is selected instance id is ${awsInstanceId} "
                                    if (awsInstanceId != "") {
                                        println "instance ID = ${awsInstanceId}"
                                        def awsResult = sh(script: "aws ec2 stop-instances --instance-ids ${awsInstanceId}", returnStdout: true).trim()
                                        while (maxCycle > 0) {
                                            awsResult = sh(script: "aws ec2 describe-instances" +
                                                " --instance-ids ${awsInstanceId}" +
                                                " --query 'Reservations[*].Instances[*].[State]'" +
                                                " --output text", 
                                                returnStdout: true).trim()
                                            println awsResult
                                            if (awsResult.contains("stopped")) {
                                                println "Instance stopped."
                                                maxCycle = 0
                                            } else {
                                                maxCycle--
                                                    if (maxCycle > 0) {
                                                        println "Instance not stopped yet. Remaining retries: ${maxCycle}"
                                                        sleep waitTime
                                                    } else {
                                                        println "Instance not stopped within ${maxWait} seconds."
                                                        currentBuild.result = 'UNSTABLE'
                                                    }
                                                }
                                            }
                                        } else {
                                            error("No running EC2 instance found with id: ${pipelineParameters.EC2_INSTANCE_ID}")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
    
    post {
        cleanup {
            /* Cleanup workspace */
            dxWorkspaceDirectoriesCleanup()
        }
    }
    
}
