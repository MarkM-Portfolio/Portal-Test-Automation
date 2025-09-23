/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2024 All Rights Reserved.        *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

// Import the shared library with the name configured in Jenkins
@Library("dx-shared-library") _

// Create object to store parameters with values
def pipelineParameters = [:]

pipeline {
    agent {
        label 'build_infra'
    }


    stages {
        // Load the pipeline parameters into object
        stage("Load parameters") {
            steps {
                dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/dx-opensearch-pod-test/parameters.yaml")
                script {
                    env.SEARCH_HOST = "https://${pipelineParameters.INSTANCE_NAME}${pipelineParameters.DOMAIN_SUFFIX}"

                    if (!pipelineParameters.INSTANCE_NAME) {
                        error("INSTANCE_NAME should not be empty")
                    }

                    if (pipelineParameters.POD_TO_TEST == "opensearch-middleware") {
                        env.POD_NAME = "search-middleware-query"
                    } else if (pipelineParameters.POD_TO_TEST == "opensearch-engine") {
                        env.POD_NAME = "dx-search-open-search-manager-0"
                    } else {
                        error("Invalid POD value")
                    }
                    if (!pipelineParameters.KUBE_DEPLOY_JOB) {
                        error("KUBE_DEPLOY_JOB should not be empty")
                    }
                }
            }
        }

        stage('Deploying the application') {
            steps {
                script {
                    buildParameters = []
                    buildParameters.add(string(name: 'INSTANCE_NAME', value: pipelineParameters.INSTANCE_NAME))
                    buildParameters.add(booleanParam(name: 'DISABLE_DX_CORE', value: "false"))
                    buildParameters.add(booleanParam(name: 'DISABLE_DESIGN_STUDIO', value: "true"))
                    buildParameters.add(booleanParam(name: 'DISABLE_REMOTESEARCH', value: "true"))
                    buildParameters.add(booleanParam(name: 'DISABLE_CONTENTCOMPOSER', value: "true"))
                    buildParameters.add(booleanParam(name: 'DISABLE_DAM', value: "true"))
                    buildParameters.add(booleanParam(name: 'DISABLE_KALTURA_PLUGIN', value: "true"))
                    buildParameters.add(booleanParam(name: 'DISABLE_RINGAPI', value: "false"))
                    buildParameters.add(booleanParam(name: 'DISABLE_PERSISTENCE', value: "false"))
                    buildParameters.add(booleanParam(name: 'DISABLE_PLUGIN_GOOGLE_VISION', value: "true"))
                    buildParameters.add(booleanParam(name: 'PERFORMANCE_RUN', value: "false"))
                    buildParameters.add(booleanParam(name: 'DISABLE_IMAGEPROCESSOR', value: "true"))
                    buildParameters.add(booleanParam(name: 'DISABLE_AMBASSADOR', value: "true"))
                    buildParameters.add(booleanParam(name: 'DISABLE_RUNTIME_CONTROLLER', value:"false"))
                    buildParameters.add(booleanParam(name: 'DISABLE_OPENLDAP', value: "true"))
                    buildParameters.add(booleanParam(name: 'DISABLE_LICENSE_MANAGER', value: "true"))
                    buildParameters.add(booleanParam(name: 'DISABLE_HAPROXY', value: "false"))
                    buildParameters.add(booleanParam(name: 'ENABLE_OPENSEARCH', value: "true"))
                    buildParameters.add(booleanParam(name: 'USE_OPENSOURCE_OPENSEARCH', value: "false"))
                    buildParameters.add(string(name: 'NATIVE_POPO_SCHEDULE', value: "n/a"))
                    
                    build(job: "${pipelineParameters.KUBE_DEPLOY_JOB}", 
                        parameters: buildParameters, 
                        propagate: true,
                        wait: true)
                }
            }
        }

        stage('Configure remote kubectl') {
            steps {
                dxKubectlNativeKubeConfig(sshTarget: "${pipelineParameters.INSTANCE_NAME}${pipelineParameters.DOMAIN_SUFFIX}")
                sh "echo \$KUBECONFIG"

                dxKubectlWorkspaceInstall()
                sh "kubectl get namespaces"
            }
        }

        stage("Authenticate to opensearch api") {
            steps {
                script {
                    CURL_RESPONSE = sh (script: "curl -kL -H 'Content-Type: application/json' -d '{ \"username\": \"searchadmin\", \"password\": \"adminsearch\"}' -X POST ${env.SEARCH_HOST}/dx/api/search/v2/admin/authenticate", returnStdout: true).trim()
                    CURL_RESPONSE = CURL_RESPONSE.replace("\n","")
                    def JSON_CURL_RESPONSE = readJSON text: CURL_RESPONSE
                    TOKEN = JSON_CURL_RESPONSE.jwt
                    println "JWT TOKEN :- ${TOKEN}"
                }
            }
        }

        stage("Get content-source id") {
            steps {
                script {
                    CURL_RESPONSE = sh (script: "curl \
                                                    -X POST -H 'Content-Type: application/json' \
                                                    -H 'Accept: application/json, text/plain, */*' \
                                                    -H 'Authorization: Bearer ${TOKEN}' \
                                                    -d '{ \"name\": \"test\", \"type\": \"wcm\", \"aclLookupHost\": \"${env.SEARCH_HOST}\"}' \
                                                    ${env.SEARCH_HOST}/dx/api/search/v2/contentsources", returnStdout: true).trim()

                    println "Curl Response :${CURL_RESPONSE}"
                    CURL_RESPONSE = CURL_RESPONSE.replace("\n","")
                    def JSON_CURL_RESPONSE = readJSON text: CURL_RESPONSE
                    CONTENT_SOURCE_ID = JSON_CURL_RESPONSE.id
                    println "CONTENT_SOURCE_ID :- ${CONTENT_SOURCE_ID}"
                }
            }
        }

        stage("Configure the search for crawling") {
            steps {
                script {
                    CURL_RESPONSE = sh (script: "curl \
                                                    -X POST -H 'Content-Type: application/json' \
                                                    -H 'Accept: application/json, text/plain, */*' \
                                                    -H 'Authorization: Bearer ${TOKEN}' \
                                                    -d '{\"contentSource\":\"${CONTENT_SOURCE_ID}\",\"type\":\"wcm\",\"configuration\":{\"targetDataSource\":\"${env.SEARCH_HOST}/wps/seedlist/myserver?SeedlistId=&Source=com.ibm.workplace.wcm.plugins.seedlist.retriever.WCMRetrieverFactory&Action=GetDocuments\",\"httpProxy\":\"\",\"schedule\":\"0 * * * *\",\"security\":{\"type\":\"basic\",\"username\":\"wpsadmin\",\"password\":\"wpsadmin\"},\"maxCrawlTime\":60,\"maxRequestTime\":60}}' \
                                                    ${env.SEARCH_HOST}/dx/api/search/v2/crawlers", returnStdout: true).trim()

                    println "Curl Response :${CURL_RESPONSE}"
                    CURL_RESPONSE = CURL_RESPONSE.replace("\n","")
                    def JSON_CURL_RESPONSE = readJSON text: CURL_RESPONSE
                    CRAWLER_ID = JSON_CURL_RESPONSE.id
                    println "CRAWLER_ID :- ${CRAWLER_ID}"
                }
            }
        }

        stage("Triggering the crawler") {
            steps {
                script {
                    CURL_RESPONSE = sh (script: "curl \
                                                    -X POST -H 'Content-Type: application/json' \
                                                    -H 'Accept: application/json, text/plain, */*' \
                                                    -H 'Authorization: Bearer ${TOKEN}' \
                                                    ${env.SEARCH_HOST}/dx/api/search/v2/crawlers/${CRAWLER_ID}/trigger", returnStdout: true).trim()

                    println "Curl Response :${CURL_RESPONSE}"
                }
            }
        }

        stage("Calling Search API - Before the pod failure") {
            steps {
                script {
                    sleep 120
                    CURL_RESPONSE = sh (script: "curl \
                                                    -X POST -H 'Content-Type: application/json' \
                                                    -H 'Accept: application/json, text/plain, */*' \
                                                    -H 'Authorization: Bearer ${TOKEN}' \
                                                    -d '{ \"query\": {}, \"scope\": [ \"${CONTENT_SOURCE_ID}\" ]}' \
                                                    ${env.SEARCH_HOST}/dx/api/search/v2/search", returnStdout: true).trim()
                    CURL_RESPONSE = CURL_RESPONSE.replace("\n","")
                    def JSON_CURL_RESPONSE = readJSON text: CURL_RESPONSE
                    TOTAL_ITEMS = JSON_CURL_RESPONSE.hits.total.value
                    println "TOTAL_ITEMS :- ${TOTAL_ITEMS}"
                }
            }
        }

        stage('Kill the pod and wait for restart') {
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        try {
                            sh  """
                                chmod 600 ${DEPLOY_KEY}
                                scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/dx-opensearch-pod-test/opensearch-pod-drop-test.sh centos@${pipelineParameters.INSTANCE_NAME}${pipelineParameters.DOMAIN_SUFFIX}:/home/centos/
                                ssh -i ${DEPLOY_KEY} -o StrictHostKeyChecking=no centos@${pipelineParameters.INSTANCE_NAME}${pipelineParameters.DOMAIN_SUFFIX} \
                                '(sh /home/centos/opensearch-pod-drop-test.sh ${env.POD_NAME} ${pipelineParameters.NAMESPACE})'
                                """

                            // Trying every 30s, for max 120 times, giving 60s settling period after first successful check
                            dxPodsCheckReadiness(
                                namespace: pipelineParameters.NAMESPACE,
                                lookupInterval: 30,
                                lookupTries: 120,
                                pendingLimit: 15,
                                containerCreateLimit: 15,
                                safetyInterval: 60,
                                podFilter: env.POD_NAME
                            )

                        } catch (Exception err) {
                            echo "Error: ${err}"
                            currentBuild.result = 'UNSTABLE'
                        }
                    }
                }
            }
        }

        stage("Validating the Search API - After the pod restart") {
            steps {
                script {
                    sleep 120
                    CURL_RESPONSE = sh (script: "curl \
                                                    -X POST -H 'Content-Type: application/json' \
                                                    -H 'Accept: application/json, text/plain, */*' \
                                                    -H 'Authorization: Bearer ${TOKEN}' \
                                                    -d '{ \"query\": {}, \"scope\": [ \"${CONTENT_SOURCE_ID}\" ]}' \
                                                    ${env.SEARCH_HOST}/dx/api/search/v2/search", returnStdout: true).trim()
                    CURL_RESPONSE = CURL_RESPONSE.replace("\n","")
                    def JSON_CURL_RESPONSE = readJSON text: CURL_RESPONSE
                    TOTAL_ITEMS = JSON_CURL_RESPONSE.hits.total.value
                    println "TOTAL_ITEMS :- ${TOTAL_ITEMS}"

                    if(TOTAL_ITEMS < 300){
                         currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }

        stage('Undeploying the native-kube instance') {
            steps {
                script {
                    buildParams = []
                    buildParams.add(string(name: 'INSTANCE_NAME', value: pipelineParameters.INSTANCE_NAME))
                    build(job: "${pipelineParameters.KUBE_UNDEPLOY_JOB}", 
                          parameters: buildParams, 
                          propagate: true,
                          wait: true)
                }
            }
        }
    }

    post {
        cleanup {
            /* Cleanup workspace */
            dxWorkspaceDirectoriesCleanup()
        }
    }
}