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
SyndicationLibrary=`echo ${SyndicationLibraries} | jq '.[] | select( .name == "syndicationlibrary" ).id' | tr -d '"'`

echo "syndication_librarya id is ${SyndicationLibraryA_ID}"
echo "syndication_libraryb id is ${SyndicationLibraryB_ID}"
echo "SyndicationLibrary id is ${SyndicationLibrary}"

# Verify syndication status
echo "Verifying Syndication status between two portal instances"
curl -b cookie.txt -s  -o Syndicators.txt -w "%{http_code}" --location --request GET "http://${SYNDICATOR_IP}:10039/wps/mycontenthandler/!ut/p/wcmrest/Syndication/Syndicators" \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=000045dibNHXRabRpPXK09BMLJN:-1; LtpaToken2=wjWfRG0zynIzFQiEqPwnaq36NOq8tiDi2qVIUgcoHUTrsgbcq2lw+2VuV+OOOHUGmyVEGNl2L0uRw3MKZIOsGPOcWeiXxxX+4CNXZ58pdJh07aUG4/SYAOymBMM97CJZXJR6SaIBAkgh9q4w8fD2sk7inPyYZzoNvH8o+4/HYdWJOaWYZTc41+4iiq0evxbWNJwH0hc84y4yX4bcWuTIpiB9QugwpQaQ/JSiz1Y4xf/mrOxwwpKzQ9pawLUzuRrmlQzHjn7dstkn6oc8UY6eiow32EbVXp9z3t6CveKVcVZYJSzHEMJq6fzYOqFNwnTcn+J3gFFwPCsYs9O37VZV3+6dhRm4tlK6bcqMdbiWAKREr6xdOpIjRAf5NQzEh5osBTw4NOTZVyPxFvMGaNEL8a2BoTRjq0P5/GUFujFcNW1ytzAFU7i6kEHkbq577ecqwbltF1UkQTZtWC/wTstjFigR8ndbn6vKP9DXelknfTVyi6+VkzjGGSHb5CRPmTiDrlU9TbVgBSU35x6+wrq4rCV+BjDT427Foechnrc7hpFBzZEImEEM+CI4x6oLNG8No4USJajkmv4PvWe81I3sYi5H9r4plHljRe5j0r6ihWUhV8Lu1kAZXNwJ7lal0m77cP/5O5qZsO2gyPp2wowTRxjOWRBEltYCi0i+PljwE90=' \
--data '{}'

echo "Getting the UUID of the Syndicator"
SyndicatorUUID=`cat Syndicators.txt | jq '.entry.content.Syndicators.SyndicatorList[0].SyndicatorUUID'`
echo "Syndication Library UUID is $SyndicatorUUID"

