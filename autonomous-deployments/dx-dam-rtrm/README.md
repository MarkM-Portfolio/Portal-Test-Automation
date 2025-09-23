# DX DAM RTRM Tests Pipeline

Performance Automation testing for DAM Database schema migration. As part of this pipeline we can perform the n-1, n-0 update deployment for RTRM testing with provided build configuration values.

This pipeline configured to trigger n-1 to develop deployments weekly once with 100k assets and this helps to capture the time taken for schema migration in logs.

Persistence storage capacity increased to 20Gi for DAM RTRM performance deployments. This tests are performed only in GKE.

[Link to pipeline](https://portal-jenkins-develop.cwp.pnp-hcl.com/job/kube/job/DAM-RTRM-Deployments/job/dam-rtrm-test-automation/)

### Preparation

To run the tests, several parameters are needed:

1. `DEPLOYMENT_LEVEL` - To Deploy latest master images for RTRM. This is required for kube deploy.
    ```
    master
    release
    ```

2. `UPDATE_DEPLOYMENT_LEVEL` - To Update latest develop images for RTRM. This is required for kube update deploy.

    ```
    develop
    release
    ```

3. `KUBE_FLAVOUR` - To provide which kube env need to deploy . This is required.

    ```
    google
    ```

4. `KUBE_UNDEPLOY_JOB` - Job which undeploys the image in google gke.

    ```
    kube/cloud-undeploy
    ```

5. `KUBE_DEPLOY_JOB` - Job which deploys the image in google gke.

    ```
    kube/cloud-deploy
    ```

6. `KUBE_UPDATE_JOB` - Job which updates the image on google gke.

    ```
    kube/cloud-update-deploy
    ```

7. `UPDATE_RELEASE_LEVEL` - Update from n to n+UPDATE_RELEASE_LEVEL
    ```
    1
    2
    ```
8. `DEPLOYMENT_METHOD` - To choose the deployment method
    ```
    dxctl
    helm
    ```
9. `DAM_RTRM_PERFORMANCE`- To enable the DAM RTRM performance
    ```
    true
    ```

### Workflow of the Pipeline
Runs weekly once
#### Load modules and Configuration
To load the configuration values for deployment.

#### Undeploying the application in existing k8 environment 
To delete the existing k8 environment before doing fresh deployment

#### Deploying the application in k8 environment
To deploy fresh k8 environment application with provided configuration values. After build got success will trigger [JMeter scripts for Uploading assets](https://portal-jenkins-develop.cwp.pnp-hcl.com/job/CI/job/DAM_RTRM_Automation_Tests/job/DAM_JMeter_Tests), [Pre Migration Acceptance Tests](https://portal-jenkins-develop.cwp.pnp-hcl.com/job/CI/job/DAM_RTRM_Automation_Tests/job/DAM_Migration_Tests/) jobs.

#### Updating the application in k8 environment
To update the existing k8 environment application with provided configuration values which enables the migration. After update deployment got success will trigger [JMeter scripts for Validating assets](https://portal-jenkins-develop.cwp.pnp-hcl.com/job/CI/job/DAM_RTRM_Automation_Tests/job/DAM_JMeter_Tests), [Post Migration Acceptance Tests](https://portal-jenkins-develop.cwp.pnp-hcl.com/job/CI/job/DAM_RTRM_Automation_Tests/job/DAM_Migration_Tests/) jobs to validate the migration process.

#### Post actions
If all tests are successful, failure or unstable the EC2 instance is deleted in post.

[Link to pipeline](https://portal-jenkins-develop.cwp.pnp-hcl.com/job/kube/job/DAM-RTRM-Deployments/job/dam-rtrm-test-automation/)

## 1. JMeter Tests Pipeline

### Description

This pipeline is used to run the JMeter tests (Uploading assets, Validating assets) for DAM RTRM in various environments provided through parameters.

[Link to pipeline](https://portal-jenkins-develop.cwp.pnp-hcl.com/job/CI/job/DAM_RTRM_Automation_Tests/job/DAM_JMeter_Tests)

### Preparation

To run the tests, several parameters are needed:

1. `TARGET_BRANCH` - To set the target branch to pick up the scripts from Portal-Performance-Tests repo
    ```
    develop
    feature/<your-branchname>
    ```

2. `SERVER_HOST` - host address of DX server. This is required to run the Performance tests.

    ```
    blr-latest.team-q-dev.com
    mnl-latest.team-q-dev.com
    ```

3. `SERVER_PORT` - port of DX server. This is required, if server has exposed the external port. For kube env not required

    ```
    10039
    ```

4. `SERVER_PROTOCOL` - protocol of the server. Please select any one of `http` and `https`.

5. `SERVER_RINGAPI_PORT` - port of RingAPI server. This is required, if RingAPI server has exposed the external port. For kube env not required.

    ```
    4000
    ```

6. `SERVER_DAMAPI_PORT` - port of DAM API server. This is required, if DAM API server has exposed the external port. For kube env not required.

    ```
    3000
    ```

7. `TARGET_JMX_FILE` - To run the specific JMeter scripts for DAM RTRM from Portal-Performance-Tests repo
    ```
  DAM_RTRM_Upload_Images_For_Performance.jmx
  DAM_RTRM_Upload_Assets_For_Performance.jmx
    ```
### Workflow of the Pipeline

#### Prepare Settings
Set the env name as DX_Performance_test_ + timestamp 

#### Create EC2 Instance 
Creates the EC2 instance to be used for the test.

#### Pull tests
Pulls the Performance tests from selected repo for testing. Saved in the workspace and then transferred to the EC2 instance.

#### Run the tests
After pulling the tests from server will connect to `performance-test-jmeter-master.team-q-dev.com` server to run the JMeter script with the appropriate parameters for the target environment. Each test has a 90 minute timeout setting for the stage. If a test times out, it will be reported as failure of the stage and set the build as unstable.

#### Post actions
If all tests are successful, the EC2 instance is deleted in post. If the overall status is failure or unstable, the EC2 instance is left up for investigation.


#### Cleanup pipeline
All EC2 instances will have 24 hours expiration. The EC2 instances that are left up for investigation and lived more than 24 hours will be deleted by an automated cleanup pipeline.

#### Parameters
`ENV_HOSTNAME` - Name of the instance to be deleted. Sample: `JMeter_Tests_Uploading_Assets_20210715-1038` or `JMeter_Tests_Validating_Assets__20210715-1038`


[Link to pipeline](https://portal-jenkins-develop.cwp.pnp-hcl.com/job/CI/job/DAM_RTRM_Automation_Tests/job/DAM_JMeter_Tests)