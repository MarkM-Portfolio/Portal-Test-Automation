# 1.EXIM--Automation--Tests

This pipeline will be  used to automate the DAM EXIM functionality by covering below stages-
1. Creating two environments, using a pipeline. Environments to be source and target.

2. Jmeter scripts are used to populate the assets in source environment.

3. Export assets from source environment to filesystem using dxclient.

4. Validate the exported assets using dxclient.

5. Import assets from filesystem to target environment using dxclient.

6. Jmeter scripts are used to validate the imported assets in target environment.

7. Remove environments after testing.

Currently the pipeline is  handling the `acceptance testing` as a prerequisite before proceeding on with the above mentioned stages and for this we are using the DAM staging acceptance test job only with the same command.


## Directory structure

    ```text
    this-pipeline/dx-dam-exim-tests
    docs/README.md                         # Contains all documentation related assets
    pipeline/dam-exim.gvy                  # Contains the Jenkinsfile of the declarative pipeline
    ```

## Pipeline output

This pipeline will automate the DAM Exim functionality as per the stages mentioned above and provide a `Success` message in the logs if the pipeline runs successfully otherwise it will generate error logs.

## Pipeline flow

The pipeline will contain stages to automate exim flow .The pipeline will have following stages -
1. Load modules and configuration
2. Prepare Settings
3. Prepare Terraform
4. Create EC2 instance
5. Prepare EC2 instance
6. Deploying the application in k8 source environment for exim test
7. Running dam-exim acceptance tests in k8 source environment
8. Deploying the application in k8 target environment for exim test
9.  exim Performace tests
10. Run JMeter performance tests for upload assets
11. Install dxclient and export assets from source to filesystem, validate exported assets,import assets to target k8
12. Run JMeter tests to verify assets in target environment
13. Undeploying the application in k8 source environment for exim test
14. Undeploying the application in k8 target environment for exim test

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
| String | NAMESPACE| `exim` | Yes | name space. |
| String | KUBE_FLAVOUR| `native` | Yes | Deploying a native kube environment. |
| String | KUBE_UNDEPLOY_JOB| `CI/kube-deploy/native-kube-remove` | Yes | Job which undeploys the environment. |
| String | KUBE_DEPLOY_JOB| `CI/kube-deploy/native-kube-next-deploy` | Yes | Job which deploys the environment. |
| String | DEPLOYMENT_METHOD| `helm` | Yes | Deployment method. |
| String | CONTEXT_ROOT_PATH| `wps` | Yes | context root. |
| String | DX_CORE_HOME_PATH| `portal` | Yes | Required for CC and DAM tests. |
| String | DOMAIN_SUFFIX| `.hcl-dx-dev.net` | Yes | Kube flavour domain suffix. |
| String | TOOL_PACKAGE_URL| `https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dxclient-new/hcl-dxclient-image-v95_CF200_20220211-2040_rohan_develop.zip` | Yes | URL from which to download dxclient zip. |
| String | STAGING_ACCEPTANCE_TEST_JOB| `CI/DAM-Staging/staging_acceptance_tests` | Yes | Job which runs acceptance tests for staging |
| String | DX_PROTOCOL| `https` | Yes | Protocol. |
| String | DX_PORT| 443 | Yes | DX port. |
| String | DAM_API_PORT| 443 | Yes | DAM port. |
| String | RING_API_PORT| 443 | Yes | RING_API port. |

#### Used credentials

Create a list of credentials that is used by this pipeline, also describe which stage of the pipeline uses it:

| Credential ID | Used ENV vars | Description | Affected Pipeline Stage |
| -- | -- | -- | -- |
| test_credentials | `TEST_SECRET_ACCESS_KEY`, `TEST_ACCESS_KEY_ID` | Used to access the test. | `Stage IV` |

#### Pipeline definition configuration

Configure the Job to use a `Pipeline script from SCM` with the target SCM being `Git`. Use the repository URL `git@git.cwp.pnp-hcl.com:Team-Q/Portal-Test-Automation.git` and a fitting git credential, e.g. `git-ssh-access-key`.

As Branch, specify what you need, default would be `develop`.

For the `Script Path` use `dx-dam-exim-tests/dam-exim.gvy`

## 2.Creating two environments, using a pipeline. Environments to be source and target.
### Description

This pipeline is used to Create two Environments in native kube.

## Directory structure

    ```text
    this-pipeline/dx-dam-exim-tests
    docs/README.md                         # Contains all documentation related assets
    pipeline/dam-exim-environments.gvy                  # Contains the Jenkinsfile of the declarative pipeline
    ```

## Pipeline output

This pipeline will create two environments (source and target) in native-kube to check the exim functionality by uploading assets into the source environment and then using DXclient to export the assets,import and validate them in target environment.

### Pipeline flow

This Pipeline Contains the following Stages

#### Load modules and Configuration
To load the configuration values for deployment.

#### Prepare Terraform
Terraform Scripts To Create the EC2 instance.

#### Create EC2 Instance 
Creates the EC2 instance to be used for the test.
#### Deploying the application in k8 source environment for exim test
Creates the source k8 environment.
#### Running dam exim acceptance tests in k8 source environment 
RUns the acceptance test scripts in the source environment.
#### Deploying the application in k8 target environment for exim test 
Creates the target k8 envionment.
#### Pull exim Performace tests 
Clone the Performance test repository and pull the tests.
#### Run JMeter performance tests for upload assets
This stage will upload assets in the source environment.
#### Install dxclient and export assets from source to filesystem, validate exported assets,import assets to target k8 
Here we will install dxclient from artifactory and will execute the DXClient commands to export the aasets,Import the assets in Target environment and validate them there in target k8 environment.
#### Run JMeter tests to verify assets in target environment 
We will verify the imported assets using jmeter scripts.

#### Destroying the dam source environment in k8
To delete the source environment in k8

#### Destroying the dam target environment in k8
To delete the target environment in k8

#### Post actions
If all tests are successful, failure or unstable the EC2 instance is deleted in post.