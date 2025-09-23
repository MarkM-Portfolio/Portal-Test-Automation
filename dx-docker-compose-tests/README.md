## DX Docker compose Tests Pipeline
### Description
This pipeline is designed to verify if the DX docker-compose is up and running, and to ensure that all services are functioning as expected.

### Workflow of the Pipeline

#### Load modules and configuration
Load modules and configuration from the different flavours using "load"

#### Create EC2 Instance
Creating an EC2 instance which will be used for deploying the docker-compose.

### Prepare EC2 instance
After a successful creation of the EC2 instance, we install all required software on it and make sure that our settings will be copied over to the target machine.

### Run docker compose
The next step after installing the prerequisites is to run the docker-compose.

### Verify all services are up and running
Used verify all services are up and running

### Declarative: Post Actions
Destroy the environment.