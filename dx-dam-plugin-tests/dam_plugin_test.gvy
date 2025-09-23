/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2022, 2024. All Rights Reserved. *
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

// test map for creating a new EC2 instance
def terraformVarsEC2 = [:]

// Create object to store parameters with values
def pipelineParameters = [:]
def deploymentTargetBranch
def deploymentTargetRepo

pipeline {
  // Runs in build_infra, since we are creating infrastructure
   agent {
        label 'build_infra'
   }
   
   stages {
      // Load the pipeline parameters, common modules and configuration
        stage('Load parameters and configuration') {
            steps {
                dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/dx-dam-plugin-tests/parameters.yaml")
                script {
                    env.EXP_API = "https://${pipelineParameters.INSTANCE_NAME}${pipelineParameters.DOMAIN_SUFFIX}/dx/api/core/v1/"
                    env.DAM_API = "https://${pipelineParameters.INSTANCE_NAME}${pipelineParameters.DOMAIN_SUFFIX}/dx/api/dam/v1/"

                    pipelineParameters.buildUser = dxJenkinsGetJobOwner()
                    println("Instance owner will be > ${pipelineParameters.buildUser} <.")

                    

                    if (pipelineParameters.DEPLOYMENT_TARGET_BRANCH.contains('release') ) {
                        deploymentTargetBranch = 'release'
                        deploymentTargetRepo = 'quintana-docker-prod'
                    } else {
                        deploymentTargetBranch = 'develop'
                        deploymentTargetRepo = 'quintana-docker'
                    }

                    // determine build version and label current job accordingly
                    def dateFormat = new SimpleDateFormat('yyyyMMdd-HHmmssSSS')
                    def date = new Date()

                    // Adjust display name of current run
                    def currentDate = "${dateFormat.format(date)}"
                    currentBuild.displayName = "${pipelineParameters.INSTANCE_NAME}_${currentDate}"
                    def removeHTTPFromDisplayName = currentBuild.displayName.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)","")
                    def tempReportBuildName = removeHTTPFromDisplayName.split('/')
                    tempReportBuildName = tempReportBuildName[0].replace(':','')
                    if (removeHTTPFromDisplayName.contains('/')) {
                        tempReportBuildName = "${tempReportBuildName}_${currentDate}"
                        tempReportBuildName = tempReportBuildName.replace(':','')
                    }
                    pipelineParameters.reportBuildname = tempReportBuildName
                    println("Report build name is > ${pipelineParameters.reportBuildname} <.")

                }
            }
        }

        // Terraform install
        stage('Install Terraform') {
            steps {
                script {
                    terraformVarsEC2.tfvar_instance_name = pipelineParameters.AWS_INSTANCE_NAME
                    terraformVarsEC2.tfvar_instance_owner = dxJenkinsGetJobOwner()
                    terraformVarsEC2.tfvar_aws_ec2_instance_type = pipelineParameters.AWS_INSTANCE_TYPE
                    terraformVarsEC2.tfvar_instance_popo_schedule = pipelineParameters.POPO_SCHEDULE
                    terraformVarsEC2.tfvar_instance_area = "TEST"
                    terraformVarsEC2.tfvar_instance_savings = "POPO_Manual"
                    terraformVarsEC2.instance_adm_user = "centos"
                }
                dxTerraformInstall (platform: "alma")
                echo "Terraform Installation done"
            }
        }

        // Launch the EC2 instance with our target parameters
        stage('Create EC2 Instance') {
            steps {
                script {
                    try {
                        terraformVarsEC2 = dxTerraformCreateEc2Instance(terraformVarsEC2)
                        // extract private ip, dns and id of created instance
                        def instanceIp = terraformVarsEC2.instance_private_ip
                        def instanceId = terraformVarsEC2.instance_id
                        echo "Instance ${instanceId} running on ${instanceIp}."
                        env.INSTANCE_IP = instanceIp
                        env.INSTANCE_ID = instanceId
                    } catch (err) {
                        error('ERROR: Creating EC2 instance failed.')
                    }
                        println 'terraformVarsEC2 = ' + terraformVarsEC2
                        println 'Test OK'
                }
            }
        }

         /*
         * After a successful creation of the EC2 instance, we install all required software on it and make sure that our settings
         * will be copied over to the target machine.
         */
        stage('Prepare EC2 instance') {
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    sh """
                        chmod 600 ${DEPLOY_KEY}
                        scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/dx-acceptance-tests/scripts/install-prereqs.sh centos@${terraformVarsEC2.instance_private_ip}:/tmp
                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'chmod +x /tmp/install-prereqs.sh && sh /tmp/install-prereqs.sh'
                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sudo mkdir -p /opt/google-vision-acceptance-test-reports && sudo chown centos: /opt/google-vision-acceptance-test-reports'
                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'mkdir -p /opt/google-vision-acceptance-test-reports/html && mkdir -p /opt/google-vision-acceptance-test-reports/xml'
                        scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r ${workspace}/test-scripts/TestReport/* centos@${terraformVarsEC2.instance_private_ip}:/opt/google-vision-acceptance-test-reports
                        scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r ${workspace}/dx-core-tests/dx-test-rest/helpers/Saxon-HE-9.5.1-3.jar centos@${terraformVarsEC2.instance_private_ip}:/opt/google-vision-acceptance-test-reports/Saxon-HE-9.5.1-3.jar
                    """
                }
            }
        }
        
        // Launch the native kube instance with our target parameters
        stage('Deploying the application in k8 environment') {
            steps {
                script {
                    buildParameters = []
                    buildParameters.add(string(name: 'INSTANCE_NAME', value: pipelineParameters.INSTANCE_NAME))
                    buildParameters.add(string(name: 'NAMESPACE', value: pipelineParameters.NAMESPACE))
                    buildParameters.add(string(name: 'DOMAIN_SUFFIX', value: pipelineParameters.DOMAIN_SUFFIX))
                    buildParameters.add(string(name: 'HOSTED_ZONE', value: pipelineParameters.HOSTED_ZONE))
                    buildParameters.add(string(name: 'BUILD_USER_ID', value: pipelineParameters.buildUser))
                    buildParameters.add(string(name: 'NEXT_JOB_DELAY_HOURS', value: '0'))
                    
                    buildParameters.add(string(name: 'IMAGE_REPOSITORY', value: deploymentTargetRepo))
                    buildParameters.add(string(name: 'CORE_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'DAM_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'CC_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'DESIGN_STUDIO_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'DAM_PLUGIN_GOOGLE_VISION_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'RINGAPI_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'IMGPROC_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'DAM_KALTURA_PLUGIN_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'RS_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'RUNTIME_CONTROLLER_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'PERSISTENCE_NODE_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'PERSISTENCE_CONNECTION_POOL_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'HAPROXY_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'LICENSE_MANAGER_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'LOGGING_SIDECAR_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'PERSISTENCE_METRICS_EXPORTER_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'LDAP_IMAGE_FILTER', value: deploymentTargetBranch))
                    buildParameters.add(string(name: 'HELM_CHARTS_FILTER', value: deploymentTargetBranch))

                    buildParameters.add(booleanParam(name: 'DISABLE_PLUGIN_GOOGLE_VISION', value: "false"))
                    
                    buildParameters.add(string(name: 'NATIVE_POPO_SCHEDULE', value: pipelineParameters.POPO_SCHEDULE))
                    
                    build(job: "${pipelineParameters.KUBE_DEPLOY_JOB}", 
                        parameters: buildParameters, 
                        propagate: true,
                        wait: true)
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
                    sshagent(credentials: ['jenkins-git']) {
                        script {
                            sh """
                                chmod 600 ${DEPLOY_KEY}
                                git clone -b ${pipelineParameters.TARGET_BRANCH} git@git.cwp.pnp-hcl.com:websphere-portal-incubator/media-library.git ${workspace}/media-library
                                ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'sudo mkdir -p /opt/media-library && sudo chown centos: /opt/media-library'
                                scp -r -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/media-library centos@${terraformVarsEC2.instance_private_ip}:/opt
                            """
                        }
                    }
                }
            } 
        }


        /*
         * Each application would then install its dependencies and run the migration tests. Timeout is currently set at 30 mins per application stage.
         * Timeout is also treated as a failure, and is caught using org.jenkinsci.pligins.workflow.steps.FlowInterruptedException. Otherwise timeouts
         * are going to be registered as ABORTED in jenkins status report. The other catch is for any other errors produced by the test. 
         */ 
        stage('Run DAM server plugins acceptance tests') {
            options {
                timeout(time: 30, unit: 'MINUTES') 
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    script {
                        Exception caughtException = null;
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            try {
                                echo "Running tests"
                                echo "DAM API will be: ${env.DAM_API}"
                                echo "EXIM API will be: ${env.EXP_API}"
                                echo "INSTANCE_IP will be: ${terraformVarsEC2.instance_private_ip}"
                                sh """
                                    chmod 600 ${DEPLOY_KEY}
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/media-library && make scope && make install && make buildServer'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/media-library/packages/server-v1 && make install && make build && make ${pipelineParameters.TEST_COMMAND} ring_api=${env.EXP_API} dam_api=${env.DAM_API} insecure=${pipelineParameters.SSL_ENABLED}'
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cp /opt/media-library/packages/server-v1/test-report/GOOGLE_VISION.html /opt/google-vision-acceptance-test-reports/html/ 2>/dev/null || : && cp /opt/media-library/packages/server-v1/test-report/GOOGLE_VISION.xml /opt/google-vision-acceptance-test-reports/xml/ 2>/dev/null || :'
                                """
                            } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
                                error "TIMEOUT ${e.toString()}"
                            } catch (Throwable e) {
                                caughtException = e;
                            }
                            if (caughtException) {
                                sh """
                                    ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cp /opt/media-library/packages/server-v1/test-report/GOOGLE_VISION.html /opt/google-vision-acceptance-test-reports/html/ 2>/dev/null || : && cp /opt/media-library/packages/server-v1/test-report/GOOGLE_VISION.xml /opt/google-vision-acceptance-test-reports/xml/ 2>/dev/null || :'
                                """
                                error caughtException.message
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
                                        echo "Generating test Google-vision-acceptance-dashboard report"
                                        sh """
                                            chmod 600 ${DEPLOY_KEY}
                                            aws s3 cp s3://dx-testarea/google-vision-acceptance-test-reports/google-vision-acceptance-test-runs.xml google-vision-acceptance-test-runs.xml
                                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/google-vision-acceptance-test-runs.xml centos@${terraformVarsEC2.instance_private_ip}:/opt/google-vision-acceptance-test-reports/
                                            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/google-vision-acceptance-test-reports && source ~/.bash_profile && chmod +x /opt/google-vision-acceptance-test-reports/google-vision-acceptance-test-master-report.sh && sh /opt/google-vision-acceptance-test-reports/google-vision-acceptance-test-master-report.sh snapshotDir="https://dxcore-test-results-secure.team-q-dev.com/DX-95-TestReports/${pipelineParameters.reportBuildname}_acceptance-test/html" && tar -czf ${pipelineParameters.reportBuildname}_acceptance-test.zip Master-Report.html wtf.css html/*'
                                            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/google-vision-acceptance-test-reports/ && chmod +x google-vision-acceptance-test-run.sh && sh google-vision-acceptance-test-run.sh ${pipelineParameters.reportBuildname}_acceptance-test Google-Vision-Acceptance'
                                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r centos@${terraformVarsEC2.instance_private_ip}:/opt/google-vision-acceptance-test-reports/google-vision-acceptance-test-runs.xml .
                                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r centos@${terraformVarsEC2.instance_private_ip}:/opt/google-vision-acceptance-test-reports/dashboard/* .
                                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r centos@${terraformVarsEC2.instance_private_ip}:/opt/google-vision-acceptance-test-reports/${pipelineParameters.reportBuildname}_acceptance-test.zip .
                                            aws s3 cp ${pipelineParameters.reportBuildname}_acceptance-test.zip s3://dx-testarea/
                                            aws s3 cp google-vision-acceptance-test-runs.xml s3://dx-testarea/google-vision-acceptance-test-reports/google-vision-acceptance-test-runs.xml
                                            aws s3 cp Google-Vision-Acceptance-dashboard.html s3://dx-testarea/google-vision-acceptance-test-reports/Google-Vision-Acceptance-dashboard.html
                                            aws s3 cp wtf.css s3://dx-testarea/google-vision-acceptance-test-reports/wtf.css
                                        """
                                    } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
                                        error "TIMEOUT ${e.toString()}"
                                    } catch (Throwable e) {
                                        caughtException = e;
                                    }
                                    if (caughtException) {
                                        sh """
                                            chmod 600 ${DEPLOY_KEY}
                                            aws s3 cp s3://dx-testarea/google-vision-acceptance-test-reports/google-vision-acceptance-test-runs.xml google-vision-acceptance-test-runs.xml
                                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/google-vision-acceptance-test-runs.xml centos@${terraformVarsEC2.instance_private_ip}:/opt/google-vision-acceptance-test-reports/
                                            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/google-vision-acceptance-test-reports/ && chmod +x google-vision-acceptance-test-failure-xml.sh && sh google-vision-acceptance-test-failure-xml.sh ${pipelineParameters.reportBuildname}_acceptance-test ${env.BUILD_URL}'
                                            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /opt/google-vision-acceptance-test-reports/ && chmod +x google-vision-acceptance-failure-test-run.sh && sh google-vision-acceptance-failure-test-run.sh ${pipelineParameters.reportBuildname}_acceptance-test Google-Vision-Acceptance'
                                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r centos@${terraformVarsEC2.instance_private_ip}:/opt/google-vision-acceptance-test-reports/google-vision-acceptance-test-runs.xml .
                                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r centos@${terraformVarsEC2.instance_private_ip}:/opt/google-vision-acceptance-test-reports/dashboard/* .
                                            aws s3 cp google-vision-acceptance-test-runs.xml s3://dx-testarea/google-vision-acceptance-test-reports/google-vision-acceptance-test-runs.xml
                                            aws s3 cp Google-Vision-Acceptance-dashboard.html s3://dx-testarea/google-vision-acceptance-test-reports/Google-Vision-Acceptance-dashboard.html
                                            aws s3 cp wtf.css s3://dx-testarea/google-vision-acceptance-test-reports/wtf.css
                                        """
                                        error caughtException.message
                                    }
                                }
                            }
                        }
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
            //Cleanup workspace
            dxTerraformDestroyEc2Instance(terraformVarsEC2)
            dxWorkspaceDirectoriesCleanup()
        }
    }
}