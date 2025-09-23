/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2001, 2020. All Rights Reserved. *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

// Import the shared library with the name configured in Jenkins
@Library("dx-shared-library") _

/* Module and Config variables */
def commonConfig
def kubeModule

/* Common paths - must be here always */
moduleDirectory = "./kube/lifecycle/modules"
scriptDirectory = "./kube/lifecycle/scripts"
configDirectory = "./kube/lifecycle/config"

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
                    // Workaround for using shared library with the nested Groovy files
                    env.THIS_BUILD_OWNER = dxJenkinsGetJobOwner()
                    commonConfig = load "${configDirectory}/common.gvy"
                    kubeModule = load "${moduleDirectory}/${commonConfig.COMMON_KUBE_FLAVOUR}.gvy"
                }
                // Install Helm in the current workspace and add it to the PATH variable
                dxHelmWorkspaceInstall()
                // Install kubectl in the current workspace and add it to the PATH variable
                dxKubectlWorkspaceInstall()
            }
        }

        /*
         * Destroy the environment of the selected flavour 
         */
        stage("Destroy Environment") {
            steps {
                script {
                    kubeModule.destroyEnvironment()
                }
            }
        }
    }

    post {
        cleanup {
            dxWorkspaceDirectoriesCleanup()
        }
    }
}