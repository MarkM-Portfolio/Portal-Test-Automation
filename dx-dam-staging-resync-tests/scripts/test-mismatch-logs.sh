#! /bin/bash

NAMESPACE=$1

echo "Script begin"
APP_SCHEMA_NAME=$(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -- psql -d dxmediadb -c 'SELECT schema_name FROM information_schema.schemata order by schema_name DESC LIMIT 1;' | awk '{print $1}' | sed -n '3p' )
echo "APP_SCHEMA_NAME - $APP_SCHEMA_NAME"


# MISMATCH_COUNT variable keeps count of number of mismatched items b/w publisher and subscriber env
MISMATCH_COUNT=0

# delete the collection-test-1 in subscriber env
# A resync log with action create and type collection should be created for first collection.
COLLECTION_CREATE_LOGS=($(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -c persistence-node bash -- psql -d dxmediadb -c "select count (*) from ${APP_SCHEMA_NAME}.staging_mismatch_log where type='COLLECTION' and action='CREATE';" | awk '{print $1" "$3" "$5}'  | sed -n '3p'))
echo "COLLECTION_CREATE_LOGS $COLLECTION_CREATE_LOGS"

if [ $COLLECTION_CREATE_LOGS == 1 ]; then
    ((MISMATCH_COUNT++))
fi

# update the collection name 'collection-test-2' to 'collection-test-updated' in subscriber env
# A resync log with action update and type collection should be created for second collection.
COLLECTION_UPDATE_LOGS=($(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -c persistence-node bash -- psql -d dxmediadb -c "select count (*) from ${APP_SCHEMA_NAME}.staging_mismatch_log where type='COLLECTION' and action='UPDATE';" | awk '{print $1" "$3" "$5}'  | sed -n '3p'))
echo "COLLECTION_UPDATE_LOGS: $COLLECTION_UPDATE_LOGS"

if [ $COLLECTION_UPDATE_LOGS == 1 ]; then
    ((MISMATCH_COUNT++))
fi

# add new collection 'collection-test-5' in subscriber env
# A resync log with action delete and type collection should be created for fifth collection.
COLLECTION_DELETE_LOGS=($(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -c persistence-node bash -- psql -d dxmediadb -c "select count (*) from ${APP_SCHEMA_NAME}.staging_mismatch_log where type='COLLECTION' and action='DELETE';" | awk '{print $1" "$3" "$5}'  | sed -n '3p'))
echo "COLLECTION_DELETE_LOGS: $COLLECTION_DELETE_LOGS"

if [ $COLLECTION_DELETE_LOGS == 1 ]; then
    ((MISMATCH_COUNT++))
fi

# delete mediaItem 'test-image-1' in subscriber env
# A resync log with action create and type media item should be created for first asset.
MEDIA_ITEM_CREATE_LOGS=($(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -c persistence-node bash -- psql -d dxmediadb -c "select count (*) from ${APP_SCHEMA_NAME}.staging_mismatch_log where type='MEDIA_ITEM' and action='CREATE';" | awk '{print $1" "$3" "$5}'  | sed -n '3p'))
echo "MEDIA_ITEM_CREATE_LOGS: $MEDIA_ITEM_CREATE_LOGS"

if [ $MEDIA_ITEM_CREATE_LOGS == 1 ]; then
    ((MISMATCH_COUNT = $MISMATCH_COUNT+ $MEDIA_ITEM_CREATE_LOGS))
fi

# delete smartphone version record for 3rd image 3rd collection
# A resync log with action create and type version should be created for smartphone version related to third asset.
VERSION_CREATE_LOGS=($(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -c persistence-node bash -- psql -d dxmediadb -c "select count (*) from ${APP_SCHEMA_NAME}.staging_mismatch_log where type='VERSION' and action='CREATE';" | awk '{print $1" "$3" "$5}'  | sed -n '3p'))
echo "VERSION_CREATE_LOGS: $VERSION_CREATE_LOGS"

if [ $VERSION_CREATE_LOGS == 6 ]; then
    ((MISMATCH_COUNT = $MISMATCH_COUNT+ $VERSION_CREATE_LOGS))
fi

# delete tablet version and rendition record for 4th image 'test-image-4'
# A couple of resync log with action create and type version along with rendition should be created for version and rendition related to fourth asset.
RENDITION_CREATE_LOGS=($(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -c persistence-node bash -- psql -d dxmediadb -c "select count (*) from ${APP_SCHEMA_NAME}.staging_mismatch_log where type='RENDITION' and action='CREATE';" | awk '{print $1" "$3" "$5}'  | sed -n '3p'))
echo "RENDITION_CREATE_LOGS: $RENDITION_CREATE_LOGS"

