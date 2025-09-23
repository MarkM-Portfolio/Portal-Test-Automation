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

// Using SimpleDateFormat for versioning the name of executions
import java.text.SimpleDateFormat

// Create object to store parameters with values
def pipelineParameters = [:]

// EC2 instance variables
def terraformVarsEC2 = [:]

def requiresEC2Instance(pipelineParameters) {
    if (pipelineParameters.TEST_DXCLIENT == true) { return true }
    if (pipelineParameters.TEST_CC == true) { return true }
    if (pipelineParameters.TEST_DAM == true) { return true }
    if (pipelineParameters.TEST_DAM_SERVER == true) { return true }
    if (pipelineParameters.TEST_DX_CORE == true) { return true }
    if (pipelineParameters.TEST_RING == true) { return true }
    if (pipelineParameters.TEST_PEOPLE_SERVICE == true) { return true }
    if (pipelineParameters.TEST_LICENSE_MANAGER == true) { return true }
    if (pipelineParameters.TEST_SEARCH_MIDDLEWARE == true) { return true }
    if (pipelineParameters.TEST_CR == true) { return true }
    if (pipelineParameters.TEST_PICKER == true) { return true }
    if (pipelineParameters.TEST_OIDC == true) { return true }
    return false
}

// Check if mapString is map syntax
// e.g. [key1: "value1", key2: "value2"]
def isMapStringOK(mapString) {
    def mapOK = false
    // Check brackets
    if (mapString.startsWith('[') && mapString.endsWith(']')) {
        // Remove all blanks and brackets, then split
        mapArray = mapString.replace(" ", "").replace("[", "").replace("]", "").split(",")
        keysOK = 0
        mapArray.each {
            // Check for correct key:value pair syntax
            if (it.contains(':"') && it.endsWith('"')) {
                ++keysOK
            }
        }
        // Check if only valid key:value pairs found
        if (mapArray.size() == keysOK) {
            mapOK = true
        }
    }
    return mapOK
}

// Get value for a passed key in passed mapString
// The map can define a default value which is returned if the requested key has no individual definition
def getValueFromMapString(mapKey, mapString) {
    def returnValue = ""
    def defaultValue = "" 
    if (mapString.contains("default:")) {
        defaultValue = mapString.split("default:")[1]
        defaultValue = defaultValue.split('"')[1]
    }
    if (mapString.contains("${mapKey}:")) {
        returnValue = mapString.split("${mapKey}:")[1]
        returnValue = returnValue.split('"')[1]
    } else {
        returnValue = defaultValue
    }
    return returnValue
}

