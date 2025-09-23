# PR-Check in Kubernetes

## Introduction

This is a centralised code base to run PR-Checks for differnt repos such as DAM, CC, RinAPI, Design Studio etc. .

## Available parameters

| Environment variable | Description | Default | Remarks
| -- | -- | -- | -- |
| `INSTANCE_NAME` | Name for the instance | kube-pr-check-test_YOUR_BRANCH_NAME | This is the branch name you are trying to run pr-checks with the prefix "kube-pr-check-test_"
| `NATIVE_KUBE_NAMESPACE` | Name for the deployment | pr-check |
| `HOST_NAME` | Host name | INSTANCE_NAME.hcl-dx-dev.net | 
| `ghprbTargetBranch` | Github pull requet Builder target branch  | develop | 
| `ghprbActualCommit` | Github pull request Builder actual commit  | YOUR_BRANCH_NAME | This is the bracnch name you are trying to run pr-checks
| `KUBE_DEPLOY_JOB` | Job to deploy native-kube environment based on deployment settings  | CI/kube-deploy/native-kube-next-deploy |
| `KUBE_REMOVE_JOB` | Job to remove native-kube environment  | CI/kube-deploy/native-kube-remove |
| `TARGET_REPO` | Target repo to run pr-checks  | This is based on the repo we are trying to run pr-checks |

## Functions from DX-Jenkins-Shared-Libraries

| Function name| Description |
| -- | -- |
| `dxKubectlWorkspaceInstall` | Function to install kubectl from shared library |
| `dxWorkspaceDirectoriesCleanup` | Function to clean-up workspace  |

## Used make targets

| Target name | Description |
| -- | -- |
| `create-test-pod` | Creates the test pod and a corresponding service to run PR-checks in |
| `copy-application-code` | Copies over complete application code to the test pod |
| `run-test-command` | Executes the test command, leveraging the environment.sh and executeDockerTest routine |
| `prepare-environment-vars` | Prepare the environment.sh file that contains all application config with the right host prefix |
