# dx-mavenized-components-ctf Job

## Description

This pipeline is used to run CTF tests for the given DXCore component(mavenized)
This pipeline take the required jar/s from the Artifactory(quintana-maven) and use that to run the CTF tests by executing the runComponentTests.sh script which is executed within the EC2 instance.

## Components list
The file component-list.json will contain the base FE component details along with the CTF test, DXCore path to the component jars.

## AMI Configuration
This pipeline takes a configured 8.5/9.0 CF17 base AMI(cf17base) which is mentioned in aws_ami.tf and that needs to be changed manually for now. Therefore an EC2 instance gets created based on the CF17 base AMI.
EC2 instance will be removed post running the CTF tests for the given DXCore component as a cleanup activity.


