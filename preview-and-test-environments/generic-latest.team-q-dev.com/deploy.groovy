/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2001, 2021. All Rights Reserved. *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

import java.text.SimpleDateFormat
import groovy.json.JsonSlurper

pipeline { 
    /*
     * This whole process should not take longer than 2 hours
     */
    options {
      timeout(time: 120, unit: 'MINUTES') 
    }    

    agent {
        label 'build_infra'    
    }

    stages {
        /*
         * We have to get the current branch name in order to determine hostnames and artifact filters.
         * When checking code out from git, jenkins provides an environment variable named GIT_BRANCH.
         * This variable can be leverage and must be cleaned up, to ensure proper formatting.
         */
        stage("Load modules and configuration") {
            steps {
                script {
                    commonModule = load "./preview-and-test-environments/generic-latest.team-q-dev.com/helper/common.gvy"
                }
            }
        }
        stage('Prepare Settings') {
            steps {
                script {
                    echo "Determine branch, will be used for filtering the artifacts to pull."
                    env.ESCAPED_GIT_BRANCH_NAME = "${GIT_BRANCH}".replace("origin/", "").replace("/", "_")
                    echo "Full name is ${env.GIT_BRANCH} - escaped and reduced to ${ESCAPED_GIT_BRANCH_NAME}."
                    /*
                     * This is a override section for all image filters.
                     * If no filters have been set by the user in jenkins, the branch will be used to filter artifacts.
                     */
                    if (!env.DX_CORE_IMAGE_FILTER){
                      env.DX_CORE_IMAGE_FILTER = "${env.ESCAPED_GIT_BRANCH_NAME}"
                    }
                    if (!env.MEDIA_LIBRARY_IMAGE_FILTER){
                      env.MEDIA_LIBRARY_IMAGE_FILTER = "${env.ESCAPED_GIT_BRANCH_NAME}"
                    }
                    if (!env.IMG_PROCESSOR_IMAGE_FILTER){
                      env.IMG_PROCESSOR_IMAGE_FILTER = "${env.ESCAPED_GIT_BRANCH_NAME}"
                    }
                    if (!env.EXPERIENCE_API_IMAGE_FILTER){
                      env.EXPERIENCE_API_IMAGE_FILTER = "${env.ESCAPED_GIT_BRANCH_NAME}"
                    }                    
                    if (!env.CONTENT_UI_IMAGE_FILTER){
                      env.CONTENT_UI_IMAGE_FILTER = "${env.ESCAPED_GIT_BRANCH_NAME}"
                    }
                    if (!env.SITE_MANAGER_IMAGE_FILTER){
                      env.SITE_MANAGER_IMAGE_FILTER = ""
                    }
                    if (!env.DX_REPO_HOST) {
                      env.DX_REPO_HOST = "${G_ARTIFACTORY_DOCKER_NAME}.${G_ARTIFACTORY_HOST}"
                    }
                    if (!env.PUBLIC_SUBNET){
                      env.PUBLIC_SUBNET = "subnet-021d6ef8ad5d03bc0"
                    }
                    if (!env.PUBLISH_EXTERNAL){
                      env.PUBLISH_EXTERNAL = "false"
                    }
                    if (!env.ARTIFACTORY_HOST){
                        env.ARTIFACTORY_HOST = "quintana-docker.artifactory.cwp.pnp-hcl.com"
                    }
                    if (!env.ARTIFACTORY_IMAGE_BASE_URL){
                        env.ARTIFACTORY_IMAGE_BASE_URL = "https://artifactory.cwp.pnp-hcl.com/artifactory/list/quintana-docker"
                    }
                    if (!env.PRIVATE_IP_PREFIX){
                        env.PRIVATE_IP_PREFIX = '10.134.'
                    }
                    if (!env.DX_CORE_IMAGE_LOCALES){
                      env.DX_CORE_IMAGE_LOCALES = "SKIP"
                    }

                    "Filtering dx-core images for ${DX_CORE_IMAGE_FILTER}"
                    "Filtering media-library images for ${MEDIA_LIBRARY_IMAGE_FILTER}"
                    "Filtering image-processor images for ${IMG_PROCESSOR_IMAGE_FILTER}"
                    "Filtering experience-api images for ${EXPERIENCE_API_IMAGE_FILTER}"
                    "Filtering content-ui images for ${CONTENT_UI_IMAGE_FILTER}"
                    "Filtering site-manager images for ${SITE_MANAGER_IMAGE_FILTER}"
                    "Friendly URLs set to: ${DX_CORE_IMAGE_LOCALES}"
                }
                /*
                 * There are multiple files that require the correct hostname to be filled in.
                 * We determine the hostname via the GIT_BRANCH and replace all placeholders in config files.
                 * If the parameter for a custom hostname is empty, we use a generated one.
                 */
                dir("${WORKSPACE}/preview-and-test-environments/generic-latest.team-q-dev.com") {
                    script {
                      echo "Replacing hostname for created environment in all config files."
                      if (!env.ENV_HOSTNAME){
                        env.ENV_HOSTNAME = "${env.ESCAPED_GIT_BRANCH_NAME}-latest.team-q-dev.com"
                      }
                      echo "New hostname will be: ${env.ENV_HOSTNAME}"
                      env.TF_VAR_ENV_HOSTNAME = env.ENV_HOSTNAME
                      sh(script: "sed -i 's/\$ENV_HOSTNAME/${env.ENV_HOSTNAME}/g' ./settings/settings.sh")
                      sh(script: "sed -i 's/\$ENV_HOSTNAME/${env.ENV_HOSTNAME}/g' ./scripts/pull-and-run-dx.sh")
                      // determine build version and label current job accordingly
                      def dateFormat = new SimpleDateFormat("yyyyMMdd-HHmm")
                      def date = new Date()
                      // Display name includes the ENV_HOSTNAME and a timestamp
                      currentBuild.displayName = "${env.ENV_HOSTNAME}_${dateFormat.format(date)}"
                      env.TF_VAR_BUILD_LABEL = "${env.ENV_HOSTNAME}"
                      env.TF_VAR_TEST_RUN_ID = "${env.ENV_HOSTNAME}"
                    }
                }
            }
        }

        /*
         *  Preparing terraform to run in the current workspace. Terraform abstracts the AWS access and handles resource lifecycles and deployments
         */
        stage('Prepare Terraform') {
            steps {
                sh """
                    curl -LJO https://releases.hashicorp.com/terraform/0.12.20/terraform_0.12.20_linux_amd64.zip
                    unzip terraform_0.12.20_linux_amd64.zip
                    chmod +x terraform
                    ./terraform --help
                """
            }
        }

        /*
         *  If we are deploying any environment, we must try to remove it before we can create a new one
         *  This is especially important if we have an external environment, since they need an internal one created first which is then moved
         *  to external (for artifactory access reasons.)
         */
        stage('Remove existing EC2 instance') {
            steps {
                script {
                    /*
                     * We need the AWS credentials for terraform and the deploy key to have proper SSH access to instances we created.
                     */ 
                    withCredentials([
                        usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')
                    ]) {
                        def pingResult = sh(script: """
                            ping -c 1 -w 2 ${ENV_HOSTNAME}
                        """, returnStatus: true)
                        if(pingResult == 0) {
                            echo "Found an instance, will determine by IP if internal or external."
                            def ipResult = sh(script: """
                                ping -c 1 -w 2 ${ENV_HOSTNAME}
                                """, returnStdout: true)
                            if(ipResult.indexOf(env.PRIVATE_IP_PREFIX) != -1) {
                                echo "This is an internal instance."
                                dir("${workspace}/preview-and-test-environments/generic-latest.team-q-dev.com/terraform/ec2-launch") {
                                    sh(script: """
                                        sed -i 's/ENVIRONMENT_HOSTNAME/${env.ENV_HOSTNAME}/g' variables.tf
                                        ${workspace}/terraform init
                                        ${workspace}/terraform destroy -auto-approve
                                    """)
                                }
                            } else {
                                dir("${workspace}/preview-and-test-environments/generic-latest.team-q-dev.com/terraform/external-ec2-launch") {
                                    echo "This is an external instance."
                                    sh(script: """
                                        sed -i 's/ENVIRONMENT_HOSTNAME/${env.ENV_HOSTNAME}/g' variables.tf
                                        ${workspace}/terraform init
                                        ${workspace}/terraform destroy -auto-approve
                                    """)    
                                }    
                                dir("${workspace}/preview-and-test-environments/generic-latest.team-q-dev.com/terraform/ami-creation") {
                                    env.TF_VAR_TEST_EC2_ID = env.INSTANCE_ID
                                    sh(script: """
                                        ${workspace}/terraform init
                                        ${workspace}/terraform destroy -auto-approve
                                    """)    
                                }
                            }                        
                        }
                    }
                }
            }
        }        
        

        /*
         * Run terraform to create an EC2 instance based on the terraform scripting and add an route53 record for it.
         */
        stage('Create EC2 instance') {
            steps {
                script {
                    /*
                     * We need the AWS credentials for terraform and the deploy key to have proper SSH access to instances we created.
                     */ 
                    withCredentials([
                        usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')
                    ]) {
                        configFileProvider([
                          configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                        ]) {
                            dir("${workspace}/preview-and-test-environments/generic-latest.team-q-dev.com/terraform/ec2-launch") {
                                // replace placeholder in the variables.tf to fit the current environment
                                sh(script: """
                                    sed -i 's/ENVIRONMENT_HOSTNAME/${env.ENV_HOSTNAME}/g' variables.tf
                                    ${workspace}/terraform init
                                    ${workspace}/terraform apply -auto-approve
                                """)
                                def instanceInformation = sh(script: """
                                    ${workspace}/terraform show -json
                                """, returnStdout: true).trim()
                                def instanceJsonInformation = readJSON text: instanceInformation
                                // extract private ip, dns and id of created instance
                                def instanceIp = instanceJsonInformation['values']['root_module']['resources'][0]['values']['private_ip']
                                def instanceDns = instanceJsonInformation['values']['root_module']['resources'][0]['values']['private_dns']
                                def instanceId = instanceJsonInformation['values']['root_module']['resources'][0]['values']['id']
                                echo "Instance ${instanceId} running on ${instanceIp}."
                                // set instanceIp, instanceDns and instanceId as variable for later use
                                env.INSTANCE_IP = instanceIp
                                env.INSTANCE_DNS = instanceDns
                                env.INSTANCE_ID = instanceId
                                // test connection to instance via ssh
                                sh(script: """
                                    chmod 600 ${DEPLOY_KEY}
                                    export TARGET_IP=${INSTANCE_IP}
                            	    sh ${workspace}/preview-and-test-environments/generic-latest.team-q-dev.com/scripts/wait_for_instance.sh
                                """)
                            }
                        }
                    }
                }             
            }
        }

        /*
         * After a successful creation of the EC2 instance, we install all required software on it and make sure that our settings
         * will be copied over to the target machine. We also perform a docker login to download the images we need.
         */ 
        stage('Prepare EC2 instance') {
            steps {
                  withCredentials([
                    usernamePassword(credentialsId: 'artifactory', passwordVariable: 'ARTIFACTORY_PASSWORD', usernameVariable: 'ARTIFACTORY_USER')
                  ]) {
                      configFileProvider([
                          configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                      ]) {
                          sh """
                            chmod 600 ${DEPLOY_KEY}
                            sh ${workspace}/preview-and-test-environments/generic-latest.team-q-dev.com/settings/env-generator.sh ${workspace}
                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/preview-and-test-environments/generic-latest.team-q-dev.com/scripts/install-prereqs.sh centos@${env.INSTANCE_IP}:/tmp
                            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'chmod +x /tmp/install-prereqs.sh && sh /tmp/install-prereqs.sh'
                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/preview-and-test-environments/generic-latest.team-q-dev.com/scripts/relaunch_container.sh centos@${env.INSTANCE_IP}:/tmp
                            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'chmod +x /tmp/relaunch_container.sh'
                            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'docker login ${env.ARTIFACTORY_DOCKER_URL} -u ${ARTIFACTORY_USER} -p ${ARTIFACTORY_PASSWORD}'
                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/preview-and-test-environments/generic-latest.team-q-dev.com/settings/settings.sh centos@${env.INSTANCE_IP}:/tmp
                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/env.sh centos@${env.INSTANCE_IP}:/tmp
                            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'chmod +x /tmp/env.sh'
                            rm -f ${workspace}/env.sh
                          """
                      } 
                }
            }
        }

        /*
         * We create a docker network to enable easy and proper inter-service communication.
         * The applications can internally be called by their names e.g. http://dx-core:10039
         */
        stage('Create docker network') {
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                  sh """
                      chmod 600 ${DEPLOY_KEY}
                      scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/preview-and-test-environments/generic-latest.team-q-dev.com/scripts/create-docker-network.sh centos@${env.INSTANCE_IP}:/tmp
                      ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'chmod +x /tmp/create-docker-network.sh && . /tmp/env.sh && . /tmp/settings.sh && sh /tmp/create-docker-network.sh'
                  """
                }
            }
        }
        /*
         * All following steps perform the same flow: Get a list of images from artifactory,
         * filter the list with the IMAGE_FILTER variables, sort it from newest to oldest and
         * pick the newest one and deploy it.
         */
        stage('Pull and run latest dx-core image') {
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        if(DX_CORE_IMAGE_FILTER != "SKIP") {
                            sh(script: """
                                chmod 600 ${DEPLOY_KEY}
                                scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/preview-and-test-environments/generic-latest.team-q-dev.com/scripts/pull-and-run-dx.sh centos@${env.INSTANCE_IP}:/tmp
                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'chmod +x /tmp/pull-and-run-dx.sh && . /tmp/env.sh && . /tmp/settings.sh && sh /tmp/pull-and-run-dx.sh'
                            """)
                            if(DX_CORE_IMAGE_LOCALES != "SKIP")
                            {
                                sh(script: """
                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'docker exec dx-core /opt/HCL/wp_profile/ConfigEngine/ConfigEngine.sh enable-friendly-locale-urls -Dfriendly-locale-list="${DX_CORE_IMAGE_LOCALES}" -DWasPassword=wpsadmin -DPortalAdminPwd=wpsadmin'
                                """)
                            }
                        } else {
                            echo "Skipping dx-core image deployment."
                        }
                    }
                }
            }
        }
        stage('Pull and run latest Experience API image') {
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        if(EXPERIENCE_API_IMAGE_FILTER != "SKIP") {
                            sh(script: """
                                chmod 600 ${DEPLOY_KEY}
                                scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/preview-and-test-environments/generic-latest.team-q-dev.com/scripts/pull-and-run-experience-api.sh centos@${env.INSTANCE_IP}:/tmp
                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'chmod +x /tmp/pull-and-run-experience-api.sh && . /tmp/env.sh && . /tmp/settings.sh && sh /tmp/pull-and-run-experience-api.sh'
                            """)
                        } else {
                            echo "Skipping experience-api deployment."
                        }
                    }
                }
            }
        }
        stage('Pull and run image processor') {
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        if(IMG_PROCESSOR_IMAGE_FILTER != "SKIP") {
                            sh(script: """
                                chmod 600 ${DEPLOY_KEY}
                                scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/preview-and-test-environments/generic-latest.team-q-dev.com/scripts/pull-and-run-image-processor.sh centos@${env.INSTANCE_IP}:/tmp
                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'chmod +x /tmp/pull-and-run-image-processor.sh && . /tmp/env.sh && . /tmp/settings.sh && sh /tmp/pull-and-run-image-processor.sh'
                            """)
                        } else {
                            echo "Skipping image-processor deployment."
                        }
                    }
                } 
            }
        }
        stage('Pull and run media library') {
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        if(MEDIA_LIBRARY_IMAGE_FILTER != "SKIP") {
                            sh(script: """
                                chmod 600 ${DEPLOY_KEY}
                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'sudo rm -rf /opt/media-library/ && sudo mkdir -p /opt/media-library && sudo chown centos: /opt/media-library'
                                scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/preview-and-test-environments/generic-latest.team-q-dev.com/config/media-library/docker-compose.yaml centos@${env.INSTANCE_IP}:/opt/media-library
                                scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/preview-and-test-environments/generic-latest.team-q-dev.com/scripts/pull-and-run-media-library.sh centos@${env.INSTANCE_IP}:/tmp
                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'chmod +x /tmp/pull-and-run-media-library.sh && . /tmp/env.sh && . /tmp/settings.sh && sh /tmp/pull-and-run-media-library.sh'
                            """)
                        } else {
                            echo "Skipping media-library deployment."
                        }
                    } 
                } 
            }
        }
       
        stage('Pull and run content-ui') {
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        if(CONTENT_UI_IMAGE_FILTER != "SKIP") {
                            sh(script: """
                                chmod 600 ${DEPLOY_KEY}
                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'sudo rm -rf /opt/content-ui/ && sudo mkdir -p /opt/content-ui && sudo chown centos: /opt/content-ui'
                                scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/preview-and-test-environments/generic-latest.team-q-dev.com/scripts/pull-and-run-content-ui.sh centos@${env.INSTANCE_IP}:/tmp
                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'chmod +x /tmp/pull-and-run-content-ui.sh && . /tmp/env.sh && . /tmp/settings.sh && sh /tmp/pull-and-run-content-ui.sh'
                            """)
                        } else {
                            echo "Skipping content-ui deployment."
                        }
                    }
                } 
            }
        }
        stage('Pull and run site-manager') {
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        if(SITE_MANAGER_IMAGE_FILTER?.trim() && SITE_MANAGER_IMAGE_FILTER != "SKIP") {
                            sh(script: """
                                chmod 600 ${DEPLOY_KEY}
                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'sudo rm -rf /opt/site-manager/ && sudo mkdir -p /opt/site-manager && sudo chown centos: /opt/site-manager'
                                scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/preview-and-test-environments/generic-latest.team-q-dev.com/scripts/pull-and-run-site-manager.sh centos@${env.INSTANCE_IP}:/tmp
                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'chmod +x /tmp/pull-and-run-site-manager.sh && . /tmp/env.sh && . /tmp/settings.sh && sh /tmp/pull-and-run-site-manager.sh'
                            """)
                        } else {
                            echo "Skipping site-manager deployment."
                        }
                    }
                } 
            }
        }
        stage('Configure Kaltura Plugin') {
            steps {
                 script {
                    withCredentials([
                            usernamePassword(credentialsId: "kaltura_credentials", passwordVariable: 'KALTURA_SECRET_KEY', usernameVariable: 'KALTURA_PARTNER_ID')
                        ]) {
                            sh """
                            echo partnerId=$KALTURA_PARTNER_ID
                            """
                            if(!env.KALTURA_DEFAULT_PLAYER_ID){
                                env.KALTURA_DEFAULT_PLAYER_ID = env.G_KALTURA_DEFAULT_PLAYER_ID;
                            }
                            // Login to RingAPI to get accessToken 
                            def loginReq = """
                            {
                                "username":"wpsadmin",
                                "password":"wpsadmin"
                            }
                            """
                            def loginRes = httpRequest(
                                url: "http://${env.ENV_HOSTNAME}:4000/dx/api/core/v1/auth/login",
                                httpMode: 'POST',
                                contentType: 'APPLICATION_JSON',
                                ignoreSslErrors: true,
                                requestBody: "${loginReq}"
                            )
                            sh """
                            echo RingApi Login response status = $loginRes.status
                            """
                            def accessToken = ""
                            if(loginRes && loginRes.status==200){ 
                                sh """
                                echo RingAPI LoggedIn successfully.
                                """
                                accessToken = loginRes.headers['set-cookie'][0]
                            }
                            // Call GET Plugin API to retrive kaltura plugin id
                           def kalturaRes = httpRequest(
                                url: "http://${env.ENV_HOSTNAME}:3000/dx/api/dam/v1/plugins",
                                httpMode: 'GET',
                                contentType: 'APPLICATION_JSON',
                                customHeaders: [[name: 'Cookie', value: "${accessToken}"]]
                            )
                            sh """
                            echo plugin GET API response status = $kalturaRes.status
                            """
                            // Retriving kaltura plugin id 
                            def KALTURA_PLUGIN_ID = ""
                            if(kalturaRes && kalturaRes.status==200){
                                def jsonContents = new JsonSlurper().parseText(kalturaRes.content)
                                jsonContents.each{ k, contents ->
                                    contents.eachWithIndex { items, index ->
                                        items.each{ key, value -> 
                                            if(value.toString().toUpperCase() == 'KALTURA'){
                                                KALTURA_PLUGIN_ID = items['id']
                                            }
                                        }
                                    }
                                }
                            }
                            println "Going to update KALTURA_PLUGIN_ID! ${KALTURA_PLUGIN_ID}"
                           // Call PUT Plugin API to do kaltura configuration
                            def configReq = """
                            {
                                "name":"kaltura",
                                "configuration":{
                                    "partnerId":"${KALTURA_PARTNER_ID}",
                                    "secretKey":"${KALTURA_SECRET_KEY}",
                                    "enable":true,
                                    "playerId":"${env.KALTURA_DEFAULT_PLAYER_ID}"
                                }
                            }
                        """
                        def configResult = httpRequest(
                            url: "http://${env.ENV_HOSTNAME}:3000/dx/api/dam/v1/plugins/${KALTURA_PLUGIN_ID}",
                            contentType: 'APPLICATION_JSON',
                            httpMode: 'PUT',
                            customHeaders: [[name: 'Cookie', value: "${accessToken}"]],
                            ignoreSslErrors: true,
                            requestBody: "${configReq}"
                        )
                        sh """
                            echo plugin PUT API response status = $configResult.status
                        """
                         if(configResult && configResult.status==200){ 
                            sh """
                            echo kaltura plugin configured successfully.
                            """
                        }
                    }
                }
            }
        }

        /*
         *  We need to generate randomized users.
         */
        stage('Generate randomized user password.') {
            when {
                allOf {
                    not {environment name: 'PUBLISH_EXTERNAL', value: ''}
                    environment name: 'PUBLISH_EXTERNAL', value: 'true'
                }
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    dir("${workspace}/preview-and-test-environments/generic-latest.team-q-dev.com/scripts/helpers") {
                        script {
                            sh(script: """
                                chmod 600 ${DEPLOY_KEY}
                                sh generate-users.sh
                                scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/preview-and-test-environments/generic-latest.team-q-dev.com/scripts/helpers/users.py centos@${env.INSTANCE_IP}:/tmp
                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'docker cp /tmp/users.py dx-core:/tmp'
                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'docker exec dx-core /opt/HCL/AppServer/bin/wsadmin.sh -username wpsadmin -password wpsadmin -lang jython -f /tmp/users.py'
                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_IP} 'docker stop dx-core && docker start dx-core'
                                """)
                        }
                    }   
                } 
            }
        }  

        /*
         *  This stage creates an AMI of the created EC2 instance via terraform
         */
        stage('Create AMI image for EC2 test instance') {
            when {
                allOf {
                    not {environment name: 'PUBLISH_EXTERNAL', value: ''}
                    environment name: 'PUBLISH_EXTERNAL', value: 'true'
                }
            }
            steps {
                script {
                    /*
                     * We need the AWS credentials for terraform and the deploy key to have proper SSH access to instances we created.
                     */ 
                    withCredentials([
                        usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')
                    ]) {
                        configFileProvider([
                          configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                        ]) {
                           dir("${workspace}/preview-and-test-environments/generic-latest.team-q-dev.com/terraform/ami-creation") {
                                // Create variable for Terraform to determine current test run
                                env.TF_VAR_TEST_EC2_ID = env.INSTANCE_ID
                                sh(script: """
                                    ${workspace}/terraform init
                                    ${workspace}/terraform apply -auto-approve
                                """)
                            }
                        }
                    }
                }
            }
        }
        /*
         *  This stage deletes the created EC2 instance via terraform including the route53 record as it is not needed anymore
         */
        stage('Delete internal facing EC2') {
            when {
                allOf {
                    not {environment name: 'PUBLISH_EXTERNAL', value: ''}
                    environment name: 'PUBLISH_EXTERNAL', value: 'true'
                }
            }
            steps {
                script {
                    /*
                     * We need the AWS credentials for terraform and the deploy key to have proper SSH access to instances we created.
                     */ 
                    withCredentials([
                        usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')
                    ]) {
                        configFileProvider([
                          configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                        ]) {
                            dir("${workspace}/preview-and-test-environments/generic-latest.team-q-dev.com/terraform/ec2-launch") {
                                sh(script: """
                                    ${workspace}/terraform init
                                    ${workspace}/terraform destroy -auto-approve
                                """)
                            }
                        }
                    }
                }
            }
        }
        /*
         *  This stage starts the external facing EC2 instance from the previously created AMI
         */
        stage('Create external EC2 instance for testing and relaunch') {
            when {
                allOf {
                    not {environment name: 'PUBLISH_EXTERNAL', value: ''}
                    environment name: 'PUBLISH_EXTERNAL', value: 'true'
                }
            }
            steps {
                script {
                    /*
                     * We need the AWS credentials for terraform and the deploy key to have proper SSH access to instances we created.
                     */ 
                    withCredentials([
                        usernamePassword(credentialsId: "aws_credentials", passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')
                    ]) {
                        configFileProvider([
                          configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                        ]) {
                            dir("${workspace}/preview-and-test-environments/generic-latest.team-q-dev.com/terraform/external-ec2-launch") {
                                sh(script: """
                                    sed -i 's/ENVIRONMENT_HOSTNAME/${env.ENV_HOSTNAME}/g' variables.tf
                                    ${workspace}/terraform init
                                    ${workspace}/terraform apply -auto-approve
                                """)
                                def instanceInformation = sh(script: """
                                    ${workspace}/terraform show -json
                                """, returnStdout: true).trim()
                                def instanceJsonInformation = readJSON text: instanceInformation
                                // extract private ip, dns and id of created instance
                                def instanceIp = instanceJsonInformation['values']['root_module']['resources'][0]['values']['private_ip']
                                def instancePublicIp = instanceJsonInformation['values']['root_module']['resources'][0]['values']['public_ip']
                                def instanceDns = instanceJsonInformation['values']['root_module']['resources'][0]['values']['private_dns']
                                def instanceId = instanceJsonInformation['values']['root_module']['resources'][0]['values']['id']
                                echo "Instance ${instanceId} running internal on ${instanceIp} and external facing on ${instancePublicIp}."
                                // set instancePublicIp as variable for later use
                                env.INSTANCE_PUBLIC_IP = instancePublicIp
                            }
                            // test connection to instance via ssh
                            sh(script: """
                                chmod 600 ${DEPLOY_KEY}
                                export TARGET_IP=${INSTANCE_PUBLIC_IP}
                            	sh ${workspace}/preview-and-test-environments/generic-latest.team-q-dev.com/scripts/wait_for_instance.sh
                            	ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${env.INSTANCE_PUBLIC_IP} 'chmod +x /tmp/relaunch_container.sh && . /tmp/env.sh && . /tmp/settings.sh && /tmp/relaunch_container.sh'
                            """)
                        }
                    }
                }
            }
        }
        /*
         *  This stage runs acceptance tests for Site Manager and other Applications
         */
/*        stage('Run acceptance test for DX latest servers') {
            steps {
                script {
                    def HAS_SM = (SITE_MANAGER_IMAGE_FILTER?.trim() && SITE_MANAGER_IMAGE_FILTER != "SKIP")

                    buildParams = commonModule.createAcceptanceTestJobParams(env.ENV_HOSTNAME, params.CONTEXT_ROOT_PATH, params.DX_CORE_HOME_PATH, HAS_SM)

                    catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                        build(job: commonConfig.ACCEPTANCE_TEST_JOB, 
                        parameters: buildParams, 
                        propagate: true, 
                        wait: true)
                    }
                }
            }
        }
 */ } 

    /*
     * Perform proper cleanup to leave a healthy jenkins agent.
     */ 
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
                
                /* remove internal instance from known-hosts */
                if (env.INSTANCE_IP) {
                    sh(script: """
                        ssh-keygen -R ${env.INSTANCE_IP} | true
                        rm -f ~/.ssh/known_hosts.old
                    """)
                }
                
                /* remove external instance from known-hosts */
                if (env.INSTANCE_PUBLIC_IP) {
                    sh(script: """
                        ssh-keygen -R ${env.INSTANCE_PUBLIC_IP} | true
                        rm -f ~/.ssh/known_hosts.old
                    """)
                }
            }
        }
    }
}
