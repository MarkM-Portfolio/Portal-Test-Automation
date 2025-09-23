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

# Log in to syndicator
echo "POST Login to syndicator"
curl --cookie-jar cookie.txt -s -o response.txt -w "%{http_code}" --location --request POST "http://${SYNDICATOR_IP}:10039/wps/portal/cxml/04_SD9ePMtCP1I800I_KydQvyHFUBADPmuQy?userid=wpsadmin&password=wpsadmin" \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=0000zaNvk6pv3D3I16jDXVbB_2U:-1; LtpaToken2=dXQEQSyxCz/mURMDYxFSJYGwggXxyCYUquNTfDzJVCP2IUv4q157nKvaxdXCIMoVBvH1BiBujmWeRXYaFva0GTtRvTozr2OeYiVubRNrWThvzn8kvxrvbJvUmxjjSmDT7LveIQ4IrTtyKoI5xzlU7xw/ijAak3UM/hN9gr+W4tkh8RGsIKOL25ubROMk1LmnVRbpV6nYiOCDxtHhl5JKh7pC7LDplzQBsT9GOGoMxXkkvMgktGi+RFgS5Yp/KfFQ+uO2JP4jOAMAXkK1fxt+YFhJfzM84zICbtqtUaudkK4s+gwDUob/iq9N2sn6nfa2O/lTfKcmoVqmoHfMfHDNWOW1qECmvE/lEvzQmrJmYqC2Jw8nxLb44Wrsubg7Q5I2B5aAb3UmjzQI6+wh+8Ezn9A85Hfl+BsHhFoeGT52Ehj5czOS6tDCBPyZ2n+hNmWmQYlPAk/f3s94kPM/Zs+woLjCQ8x/DnvVZSupaMqlqHEGTpHGifM8yTY9PH3k1/xtq9N1eokR+p94SH3Po4SKtT/NbTzyHTtahzH2ITeJAOB06OUfQ/Lb0RrH0zarze931kSmPI+t3GbP4sN9z/jotL/HCuTw96oq4l0+PAqGjPE4kqJyMKt8b4rEzmBB4F5d1wm1Ben1FI8gIQ6UWcsAnMfoqdhH3iDJdWmwHsr86880Roum+5qVfrcX+LXjE/LQ4asKLapc51sotJsXl9+oxg==' \
--data '{
	"username":"wpsadmin",
	"password":"wpsadmin"
}'

sleep 10

# Log in to subscriber
echo "POST Login to subscriber"
curl --cookie-jar subscriberCookie.txt -s -o subscriberResponse.txt -w "%{http_code}" --location --request POST "http://${SUBSCRIBER_IP}:10039/wps/portal/cxml/04_SD9ePMtCP1I800I_KydQvyHFUBADPmuQy?userid=wpsadmin&password=wpsadmin" \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=0000zaNvk6pv3D3I16jDXVbB_2U:-1; LtpaToken2=dXQEQSyxCz/mURMDYxFSJYGwggXxyCYUquNTfDzJVCP2IUv4q157nKvaxdXCIMoVBvH1BiBujmWeRXYaFva0GTtRvTozr2OeYiVubRNrWThvzn8kvxrvbJvUmxjjSmDT7LveIQ4IrTtyKoI5xzlU7xw/ijAak3UM/hN9gr+W4tkh8RGsIKOL25ubROMk1LmnVRbpV6nYiOCDxtHhl5JKh7pC7LDplzQBsT9GOGoMxXkkvMgktGi+RFgS5Yp/KfFQ+uO2JP4jOAMAXkK1fxt+YFhJfzM84zICbtqtUaudkK4s+gwDUob/iq9N2sn6nfa2O/lTfKcmoVqmoHfMfHDNWOW1qECmvE/lEvzQmrJmYqC2Jw8nxLb44Wrsubg7Q5I2B5aAb3UmjzQI6+wh+8Ezn9A85Hfl+BsHhFoeGT52Ehj5czOS6tDCBPyZ2n+hNmWmQYlPAk/f3s94kPM/Zs+woLjCQ8x/DnvVZSupaMqlqHEGTpHGifM8yTY9PH3k1/xtq9N1eokR+p94SH3Po4SKtT/NbTzyHTtahzH2ITeJAOB06OUfQ/Lb0RrH0zarze931kSmPI+t3GbP4sN9z/jotL/HCuTw96oq4l0+PAqGjPE4kqJyMKt8b4rEzmBB4F5d1wm1Ben1FI8gIQ6UWcsAnMfoqdhH3iDJdWmwHsr86880Roum+5qVfrcX+LXjE/LQ4asKLapc51sotJsXl9+oxg==' \
--data '{
	"username":"wpsadmin",
	"password":"wpsadmin"
}'

