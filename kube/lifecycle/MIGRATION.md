# Migration test automation

## Introduction

This pipeline can be used to perform an Operator to Helm migration in [GKE, AKS, Openshift].  
It will use an existing Operator deployment and will perform a side by side migration.  
After the migration is performed both deployments will be up and running.

## Procedure

The automation follows the customer documentation that is available [here](https://pages.git.cwp.pnp-hcl.com/Team-Q/documentation/docs/containerization/helm/migration/migration_core).

It will back up both DAM and Core contents, copy over to the Helm deployment and restore the data.

## Parameters

These parameters are all specific to the migration pipeline use, others may be inherited from the regular "kube-deploy" pipeline.

| Parameter | Description | Example |
| -- | -- | -- |
| NAMESPACE | Namespace of the DXCTL deployment that should be picked for migration | pmi-migration-test |
| KUBE_FLAVOUR | Flavour in which the DXCTL deployment exists | google |
| IMAGE_REPOSITORY | Repository used for images during the Helm install | google |
| CLUSTER_NAME | Cluster used e.g. in google | dx-jenkins-cluster |
