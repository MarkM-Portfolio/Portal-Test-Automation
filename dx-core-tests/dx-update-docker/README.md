# DX Docker update test pipeline

This pipeline executes a customized DX Docker update test.

# Steps

1. Creates an EC2 instance from an AMI that has 
    - Docker installed
    - Docker service starting on reboot
    - The core image of CF195 docker installed into Docker using a virtualized disk space of /opt/HCL/wp_profile
    - The core image set to a non-default context root of /this/works (as opposed to wps/portal)
    - The PortalAdminPwd and WASPassword updated to 'password' 
2. Pull the latest docker image and update the image above to it.
3. Logs in and makes sure this is successful.

# Jenkins Pipeline

https://portal-jenkins-test.cwp.pnp-hcl.com/job/DXCore_Tests/job/dx-update-docker/