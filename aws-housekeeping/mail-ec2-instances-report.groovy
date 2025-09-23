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
def ownersWithMultipleInstances = []
def emailAddressesList = []
def managers = ['pinki.sahachoudhury@hcl.com', 'colleen.lewis@hcl.com', 'howard.krovetz@hcl.com', 'maryann.jacobs@hcl.com', 'joel.depuyart@hcl.com','cameron.bosnic@hcl.com', 'podduturi.saikumar@hcl.com', 'davidst@hcl.com', 'rizamay.pagayon@hcl.com']

pipeline { 
    agent {
        label 'build_infra'
    }

    stages {
        stage('Load Parameters') {
            steps {
                script {
                    dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/aws-housekeeping/mail-ec2-instances-report.yaml")
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
                            echo "Area,InstanceId,Name,Owner,POPOSchedule,Status,Type" > instances.csv
                            aws ec2 describe-instances --query "Reservations[*].Instances[*].{InstanceId:InstanceId,Name:Tags[?Key=='Name']|[0].Value,Owner:Tags[?Key=='Owner']|[0].Value,Status:State.Name,Type:InstanceType,POPOSchedule:Tags[?Key=='POPOSchedule']|[0].Value,Area:Tags[?Key=='Area']|[0].Value}" --filters "Name=instance-state-name,Values=running,stopped" --region us-east-1 --output text | grep -Ev "BUILD|INFRA" | awk -F'\t' '{print \$1 "," \$2 "," \$3 "," \$4 "," \$5 "," \$6 "," \$7}' | sort -t',' -k4 >> instances.csv
                        """
                    }
                }
            }
        }
        stage('Identify Owners with More Than One Instance') {
            steps {
                script {
                    ownersWithMultipleInstances = sh(script: 'awk -F\',\' \'{print $4}\' instances.csv | sort | uniq -c | awk \'$1 > 1 {print $2}\'', returnStdout: true).trim().split()
                    // Echo the owners with more than one instance
                    echo "Owners with more than one instance: ${ownersWithMultipleInstances.join(', ')}"
                    // Create a file with owners having multiple instances using sh
                    sh """
                        echo "${ownersWithMultipleInstances.join('\n')}" > ownersWithMultipleInstances.csv
                    """
                    /* Copy CSV file to the workspace */
                    sh """
                        cp ownersWithMultipleInstances.csv ${env.WORKSPACE}/ownersWithMultipleInstancesReport.csv
                    """
                    stash includes: 'ownersWithMultipleInstancesReport.csv', name: 'ownersCSVStash'
                }
            }
        }
        stage('Send Email Notifications to Owners and Managers') {
            steps {
                script {
                    // Accessing and BUILD_NUMBER environment variables
                    def buildNumber = env.BUILD_NUMBER
                    def jobUrl = "${env.BUILD_URL}".substring(0, "${env.BUILD_URL}".lastIndexOf("/"))
                    def csvLink = "${jobUrl}/artifact/instanceReport.csv"
                    /* Copy CSV file to the workspace */
                    sh """
                        cp instances.csv ${env.WORKSPACE}/instanceReport.csv
                    """
                    /* Stash the CSV file */
                    stash includes: 'instanceReport.csv', name: 'csvStash'
                    // Checking if sending email is enabled in pipeline parameters
                    if (pipelineParameters.SEND_EMAIL == "YES") {
                        def emailSubject = "Report of instance usage for AWS cost reduction purpose in us-east-1"
                        
                        // Loop through the lists to send individual emails
                        for (int i = 0; i < ownersWithMultipleInstances.size(); i++) {
                            def owner = ownersWithMultipleInstances[i]
                            if (!(owner == "howard.krovetz@hcl.com" || owner == "brian.anderson@hcl.com" || owner == "timothy.otoole@hcl.com" || owner == "philipp.milich@hcl.com")) {
                            emailext (
                                subject: emailSubject,
                                body: """Dear $owner,This is a notification regarding your AWS instances in the us-east-1 region.We have identified that you own multiple instances in this region. It's essential to regularly review your AWS resources to ensure optimal usage and cost efficiency.
**Action Required:** Please review the listed instances and assess whether each one is still necessary for your operations. If any instances are no longer needed, we recommend deleting them to reduce AWS costs.
**CSV Report:**You can download the detailed CSV report [here](${csvLink}). The report includes information about each instance.
Thank you for your cooperation in optimizing AWS resource usage.""",
                                mimeType: 'text/plain',
                                attachmentsPattern: "**/instanceReport.csv",
                                to: owner
                                )
                        }
                        }

                        // Loop through managers to send individual emails
                        for (int i = 0; i < managers.size(); i++) {
                            def manager = managers[i]
                            emailext (
                                subject: "Manager --- Report of instance usage for AWS cost reduction purpose in us-east-1",
                                body: """Dear $manager,

This is a notification regarding AWS instances in the us-east-1 region. We have identified instances owned by multiple users.

**Action Required:**
Please review the attached CSV report for detailed information about each instance.

**CSV Report:**
You can download the detailed CSV report [here](${csvLink}). The report includes information about each instance.

Thank you for your attention to AWS resource optimization.""",
                                mimeType: 'text/plain',
                                attachmentsPattern: "**/instanceReport.csv",
                                to: manager
                            )
                        }
                    } else {
                        echo "Email notification disabled."
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
        }
        success {
            /* Archive the stashed CSV files for download */
            archiveArtifacts 'instanceReport.csv'
            archiveArtifacts 'ownersWithMultipleInstances.csv'
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