# Get the ID of the previously created syndication libraries
echo "getting the ID of the syndication library"
curl -b cookie.txt -s  -o SyndicationLibrary.txt -w "%{http_code}" --location --request GET "http://${SYNDICATOR_IP}:10039/wps/mycontenthandler/wcmrest-v2/libraries" \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=000045dibNHXRabRpPXK09BMLJN:-1; LtpaToken2=wjWfRG0zynIzFQiEqPwnaq36NOq8tiDi2qVIUgcoHUTrsgbcq2lw+2VuV+OOOHUGmyVEGNl2L0uRw3MKZIOsGPOcWeiXxxX+4CNXZ58pdJh07aUG4/SYAOymBMM97CJZXJR6SaIBAkgh9q4w8fD2sk7inPyYZzoNvH8o+4/HYdWJOaWYZTc41+4iiq0evxbWNJwH0hc84y4yX4bcWuTIpiB9QugwpQaQ/JSiz1Y4xf/mrOxwwpKzQ9pawLUzuRrmlQzHjn7dstkn6oc8UY6eiow32EbVXp9z3t6CveKVcVZYJSzHEMJq6fzYOqFNwnTcn+J3gFFwPCsYs9O37VZV3+6dhRm4tlK6bcqMdbiWAKREr6xdOpIjRAf5NQzEh5osBTw4NOTZVyPxFvMGaNEL8a2BoTRjq0P5/GUFujFcNW1ytzAFU7i6kEHkbq577ecqwbltF1UkQTZtWC/wTstjFigR8ndbn6vKP9DXelknfTVyi6+VkzjGGSHb5CRPmTiDrlU9TbVgBSU35x6+wrq4rCV+BjDT427Foechnrc7hpFBzZEImEEM+CI4x6oLNG8No4USJajkmv4PvWe81I3sYi5H9r4plHljRe5j0r6ihWUhV8Lu1kAZXNwJ7lal0m77cP/5O5qZsO2gyPp2wowTRxjOWRBEltYCi0i+PljwE90=' \
--data '{}'
SyndicationLibraries=`cat SyndicationLibrary.txt | jq '.["library-entries"]'`
SyndicationLibraryA_ID=`echo ${SyndicationLibraries} | jq '.[] | select( .name == "syndication_librarya" ).id' | tr -d '"'`
SyndicationLibraryB_ID=`echo ${SyndicationLibraries} | jq '.[] | select( .name == "syndication_libraryb" ).id' | tr -d '"'`
SyndicationLibrary=`echo ${SyndicationLibraries} | jq '.[] | select( .name == "syndicationlibrary" ).id'`
SurfCityLibrary=`echo ${SyndicationLibraries} | jq '.[] | select( .name == "surfcity-surfshop" ).id'`

echo "syndication_librarya id is ${SyndicationLibraryA_ID}"
echo "syndication_libraryb id is ${SyndicationLibraryB_ID}"
echo "SyndicationLibrary id is ${SyndicationLibrary}"
echo "SurfCityLibrary id is ${SurfCityLibrary}"

echo "Verifying if Menu components is succesfully syndicated from library SyndicationLibraryA"
curl -b subscriberCookie.txt -s  -o ValidateMenuA.txt -w "%{http_code}" --location --request GET "http://${SUBSCRIBER_IP}:10039/wps/mycontenthandler/wcmrest-v2/component/menus?libraryID=${SyndicationLibraryA_ID}" \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=000045dibNHXRabRpPXK09BMLJN:-1; LtpaToken2=wjWfRG0zynIzFQiEqPwnaq36NOq8tiDi2qVIUgcoHUTrsgbcq2lw+2VuV+OOOHUGmyVEGNl2L0uRw3MKZIOsGPOcWeiXxxX+4CNXZ58pdJh07aUG4/SYAOymBMM97CJZXJR6SaIBAkgh9q4w8fD2sk7inPyYZzoNvH8o+4/HYdWJOaWYZTc41+4iiq0evxbWNJwH0hc84y4yX4bcWuTIpiB9QugwpQaQ/JSiz1Y4xf/mrOxwwpKzQ9pawLUzuRrmlQzHjn7dstkn6oc8UY6eiow32EbVXp9z3t6CveKVcVZYJSzHEMJq6fzYOqFNwnTcn+J3gFFwPCsYs9O37VZV3+6dhRm4tlK6bcqMdbiWAKREr6xdOpIjRAf5NQzEh5osBTw4NOTZVyPxFvMGaNEL8a2BoTRjq0P5/GUFujFcNW1ytzAFU7i6kEHkbq577ecqwbltF1UkQTZtWC/wTstjFigR8ndbn6vKP9DXelknfTVyi6+VkzjGGSHb5CRPmTiDrlU9TbVgBSU35x6+wrq4rCV+BjDT427Foechnrc7hpFBzZEImEEM+CI4x6oLNG8No4USJajkmv4PvWe81I3sYi5H9r4plHljRe5j0r6ihWUhV8Lu1kAZXNwJ7lal0m77cP/5O5qZsO2gyPp2wowTRxjOWRBEltYCi0i+PljwE90=' \
--data '{}'

