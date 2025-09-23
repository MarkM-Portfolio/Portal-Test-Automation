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

// Use our DX shared library
@Library("dx-shared-library") _

def formattedString
def pipelineParameters = [:]
def commonConfig
def kubeModule

/* Common paths - must be here always */
moduleDirectory = "./kube/lifecycle/modules"
scriptDirectory = "./kube/lifecycle/scripts"
configDirectory = "./kube/lifecycle/config"


pipeline {
    // Runs in build_infra, since we are creating infrastructure
    agent {
        label 'build_infra'
    }

    stages {
        stage("Load modules and configuration") {
            steps {
                dxKubectlWorkspaceInstall()
            }
        }

        stage('Load parameters') {
            steps {
                script {
                    dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/kube/eks-cluster/parameters.yaml")
                }
            }
        }

        stage('Create parameter overrides') {
            steps {
                script {
                    def inputParams = [
                        ClusterName: pipelineParameters.CLUSTER_NAME,
                        VpcId: pipelineParameters.VPC_ID,
                        SubnetIds: pipelineParameters.SUBNETS,
                        NodeGroupName: pipelineParameters.NODE_GROUP_NAME,
                        NodeInstanceType: pipelineParameters.NODE_INSTANCE_TYPE,
                        DesiredCapacity: pipelineParameters.DESIRED_WORKER_NODES.toInteger(),
                        KubernetesVersion: pipelineParameters.KUBERNETES_VERSION,
                        ResourceTagTermFlag: pipelineParameters.RESOURCE_TAG_TERM_FLAG,
                        ResourceTagArea: pipelineParameters.RESOURCE_TAG_AREA,
                        ResourceTagOwner: pipelineParameters.RESOURCE_TAG_OWNER,
                        ResourceTagSavings: pipelineParameters.RESOURCE_TAG_SAVINGS
                    ]

                    def keyValueStrings = inputParams.collect { key, value ->
                        "${key}=\"${value}\""
                    }

                    formattedString = keyValueStrings.join(" ")
                    CLUSTER_NAME = pipelineParameters.CLUSTER_NAME
                    CLUSTER_REGION = pipelineParameters.CLUSTER_REGION

                }
            }
        }
        stage('Configure AWS') {
            steps {
                // Using credentials binding to securely access AWS credentials
                withCredentials([usernamePassword(credentialsId: 'aws_credentials', usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                    // Install and configure AWS CLI
                    sh '''
                        aws configure set aws_access_key_id ${AWS_ACCESS_KEY_ID}
                        aws configure set aws_secret_access_key ${AWS_SECRET_ACCESS_KEY}
                        aws configure set default.region ${CLUSTER_REGION}
                    '''
                }
            }
        }

        stage('Deploy EKS Cluster') {
            when {
                expression { pipelineParameters.CLUSTER_MANAGER_MODE == "Create" }
            }
            steps {
                // Deploy the CloudFormation stack
                echo "${formattedString}"
                sh "aws cloudformation deploy --template-file kube/eks-cluster/eks_cf_template.yml --stack-name ${CLUSTER_NAME} --parameter-overrides ${formattedString} --capabilities CAPABILITY_NAMED_IAM"
            }
        }

        stage('Configure EKS Cluster') {
            when {
                expression { pipelineParameters.CLUSTER_MANAGER_MODE == "Create" }
            }
            steps {
                script {
                    dxEksctlWorkspaceInstall()
                }
                // Update kubeconfig for the EKS cluster
                dxKubectlAwsConfig(awsClusterName:pipelineParameters.CLUSTER_NAME)

                // Configure EKS Cluster
                sh '''
                    
                    # Associate IAM OIDC provider with the EKS cluster
                    eksctl utils associate-iam-oidc-provider --cluster ${CLUSTER_NAME} --approve
                    
                    # Get the ARN of the EKSClusterAutoscalerPolicy
                    POLICY_ARN=\$(aws iam list-policies --query 'Policies[*].[PolicyName, Arn]' --output text | grep EKSClusterAutoscalerPolicy | awk '{print $2}')
                    
                    # Create IAM service account for cluster autoscaler
                    eksctl create iamserviceaccount --cluster ${CLUSTER_NAME} --namespace kube-system --name cluster-autoscaler-eks  --attach-policy-arn \${POLICY_ARN} --override-existing-serviceaccounts --approve
                    curl -o kube/eks-cluster/cluster-autoscaler-autodiscover.yaml https://raw.githubusercontent.com/kubernetes/autoscaler/master/cluster-autoscaler/cloudprovider/aws/examples/cluster-autoscaler-autodiscover.yaml
                    sed -i "s|<YOUR CLUSTER NAME>|${CLUSTER_NAME}|g" kube/eks-cluster/cluster-autoscaler-autodiscover.yaml
  
                    # Apply cluster-autoscaler-autodiscover.yaml
                    kubectl apply -f kube/eks-cluster/cluster-autoscaler-autodiscover.yaml
                    
                    # Cleanup files
                    if [ -e "kube/eks-cluster/cluster-autoscaler-autodiscover.yaml" ]; then
                        rm -rf "kube/eks-cluster/cluster-autoscaler-autodiscover.yaml"
                    fi
                '''
            }
        }

        stage('Destroy EKS Cluster') {
            when {
                expression { params.CLUSTER_MANAGER_MODE == "Destroy" }
            }
            steps {
                script {
                    // Check if cluster exists
                    def clusterExists = sh(script: "aws eks describe-cluster --name ${CLUSTER_NAME}", returnStatus: true) == 0
                    if (clusterExists) {
                        // Install Helm in the current workspace and add it to the PATH variable
                        dxHelmWorkspaceInstall()

                        // Workaround for using shared library with the nested Groovy files
                        env.THIS_BUILD_OWNER = dxJenkinsGetJobOwner()
                        commonConfig = load "${configDirectory}/common.gvy"
                        kubeModule = load "${moduleDirectory}/aws.gvy"
                        kubeModule.destroyEnvironmentAllNamespaces()
                        
                    } else {
                        echo "EKS cluster '${CLUSTER_NAME}' does not exist."
                    }
                }
                // Delete the CloudFormation stack
                 sh 'aws cloudformation delete-stack --stack-name ${CLUSTER_NAME}'
                // Wait until the EKS cluster is deleted
                 sh 'aws cloudformation wait stack-delete-complete --stack-name ${CLUSTER_NAME}'
            }
        }
    }
    post {
        failure {
            script {
                // Check if cluster exists
                def clusterExists = sh(script: "aws eks describe-cluster --name ${CLUSTER_NAME}", returnStatus: true) == 0
                if (clusterExists) {
                    // Install Helm in the current workspace and add it to the PATH variable
                    dxHelmWorkspaceInstall()

                    // Workaround for using shared library with the nested Groovy files
                    env.THIS_BUILD_OWNER = dxJenkinsGetJobOwner()
                    commonConfig = load "${configDirectory}/common.gvy"
                    kubeModule = load "${moduleDirectory}/aws.gvy"
                    kubeModule.destroyEnvironmentAllNamespaces()

                }
                // Delete the CloudFormation stack
                sh 'aws cloudformation delete-stack --stack-name ${CLUSTER_NAME}'
                // Wait until the EKS cluster is deleted
                sh 'aws cloudformation wait stack-delete-complete --stack-name ${CLUSTER_NAME}'
            }
        }
        cleanup {
            dxWorkspaceDirectoriesCleanup()
        }
    }
}
