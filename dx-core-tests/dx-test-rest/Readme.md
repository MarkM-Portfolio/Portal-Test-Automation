# DX Core Test Rest pipeline

This pipeline executes test.rest unit tests and IUT wcmsuite and deploys report to s3 bucket.

# Steps

1. Creates an EC2 instance
2. Pull Portal-Developer-tools, so that we can use wsbld to run test
3. Download Docker, IBM Java, and Unzip
4. Pull the latest FEs
5. Build and run test.rest unit test
6. Build and run IUT tests
7. Convert xml result to html and copies to aws s3 bucket

# Jenkins Pipeline

https://portal-jenkins-test.cwp.pnp-hcl.com/job/DXCore_Tests/job/IUT-UNIT-TESTS/