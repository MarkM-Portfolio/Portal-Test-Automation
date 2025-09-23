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
libraryTitle="ImportArtifactsLibrary"
contentTemplateTitle="ImportArtifactsContentTemplateTitle"
contentName="ImportArtifactsContentName"
styleSheetName="ImportArtifactsStylesheetsName"
siteName="ImportArtifactsContentSites"
pageName="ImportArtifactsContentSitesPage"

echo "POST Login"
curl --insecure --cookie-jar cookie.txt -s -o response.txt -w "%{http_code}\n" --location --request POST "$1/portal/cxml/04_SD9ePMtCP1I800I_KydQvyHFUBADPmuQy?userid=wpsadmin&password=wpsadmin" \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=0000zaNvk6pv3D3I16jDXVbB_2U:-1; LtpaToken2=dXQEQSyxCz/mURMDYxFSJYGwggXxyCYUquNTfDzJVCP2IUv4q157nKvaxdXCIMoVBvH1BiBujmWeRXYaFva0GTtRvTozr2OeYiVubRNrWThvzn8kvxrvbJvUmxjjSmDT7LveIQ4IrTtyKoI5xzlU7xw/ijAak3UM/hN9gr+W4tkh8RGsIKOL25ubROMk1LmnVRbpV6nYiOCDxtHhl5JKh7pC7LDplzQBsT9GOGoMxXkkvMgktGi+RFgS5Yp/KfFQ+uO2JP4jOAMAXkK1fxt+YFhJfzM84zICbtqtUaudkK4s+gwDUob/iq9N2sn6nfa2O/lTfKcmoVqmoHfMfHDNWOW1qECmvE/lEvzQmrJmYqC2Jw8nxLb44Wrsubg7Q5I2B5aAb3UmjzQI6+wh+8Ezn9A85Hfl+BsHhFoeGT52Ehj5czOS6tDCBPyZ2n+hNmWmQYlPAk/f3s94kPM/Zs+woLjCQ8x/DnvVZSupaMqlqHEGTpHGifM8yTY9PH3k1/xtq9N1eokR+p94SH3Po4SKtT/NbTzyHTtahzH2ITeJAOB06OUfQ/Lb0RrH0zarze931kSmPI+t3GbP4sN9z/jotL/HCuTw96oq4l0+PAqGjPE4kqJyMKt8b4rEzmBB4F5d1wm1Ben1FI8gIQ6UWcsAnMfoqdhH3iDJdWmwHsr86880Roum+5qVfrcX+LXjE/LQ4asKLapc51sotJsXl9+oxg==' \
--data '{
	"username":"wpsadmin",
	"password":"wpsadmin"
}'

echo "Get library type by title $libraryTitle"
curl --insecure -b cookie.txt -s -o ImportArtifactsLibrary.txt -w "%{http_code}\n" --location --request GET "$1/mycontenthandler/wcmrest/query?type=Library&title=$libraryTitle&mime-type=application/json" \
--header 'Content-Type: text/plain' \
--header 'Cookie: JSESSIONID=0000lmG0NZCvpX7miPAqmF6jSW4:-1; LtpaToken2=cD4ZjDInifWJHEu9X0WuLspqRb4xbgryuIyBMGTN8/Gp5c5gHa4MFUihyrcwloQ2FblUEpnYqFmiqYi0Agvsrj5teS7z7GSoeiTN7b8oAVRlDCm5j2l3noTJuFoU0+mes34sYJmzINMcgNYoaJIEJQ43Gk5XspBpvhk9q4vokq2hDiPQbVDRxWp2QhR5JAXyCid9QalmYPgaVkH94bfXZih/tB1caB5xZA3hZkCEnpxKTR6nr/jlQJvk/MyRZ5quK1UymB8fBMTTvLjNuHzuQ8XSSdWrhY72v2KoCn/H4YFbWVf9aZ5xvMlKiqFWhCt8NhpgqigijfZDcBNIvbTFDAvojfXpW5n8NlpUkiOqXVd7R7D7LXVIq7gMfsLAXXNz+aiOzcj3aSDVw/LbWvhCX+KP409m0CKaA0gg2vkbJD9jl7vjH718q1MvV8XZowQEgN1VeMWDFAMOmRlpYmIHUVLah9/mylkhRNaJobK7K0OiA8JtfcB+D9JJRqjOqPpyd9N/D1/E1Qgr4qQt89Pnz+i9Od0+eR16zQRTpsfLUzlssrukRtE4TbSEySK6rG4P6p4KJVr42lcnTXz3iRXUG4blprW2fes8sFTjYO+3W8aoTUlbjyUZfDqg2jI8F0BAVvKqfELUbRMsAZjaStoaYwn0mZAu1Nuzi6OuwY7IE90=' \
--data '{}'

