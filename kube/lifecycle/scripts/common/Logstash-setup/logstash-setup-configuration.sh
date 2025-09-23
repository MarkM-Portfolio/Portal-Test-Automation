# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2024. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

#!/bin/bash

# Accept parameters
OS_PROTOCOL="$1"
OS_HOSTNAME="$2"
OS_INDEX_NAME="$3"
OS_USERNAME="$4"
OPENSEARCH_PASSWORD="$5"

LOGSTASH_VERSION="$6"

if [ -z "$LOGSTASH_VERSION" ]; then
    echo "Error: Logstash version not provided."
    exit 1
fi

# Install wget
echo "Install wget"
sudo yum install -y wget

# Download Logstash
echo "Download Logstash"
wget "https://artifacts.opensearch.org/logstash/logstash-oss-with-opensearch-output-plugin-${LOGSTASH_VERSION}-linux-x64.tar.gz"

# Extract Logstash
echo "Extract Logstash"
tar -zxvf "logstash-oss-with-opensearch-output-plugin-${LOGSTASH_VERSION}-linux-x64.tar.gz"

# Go to Logstash directory
echo "Go to Logstash directory"
cd "logstash-${LOGSTASH_VERSION}" || exit

# Install OpenSearch output plugin
echo "Install OpenSearch output plugin"
bin/logstash-plugin install logstash-output-opensearch

# Modifying Logstash yaml file to enable DLQ
echo "Modify Logstash yaml file to enable DLQ"
cat <<EOF >> config/logstash.yml
dead_letter_queue.enable: true
EOF

# Write pipeline.conf file
echo "Write pipeline.conf file"
cat <<EOF > /home/centos/logstash-${LOGSTASH_VERSION}/config/pipeline.conf
input {
    file {
        path => "/home/centos/logs_output/*.log"
        mode => "read"  
        start_position => "beginning"
        type => "sample"
    }
}

output {
    opensearch {
        hosts => ["${OS_PROTOCOL}://${OS_HOSTNAME}:443"]
        index => "${OS_INDEX_NAME}"
        auth_type => {
            type => "basic"
            user => "${OS_USERNAME}"
            password => "${OPENSEARCH_PASSWORD}"
        }
        ssl => true
        ssl_certificate_verification => false
    }
}

EOF

# Setup status Logstash
echo "Logstash pipeline configuration file created."

# End of script
echo "Logstash setup and configuration completed."
