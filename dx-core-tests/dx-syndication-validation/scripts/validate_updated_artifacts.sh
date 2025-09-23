#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2022. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

set -e

if [ XXX$1 != "XXX" ]; then
    SYNDICATOR_IP=$1
    SUBSCRIBER_IP=$2
    echo "Syndicator IP is : "$SYNDICATOR_IP
    echo "Subscriber IP is : "$SUBSCRIBER_IP
fi

# Log in to subscriber
echo "POST Login to subscriber"
curl --cookie-jar subscriberCookie.txt -s -o subscriberResponse.txt -w "%{http_code}" --location --request POST "http://${SUBSCRIBER_IP}:10039/wps/portal/cxml/04_SD9ePMtCP1I800I_KydQvyHFUBADPmuQy?userid=wpsadmin&password=wpsadmin" \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=0000zaNvk6pv3D3I16jDXVbB_2U:-1; LtpaToken2=dXQEQSyxCz/mURMDYxFSJYGwggXxyCYUquNTfDzJVCP2IUv4q157nKvaxdXCIMoVBvH1BiBujmWeRXYaFva0GTtRvTozr2OeYiVubRNrWThvzn8kvxrvbJvUmxjjSmDT7LveIQ4IrTtyKoI5xzlU7xw/ijAak3UM/hN9gr+W4tkh8RGsIKOL25ubROMk1LmnVRbpV6nYiOCDxtHhl5JKh7pC7LDplzQBsT9GOGoMxXkkvMgktGi+RFgS5Yp/KfFQ+uO2JP4jOAMAXkK1fxt+YFhJfzM84zICbtqtUaudkK4s+gwDUob/iq9N2sn6nfa2O/lTfKcmoVqmoHfMfHDNWOW1qECmvE/lEvzQmrJmYqC2Jw8nxLb44Wrsubg7Q5I2B5aAb3UmjzQI6+wh+8Ezn9A85Hfl+BsHhFoeGT52Ehj5czOS6tDCBPyZ2n+hNmWmQYlPAk/f3s94kPM/Zs+woLjCQ8x/DnvVZSupaMqlqHEGTpHGifM8yTY9PH3k1/xtq9N1eokR+p94SH3Po4SKtT/NbTzyHTtahzH2ITeJAOB06OUfQ/Lb0RrH0zarze931kSmPI+t3GbP4sN9z/jotL/HCuTw96oq4l0+PAqGjPE4kqJyMKt8b4rEzmBB4F5d1wm1Ben1FI8gIQ6UWcsAnMfoqdhH3iDJdWmwHsr86880Roum+5qVfrcX+LXjE/LQ4asKLapc51sotJsXl9+oxg==' \
--data '{
	"username":"wpsadmin",
	"password":"wpsadmin"
}'

echo "Verifying if a renamed content item is succesfully syndicated"
curl -b subscriberCookie.txt -s  -o UpdatedContent.txt -w "%{http_code}" --location --request GET "http://${SUBSCRIBER_IP}:10039/wps/mycontenthandler/wcmrest/Content/bb3725d3-3886-4ed0-9f03-510ceafd087e" \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=000045dibNHXRabRpPXK09BMLJN:-1; LtpaToken2=wjWfRG0zynIzFQiEqPwnaq36NOq8tiDi2qVIUgcoHUTrsgbcq2lw+2VuV+OOOHUGmyVEGNl2L0uRw3MKZIOsGPOcWeiXxxX+4CNXZ58pdJh07aUG4/SYAOymBMM97CJZXJR6SaIBAkgh9q4w8fD2sk7inPyYZzoNvH8o+4/HYdWJOaWYZTc41+4iiq0evxbWNJwH0hc84y4yX4bcWuTIpiB9QugwpQaQ/JSiz1Y4xf/mrOxwwpKzQ9pawLUzuRrmlQzHjn7dstkn6oc8UY6eiow32EbVXp9z3t6CveKVcVZYJSzHEMJq6fzYOqFNwnTcn+J3gFFwPCsYs9O37VZV3+6dhRm4tlK6bcqMdbiWAKREr6xdOpIjRAf5NQzEh5osBTw4NOTZVyPxFvMGaNEL8a2BoTRjq0P5/GUFujFcNW1ytzAFU7i6kEHkbq577ecqwbltF1UkQTZtWC/wTstjFigR8ndbn6vKP9DXelknfTVyi6+VkzjGGSHb5CRPmTiDrlU9TbVgBSU35x6+wrq4rCV+BjDT427Foechnrc7hpFBzZEImEEM+CI4x6oLNG8No4USJajkmv4PvWe81I3sYi5H9r4plHljRe5j0r6ihWUhV8Lu1kAZXNwJ7lal0m77cP/5O5qZsO2gyPp2wowTRxjOWRBEltYCi0i+PljwE90=' \
--data '{}' 

