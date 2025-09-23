/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2022, 2023. All Rights Reserved.   *
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
    This pipeline has a mandatory parameter called EC2_INSTANCE_NAME of type String Parameter
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
                dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/aws-housekeeping/restart-ec2-instance.yaml")
            }
        }
        stage('Adjusting POPO settings') {
            steps {
                withAWS(credentials: 'aws_credentials', region: 'us-east-1') {
                    script {
                        if(env.EC2_INSTANCE_NAME == ""){
                            error("Parameters provided to execute this job are not enough. Please check parameters and run again!!")
                        }
                        def popoScheduleDefined = sh(script: "aws ec2 describe-instances --filters 'Name=tag:Name,Values=${env.EC2_INSTANCE_NAME}' --query 'Reservations[].Instances[].Tags[?Key==`POPOSchedule`].Value' --output text", returnStdout: true).trim()
                        def popoScheduleDesired = env.NATIVE_POPO_SCHEDULE
                        if(popoScheduleDesired == "Do not alter current POPO settings") {
                            echo "No changes in the current POPO settings have been made to this instance."
                        }
                        else if(popoScheduleDefined == popoScheduleDesired) {
                            echo "The provided NATIVE_POPO_SCHEDULE parameter has same value set on the instance currently.Please select a new parameter value to change on instance."
                        }
                        else if((popoScheduleDefined != popoScheduleDesired) && popoScheduleDesired != "Do not alter current POPO settings") {
                            def instanceId = sh(script: "aws ec2 describe-instances --filters 'Name=tag:Name,Values=${env.EC2_INSTANCE_NAME}' --query 'Reservations[].Instances[].InstanceId' --output text | tr '\n' ' '", returnStdout: true).trim()
                            sh(script: "aws ec2 create-tags --resources ${instanceId} --tags Key=POPOSchedule,Value=${popoScheduleDesired}", returnStdout: true)
                            echo "The POPO Settings have been updated"
                        }

                    }
                }
            }
        }
        
        stage('Perform AWS instance action') {
            steps {
                withAWS(credentials: 'aws_credentials', region: 'us-east-1') {
                    script {
                        // Wait time and maximum cycles to wait for instance is running again
                        def maxCycle = 100
                        def waitTime = 5
                        def maxWait = maxCycle * waitTime
                        def awsInstanceId
                        // Get instance ID for stopped instance with name given in env.EC2_INSTANCE_NAME
                        if(env.EC2_INSTANCE_NAME == ""){
                            error("Parameters provided to execute this job are not enough. Please check parameters and run again!!")
                        }
                        if(env.ACTION == 'no-action'){
                            echo "No action is taken on current state of the instance"
                        }
                        if(env.ACTION == 'start'){
                            awsInstanceId = sh(script: "aws ec2 describe-instances" +
                                " --filters \"Name=instance-state-name,Values=stopped\"" +
                                " \"Name=tag:Name,Values=${env.EC2_INSTANCE_NAME}\"" +
                                " --query 'Reservations[*].Instances[*].[InstanceId]'" +
                                " --output text", 
                                returnStdout: true).trim()
                            echo "If start action is selected then instance id is ${awsInstanceId} "
                             if (awsInstanceId != "") {
                                println "Restarting EC2 instance named ${env.EC2_INSTANCE_NAME} and instance ID = ${awsInstanceId}"
                                def awsResult = sh(script: "aws ec2 start-instances --instance-ids ${awsInstanceId}",returnStdout: true).trim()
                                while (maxCycle > 0) {
                                    awsResult = sh(script: "aws ec2 describe-instances" +
                                        " --filters \"Name=tag:Name,Values=${env.EC2_INSTANCE_NAME}\"" +
                                        " --query 'Reservations[*].Instances[*].[State]'" +
                                        " --output text", 
                                        returnStdout: true).trim()
                                    println awsResult
                                    if (awsResult.contains("running")) {
                                        println "Instance restarted."
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
                                error( "No stopped EC2 instance found matching name ${env.EC2_INSTANCE_NAME}")
                            }
                        }
                        if(env.ACTION == 'stop'){
                                awsInstanceId = sh(script: "aws ec2 describe-instances" +
                                        " --filters \"Name=instance-state-name,Values=running\"" +
                                        " \"Name=tag:Name,Values=${env.EC2_INSTANCE_NAME}\"" +
                                        " --query 'Reservations[*].Instances[*].[InstanceId]'" +
                                        " --output text",
                                    returnStdout: true).trim()
                                echo "If stop action is selected instance id is ${awsInstanceId} "
                                    if (awsInstanceId != "") {
                                        println "Stopping EC2 instance named ${env.EC2_INSTANCE_NAME} and instance ID = ${awsInstanceId}"
                                        def awsResult = sh(script: "aws ec2 stop-instances --instance-ids ${awsInstanceId}", returnStdout: true).trim()
                                        while (maxCycle > 0) {
                                            awsResult = sh(script: "aws ec2 describe-instances" +
                                                " --filters \"Name=tag:Name,Values=${env.EC2_INSTANCE_NAME}\"" +
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
                                            error("No running EC2 instance found matching name ${env.EC2_INSTANCE_NAME}")
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
