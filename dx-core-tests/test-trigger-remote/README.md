# DX Core Tests - test-trigger-remote

This job is used to trigger a remote `test-trigger-gateway` on `shire`.

## Setup

Configure a jenkins pipeline job to point to this git repository and configure it to use this Jenkinsfile.

Configure the following parameters:

|Type|Name|Example|Description|
|--|--|--|--|
|Run Parameter|DXBuildVersion|build/legacy_build_develop|This is the dx-core build job that is being watched.|
|String Parameter|DELIVERY_MSA_SHARE_MNT|/msa/rtpmsa/projects/b/build.portal|Path to source MSA|
|String Parameter|PLAYGROUND_MSA_SHARE_MNT|/msa/rtpmsa/projects/b/playground.build.portal|Path to target MSA|
|String Parameter|LEGACY_MODE|true|Must a copy be performed or not|

Configure this Job to run after the DX Core target job has run.