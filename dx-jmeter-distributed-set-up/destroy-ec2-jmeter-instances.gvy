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

// This pipeline is to destroy EC2 instance with optional params DX_SQUAD using shared library and test.

// Import the shared library with the name configured in Jenkins
@Library('dx-shared-library') _

// test map for creating a new EC2 instance
def terraformVarsEC2 = [:]

// Create object to store parameters with values
def pipelineParameters = [:]

pipeline {

    agent {
        label 'build_infra'
    }
   
    stages {
        stage('Install Terraform') {
            steps {
                script {
                    dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/dx-jmeter-distributed-set-up/dxTerraformDestroyEc2Instance.yaml")
                    dxTerraformInstall()
                }
            }
        }
        stage('Destroy EC2 Instance') {
            steps {
                script {
                    terraformVarsEC2.tfvar_instance_name = pipelineParameters.DX_INSTANCE_NAME
                    terraformVarsEC2.tfvar_instance_owner = pipelineParameters.DX_INSTANCE_OWNER
                    terraformVarsEC2.tfvar_instance_DX_SQUAD = pipelineParameters.DX_SQUAD
                    terraformVarsEC2.tfvar_aws_ec2_instance_type = pipelineParameters.INSTANCE_TYPE
                    terraformVarsEC2.tfvar_dedicated_host_id = pipelineParameters.DEDICATED_HOST_ID
                    try {
                        terraformVarsEC2 = dxTerraformDestroyEc2Instance(terraformVarsEC2)
                    } catch (err) {
                        error("ERROR: Destroying EC2 instance failed.")
                    }
                    println "terraformVarsEC2 = " + terraformVarsEC2
                    println "Test OK"
                }
            }
        }
    }

    post {
        cleanup {
            //Cleanup workspace
            dxWorkspaceDirectoriesCleanup()
        }
    }
}
