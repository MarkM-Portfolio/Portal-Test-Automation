# DX Core Test JAAS pipeline

This pipeline executes test.jaas unit tests.

# Steps

1. Creates an EC2 instance
2. Pull Portal-Developer-tools, so that we can use wsbld to run test
3. Download Docker, IBM Java, and Unzip
4. Pull the latest FEs
5. Build and run test.jaas unit test

# Jenkins Pipeline

https://portal-jenkins-develop.cwp.pnp-hcl.com/job/Gaia/job/JAAS_Unit_Test_Run/