# echo "Creating a DxContentSite"
# curl -b cookie.txt  -o Site.txt -w "%{http_code}" --location --request POST "http://${SYNDICATOR_IP}:10039/wps/mycontenthandler/!ut/p/digest!CWb11PoiCvMXKblug8zpmg/wcmrest-v2/content-sites/" \
# --header 'Authorization: Basic d3BzYWRtaW46d3BzYWRtaW4=' \
# --header 'Content-Type: application/json' \
# --data '{
#     "title": {
#         "lang": "en",
#         "value": "DEMO_SITE_1620184433506"
#     },
#     "name": "DEMO_SITE_1620184433506",
#     "type": "DxContentSite",
#     "libraryID": "'"$SyndicationLibrary"'",
#     "data": {
#         "presentation": {
#             "model": {
#                 "components": "[{\"droppable\":\".cell\",\"components\":[{\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"i2de\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]}],\"resizable\":{\"br\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":0},\"classes\":[{\"private\":1,\"name\":\"row\"},\"rrw2\"],\"name\":\"Row\",\"attributes\":{\"id\":\"icd7\"}},{\"droppable\":\".cell\",\"components\":[{\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"itlo\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]},{\"components\":[{\"components\":[{\"copyable\":false,\"_innertext\":false,\"draggable\":false,\"removable\":false,\"highlightable\":0,\"type\":\"textnode\",\"content\":\"This is a test\"},{\"copyable\":false,\"_innertext\":false,\"draggable\":false,\"removable\":false,\"classes\":[\"link\"],\"attributes\":{\"href\":\"\",\"id\":\"ibjv\"},\"highlightable\":0,\"type\":\"link\"}],\"attributes\":{\"id\":\"i5ni\"},\"type\":\"text\"}],\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"ipy8\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]}],\"resizable\":{\"br\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":0},\"classes\":[{\"private\":1,\"name\":\"row\"}],\"name\":\"Row\"},{\"attributes\":{\"src\":\"data:image\/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIHZpZXdCb3g9IjAgMCAyNCAyNCIgc3R5bGU9ImZpbGw6IHJnYmEoMCwwLDAsMC4xNSk7IHRyYW5zZm9ybTogc2NhbGUoMC43NSkiPgogICAgICAgIDxwYXRoIGQ9Ik04LjUgMTMuNWwyLjUgMyAzLjUtNC41IDQuNSA2SDVtMTYgMVY1YTIgMiAwIDAgMC0yLTJINWMtMS4xIDAtMiAuOS0yIDJ2MTRjMCAxLjEuOSAyIDIgMmgxNGMxLjEgMCAyLS45IDItMnoiPjwvcGF0aD4KICAgICAgPC9zdmc+\",\"id\":\"iibe\"},\"type\":\"image\"}]",
#                 "styles": "[{\"droppable\":\".cell\",\"components\":[{\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"i2de\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]}],\"resizable\":{\"br\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":0},\"classes\":[{\"private\":1,\"name\":\"row\"},\"rrw2\"],\"name\":\"Row\",\"attributes\":{\"id\":\"icd7\"}},{\"droppable\":\".cell\",\"components\":[{\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"itlo\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]},{\"components\":[{\"components\":[{\"copyable\":false,\"_innertext\":false,\"draggable\":false,\"removable\":false,\"highlightable\":0,\"type\":\"textnode\",\"content\":\"This is a test\"},{\"copyable\":false,\"_innertext\":false,\"draggable\":false,\"removable\":false,\"classes\":[\"link\"],\"attributes\":{\"href\":\"\",\"id\":\"ibjv\"},\"highlightable\":0,\"type\":\"link\"}],\"attributes\":{\"id\":\"i5ni\"},\"type\":\"text\"}],\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"ipy8\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]}],\"resizable\":{\"br\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":0},\"classes\":[{\"private\":1,\"name\":\"row\"}],\"name\":\"Row\"},{\"attributes\":{\"src\":\"data:image\/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIHZpZXdCb3g9IjAgMCAyNCAyNCIgc3R5bGU9ImZpbGw6IHJnYmEoMCwwLDAsMC4xNSk7IHRyYW5zZm9ybTogc2NhbGUoMC43NSkiPgogICAgICAgIDxwYXRoIGQ9Ik04LjUgMTMuNWwyLjUgMyAzLjUtNC41IDQuNSA2SDVtMTYgMVY1YTIgMiAwIDAgMC0yLTJINWMtMS4xIDAtMiAuOS0yIDJ2MTRjMCAxLjEuOSAyIDIgMmgxNGMxLjEgMCAyLS45IDItMnoiPjwvcGF0aD4KICAgICAgPC9zdmc+\",\"id\":\"iibe\"},\"type\":\"image\"}]"
#             },
#             "markup": {
#                 "style-reference": "wcmrest:LibraryStyleSheetComponent/15da7680-3c37-4ded-86e1-5414136785f9",
#                 "markup": "<b>This is a sample set of mark up to test that transformer.</b>"
#             }
#         },
#         "properties": {
#             "slug": "demo"

#         }
#     }
# }'

# SiteID=`cat Site.txt | jq -r '.id'`

# if [ -z "$SiteID" ] || [ "$SiteID" = "null" ]
# then 
#     echo "Failed to create Site, see Site.txt file for errors"
#     exit 200
# else 
#     echo "SiteID is $SiteID"
# fi