cat ImportArtifactsLibrary.txt | jq -e 'if .feed.total == "1" then "'$libraryTitle' exists" else error("Failed to get '$libraryTitle'") end'
ImportArtifactsLibraryID=`cat ImportArtifactsLibrary.txt | jq -r '.feed.entry[0].id' | sed 's/wcmrest://'`


echo "Get content template type by title $contentTemplateTitle"
curl --insecure -b cookie.txt -s -o ImportArtifactsContentTemplate.txt -w "%{http_code}\n" --location --request GET "$1/mycontenthandler/wcmrest/query?type=ContentTemplate&title=$contentTemplateTitle&mime-type=application/json" \
--header 'Content-Type: text/plain' \
--header 'Cookie: JSESSIONID=0000lmG0NZCvpX7miPAqmF6jSW4:-1; LtpaToken2=cD4ZjDInifWJHEu9X0WuLspqRb4xbgryuIyBMGTN8/Gp5c5gHa4MFUihyrcwloQ2FblUEpnYqFmiqYi0Agvsrj5teS7z7GSoeiTN7b8oAVRlDCm5j2l3noTJuFoU0+mes34sYJmzINMcgNYoaJIEJQ43Gk5XspBpvhk9q4vokq2hDiPQbVDRxWp2QhR5JAXyCid9QalmYPgaVkH94bfXZih/tB1caB5xZA3hZkCEnpxKTR6nr/jlQJvk/MyRZ5quK1UymB8fBMTTvLjNuHzuQ8XSSdWrhY72v2KoCn/H4YFbWVf9aZ5xvMlKiqFWhCt8NhpgqigijfZDcBNIvbTFDAvojfXpW5n8NlpUkiOqXVd7R7D7LXVIq7gMfsLAXXNz+aiOzcj3aSDVw/LbWvhCX+KP409m0CKaA0gg2vkbJD9jl7vjH718q1MvV8XZowQEgN1VeMWDFAMOmRlpYmIHUVLah9/mylkhRNaJobK7K0OiA8JtfcB+D9JJRqjOqPpyd9N/D1/E1Qgr4qQt89Pnz+i9Od0+eR16zQRTpsfLUzlssrukRtE4TbSEySK6rG4P6p4KJVr42lcnTXz3iRXUG4blprW2fes8sFTjYO+3W8aoTUlbjyUZfDqg2jI8F0BAVvKqfELUbRMsAZjaStoaYwn0mZAu1Nuzi6OuwY7IE90=' \
--data '{}'

cat ImportArtifactsContentTemplate.txt | jq -e 'if .feed.total >= "1" then "'$contentTemplateTitle' exists" else error("Failed to get '$contentTemplateTitle'") end'

