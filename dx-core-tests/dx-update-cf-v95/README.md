# dx-update-cf-v95 Job

## Description

This pipeline is very similar to the dx-update-cf pipeline.  It takes a configured 8.5/9.0 CF17 (CF17 is the first DX CF to have Practitioner Studio and Woodburn Studio) base AMI with v95-UI-features enabled and performs an update to the configured CF version.  Therefore an EC2 instance gets created based on the CF17 base AMI. This EC2 instance will be configured with a Route53 record.  The record can be used to easily access the built environment.  After creation of the EC2 Instance, the update to the CF will be performed. Upon success, a new base AMI with the new CF will be created. EC2 instance, AMI and Route53 entry remain persistent until housekeeping will remove them due to expiration.

Please see the [dx-update-cf](../dx-update-cf/README.md) README for full details.

