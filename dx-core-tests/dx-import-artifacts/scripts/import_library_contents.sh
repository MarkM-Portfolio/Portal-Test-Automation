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

echo $1

echo "POST Login"
curl --insecure --cookie-jar cookie.txt -s -o response.txt -w "%{http_code}\n" --location --request POST "$1/portal/cxml/04_SD9ePMtCP1I800I_KydQvyHFUBADPmuQy?userid=wpsadmin&password=wpsadmin" \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=0000zaNvk6pv3D3I16jDXVbB_2U:-1; LtpaToken2=dXQEQSyxCz/mURMDYxFSJYGwggXxyCYUquNTfDzJVCP2IUv4q157nKvaxdXCIMoVBvH1BiBujmWeRXYaFva0GTtRvTozr2OeYiVubRNrWThvzn8kvxrvbJvUmxjjSmDT7LveIQ4IrTtyKoI5xzlU7xw/ijAak3UM/hN9gr+W4tkh8RGsIKOL25ubROMk1LmnVRbpV6nYiOCDxtHhl5JKh7pC7LDplzQBsT9GOGoMxXkkvMgktGi+RFgS5Yp/KfFQ+uO2JP4jOAMAXkK1fxt+YFhJfzM84zICbtqtUaudkK4s+gwDUob/iq9N2sn6nfa2O/lTfKcmoVqmoHfMfHDNWOW1qECmvE/lEvzQmrJmYqC2Jw8nxLb44Wrsubg7Q5I2B5aAb3UmjzQI6+wh+8Ezn9A85Hfl+BsHhFoeGT52Ehj5czOS6tDCBPyZ2n+hNmWmQYlPAk/f3s94kPM/Zs+woLjCQ8x/DnvVZSupaMqlqHEGTpHGifM8yTY9PH3k1/xtq9N1eokR+p94SH3Po4SKtT/NbTzyHTtahzH2ITeJAOB06OUfQ/Lb0RrH0zarze931kSmPI+t3GbP4sN9z/jotL/HCuTw96oq4l0+PAqGjPE4kqJyMKt8b4rEzmBB4F5d1wm1Ben1FI8gIQ6UWcsAnMfoqdhH3iDJdWmwHsr86880Roum+5qVfrcX+LXjE/LQ4asKLapc51sotJsXl9+oxg==' \
--data '{
	"username":"wpsadmin",
	"password":"wpsadmin"
}'

echo "Create Import-Artifacts Library"
curl --insecure -b cookie.txt -s -o ImportArtifactsLibrary.txt -w "%{http_code}\n" --location --request POST "$1/mycontenthandler/wcmrest/Library?mime-type=application/json" \
--header 'Content-Type: text/plain' \
--header 'Cookie: JSESSIONID=0000lmG0NZCvpX7miPAqmF6jSW4:-1; LtpaToken2=cD4ZjDInifWJHEu9X0WuLspqRb4xbgryuIyBMGTN8/Gp5c5gHa4MFUihyrcwloQ2FblUEpnYqFmiqYi0Agvsrj5teS7z7GSoeiTN7b8oAVRlDCm5j2l3noTJuFoU0+mes34sYJmzINMcgNYoaJIEJQ43Gk5XspBpvhk9q4vokq2hDiPQbVDRxWp2QhR5JAXyCid9QalmYPgaVkH94bfXZih/tB1caB5xZA3hZkCEnpxKTR6nr/jlQJvk/MyRZ5quK1UymB8fBMTTvLjNuHzuQ8XSSdWrhY72v2KoCn/H4YFbWVf9aZ5xvMlKiqFWhCt8NhpgqigijfZDcBNIvbTFDAvojfXpW5n8NlpUkiOqXVd7R7D7LXVIq7gMfsLAXXNz+aiOzcj3aSDVw/LbWvhCX+KP409m0CKaA0gg2vkbJD9jl7vjH718q1MvV8XZowQEgN1VeMWDFAMOmRlpYmIHUVLah9/mylkhRNaJobK7K0OiA8JtfcB+D9JJRqjOqPpyd9N/D1/E1Qgr4qQt89Pnz+i9Od0+eR16zQRTpsfLUzlssrukRtE4TbSEySK6rG4P6p4KJVr42lcnTXz3iRXUG4blprW2fes8sFTjYO+3W8aoTUlbjyUZfDqg2jI8F0BAVvKqfELUbRMsAZjaStoaYwn0mZAu1Nuzi6OuwY7IE90=' \
--data '<?xml version="1.0" encoding="UTF-8"?>
<entry xmlns="http://www.w3.org/2005/Atom" xmlns:wcm="http://www.ibm.com/xmlns/wcm/8.0">
    <wcm:name>ImportArtifactsLibrary</wcm:name>
    <content type="application/vnd.ibm.wcm+xml">
        <wcm:library xmlns="http://www.ibm.com/xmlns/wcm/8.0">
            <enabled>true</enabled>
            <language>en</language>
            <includeDefaultItems>true</includeDefaultItems>
        </wcm:library>
    </content>
