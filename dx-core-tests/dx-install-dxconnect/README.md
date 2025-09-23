# dx-install-dxconnect Job

## Description

This pipeline spins up a new instance of an AMI with the latest DX CF level.
After that, it performs the re-install task of dxconnect and then uses a wsadmin command to ensure that
the dxconnect application was properly deployed and is running in WAS.

After dxconnect is installed, a new AMI is created and the usual route 53 DNS routing is added.



