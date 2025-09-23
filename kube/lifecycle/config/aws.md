# AWS configuration parameters

**AWS Config :
    List of environment variables Specific to AWS environment.**

| Parameter | Description | Default |
|--|--|--|
|NAMESPACE|project name to be created AWS|
|aws_credentials|cloud credential with AWS credentials if job is triggered AWS|cloudCredentials specific to AWS credentials|
|CORE_PV_NAME| DX core persistence volume |if no PV specified Jenkins create a PV with NAMESPACE-core-pv|
|DAM_PV_NAME|DAM persistence volume|if no PV specified Jenkins creates a PV with  NAMESPACE-dam-pv|
|DAM_STORAGECLASS|DAM storage class|dx-deploy-stg|
|RS_PV_NAME|Remote Search persistence volume |if no PV specified uses default storage from AWS environment|
|RS_STORAGECLASS|Remote search storage class|gp2|
|DX_CORE_REPLICAS|No of replicas for DX CORE |1|
|NFS_SERVER| NFS Server |ec2-user@34.237.145.57|
|CLUSTER_NAME|AWS Cluster|eks_cluster_dx01|
