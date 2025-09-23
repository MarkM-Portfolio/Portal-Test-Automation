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

def pipelineParameters = [:]

pipeline { 
    agent {
        label 'build_infra'
    }

    stages {
        stage('Load Parameters') {
            steps {
                script {
                    dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/aws-housekeeping/report-ec2-instances.yaml")
                }
            }
        }
        stage('Get AWS instances with their tags') {
            steps {
                withAWS(credentials: 'aws_credentials', region: 'us-east-1') {
                    script {
                        sh """
                            set +x
                            export PATH="$PATH:${env.WORKSPACE}"
                            cd ${env.WORKSPACE}
                            echo "InstanceId,Name,Owner,POPOSchedule,Status,Type" > instances.csv
                            aws ec2 describe-instances --query "Reservations[*].Instances[*].{InstanceId:InstanceId,Name:Tags[?Key=='Name']|[0].Value,Owner:Tags[?Key=='Owner']|[0].Value,Status:State.Name,Type:InstanceType,POPOSchedule:Tags[?Key=='POPOSchedule']|[0].Value}" --region us-east-1 --output text | awk -F'\t' '{print \$1 "," \$2 "," \$3 "," \$4 "," \$5 "," \$6}' | sort -t',' -k3 >> instances.csv
                        """
                        
                    }
                }
            }
        }
    }
    
    post {
        always {
            /* Copy CSV file to the workspace */
            sh """
                cp instances.csv ${env.WORKSPACE}/instanceReport.csv
            """
            /* Stash the CSV file */
            stash includes: 'instanceReport.csv', name: 'csvStash'
            
            /* Send the CSV file to Microsoft Teams */
            script {
                // Accessing and BUILD_NUMBER environment variables
                def buildNumber = env.BUILD_NUMBER
                // Use the values in the payload or any other part of the pipeline
                echo "Build Number: ${buildNumber}"
                // You can also use these variables to construct the CSV link
                def jobUrl = "${env.BUILD_URL}".substring(0, "${env.BUILD_URL}".lastIndexOf("/"))
                echo "jobUrl Link: ${jobUrl}"
                def csvLink = "${jobUrl}/artifact/instanceReport.csv"
                def payload = """
                          {
                            "@type": "MessageCard",
                            "@context": "http://schema.org/extensions",
                            "themeColor": "#E5E4E2",
                            "summary": "Report of instance usage for AWS cost reduction purpose in us-east-1",
                            "title": "Report of instance usage for AWS cost reduction purpose in us-east-1",
                            "text": "Report of instance usage for AWS cost reduction purpose in us-east-1 [us-east-1](https://console.aws.amazon.com/ec2/v2/home?region=us-east-1)   \n",
                            "sections": [
                                {
                                "activityTitle": "## Intended Audience can click on this below link and the CSV file will be automatically get downloaded on their respective systems to be viewed.",
                                "text": "[pinki.sahachoudhury@hcl.com](mailto:pinki.sahachoudhury@hcl.com)  \n[howard.krovetz@hcl.com](mailto:howard.krovetz@hcl.com)  \n[colleen.lewis@hcl.com](mailto:colleen.lewis@hcl.com)  \n[joel.depuyart@hcl.com](mailto:joel.depuyart@hcl.com)"
                                },
                                {
                                "activityTitle": "## Report of instance usage for AWS cost reduction purpose in us-east-1",
                                "text": "${csvLink}"
                                }  
                            ]
                        }
                    """
                if(pipelineParameters.SEND_TEAMS == "true") {
                    if(!pipelineParameters.WEBHOOK_URL){
                        pipelineParameters.WEBHOOK_URL = "https://hclo365.webhook.office.com/webhookb2/8a6712b0-0629-4fbb-9e35-641ae6c7f577@189de737-c93a-4f5a-8b68-6f4ca9941912/IncomingWebhook/59c4ca8bff204174b95ed0db3c67cf5a/3b42c90c-443d-4ada-980c-96a5cbf337a6"
                    }
                    sh """
                        curl -X POST -H 'Content-Type: application/json' -d '${payload}' '${pipelineParameters.WEBHOOK_URL}'
                    """                                    
                } else {
                    echo "Teams message disabled."
                }
            }
        }
        success {
            /* Archive the stashed CSV file for download */
            archiveArtifacts 'instanceReport.csv'
        }
        
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
