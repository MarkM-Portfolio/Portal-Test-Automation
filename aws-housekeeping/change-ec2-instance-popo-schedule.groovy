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
    This pipeline has a mandatory parameter called EC2_INSTANCE_ID of type String Parameter
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
                dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/aws-housekeeping/change-ec2-instance-popo-schedule.yaml")
            }
        }

        stage('Adjusting POPO settings') {
            steps {
                withAWS(credentials: 'aws_credentials', region: 'us-east-1') {
                    script {
                        def instanceId = pipelineParameters.EC2_INSTANCE_ID
                        sh """
                            set +x
                            export PATH="$PATH:${env.workspace}"
                            cd ${env.workspace}
                            pwd
                            echo "*****"
                            echo "Before changing POPO settings"
                            echo "*****"
                            aws ec2 describe-instances --filters --instance-ids ${instanceId} --query "Reservations[*].Instances[*].{InstanceId:InstanceId,Owner:Tags[?Key=='Owner']|[0].Value,Name:Tags[?Key=='Name']|[0].Value,Status:State.Name,Type:InstanceType,POPOSchedule:Tags[?Key=='POPOSchedule']|[0].Value}" --output table
                            echo "*****"
                        """
                        if(instanceId == ""){
                            error("Parameters provided to execute this job are not enough. Please check parameters and run again!!")
                        }
                        def popoScheduleDefined = sh(script: "aws ec2 describe-instances --instance-ids ${instanceId} --query 'Reservations[].Instances[].Tags[?Key==`POPOSchedule`].Value' --output text", returnStdout: true).trim()
                        def popoScheduleDesired = pipelineParameters.NATIVE_POPO_SCHEDULE 
                        if(popoScheduleDefined == popoScheduleDesired) {
                            echo "The provided NATIVE_POPO_SCHEDULE parameter has same value set on the instance currently.Please select a new parameter value to change on instance."
                        }
                        else {
                            sh(script: "aws ec2 create-tags --resources ${instanceId} --tags Key=POPOSchedule,Value='${popoScheduleDesired}'", returnStdout: true)
                            echo "The POPO Settings have been updated"
                        }
                        sh """
                            set +x
                            export PATH="$PATH:${env.workspace}"
                            cd ${env.workspace}
                            pwd
                            echo "*****"
                            echo "After changing POPO settings"
                            echo "*****"
                            aws ec2 describe-instances --filters --instance-ids ${instanceId} --query "Reservations[*].Instances[*].{InstanceId:InstanceId,Owner:Tags[?Key=='Owner']|[0].Value,Name:Tags[?Key=='Name']|[0].Value,Status:State.Name,Type:InstanceType,POPOSchedule:Tags[?Key=='POPOSchedule']|[0].Value}" --output table
                            echo "*****"
                        """
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
