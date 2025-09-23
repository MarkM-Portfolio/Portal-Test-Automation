# APPSCAN

## Pipeline to run Appscan against LEAP source code
This pipeline provides a process to scan source code from a single LEAP repo or the full product code. The list of all product repos is maintained with the seed job (see **Usage** section below).

## Usage
The pipeline provides two build parameters to control the build. Both are choice parameters to provide only valid values to the pipeline. The possible choices are maintained in the seed job's [job.yaml](https://git.cwp.pnp-hcl.com/Team-Q/dx-jenkins-jobs/blob/develop/jobs/appscan-leap/jobs.yaml) via the parameters section.

|Parameter|Setting|Description|
|--|--|--|
|BUILD_TYPE|choice: values set from array **buildtype[]**|Select which branch to use for code extract (develop or release). Choosing **release** will use the latest available release branch.|
|SCAN_REPO|choice: values set from array **scanlist[]**|Select a single repo to scan or **full** to scan the whole product.| 

## Process description
This chapter gives a rough overview on the pipeline flow. 

#### Agent
The build will run on any system labeled with **build_infra**. This will create an EC2 instance which then will run the scan. The instance will be destroyed at the end of the run. The name of this instance will be unique so multiple scans can run in parallel.

|Instance name|Description|
|--|--|
|_prefix_\__timestamp_\__suffix_|This is used as AWS instance name.<br>   _prefix_ is defined in parameters.yaml as INSTANCE_NAME.<br>   _timestamp_ is calculated on pipeline start.<br>   _suffix_ is build from the repo selection.|

#### Parameters
As already mentioned above this pipeline has 2 build parameters. These are automatically reinitialized each time it's [seed job](https://portal-jenkins-staging.cwp.pnp-hcl.com/job/Seed-Jobs-Rooted/job/appscan-leap/) is is running. In the seed job's [job.yaml](https://git.cwp.pnp-hcl.com/Team-Q/dx-jenkins-jobs/blob/develop/jobs/appscan-leap/jobs.yaml) you'll find the definitions for **BUILD_TYPE** and **SCAN_REPO**.

#### Stages

|Stage|Description|
|--|--|
|Load parameters|This stage is used to load and set all parameters relevant to the pipeline.|
|Install Terraform|This stage installs terraform using the shared library **dxTerraformInstall()**|
|Setup environment|This stage prepares the EC2 instance as host for the Appscan run via a docker container. This includes a cleanup if reusing an existing EC2 instance when running the pipeline in debug mode. Then copying the appscan directory onto the EC2 instance and finally executing some scripts for the setup. These scripts are all located in appscan/script.|
|Create EC2 Instance|This stage creates a new EC2 instance using the shared library **dxTerraformCreateEc2Instance()** passing the parameters set in the first stage.|
|Clone git repositories|This stage covers the actual download of all repos selected for the scan using the script **04-clone-repositories.sh**. After the pure cloning this script will create the appscan script **cli_script.txt** running only those repo scans passed in.|
|Run AppScan scans|This stage runs the actual Appscan task using script **05-run-appscan.sh**. The script makes use of running the scan via a preconfigured container running on the EC2 instance and created in an ealier stage.|
|Upload reports to artifactory|This stage finally uploads the scan results into Artifactory at https://artifactory.cwp.pnp-hcl.com/ui/repos/tree/General/quintana-generic/appscan/leap-reports . Here you find folders for develop and the release branches. The scan result is uploaded within its corresponding branch folder into a subfolder named from the _timestamp_ the build was started followed by a _suffix_ created from the repo selection.\(i.e. 20230125-164800_full or 20230206-143822_content-ui)|
|Destroy EC2 instance|Destroys the previously created EC2 instance. This stage can be skipped by setting the DEBUG_MODE to true in parameters.yaml|

#### Post
This section provides the final cleanup at the end of the pipeline. Here we're using the shared library **dxWorkspaceDirectoriesCleanup()** which does this job for you.

## parameters.yaml
Besides the 2 build parameters mentioned above which provide choices for each individual build, the pipeline loads additional settings into the **pipelineParameters** list from the parameters.yaml file. This file provides parameters affecting the pipeline but don't change very often and need control about if changed.

|Parameter|Setting|Description|
|--|--|--|
|INSTANCE_TYPE|string: i.e. t2.xlarge, m5.8xlarge|Set instance type for EC2 instance running the scan.|
|INSTANCE_NAME|string: i.e. dx-appscan-scan|Instance name prefix used for tagging in AWS.| 
|DEBUG_MODE|boolean: true or false|If true runs pipeline in debug mode which keeps the EC2 instance at the end of the build.| 

