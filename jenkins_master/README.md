# Jenkins master management

This directory contains several pipelines for maintaining a jenkins cluster, especially the master instance.

This pipeline provides an infrastructure as code (IaC) method to setup a new Jenkins cluster consisting of one master and several agents.

## Directory structure

```text
this-pipeline/
  backup_restore/                       # Contains all code logic for the jenkins master backup and restore logic
  configurations/                       # Contains the configuration files used for a fresh deploy and destroy
  setupscripts/                         # Scripts used for the fresh deploy
    helpers/                            # Sub helper scripts used for the fresh deploy
  terraform/                            # Terraform scripting used to create EC2 instances
  backupJenkins.groovy                  # Backup pipeline
  destroy.groovy                        # Destroy pipeline
  Jenkinsfile                           # Fresh deploy pipeline
  monitorAgents.groovy                  # Jenkins agent monitor # WIP by ralf.gliemer
  restoreJenkins.groovy                 # Restore pipeline
  README.md                             # This document
```

## Pipeline output

### Fresh deploy

The fresh deploy pipeline will create a Jenkins instance based on the selected configuration file in `configurations/`. This may contain credentials to be migrated, directories to be created and Jenkins agents to be provisioned.

### Backup

The backup pipeline will create a backup from the Jenkins environment you select and put it into artifactory.

### Restore

The restore pipeline will re-create a Jenkins master by using a selected backup taken from artifactory.

### Destroy

The destroy pipeline will remove the Jenkins master and agents of the selected environment. Environments are defined using a configuration file in `configurations/`. This only works for environments that have been created using the fresh deploy pipeline before. Please note, that if the list of agents in the configuration file changed between the execution of fresh deploy and destroy, **not all agents will get cleaned up**. This is also true for agents that have been provisioned outside of this automation.

### Monitor Agents

The monitor agents pipeline will use E-Mail notifications to inform people if agents do not work anymore. This is currently WIP and not used in production.

## Pipeline flow

### Fresh deploy

The following description shows a high level overview on the steps included for provisioning a new environment.

- Read stage properties
- Prepare Terraform
- Create (master) instance, deploy prereqs
- Install Jenkins
- Install Jenkins plugins
- Setup Jenkins security
- Setup In-Script approvals
- Create agents instances and deploy prereqs
- Register agents with Jenkins and add directories
- Switch to SSL
- Deploy Monitoring

To create an new Jenkins run job **housekeeping/create-new-jenkins**

#### Read stage properties

In this step, the basic environment variables and deployment properties are configured.

Deployment properties are taken from the selected Jenkins stage property file.

Those files are placed in the `configurations/` directory and contain the full deployment properties for the selected Jenkins stage. The object created out of this file is present in the whole pipeline execution and referenced where needed to provide the correct configuration.

The environment to be deployed is determined by the `params.environment` variable.

#### Create (master) instance, deploy prereqs

The master will be created on a new EC2 instance with type configured in **INSTANCE_TYPE_MASTER** and registered in Route53 with a FQDN of **&lt;jenkins.subDomain&gt;.&lt;jenkins.domain&gt;**. The owner of the instance and its EBS boot volume will be the initiator of the Jenkins job.

The basic prereqs are installed as part of the EC2 instance creation and are defined in **jenkins_master/setupscripts/helpers/01-setup-prereqs.sh**. The setupscripts directory is copied into the home directory of the master instance.

The IP address is stored in the global environment variable **INSTANCE_MASTER_IP** so it can be used in later steps.

#### Install Jenkins and the plugins

The Jenkins installation will run on the new EC2 instance by starting **02-install-jenkins.sh** in the setupscripts directory.

The plugins will then be installed by running **03-install-jenkins-plugins.sh** also in the setupscripts directory. The plugins are taken from the `jenkins.plugins` list of the deployment property yaml file.

#### Setup Jenkins security

This step installs the basic Jenkins security and clones given credentials configured in `jenkins.migrationCredentials` from the current system into the new Jenkins master.

