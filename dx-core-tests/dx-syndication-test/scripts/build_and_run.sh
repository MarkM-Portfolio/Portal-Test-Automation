#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2021. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

source ~/.bash_profile 

docker exec -i dx_core /bin/sh -c "
cd /opt/HCL/wp_profile/ConfigEngine/;
./ConfigEngine.sh create-virtual-portal -DWasPassword=wpsadmin -DPortalAdminPwd=wpsadmin -DVirtualPortalTitle=SyndicationVP4 -DVirtualPortalContext=vp4"

docker exec -i dx_core /bin/sh -c "
cd /opt/HCL/wp_profile/ConfigEngine/;
./ConfigEngine.sh list-all-virtual-portals -DWasPassword=wpsadmin -DPortalAdminPwd=wpsadmin"

docker cp /opt/git/result.xml dx_core:/opt/HCL/wp_profile/PortalServer/bin/

docker exec -i dx_core /bin/sh -c "
cd /opt/HCL/wp_profile/PortalServer/bin;
./xmlaccess.sh -user wpsadmin -password wpsadmin -url http://INSTANCE_IP:10039/wps/config/vp4 -in result.xml -out import.xml"

docker cp /opt/git/CreateVaultSlot.xml dx_core:/opt/HCL/wp_profile/PortalServer/bin/

docker exec -i dx_core /bin/sh -c "
cd /opt/HCL/wp_profile/PortalServer/bin;
./xmlaccess.sh -in CreateVaultSlot.xml -out slot-out.xml -url http://INSTANCE_IP:10039/wps/config -user wpsadmin -password wpsadmin"

sleep 20

echo "POST Login"
curl --cookie-jar cookie.txt -s -o response.txt -w "%{http_code}" --location --request POST 'http://INSTANCE_IP:10039/wps/portal/cxml/04_SD9ePMtCP1I800I_KydQvyHFUBADPmuQy?userid=wpsadmin&password=wpsadmin' \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=0000zaNvk6pv3D3I16jDXVbB_2U:-1; LtpaToken2=dXQEQSyxCz/mURMDYxFSJYGwggXxyCYUquNTfDzJVCP2IUv4q157nKvaxdXCIMoVBvH1BiBujmWeRXYaFva0GTtRvTozr2OeYiVubRNrWThvzn8kvxrvbJvUmxjjSmDT7LveIQ4IrTtyKoI5xzlU7xw/ijAak3UM/hN9gr+W4tkh8RGsIKOL25ubROMk1LmnVRbpV6nYiOCDxtHhl5JKh7pC7LDplzQBsT9GOGoMxXkkvMgktGi+RFgS5Yp/KfFQ+uO2JP4jOAMAXkK1fxt+YFhJfzM84zICbtqtUaudkK4s+gwDUob/iq9N2sn6nfa2O/lTfKcmoVqmoHfMfHDNWOW1qECmvE/lEvzQmrJmYqC2Jw8nxLb44Wrsubg7Q5I2B5aAb3UmjzQI6+wh+8Ezn9A85Hfl+BsHhFoeGT52Ehj5czOS6tDCBPyZ2n+hNmWmQYlPAk/f3s94kPM/Zs+woLjCQ8x/DnvVZSupaMqlqHEGTpHGifM8yTY9PH3k1/xtq9N1eokR+p94SH3Po4SKtT/NbTzyHTtahzH2ITeJAOB06OUfQ/Lb0RrH0zarze931kSmPI+t3GbP4sN9z/jotL/HCuTw96oq4l0+PAqGjPE4kqJyMKt8b4rEzmBB4F5d1wm1Ben1FI8gIQ6UWcsAnMfoqdhH3iDJdWmwHsr86880Roum+5qVfrcX+LXjE/LQ4asKLapc51sotJsXl9+oxg==' \
--data '{
	"username":"wpsadmin",
	"password":"wpsadmin"
}'

