# dx-install-remotesearch Job

## Description

This pipeline takes a base AMI that contains WAS 8.5.5 and RemoteSearch 8.5 and then installs the latest Remote Search CF level on top of it.
Therefore an EC2 instance gets created based on the base AMI. This EC2 instance will be configured with a Route53 record.
This record can be used to easily access the built environment.
After creation of the EC2 Instance, the update to the CF will be performed. Upon success, a new base AMI with the new CF will be created.
EC2 instance, AMI and Route53 entry remain persistent until housekeeping will remove them due to expiration.

The corresponding Jenkins job can be found here:  [https://portal-jenkins-test.cwp.pnp-hcl.com/job/CI/job/dx-core-tests/job/dx-remotesearch-ami/](https://portal-jenkins-test.cwp.pnp-hcl.com/job/CI/job/dx-core-tests/job/dx-remotesearch-ami/)

