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
    
    /*
     * Preparing all settings we might need, using defaults if no override happens through jenkins params
     */
    stages {
        stage('Prepare pipeline') {
            steps {
                script {
                    // Target system
                    if (!env.TARGET_SYSTEM){
                        println "ERROR: Target system missing!"
                        sh "exit 1"
                    }
                    
                    // Cert domain
                    if (!env.CERT_DOMAIN){
                        env.CERT_DOMAIN = 'team-q-dev.com'
                    }
                    
                    // ssh access key ID defined in Jenkins
                    if (!env.ACCESS_KEY_ID){
                        env.ACCESS_KEY_ID = 'test-automation-deployments'
                    }
                    
                    // Work settings
                    env.CERT_HOME = "${workspace}/letsencrypt/certs"
                    env.ARTIFACTORY_URL = "https://$G_ARTIFACTORY_HOST/artifactory/$G_ARTIFACTORY_GENERIC_NAME/dx-truststore"
                }
            }
        }
        
        /*
         *  Get the ssh key from Jenkins
         */
        stage('Get ssh access key') {
            steps {
                script {
                    dir("${workspace}/letsencrypt/le-wildcard-scripts") {
                        configFileProvider([
                            configFile(fileId: env.ACCESS_KEY_ID, variable: 'DEPLOY_KEY')
                        ]) {
                            // prepare terraform and execute terraform, use private key to access machine
                            sh(script: """
                                cp $DEPLOY_KEY test-automation.pem
                                chmod 400 test-automation.pem
                                ls -lah
                            """)                                
                        }
                    }
                }
            }
        }
        
        /*
         *  We will get the Let's Encrypt wildcard certificate
         */
        stage('Get certificate from truststore') {
            steps {
                script {
                    dir("${workspace}/letsencrypt/le-wildcard-scripts") {
                        sh """
                           mkdir -p ${env.CERT_HOME}
                           sh L2-download-lewc_cert.sh ${env.ARTIFACTORY_URL} ${env.CERT_DOMAIN} ${env.CERT_HOME}
                           ls -al ${env.CERT_HOME}/${env.CERT_DOMAIN}
                        """
                    }
                }
            }
        }
        
        /*
         *  We will copy the Let's Encrypt wildcard certificate on target system
         */
        stage('Copy certificate on target system') {
            steps {
                script {
                    dir("${workspace}/letsencrypt/le-wildcard-scripts") {
                        sh """
                           sh X2-copy-cert-on-target.sh ${env.TARGET_SYSTEM}.${env.CERT_DOMAIN} ${env.CERT_HOME}/${env.CERT_DOMAIN}
                        """
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
