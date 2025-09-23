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

echo "Updating the name of a Content Item (surfCity - Surf Shop - Products - Boards - Short Boards - Soap)"
curl -b cookie.txt  -o Update.txt -w "%{http_code}" --location --request PUT "http://${SYNDICATOR_IP}:10039/wps/mycontenthandler/wcmrest/Content/bb3725d3-3886-4ed0-9f03-510ceafd087e/" \
--header 'Authorization: Basic d3BzYWRtaW46d3BzYWRtaW4=' \
--header 'Content-Type: application/atom+xml' \
--data '<atom:entry xmlns:atom="http://www.w3.org/2005/Atom" xmlns:wcm="wcm/namespace">
    <wcm:name>Updated Soap Name</wcm:name>
    <atom:title>Updated Soap Title</atom:title>
</atom:entry>'

echo "Moving a Content Item (surfCity - Proucts - Kayaks - Touring Kayaks - Touring Kayak)"
curl -b cookie.txt  -o CIUpdate.txt -w "%{http_code}" --location --request PUT "http://${SYNDICATOR_IP}:10039/wps/mycontenthandler/wcmrest/Content/3be09f77-2ffb-4541-803e-8b5c3f4e3f3a/" \
--header 'Authorization: Basic d3BzYWRtaW46d3BzYWRtaW4=' \
--header 'Content-Type: application/atom+xml' \
--data '<atom:entry xmlns:atom="http://www.w3.org/2005/Atom" xmlns:wcm="wcm/namespace">
    <wcm:name>Touring Kayak Moved</wcm:name>
    <atom:title>Tourinng Kayak Moved</atom:title>
    <atom:link atom:rel="parent" atom:href="/wps/mycontenthandler/!ut/p/digest!AsAbNWiSYLAgoLwvrtZVwQ/wcmrest/SiteArea/a80c0ff0-feb2-4253-8652-8ec7f4635dec"/>
</atom:entry>'

echo "Deleting a Content Item (surfCity - Products - Boards - Long Boards - Long Board) from the syndicator"
curl -b cookie.txt  -o Delete.txt -w "%{http_code}" --location --request DELETE "http://${SYNDICATOR_IP}:10039/wps/mycontenthandler/wcmrest/Content/7552b375-8ea6-4304-9606-6bfc6cca202f/" \
--header 'Authorization: Basic d3BzYWRtaW46d3BzYWRtaW4=' \
--header 'Content-Type: application/atom+xml' \
--data '<atom:entry xmlns:atom="http://www.w3.org/2005/Atom" xmlns:wcm="wcm/namespace">
</atom:entry>'

echo "Updating the default content for surfCity - Products"
curl -b cookie.txt  -o DCUpdate.txt -w "%{http_code}" --location --request PUT "http://${SYNDICATOR_IP}:10039/wps/mycontenthandler/wcmrest/SiteArea/384830c0-4706-436d-afa1-8d725658361a/" \
--header 'Authorization: Basic d3BzYWRtaW46d3BzYWRtaW4=' \
--header 'Content-Type: application/atom+xml' \
--data '<atom:entry xmlns:atom="http://www.w3.org/2005/Atom" xmlns:wcm="wcm/namespace">
    <atom:link atom:rel="default-content" atom:href="/wps/mycontenthandler/!ut/p/digest!5d4JDXp7pplOKpQvF1ibcA/wcmrest/Content/6ac87ef8-de68-4df7-af6e-7b897d9159ac" xml:lang="en" label="Default Content"/>
</atom:entry>'