echo "Get content type by name $contentName"
curl --insecure -b cookie.txt -s -o ImportArtifactsContent.txt -w "%{http_code}\n" --location --request GET "$1/mycontenthandler/wcmrest-v2/contents?libraryId=$ImportArtifactsLibraryID" \
--header 'Content-Type: text/plain' \
--header 'Cookie: JSESSIONID=0000lmG0NZCvpX7miPAqmF6jSW4:-1; LtpaToken2=cD4ZjDInifWJHEu9X0WuLspqRb4xbgryuIyBMGTN8/Gp5c5gHa4MFUihyrcwloQ2FblUEpnYqFmiqYi0Agvsrj5teS7z7GSoeiTN7b8oAVRlDCm5j2l3noTJuFoU0+mes34sYJmzINMcgNYoaJIEJQ43Gk5XspBpvhk9q4vokq2hDiPQbVDRxWp2QhR5JAXyCid9QalmYPgaVkH94bfXZih/tB1caB5xZA3hZkCEnpxKTR6nr/jlQJvk/MyRZ5quK1UymB8fBMTTvLjNuHzuQ8XSSdWrhY72v2KoCn/H4YFbWVf9aZ5xvMlKiqFWhCt8NhpgqigijfZDcBNIvbTFDAvojfXpW5n8NlpUkiOqXVd7R7D7LXVIq7gMfsLAXXNz+aiOzcj3aSDVw/LbWvhCX+KP409m0CKaA0gg2vkbJD9jl7vjH718q1MvV8XZowQEgN1VeMWDFAMOmRlpYmIHUVLah9/mylkhRNaJobK7K0OiA8JtfcB+D9JJRqjOqPpyd9N/D1/E1Qgr4qQt89Pnz+i9Od0+eR16zQRTpsfLUzlssrukRtE4TbSEySK6rG4P6p4KJVr42lcnTXz3iRXUG4blprW2fes8sFTjYO+3W8aoTUlbjyUZfDqg2jI8F0BAVvKqfELUbRMsAZjaStoaYwn0mZAu1Nuzi6OuwY7IE90=' \
--data '{}'

cat ImportArtifactsContent.txt | jq -e 'if .total >= "3" then "'$contentName' exists" else error("Failed to get '$contentName'") end'

echo "Get stylesheet $styleSheetName in $libraryTitle - ID $ImportArtifactsLibraryID"
curl --insecure -b cookie.txt -s -o ImportArtifactsStylesheets.txt -w "%{http_code}\n" --location --request GET "$1/mycontenthandler/wcmrest-v2/component/stylesheets?libraryId=$ImportArtifactsLibraryID" \
--header 'Content-Type: text/plain' \
--header 'Cookie: JSESSIONID=0000lmG0NZCvpX7miPAqmF6jSW4:-1; LtpaToken2=cD4ZjDInifWJHEu9X0WuLspqRb4xbgryuIyBMGTN8/Gp5c5gHa4MFUihyrcwloQ2FblUEpnYqFmiqYi0Agvsrj5teS7z7GSoeiTN7b8oAVRlDCm5j2l3noTJuFoU0+mes34sYJmzINMcgNYoaJIEJQ43Gk5XspBpvhk9q4vokq2hDiPQbVDRxWp2QhR5JAXyCid9QalmYPgaVkH94bfXZih/tB1caB5xZA3hZkCEnpxKTR6nr/jlQJvk/MyRZ5quK1UymB8fBMTTvLjNuHzuQ8XSSdWrhY72v2KoCn/H4YFbWVf9aZ5xvMlKiqFWhCt8NhpgqigijfZDcBNIvbTFDAvojfXpW5n8NlpUkiOqXVd7R7D7LXVIq7gMfsLAXXNz+aiOzcj3aSDVw/LbWvhCX+KP409m0CKaA0gg2vkbJD9jl7vjH718q1MvV8XZowQEgN1VeMWDFAMOmRlpYmIHUVLah9/mylkhRNaJobK7K0OiA8JtfcB+D9JJRqjOqPpyd9N/D1/E1Qgr4qQt89Pnz+i9Od0+eR16zQRTpsfLUzlssrukRtE4TbSEySK6rG4P6p4KJVr42lcnTXz3iRXUG4blprW2fes8sFTjYO+3W8aoTUlbjyUZfDqg2jI8F0BAVvKqfELUbRMsAZjaStoaYwn0mZAu1Nuzi6OuwY7IE90=' \
--data '{}'

cat ImportArtifactsStylesheets.txt | jq -e 'if .total >= "1" then "'$styleSheetName' exists" else error("Failed to get '$styleSheetName'") end'


