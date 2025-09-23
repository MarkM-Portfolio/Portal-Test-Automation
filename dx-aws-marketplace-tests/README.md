## AWS Marketplace Tests Pipeline
### Description
These pipelines are used to test DX deployment from AWS Marketplace ECR to EKS and Native Kube flavors.

### Workflow of the Pipeline

#### Prepare Environment
Used to load parameters from parameters.yaml

#### Deploying the application to Native Kube from AWS Marketplace ECR
Used to deploy the application to a Native Kube instance via native-kube-next-deploy Job

#### Deploying the application to EKS from AWS Marketplace ECR
Used to deploy the application to a EKS Cluster via cloud-deploy Job

#### Run Acceptance Tests
Run to acceptance test against deployments