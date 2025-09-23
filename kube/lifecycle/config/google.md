# gke configuration parameters

**GKEConfig :
    List of environment variables Specific to gke environment.**

| Parameter | Description | Default |
|--|--|--|
|NAMESPACE|project name to be created gke|
|gke_credentials|cloud credential with gke credentials if job is triggered gke|cloudCredentials specific gkeCredentials|
|CORE_PV_NAME| DX core Persistence volume |if no PV specified jenkins create a PV with NAMESPACE-core-pv|
|CORE_STORAGECLASS| DX core storage class |dx-deploy-stg|
|DAM_PV_NAME|DX core storage class|if no PV specified jenkins creates a PV with  NAMESPACE-dam-pv|
|DAM_STORAGECLASS|DAM storage class|dx-deploy-stg|
|RS_STORAGECLASS|remote search storage class|gp2|
|DX_CORE_REPLICAS|No of replicas for DX CORE |1|
|NFS_SERVER| NFS Server |centos@ec2-18-222-163-104.us-east-2.compute.amazonaws.com|
|CLUSTER_NAME|GKE Cluster|dx-jenkins-cluster|