</entry>'

ImportArtifactsLibrary=`cat ImportArtifactsLibrary.txt | jq -r '.entry.id' | sed 's/wcmrest://'`

echo "ImportArtifactsLibrary UUID is $ImportArtifactsLibrary"

echo "Create ImportArtifactsContentTemplate"
curl --insecure -b cookie.txt -s -o ImportArtifactsContentTemplate.txt -w "%{http_code}\n" --location --request POST "$1/mycontenthandler/wcmrest/ContentTemplate?mime-type=application/json" \
--header 'Content-Type: text/plain' \
--header 'Cookie: JSESSIONID=0000lmG0NZCvpX7miPAqmF6jSW4:-1; LtpaToken2=cD4ZjDInifWJHEu9X0WuLspqRb4xbgryuIyBMGTN8/Gp5c5gHa4MFUihyrcwloQ2FblUEpnYqFmiqYi0Agvsrj5teS7z7GSoeiTN7b8oAVRlDCm5j2l3noTJuFoU0+mes34sYJmzINMcgNYoaJIEJQ43Gk5XspBpvhk9q4vokq2hDiPQbVDRxWp2QhR5JAXyCid9QalmYPgaVkH94bfXZih/tB1caB5xZA3hZkCEnpxKTR6nr/jlQJvk/MyRZ5quK1UymB8fBMTTvLjNuHzuQ8XSSdWrhY72v2KoCn/H4YFbWVf9aZ5xvMlKiqFWhCt8NhpgqigijfZDcBNIvbTFDAvojfXpW5n8NlpUkiOqXVd7R7D7LXVIq7gMfsLAXXNz+aiOzcj3aSDVw/LbWvhCX+KP409m0CKaA0gg2vkbJD9jl7vjH718q1MvV8XZowQEgN1VeMWDFAMOmRlpYmIHUVLah9/mylkhRNaJobK7K0OiA8JtfcB+D9JJRqjOqPpyd9N/D1/E1Qgr4qQt89Pnz+i9Od0+eR16zQRTpsfLUzlssrukRtE4TbSEySK6rG4P6p4KJVr42lcnTXz3iRXUG4blprW2fes8sFTjYO+3W8aoTUlbjyUZfDqg2jI8F0BAVvKqfELUbRMsAZjaStoaYwn0mZAu1Nuzi6OuwY7IE90=' \
--data '<?xml version="1.0" encoding="UTF-8"?>
<entry xmlns="http://www.w3.org/2005/Atom" xmlns:wcm="http://www.ibm.com/xmlns/wcm/8.0">
    <title>ImportArtifactsContentTemplateTitle</title>
    <wcm:name>ImportArtifactsContentTemplateName</wcm:name>
    <summary xml:lang="en">ImportArtifactsContentTemplateDescription</summary>
    <link rel="library" href="/wps/mycontenthandler/wcmrest/Library/'"$ImportArtifactsLibrary"'" label="Library"/>
</entry>'

ImportArtifactsContentTemplate=`cat ImportArtifactsContentTemplate.txt | jq -r '.entry.id' | sed 's/wcmrest://'`

echo "ImportArtifactsContentTemplate UUID is $ImportArtifactsContentTemplate"