cat ValidateMenuA.txt | jq -e 'if .total >= "1" then "Articles list menu component is successfully syndicated from SyndicationLibraryA" else error("Failed to syndicate Articles list menu component from SyndicationLibraryA") end'

echo "Verifying if Menu components is succesfully syndicated from library SyndicationLibraryB"
curl -b subscriberCookie.txt -s  -o ValidateMenuB.txt -w "%{http_code} " --location --request GET "http://${SUBSCRIBER_IP}:10039/wps/mycontenthandler/wcmrest-v2/component/menus?libraryID=${SyndicationLibraryB_ID}" \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=000045dibNHXRabRpPXK09BMLJN:-1; LtpaToken2=wjWfRG0zynIzFQiEqPwnaq36NOq8tiDi2qVIUgcoHUTrsgbcq2lw+2VuV+OOOHUGmyVEGNl2L0uRw3MKZIOsGPOcWeiXxxX+4CNXZ58pdJh07aUG4/SYAOymBMM97CJZXJR6SaIBAkgh9q4w8fD2sk7inPyYZzoNvH8o+4/HYdWJOaWYZTc41+4iiq0evxbWNJwH0hc84y4yX4bcWuTIpiB9QugwpQaQ/JSiz1Y4xf/mrOxwwpKzQ9pawLUzuRrmlQzHjn7dstkn6oc8UY6eiow32EbVXp9z3t6CveKVcVZYJSzHEMJq6fzYOqFNwnTcn+J3gFFwPCsYs9O37VZV3+6dhRm4tlK6bcqMdbiWAKREr6xdOpIjRAf5NQzEh5osBTw4NOTZVyPxFvMGaNEL8a2BoTRjq0P5/GUFujFcNW1ytzAFU7i6kEHkbq577ecqwbltF1UkQTZtWC/wTstjFigR8ndbn6vKP9DXelknfTVyi6+VkzjGGSHb5CRPmTiDrlU9TbVgBSU35x6+wrq4rCV+BjDT427Foechnrc7hpFBzZEImEEM+CI4x6oLNG8No4USJajkmv4PvWe81I3sYi5H9r4plHljRe5j0r6ihWUhV8Lu1kAZXNwJ7lal0m77cP/5O5qZsO2gyPp2wowTRxjOWRBEltYCi0i+PljwE90=' \
--data '{}'

cat ValidateMenuB.txt | jq -e 'if .total >= "1" then "Articles list menu component is successfully syndicated from SyndicationLibraryB" else error("Failed to syndicate Articles list menu component from SyndicationLibraryB") end'

echo "Verifying if page is syndicated and can be previewed from library SyndicationLibraryB"
curl -b subscriberCookie.txt -s  -o ValidateSyndication_Content_B1.txt -w "%{http_code}" --location --request GET "http://${SUBSCRIBER_IP}:10039/wps/wcm/myconnect/syndication_libraryb/articles/syndicate_content_b1" \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=000045dibNHXRabRpPXK09BMLJN:-1; LtpaToken2=wjWfRG0zynIzFQiEqPwnaq36NOq8tiDi2qVIUgcoHUTrsgbcq2lw+2VuV+OOOHUGmyVEGNl2L0uRw3MKZIOsGPOcWeiXxxX+4CNXZ58pdJh07aUG4/SYAOymBMM97CJZXJR6SaIBAkgh9q4w8fD2sk7inPyYZzoNvH8o+4/HYdWJOaWYZTc41+4iiq0evxbWNJwH0hc84y4yX4bcWuTIpiB9QugwpQaQ/JSiz1Y4xf/mrOxwwpKzQ9pawLUzuRrmlQzHjn7dstkn6oc8UY6eiow32EbVXp9z3t6CveKVcVZYJSzHEMJq6fzYOqFNwnTcn+J3gFFwPCsYs9O37VZV3+6dhRm4tlK6bcqMdbiWAKREr6xdOpIjRAf5NQzEh5osBTw4NOTZVyPxFvMGaNEL8a2BoTRjq0P5/GUFujFcNW1ytzAFU7i6kEHkbq577ecqwbltF1UkQTZtWC/wTstjFigR8ndbn6vKP9DXelknfTVyi6+VkzjGGSHb5CRPmTiDrlU9TbVgBSU35x6+wrq4rCV+BjDT427Foechnrc7hpFBzZEImEEM+CI4x6oLNG8No4USJajkmv4PvWe81I3sYi5H9r4plHljRe5j0r6ihWUhV8Lu1kAZXNwJ7lal0m77cP/5O5qZsO2gyPp2wowTRxjOWRBEltYCi0i+PljwE90=' \
--data '{}'

