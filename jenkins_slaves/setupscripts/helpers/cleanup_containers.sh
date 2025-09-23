#!/bin/bash

# We create a timestamp that matches our threshold, e.g. 48 hours ago by using date -d
threshold=$(date -d '48 hours ago' +%s)

# We run docker PS to get a fixed format list of all running containers
# Output format looks like the following pattern
# bdcd99246985 2020-09-14 12:36:04 +0000 UTC
docker ps -a -q --format "{{.ID}} {{.CreatedAt}}" | while read line
do
        # We use set to easily split up the line into its blocks
        set $line

        # The ID can be found in the first block, according to above pattern
        id=$1

        # We now use the date and time stamp to create a fitting comparison timestamp
        # The date information resides in blocks 2 (date) and 3 (time)
        containerDate=$(date -d "$(echo ${@:2:3})" +%s)

        echo "Checking container $1"

        # At last we compare our threshold date to the container date
        if [ $containerDate -le $threshold ]; then
                echo "Stopping and Removing container $1"
                docker stop $id
                docker rm $id
        fi
done