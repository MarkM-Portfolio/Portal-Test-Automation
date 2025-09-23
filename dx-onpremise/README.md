# Portal-Test-Automation: dx-onpremise

## Configuration and running on-premise automation in Jenkins

### Prerequisites

* Configured FTP credentials: ftp_credentials
* Configured AWS credentials: aws_credentials
* Configured Artifactory credentials: artifactory
* Jenkins Agent labeled: `generic`

### Pipeline Setup

|Parameter|Setting|Description|
|--|--|--|
|This project is parameterized|true||

Parameters:

**Run Parameter**

|Attribute|Setting|Description|
|--|--|--|
|INSTANCE_NAME|dx-onpremise|Name of the instance to be created or removed|
|INSTANCE_TYPE|t2.large| Type of the instance to be created or removed|
|DX_CORE_BUILD_VERSION|DX_Core_20200415-072707_rohan_develop|DX Build version for which instance needs to be launched|
|DBTYPE|derby or db2| Type of the database|
|DB_HOST|ec2-34-235-147-224.compute-1.amazonaws.com| DB2 Host to be used if DBType selected is db2|
|ENABLE_LDAP_CONFIG|unchecked by default| Enable or Disable Federated Security(LDAP)|
|LDAP_CONFIG_HOST|10.134.210.87| LDAP Host IP|
|LDAP_CONFIG_PORT|389| LDAP Host Port|
|LDAP_CONFIG_BIND_DN|cn=root| LDAP BindDN|
|LDAP_CONFIG_BASE_ENTRY|DC=USERS| LDAP Base DN|
|LDAP_CONFIG_SERVER_TYPE|IDS| LDAP Server Type|
|LDAP_CONFIG_BIND_PASSWORD|{xor}L28tKz4zayo=| LDAP Bind Password|
|DX_USERNAME|wpsadmin| DX Username|
|DX_PASSWORD|wpsadmin| DX Password|
|CLUSTERED_ENV|unchecked by default| Enable or Disable DX Cluster Creation|
|DOMAIN_SUFFIX|'.apps.dx-cluster-dev.hcl-dx-dev.net' or '.team-q-dev.com'|Determines the full DNS name|
|USE_PUBLIC_IP|unchecked by default|Determines the subnet in which to create the environment - if the public one it also gets a public IP (required for hybrid)|
|CONFIGURE_HYBRID|unchecked by default|Configures DAM and CC pointing to the HYBRID_KUBE_HOST and sets the SSO domain based on the DOMAIN_SUFFIX|
|HYBRID_KUBE_HOST|blank|The FQDN of the dx-hybrid-service of the kube component|
|REMOTE_SEARCH_ENV|unchecked by default| Enable or Disable Installation and Configuration of remote search|

## Configuration and running automation on a local machine

### Prerequisites

* Access to PNP VPN
* Terraform installed on the machine
* test-automation-deployments.pem in `/dx-onpremise/terraform/ec2-dx-onpremise-launch`

### Prepare environment variables

Export the following variables with corresponding values.

```
export ARTIFACTORY_USER=horst.schlaemmer
export ARTIFACTORY_PASSWORD=verysecret
export AWS_SECRET_ACCESS_KEY=access_key_secret
export AWS_ACCESS_KEY_ID=access_key_id
```

### Create EC2 instance

```
cd /terraform/ec2-dx-onpremise-launch
terraform init
terraform apply
```

### Get IP adress of created instance
```
cd /terraform/ec2-dx-onpremise-launch
terraform show
```

### Execute scripting remotely

Replace `<private_ip>` with IP retrieved in previous step.