cat UpdatedContent.txt | jq -e 'if .entry.title.value == "Updated Soap Title" then "Updated content item title is successfully syndicated" else error("Failed to syndicate updated content item title") end'
cat UpdatedContent.txt | jq -e 'if .entry.name == "Updated Soap Name" then "Updated content item name is successfully syndicated" else error("Failed to syndicate updated content item name") end'


echo "Verifying if a content item that was moved to a different site area is successfully syndicated"
curl -b subscriberCookie.txt -s  -o MovedContent.txt -w "%{http_code}" --location --request GET "http://${SUBSCRIBER_IP}:10039/wps/mycontenthandler/wcmrest/Content/3be09f77-2ffb-4541-803e-8b5c3f4e3f3a" \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=000045dibNHXRabRpPXK09BMLJN:-1; LtpaToken2=wjWfRG0zynIzFQiEqPwnaq36NOq8tiDi2qVIUgcoHUTrsgbcq2lw+2VuV+OOOHUGmyVEGNl2L0uRw3MKZIOsGPOcWeiXxxX+4CNXZ58pdJh07aUG4/SYAOymBMM97CJZXJR6SaIBAkgh9q4w8fD2sk7inPyYZzoNvH8o+4/HYdWJOaWYZTc41+4iiq0evxbWNJwH0hc84y4yX4bcWuTIpiB9QugwpQaQ/JSiz1Y4xf/mrOxwwpKzQ9pawLUzuRrmlQzHjn7dstkn6oc8UY6eiow32EbVXp9z3t6CveKVcVZYJSzHEMJq6fzYOqFNwnTcn+J3gFFwPCsYs9O37VZV3+6dhRm4tlK6bcqMdbiWAKREr6xdOpIjRAf5NQzEh5osBTw4NOTZVyPxFvMGaNEL8a2BoTRjq0P5/GUFujFcNW1ytzAFU7i6kEHkbq577ecqwbltF1UkQTZtWC/wTstjFigR8ndbn6vKP9DXelknfTVyi6+VkzjGGSHb5CRPmTiDrlU9TbVgBSU35x6+wrq4rCV+BjDT427Foechnrc7hpFBzZEImEEM+CI4x6oLNG8No4USJajkmv4PvWe81I3sYi5H9r4plHljRe5j0r6ihWUhV8Lu1kAZXNwJ7lal0m77cP/5O5qZsO2gyPp2wowTRxjOWRBEltYCi0i+PljwE90=' \
--data '{}' 

# OLD METHOD 
# cat MovedContent.txt | jq -e 'if [.entry.link[10].href|endswith("wcmrest/SiteArea/a80c0ff0-feb2-4253-8652-8ec7f4635dec")] then "Moved content item is successfully syndicated" else error("Failed to syndicate moved content item") end'
# NEW METHOD

if grep "wcmrest/SiteArea/a80c0ff0-feb2-4253-8652-8ec7f4635dec" MovedContent.txt;
then echo "Moved content item is successfully syndicated" 
else echo "Failed to syndicate moved content item" 
exit 1 
fi
cat MovedContent.txt | jq -e 'if .entry.title.value == "Tourinng Kayak Moved" then "Moved content item title is successfully syndicated" else error("Failed to syndicate moved content item title") end'


