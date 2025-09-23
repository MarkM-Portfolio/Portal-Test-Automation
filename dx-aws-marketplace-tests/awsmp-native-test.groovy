/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2024. All Rights Reserved.       *
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

if (!env.CHARTS_FILTER) {
    env.CHARTS_FILTER = '2.28.0-alpha.2'
}

def pipelineParameters = [:]
def randomNo = new Random().nextInt(100000)
def helmCHartFilter = env.CHARTS_FILTER

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
                dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/dx-aws-marketplace-tests/parameters.yaml")
            }
        }
       
        stage('Deploying the application to Native Kube from AWS Marketplace ECR') {
            steps {
                script {
                    buildParameters = []
                    buildParameters.add(string(name: 'INSTANCE_NAME', value: "aws-marketplace-tests-${randomNo}"))
                    buildParameters.add(string(name: 'NAMESPACE', value: pipelineParameters.NATIVE_KUBE_NAMESPACE))
                    buildParameters.add(string(name: 'DOMAIN_SUFFIX', value: pipelineParameters.DOMAIN_SUFFIX))
                    buildParameters.add(string(name: 'HOSTED_ZONE', value: "Z3OEC7SLEHQ2P3"))
                    buildParameters.add(string(name: 'BUILD_USER_ID', value: 'jeremiahsteph.aquin@hcl.com'))
                    buildParameters.add(string(name: 'NEXT_JOB_DELAY_HOURS', value: '0'))
                    buildParameters.add(string(name: 'NATIVE_POPO_SCHEDULE', value: 'n/a'))
                    buildParameters.add(string(name: 'IMAGE_REPOSITORY', value: 'aws-marketplace'))
                    buildParameters.add(string(name: 'HELM_CHARTS_FILTER', value: "${helmCHartFilter}"))
                    build(job: "${pipelineParameters.KUBE_DEPLOY_JOB}", 
                        parameters: buildParameters, 
                        propagate: true,
                        wait: true)
                }
            }
        }

        stage('Configure remote kubectl') {
            steps {
                dxKubectlNativeKubeConfig(sshTarget: "aws-marketplace-tests-${randomNo}.team-q-dev.com")
                sh "echo \$KUBECONFIG"
            }
        }

        stage('Run Acceptance Tests') {
            steps {
                script {
                    buildParameters = []
                    buildParameters.add(string(name: 'PORTAL_HOST', value: "https://aws-marketplace-tests-${randomNo}.team-q-dev.com/wps/portal"))
                    buildParameters.add(string(name: 'EXP_API', value: "https://aws-marketplace-tests-${randomNo}.team-q-dev.com/dx/api/core/v1"))
                    buildParameters.add(string(name: 'APP_ENDPOINT', value: "https://aws-marketplace-tests-${randomNo}.team-q-dev.com/dx/ui/dam"))
                    buildParameters.add(string(name: 'WCMREST', value: "https://aws-marketplace-tests-${randomNo}.team-q-dev.com/wps"))
                    buildParameters.add(string(name: 'TARGET_BRANCH', value: pipelineParameters.TARGET_BRANCH))
                    buildParameters.add(string(name: 'DXCONNECT_HOST', value: "https://aws-marketplace-tests-${randomNo}.team-q-dev.com"))
                    buildParameters.add(string(name: 'IMAGE_PROCESSOR_API', value: "https://aws-marketplace-tests-${randomNo}.team-q-dev.com/dx/api/image-processor/v1"))
                    buildParameters.add(string(name: 'DAM_API', value: "https://aws-marketplace-tests-${randomNo}.team-q-dev.com/dx/api/dam/v1"))
                    buildParameters.add(booleanParam(name: 'TEST_DX_CORE', value: "true"))
                    buildParameters.add(booleanParam(name: 'TEST_RING', value: "true"))
                    buildParameters.add(booleanParam(name: 'TEST_CC', value: "true"))
                    buildParameters.add(booleanParam(name: 'TEST_DAM', value: "true"))
                    buildParameters.add(booleanParam(name: 'TEST_DXCLIENT', value: "true"))
                    buildParameters.add(booleanParam(name: 'TEST_URL_LOCALE', value: "false"))
                    buildParameters.add(booleanParam(name: 'TEST_THEME_EDITOR', value: "true"))
                    buildParameters.add(booleanParam(name: 'TEST_DAM_SERVER', value: "true"))
                    buildParameters.add(booleanParam(name: 'TEST_LICENSE_MANAGER', value: "true"))
                    buildParameters.add(booleanParam(name: 'TEST_CR', value: "true"))
                    buildParameters.add(booleanParam(name: 'TEST_PICKER', value: "true"))
                    buildParameters.add(booleanParam(name: 'TEST_PEOPLE_SERVICE', value: "false"))
                    buildParameters.add(booleanParam(name: 'TEST_SEARCH_MIDDLEWARE', value: "false"))
                    buildParameters.add(booleanParam(name: 'RS_AUTO_CONFIG', value: "true"))
                    buildParameters.add(booleanParam(name: 'TEST_SEARCH_SETUP', value: "true"))
                    buildParameters.add(booleanParam(name: 'TEST_OIDC', value: "false"))
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
                buildParameters.add(string(name: 'INSTANCE_NAME', value: "aws-marketplace-tests-${randomNo}"))
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
