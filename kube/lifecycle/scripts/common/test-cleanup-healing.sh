#! /bin/bash

NAMESPACE=$1
buildName=$2
buildUrl=$3

echo "Script begin"
APP_SCHEMA_NAME=$(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -- psql -d dxmediadb -c 'SELECT schema_name FROM information_schema.schemata order by schema_name DESC LIMIT 1;' | awk '{print $1}' | sed -n '3p' )
echo "APP_SCHEMA_NAME - $APP_SCHEMA_NAME"

TOTAL_COUNT=12
# HEALING_COUNT variable keeps count of number of healed or cleaned up items
HEALING_COUNT=0

SCAN_STATUS_1=($(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -c persistence-node bash -- psql -d dxmediadb -c "select scan_status from ${APP_SCHEMA_NAME}.media_item where name like 'Green-Pot-1.jpg';" | awk '{print $1" "$3" "$5}'  | sed -n '3p'))
echo "SCAN_STATUS_1 $SCAN_STATUS_1"
if [ "$SCAN_STATUS_1" == "VALIDATION_COMPLETED" ]; then
    ((HEALING_COUNT++))
    echo "1st image validation completed"
fi

SCAN_STATUS_2=($(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -c persistence-node bash -- psql -d dxmediadb -c "select scan_status from ${APP_SCHEMA_NAME}.media_item where name like 'Green-Pot-2.jpg';" | awk '{print $1" "$3" "$5}'  | sed -n '3p'))
echo "SCAN_STATUS_2 $SCAN_STATUS_2"

if [ "$SCAN_STATUS_2" == "VALIDATION_COMPLETED" ]; then
    ((HEALING_COUNT++))
    echo "2nd image validation completed"
fi

SCAN_STATUS_3=($(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -c persistence-node bash -- psql -d dxmediadb -c "select scan_status from ${APP_SCHEMA_NAME}.media_item where name like 'Green-Pot-3.jpg';" | awk '{print $1" "$3" "$5}'  | sed -n '3p'))
echo "SCAN_STATUS_3 $SCAN_STATUS_3"

if [ "$SCAN_STATUS_3" == "VALIDATION_COMPLETED" ]; then
    ((HEALING_COUNT++))
    echo "3rd image validation completed"
fi

SCAN_STATUS_4=($(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -c persistence-node bash -- psql -d dxmediadb -c "select scan_status from ${APP_SCHEMA_NAME}.media_item where name like 'Green-Pot-4.jpg';" | awk '{print $1" "$3" "$5}'  | sed -n '3p'))
echo "SCAN_STATUS_4 $SCAN_STATUS_4"

if [ "$SCAN_STATUS_4" == "VALIDATION_COMPLETED" ]; then
    ((HEALING_COUNT++))
    echo "4th image validation completed"
fi

SCAN_STATUS_5=($(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -c persistence-node bash -- psql -d dxmediadb -c "select scan_status from ${APP_SCHEMA_NAME}.media_item where name like 'Green-Pot-5.jpg';" | awk '{print $1" "$3" "$5}'  | sed -n '3p'))
echo "SCAN_STATUS_5 $SCAN_STATUS_5"

if [ "$SCAN_STATUS_5" == "VALIDATION_COMPLETED" ]; then
    ((HEALING_COUNT++))
    echo "5th image validation completed"
fi

COUNT_IMG_6=($(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -c persistence-node bash -- psql -d dxmediadb -c "select count (*) from ${APP_SCHEMA_NAME}.media_item where name like 'Green-Pot-6.jpg';" | awk '{print $1" "$3" "$5}'  | sed -n '3p'))
echo "COUNT_IMG_6 ${COUNT_IMG_6[0]}"

if [ "${COUNT_IMG_6[0]}" == "0" ]; then
    ((HEALING_COUNT++))
    echo "6th image cleaned up"
fi

COUNT_IMG_7=($(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -c persistence-node bash -- psql -d dxmediadb -c "select count (*) from ${APP_SCHEMA_NAME}.media_item where name like 'Green-Pot-7.jpg';" | awk '{print $1" "$3" "$5}'  | sed -n '3p'))
echo "COUNT_IMG_7 ${COUNT_IMG_7[0]}"

