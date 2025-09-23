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

// Import the shared library with the name configured in Jenkins
@Library("dx-shared-library") _

import java.text.SimpleDateFormat;

pipeline { 
    
    agent {
        node {
            label "build_infra"
        }
    }
    
    parameters {
        choice(name: 'MASTER_ENV', choices: ['PJD','PJT','PJDT'], description: 'Select Jenkins master environment to restore.')
        string(name: 'BACKUP_DATE', defaultValue: '', description: 'Enter date of backup to restore (dd.mm.yyyy, default: latest)',  trim: true)
        choice(name: 'RESTORE_OPTION', choices: ['target system offline','Jenkins not responding','force replace Jenkins','force replace system'], description: 'Select restore option.')
        string(name: 'NEW_SYSTEM_NAME', defaultValue: '', description: 'Enter system name to restore backup on rather than default.',  trim: true)
        choice(name: 'DRY_RUN', choices: ['yes','no'], description: 'Select dummy or real restore.')
    }
    
    stages {
        stage("Check backup") {
            steps {
                script {
                    env.REPO_PATH = "dx-jenkins-backup"
                    env.RESTORE_FILE = ""
                    env.GENERIC_URL = "https://${G_ARTIFACTORY_HOST}/artifactory/${G_ARTIFACTORY_GENERIC_NAME}"
                    env.SYSTEM_NAME = "portal-jenkins-test.cwp.pnp-hcl.com"
                    if (!env.MASTER_ENV) {
                        env.MASTER_ENV = params.MASTER_ENV
                    }
                    if (!env.NEW_SYSTEM_NAME) {
                        env.NEW_SYSTEM_NAME = ""
                    }
                    if (!env.DRY_RUN) {
                        env.DRY_RUN = params.DRY_RUN
                    }
                    if (env.BACKUP_DATE) {
                        println "Get backup file for ${env.MASTER_ENV} from ${env.BACKUP_DATE}"
                        boolean match = "${env.BACKUP_DATE}" ==~ /\d{2}\.\d{2}\.\d{4}/
                        if (!match) {
                            println "ERROR: Incorrect date ${env.BACKUP_DATE}. Use format dd.mm.yyyy"
                            sh "exit 1"
                        }
                        backupDate = env.BACKUP_DATE.substring(6) + env.BACKUP_DATE.substring(3,5) + env.BACKUP_DATE.substring(0,2)
                        env.RESTORE_FILE = sh (script: "set +x && curl -s ${env.GENERIC_URL}/${env.REPO_PATH}/ | grep ${env.MASTER_ENV}-bkp_${backupDate}_ | cut -d '\"' -f 2", returnStdout: true).trim()
                        if (env.RESTORE_FILE == "") {
                            availBkps = sh (script: "set +x && curl -s ${env.GENERIC_URL}/${env.REPO_PATH}/ | grep ${env.MASTER_ENV}-bkp_ | cut -d '\"' -f 2", returnStdout: true).trim()
                            println "ERROR: No backup for date ${env.BACKUP_DATE}\nAvailable backups:\n${availBkps}"
                            sh "exit 1"
                        }
                    } else {
                        println "Get latest backup file for ${env.MASTER_ENV}"
                        env.RESTORE_FILE = sh (script: "set +x && curl -s ${env.GENERIC_URL}/${env.REPO_PATH}/ | grep ${env.MASTER_ENV}-bkp_ | tail -1 | cut -d '\"' -f 2", returnStdout: true).trim()
                    }
                    
                    // Change display name
                    currentBuild.displayName = "${env.MASTER_ENV}"
                }
            }
        }
        
        stage("Check system") {
            steps {
                script {
                    env.RESTORE_TYPE = "new"
                    if (env.MASTER_ENV == "PJD") {
                        env.SYSTEM_NAME = "portal-jenkins-develop.cwp.pnp-hcl.com"
                    }
                    if (env.MASTER_ENV == "PJDT") {
                        env.SYSTEM_NAME = "portal-jenkins-develop-test.team-q-dev.com"
                    }
                    if (env.NEW_SYSTEM_NAME != "") {
                        env.SYSTEM_NAME = env.NEW_SYSTEM_NAME
                    }
                    jenkinsResponse = ""
                    pingResponse = sh (script: "set +x && ping -c 1 -W 2 ${env.SYSTEM_NAME} & true", returnStdout: true).trim()
                    println "Ping response\n${pingResponse}"
                    if (pingResponse.contains(", 0% packet loss,")) {
                        if (env.RESTORE_OPTION == "target system offline") {
                            println "ERROR:  Restore not possible.\nREASON: Target system \"${env.SYSTEM_NAME}\" is still online.\n        Selected option: target system must be offline"
                            sh "exit 1"
                        } else {
                            jenkinsResponse = sh (script: "set +x && curl -f -s --insecure https://${env.SYSTEM_NAME}/login && echo \"SUCCESS\" || echo \"FAILED\"", returnStdout: true).trim()
                            if (jenkinsResponse.endsWith("FAILED")) {
                                jenkinsResponse = sh (script: "set +x && curl -f -s --insecure http://${env.SYSTEM_NAME}/login && echo \"SUCCESS\" || echo \"FAILED\"", returnStdout: true).trim()
                            }
                            println "Jenkins response\n${jenkinsResponse}"
                            if (jenkinsResponse.endsWith("SUCCESS")) {
                                if (env.RESTORE_OPTION == "Jenkins not responding") {
                                    println "ERROR:  Restore not possible.\nREASON: Target system \"${env.SYSTEM_NAME}\" has a running Jenkins instance.\n        Selected option: target system must have no Jenkins responding"
                                    sh "exit 1"
                                }
                            }
                            env.RESTORE_TYPE = "recreate"
                            if (env.RESTORE_OPTION == "force replace Jenkins") {
                                env.RESTORE_TYPE = "jenkins"
                                instanceIp = pingResponse.split(' ')[2]
                                instanceIp = instanceIp.replace("(", "")
                                instanceIp = instanceIp.replace(")", "")
                                // set variables for later use
                                env.INSTANCE_MASTER_IP = instanceIp
                                env.INSTANCE_NAME_MASTER = env.SYSTEM_NAME
                                println "INSTANCE_NAME_MASTER = ${env.INSTANCE_NAME_MASTER}\nINSTANCE_MASTER_IP = ${env.INSTANCE_MASTER_IP}"
                            }
                        }
                    }
                }
            }
        }
        
        stage("Prepare restore") {
            steps {
                script {
                    // Artifactory settings
                    env.REPO_FILE = "${env.REPO_PATH}/${env.RESTORE_FILE}"
                    // Default Jenkins admin
                    env.ADMIN_USER="admin"
                    // Jenkins version
                    env.JENKINS_VERSION = "centos"
                    
                    configFileProvider([
                        configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                    ]) {
                        dir("${workspace}/jenkins_master/terraform") {
                            sh """
                                cp $DEPLOY_KEY test-automation.pem
                                chmod 0400 test-automation.pem
                            """
                        }
                    }
                    
                    println "RESTORE_FILE = ${env.RESTORE_FILE}\nRESTORE_TYPE = ${env.RESTORE_TYPE}"
                    
                    dir("${workspace}/jenkins_master/backup_restore") {
                        sh """
                            sh ./downloadArtifactory.sh ${env.REPO_FILE} '.' ${env.GENERIC_URL}
                            ls -al
                        """
                    }
                }
            }
            
        }
        
        stage("Prepare terraform") {
            when {
                anyOf {
                    environment name: 'RESTORE_TYPE', value: 'new' 
                    environment name: 'RESTORE_TYPE', value: 'recreate' 
                }
            }
            steps {
                script {
                    // EC2 Instance settings
                    // Build user
                    env.INSTANCE_OWNER = dxJenkinsGetJobOwner()
                    env.INSTANCE_NAME_MASTER = env.SYSTEM_NAME
                    env.INSTANCE_TYPE_MASTER = "t2.xlarge"
                    // Terraform settings
                    env.TERRAFORM_ZIP = "terraform_0.12.20_linux_amd64.zip"
                    env.TERRAFORM_DOWNLOAD = "dx-build-prereqs/terraform/${env.TERRAFORM_ZIP}"
                    
                    dir("${workspace}/jenkins_master/terraform") {
                        sh """
                            curl -LJO "${env.GENERIC_URL}/${env.TERRAFORM_DOWNLOAD}"
                            unzip "${env.TERRAFORM_ZIP}"
                            chmod +x terraform
                            ./terraform --help
                            ls -lah
                        """
                        withCredentials([
                            usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID'),
                        ]) {
                            sh """
                                ./terraform init -backend-config="key=terraform-status/jenkinsmaster/${env.INSTANCE_NAME_MASTER}.key"
                            """
                        }
                    }
                }
            }
        }
        
        stage("Destroy EC2 instance") {
            when {
                environment name: 'RESTORE_TYPE', value: 'recreate'
            }
            steps {
                script {
                    println "Destroy exisitng EC2 instance ${env.SYSTEM_NAME}"
                    if (env.DRY_RUN == "no") {
                        withCredentials([
                            usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')
                        ]) {
                            dir("${workspace}/jenkins_master/terraform") {
                                sh """
                                    ./terraform destroy -auto-approve -var instance_name="${env.INSTANCE_NAME_MASTER}"
                                """
                            }    
                        }
                    } else {
                        println "DRY_RUN: Execution skipped!"
                    }
                }
            }
        }
        
        stage("Create new EC2 instance") {
            when {
                anyOf {
                    environment name: 'RESTORE_TYPE', value: 'new' 
                    environment name: 'RESTORE_TYPE', value: 'recreate' 
                }
            }
            steps {
                script {
                    println "Creating new EC2 instance ${env.SYSTEM_NAME}"
                    if (env.DRY_RUN == "no") {
                        withCredentials([
                            usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID'),
                        ]) {
                            dir("${workspace}/jenkins_master/terraform") {
                                sh """
                                    ./terraform apply -auto-approve -var instance_name="${env.INSTANCE_NAME_MASTER}" -var instance_owner="${INSTANCE_OWNER}" -var aws_ec2_instance_type="${env.INSTANCE_TYPE_MASTER}"
                                """
                                // use terraform show to get all information about the instance for later use
                                def instanceId = ""
                                def instanceIp = ""
                                def instanceInformation = sh(script: """
                                                              ./terraform show -json
                                                          """, returnStdout: true).trim()
                                // extract private ip and id of created instance
                                // using string methods since private_ip isn't always in instanceJsonInformation['values']['root_module']['resources'][0]
                                if (instanceInformation.contains("\"address\":\"aws_instance.jenkins-master\"")) {
                                    idx = instanceInformation.indexOf("\"address\":\"aws_instance.jenkins-master\"")
                                    addressInformation = instanceInformation.substring(idx)
                                    idx = addressInformation.indexOf("\"id\":")
                                    instanceId = addressInformation.substring(idx+6)
                                    instanceId = instanceId.split('"')[0];
                                    idx = addressInformation.indexOf("\"private_ip\":")
                                    instanceIp = addressInformation.substring(idx+14)
                                    instanceIp = instanceIp.split('"')[0];
                                }
                                if (instanceIp == "") {
                                   echo "ERROR: Something went wrong during EC2 startup.\n       Could not find [address:aws_instance.jenkins-master, entry."
                                   echo "instanceInformation = ${instanceInformation}"
                                   sh "exit 1"
                                } else {
                                   echo "Instance ${instanceId} running on ${instanceIp}."
                                   // set instanceIp, instanceId as variable for later use
                                   env.INSTANCE_MASTER_IP = instanceIp
                                   env.INSTANCE_MASTER_ID = instanceId
                                   println "INSTANCE_NAME_MASTER = ${env.INSTANCE_NAME_MASTER}\nINSTANCE_OWNER = ${env.INSTANCE_OWNER}\nINSTANCE_MASTER_IP = ${env.INSTANCE_MASTER_IP}\nINSTANCE_MASTER_ID = ${env.INSTANCE_MASTER_ID}"
                                }                              
                            }
                        }
                    } else {
                        println "DRY_RUN: Execution skipped!"
                    }
                }
            }
        }
        
        stage('Install Jenkins base') {
            when {
                anyOf {
                    environment name: 'RESTORE_TYPE', value: 'new' 
                    environment name: 'RESTORE_TYPE', value: 'recreate' 
                }
            }
            steps {
                dir("${workspace}/jenkins_master/terraform") {
                    sh """
                        ssh -i test-automation.pem -o StrictHostKeyChecking=no \
                        centos@${env.INSTANCE_MASTER_IP} '(cd ~/setupscripts && \
                        sudo sh 02-install-jenkins.sh ${env.JENKINS_VERSION} ${env.ADMIN_USER})'
                    """
                }
            }
        }
        
        stage('Copy backup tar') {
            steps {
                dir("${workspace}/jenkins_master/terraform") {
                    sh """
                        ssh -i test-automation.pem -o StrictHostKeyChecking=no centos@${env.INSTANCE_MASTER_IP} '(mkdir -p ~/setupscripts/helpers)'
                        scp -i test-automation.pem -o StrictHostKeyChecking=no ${workspace}/jenkins_master/backup_restore/${env.RESTORE_FILE} centos@${env.INSTANCE_MASTER_IP}:~/setupscripts/${env.RESTORE_FILE}
                        scp -i test-automation.pem -o StrictHostKeyChecking=no ${workspace}/jenkins_master/backup_restore/restoreJenkins.sh centos@${env.INSTANCE_MASTER_IP}:~/setupscripts/helpers/restoreJenkins.sh
                        scp -i test-automation.pem -o StrictHostKeyChecking=no ${workspace}/jenkins_master/backup_restore/addCronJob.sh centos@${env.INSTANCE_MASTER_IP}:~/setupscripts/helpers/addCronJob.sh
                        scp -i test-automation.pem -o StrictHostKeyChecking=no ${workspace}/jenkins_master/setupscripts/helpers/runRootJob.sh centos@${env.INSTANCE_MASTER_IP}:~/setupscripts/helpers/runRootJob.sh
                        rm -f ${workspace}/jenkins_master/backup_restore/${env.RESTORE_FILE}
                    """
                }
            }
        }
        
        stage('Restore backup on target') {
            steps {
                dir("${workspace}/jenkins_master/terraform") {
                    sh """
                        ssh -i test-automation.pem -o StrictHostKeyChecking=no centos@${env.INSTANCE_MASTER_IP} '(cd ~/setupscripts && sudo sh helpers/restoreJenkins.sh ${env.RESTORE_FILE} helpers/runRootJob.sh)'
                    """
                }
            }
        }
        
    }
    
    post {
        cleanup {
            script {
                if (env.INSTANCE_MASTER_IP) {
                    dir("${workspace}/jenkins_master/terraform") {
                        sh """
                            ssh -i test-automation.pem -o StrictHostKeyChecking=no \
                            centos@${env.INSTANCE_MASTER_IP} '(sudo rm -fR ~/setupscripts && \
                            sudo rm -fR /tmp/install_jenkins)'
                        """
                    }
                    sh(script: """
                        ssh-keygen -R ${env.INSTANCE_MASTER_IP} | true
                        rm -f ~/.ssh/known_hosts.old
                    """)
                }
                
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
