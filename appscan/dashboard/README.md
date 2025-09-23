# APPSCAN Dashboard
This provides 2 pipelines

- [**Publish APPSCAN result on dashboard**](#pipeline-to-publish-appscan-result-on-dashboard)
- [**Release check for automatic trigger new releasae scan**](#release-check-pipeline)

## Pipeline to publish APPSCAN result on dashboard
This pipeline provides a process to publish APPSCAN results to a dashboard.

## Usage
The pipeline provides two build parameters to control the build. Both are choice parameters to provide only valid values to the pipeline. The possible choices are maintained in the seed job's [job.yaml](https://git.cwp.pnp-hcl.com/Team-Q/dx-jenkins-jobs/blob/develop/jobs/appscan-dx/jobs.yaml) via the parameters section.

|Parameter|Setting|Description|
|--|--|--|
|APPSCAN_REPORT|string: Artifactory appscan report folder|A folder name from the Artifactory repo https://artifactory.cwp.pnp-hcl.com/ui/repos/tree/General/quintana-generic.|
|appscanProjectFolder|string: values set by seedjob|Project folder on S3 bucket for dashboard results.|

## Process description
This chapter gives a rough overview on the pipeline flow. 

#### Parameters
As already mentioned above this pipeline has 1 mandatory build parameter which provides the Artifactory folder which has the scan results to publish. Additional optional parameters may be configured. See section [parameters.yaml](#parametersyaml)

#### Stages

|Stage|Description|
|--|--|
|Load parameters|This stage is used to load and set all parameters relevant to the pipeline.|
|Download scan result from Artifactory|This stage downloads the scan results from the given Artifactory folder.|
|Download baseline from Artifactory|This stage downloads the baseline to compare with from Artifactory if publishing a full product scan. Otherwise this stage is skipped.|
|Create new baseline from Artifactory scan result|This stage creates a new baseline from the downloaded scan result.|
|Analyze and create reports|This stage analyzes the scan results and creates the necessary reports to publish.|
|Create new findings reports|This stage creates a detailed findings report to publish.|
|Upload to dashboard|This stage finally uploads the reports to the dashboard.|
|Upload baseline to artifactory and dashboard|This stage only runs for creating a new baseline and uploads it to the dashboard and Artifactory.|

#### Post
This section provides the final cleanup at the end of the pipeline. Here we're using the shared library **dxWorkspaceDirectoriesCleanup()** which does this job for you.

## parameters.yaml
Besides the manadatory build parameter mentioned above which provides the Artifactory results folder, the pipeline loads additional settings into the **pipelineParameters** list from the parameters.yaml file. This file provides parameters affecting the pipeline but don't change very often and need control about if changed.
Parameters defined in here may be configured as optional parameters in the job pipeline. Be careful if job is created via seed job. In such case changes to the configuration will be overwritten with the next seed job cycle.

|Parameter|Setting|Description|
|--|--|--|
|**Used in both pipelines**|--|--|
|APPSCAN_REPORT|string:<br>set by seedjob|Artifactory folder to create new dashboard entry or baseline from or Git release to check for.|
|appscanProjectFolder|string:<br>set by seedjob|Project folder for dashboard.|
|**Used in Jenkinsfile only**|--|--|
|NEW_BASELINE|boolean:<br>true or false|Set if the job should create a new baseline from the given scan result and upload to dashboard and Artifactory.|
|appscanS3Root|string:<br>S3 bucket URL|Root URL on S3 bucket where dashboard is published.|
|appscanBaselineFolder|string:<br>baseline|Dashboard folder where baseline is published.|
|appscanBaselineReport|string:<br>appscanBaseline|Dashboard baseline report name.|
|appscanResultMainFolder|string:<br>scan-results|Dashboard folder where scan results are published.|
|appscanXml|string:<br>appscanResult|Main part of resulting XML files.|
|jqueryScript|string:<br>jquery-3.6.4.min.js|jquery javascript version used on dashboard.|
|**Used in ReleaseCheck only**|--|--|
|APPSCAN_SCAN_JOB|string:<br>Jenkins jobname|Set reports folder within the root folder for uploads. Default is *reports*.<br>Must not be empty if configured in pipeline.|
|PROJECT_CHECK|string:<br>git-org/git-repo.git|Set organisation and repo to check in Git. Default is *websphere-portal/base.git*.<br>Must not be empty if configured in pipeline.|
|RELEASE_TOKEN|string:<br>/release/identifier|Set release token to check if scan is set to release. Default is */release/95_*.<br>Must not be empty if configured in pipeline.|
|APPSCAN_SEED_JOB|string:<br>Jenkins jobname|Seedjob creating pipeline.|
|appscanSeedJobBranch|string:<br>Git branch name|Git branch used together with APPSCAN_SEED_JOB. Change only via Jenkins configuration parameter when testing together with new seedjob.|

## Release check pipeline
This pipeline provides a process to check if new release branches are spawned in Git. If found new release branches automatically triggers the necessary APPSCAN pipeline. The pipeline script is **ReleaseCheck.groovy**.

## Usage
The pipeline has 4 mandatory build parameter to control the build. These parameters provide needed values to the pipeline. The default values are maintained in the seed job's [job.yaml](https://git.cwp.pnp-hcl.com/Team-Q/dx-jenkins-jobs/blob/develop/jobs/appscan-dx/jobs.yaml) via the parameters section.

|Parameter|Setting|Description|
|--|--|--|
|APPSCAN_REPORT|choice: values set by seedjob|Select which branch to check for. Choosing **_nextrelease** will check for new available release branches.|
|PROJECT_CHECK|string: value set by seedjob|Set main Git repo to start checking.|
|APPSCAN_SEED_JOB|string: value set by seedjob|Seed job directory in Git which created this Jenkins pipeline. Used to read SCAN_REPO list.|
|appscanProjectFolder|string: values set by seedjob|Project folder on S3 bucket for dashboard results.|

## Process description
This chapter gives a rough overview on the pipeline flow. 

#### Parameters
As already mentioned above this pipeline has 4 build parameters. These are automatically reinitialized each time it's [seed job](https://portal-jenkins-staging.cwp.pnp-hcl.com/job/Seed-Jobs-Rooted/job/appscan-dx/) is is running. In the seed job's [job.yaml](https://git.cwp.pnp-hcl.com/Team-Q/dx-jenkins-jobs/blob/develop/jobs/appscan-dx/jobs.yaml) you'll find the definitions for **APPSCAN_REPORT**.
Additional optional parameters may be configured. See section [parameters.yaml](#parametersyaml). This file is shared for both pipelines.

#### Stages

|Stage|Description|
|--|--|
|Load parameters|This stage is used to load and set all parameters relevant to the pipeline.|
|Check for new release|This stage gets the latest available release in Git and compares this with the latest scanned release found in Artifactory.|
|Run appscan release scan|This stage triggers a new scan if the previous step found a new branched release.|

#### Post
This section provides the final cleanup at the end of the pipeline. Here we're using the shared library **dxWorkspaceDirectoriesCleanup()** which does this job for you.