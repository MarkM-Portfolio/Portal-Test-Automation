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

// Import the shared library with the name configured in Jenkins
@Library("dx-shared-library") _

// test map for creating a new EC2 instance
def terraformVarsEC2 = [:]

// Create object to store parameters with values
def pipelineParameters = [:]

// Map for creating a new Route53 entry in zone team-q-dev.com
def terraformVarsRoute53 = [:]

pipeline {
  // Runs in build_infra, since we are creating infrastructure
   agent {
        label 'build_infra'
   }
   
   stages {

      // Load the pipeline parameters into object
        stage('Load parameters') {
            steps {
                dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/dx-aws-jmeter/parameters.yaml")
                script {
                    terraformVarsEC2.tfvar_instance_name = pipelineParameters.INSTANCE_NAME
                    terraformVarsEC2.tfvar_aws_ec2_instance_type = pipelineParameters.INSTANCE_TYPE
                    terraformVarsEC2.tfvar_dedicated_host_id = pipelineParameters.DEDICATED_HOST_ID
                    terraformVarsEC2.tfvar_instance_area = pipelineParameters.INSTANCE_AREA
                    terraformVarsEC2.tfvar_vpc_security_groups = pipelineParameters.VPC_SECURITY_GROUPS
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

        // Instance owner check
        stage('Prepare Settings') {
            steps {
                 script {
                         // Determine owner of EC2 instance
                        INSTANCE_OWNER = dxJenkinsGetJobOwner()
                        terraformVarsEC2.tfvar_instance_owner = "${INSTANCE_OWNER}"
                        println("Instance owner will be > ${INSTANCE_OWNER} <.")
                          // subnet id
                        terraformVarsEC2.tfvar_aws_subnet = pipelineParameters.AWS_SUBNET_ID
                        echo "AWS Subnet ID : ${terraformVarsEC2.tfvar_aws_subnet}"
                    }
                }
        }

        // Terraform install
        stage('Install Terraform') {
            steps {
                script {
                    dxTerraformInstall()
                    echo "Terraform Installation done"
                }
            }
        }

        // Launch the EC2 instance with our target parameters
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


      // We need to tweak some scripts, install java and JMeter
        stage('Setup environment') {
            steps {
                configFileProvider([
                    configFile(fileId: 'test-automation-deployments', variable: 'DEPLOY_KEY')
                ]) {
                    sh """
                        chmod 600 ${DEPLOY_KEY}
                        scp -r -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} ${workspace}/dx-aws-jmeter/scripts centos@${terraformVarsEC2.instance_private_ip}:/home/centos/
                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /home/centos/scripts && sh 01-setup-prereqs.sh'
                        ssh -o StrictHostKeyChecking=no -i ${DEPLOY_KEY} centos@${terraformVarsEC2.instance_private_ip} 'cd /home/centos/scripts && sh 02-install-jmeter.sh ${pipelineParameters.JMETER_BINARY_VERSION}'                  
                    """
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