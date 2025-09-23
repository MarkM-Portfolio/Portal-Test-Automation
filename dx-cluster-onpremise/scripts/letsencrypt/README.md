# Using CA certificates from Let's Encrypt

This step provides an infrastructure as code (IaC) method to use CA certificates created by Let's Encrypt.
Let's Encrypt is a free, automated, and open certificate authority (CA), run for the public's benefit without any costs.

## Genral desciption
This step provides methods to install certificates from Let's Encrypt.

| Parameter | Default Value | Notes |
| --- | --- | --- |
| HOSTNAME |  | System name where to install files (mandatory). |

For as much flexibility as possible this step uses shell scripts which are provided in common-scripts.

| Directory | Notes |
| --- | --- |
| common-scripts | This directory has common script files not related to a dedicated target system. |

## common-scripts
**C1-add-acmesh.sh**
During implementation it turned out that Certbot still proposed in the Let's Encrypt documentation is not the best to use anymore as it needs snap and Python as prereqs. The tool acmesh is a pure bash script to support Let's Encrypt with no need of any prereqs. Since this tool is much more reliable it is now used in the pipeline. 

**C2-get-ca_cert.sh**
If either acmesh or certbot is installed this script will request and download a new CA certificate for a given AWS instance. This script needs following parameters.

| Parameter | Notes |
| --- | --- |
| HOSTNAME | System name where to install certificate. |
| AWS_ACCESS_KEY_ID | AWS access key ID. |
| AWS_SECRET_ACCESS_KEY | AWS secret access key. |

