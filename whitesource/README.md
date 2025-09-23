# Whitesource scans

We need to periodically scan our delivery artifacts for proper licensing of all external libraries we are depending on as well as run vulnerability scans on these.

## Running a Whitesource scan

To run a Whitesource scan, you basically tell this tool the Artifactory URL of the Docker image you want to scan. The tool then spins off a new EC2 instance, equips it with all necessary toolings, runs the Whitesource scan and posts these back to the Whitesource SaaS environment.

The tool only supports a predefined set of Docker images.
