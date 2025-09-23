#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2023. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

if [ XXX$1 != "XXX" ]; then
    SYNDICATOR_IP=$1
    SUBSCRIBER_IP=$2
    SYNDICATE_ITEMS=$3
    SYNDICATE_LIBRARIES=$4
    echo "Syndicator IP is : "$SYNDICATOR_IP
    echo "Subscriber IP is : "$SUBSCRIBER_IP
    echo "SYNDICATE_ITEMS is : "$SYNDICATE_ITEMS
    echo "SYNDICATE_LIBRARIES is : "$SYNDICATE_LIBRARIES
fi

# Install jq
echo "Installing jq"
sudo yum install epel-release -y
sudo yum install jq -y
jq -Version

# Enable content sites
# echo "enabling content sites"
# cd /opt/IBM/WebSphere/wp_profile/ConfigEngine
# ./ConfigEngine.sh enable-content-sites -DWasPassword=wpsadmin -DPortalAdminPwd=wpsadmin

# Start Portal manually due to commenting out the enablement of content site above
echo "Starting Portal so it is available"
/opt/IBM/WebSphere/wp_profile/bin/startServer.sh WebSphere_Portal
/opt/IBM/WebSphere/wp_profile/bin/serverStatus.sh -all -username wpsadmin -password wpsadmin

# Copy XML to create credential vault slot to the correct spot and create the vault slot
echo "creating a credential vault slot"
cp /opt/zips/CreateVaultSlot.xml /opt/IBM/WebSphere/wp_profile/PortalServer/bin/
cd /opt/IBM/WebSphere/wp_profile/PortalServer/bin
./xmlaccess.sh -in CreateVaultSlot.xml -out slot-out.xml -url http://${SUBSCRIBER_IP}:10039/wps/config -user wpsadmin -password wpsadmin

sleep 20

# Replace placeholder value in WCM Config Service with actual value so that the subscriber has the correct URL
echo "updating WCM Config Service with the correct subscriber URL"
cd /opt/IBM/WebSphere/wp_profile/PortalServer/wcm/shared/app/config/wcmservices
cp WCMConfigService.properties WCMConfigService.properties_backup
sed -i "$ a deployment.subscriberUrl=http://${SUBSCRIBER_IP}:10039/wps/wcm/connect/?MOD=Subs" WCMConfigService.properties
cat WCMConfigService.properties | grep deployment.subscriberUrl

# Run the config task to update the properties in the Resource Provider
echo "running the update-wcm-service-properties task"
cd /opt/IBM/WebSphere/wp_profile/ConfigEngine
./ConfigEngine.sh update-wcm-service-properties -DPortalAdminPwd=wpsadmin -DWasUserid=wpsadmin -DWasPassword=wpsadmin

# Restart Portal for the changes to take effect
echo "Restarting Portal"
cd /opt/IBM/WebSphere/wp_profile/bin
./stopServer.sh WebSphere_Portal -username wpsadmin -password wpsadmin
./startServer.sh WebSphere_Portal

sleep 20