def dXClientHelperFunction(workspace, pipelineParameters) {
    dir("${workspace}/enchanted-dxclient-test/src/__tests__/helpers/") {
        boolean match = "${pipelineParameters.PORTAL_HOST}" ==~ /^(https?:)\/\/([A-Za-z0-9-\.]+)(:[0-9]+)?(\/.*)?/
        if (!match) {
            error "ERROR: invalid portal host: ${pipelineParameters.PORTAL_HOST}."
        }

        // parse the dxProtocol, hostname and dxPort from the PORTAL_HOST parameter
        PORTAL_HOST_ARRAY = pipelineParameters.PORTAL_HOST.split(":")
        DX_PROTOCOL = PORTAL_HOST_ARRAY[0]
        DX_HOST = PORTAL_HOST_ARRAY[1].substring(2)
        sh "echo ${PORTAL_HOST_ARRAY.size()} ${PORTAL_HOST_ARRAY}"

        if(PORTAL_HOST_ARRAY.size()>2) {
            PORTAL_URL = PORTAL_HOST_ARRAY[2].split("/")
            sh "echo ${PORTAL_URL.size()} ${PORTAL_URL}"
            DX_PORT = PORTAL_URL[0]
            if(PORTAL_URL.size() > 1){
            // parse the context root from the portal url
                CONTEXT_ROOT = PORTAL_URL[1]
            } else {
                CONTEXT_ROOT = ""
            }
        } else {
            DX_PORT = 443
            PORTAL_URL = DX_HOST.split("/")
            sh "echo ${PORTAL_URL.size()} ${PORTAL_URL}"
            DX_HOST = PORTAL_URL[0]
            if(PORTAL_URL.size() > 1){
            // parse the context root from the portal url
                CONTEXT_ROOT = PORTAL_URL[1]
            } else {
                CONTEXT_ROOT = ""
            }
        }

        match = "${pipelineParameters.DXCONNECT_HOST}" ==~ /^(https?:)\/\/([A-Za-z0-9-\.]+)(:[0-9]+)?(.*)/
        if (!match) {
            error "ERROR: invalid dxconnect host: ${pipelineParameters.DXCONNECT_HOST}."
        }

        // parse the dxconnect hostname and dxconnect port from the DXCONNECT_HOST parameter
        DXCONNECT_HOST_ARRAY = pipelineParameters.DXCONNECT_HOST.split(":")
        DXCONNECT_HOSTNAME = DXCONNECT_HOST_ARRAY[1].substring(2)
        if (DXCONNECT_HOST_ARRAY.size()>2) {
        DXCONNECT_PORT = DXCONNECT_HOST_ARRAY[2].split("/")[0]
        } else {
            DXCONNECT_PORT = 443
            DXCONNECT_HOST = DXCONNECT_HOST.split("/")[0]
        }

        if(CONTEXT_ROOT.isEmpty()){
            XML_CONFIG_PATH="/config"
            CONTENT_HANDLER_PATH="/mycontenthandler"
        } else {
            XML_CONFIG_PATH="/${CONTEXT_ROOT}/config"
            CONTENT_HANDLER_PATH="/${CONTEXT_ROOT}/mycontenthandler"
        }

        sh "echo  ${CONTEXT_ROOT} ${XML_CONFIG_PATH}"

        if(pipelineParameters.PORTAL_HOST.contains("clstrhyb")) {
            pipelineParameters.DX_SOAP_PORT = pipelineParameters.DX_HYBRID_SOAP_PORT
        }
        // Replace the mock constant file values by the target server details
        def mockConstantsText = readFile file: "mockConstants.ts"
        mockConstantsText = mockConstantsText.replaceAll("dxProtocol: '.*'", "dxProtocol: '${DX_PROTOCOL}'")
        mockConstantsText = mockConstantsText.replaceAll("hostname: '.*'", "hostname: '${DX_HOST}'")
        mockConstantsText = mockConstantsText.replaceAll("dxPort: '.*'", "dxPort: '${DX_PORT}'")
        mockConstantsText = mockConstantsText.replaceAll("dxUsername: '.*'", "dxUsername: '${pipelineParameters.USERNAME}'")
        mockConstantsText = mockConstantsText.replaceAll("dxPassword: '.*'", "dxPassword: '${pipelineParameters.PASSWORD}'")
        mockConstantsText = mockConstantsText.replaceAll("dxConnectHostname: '.*'", "dxConnectHostname: '${DXCONNECT_HOSTNAME}'")
        mockConstantsText = mockConstantsText.replaceAll("dxConnectUsername: '.*'", "dxConnectUsername: '${pipelineParameters.DXCONNECT_USERNAME}'")
        mockConstantsText = mockConstantsText.replaceAll("dxConnectPassword: '.*'", "dxConnectPassword: '${pipelineParameters.DXCONNECT_PASSWORD}'")
        mockConstantsText = mockConstantsText.replaceAll("dxConnectPort: '.*'", "dxConnectPort: '${DXCONNECT_PORT}'")
        mockConstantsText = mockConstantsText.replaceAll("xmlConfigPath: '.*'", "xmlConfigPath: '${XML_CONFIG_PATH}'")
        mockConstantsText = mockConstantsText.replaceAll("contenthandlerPath: '.*'", "contenthandlerPath: '${CONTENT_HANDLER_PATH}'")
        mockConstantsText = mockConstantsText.replaceAll("dxSoapPort: '.*'", "dxSoapPort: '${pipelineParameters.DX_SOAP_PORT}'")
        mockConstantsText = mockConstantsText.replaceAll("dxContextRoot: '.*'", "dxContextRoot: '${CONTEXT_ROOT}'")
        /* Write to mockConstants file */
        writeFile file: "mockConstants.ts", text: mockConstantsText
        sh "cat mockConstants.ts"
    }
}
def runPeopleServiceTests(DEPLOY_KEY, pipelineParameters, terraformVarsEC2){
    String caughtExceptionMessage = null;
    catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
        try {
            echo "Running tests"
            def urlString = pipelineParameters.TEST_PEOPLE_SERVICE_URL
            def host = urlString.tokenize('/')[1].tokenize('?')[0] ?: ""
            def protocol = urlString.tokenize('://')[0]
            def filePath = "/opt/people-service/tests/src/api/environments/env.json"
            sh """
                chmod 600 ${DEPLOY_KEY}
                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} "jq '.values |= map(if .key == \\"host\\" then .value = \\"${host}\\" else . end | if .key == \\"protocol\\" then .value = \\"${protocol}\\" else . end )' ${filePath} > temp.json && mv temp.json ${filePath}"
                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/people-service && make install-auto-ui && make install-auto-api'
                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/people-service && make test-auto-api && make test-auto-ui isEC2Instance=true peopleServiceUrl=${pipelineParameters.TEST_PEOPLE_SERVICE_URL}'
                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cp -r /opt/people-service/tests/src/ui/reports/html/jest-html-reporters-attach /opt/acceptance-test-reports/html/ 2>/dev/null || : && cp /opt/people-service/tests/src/ui/reports/html/test-reports.html /opt/acceptance-test-reports/html/people-service-ui.html 2>/dev/null || : && cp /opt/people-service/tests/src/api/reports/html/collection.json.html /opt/acceptance-test-reports/html/people-service-api.html 2>/dev/null || : && cp /opt/people-service/tests/src/api/reports/json/collection.json.json /opt/acceptance-test-reports/xml/ 2>/dev/null || :'
            """
        } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
            caughtExceptionMessage = "TIMEOUT ${e.toString()}"
        } catch (Throwable e) {
            caughtExceptionMessage = e.message;
        }
        if (caughtExceptionMessage) {
            sh """
                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cp -r /opt/people-service/tests/src/ui/reports/html/jest-html-reporters-attach /opt/acceptance-test-reports/html/ 2>/dev/null || : && cp /opt/people-service/tests/src/ui/reports/html/test-reports.html /opt/acceptance-test-reports/html/people-service-ui.html 2>/dev/null || : && cp /opt/people-service/tests/src/api/reports/html/collection.json.html /opt/acceptance-test-reports/html/people-service-api.html 2>/dev/null || : && cp /opt/people-service/tests/src/api/reports/json/collection.json.json /opt/acceptance-test-reports/xml/ 2>/dev/null || :'
            """
            error caughtExceptionMessage
        }
    }
}
def runOidcTests(DEPLOY_KEY, pipelineParameters, terraformVarsEC2){
   	echo "Running DX OIDC acceptance tests"
    String caughtExceptionMessage = null;
    catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
    try {
        echo "Running tests"
        def urlString = pipelineParameters.TEST_OIDC_ADMIN_URL
        def newHost = urlString.tokenize('/')[1] + '/' + (urlString.tokenize('/')[2].tokenize('?')[0] ?: "")
        def newRealm = "hcl"
        def newClientId = "hcl-dx-oidc-client"
        def newClientSecret = "hcl-dx-oidc-client-secret"
        def newUsername = "tuser1"
        def newPassword = "passw0rd"
        def filePath = "/opt/hclds-keycloak/tests/src/api/environments/env.json"
        sh """
            chmod 600 ${DEPLOY_KEY}
            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} "jq '.values |= map(if .key == \\"host\\" then .value = \\"${newHost}\\" else . end | if .key == \\"realm\\" then .value = \\"${newRealm}\\" else . end | if .key == \\"client-id\\" then .value = \\"${newClientId}\\" else . end | if .key == \\"client-secret\\" then .value = \\"${newClientSecret}\\" else . end | if .key == \\"username\\" then .value = \\"${newUsername}\\" else . end | if .key == \\"password\\" then .value = \\"${newPassword}\\" else . end )' ${filePath} > temp.json && mv temp.json ${filePath}"
            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/hclds-keycloak && make install-auto-dx-ui && make install-auto-oidc-ui && make install-auto-oidc-api'
            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/hclds-keycloak && make test-auto-dx-ui-endpoint isEC2Instance=true portalUrl=${pipelineParameters.TEST_DX_PORTAL_URL} personalizedPortalUrl=${pipelineParameters.TEST_DX_PERSONALIZED_PORTAL_URL} portalAdminUrl=${pipelineParameters.TEST_DX_PORTAL_ADMIN_URL} oidcAdminUrl=${pipelineParameters.TEST_OIDC_ADMIN_URL} insecure=${pipelineParameters.SSL_ENABLED} ldapUsername=${pipelineParameters.TEST_LDAP_USERNAME} ldapUserPassword=${pipelineParameters.TEST_LDAP_PASSWORD} adminUsername=${pipelineParameters.TEST_ADMIN_USERNAME} adminPassword=${pipelineParameters.TEST_ADMIN_PASSWORD} && make test-auto-oidc-ui-endpoint isEC2Instance=true insecure=${pipelineParameters.SSL_ENABLED} oidcAdminUrl=${pipelineParameters.TEST_OIDC_ADMIN_URL} oidcRealmsUrl=${pipelineParameters.TEST_OIDC_REALMS_URL} && make test-auto-oidc-api'
            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cp -r /opt/hclds-keycloak/tests/src/dx-ui/reports/html /opt/acceptance-test-reports/html/dx-oidc/ 2>/dev/null || : && cp /opt/hclds-keycloak/tests/src/dx-ui/reports/html/dx-oidc-test-reports.html /opt/acceptance-test-reports/html/dx-oidc/dx-oidc-test-reports.html 2>/dev/null || :'
            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cp -r /opt/hclds-keycloak/tests/src/ui/reports/html /opt/acceptance-test-reports/html/oidc-ui/ 2>/dev/null || : && cp /opt/hclds-keycloak/tests/src/ui/reports/html/test-reports.html /opt/acceptance-test-reports/html/oidc-ui/test-reports.html 2>/dev/null || : && cp /opt/hclds-keycloak/tests/src/api/reports/html/collection.json.html /opt/acceptance-test-reports/html/oidc-api-test-results.html 2>/dev/null || : && cp /opt/hclds-keycloak/tests/src/api/reports/json/collection.json.json /opt/acceptance-test-reports/xml/ 2>/dev/null || :'
        """
        } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
            caughtExceptionMessage = "TIMEOUT ${e.toString()}"
        } catch (Throwable e) {
            caughtExceptionMessage = e.message;
        }
        if (caughtExceptionMessage) {
            sh """
                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cp -r /opt/hclds-keycloak/tests/src/dx-ui/reports/html /opt/acceptance-test-reports/html/dx-oidc/ 2>/dev/null || : && cp /opt/hclds-keycloak/tests/src/dx-ui/reports/html/dx-oidc-test-reports.html /opt/acceptance-test-reports/html/dx-oidc/dx-oidc-test-reports.html 2>/dev/null || :'
                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cp -r /opt/hclds-keycloak/tests/src/ui/reports/html /opt/acceptance-test-reports/html/oidc-ui/ 2>/dev/null || : && cp /opt/hclds-keycloak/tests/src/ui/reports/html/test-reports.html /opt/acceptance-test-reports/html/oidc-ui/test-reports.html 2>/dev/null || : && cp /opt/hclds-keycloak/tests/src/api/reports/html/collection.json.html /opt/acceptance-test-reports/html/oidc-api-test-reports.html 2>/dev/null || : && cp /opt/hclds-keycloak/tests/src/api/reports/json/collection.json.json /opt/acceptance-test-reports/xml/ 2>/dev/null || :'
            """
            error caughtExceptionMessage
        }
    }
}     