# echo "Creating a DxContentPage"
# curl -b cookie.txt  -o Page.txt -w "%{http_code}" --location --request POST "http://${SYNDICATOR_IP}:10039/wps/mycontenthandler/!ut/p/digest!CWb11PoiCvMXKblug8zpmg/wcmrest-v2/content-pages/" \
# --header 'Authorization: Basic d3BzYWRtaW46d3BzYWRtaW4=' \
# --header 'Content-Type: application/json' \
# --data '{
#     "title": {
#         "lang": "en",
#         "value": "DEMO_PAGE_1620184745295"
#     },
#     "name": "DEMO_PAGE_1620184745295",
#     "type": "DxContentPage",
    
#     "libraryID": "'"$SyndicationLibrary"'",
#     "data": {
#         "presentation": {
#             "model": {
#                 "components": "[{\"droppable\":\".cell\",\"components\":[{\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"i2de\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]}],\"resizable\":{\"br\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":0},\"classes\":[{\"private\":1,\"name\":\"row\"},\"rrw2\"],\"name\":\"Row\",\"attributes\":{\"id\":\"icd7\"}},{\"droppable\":\".cell\",\"components\":[{\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"itlo\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]},{\"components\":[{\"components\":[{\"copyable\":false,\"_innertext\":false,\"draggable\":false,\"removable\":false,\"highlightable\":0,\"type\":\"textnode\",\"content\":\"This is a test\"},{\"copyable\":false,\"_innertext\":false,\"draggable\":false,\"removable\":false,\"classes\":[\"link\"],\"attributes\":{\"href\":\"\",\"id\":\"ibjv\"},\"highlightable\":0,\"type\":\"link\"}],\"attributes\":{\"id\":\"i5ni\"},\"type\":\"text\"}],\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"ipy8\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]}],\"resizable\":{\"br\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":0},\"classes\":[{\"private\":1,\"name\":\"row\"}],\"name\":\"Row\"},{\"attributes\":{\"src\":\"data:image\/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIHZpZXdCb3g9IjAgMCAyNCAyNCIgc3R5bGU9ImZpbGw6IHJnYmEoMCwwLDAsMC4xNSk7IHRyYW5zZm9ybTogc2NhbGUoMC43NSkiPgogICAgICAgIDxwYXRoIGQ9Ik04LjUgMTMuNWwyLjUgMyAzLjUtNC41IDQuNSA2SDVtMTYgMVY1YTIgMiAwIDAgMC0yLTJINWMtMS4xIDAtMiAuOS0yIDJ2MTRjMCAxLjEuOSAyIDIgMmgxNGMxLjEgMCAyLS45IDItMnoiPjwvcGF0aD4KICAgICAgPC9zdmc+\",\"id\":\"iibe\"},\"type\":\"image\"}]",
#                 "styles": "[{\"droppable\":\".cell\",\"components\":[{\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"i2de\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]}],\"resizable\":{\"br\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":0},\"classes\":[{\"private\":1,\"name\":\"row\"},\"rrw2\"],\"name\":\"Row\",\"attributes\":{\"id\":\"icd7\"}},{\"droppable\":\".cell\",\"components\":[{\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"itlo\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]},{\"components\":[{\"components\":[{\"copyable\":false,\"_innertext\":false,\"draggable\":false,\"removable\":false,\"highlightable\":0,\"type\":\"textnode\",\"content\":\"This is a test\"},{\"copyable\":false,\"_innertext\":false,\"draggable\":false,\"removable\":false,\"classes\":[\"link\"],\"attributes\":{\"href\":\"\",\"id\":\"ibjv\"},\"highlightable\":0,\"type\":\"link\"}],\"attributes\":{\"id\":\"i5ni\"},\"type\":\"text\"}],\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"ipy8\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]}],\"resizable\":{\"br\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":0},\"classes\":[{\"private\":1,\"name\":\"row\"}],\"name\":\"Row\"},{\"attributes\":{\"src\":\"data:image\/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIHZpZXdCb3g9IjAgMCAyNCAyNCIgc3R5bGU9ImZpbGw6IHJnYmEoMCwwLDAsMC4xNSk7IHRyYW5zZm9ybTogc2NhbGUoMC43NSkiPgogICAgICAgIDxwYXRoIGQ9Ik04LjUgMTMuNWwyLjUgMyAzLjUtNC41IDQuNSA2SDVtMTYgMVY1YTIgMiAwIDAgMC0yLTJINWMtMS4xIDAtMiAuOS0yIDJ2MTRjMCAxLjEuOSAyIDIgMmgxNGMxLjEgMCAyLS45IDItMnoiPjwvcGF0aD4KICAgICAgPC9zdmc+\",\"id\":\"iibe\"},\"type\":\"image\"}]"
#             },
#             "markup": {
#                 "style-reference": "wcmrest:StyleSheetComponent/15da7680-3c37-4ded-86e1-5414136785f9",
#                 "markup": "<b>This is a sample set of mark up to test that transformer.</b>"
#             }
#         },
#         "properties" :
#         {
#             "slug" : "theSiteV1"
#         }
#     }
# }'