if [ "$(grep -c "Sample Article 2" ValidateSyndication_Content_B1.txt)" -ge 1 ];
  then
    echo "Syndication_Content_B1 is successfully syndicated"
  else
    echo "Failed to Syndication_Content_B1 or preview, see ValidateSyndication_Content_B1.txt for errors"
    exit 201
fi

echo "Verifying if page is syndicated and can preview from library SyndicationLibraryB"
curl -b subscriberCookie.txt -s  -o ValidateSyndicate_Content_A3.txt -w "%{http_code}" --location --request GET "http://${SUBSCRIBER_IP}:10039/wps/wcm/myconnect/syndication_librarya/articles/syndicate_content_a3" \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=000045dibNHXRabRpPXK09BMLJN:-1; LtpaToken2=wjWfRG0zynIzFQiEqPwnaq36NOq8tiDi2qVIUgcoHUTrsgbcq2lw+2VuV+OOOHUGmyVEGNl2L0uRw3MKZIOsGPOcWeiXxxX+4CNXZ58pdJh07aUG4/SYAOymBMM97CJZXJR6SaIBAkgh9q4w8fD2sk7inPyYZzoNvH8o+4/HYdWJOaWYZTc41+4iiq0evxbWNJwH0hc84y4yX4bcWuTIpiB9QugwpQaQ/JSiz1Y4xf/mrOxwwpKzQ9pawLUzuRrmlQzHjn7dstkn6oc8UY6eiow32EbVXp9z3t6CveKVcVZYJSzHEMJq6fzYOqFNwnTcn+J3gFFwPCsYs9O37VZV3+6dhRm4tlK6bcqMdbiWAKREr6xdOpIjRAf5NQzEh5osBTw4NOTZVyPxFvMGaNEL8a2BoTRjq0P5/GUFujFcNW1ytzAFU7i6kEHkbq577ecqwbltF1UkQTZtWC/wTstjFigR8ndbn6vKP9DXelknfTVyi6+VkzjGGSHb5CRPmTiDrlU9TbVgBSU35x6+wrq4rCV+BjDT427Foechnrc7hpFBzZEImEEM+CI4x6oLNG8No4USJajkmv4PvWe81I3sYi5H9r4plHljRe5j0r6ihWUhV8Lu1kAZXNwJ7lal0m77cP/5O5qZsO2gyPp2wowTRxjOWRBEltYCi0i+PljwE90=' \
--data '{}'

if [ "$(grep -c "Syndicate_Content_A3" ValidateSyndicate_Content_A3.txt)" -ge 1 ];
  then
    echo "Syndicate_Content_A3 is successfully syndicated"
  else
    echo "Failed to Syndicate_Content_A3 or preview, see ValidateSyndicate_Content_A3.txt for errors"
    exit 201
fi

