#!/bin/bash

: ${S3_BUCKET:=dx-failed-kube-deployment-logs}
: ${FILE_AGE_IN_DAYS:=7}

`aws s3 ls s3://$S3_BUCKET/ > logfiles.out`

TODAY=$(date +%j)

while read line; do
    FILEDATE=$(echo $line | awk '{print $1}')
    FILE_DATE=$(date -d $FILEDATE +%j)
    FILE_NAME=$(echo $line | awk '{print $4}')
    FILE_AGE=$(( $TODAY - $FILE_DATE ))
    if [[ $FILE_AGE -gt $FILE_AGE_IN_DAYS ]] || [[ $FILE_AGE -lt 0 ]]; then
        echo "Removing $FILE_NAME as it is $FILE_AGE days old."
        `aws s3api delete-object --bucket $S3_BUCKET --key $FILE_NAME`
    fi
done < logfiles.out