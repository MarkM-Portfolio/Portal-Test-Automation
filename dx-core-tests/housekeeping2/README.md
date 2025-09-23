# DX Core Tests - Housekeeping

This Job takes care of removing obsolete AWS Resources like EC2 instances, AMIs and Route53 dns entries.  
The original housekeeping job fails if any more stages are added.  This additional job allows for 
additional stages, and, therefore, more cleanup.