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

/* Global definitions for using common Jenkins library */
def commonJenkinsLib
def commonLibFile = "./jenkins_common/common-lib.gvy"

pipeline {
    agent {
        label 'build_infra'
    }

    stages {
        /*
         * Load modules and configuration from the different flavours using "load"
         * Use one module and load the fitting flavour
         */
        stage("Load modules and configuration") {
            steps {
                script {
                    commonJenkinsLib = load "${commonLibFile}" 
                    commonJenkinsLib.messageFromJenkinsCommonLib("Hello from common Jenkins lib")
                }
            }
        }
        
        stage("Check Route53") {
            steps {
                script {
                    commonJenkinsLib.checkDeleteRoute53_Record(env.AWS_ZONE, env.EC2_INSTANCE)
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