#! /bin/bash

NAMESPACE=$1

echo "Script begin"
APP_SCHEMA_NAME=$(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -- psql -d dxmediadb -c 'SELECT schema_name FROM information_schema.schemata order by schema_name DESC LIMIT 1;' | awk '{print $1}' | sed -n '3p' ) 
echo "APP_SCHEMA_NAME - $APP_SCHEMA_NAME"
# 01. Delete smartphone rendition record for 1st image.
echo "1. Delete smartphone rendition record for 1st image"
kubectl exec -n ${NAMESPACE} pod/dx-deployment-persistence-node-0 -- psql -d dxmediadb -c "delete from ${APP_SCHEMA_NAME}.version where rendition_id in (select id from ${APP_SCHEMA_NAME}.rendition where rendition_type = 'Smartphone' and media_item_id in (select id from ${APP_SCHEMA_NAME}.media_item where name like 'Green-Pot-1.jpg'));"
kubectl exec -n ${NAMESPACE} pod/dx-deployment-persistence-node-0 -- psql -d dxmediadb -c "delete from ${APP_SCHEMA_NAME}.rendition where rendition_type = 'Smartphone' and media_item_id in (select id from ${APP_SCHEMA_NAME}.media_item where name like 'Green-Pot-1.jpg');"

# 02. Delete smartphone version record for 2nd image.
echo "2. Delete smartphone version record for 2nd image."
kubectl exec -n ${NAMESPACE} pod/dx-deployment-persistence-node-0 -- psql -d dxmediadb -c "delete from ${APP_SCHEMA_NAME}.version where rendition_id in (select id from ${APP_SCHEMA_NAME}.rendition where rendition_type = 'Smartphone' and media_item_id in (select id from ${APP_SCHEMA_NAME}.media_item where name like 'Green-Pot-2.jpg'));"