echo "Create Syndication Library"
curl -b cookie.txt -s  -o SyndicationLibrary.txt -w "%{http_code}" --location --request POST 'http://INSTANCE_IP:10039/wps/mycontenthandler/wcmrest/Library?mime-type=application/json' \
--header 'Content-Type: text/plain' \
--header 'Cookie: JSESSIONID=0000lmG0NZCvpX7miPAqmF6jSW4:-1; LtpaToken2=cD4ZjDInifWJHEu9X0WuLspqRb4xbgryuIyBMGTN8/Gp5c5gHa4MFUihyrcwloQ2FblUEpnYqFmiqYi0Agvsrj5teS7z7GSoeiTN7b8oAVRlDCm5j2l3noTJuFoU0+mes34sYJmzINMcgNYoaJIEJQ43Gk5XspBpvhk9q4vokq2hDiPQbVDRxWp2QhR5JAXyCid9QalmYPgaVkH94bfXZih/tB1caB5xZA3hZkCEnpxKTR6nr/jlQJvk/MyRZ5quK1UymB8fBMTTvLjNuHzuQ8XSSdWrhY72v2KoCn/H4YFbWVf9aZ5xvMlKiqFWhCt8NhpgqigijfZDcBNIvbTFDAvojfXpW5n8NlpUkiOqXVd7R7D7LXVIq7gMfsLAXXNz+aiOzcj3aSDVw/LbWvhCX+KP409m0CKaA0gg2vkbJD9jl7vjH718q1MvV8XZowQEgN1VeMWDFAMOmRlpYmIHUVLah9/mylkhRNaJobK7K0OiA8JtfcB+D9JJRqjOqPpyd9N/D1/E1Qgr4qQt89Pnz+i9Od0+eR16zQRTpsfLUzlssrukRtE4TbSEySK6rG4P6p4KJVr42lcnTXz3iRXUG4blprW2fes8sFTjYO+3W8aoTUlbjyUZfDqg2jI8F0BAVvKqfELUbRMsAZjaStoaYwn0mZAu1Nuzi6OuwY7IE90=' \
--data '<?xml version="1.0" encoding="UTF-8"?>
<entry xmlns="http://www.w3.org/2005/Atom" xmlns:wcm="http://www.ibm.com/xmlns/wcm/8.0">
    <wcm:name>SyndicationLibrary</wcm:name>
    <content type="application/vnd.ibm.wcm+xml">
        <wcm:library xmlns="http://www.ibm.com/xmlns/wcm/8.0">
            <allowDeletion>false</allowDeletion>
            <enabled>true</enabled>
            <language>en</language>
            <includeDefaultItems>true</includeDefaultItems>
        </wcm:library>
    </content>
</entry>'

SyndicationLibrary=`cat SyndicationLibrary.txt | jq -r '.entry.id' | sed 's/wcmrest://'`

sleep 30

echo "Setting Syndication between base and VP"
docker exec -i dx_core /bin/sh -c "
cd /opt/HCL/wp_profile/ConfigEngine/
./ConfigEngine.sh run-wcm-admin-task-subscribe-now -Dsyndicator=http://INSTANCE_IP:10039/wps/wcm -DvaultSlotName=syndication-slot -DsyndicatorName=syndicator1 -DsubscriberName=subscriber1 -DVirtualPortalContext=vp4 -Dpublished-items='Web Content,SyndicationLibrary' -DPortalAdminPwd=wpsadmin -DWasPassword=wpsadmin"

sleep 30

echo "Verifying Syndication status between base and VP"
curl -b cookie.txt -s  -o Syndicators.txt -w "%{http_code}" --location --request GET 'http://INSTANCE_IP:10039/wps/mycontenthandler/!ut/p/wcmrest/Syndication/Syndicators' \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=000045dibNHXRabRpPXK09BMLJN:-1; LtpaToken2=wjWfRG0zynIzFQiEqPwnaq36NOq8tiDi2qVIUgcoHUTrsgbcq2lw+2VuV+OOOHUGmyVEGNl2L0uRw3MKZIOsGPOcWeiXxxX+4CNXZ58pdJh07aUG4/SYAOymBMM97CJZXJR6SaIBAkgh9q4w8fD2sk7inPyYZzoNvH8o+4/HYdWJOaWYZTc41+4iiq0evxbWNJwH0hc84y4yX4bcWuTIpiB9QugwpQaQ/JSiz1Y4xf/mrOxwwpKzQ9pawLUzuRrmlQzHjn7dstkn6oc8UY6eiow32EbVXp9z3t6CveKVcVZYJSzHEMJq6fzYOqFNwnTcn+J3gFFwPCsYs9O37VZV3+6dhRm4tlK6bcqMdbiWAKREr6xdOpIjRAf5NQzEh5osBTw4NOTZVyPxFvMGaNEL8a2BoTRjq0P5/GUFujFcNW1ytzAFU7i6kEHkbq577ecqwbltF1UkQTZtWC/wTstjFigR8ndbn6vKP9DXelknfTVyi6+VkzjGGSHb5CRPmTiDrlU9TbVgBSU35x6+wrq4rCV+BjDT427Foechnrc7hpFBzZEImEEM+CI4x6oLNG8No4USJajkmv4PvWe81I3sYi5H9r4plHljRe5j0r6ihWUhV8Lu1kAZXNwJ7lal0m77cP/5O5qZsO2gyPp2wowTRxjOWRBEltYCi0i+PljwE90=' \
--data '{}'

