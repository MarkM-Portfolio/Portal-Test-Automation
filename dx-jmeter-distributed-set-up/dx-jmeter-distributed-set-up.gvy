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

@Library('dx-shared-library') _

// test map for creating a new EC2 instance
def terraformVarsEC2 = [:]
def jmeterInstanceDetails = []
def pipelineParameters = [:]
def masterInstance = [:]  

pipeline {
    agent {
        label 'build_infra'
    }

    stages {
        stage('Load Parameters') {
            steps {
                script {
                    dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/dx-jmeter-distributed-set-up/parameters.yaml")
                }
            }
        }

        // Prepare settings
        stage('Prepare Settings') {
            steps {
                script {
                    def dateFormat = new SimpleDateFormat('yyyyMMdd-HHmmss')
                    def date = new Date()

                    masterInstance.tfvar_aws_ec2_instance_type = pipelineParameters.EC2_INSTANCE_TYPE
                    masterInstance.tfvar_instance_area = pipelineParameters.INSTANCE_AREA

                    INSTANCE_OWNER = dxJenkinsGetJobOwner()
                    masterInstance.tfvar_instance_owner = "${INSTANCE_OWNER}"

                    echo 'Assigning hostname + timestamp'
                    masterInstance.tfvar_instance_name = "${pipelineParameters.DX_INSTANCE_NAME}_${dateFormat.format(date)}"
                    echo "New instance will be: ${masterInstance.tfvar_instance_name}"

                    masterInstance.tfvar_aws_subnet = pipelineParameters.AWS_SUBNET_ID
                    echo "AWS Subnet name : ${pipelineParameters.AWS_SUBNET_NAME}"
                    echo "AWS Subnet ID : ${masterInstance.tfvar_aws_subnet}"
                }
            }
        }

        // Terraform install
        stage('Install Terraform') {
            steps {
                script {
                    dxTerraformInstall()
                    echo 'Terraform Installation done'
                }
            }
        }

        // Launch the EC2 instance with our target parameters
        stage('Create EC2 Instance') {
            steps {
                script {
                    masterInstance = dxTerraformCreateEc2Instance(masterInstance)
                }
            }
        }

        /*
         * After a successful creation of the EC2 instance, we install all required software on it and make sure that our settings
         * will be copied over to the target machine.
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
                            scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/dx-jmeter-distributed-set-up/install-prereqs.sh centos@${masterInstance.instance_private_ip}:/tmp
                            ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${masterInstance.instance_private_ip} 'chmod +x /tmp/install-prereqs.sh  && sh /tmp/install-prereqs.sh ${pipelineParameters.JMETER_BINARY_VERSION}'
                        """
                    }
                }
            }
        }

        stage('Create slaves and distributed setup') {
            steps {
                script {
                    try {
                        terraformVarsEC2.numberOfInstances = pipelineParameters.NUMBER_OF_JMETER_INSTANCES
                        terraformVarsEC2.tfvar_aws_ec2_instance_type = pipelineParameters.INSTANCE_TYPE
                        terraformVarsEC2.tfvar_instance_name = pipelineParameters.DX_INSTANCE_NAME
                        terraformVarsEC2.tfvar_instance_DX_SQUAD = pipelineParameters.DX_SQUAD
                        terraformVarsEC2.tfvar_dedicated_host_id = pipelineParameters.DEDICATED_HOST_ID
                        terraformVarsEC2.tfvar_instance_area = pipelineParameters.INSTANCE_AREA
                        terraformVarsEC2.tfvar_jmeterBinaryVersion = pipelineParameters.JMETER_BINARY_VERSION
                        terraformVarsEC2.tfvar_masterInstance = masterInstance
                        
                        jmeterInstanceDetails = dxJMeterDistributedSetup(terraformVarsEC2)
                        jmeterInstanceDetails.add(masterInstance)
                        echo "jmeterInstanceDetails: ${jmeterInstanceDetails}"
                    } catch (err) {
                        error('ERROR: Creating EC2 instance failed.'+ err)
                    }
                }
            }
        }
    }

    post {
        cleanup {
            script {
                    if (!jmeterInstanceDetails) {
                            error 'No instances found for cleanup.'
                    }
                    // loop through to destroy the jmeter slaves
                    jmeterInstanceDetails.each { instance ->
                            try {
                                    def buildParameters = [
                                        string(name: 'DX_INSTANCE_NAME', value: instance.instance_name),
                                        string(name: 'DX_INSTANCE_OWNER', value: instance.tags["Owner"]),
                                        string(name: 'DX_SQUAD', value: instance.tags["DX_SQUAD"]),
                                        string(name: 'INSTANCE_TYPE', value: "c5.2xlarge"),
                                        string(name: 'JMETER_BINARY_VERSION', value: "apache-jmeter-5.6.3"),
                                        string(name: 'DEDICATED_HOST_ID', value: "")
                                    ]

                                    build(
                                        job: "${pipelineParameters.EC2_DESTROY_JOB}",
                                        parameters: buildParameters,
                                        propagate: true,
                                        wait: true
                                    )
                                    println "Destroyed instance: ${instance.instance_name}"

                            } catch (err) {
                                error "Failed to destroy instance: ${instance.instance_name}. Error: ${err}"
                            }
                        }
                    dxWorkspaceDirectoriesCleanup()
            }
        }
    }
}
