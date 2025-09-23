#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2001, 2022. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

# define response and configuration files that are being populated with necessary data

WAS_PASSWORD=wpsadmin
DX_PASSWORD=wpsadmin

cd /tmp/dx-onpremise

echo "POST Login"
curl --cookie-jar cookie.txt -s -o response.txt -w "%{http_code}" --location --request POST 'http://'${INSTANCE_IP}':10039/'${CONTEXT_ROOT_PATH}'/'${DX_CORE_HOME_PATH}'/cxml/04_SD9ePMtCP1I800I_KydQvyHFUBADPmuQy?userid=wpsadmin&password=wpsadmin' \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=0000zaNvk6pv3D3I16jDXVbB_2U:-1; LtpaToken2=dXQEQSyxCz/mURMDYxFSJYGwggXxyCYUquNTfDzJVCP2IUv4q157nKvaxdXCIMoVBvH1BiBujmWeRXYaFva0GTtRvTozr2OeYiVubRNrWThvzn8kvxrvbJvUmxjjSmDT7LveIQ4IrTtyKoI5xzlU7xw/ijAak3UM/hN9gr+W4tkh8RGsIKOL25ubROMk1LmnVRbpV6nYiOCDxtHhl5JKh7pC7LDplzQBsT9GOGoMxXkkvMgktGi+RFgS5Yp/KfFQ+uO2JP4jOAMAXkK1fxt+YFhJfzM84zICbtqtUaudkK4s+gwDUob/iq9N2sn6nfa2O/lTfKcmoVqmoHfMfHDNWOW1qECmvE/lEvzQmrJmYqC2Jw8nxLb44Wrsubg7Q5I2B5aAb3UmjzQI6+wh+8Ezn9A85Hfl+BsHhFoeGT52Ehj5czOS6tDCBPyZ2n+hNmWmQYlPAk/f3s94kPM/Zs+woLjCQ8x/DnvVZSupaMqlqHEGTpHGifM8yTY9PH3k1/xtq9N1eokR+p94SH3Po4SKtT/NbTzyHTtahzH2ITeJAOB06OUfQ/Lb0RrH0zarze931kSmPI+t3GbP4sN9z/jotL/HCuTw96oq4l0+PAqGjPE4kqJyMKt8b4rEzmBB4F5d1wm1Ben1FI8gIQ6UWcsAnMfoqdhH3iDJdWmwHsr86880Roum+5qVfrcX+LXjE/LQ4asKLapc51sotJsXl9+oxg==' \
--data '{
	"username":"wpsadmin",
	"password":"wpsadmin"
}'

echo "Delete search service"
curl -b cookie.txt -s -o delete.txt -w "%{http_code}" --location --request DELETE 'http://'${INSTANCE_IP}':10039/'${CONTEXT_ROOT_PATH}'/mycontenthandler/!ut/p/searchadmin/service/Default+Search+Service' \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--data '{
    "Portal Search Services": [
        {
            "Service Name": "Default Search Service"}
    ]}'

