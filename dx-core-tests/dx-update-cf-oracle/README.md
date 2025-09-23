# dx-update-cf-oracle Job

## Description

This pipeline is the successor to PortalUpdate-CF17-Linux-Oracle-Jenkinsfile.groovy

This pipeline takes a configured 8.5/9.0 CF19 base AMI and performs an update to the configured CF version.
Therefore an EC2 instance gets created based on the CF19 base AMI. This EC2 instance will be configured with a Route53 record.
This record can be used to easily access the built environment.
After creation of the EC2 Instance, the update to the CF will be performed. Upon success, a new base AMI with the new CF will be created.
EC2 instance, AMI and Route53 entry remain persistent until housekeeping will remove them due to expiration.

The rest of the pipeline follows the same flow as the dx-update-cf job.  The readme for that job contains more info and it can be seen [here](../dx-update-cf/README.md).