echo "Get Workflow ID"
curl --insecure -b cookie.txt -s -o ImportArtifactsWorkflow.txt -w "%{http_code}\n" --location --request GET "$1/mycontenthandler/wcmrest/query?libraryid=$ImportArtifactsLibrary&type=workflow&mime-type=application/json" \
--header 'Content-Type: text/plain' \
--header 'Cookie: JSESSIONID=0000lmG0NZCvpX7miPAqmF6jSW4:-1; LtpaToken2=cD4ZjDInifWJHEu9X0WuLspqRb4xbgryuIyBMGTN8/Gp5c5gHa4MFUihyrcwloQ2FblUEpnYqFmiqYi0Agvsrj5teS7z7GSoeiTN7b8oAVRlDCm5j2l3noTJuFoU0+mes34sYJmzINMcgNYoaJIEJQ43Gk5XspBpvhk9q4vokq2hDiPQbVDRxWp2QhR5JAXyCid9QalmYPgaVkH94bfXZih/tB1caB5xZA3hZkCEnpxKTR6nr/jlQJvk/MyRZ5quK1UymB8fBMTTvLjNuHzuQ8XSSdWrhY72v2KoCn/H4YFbWVf9aZ5xvMlKiqFWhCt8NhpgqigijfZDcBNIvbTFDAvojfXpW5n8NlpUkiOqXVd7R7D7LXVIq7gMfsLAXXNz+aiOzcj3aSDVw/LbWvhCX+KP409m0CKaA0gg2vkbJD9jl7vjH718q1MvV8XZowQEgN1VeMWDFAMOmRlpYmIHUVLah9/mylkhRNaJobK7K0OiA8JtfcB+D9JJRqjOqPpyd9N/D1/E1Qgr4qQt89Pnz+i9Od0+eR16zQRTpsfLUzlssrukRtE4TbSEySK6rG4P6p4KJVr42lcnTXz3iRXUG4blprW2fes8sFTjYO+3W8aoTUlbjyUZfDqg2jI8F0BAVvKqfELUbRMsAZjaStoaYwn0mZAu1Nuzi6OuwY7IE90=' \
--data '{}'

ImportArtifactsWorkflow=`cat ImportArtifactsWorkflow.txt | jq -r '.feed.entry[0].id' | sed 's/wcmrest://'`

echo "ImportArtifactsWorkflow UUID is $ImportArtifactsWorkflow"

echo "Get SiteArea ID"
curl --insecure -b cookie.txt -s -o ImportArtifactsSiteArea.txt -w "%{http_code}\n" --location --request GET "$1/mycontenthandler/wcmrest/query?libraryid=$ImportArtifactsLibrary&type=sitearea&mime-type=application/json" \
--header 'Content-Type: text/plain' \
--header 'Cookie: JSESSIONID=0000lmG0NZCvpX7miPAqmF6jSW4:-1; LtpaToken2=cD4ZjDInifWJHEu9X0WuLspqRb4xbgryuIyBMGTN8/Gp5c5gHa4MFUihyrcwloQ2FblUEpnYqFmiqYi0Agvsrj5teS7z7GSoeiTN7b8oAVRlDCm5j2l3noTJuFoU0+mes34sYJmzINMcgNYoaJIEJQ43Gk5XspBpvhk9q4vokq2hDiPQbVDRxWp2QhR5JAXyCid9QalmYPgaVkH94bfXZih/tB1caB5xZA3hZkCEnpxKTR6nr/jlQJvk/MyRZ5quK1UymB8fBMTTvLjNuHzuQ8XSSdWrhY72v2KoCn/H4YFbWVf9aZ5xvMlKiqFWhCt8NhpgqigijfZDcBNIvbTFDAvojfXpW5n8NlpUkiOqXVd7R7D7LXVIq7gMfsLAXXNz+aiOzcj3aSDVw/LbWvhCX+KP409m0CKaA0gg2vkbJD9jl7vjH718q1MvV8XZowQEgN1VeMWDFAMOmRlpYmIHUVLah9/mylkhRNaJobK7K0OiA8JtfcB+D9JJRqjOqPpyd9N/D1/E1Qgr4qQt89Pnz+i9Od0+eR16zQRTpsfLUzlssrukRtE4TbSEySK6rG4P6p4KJVr42lcnTXz3iRXUG4blprW2fes8sFTjYO+3W8aoTUlbjyUZfDqg2jI8F0BAVvKqfELUbRMsAZjaStoaYwn0mZAu1Nuzi6OuwY7IE90=' \
--data '{}'