echo "Creating a copy of the surfCity - Boards - Fun Board content item"
curl -g -b cookie.txt  -o FunBoardCopyCreate.txt -w "%{http_code}" --location --request POST "http://${SYNDICATOR_IP}:10039/wps/mycontenthandler/wcmrest/Content" \
--header 'Authorization: Basic d3BzYWRtaW46d3BzYWRtaW4=' \
--header 'Content-Type: application/atom+xml' \
--data '<?xml version="1.0" encoding="UTF-8"?><entry xmlns="http://www.w3.org/2005/Atom" xmlns:wcm="http://www.ibm.com/xmlns/wcm/8.0">
    <title xml:lang="en">FunBoardCopy</title>
    <summary xml:lang="en"></summary>
    <wcm:name>FunBoardCopy</wcm:name>
    <wcm:type>Content</wcm:type>
    <updated>2022-05-04T18: 03: 28.472Z</updated>
    <published>2022-04-11T10: 50: 02.965Z</published>
    <wcm:created>2022-04-11T10: 50: 02.565Z</wcm:created>
    <author>
        <wcm:distinguishedName>uid=wpsadmin,o=defaultWIMFileBasedRealm</wcm:distinguishedName>
        <uri>/wps/mycontenthandler/!ut/p/digest!Yo9FjUxgXWAJzRfxu8w2cg/um/users/profiles/Z9eAeI1DA3QG6L9P6MMK65JPGJMG623CAJM072JC2MMGCH9O46OC6MHDCJQ86M1</uri>
        <name>wpsadmin</name>
        <type>USER</type>
    </author>
    <wcm:owner>
        <wcm:distinguishedName>uid=wpsadmin,o=defaultWIMFileBasedRealm</wcm:distinguishedName>
        <uri>/wps/mycontenthandler/!ut/p/digest!Yo9FjUxgXWAJzRfxu8w2cg/um/users/profiles/Z9eAeI1DA3QG6L9P6MMK65JPGJMG623CAJM072JC2MMGCH9O46OC6MHDCJQ86M1</uri>
        <name>wpsadmin</name>
        <type>USER</type>
    </wcm:owner>
    <wcm:lastModifier>
        <wcm:distinguishedName>uid=wpsadmin,o=defaultWIMFileBasedRealm</wcm:distinguishedName>
        <uri>/wps/mycontenthandler/!ut/p/digest!Yo9FjUxgXWAJzRfxu8w2cg/um/users/profiles/Z9eAeI1DA3QG6L9P6MMK65JPGJMG623CAJM072JC2MMGCH9O46OC6MHDCJQ86M1</uri>
        <name>wpsadmin</name>
        <type>USER</type>
    </wcm:lastModifier>
    <wcm:creator>
        <wcm:distinguishedName>uid=wpsadmin,o=defaultWIMFileBasedRealm</wcm:distinguishedName>
        <uri>/wps/mycontenthandler/!ut/p/digest!Yo9FjUxgXWAJzRfxu8w2cg/um/users/profiles/Z9eAeI1DA3QG6L9P6MMK65JPGJMG623CAJM072JC2MMGCH9O46OC6MHDCJQ86M1</uri>
        <name>wpsadmin</name>
        <type>USER</type>
    </wcm:creator>
    <wcm:profile/>
    <wcm:workflow>
        <wcm:publishDate>2022-04-11T10: 50: 02.560Z</wcm:publishDate>
    </wcm:workflow>
    <link rel="self" href="/wps/mycontenthandler/!ut/p/digest!Yo9FjUxgXWAJzRfxu8w2cg/wcmrest/Content/3465d4a5-8d3d-4ab6-bf25-d216b75b6b6c" xml:lang="en" label="Read"/>
    <link rel="edit" href="/wps/mycontenthandler/!ut/p/digest!Yo9FjUxgXWAJzRfxu8w2cg/wcmrest/Content/3465d4a5-8d3d-4ab6-bf25-d216b75b6b6c" xml:lang="en" label="Edit"/>
    <link rel="delete" href="/wps/mycontenthandler/!ut/p/digest!Yo9FjUxgXWAJzRfxu8w2cg/wcmrest/Content/3465d4a5-8d3d-4ab6-bf25-d216b75b6b6c" xml:lang="en" label="Delete"/>
    <link rel="workflow-stage" href="/wps/mycontenthandler/!ut/p/digest!Yo9FjUxgXWAJzRfxu8w2cg/wcmrest/WorkflowStage/6a2f1538-425d-44e0-ab23-0229506371ca" xml:lang="en" label="Workflow Stage"/>
    <link rel="restart" href="/wps/mycontenthandler/!ut/p/digest!Yo9FjUxgXWAJzRfxu8w2cg/wcmrest/item/3465d4a5-8d3d-4ab6-bf25-d216b75b6b6c/restart" xml:lang="en" label="Restart"/>
    <link rel="previous-stage" href="/wps/mycontenthandler/!ut/p/digest!Yo9FjUxgXWAJzRfxu8w2cg/wcmrest/item/3465d4a5-8d3d-4ab6-bf25-d216b75b6b6c/previous-stage" xml:lang="en" label="Previous Stage"/>
    <link rel="create-draft" href="/wps/mycontenthandler/!ut/p/digest!Yo9FjUxgXWAJzRfxu8w2cg/wcmrest/item/3465d4a5-8d3d-4ab6-bf25-d216b75b6b6c/create-draft" xml:lang="en" label="Create Draft"/>
    <link rel="workflow" href="/wps/mycontenthandler/!ut/p/digest!Yo9FjUxgXWAJzRfxu8w2cg/wcmrest/Workflow/8280161d-9664-4007-8921-f1b5856923dc" xml:lang="en" label="Workflow"/>
    <link rel="access-control" href="/wps/mycontenthandler/!ut/p/digest!Yo9FjUxgXWAJzRfxu8w2cg/ac/access:oid:Z6QReDeJ1DCJQGCK9OAJM074RC8MMG61JOCJM8C6JCAJMGCI9CC3HT6LHOC3HP633" xml:lang="en" label="Access Control"/>
    <link rel="library" href="/wps/mycontenthandler/!ut/p/digest!Yo9FjUxgXWAJzRfxu8w2cg/wcmrest/Library/055cb75f-2686-46b6-947a-4afb4420902b" xml:lang="en" label="Library"/>
    <link rel="parent" href="/wps/mycontenthandler/!ut/p/digest!Yo9FjUxgXWAJzRfxu8w2cg/wcmrest/SiteArea/cb758bbf-4544-4bf2-82c8-3fa050eaa86b" xml:lang="en" label="Parent"/>
    <link rel="versions" href="/wps/mycontenthandler/!ut/p/digest!Yo9FjUxgXWAJzRfxu8w2cg/wcmrest/item/3465d4a5-8d3d-4ab6-bf25-d216b75b6b6c/versions" xml:lang="en" label="Versions"/>
    <link rel="preview" href="/wps/poc/!ut/p/digest!Yo9FjUxgXWAJzRfxu8w2cg/wcm/oid:3465d4a5-8d3d-4ab6-bf25-d216b75b6b6c" xml:lang="en" label="Preview"/>
    <link rel="edit-media" href="/wps/mycontenthandler/!ut/p/digest!Yo9FjUxgXWAJzRfxu8w2cg/wcmrest/Content/3465d4a5-8d3d-4ab6-bf25-d216b75b6b6c" type="application/vnd.ibm.wcm+xml" xml:lang="en" label="Edit Media"/>
    <link rel="content-template" href="/wps/mycontenthandler/!ut/p/digest!Yo9FjUxgXWAJzRfxu8w2cg/wcmrest/ContentTemplate/e25b9392-3594-435e-979b-6c76bda40b75" xml:lang="en" label="Content Template"/>
    <link rel="elements" href="/wps/mycontenthandler/!ut/p/digest!Yo9FjUxgXWAJzRfxu8w2cg/wcmrest/Content/3465d4a5-8d3d-4ab6-bf25-d216b75b6b6c/elements" xml:lang="en" label="Elements"/>
    <category scheme="wcmrest:favorite" term="false" xml:lang="en"/>
    <category scheme="wcmrest:locked" term="true" label="uid=wpsadmin,o=defaultWIMFileBasedRealm"/>
    <content type="application/vnd.ibm.wcm+xml">
        <wcm:content xmlns="http://www.ibm.com/xmlns/wcm/8.0">
            <elements xmlns:atom="http://www.w3.org/2005/Atom">
                <element name="Name">
                    <title xml:lang="en">Name</title>
                    <type>ShortTextComponent</type>
                    <data type="text/plain">Fun Board</data>
                </element>
                <element name="Summary">
                    <title xml:lang="en">Summary</title>
                    <type>TextComponent</type>
                    <data type="text/plain">The favorite type of board between beginners and heavier surfers.</data>
                </element>
                <element name="Body">
                    <title xml:lang="en">Body</title>
                    <type>RichTextComponent</type>
                    <data type="text/html"></data>
                </element>
                <element name="length">
                    <title xml:lang="en">length</title>
                    <type>ShortTextComponent</type>
                    <data type="text/plain"/>
                </element>
                <element name="liters">
                    <title xml:lang="en">liters</title>
                    <type>ShortTextComponent</type>
                    <data type="text/plain"/>
                </element>
                <element name="PreBody">
                    <title xml:lang="en">PreBody</title>
                    <type>ReferenceComponent</type>
                    <data type="application/vnd.ibm.wcm+xml">
                        <reference></reference>
                    </data>
                </element>
                <element name="PostBody">
                    <title xml:lang="en">PostBody</title>
                    <type>ReferenceComponent</type>
                    <data type="application/vnd.ibm.wcm+xml">
                        <reference>/wps/mycontenthandler/!ut/p/digest!Yo9FjUxgXWAJzRfxu8w2cg/wcmrest/LibraryMenuComponent/32f84b50-fe37-4e22-a4e6-b250b9026b3a</reference>
                    </data>
                </element>
                <element name="Image1">
                    <title xml:lang="en">Image1</title>
                    <type>ImageComponent</type>
                    <data type="application/vnd.ibm.wcm+xml">
                        <image>
                            <dimension height="" width="" border=""/>
                            <altText></altText>
                            <tagName></tagName>
                            <renditionList/>
                        </image>
                    </data>
                </element>
                <element name="image2">
                    <title xml:lang="en">image2</title>
                    <type>ImageComponent</type>
                    <data type="application/vnd.ibm.wcm+xml">
                        <image>
                            <dimension height="" width="" border=""/>
                            <altText></altText>
                            <tagName></tagName>
                            <renditionList/>
                        </image>
                    </data>
                </element>
                <element name="link">
                    <title xml:lang="en">link</title>
                    <type>LinkComponent</type>
                    <data type="application/vnd.ibm.wcm+xml">
                        <linkElement>
                            <destination type="external" allowClear="false"></destination>
                            <display type="title"></display>
                            <description useDestination="false"></description>
                            <target>None</target>
                            <additionalAttributes></additionalAttributes>
                        </linkElement>
                    </data>
                </element>
            </elements>
        </wcm:content>
    </content>
