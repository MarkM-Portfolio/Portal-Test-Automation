# Job unscheduler

This Pipeline can be used to remove all SCM and Timer triggers on a Jenkins instance.

This can be useful on instances like PJD, where we don't want Jobs to run regularly, as it is a development playground.

## Pipeline configuration

Create a declarative pipeline Job in jenkins, targeting the Jenkinsfile in this directory. It does not require any additional parameters. For best results this job should run on a meaningful schedule.

## Job configuration

By default the unscheduler always skips itself from removing its own SCM and Timer trigger. So this is nothing to care about. But the unscheduler has a configuration file **configUnscheduler.yaml** where you can define additional jobs to exclude from removing SCM and Timer triggers.

configUnscheduler.yaml:
```yaml

exclude:
  - job: "housekeeping/backup-jenkins_seeded"

```

As you can see this config file provides an exclude map having a **job:** key for each job to exclude from the SCM and Timer triggers removal process. The value for a job declaration is a string with the Jenkins jobname shown as **Full project name:** on the main Jenkins job page. In the above example Jenkins job **housekeeping/backup-jenkins_seeded** is excluded from processing.