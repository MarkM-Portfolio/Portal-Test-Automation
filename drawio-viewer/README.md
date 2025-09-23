# drawio-viewer

This pipeline spawns a self-hosted instance of draw.io that we can use to view draw.io diagrams dynamically in the browser.

## Directory structure

```text
drawio-viewer/
  helpers/                          # Contains all helper files like shell-scripts and the nginx config
  Jenkinsfile                       # Performs the deployment by creating an EC2 instance and a Route53 entry
  destroy.groovy                    # Removed the created EC2 instance and Route53 entry
  README.md                         # This document
```

## Pipeline output

This pipeline will create a singleton instance with the URL `https://dx-drawio-viewer.team-q-dev.com` that will contain a self-hosted version of draw.io.

This instance can be used to view diagrams in the browser.

## Pipeline flow

### Terraform

The pipeline will perform the creation of an EC2 instance of type `t3a.micro` which consists of 2 CPU cores and 1GB of memory. This is completely sufficient for hosting draw.io, while still being cheap.

For that instance there will be a Route53 entry created, that will point the DNS entry `dx-drawio-viewer.team-q-dev.com` to the instances private IP.

After that creation has completed, remote scripts are copied via `scp` and executed via `ssh`.

### Scripting

The scripting for this pipeline consists of two shell-scripts.

- `01-setup-docker.sh` will perform a basic docker installation on the remote instance for the user `centos`
- `02-setup-drawio.sh` will perform a docker based setup of draw.io with a nginx reverse proxy in front of it.

The draw.io setup script will remove all existing deployments if present and pull the container images for draw.io and nginx from our artifactory.  
Next it will create a docker network with the name `drawio` that is used to route traffic from nginx to the draw.io container.

Before creating any containers, the script pull the wildcard TLS certificate for `team-q-dev.com` and place it in a directory for later use.

The draw.io container with the name `drawio` will be launched and attached to the network. It will neither forward any ports to the host nor get a persistent volume.  
The nginx container with the name `nginx` will be launched and attached to the network. It will forward the ports `80` and `443` to the host and will mount the certificate directory as well as our custom `nginx.conf` in read only mode.  

Both containers are set to restart unless manually stopped. This ensures they will start if the machine gets rebooted.

## Configuration of Jenkins Job

### Target Agent

Since this pipeline creates infrastructure, it will run on the `build_infra` agents.

### Parameters

This pipeline does not accept any parameters.

### Used credentials

Create a list of credentials that is used by this pipeline, also describe which stage of the pipeline uses it:

| Credential ID | Used ENV vars | Description | Affected Pipeline Stage |
| -- | -- | -- | -- |
| test-automation-deployments | `DEPLOY_KEY` | Used to access the remote instance via ssh. | `Setup Draw.io` |

### Pipeline definition configuration

Configure the Job to use a `Pipeline script from SCM` with the target SCM being `Git`. Use the repository URL `git@git.cwp.pnp-hcl.com:Team-Q/Portal-Test-Automation.git` and a fitting git credential, e.g. `git-ssh-access-key`.

As Branch, specify what you need, default would be `develop`.

For the `Script Path` use `drawio-viewer/Jenkinsfile`