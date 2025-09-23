# Using CA certificates from Let's Encrypt

This pipeline provides an infrastructure as code (IaC) method to use CA certificates created by Let's Encrypt.
Let's Encrypt is a free, automated, and open certificate authority (CA), run for the public's benefit without any costs.

## Genral desciption
This pipeline provides methods to install and maintain certificates from Let's Encrypt.

| Parameter | Default Value | Notes |
| --- | --- | --- |
| TARGET_SYSTEM |  | System name where to install files (mandatory). |
| JKS_DEPLOY | no | If set to **yes** the pipeline will deploy the certificate and install renewal script on target system. |
| AWS_CREDENTIALS_ID | aws_credentials | AWS credentials ID to get access credentials from. |
| PEM_FILE_ID | test-automation-deployments | ID of a managed file which serves as ssh keyfile to access target system. |
| ADMIN_USER | admin | Jenkins user to restart Jenkins. This is set during Jenkins installation. |
| ADMIN_PASSWD | admin | Password for above Jenkins user. |
| JKS_PASSWORD | admin123 | Password to protect Jenkins keystore. This is set during Jenkins installation. |

For as much flexibility as possible the pipeline uses shell scripts which are provided in different subdirectories.

| Directory | Notes |
| --- | --- |
| common-scripts | This directory has common script files not related to a dedicated target system. |
| jenkins-scripts | This directory has scripts related to install certificates on a Jenkins master. |

## common-scripts
**C1-add-certbot.sh**
Certbot is a tools to support easy installation and maintenance of Let's Encrypt certificates. This scripts installs this tools along with all prereqs and the dns-route53 plugins to validate AWS instances.

**C2-add-acmesh.sh**
During implementation it turned out that Certbot still proposed in the Let's Encrypt documentation is not the best to use anymore as it needs snap and Python as prereqs. The tool acmesh is a pure bash script to support Let's Encrypt with no need of any prereqs. Since this tool is much more reliable it is now used in the pipeline. 

**C3-get-ca_cert.sh**
If either acmesh or certbot is installed this script will request and download a new CA certificate for a given AWS instance. This script needs following parameters.

| Parameter | Notes |
| --- | --- |
| TARGET_SYSTEM | System name where to install certificate. |
| AWS_ACCESS_KEY_ID | AWS access key ID. |
| AWS_SECRET_ACCESS_KEY | AWS secret access key. |

When using the pipeline these parameter are passed by the pipeline.

**C4-add-cronjob.sh**
This script can be used to add a cronjob to the target system. This cronjob should be capable to request a renewal of the issued certificate when it is about to expire. The parameter to pass is the name of the renewal script as full qualified path.

For a Jenkins system the script J2-renew-ca_cert.sh from the jenkins-scripts directory will be used.

## jenkins-scripts
**J1-add-ca_cert.sh**
This script requests and downloads a new CA certificate using C3-get-ca_cert.sh . After that it creates a pkcs12 file secured with a password which is then used as input for the creating the final Jenkins keystore. For the automated certificate renewal the script creates a config file which is read by the renewal scipt. After the certificate installation a safe Jenkins reboot is initiated and finally installes renewal cronjob.

**J2-renew-ca_cert.sh**
This is the renewal script used to automatically renew the installed certificate if it is about to expire. If a config file is found it will be used as input.

**J3-safeRestart.sh**
This script provides a wy to safely restart Jenkins.

## wildcard certificates
Wildcard certificates (e.g. *.domain_name) compared to single domain certificates (issued for a single system) can be used for multiple systems at the same time. The advantage is having just one certificate for all systems within the registered domain. A small disadvantage, this needs a centralized process for maintenance. This is where this process gets in.

A detailed description can be found in our docusaurus.

## wildcard certificate scripts
**L1-get-lewc_cert.sh**
This script requests and downloads a new wirdcard certificate from Let's Encrypt. After that this certicate is uploaded into a truststore in Artifactory at https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-truststore using script C5-backup-cert.sh located in the common-scripts directory.

**L2-download-lewc_cert.sh**
This script downloads all files for a given certificate.

**L3-update-lewc_cert.sh**
This script takes care on the renewal of a given certificate. A renewed certificate will be updated in the truststore using C5-backup-cert.sh . 
