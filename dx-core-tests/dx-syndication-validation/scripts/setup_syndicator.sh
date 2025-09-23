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
    echo "Syndicator IP is : "$SYNDICATOR_IP
    echo "Subscriber IP is : "$SUBSCRIBER_IP
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
./xmlaccess.sh -in CreateVaultSlot.xml -out slot-out.xml -url http://${SYNDICATOR_IP}:10039/wps/config -user wpsadmin -password wpsadmin

echo "Import libraries"
cd /opt/IBM/WebSphere/wp_profile/ConfigEngine
./ConfigEngine.sh import-wcm-data -DWasPassword=wpsadmin -DPortalAdminPwd=wpsadmin -Dimport.directory="/opt/zips/SyndicationCrossReferenceLibraries;/opt/zips/SurfCityLibrary"

sleep 20

# Login
echo "POST Login"
curl --cookie-jar cookie.txt -s -o response.txt -w "%{http_code}" --location --request POST "http://${SYNDICATOR_IP}:10039/wps/portal/cxml/04_SD9ePMtCP1I800I_KydQvyHFUBADPmuQy?userid=wpsadmin&password=wpsadmin" \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=0000zaNvk6pv3D3I16jDXVbB_2U:-1; LtpaToken2=dXQEQSyxCz/mURMDYxFSJYGwggXxyCYUquNTfDzJVCP2IUv4q157nKvaxdXCIMoVBvH1BiBujmWeRXYaFva0GTtRvTozr2OeYiVubRNrWThvzn8kvxrvbJvUmxjjSmDT7LveIQ4IrTtyKoI5xzlU7xw/ijAak3UM/hN9gr+W4tkh8RGsIKOL25ubROMk1LmnVRbpV6nYiOCDxtHhl5JKh7pC7LDplzQBsT9GOGoMxXkkvMgktGi+RFgS5Yp/KfFQ+uO2JP4jOAMAXkK1fxt+YFhJfzM84zICbtqtUaudkK4s+gwDUob/iq9N2sn6nfa2O/lTfKcmoVqmoHfMfHDNWOW1qECmvE/lEvzQmrJmYqC2Jw8nxLb44Wrsubg7Q5I2B5aAb3UmjzQI6+wh+8Ezn9A85Hfl+BsHhFoeGT52Ehj5czOS6tDCBPyZ2n+hNmWmQYlPAk/f3s94kPM/Zs+woLjCQ8x/DnvVZSupaMqlqHEGTpHGifM8yTY9PH3k1/xtq9N1eokR+p94SH3Po4SKtT/NbTzyHTtahzH2ITeJAOB06OUfQ/Lb0RrH0zarze931kSmPI+t3GbP4sN9z/jotL/HCuTw96oq4l0+PAqGjPE4kqJyMKt8b4rEzmBB4F5d1wm1Ben1FI8gIQ6UWcsAnMfoqdhH3iDJdWmwHsr86880Roum+5qVfrcX+LXjE/LQ4asKLapc51sotJsXl9+oxg==' \
--data '{
	"username":"wpsadmin",
	"password":"wpsadmin"
}'

# Create library to be syndicated
echo "Create Syndication Library"
curl -b cookie.txt -s  -o SyndicationLibrary.txt -w "%{http_code}" --location --request POST "http://${SYNDICATOR_IP}:10039/wps/mycontenthandler/wcmrest/Library?mime-type=application/json" \
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



sleep 30
