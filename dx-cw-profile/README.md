## CW Profile Tests Pipeline
### Description
This pipeline is used to run the CW Profile testcases.

### Workflow of the Pipeline

#### Prepare Environment
Used to load parameters from parameters.yaml

#### Deploying the application
Used to deploy the application via native-kube-next-deploy Job

#### Updating User Registry
Run to verify upgrading to a newer version containing the cw_profile persistence changes then the file registry of core is copied.

#### Backward Compatibility
Run to verify upgrading from an older version and the wpsadmin password is unchanged, the cw_profile and the config wizard user registry contain the default password.

#### Symbolic link created to container
Run to verify cw_profile symbolic link created to container.

#### cw_profile folder to be persisted into a persistent volume even after restart of core pods
Native kube is deployed with 2 core pods enabling cw-profile persistence volume and run the acceptance test pipeline for dxclient, the acceptance test cases pass

#### Concurrency validation check
Native kube is deployed with 2 core pods enabling cw-profile persistence volume and run the acceptance test pipeline for dxclient, the acceptance test cases pass