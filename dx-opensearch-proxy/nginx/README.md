# Opensearch proxy nginx configuration

This pipeline creates a proxy url for DX  public use using nginx.

## Directory structure

```text
dx-opensearch-proxy/nginx
    helpers/                          # Contains all helper files like shell-scripts and the nginx config
    Jenkinsfile                       # Performs the deployment by creating an EC2 instance and a Route53 entry
    parameters.yaml                   # Contains all possible parameters for this pipeline
    README.md                         # This document
```

## Pipeline output

The Pipeline creates an EC2 instance that is configured to act as an authentication proxy. For that purpose a NGINX server is configured.

The NGINX server will check for the user agent and the calling IP to verify if the incoming request is valid. The pipeline will create a route53 configuration values with the prefix `public-` with DX-URL.

The NGINX proxy will be listing on port `443` for incoming https requests.


## Pipeline flow

### Terraform

The pipeline will perform the creation of an EC2 instance of type `t3a.small`. This is completely sufficient to run the proxy.

For that instance there will be a Route53 entry created, that will point the DNS entry `YOUR-INSTANCE_NAME.team-q-dev.com` to the instances private IP.

After that creation has completed, remote scripts are copied via `scp` and executed via `ssh`.

### Scripting

The scripting for this pipeline consists of multiple shell-scripts.

- `00-setup-all.sh` Executes all subsequent setup scripts in the correct order
- `01-setup-nginx.sh` Installs a plain NGINX setup without special configuration
- `02-configure-nginx.sh` Re-configures NGINX with our desired proxy configuration.

The individual steps of these scripts can be seen in their respective code, as it is well commented.

### NGINX configuration

The NGINX configuration is done with `ngingx.conf`. For the dynamic creation of server entries this file has a `include /etc/nginx/conf.d/*.conf;` statement at the end of the **http** section. This will automatically include all configuration files found in `/etc/nginx/conf.d`. These files are created by the pipeline using the template file `server_tpl.conf` from the helpers directory.

Server configuration are created from the `proxy-urls.json` file also located in the helpers directory. This JSON file defines all servers to be added to the NGINX proxy.

```
{
    "proxyDomain": "your-proxy-domain-name",             # domain for all servers
    "servers": [
        {
            "configName": "your-configuration-name",     # name for .conf file
            "proxyName":  "your-proxy-server-name",      # prefix for proxyDomain
            "targetUrl":  "target-url-where-to-route"    # target URL
        }
    ]
}
```

Example:
```
{
    "proxyDomain": "team-q-dev.com",
    "servers": [
        {
            "configName": "google",
            "proxyName":  "public-ralf_test_nginx",
            "targetUrl":  "https://www.google.com"
        },
        {
            "configName": "ibm",
            "proxyName":  "public2-ralf_test_nginx",
            "targetUrl":  "https://www.ibm.com"
        }
    ]
}
```

## Configuration of Jenkins Job

### Target Agent

Since this pipeline creates infrastructure, it will run on the `build_infra` agents.

### Parameters

See the `parameters.yaml` for all available parameters.

### Used credentials

This pipeline requires a Jenkins API key to configure the NGINX server. The ID of the credentials used is defined by the parameter `JENKINS_API_CREDENTIAL_ID`.
### Pipeline definition configuration

Configure the Job to use a `Pipeline script from SCM` with the target SCM being `Git`. Use the repository URL `git@git.cwp.pnp-hcl.com:Team-Q/Portal-Test-Automation.git` and a fitting git credential, e.g. `git-ssh-access-key`.

As Branch, specify what you need, default would be `develop`.

For the `Script Path` use `dx-opensearch-proxy/nginx/Jenkinsfile`