After getting all credentials from the current system the security install will start by calling **04-setup-jenkins-security.sh**. The basic security includes a predefined set of credentials defined in **jenkins_master/setupscripts/helpers/default-creds.txt**. After that the cloned credentials get installed.

**default-creds.txt** is a XML look alike file. Each credential starts with a `<cred-def` identifier and ends with a single `/>` line.

```xml
<cred-def crdentialType
credentialID
credentialDescription
credentialUser or credentialPassphrase
[credentialPassword or credentialKey]
/>
```

The following matrix shows the supported credentialTypes and what needs to be defined.

| Type | ID | Description | User/Passphrase | Password/Key |
| --- | --- | --- | --- | --- |
| BasicSSHUserPrivateKey | unique ID | text | UserName | Password |
| UsernamePasswordCredentialsImpl | unique ID | text | UserName | multiline Key |
| StringCredentialsImpl | unique ID | text | Passphrase | --- |

**Examples for each credential type:**

```xml
<cred-def BasicSSHUserPrivateKey
key-id-user1
Key for User 1
user1
-----BEGIN RSA PRIVATE KEY-----
xxxxxxxxxxxxxx
yyyyyyyyyyyyyy
zzzzzzzzzzzzzz
-----END RSA PRIVATE KEY-----
/>
```

```xml
<cred-def UsernamePasswordCredentialsImpl
pw-id-user2
UserID/Password for User 2
user2
abc123-789XYZ
/>
```

```xml
<cred-def StringCredentialsImpl
pass-id-user3
Access passphrase for User 3
ThisIsAn_ACCESS_PassphraseForUser3
/>
```

#### Setup In-Script approvals

This step clones all In-Script approval from the current system into the new Jenkins master by running **05-migrate-jenkins-in-process.sh**.

#### Create agent instances and deploy prereqs

Create all new agents as EC2 instances. This makes use of the scripts stored in this project in directory **jenkins_slaves**.

The list of agents to be created is defined in `jenkins.agentDefinition`

#### Register agents with Jenkins and create directories

Register the new created EC2 instances with the Jenkins master and set the master agent offline running **06-setup-jenkins-agents.sh**.
Following is the list of mandatory parameters to handover to the script in their respective order.

| Order | Variable Name | Description |
| --- | --- | --- |
| $1 | JENKINS_URL | FQDN for Jenkins master as mentioned earlier |
| $2 | AGENTS_LIST | Comma separated list of agents to register, see INSTANCE_NAME_SLAVES in prepare step |
| $3 | URL_DOMAIN_NAME | Domain name used to register each agent, see prepare step |
| $4 | WPBUILD_CREDENTIALS_ID | Credential ID of user to lunch agent, see prepare step |
| $5 | USER_ADMIN | Admin user for Jenkins installation, see prepare step |
| $6 | DIRECTORY_LIST | List of directories to be created in jenkins |

The configuration file to register the agents is located in the helpers directory, named **agents.txt**, and has one line for each agent to register. Each line is a comma separated list of 4 strings and the file must end with a single "end-of-agents" line. It will be created automatically during runtime.

| Position | Description |
| --- | --- |
| 1 | Jenkins agent name. This will be compared to the given list to register. |
| 2 | Number of executor to create for this agent |
| 3 | List of label to register with this agent separated by blanks |
| 4 | Description for this agent |

The input for that file is defined in `jenkins.agentDefinitions` and follows has the following schema:

```yaml
jenkins:
  agentDefitions:
    - name: "agent_A"
      executors: "6"
      labels:
        - "one_label"
        - "second_label"
      description: "Just an agent"
```

#### Configure SSL

Configures a self-signed SSL certificate and prepares Jenkins to be configured for HTTPS access.
#### Deploy Monitoring

Deploys a prometheus and Grafana Instance on the Master EC2 instance, configured to monitor the Jenkins Master.

## Configuration of Jenkins Job

### Fresh deploy

#### Target Agent

The pipeline will run on agents labeled with `build_infra`.

#### Parameters

