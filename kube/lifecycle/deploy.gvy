/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2021, 2024. All Rights Reserved. *
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
    environment {
        DAM_KALTURA_PLUGIN_CREDENTIALS = credentials('dam-kaltura-plugin-secret-key')
        DAM_PLUGIN_GOOGLE_VISION_CREDENTIALS = credentials('google-vision-api-key')
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
                    commonModule = load "${moduleDirectory}/common.gvy"
                    kubeModule = load "${moduleDirectory}/${commonConfig.COMMON_KUBE_FLAVOUR}.gvy"
                    if (env.KUBE_FLAVOUR == "openshift" && env.NAMESPACE.length() > openshiftConfig.OS_NAMESPACE_LENGTH ){
                        echo "KUBE_FLAVOUR: ${env.KUBE_FLAVOUR}"
                        echo "NAMESPACE: ${env.NAMESPACE.length()}"
                        error("Namespace length shouldn't be more than 20 characters.")
                    }
                    //Prepare settings for harbor images and helmcharts 
                    if (env.IMAGE_REPOSITORY == "harbor") {
                        env.SOURCE_IMAGE_REPOSITORY_CREDENTIALS = "sofy-harbor-universal"
                        env.SOURCE_IMAGE_REPOSITORY = "https://hclcr.io"
                    }
                }
                // Install Helm in the current workspace and add it to the PATH variable
                dxHelmWorkspaceInstall()
                // Install kubectl in the current workspace and add it to the PATH variable
                dxKubectlWorkspaceInstall()
            }
        }

        stage('Push images to registry') {
            when { expression { env.PUSH_IMAGE_TO_REGISTRY == "true"} }
            steps {
                script {
                    imageFilters = ""
                    buildParams = []
                    mapperFile = "${workspace}/kube/lifecycle/imageFilterMapper.yaml"
                    mapper = [:]
                    
                    // Load mapper which may map
                    if (fileExists(mapperFile)) {
                        println "Read mapping file: ${mapperFile}"
                        mapper = readYaml file: mapperFile
                    }
                    
                    // For normal deployments we use the filter variables, for master ones we use the master image determination
                    if (commonConfig.COMMON_MASTER_DEPLOYMENT_LEVEL == 'NA') {
                        println('Regular image transfer trigger')
                        // Create multi-line string IMAGE_FILTERS parameter value for image upload job.
                        // Loop through all parameters and select those ending with "_IMAGE_FILTER".
                        // Use either the mapping value from mapper as image name or calculate from parameter name
                        params.each {
                            if (it.key.endsWith("_IMAGE_FILTER")) {
                                if (it.value) {
                                    if (mapper.imageFilterNames[it.key]) {
                                        imgName = mapper.imageFilterNames[it.key]
                                    } else {
                                        // Create image name from job parameter
                                        // 1. remove "_IMAGE_FILTER", 2. change "_" to "-", 3. set all to lower case
                                        imgName = it.key.replace("_IMAGE_FILTER", "").replace("_", "-").toLowerCase()
                                    }
                                    imageFilters = "${imageFilters}  ${imgName}: ${it.value}\n"
                                }
                            }
                        }
                    } else {
                        println ('Master image transfer trigger')
                        // Master image generalization
                        def masterFilter = commonModule.determineMasterImageFilter()
                        imageFilters = "${imageFilters}  ringapi: ${masterFilter}\n"
                        imageFilters = "${imageFilters}  persistence-connection-pool: ${masterFilter}\n"
                        imageFilters = "${imageFilters}  design-studio: ${masterFilter}\n"
                        imageFilters = "${imageFilters}  cc: ${masterFilter}\n"
                        imageFilters = "${imageFilters}  dam-kaltura-plugin: ${masterFilter}\n"
                        imageFilters = "${imageFilters}  rs: ${masterFilter}\n"
                        imageFilters = "${imageFilters}  ldap: ${masterFilter}\n"
                        imageFilters = "${imageFilters}  persistence-node: ${masterFilter}\n"
                        imageFilters = "${imageFilters}  haproxy: ${masterFilter}\n"
                        imageFilters = "${imageFilters}  license-manager: ${masterFilter}\n"
                        imageFilters = "${imageFilters}  persistence-metrics-exporter: ${masterFilter}\n"
                        imageFilters = "${imageFilters}  imgproc: ${masterFilter}\n"
                        imageFilters = "${imageFilters}  dam-plugin-google-vision: ${masterFilter}\n"
                        imageFilters = "${imageFilters}  logging-sidecar: ${masterFilter}\n"
                        imageFilters = "${imageFilters}  prereqs-checker: ${masterFilter}\n"
                        imageFilters = "${imageFilters}  core: ${masterFilter}\n"
                        imageFilters = "${imageFilters}  dam: ${masterFilter}\n"
                        imageFilters = "${imageFilters}  runtime-controller: ${masterFilter}\n"
                        imageFilters = "${imageFilters}  opensearch: ${masterFilter}\n"
                        imageFilters = "${imageFilters}  search-middleware: ${masterFilter}\n"
                    }

                    println("Using the following image filter setup for image transfer:\n${imageFilters}")

                    echo "PUSH_IMAGE_TO_REGISTRY: ${env.PUSH_IMAGE_TO_REGISTRY}"
                    if (env.IMAGE_REPOSITORY == "openshift" || env.IMAGE_REPOSITORY == "aws") {
                        buildParams.add(
                        [$class: 'StringParameterValue',
                         name: 'TARGET_REGISTRY_ENVIRONMENT',
                         value: "ECR/OCR"])
                        env.TARGET_REGISTRY_ENVIRONMENT = "ECR/OCR"
                    } else if (env.IMAGE_REPOSITORY == "google") {
                        buildParams.add(
                        [$class: 'StringParameterValue',
                         name: 'TARGET_REGISTRY_ENVIRONMENT',
                         value: "GCR"])
                    } else if (env.IMAGE_REPOSITORY == "azure") {
                        buildParams.add(
                        [$class: 'StringParameterValue',
                         name: 'TARGET_REGISTRY_ENVIRONMENT',
                         value: "ACR"])
                    }
                    if (commonConfig.COMMON_MASTER_DEPLOYMENT_LEVEL == 'NA') {
                        if (env.CORE_IMAGE_FILTER.contains('master')) {
                            buildParams.add(
                            [$class: 'StringParameterValue',
                            name: 'SOURCE_IMAGE_STAGE',
                            value: "master"])
                        } else if (env.CORE_IMAGE_FILTER.contains('release')) {
                            buildParams.add(
                            [$class: 'StringParameterValue',
                            name: 'SOURCE_IMAGE_STAGE',
                            value: "release"])
                        } else {
                            buildParams.add(
                            [$class: 'TextParameterValue',
                            name: 'SOURCE_IMAGE_STAGE',
                            value: "develop"])
                        }
                    } else {
                        buildParams.add(
                        [$class: 'StringParameterValue',
                        name: 'SOURCE_IMAGE_STAGE',
                        value: "master"])
                    }
                    if (imageFilters) {
                        // Strip off last added newline 
                        imageFilters = imageFilters.substring(0, imageFilters.length() - 1);
                        buildParams.add(
                        [$class: 'TextParameterValue',
                         name: 'IMAGE_FILTERS',
                         value: imageFilters])
                    }
                    build(job: commonModule.PUSH_IMAGE_TO_REGISTRY_JOB,
                          parameters: buildParams,
                          propagate: true,
                          wait: true)
                }
            }
        }

        /*
         * Create the environment of the selected flavour
         */
        stage("Create Environment") {
            steps {
                script {
                    if (commonConfig.COMMON_HYBRID.toBoolean()) {
                        kubeModule.createHybridEnvironment()
                    } else {
                        kubeModule.createEnvironment()
                    }
                    // Only schedule next job in case of non-native kube instances
                    // Native kube instances will get cleaned up by the TTL housekeeping job
                    if (commonConfig.COMMON_KUBE_FLAVOUR != 'native') {
                        commonModule.scheduleNextJob()
                    }
                }
            }
        }
        stage("Run Health Checks") {
            when { expression { commonConfig.PLAIN_KUBERNETES == "false" && (commonConfig.COMMON_DEPLOY_DX == "true" || commonConfig.COMMON_DEPLOY_LEAP == "true" || commonConfig.COMMON_DEPLOY_PEOPLESERVICE == "true")} }
            steps {
                script {
                    kubeConfig = load "${configDirectory}/${commonConfig.COMMON_KUBE_FLAVOUR}.gvy"
                    commonModule.runHealthCheck(kubeConfig)  
                }
            }
        }
    
        /*
         * Enable Logstash setup
         */
        stage('Install Logstash, create pipeline configuration file') {
            when { expression { commonConfig.ENABLE_LOGSTASH_SETUP == "true"} }
            steps {
                script {
                    host_name = "${env.INSTANCE_NAME}${env.DOMAIN_SUFFIX}"
                    commonModule.logstashSetup(host_name)
                }
            }
        }
        /*
         * Prometheus and Grafana will be installing for monitoring metrics
         */
        stage('Installing Prometheus and Grafana') {
            when { expression { commonConfig.COMMON_METRICS_MONITORING_ENABLED == "true"} }
            steps {
                script {
                    commonModule.installPrometheusAndGrafana(env.INSTANCE_NAME, env.DOMAIN_SUFFIX)
                }
            }
        }

         /*
         * Enable open LDAP users set up
         */
        stage('OpenLDAP users set up') {
            when { expression { commonConfig.COMMON_ENABLE_OPENLDAP_SET_UP == "true"} }
            steps {
                script {
                    host_name = "${env.INSTANCE_NAME}${env.DOMAIN_SUFFIX}"
                    commonModule.openLDAPUserSetUp(host_name)
                }
            }
        }

        /*
        * Run config engine task to enable OIDC configuration
        */
        stage('Enable OIDC ConfigEngine Task') {
            when { expression { commonConfig.COMMON_ENABLE_OIDC_CONFIGURATION == "true" && commonConfig.COMMON_DEPLOY_DX == "true" } }
            steps {
                script {                    
                    commonModule.configureOIDC()
                }
            }
        }
        
        /*
        * Run acceptance tests after full setup only for native kube flavour, since OIDC is available only in native. 
        * All other flavours run this in their module in the deployEnvironment function
        */
        stage('Run acceptance test') {
            when { expression { commonConfig.COMMON_KUBE_FLAVOUR == 'native' && !("${commonConfig.COMMON_SKIP_ACCEPTANCE_TESTS}".toBoolean()) } }
            steps {
                script {
                    commonModule.executeBuffer(900);
                    CORE_IMAGE_TAG = commonModule.getImageTag(
                        commonConfig.COMMON_CORE_IMAGE_PATH,
                        commonConfig.COMMON_CORE_IMAGE_FILTER
                    )
                    commonModule.isAcceptanceTestsSuccess(
                        CORE_IMAGE_TAG, 
                        "${env.INSTANCE_NAME}${env.DOMAIN_SUFFIX}",
                        true
                    )
                }
            }
        }

        /*
        * Run data setup only for native kube flavour, since OIDC is available only in native. 
        * All other flavours run this in their module in the deployEnvironment function
        */
        stage('Run data setup') {
            when { expression { commonConfig.COMMON_KUBE_FLAVOUR == 'native' && !("${commonConfig.COMMON_SKIP_DATA_SETUP_VERIFY}".toBoolean()) } }
            steps {
                script {
                    commonModule.executeBuffer(900);
                    CORE_IMAGE_TAG = commonModule.getImageTag(
                        commonConfig.COMMON_CORE_IMAGE_PATH,
                        commonConfig.COMMON_CORE_IMAGE_FILTER
                    )
                    jobParams = commonModule.createAcceptanceAndSetupVerifJobParams(
                        CORE_IMAGE_TAG,
                        env.INSTANCE_NAME,
                        env.DOMAIN_SUFFIX
                    )
                    commonModule.isSetupAndVerifyDataSuccess(
                        CORE_IMAGE_TAG,
                        jobParams,
                        'setup'
                    )
                }
            }
        }
    }

    post {
        /*
         * If the job is aborted, we make sure we get rid of everything we created so far
         */
        aborted {
            script {
                if (env.KUBE_FLAVOUR == "aws") {
                    echo "kubeModule.destroyAfterError currently disabled for KUBE_FLAVOUR = aws because of DXQ-20744"
                } else {
                    if (env.CLEANUP_ON_FAILURE){
                        kubeModule.destroyAfterError()
                    }
                }
            }
        }

        /*
         * If the job failed, we make sure we get rid of everything we created so far
         */
        failure {
            script {
                if (env.KUBE_FLAVOUR == "aws") {
                    echo "kubeModule.destroyAfterError currently disabled for KUBE_FLAVOUR = aws because of DXQ-20744"
                } else {
                    if (env.CLEANUP_ON_FAILURE){
                        kubeModule.destroyAfterError()
                    }
                }
            }
        }

        cleanup {
            dxWorkspaceDirectoriesCleanup()
        }
    }
}
