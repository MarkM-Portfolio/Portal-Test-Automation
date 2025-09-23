# Job TTL Cleanup

This Pipeline can be used to remove all EC2 instances having TTL tags in their AWS configuration.

This will supersede the method to create an undeploy job on each EC2 creation.

## Pipeline configuration

Create a declarative pipeline Job in jenkins, targeting the Jenkinsfile in this directory.
