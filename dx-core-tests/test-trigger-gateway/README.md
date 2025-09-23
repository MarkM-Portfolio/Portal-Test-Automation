# DX Core Tests - test-trigger-gateway

This job is used to trigger DX-Core tests from multiple DX-Core builds.

## Setup

Configure a jenkins pipeline job to point to this git repository and configure it to use this Jenkinsfile.

Save the configuration and run this job **once**.
The first execution will fail, but allow jenkins to inherit the correct parameters from the Jenkinsfile.

After the first run, go and reconfigure the Job.
This time, create a parameter of the type `Run Parameter` that adds to the already existing ones.
As the `Name` you use `DxBuildVersion`.
As the `Project` you use the DX Core target Job that triggers this gateway. E.g.`build/legacy-build`.

Configure this Job to be `Trigger builds remotely` active and use `remoteTestTrigger` as the token.
Configure this Job tu run after the DX Core target job has run.