#### Scripts for installing DX on primary node
```
cd /scripts
ssh -i test-automation-deployments.pem centos@<private_ip> '(cd /tmp/dx-onpremise/scripts && sudo sh 01-setup-prereqs.sh)'
eval "ssh -i test-automation-deployments.pem centos@<private_ip> \
  '(cd /tmp/dx-onpremise/scripts && ARTIFACTORY_HOST=$ARTIFACTORY_HOST \
  sh 02-prepare-dx-setup.sh)'"
ssh -i test-automation-deployments.pem centos@<private_ip> '(cd /tmp/dx-onpremise/scripts && sh 03-install-dx-portal85base.sh)'
ssh -i test-automation-deployments.pem centos@<private_ip> '(cd /tmp/dx-onpremise/scripts && sh 04-install-dx-applycf.sh)'
ssh -i test-automation-deployments.pem centos@<private_ip> '(cd /tmp/dx-onpremise/scripts && sh 05-install-dx-configengine.sh)'
ssh -i test-automation-deployments.pem centos@<private_ip> '(cd /tmp/dx-onpremise/scripts && sh 06-start-server.sh)'
```

#### Scripts for DB Transfer and Configuring LDAP with DX Standalone
```
ssh -i test-automation-deployments.pem centos@<private_ip> '(cd /tmp/dx-onpremise/configureDb2/ && sh transferDB.sh)'
ssh -i test-automation-deployments.pem centos@<private_ip> '(cd /tmp/dx-onpremise/configureLDAP && sh configLDAP.sh)'
```

#### Scripts for cluster creation with primary node
```
ssh -i test-automation-deployments.pem centos@<private_ip> '(cd /tmp/dx-onpremise/configureDb2/ && sh transferDB.sh)'
```
* Create DMGR profile
```
ssh -i test-automation-deployments.pem centos@<private_ip> '(cd /tmp/dx-onpremise/scripts/cluster && sh dmgr-profile.sh)'
```
* Federating primary node
```
ssh -i test-automation-deployments.pem centos@<private_ip> '(cd /tmp/dx-onpremise/scripts/cluster && sh cluster-primary-node.sh)'
```
* LDAP configuration for cluster
```
ssh -i test-automation-deployments.pem centos@<private_ip> '(cd /tmp/dx-onpremise/configureLDAP && sh configLDAPForCluster.sh)'

```
* Restarting the DMGR to reflect the changes
```
ssh -i test-automation-deployments.pem centos@<private_ip> '(cd /tmp/dx-onpremise/scripts/cluster && sh restart-dmgr.sh)'
```

#### Scripts for DX binary installing on secondary node
```
cd /scripts
ssh -i test-automation-deployments.pem centos@<private_ip> '(cd /tmp/dx-onpremise/scripts && sudo sh 01-setup-prereqs.sh)'
eval "ssh -i test-automation-deployments.pem centos@<private_ip> \
  '(cd /tmp/dx-onpremise/scripts && ARTIFACTORY_HOST=$ARTIFACTORY_HOST \
  sh 02-prepare-dx-setup.sh)'"
ssh -i test-automation-deployments.pem centos@<private_ip> '(cd /tmp/dx-onpremise/scripts && sh 03-install-dx-portal85base-binary.sh)'
ssh -i test-automation-deployments.pem centos@<private_ip> '(cd /tmp/dx-onpremise/scripts && sh 04-install-dx-tool-imcl.sh)'
```
* Copying profiletemplates.zip from primary to secondary and installing profile templates in PortalServer
```
ssh -i test-automation-deployments.pem centos@<private_ip> '(cd /tmp/dx-onpremise/scripts/cluster && sh install-portal-templates.sh)'
```
* Adding additional node to the cluster and federating
```
ssh -i test-automation-deployments.pem centos@<private_ip> '(cd /tmp/dx-onpremise/scripts/cluster && sh add-additional-node.sh)'
```


### Remove EC2 instance

```
cd /terraform/ec2-dx-onpremise-remove
terraform init
terraform destroy
```

## Configuration and running build on a local machine

**Please note: This does only apply to CentOS or MacOS.**

### Prerequisites

* Access to PNP VPN
