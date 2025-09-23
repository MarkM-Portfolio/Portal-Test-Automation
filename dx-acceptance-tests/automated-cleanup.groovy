/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2021. All Rights Reserved.       *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */
pipeline {
    agent {
        label 'build_infra'
    }

    stages {
        stage('Prepare terraform') {
            steps {
                sh '''
                    curl -LJO https://releases.hashicorp.com/terraform/0.12.20/terraform_0.12.20_linux_amd64.zip
                    unzip -o terraform_0.12.20_linux_amd64.zip
                    chmod +x terraform
                    ./terraform --help
                '''
            }
        }
        /*
         *  We use terraform to destroy old EC2 instances that have expired. For all instances that are expired, the terraform destroy command will be issued.
         */
        stage('Remove expired DX_Acceptance_Tests EC2 instances'){
            options {
                timeout(time: 60, unit: 'MINUTES') 
            }
            steps {
                withAWS(credentials: 'aws_credentials', region: 'us-east-1') {
                    dir("${workspace}/dx-acceptance-tests/terraform/ec2-launch") {
                        script {
                            def currentTime = System.currentTimeMillis()
                            // query for all DX_Acceptance_Tests instances, just return the tags Expires and Name
                            def awsResult = sh(script: "aws ec2 describe-instances " + 
                                "--filters 'Name=tag:Name, Values=DX_Acceptance_Tests*'" + 
                                " --query 'Reservations[].Instances[].{Instance: InstanceId,Expires: Tags[?Key==`expires`].Value,Name: Tags[?Key==`Name`].Value}'", 
                                returnStdout: true).trim()
                            awsResult = awsResult.replace("\n","")
                            def jsonAwsResult = readJSON text: awsResult
                            echo "Numbers of instances candidate for deletion: ${jsonAwsResult.size()}"
                            echo jsonAwsResult.toString();
                            jsonAwsResult.each {
                                if (it['Expires'][0] == null){
                                    echo "Instance ${it['Instance']} does not have expiration date, will not be deleted"
                                    return;
                                }
                                if(it['Expires'][0].toLong() > currentTime) {
                                    echo "Instance ${it['Instance']} not yet expired."
                                    return;
                                }
                                // for each instance that is expired, perform the destroy command
                                if(it['Expires'][0].toLong() < currentTime) {
                                    echo "Instance ${it['Instance']} is already expired to be deleted!"
                                    // dynamically create a terraform config for the instance
                                    sh(script: """
                                        cp variables.tf variables.tf.def
                                        sed -i 's/ENVIRONMENT_HOSTNAME/${it['Name'][0]}/g' variables.tf
                                        cat variables.tf
                                        ${workspace}/terraform init
                                        ${workspace}/terraform destroy -refresh=false -auto-approve
                                        cp variables.tf.def variables.tf
                                    """)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    post {
        cleanup {
            script {
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
}
