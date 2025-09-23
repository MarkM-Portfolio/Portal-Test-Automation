# DX Syndication test pipeline

This pipeline executes DX Syndication tests.

# Steps

1. Creates two new EC2 instances of the latest on-prem CF build
2. Enable content sites on each instance
3. Run Syndication tests by creating new library and populating with content and new JCR types and verify that the new library is successfully syndicated

# Jenkins Pipeline

https://portal-jenkins-test.cwp.pnp-hcl.com/job/DXCore_Tests/job/dx-syndication-onpremise