## 1. DAM API Performance Tests Pipeline
### Description

This pipeline is used to run the DAM API performance tests in native kube environments.

[Link to pipeline](https://portal-jenkins-develop.cwp.pnp-hcl.com/job/kube/job/dam-api-performance-tests/)

### Preparation

To run the tests, several parameters are needed:

1. `TARGET_BRANCH` - To set the target branch to pick up the scripts from Portal-Performance-Tests repo
    ```
    develop
    feature/<your-branchname>
    ```

2. `SERVER_HOST` - host address of DX server. This is required to run the Performance tests.

    ```
    halo-halo.latest.team-q-dev.com
    toblerone-latest.team-q-dev.com
    ```

3. `SERVER_PORT` - port of DX server. This is required, if server has exposed the external port. For kube env not required

    ```
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
7. `TARGET_JMX_FILE` - To run the specific JMeter scripts for DAM regression tests from Portal-Performance-Tests repo
    ```
    DAM_Regression_Test.jmx
    ```
8. `DEPLOYMENT_LEVEL` - To Deploy latest images for DAM regression tests. This is required for kube deploy.
    ```
    development
    ```
9. `KUBE_FLAVOUR` - To provide which kube env need to deploy . This is required.

    ```
    native
    ```

10. `KUBE_UNDEPLOY_JOB` - Job which undeploys the image in google gke.

    ```
    CI/kube-deploy/native-kube-remove
    ```

11. `KUBE_DEPLOY_JOB` - Job which deploys the image in google gke.

    ```
    CI/kube-deploy/native-kube-next-deploy
    ```

12. `DEPLOYMENT_METHOD` - To choose the deployment method
    ```
    dxctl
    helm
    ```

13. `TARGET_JMX_FILE` - To run the specific JMeter scripts for DAM RTRM from Portal-Performance-Tests repo
    ```
    DAM_Regression_Test.jmx
    ```


14. `NAMESPACE` - Instance name for native kube
    ```
    dam_regression_test
    ```


15. `CLUSTER_NAME` - Cluster name where the deployment should be deployed to
    ```
    ''
    ```

16. `CLUSTER_REGION` - Region of the cluster
    ```
    ''
    ```

17. `CONTEXT_ROOT_PATH` - Context root
    ```
    wps
    ```

18. `DX_CORE_HOME_PATH` - Home path
    ```
    portal
    ```

19. `PERSONALIZED_DX_CORE_PATH` - Personalized path
    ```
    myportal
    ```

20. `SSL_ENABLED` - Required for testing environments with https/self-signed certificates like native.kube.
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

#### Undeploying the application in existing k8 environment 
To delete the existing k8 environment before doing fresh deployment

#### Deploying the application in k8 environment
To deploy fresh k8 environment application with provided configuration values.

#### Pull tests
Pulls the Performance tests from selected repo for testing. Saved in the workspace and then transferred to the EC2 instance.

#### Run the tests
After pulling the tests from server will connect to `performance-test-jmeter-master.team-q-dev.com` server to run the JMeter script with the appropriate parameters for the target environment. Each test has a 90 minute timeout setting for the stage. If a test times out, it will be reported as failure of the stage and set the build as unstable.

#### Post actions
If all tests are successful, failure or unstable the EC2 instance is deleted in post.

#### Parameters
`ENV_HOSTNAME` - Name of the instance to be deleted. Sample: `dx_dam_api_performance_tests_20210715-1038`

