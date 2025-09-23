# DX Acceptance Tests Pipeline

## Description

This pipeline is used to run the acceptance tests for Core, Ring, DAM, and CC in various environments provided through parameters.

[Link to pipeline](https://portal-jenkins-staging.cwp.pnp-hcl.com/job/CI/job/Automated_Tests/job/Acceptance_Tests/)

## Preparation

To run the tests, several parameters are needed:

1. Which application to test (Core, Ring, DAM, DAM Server, CC, Dxclient or all)
2. `PORTAL_HOST` - url of the dx instance to be tested
```
http://mnl-latest.team-q-dev.com:10039/wps/portal
https://native-kube-latest-release.team-q-dev.com/wps/portal
```

3. `EXP_API` - url of Experience API. This is required for CC and DAM acceptance tests.

```
http://mnl-latest.team-q-dev.com:4000/dx/api/core/v1
https://native-kube-latest-release.team-q-dev.com/dx/api/core/v1
```

4. `APP_ENDPOINT` - required for DAM acceptance tests

```
http://mnl-latest.team-q-dev.com:3000/dx/ui/dam
https://native-kube-latest-release.team-q-dev.com/dx/ui/dam
```

5. `SSL_ENABLED` - check this box if testing an environment with https protocol or environments with self-signed certificates. This bypasses the warning in the browser for the acceptance tests to continue.

6. `TARGET_BRANCH` - set name of the branch to pull from the apps. Used to check if we want to test develop or release branches. Branch that is set here will be set for all tests. (see also [Notes on using TARGET_BRANCH to pass as map for debugging](#markdown-header-notes-on-using-target_branch-to-pass-as-map-for-debugging))

7. `USERNAME` - To be used in the tests.

8. `PASSWORD` - To be used in the tests.

9. `DXCONNECT_HOST` - Dxconnect URL, required for dxclient acceptance test. Port number can be 10202 for standalone and 443 for Kube.

```
https://mnl-latest.team-q-dev.com:10202
https://native-kube-latest-release.team-q-dev.com:443
https://native-kube-latest-release.team-q-dev.com
```

10. `DXCONNECT_USERNAME` - Dxconnect username to be used in dxclient acceptance tests.

11. `DXCONNECT_PASSWORD` - Dxconnect password To be used in the dxclient acceptance tests.

12. `HOST_IP_ADDRESS` - IP address to add in the host file. Required for testing environments with private DNS.

13. `HOSTNAME` - Hostname to add in hosts file. Required for testing environments with private DNS.

14. `ARTIFACTORY_HOST` - Hostname of artifactory. Required for testing DAM server.

15. `ARTIFACTORY_IMAGE_BASE_URL` - URL of artifactory. Required for testing DAM server.

16. `MEDIA_LIBRARY_PERSISTENCE_IMAGE_FILTER` - Image filter to be used. This is optional but used in testing DAM server.

17. `WCMREST` - WCMREST v2 URL, required for site manager acceptance test.
```
http://mnl-latest.team-q-dev.com:10039/wps
https://native-kube-latest-release.team-q-dev.com/wps
```
18. `IMAGE_PROCESSOR_API` - URL of image processor api. Required for testing DAM Server.
```
http://mnl-latest.team-q-dev.com:8080/dx/api/image-processor/v1
https://native-kube-latest-release.team-q-dev.com/dx/api/image-processor/v1
```
19. `DAM_API` - URL of dam api. Required for testing DAM Server.
```
http://mnl-latest.team-q-dev.com:3000/dx/api/dam/v1
https://native-kube-latest-release.team-q-dev.com/dx/api/dam/v1
```

#### Notes on using TARGET_BRANCH to pass as map for debugging
Usually the TARGET_BRANCH parameter is a single branch name used to download all testsuites. But sometimes it is necessary for debugging to get one or more testsuites from different branches. For this special purpose this field can also take a map declaration as input.
```
[key1: "value1", key2: "value2", ...]
```
A ***key*** can be any of the selectable test names defined in the [Jenkins pipeline](https://portal-jenkins-staging.cwp.pnp-hcl.com/job/CI/job/Automated_Tests/job/Acceptance_Tests/build?delay=0sec).
(e.g. TEST_DX_CORE, TEST_CC, ...)
A special key definition is used for a default value.
```
[default: "defaultvalue", ...]
```
This can be used to provid a default value to be returned if the requested key isn't found in the map otherwise the return value is empty. This comes in handy if you just have a few test branches and the rest should use the same branch.
```
[default: "defaultvalue", key1: "value1", key2: "value2"]
```
**NOTE: This map usage is only recommanded for debugging purpose!!**

## Workflow of the Pipeline

### Prepare Settings
Set the env name as DX_Acceptance_tests + timestamp

### Create EC2 Instance
Creates the EC2 instance to be used for the test.

### Prepare EC2 Instance
Installs the prerequisites for the tests in the EC2 instance. This installs chromedriver, chrome browser, and OpenJDK. This also registers the Enchanted repositories in npm. These settings can be seen in `install-prereqs.sh`

### Pull tests
Pulls the repositories of the apps selected for testing. Saved in the workspace and then transferred to the EC2 instance.

### Run the tests
Each application selected will install necessary dependencies through `make install` and then run the acceptance tests through `make test-acceptance-endpoint` with the appropriate parameters for the target environment. Each test has a 30 minute timeout setting for the stage. If a test times out, it will be reported as failure of the stage and set the build as unstable.

### Post actions
If all tests are successful, the EC2 instance is deleted in post. If the overall status is failure or unstable, the EC2 instance is left up for investigation.


### Cleanup pipeline
All EC2 instances will have 24 hours expiration. The EC2 instances that are left up for investigation and lived more than 24 hours will be deleted by an automated cleanup pipeline.

### Parameters
`ENV_HOSTNAME` - Name of the instance to be deleted. Sample: `DX_Acceptance_Tests_​20210130-1804`

## Scheduled Tests
A pipeline that runs the acceptance tests pipeline on different environments on a fixed schedule. Tests are scheduled to run once a day at the schedule below to avoid conflicting with regularly scheduled builds of the tested machines.

Germany | India | Philippines | US (EST)
-- | -- | -- | --
02:00 | 06:30 | 09:00 | 20:00


### Environments tested
- us-latest.team-q-dev.com
- native-kube-latest.team-q-dev.com

[Link to pipeline](https://portal-jenkins-test.cwp.pnp-hcl.com/job/CI/job/Automated_Tests/job/Scheduled_Acceptance_Tests/)