# Login to the syndicator
echo "POST Login"
curl --cookie-jar cookie.txt -s -o response.txt -w "%{http_code}" --location --request POST "http://${SYNDICATOR_IP}:10039/wps/portal/cxml/04_SD9ePMtCP1I800I_KydQvyHFUBADPmuQy?userid=wpsadmin&password=wpsadmin" \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=0000zaNvk6pv3D3I16jDXVbB_2U:-1; LtpaToken2=dXQEQSyxCz/mURMDYxFSJYGwggXxyCYUquNTfDzJVCP2IUv4q157nKvaxdXCIMoVBvH1BiBujmWeRXYaFva0GTtRvTozr2OeYiVubRNrWThvzn8kvxrvbJvUmxjjSmDT7LveIQ4IrTtyKoI5xzlU7xw/ijAak3UM/hN9gr+W4tkh8RGsIKOL25ubROMk1LmnVRbpV6nYiOCDxtHhl5JKh7pC7LDplzQBsT9GOGoMxXkkvMgktGi+RFgS5Yp/KfFQ+uO2JP4jOAMAXkK1fxt+YFhJfzM84zICbtqtUaudkK4s+gwDUob/iq9N2sn6nfa2O/lTfKcmoVqmoHfMfHDNWOW1qECmvE/lEvzQmrJmYqC2Jw8nxLb44Wrsubg7Q5I2B5aAb3UmjzQI6+wh+8Ezn9A85Hfl+BsHhFoeGT52Ehj5czOS6tDCBPyZ2n+hNmWmQYlPAk/f3s94kPM/Zs+woLjCQ8x/DnvVZSupaMqlqHEGTpHGifM8yTY9PH3k1/xtq9N1eokR+p94SH3Po4SKtT/NbTzyHTtahzH2ITeJAOB06OUfQ/Lb0RrH0zarze931kSmPI+t3GbP4sN9z/jotL/HCuTw96oq4l0+PAqGjPE4kqJyMKt8b4rEzmBB4F5d1wm1Ben1FI8gIQ6UWcsAnMfoqdhH3iDJdWmwHsr86880Roum+5qVfrcX+LXjE/LQ4asKLapc51sotJsXl9+oxg==' \
--data '{
	"username":"wpsadmin",
	"password":"wpsadmin"
}'

# Subscribe to syndicator
echo "Setting Syndication between the two portal instances"
cd /opt/IBM/WebSphere/wp_profile/ConfigEngine/
./ConfigEngine.sh run-wcm-admin-task-subscribe-now -Dsyndicator=http://${SYNDICATOR_IP}:10039/wps/wcm -DvaultSlotName=syndication-slot -DsyndicatorName=syndicator1 -DsubscriberName=subscriber1 -DsubscriberURL=http://${SUBSCRIBER_IP}:10039/wps/wcm/connect ${SYNDICATE_ITEMS}="${SYNDICATE_LIBRARIES}" -DPortalAdminPwd=wpsadmin -DWasPassword=wpsadmin

echo "Verifying Syndication status between two portal instances"
curl -b cookie.txt -s  -o Syndicators.txt -w "%{http_code}" --location --request GET "http://${SYNDICATOR_IP}:10039/wps/mycontenthandler/!ut/p/wcmrest/Syndication/Syndicators" \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=000045dibNHXRabRpPXK09BMLJN:-1; LtpaToken2=wjWfRG0zynIzFQiEqPwnaq36NOq8tiDi2qVIUgcoHUTrsgbcq2lw+2VuV+OOOHUGmyVEGNl2L0uRw3MKZIOsGPOcWeiXxxX+4CNXZ58pdJh07aUG4/SYAOymBMM97CJZXJR6SaIBAkgh9q4w8fD2sk7inPyYZzoNvH8o+4/HYdWJOaWYZTc41+4iiq0evxbWNJwH0hc84y4yX4bcWuTIpiB9QugwpQaQ/JSiz1Y4xf/mrOxwwpKzQ9pawLUzuRrmlQzHjn7dstkn6oc8UY6eiow32EbVXp9z3t6CveKVcVZYJSzHEMJq6fzYOqFNwnTcn+J3gFFwPCsYs9O37VZV3+6dhRm4tlK6bcqMdbiWAKREr6xdOpIjRAf5NQzEh5osBTw4NOTZVyPxFvMGaNEL8a2BoTRjq0P5/GUFujFcNW1ytzAFU7i6kEHkbq577ecqwbltF1UkQTZtWC/wTstjFigR8ndbn6vKP9DXelknfTVyi6+VkzjGGSHb5CRPmTiDrlU9TbVgBSU35x6+wrq4rCV+BjDT427Foechnrc7hpFBzZEImEEM+CI4x6oLNG8No4USJajkmv4PvWe81I3sYi5H9r4plHljRe5j0r6ihWUhV8Lu1kAZXNwJ7lal0m77cP/5O5qZsO2gyPp2wowTRxjOWRBEltYCi0i+PljwE90=' \
--data '{}'

echo "Getting the UUID of the Syndicator"
SyndicatorUUID=`cat Syndicators.txt | jq '.entry.content.Syndicators.SyndicatorList[0].SyndicatorUUID'`
echo "Syndication Library UUID is $SyndicatorUUID"

sleep 30
