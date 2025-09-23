# DB2 Server Instance

The pipelines in this area manage instances of DB2 servers using Terraform and provide the DB2 functionality via Docker containers. For more information on the DB2 Docker container see: https://git.cwp.pnp-hcl.com/Team-Q/Portal-Docker-Images/tree/develop/dx-db2
## Creation

The "deploy.gvy" pipeline creates a server in the eks_cluster_dx01 subnet and registers it in Route53 under team-q-dev.com. If instructed, it also allocates a public IP address (in which case that is what is registered in Route53). Currently, due to inter-subnet communication restrictions a public IP address MUST be selected, otherwise Jenkins will not have access to complete the setup.

### Parameters

|Parameter|Default|Description|
|--|--|--|
|INSTANCE_NAME|blank|Name of the instance to be created|
|USE_PUBLIC_IP|true|Should a public IP be allocated? Must be true for now|
## Deletion

The "destroy.gvy" pipeline deletes a server with the specified name and removes its entry in Route53.

### Parameters

|Parameter|Default|Description|
|--|--|--|
|INSTANCE_NAME|blank|Name of the instance to be removed|
