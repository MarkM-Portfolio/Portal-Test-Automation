# Native kubernetes configuration parameters

The parameters below are specific to deployments on native kube and additional to the general parameters for all kube flavours.
## For creation

| Parameter | Description | Default |
|--|--|--|
| AWS_CREDENTIALS_ID | ID of the entry in the Jenkins credential store with credentials to access AWS | `aws_credentials` |
| INSTANCE_TYPE | Type of EC2 instance to create | `c5.4xlarge` |
| INSTANCE_NAME | Name of the EC2 instance (and entry in Route 53) to create | `native-kube` |
| DX_CORE_REPLICAS | Number of replicas of DX Core pod to create | `1` |
| DB2_PASSWORD | Password with which to connect to DB2 instance | `diet4coke` |
| DB2_IMAGE_PATH | Path in docker registry to DB2 image to use | `dx-db2` |
| DB2_IMAGE_TAG | Tag in docker registry for DB2 image to use | `v11.5` |
| IS_NJDC_DEPLOYMENT | Set to true if native-kube deployment is in NJDC | `false` |
| NJDC_INSTANCE_IP | Instance IP of NJDC to deploy native-kube | `` |

## For destruction

| Parameter | Description | Default |
|--|--|--|
| AWS_CREDENTIALS_ID | ID of the entry in the Jenkins credential store with credentials to access AWS | `aws_credentials` |
| INSTANCE_NAME | Name of the EC2 instance (and entry in Route 53) to remove | `native-kube` |
| IS_NJDC_DEPLOYMENT | Set to true if native-kube deployment was in NJDC | `false` |
| NJDC_INSTANCE_IP | Instance IP of NJDC to remove and cleanup native-kube | `` |

## For update

| Parameter | Description | Default |
|--|--|--|
| INSTANCE_NAME | Name of the EC2 instance to update | `native-kube` |

## For using Harbor 

| Parameter | Description | Default |
|--|--|--|
| IMAGE_REPOSITORY | Name of the Repository | `harbor` |
| HARBOR_PROJECT | Choose among the projects to fetch and deploy images and helmcharts from harbor  | `dx-staging` |
| HARBOR_IMAGES_FILTER | For Harbor images filter please specify the YYYYMMDD date format Ex: `20220429`  |  |
| HELM_CHARTS_FILTER | For Harbor Please specify the latest available version of helmchart Ex: `2.6.12`  |  |