# echo "Verifying if Site type is succesfully syndicated"
# curl -b subscriberCookie.txt -s  -o Site.txt -w "%{http_code}" --location --request GET "http://${SUBSCRIBER_IP}:10039/wps/mycontenthandler/"'!'"ut/p/digest"'!'"CWb11PoiCvMXKblug8zpmg/wcmrest-v2/content-sites/$SiteID" \
# --header 'Accept: application/json' \
# --header 'Content-Type: application/json' \
# --header 'Cookie: JSESSIONID=000045dibNHXRabRpPXK09BMLJN:-1; LtpaToken2=wjWfRG0zynIzFQiEqPwnaq36NOq8tiDi2qVIUgcoHUTrsgbcq2lw+2VuV+OOOHUGmyVEGNl2L0uRw3MKZIOsGPOcWeiXxxX+4CNXZ58pdJh07aUG4/SYAOymBMM97CJZXJR6SaIBAkgh9q4w8fD2sk7inPyYZzoNvH8o+4/HYdWJOaWYZTc41+4iiq0evxbWNJwH0hc84y4yX4bcWuTIpiB9QugwpQaQ/JSiz1Y4xf/mrOxwwpKzQ9pawLUzuRrmlQzHjn7dstkn6oc8UY6eiow32EbVXp9z3t6CveKVcVZYJSzHEMJq6fzYOqFNwnTcn+J3gFFwPCsYs9O37VZV3+6dhRm4tlK6bcqMdbiWAKREr6xdOpIjRAf5NQzEh5osBTw4NOTZVyPxFvMGaNEL8a2BoTRjq0P5/GUFujFcNW1ytzAFU7i6kEHkbq577ecqwbltF1UkQTZtWC/wTstjFigR8ndbn6vKP9DXelknfTVyi6+VkzjGGSHb5CRPmTiDrlU9TbVgBSU35x6+wrq4rCV+BjDT427Foechnrc7hpFBzZEImEEM+CI4x6oLNG8No4USJajkmv4PvWe81I3sYi5H9r4plHljRe5j0r6ihWUhV8Lu1kAZXNwJ7lal0m77cP/5O5qZsO2gyPp2wowTRxjOWRBEltYCi0i+PljwE90=' \
# --data '{}' 

# echo "Verifying if Page type is succesfully syndicated"
# curl -b subscriberCookie.txt -s  -o Page.txt -w "%{http_code}" --location --request GET "http://${SUBSCRIBER_IP}:10039/wps/mycontenthandler/!ut/p/wcmrest-v2/search?type=DxContentPage" \
# --header 'Accept: application/json' \
# --header 'Content-Type: application/json' \
# --header 'Cookie: JSESSIONID=000045dibNHXRabRpPXK09BMLJN:-1; LtpaToken2=wjWfRG0zynIzFQiEqPwnaq36NOq8tiDi2qVIUgcoHUTrsgbcq2lw+2VuV+OOOHUGmyVEGNl2L0uRw3MKZIOsGPOcWeiXxxX+4CNXZ58pdJh07aUG4/SYAOymBMM97CJZXJR6SaIBAkgh9q4w8fD2sk7inPyYZzoNvH8o+4/HYdWJOaWYZTc41+4iiq0evxbWNJwH0hc84y4yX4bcWuTIpiB9QugwpQaQ/JSiz1Y4xf/mrOxwwpKzQ9pawLUzuRrmlQzHjn7dstkn6oc8UY6eiow32EbVXp9z3t6CveKVcVZYJSzHEMJq6fzYOqFNwnTcn+J3gFFwPCsYs9O37VZV3+6dhRm4tlK6bcqMdbiWAKREr6xdOpIjRAf5NQzEh5osBTw4NOTZVyPxFvMGaNEL8a2BoTRjq0P5/GUFujFcNW1ytzAFU7i6kEHkbq577ecqwbltF1UkQTZtWC/wTstjFigR8ndbn6vKP9DXelknfTVyi6+VkzjGGSHb5CRPmTiDrlU9TbVgBSU35x6+wrq4rCV+BjDT427Foechnrc7hpFBzZEImEEM+CI4x6oLNG8No4USJajkmv4PvWe81I3sYi5H9r4plHljRe5j0r6ihWUhV8Lu1kAZXNwJ7lal0m77cP/5O5qZsO2gyPp2wowTRxjOWRBEltYCi0i+PljwE90=' \
# --data '{}' 

# cat Page.txt | jq -e 'if .total >= "2" then "DxContentPage type is successfully syndicated" else error("Failed to syndicate DxContentPage") end'