echo "Get site $siteName in $libraryTitle - ID $ImportArtifactsLibraryID"
curl --insecure -b cookie.txt -s -o ImportArtifactsContentSites.txt -w "%{http_code}\n" --location --request GET "$1/mycontenthandler/wcmrest-v2/content-sites?libraryId=$ImportArtifactsLibraryID" \
--header 'Content-Type: text/plain' \
--header 'Cookie: JSESSIONID=0000lmG0NZCvpX7miPAqmF6jSW4:-1; LtpaToken2=cD4ZjDInifWJHEu9X0WuLspqRb4xbgryuIyBMGTN8/Gp5c5gHa4MFUihyrcwloQ2FblUEpnYqFmiqYi0Agvsrj5teS7z7GSoeiTN7b8oAVRlDCm5j2l3noTJuFoU0+mes34sYJmzINMcgNYoaJIEJQ43Gk5XspBpvhk9q4vokq2hDiPQbVDRxWp2QhR5JAXyCid9QalmYPgaVkH94bfXZih/tB1caB5xZA3hZkCEnpxKTR6nr/jlQJvk/MyRZ5quK1UymB8fBMTTvLjNuHzuQ8XSSdWrhY72v2KoCn/H4YFbWVf9aZ5xvMlKiqFWhCt8NhpgqigijfZDcBNIvbTFDAvojfXpW5n8NlpUkiOqXVd7R7D7LXVIq7gMfsLAXXNz+aiOzcj3aSDVw/LbWvhCX+KP409m0CKaA0gg2vkbJD9jl7vjH718q1MvV8XZowQEgN1VeMWDFAMOmRlpYmIHUVLah9/mylkhRNaJobK7K0OiA8JtfcB+D9JJRqjOqPpyd9N/D1/E1Qgr4qQt89Pnz+i9Od0+eR16zQRTpsfLUzlssrukRtE4TbSEySK6rG4P6p4KJVr42lcnTXz3iRXUG4blprW2fes8sFTjYO+3W8aoTUlbjyUZfDqg2jI8F0BAVvKqfELUbRMsAZjaStoaYwn0mZAu1Nuzi6OuwY7IE90=' \
--data '{}'

cat ImportArtifactsContentSites.txt | jq -e 'if .total >= "1" then "'$siteName' exists" else error("Failed to get '$siteName'") end'

echo "Get site content page $pageName in $libraryTitle - ID $ImportArtifactsLibraryID"
curl --insecure -b cookie.txt -s -o ImportArtifactsContentSitesPage.txt -w "%{http_code}\n" --location --request GET "$1/mycontenthandler/wcmrest-v2/content-pages?libraryId=$ImportArtifactsLibraryID" \
--header 'Content-Type: text/plain' \
--header 'Cookie: JSESSIONID=0000lmG0NZCvpX7miPAqmF6jSW4:-1; LtpaToken2=cD4ZjDInifWJHEu9X0WuLspqRb4xbgryuIyBMGTN8/Gp5c5gHa4MFUihyrcwloQ2FblUEpnYqFmiqYi0Agvsrj5teS7z7GSoeiTN7b8oAVRlDCm5j2l3noTJuFoU0+mes34sYJmzINMcgNYoaJIEJQ43Gk5XspBpvhk9q4vokq2hDiPQbVDRxWp2QhR5JAXyCid9QalmYPgaVkH94bfXZih/tB1caB5xZA3hZkCEnpxKTR6nr/jlQJvk/MyRZ5quK1UymB8fBMTTvLjNuHzuQ8XSSdWrhY72v2KoCn/H4YFbWVf9aZ5xvMlKiqFWhCt8NhpgqigijfZDcBNIvbTFDAvojfXpW5n8NlpUkiOqXVd7R7D7LXVIq7gMfsLAXXNz+aiOzcj3aSDVw/LbWvhCX+KP409m0CKaA0gg2vkbJD9jl7vjH718q1MvV8XZowQEgN1VeMWDFAMOmRlpYmIHUVLah9/mylkhRNaJobK7K0OiA8JtfcB+D9JJRqjOqPpyd9N/D1/E1Qgr4qQt89Pnz+i9Od0+eR16zQRTpsfLUzlssrukRtE4TbSEySK6rG4P6p4KJVr42lcnTXz3iRXUG4blprW2fes8sFTjYO+3W8aoTUlbjyUZfDqg2jI8F0BAVvKqfELUbRMsAZjaStoaYwn0mZAu1Nuzi6OuwY7IE90=' \
--data '{}'

cat ImportArtifactsContentSitesPage.txt | jq -e 'if .total >= "1" then "'$pageName' exists" else error("Failed to get '$pageName'") end'
