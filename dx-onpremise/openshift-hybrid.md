# Openshift Hybrid Deployment Pipelines

These pipelines create or destroy complete hybrid environments - i.e. both the on-premise and OpenShift components. They do this just by calling Jenkins jobs to do each part so are quite simple in and of themselves.

## openshift-hybrid-deploy

This pipeline performs the hybrid deployment and takes the following basic parameters plus a variable set of additional parameters as needed by the downstream pipelines.

|Parameter|Default|Description|
|--|--|--|
|ON_PREM_INSTANCE_NAME|blank|Name of the on-premise instance to be created|
|DOMAIN_SUFFIX|.apps.dx-cluster-dev.hcl-dx-dev.net|Suffix for FQDN of on-prem part|
|NAMESPACE|blank|Name of the Openshift namespace to create containing DAM, CC etc.|
|ON_PREM_JOB|hybrid-onpremise-deploy|The Jenkins job that will create the on-premise part|
|ON_PREM_PARAM_LIST|varies|Comma-separated list of parameters to copy from this job to the on-premise job (excluding (ON_PREM_)INSTANCE_NAME, DOMAIN_SUFFIX, CONFIGURE_HYBRID, USE_PUBLIC_IP, HYBRID_KUBE_HOST)|
|ON_PREM_HTTPS_PORT|443|Port on which the kube components should connect to the on-premise component|
|KUBE_JOB|hybrid-kube-deploy|The Jenkins job that will create the kube part|
|KUBE_PARAM_LIST|varies|Comma-separated list of parameters to copy from this job to the on-premise job (excluding HYBRID, HYBRID_HOST, HYBRID_PORT)|

## openshift-hybrid-destroy

This pipeline performs the hybrid removal and takes the following parameters.

|Parameter|Default|Description|
|--|--|--|
|ON_PREM_INSTANCE_NAME|blank|Name of the on-premise instance to be removed|
|CLUSTERED_ENV|false|Whether the on-premise environment to be removed is clustered|
|ON_PREM_JOB|hybrid-onpremise-destroy|The Jenkins job that will remove the on-premise part|
|NAMESPACE|blank|Name of the Openshift namespace to be removed|
|KUBE_FLAVOUR|openshift|Type of kube environment to be removed|
|KUBE_CLUSTER_NAME|blank|Kube cluster on which the environment is situated if not default|
|KUBE_CLUSTER_REGION|blank|AWS region on which the environment is situated if not default|
|NFS_SERVER|blank|NFS server on which the kube environment PVs are situated if not default|
|KUBE_JOB|hybrid-kube-deploy|The Jenkins job that will create the kube part|