| Type | Parameter name | Sample content | Required | Description |
| -- | -- | -- | -- | -- |
| Choice | environment | pjt | Yes | Determines which configuration to use. Will be defined by the pipeline itself, currently accepts `pjd`, `pjt` and `pjs`. |
| String | TF_LOG | WARN | No | Determines log level of terraform, defaults to `WARN` |
| String | TERRAFORM_ZIP | terraform_0.12.20_linux_amd64.zip | No | Zip of terraform to be pulled from artifactory, defaults to `terraform_0.12.20_linux_amd64.zip` |

#### Used credentials

| Credential ID | Used ENV vars | Description | Affected Pipeline Stage |
| -- | -- | -- | -- |
| aws_credentials | `AWS_SECRET_ACCESS_KEY`, `AWS_ACCESS_KEY_ID` | AWS Credentials used for creation of EC2 instances. | `Create instance, deploy prereqs`, `Create agent instances, deploy prereqs` , `cleanup` |

#### Pipeline definition configuration

Configure the Job to use a `Pipeline script from SCM` with the target SCM being `Git`. Use the repository URL `git@git.cwp.pnp-hcl.com:Team-Q/Portal-Test-Automation.git` and a fitting git credential, e.g. `git-ssh-access-key`.

As Branch, specify what you need, default would be `develop`.

For the `Script Path` use `jenkins_master/Jenkinsfile`

### Destroy

#### Target Agent

The pipeline will run on agents labeled with `build_infra`.

#### Parameters

| Type | Parameter name | Sample content | Required | Description |
| -- | -- | -- | -- | -- |
| Choice | environment | pjt | Yes | Determines which configuration to use. Will be defined by the pipeline itself, currently accepts `pjd` and `pjt`. |
| String | TF_LOG | WARN | No | Determines log level of terraform, defaults to `WARN` |
| String | TERRAFORM_ZIP | terraform_0.12.20_linux_amd64.zip | No | Zip of terraform to be pulled from artifactory, defaults to `terraform_0.12.20_linux_amd64.zip` |

#### Used credentials

| Credential ID | Used ENV vars | Description | Affected Pipeline Stage |
| -- | -- | -- | -- |
| aws_credentials | `AWS_SECRET_ACCESS_KEY`, `AWS_ACCESS_KEY_ID` | AWS Credentials used for creation of EC2 instances. | `Destroy master and agents` |

#### Pipeline definition configuration

Configure the Job to use a `Pipeline script from SCM` with the target SCM being `Git`. Use the repository URL `git@git.cwp.pnp-hcl.com:Team-Q/Portal-Test-Automation.git` and a fitting git credential, e.g. `git-ssh-access-key`.

As Branch, specify what you need, default would be `develop`.

For the `Script Path` use `jenkins_master/destroy.groovy`.

## Monitoring access

Monitoring can be accessed via http://`jenkins.subDomain`.`jenkins.domain`:3000 and credentials `admin`:`p0rtal4u`.

---

## Jenkins Failure Build Alerts

Script File: [jenkins_alert.py](jenkins_master/setupscripts/helpers/jenkins-build-alert.py)

#### Provide values to the following variables

```
export SERVER_URL = "portal-jenkins-develop.cwp.pnp-hcl.com"
export USER = <PJD Jenkins User Id>
export API_TOKEN = <PJD Jenkins User API Token>
export WEBHOOK_URL = <Google Chat Workspace Webhook URL>
```

| ENV vars | Description | Get Value From |
| -- | -- | -- |
| `SERVER_URL` | The URL of Portal Jenkins Develop | Portal Jenkins Develop Website |
| `USER` | Jenkins User ID | `https://portal-jenkins-develop.cwp.pnp-hcl.com/user/username@hcl.com/` |
| `API_TOKEN` | Jenkins User API Token | Create API Token in `https://portal-jenkins-develop.cwp.pnp-hcl.com/user/username@hcl.com/configure` |
| `WEBHOOK_URL` | Google Chat Workspace Webhook URL | Create new workspace in Google Chat and add webhook |