SyndicatorUUID=`cat Syndicators.txt | jq '.entry.content.Syndicators.SyndicatorList[0].SyndicatorUUID'`
echo $SyndicatorUUID

curl -b cookie.txt -s  -o Subscribers.txt -w "%{http_code}" --location --request GET 'http://INSTANCE_IP:10039/wps/mycontenthandler/vp4/!ut/p/wcmrest/Syndication/Subscribers' \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=000045dibNHXRabRpPXK09BMLJN:-1; LtpaToken2=wjWfRG0zynIzFQiEqPwnaq36NOq8tiDi2qVIUgcoHUTrsgbcq2lw+2VuV+OOOHUGmyVEGNl2L0uRw3MKZIOsGPOcWeiXxxX+4CNXZ58pdJh07aUG4/SYAOymBMM97CJZXJR6SaIBAkgh9q4w8fD2sk7inPyYZzoNvH8o+4/HYdWJOaWYZTc41+4iiq0evxbWNJwH0hc84y4yX4bcWuTIpiB9QugwpQaQ/JSiz1Y4xf/mrOxwwpKzQ9pawLUzuRrmlQzHjn7dstkn6oc8UY6eiow32EbVXp9z3t6CveKVcVZYJSzHEMJq6fzYOqFNwnTcn+J3gFFwPCsYs9O37VZV3+6dhRm4tlK6bcqMdbiWAKREr6xdOpIjRAf5NQzEh5osBTw4NOTZVyPxFvMGaNEL8a2BoTRjq0P5/GUFujFcNW1ytzAFU7i6kEHkbq577ecqwbltF1UkQTZtWC/wTstjFigR8ndbn6vKP9DXelknfTVyi6+VkzjGGSHb5CRPmTiDrlU9TbVgBSU35x6+wrq4rCV+BjDT427Foechnrc7hpFBzZEImEEM+CI4x6oLNG8No4USJajkmv4PvWe81I3sYi5H9r4plHljRe5j0r6ihWUhV8Lu1kAZXNwJ7lal0m77cP/5O5qZsO2gyPp2wowTRxjOWRBEltYCi0i+PljwE90=' \
--data '{}'

SubscriberUUID=`cat Subscribers.txt | jq '.entry.content.Subscribers.SubscriberList[0].SubscriberUUID'`
echo $SubscriberUUID

echo "Verifying Subscriber status between base and VP"
curl -b cookie.txt -s  -o SubscriberStatus.txt -w "%{http_code}" --location --request GET 'http://INSTANCE_IP:10039/wps/mycontenthandler/wcmrest/Syndicator/$SubscriberUUID/status' \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=000045dibNHXRabRpPXK09BMLJN:-1; LtpaToken2=wjWfRG0zynIzFQiEqPwnaq36NOq8tiDi2qVIUgcoHUTrsgbcq2lw+2VuV+OOOHUGmyVEGNl2L0uRw3MKZIOsGPOcWeiXxxX+4CNXZ58pdJh07aUG4/SYAOymBMM97CJZXJR6SaIBAkgh9q4w8fD2sk7inPyYZzoNvH8o+4/HYdWJOaWYZTc41+4iiq0evxbWNJwH0hc84y4yX4bcWuTIpiB9QugwpQaQ/JSiz1Y4xf/mrOxwwpKzQ9pawLUzuRrmlQzHjn7dstkn6oc8UY6eiow32EbVXp9z3t6CveKVcVZYJSzHEMJq6fzYOqFNwnTcn+J3gFFwPCsYs9O37VZV3+6dhRm4tlK6bcqMdbiWAKREr6xdOpIjRAf5NQzEh5osBTw4NOTZVyPxFvMGaNEL8a2BoTRjq0P5/GUFujFcNW1ytzAFU7i6kEHkbq577ecqwbltF1UkQTZtWC/wTstjFigR8ndbn6vKP9DXelknfTVyi6+VkzjGGSHb5CRPmTiDrlU9TbVgBSU35x6+wrq4rCV+BjDT427Foechnrc7hpFBzZEImEEM+CI4x6oLNG8No4USJajkmv4PvWe81I3sYi5H9r4plHljRe5j0r6ihWUhV8Lu1kAZXNwJ7lal0m77cP/5O5qZsO2gyPp2wowTRxjOWRBEltYCi0i+PljwE90=' \
--data '{}'

