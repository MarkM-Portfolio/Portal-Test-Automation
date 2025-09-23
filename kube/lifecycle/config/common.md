# Common configuration parameters

## Image filters

The following parameters can be used to determine the images being pulled for the environment.

### Parameters

| Parameter | Description | Default |
|--|--|--|
| CORE_IMAGE_FILTER | Can be a full image tag or a fragment of a tag, will try to get the latest best match. | `develop` |
| DAM_IMAGE_FILTER | Can be a full image tag or a fragment of a tag, will try to get the latest best match. | `develop` |
| CC_IMAGE_FILTER | Can be a full image tag or a fragment of a tag, will try to get the latest best match. | `develop` |
| DAM_PLUGIN_GOOGLE_VISION_IMAGE_FILTER | Can be a full image tag or a fragment of a tag, will try to get the latest best match. | `develop` |
| RINGAPI_IMAGE_FILTER | Can be a full image tag or a fragment of a tag, will try to get the latest best match. | `develop` |
| IMGPROC_IMAGE_FILTER | Can be a full image tag or a fragment of a tag, will try to get the latest best match. | `develop` |
| DAM_KALTURA_PLUGIN_IMAGE_FILTER | Can be a full image tag or a fragment of a tag, will try to get the latest best match. | `develop` |
| LDAP_IMAGE_FILTER | Can be a full image tag or a fragment of a tag, will try to get the latest best match. | `develop` |
| RS_IMAGE_FILTER | Can be a full image tag or a fragment of a tag, will try to get the latest best match. | `develop` |
| RUNTIME_CONTROLLER_IMAGE_FILTER | Can be a full image tag or a fragment of a tag, will try to get the latest best match. | `develop` |
| PERSISTENCE_METRICS_EXPORTER_IMAGE_FILTER | Can be a full image tag or a fragment of a tag, will try to get the latest best match. | `develop` |
| OPENSEARCH_IMAGE_FILTER | Can be a full image tag or a fragment of a tag, will try to get the latest best match. | `develop` |
| SEARCH_MIDDLEWARE_IMAGE_FILTER | Can be a full image tag or a fragment of a tag, will try to get the latest best match. | `develop` |
| MASTER_DEPLOYMENT_LEVEL | Universal Master version parameter(This is applicable only for Master deployments - N, N-1, N-2). | `NA`(Not Applicable) |


## Image paths

The following parameters are used to describe the image paths of all used images in the environment. They are relative to the image repository base.

### Parameters

| Parameter | Description | Default |
|--|--|--|
| CORE_IMAGE_PATH | Path to image. | `dx-build-output/core/dxen` |
| DAM_IMAGE_PATH | Path to image. | `dx-build-output/core-addon/media-library` |
| CC_IMAGE_PATH | Path to image. | `dx-build-output/core-addon/content-ui` |
| DAM_PLUGIN_GOOGLE_VISION_IMAGE_PATH | Path to image. | `dx-build-output/core-addon/api/dam-plugin-google-vision` |
| RINGAPI_IMAGE_PATH | Path to image. | `dx-build-output/core-addon/api/ringapi` |
| IMGPROC_IMAGE_PATH | Path to image. | `dx-build-output/core-addon/image-processor` |
| DAM_KALTURA_PLUGIN_IMAGE_PATH | Path to image. | `dx-build-output/core-addon/api/dam-plugin-kaltura` |
| REDIS_IMAGE_PATH | Path to image. | `dx-build-output/common/redis` |
| LDAP_IMAGE_PATH | Path to image. | `dx-build-output/core-addon/dx-openldap` |
| RS_IMAGE_PATH | Path to image. | `dx-build-output/core/dxrs` |
| RUNTIME_CONTROLLER_IMAGE_PATH | Path to image. | `dx-build-output/operator/hcldx-runtime-controller` |
| PERSISTENCE_METRICS_EXPORTER_IMAGE_PATH | Path to image. | `dx-build-output/core-addon/persistence/postgres-metrics-exporter` |
| OPENSEARCH_IMAGE_PATH | Path to image. | `dx-build-output/search/dx-opensearch` |
| SEARCH_MIDDLEWARE_IMAGE_PATH | Path to image. | `dx-build-output/search/dx-search-middleware` |