# echo "Verifying if DxSymbol is succesfully syndicated"
# curl -b subscriberCookie.txt -s  -o Symbol.txt -w "%{http_code}" --location --request GET "http://${SUBSCRIBER_IP}:10039/wps/mycontenthandler/!ut/p/wcmrest-v2/search?type=DxSymbol" \
# --header 'Accept: application/json' \
# --header 'Content-Type: application/json' \
# --header 'Cookie: JSESSIONID=000045dibNHXRabRpPXK09BMLJN:-1; LtpaToken2=wjWfRG0zynIzFQiEqPwnaq36NOq8tiDi2qVIUgcoHUTrsgbcq2lw+2VuV+OOOHUGmyVEGNl2L0uRw3MKZIOsGPOcWeiXxxX+4CNXZ58pdJh07aUG4/SYAOymBMM97CJZXJR6SaIBAkgh9q4w8fD2sk7inPyYZzoNvH8o+4/HYdWJOaWYZTc41+4iiq0evxbWNJwH0hc84y4yX4bcWuTIpiB9QugwpQaQ/JSiz1Y4xf/mrOxwwpKzQ9pawLUzuRrmlQzHjn7dstkn6oc8UY6eiow32EbVXp9z3t6CveKVcVZYJSzHEMJq6fzYOqFNwnTcn+J3gFFwPCsYs9O37VZV3+6dhRm4tlK6bcqMdbiWAKREr6xdOpIjRAf5NQzEh5osBTw4NOTZVyPxFvMGaNEL8a2BoTRjq0P5/GUFujFcNW1ytzAFU7i6kEHkbq577ecqwbltF1UkQTZtWC/wTstjFigR8ndbn6vKP9DXelknfTVyi6+VkzjGGSHb5CRPmTiDrlU9TbVgBSU35x6+wrq4rCV+BjDT427Foechnrc7hpFBzZEImEEM+CI4x6oLNG8No4USJajkmv4PvWe81I3sYi5H9r4plHljRe5j0r6ihWUhV8Lu1kAZXNwJ7lal0m77cP/5O5qZsO2gyPp2wowTRxjOWRBEltYCi0i+PljwE90=' \
# --data '{}' 

# cat Symbol.txt | jq -e 'if .total >= "1" then "DxSymbol type is successfully syndicated" else error("Failed to syndicate DxSymbol") end'

# SurfCity Library validation
echo "Verifying if a SurfCity site area is succesfully syndicated"
curl -b subscriberCookie.txt -s  -o SurfCitySiteArea.txt -w "%{http_code}" --location --request GET "http://${SUBSCRIBER_IP}:10039/wps/mycontenthandler/wcmrest/SiteArea/26f805ac-28bd-4859-808d-1a863d9c3490" \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=000045dibNHXRabRpPXK09BMLJN:-1; LtpaToken2=wjWfRG0zynIzFQiEqPwnaq36NOq8tiDi2qVIUgcoHUTrsgbcq2lw+2VuV+OOOHUGmyVEGNl2L0uRw3MKZIOsGPOcWeiXxxX+4CNXZ58pdJh07aUG4/SYAOymBMM97CJZXJR6SaIBAkgh9q4w8fD2sk7inPyYZzoNvH8o+4/HYdWJOaWYZTc41+4iiq0evxbWNJwH0hc84y4yX4bcWuTIpiB9QugwpQaQ/JSiz1Y4xf/mrOxwwpKzQ9pawLUzuRrmlQzHjn7dstkn6oc8UY6eiow32EbVXp9z3t6CveKVcVZYJSzHEMJq6fzYOqFNwnTcn+J3gFFwPCsYs9O37VZV3+6dhRm4tlK6bcqMdbiWAKREr6xdOpIjRAf5NQzEh5osBTw4NOTZVyPxFvMGaNEL8a2BoTRjq0P5/GUFujFcNW1ytzAFU7i6kEHkbq577ecqwbltF1UkQTZtWC/wTstjFigR8ndbn6vKP9DXelknfTVyi6+VkzjGGSHb5CRPmTiDrlU9TbVgBSU35x6+wrq4rCV+BjDT427Foechnrc7hpFBzZEImEEM+CI4x6oLNG8No4USJajkmv4PvWe81I3sYi5H9r4plHljRe5j0r6ihWUhV8Lu1kAZXNwJ7lal0m77cP/5O5qZsO2gyPp2wowTRxjOWRBEltYCi0i+PljwE90=' \
--data '{}' 

cat SurfCitySiteArea.txt | jq -e 'if .entry.title.value == "Surf Shop" then "SurfCity site area is successfully syndicated" else error("Failed to syndicate SurfCity site area") end'

