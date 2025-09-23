# dx-update-cf Job

## Description

This pipeline is based on the dx-update-cf pipeline.  The readme for that can be found [here](../dx-update-cf/README.md).

This pipeline starts with a Portal 8.5 GM installation with non-default settings and performs the CF upgrade to the configured CF on that and then saves it as an AMI.

**Portal install location:**
`/opt/test/WebSphere9/test_profile`

**WAS install location:**
`/opt/test/WebSphere9/AppServer`

**Portal URL:**
`http://EC2_instance_IP:20027/my/alternate`

**WAS URL:**
`http://EC2_instance_IP:20026/ibm/console`

**Portal/WAS admin ID:**
`portaladmin`

**Portal/WAS admin PW:**
`portaladmin`