# echo "Creating a DxSymbol"
# curl -b cookie.txt -s  -o Symbol.txt -w "%{http_code}" --location --request POST "http://${SYNDICATOR_IP}:10039/wps/mycontenthandler/!ut/p/digest!CWb11PoiCvMXKblug8zpmg/wcmrest-v2/containers/" \
# --header 'Authorization: Basic d3BzYWRtaW46d3BzYWRtaW4=' \
# --header 'Content-Type: application/json' \
# --header 'Cookie: JSESSIONID=0000Qv8edYlR31Xp9F2qouMGoA5:-1; LtpaToken2=1wvOYgK01fEtMh8+GzmhPlj8pGF7+9zlRvGrTkuF0JMMaj6ifFZv2h4QuU7IDE+HjoiEBsu7Kd9DeW6bV+A31MlnDejc6b+Y+fRfOyDPmGA7WfcgPFJxSyb4KsjCNiUAmU+j7bpTeTIUcbPlu586HhyqTbiLeN7bsuO6+1Iq4WlWjXk9WbVcihVQSY7deNzMV3IA68xSbhSVkXtkLCGLGDjVhkLsAPfsoLLkAQU0LbhmwGh30YKuF40PjPK89QYwa2GcoZYKoSvidOMoXp43muDKNURwLFLz72Jk1/jkG5y05hbi4aLcjfwgwzBVSjpsP303WddTtYEkc4BqSwt1gF9p9Ug2cuiowdRsowKi4U4LsDz+IB+BF+ZZmdDTzz+4Aay+/+7GKT42B9opCH/KxUVE9CNVG4a8O2K9z/+xigvNJZ+vEIE2EXQJtk0yhE3h2+BO6l9YZ1Ts5YuMhcZYbd0awaMQybjuOK9JHGFQAVL/x7hPvcz9rBtzN5LvkE6fW4rosyNGYs/bbneZFebRvNG801XV9EqOUbULhcbOXGmzp3bCgvGwEFo2HMuPBr3GObnAs13JA/DGD8h7k1C3g+lAnRac+/1qoB84xUBKwv9IY8JqM3/qwCvW92jKXBXVMld48Mw7v1mvWSOVFRKp06NCGJxqSYxPOLnGgUUJ86M=' \
# --data '{
#     "title": {
#         "lang": "en",
#         "value": "DxSymbol"
#     },
#     "name": "DxSymbol",
#     "type": "DxSymbol",
#     "libraryID": "'"$SyndicationLibrary"'",
#     "data": {
#         "presentation": {
#             "model": {
#                 "components": "[{\"droppable\":\".cell\",\"components\":[{\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"i2de\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]}],\"resizable\":{\"br\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":0},\"classes\":[{\"private\":1,\"name\":\"row\"},\"rrw2\"],\"name\":\"Row\",\"attributes\":{\"id\":\"icd7\"}},{\"droppable\":\".cell\",\"components\":[{\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"itlo\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]},{\"components\":[{\"components\":[{\"copyable\":false,\"_innertext\":false,\"draggable\":false,\"removable\":false,\"highlightable\":0,\"type\":\"textnode\",\"content\":\"This is a test\"},{\"copyable\":false,\"_innertext\":false,\"draggable\":false,\"removable\":false,\"classes\":[\"link\"],\"attributes\":{\"href\":\"\",\"id\":\"ibjv\"},\"highlightable\":0,\"type\":\"link\"}],\"attributes\":{\"id\":\"i5ni\"},\"type\":\"text\"}],\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"ipy8\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]}],\"resizable\":{\"br\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":0},\"classes\":[{\"private\":1,\"name\":\"row\"}],\"name\":\"Row\"},{\"attributes\":{\"src\":\"data:image\/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIHZpZXdCb3g9IjAgMCAyNCAyNCIgc3R5bGU9ImZpbGw6IHJnYmEoMCwwLDAsMC4xNSk7IHRyYW5zZm9ybTogc2NhbGUoMC43NSkiPgogICAgICAgIDxwYXRoIGQ9Ik04LjUgMTMuNWwyLjUgMyAzLjUtNC41IDQuNSA2SDVtMTYgMVY1YTIgMiAwIDAgMC0yLTJINWMtMS4xIDAtMiAuOS0yIDJ2MTRjMCAxLjEuOSAyIDIgMmgxNGMxLjEgMCAyLS45IDItMnoiPjwvcGF0aD4KICAgICAgPC9zdmc+\",\"id\":\"iibe\"},\"type\":\"image\"}]",
#                 "styles": "[{\"droppable\":\".cell\",\"components\":[{\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"i2de\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]}],\"resizable\":{\"br\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":0},\"classes\":[{\"private\":1,\"name\":\"row\"},\"rrw2\"],\"name\":\"Row\",\"attributes\":{\"id\":\"icd7\"}},{\"droppable\":\".cell\",\"components\":[{\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"itlo\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]},{\"components\":[{\"components\":[{\"copyable\":false,\"_innertext\":false,\"draggable\":false,\"removable\":false,\"highlightable\":0,\"type\":\"textnode\",\"content\":\"This is a test\"},{\"copyable\":false,\"_innertext\":false,\"draggable\":false,\"removable\":false,\"classes\":[\"link\"],\"attributes\":{\"href\":\"\",\"id\":\"ibjv\"},\"highlightable\":0,\"type\":\"link\"}],\"attributes\":{\"id\":\"i5ni\"},\"type\":\"text\"}],\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"ipy8\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]}],\"resizable\":{\"br\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":0},\"classes\":[{\"private\":1,\"name\":\"row\"}],\"name\":\"Row\"},{\"attributes\":{\"src\":\"data:image\/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIHZpZXdCb3g9IjAgMCAyNCAyNCIgc3R5bGU9ImZpbGw6IHJnYmEoMCwwLDAsMC4xNSk7IHRyYW5zZm9ybTogc2NhbGUoMC43NSkiPgogICAgICAgIDxwYXRoIGQ9Ik04LjUgMTMuNWwyLjUgMyAzLjUtNC41IDQuNSA2SDVtMTYgMVY1YTIgMiAwIDAgMC0yLTJINWMtMS4xIDAtMiAuOS0yIDJ2MTRjMCAxLjEuOSAyIDIgMmgxNGMxLjEgMCAyLS45IDItMnoiPjwvcGF0aD4KICAgICAgPC9zdmc+\",\"id\":\"iibe\"},\"type\":\"image\"}]"
#             },
#             "markup": {
#                 "style-reference": "wcmrest:LibraryStyleSheetComponent/15da7680-3c37-4ded-86e1-5414136785f9",
#                 "markup": "<b>This is a sample set of mark up to test that transformer.</b>"
#             }
#         },
#         "properties": {
#             "slug": "theSite"
#         }
#     }
# }'