ImportArtifactsSiteArea=`cat ImportArtifactsSiteArea.txt | jq -r '.feed.entry[0].id' | sed 's/wcmrest://'`

echo "ImportArtifactsSiteArea UUID is $ImportArtifactsSiteArea"

echo "Create ImportArtifactsContent"
curl --insecure -b cookie.txt -s -o ImportArtifactsContent.txt -w "%{http_code}\n" --location --request POST "$1/mycontenthandler/wcmrest/Content?mime-type=application/json" \
--header 'Content-Type: application/xml' \
--header 'Cookie: JSESSIONID=0000lmG0NZCvpX7miPAqmF6jSW4:-1; LtpaToken2=cD4ZjDInifWJHEu9X0WuLspqRb4xbgryuIyBMGTN8/Gp5c5gHa4MFUihyrcwloQ2FblUEpnYqFmiqYi0Agvsrj5teS7z7GSoeiTN7b8oAVRlDCm5j2l3noTJuFoU0+mes34sYJmzINMcgNYoaJIEJQ43Gk5XspBpvhk9q4vokq2hDiPQbVDRxWp2QhR5JAXyCid9QalmYPgaVkH94bfXZih/tB1caB5xZA3hZkCEnpxKTR6nr/jlQJvk/MyRZ5quK1UymB8fBMTTvLjNuHzuQ8XSSdWrhY72v2KoCn/H4YFbWVf9aZ5xvMlKiqFWhCt8NhpgqigijfZDcBNIvbTFDAvojfXpW5n8NlpUkiOqXVd7R7D7LXVIq7gMfsLAXXNz+aiOzcj3aSDVw/LbWvhCX+KP409m0CKaA0gg2vkbJD9jl7vjH718q1MvV8XZowQEgN1VeMWDFAMOmRlpYmIHUVLah9/mylkhRNaJobK7K0OiA8JtfcB+D9JJRqjOqPpyd9N/D1/E1Qgr4qQt89Pnz+i9Od0+eR16zQRTpsfLUzlssrukRtE4TbSEySK6rG4P6p4KJVr42lcnTXz3iRXUG4blprW2fes8sFTjYO+3W8aoTUlbjyUZfDqg2jI8F0BAVvKqfELUbRMsAZjaStoaYwn0mZAu1Nuzi6OuwY7IE90=' \
--data '<?xml version="1.0" encoding="UTF-8"?>
<atom:entry xmlns:atom="http://www.w3.org/2005/Atom" xmlns:wcm="wcm/namespace">
			<wcm:name>ImportArtifactsContent Name</wcm:name>
			<atom:title>ImportArtifactsContent Title</atom:title>
			<atom:link atom:rel="parent" atom:href="/wps/
            mycontenthandler/wcmrest/item/'"$ImportArtifactsSiteArea"'"/>
			<atom:link atom:rel="workflow" atom:href="/wps/
            mycontenthandler/wcmrest/item/'"$ImportArtifactsWorkflow"'"/>
			<atom:link atom:rel="content-template" atom:href="/wps/
            mycontenthandler/wcmrest/item/'"$ImportArtifactsContentTemplate"'"/>
		</atom:entry>'

ImportArtifactsContent=`cat ImportArtifactsContent.txt | jq -r '.entry.id' | sed 's/wcmrest://'`

echo "ImportArtifactsContent UUID is $ImportArtifactsContent"