echo "Verifying if a deleted content item (Long Boards) is succesfully syndicated"
curl -b subscriberCookie.txt -s  -o DeletedContent.txt -w "%{http_code}" --location --request GET "http://${SUBSCRIBER_IP}:10039/wps/mycontenthandler/wcmrest/Content/7552b375-8ea6-4304-9606-6bfc6cca202f" \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=000045dibNHXRabRpPXK09BMLJN:-1; LtpaToken2=wjWfRG0zynIzFQiEqPwnaq36NOq8tiDi2qVIUgcoHUTrsgbcq2lw+2VuV+OOOHUGmyVEGNl2L0uRw3MKZIOsGPOcWeiXxxX+4CNXZ58pdJh07aUG4/SYAOymBMM97CJZXJR6SaIBAkgh9q4w8fD2sk7inPyYZzoNvH8o+4/HYdWJOaWYZTc41+4iiq0evxbWNJwH0hc84y4yX4bcWuTIpiB9QugwpQaQ/JSiz1Y4xf/mrOxwwpKzQ9pawLUzuRrmlQzHjn7dstkn6oc8UY6eiow32EbVXp9z3t6CveKVcVZYJSzHEMJq6fzYOqFNwnTcn+J3gFFwPCsYs9O37VZV3+6dhRm4tlK6bcqMdbiWAKREr6xdOpIjRAf5NQzEh5osBTw4NOTZVyPxFvMGaNEL8a2BoTRjq0P5/GUFujFcNW1ytzAFU7i6kEHkbq577ecqwbltF1UkQTZtWC/wTstjFigR8ndbn6vKP9DXelknfTVyi6+VkzjGGSHb5CRPmTiDrlU9TbVgBSU35x6+wrq4rCV+BjDT427Foechnrc7hpFBzZEImEEM+CI4x6oLNG8No4USJajkmv4PvWe81I3sYi5H9r4plHljRe5j0r6ihWUhV8Lu1kAZXNwJ7lal0m77cP/5O5qZsO2gyPp2wowTRxjOWRBEltYCi0i+PljwE90=' \
--data '{}' 

# OLD METHOD 
# If the deleted content item syndicated correctly, DeletedContent.txt should be empty.  If not, syndication failed.
# if [ ! -s DeletedContent.txt ]; then echo “Deleted item syndicated correctly“;else echo “Deleted item did not syndicate correctly”;fi
# NEW METHOD
# If the deleted content item syndicated, then the Subscriber will return that item was not found - output file will not be empty but rather contain the message that Item Not Found
cat DeletedContent.txt | jq -e 'if .errors.message[0].code == "ITEM_NOT_FOUND_1" then "Deleted item syndicated correctly and was not found on Subscriber" else error("Deleted item did not syndicate correctly") end'

echo "Verifying that updated default content of a site area successfully syndicated"
curl -b subscriberCookie.txt -s  -o UpdatedSiteArea.txt -w "%{http_code}" --location --request GET "http://${SUBSCRIBER_IP}:10039/wps/mycontenthandler/wcmrest/SiteArea/384830c0-4706-436d-afa1-8d725658361a" \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=000045dibNHXRabRpPXK09BMLJN:-1; LtpaToken2=wjWfRG0zynIzFQiEqPwnaq36NOq8tiDi2qVIUgcoHUTrsgbcq2lw+2VuV+OOOHUGmyVEGNl2L0uRw3MKZIOsGPOcWeiXxxX+4CNXZ58pdJh07aUG4/SYAOymBMM97CJZXJR6SaIBAkgh9q4w8fD2sk7inPyYZzoNvH8o+4/HYdWJOaWYZTc41+4iiq0evxbWNJwH0hc84y4yX4bcWuTIpiB9QugwpQaQ/JSiz1Y4xf/mrOxwwpKzQ9pawLUzuRrmlQzHjn7dstkn6oc8UY6eiow32EbVXp9z3t6CveKVcVZYJSzHEMJq6fzYOqFNwnTcn+J3gFFwPCsYs9O37VZV3+6dhRm4tlK6bcqMdbiWAKREr6xdOpIjRAf5NQzEh5osBTw4NOTZVyPxFvMGaNEL8a2BoTRjq0P5/GUFujFcNW1ytzAFU7i6kEHkbq577ecqwbltF1UkQTZtWC/wTstjFigR8ndbn6vKP9DXelknfTVyi6+VkzjGGSHb5CRPmTiDrlU9TbVgBSU35x6+wrq4rCV+BjDT427Foechnrc7hpFBzZEImEEM+CI4x6oLNG8No4USJajkmv4PvWe81I3sYi5H9r4plHljRe5j0r6ihWUhV8Lu1kAZXNwJ7lal0m77cP/5O5qZsO2gyPp2wowTRxjOWRBEltYCi0i+PljwE90=' \
--data '{}' 

# OLD METHOD
# cat UpdatedSiteArea.txt | jq -e 'if .entry.link[4].href == "/wps/mycontenthandler/!ut/p/digest!5d4JDXp7pplOKpQvF1ibcA/wcmrest/Content/6ac87ef8-de68-4df7-af6e-7b897d9159ac" then "Updated default content of a site area successfully synciated" else error("Failed to syndicate updated default content of a site area") end'
# NEW METHOD

if grep "wcmrest/Content/6ac87ef8-de68-4df7-af6e-7b897d9159ac" UpdatedSiteArea.txt;
then echo "Updated default content of a site area successfully syndicated" 
else echo "Failed to syndicate updated default content of a site area" 
exit 1 
fi