echo "Creating a SyndicationProject"
curl -b cookie.txt  -o Project.txt -w "%{http_code}" --location --request POST "http://${SYNDICATOR_IP}:10039/wps/mycontenthandler/wcmrest/Project?mime-type=application/json" \
--header 'Authorization: Basic d3BzYWRtaW46d3BzYWRtaW4=' \
--header 'Content-Type: application/atom+xml' \
--data '<entry xmlns="http://www.w3.org/2005/Atom" xmlns:wcm="wcm/namespace">
    <title>SyndicationProject</title>
    <wcm:name>SyndicationProject</wcm:name>
    <wcm:description>Project for syndication test</wcm:description>
</entry>'

ProjectID=`cat Project.txt | jq -r '.entry.id' | sed 's/wcmrest://'`

if [ -z "$ProjectID" ] || [ "$ProjectID" = "null" ]
then 
    echo "Failed to create Project, see Project.txt file for errors"
    exit 200
else 
    echo "ProjectID is $ProjectID"
fi

# ParentPageID=`cat Page.txt | jq -r '.parentID'`

# if [ -z "$ParentPageID" ] || [ "$ParentPageID" = "null" ]
# then 
#     echo "Failed to get ParentID"
#     exit 200
# else 
#     echo "ParentPageID is $ParentPageID"
# fi

