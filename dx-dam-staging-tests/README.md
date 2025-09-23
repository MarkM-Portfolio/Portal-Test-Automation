# 1.DAM STAGING -- Automation--Tests

staging-long-run.gvy - This pipeline will be used to automate the DAM STAGING long run functionality by covering below stages-

1. Creating two environments, using a pipeline. Environments to be publisher and subscriber

2. Created EC2 instance and pull all Jmeter scripts required for DAM staging continous staging

3. Register subscriber in publisher environment using JMeter script by giving sync interval of 15 mins

4. Jmeter scripts are used to populate the assets in source environment for every 1 hour.

5. After 2 days check the operation in subscriber

6. Validate the sync status assets of subscriber

7. Jmeter scripts are used to verify the total collections and assets in subscriber environment

## Directory structure

    ```text
    this-pipeline/dx-dam-staging-tests
    /README.md                         # Contains all documentation related assets
    pipeline/staging-long-run.gvy           # Contains the Jenkinsfile of the declarative pipeline
    ```

## Pipeline output

This pipeline will automate the DAM Exim functionality as per the stages mentioned above and provide a `Success` message in the logs if the pipeline runs successfully otherwise it will generate error logs.

## Pipeline flow

The pipeline will contain stages to automate DAM staging long run flow .The pipeline will have following stages -
1. Load modules and configuration
2. Prepare Settings
3. Install Terraform            
4. Create EC2 instance
6. Deploying the staging application in k8 environment
7. Deploying the production application in k8 environment
8. Deploying the application in k8 target environment for exim test
9. Pull DAM staging jmeter tests
10. Run JMeter tests to register subscriber
11. Run JMeter tests to upload assets
12. Run JMeter tests to verify assets in target environment
13. Run scripts to capture operations time in subscriber environment
14. Run JMeter tests to find subscriber sync status
15. Run JMeter tests to validate assets in subscriber environment

### Configuration of Jenkins Job

#### Target Agent

Agent used is -'build_infra'

#### Parameters

| Type | Parameter name | Sample content | Required | Description |
| -- | -- | -- | -- | -- |
| String | STAGING_ACCEPTANCE_TEST_JOB| TARGET_BRANCH| `CI/DAM-Staging/staging_acceptance_tests` | Yes | Job which runs acceptance tests for staging tests. |
| String | TARGET_BRANCH| `develop` | Yes | Target branch. |
| Boolean | SSL_ENABLED| true | Yes | Required for testing environments with https/self-signed certificates like native.kube. |
| String | DEPLOYMENT_LEVEL| `develop` | Yes | Deploying latest images. |
| String | NAMESPACE| `staging-long-run` | Yes | name space. |
| String | KUBE_FLAVOUR| `native` | Yes | Deploying a native kube environment. |
| String | KUBE_UNDEPLOY_JOB| `CI/kube-deploy/native-kube-remove` | Yes | Job which undeploys the environment. |
| String | KUBE_DEPLOY_JOB| `CI/kube-deploy/native-kube-next-deploy-long-run` | Yes | Job which deploys the environment. |
| String | DEPLOYMENT_METHOD| `helm` | Yes | Deployment method. |
| String | CONTEXT_ROOT_PATH| `wps` | Yes | context root. |
| String | DX_CORE_HOME_PATH| `portal` | Yes | Required for CC and DAM tests. |
| String | DOMAIN_SUFFIX| `.team-q-dev.com` | Yes | Kube flavour domain suffix. |


#### Pipeline definition configuration

Configure the Job to use a `Pipeline script from SCM` with the target SCM being `Git`. Use the repository URL `git@git.cwp.pnp-hcl.com:Team-Q/Portal-Test-Automation.git` and a fitting git credential, e.g. `git-ssh-access-key`.

As Branch, specify what you need, default would be `develop`.

For the `Script Path` use `dx-dam-staging-tests/staging-long-run.gvy`

## Pipeline output

This pipeline will create two environments (publisher and subscriber) in native-kube to check the DAM Staging long run functionality by uploading assets into the source environment and check the assets validate them in subscriber environment.

#### Post actions
Remove EC2 instance and cleanup temporary directory


staging-delete-update-run.gvy - Jenkins job to do DAM staging short-run test for update and delete functionalities
## Pipeline flow

The pipeline will contain stages to automate DAM staging short run flow to verfify update and delete collections and assets .The pipeline will have following stages -

1. Load modules and configuration
2. Prepare Settings
3. Undeploying the publisher application in k8 environment
4. Undeploying the subscriber application in k8 environment
5. Install Terraform            
6. Create EC2 instance
6. Deploying the publisher application in k8 environment
7. Deploying the subscriber application in k8 environment
8. Deploying the application in k8 target environment for exim test
9. Pull DAM staging jmeter tests
10. Run JMeter tests to register subscriber
11. Run JMeter tests to upload assets for delete
12. Run scripts to capture operations time in publisher environment
13. Run scripts to capture operations time in subscriber environment
14. Run JMeter tests to delete collections
15. Run scripts to capture operations time in subscriber environment - for delete assets
16. Run JMeter tests to find subscriber sync status after deletion of collections
17. Run JMeter tests to upload assets for update
18. Run scripts to capture operations time in publisher environment - for update assets
19. Run JMeter tests to update collections name and assets title
20. Run scripts to capture operations time in subscriber environment - for update assets
21. Run JMeter tests to find subscriber sync status after updation of collections and asset names
22. Run JMeter tests to validate collections and assets in subscriber environment

PJD link - https://portal-jenkins-develop.cwp.pnp-hcl.com/job/CI/job/DAM-Staging_seeded/job/DAM_Staging_Delete_Update_Test/