echo "Create ImportArtifacts Stylesheet"
curl --insecure -b cookie.txt -s -o ImportArtifactsStylesheets.txt -w "%{http_code}\n" --location --request POST "$1/mycontenthandler/wcmrest-v2/component/stylesheets" \
--header 'Content-Type: text/plain' \
--header 'Cookie: JSESSIONID=0000lmG0NZCvpX7miPAqmF6jSW4:-1; LtpaToken2=cD4ZjDInifWJHEu9X0WuLspqRb4xbgryuIyBMGTN8/Gp5c5gHa4MFUihyrcwloQ2FblUEpnYqFmiqYi0Agvsrj5teS7z7GSoeiTN7b8oAVRlDCm5j2l3noTJuFoU0+mes34sYJmzINMcgNYoaJIEJQ43Gk5XspBpvhk9q4vokq2hDiPQbVDRxWp2QhR5JAXyCid9QalmYPgaVkH94bfXZih/tB1caB5xZA3hZkCEnpxKTR6nr/jlQJvk/MyRZ5quK1UymB8fBMTTvLjNuHzuQ8XSSdWrhY72v2KoCn/H4YFbWVf9aZ5xvMlKiqFWhCt8NhpgqigijfZDcBNIvbTFDAvojfXpW5n8NlpUkiOqXVd7R7D7LXVIq7gMfsLAXXNz+aiOzcj3aSDVw/LbWvhCX+KP409m0CKaA0gg2vkbJD9jl7vjH718q1MvV8XZowQEgN1VeMWDFAMOmRlpYmIHUVLah9/mylkhRNaJobK7K0OiA8JtfcB+D9JJRqjOqPpyd9N/D1/E1Qgr4qQt89Pnz+i9Od0+eR16zQRTpsfLUzlssrukRtE4TbSEySK6rG4P6p4KJVr42lcnTXz3iRXUG4blprW2fes8sFTjYO+3W8aoTUlbjyUZfDqg2jI8F0BAVvKqfELUbRMsAZjaStoaYwn0mZAu1Nuzi6OuwY7IE90=' \
--data '{
    "title": {
        "lang": "en",
        "value": "ImportArtifactsStylesheets"
    },
    "name": "ImportArtifactsStylesheetsName",
    "type": "LibraryStyleSheetComponent",
    "status": "PUBLISHED",
    "libraryID": "'"$ImportArtifactsLibrary"'",
    "lock": {
        "isLocked": "false"
    },
    "data": {
        "mediaType": "TV",
        "styleSheetType": "Preferred",
        "title": "test style sheet element title from post",
        "fileName": "postTest1.css",
        "content": "/* POST test css. */\nbody {\n  margin: 25px;\n  background-color: rgb(240,240,240);\n  font-family: arial, sans-serif;\n  font-size: 14px;\n}\n\n/* Applies to all <h1>...</h1> elements. */\nh1 {\n  font-size: 35px;\n  font-weight: normal;\n  margin-top: 5px;\n}\n\n/* Applies to all elements with <... class=\"someclass\"> specified. */\n.someclass { color: red; }\n\n/* Applies to the element with <... id=\"someid\"> specified. */\n#someid { color: purple; }\n"
    }
}'

ImportArtifactsStylesheets=`cat ImportArtifactsStylesheets.txt | jq -r '.id'`

echo "ImportArtifactsStylesheets UUID is $ImportArtifactsStylesheets"