echo "Creating a DxContentSite"
curl -b cookie.txt  -o Site.txt -w "%{http_code}" --location --request POST 'http://INSTANCE_IP:10039/wps/mycontenthandler/!ut/p/digest!CWb11PoiCvMXKblug8zpmg/wcmrest-v2/content-sites/' \
--header 'Authorization: Basic d3BzYWRtaW46d3BzYWRtaW4=' \
--header 'Content-Type: application/json' \
--data '{
    "title": {
        "lang": "en",
        "value": "DEMO_SITE_1620184433506"
    },
    "name": "DEMO_SITE_1620184433506",
    "type": "DxContentSite",
    "libraryID": "'"$SyndicationLibrary"'",
    "data": {
        "presentation": {
            "model": {
                "components": "[{\"droppable\":\".cell\",\"components\":[{\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"i2de\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]}],\"resizable\":{\"br\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":0},\"classes\":[{\"private\":1,\"name\":\"row\"},\"rrw2\"],\"name\":\"Row\",\"attributes\":{\"id\":\"icd7\"}},{\"droppable\":\".cell\",\"components\":[{\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"itlo\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]},{\"components\":[{\"components\":[{\"copyable\":false,\"_innertext\":false,\"draggable\":false,\"removable\":false,\"highlightable\":0,\"type\":\"textnode\",\"content\":\"This is a test\"},{\"copyable\":false,\"_innertext\":false,\"draggable\":false,\"removable\":false,\"classes\":[\"link\"],\"attributes\":{\"href\":\"\",\"id\":\"ibjv\"},\"highlightable\":0,\"type\":\"link\"}],\"attributes\":{\"id\":\"i5ni\"},\"type\":\"text\"}],\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"ipy8\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]}],\"resizable\":{\"br\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":0},\"classes\":[{\"private\":1,\"name\":\"row\"}],\"name\":\"Row\"},{\"attributes\":{\"src\":\"data:image\/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIHZpZXdCb3g9IjAgMCAyNCAyNCIgc3R5bGU9ImZpbGw6IHJnYmEoMCwwLDAsMC4xNSk7IHRyYW5zZm9ybTogc2NhbGUoMC43NSkiPgogICAgICAgIDxwYXRoIGQ9Ik04LjUgMTMuNWwyLjUgMyAzLjUtNC41IDQuNSA2SDVtMTYgMVY1YTIgMiAwIDAgMC0yLTJINWMtMS4xIDAtMiAuOS0yIDJ2MTRjMCAxLjEuOSAyIDIgMmgxNGMxLjEgMCAyLS45IDItMnoiPjwvcGF0aD4KICAgICAgPC9zdmc+\",\"id\":\"iibe\"},\"type\":\"image\"}]",
                "styles": "[{\"droppable\":\".cell\",\"components\":[{\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"i2de\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]}],\"resizable\":{\"br\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":0},\"classes\":[{\"private\":1,\"name\":\"row\"},\"rrw2\"],\"name\":\"Row\",\"attributes\":{\"id\":\"icd7\"}},{\"droppable\":\".cell\",\"components\":[{\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"itlo\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]},{\"components\":[{\"components\":[{\"copyable\":false,\"_innertext\":false,\"draggable\":false,\"removable\":false,\"highlightable\":0,\"type\":\"textnode\",\"content\":\"This is a test\"},{\"copyable\":false,\"_innertext\":false,\"draggable\":false,\"removable\":false,\"classes\":[\"link\"],\"attributes\":{\"href\":\"\",\"id\":\"ibjv\"},\"highlightable\":0,\"type\":\"link\"}],\"attributes\":{\"id\":\"i5ni\"},\"type\":\"text\"}],\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"ipy8\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]}],\"resizable\":{\"br\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":0},\"classes\":[{\"private\":1,\"name\":\"row\"}],\"name\":\"Row\"},{\"attributes\":{\"src\":\"data:image\/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIHZpZXdCb3g9IjAgMCAyNCAyNCIgc3R5bGU9ImZpbGw6IHJnYmEoMCwwLDAsMC4xNSk7IHRyYW5zZm9ybTogc2NhbGUoMC43NSkiPgogICAgICAgIDxwYXRoIGQ9Ik04LjUgMTMuNWwyLjUgMyAzLjUtNC41IDQuNSA2SDVtMTYgMVY1YTIgMiAwIDAgMC0yLTJINWMtMS4xIDAtMiAuOS0yIDJ2MTRjMCAxLjEuOSAyIDIgMmgxNGMxLjEgMCAyLS45IDItMnoiPjwvcGF0aD4KICAgICAgPC9zdmc+\",\"id\":\"iibe\"},\"type\":\"image\"}]"
            },
            "markup": {
                "style-reference": "wcmrest:LibraryStyleSheetComponent/15da7680-3c37-4ded-86e1-5414136785f9",
                "markup": "<b>This is a sample set of mark up to test that transformer.</b>"
            }
        },
        "properties": {
            "slug": "demo"

        }
    }
}'

