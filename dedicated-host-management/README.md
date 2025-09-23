# Dedicated Host Management

This automation can be used to create EC2 Dedicated Hosts in AWS.
Those can then be used for later creation of EC2 instances.

## Parameters

Please see the `parameters.yaml` file for a list of available parameters and their default values.

## Usage

Configure the parameters as required, ensure that the availability zone you choose is matching your desired target subnet for the EC2 instances.

|Subnet Name|Subnet ID|Description|Availability Zone|
| -- | -- | -- | -- |
| Dev01 | subnet-02a350a23b3e39a43 | Subnet for development work instances | us-east-1a |
| Dev02 | subnet-033035ecf3a0e7ff4 | Subnet for development work instances | us-east-1c |
| Build01 | subnet-014047f30974086c8 | Subnet for build instances | us-east-1a |
| Build02 | subnet-00153ed57f803609e | Subnet for build instances | us-east-1b |
| Support01 | subnet-0d8701e99946ea834 | Subnet for support instances | us-east-1a |
| Support02 | subnet-07df4340bd57769e3 | Subnet for support instances | us-east-1a |
| Infra01 | subnet-0c5bb98f368105470 | Subnet for infra instances | us-east-1a |
| Infra02 | subnet-0e374a4862791225d | Subnet for infra instances | us-east-1b |