</entry>'

sleep 300

# echo "Updating the DxContentPage The Palace Hotel - Home"
# curl -b cookie.txt  -o UpdateContentPage.txt -w "%{http_code}" --location --request PUT "http://${SYNDICATOR_IP}:10039/wps/mycontenthandler/wcmrest-v2/content-pages/dd9af8c2-2136-4fb0-a9c1-9734466f0cc4/" \
# --header 'Authorization: Basic d3BzYWRtaW46d3BzYWRtaW4=' \
# --header 'Content-Type: application/atom+xml' \
# --data '{
#     "id": "dd9af8c2-2136-4fb0-a9c1-9734466f0cc4",
#     "title": {
#       "lang": "en",
#       "value": "HomeUpdated_Test"
#     },
#     "name": "HomeUpdated_Test",
#     "type": "DxContentPage",
#     "created": "Mon, 19 Jul 2021 10:17:54.499Z",
#     "lastModifier": {
#       "distinguishedName": "uid=wpsadmin,o=defaultWIMFileBasedRealm",
#       "uri": "/wps/mycontenthandler/!ut/p/digest!ewe1e4gVzc-wZV9seR3JnQ/um/users/profiles/Z9eAeI1DA3QG6L9P6MMK65JPGJMG623CAJM072JC2MMGCH9O46OC6MHDCJQ86M1",
#       "name": "wpsadmin",
#       "type": "USER"
#     },
#     "creator": {
#       "distinguishedName": "uid=wpsadmin,o=defaultWIMFileBasedRealm",
#       "uri": "/wps/mycontenthandler/!ut/p/digest!ewe1e4gVzc-wZV9seR3JnQ/um/users/profiles/Z9eAeI1DA3QG6L9P6MMK65JPGJMG623CAJM072JC2MMGCH9O46OC6MHDCJQ86M1",
#       "name": "wpsadmin",
#       "type": "USER"
#     },
#     "profile": {},
#     "status": "PUBLISHED",
#     "lastModified": "Tue, 26 Apr 2022 08:25:34.982Z",
#     "libraryID": "ded26275-c13e-42a8-a20c-8a0dca96a57a",
#     "parentID": "1c89340f-ba6c-45d6-bc19-efa1ae6a8f4a",
#     "siteID": "1c89340f-ba6c-45d6-bc19-efa1ae6a8f4a",
#     "isSite": "false",
#     "lock": {
#       "isLocked": "false"
#     },
#     "history": {
#       "entries": [
#         {
#           "date": "Tue, 26 Apr 2022 08:25:34.982Z",
#           "name": "wpsadmin",
#           "message": "Document updated by wpsadmin"
#         }
#       ]
#     },
#     "links": [
#       {
#         "rel": "self",
#         "href": "/wps/mycontenthandler/!ut/p/digest!ewe1e4gVzc-wZV9seR3JnQ/wcmrest-v2/content-pages/dd9af8c2-2136-4fb0-a9c1-9734466f0cc4",
#         "lang": "en",
#         "label": "Read"
#       },
#       {
#         "rel": "edit",
#         "href": "/wps/mycontenthandler/!ut/p/digest!ewe1e4gVzc-wZV9seR3JnQ/wcmrest-v2/content-pages/dd9af8c2-2136-4fb0-a9c1-9734466f0cc4",
#         "lang": "en",
#         "label": "Edit"
#       },
#       {
#         "rel": "delete",
#         "href": "/wps/mycontenthandler/!ut/p/digest!ewe1e4gVzc-wZV9seR3JnQ/wcmrest-v2/content-pages/dd9af8c2-2136-4fb0-a9c1-9734466f0cc4",
#         "lang": "en",
#         "label": "Delete"
#       },
#       {
#         "rel": "edit-media",
#         "href": "/wps/mycontenthandler/!ut/p/digest!ewe1e4gVzc-wZV9seR3JnQ/wcmrest-v2/content-pages/dd9af8c2-2136-4fb0-a9c1-9734466f0cc4",
#         "type": "application/vnd.ibm.wcm+xml",
#         "lang": "en",
#         "label": "Edit Media"
#       }
#     ],
#     "data": {
#       "presentation": {
#         "model": {
#           "components": "[{\"components\":[{\"classes\":[{\"private\":1,\"name\":\"dx-content-container\"}],\"attributes\":{\"start\":\"<div class='' id='ig0gn'>\",\"end\":\"<\\/div>\",\"id\":\"ig0gn\",\"uuid\":\"d90ad4a4-bb0d-4a0e-8e94-3c72c499fe42\"},\"type\":\"wcm-content-container\"},{\"classes\":[{\"private\":1,\"name\":\"dx-content-container\"}],\"attributes\":{\"start\":\"<div class='' id='ipenty'>\",\"end\":\"<\\/div>\",\"id\":\"ipenty\",\"uuid\":\"59dd6bcd-b254-4095-8ee3-24e59cf51454\"},\"type\":\"wcm-content-container\"},{\"components\":[{\"classes\":[{\"private\":1,\"name\":\"dx-content-container\"}],\"attributes\":{\"start\":\"<div class='' id='i7vil'>\",\"end\":\"<\\/div>\",\"id\":\"i7vil\",\"uuid\":\"59d356ff-bd95-4dd7-b468-ba6b0851b596\"},\"type\":\"wcm-content-container\"},{\"components\":[{\"components\":[{\"components\":[{\"classes\":[{\"private\":1,\"name\":\"dx-content-container\"},\"gallery-item\"],\"attributes\":{\"start\":\"<div class='gallery-item' id='ilc5d'>\",\"end\":\"<\\/div>\",\"id\":\"ilc5d\",\"uuid\":\"bce7a706-dcab-48c5-abb0-23e4d5ae6959\"},\"type\":\"wcm-content-container\"}],\"classes\":[{\"private\":1,\"name\":\"dx-div\"},\"gallery-tile\"],\"type\":\"Div\"},{\"components\":[{\"classes\":[{\"private\":1,\"name\":\"dx-content-container\"},\"gallery-item\"],\"attributes\":{\"start\":\"<div class='gallery-item' id='i88a7'>\",\"end\":\"<\\/div>\",\"id\":\"i88a7\",\"uuid\":\"b445a480-5c2e-42a5-8647-49f3251d7102\"},\"type\":\"wcm-content-container\"}],\"classes\":[{\"private\":1,\"name\":\"dx-div\"},\"gallery-tile\"],\"type\":\"Div\"}],\"classes\":[{\"private\":1,\"name\":\"dx-div\"},\"gallery-col\"],\"type\":\"Div\"},{\"components\":[{\"components\":[{\"classes\":[{\"private\":1,\"name\":\"dx-content-container\"},\"gallery-item\"],\"attributes\":{\"start\":\"<div class='gallery-item' id='i5pyhn'>\",\"end\":\"<\\/div>\",\"id\":\"i5pyhn\",\"uuid\":\"dc7398db-1814-4d13-9a7b-94d1636849fb\"},\"type\":\"wcm-content-container\"}],\"classes\":[{\"private\":1,\"name\":\"dx-div\"},\"gallery-tile\",\"large-tile\"],\"type\":\"Div\"}],\"classes\":[{\"private\":1,\"name\":\"dx-div\"},\"gallery-col\"],\"type\":\"Div\"},{\"components\":[{\"components\":[{\"classes\":[{\"private\":1,\"name\":\"dx-content-container\"},\"gallery-item\"],\"attributes\":{\"start\":\"<div class='gallery-item' id='iuzz9a'>\",\"end\":\"<\\/div>\",\"id\":\"iuzz9a\",\"uuid\":\"315f06ea-4cce-4f5b-b11c-3d206d0da6ad\"},\"type\":\"wcm-content-container\"}],\"classes\":[{\"private\":1,\"name\":\"dx-div\"},\"gallery-tile\"],\"type\":\"Div\"},{\"components\":[{\"classes\":[{\"private\":1,\"name\":\"dx-content-container\"},\"gallery-item\"],\"attributes\":{\"start\":\"<div class='gallery-item' id='iuqjku'>\",\"end\":\"<\\/div>\",\"id\":\"iuqjku\",\"uuid\":\"fdbf4e74-cafc-4bb9-81a3-5c7eaf8983e7\"},\"type\":\"wcm-content-container\"}],\"classes\":[{\"private\":1,\"name\":\"dx-div\"},\"gallery-tile\"],\"type\":\"Div\"}],\"classes\":[{\"private\":1,\"name\":\"dx-div\"},\"gallery-col\"],\"type\":\"Div\"},{\"components\":[{\"components\":[{\"classes\":[{\"private\":1,\"name\":\"dx-content-container\"},\"gallery-item\"],\"attributes\":{\"start\":\"<div class='gallery-item' id='ivb1dc'>\",\"end\":\"<\\/div>\",\"id\":\"ivb1dc\",\"uuid\":\"0df3998b-2ecf-4863-ada4-25eb263c0a5e\"},\"type\":\"wcm-content-container\"}],\"classes\":[{\"private\":1,\"name\":\"dx-div\"},\"gallery-tile\"],\"type\":\"Div\"},{\"components\":[{\"classes\":[{\"private\":1,\"name\":\"dx-content-container\"},\"gallery-item\"],\"attributes\":{\"start\":\"<div class='gallery-item' id='ii3vsu'>\",\"end\":\"<\\/div>\",\"id\":\"ii3vsu\",\"uuid\":\"d239ab85-7338-4a22-949d-b9dc7505f9f5\"},\"type\":\"wcm-content-container\"}],\"classes\":[{\"private\":1,\"name\":\"dx-div\"},\"gallery-tile\"],\"type\":\"Div\"}],\"classes\":[{\"private\":1,\"name\":\"dx-div\"},\"gallery-col\"],\"type\":\"Div\"}],\"classes\":[{\"private\":1,\"name\":\"dx-div\"},\"gallery\"],\"attributes\":{\"id\":\"iawrt\"},\"type\":\"Div\"}],\"classes\":[{\"private\":1,\"name\":\"dx-section-layout\"}],\"attributes\":{\"id\":\"irlpcw\"},\"type\":\"Section\",\"custom-name\":\"Section\"},{\"components\":[{\"components\":[{\"components\":[{\"classes\":[{\"private\":1,\"name\":\"dx-content-container\"}],\"attributes\":{\"start\":\"<div class='' id='ivuwzg'>\",\"end\":\"<\\/div>\",\"id\":\"ivuwzg\",\"uuid\":\"f8ed20f9-e912-4546-8578-ea1372621762\"},\"type\":\"wcm-content-container\"}],\"classes\":[{\"private\":1,\"name\":\"dx-columns\"},\"feature-col\"],\"attributes\":{\"id\":\"ir7wk5\"},\"type\":\"Columns\"},{\"components\":[{\"classes\":[{\"private\":1,\"name\":\"dx-content-container\"}],\"attributes\":{\"start\":\"<div class='' id='iw0tfy'>\",\"end\":\"<\\/div>\",\"id\":\"iw0tfy\",\"uuid\":\"6cf083df-8877-4e6e-8e72-a2a59c80c5b7\"},\"type\":\"wcm-content-container\"}],\"classes\":[{\"private\":1,\"name\":\"dx-columns\"},\"feature-col\"],\"attributes\":{\"id\":\"ipgceo\"},\"type\":\"Columns\"}],\"classes\":[{\"private\":1,\"name\":\"dx-column-layout\"},\"feature-row\"],\"attributes\":{\"id\":\"igfcse\"},\"type\":\"Column\"}],\"classes\":[{\"private\":1,\"name\":\"dx-section-layout\"},\"featured\"],\"attributes\":{\"id\":\"ihhtd\"},\"type\":\"Section\"},{\"classes\":[{\"private\":1,\"name\":\"dx-content-container\"}],\"attributes\":{\"start\":\"<div class='' id='iv34ll'>\",\"end\":\"<\\/div>\",\"id\":\"iv34ll\",\"uuid\":\"ca88dd58-23c1-4992-b9a1-3dbbe89335b7\"},\"type\":\"wcm-content-container\"}],\"attributes\":{\"id\":\"dx-body-dd9af8c2-2136-4fb0-a9c1-9734466f0cc4\"},\"type\":\"wrapper\"}]",
#           "styles": "[]"
#         },
#         "markup": {
#           "style-reference": "/wps/mycontenthandler/!ut/p/digest!ewe1e4gVzc-wZV9seR3JnQ/wcmrest/LibraryStyleSheetComponent/97799dd1-8382-4402-9c2e-bae8ca7b7c99",
#           "markup": "<body id=\"dx-body-dd9af8c2-2136-4fb0-a9c1-9734466f0cc4\" >[DxContainer htmlid=\"ig0gn\" id=\"d90ad4a4-bb0d-4a0e-8e94-3c72c499fe42:OC9kZXNpZ24gc3R1ZGlvIGRlbW8vdGhlIHBhbGFjZSBob3RlbC9ob21lL3BhbGFjZSBob3RlbCBoZXJvIGNvbnRhaW5lcg==\" htmlencode=\"true\" name=\"design studio demo/the palace hotel/home/palace hotel hero container\" type=\"design studio demo/the palace hotel/home/palace hotel hero container\" start=\"<div class='' id='ig0gn'>\" end=\"</div>\"][DxContainer htmlid=\"ipenty\" id=\"59dd6bcd-b254-4095-8ee3-24e59cf51454:OC9kZXNpZ24gc3R1ZGlvIGRlbW8vdGhlIHBhbGFjZSBob3RlbC9ob21lL3BhbGFjZSBob3RlbCBkZXRhaWxzIGNvbnRhaW5lcg==\" htmlencode=\"true\" name=\"design studio demo/the palace hotel/home/palace hotel details container\" type=\"design studio demo/the palace hotel/home/palace hotel details container\" start=\"<div class='' id='ipenty'>\" end=\"</div>\"]<section id=\"irlpcw\"  class=\"dx-section-layout\">[DxContainer htmlid=\"i7vil\" id=\"59d356ff-bd95-4dd7-b468-ba6b0851b596:OC9kZXNpZ24gc3R1ZGlvIGRlbW8vdGhlIHBhbGFjZSBob3RlbC9ob21lL2xhYmVsIC0gdGhlIHBhbGFjZSBob3RlbCBnYWxsZXJ5IGNvbnRhaW5lcg==\" htmlencode=\"true\" name=\"design studio demo/the palace hotel/home/label - the palace hotel gallery container\" type=\"design studio demo/the palace hotel/home/label - the palace hotel gallery container\" start=\"<div class='' id='i7vil'>\" end=\"</div>\"]<div id=\"iawrt\"  class=\"dx-div gallery\"><div  class=\"dx-div gallery-col\"><div  class=\"dx-div gallery-tile\">[DxContainer htmlid=\"ilc5d\" id=\"bce7a706-dcab-48c5-abb0-23e4d5ae6959:OC9kZXNpZ24gc3R1ZGlvIGRlbW8vdGhlIHBhbGFjZSBob3RlbC9ob21lL2dhbGxlcnkgaXRlbSAtIHdlYnNpdGUgY29udGFpbmVy\" htmlencode=\"true\" name=\"design studio demo/the palace hotel/home/gallery item - website container\" type=\"design studio demo/the palace hotel/home/gallery item - website container\" start=\"<div class='gallery-item' id='ilc5d'>\" end=\"</div>\"]</div><div  class=\"dx-div gallery-tile\">[DxContainer htmlid=\"i88a7\" id=\"b445a480-5c2e-42a5-8647-49f3251d7102:OC9kZXNpZ24gc3R1ZGlvIGRlbW8vdGhlIHBhbGFjZSBob3RlbC9ob21lL2dhbGxlcnkgaXRlbSAtIGxvYmJ5IGNvbnRhaW5lcg==\" htmlencode=\"true\" name=\"design studio demo/the palace hotel/home/gallery item - lobby container\" type=\"design studio demo/the palace hotel/home/gallery item - lobby container\" start=\"<div class='gallery-item' id='i88a7'>\" end=\"</div>\"]</div></div><div  class=\"dx-div gallery-col\"><div  class=\"dx-div gallery-tile large-tile\">[DxContainer htmlid=\"i5pyhn\" id=\"dc7398db-1814-4d13-9a7b-94d1636849fb:OC9kZXNpZ24gc3R1ZGlvIGRlbW8vdGhlIHBhbGFjZSBob3RlbC9ob21lL2dhbGxlcnkgaXRlbSAtIHNwaXJhbCBzdGFpcmNhc2UgY29udGFpbmVy\" htmlencode=\"true\" name=\"design studio demo/the palace hotel/home/gallery item - spiral staircase container\" type=\"design studio demo/the palace hotel/home/gallery item - spiral staircase container\" start=\"<div class='gallery-item' id='i5pyhn'>\" end=\"</div>\"]</div></div><div  class=\"dx-div gallery-col\"><div  class=\"dx-div gallery-tile\">[DxContainer htmlid=\"iuzz9a\" id=\"315f06ea-4cce-4f5b-b11c-3d206d0da6ad:OC9kZXNpZ24gc3R1ZGlvIGRlbW8vdGhlIHBhbGFjZSBob3RlbC9ob21lL2dhbGxlcnkgaXRlbSAtIGV4ZWN1dGl2ZSBzdWl0ZSBjb250YWluZXI=\" htmlencode=\"true\" name=\"design studio demo/the palace hotel/home/gallery item - executive suite container\" type=\"design studio demo/the palace hotel/home/gallery item - executive suite container\" start=\"<div class='gallery-item' id='iuzz9a'>\" end=\"</div>\"]</div><div  class=\"dx-div gallery-tile\">[DxContainer htmlid=\"iuqjku\" id=\"fdbf4e74-cafc-4bb9-81a3-5c7eaf8983e7:OC9kZXNpZ24gc3R1ZGlvIGRlbW8vdGhlIHBhbGFjZSBob3RlbC9ob21lL2dhbGxlcnkgaXRlbSAtIGxvdW5nZSBjb250YWluZXI=\" htmlencode=\"true\" name=\"design studio demo/the palace hotel/home/gallery item - lounge container\" type=\"design studio demo/the palace hotel/home/gallery item - lounge container\" start=\"<div class='gallery-item' id='iuqjku'>\" end=\"</div>\"]</div></div><div  class=\"dx-div gallery-col\"><div  class=\"dx-div gallery-tile\">[DxContainer htmlid=\"ivb1dc\" id=\"0df3998b-2ecf-4863-ada4-25eb263c0a5e:OC9kZXNpZ24gc3R1ZGlvIGRlbW8vdGhlIHBhbGFjZSBob3RlbC9ob21lL2dhbGxlcnkgaXRlbSAtIGZhbWlseSBzdWl0ZSBjb250YWluZXI=\" htmlencode=\"true\" name=\"design studio demo/the palace hotel/home/gallery item - family suite container\" type=\"design studio demo/the palace hotel/home/gallery item - family suite container\" start=\"<div class='gallery-item' id='ivb1dc'>\" end=\"</div>\"]</div><div  class=\"dx-div gallery-tile\">[DxContainer htmlid=\"ii3vsu\" id=\"d239ab85-7338-4a22-949d-b9dc7505f9f5:OC9kZXNpZ24gc3R1ZGlvIGRlbW8vdGhlIHBhbGFjZSBob3RlbC9ob21lL2dhbGxlcnkgaXRlbSAtIGNvbmNpZXJnZSBjb250YWluZXI=\" htmlencode=\"true\" name=\"design studio demo/the palace hotel/home/gallery item - concierge container\" type=\"design studio demo/the palace hotel/home/gallery item - concierge container\" start=\"<div class='gallery-item' id='ii3vsu'>\" end=\"</div>\"]</div></div></div></section><section id=\"ihhtd\"  class=\"dx-section-layout featured\"><div id=\"igfcse\"  class=\"dx-column-layout feature-row\"><div id=\"ir7wk5\"  class=\"dx-columns feature-col\">[DxContainer htmlid=\"ivuwzg\" id=\"f8ed20f9-e912-4546-8578-ea1372621762:OC9kZXNpZ24gc3R1ZGlvIGRlbW8vdGhlIHBhbGFjZSBob3RlbC9ob21lL2ZlYXR1cmVkIHByb2R1Y3QgLSBtaWQgY2VudHVyeSBjaGFpciBjb250YWluZXI=\" htmlencode=\"true\" name=\"design studio demo/the palace hotel/home/featured product - mid century chair container\" type=\"design studio demo/the palace hotel/home/featured product - mid century chair container\" start=\"<div class='' id='ivuwzg'>\" end=\"</div>\"]</div><div id=\"ipgceo\"  class=\"dx-columns feature-col\">[DxContainer htmlid=\"iw0tfy\" id=\"6cf083df-8877-4e6e-8e72-a2a59c80c5b7:OC9kZXNpZ24gc3R1ZGlvIGRlbW8vdGhlIHBhbGFjZSBob3RlbC9ob21lL2NvbnRhY3QgdXMgLSBhbWFuZGEgc21pdGhzZW4gY29udGFpbmVy\" htmlencode=\"true\" name=\"design studio demo/the palace hotel/home/contact us - amanda smithsen container\" type=\"design studio demo/the palace hotel/home/contact us - amanda smithsen container\" start=\"<div class='' id='iw0tfy'>\" end=\"</div>\"]</div></div></section>[DxContainer htmlid=\"iv34ll\" id=\"ca88dd58-23c1-4992-b9a1-3dbbe89335b7:OC9kZXNpZ24gc3R1ZGlvIGRlbW8vdGhlIHBhbGFjZSBob3RlbC9ob21lL2hvbWUgLSB3b29kYnVybiBzdHVkaW8gZm9vdGVyIGNvbnRhaW5lcg==\" htmlencode=\"true\" name=\"design studio demo/the palace hotel/home/home - woodburn studio footer container\" type=\"design studio demo/the palace hotel/home/home - woodburn studio footer container\" start=\"<div class='' id='iv34ll'>\" end=\"</div>\"]</body>"
#         }
#       },
#       "properties": {
#         "slug": "home",
#         "renderURL": "/wps/wcm/connect/design+studio+demo/the+palace+hotel/home"
#       }
#     }
#   }'