# 03. Delete tablet rendition binary for 3rd image.
echo "3. Delete tablet rendition binary for 3rd image"
MEDIA_STORAGE_INFO=($(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -c persistence-node bash -- psql -d dxmediadb -c "select storage_location, file_id, file_extension from ${APP_SCHEMA_NAME}.media_storage as media_storage
left join ${APP_SCHEMA_NAME}.version as version on  version.media_storage_id = media_storage.id
left join (select * from ${APP_SCHEMA_NAME}.rendition where rendition_type='Tablet')  as rendition on rendition.id =version.rendition_id
left join ${APP_SCHEMA_NAME}.media_item as media_item on media_item.id = rendition.media_item_id 
where media_item.name = 'Green-Pot-3.jpg';" | awk '{print $1" "$3" "$5}'  | sed -n '3p'))

echo "MEDIA_STORAGE_INFO $MEDIA_STORAGE_INFO"
FOLDER=${MEDIA_STORAGE_INFO[0]}
FILE_NAME=${MEDIA_STORAGE_INFO[1]}
FILE_EXTENSION=${MEDIA_STORAGE_INFO[2]}
echo "rm ../upload/$FOLDER/$FILE_NAME.$FILE_EXTENSION"
kubectl exec -n ${NAMESPACE} dx-deployment-digital-asset-management-0 -- bash -c "rm ../upload/$FOLDER/$FILE_NAME.$FILE_EXTENSION"

# 04. Delete tablet rendition thumbnail binary for 4th image.
echo "4. Delete tablet rendition thumbnail binary for 4th image."

MEDIA_STORAGE_INFO_2=($(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -c persistence-node bash -- psql -d dxmediadb -c "select storage_location, file_id, file_extension from ${APP_SCHEMA_NAME}.media_storage where id in (select thumbnail_media_storage_id from ${APP_SCHEMA_NAME}.rendition where rendition_type = 'Tablet' and media_item_id in (select id from ${APP_SCHEMA_NAME}.media_item where name like 'Green-Pot-4.jpg'));" | awk '{print $1" "$3" "$5}'  | sed -n '3p')
echo "MEDIA_STORAGE_INFO_2 $MEDIA_STORAGE_INFO_2")
FOLDER_2=${MEDIA_STORAGE_INFO_2[0]}
FILE_NAME_2=${MEDIA_STORAGE_INFO_2[1]}
FILE_EXTENSION_2=${MEDIA_STORAGE_INFO_2[2]}
echo "rm ../upload/$FOLDER_2/$FILE_NAME_2.$FILE_EXTENSION_2"
kubectl exec -n ${NAMESPACE} dx-deployment-digital-asset-management-0 -- bash -c "rm ../upload/$FOLDER_2/$FILE_NAME_2.$FILE_EXTENSION_2"

# 05. Delete binary for latest version of Desktop rendition for 5th image.
echo "5. Delete binary for latest version of Desktop rendition for 5th image."
MEDIA_STORAGE_INFO_3=($(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -c persistence-node bash -- psql -d dxmediadb -c "select storage_location, file_id, file_extension from ${APP_SCHEMA_NAME}.media_storage where id in (select media_storage_id from ${APP_SCHEMA_NAME}.version where version = '2');" | awk '{print $1" "$3" "$5}'  | sed -n '3p'))

echo "MEDIA_STORAGE_INFO_3 $MEDIA_STORAGE_INFO_3"
FOLDER_3=${MEDIA_STORAGE_INFO_3[0]}
FILE_NAME_3=${MEDIA_STORAGE_INFO_3[1]}
FILE_EXTENSION_3=${MEDIA_STORAGE_INFO_3[2]}
echo "rm ../upload/$FOLDER_3/$FILE_NAME_3.$FILE_EXTENSION_3"
kubectl exec -n ${NAMESPACE} dx-deployment-digital-asset-management-0 -- bash -c "rm ../upload/$FOLDER_3/$FILE_NAME_3.$FILE_EXTENSION_3"

# 06. Delete record from media collection relation table for 6th image.
echo "6. Delete record from media collection relation table for 6th image"
kubectl exec -n ${NAMESPACE} pod/dx-deployment-persistence-node-0 -- psql -d dxmediadb -c "delete from ${APP_SCHEMA_NAME}.collection_media_relation where media_item_id in (select id from ${APP_SCHEMA_NAME}.media_item where name like 'Green-Pot-6.jpg');"

# 07. Delete directory for 7th image.
echo "7. Delete directory for 7th image"

MEDIA_STORAGE_INFO_4=($(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -c persistence-node bash -- psql -d dxmediadb -c "select storage_location, file_id, file_extension from ${APP_SCHEMA_NAME}.media_storage where id in (select thumbnail_media_storage_id from ${APP_SCHEMA_NAME}.rendition where rendition_type = 'Tablet' and media_item_id in (select id from ${APP_SCHEMA_NAME}.media_item where name like 'Green-Pot-7.jpg'));" | awk '{print $1" "$3" "$5}'  | sed -n '3p'))
echo "MEDIA_STORAGE_INFO_4 $MEDIA_STORAGE_INFO_4"
FOLDER_4=${MEDIA_STORAGE_INFO_4[0]%/*}
echo "rm -r ../upload/$FOLDER_4"
kubectl exec -n ${NAMESPACE} dx-deployment-digital-asset-management-0 -- bash -c "rm -r ../upload/$FOLDER_4"


# 08. Delete images inside directory for 8th image.
echo "8. Delete images inside directory for 8th image."
MEDIA_STORAGE_INFO_5=($(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -c persistence-node bash -- psql -d dxmediadb -c "select storage_location from ${APP_SCHEMA_NAME}.media_storage where id in (select base_media_storage_id from ${APP_SCHEMA_NAME}.media_item where name like 'Green-Pot-8.jpg');" | awk '{print $1" "$3" "$5}'  | sed -n '3p'))
echo "MEDIA_STORAGE_INFO_5 $MEDIA_STORAGE_INFO_5"
FOLDER_5=${MEDIA_STORAGE_INFO_5[0]}
echo "rm -r ../upload/$FOLDER_5"

kubectl exec -n ${NAMESPACE} dx-deployment-digital-asset-management-0 -- bash -c "rm -r ../upload/$FOLDER_5"

# 09. Create empty directory with random name.
echo "9. Create empty directory with random name."
kubectl exec -n ${NAMESPACE} dx-deployment-digital-asset-management-0 -- bash -c "mkdir -p ../upload/dx-dam-media/zzz"

# 10. Add random record in media storage table with wrong file name and path.
echo "10. Add random record in media storage table with wrong file name and path."
kubectl exec -n ${NAMESPACE} pod/dx-deployment-persistence-node-0 -- psql -d dxmediadb -c "INSERT INTO ${APP_SCHEMA_NAME}.media_storage VALUES ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa','634546cf1fbb14c2a8abc986dba3da6e','dx-dam-media/aaa/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa','filesystem','jpg','aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',50000,NULL,'2023-03-17 12:01:03.533+00', '2023-03-17 12:01:03.533+00');"
echo "Script ends"
