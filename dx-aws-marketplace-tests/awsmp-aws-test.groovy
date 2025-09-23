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

if (!env.CHARTS_FILTER) {
    env.CHARTS_FILTER = '2.28.0-alpha.2'
}

def pipelineParameters = [:]
def randomNo = new Random().nextInt(100000)
def helmCHartFilter = env.CHARTS_FILTER

pipeline { 
    options {
      timeout(time: 180, unit: 'MINUTES') 
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
       
        stage('Deploying the application to EKS from AWS Marketplace ECR') {
            steps {
                script {
                    buildParameters = []
                    buildParameters.add(string(name: 'KUBE_FLAVOUR', value: 'aws'))
                    buildParameters.add(string(name: 'IMAGE_REPOSITORY', value: 'aws-marketplace'))
                    buildParameters.add(string(name: 'CLUSTER_NAME', value: 'eks_cluster_auto'))
                    buildParameters.add(string(name: 'CLUSTER_REGION', value: 'us-east-1'))
                    buildParameters.add(string(name: 'INSTANCE_AREA', value: 'TEST'))
                    buildParameters.add(string(name: 'NEXT_JOB_DELAY_HOURS', value: '0'))
                    buildParameters.add(string(name: 'NATIVE_POPO_SCHEDULE', value: 'n/a'))

                    buildParameters.add(booleanParam(name: 'PUSH_IMAGE_TO_REGISTRY', value: false))
                    buildParameters.add(booleanParam(name: 'SKIP_ACCEPTANCE_TESTS', value: true))
                    buildParameters.add(booleanParam(name: 'SKIP_DATA_SETUP_VERIFY', value: true))
                    buildParameters.add(string(name: 'HELM_CHARTS_FILTER', value: "${helmCHartFilter}"))

                    build(job: "${pipelineParameters.CLOUD_DEPLOY_JOB}", 
                        parameters: buildParameters, 
                        propagate: true,
                        wait: true)
                }
            }
        }

        stage('Run Acceptance Tests') {
            steps {
                script {
                    buildParameters = []
                    buildParameters.add(string(name: 'PORTAL_HOST', value: "https://dxns.hcl-dx-dev.net/wps/portal"))
                    buildParameters.add(string(name: 'EXP_API', value: "https://dxns.hcl-dx-dev.net/dx/api/core/v1"))
                    buildParameters.add(string(name: 'APP_ENDPOINT', value: "https://dxns.hcl-dx-dev.net/dx/ui/dam"))
                    buildParameters.add(string(name: 'WCMREST', value: "https://dxns.hcl-dx-dev.net/wps"))
                    buildParameters.add(string(name: 'TARGET_BRANCH', value: pipelineParameters.TARGET_BRANCH))
                    buildParameters.add(string(name: 'DXCONNECT_HOST', value: "https://dxns.hcl-dx-dev.net"))
                    buildParameters.add(string(name: 'IMAGE_PROCESSOR_API', value: "https://dxns.hcl-dx-dev.net/dx/api/image-processor/v1"))
                    buildParameters.add(string(name: 'DAM_API', value: "https://dxns.hcl-dx-dev.net/dx/api/dam/v1"))
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
                buildParameters.add(string(name: 'KUBE_FLAVOUR', value: 'aws'))
                buildParameters.add(string(name: 'CLUSTER_NAME', value: 'eks_cluster_auto'))
                buildParameters.add(string(name: 'CLUSTER_REGION', value: 'us-east-1'))
                buildParameters.add(string(name: 'BUILD_USER_ID', value: 'jeremiahsteph.aquin@hcl.com'))
                buildParameters.add(string(name: 'DOMAIN_SUFFIX', value: pipelineParameters.DOMAIN_SUFFIX))
                buildParameters.add(string(name: 'HOSTED_ZONE', value: "Z3OEC7SLEHQ2P3"))
                build(job: "${pipelineParameters.CLOUD_UNDEPLOY_JOB}", 
                    parameters: buildParameters, 
                    propagate: true,
                    wait: true)
            }
        }
    }  
}