echo "Create Remote PSE service"
curl -b cookie.txt -s -o ejb.txt -w "%{http_code}" --location --request POST 'http://'${INSTANCE_IP}':10039/'${CONTEXT_ROOT_PATH}'/mycontenthandler/!ut/p/searchadmin/service/Remote+PSE+service+EJB' \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--data '{
    "RESOURCE_ENVIRONMENT_PROVIDER_NAME": "SearchPropertiesService",
    "facetedFields": "null",
    "WORK_MANAGER_DEPLOY": "wps/searchIndexWM",
    "EJB_Example": "ejb/com/ibm/hrl/portlets/WsPse/WebScannerLiteEJBHome",
    "DefaultCollectionsDirectory": "null",
    "CONTENT_SOURCE_TYPE_FEATURE_NAME": "ContentSourceType",
    "EJB": "ejb/com/ibm/hrl/portlets/WsPse/WebScannerLiteEJBHome",
    "MAX_BUILD_BATCH_SIZE": "10000",
    "fieldTypes": "null",
    "WORK_MANAGER_NATIVE": "force.hrl.work.manager.use.native.threads",
    "WORK_MANAGER": "wps/searchIndexWM",
    "PSE_TYPE_option_3": "soap",
    "PSE_TYPE_option_2": "ejb",
    "PSE_TYPE_option_1": "localhost",
    "IIOP_URL": "iiop://'${REMOTE_SEARCH_INSTANCE_IP}':2809",
    "VALIDATE_COOKIE": "123",
    "PortalCollectionSourceName": "Remote PSE service EJB2",
    "WORK_MANAGER_NAME": "wps/searchIndexWM",
    "PSE_TYPE": "ejb",
    "CONTENT_SOURCE_TYPE_FEATURE_VAL_PORTAL": "Portal",
    "HTTP_MAX_BODY_SIZE_MB": "20",
    "MAX_BUILD_INTERVAL_TIME_SECONDS": "300",
    "SetProperties": "on",
    "PortalCollectionName": "TestGood",
    "IIOP_URL_Example": "iiop://localhost:2811",
    "CLEAN_UP_TIME_OF_DAY_HOURS": "0",
    "SOAP_URL_Example": "http://localhost:10000/WebScannerSOAP/servlet/rpcrouter",
    "mappedFields": "null",
    "OPEN_WCM_WINDOW": "/'${CONTEXT_ROOT_PATH}'/myportal/wcmContent?WCM_GLOBAL_CONTEXT=",
    "SOAP_URL": "null",
    "DEFAULT_acls_FIELDINFO": "contentSearchable=false, fieldSearchable=true, returnable=true, sortable=false, supportsExactMatch=true, parametric=false, typeAhead=false",
    "SecurityResolverId": "com.ibm.lotus.search.plugins.provider.core.PortalSecurityResolverFactory",
    "CONTENT_SOURCE_TYPE_FEATURE_VAL_UPLOAD": "Upload",
    "CONTENT_SOURCE_TYPE_FEATURE_VAL_WEB": "Web",
    "OpenResultMode": "new",
    "SEARCH_SECURITY_MODE": "SECURITY_MODE_PRE_POST_FILTER"
}'

echo "Create Portal Search collection"
curl -b cookie.txt -s -o psc.txt -w "%{http_code}" --location --request POST 'http://'${INSTANCE_IP}':10039/'${CONTEXT_ROOT_PATH}'/mycontenthandler/!ut/p/searchadmin/service/Remote+PSE+service+EJB/collection/Portal+Search+Collection' \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--data '{
    "location": "/opt/HCL/AppServer/profiles/prs_profile/SearchCollections/PortalSearchCollection",
    "IndexTitleKey": "Portal Search Collection",
    "IndexNameKey": "Portal Search Collection",
    "IndexLanguageKey": "en_US",
    "CollectionStatus": "true",
    "IndexDescriptionKey": "Portal Search Collection",
    "DictionaryAnalysis": "true"
}'

echo "Create JCR Search collection"
curl -b cookie.txt -s -o jcr.txt -w "%{http_code}" --location --request POST 'http://'${INSTANCE_IP}':10039/'${CONTEXT_ROOT_PATH}'/mycontenthandler/!ut/p/searchadmin/service/Remote+PSE+service+EJB/collection/JCRCollection1' \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--data '{
    "location": "/opt/HCL/AppServer/profiles/prs_profile/SearchCollections/JCRCollection1",
    "IndexTitleKey": "JCRCollection1",
    "IndexNameKey": "JCRCollection1",
    "IndexLanguageKey": "en_US",
    "location": "/opt/HCL/AppServer/profiles/prs_profile/SearchCollections/JCRCollection1",
    "CollectionStatus": "true",
    "IndexDescriptionKey": "JCRCollection1",
    "DictionaryAnalysis": "true"
}'

echo "Create WCM Content provider"
curl -b cookie.txt -s -o wcm.txt -w "%{http_code}" --location --request POST 'http://'${INSTANCE_IP}':10039/'${CONTEXT_ROOT_PATH}'/mycontenthandler/!ut/p/searchadmin/service/Remote+PSE+service+EJB/collection/Portal+Search+Collection/provider/WCMContentSource' \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--data '{
    "Type": "WCM_CRAWLER",
    "Maximum Time": "60",
    "Maximum Depth": "-1",
    "Maximum Nodes": "-1",
    "Force Complete Crawl": "false",
    "URL": "https://'${INSTANCE_IP}':10042/'${CONTEXT_ROOT_PATH}'/seedlist/myserver?SeedlistId=&Source=com.ibm.workplace.wcm.plugins.seedlist.retriever.WCMRetrieverFactory&Action=GetDocuments",
    "Name": "WCMContentSource",
    "Security Info": {
        "Host Realm for User": "",
        "User Name on Server": "wpsadmin",
        "Host Name of Server": "'${INSTANCE_IP}'",
        "User Password on Server": "wpsadmin",
        "Host Authentication Type for User": "http-basic"
    }
}'

