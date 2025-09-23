#! /bin/bash

NAMESPACE=$1

echo "Script begin"
APP_SCHEMA_NAME=$(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -- psql -d dxmediadb -c 'SELECT schema_name FROM information_schema.schemata order by schema_name DESC LIMIT 1;' | awk '{print $1}' | sed -n '3p' ) 
echo "APP_SCHEMA_NAME - $APP_SCHEMA_NAME"

counter=1

while [ $counter -le 100 ];
do
    OPERATIONS_COUNT=($(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -c persistence-node bash -- psql -d dxmediadb -c "select count (*) from ${APP_SCHEMA_NAME}.operation where trigger_function LIKE 'syncStaging%' and (status != 'FAILED' and status != 'ABORT');" | awk '{print $1" "$3" "$5}'  | sed -n '3p'))
    echo "OPERATIONS_COUNT: $OPERATIONS_COUNT"
    if [[ $OPERATIONS_COUNT == 0 ]]; then
        break
    fi

    if [[ $counter == 100 ]]; then
        echo "No retries left"
        exit 1
    fi
    echo " retrying another $((100-$counter)) times"
    ((counter++))
    echo "Sleeping for 20 seconds ..."
    sleep 20
done

# 01. Delete smartphone version record for 3rd image 3rd collection.
echo "1. Delete smartphone version record for 3rd image 3rd collection."
kubectl exec -n ${NAMESPACE} pod/dx-deployment-persistence-node-0 -- psql -d dxmediadb -c "delete from ${APP_SCHEMA_NAME}.version where rendition_id in (select id from ${APP_SCHEMA_NAME}.rendition where rendition_type = 'Smartphone' and media_item_id in (select id from ${APP_SCHEMA_NAME}.media_item where name like 'test-image-3.jpg'));"

# 02. Delete tablet version and rendition record for 4th image 3rd collection.
echo "2. Delete tablet version and rendition record for 4th image 3rd collection"
kubectl exec -n ${NAMESPACE} pod/dx-deployment-persistence-node-0 -- psql -d dxmediadb -c "delete from ${APP_SCHEMA_NAME}.version where rendition_id in (select id from ${APP_SCHEMA_NAME}.rendition where rendition_type = 'Smartphone' and media_item_id in (select id from ${APP_SCHEMA_NAME}.media_item where name like 'test-image-4.jpg'));"
kubectl exec -n ${NAMESPACE} pod/dx-deployment-persistence-node-0 -- psql -d dxmediadb -c "delete from ${APP_SCHEMA_NAME}.rendition where rendition_type = 'Smartphone' and media_item_id in (select id from ${APP_SCHEMA_NAME}.media_item where name like 'test-image-4.jpg');"

echo "Script ends"
