# Logstash Setup in Kubernetes Native Environment

## What is Logstash?
  
Logstash is an open-source data processing pipeline that ingests data from multiple sources, transforms it, and sends it to your desired destination. It's part of the Elastic Stack (ELK Stack) and is widely used for log and event data processing.

## Update Pipeline Configuration

 - Modify the provided pipeline.conf file according to your requirements. This file defines the input sources, filters, and output destinations for Logstash.

## config file   

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

- `Input Configuration`: The file input plugin is configured to monitor the directory where our collected pod logs will be stored. The `path` parameter specifies the path to the directory containing the log files, while mode is set to `read` to read existing files. Logs are read from the `beginning` (start_position => "beginning"), and each log entry is tagged with the type `sample`.

- `Output Configuration`: The `opensearch` output plugin is configured to send logs to an OpenSearch server. The hosts parameter specifies the `server's protocol, hostname, and port. Additionally, the logs index, user, and password` parameters are provided for authentication. SSL encryption is enabled (ssl => true), with certificate verification disabled (ssl_certificate_verification => false).

## How to Enable Logstash Setup?

To enable Logstash setup within your Kubernetes environment, follow these steps:

- `Set Environment Variable`: Ensure ENABLE_LOGSTASH_SETUP is set to "true" in your deployment configuration to trigger Logstash installation and configuration.

   `def ENABLE_LOGSTASH_SETUP `= "true" 

This variable can be used to toggle Logstash setup within your Groovy scripts or Jenkins pipelines. You can adjust its value as needed to enable or disable Logstash setup based on your requirements.

## Example:

stage('Install Logstash, create pipeline configuration file') {
    when { expression { commonConfig.ENABLE_LOGSTASH_SETUP == "true"} }
    steps {
        script {
            host_name = "${env.INSTANCE_NAME}${env.DOMAIN_SUFFIX}"
            commonModule.logstashSetup(host_name)
        }
    }
}

- `logstash-setup-configuration.sh`: This is the shell script we are using to execute Logstash installation and setup, along with pipeline configuration creation and successful configuration

- `Define Script Parameters`: When executing the provided shell script, ensure to pass the necessary parameters