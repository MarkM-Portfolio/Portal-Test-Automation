# dx-enable-url-locales Job

## Description

This pipeline spins up a new instance of an AMI with the latest DX CF level.
After that, it runs the ConfigEngine task to enable friendly locale URLs for a few specified languages.

./ConfigEngine.sh enable-friendly-locale-urls -Dfriendly-locale-list="de,en,es,nb,no,nn,zh-cn,zh-hk,zh-sg,zh-tw"

After friendly locale URLs are enabled, a new AMI is created and the usual route 53 DNS routing is added.



