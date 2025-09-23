# Jenkins Backup Job

The backup job zips the Jenkins home directory as defined in `/etc/sysconfig/jenkins` but excludes `$JENKINS_HOME/workspace`, `$JENKINS_HOME/caches`, and `$JENKINS_HOME/logs`. 

In addition, the files `/etc/sysconfig/jenkins` and jenkins.war are also added to that zip.  Finally the zip is uploaded to [Artifactory](https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-jenkins-backup/).  The way how to find the jenkins.war file can be seen in [fullBackupJenkins.sh](backup_restore/fullBackupJenkins.sh) as function `getJenkinsWarLocation()`.

The backup process is split into 2 processes since the Jenkins user cannot access system directories. 

First, we have the [backup-jenkins_seeded](https://git.cwp.pnp-hcl.com/Team-Q/dx-jenkins-jobs/blob/develop/jobs/housekeeping/jobs.yaml) job which triggers the backup once a day using a technique usually used to pass build artifacts between 2 independent Jenkins jobs.  [backupJenkins.groovy](backupJenkins.groovy) prepares the backup scripts [runAsRoot.job](backup_restore/runAsRoot.job) and [fullBackupJenkins.sh](backup_restore/fullBackupJenkins.sh) and archives them as build artifact. 

The second process was created when the Jenkins master was set up, [addCronJob.sh](backup_restore/addCronJob.sh) was run by [02-install-jenkins.sh](setupscripts/02-install-jenkins.sh) as `root` on the Jenkins master which installed a root cron job to run `/root/runRootJob.sh` every 5 minutes.  This cron job searches for a `runAsRoot.job` created by any of the Jenkins jobs.  If found, it will take the value of the `ROOT_JOB=` declaration (in this case, `fullBackupJenkins.sh`) and runs that script with root access. The console output of this job is logged in `/root/runRootJob.log`.

