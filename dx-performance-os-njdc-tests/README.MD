# Automation of performance tests in NJDC-Openshift environment

## Introduction

This is a code base to run DX Performance tests in njdc openshift environment.

## Available parameters

| Environment variable | Description | Default | Remarks
| -- | -- | -- | -- |
| `DEPLOYMENT_LEVEL` | Deploying develop images | develop | 
| `KUBE_FLAVOUR` | kube flavour | openshiftnjdc |
| `DEPLOYMENT_METHOD` | Deployment method | helm | 
| `IMAGE_REPOSITORY` |  Artifactory image repository | openshiftnjdc | 
| `NAMESPACE` | NAMESPACE for openshift njdc kube instance  | os-njdc-perf | 
| `KUBE_UNDEPLOY_JOB` | Job which undeploys the image in openshift njdc  | kube/cloud-undeploy |
| `KUBE_DEPLOY_JOB` | Job which deploys the image in openshift njdc  | kube/cloud-deploy |
| `DOMAIN_SUFFIX` | Domain suffix | .hcl-dx-dev.net |
| `CONTEXT_ROOT_PATH` | Context root  | wps |
| `DX_CORE_HOME_PATH` | Home path | portal |
| `PERSONALIZED_DX_CORE_PATH` | Personalized path | myportal |

## Functions from DX-Jenkins-Shared-Libraries

| Function name| Description |
| -- | -- |
| `dxParametersLoadFromFile` | Function to load params from yaml file |
| `dxWorkspaceDirectoriesCleanup` | Function to clean-up workspace  |

#### Load modules and Configuration
To load the configuration values for deployment.

#### Prepare Settings
Set the env name as DX_Performance_test_ + timestamp
#### Undeploying the application in existing k8 environment 
To delete the existing k8 environment before doing fresh deployment
#### Deploying the application in k8 environment
To deploy fresh k8 environment application with provided configuration values.
#### Run JMeter performance tests for upload assets
Run the JMeter tests for uploading 15k assets, shell script copied to jmeter machine to capture the time for upload and results copied to log file, will be removed once all tests done
#### Run scripts to capture DAM operations time
Shell script copied to NJDC jump server instance, used kube-ctl commands to capture the time for DAM operations and results copied to log file.
#### Run JMeter performance tests for response time of fetching binaries
Run the JMeter tests for fetching binaries, shell script copied to jmeter(master)machine to capture the mean time for fetching binaries and results copied to log file, will be removed once all tests done. 
#### Run 1 hour JMeter performance tests for Friendly URL
Run 1 hour JMeter tests for Friendly URL, shell script copied to jmeter(master)machine to capture the mean time for get api by AssetID, AssetName and customURL, results copied to log in file, will be removed once all tests done.

Also in this step will combine all logs and convert to xml file. Remove all temporary log files once all steps completed.
#### Run JMeter performance tests for response time of fetching binaries from collection having anonymous access
Run the JMeter tests for fetching binaries from collection having anonymous access, shell script copied to jmeter(master)machine to capture the mean time for fetching binaries and results copied to log file, will be removed once all tests done. 
#### Run the tests and Generate Report
Execute scripts to generate the XML file with upload time, operation time and Binary fetch Time values, get api time with friendlyURL values, generate report and push the report to s3 bucket. 
Also added threshold values for upload time, operation time, Binary fetch time and get api time with friendlyURL and access jmeter dashboard report on the click of build for each jmx script(asset Upload, fetchBinary, Get api) .
#### Post actions
If all tests are successful, dxWorkspaceDirectoriesCleanup Function executed to clean-up workspace in post.
#### Run JMeter tests for DesignStudio Rendering
New groovy file(openshift_njdc_designstudio_performance_tests.gvy) created  for DesignStudio Rendering Test.