echo "Verifying that a copy of a content item successfully syndicated"
curl -b subscriberCookie.txt -s  -o FunBoardCopy.txt -w "%{http_code}" --location --request GET "http://${SUBSCRIBER_IP}:10039/wps/mycontenthandler/wcmrest/query?name=FunBoardCopy&mime-type=application/json" \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=000045dibNHXRabRpPXK09BMLJN:-1; LtpaToken2=wjWfRG0zynIzFQiEqPwnaq36NOq8tiDi2qVIUgcoHUTrsgbcq2lw+2VuV+OOOHUGmyVEGNl2L0uRw3MKZIOsGPOcWeiXxxX+4CNXZ58pdJh07aUG4/SYAOymBMM97CJZXJR6SaIBAkgh9q4w8fD2sk7inPyYZzoNvH8o+4/HYdWJOaWYZTc41+4iiq0evxbWNJwH0hc84y4yX4bcWuTIpiB9QugwpQaQ/JSiz1Y4xf/mrOxwwpKzQ9pawLUzuRrmlQzHjn7dstkn6oc8UY6eiow32EbVXp9z3t6CveKVcVZYJSzHEMJq6fzYOqFNwnTcn+J3gFFwPCsYs9O37VZV3+6dhRm4tlK6bcqMdbiWAKREr6xdOpIjRAf5NQzEh5osBTw4NOTZVyPxFvMGaNEL8a2BoTRjq0P5/GUFujFcNW1ytzAFU7i6kEHkbq577ecqwbltF1UkQTZtWC/wTstjFigR8ndbn6vKP9DXelknfTVyi6+VkzjGGSHb5CRPmTiDrlU9TbVgBSU35x6+wrq4rCV+BjDT427Foechnrc7hpFBzZEImEEM+CI4x6oLNG8No4USJajkmv4PvWe81I3sYi5H9r4plHljRe5j0r6ihWUhV8Lu1kAZXNwJ7lal0m77cP/5O5qZsO2gyPp2wowTRxjOWRBEltYCi0i+PljwE90=' \
--data '{}' 

# OLD METHOD
# cat FunBoardCopy.txt | jq -e 'if .feed.entry[0].title.value == "FunBoardCopy" then "Copied content item is successfully syndicated" else error("Failed to syndicate copied content item") end'
# NEW METHOD
if grep "FunBoardCopy" FunBoardCopy.txt;
then echo "Copied content item is successfully syndicated" 
else echo "Failed to syndicate copied content item" 
exit 1 
fi

# echo "Verifying if an updated DxContentPage is successfully syndicated"
# curl -b subscriberCookie.txt  -o UpdatedContentPage.txt -w "%{http_code}" --location --request GET "http://${SUBSCRIBER_IP}:10039/wps/mycontenthandler/wcmrest-v2/content-pages/dd9af8c2-2136-4fb0-a9c1-9734466f0cc4" \
# --header 'Authorization: Basic d3BzYWRtaW46d3BzYWRtaW4=' \
# --header 'Content-Type: application/atom+xml' \
# --data '{}'

# cat UpdatedContentPage.txt | jq -e 'if .title.value == "HomeUpdated_Test" then "Updated DxContentPage Title is successfully syndicated" else error("Failed to syndicate updated DxContentPage Title") end'
# cat UpdatedContentPage.txt | jq -e 'if .name == "HomeUpdated_Test" then "Updated DxContentPage Name is successfully syndicated" else error("Failed to syndicate updated DxContentPage Name") end'

# echo "Verifying if updated DxSymbol is successfully syndicated"
# curl -b subscriberCookie.txt  -o UpdatedDxSymbol.txt -w "%{http_code}" --location --request GET "http://${SUBSCRIBER_IP}:10039/wps/mycontenthandler/wcmrest-v2/containers/686d81b7-95b7-4f28-abda-ab754b58633f" \
# --header 'Authorization: Basic d3BzYWRtaW46d3BzYWRtaW4=' \
# --header 'Content-Type: application/atom+xml' \
# --data '{}'

# cat UpdatedDxSymbol.txt | jq -e 'if .title.value == "Designer - Amanda Smithsen container updated" then "Updated DxSymbol Title is successfully syndicated" else error("Failed to syndicate updated DxSymbol Title") end'
# cat UpdatedDxSymbol.txt | jq -e 'if .name == "Designer - Amanda Smithsen container updated" then "Updated DxSymbol Name is successfully syndicated" else error("Failed to syndicate updated DxSymbol Name") end'