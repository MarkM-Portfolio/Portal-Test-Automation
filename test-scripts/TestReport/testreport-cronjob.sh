#!/bin/sh
#
 ####################################################################
 # Licensed Materials - Property of HCL                             #
 #                                                                  #
 # Copyright HCL Technologies Ltd. 2001, 2023. All Rights Reserved. #
 #                                                                  #
 # Note to US Government Users Restricted Rights:                   #
 #                                                                  #
 # Use, duplication or disclosure restricted by GSA ADP Schedule    #
 ####################################################################
 #

LOG_FILE='testReportLog.txt'
LOG_FILE_DIR='/opt'

FIND='/usr/bin/find'
LS='/usr/bin/ls'
MV='/usr/bin/mv'
CP='/usr/bin/cp'
AWS='/usr/local/bin/aws'

DATE=`/usr/bin/date`
echo $DATE >> $LOG_FILE_DIR/$LOG_FILE
ZIP_FILES_S3=`$FIND /usr/share/nginx/html/DX-95-TestReports/s3mount/ -maxdepth 1 -type f -name \*.zip -exec /usr/bin/ls {} \;`

FOUND_FILES='false'

if [ -z $ZIP_FILES_S3 ];
then
    echo "Found no zip files in /usr/share/nginx/html/DX-95-TestReports/s3mount/ to process" >> $LOG_FILE_DIR/$LOG_FILE
else
    echo "Found the following zip files in /usr/share/nginx/html/DX-95-TestReports/s3mount/ to process" >> $LOG_FILE_DIR/$LOG_FILE
    echo $ZIP_FILES_S3 >> $LOG_FILE_DIR/$LOG_FILE
fi

