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
    DAM_RTRM_Upload_Assets.jmx
    DAM_RTRM_Validation_Assets.jmx
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

## 2. Migration Tests Pipeline

### Description

This pipeline is used to run the Pre and Post migration acceptance tests for DAM RTRM in various environments provided through parameters.

[Link to pipeline](https://portal-jenkins-develop.cwp.pnp-hcl.com/job/CI/job/DAM_RTRM_Automation_Tests/job/DAM_Migration_Tests/)

### Preparation

To run the tests, several parameters are needed:

1. `TARGET_BRANCH` - To set the target branch to pick up the scripts from media-library repo
    ```
    develop
    feature/<your-branchname>
    ```

2. `DAM_API` - URL of DAM server. This is required to run the Pre migration acceptance tests.

    ```
    http://blr-latest.team-q-dev.com:3000/dx/api/dam/v1
    http://mnl-latest.team-q-dev.com:3000/dx/api/dam/v1
    ```

3. `EXP_API` - URL of RingAPI server. This is required to run the Pre migration acceptance tests.

    ```
    http://blr-latest.team-q-dev.com:4000/dx/api/core/v1
    http://mnl-latest.team-q-dev.com:4000/dx/api/core/v1
    ```

4. `SSL_ENABLED` - Required for testing environments with HTTPS/self-signed certificates like a native. Kube.

5. `HOST_IP_ADDRESS` - IP address to add in hosts file note: Required for testing environments with private DNS.

    ```
    10.134.208.19
    ```

6. `TEST_COMMAND` - To run the acceptance tests in target branch and to pick up the scripts from media-library repo.

    ```
    pre-migration-test-acceptance-endpoint
    post-migration-test-acceptance-endpoint
    ```

7. `ARTIFACTORY_HOST` - Hostname of artifactory. Required for testing DAM server.

8. `ARTIFACTORY_IMAGE_BASE_URL` - URL of artifactory. Required for testing DAM server.

9. `MEDIA_LIBRARY_PERSISTENCE_IMAGE_FILTER` - Image filter to be used. This is optional but used in testing DAM server.

### Workflow of the Pipeline

#### Prepare Settings
Set the env name as DX_Pre_Migration_Tests_ + timestamp 

#### Create EC2 Instance 
Creates the EC2 instance to be used for the test.

### Prepare EC2 Instance
Installs the prerequisites for the tests in the EC2 instance. This installs NodeJs, NVM, and Docker CE. This also registers the Enchanted repositories in npm. These settings can be seen in `install-prereqs.sh`
#### Pull tests
Pulls the Pre migration tests from selected repo for testing. Saved in the workspace and then transferred to the EC2 instance.

#### Run the tests
Each application selected will install necessary dependencies through `make install` and then run the acceptance tests through `make pre-migration-test-acceptance-endpoint` or `make post-migration-test-acceptance-endpoint` with the appropriate parameters for the target environment. This `make post-migration-test-without-acceptance` command executes unit and integration tests for validating migrated schema and data changes. Each test has a 30 minute timeout setting for the stage. If a test times out, it will be reported as failure of the stage and set the build as unstable.

#### Post actions
If all tests are successful, the EC2 instance is deleted in post. If the overall status is failure or unstable, the EC2 instance is left up for investigation.


#### Cleanup pipeline
All EC2 instances will have 24 hours expiration. The EC2 instances that are left up for investigation and lived more than 24 hours will be deleted by an automated cleanup pipeline.

#### Parameters
`ENV_HOSTNAME` - Name of the instance to be deleted. Sample: `Pre_Migration_Tests_20210702-1009` or `Post_Migration_Tests_20210715-1141`


[Link to pipeline](https://portal-jenkins-develop.cwp.pnp-hcl.com/job/CI/job/DAM_RTRM_Automation_Tests/job/DAM_Migration_Tests/)