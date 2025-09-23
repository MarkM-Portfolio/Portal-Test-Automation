/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2023. All Rights Reserved.       *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

import java.text.SimpleDateFormat
@Library("dx-shared-library") _

/*
* Sets the default branch to pull for running the tests. If no branch is provided branch will be set to develop.
*/
if (!env.TARGET_BRANCH) {
    env.TARGET_BRANCH = 'develop'
}

def pipelineParameters = [:]
def randomNo = new Random().nextInt(100000)

pipeline { 
    options {
      timeout(time: 120, unit: 'MINUTES') 
    }    
    
    agent {
        label 'build_infra'    
    }
    
    stages {
        stage('Prepare Environment') {
            steps {
                dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/dx-cw-profile/parameters.yaml")
            }
        }
       
        stage('Deploying the application') {
            steps {
                script {
                    buildParameters = []
                    buildParameters.add(string(name: 'INSTANCE_NAME', value: "cw-profile-tests-${randomNo}"))
                    buildParameters.add(string(name: 'NAMESPACE', value: pipelineParameters.NATIVE_KUBE_NAMESPACE))
                    buildParameters.add(string(name: 'DOMAIN_SUFFIX', value: pipelineParameters.DOMAIN_SUFFIX))
                    buildParameters.add(string(name: 'HOSTED_ZONE', value: "Z3OEC7SLEHQ2P3"))
                    buildParameters.add(string(name: 'BUILD_USER_ID', value: 'jeremiahsteph.aquin@hcl.com'))
                    buildParameters.add(string(name: 'NEXT_JOB_DELAY_HOURS', value: '0'))
                    buildParameters.add(string(name: 'DX_CORE_REPLICAS', value: '1'))

                    buildParameters.add(booleanParam(name: 'DISABLE_DESIGN_STUDIO', value: "true"))
                    buildParameters.add(booleanParam(name: 'DISABLE_REMOTESEARCH', value: "true"))
                    buildParameters.add(booleanParam(name: 'DISABLE_CONTENTCOMPOSER', value: "true"))
                    buildParameters.add(booleanParam(name: 'DISABLE_DAM', value: "true"))
                    buildParameters.add(booleanParam(name: 'DISABLE_KALTURA_PLUGIN', value: "true"))
                    buildParameters.add(booleanParam(name: 'DISABLE_RINGAPI', value: "false"))
                    buildParameters.add(booleanParam(name: 'DISABLE_PERSISTENCE', value: "true"))
                    buildParameters.add(booleanParam(name: 'DISABLE_PLUGIN_GOOGLE_VISION', value: "true"))
                    buildParameters.add(booleanParam(name: 'PERFORMANCE_RUN', value: "false"))
                    buildParameters.add(booleanParam(name: 'DISABLE_IMAGEPROCESSOR', value: "true"))
                    buildParameters.add(booleanParam(name: 'DISABLE_AMBASSADOR', value: "false"))
                    buildParameters.add(booleanParam(name: 'DISABLE_RUNTIME_CONTROLLER', value:"false"))
                    buildParameters.add(booleanParam(name: 'DISABLE_HAPROXY', value: "false"))
                    buildParameters.add(booleanParam(name: 'DISABLE_RUNTIME_CONTROLLER', value: "false"))
                    buildParameters.add(booleanParam(name: 'DISABLE_OPENLDAP', value: "true"))
                    buildParameters.add(booleanParam(name: 'ENABLE_DB_CONFIG', value: "true"))                
                    //There is no schedule required for these machines
                    buildParameters.add(string(name: 'NATIVE_POPO_SCHEDULE', value: 'n/a'))
                    
                    build(job: "${pipelineParameters.KUBE_DEPLOY_JOB}", 
                        parameters: buildParameters, 
                        propagate: true,
                        wait: true)
                }
            }
        }

        stage('Configure remote kubectl') {
            steps {
                dxKubectlNativeKubeConfig(sshTarget: "cw-profile-tests-${randomNo}.team-q-dev.com")
                sh "echo \$KUBECONFIG"
            }
        }

        // Use kubectl to check remote resources
        stage('Use remote kubectl') {
            steps {
                dxKubectlWorkspaceInstall()
                sh "kubectl get namespaces"
            }
        }

        // This stage test backwards compatibility of cw_profile credentials
        stage('Check Backward Compatility of Credentials') {
            steps {
                dxHelmWorkspaceInstall()
                dxKubectlWorkspaceInstall()
                dir("${workspace}/kube/lifecycle/scripts/common") {
                    configFileProvider([
                        configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                    ]) {
                        script {
                            //Extract PEM file
                            sh """
                                cp $DEPLOY_KEY test-automation-deployments.pem
                                chmod 0600 test-automation-deployments.pem
                            """

                            //Verify Core login Credentials
                            final String corePreLoginResponse = sh(script: "curl -s -L -D - -o /dev/null -d 'j_username=wpsadmin&j_password=wpsadmin&action=Log+in' -X POST https://cw-profile-tests-${randomNo}.team-q-dev.com/ibm/console/j_security_check | grep -o -i logonError | wc -l", returnStdout: true).trim()
                            echo "corePreLoginResponse code: ${corePreLoginResponse}"
                            if ("${corePreLoginResponse}" != "0") {
                                error("Failed Core credential authentication.")
                            }

                            //Update Core password credential
                            sh """
                                kubectl cp ${env.WORKSPACE}/dx-cw-profile/helpers/updateUser.py -n ${pipelineParameters.NATIVE_KUBE_NAMESPACE} dx-deployment-core-0:/opt/app/updateUser.py -c core
                                kubectl exec -n ${pipelineParameters.NATIVE_KUBE_NAMESPACE} dx-deployment-core-0 -c core -- /bin/bash -c "chmod +x /opt/app/"
                                kubectl exec -n ${pipelineParameters.NATIVE_KUBE_NAMESPACE} dx-deployment-core-0 -c core -- /bin/bash -c "/opt/HCL/wp_profile/bin/wsadmin.sh -connType SOAP -username wpsadmin -password wpsadmin -lang jython -f /opt/app/updateUser.py"
                            """

                            //Verify Core login credentials after update
                            final String coreUpdatedLoginResponse = sh(script: "curl -s -L -D - -o /dev/null -d 'j_username=wpsadmin&j_password=testpassword&action=Log+in' -X POST https://cw-profile-tests-${randomNo}.team-q-dev.com/ibm/console/j_security_check | grep -o -i logonError | wc -l", returnStdout: true).trim()
                            echo "coreUpdatedLoginResponse code: ${coreUpdatedLoginResponse}"
                            if ("${coreUpdatedLoginResponse}" != "0") {
                                error("Failed Updated Core credential authentication.")
                            }

                            // Remove existing profile to test the cw_profile persistence on the first upgrade
                            sh """
                                kubectl exec -n dxns dx-deployment-core-0 -c core -- /bin/bash -c "rm -rf /opt/HCL/profiles/cw_prof"
                            """

                            // check pods for readiness
                            dxPodsCheckReadiness(
                                namespace: pipelineParameters.NATIVE_KUBE_NAMESPACE,
                                lookupInterval: 90,
                                lookupTries: 180,
                                pendingLimit: 15,
                                containerCreateLimit: 15,
                                safetyInterval: 60
                            )
                        }
                      }
                }
            }
        }

        // This stage test backwards compatibility of cw_profile credentials
        stage('Upgrade Deployment to Test Backward Compatility of Credentials') {
            steps {
                dxHelmWorkspaceInstall()
                dxKubectlWorkspaceInstall()
                dir("${workspace}/kube/lifecycle/scripts/common") {
                    configFileProvider([
                        configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                    ]) {
                        script {
                            //Extract PEM file
                            sh """
                                cp $DEPLOY_KEY test-automation-deployments.pem
                                chmod 0600 test-automation-deployments.pem
                            """

                            //Upgrade deployment with updated core credentials
                            sh  """
                                ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@cw-profile-tests-${randomNo}.team-q-dev.com \
                                '(cd /home/centos/native-kube/helm/hcl-dx-deployment && helm upgrade dx-deployment -n ${pipelineParameters.NATIVE_KUBE_NAMESPACE} . --set security.core.wasUser='wpsadmin' --set security.core.wasPassword='testpassword' --set security.core.customWasSecret='' --set security.core.wpsUser='wpsadmin' --set security.core.wpsPassword='testpassword' --set security.core.customWpsSecret='' --set security.core.configWizardUser='wpsadmin' --set security.core.configWizardPassword='testpassword' --set security.core.customConfigWizardSecret='' -f ./deploy-values.yaml)'
                            """

                            // check pods for readiness
                            dxPodsCheckReadiness(
                                namespace: pipelineParameters.NATIVE_KUBE_NAMESPACE,
                                lookupInterval: 90,
                                lookupTries: 180,
                                pendingLimit: 15,
                                containerCreateLimit: 15,
                                safetyInterval: 60
                            )
                        }
                      }
                }
            }
        }

        // This stage test backwards compatibility of cw_profile credentials
        stage('Verify Backward Compatility of Credentials') {
            steps {
                dxHelmWorkspaceInstall()
                dxKubectlWorkspaceInstall()
                dir("${workspace}/kube/lifecycle/scripts/common") {
                    configFileProvider([
                        configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                    ]) {
                        script {
                            //Extract PEM file
                            sh """
                                cp $DEPLOY_KEY test-automation-deployments.pem
                                chmod 0600 test-automation-deployments.pem
                            """

                            //Verify Core login credentials after upgrade
                            final String coreLoginResponse = sh(script: "curl -s -L -D - -o /dev/null -d 'j_username=wpsadmin&j_password=testpassword&action=Log+in' -X POST https://cw-profile-tests-${randomNo}.team-q-dev.com/ibm/console/j_security_check | grep -o -i logonError | wc -l", returnStdout: true).trim()
                            echo "coreLoginResponse code: ${coreLoginResponse}"
                            if ("${coreLoginResponse}" != "0") {
                                error("Failed Core credential authentication.")
                            }

                            //Verify CW login credentials after upgrade
                            final String cwLoginResponse = sh(script: "curl -s -L -D - -o /dev/null -d 'j_username=wpsadmin&j_password=testpassword&action=Log+in' -X POST https://cw-profile-tests-${randomNo}.team-q-dev.com:10203/ibm/console/j_security_check | grep -o -i logonError | wc -l", returnStdout: true).trim()
                            echo "cwLoginResponse code: ${cwLoginResponse}"
                            if ("${cwLoginResponse}" != "0") {
                                error("Failed CW credential authentication.")
                            }

                            // check pods for readiness
                            dxPodsCheckReadiness(
                                namespace: pipelineParameters.NATIVE_KUBE_NAMESPACE,
                                lookupInterval: 90,
                                lookupTries: 180,
                                pendingLimit: 15,
                                containerCreateLimit: 15,
                                safetyInterval: 60
                            )
                        }
                      }
                }
            }
        }

        // This stage test cw_profile credential updates
        stage('Update CW Registry') {
            steps {
                dxHelmWorkspaceInstall()
                dxKubectlWorkspaceInstall()
                dir("${workspace}/kube/lifecycle/scripts/common") {
                    configFileProvider([
                        configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                    ]) {
                        script {
                            //Extract PEM file
                            sh """
                                cp $DEPLOY_KEY test-automation-deployments.pem
                                chmod 0600 test-automation-deployments.pem
                            """

                            //Update cw_profile credentials to test profile persistence
                            sh  """
                                ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@cw-profile-tests-${randomNo}.team-q-dev.com \
                                '(cd /home/centos/native-kube/helm/hcl-dx-deployment && helm upgrade dx-deployment -n ${pipelineParameters.NATIVE_KUBE_NAMESPACE} . --set security.core.wasUser='wpsadmin' --set security.core.wasPassword='testpassword' --set security.core.customWasSecret='' --set security.core.wpsUser='wpsadmin' --set security.core.wpsPassword='testpassword' --set security.core.customWpsSecret='' --set security.core.configWizardUser='wpsadmin' --set security.core.configWizardPassword='cwtestpassword' --set security.core.customConfigWizardSecret='' -f ./deploy-values.yaml)'
                            """

                            // check pods for readiness
                            dxPodsCheckReadiness(
                                namespace: pipelineParameters.NATIVE_KUBE_NAMESPACE,
                                lookupInterval: 90,
                                lookupTries: 180,
                                pendingLimit: 15,
                                containerCreateLimit: 15,
                                safetyInterval: 60
                            )
                        }
                      }
                }
            }
        }

        // This stage test cw_profile credential updates
        stage('Verify CW Registry') {
            steps {
                dxHelmWorkspaceInstall()
                dxKubectlWorkspaceInstall()
                dir("${workspace}/kube/lifecycle/scripts/common") {
                    configFileProvider([
                        configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                    ]) {
                        script {
                            //Extract PEM file
                            sh """
                                cp $DEPLOY_KEY test-automation-deployments.pem
                                chmod 0600 test-automation-deployments.pem
                            """

                            //Verify New CW login credentials after registry update if its persisted with the profile
                            final String newCwLoginResponse = sh(script: "curl -s -L -D - -o /dev/null -d 'j_username=wpsadmin&j_password=cwtestpassword&action=Log+in' -X POST https://cw-profile-tests-${randomNo}.team-q-dev.com:10203/ibm/console/j_security_check | grep -o -i logonError | wc -l", returnStdout: true).trim()
                            echo "newCwLoginResponse code: ${newCwLoginResponse}"
                            if ("${newCwLoginResponse}" != "0") {
                                error("Failed new CW credential authentication.")
                            }

                            // check pods for readiness
                            dxPodsCheckReadiness(
                                namespace: pipelineParameters.NATIVE_KUBE_NAMESPACE,
                                lookupInterval: 90,
                                lookupTries: 180,
                                pendingLimit: 15,
                                containerCreateLimit: 15,
                                safetyInterval: 60
                            )
                        }
                      }
                }
            }
        }

        // This stage reverts credentials to default for concurrency test
        stage('Revert Credentials') {
            steps {
                dxHelmWorkspaceInstall()
                dxKubectlWorkspaceInstall()
                dir("${workspace}/kube/lifecycle/scripts/common") {
                    configFileProvider([
                        configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                    ]) {
                        script {
                            //Extract PEM file
                            sh """
                                cp $DEPLOY_KEY test-automation-deployments.pem
                                chmod 0600 test-automation-deployments.pem
                            """

                            //Revering cw_profile credentials
                            sh  """
                                ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@cw-profile-tests-${randomNo}.team-q-dev.com \
                                '(cd /home/centos/native-kube/helm/hcl-dx-deployment && helm upgrade dx-deployment -n ${pipelineParameters.NATIVE_KUBE_NAMESPACE} . --set security.core.wasUser='wpsadmin' --set security.core.wasPassword='wpsadmin' --set security.core.customWasSecret='' --set security.core.wpsUser='wpsadmin' --set security.core.wpsPassword='wpsadmin' --set security.core.customWpsSecret='' --set security.core.configWizardUser='wpsadmin' --set security.core.configWizardPassword='wpsadmin' --set security.core.customConfigWizardSecret='' -f ./deploy-values.yaml)'
                            """

                            // check pods for readiness
                            dxPodsCheckReadiness(
                                namespace: pipelineParameters.NATIVE_KUBE_NAMESPACE,
                                lookupInterval: 90,
                                lookupTries: 180,
                                pendingLimit: 15,
                                containerCreateLimit: 15,
                                safetyInterval: 60
                            )
                        }
                      }
                }
            }
        }
        
        // This stage verifies if the container profile and the profile in the volume is symbolically linked
        stage('Run to verify cw_profile symbolic link created to container.') {
            steps {
                dxKubectlWorkspaceInstall()
                sh "kubectl exec -n ${pipelineParameters.NATIVE_KUBE_NAMESPACE} --tty --stdin dx-deployment-core-0 -c core -- /bin/bash [ -d /opt/HCL/profiles/cw_prof ]  && echo 'Directory cw_prof exists in core container.'"
            }
        }

        // This stage verifies if the cw_profile is persisted in the persistent volume by running 2 core pods
        stage('Run to verify cw_profile with two core pods.') {
            steps {
                dxKubectlWorkspaceInstall()
                sh "kubectl scale StatefulSet/dx-deployment-core -n ${pipelineParameters.NATIVE_KUBE_NAMESPACE} --replicas=2"

                // check pods for readiness
                dxPodsCheckReadiness(
                    namespace: pipelineParameters.NATIVE_KUBE_NAMESPACE,
                    lookupInterval: 90,
                    lookupTries: 180,
                    pendingLimit: 15,
                    containerCreateLimit: 15,
                    safetyInterval: 60
                )               
            }
        }

        // This stage verifies if the cw_profile is persisted in the persistent volume by running 2 core pods
        stage('Run to verify cw_profile folder to be persisted into a persistent volume even after the pods restart') {
            steps {
                dxKubectlWorkspaceInstall()
                sh "kubectl delete pod/dx-deployment-core-0 -n ${pipelineParameters.NATIVE_KUBE_NAMESPACE} && kubectl delete pod/dx-deployment-core-1 -n ${pipelineParameters.NATIVE_KUBE_NAMESPACE}"

                dir("${workspace}/kube/lifecycle/scripts/common") {
                    configFileProvider([
                        configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                    ]) {
                        script {
                            try {
                                    /* Extract PEM file */
                                sh """
                                    cp $DEPLOY_KEY test-automation-deployments.pem
                                    chmod 0600 test-automation-deployments.pem
                                """

                                /* Run bash script to capture operations time */
                                sh  """
                                    ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@cw-profile-tests-${randomNo}.team-q-dev.com \
                                    '([ -d /home/centos/native-kube/volumes/rwxvol-core-profile-1/cw_prof ] || [ -d /home/centos/native-kube/volumes/rwxvol-core-profile-2/cw_prof ] || [ -d /home/centos/native-kube/volumes/rwxvol-core-profile-3/cw_prof ] || [ -d /home/centos/native-kube/volumes/rwxvol-core-profile-4/cw_prof ]  || [ -d /home/centos/native-kube/volumes/rwxvol-core-profile-5/cw_prof ]  && echo "Directory cw_prof exists in persistence volume.")'
                                """
                            } catch(Exception err) {
                                error('Failed to persist cw_profile folder into a persistent volume.')
                            }

                            // check pods for readiness
                            dxPodsCheckReadiness(
                                namespace: pipelineParameters.NATIVE_KUBE_NAMESPACE,
                                lookupInterval: 90,
                                lookupTries: 180,
                                pendingLimit: 15,
                                containerCreateLimit: 15,
                                safetyInterval: 60
                            )
                         }
                      }
                }
            }
        }

        // This stage verifies concurrency for DXClient Acceptance Test
        stage('Concurrency validation check dxclient Acceptance Tests') {
            steps {
                script {
                    buildParameters = []
                    buildParameters.add(string(name: 'PORTAL_HOST', value: "https://cw-profile-tests-${randomNo}.team-q-dev.com/wps/portal"))
                    buildParameters.add(string(name: 'EXP_API', value: "https://cw-profile-tests-${randomNo}.team-q-dev.com/dx/api/core/v1"))
                    buildParameters.add(string(name: 'APP_ENDPOINT', value: "https://cw-profile-tests-${randomNo}.team-q-dev.com/dx/ui/dam"))
                    buildParameters.add(string(name: 'WCMREST', value: "https://cw-profile-tests-${randomNo}.team-q-dev.com/wps"))
                    buildParameters.add(string(name: 'TARGET_BRANCH', value: pipelineParameters.TARGET_BRANCH))
                    buildParameters.add(string(name: 'DXCONNECT_HOST', value: "https://cw-profile-tests-${randomNo}.team-q-dev.com"))
                    buildParameters.add(string(name: 'IMAGE_PROCESSOR_API', value: "https://cw-profile-tests-${randomNo}.team-q-dev.com/dx/api/image-processor/v1"))
                    buildParameters.add(string(name: 'DAM_API', value: "https://cw-profile-tests-${randomNo}.team-q-dev.com/dx/api/dam/v1"))
                    buildParameters.add(booleanParam(name: 'TEST_SEARCH_SETUP', value: "false"))
                    buildParameters.add(booleanParam(name: 'TEST_DXCLIENT', value: "true"))
                    build(job: "${pipelineParameters.ACCEPTANCE_TEST_JOB}",
                          parameters: buildParameters,
                          propagate: true,
                          wait: true)
                }
            }
        }

    }

    post {
        cleanup {
            script {
                /*After a test execution, the instance is not required anymore and will be deleted.*/
                buildParameters = []
                buildParameters.add(string(name: 'INSTANCE_NAME', value: "cw-profile-tests-${randomNo}"))
                buildParameters.add(string(name: 'BUILD_USER_ID', value: 'jeremiahsteph.aquin@hcl.com'))
                buildParameters.add(string(name: 'DOMAIN_SUFFIX', value: pipelineParameters.DOMAIN_SUFFIX))
                buildParameters.add(string(name: 'HOSTED_ZONE', value: "Z3OEC7SLEHQ2P3"))
                build(job: "${pipelineParameters.KUBE_REMOVE_JOB}", 
                    parameters: buildParameters, 
                    propagate: true,
                    wait: true)
            }
        }
    }  
}
