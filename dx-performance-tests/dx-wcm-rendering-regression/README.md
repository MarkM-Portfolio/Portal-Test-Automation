## WCM Rendering JMeter Regression Tests Pipeline
## Description

This pipeline is used to run the JMeter tests for WCM Rendering Regression tests in On Prem environments.

## Available parameters

| Environment variable | Description | Default | Remarks
| -- | -- | -- | -- |
| `SERVER_PROTOCOL` | Portal server potocol | http | 
| `TARGET_JMX_FILE` | Please provide the valid jmx file | Portal_WCM_Performance_Test_Scenario.jmx |
| `TARGET_BRANCH` | To set the target branch to pick up the scripts | develop | 
| `CF_VERSION` |  CF Version which need to be deployed | CF205 | 
| `INSTANCE_IP` | IP of the portal server  | 10.190.75.36 | 
| `JMETER_INSTANCE_IP` | IP of Jmeter instance | 10.190.75.28 |
| `JMETER_AGENT1` | IP of Jmeter agent1 | 10.190.75.29 |
| `JMETER_AGENT2` | IP of Jmeter agent2 | 10.190.75.31 |
| `GVUSERS_PER_TYPE` | Virtual Users  | 10 |
| `GTEST_DURATION` | Duration of Test | 3600 |
| `DXBuildNumber_NAME` | BuildName | DX_Core_20220927-190130_rivendell_release_95_CF207 |

## Functions from DX-Jenkins-Shared-Libraries

| Function name| Description |
| -- | -- |
| `dxParametersLoadFromFile` | Function to load params from yaml file |
| `dxWorkspaceDirectoriesCleanup` | Function to clean-up workspace  |

#### Load modules and Configuration
To load the configuration values for deployment.

#### Prepare Settings
Set the env name as DX_Performance_test_ + timestamp
#### Perform the steps for  Release update on the NJDC machine 
Run Release update to a given DXBuildNumber
#### Run the JMeter scripts
Run JMeter performance tests