echo "Getting the Sample Article Content ID"
curl -b cookie.txt -s  -o Content.txt -w "%{http_code}" --location --request GET "http://${SYNDICATOR_IP}:10039/wps/mycontenthandler/!ut/p/wcmrest-v2/search?libraryID=$SyndicationLibrary&title=Sample%20Article&type=Content" \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=000045dibNHXRabRpPXK09BMLJN:-1; LtpaToken2=wjWfRG0zynIzFQiEqPwnaq36NOq8tiDi2qVIUgcoHUTrsgbcq2lw+2VuV+OOOHUGmyVEGNl2L0uRw3MKZIOsGPOcWeiXxxX+4CNXZ58pdJh07aUG4/SYAOymBMM97CJZXJR6SaIBAkgh9q4w8fD2sk7inPyYZzoNvH8o+4/HYdWJOaWYZTc41+4iiq0evxbWNJwH0hc84y4yX4bcWuTIpiB9QugwpQaQ/JSiz1Y4xf/mrOxwwpKzQ9pawLUzuRrmlQzHjn7dstkn6oc8UY6eiow32EbVXp9z3t6CveKVcVZYJSzHEMJq6fzYOqFNwnTcn+J3gFFwPCsYs9O37VZV3+6dhRm4tlK6bcqMdbiWAKREr6xdOpIjRAf5NQzEh5osBTw4NOTZVyPxFvMGaNEL8a2BoTRjq0P5/GUFujFcNW1ytzAFU7i6kEHkbq577ecqwbltF1UkQTZtWC/wTstjFigR8ndbn6vKP9DXelknfTVyi6+VkzjGGSHb5CRPmTiDrlU9TbVgBSU35x6+wrq4rCV+BjDT427Foechnrc7hpFBzZEImEEM+CI4x6oLNG8No4USJajkmv4PvWe81I3sYi5H9r4plHljRe5j0r6ihWUhV8Lu1kAZXNwJ7lal0m77cP/5O5qZsO2gyPp2wowTRxjOWRBEltYCi0i+PljwE90=' \
--data '{}' 

ContentID=`cat Content.txt | jq -r '.entries[].id'`

if [ -z "$ContentID" ] || [ "$ContentID" = "null" ]
then 
    echo "Failed to get ContentID"
    exit 200
else 
    echo "ContentID is $ContentID"
fi

