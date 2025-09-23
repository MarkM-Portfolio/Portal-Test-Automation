# Route 53 housekeeping

## Description

This automation is used to identify orphaned Route53 records and delete them.

It will try to find EC2 instances that have Records IP address either as a private or public IP.

If no instance has been found, the record is considered orphaned and can be deleted.

## Parameters

|Parameter|Description|default|
|--|--|--|
|AWS_ZONE_ID|ID of AWS Route53 zone.|Z3OEC7SLEHQ2P3 - team-q-dev.com|
|DELETE_ENTRIES|Determines if orphaned records will be deleted.|false|
|SEND_TEAMS|Determines if a teams notification should be send.|true|
|WEBHOOK_URL|Url used to send teams notification to.|<https://hclo365.webhook.office.com/webhookb2/8a6712b0-0629-4fbb-9e35-641ae6c7f577@189de737-c93a-4f5a-8b68-6f4ca9941912/IncomingWebhook/306939d536234a119edcd66ad98f6cd9/02dcf8e7-5b47-496e-91d0-fc764f400ea0>|