if [ $RENDITION_CREATE_LOGS == 5 ]; then
    ((MISMATCH_COUNT = $MISMATCH_COUNT+ $RENDITION_CREATE_LOGS))
fi



# remove keywords from mediaItem 'test-image-5' in subscriber env
KEYWORD_CREATE_LOGS=($(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -c persistence-node bash -- psql -d dxmediadb -c "select count (*) from ${APP_SCHEMA_NAME}.staging_mismatch_log where type='KEYWORD' and action='CREATE';" | awk '{print $1" "$3" "$5}'  | sed -n '3p'))
echo "KEYWORD_CREATE_LOGS: $KEYWORD_CREATE_LOGS"

if [ $KEYWORD_CREATE_LOGS == 1 ]; then
    ((MISMATCH_COUNT++))
fi

# remove mediaItem 'test-image-6.jpg' from favorites in subscriber env
# A resync log with action create and type favorite should be created for sixth asset.
FAVORITE_CREATE_LOGS=($(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -c persistence-node bash -- psql -d dxmediadb -c "select count (*) from ${APP_SCHEMA_NAME}.staging_mismatch_log where type='FAVORITE' and action='CREATE';" | awk '{print $1" "$3" "$5}'  | sed -n '3p'))
echo "FAVORITE_CREATE_LOGS: $FAVORITE_CREATE_LOGS"

if [ $FAVORITE_CREATE_LOGS == 1 ]; then
    ((MISMATCH_COUNT++))
fi

# change custom url for mediaItem 'test-image-7' from '/test' to '/testUpdated'
CUSTOM_URL_DELETE_LOGS=($(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -c persistence-node bash -- psql -d dxmediadb -c "select count (*) from ${APP_SCHEMA_NAME}.staging_mismatch_log where type='CUSTOM_URL' and action='DELETE';" | awk '{print $1" "$3" "$5}'  | sed -n '3p'))
echo "CUSTOM_URL_DELETE_LOGS: $CUSTOM_URL_DELETE_LOGS"

if [ $CUSTOM_URL_DELETE_LOGS == 1 ]; then
    ((MISMATCH_COUNT++))
fi

CUSTOM_URL_CREATE_LOGS=($(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -c persistence-node bash -- psql -d dxmediadb -c "select count (*) from ${APP_SCHEMA_NAME}.staging_mismatch_log where type='CUSTOM_URL' and action='CREATE';" | awk '{print $1" "$3" "$5}'  | sed -n '3p'))
echo "CUSTOM_URL_CREATE_LOGS: $CUSTOM_URL_CREATE_LOGS"

if [ $CUSTOM_URL_CREATE_LOGS == 1 ]; then
    ((MISMATCH_COUNT++))
fi

# give anonymous access to collection-test-4 in publisher env, then remove same from subscriber env
PERMISSION_CREATE_LOGS=($(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -c persistence-node bash -- psql -d dxmediadb -c "select count (*) from ${APP_SCHEMA_NAME}.staging_mismatch_log where type='PERMISSION' and action='CREATE';" | awk '{print $1" "$3" "$5}'  | sed -n '3p'))
echo "PERMISSION_CREATE_LOGS: $PERMISSION_CREATE_LOGS"

if [ $PERMISSION_CREATE_LOGS == 1 ]; then
    ((MISMATCH_COUNT++))
fi

# give access permission to All authenticated DX users to the collection-test-5 in Subscriber env
PERMISSION_DELETE_LOGS=($(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -c persistence-node bash -- psql -d dxmediadb -c "select count (*) from ${APP_SCHEMA_NAME}.staging_mismatch_log where type='PERMISSION' and action='DELETE';" | awk '{print $1" "$3" "$5}'  | sed -n '3p'))
echo "PERMISSION_DELETE_LOGS: $PERMISSION_DELETE_LOGS"

if [ $PERMISSION_DELETE_LOGS == 1 ]; then
    ((MISMATCH_COUNT++))
fi

# delete the resource associated with collection-test-6 in subscriber
RESOURCE_CREATE_LOGS=($(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -c persistence-node bash -- psql -d dxmediadb -c "select count (*) from ${APP_SCHEMA_NAME}.staging_mismatch_log where type='RESOURCE' and action='CREATE';" | awk '{print $1" "$3" "$5}'  | sed -n '3p'))
echo "RESOURCE_CREATE_LOGS: $RESOURCE_CREATE_LOGS"

if [ $RESOURCE_CREATE_LOGS == 1 ]; then
    ((MISMATCH_COUNT++))
fi

echo "mismatched items b/w publisher and subscriber env - $MISMATCH_COUNT"
if [ $MISMATCH_COUNT -eq 22 ]; then
    echo "Logging of staging mismatched items SUCCESSFUL !"
else
    echo "Logging of staging mismatched items FAILED !"
    exit 1
fi
echo "Script ends"