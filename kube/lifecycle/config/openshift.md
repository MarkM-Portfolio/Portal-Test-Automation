# Openshift configuration parameters

**openshiftConfig :
    List of environment variables Specific to openshift environment.**

| Parameter | Description | Default |
|--|--|--|
|NAMESPACE|project name to be created openshift|
|openshift_credentials|cloud credential with openshift credentials if job is triggered openshift|cloudCredentials specific openshiftCredentials|
|OC_CLIENT_URL|openshift client version, specific version url can be found here : https://github.com/openshift/origin/releases|https://github.com/openshift/origin/releases/download/v3.11.0/openshift-origin-client-tools-v3.11.0-0cbc58b-linux-64bit.tar.gz|
|CORE_PV_NAME| DX core Persistence volume |if no PV specified jenkins create a PV with NAMESPACE-core-pv|
|CORE_STORAGECLASS| DX core storage class |dx-deploy-stg|
|DAM_PV_NAME|DX core storage class|if no PV specified jenkins creates a PV with  NAMESPACE-dam-pv|
|DAM_STORAGECLASS|DAM storage class|dx-deploy-stg|
|RS_STORAGECLASS|remote search storage class|gp2|
|DX_CORE_REPLICAS|No of replicas for DX CORE |1|
|NFS_SERVER| NFS Server |centos@ec2-18-222-163-104.us-east-2.compute.amazonaws.com|

    List of environment variables Specific to NJDC OpenShift environment.**

|Parameter | Value |
|--|--|
|DB_HOST | 10.190.75.30 |
|DB_TYPE | db2 |
|DEFAULT_STORAGECLASS | thin |
|DOMAIN | apps.quocp.nonprod.hclpnp.com |
|IMAGE_REPOSITORY | openshiftnjdc or openshiftnjdc-prod |
|NFS_CREDENTIALS_ID | mary.dooley/***** (njdc_nfs_credentials (currently using mary.dooley)) |
|KUBE_FLAVOR | openshiftnjdc |
|NFS_HOST | mary.dooley@10.190.75.13 |
|NFS_PATH | /ocpqu |
|NFS_SERVER | mary.dooley@10.190.75.13 |
|OC_SERVER_URL | https://api.quocp.nonprod.hclpnp.com:6443 |
|OPENSHIFT_CREDENTIALS_ID | mary.dooley/****** (Credentias for the NJDC QUOCP environment.  Current using mary.dooley) |
|RS_STORAGECLASS | thin |
| WCM_PERFORMANCE | true to deploy to WCM Performance Test environment |

    List of environment variables Specific to ROSA OpenShift environment.**

|Parameter | Value |
|--|--|
|DB_HOST | '' |
|DB_TYPE | db2 |
|DEFAULT_STORAGECLASS | gp3 |
|DOMAIN | apps.dx-cluster-dev.hcl-dx-dev.net |
|IMAGE_REPOSITORY | 657641368736.dkr.ecr.us-east-2.amazonaws.com |
|NFS_CREDENTIALS_ID | ubuntu/***** (openshift_rosa_nfs) |
|KUBE_FLAVOR | openshift |
|NFS_HOST | ubuntu@18.218.47.60 |
|NFS_PATH | /nfs/jenkinsnfs |
|NFS_SERVER | ubuntu@18.218.47.60 |
|OC_SERVER_URL | https://api.dx-cluster-dev.hcl-dx-dev.net:6443 |
|OPENSHIFT_CREDENTIALS_ID | cluster-admin/****** (Credentials for the ROSA cluster, cred openshift_rosa_credentials) |
|RS_STORAGECLASS | gp3 |
|WCM_PERFORMANCE | false |


Deployment URL (develop): https://dx-deployment-passthrough-os-dev-fresh.apps.dx-cluster-dev.hcl-dx-dev.net/wps/portal

Deployment URL (release): https://dx-deployment-passthrough-os-rel-fresh.apps.dx-cluster-dev.hcl-dx-dev.net/wps/portal
