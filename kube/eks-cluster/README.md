# EKS Cluster on AWS
This pipeline creates an EKS cluster to be used by the EKS autonomous deployment pipelines. 
This project contains the necessary files for deploying an EKS cluster using CloudFormation and Groovy scripts.

## Prerequisites
- AWS CLI installed and configured
- AWS credentials with sufficient permissions
- Groovy installed
- Git installed

## Directory structure

```text
eks-cluster/
  deploy.gvy                        # Pipeline script for EKS cluster manager
  README.md                         # This document
  eks_cf_template.yml               # CloudFormation template
  parameters.yaml                   # Parameter list needed for EKS cluster manager
```

## Configuration of Jenkins Job

### Target Agent

Since this pipeline creates infrastructure, it will run on the `build_infra` agents.

### Parameters

| Parameter Name          | Description                                      | Default Value                       |
|-------------------------|--------------------------------------------------|-------------------------------------|
| CLUSTER_MANAGER_MODE    | Select the action to perform: Create, Destroy, or None on a given cluster as per your needs. | None                             |
| CLUSTER_NAME            | Name of the EKS cluster                          | eks-cluster-dx01                    |
| CLUSTER_REGION          | AWS region where the EKS cluster will be deployed | us-east-1                           |
| VPC_ID                  | ID of the VPC where the EKS cluster will be deployed | vpc-0a5b5af3ec875492d              |
| SUBNETS                 | Comma-separated list of subnet IDs for the EKS cluster | subnet-0a2fc24d57d19c31b, subnet-0f5af81d4a148c527 |
| NODE_GROUP_NAME         | Name of the node group                           | eks-nodes-dx01                      |
| NODE_INSTANCE_TYPE      | Instance type for the worker nodes               | t3.xlarge                           |
| DESIRED_WORKER_NODES    | Number of desired worker nodes                   | 1                                   |
| KUBERNETES_VERSION      | Kubernetes version number                        | 1.28                                |
| RESOURCE_TAG_TERM_FLAG | Tag term flag for the resources                  | N                                   |
| RESOURCE_TAG_AREA       | Tag area for the resources                       | INFRA                               |
| RESOURCE_TAG_OWNER      | Tag owner for the resources                      | nitin.jagjivan@hcl.com              |


### Pipeline definition configuration

Configure the Job to use a `Pipeline script from SCM` with the target SCM being `Git`. Use the repository URL `git@git.cwp.pnp-hcl.com:Team-Q/Portal-Test-Automation.git` and a fitting git credential, e.g. `git-ssh-access-key`.

As Branch, specify what you need, default would be `develop`.
