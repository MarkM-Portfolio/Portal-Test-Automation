## Automation - SoFy solution deployment on GKE

### Goal
This document outlines an automation of sofy solution download and deploy the helm charts on GKE environment then run the acceptance tests against it.

### Pre-requisites

* Configured FTP credentials: `ftp_credentials`
* Configured GKE credentials: `gke_credentials`
* Configured Artifactory credentials: `artifactory`
* Jenkins Agent labeled: `build_infra`

### Parameters

|Attribute|Default Value|Description|
|--|--|--|
|SOLUTION_NAME| |Name of the sofy solution to be created.|
|NAMESPACE| |Project/Namespace for the deployment|
|SOLUTION_DESCRIPTION| |Description for the sofy solution|
|SERVICE_NAME|dx|Name of the DX service|
|SKIP_ACCEPTANCE_TESTS|checked|Execute acceptance tests for the deployment|
|AUTO_DELETE|false|Delete the solution automatically if it checked|
|DX_VERSION|0.1.65|version of the DX to be deployed|
|CONTEXT_ROOT_PATH|wps|Context root path|
|DX_CORE_HOME_PATH|portal|DX core home path|
|IMAGE_REPO|gcr|Images registry|
|RELEASE_TYPE|ga|Helm charts release type|

### Pipeline

[https://portal-jenkins-test.cwp.pnp-hcl.com/job/kube/job/sobud-gke-deploy/](https://portal-jenkins-test.cwp.pnp-hcl.com/job/kube/job/sobud-gke-deploy/)