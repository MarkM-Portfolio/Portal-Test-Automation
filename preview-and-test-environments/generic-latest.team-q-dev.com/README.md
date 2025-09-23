# Deployment Pipeline for a generic latest machine

## Introduction

This pipeline is designed to deploy a latest-machine on a non Kubernetes based deployment using plain docker.
The deployment includes the creation of the corresponding EC2 instance and a Route53 record for accessibility via a friendly hostname.

This pipeline - unless others we already had - is not tied to a specific hostname or machine. 
It leverages the name of the GIT_BRANCH it is being executed from and uses this to determine its hostname and the artifacts to be used.

As an example, we can look at the following two scenarios:

a) This pipeline being deployed and configured to be run from the `develop` branch of the repository.
This leads to the hostname being `develop-latest.team-q-dev.com`.
All artifacts being loaded from artifactory must contain `develop` in their tagging.

b) This pipeline being deployed and configured to be run from the `release/95_CF172` branch of the repository.
This leads to the hostname being `release_95_CF172-latest.team-q-dev.com`.
All artifacts being loaded from artifactory must contain `release_95_CF172` in their tagging.

It is also possible to override the filter being used for specific docker images, by specifying parameters for the jenkins run.
Please see the parameter list following in this document to get further information.

## Services

|Service|Address|Comment|
|--|--|--|
|dx-core|HOSTNAME:10039/wps/portal|DX-Core application|
|DAM|HOSTNAME:3000/dx/api/dam/v1|API endpoint for ML|
|DAM|HOSTNAME:3000/dx/api/dam/v1/explorer|Swagger UI for ML|
|DAM|HOSTNAME:3000/dx/ui/dam|Standalone UI for ML|
|DAM|HOSTNAME:3000/dx/ui/dam/static|Static portlet resources for ML|
|Content-UI|HOSTNAME:5000/dx/ui/content|Standalone UI for content-ui|
|Content-UI|HOSTNAME:5000/dx/ui/content/static|Static portlet resources for content-ui|
|Site-Manager|HOSTNAME:5500/dx/ui/site-manager|Standalone UI for site-manager|
|Site-Manager|HOSTNAME:5500/dx/ui/site-manager/static|Static portlet resources for site-manager|
|Ring API|HOSTNAME:4000/dx/api/core/v1|API endpoint for RingAPI|
|Ring API|HOSTNAME:4000/dx/api/core/v1/explorer|Swagger UI for RingAPI|
|Image Processor|HOSTNAME:8080/dx/api/image-processor/v1|Image-processor API|
|Image Processor|HOSTNAME:8080/dx/api/image-processor/v1/explorer|Image-processor Swagger UI|


## Tools used

The following tools are used to provide this environment:
- **docker-compose** - docker-compose is being used to run a non-clustered instance of the [postgresql-persistence-layer](https://git.cwp.pnp-hcl.com/websphere-portal-incubator/postgresql-persistence-layer)
- **docker** - docker is used to run the required docker images (e.g. experience-api)
- **terraform** - used to manage the lifecycle of the created AWS resources

## Names used
- HCL DX is running as a docker container with the name `dx-core`
- HCL DX Ring-API is running as a docker container with the name `experience-api`
- HCL DX DAM docker container with the name `media-library`
- HCL DX Image Processor docker container with the name `image-processor`
- HCL DX CC docker container with the name `content-ui`
- HCL DX Site Manager docker container with the name `site-manager`

## Credentials and files used
This pipeline leverages the following credentials inside jenkins to gain access to repositories and servers:
- **test-automation-deployments** - this keypair is used to connect to the machine via ssh - it has been selected at initial creation of the EC2 instance
- **artifactory** - these credentials are used to connect to the docker repository
- **aws_credentials** - these credentials are used to manage AWS Resources via Terraform

## Pipeline steps

For a description of the individual steps, please look at the deploy.groovy and destroy.groovy files, which contain the steps with explaining commentary.

## Removing a created instance / Lifecycle

There are two jenkins pipelines provided in this package.
The first one is the `deploy.groovy` which will create all the resources in AWS and install all applications with the correct settings.
The second one is the `destroy.groovy` which will remove all resources created by the `deploy.groovy`.

**PLEASE NOTE:** the `destroy.groovy` can only remove an instance that is created from the git branch it is targeted to. 
Make sure to target both pipelines to the same git branch at all times to be able to create and destroy the correct instance.

## Parameters

For all possible parameters, please see the `settings.sh` file.

To override the images being taken from artifactory, you can use the following parameters via `This build is parameterized` in Jenkins:

**DX_CORE_IMAGE_FILTER**: Filter clause for the dx-core image tag, use SKIP if the application shall not be deployed

**MEDIA_LIBRARY_IMAGE_FILTER**: Filter clause for the media-library image tag, use SKIP if the application shall not be deployed

**MEDIA_LIBRARY_PERSISTENCE_IMAGE_FILTER**: Filter clause for the media-library image tag

**EXPERIENCE_API_IMAGE_FILTER**: Filter clause for the experience-api image tag, use SKIP if the application shall not be deployed

**IMAGE_PROCESSOR_IMAGE_FILTER**: Filter clause for the image-processor image tag, use SKIP if the application shall not be deployed

**CONTENT_UI_IMAGE_FILTER**: Filter clause for the content-ui image tag, use SKIP if the application shall not be deployed

**SITE_MANAGER_IMAGE_FILTER**: Filter clause for the site-manager image tag, use SKIP if the application shall not be deployed

**ENV_HOSTNAME**: To use a specific hostname for the instance to be deployed, specify a fully qualified hostname like `test-latest.team-q-dev.com`

**PUBLISH_EXTERNAL**: Set this to `true` if you want an external facing machine.