# echo "Updating the DxSymbol (Designer - Amanda Smithsen container)"
# curl -b cookie.txt  -o UpdateDxSymbol.txt -w "%{http_code}" --location --request PUT "http://${SYNDICATOR_IP}:10039/wps/mycontenthandler/wcmrest-v2/containers/686d81b7-95b7-4f28-abda-ab754b58633f" \
# --header 'Authorization: Basic d3BzYWRtaW46d3BzYWRtaW4=' \
# --header 'Content-Type: application/atom+xml' \
# --data '{
#     "contentReference": "/wps/mycontenthandler/!ut/p/digest!ewe1e4gVzc-wZV9seR3JnQ/wcmrest/Content/268380ab-3708-42d5-8030-84df0c61810c",
#     "id": "686d81b7-95b7-4f28-abda-ab754b58633f",
#     "title": {
#       "lang": "en",
#       "value": "Designer - Amanda Smithsen container updated"
#     },
#     "name": "Designer - Amanda Smithsen container updated",
#     "type": "DxSymbol",
#     "created": "Fri, 23 Jul 2021 18:52:21.613Z",
#     "lastModifier": {
#       "distinguishedName": "uid=wpsadmin,o=defaultWIMFileBasedRealm",
#       "uri": "/wps/mycontenthandler/!ut/p/digest!ewe1e4gVzc-wZV9seR3JnQ/um/users/profiles/Z9eAeI1DA3QG6L9P6MMK65JPGJMG623CAJM072JC2MMGCH9O46OC6MHDCJQ86M1",
#       "name": "wpsadmin",
#       "type": "USER"
#     },
#     "creator": {
#       "distinguishedName": "uid=wpsadmin,o=defaultWIMFileBasedRealm",
#       "uri": "/wps/mycontenthandler/!ut/p/digest!ewe1e4gVzc-wZV9seR3JnQ/um/users/profiles/Z9eAeI1DA3QG6L9P6MMK65JPGJMG623CAJM072JC2MMGCH9O46OC6MHDCJQ86M1",
#       "name": "wpsadmin",
#       "type": "USER"
#     },
#     "profile": {},
#     "status": "PUBLISHED",
#     "lastModified": "Tue, 26 Apr 2022 14:50:28.275Z",
#     "libraryID": "ded26275-c13e-42a8-a20c-8a0dca96a57a",
#     "parentID": "8318b5f7-4100-458a-9aa7-382b436e5a36",
#     "lock": {
#       "isLocked": "false"
#     },
#     "history": {
#       "entries": [
#         {
#           "date": "Tue, 26 Apr 2022 14:50:28.275Z",
#           "name": "wpsadmin",
#           "message": "Document updated by wpsadmin"
#         }
#       ]
#     },
#     "links": [
#       {
#         "rel": "self",
#         "href": "/wps/mycontenthandler/!ut/p/digest!ewe1e4gVzc-wZV9seR3JnQ/wcmrest-v2/containers/686d81b7-95b7-4f28-abda-ab754b58633f",
#         "lang": "en",
#         "label": "Read"
#       },
#       {
#         "rel": "edit",
#         "href": "/wps/mycontenthandler/!ut/p/digest!ewe1e4gVzc-wZV9seR3JnQ/wcmrest-v2/containers/686d81b7-95b7-4f28-abda-ab754b58633f",
#         "lang": "en",
#         "label": "Edit"
#       },
#       {
#         "rel": "delete",
#         "href": "/wps/mycontenthandler/!ut/p/digest!ewe1e4gVzc-wZV9seR3JnQ/wcmrest-v2/containers/686d81b7-95b7-4f28-abda-ab754b58633f",
#         "lang": "en",
#         "label": "Delete"
#       },
#       {
#         "rel": "edit-media",
#         "href": "/wps/mycontenthandler/!ut/p/digest!ewe1e4gVzc-wZV9seR3JnQ/wcmrest-v2/containers/686d81b7-95b7-4f28-abda-ab754b58633f",
#         "type": "application/vnd.ibm.wcm+xml",
#         "lang": "en",
#         "label": "Edit Media"
#       },
#       {
#         "rel": "self",
#         "href": "/wps/mycontenthandler/!ut/p/digest!ewe1e4gVzc-wZV9seR3JnQ/wcmrest-v2/containers/686d81b7-95b7-4f28-abda-ab754b58633f",
#         "lang": "en",
#         "label": "Read"
#       },
#       {
#         "rel": "edit",
#         "href": "/wps/mycontenthandler/!ut/p/digest!ewe1e4gVzc-wZV9seR3JnQ/wcmrest-v2/containers/686d81b7-95b7-4f28-abda-ab754b58633f",
#         "lang": "en",
#         "label": "Edit"
#       },
#       {
#         "rel": "delete",
#         "href": "/wps/mycontenthandler/!ut/p/digest!ewe1e4gVzc-wZV9seR3JnQ/wcmrest-v2/containers/686d81b7-95b7-4f28-abda-ab754b58633f",
#         "lang": "en",
#         "label": "Delete"
#       }
#     ],
#     "data": {
#       "presentation": {
#         "model": {
#         },
#         "markup": {
#         }
#       }
#     }
#   }'