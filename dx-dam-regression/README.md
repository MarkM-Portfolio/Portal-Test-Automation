## 1.DAM Regression Tests Pipeline
### Description

This pipeline is used to run the JMeter tests for DAM Regression tests in native kube environments and capture the response times for upload, operations time and content render time and push the results to dashboard.

[Link to pipeline](https://portal-jenkins-develop.cwp.pnp-hcl.com/job/kube/job/dam-regression-test/)

### Preparation

To run the tests, several parameters are needed as shown below:

1. `DEPLOYMENT_LEVEL` - To Deploy latest images for DAM regression tests. This is required for kube deploy.
    ```
    development
    ```

2. `NAMESPACE` - Instance name for native kube
    ```
    dam_regression_test
    ```

3. `KUBE_FLAVOUR` - To provide which kube env need to deploy . This is required.

    ```
    native
    ```

4. `KUBE_UNDEPLOY_JOB` - Job which undeploys the image in google gke.

    ```
    CI/kube-deploy/native-kube-remove
    ```

5. `KUBE_DEPLOY_JOB` - Job which deploys the image in google gke.

    ```
    CI/kube-deploy/native-kube-next-deploy
    ```

6. `DEPLOYMENT_METHOD` - To choose the deployment method
    ```
    dxctl
    helm
    ```

7. `CLUSTER_NAME` - Cluster name where the deployment should be deployed to
    ```
    ''
    ```

8. `CLUSTER_REGION` - Region of the cluster
    ```
    ''
    ```

9. `CONTEXT_ROOT_PATH` - Context root
    ```
    wps
    ```

10. `DX_CORE_HOME_PATH` - Home path
    ```
    portal
    ```

11. `PERSONALIZED_DX_CORE_PATH` - Personalized path
    ```
    myportal
    ```

12. `DOMAIN_SUFFIX` - To set the domain suffix
    ```
    .hcl-dx-dev.net
    ```

13. `SSL_ENABLED` - Required for testing environments with https/self-signed certificates like native.kube.
    ```
    true
    ```

15. `TARGET_BRANCH` - To set the target branch to pick up the scripts from Portal-Performance-Tests repo
    ```
    develop
    feature/<your-branchname>
    ```

14. `TARGET_JMX_FILE` - To run the specific JMeter scripts for DAM regression tests from Portal-Performance-Tests repo
    ```
    DAM_Regression_Test.jmx
    ```

16. `TARGET_JMX_FILE_FOR_FETCH_BINARY` - To run the specific JMeter scripts for DAM binary tests from Portal-Performance-Tests repo
    ```
    DAM_Regression_Fetch_Binary_tests.jmx
    ```

17. `TARGET_JMX_FILE_FOR_FRIENDLY_URL` - To run the specific JMeter scripts for DAM get api with friendly url from Portal-Performance-Tests repo
    ```
    DAM_API_Performance_Tests_for_getapi_friendly_urls.jmx
  ```

18. `PERFORMANCE_RUN_FLAG`- To set PERFORMANCE_RUN param to true only in dam regression daily builds to stub Google vision
    ```
    true
    ```

### Workflow of the Pipeline
Runs Daily

#### Load modules and Configuration
To load the configuration values for deployment.

#### Prepare Settings
Set the env name as DX_Performance_test_ + timestamp 

#### Create EC2 Instance 
Creates the EC2 instance to be used for the test.

### Prepare EC2 Instance
Installs the prerequisites for the tests in the EC2 instance. This installs OpenJDK.

#### Undeploying the application in existing k8 environment 
To delete the existing k8 environment before doing fresh deployment

#### Deploying the application in k8 environment
To deploy fresh k8 environment application with provided configuration values.

#### Pull tests
Pulls the Performance tests from selected repo for testing. Saved in the workspace and then transferred to the EC2 instance.

#### Run JMeter performance tests for upload assets
Run the JMeter tests from performance repo for uploading assets, shell script copied to `performance-test-jmeter-master.team-q-dev.com` to capture the time for upload and results copied to log in reports folder, will be removed once all tests done

#### Run scripts to capture DAM operations time
Shell script copied to native-kube instance use kube-ctl commands to capture the time for DAM operations will sleep of 2minutes for 15 retries.

#### Run JMeter performance tests for response time of fetching binaries and copy the performance results to xml
Run the JMeter tests from performance repo for uploading assets and capture render time, shell script copied to `performance-test-jmeter-master.team-q-dev.com` to capture the time for upload and results copied to log in reports folder, will be removed once all tests done. 
Also in this step will combine all logs and convert to xml file. Remove all temporary log files once all steps completed.

#### Run JMeter performance tests to get response time of get api for friendlyURL and copy the performance results to xml
Run the JMeter tests from performance repo for dam get api for friendlyURL, shell script copied to `performance-test-jmeter-master.team-q-dev.com` to capture the mean time for get api by AssetID, AssetName and customURL, results copied to log in reports folder, will be removed once all tests done. 

#### Run the tests and Generate Report
After pulling the tests from server will connect to `performance-test-jmeter-master.team-q-dev.com` server to run the JMeter script with the appropriate parameters for the target environment and execute scripts to generate the XML file with upload time, operation time and Binary fetch Time values, get api time with friendlyURL values, generate report and push the report to s3 bucket. 
Also added threshold values for upload time, operation time, Binary fetch time and get api time with friendlyURL and access jmeter dashboard report on the click of build for each jmx script(asset Upload, fetchBinary, Get api) .

Each test has a 90 minute timeout setting for the stage. If a test times out, it will be reported as failure of the stage and set the build as unstable.

#### Post actions
If all tests are successful, failure or unstable the EC2 instance is deleted in post.

#### Parameters
`ENV_HOSTNAME` - Name of the instance to be deleted. Sample: `dx_dam_regression_tests_20210715-1038`

