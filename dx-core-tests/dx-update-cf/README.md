# dx-update-cf Job

## Description

This pipeline takes a configured 8.5/9.0 CF199 base AMI and performs an update to the configured CF version.
Therefore an EC2 instance gets created based on the CF199 base AMI. This EC2 instance will be configured with a Route53 record.
This record can be used to easily access the built environment.
After creation of the EC2 Instance, the update to the CF will be performed. Upon success, a new base AMI with the new CF will be created.
EC2 instance, AMI and Route53 entry remain persistent until housekeeping will remove them due to expiration.

## Pipeline flow

The following image describes the basic pipeline flow.

![dx-update-cf-flow](docs/dx-update-cf-flow.png)

The Job will be triggered by a successful DX-Core build. It will access the DX-Core builds NAME, to determine which build should be used.
This BUILD_LABEL is being used to tag the created resources such as EC2 instance and AMI, so that tasks which will pick those up are aware of the build version.

The running execution of the pipeline will be labeled in the following pattern: `dx-update-cf_YYYYMMDD-hhmm`.

The pipeline will create an EC2 instance based on the configured CF16 base AMI.
This instance name will be the name of the current pipeline execution, so `dx-update-cf_YYYYMMDD-hhmm`.
Besides that, the instance will receive a configured time-to-live in hours, that will also be added as a tag.
The DX-Core build label will added as a tag as well.

After creation of the EC2 instance, a Route53 record for the instance with the address `dx-update-cf_YYYYMMDD-hhmm.team-q-dev.com` will be created.

After all setup steps, the CF update of the DX-Core instance will be performed and test-zips will be copied.

If this step is successful, the pipeline creates an AMI out of the EC2 instance, which will receive the same tags as the EC2 instance.

After a successful run, all temporary data is being removed.

## Generated resources / artifacts

### EC2 instance

An EC2 instance will be created with the following tags (values are an example)

![ec2-instance-tags](docs/ec2-instance-tags.png)

### Route53 record

The following Route53 entry will be created (values are an example)

![route53-record](docs/route53-record.png)

### AMI

An AMI will be created with the following tags (values are an example)

![ami-tags](docs/ami-tags.png)

## Housekeeping / Time-to-live (TTL)

All resources are created with a TTL. The TTL can be configured in the pipeline, default value is 24 hours.
The pipeline will add an `expired` tag to the resources, which contains a timestamp based on SystemMillis().

Housekeeping (seperate job) will check those tags and remove expired resources.

## Connectivity / Dependencies to other Jobs

This pipeline will run after a DX-Core build and pick-up the NAME of the Build which needs to have the following syntax:

`BUILD_CONTEXT_YYYYMMDD-hhmmss_BRACH_NAME`

All other dependent tasks will need to pickup the NAME of this job to determine names of AMI etc.

## Handling Search (Tika/Stellent) during CF Upgrade
With the upcoming changes to search and the removal of Stellent, there is a new parameter in this pipeline.  By default, it's set to false,
which means that Stellent will be removed and Tika will be enabled during the applyCF step.  If you wish to keep Stellent in place, 
set the following parameter to true:

        `KEEP_STELLENT`:  Keep the existing Stellent in place during applyCF