echo "Verifying if a SurfCity content item is succesfully syndicated"
curl -b subscriberCookie.txt -s  -o SurfCityContent.txt -w "%{http_code}" --location --request GET "http://${SUBSCRIBER_IP}:10039/wps/mycontenthandler/wcmrest/Content/8764ba0c-c7ad-4f17-9d33-e56c92653f1d" \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=000045dibNHXRabRpPXK09BMLJN:-1; LtpaToken2=wjWfRG0zynIzFQiEqPwnaq36NOq8tiDi2qVIUgcoHUTrsgbcq2lw+2VuV+OOOHUGmyVEGNl2L0uRw3MKZIOsGPOcWeiXxxX+4CNXZ58pdJh07aUG4/SYAOymBMM97CJZXJR6SaIBAkgh9q4w8fD2sk7inPyYZzoNvH8o+4/HYdWJOaWYZTc41+4iiq0evxbWNJwH0hc84y4yX4bcWuTIpiB9QugwpQaQ/JSiz1Y4xf/mrOxwwpKzQ9pawLUzuRrmlQzHjn7dstkn6oc8UY6eiow32EbVXp9z3t6CveKVcVZYJSzHEMJq6fzYOqFNwnTcn+J3gFFwPCsYs9O37VZV3+6dhRm4tlK6bcqMdbiWAKREr6xdOpIjRAf5NQzEh5osBTw4NOTZVyPxFvMGaNEL8a2BoTRjq0P5/GUFujFcNW1ytzAFU7i6kEHkbq577ecqwbltF1UkQTZtWC/wTstjFigR8ndbn6vKP9DXelknfTVyi6+VkzjGGSHb5CRPmTiDrlU9TbVgBSU35x6+wrq4rCV+BjDT427Foechnrc7hpFBzZEImEEM+CI4x6oLNG8No4USJajkmv4PvWe81I3sYi5H9r4plHljRe5j0r6ihWUhV8Lu1kAZXNwJ7lal0m77cP/5O5qZsO2gyPp2wowTRxjOWRBEltYCi0i+PljwE90=' \
--data '{}' 

cat SurfCityContent.txt | jq -e 'if .entry.title.value == "VOODOO CHILD" then "SurfCity content item is successfully syndicated" else error("Failed to syndicate SurfCity content item") end'

echo "Verifying if a SurfCity navigator component is succesfully syndicated"
curl -b subscriberCookie.txt -s  -o SurfCityNavigatorComponent.txt -w "%{http_code}" --location --request GET "http://${SUBSCRIBER_IP}:10039/wps/mycontenthandler/wcmrest/LibraryNavigatorComponent/c0bc03b7-44b0-4f4d-a3eb-1f5c203d2a22" \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=000045dibNHXRabRpPXK09BMLJN:-1; LtpaToken2=wjWfRG0zynIzFQiEqPwnaq36NOq8tiDi2qVIUgcoHUTrsgbcq2lw+2VuV+OOOHUGmyVEGNl2L0uRw3MKZIOsGPOcWeiXxxX+4CNXZ58pdJh07aUG4/SYAOymBMM97CJZXJR6SaIBAkgh9q4w8fD2sk7inPyYZzoNvH8o+4/HYdWJOaWYZTc41+4iiq0evxbWNJwH0hc84y4yX4bcWuTIpiB9QugwpQaQ/JSiz1Y4xf/mrOxwwpKzQ9pawLUzuRrmlQzHjn7dstkn6oc8UY6eiow32EbVXp9z3t6CveKVcVZYJSzHEMJq6fzYOqFNwnTcn+J3gFFwPCsYs9O37VZV3+6dhRm4tlK6bcqMdbiWAKREr6xdOpIjRAf5NQzEh5osBTw4NOTZVyPxFvMGaNEL8a2BoTRjq0P5/GUFujFcNW1ytzAFU7i6kEHkbq577ecqwbltF1UkQTZtWC/wTstjFigR8ndbn6vKP9DXelknfTVyi6+VkzjGGSHb5CRPmTiDrlU9TbVgBSU35x6+wrq4rCV+BjDT427Foechnrc7hpFBzZEImEEM+CI4x6oLNG8No4USJajkmv4PvWe81I3sYi5H9r4plHljRe5j0r6ihWUhV8Lu1kAZXNwJ7lal0m77cP/5O5qZsO2gyPp2wowTRxjOWRBEltYCi0i+PljwE90=' \
--data '{}' 

cat SurfCityNavigatorComponent.txt | jq -e 'if .entry.title.value == "left-navigator" then "SurfCity navigator component is successfully syndicated" else error("Failed to syndicate SurfCity navigator component") end'

