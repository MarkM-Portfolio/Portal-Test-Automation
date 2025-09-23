echo "SERVER_PROTOCOL = ${SERVER_PROTOCOL}"
echo "SERVER_HOST_PUBLISHER = ${SERVER_HOST_PUBLISHER}"
echo "SERVER_HOST_SUBSCRIBER = ${SERVER_HOST_SUBSCRIBER}"

counter=0

checkSubscriberAvailability()
{
  resCheckSubscriber=$(
    curl -s -w "%{http_code}\n" --request GET "https://${SERVER_HOST_SUBSCRIBER}/dx/api/dam/v1/collections" \
    -H 'accept: application/json'
  )
  if [ `echo $resCheckSubscriber | grep -c "200"` -lt 0 ]
  then
    counter=$(expr $counter + 1)
    echo "${SERVER_HOST_SUBSCRIBER} service unavailable waiting for 10 more seconds"
    sleep 10
    if [ $counter -lt 30 ]
    then
      checkSubscriberAvailability
    else
      echo "${SERVER_HOST_SUBSCRIBER} service unavailable after 5 mins"
      exit 1
    fi
  fi
}

checkSubscriberAvailability

resLogin=$(curl -c ./cookie.txt --location --request POST "${SERVER_PROTOCOL}://${SERVER_HOST_PUBLISHER}/dx/api/core/v1/auth/login" \
--header 'Content-Type: application/json' \
--data '{
    "username": "wpsadmin",
    "password": "wpsadmin"
}')
echo "Login Response: ${resLogin}"

resRegister=$(curl -b ./cookie.txt --location --request POST "${SERVER_PROTOCOL}://${SERVER_HOST_PUBLISHER}/dx/api/dam/v1/staging/subscriber" \
--header 'Content-Type: application/json' \
--data "{
  \"targetHost\": \""${SERVER_HOST_SUBSCRIBER}\"",
  \"cycleLength\": 2,
  \"status\": true
}")
echo "Subscriber Registration Response: ${resRegister}"