pipeline {
    agent {
        label 'build_infra'
    }
    stages {
        stage('Load parameters and settings') {
            steps {
                // This example uses the file called dxParametersLoadFromFile.yaml
                dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/dx-acceptance-tests/acceptance-test-automation.yaml")
                // Determine the job Display name 
                script {
                    // determine build version and label current job accordingly
                    def dateFormat = new SimpleDateFormat('yyyyMMdd-HHmmssSSS')
                    def date = new Date()

                    // Determine hostname for EC2 instance
                    pipelineParameters.environmentHostname = "DX_Acceptance_Tests_${dateFormat.format(date)}"
                    println("Hostname of EC2 instance that will perform the test execution: > ${pipelineParameters.environmentHostname} <.")

                    // Adjust display name of current run
                    def currentDate = "${dateFormat.format(date)}"
                    currentBuild.displayName = "${pipelineParameters.PORTAL_HOST}_${currentDate}"
                    def removeHTTPFromDisplayName = currentBuild.displayName.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)","")
                    def tempReportBuildName = removeHTTPFromDisplayName.split('/')
                    tempReportBuildName = tempReportBuildName[0].replace(':','')
                    if (removeHTTPFromDisplayName.contains('/')) {
                        tempReportBuildName = "${tempReportBuildName}_${currentDate}"
                        tempReportBuildName = tempReportBuildName.replace(':','')
                    }
                    pipelineParameters.reportBuildname = tempReportBuildName
                    println("Report build name is > ${pipelineParameters.reportBuildname} <.")

                    // Determine TTL timestamp for EC2 instance
                    pipelineParameters.ttl = (System.currentTimeMillis() + (pipelineParameters.RESOURCES_TTL.toString().toInteger() * 60 * 60 * 1000))

                    // Determine owner of EC2 instance
                    pipelineParameters.buildUser = dxJenkinsGetJobOwner()
                    println("Instance owner will be > ${pipelineParameters.buildUser} <.")

                    // Determine test command
                    if (pipelineParameters.TEST_EXCLUDED) {
                        pipelineParameters.makeCommand = 'test-excluded-acceptance'
                    } else {
                        pipelineParameters.makeCommand = 'test-acceptance-endpoint'
                    }

                    // Check if we really require an EC2 instance
                    env.EC2_REQUIRED = requiresEC2Instance(pipelineParameters)
                 
                    // Check TARGET_BRANCH if passed as map and then create TARGET_BRANCH_MAP
                    if (pipelineParameters.TARGET_BRANCH.startsWith("[")) {
                        pipelineParameters.TARGET_BRANCH_MAP = pipelineParameters.TARGET_BRANCH.replace(" ", "")
                        if (isMapStringOK(pipelineParameters.TARGET_BRANCH_MAP)) {
                            if (getValueFromMapString("default", pipelineParameters.TARGET_BRANCH_MAP) == "") {
                                error("TARGET_BRANCH has been passed as map but doesn't have the mandatory default entry.\nSyntax: [ default: \"develop\", TEST_CC: \"feature/DXQ-12345\" ]")
                            }
                        } else {
                            error("TARGET_BRANCH has been passed as map but the syntax is not correct.\nExample: [ default: \"develop\", key1: \"value1\", key2: \"value2\", ... ]")
                        }
                    }
                    
                    // only if PORTAL_HOST = toblerone-release-latest keep TEST_LICENSE_MANAGER as is otherwise force to false
                    if (!pipelineParameters.PORTAL_HOST.contains("toblerone-release-latest")) {
                    	echo "Important:\nAcceptance test not running on toblerone-release-latest\nForce TEST_LICENSE_MANAGER = false"
                    	pipelineParameters.TEST_LICENSE_MANAGER = false
                    }
                }
            }
        }

        /*
         * Basic health check for the environment under test, allows us to easily stop execution before running actual tests.
         */
        stage('Environment healthcheck') {
            steps {
                println("Checking health for environment > ${pipelineParameters.PORTAL_HOST} <.")
                script {
                    // Try to check if the instance is up, return error if it fails
                    try {
                        sh "curl -s -L -k -i -f ${pipelineParameters.PORTAL_HOST} > ${env.WORKSPACE}/healthcheck-response.txt"
                    } catch (Throwable e) {
                        def failReason = ''
                        if (e.message.contains('exit code 6')) {
                            failReason = 'Could not resolve host.'
                        }
                        if (e.message.contains('exit code 7')) {
                            failReason = 'Failed to connect() to host or proxy.'
                        }
                        if (e.message.contains('exit code 22')) {
                            failReason = 'HTTP server returns an error code that is >= 400.'
                        }
                        if (e.message.contains('exit code 35')) {
                            failReason = 'HTTP does not properly talk HTTPS.'
                        }
                        // echo the HTTP response, since we have an error and it might appear interesting
                        sh "cat ${env.WORKSPACE}/healthcheck-response.txt"
                        error("Environment > ${pipelineParameters.PORTAL_HOST} < is not healthy, reason: ${failReason}")
                    }
                }
            }
        }

        /*
         * Prepare terraform with custom config
         */
        stage('Install and Prepare Terraform') {
            steps {
                script {
                    terraformVarsEC2.tfvar_instance_name = pipelineParameters.environmentHostname
                    terraformVarsEC2.tfvar_instance_owner = dxJenkinsGetJobOwner()
                    terraformVarsEC2.tfvar_EXPIRATION_STAMP = pipelineParameters.ttl
                    terraformVarsEC2.instance_adm_user = "centos"
                }
                dxTerraformInstall (platform: "alma")
            }
        }

        /*
         * Create EC2 instance
         */
        stage('Create EC2 Instance') {
            steps {
                script {
                    dxTerraformCustomConfig (source: 'dx-acceptance-tests/terraform/ec2-launch-alma')
                    try {
                        terraformVarsEC2 = dxTerraformCreateEc2Instance(terraformVarsEC2)
                    } catch (err) {
                        error("Creating EC2 instance failed.")
                    }
                    println "terraformVarsEC2 = " + terraformVarsEC2
                }
            }
        }

        /*
         * After a successful creation of the EC2 instance, we install all required software on it and make sure that our settings
         * will be copied over to the target machine.
         */
        stage('Prepare EC2 instance') {
            when {
                expression { env.EC2_REQUIRED == 'true' }
            }
            steps {
                configFileProvider([
                    configFile(fileId: env.SL_TERRAFORM_EC2_KEYFILE_ID, variable: 'DEPLOY_KEY')
                ]) {
                    sh """
                        chmod 600 ${DEPLOY_KEY}
                        scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/dx-acceptance-tests/scripts/install-prereqs.sh centos@${terraformVarsEC2.instance_private_ip}:/tmp
                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'chmod +x /tmp/install-prereqs.sh && sh /tmp/install-prereqs.sh'
                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sudo mkdir -p /opt/acceptance-test-reports && sudo chown centos: /opt/acceptance-test-reports'
                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'mkdir -p /opt/acceptance-test-reports/html && mkdir -p /opt/acceptance-test-reports/xml'
                        scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r ${workspace}/test-scripts/TestReport/* centos@${terraformVarsEC2.instance_private_ip}:/opt/acceptance-test-reports
                        scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r ${workspace}/dx-core-tests/dx-test-rest/helpers/Saxon-HE-9.5.1-3.jar centos@${terraformVarsEC2.instance_private_ip}:/opt/acceptance-test-reports/Saxon-HE-9.5.1-3.jar
                    """
                }
            }
        }

        /*
         * Once prerequisites are installed, we pull the repositories needed for the tests in the workspace, and scp transfer it to the EC2 instance.
         */
        stage('Pull tests') {
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    sshagent(credentials: ['github01.hclpnp.com']) {
                        script {
                            sh "chmod 600 ${DEPLOY_KEY}"
                            if (pipelineParameters.TEST_PEOPLE_SERVICE == true) {
                                if (pipelineParameters.TARGET_BRANCH_MAP) {
                                    pipelineParameters.TARGET_BRANCH = getValueFromMapString("TEST_PEOPLE_SERVICE", pipelineParameters.TARGET_BRANCH_MAP)
                                }
                                sh """
                                    git clone -b ${pipelineParameters.TARGET_BRANCH} git@github01.hclpnp.com:hclds/people-service.git ${workspace}/people-service
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sudo mkdir -p /opt/people-service && sudo chown centos: /opt/people-service'
                                    scp -r -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/people-service centos@${terraformVarsEC2.instance_private_ip}:/opt
                                """
                            }
                        }
                    }
                    sshagent(credentials: ['jenkins-git']) {
                        script {
                            sh "chmod 600 ${DEPLOY_KEY}"
                            if (pipelineParameters.TEST_CC == true) {
                                if (pipelineParameters.TARGET_BRANCH_MAP) {
                                    pipelineParameters.TARGET_BRANCH = getValueFromMapString("TEST_CC", pipelineParameters.TARGET_BRANCH_MAP)
                                }
                                sh """
                                    git clone -b ${pipelineParameters.TARGET_BRANCH} git@git.cwp.pnp-hcl.com:websphere-portal-incubator/content-ui.git ${workspace}/content-ui
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sudo mkdir -p /opt/content-ui && sudo chown centos: /opt/content-ui'
                                    scp -r -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/content-ui centos@${terraformVarsEC2.instance_private_ip}:/opt
                                """
                            }
                            if (pipelineParameters.TEST_DAM == true || pipelineParameters.TEST_DAM_SERVER == true) {
                                if (pipelineParameters.TARGET_BRANCH_MAP) {
                                    pipelineParameters.TARGET_BRANCH = getValueFromMapString("TEST_DAM", pipelineParameters.TARGET_BRANCH_MAP)
                                }
                                sh """
                                    git clone -b ${pipelineParameters.TARGET_BRANCH} git@git.cwp.pnp-hcl.com:websphere-portal-incubator/media-library.git ${workspace}/media-library
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sudo mkdir -p /opt/media-library && sudo chown centos: /opt/media-library'
                                    scp -r -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/media-library centos@${terraformVarsEC2.instance_private_ip}:/opt
                                """
                            }
                            if (pipelineParameters.TEST_DAM_SERVER == true) {
                                if (pipelineParameters.TARGET_BRANCH_MAP) {
                                    pipelineParameters.TARGET_BRANCH = getValueFromMapString("TEST_DAM_SERVER", pipelineParameters.TARGET_BRANCH_MAP)
                                }
                                sh """
                                    scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/dx-acceptance-tests/config/media-library/docker-compose.yaml centos@${terraformVarsEC2.instance_private_ip}:/opt/media-library
                                    scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/dx-acceptance-tests/scripts/pull-and-run-postgres.sh centos@${terraformVarsEC2.instance_private_ip}:/tmp
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'chmod +x /tmp/pull-and-run-postgres.sh && ARTIFACTORY_IMAGE_BASE_URL=${pipelineParameters.ARTIFACTORY_IMAGE_BASE_URL} ARTIFACTORY_HOST=${pipelineParameters.ARTIFACTORY_HOST} MEDIA_LIBRARY_PERSISTENCE_IMAGE_FILTER=${pipelineParameters.MEDIA_LIBRARY_PERSISTENCE_IMAGE_FILTER} sh /tmp/pull-and-run-postgres.sh'
                                """
                            }
                            if (pipelineParameters.TEST_RING == true) {
                                if (pipelineParameters.TARGET_BRANCH_MAP) {
                                    pipelineParameters.TARGET_BRANCH = getValueFromMapString("TEST_RING", pipelineParameters.TARGET_BRANCH_MAP)
                                }
                                sh """
                                    git clone -b ${pipelineParameters.TARGET_BRANCH} git@git.cwp.pnp-hcl.com:websphere-portal-incubator/RingAPI.git ${workspace}/ring-api
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sudo mkdir -p /opt/ring-api && sudo chown centos: /opt/ring-api'
                                    scp -r -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/ring-api centos@${terraformVarsEC2.instance_private_ip}:/opt
                                """
                            }
                            if (pipelineParameters.TEST_OIDC == true) {
                                if (pipelineParameters.TARGET_BRANCH_MAP) {
                                    pipelineParameters.TARGET_BRANCH = getValueFromMapString("TEST_OIDC", pipelineParameters.TARGET_BRANCH_MAP)
                                }
                                sh """
                                    git clone -b ${pipelineParameters.TARGET_BRANCH} git@git.cwp.pnp-hcl.com:hclds/hclds-keycloak.git ${workspace}/hclds-keycloak
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sudo mkdir -p /opt/hclds-keycloak && sudo chown centos: /opt/hclds-keycloak'
                                    scp -r -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/hclds-keycloak centos@${terraformVarsEC2.instance_private_ip}:/opt
                                """
                            }
                            if (pipelineParameters.TEST_DX_CORE == true) {
                                if (pipelineParameters.TARGET_BRANCH_MAP) {
                                    pipelineParameters.TARGET_BRANCH = getValueFromMapString("TEST_DX_CORE", pipelineParameters.TARGET_BRANCH_MAP)
                                }
                                sh """
                                    git clone -b ${pipelineParameters.TARGET_BRANCH} git@git.cwp.pnp-hcl.com:websphere-portal/portal-integration-test.git ${workspace}/portal-integration-test
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sudo mkdir -p /opt/portal-integration-test && sudo chown centos: /opt/portal-integration-test'
                                    scp -r -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/portal-integration-test centos@${terraformVarsEC2.instance_private_ip}:/opt
                                """
                            }
                            if (pipelineParameters.TEST_DXCLIENT == true) {
                                if (pipelineParameters.TARGET_BRANCH_MAP) {
                                    pipelineParameters.TARGET_BRANCH = getValueFromMapString("TEST_DXCLIENT", pipelineParameters.TARGET_BRANCH_MAP)
                                }
                                sh """
                                    git clone -b ${pipelineParameters.TARGET_BRANCH} git@git.cwp.pnp-hcl.com:websphere-portal-incubator/enchanted-dxclient.git ${workspace}/enchanted-dxclient-test
                                """
                                dXClientHelperFunction(workspace, pipelineParameters)
                                sh """
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sudo mkdir -p /opt/enchanted-dxclient-test && sudo chown centos: /opt/enchanted-dxclient-test'
                                    scp -r -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/enchanted-dxclient-test centos@${terraformVarsEC2.instance_private_ip}:/opt
                                """
                            }
                            if (pipelineParameters.TEST_LICENSE_MANAGER == true) {
                                // parse the dxconnect hostname from the DXCONNECT_HOST parameter
                                DXCONNECT_HOST_ARRAY = pipelineParameters.DXCONNECT_HOST.split(":")
                                DXCONNECT_HOSTNAME = DXCONNECT_HOST_ARRAY[1].substring(2)
                                // Install and configure kubectl in local workspace and then copy on EC2 instance
                                dxKubectlWorkspaceInstall()
                                dxKubectlNativeKubeConfig(sshTarget: DXCONNECT_HOSTNAME)
                                if (pipelineParameters.TARGET_BRANCH_MAP) {
                                    pipelineParameters.TARGET_BRANCH = getValueFromMapString("TEST_LICENSE_MANAGER", pipelineParameters.TARGET_BRANCH_MAP)
                                }
                                sh """
                                    git clone -b ${pipelineParameters.TARGET_BRANCH} git@git.cwp.pnp-hcl.com:websphere-portal-incubator/dx-license-manager.git ${workspace}/license-manager
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sudo mkdir -p /opt/license-manager && sudo chown centos: /opt/license-manager'
                                    scp -r -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/license-manager centos@${terraformVarsEC2.instance_private_ip}:/opt
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sudo mkdir -p /opt/kube && sudo chown centos: /opt/kube'
                                    scp -r -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/kube centos@${terraformVarsEC2.instance_private_ip}:/opt
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'mkdir -p /home/centos/.kube'
                                    scp -r -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/remote-kube.yaml centos@${terraformVarsEC2.instance_private_ip}:/home/centos/.kube/config
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'chmod 600 /home/centos/.kube/config'
                                """
                            }
                            if (pipelineParameters.TEST_CR == true) {
                                if (pipelineParameters.TARGET_BRANCH_MAP) {
                                    pipelineParameters.TARGET_BRANCH = getValueFromMapString("TEST_CR", pipelineParameters.TARGET_BRANCH_MAP)
                                }
                                sh """
                                    git clone -b ${pipelineParameters.TARGET_BRANCH} git@git.cwp.pnp-hcl.com:websphere-portal-incubator/content-reporting.git ${workspace}/content-reporting
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sudo mkdir -p /opt/content-reporting && sudo chown centos: /opt/content-reporting'
                                    scp -r -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/content-reporting centos@${terraformVarsEC2.instance_private_ip}:/opt
                                """
                            }
                            if (pipelineParameters.TEST_PICKER == true) {
                                if (pipelineParameters.TARGET_BRANCH_MAP) {
                                    pipelineParameters.TARGET_BRANCH = getValueFromMapString("TEST_PICKER", pipelineParameters.TARGET_BRANCH_MAP)
                                }
                                sh """
                                    git clone -b ${pipelineParameters.TARGET_BRANCH} git@git.cwp.pnp-hcl.com:websphere-portal-incubator/dx-picker.git ${workspace}/dx-picker
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sudo mkdir -p /opt/dx-picker && sudo chown centos: /opt/dx-picker'
                                    scp -r -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/dx-picker centos@${terraformVarsEC2.instance_private_ip}:/opt
                                """
                            }
                            /* Pull code from new search middleware */
                            if (pipelineParameters.TEST_SEARCH_MIDDLEWARE == true) {
                                if (pipelineParameters.TARGET_BRANCH_MAP) {
                                    pipelineParameters.TARGET_BRANCH = getValueFromMapString("TEST_SEARCH_MIDDLEWARE", pipelineParameters.TARGET_BRANCH_MAP)
                                }
                                sh """
                                    git clone -b ${pipelineParameters.TARGET_BRANCH} git@git.cwp.pnp-hcl.com:websphere-portal-incubator/opensearch-middleware.git ${workspace}/opensearch-middleware
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sudo mkdir -p /opt/opensearch-middleware && sudo chown centos: /opt/opensearch-middleware'
                                    scp -r -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/opensearch-middleware centos@${terraformVarsEC2.instance_private_ip}:/opt
                                """
                            }
                        }
                    }
                }
            }
        }

        /*
         * Each application would then install its dependencies and run the acceptance tests. Timeout is currently set at 30 mins per application stage.
         * Timeout is also treated as a failure, and is caught using org.jenkinsci.pligins.workflow.steps.FlowInterruptedException. Otherwise timeouts
         * are going to be registered as ABORTED in jenkins status report. The other catch is for any other errors produced by the test.
         */
        stage('Run DX Core acceptance tests') {
            options {
                timeout(time: 30, unit: 'MINUTES')
            }
            when {
                expression { pipelineParameters.TEST_DX_CORE == true }
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        String caughtExceptionMessage = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo "Running tests"
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/portal-integration-test/packages/ui && make install && make test-acceptance-endpoint portal=${pipelineParameters.PORTAL_HOST}  insecure=${pipelineParameters.SSL_ENABLED} testCC=${pipelineParameters.TEST_CC} testDAM=${pipelineParameters.TEST_DAM} testURLLocale=${pipelineParameters.TEST_URL_LOCALE} testThemeEditor=${pipelineParameters.TEST_THEME_EDITOR}'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'mv /opt/portal-integration-test/packages/ui/jest_html_reporters.html /opt/portal-integration-test/packages/ui/test-report/Core.html && cp /opt/portal-integration-test/packages/ui/test-report/Core.html /opt/acceptance-test-reports/html/ && cp /opt/portal-integration-test/packages/ui/test-report/Core.xml /opt/acceptance-test-reports/xml/'
                                """
                            } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
                                caughtExceptionMessage = "TIMEOUT ${e.toString()}"
                            } catch (Throwable e) {
                                caughtExceptionMessage = e.message;
                            }
                            if (caughtExceptionMessage) {
                                sh """
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'mv /opt/portal-integration-test/packages/ui/jest_html_reporters.html /opt/portal-integration-test/packages/ui/test-report/Core.html && cp /opt/portal-integration-test/packages/ui/test-report/Core.html /opt/acceptance-test-reports/html/ && cp /opt/portal-integration-test/packages/ui/test-report/Core.xml /opt/acceptance-test-reports/xml/'
                                """
                                error caughtExceptionMessage
                            }
                        }
                    }
                }
            }
        }

        stage('Run Remote Search Auto configuration tests') {
            options {
                timeout(time: 30, unit: 'MINUTES')
            }
            when {
                expression { pipelineParameters.TEST_SEARCH_SETUP == true }
            }
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    script {
                        buildParameters = []
                        buildParameters.add(string(name: 'USERNAME', value: USERNAME))
                        buildParameters.add(string(name: 'PASSWORD', value: PASSWORD))
                        buildParameters.add(string(name: 'PORTAL_HOST', value: pipelineParameters.WCMREST))
                        buildParameters.add(booleanParam(name: 'RS_AUTO_CONFIG', value: pipelineParameters.RS_AUTO_CONFIG))
                        build(
                        job: "${pipelineParameters.RS_AUTOCONFIG_TEST}", 
                        parameters: buildParameters, 
                        propagate: false,
                        wait: true
                    )
                    }
                }
            }
        }

        stage('Run RingAPI acceptance tests') {
            options {
                timeout(time: 30, unit: 'MINUTES')
            }
            when {
                expression { pipelineParameters.TEST_RING == true }
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        String caughtExceptionMessage = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo "Running tests"
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/ring-api && make install'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/ring-api/packages/server-v1 && make build && make ${pipelineParameters.makeCommand} dx_core=${pipelineParameters.PORTAL_HOST} insecure=${pipelineParameters.SSL_ENABLED}'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cp /opt/ring-api/packages/server-v1/test-report/RING_API.html /opt/acceptance-test-reports/html/ 2>/dev/null || : && cp /opt/ring-api/packages/server-v1/test-report/RING_API.xml /opt/acceptance-test-reports/xml/ 2>/dev/null || :'
                                """
                            } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
                                caughtExceptionMessage = "TIMEOUT ${e.toString()}"
                            } catch (Throwable e) {
                                caughtExceptionMessage = e.message;
                            }
                            if (caughtExceptionMessage) {
                                sh """
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cp /opt/ring-api/packages/server-v1/test-report/RING_API.html /opt/acceptance-test-reports/html/ 2>/dev/null || : && cp /opt/ring-api/packages/server-v1/test-report/RING_API.xml /opt/acceptance-test-reports/xml/ 2>/dev/null || :'
                                """
                                error caughtExceptionMessage
                            }
                        }
                    }
                }
            }
        }

        stage('Run CC acceptance tests') {
            options {
                timeout(time: 30, unit: 'MINUTES')
            }
            when {
                expression { pipelineParameters.TEST_CC == true }
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        String caughtExceptionMessage = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo "Running tests"
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/content-ui && make install'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/content-ui/packages/ui && make ${pipelineParameters.makeCommand} dx_core=${pipelineParameters.PORTAL_HOST} ring_api=${pipelineParameters.EXP_API} insecure=${pipelineParameters.SSL_ENABLED}'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'mv /opt/content-ui/packages/ui/jest_html_reporters.html /opt/content-ui/packages/ui/test-report/Content_Composer.html && cp /opt/content-ui/packages/ui/test-report/Content_Composer.html /opt/acceptance-test-reports/html/ && cp /opt/content-ui/packages/ui/test-report/Content_Composer.xml /opt/acceptance-test-reports/xml/'
                                """
                            } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
                                caughtExceptionMessage = "TIMEOUT ${e.toString()}"
                            } catch (Throwable e) {
                                caughtExceptionMessage = e.message;
                            }
                            if (caughtExceptionMessage) {
                                sh """
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'mv /opt/content-ui/packages/ui/jest_html_reporters.html /opt/content-ui/packages/ui/test-report/Content_Composer.html && cp /opt/content-ui/packages/ui/test-report/Content_Composer.html /opt/acceptance-test-reports/html/ && cp /opt/content-ui/packages/ui/test-report/Content_Composer.xml /opt/acceptance-test-reports/xml/'
                                """
                                error caughtExceptionMessage
                            }
                        }
                    }
                }
            }
        }

        stage('Run DAM acceptance tests') {
            options {
                timeout(time: 30, unit: 'MINUTES')
            }
            when {
                expression { pipelineParameters.TEST_DAM == true }
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        String caughtExceptionMessage = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo "Running tests"
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/media-library && make install'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/media-library/packages/ui && TEST_BROWSER_HEADLESS=true make ${pipelineParameters.makeCommand} dx_core=${pipelineParameters.PORTAL_HOST} ring_api=${pipelineParameters.EXP_API} app=${pipelineParameters.APP_ENDPOINT} dam=${pipelineParameters.DAM_API} insecure=${pipelineParameters.SSL_ENABLED}'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'mv /opt/media-library/packages/ui/jest_html_reporters.html /opt/media-library/packages/ui/test-report/Digital_Asset_Manger.html && cp /opt/media-library/packages/ui/test-report/Digital_Asset_Manger.html /opt/acceptance-test-reports/html/ && cp /opt/media-library/packages/ui/test-report/Digital_Asset_Manger.xml /opt/acceptance-test-reports/xml/'
                                """
                            } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
                                caughtExceptionMessage = "TIMEOUT ${e.toString()}"
                            } catch (Throwable e) {
                                caughtExceptionMessage = e.message;
                            }
                            if (caughtExceptionMessage) {
                                sh """
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'mv /opt/media-library/packages/ui/jest_html_reporters.html /opt/media-library/packages/ui/test-report/Digital_Asset_Manger.html && cp /opt/media-library/packages/ui/test-report/Digital_Asset_Manger.html /opt/acceptance-test-reports/html/ && cp /opt/media-library/packages/ui/test-report/Digital_Asset_Manger.xml /opt/acceptance-test-reports/xml/'
                                """
                                error caughtExceptionMessage
                            }
                        }
                    }
                }
            }
        }

        stage('Run DAM SERVER acceptance tests') {
            options {
                timeout(time: 30, unit: 'MINUTES')
            }
            when {
                expression { pipelineParameters.TEST_DAM_SERVER == true }
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        String caughtExceptionMessage = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo "Running tests"
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/media-library && make scope && make install && make build'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/media-library/packages/server-v1 && make build-static-ui && make ${pipelineParameters.makeCommand} dam_api=${pipelineParameters.DAM_API} image_processor_api=${pipelineParameters.IMAGE_PROCESSOR_API} ring_api=${pipelineParameters.EXP_API} insecure=${pipelineParameters.SSL_ENABLED}'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cp /opt/media-library/packages/server-v1/test-report/DAM_SERVER.html /opt/acceptance-test-reports/html/ 2>/dev/null || : && cp /opt/media-library/packages/server-v1/test-report/DAM_SERVER.xml /opt/acceptance-test-reports/xml/ 2>/dev/null || :'
                                """
                            } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
                                caughtExceptionMessage = "TIMEOUT ${e.toString()}"
                            } catch (Throwable e) {
                                caughtExceptionMessage = e.message;
                            }
                            if (caughtExceptionMessage) {
                                sh """
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cp /opt/media-library/packages/server-v1/test-report/DAM_SERVER.html /opt/acceptance-test-reports/html/ 2>/dev/null || : && cp /opt/media-library/packages/server-v1/test-report/DAM_SERVER.xml /opt/acceptance-test-reports/xml/ 2>/dev/null || :'
                                """
                                error caughtExceptionMessage
                            }
                        }
                    }
                }
            }
        }

        stage('Run DAM Backup & Restore tests') {
            options {
                timeout(time: 30, unit: 'MINUTES')
            }
            when {
                expression { pipelineParameters.TEST_BACKUP_AND_RESTORE == true }
            }
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    script {
                        def backupAndRestoreModule = load "${workspace}/dx-acceptance-tests/scripts/backup-and-restore.groovy"
                        backupAndRestoreModule.performTest(pipelineParameters)
                    }
                }
            }
        }

        stage('Run Dxclient acceptance tests') {
            options {
                timeout(time: 30, unit: 'MINUTES')
            }
            when {
                expression { pipelineParameters.TEST_DXCLIENT == true }
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        String caughtExceptionMessage = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/enchanted-dxclient-test && make install && make build && make test-acceptance'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cp /opt/enchanted-dxclient-test/test-report/DXClient.html /opt/acceptance-test-reports/html/ 2>/dev/null || : && cp /opt/enchanted-dxclient-test/test-report/DXClient.xml /opt/acceptance-test-reports/xml/ 2>/dev/null || :'
                                """
                            } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
                                caughtExceptionMessage = "TIMEOUT ${e.toString()}"
                            } catch (Throwable e) {
                                caughtExceptionMessage = e.message;
                            }
                            if (caughtExceptionMessage) {
                                sh """
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cp /opt/enchanted-dxclient-test/test-report/DXClient.html /opt/acceptance-test-reports/html/ 2>/dev/null || : && cp /opt/enchanted-dxclient-test/test-report/DXClient.xml /opt/acceptance-test-reports/xml/ 2>/dev/null || :'
                                """
                                error caughtExceptionMessage
                            }
                        }
                    }
                }
            }
        }

        stage('Run Content Reporting acceptance tests') {
            options {
                timeout(time: 30, unit: 'MINUTES')
            }
            when {
                expression { pipelineParameters.TEST_CR == true }
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        String caughtExceptionMessage = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo "Running tests"
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/content-reporting && make install'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/content-reporting/packages/ui && make ${pipelineParameters.makeCommand} dx_core=${pipelineParameters.PORTAL_HOST} insecure=${pipelineParameters.SSL_ENABLED}'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'mv /opt/content-reporting/packages/ui/jest_html_reporters.html /opt/content-reporting/packages/ui/test-report/Content_Reporting.html && cp -r /opt/content-reporting/packages/ui/jest-html-reporters-attach/ /opt/acceptance-test-reports/html/jest-html-reporters-attach/ && cp /opt/content-reporting/packages/ui/test-report/Content_Reporting.html /opt/acceptance-test-reports/html/ && cp /opt/content-reporting/packages/ui/test-report/Content_Reporting.xml /opt/acceptance-test-reports/xml/'
                                """
                            } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
                                caughtExceptionMessage = "TIMEOUT ${e.toString()}"
                            } catch (Throwable e) {
                                caughtExceptionMessage = e.message;
                            }
                            if (caughtExceptionMessage) {
                                sh """
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'mv /opt/content-reporting/packages/ui/jest_html_reporters.html /opt/content-reporting/packages/ui/test-report/Content_Reporting.html && cp -r /opt/content-reporting/packages/ui/jest-html-reporters-attach/ /opt/acceptance-test-reports/html/jest-html-reporters-attach/ && cp /opt/content-reporting/packages/ui/test-report/Content_Reporting.html /opt/acceptance-test-reports/html/ && cp /opt/content-reporting/packages/ui/test-report/Content_Reporting.xml /opt/acceptance-test-reports/xml/'
                                """
                                error caughtExceptionMessage
                            }
                        }
                    }
                }
            }
        }

        stage('Run Picker acceptance tests') {
            options {
                timeout(time: 30, unit: 'MINUTES')
            }
            when {
                expression { pipelineParameters.TEST_PICKER == true }
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        String caughtExceptionMessage = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo "Running tests"
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/dx-picker && make install'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/dx-picker/packages/ui && make ${pipelineParameters.makeCommand} dx_core=${pipelineParameters.PORTAL_HOST} ring_api=${pipelineParameters.EXP_API} insecure=${pipelineParameters.SSL_ENABLED}'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'mv /opt/dx-picker/packages/ui/jest_html_reporters.html /opt/dx-picker/packages/ui/test-report/Dx_Picker.html && cp /opt/dx-picker/packages/ui/test-report/Dx_Picker.html /opt/acceptance-test-reports/html/ && cp /opt/dx-picker/packages/ui/test-report/Dx_Picker.xml /opt/acceptance-test-reports/xml/'
                                """
                            } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
                                caughtExceptionMessage = "TIMEOUT ${e.toString()}"
                            } catch (Throwable e) {
                                caughtExceptionMessage = e.message;
                            }
                            if (caughtExceptionMessage) {
                                sh """
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'mv /opt/dx-picker/packages/ui/jest_html_reporters.html /opt/dx-picker/packages/ui/test-report/Dx_Picker.html && cp /opt/dx-picker/packages/ui/test-report/Dx_Picker.html /opt/acceptance-test-reports/html/ && cp /opt/dx-picker/packages/ui/test-report/Dx_Picker.xml /opt/acceptance-test-reports/xml/'
                                """
                                error caughtExceptionMessage
                            }
                        }
                    }
                }
            }
        }

        stage('Run People Service acceptance tests') {
            options {
                timeout(time: 30, unit: 'MINUTES')
            }
            when {
                expression { pipelineParameters.TEST_PEOPLE_SERVICE == true }
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        runPeopleServiceTests(DEPLOY_KEY, pipelineParameters, terraformVarsEC2)
                    }
                }
            }
        }

        stage('Run DX OIDC acceptance tests') {
            options {
                timeout(time: 30, unit: 'MINUTES')
            }
            when {
                expression { pipelineParameters.TEST_OIDC == true }
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        runOidcTests(DEPLOY_KEY, pipelineParameters, terraformVarsEC2)                        
                    }   
                }
            } 
        } 

        stage('Run License Manager tests') {
            options {
                timeout(time: 40, unit: 'MINUTES')
            }
            when {
                expression { pipelineParameters.TEST_LICENSE_MANAGER == true }
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        String caughtExceptionMessage = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                sh """
                                    echo "Running license manager tests"
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/license-manager/license-check/src && pwd && ls && \
                                        export PATH="/opt/kube:\$PATH"
                                        sh testLicenseManager.sh "${pipelineParameters.KUBERNETES_NAMESPACE}" "${pipelineParameters.KUBERNETES_FLAVOUR}"'
                                    echo "create test-acceptance report"
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cp /opt/license-manager/test-report/License_Manager.html /opt/acceptance-test-reports/html/ 2>/dev/null || : && cp /opt/license-manager/test-report/License_Manager.xml /opt/acceptance-test-reports/xml/ 2>/dev/null || :'
                                """
                            } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
                                caughtExceptionMessage = "TIMEOUT ${e.toString()}"
                            } catch (Throwable e) {
                                caughtExceptionMessage = e.message;
                            }
                            if (caughtExceptionMessage) {
                                sh """
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cp /opt/license/test-report/License_Manager.html /opt/acceptance-test-reports/html/ 2>/dev/null || : && cp /opt/license-manager/test-report/License_Manager.xml /opt/acceptance-test-reports/xml/ 2>/dev/null || :'
                                """
                                error caughtExceptionMessage
                            }
                        }
                    }
                }
            }
        }

        /*
         * Run search middleware acceptance tests
         */
        stage('Run search middleware tests') {
            options {
                timeout(time: 30, unit: 'MINUTES')
            }
            when {
                expression { pipelineParameters.TEST_SEARCH_MIDDLEWARE == true }
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        String caughtExceptionMessage = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo "Running tests"
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/opensearch-middleware && make install'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/opensearch-middleware/packages/server-v2 && make build && TARGET_ENVIRONMENT=\"${pipelineParameters.SEARCH_TARGET_ENVIRONMENT}\" npm run test:only-acceptance'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cp /opt/opensearch-middleware/packages/server-v2/test-report/search-middleware.html /opt/acceptance-test-reports/html/ 2>/dev/null || : && cp /opt/opensearch-middleware/packages/server-v2/test-report/search-middleware.xml /opt/acceptance-test-reports/xml/ 2>/dev/null || :'
                                """
                            } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
                                caughtExceptionMessage = "TIMEOUT ${e.toString()}"
                            } catch (Throwable e) {
                                caughtExceptionMessage = e.message;
                            }
                            if (caughtExceptionMessage) {
                                sh """
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cp /opt/opensearch-middleware/packages/server-v2/test-report/search-middleware.html /opt/acceptance-test-reports/html/ 2>/dev/null || : && cp /opt/opensearch-middleware/packages/server-v2/test-report/search-middleware.xml /opt/acceptance-test-reports/xml/ 2>/dev/null || :'
                                """
                                error caughtExceptionMessage
                            }
                        }
                    }
                }
            }
        }

        stage('Generate Test Report') {
            steps {
                withCredentials([
                    usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID'),
                ]) {
                    dir("${workspace}") {
                        configFileProvider([
                                configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                        ]) {
                            script {
                                Exception caughtException = null;
                                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                                    try {
                                        echo "Generating test dashboard report"
                                        sh """
                                            chmod 600 ${DEPLOY_KEY}
                                            aws s3 cp s3://dx-testarea/acceptance-test-reports/acceptance-test-combined-runs.xml acceptance-test-combined-runs.xml
                                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/acceptance-test-combined-runs.xml centos@${terraformVarsEC2.instance_private_ip}:/opt/acceptance-test-reports/
                                            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/acceptance-test-reports && source ~/.bash_profile && chmod +x /opt/acceptance-test-reports/acceptance-test-master-report.sh && sh /opt/acceptance-test-reports/acceptance-test-master-report.sh snapshotDir="https://dxcore-test-results-secure.team-q-dev.com/DX-95-TestReports/${pipelineParameters.reportBuildname}_acceptance-test/html" && tar -czf ${pipelineParameters.reportBuildname}_acceptance-test.zip Master-Report.html wtf.css html/*'
                                            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/acceptance-test-reports/ && chmod +x acceptance-test-run.sh && sh acceptance-test-run.sh ${pipelineParameters.reportBuildname}_acceptance-test Acceptance'
                                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r centos@${terraformVarsEC2.instance_private_ip}:/opt/acceptance-test-reports/acceptance-test-combined-runs.xml .
                                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r centos@${terraformVarsEC2.instance_private_ip}:/opt/acceptance-test-reports/dashboard/* .
                                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r centos@${terraformVarsEC2.instance_private_ip}:/opt/acceptance-test-reports/${pipelineParameters.reportBuildname}_acceptance-test.zip .
                                            aws s3 cp ${pipelineParameters.reportBuildname}_acceptance-test.zip s3://dx-testarea/
                                            aws s3 cp acceptance-test-combined-runs.xml s3://dx-testarea/acceptance-test-reports/acceptance-test-combined-runs.xml
                                            aws s3 cp Acceptance-dashboard.html s3://dx-testarea/acceptance-test-reports/Acceptance-dashboard.html
                                            aws s3 cp wtf.css s3://dx-testarea/acceptance-test-reports/wtf.css
                                        """
                                    } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
                                        error "TIMEOUT ${e.toString()}"
                                    } catch (Throwable e) {
                                        caughtException = e;
                                    }
                                    if (caughtException) {
                                        error caughtException.message
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /*
     * Perform proper cleanup to leave a healthy jenkins agent. On build success we clean up the EC2 instance. On fails/unstable EC2 is left up and
     * to be terminated manually.
     */
    post {
        cleanup {
            script {
                try {
                    // Expiration stamp taken from the TTL value
                    env.TF_VAR_EXPIRATION_STAMP = "${pipelineParameters.ttl}"
                    // Environment name passed into TF files
                    env.TF_VAR_INSTANCE_NAME = "${pipelineParameters.environmentHostname}"
                    // Environment owner passed into TF files
                    env.TF_VAR_instance_owner = "${pipelineParameters.buildUser}"
                    terraformVarsEC2.tfvar_instance_name = pipelineParameters.environmentHostname
                    dxTerraformDestroyEc2Instance(terraformVarsEC2)
                } catch (Throwable e) {
                    println('Unable to destroy EC2 instance!')
                }

                /* remove internal instance from known-hosts */
                if (terraformVarsEC2.instance_private_ip) {
                    sh(script: """
                        ssh-keygen -R ${terraformVarsEC2.instance_private_ip} | true
                        rm -f ~/.ssh/known_hosts.old
                    """)
                }

                dxWorkspaceDirectoriesCleanup()
            }
        }
    }
}
