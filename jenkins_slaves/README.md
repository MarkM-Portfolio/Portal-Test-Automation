# Jenkins agent management

In our current jenkins environment we have a several different slaves with different sets of capabilities. Additionally the hardware requirements for each and every of these systems might differ. This makes setting up a new slave with changed hardware (e.g. NVME instead of EBS) difficult from AMIs. The goal was to provide a set of scripts which can be easily automated, extended and reused to spin up a new slave with all the necessary features as well as special scripting to support specific hardware features.

# Automated provisioning

## Create a single, permanent agent

To create an agent run the Jenkins job "housekeeping/create-new-agent", providing the following parameters:

| Parameter | Value | Notes |
| --- | --- | --- |
| INSTANCE_NAME | unique host name | Agent name and host name of underlying EC2 instance |
| AGENT_DESCRIPTION | text | Description of this agent's purpose |
| AGENT_LABELS | space-separated list of labels | To allow matching of jobs to nodes |
| NUMBER_OF_EXECUTORS | number of executors on new agent | Default is 4 |
| JENKINS_API_CREDENTIALS_ID | credentials ID in Jenkins store | Credentials that must be user name / API key, not user name / password |


The agent will be created on a new EC2 instance with type "m5ad.xlarge" and registered in Route53 with a FQDN of "&lt;INSTANCE_NAME&gt;.team-q-dev.com". The owner of the instance and its EBS boot volume will be the initiator of the Jenkins job.

The jenkins home directory and the docker directories will be placed on the NVMe disk for better performance.

## Decommission agent

To decommission an agent run the Jenkins job "housekeeping/decommission-agent", providing the following parameters:

| Parameter | Value | Notes |
| --- | --- | --- |
| INSTANCE_NAME | unique host name | Agent name and host name of underlying EC2 instance |
| DELAY_BEFORE_DELETE_MINUTES | minutes between agent being marked offline and being deleted | Default is 60 |
| JENKINS_API_CREDENTIALS_ID | credentials ID in Jenkins store | Credentials that must be user name / API key, not user name / password |


The agent will be immediately marked as unable to accept new jobs but will continue executing any in-progress jobs. After the specified number of minutes the agent will be fully deleted (i.e. deregistered with Jenkins and then the EC2 instance, volume and Route53 record deleted).

## Delete agent

To delete an agent run the Jenkins job "housekeeping/delete-agent", providing the following parameters:

| Parameter | Value | Notes |
| --- | --- | --- |
| INSTANCE_NAME | unique host name | Agent name and host name of underlying EC2 instance |
| JENKINS_API_CREDENTIALS_ID | credentials ID in Jenkins store | Credentials that must be user name / API key, not user name / password |


The agent will be immediately deleted (i.e. deregistered with Jenkins and then the EC2 instance, volume and Route53 record deleted).

## Create a pool of temporary agents

To create a set of identical agents with a prescribed lifespan run the Jenkins job "housekeeping/create-time-limited-agents", providing the following parameters:

| Parameter | Value | Notes |
| --- | --- | --- |
| AGENT_CATEGORY | The part of the name before the number | e.g. prcheck |
| AGENT_PURPOSE | The part of the name after the underscore | e.g. test |
| START_INSTANCE_NUMBER | The numeric part of the name of the first agent to create | Should not overlap other agents with the same category and purpose |
| NUMBER_OF_INSTANCES | How many agents to create | They will have consecutive numbers starting at START_INSTANCE_NUMBER |
| AGENT_DESCRIPTION | text | Description of these agents' purpose |
| AGENT_LABELS | Space-separated list of labels for the agents | To allow matching of jobs to nodes |
| NUMBER_OF_EXECUTORS | Number of executors on each new agent | Default is 1 |
| AGENT_LONGEVITY_HOURS | Hours before new agents are marked as offline | Default is 4 |
| DELAY_BEFORE_DELETE_MINUTES | Minutes between agents being marked offline and being deleted | Default is 60 |
| JENKINS_API_CREDENTIALS_ID | credentials ID in Jenkins store | Credentials that must be user name / API key, not user name / password |


To better understand some of the terms used above, please see the [agent naming conventions](https://pages.git.cwp.pnp-hcl.com/Team-Q/development-doc/docs/documentation/portal-build/jenkins-naming-schema/).

The agents will be created as described in the section for a single agent above. They will then be automatically decommissioned after the specified number of hours.

# Manual provisioning

Follow the steps in the subsections below to provision a standard agent manually.

## provisioning a new slave in aws

Currently a manual step specifying which type of instance you like to have. Maybe scripts from the performance automation can be reused.

1. Launch a new Instance via AWS Console with the following settings
   - AMI -> AWS Market Place -> CentOS 7 (x86_64) - with Updates HVM
   - Your required instance type (https://aws.amazon.com/ec2/instance-types/?nc1=h_ls) please also compare prices
   - network: vpc-0c3e842b4285e0b0c | cwp-quin-east
   - Subnet Build01
   - Select storage as applicable for your need -> Check "delete on termination"
   - Security Group: Select existing -> AllTrafficeTester
   - review and launch
   - Select a Key pair which you have access to
2. Once the instance is launched add the following labels:
   - Name: "b-[stage]-[Lord of the ring Character]" eg. "b-rohan-strider"
   - Area: BUILD
   - Owner: [your mail]
   - termFlag: N
3. Navigate to the connected storage device and add the following labels 
   - Name: name of the connected instance
   - Area: BUILD
   - Owner: [your mail]
   - termFlag: N
4. Add DNS entry for route53
   - copy IP from instance
   - Route53 -> Hosted Zones -> team-q-dev.com -> Create Record Set
     - Enter name of the server and add the IP
     - Press Create


## populate with necessary software and credentials
To setup a new jenkins slave server, just copy the complete `setupscripts` directory into a workdir on a blank CentOS machine.
As `root` navigate into the workdir on the target machine and execute the scripts in their numeric order.

## Add slave at jenkins master

1. Login to jenkins Master
2. Manage Jenkins -> Manage Nodes -> New Node
   - Name: [Lord of the ring Character]
   - Permanent Agent
     - Description
     - Number of executers: This will define how many jobs can run in parallel
     - Remote root directory: /home/wpbuild/jenkins
     - Labels: the build labels which the system is capable to perform
     - Usage: Only jobs with label expression matching this node
     - Launch Method: Launch Agent agents via SSH
     - Host: [url specified in Route53]
     - Credentials: wpbuild
     - Host Key verification: Non
     - Availability: Keep this agent online

## Stale docker container cleanup

For the user `wpbuild` there is a cronjob in place that will automatically stop and remove all docker containers older than 48 hours.
This check runs every 5 minutes.
The logs of the last run of this cronjob can be found in `/tmp/docker_cleanup.log`