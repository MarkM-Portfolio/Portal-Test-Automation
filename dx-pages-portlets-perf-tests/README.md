# DX Pages and Portles performance Automation Tests

This pipeline will be used to automate the DX Pages and Portles performance Automation Tests

- Using DXClient in this job to 
    1. deploy JSF sample portlet 
    2. to import 8 pages
    3. to set permissions to search center and JSF portlets for both anonymous and authenticated portal users
- Import WCM and Script app libraries to native-kube instance using configEngine task 'import-wcm-data'
- Above two steps are successfully executed, we can see total 8 pages with portlets in the deployment passed in parameters.