echo "Create Portal Content provider"
curl -b cookie.txt -s -o pcr.txt -w "%{http_code}" --location --request POST 'http://'${INSTANCE_IP}':10039/'${CONTEXT_ROOT_PATH}'/mycontenthandler/!ut/p/searchadmin/service/Remote+PSE+service+EJB/collection/Portal+Search+Collection/provider/Portal+Content' \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--data '{
    "Type": "PORTAL_CRAWLER",
    "Maximum Time": "60",
    "Maximum Depth": "-1",
    "Maximum Nodes": "-1",
    "Force Complete Crawl": "true",
    "URL": "http://'${INSTANCE_IP}':10039/'${CONTEXT_ROOT_PATH}'/seedlist/myserver?Source=com.ibm.lotus.search.plugins.seedlist.retriever.portal.PortalRetrieverFactory&Action=GetDocuments&Range=100&locale=en-US",
    "Name": "Portal Content",
    "Security Info": {
        "Host Realm for User": "",
        "User Name on Server": "wpsadmin",
        "User Password on Server": "wpsadmin",
        "Host Name of Server": "'${INSTANCE_IP}'",
        "Host Authentication Type for User": "http-basic"
    }
}'

echo "Create JCR Content provider"
curl -b cookie.txt -s -o jcrp.txt -w "%{http_code}" --location --request POST 'http://'${INSTANCE_IP}':10039/'${CONTEXT_ROOT_PATH}'/mycontenthandler/!ut/p/searchadmin/service/Remote+PSE+service+EJB/collection/JCRCollection1/provider/JCR+Content' \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--data '{
    "Type": "SEED_LIST",
    "Maximum Time": "60",
    "Maximum Depth": "-1",
    "Maximum Nodes": "-1",
    "Force Complete Crawl": "false",
    "URL": "https://'${INSTANCE_IP}':10042/'${CONTEXT_ROOT_PATH}'/seedlist/myserver?Action=GetDocuments&Format=ATOM&Locale=en_US&Range=100&Source=com.ibm.lotus.search.plugins.seedlist.retriever.jcr.JCRRetrieverFactory&Start=0&SeedlistId=1@OOTB_CRAWLER1",
    "Name": "JCR Content",
    "Security Info": {
        "Host Realm for User": "CrawlerUsersRealm",
        "User Name on Server": "wpsadmin",
        "Host Name of Server": "'${INSTANCE_IP}'",
        "User Password on Server": "wpsadmin",
        "Host Authentication Type for User": "http-basic"
    }
}'

echo "Start JCR Crawler"
curl -b cookie.txt -s -o jcrcrawl.txt -w "%{http_code}" --location --request POST 'http://'${INSTANCE_IP}':10039/'${CONTEXT_ROOT_PATH}'/mycontenthandler/!ut/p/searchadmin/service/Remote+PSE+service+EJB/collection/JCRCollection1/provider/JCR+Content/crawl' \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--data '{}'

echo "Start WCM Content Crawler"
curl -b cookie.txt -s -o wcmcrawl.txt -w "%{http_code}" --location --request POST 'http://'${INSTANCE_IP}':10039/'${CONTEXT_ROOT_PATH}'/mycontenthandler/!ut/p/searchadmin/service/Remote+PSE+service+EJB/collection/Portal+Search+Collection/provider/WCMContentSource/crawl' \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--data '{}'

echo "Start Portal Content Crawler"
curl -b cookie.txt -s -o pccrawl.txt -w "%{http_code}" --location --request POST 'http://'${INSTANCE_IP}':10039/'${CONTEXT_ROOT_PATH}'/mycontenthandler/!ut/p/searchadmin/service/Remote+PSE+service+EJB/collection/Portal+Search+Collection/provider/Portal+Content/crawl' \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--data '{}'