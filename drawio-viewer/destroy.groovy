/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2022. All Rights Reserved.       *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

// Use our DX shared library
@Library("dx-shared-library") _

// Map for creating the EC2 instance
def terraformVarsEC2 = [:]
terraformVarsEC2.tfvar_instance_name = "dx-drawio-viewer"
terraformVarsEC2.tfvar_instance_owner = "philipp.milich@hcl.com"
// Since we are only starting a small webapp, a 2core 1GB memory instance is sufficient
terraformVarsEC2.tfvar_aws_ec2_instance_type = "t3a.micro"

// test map for creating a new Route53 entry in zone team-q-dev.com
def terraformVarsRoute53 = [:]
terraformVarsRoute53.tfvar_record_name = "dx-drawio-viewer.team-q-dev.com"
terraformVarsRoute53.tfvar_record_type = "A"
terraformVarsRoute53.tfvar_hosted_zone = "Z3OEC7SLEHQ2P3"

pipeline {
    // Runs in build_infra, since we are creating infrastructure
    agent {
        label 'build_infra'
    }

    /* Our actual code execution in the pipeline */
    stages {
        // Install terraform in our workspace
        stage("Install Terraform") {
            steps {
                dxTerraformInstall()
            }
        }

        // Remove the route53 DNS entry from AWS
        stage('Destroy Route53 entry') {
            steps {
                dxTerraformDestroyRoute53Entry(terraformVarsRoute53)
            }
        }

        // Destroy the EC2 instance incl. storage
        stage('Destroy EC2 instance') {
            steps {
                dxTerraformDestroyEc2Instance(terraformVarsEC2)
            }
        }
    }

    post {
        cleanup {
            dxWorkspaceDirectoriesCleanup()
        }
    }
}