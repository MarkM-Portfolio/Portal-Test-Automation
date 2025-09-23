/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2022. All Rights Reserved.       *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

// Import the shared library with the name configured in Jenkins
@Library("dx-shared-library") _

pipeline { 
    
    agent {
        node {
            label "build_infra"
        }
    }
    
    stages {
        stage("Prepare backup") {
            steps {
                script {
                    withCredentials([
                        usernamePassword(credentialsId: 'artifactory', passwordVariable: 'ARTIFACTORY_PASSWORD', usernameVariable: 'ARTIFACTORY_USER'),
                    ]) {
                        FILE_PREFIX = "UNKNOWN"
                        if (env.JOB_URL.contains("/portal-jenkins-test.cwp")) {
                            FILE_PREFIX = "PJT"
                        }
                        if (env.JOB_URL.contains("/portal-jenkins-develop.cwp")) {
                            FILE_PREFIX = "PJD"
                        }
                        if (env.JOB_URL.contains("/portal-jenkins-staging.cwp")) {
                            FILE_PREFIX = "PJS"
                        }
                        MAX_BKP = "5"
                        UPLOAD_PATH = "/dx-jenkins-backup/"
                        GENERIC_URL = "https://${G_ARTIFACTORY_HOST}/artifactory/${G_ARTIFACTORY_GENERIC_NAME}"
                        CREDS = "${ARTIFACTORY_USER}:${ARTIFACTORY_PASSWORD}"
                        ROOT_JOB = "fullBackupJenkins.sh"
                        MASTER_DIR = env.JOB_NAME.replaceAll("/","/jobs/")
                        JOB_DIR = "\$JENKINS_HOME/jobs/${MASTER_DIR}/builds/${env.BUILD_NUMBER}/archive"
                        dir("${workspace}/jenkins_master/backup_restore") {
                            sh """
                                sed -i.bak 's,\$FILE_PREFIX,${FILE_PREFIX},g' ./${ROOT_JOB}
                                sed -i.bak 's,\$UPLOAD_PATH,${UPLOAD_PATH},g' ./${ROOT_JOB}
                                sed -i.bak 's,\$GENERIC_URL,${GENERIC_URL},g' ./${ROOT_JOB}
                                sed -i.bak 's,\$CREDS,${CREDS},g' ./${ROOT_JOB}
                                sed -i.bak 's,\$MAX_BKP,${MAX_BKP},g' ./${ROOT_JOB}
                                sed -i.bak 's,\$ROOT_JOB,${JOB_DIR}/${ROOT_JOB},g' ./runAsRoot.job
                                rm ./*.bak
                                cat ./runAsRoot.job
                            """
                        }
                        sh """
                            mv jenkins_master/backup_restore/fullBackupJenkins.sh .
                            mv jenkins_master/backup_restore/runAsRoot.job .
                        """
                    }
                }
            }
        }
    }
    
    post {
        always {
            archiveArtifacts artifacts: 'fullBackupJenkins.sh', onlyIfSuccessful: true
            archiveArtifacts artifacts: 'runAsRoot.job', onlyIfSuccessful: true
        }
        
        cleanup {
            dxWorkspaceDirectoriesCleanup()
        }
    }
}
