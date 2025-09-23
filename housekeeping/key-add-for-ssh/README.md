# key-add-for-ssh

This Pipeline allows to add public SSH keys to remote instances by connecting to them via SSH. The public key will be validated before it is transferred over to the remote machine.

## Directory structure

```
housekeeping/
  key-add-for-ssh/
    README.md                       # This file
    Jenkinsfile                     # Contains the Pipeline execution code
    parameters.yaml                 # Contains all allowed parameters for this Pipeline
```

## Pipeline output

This pipeline will add the provided public SSH key to a remote EC2 instance that has been created using our Jenkins automation.

## Pipeline Flow

### Load parameters

All passed in parameters will be loaded and validated if they are empty or not.

### Validate target environment

This stage will check if the provided `TARGET_INSTANCE` is allowed for SSH key adding. It will check with a `RegEx` and with a predefined list of target environments.

If the environment of choice is not matching the `Regex` or in the list, the Pipeline will exit with an error.

Additionally it will check if `TARGET_INSTANCE` is a FQDN. Otherwise it will try to append `.team-q-dev.com` to handle cases where only a hostname is supplied.

### Validate PUBLIC_KEY

This will execute the openssl tools to validate the provided public SSH key from the `PUBLIC_KEY` parameter.

### Add authorized_key on target environment

Add the valid public key to the remote instance.

## Configuration of Jenkins Job

### Target Agent

This pipeline will run on agents labeled with `build_infra`.

### Parameters

See `parameters.yaml` for a list of all available parameters.

### Used credentials

| Credential ID | Used ENV vars | Description | Affected Pipeline Stage |
| -- | -- | -- | -- |
| test-automation-deployments | `DEPLOY_KEY` | Used to access the remote machine via SSH. | `Add authorized_key on target environment` |

### Pipeline definition configuration

Configure the Job to use a `Pipeline script from SCM` with the target SCM being `Git`. Use the repository URL `git.cwp.pnp-hcl.com:Team-Q/Portal-Test-Automation.git` and a fitting git credential, e.g. `git-ssh-access-key`.

As Branch, specify what you need, default would be `develop`.

For the `Script Path` use `housekeeping/key-add-for-ssh/Jenkinsfile`

## Usage

### Obtain a SSH public key for your development machine

To connect to a target machine, you need a SSH key pair for your development machine. 
It consists of a private key, which uniquely identifies your machine and should never be exposed to anybody and a public key,
which is used to identify your machine safely by other systems.

To check if you have an SSH public key already, open up a `Terminal` and execute the following action:

```sh
# Check content of your SSH directory
ls -lah ~/.ssh/
```

The output should somewhat look like this:

```text
total 20K
drwx------.  2 pmilich pmilich   65 Feb 16 11:35 .
drwx------. 22 pmilich pmilich 4.0K Feb 16 11:35 ..
-rw-------.  1 pmilich pmilich  419 Feb 14 09:01 id_ed25519
-rw-r--r--.  1 pmilich pmilich  108 Feb 14 09:01 id_ed25519.pub
-rw-r--r--.  1 pmilich pmilich 7.5K Feb 16 11:42 known_hosts
```

or like this

```text
total 20K
drwx------.  2 pmilich pmilich   65 Feb 16 11:35 .
drwx------. 22 pmilich pmilich 4.0K Feb 16 11:35 ..
-rw-------.  1 pmilich pmilich  419 Feb 14 09:01 id_rsa
-rw-r--r--.  1 pmilich pmilich  108 Feb 14 09:01 id_rsa.pub
-rw-r--r--.  1 pmilich pmilich 7.5K Feb 16 11:42 known_hosts

```

If you don't have any output that looks like this, and have neither the `id_rsa` nor the `id_ed25519` files, follow the guidance on how to create a SSH key:

[MacOS: Generating SSH Key](https://pages.git.cwp.pnp-hcl.com/Team-Q/development-doc/docs/technical-enablement/git-doc/#generate-ssh-key-using-git-bash)

### Using the Pipeline

Use `cat ~/.ssh/id_rsa.pub` or `cat ~/.ssh/id_ed25519.pub` (depending on what you have in that directory) in a Terminal session on your development machine to retrieve your public key.
Mark and copy the output in the terminal.

In the Pipeline, paste your key into the `PUBLIC_KEY` field.

After that, select your target environment from the `TARGET_ENVIRONMENT` list OR if your are using the `FQDN` version of the Pipeline, enter the full qualified domain name into the text field, e.g. `native-kube-pmi.team-q-dev.com`.

Click `Build` to start the process.

If all steps have been successful, you can use 

```
ssh centos@<TARGET_ENVIRONMENT>
```

to connect to the environment.

**If you experience an error**
Look into the log file of the run in Jenkins, to see the error message.
