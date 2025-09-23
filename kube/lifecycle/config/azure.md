# AKS configuration parameters

**AKS Config :
    List of environment variables Specific to AKS environment.**

| Parameter | Description | Default |
|--|--|--|
|NAMESPACE|project name to be created AKS|
|aks_credentials|cloud credential with AKS credentials if job is triggered AKS|cloudCredentials specific to AKS credentials|
|CORE_PV_NAME| DX core persistence volume |if no PV specified Jenkins create a PV with NAMESPACE-core-pv|
|DAM_PV_NAME|DAM persistence volume|if no PV specified Jenkins creates a PV with  NAMESPACE-dam-pv|
|DAM_STORAGECLASS|DAM storage class|dx-deploy-stg|
|RS_PV_NAME|Remote Search persistence volume |if no PV specified uses default storage from AKS environment|
|RS_STORAGECLASS|Remote search storage class|managed-premium|
|DX_CORE_REPLICAS|No of replicas for DX CORE |1|
|NFS_SERVER| NFS Server |azureuser@20.55.82.13|
|CLUSTER_NAME|AKS Cluster|dx-dev-cluster|
|RESOURCE_GROP|aks resource-group |AZ-QUINTANA-RG-US-EAS|
|SUBSCRIPTION|set subscription for azure account | d6e737b5-1a6d-49fa-9921-b168e2f8ecd6|
|TENANT|Tenant ID (for service principal login) |dc207805-8a5c-4370-a00a-2573016a71a7
