# Pod Log Collection and Logstash Integration

## Overview

This README outlines the process of collecting logs from Kubernetes pods and pushing them to a production server using Logstash. Four scripts are used for this 

- `start_log_collection.sh` for collecting pod logs and  `start_logstash.sh` for starting Logstash with the appropriate configurations.
-`print_collected_pod_logfiles.sh` for display all the collected pod logs and log directory size in jenkin console.
- `stop_log_collection.sh` for stop collecting pod logs with use of killing process id of start_log_collection.sh after completion of Rendering script  execution. 
- `stop_logstash.sh` for stop logstash after sent all collected pod logs to destination  prod server.

## purpose of Log collection:

 The `start_log_collection.sh`script is responsible for continuously fetching logs from all pods within a specified namespace and storing them in a designated directory.

## Usage

1.Configuration:
   - NAMESPACE: The Kubernetes namespace `dxns` from which logs are fetched.
   - LOG_DIRECTORY: The directory `logs_output` where the fetched logs are stored.

2.Execution:
   - Run the script using bash `start_log_collection.sh`.

## Workflow

1. Namespace and Directory Setup:
   - The script initializes variables for the namespace and log directory, creating the directory if it doesn't exist.
2. Log Fetching Function:
   - Defines a function fetch_logs() to fetch logs for a given pod name.
   - Logs are retrieved using kubectl logs command.
   - If the pod is not found, an appropriate message is displayed.
   - Logs are stored in files named with the pod name and timestamp.
3. Continuous Log Collection:
   - A continuous loop fetches logs for all pods in the namespace.
   - Logs are fetched every 30 seconds.
4. Logging:
   - Output and error streams are redirected to a log file (log_collection.log).

## Purpose of start Logstash:

The `start_logstash.sh` script starts Logstash with a custom configuration file (pipeline.conf) to process and forward logs to a production server.

## Usage

1.Configuration:
   - Ensure Logstash is properly configured with input, filter, and output plugins in pipeline.conf.
2.Execution:
   - Run the script using bash start_logstash.sh.

## Workflow

1.Logstash Initialization:
  - Changes directory to Logstash installation directory.
  - Starts Logstash with the specified pipeline configuration.
2.Logging:
  - Logstash output and error streams are redirected to a log file (logstash.log).
3.PID Management:
  - Logs the PID (Process ID) of the running Logstash process to a file (logstash.pid) for future reference.

## purpose of  print collected pod logfiles

-  `print_collected_pod_logfiles.sh` The script is utilized to showcase the size of the log directory. It exhibits all the pod log files within the Jenkins console and duplicates all stored pod log files from the log directory into another folder. This facilitates anyone who wishes to ascertain the number of generated pod log files by enabling them to access and view the contents of that folder.


## purpose of stop Log collection:

 -  `stop_log_collection.sh` script is used to halt the collection of pod logs by terminating the process initiated by start_log_collection.sh after the rendering script execution is completed.

## Purpose of stop Logstash:

 - The `stop_logstash.sh` script is employed to terminate Logstash after all collected pod logs have been successfully sent to the production server.