----
## Image and generic repositories

The following parameters are used to determine which image repository to use for pulling images and which generic repository should be used to retrieve the deployment scripts. Both parameters can be overwritten and are mapped to defaults, depending on the setting they are set to.

### Parameters

| Parameter | Description | Default |
|--|--|--|
| IMAGE_REPOSITORY | Image repository name | `quintana-docker.artifactory.cwp.pnp-hcl.com` |
| SCRIPTS_REPOSITORY | Scripts repository identifier | `quintana-generic` |

### Defaults mapping table

| IMAGE_REPOSITORY value | SCRIPTS_REPOSITORY value | Used image repository | Used generic repository |
|--|--|--|--|
| [empty] | [empty] | quintana-docker.artifactory.cwp.pnp-hcl.com | quintana-generic |
| quintana-docker | [empty] | quintana-docker.artifactory.cwp.pnp-hcl.com | quintana-generic |
| quintana-docker-prod | [empty] | quintana-docker-prod.artifactory.cwp.pnp-hcl.com | quintana-generic-prod |
| openshift | [empty] | 657641368736.dkr.ecr.us-east-2.amazonaws.com | quintana-generic |
| [custom-image-repo] | [empty] | value of [custom-image-repo] | quintana-generic |
| [empty] | [custom-script-repo] | quintana-docker.artifactory.cwp.pnp-hcl.com | value of [custom-script-repo] |
| quintana-docker | [custom-script-repo] | quintana-docker.artifactory.cwp.pnp-hcl.com | value of [custom-script-repo] |
| quintana-docker-prod | [custom-script-repo] | quintana-docker-prod.artifactory.cwp.pnp-hcl.com | value of [custom-script-repo] |
| openshift | [custom-script-repo] | 657641368736.dkr.ecr.us-east-2.amazonaws.com | value of [custom-script-repo] |
| openshiftnjdc | [empty] | quintana-docker.artifactory.cwp.pnp-hcl.com | quintana-docker |
| [custom-image-repo] | [custom-script-repo] | value of [custom-image-repo] | value of [custom-script-repo] |

## Kube flavour selection

By default, the configuration will determine the kube flavour to deploy based on a prefix in the `JOB_NAME` of the Jenkins Job.

### Prefix mapping

| Job prefix | Used flavour |
|--|--|
|[unknown]| native |
| kube_native_ | native |
| openshift_ | openshift |

### Override
The flavour can be selected manually by setting a parameter `KUBE_FLAVOUR` to a fitting value.
Currently available are `native` and `openshift`.

## Enable LDAP & DB Config

| Parameter | Description | Default |
|--|--|--|
| ENABLE_DB_CONFIG | enable checkbox if Configure DataBase to DB2/ORACLE  | false|
| DB_HOST |if (COMMON_ENABLE_DB_CONFIG == true) Provide DB Host ('oracle', 'db2') |  |
| DB_TYPE | if (COMMON_ENABLE_DB_CONFIG == true) select ['oracle', 'db2'] | Select the database. |
| ENABLE_LDAP_CONFIG | enable Checkbox enable LDAP Configurations | false|
| LDAP_CONFIG_HOST | if (COMMON_ENABLE_LDAP_CONFIG == true) : LDAP host | `dx-deployment-service-openldap` |
| LDAP_CONFIG_PORT | if (COMMON_ENABLE_LDAP_CONFIG == true): LDAP port | `1389` |
| LDAP_CONFIG_BIND_DN | if (COMMON_ENABLE_LDAP_CONFIG == true): LDAP server bind user DN | `dx_user,dc=dx,dc=com` |
| LDAP_CONFIG_BIND_PASSWORD | if (COMMON_ENABLE_LDAP_CONFIG == true): LDAP server bind user password | (password for Open LDAP dx_user) |
| LDAP_CONFIG_SERVER_TYPE | if (COMMON_ENABLE_LDAP_CONFIG == true): LDAP server type (e.g. IDS) | `custom` |
| LDAP_CONFIG_BASE_ENTRY | if (COMMON_ENABLE_LDAP_CONFIG == true): LDAP server base entry for users | `ou=users,dc=dx,dc=com` |
| DX_USERNAME | if (COMMON_ENABLE_DB_CONFIG == true) or if (COMMON_ENABLE_LDAP_CONFIG == true) : DX Portal profile username | wpsadmin |
| DX_PASSWORD | if (COMMON_ENABLE_DB_CONFIG == true) or if (COMMON_ENABLE_LDAP_CONFIG == true) : DX Portal profile password | wpsadmin |

