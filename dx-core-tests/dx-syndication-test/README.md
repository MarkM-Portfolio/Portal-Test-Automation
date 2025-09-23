# DX Syndication test pipeline

This pipeline executes DX Syndication tests.

# Steps

1. Creates an EC2 instance
2. Pull the latest docker image and enable Site Manager
3. Run Syndication tests by creating new library and populating with content and new JCR types and verify that the new library is successfully syndicated

# Jenkins Pipeline

https://portal-jenkins-test.cwp.pnp-hcl.com/job/DXCore_Tests/job/dx-syndication/