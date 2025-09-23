cat <<EOF > ./delete_job.json
{
    "Comment": "Deleting $1",
    "Changes": [
        {
            "Action": "DELETE",
            "ResourceRecordSet": {
                "Name": "$1",
                "Type": "$2",
                "TTL": $3,
                "ResourceRecords": [
                    {
                        "Value": "$4"
                    }
                ]                
            }
        }
    ]
}
EOF