echo "Create ImportArtifactsContentSites"
curl --insecure -b cookie.txt -s -o ImportArtifactsContentSites.txt -w "%{http_code}\n" --location --request POST "$1/mycontenthandler/wcmrest-v2/content-sites/" \
--header 'Content-Type: text/plain' \
--header 'Cookie: JSESSIONID=0000lmG0NZCvpX7miPAqmF6jSW4:-1; LtpaToken2=cD4ZjDInifWJHEu9X0WuLspqRb4xbgryuIyBMGTN8/Gp5c5gHa4MFUihyrcwloQ2FblUEpnYqFmiqYi0Agvsrj5teS7z7GSoeiTN7b8oAVRlDCm5j2l3noTJuFoU0+mes34sYJmzINMcgNYoaJIEJQ43Gk5XspBpvhk9q4vokq2hDiPQbVDRxWp2QhR5JAXyCid9QalmYPgaVkH94bfXZih/tB1caB5xZA3hZkCEnpxKTR6nr/jlQJvk/MyRZ5quK1UymB8fBMTTvLjNuHzuQ8XSSdWrhY72v2KoCn/H4YFbWVf9aZ5xvMlKiqFWhCt8NhpgqigijfZDcBNIvbTFDAvojfXpW5n8NlpUkiOqXVd7R7D7LXVIq7gMfsLAXXNz+aiOzcj3aSDVw/LbWvhCX+KP409m0CKaA0gg2vkbJD9jl7vjH718q1MvV8XZowQEgN1VeMWDFAMOmRlpYmIHUVLah9/mylkhRNaJobK7K0OiA8JtfcB+D9JJRqjOqPpyd9N/D1/E1Qgr4qQt89Pnz+i9Od0+eR16zQRTpsfLUzlssrukRtE4TbSEySK6rG4P6p4KJVr42lcnTXz3iRXUG4blprW2fes8sFTjYO+3W8aoTUlbjyUZfDqg2jI8F0BAVvKqfELUbRMsAZjaStoaYwn0mZAu1Nuzi6OuwY7IE90=' \
--data '{
    "title": {
        "lang": "en",
        "value": "ImportArtifactsContentSites"
    },
    "name": "ImportArtifactsContentSites",
    "type": "DxContentSite",
    "libraryID": "'"$ImportArtifactsLibrary"'",
    "data": {
        "presentation": {
            "model": {
                "components": "[{\"droppable\":\".cell\",\"components\":[{\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"i2de\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]}],\"resizable\":{\"br\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":0},\"classes\":[{\"private\":1,\"name\":\"row\"},\"rrw2\"],\"name\":\"Row\",\"attributes\":{\"id\":\"icd7\"}},{\"droppable\":\".cell\",\"components\":[{\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"itlo\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]},{\"components\":[{\"components\":[{\"copyable\":false,\"_innertext\":false,\"draggable\":false,\"removable\":false,\"highlightable\":0,\"type\":\"textnode\",\"content\":\"This is a test\"},{\"copyable\":false,\"_innertext\":false,\"draggable\":false,\"removable\":false,\"classes\":[\"link\"],\"attributes\":{\"href\":\"\",\"id\":\"ibjv\"},\"highlightable\":0,\"type\":\"link\"}],\"attributes\":{\"id\":\"i5ni\"},\"type\":\"text\"}],\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"ipy8\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]}],\"resizable\":{\"br\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":0},\"classes\":[{\"private\":1,\"name\":\"row\"}],\"name\":\"Row\"},{\"attributes\":{\"src\":\"data:image\/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIHZpZXdCb3g9IjAgMCAyNCAyNCIgc3R5bGU9ImZpbGw6IHJnYmEoMCwwLDAsMC4xNSk7IHRyYW5zZm9ybTogc2NhbGUoMC43NSkiPgogICAgICAgIDxwYXRoIGQ9Ik04LjUgMTMuNWwyLjUgMyAzLjUtNC41IDQuNSA2SDVtMTYgMVY1YTIgMiAwIDAgMC0yLTJINWMtMS4xIDAtMiAuOS0yIDJ2MTRjMCAxLjEuOSAyIDIgMmgxNGMxLjEgMCAyLS45IDItMnoiPjwvcGF0aD4KICAgICAgPC9zdmc+\",\"id\":\"iibe\"},\"type\":\"image\"}]",
                "styles": "[{\"droppable\":\".cell\",\"components\":[{\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"i2de\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]}],\"resizable\":{\"br\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":0},\"classes\":[{\"private\":1,\"name\":\"row\"},\"rrw2\"],\"name\":\"Row\",\"attributes\":{\"id\":\"icd7\"}},{\"droppable\":\".cell\",\"components\":[{\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"itlo\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]},{\"components\":[{\"components\":[{\"copyable\":false,\"_innertext\":false,\"draggable\":false,\"removable\":false,\"highlightable\":0,\"type\":\"textnode\",\"content\":\"This is a test\"},{\"copyable\":false,\"_innertext\":false,\"draggable\":false,\"removable\":false,\"classes\":[\"link\"],\"attributes\":{\"href\":\"\",\"id\":\"ibjv\"},\"highlightable\":0,\"type\":\"link\"}],\"attributes\":{\"id\":\"i5ni\"},\"type\":\"text\"}],\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"ipy8\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]}],\"resizable\":{\"br\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":0},\"classes\":[{\"private\":1,\"name\":\"row\"}],\"name\":\"Row\"},{\"attributes\":{\"src\":\"data:image\/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIHZpZXdCb3g9IjAgMCAyNCAyNCIgc3R5bGU9ImZpbGw6IHJnYmEoMCwwLDAsMC4xNSk7IHRyYW5zZm9ybTogc2NhbGUoMC43NSkiPgogICAgICAgIDxwYXRoIGQ9Ik04LjUgMTMuNWwyLjUgMyAzLjUtNC41IDQuNSA2SDVtMTYgMVY1YTIgMiAwIDAgMC0yLTJINWMtMS4xIDAtMiAuOS0yIDJ2MTRjMCAxLjEuOSAyIDIgMmgxNGMxLjEgMCAyLS45IDItMnoiPjwvcGF0aD4KICAgICAgPC9zdmc+\",\"id\":\"iibe\"},\"type\":\"image\"}]"
            },
            "markup": {
                "style-reference": "wcmrest:LibraryStyleSheetComponent/'"$ImportArtifactsStylesheets"'",
                "markup": "<b>This is a sample set of mark up to test that transformer.</b>"
            }
        },
        "properties": {
            "slug": "demo",
            "prop1": "test"

        }
    }
}'