SiteID=`cat Site.txt | jq -r '.id'`
echo $SiteID

echo "Creating a DxContentPage"
curl -b cookie.txt  -o Page.txt -w "%{http_code}" --location --request POST 'http://INSTANCE_IP:10039/wps/mycontenthandler/!ut/p/digest!CWb11PoiCvMXKblug8zpmg/wcmrest-v2/content-pages/' \
--header 'Authorization: Basic d3BzYWRtaW46d3BzYWRtaW4=' \
--header 'Content-Type: application/json' \
--data '{
    "title": {
        "lang": "en",
        "value": "DEMO_PAGE_1620184745295"
    },
    "name": "DEMO_PAGE_1620184745295",
    "type": "DxContentPage",
    
    "libraryID": "'"$SyndicationLibrary"'",
    "data": {
        "presentation": {
            "model": {
                "components": "[{\"droppable\":\".cell\",\"components\":[{\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"i2de\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]}],\"resizable\":{\"br\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":0},\"classes\":[{\"private\":1,\"name\":\"row\"},\"rrw2\"],\"name\":\"Row\",\"attributes\":{\"id\":\"icd7\"}},{\"droppable\":\".cell\",\"components\":[{\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"itlo\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]},{\"components\":[{\"components\":[{\"copyable\":false,\"_innertext\":false,\"draggable\":false,\"removable\":false,\"highlightable\":0,\"type\":\"textnode\",\"content\":\"This is a test\"},{\"copyable\":false,\"_innertext\":false,\"draggable\":false,\"removable\":false,\"classes\":[\"link\"],\"attributes\":{\"href\":\"\",\"id\":\"ibjv\"},\"highlightable\":0,\"type\":\"link\"}],\"attributes\":{\"id\":\"i5ni\"},\"type\":\"text\"}],\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"ipy8\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]}],\"resizable\":{\"br\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":0},\"classes\":[{\"private\":1,\"name\":\"row\"}],\"name\":\"Row\"},{\"attributes\":{\"src\":\"data:image\/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIHZpZXdCb3g9IjAgMCAyNCAyNCIgc3R5bGU9ImZpbGw6IHJnYmEoMCwwLDAsMC4xNSk7IHRyYW5zZm9ybTogc2NhbGUoMC43NSkiPgogICAgICAgIDxwYXRoIGQ9Ik04LjUgMTMuNWwyLjUgMyAzLjUtNC41IDQuNSA2SDVtMTYgMVY1YTIgMiAwIDAgMC0yLTJINWMtMS4xIDAtMiAuOS0yIDJ2MTRjMCAxLjEuOSAyIDIgMmgxNGMxLjEgMCAyLS45IDItMnoiPjwvcGF0aD4KICAgICAgPC9zdmc+\",\"id\":\"iibe\"},\"type\":\"image\"}]",
                "styles": "[{\"droppable\":\".cell\",\"components\":[{\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"i2de\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]}],\"resizable\":{\"br\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":0},\"classes\":[{\"private\":1,\"name\":\"row\"},\"rrw2\"],\"name\":\"Row\",\"attributes\":{\"id\":\"icd7\"}},{\"droppable\":\".cell\",\"components\":[{\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"itlo\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]},{\"components\":[{\"components\":[{\"copyable\":false,\"_innertext\":false,\"draggable\":false,\"removable\":false,\"highlightable\":0,\"type\":\"textnode\",\"content\":\"This is a test\"},{\"copyable\":false,\"_innertext\":false,\"draggable\":false,\"removable\":false,\"classes\":[\"link\"],\"attributes\":{\"href\":\"\",\"id\":\"ibjv\"},\"highlightable\":0,\"type\":\"link\"}],\"attributes\":{\"id\":\"i5ni\"},\"type\":\"text\"}],\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"ipy8\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]}],\"resizable\":{\"br\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":0},\"classes\":[{\"private\":1,\"name\":\"row\"}],\"name\":\"Row\"},{\"attributes\":{\"src\":\"data:image\/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIHZpZXdCb3g9IjAgMCAyNCAyNCIgc3R5bGU9ImZpbGw6IHJnYmEoMCwwLDAsMC4xNSk7IHRyYW5zZm9ybTogc2NhbGUoMC43NSkiPgogICAgICAgIDxwYXRoIGQ9Ik04LjUgMTMuNWwyLjUgMyAzLjUtNC41IDQuNSA2SDVtMTYgMVY1YTIgMiAwIDAgMC0yLTJINWMtMS4xIDAtMiAuOS0yIDJ2MTRjMCAxLjEuOSAyIDIgMmgxNGMxLjEgMCAyLS45IDItMnoiPjwvcGF0aD4KICAgICAgPC9zdmc+\",\"id\":\"iibe\"},\"type\":\"image\"}]"
            },
            "markup": {
                "style-reference": "wcmrest:StyleSheetComponent/15da7680-3c37-4ded-86e1-5414136785f9",
                "markup": "<b>This is a sample set of mark up to test that transformer.</b>"
            }
        },
        "properties" :
        {
            "slug" : "theSiteV1"
        }
    }
}'