for files in $ZIP_FILES_S3;
    do
        FOUND_FILES='true'
        zipFile=$(basename "$files")
        echo $zipFile
	    zipFile_timestamp=${zipFile#WP8.5_integration_extract_}
	    echo $zipFile_timestamp
        #removes the .zip extension from the file
        cd /usr/share/nginx/html/DX-95-TestReports/
        mkdir ${zipFile_timestamp%.*}
        cd ${zipFile_timestamp%.*}

        # make sure the file exists and is readable
        FILE_TO_READ='/usr/share/nginx/html/DX-95-TestReports/s3mount/'$zipFile
        if [ -r $FILE_TO_READ ];
        then
            tar -zxvf $FILE_TO_READ
            TAR_RET_CODE=$?
        else
            echo "The file" $FILE_TO_READ " either does not exists or is not readable" >> $LOG_FILE_DIR/$LOG_FILE
            echo "Exiting the shell script now" >> $LOG_FILE_DIR/$LOG_FILE
            exit 1
        fi

        if [[ $TAR_RET_CODE != 0 ]]
        then
            echo "Failed to ungzip/untar the file /usr/share/nginx/html/DX-95-TestReports/s3mount/$zipFile"  >> $LOG_FILE_DIR/$LOG_FILE
            echo "\tThe failing return code is: " $TAR_RET_CODE >> $LOG_FILE_DIR/$LOG_FILE
        else 
            echo "Successfully ungzipped and untared file /usr/share/nginx/html/DX-95-TestReports/s3mount/$zipFile"  >> $LOG_FILE_DIR/$LOG_FILE
        fi

        if echo "$zipFile" |grep '_full_frt_ctf'; then
                #If it's a Full FRT job, copy it to the 'latest' directory first.
                echo $zipFile 
                echo "Looks like a full regression run, copying to 'latest' dir before moving to unzipped directory"
                rm -rf /usr/share/nginx/html/DX-95-TestReports/latest/*
                $CP -R /usr/share/nginx/html/DX-95-TestReports/${zipFile_timestamp%.*}/* /usr/share/nginx/html/DX-95-TestReports/latest
                
                
                
                $MV /usr/share/nginx/html/DX-95-TestReports/s3mount/${zipFile} /usr/share/nginx/html/DX-95-TestReports/s3mount/unzippedTestReports
        else
                #If it's not a full job, just copy it to the unzippedTestReports directory
                echo $zipFile 
                echo "Looks like a BVT or WTF run so just copy to the unzipped directory"
                $MV /usr/share/nginx/html/DX-95-TestReports/s3mount/${zipFile} /usr/share/nginx/html/DX-95-TestReports/s3mount/unzippedTestReports
        fi 

done

if [ "$FOUND_FILES" = "true" ];
then
    echo "Found files to process" >> $LOG_FILE_DIR/$LOG_FILE
else
    echo "Found no files to process" >> $LOG_FILE_DIR/$LOG_FILE
fi

SUBDIRS='wtf iut core_api ds-regression-reports'
EXTENSIONS='html css'

for DIRECTORY in $SUBDIRS;
do
    echo "Doing the following subdirectory:" $DIRECTORY >> $LOG_FILE_DIR/$LOG_FILE
    BASE_DIR='/usr/share/nginx/html/DX-95-TestReports/s3mount/'$DIRECTORY
    for EXTENSION in $EXTENSIONS;
        do
            echo "Doing the following extension: " $EXTENSION " in directory " $DIRECTORY >> $LOG_FILE_DIR/$LOG_FILE
            echo "Looking for files of this name: *." $EXTENSION
            echo "Looking for file in this directory: " $BASE_DIR
            FILES_S3=`find $BASE_DIR -type f -name *.$EXTENSION -exec /usr/bin/ls {} \;`
            for files in $FILES_S3;
            do
                FILE_NAME=$(basename "$files")
                echo "Processing file name: " $FILE_NAME >> $LOG_FILE_DIR/$LOG_FILE
                UPPER_EXTENSION=$(echo $EXTENSION | tr '[:lower:]' '[:upper:]')
                if [ ${#DIRECTORY} -le 8 ]
                then
                # Some folks put the directory names in lower case (long directory names)i. "core_api" was the longest that was uppercased at the time of this writing
                    $MV /usr/share/nginx/html/DX-95-TestReports/s3mount/${DIRECTORY}/${FILE_NAME} /usr/share/nginx/html/DX-95-TestReports/${UPPER_EXTENSION}
                else
                    $MV /usr/share/nginx/html/DX-95-TestReports/s3mount/${DIRECTORY}/${FILE_NAME} /usr/share/nginx/html/DX-95-TestReports/${EXTENSION}
                fi
                if [[ $? != 0 ]]
                then
                    echo "Failed to mv the file /usr/share/nginx/html/DX-95-TestReports/s3mount/"$DIRECTORY"/"${FILE_NAME} >> $LOG_FILE_DIR/$LOG_FILE
                else
                    echo "Successfully mv file /usr/share/nginx/html/DX-95-TestReports/s3mount/"$DIRECTORY"/"${FILE_NAME}  >> $LOG_FILE_DIR/$LOG_FILE
                fi
            done
        done       
done

echo "Copy file: " /usr/share/nginx/html/DX-95-TestReports/s3mount/performance-test-reports/Performance-dashboard.html >> $LOG_FILE_DIR/$LOG_FILE
$AWS s3 cp s3://dx-testarea/performance-test-reports/Performance-dashboard.html /usr/share/nginx/html/DX-95-TestReports/performance-test-reports

echo "Copy file: " /usr/share/nginx/html/DX-95-TestReports/s3mount/performance-test-reports/wtf.css >> $LOG_FILE_DIR/$LOG_FILE
$AWS s3 cp s3://dx-testarea/performance-test-reports/wtf.css /usr/share/nginx/html/DX-95-TestReports/performance-test-reports

echo "Copy file: " /usr/share/nginx/html/DX-95-TestReports/s3mount/performance-test-reports/Performance_Njdc-dashboard.html >> $LOG_FILE_DIR/$LOG_FILE
$AWS s3 cp s3://dx-testarea/performance-test-reports/Performance_Njdc-dashboard.html /usr/share/nginx/html/DX-95-TestReports/performance-test-reports

echo "Copy file: " /usr/share/nginx/html/DX-95-TestReports/s3mount/performance-test-reports/wtf_njdc.css >> $LOG_FILE_DIR/$LOG_FILE
$AWS s3 cp s3://dx-testarea/performance-test-reports/wtf_njdc.css /usr/share/nginx/html/DX-95-TestReports/performance-test-reports

echo "Copy file: " /usr/share/nginx/html/DX-95-TestReports/s3mount/acceptance-test-reports/Acceptance-dashboard.html >> $LOG_FILE_DIR/$LOG_FILE
$AWS s3 cp s3://dx-testarea/acceptance-test-reports/Acceptance-dashboard.html /usr/share/nginx/html/DX-95-TestReports/acceptance-test-reports

echo "Copy file: " /usr/share/nginx/html/DX-95-TestReports/s3mount/acceptance-test-reports/wtf.css >> $LOG_FILE_DIR/$LOG_FILE
$AWS s3 cp s3://dx-testarea/acceptance-test-reports/wtf.css /usr/share/nginx/html/DX-95-TestReports/acceptance-test-reports

echo "Copy file: " /usr/share/nginx/html/DX-95-TestReports/s3mount/wtf/wtf.css >> $LOG_FILE_DIR/$LOG_FILE
$AWS s3 cp s3://dx-testarea/wtf/wtf.css /usr/share/nginx/html/DX-95-TestReports/WTF

echo "Copy file: " /usr/share/nginx/html/DX-95-TestReports/s3mount/wtf/WTF2-combined-runs.xml >> $LOG_FILE_DIR/$LOG_FILE
$AWS s3 cp s3://dx-testarea/wtf/WTF2-combined-runs.xml /usr/share/nginx/html/DX-95-TestReports/WTF

echo "Copy file: " /usr/share/nginx/html/DX-95-TestReports/s3mount/wtf/WTF2-dashboard.html >> $LOG_FILE_DIR/$LOG_FILE
$AWS s3 cp s3://dx-testarea/wtf/WTF2-dashboard.html /usr/share/nginx/html/DX-95-TestReports/WTF

echo "Copy file: " /usr/share/nginx/html/DX-95-TestReports/s3mount/wtf/WTF3-combined-runs.xml >> $LOG_FILE_DIR/$LOG_FILE
$AWS s3 cp s3://dx-testarea/wtf/WTF3-combined-runs.xml /usr/share/nginx/html/DX-95-TestReports/WTF

echo "Copy file: " /usr/share/nginx/html/DX-95-TestReports/s3mount/wtf/WTF3-dashboard.html >> $LOG_FILE_DIR/$LOG_FILE
$AWS s3 cp s3://dx-testarea/wtf/WTF3-dashboard.html /usr/share/nginx/html/DX-95-TestReports/WTF

echo "Copy file: " /usr/share/nginx/html/DX-95-TestReports/s3mount/wtf/WTF4-combined-runs.xml >> $LOG_FILE_DIR/$LOG_FILE
$AWS s3 cp s3://dx-testarea/wtf/WTF4-combined-runs.xml /usr/share/nginx/html/DX-95-TestReports/WTF

echo "Copy file: " /usr/share/nginx/html/DX-95-TestReports/s3mount/wtf/WTF4-dashboard.html >> $LOG_FILE_DIR/$LOG_FILE
$AWS s3 cp s3://dx-testarea/wtf/WTF4-dashboard.html /usr/share/nginx/html/DX-95-TestReports/WTF

echo "Copy file: " /usr/share/nginx/html/DX-95-TestReports/s3mount/wtf/WTF1FARM-combined-runs.xml >> $LOG_FILE_DIR/$LOG_FILE
$AWS s3 cp s3://dx-testarea/wtf/WTF1FARM-combined-runs.xml /usr/share/nginx/html/DX-95-TestReports/WTF

echo "Copy file: " /usr/share/nginx/html/DX-95-TestReports/s3mount/wtf/WTF1FARM-dashboard.html >> $LOG_FILE_DIR/$LOG_FILE
$AWS s3 cp s3://dx-testarea/wtf/WTF1FARM-dashboard.html /usr/share/nginx/html/DX-95-TestReports/WTF

echo "Copy file: " /usr/share/nginx/html/DX-95-TestReports/s3mount/wtf/WTF1PS-combined-runs.xml >> $LOG_FILE_DIR/$LOG_FILE
$AWS s3 cp s3://dx-testarea/wtf/WTF1PS-combined-runs.xml /usr/share/nginx/html/DX-95-TestReports/WTF

echo "Copy file: " /usr/share/nginx/html/DX-95-TestReports/s3mount/wtf/WTF1PS-dashboard.html >> $LOG_FILE_DIR/$LOG_FILE
$AWS s3 cp s3://dx-testarea/wtf/WTF1PS-dashboard.html /usr/share/nginx/html/DX-95-TestReports/WTF

echo "Copy file: " /usr/share/nginx/html/DX-95-TestReports/s3mount/iut/wtf.css >> $LOG_FILE_DIR/$LOG_FILE
$AWS s3 cp s3://dx-testarea/iut/wtf.css /usr/share/nginx/html/DX-95-TestReports/IUT

echo "Copy file: " /usr/share/nginx/html/DX-95-TestReports/s3mount/iut/IUT-combined-runs.xml >> $LOG_FILE_DIR/$LOG_FILE
$AWS s3 cp s3://dx-testarea/iut/IUT-combined-runs.xml /usr/share/nginx/html/DX-95-TestReports/IUT

echo "Copy file: " /usr/share/nginx/html/DX-95-TestReports/s3mount/iut/IUT-dashboard.html >> $LOG_FILE_DIR/$LOG_FILE
$AWS s3 cp s3://dx-testarea/iut/IUT-dashboard.html /usr/share/nginx/html/DX-95-TestReports/IUT

echo "Copy file: " /usr/share/nginx/html/DX-95-TestReports/s3mount/core_api/api.css >> $LOG_FILE_DIR/$LOG_FILE
$AWS s3 cp s3://dx-testarea/core_api/api.css /usr/share/nginx/html/DX-95-TestReports/CORE_API

echo "Copy file: " /usr/share/nginx/html/DX-95-TestReports/s3mount/core_api/API-combined-runs.xml >> $LOG_FILE_DIR/$LOG_FILE
$AWS s3 cp s3://dx-testarea/core_api/API-combined-runs.xml /usr/share/nginx/html/DX-95-TestReports/CORE_API

echo "Copy file: " /usr/share/nginx/html/DX-95-TestReports/s3mount/core_api/API-dashboard.html >> $LOG_FILE_DIR/$LOG_FILE
$AWS s3 cp s3://dx-testarea/core_api/API-dashboard.html /usr/share/nginx/html/DX-95-TestReports/CORE_API

echo "copying and extracting wtf1 dashboard files"
cd /usr/share/nginx/html/DX-95-TestReports/WTF &&  $AWS s3 cp s3://dx-testarea/wtf/WTF1.zip .  && yes | unzip WTF1.zip

echo "Test Report unzipped : $(date) $(/usr/bin/ls -1 | wc -l)" >> /usr/share/nginx/html/DX-95-TestReports/dx-report-cron-job-log.txt

#Delete test reports older than 2 weeks to save disk space
echo "Deleting test reports that are more than 2 weeks old"
find /usr/share/nginx/html/DX-95-TestReports -maxdepth 1 -type d -ctime +14 | grep -E '20[0-9]{6}-[0-9]{4}' | xargs rm -r