echo "Verifying if a SurfCity category is succesfully syndicated"
curl -b subscriberCookie.txt -s  -o SurfCityCategory.txt -w "%{http_code}" --location --request GET "http://${SUBSCRIBER_IP}:10039/wps/mycontenthandler/wcmrest/Category/5d28fbfe-c310-4a51-b0d8-fc15b2c9c92b" \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=000045dibNHXRabRpPXK09BMLJN:-1; LtpaToken2=wjWfRG0zynIzFQiEqPwnaq36NOq8tiDi2qVIUgcoHUTrsgbcq2lw+2VuV+OOOHUGmyVEGNl2L0uRw3MKZIOsGPOcWeiXxxX+4CNXZ58pdJh07aUG4/SYAOymBMM97CJZXJR6SaIBAkgh9q4w8fD2sk7inPyYZzoNvH8o+4/HYdWJOaWYZTc41+4iiq0evxbWNJwH0hc84y4yX4bcWuTIpiB9QugwpQaQ/JSiz1Y4xf/mrOxwwpKzQ9pawLUzuRrmlQzHjn7dstkn6oc8UY6eiow32EbVXp9z3t6CveKVcVZYJSzHEMJq6fzYOqFNwnTcn+J3gFFwPCsYs9O37VZV3+6dhRm4tlK6bcqMdbiWAKREr6xdOpIjRAf5NQzEh5osBTw4NOTZVyPxFvMGaNEL8a2BoTRjq0P5/GUFujFcNW1ytzAFU7i6kEHkbq577ecqwbltF1UkQTZtWC/wTstjFigR8ndbn6vKP9DXelknfTVyi6+VkzjGGSHb5CRPmTiDrlU9TbVgBSU35x6+wrq4rCV+BjDT427Foechnrc7hpFBzZEImEEM+CI4x6oLNG8No4USJajkmv4PvWe81I3sYi5H9r4plHljRe5j0r6ihWUhV8Lu1kAZXNwJ7lal0m77cP/5O5qZsO2gyPp2wowTRxjOWRBEltYCi0i+PljwE90=' \
--data '{}' 

cat SurfCityCategory.txt | jq -e 'if .entry.title.value == "Retail" then "SurfCity category is successfully syndicated" else error("Failed to syndicate SurfCity category") end'

echo "Verifying if Draft Content is successfully syndicated"
curl -b subscriberCookie.txt -s  -o DraftContent.txt -w "%{http_code}" --location --request GET "http://${SUBSCRIBER_IP}:10039/wps/mycontenthandler/!ut/p/wcmrest-v2/search?type=Content&name=SyndicationDraftContent" \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=000045dibNHXRabRpPXK09BMLJN:-1; LtpaToken2=wjWfRG0zynIzFQiEqPwnaq36NOq8tiDi2qVIUgcoHUTrsgbcq2lw+2VuV+OOOHUGmyVEGNl2L0uRw3MKZIOsGPOcWeiXxxX+4CNXZ58pdJh07aUG4/SYAOymBMM97CJZXJR6SaIBAkgh9q4w8fD2sk7inPyYZzoNvH8o+4/HYdWJOaWYZTc41+4iiq0evxbWNJwH0hc84y4yX4bcWuTIpiB9QugwpQaQ/JSiz1Y4xf/mrOxwwpKzQ9pawLUzuRrmlQzHjn7dstkn6oc8UY6eiow32EbVXp9z3t6CveKVcVZYJSzHEMJq6fzYOqFNwnTcn+J3gFFwPCsYs9O37VZV3+6dhRm4tlK6bcqMdbiWAKREr6xdOpIjRAf5NQzEh5osBTw4NOTZVyPxFvMGaNEL8a2BoTRjq0P5/GUFujFcNW1ytzAFU7i6kEHkbq577ecqwbltF1UkQTZtWC/wTstjFigR8ndbn6vKP9DXelknfTVyi6+VkzjGGSHb5CRPmTiDrlU9TbVgBSU35x6+wrq4rCV+BjDT427Foechnrc7hpFBzZEImEEM+CI4x6oLNG8No4USJajkmv4PvWe81I3sYi5H9r4plHljRe5j0r6ihWUhV8Lu1kAZXNwJ7lal0m77cP/5O5qZsO2gyPp2wowTRxjOWRBEltYCi0i+PljwE90=' \
--data '{}' 

cat DraftContent.txt | jq -e 'if .total >= "1" then "DraftContent is successfully syndicated" else error("Failed to syndicate DraftContent") end'


echo "Verifying if SyndicationProject is successfully syndicated"
curl -b subscriberCookie.txt  -o Project.txt -w "%{http_code}" --location --request GET "http://${SUBSCRIBER_IP}:10039/wps/mycontenthandler/!ut/p/wcmrest-v2/search?type=Project&name=SyndicationProject" \
--header 'Authorization: Basic d3BzYWRtaW46d3BzYWRtaW4=' \
--header 'Content-Type: application/atom+xml' \
--data '{}'

cat Project.txt | jq -e 'if .total >= "1" then "SyndicationProject is successfully syndicated" else error("Failed to syndicate SyndicationProject") end'
