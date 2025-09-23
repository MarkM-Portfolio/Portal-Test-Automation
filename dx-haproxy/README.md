## HAProxy Tests Pipeline
### Description
This pipeline is used to run the HAProxy testcases.

### Workflow of the Pipeline

#### Prepare Environment
Used to load parameters from parameters.yaml

#### Deploying the application
Used to deploy the application via native-kube-next-deploy Job

#### Configure remote kubectl
Used for configuring kubectl remotely

### Run scripts to upgrade the service port
Used for upgrading the helm values to update the service port to 454

### Verify the dx deployment is working with service port
The verification step to check services are accessible from the service port

### Run scripts to upgrade the SSL offloading is disabled
Used for upgrading the helm values to update the SSL offloading is disabled

### Verify the dx deployment is working with SSL offloading is disabled
The verification step to check working with SSL offloading is disabled

### Run scripts to upgrade the SSL offloading is enabled
Used for upgrading the helm values to update the SSL offloading is enabled
### Verify the dx deployment is working with SSL offloading is enabled
The verification step to check working with SSL offloading is enabled

### Run scripts to upgrade the service type to ClusterIP
Used for upgrading the helm values to update the service type to ClusterIP
### Verify the dx deployment is not reachable to external user
The verification step to check services not reachable to external use

### Run scripts to upgrade the service type to set LoadBalancer
Used for upgrading the helm values to update the service type to set LoadBalancer
### Verify the dx deployment reachable to external user
The verification step to check deployment reachable to external user

### Run scripts to upgrade the HTTP Strict Transport Security is disabled
Used for upgrading the helm values to update the HTTP Strict Transport Security is disabled
### Verify the dx deployment has no automatic redirect to https
The verification step to check no automatic redirect to https

### Run scripts to upgrade the HTTP Strict Transport Security is enabled
Used for upgrading the helm values to update the HTTP Strict Transport Security is disabled
### Verify the dx deployment automatic redirect to https
The verification step to check automatic redirect to https in case of http

#### Cleanup pipeline
Used to delete the instance via native-kube-remove Job. The instance will be removed by this cleanup pipeline.
