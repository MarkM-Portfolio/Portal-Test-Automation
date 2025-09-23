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
        label 'build_dxcore'
    }
    
    /*
     * Preparing all settings we might need, using defaults if no override happens through jenkins params
     */
    stages {
        stage('Prepare pipeline') {
            steps {
                script {
                    // Domain settings
                    if (!env.CERT_DOMAIN){
                        env.CERT_DOMAIN = 'team-q-dev.com'
                    }
                    
                    // Credential settings
                    if (!env.AWS_CREDENTIALS_ID){
                        env.AWS_CREDENTIALS_ID = 'aws_credentials'
                    }
                    if (!env.ARTIFACTORY_CREDENTIALS){
                        env.ARTIFACTORY_CREDENTIALS = 'artifactory'
                    }
                    
                    // Staging settings, if set to yes use Let's Encrypt staging (test) environment
                    if (env.LE_TEST == "yes"){
                        env.LE_STAGING = '--staging'
                    } else {
                        env.LE_STAGING = ""
                    }
                    
                    // Set maximum renewal time left
                    if (!env.MAX_CERT_RENEW_LEFT){
                        env.MAX_CERT_RENEW_LEFT = '10'
                    }
                    
                    // Work settings
                    env.CERT_HOME = "${workspace}/letsencrypt/certs"
                    env.ARTIFACTORY_URL = "https://$G_ARTIFACTORY_HOST/artifactory/$G_ARTIFACTORY_GENERIC_NAME/dx-truststore"
                    
                    sh "mkdir -p ${env.CERT_HOME}"
                    dir("${env.CERT_HOME}") {
                        getCertConf = sh (script: "set +x && curl -s ${env.ARTIFACTORY_URL}/${env.CERT_DOMAIN}/${env.CERT_DOMAIN}.conf", returnStdout: true).trim()
                        env.NEW_CERT = "no"
                        env.UPD_CERT = "no"
                        env.ADD_CERT = "no"
                        env.BKP_CERT = "no"
                        env.INST_ACMESH = "yes"
                        if (getCertConf.contains("File not found.")) {
                           env.NEW_CERT = "yes"
                           if (env.LE_TEST != "yes") {
                              env.BKP_CERT = "yes"
                           }
                           echo "No certificate for ${env.CERT_DOMAIN} found in truststore."
                           echo "Get new certificate."
                        } else {
                           nextRenewTime = getCertConf.split('Le_NextRenewTime=')[1]
                           nextRenewTime = nextRenewTime.split("'")[1]
                           curTime = sh (script: "set +x && echo \$(date +'%s')", returnStdout: true).trim()
                           leftDays = (nextRenewTime.toInteger() - curTime.toInteger())/86400
                           if (leftDays < env.MAX_CERT_RENEW_LEFT.toInteger()) {
                              env.UPD_CERT = "yes"
                              env.BKP_CERT = "yes"
                              echo "Less than ${env.MAX_CERT_RENEW_LEFT} days left to renew certificate for ${env.CERT_DOMAIN}."
                              echo "Renewing certificate now."
                           } else {
                              env.INST_ACMESH = "no"
                              echo "More than ${env.MAX_CERT_RENEW_LEFT} days left to renew certificate for ${env.CERT_DOMAIN}."
                              echo "No action required."
                           }
                        }
                    }
                }
            }
        }
        
        /*
         *  This installs the acmesh client to interact with Let's Encrypt
         */
        stage('Install acmesh') {
            when {
                environment name: 'INST_ACMESH', value: 'yes'
            }
            steps {
                script {
                    dir("${workspace}/letsencrypt/common-scripts") {
                        sh """
                           sh C2-add-acmesh.sh
                        """
                    }
                }
            }
        }
        
        /*
         *  We will get the Let's Encrypt wildcard certificate
         */
        stage('Get new wildcard certificate') {
            when {
                environment name: 'NEW_CERT', value: 'yes'
            }
            steps {
                script {
                    withCredentials([
                        usernamePassword(credentialsId: "${env.AWS_CREDENTIALS_ID}", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID'),
                    ]) {
                        dir("${workspace}/letsencrypt/le-wildcard-scripts") {
                            sh """
                               sh L1-get-lewc_cert.sh -d ${env.CERT_DOMAIN} --access-key ${AWS_ACCESS_KEY_ID} --secret ${AWS_SECRET_ACCESS_KEY} --cert-home ${env.CERT_HOME} ${env.LE_STAGING}
                               ls -al ${env.CERT_HOME}/${env.CERT_DOMAIN}
                            """
                        }
                    }
                }
            }
        }
        
        /*
         *  We will update the Let's Encrypt wildcard certificate
         */
        stage('Update wildcard certificate') {
            when {
                environment name: 'UPD_CERT', value: 'yes'
            }
            steps {
                script {
                    withCredentials([
                        usernamePassword(credentialsId: "${env.AWS_CREDENTIALS_ID}", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID'),
                    ]) {
                        dir("${workspace}/letsencrypt/le-wildcard-scripts") {
                            sh """
                               sh L2-download-lewc_cert.sh ${env.ARTIFACTORY_URL} ${env.CERT_DOMAIN} ${env.CERT_HOME}
                               sh L3-update-lewc_cert.sh -d ${env.CERT_DOMAIN} --access-key ${AWS_ACCESS_KEY_ID} --secret ${AWS_SECRET_ACCESS_KEY} --cert-home ${env.CERT_HOME} ${env.LE_STAGING}
                            """
                        }
                    }
                }
            }
        }
        
        /*
         *  We will add the Let's Encrypt certificate to Jenkins store
         */
        stage('Add/Update LE certificate in Jenkins') {
            when {
                environment name: 'ADD_CERT', value: 'yes'
            }
            steps {
                script {
                    withCredentials([
                        usernamePassword(credentialsId: "${env.AWS_CREDENTIALS_ID}", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID'),
                    ]) {
                        dir("${workspace}/letsencrypt/le-wildcard-scripts") {
                            sh """
                               echo sh L3-add-lewc_cert.sh -d ${env.CERT_DOMAIN} --access-key ${AWS_ACCESS_KEY_ID} --secret ${AWS_SECRET_ACCESS_KEY} ${env.LE_STAGING}
                            """
                        }
                    }
                }
            }
        }
        
        /*
         *  We will backup the Let's Encrypt wildcard certificate
         */
        stage('Add/Update LE certificate in Artifactory') {
            when {
                environment name: 'BKP_CERT', value: 'yes'
            }
            steps {
                script {
                    withCredentials([
                        usernamePassword(credentialsId: "${ARTIFACTORY_CREDENTIALS}", passwordVariable: 'ARTIFACTORY_PASSWORD', usernameVariable: 'ARTIFACTORY_USER')
                    ]) {
                        dir("${workspace}/letsencrypt/common-scripts") {
                            sh """
                               sh C5-backup-cert.sh ${env.CERT_HOME} ${env.CERT_DOMAIN} ${env.ARTIFACTORY_URL} ${ARTIFACTORY_USER} ${ARTIFACTORY_PASSWORD}
                            """
                        }
                    }
                }
            }
        }

    }
    
    post {
    
        cleanup {
            script {
                /* Uninstall acmesh */
                if (INST_ACMESH == "yes") {
                    dir("${workspace}/letsencrypt/common-scripts") {
                        sh "sh X1-remove-acmesh.sh"
                    }
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
