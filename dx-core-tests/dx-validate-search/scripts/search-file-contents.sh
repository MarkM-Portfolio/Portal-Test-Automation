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


WAS_PASSWORD=wpsadmin
DX_PASSWORD=wpsadmin

cd /tmp/

echo "POST Login"
curl --cookie-jar cookie.txt -s -o response.txt -w "%{http_code}\n" --location --request POST 'http://'${INSTANCE_IP}':10039/'${CONTEXT_ROOT_PATH}'/'${DX_CORE_HOME_PATH}'/cxml/04_SD9ePMtCP1I800I_KydQvyHFUBADPmuQy?userid=wpsadmin&password=wpsadmin' \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=0000zaNvk6pv3D3I16jDXVbB_2U:-1; LtpaToken2=dXQEQSyxCz/mURMDYxFSJYGwggXxyCYUquNTfDzJVCP2IUv4q157nKvaxdXCIMoVBvH1BiBujmWeRXYaFva0GTtRvTozr2OeYiVubRNrWThvzn8kvxrvbJvUmxjjSmDT7LveIQ4IrTtyKoI5xzlU7xw/ijAak3UM/hN9gr+W4tkh8RGsIKOL25ubROMk1LmnVRbpV6nYiOCDxtHhl5JKh7pC7LDplzQBsT9GOGoMxXkkvMgktGi+RFgS5Yp/KfFQ+uO2JP4jOAMAXkK1fxt+YFhJfzM84zICbtqtUaudkK4s+gwDUob/iq9N2sn6nfa2O/lTfKcmoVqmoHfMfHDNWOW1qECmvE/lEvzQmrJmYqC2Jw8nxLb44Wrsubg7Q5I2B5aAb3UmjzQI6+wh+8Ezn9A85Hfl+BsHhFoeGT52Ehj5czOS6tDCBPyZ2n+hNmWmQYlPAk/f3s94kPM/Zs+woLjCQ8x/DnvVZSupaMqlqHEGTpHGifM8yTY9PH3k1/xtq9N1eokR+p94SH3Po4SKtT/NbTzyHTtahzH2ITeJAOB06OUfQ/Lb0RrH0zarze931kSmPI+t3GbP4sN9z/jotL/HCuTw96oq4l0+PAqGjPE4kqJyMKt8b4rEzmBB4F5d1wm1Ben1FI8gIQ6UWcsAnMfoqdhH3iDJdWmwHsr86880Roum+5qVfrcX+LXjE/LQ4asKLapc51sotJsXl9+oxg==' \
--data '{
	"username":"wpsadmin",
	"password":"wpsadmin"
}'

echo "Searching for content using keyword 'sort of'"
curl -b cookie.txt -s -o searchinpptx.txt -w "%{http_code}\n" --location --request GET 'http://'${INSTANCE_IP}':10039/'${CONTEXT_ROOT_PATH}'/mycontenthandler/!ut/p/searchfeed/search?queryLang=en&locale=en&resultLang=en&query=sort%20of&scope=com.ibm.lotus.search.ALL_SOURCES&start=0&results=10&querySuggestionEnabled=true&mime-type=application/json' \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--data '{}'

find searchinpptx.txt | xargs -I {} bash -c 'if grep -Fq "File with all sorts of stuff PPT.pptx" {}; then echo "Found in File with all sorts of stuff PPT.pptx" ; else echo "Error: No results found"; exit 1; fi'
find searchinpptx.txt | xargs -I {} bash -c 'if grep -Fq "File with all sorts of stuff.docx" {}; then echo "Found in File with all sorts of stuff.docx" ; else echo "Error: No results found"; exit 1; fi'

echo "Searching for content in xlsx file"
curl -b cookie.txt -s -o searchinxlsx.txt -w "%{http_code}\n" --location --request GET 'http://'${INSTANCE_IP}':10039/'${CONTEXT_ROOT_PATH}'/mycontenthandler/!ut/p/searchfeed/search?queryLang=en&locale=en&resultLang=en&query=Vincenza&scope=com.ibm.lotus.search.ALL_SOURCES&start=0&results=10&querySuggestionEnabled=true&mime-type=application/json' \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--data '{}'

find searchinxlsx.txt | xargs -I {} bash -c 'if grep -Fq "file_example_XLSX_10.xlsx" {}; then echo "Found in file_example_XLSX_10.xlsx" ; else echo "Error: No results found"; exit 1; fi'

echo "Searching for content in htm file"
curl -b cookie.txt -s -o searchinhtm.txt -w "%{http_code}\n" --location --request GET 'http://'${INSTANCE_IP}':10039/'${CONTEXT_ROOT_PATH}'/mycontenthandler/!ut/p/searchfeed/search?queryLang=en&locale=en&resultLang=en&query=This%20page%20uses%20frames&scope=com.ibm.lotus.search.ALL_SOURCES&start=0&results=10&querySuggestionEnabled=true&mime-type=application/json' \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--data '{}'

find searchinhtm.txt | xargs -I {} bash -c 'if grep -Fq "CF200 Endgame coverage.htm" {}; then echo "Found in CF200 Endgame coverage.htm" ; else echo "Error: No results found"; exit 1; fi'