echo "Creating a DxSymbol"
curl -b cookie.txt -s  -o Symbol.txt -w "%{http_code}" --location --request POST 'http://INSTANCE_IP:10039/wps/mycontenthandler/!ut/p/digest!CWb11PoiCvMXKblug8zpmg/wcmrest-v2/containers/' \
--header 'Authorization: Basic d3BzYWRtaW46d3BzYWRtaW4=' \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=0000Qv8edYlR31Xp9F2qouMGoA5:-1; LtpaToken2=1wvOYgK01fEtMh8+GzmhPlj8pGF7+9zlRvGrTkuF0JMMaj6ifFZv2h4QuU7IDE+HjoiEBsu7Kd9DeW6bV+A31MlnDejc6b+Y+fRfOyDPmGA7WfcgPFJxSyb4KsjCNiUAmU+j7bpTeTIUcbPlu586HhyqTbiLeN7bsuO6+1Iq4WlWjXk9WbVcihVQSY7deNzMV3IA68xSbhSVkXtkLCGLGDjVhkLsAPfsoLLkAQU0LbhmwGh30YKuF40PjPK89QYwa2GcoZYKoSvidOMoXp43muDKNURwLFLz72Jk1/jkG5y05hbi4aLcjfwgwzBVSjpsP303WddTtYEkc4BqSwt1gF9p9Ug2cuiowdRsowKi4U4LsDz+IB+BF+ZZmdDTzz+4Aay+/+7GKT42B9opCH/KxUVE9CNVG4a8O2K9z/+xigvNJZ+vEIE2EXQJtk0yhE3h2+BO6l9YZ1Ts5YuMhcZYbd0awaMQybjuOK9JHGFQAVL/x7hPvcz9rBtzN5LvkE6fW4rosyNGYs/bbneZFebRvNG801XV9EqOUbULhcbOXGmzp3bCgvGwEFo2HMuPBr3GObnAs13JA/DGD8h7k1C3g+lAnRac+/1qoB84xUBKwv9IY8JqM3/qwCvW92jKXBXVMld48Mw7v1mvWSOVFRKp06NCGJxqSYxPOLnGgUUJ86M=' \
--data '{
    "title": {
        "lang": "en",
        "value": "DxSymbol"
    },
    "name": "DxSymbol",
    "type": "DxSymbol",
    "libraryID": "'"$SyndicationLibrary"'",
    "data": {
        "presentation": {
            "model": {
                "components": "[{\"droppable\":\".cell\",\"components\":[{\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"i2de\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]}],\"resizable\":{\"br\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":0},\"classes\":[{\"private\":1,\"name\":\"row\"},\"rrw2\"],\"name\":\"Row\",\"attributes\":{\"id\":\"icd7\"}},{\"droppable\":\".cell\",\"components\":[{\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"itlo\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]},{\"components\":[{\"components\":[{\"copyable\":false,\"_innertext\":false,\"draggable\":false,\"removable\":false,\"highlightable\":0,\"type\":\"textnode\",\"content\":\"This is a test\"},{\"copyable\":false,\"_innertext\":false,\"draggable\":false,\"removable\":false,\"classes\":[\"link\"],\"attributes\":{\"href\":\"\",\"id\":\"ibjv\"},\"highlightable\":0,\"type\":\"link\"}],\"attributes\":{\"id\":\"i5ni\"},\"type\":\"text\"}],\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"ipy8\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]}],\"resizable\":{\"br\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":0},\"classes\":[{\"private\":1,\"name\":\"row\"}],\"name\":\"Row\"},{\"attributes\":{\"src\":\"data:image\/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIHZpZXdCb3g9IjAgMCAyNCAyNCIgc3R5bGU9ImZpbGw6IHJnYmEoMCwwLDAsMC4xNSk7IHRyYW5zZm9ybTogc2NhbGUoMC43NSkiPgogICAgICAgIDxwYXRoIGQ9Ik04LjUgMTMuNWwyLjUgMyAzLjUtNC41IDQuNSA2SDVtMTYgMVY1YTIgMiAwIDAgMC0yLTJINWMtMS4xIDAtMiAuOS0yIDJ2MTRjMCAxLjEuOSAyIDIgMmgxNGMxLjEgMCAyLS45IDItMnoiPjwvcGF0aD4KICAgICAgPC9zdmc+\",\"id\":\"iibe\"},\"type\":\"image\"}]",
                "styles": "[{\"droppable\":\".cell\",\"components\":[{\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"i2de\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]}],\"resizable\":{\"br\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":0},\"classes\":[{\"private\":1,\"name\":\"row\"},\"rrw2\"],\"name\":\"Row\",\"attributes\":{\"id\":\"icd7\"}},{\"droppable\":\".cell\",\"components\":[{\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"itlo\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]},{\"components\":[{\"components\":[{\"copyable\":false,\"_innertext\":false,\"draggable\":false,\"removable\":false,\"highlightable\":0,\"type\":\"textnode\",\"content\":\"This is a test\"},{\"copyable\":false,\"_innertext\":false,\"draggable\":false,\"removable\":false,\"classes\":[\"link\"],\"attributes\":{\"href\":\"\",\"id\":\"ibjv\"},\"highlightable\":0,\"type\":\"link\"}],\"attributes\":{\"id\":\"i5ni\"},\"type\":\"text\"}],\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"ipy8\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]}],\"resizable\":{\"br\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":0},\"classes\":[{\"private\":1,\"name\":\"row\"}],\"name\":\"Row\"},{\"attributes\":{\"src\":\"data:image\/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIHZpZXdCb3g9IjAgMCAyNCAyNCIgc3R5bGU9ImZpbGw6IHJnYmEoMCwwLDAsMC4xNSk7IHRyYW5zZm9ybTogc2NhbGUoMC43NSkiPgogICAgICAgIDxwYXRoIGQ9Ik04LjUgMTMuNWwyLjUgMyAzLjUtNC41IDQuNSA2SDVtMTYgMVY1YTIgMiAwIDAgMC0yLTJINWMtMS4xIDAtMiAuOS0yIDJ2MTRjMCAxLjEuOSAyIDIgMmgxNGMxLjEgMCAyLS45IDItMnoiPjwvcGF0aD4KICAgICAgPC9zdmc+\",\"id\":\"iibe\"},\"type\":\"image\"}]"
            },
            "markup": {
                "style-reference": "wcmrest:LibraryStyleSheetComponent/15da7680-3c37-4ded-86e1-5414136785f9",
                "markup": "<b>This is a sample set of mark up to test that transformer.</b>"
            }
        },
        "properties": {
            "slug": "theSite"
        }
    }
}'

