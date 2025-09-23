# Master Packaging Manifest

## Introduction

This Jenkins pipeline is used to push the master packaging manifest to the Artifactory.

## Available parameters

| Environment variable | Description | Default
| -- | -- | -- |
| `MASTER_DEPLOYMENT_LEVEL` | Master deployment level, eg: 0=N, 1=N-1, 2=N-2 | 0
| `IMAGE_REPOSITORY` | Image repository to pull the master builds from artifactory | quintana-docker-prod
| `CORE_IMAGE_FILTER` | Core image filter | master
| `DAM_IMAGE_FILTER` | Media library image filter  | master
| `CC_IMAGE_FILTER` | Content UI image filter  | master
| `RINGAPI_IMAGE_FILTER` | RingApi image filter | master
| `IMGPROC_IMAGE_FILTER` | Image Processor image filter | master
| `PERSISTENCE_IMAGE_FILTER` | Persistence image filter | master
| `LDAP_IMAGE_FILTER` | Open Ldap image filter  | master
| `RS_IMAGE_FILTER` | Remote search image filter | master
| `DESIGN_STUDIO_IMAGE_FILTER` | Design studio image filter | master
| `RUNTIME_CONTROLLER_IMAGE_FILTER` | Runtime controller image filter | master
| `HELM_CHARTS_FILTER` | HELM chart filter  | master
| `PERSISTENCE_NODE_IMAGE_FILTER` | Persistence node image filter | master
| `PERSISTENCE_CONNECTION_POOL_IMAGE_FILTER` | Persistence connection pool image filter | master
| `PERSISTENCE_METRICS_EXPORTER_IMAGE_FILTER` | Persistence metrics exporter image filter | master
| `SIDECAR_IMAGE_FILTER` | Sidecar image filter | master
| `HAPROXY_IMAGE_FILTER` | HAPROXY image filter | master
| `DAM_PLUGIN_GOOGLE_VISION_IMAGE_FILTER` | DAM plugin google vision image filter | master
| `DAM_KALTURA_PLUGIN_IMAGE_FILTER` | DAM kaltura plugin image filter | master
| `LICENSE_MANAGER_IMAGE_FILTER` | Licence manager image filter | master

