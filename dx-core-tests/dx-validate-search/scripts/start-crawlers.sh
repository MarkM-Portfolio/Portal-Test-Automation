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

echo "Start JCR Crawler"
curl -b cookie.txt -s -o jcrcrawl.txt -w "%{http_code}\n" --location --request POST 'http://'${INSTANCE_IP}':10039/'${CONTEXT_ROOT_PATH}'/mycontenthandler/!ut/p/searchadmin/service/Default+Search+Service/collection/JCRCollection1/provider/JCRSource/crawl' \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--data '{}'

echo "Start WCM Content Crawler"
curl -b cookie.txt -s -o wcmcrawl.txt -w "%{http_code}\n" --location --request POST 'http://'${INSTANCE_IP}':10039/'${CONTEXT_ROOT_PATH}'/mycontenthandler/!ut/p/searchadmin/service/Default+Search+Service/collection/Default+Search+Collection/provider/WCM+Content+Source/crawl' \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--data '{}'

echo "Start Portal Content Crawler"
curl -b cookie.txt -s -o pccrawl.txt -w "%{http_code}\n" --location --request POST 'http://'${INSTANCE_IP}':10039/'${CONTEXT_ROOT_PATH}'/mycontenthandler/!ut/p/searchadmin/service/Default+Search+Service/collection/Default+Search+Collection/provider/Portal+Content+Source/crawl' \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--data '{}'

sleep 900