sleep 200

echo "Verifying if Site type is succesfully syndicated"
curl -b cookie.txt -s  -o SiteVP.txt -w "%{http_code}" --location --request GET "http://INSTANCE_IP:10039/wps/mycontenthandler/vp4/"'!'"ut/p/digest"'!'"CWb11PoiCvMXKblug8zpmg/wcmrest-v2/content-sites/$SiteID" \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=000045dibNHXRabRpPXK09BMLJN:-1; LtpaToken2=wjWfRG0zynIzFQiEqPwnaq36NOq8tiDi2qVIUgcoHUTrsgbcq2lw+2VuV+OOOHUGmyVEGNl2L0uRw3MKZIOsGPOcWeiXxxX+4CNXZ58pdJh07aUG4/SYAOymBMM97CJZXJR6SaIBAkgh9q4w8fD2sk7inPyYZzoNvH8o+4/HYdWJOaWYZTc41+4iiq0evxbWNJwH0hc84y4yX4bcWuTIpiB9QugwpQaQ/JSiz1Y4xf/mrOxwwpKzQ9pawLUzuRrmlQzHjn7dstkn6oc8UY6eiow32EbVXp9z3t6CveKVcVZYJSzHEMJq6fzYOqFNwnTcn+J3gFFwPCsYs9O37VZV3+6dhRm4tlK6bcqMdbiWAKREr6xdOpIjRAf5NQzEh5osBTw4NOTZVyPxFvMGaNEL8a2BoTRjq0P5/GUFujFcNW1ytzAFU7i6kEHkbq577ecqwbltF1UkQTZtWC/wTstjFigR8ndbn6vKP9DXelknfTVyi6+VkzjGGSHb5CRPmTiDrlU9TbVgBSU35x6+wrq4rCV+BjDT427Foechnrc7hpFBzZEImEEM+CI4x6oLNG8No4USJajkmv4PvWe81I3sYi5H9r4plHljRe5j0r6ihWUhV8Lu1kAZXNwJ7lal0m77cP/5O5qZsO2gyPp2wowTRxjOWRBEltYCi0i+PljwE90=' \
--data '{}' 

