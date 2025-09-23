# New WCM Crawler

This pipeline adds a new WCM crawler to the EAP environment.

## Directory structure

```text
dx-opensearch-eap/new-wcm-crawler
    helpers/                       # Contains all helper files like shell-scripts
    Jenkinsfile                    # Performs the WCM crawler creation
    parameters.yaml                # Contains all possible parameters for this pipeline
    README.md                      # This document
```

## Pipeline output

A new working WCM crawler in the EAP environment tested and triggered once.

## Pipeline flow

### Overview

- create a new entry in the management index with the give name and type wcm.
- add new crawler
- test new crawler connection
- trigger new crawler
- test crawling result

### Scripting

The scripting for this pipeline consists of multiple shell-scripts.

- `01-create-mgmt-index.sh` - Create new entry in management index
- `02-add-new-crawler.sh` - Add new crawler to environment
- `03-test-crawler-connection.sh` - Test crawler connection to WCM
- `04-trigger-new-crawler.sh` - Start new crawler once
- `05-test-result.sh` - Test crawling result

## Configuration of Jenkins Job

### Parameters

See the `parameters.yaml` for all available parameters.

### Pipeline definition configuration

Configure the Job to use a `Pipeline script from SCM` with the target SCM being `Git`. Use the repository URL `git@git.cwp.pnp-hcl.com:Team-Q/Portal-Test-Automation.git` and a fitting git credential, e.g. `git-ssh-access-key`.

As Branch, specify what you need, default would be `develop`.

For the `Script Path` use `dx-opensearch-eap/new-wcm-crawler/Jenkinsfile`