ImportArtifactsContentSites=`cat ImportArtifactsContentSites.txt | jq -r '.id'`

echo "ImportArtifactsContentSites UUID is $ImportArtifactsContentSites"

echo "Create ImportArtifactsContentSitesPage"
curl --insecure -b cookie.txt -s -o ImportArtifactsContentSitesPage.txt -w "%{http_code}\n" --location --request POST "$1/mycontenthandler/wcmrest-v2/content-sites/" \
--header 'Content-Type: text/plain' \
--header 'Cookie: JSESSIONID=0000lmG0NZCvpX7miPAqmF6jSW4:-1; LtpaToken2=cD4ZjDInifWJHEu9X0WuLspqRb4xbgryuIyBMGTN8/Gp5c5gHa4MFUihyrcwloQ2FblUEpnYqFmiqYi0Agvsrj5teS7z7GSoeiTN7b8oAVRlDCm5j2l3noTJuFoU0+mes34sYJmzINMcgNYoaJIEJQ43Gk5XspBpvhk9q4vokq2hDiPQbVDRxWp2QhR5JAXyCid9QalmYPgaVkH94bfXZih/tB1caB5xZA3hZkCEnpxKTR6nr/jlQJvk/MyRZ5quK1UymB8fBMTTvLjNuHzuQ8XSSdWrhY72v2KoCn/H4YFbWVf9aZ5xvMlKiqFWhCt8NhpgqigijfZDcBNIvbTFDAvojfXpW5n8NlpUkiOqXVd7R7D7LXVIq7gMfsLAXXNz+aiOzcj3aSDVw/LbWvhCX+KP409m0CKaA0gg2vkbJD9jl7vjH718q1MvV8XZowQEgN1VeMWDFAMOmRlpYmIHUVLah9/mylkhRNaJobK7K0OiA8JtfcB+D9JJRqjOqPpyd9N/D1/E1Qgr4qQt89Pnz+i9Od0+eR16zQRTpsfLUzlssrukRtE4TbSEySK6rG4P6p4KJVr42lcnTXz3iRXUG4blprW2fes8sFTjYO+3W8aoTUlbjyUZfDqg2jI8F0BAVvKqfELUbRMsAZjaStoaYwn0mZAu1Nuzi6OuwY7IE90=' \
--data '{
    "title": {
        "lang": "en",
        "value": "ImportArtifactsContentSitesPage"
    },
    "name": "ImportArtifactsContentSitesPage",
    "type": "DxContentPage",
    "libraryID": "'"$ImportArtifactsLibrary"'",
    "parentID": "'"$ImportArtifactsContentSites"'",
    "data": {
         "presentation": {
            "model": {
                "components": "[{\"droppable\":\".cell\",\"components\":[{\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"i2de\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]}],\"resizable\":{\"br\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":0},\"classes\":[{\"private\":1,\"name\":\"row\"},\"rrw2\"],\"name\":\"Row\",\"attributes\":{\"id\":\"icd7\"}},{\"droppable\":\".cell\",\"components\":[{\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"itlo\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]},{\"components\":[{\"components\":[{\"copyable\":false,\"_innertext\":false,\"draggable\":false,\"removable\":false,\"highlightable\":0,\"type\":\"textnode\",\"content\":\"This is a test\"},{\"copyable\":false,\"_innertext\":false,\"draggable\":false,\"removable\":false,\"classes\":[\"link\"],\"attributes\":{\"href\":\"\",\"id\":\"ibjv\"},\"highlightable\":0,\"type\":\"link\"}],\"attributes\":{\"id\":\"i5ni\"},\"type\":\"text\"}],\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"ipy8\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]}],\"resizable\":{\"br\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":0},\"classes\":[{\"private\":1,\"name\":\"row\"}],\"name\":\"Row\"},{\"attributes\":{\"src\":\"data:image\/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIHZpZXdCb3g9IjAgMCAyNCAyNCIgc3R5bGU9ImZpbGw6IHJnYmEoMCwwLDAsMC4xNSk7IHRyYW5zZm9ybTogc2NhbGUoMC43NSkiPgogICAgICAgIDxwYXRoIGQ9Ik04LjUgMTMuNWwyLjUgMyAzLjUtNC41IDQuNSA2SDVtMTYgMVY1YTIgMiAwIDAgMC0yLTJINWMtMS4xIDAtMiAuOS0yIDJ2MTRjMCAxLjEuOSAyIDIgMmgxNGMxLjEgMCAyLS45IDItMnoiPjwvcGF0aD4KICAgICAgPC9zdmc+\",\"id\":\"iibe\"},\"type\":\"image\"}]",
                "styles": "[{\"droppable\":\".cell\",\"components\":[{\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"i2de\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]}],\"resizable\":{\"br\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":0},\"classes\":[{\"private\":1,\"name\":\"row\"},\"rrw2\"],\"name\":\"Row\",\"attributes\":{\"id\":\"icd7\"}},{\"droppable\":\".cell\",\"components\":[{\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"itlo\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]},{\"components\":[{\"components\":[{\"copyable\":false,\"_innertext\":false,\"draggable\":false,\"removable\":false,\"highlightable\":0,\"type\":\"textnode\",\"content\":\"This is a test\"},{\"copyable\":false,\"_innertext\":false,\"draggable\":false,\"removable\":false,\"classes\":[\"link\"],\"attributes\":{\"href\":\"\",\"id\":\"ibjv\"},\"highlightable\":0,\"type\":\"link\"}],\"attributes\":{\"id\":\"i5ni\"},\"type\":\"text\"}],\"draggable\":\".row\",\"resizable\":{\"br\":0,\"bc\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"step\":0.2,\"currentUnit\":1,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":1,\"keyWidth\":\"flex-basis\"},\"classes\":[{\"private\":1,\"name\":\"cell\"}],\"name\":\"Cell\",\"attributes\":{\"id\":\"ipy8\"},\"unstylable\":[\"width\"],\"stylable-require\":[\"flex-basis\"]}],\"resizable\":{\"br\":0,\"tl\":0,\"cl\":0,\"bl\":0,\"minDim\":1,\"tr\":0,\"tc\":0,\"cr\":0},\"classes\":[{\"private\":1,\"name\":\"row\"}],\"name\":\"Row\"},{\"attributes\":{\"src\":\"data:image\/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIHZpZXdCb3g9IjAgMCAyNCAyNCIgc3R5bGU9ImZpbGw6IHJnYmEoMCwwLDAsMC4xNSk7IHRyYW5zZm9ybTogc2NhbGUoMC43NSkiPgogICAgICAgIDxwYXRoIGQ9Ik04LjUgMTMuNWwyLjUgMyAzLjUtNC41IDQuNSA2SDVtMTYgMVY1YTIgMiAwIDAgMC0yLTJINWMtMS4xIDAtMiAuOS0yIDJ2MTRjMCAxLjEuOSAyIDIgMmgxNGMxLjEgMCAyLS45IDItMnoiPjwvcGF0aD4KICAgICAgPC9zdmc+\",\"id\":\"iibe\"},\"type\":\"image\"}]"
            },
            "markup": {
                "style-reference": "wcmrest:LibraryStyleSheetComponent/'"$ImportArtifactsStylesheets"'",
                "markup": "<b>This is a sample set of mark up to test that transformer.</b>"
            }
        },
        "properties": {
            "slug": "demo",
            "prop1": "test"

        }
    }
}'

ImportArtifactsContentSitesPage=`cat ImportArtifactsContentSitesPage.txt | jq -r '.id'`

echo "ImportArtifactsContentSitesPage UUID is $ImportArtifactsContentSitesPage"