echo "Getting the templateID"
curl -b cookie.txt -s  -o ContentTemplate.txt -w "%{http_code}" --location --request GET "http://${SYNDICATOR_IP}:10039/wps/mycontenthandler/!ut/p/wcmrest-v2/contents/$ContentID" \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=000045dibNHXRabRpPXK09BMLJN:-1; LtpaToken2=wjWfRG0zynIzFQiEqPwnaq36NOq8tiDi2qVIUgcoHUTrsgbcq2lw+2VuV+OOOHUGmyVEGNl2L0uRw3MKZIOsGPOcWeiXxxX+4CNXZ58pdJh07aUG4/SYAOymBMM97CJZXJR6SaIBAkgh9q4w8fD2sk7inPyYZzoNvH8o+4/HYdWJOaWYZTc41+4iiq0evxbWNJwH0hc84y4yX4bcWuTIpiB9QugwpQaQ/JSiz1Y4xf/mrOxwwpKzQ9pawLUzuRrmlQzHjn7dstkn6oc8UY6eiow32EbVXp9z3t6CveKVcVZYJSzHEMJq6fzYOqFNwnTcn+J3gFFwPCsYs9O37VZV3+6dhRm4tlK6bcqMdbiWAKREr6xdOpIjRAf5NQzEh5osBTw4NOTZVyPxFvMGaNEL8a2BoTRjq0P5/GUFujFcNW1ytzAFU7i6kEHkbq577ecqwbltF1UkQTZtWC/wTstjFigR8ndbn6vKP9DXelknfTVyi6+VkzjGGSHb5CRPmTiDrlU9TbVgBSU35x6+wrq4rCV+BjDT427Foechnrc7hpFBzZEImEEM+CI4x6oLNG8No4USJajkmv4PvWe81I3sYi5H9r4plHljRe5j0r6ihWUhV8Lu1kAZXNwJ7lal0m77cP/5O5qZsO2gyPp2wowTRxjOWRBEltYCi0i+PljwE90=' \
--data '{}' 

ContentTemplateID=`cat ContentTemplate.txt | jq -r '.templateID'`

if [ -z "$ContentTemplateID" ] || [ "$ContentTemplateID" = "null" ]
then 
    echo "Failed to get ContentTemplate ID"
    exit 200
else 
    echo "ContentTemplateID is $ContentTemplateID"
fi

ParentID=`cat ContentTemplate.txt | jq -r '.parentID'`

if [ -z "$ParentID" ] || [ "$ParentID" = "null" ]
then 
    echo "Failed to get ParentID"
    exit 200
else 
    echo "ParentID is $ParentID"
fi

echo "Creating a SyndicationProjectDraftContent"
curl -b cookie.txt  -o DraftContent.txt -w "%{http_code}" --location --request POST "http://${SYNDICATOR_IP}:10039/wps/mycontenthandler/wcmrest/Content?mime-type=application/json" \
--header 'Authorization: Basic d3BzYWRtaW46d3BzYWRtaW4=' \
--header 'Content-Type: application/atom+xml' \
--data '<atom:entry xmlns:atom="http://www.w3.org/2005/Atom" xmlns:wcm="wcm/namespace">
			<wcm:name>SyndicationDraftContent</wcm:name>
			<atom:title>SyndicationDraftContent</atom:title>
            <atom:link atom:rel="parent" atom:href="/wps/
            mycontenthandler/wcmrest/item/'$ParentID'"/>
			<atom:link atom:rel="library" atom:href="/wps/
            mycontenthandler/wcmrest/libraries/'$SyndicationLibrary'"/>
			<atom:link atom:rel="content-template" atom:href="/wps/
            mycontenthandler/wcmrest/item/'$ContentTemplateID'"/>
            <atom:link atom:rel="project" atom:href="/wps/
            mycontenthandler/wcmrest/Project/'$ProjectID'"/>
		</atom:entry>'

DraftContentID=`cat DraftContent.txt | jq -r '.entry.id' | sed 's/wcmrest://'`

if [ -z "$DraftContentID" ] || [ "$DraftContentID" = "null" ]
then 
    echo "Failed to create a DraftContent, see DraftContent.txt file for errors"
    exit 200
else 
    echo "DraftContentID is $DraftContentID"
fi

sleep 200
