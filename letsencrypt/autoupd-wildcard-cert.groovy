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
                    // Bae maintenance job
                    if (!env.MAINT_JOB){
                        env.MAINT_JOB = 'housekeeping/maintain_single_wildcard_cert'
                    }
                }
            }
        }
        
        stage('Check/Update apps.dx-cluster-dev.hcl-dx-dev.net') {
            steps {
                script {
                    build job:env.MAINT_JOB , parameters:[string(name: 'CERT_DOMAIN',value: 'apps.dx-cluster-dev.hcl-dx-dev.net')] , wait: true
                }
            }
        }

        stage('Check/Update apps.hcl-dx-dev.net') {
            steps {
                script {
                    build job:env.MAINT_JOB , parameters:[string(name: 'CERT_DOMAIN',value: 'apps.hcl-dx-dev.net')] , wait: true
                }
            }
        }
        
        stage('Check/Update apps.hcl-dxdev.hcl-dx-dev.net') {
            steps {
                script {
                    build job:env.MAINT_JOB , parameters:[string(name: 'CERT_DOMAIN',value: 'apps.hcl-dxdev.hcl-dx-dev.net')] , wait: true
                }
            }
        }
        
        stage('Check/Update hcl-dx-dev.net') {
            steps {
                script {
                    build job:env.MAINT_JOB , parameters:[string(name: 'CERT_DOMAIN',value: 'hcl-dx-dev.net')] , wait: true
                }
            }
        }
        
        stage('Check/Update hcl-dxdev.hcl-dx-dev.net') {
            steps {
                script {
                    build job:env.MAINT_JOB , parameters:[string(name: 'CERT_DOMAIN',value: 'hcl-dxdev.hcl-dx-dev.net')] , wait: true
                }
            }
        }
        
        stage('Check/Update team-q-dev.com') {
            steps {
                script {
                    build job:env.MAINT_JOB , parameters:[string(name: 'CERT_DOMAIN',value: 'team-q-dev.com')] , wait: true
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
