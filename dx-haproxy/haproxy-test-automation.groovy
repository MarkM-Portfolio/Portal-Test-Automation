/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2022, 2023. All Rights Reserved. *
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
                dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/dx-haproxy/parameters.yaml")
            }
        }
       
        stage('Deploying the application') {
            steps {
                script {
                    buildParameters = []
                    buildParameters.add(string(name: 'INSTANCE_NAME', value: "haproxy-tests-${randomNo}"))
                    buildParameters.add(string(name: 'NAMESPACE', value: pipelineParameters.NATIVE_KUBE_NAMESPACE))
                    buildParameters.add(string(name: 'DOMAIN_SUFFIX', value: pipelineParameters.DOMAIN_SUFFIX))
                    buildParameters.add(string(name: 'HOSTED_ZONE', value: "Z3OEC7SLEHQ2P3"))
                    buildParameters.add(string(name: 'BUILD_USER_ID', value: 'alina.thomas@hcl.com'))
                    buildParameters.add(string(name: 'NEXT_JOB_DELAY_HOURS', value: '0'))
                    buildParameters.add(string(name: 'HAPROXY_IMAGE_FILTER', value: pipelineParameters.HAPROXY_IMAGE_FILTER))


                    buildParameters.add(booleanParam(name: 'DISABLE_DESIGN_STUDIO', value: "true"))
                    buildParameters.add(booleanParam(name: 'DISABLE_REMOTESEARCH', value: "true"))
                    buildParameters.add(booleanParam(name: 'DISABLE_CONTENTCOMPOSER', value: "false"))
                    buildParameters.add(booleanParam(name: 'DISABLE_DAM', value: "false"))
                    buildParameters.add(booleanParam(name: 'DISABLE_KALTURA_PLUGIN', value: "true"))
                    buildParameters.add(booleanParam(name: 'DISABLE_RINGAPI', value: "false"))
                    buildParameters.add(booleanParam(name: 'DISABLE_PERSISTENCE', value: "false"))
                    buildParameters.add(booleanParam(name: 'DISABLE_PLUGIN_GOOGLE_VISION', value: "true"))
                    buildParameters.add(booleanParam(name: 'PERFORMANCE_RUN', value: "false"))
                    buildParameters.add(booleanParam(name: 'DISABLE_IMAGEPROCESSOR', value: "false"))
                    buildParameters.add(booleanParam(name: 'DISABLE_AMBASSADOR', value: "false"))
                    buildParameters.add(booleanParam(name: 'DISABLE_RUNTIME_CONTROLLER', value:"false"))
                    buildParameters.add(booleanParam(name: 'DISABLE_HAPROXY', value: "false"))
                    buildParameters.add(booleanParam(name: 'DISABLE_RUNTIME_CONTROLLER', value: "false"))
                    buildParameters.add(booleanParam(name: 'DISABLE_OPENLDAP', value: "true"))
                    
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
                dxKubectlNativeKubeConfig(sshTarget: "haproxy-tests-${randomNo}.team-q-dev.com")
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

         stage('Use scale up replicas haproxy') {
            steps {
                dxKubectlWorkspaceInstall()
                sh "kubectl scale deployment/dx-deployment-haproxy -n dxns --replicas=3"
                script {
                    final String response = sh(script: "curl -o /dev/null -s -w %{http_code} https://haproxy-tests-${randomNo}.team-q-dev.com/dx/api/core/v1/explorer/index.html", returnStdout: true).trim()
                    echo "Response code: ${response}"
                    if ("${response}" != "200") {
                        error("Failed to verify scale up replicas haproxy with response code ${response}.")

                    }
                }
            }
        }

         stage('Use scale down replicas haproxy') {
            steps {
                dxKubectlWorkspaceInstall()
                sh "kubectl scale deployment/dx-deployment-haproxy -n dxns --replicas=1"
                script {
                    final String response = sh(script: "curl -o /dev/null -s -w %{http_code} https://haproxy-tests-${randomNo}.team-q-dev.com/dx/api/core/v1/explorer/index.html", returnStdout: true).trim()
                    echo "Response code: ${response}"
                    if ("${response}" != "200") {
                        error("Failed to verify scale down replicas haproxy with response code ${response}.")

                    }
                }
            }
        }

        stage('Run scripts to upgrade the SSL offloading is disabled') {
            steps {
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
                                    ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@haproxy-tests-${randomNo}.team-q-dev.com \
                                    '(cd /home/centos/native-kube && helm -n dxns --set networking.haproxy.ssl=false upgrade -f ./install-deploy-values.yaml dx-deployment ./install-hcl-dx-deployment)'
                                """
                            } catch(Exception err) {
                                error('Failed to helm upgrade the SSL offloading is disabled for HAProxy.')
                            }
                         }
                      }
                }
            }
        }

        stage('Verify the dx deployment is working with SSL offloading is disabled') {
            steps {
                script {
                    // check pods for readiness
                    dxPodsCheckReadiness(
                        namespace: pipelineParameters.NATIVE_KUBE_NAMESPACE,
                        lookupInterval: 90,
                        lookupTries: 180,
                        pendingLimit: 15,
                        containerCreateLimit: 15,
                        safetyInterval: 60
                    )
                    final String response = sh(script: "curl -o /dev/null -s -w %{http_code} http://haproxy-tests-${randomNo}.team-q-dev.com/dx/api/core/v1/explorer/index.html", returnStdout: true).trim()
                    echo "Response code: ${response}"
                    if ("${response}" != "200") {
                        error("Failed to verify the dx deployment is working with SSL offloading is disabled with response code ${response}.")
                    }
                }
            }
        }

        stage('Run scripts to upgrade the SSL offloading is enabled') {
            steps {
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
                                    ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@haproxy-tests-${randomNo}.team-q-dev.com \
                                    '(cd /home/centos/native-kube && helm -n dxns --set networking.haproxy.ssl=true upgrade -f ./install-deploy-values.yaml dx-deployment ./install-hcl-dx-deployment)'
                                """
                            } catch(Exception err) {
                                error('Failed to helm upgrade the SSL offloading is enabled for HAProxy.')
                            }
                         }
                      }
                }
            }
        }

        stage('Verify the dx deployment is working with SSL offloading is enabled') {
            steps {
                script {
                    // check pods for readiness
                    dxPodsCheckReadiness(
                        namespace: pipelineParameters.NATIVE_KUBE_NAMESPACE,
                        lookupInterval: 90,
                        lookupTries: 180,
                        pendingLimit: 15,
                        containerCreateLimit: 15,
                        safetyInterval: 60
                    )
                    final String response = sh(script: "curl -o /dev/null -s -w %{http_code} https://haproxy-tests-${randomNo}.team-q-dev.com/dx/api/core/v1/explorer/index.html", returnStdout: true).trim()
                    echo "Response code: ${response}"
                    if ("${response}" != "200") {
                        error("Failed to verify the dx deployment is working with SSL offloading is enabled with response code ${response}.")
                    }
                }
            }
        }

        stage('Run scripts to upgrade the service type to ClusterIP') {
            steps {
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
                                    ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@haproxy-tests-${randomNo}.team-q-dev.com \
                                    '(cd /home/centos/native-kube && helm -n dxns --set networking.haproxy.serviceType=ClusterIP upgrade -f ./install-deploy-values.yaml dx-deployment ./install-hcl-dx-deployment)'
                                """
                            } catch(Exception err) {
                               error('Failed to helm upgrade the service type to ClusterIP for HAProxy.')
                            }
                         }
                      }
                }
            }
        }

        stage('Verify the dx deployment not reachable to external user') {
            steps {
                script {
                    // check pods for readiness
                    dxPodsCheckReadiness(
                        namespace: pipelineParameters.NATIVE_KUBE_NAMESPACE,
                        lookupInterval: 90,
                        lookupTries: 180,
                        pendingLimit: 15,
                        containerCreateLimit: 15,
                        safetyInterval: 60
                    )
                    final String response = sh(script: "curl https://haproxy-tests-${randomNo}.team-q-dev.com/dx/api/core/v1 -k -s -f -o /dev/null && echo SUCCESS || echo ERROR", returnStdout: true).trim()
                    echo "Response code: ${response}"
                    if ("${response}" == "SUCCESS") {
                        error("Failed to verify the dx deployment not reachable to external user with response code ${response}.")
                    }
                }
            }
        }

        stage('Run scripts to upgrade the service type to set LoadBalancer') {
            steps {
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
                                    ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@haproxy-tests-${randomNo}.team-q-dev.com \
                                    '(cd /home/centos/native-kube && helm -n dxns --set networking.haproxy.serviceType=LoadBalancer upgrade -f ./install-deploy-values.yaml dx-deployment ./install-hcl-dx-deployment)'
                                """
                            } catch(Exception err) {
                                  error('Failed to helm upgrade the service type to set LoadBalancer for HAProxy.')

                            }
                         }
                      }
                }
            }
        }

        stage('Verify the dx deployment reachable to external user') {
            steps {
                script {
                    // check pods for readiness
                    dxPodsCheckReadiness(
                        namespace: pipelineParameters.NATIVE_KUBE_NAMESPACE,
                        lookupInterval: 90,
                        lookupTries: 180,
                        pendingLimit: 15,
                        containerCreateLimit: 15,
                        safetyInterval: 60
                    )
                    final String response = sh(script: "curl -o /dev/null -s -w %{http_code} https://haproxy-tests-${randomNo}.team-q-dev.com/dx/api/core/v1/explorer/index.html", returnStdout: true).trim()
                    echo "Response code: ${response}"
                    if ("${response}" != "200") {
                        error("Failed to verify the dx deployment reachable to external user with response code ${response}.")

                    }
                }
            }
        }

        stage('Run scripts to upgrade the service port') {
            steps {
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
                                    ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@haproxy-tests-${randomNo}.team-q-dev.com \
                                    '(cd /home/centos/native-kube && helm -n dxns --set networking.haproxy.servicePort=454 upgrade -f ./install-deploy-values.yaml dx-deployment ./install-hcl-dx-deployment)'
                                """
                            } catch(Exception err) {
                                  error('Failed to helm upgrade the service port for HAProxy.')
                            }
                         }
                      }
                }
            }
        }

        stage('Verify the dx deployment is working with service port') {
            steps {
                script {
                    // check pods for readiness
                    dxPodsCheckReadiness(
                        namespace: pipelineParameters.NATIVE_KUBE_NAMESPACE,
                        lookupInterval: 90,
                        lookupTries: 180,
                        pendingLimit: 15,
                        containerCreateLimit: 15,
                        safetyInterval: 60
                    )
                    final String response = sh(script: "curl -o /dev/null -s -w %{http_code} https://haproxy-tests-${randomNo}.team-q-dev.com:454/dx/api/core/v1/explorer/index.html", returnStdout: true).trim()
                    echo "Response code: ${response}"
                    if ("${response}" != "200") {
                        error('Failed to access the deployment using the HAProxy service port.')
                    }
                }
            }
        }

        stage('Run scripts to upgrade the HTTP Strict Transport Security is disabled') {
            steps {
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
                                    ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@haproxy-tests-${randomNo}.team-q-dev.com \
                                    '(cd /home/centos/native-kube && helm -n dxns --set networking.haproxy.strictTransportSecurity.enabled=false upgrade -f ./install-deploy-values.yaml dx-deployment ./install-hcl-dx-deployment)'
                                """
                            } catch(Exception err) {
                                  error('Failed to helm upgrade the service type to set LoadBalancer for HAProxy.')

                            }
                         }
                      }
                }
            }
        }

        stage('Verify the dx deployment no automatic redirect to https') {
            steps {
                script {
                    // check pods for readiness
                    dxPodsCheckReadiness(
                        namespace: pipelineParameters.NATIVE_KUBE_NAMESPACE,
                        lookupInterval: 90,
                        lookupTries: 180,
                        pendingLimit: 15,
                        containerCreateLimit: 15,
                        safetyInterval: 60
                    )
                    final String response = sh(script: "curl http://haproxy-tests-${randomNo}.team-q-dev.com:454/dx/api/core/v1 -k -s -f -o /dev/null && echo SUCCESS || echo ERROR", returnStdout: true).trim()
                    echo "Response code: ${response}"
                    if ("${response}" == "SUCCESS") {
                        error("Failed to verify the dx deployment reachable to external user with response code ${response}.")

                    }
                }
            }
        }

        stage('Run scripts to upgrade the HTTP Strict Transport Security is enabled') {
            steps {
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
                                    ssh -i test-automation-deployments.pem -o StrictHostKeyChecking=no centos@haproxy-tests-${randomNo}.team-q-dev.com \
                                    '(cd /home/centos/native-kube && helm -n dxns --set networking.haproxy.strictTransportSecurity.enabled=true upgrade -f ./install-deploy-values.yaml dx-deployment ./install-hcl-dx-deployment)'
                                """
                            } catch(Exception err) {
                                  error('Failed to helm upgrade the service type to set LoadBalancer for HAProxy.')

                            }
                         }
                      }
                }
            }
        }
    }

    post {
        cleanup {
            script {
                /*After a test execution, the instance is not required anymore and will be deleted.*/
                buildParameters = []
                buildParameters.add(string(name: 'INSTANCE_NAME', value: "haproxy-tests-${randomNo}"))
                buildParameters.add(string(name: 'BUILD_USER_ID', value: 'alina.thomas@hcl.com'))
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