if [ "${COUNT_IMG_7[0]}" == "0" ]; then
    ((HEALING_COUNT++))
    echo "7th image cleaned up"
fi

COUNT_IMG_8=($(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -c persistence-node bash -- psql -d dxmediadb -c "select count (*) from ${APP_SCHEMA_NAME}.media_item where name like 'Green-Pot-8.jpg';" | awk '{print $1" "$3" "$5}'  | sed -n '3p'))
echo "COUNT_IMG_8 ${COUNT_IMG_8[0]}"

if [ "${COUNT_IMG_8[0]}" == "0" ]; then
    ((HEALING_COUNT++))
    echo "8th image cleaned up"
fi

SCAN_STATUS_9=($(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -c persistence-node bash -- psql -d dxmediadb -c "select scan_status from ${APP_SCHEMA_NAME}.media_item where name like 'Green-Pot-4.jpg';" | awk '{print $1" "$3" "$5}'  | sed -n '3p'))
echo "SCAN_STATUS_9 $SCAN_STATUS_9"

if [ "$SCAN_STATUS_9" == "VALIDATION_COMPLETED" ]; then
    ((HEALING_COUNT++))
    echo "9th image validation completed"
fi

SCAN_STATUS_10=($(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -c persistence-node bash -- psql -d dxmediadb -c "select scan_status from ${APP_SCHEMA_NAME}.media_item where name like 'Green-Pot-5.jpg';" | awk '{print $1" "$3" "$5}'  | sed -n '3p'))
echo "SCAN_STATUS_10 $SCAN_STATUS_10"

if [ "$SCAN_STATUS_10" == "VALIDATION_COMPLETED" ]; then
    ((HEALING_COUNT++))
    echo "10th image validation completed"
fi
echo "HEALING_COUNT - $HEALING_COUNT"

# To check if empty directory with random name is removed, we try to change to that directory by "cd ../upload/dx-dam-media/zzz" and if that fails we increment the HEALING_COUNT
(kubectl exec -n $NAMESPACE dx-deployment-digital-asset-management-0 -- bash -c "cd ../upload/dx-dam-media/zzz") || (((HEALING_COUNT++)))
echo "HEALING_COUNT - $HEALING_COUNT"

COUNT_MEDIA_STORAGE_RANDOM_ENTRY=($(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -c persistence-node bash -- psql -d dxmediadb -c "select count (*) from ${APP_SCHEMA_NAME}.media_storage where id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';" | awk '{print $1" "$3" "$5}'  | sed -n '3p'))
echo "COUNT_MEDIA_STORAGE_RANDOM_ENTRY ${COUNT_MEDIA_STORAGE_RANDOM_ENTRY[0]}"

if [ "${COUNT_MEDIA_STORAGE_RANDOM_ENTRY[0]}" == "0" ]; then
    ((HEALING_COUNT++))
    echo "Random media storage entry is cleaned up"
fi

echo "HEALING_COUNT - $HEALING_COUNT"

FAILED_COUNT=$((TOTAL_COUNT - HEALING_COUNT))

# write to xml
# Check if Dam_Cleanup_Tests_Results.xml exists and remove it
if [ -e "/home/centos/native-kube/Dam_Cleanup_Tests_Results.xml" ]; then
    rm "/home/centos/native-kube/Dam_Cleanup_Tests_Results.xml"
    echo "Existing Dam_Cleanup_Tests_Results.xml removed."
fi


echo " <dam_cleanup_test buildName="\"${buildName}\"" buildUrl="\"${buildUrl}\"" tests="\"${TOTAL_COUNT}\"" failures="\"${FAILED_COUNT}\""/> ">> /home/centos/native-kube/Dam_Cleanup_Tests_Results.xml

if [ $HEALING_COUNT -eq 12 ]; then
    echo "Clean up and Regeneration of renditions and versions are SUCCESSFUL !"
else
    echo "Clean up and Regeneration of renditions and versions are FAILED !"
    exit 1
fi
echo "Script ends"
