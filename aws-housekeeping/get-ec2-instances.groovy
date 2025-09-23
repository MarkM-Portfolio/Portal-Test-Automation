/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2022, 2023. All Rights Reserved.       *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */
 @Library("dx-shared-library") _

 // Create object to store parameters with values
 def pipelineParameters = [:]

 pipeline { 
    agent {
        label 'build_infra'
    }

    stages {
        stage('Load Parameters') {
            steps {
                script {
                    dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/aws-housekeeping/get-ec2-parameter.yaml")
                }
            }
        }
        stage('Get AWS instances with their tags') {
            steps {
                withAWS(credentials: 'aws_credentials', region: "${pipelineParameters.REGION}") {
                    script {
                        def area = "TEST"
                        // If the user has provided the area, then use it
                        if(pipelineParameters.INSTANCE_AREA) {
                            area = pipelineParameters.INSTANCE_AREA
                        }
                        sh """
                            set +x
                            export PATH="$PATH:${env.WORKSPACE}"
                            cd ${env.WORKSPACE}
                            pwd
                            echo "***********************************************************"
                            echo "Number of instances on this owner are:"
                            aws ec2 describe-instances --filters "Name=tag:Owner,Values=${pipelineParameters.USER_EMAIL}" --query "Reservations[*].Instances[*].{InstanceId:InstanceId,Owner:Tags[?Key=='Owner']|[0].Value,Name:Tags[?Key=='Name']|[0].Value,Status:State.Name,Type:InstanceType,POPOSchedule:Tags[?Key=='POPOSchedule']|[0].Value}" --region ${env.REGION} --output text | wc -l
                            echo "Describing ec2 instances under this Owner in Tabular Output"
                            echo "Please note that only instances with Area=$area,SUPPORT will be shown, so the number of total instances above may vary from the table below."
                            aws ec2 describe-instances --filters "Name=tag:Owner,Values=${pipelineParameters.USER_EMAIL}" "Name=tag:Area,Values=$area,SUPPORT" --query "Reservations[*].Instances[*].{InstanceId:InstanceId,Owner:Tags[?Key=='Owner']|[0].Value,Area:Tags[?Key=='Area']|[0].Value,Name:Tags[?Key=='Name']|[0].Value,Status:State.Name,Type:InstanceType,POPOSchedule:Tags[?Key=='POPOSchedule']|[0].Value}" --region ${env.REGION} --output table
                            echo "***********************************************************"
                        """
                    }
                }
            }
        }
    }
    
    post {
        cleanup {
            /* Cleanup workspace */
            dir("${workspace}") {
                deleteDir()
            }

            /* Cleanup workspace@tmp */
            dir("${workspace}@tmp") {
                deleteDir()
            }
        }
    }
    
}

