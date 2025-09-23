# dx-update-cf-ora21c Job

## Description

This pipeline takes a configured 8.5/9.0 CF211 base AMI and performs an update to the configured CF version.  
The base AMI is configured to have it's DB hosted in AWS RDS.
Therefore an EC2 instance gets created based on the CF211 base AMI. This EC2 instance will be configured with a Route53 record.
This record can be used to easily access the built environment.
After creation of the EC2 Instance, a new RDS Oracle 21c instance will be created from an existing snapshot and then hook Portal up to the new DB.  Next, the update to the CF will be performed. Upon success, a new base AMI with the new CF will be created.
EC2 instance, AMI and Route53 entry remain persistent until housekeeping will remove them due to expiration.
Downstream CTF test jobs will then be triggered.

The rest of the pipeline follows the same flow as the dx-update-cf job.  The readme for that job contains more info and it can be seen [here](../dx-update-cf/README.md).

