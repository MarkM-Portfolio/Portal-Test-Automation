/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2023, 2024 All Rights Reserved.  *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

import java.text.SimpleDateFormat

// Use our DX shared library
@Library("dx-shared-library") _

// Map for pipeline parameters
def pipelineParameters = [:]

// Map for creating the EC2 instance
def terraformVarsEC2 = [:]

// Map for creating a new Route53 entry in zone team-q-dev.com
def terraformVarsRoute53 = [:]

/*
 * Schedule a follow-on job to destroy instance after certain hours
 */
def scheduleNextJob(instance_name, destroy_job_name, destroy_job_delay_hours) {

        println("Instance to destroy: $instance_name; scheduleNextJob - $destroy_job_name - $destroy_job_delay_hours")

        // Calculate the time in seconds until the next job should be triggered
        int nextJobQuietPeriod = (destroy_job_delay_hours.toInteger()) * 3600
        if(nextJobQuietPeriod > 0 ){
            // Parse the provided parameters
            def buildParameters = []
                buildParameters.add(string(name: "INSTANCE_NAME", value: instance_name))
            // Trigger the target job build and populate the parameters
            println "Trigger ${destroy_job_name}\nParameters: ${buildParameters}"
            build(
                job: destroy_job_name,
                propagate: false,
                wait: false,
                parameters: buildParameters,
                quietPeriod: nextJobQuietPeriod
            )
        }
    }

pipeline {
    // Runs in build_infra, since we are creating infrastructure
    agent {
        label 'build_infra'
    }

    /* Our actual code execution in the pipeline */
    stages {
        // Load and configure parameters
        stage("Load parameters") {
            steps {
                script {
                    dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/db2-refactored/parameters.yaml")
                    terraformVarsEC2.tfvar_instance_name = pipelineParameters.INSTANCE_NAME
                    terraformVarsEC2.tfvar_instance_owner = dxJenkinsGetJobOwner()
                    terraformVarsEC2.tfvar_instance_area = pipelineParameters.INSTANCE_AREA
                    terraformVarsEC2.tfvar_aws_ec2_instance_type = pipelineParameters.INSTANCE_TYPE
                    terraformVarsEC2.tfvar_dedicated_host_id = pipelineParameters.DEDICATED_HOST_ID
                    terraformVarsEC2.tfvar_vpc_security_groups = pipelineParameters.VPC_SECURITY_GROUPS
                    terraformVarsEC2.tfvar_use_public_ip = pipelineParameters.USE_PUBLIC_IP
                    terraformVarsEC2.tfvar_aws_subnet = pipelineParameters.AWS_SUBNET
                    terraformVarsEC2.tfvar_instance_popo_schedule = pipelineParameters.POPO_SCHEDULE

                    terraformVarsRoute53.tfvar_record_name = "${pipelineParameters.INSTANCE_NAME}${pipelineParameters.DOMAIN_SUFFIX}"
                    terraformVarsRoute53.tfvar_record_type = "A"
                    terraformVarsRoute53.tfvar_hosted_zone = pipelineParameters.HOSTED_ZONE
                    // Transform VPC Security groups
                    if (pipelineParameters.VPC_SECURITY_GROUPS.indexOf('vpc_security_groups=') != -1) {
                        println "VPC security groups includes prefix, removing."
                        pipelineParameters.VPC_SECURITY_GROUPS = pipelineParameters.VPC_SECURITY_GROUPS.replace('vpc_security_groups=', '')
                    }
                    println "Security groups: ${pipelineParameters.VPC_SECURITY_GROUPS}"
                }
            }
        }

        // Install terraform in our workspace
        stage("Install Terraform") {
            steps {
                dxTerraformInstall()
            }
        }

        // Launch the EC2 instance with out target parameters
        stage('Create EC2 Instance') {
            steps {
                script {
                    terraformVarsEC2 = dxTerraformCreateEc2Instance(terraformVarsEC2)
                }
            }
        }

        // Create a route53 entry so we can use proper TLS access
        stage('Create Route53 entry') {
            steps {
                script {
                    if (pipelineParameters.USE_PUBLIC_IP) {
                        terraformVarsRoute53.tfvar_ip_address = terraformVarsEC2.instance_public_ip
                        pipelineParameters.INSTANCE_IP = terraformVarsEC2.instance_public_ip
                    } else {
                        terraformVarsRoute53.tfvar_ip_address = terraformVarsEC2.instance_private_ip
                        pipelineParameters.INSTANCE_IP = terraformVarsEC2.instance_private_ip
                    }
                    dxTerraformCreateRoute53Entry(terraformVarsRoute53)
                }
            }
        }

        // Schedule next job to destroy DB2
        stage('Schedule destroy job') {
            steps {
                script {
                    scheduleNextJob(terraformVarsEC2.instance_name, pipelineParameters.DB2_DESTROY_JOB, pipelineParameters.NEXT_JOB_DELAY_HOURS)
                }
            }
        }

        // Download the container image to push it to the remove instance
        stage('Download image') {
            steps {
                sh """
                    docker pull ${pipelineParameters.IMAGE_REPOSITORY}/${pipelineParameters.DOCKER_IMAGE_NAME}:${pipelineParameters.DB2_TAG}
                    docker save -o db2-image.docker ${pipelineParameters.IMAGE_REPOSITORY}/${pipelineParameters.DOCKER_IMAGE_NAME}:${pipelineParameters.DB2_TAG}
                """
            }
        }

        // Setup all configuration on the remote instance
        stage('Perform setup') {
            steps {
                configFileProvider([
                    configFile(
                        fileId: 'test-automation-deployments',
                        variable: 'DEPLOY_KEY'
                    )
                ]) {
                    sh """
                        sed -i "
                            s|DB2_PASSWORD_PLACEHOLDER|${pipelineParameters.DB2_PASSWORD}|g
                            s|IMAGE_NAME_PLACEHOLDER|${pipelineParameters.IMAGE_REPOSITORY}/${pipelineParameters.DOCKER_IMAGE_NAME}:${pipelineParameters.DB2_TAG}|g
                        " ${env.WORKSPACE}/db2/scripts/02-launch-db2-container.sh
                        chmod 600 ${DEPLOY_KEY}
                        scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r ${env.WORKSPACE}/db2/scripts centos@${pipelineParameters.INSTANCE_IP}:/home/centos/scripts
                        scp -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} -r ${env.WORKSPACE}/db2-image.docker centos@${pipelineParameters.INSTANCE_IP}:/tmp/db2-image.docker
                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${pipelineParameters.INSTANCE_IP} 'sudo sh ~/scripts/01-install-docker.sh'
                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${pipelineParameters.INSTANCE_IP} 'sudo sh ~/scripts/02-launch-db2-container.sh ${pipelineParameters.INSTANCE_NAME}${pipelineParameters.DOMAIN_SUFFIX} ${pipelineParameters.INSTANCE_IP}'
                    """
                }
            }
        }
    }

    post {
        cleanup {
            dxWorkspaceDirectoriesCleanup()
        }
    }
}