# AWS housekeeping

## AWS EC2 instance cleanup
This logic is supposed to be a safety net for proper cleanup. Test automation etc should not purely rely on this housekeeping job and therefore should implement their own cleanup at the end of the pipeline if appropriate.

The housekeeping excludes all instances which are either tagged with AREA=SUPPORT or termFlag=N. The usage of these should be rare. Most instances (including demo environments) should be volatile.

The housekeeping job runs every **24h on workdays** and executes the following tasks. Please be aware that the interval could be adjusted in the future. Any adjustment will also be communicated in the [DevOps Community](https://teams.microsoft.com/l/channel/19%3a40d4da38dfd94be9ad4803bf15a37384%40thread.skype/Community%2520-%2520DevOps?groupId=8a6712b0-0629-4fbb-9e35-641ae6c7f577&tenantId=189de737-c93a-4f5a-8b68-6f4ca9941912).
1. Terminate all instances with termFlag=Y
2. Find all instances which are not AREA=SUPPORT or termFlag=N
3. Iterate through the instances and check:
  - If no expire date exists: set expire date in **48h**
  - If an expire date is in the past: set termFlag=Y so it will be removed with the next cycle
4. Report in the DevOps Community:
  - Instances which got terminated
  - Instances which got their termFlag set to Y
  - Instances which are close (24h) to their expire date
  - Instances which had no expire date and got the default value
  - Instances which have an expired date and termFlag=N to indicate broken housekeeping
  - Each line will contain the name, Id, owner and expire date in a human readable format

If you need an instance longer than anticipated you need to adjust the expire date or termFlag. The expire date is set as a unix timestamp in milliseconds to prevent any parsing errors. Since the expire date is usually set via code this should not cause any issues. If you need to convert unix timestamps manually you can use this [website](https://currentmillis.com/).

*note: Step 1 is currently executed and monitored by Brian Anderson. Step 2-4 operate currently in a dry-run mode to prevent accidental deletions.*

## Get AWS EC2 instances
This pipeline is to list all the aws ec2 instances created by a specific user in the specific region.
### Job inputs (parameters)
|Parameter|Setting|Description|
|--|--|--|
|USER_EMAIL|default: empty|Enter the user email to list all the aws instances belongs to that user|
|REGION|default: us-east-1|Enter the region to list the aws instances present in that region|
