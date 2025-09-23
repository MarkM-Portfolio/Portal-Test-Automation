# drawio-viewer

This pipeline validates the configuration of remote search on a target instance. This will be used in combination with the Remote Search auto-configuration provided with Helm.

## Directory structure

```text
drawio-viewer/
  paramters.yaml                    # Contains all configurable parameters for this pipeline.,
  rs-autoconfig-tests.groovy        # Performs testing
  README.md                         # This document
```

## Pipeline output

This pipeline will either succeed or fail based on the test result.

## Pipeline flow

The Pipeline performs several tests for Remote Search configuration, based on the input parameters. See the corresponding stages in the groovy file for further information.

## Configuration of Jenkins Job

### Target Agent

Since this pipeline creates infrastructure, it will run on the `build_infra` agents.

### Parameters

This pipeline does not accept any parameters.

### Used credentials

See the `parameters.yaml` for further information.

### Pipeline definition configuration

Configure the Job to use a `Pipeline script from SCM` with the target SCM being `Git`. Use the repository URL `git@git.cwp.pnp-hcl.com:Team-Q/Portal-Test-Automation.git` and a fitting git credential, e.g. `git-ssh-access-key`.

As Branch, specify what you need, default would be `develop`.

For the `Script Path` use `rs-autoconfig-test/rs-autoconfig-tests.groovy`