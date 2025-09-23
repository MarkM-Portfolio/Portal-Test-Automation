/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2023. All Rights Reserved.       *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

// Use our DX shared library
@Library("dx-shared-library") _

// Map for pipeline parameters
def pipelineParameters = [:]

// Map for creating the EC2 instance
def terraformVarsEC2 = [:]

// Map for creating a new Route53 entry
def terraformVarsRoute53 = [:]

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
                    dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/dx-aws-jmeter/parameters.yaml")
                    terraformVarsEC2.tfvar_instance_name = pipelineParameters.INSTANCE_NAME
                    terraformVarsEC2.tfvar_instance_owner = dxJenkinsGetJobOwner()
                    terraformVarsEC2.tfvar_instance_area = pipelineParameters.INSTANCE_AREA
                    terraformVarsEC2.tfvar_aws_ec2_instance_type = pipelineParameters.INSTANCE_TYPE
                    terraformVarsEC2.tfvar_dedicated_host_id = pipelineParameters.DEDICATED_INSTANCE_ID
                    terraformVarsEC2.tfvar_use_public_ip = pipelineParameters.USE_PUBLIC_IP
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

        // Destroy the EC2 instance with out target parameters
        stage('Destroy EC2 Instance') {
            steps {
                dxTerraformDestroyEc2Instance(terraformVarsEC2)
            }
        }

        // Create a route53 entry so we can use proper TLS access
        stage('Destroy Route53 entry') {
            steps {
                dxTerraformDestroyRoute53Entry(terraformVarsRoute53)
            }
        }
    }

    post {
        cleanup {
            dxWorkspaceDirectoriesCleanup()
        }
    }
}