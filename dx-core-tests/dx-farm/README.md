# DX Farming test pipeline

This pipeline creates three on-prem DX Core instances with the latest CF build as well as V95 and then creates a farm environment from them.

# Steps

1.  Creates three new EC2 instances of the latest on-prem CF V95 build
        Farm1 will be used as the DB2 server
        Farm2 will be the main farm node as well as the HTTP Server
        Farm3 will be the secondary farm node

2.  On each of the 3 instances, modify the hosts file so that they can communicate

3.  On Farm2, install and configure IHS, the required JDK and the web server plugin
        Use the ConfigEngine task to point to the DB2 server that lives on Farm1
        Set up Farm 2 as the main farm mode

4.  On Farm3, run ConfigEngine task to point to the DB2 server that lives on Farm1
        Then, set up Farm3 as the secondary farm node

5.  Once the farm is completed, it can be accessed at:
        `http://(farm2 IP address or FQDN)/wps/portal`

6.  When running the pipeline, there is a checkbox called `RUN_UAT`.  Select this checkbox to run the WTF UAT (this is the default behavior) and deselect it if you desire a clean DX farm environment for development purposes.

# Jenkins Pipeline

https://portal-jenkins-test.cwp.pnp-hcl.com/job/CI/job/dx-core-tests/job/dx-farm/


This pipeline requires several parameters to run:
1.  `DXBuildNumber`
        This is inherited from the upstream job CI/dx-core-tests/dx-update-cf-v95
        
2.  `RESOURCES_TTL`
       The default length of time (in hours) for how long the AWS resources created by the pipeline will exist before being terminated
       
3.  `CF_VERSION`
        The CF version that is deployed and tested against.  Also  used for labeling the test results that are reported to the dashboard
        
4.  `RUN_UAT`
        Check this box to run a UAT on the farm environment once it's complete.  
        Uncheck to skip the UAT.
        