Note: if ENABLE_DB_CONFIG is true, DB_TYPE is 'db2' and DB_HOST is empty then a Jenkins job will be called to create a new DB2 server to use.
## Kubernetes environment
| Parameter | Description | Default |
|--|--|--|
|KUBE_VERSION|Kube version (for native environments only, `latest` is allowed, only versions 1.21 and above are supported) |`1.21.13`|

## OpenSearch environment
| Parameter | Description | Default |
|--|--|--|
|ENABLE_OPENSEARCH|Specify if OpenSearch should be deployed and enabled |`false`|
|USE_OPENSOURCE_OPENSEARCH|Specify if opensearch.org container should be used rather than the HCL container |`false`|
|OPENSEARCH_VERSION|OpenSearch version (only if USE_OPENSOURCE_OPENSEARCH = true, for native environments only, only versions 2.8.0 and above are supported) | |
|CUSTOM_SEARCH_ADMIN_USER|Search middleware custom admin username | searchadmin |
|CUSTOM_SEARCH_ADMIN_PASSWORD|Search middleware custom admin password | adminsearch |
|CUSTOM_SEARCH_ADMIN_USER|Search middleware custom priviledged push username | pushadmin |
|CUSTOM_SEARCH_ADMIN_PASSWORD|Search middleware custom priviledged push user password | adminpush |

## Metrics deployment
| Parameter | Description | Default |
|--|--|--|
|ENABLE_METRICS|Specify if metrics should be deployed and enabled for all supported apps |`true`|

## Next job functionality

The following parameters allow the specification of a follow-on job that will be scheduled to run after a configurable delay. It is anticipated that this functionality will be used most often to run a "destroy" job a certain number of hours after a "create" job.

### Parameters

| Parameter | Description | Default |
|--|--|--|
| NEXT_JOB_DELAY_HOURS | Number of hours before running (float value) | 0 |
| NEXT_JOB_NAME | Path to job to run in Jenkins (e.g. `CI/kube-deploy/native-kube-remove`) | `` |
| NEXT_JOB_PARAM_LIST | Comma-separated list of parameter names to pass to next job | `` |
| JENKINS_URL | URL to Jenkins server, should be supplied automatically by Jenkins | Current Jenkins server |
| JENKINS_API_CREDENTIALS_ID | ID of (user name / API key) credentials in the Jenkins store used to connect back to Jenkins | `GrahamHarperJenkins` |

In order for a job to run, at a minimum NEXT_JOB_DELAY_HOURS must be greater than zero and NEXT_JOB_NAME must specify a valid job.

When the listed parameters are passed to the new job their values will be the values of the similarly-named parameters of the original job.
Note: do not specify a parameter that the next job does not accept or Jenkins will reject the scheduling request.

## Hybrid

The following parameters specify whether and how to perform a hybrid deployment rather full kubernetes.

| Parameter | Description | Default |
|--|--|--|
| HYBRID | Perform a hybrid deployment? | false |
| HYBRID_HOST | FQDN of on-premise core server (or it's HTTP server, load balancer etc.) to which to connect | blank |
| HYBRID_PORT | Port on which to connect to on-premise server (HTTPS) | 443 |
| ARTIFACTORY_TRUSTSTOREURL | URL to get TLS certificates from | `https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-truststore` |

## Kube Helm

The following parameters relate to the new helm-based deployment:

| Parameter | Description | Default |
|--|--|--|
| HELM_CHARTS_FILTER | Filter for selecting a set of charts - can be a full tar name or a fragment thereof, will get the latest match. | `develop` |
| HELM_CHARTS_PATH | Path to the charts in the artifactory area. | `hcl-dx-deployment` |
| HELM_CHARTS_AREA | Artifactory area from which to retrieve charts. | `quintana-helm` |
| AUTHORING_ENVIRONMENT | Tune the environment for authoring or not | `true` |
