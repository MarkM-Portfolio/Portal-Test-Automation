# Jenkins Job Transfer

## Introduction

This job can be used to transfer jobs from one Jenkins environment to another one. Currently the transfer from PJS to PJD and from PJD to PJS is supported.

## Usage

This job accepts the following parameters:

| Parameter | Description | Example |
| -- | -- | -- |
| DIRECTION | Defines the direction of the transfer. Either accepts the value `staging-to-dev` or `dev-to-staging`. |`staging-to-dev`|
| SOURCE_JOB_NAME | Defines the name of the job that should be transferred. Only the job name, without any paths etc. It must be present at the SOURCE environment in the SOURCE_DIRECTORY defined. | `z_azure-deploy-test` |
| TARGET_JOB_NAME | Defines the name of the job after transfer in the TARGET environment. Only the job name, without any paths etc. If provided, it will be stripped off of all spaces. Else, it will default to be the same as the SOURCE_JOB_NAME. | `z_azure-deploy-test-copy` |
| SOURCE_DIRECTORY | Defines the source directory to copy the job from. Missing `job` subdirectories needed as of the jenkins job url notation will be added automatically. | `/job/kube/` or `CI/automation-deployments` |
| TARGET_DIRECTORY | Defines the target directory to copy the job to. Missing `job` subdirectories needed as of the jenkins job url notation will be added automatically. | `/job/kube/` or `CI/automation-deployments` |

Defaults are set via parameters.yaml using dxParametersLoadFromFile from our shared library.