echo "Verifying if Page type is succesfully syndicated"
curl -b cookie.txt -s  -o PageVP.txt -w "%{http_code}" --location --request GET 'http://INSTANCE_IP:10039/wps/mycontenthandler/vp4/!ut/p/wcmrest-v2/search?type=DxContentPage' \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=000045dibNHXRabRpPXK09BMLJN:-1; LtpaToken2=wjWfRG0zynIzFQiEqPwnaq36NOq8tiDi2qVIUgcoHUTrsgbcq2lw+2VuV+OOOHUGmyVEGNl2L0uRw3MKZIOsGPOcWeiXxxX+4CNXZ58pdJh07aUG4/SYAOymBMM97CJZXJR6SaIBAkgh9q4w8fD2sk7inPyYZzoNvH8o+4/HYdWJOaWYZTc41+4iiq0evxbWNJwH0hc84y4yX4bcWuTIpiB9QugwpQaQ/JSiz1Y4xf/mrOxwwpKzQ9pawLUzuRrmlQzHjn7dstkn6oc8UY6eiow32EbVXp9z3t6CveKVcVZYJSzHEMJq6fzYOqFNwnTcn+J3gFFwPCsYs9O37VZV3+6dhRm4tlK6bcqMdbiWAKREr6xdOpIjRAf5NQzEh5osBTw4NOTZVyPxFvMGaNEL8a2BoTRjq0P5/GUFujFcNW1ytzAFU7i6kEHkbq577ecqwbltF1UkQTZtWC/wTstjFigR8ndbn6vKP9DXelknfTVyi6+VkzjGGSHb5CRPmTiDrlU9TbVgBSU35x6+wrq4rCV+BjDT427Foechnrc7hpFBzZEImEEM+CI4x6oLNG8No4USJajkmv4PvWe81I3sYi5H9r4plHljRe5j0r6ihWUhV8Lu1kAZXNwJ7lal0m77cP/5O5qZsO2gyPp2wowTRxjOWRBEltYCi0i+PljwE90=' \
--data '{}' 

cat PageVP.txt | jq -e 'if .total == "2" then "DxContentPage type is successfully syndicated" else error("Failed to syndicate DxContentPage") end'

echo "Verifying if DxSymbol is succesfully syndicated"
curl -b cookie.txt -s  -o SymbolVP.txt -w "%{http_code}" --location --request GET 'http://INSTANCE_IP:10039/wps/mycontenthandler/vp4/!ut/p/wcmrest-v2/search?type=DxSymbol' \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=000045dibNHXRabRpPXK09BMLJN:-1; LtpaToken2=wjWfRG0zynIzFQiEqPwnaq36NOq8tiDi2qVIUgcoHUTrsgbcq2lw+2VuV+OOOHUGmyVEGNl2L0uRw3MKZIOsGPOcWeiXxxX+4CNXZ58pdJh07aUG4/SYAOymBMM97CJZXJR6SaIBAkgh9q4w8fD2sk7inPyYZzoNvH8o+4/HYdWJOaWYZTc41+4iiq0evxbWNJwH0hc84y4yX4bcWuTIpiB9QugwpQaQ/JSiz1Y4xf/mrOxwwpKzQ9pawLUzuRrmlQzHjn7dstkn6oc8UY6eiow32EbVXp9z3t6CveKVcVZYJSzHEMJq6fzYOqFNwnTcn+J3gFFwPCsYs9O37VZV3+6dhRm4tlK6bcqMdbiWAKREr6xdOpIjRAf5NQzEh5osBTw4NOTZVyPxFvMGaNEL8a2BoTRjq0P5/GUFujFcNW1ytzAFU7i6kEHkbq577ecqwbltF1UkQTZtWC/wTstjFigR8ndbn6vKP9DXelknfTVyi6+VkzjGGSHb5CRPmTiDrlU9TbVgBSU35x6+wrq4rCV+BjDT427Foechnrc7hpFBzZEImEEM+CI4x6oLNG8No4USJajkmv4PvWe81I3sYi5H9r4plHljRe5j0r6ihWUhV8Lu1kAZXNwJ7lal0m77cP/5O5qZsO2gyPp2wowTRxjOWRBEltYCi0i+PljwE90=' \
--data '{}' 

cat SymbolVP.txt | jq -e 'if .total == "1" then "DxSymbol type is successfully syndicated" else error("Failed to syndicate DxSymbol") end'
