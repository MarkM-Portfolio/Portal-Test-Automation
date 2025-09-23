# Jenkins common functions library

This directory contains the common-lib.gvy library and a test-pipleine which can be used to test library functions.

The library can be added to a pipeline by using the load step within any stage prior to using any of its functions.

Therefore you need to add the following definitions before the pipeline declaration.

```
def commonJenkinsLib
def commonLibFile = "./jenkins_common/common-lib.gvy"
```

The load will then be done by adding the following line into a preparation stage before first usage.

```
commonJenkinsLib = load "${commonLibFile}"
```

## Using a lib function in a pipeline

After the common-lib has been loaded it can be used just like imported package functions.

```
commonJenkinsLib.<function_name>
```

## Exposed functions

Here you find all functions currently exposed by the common-lib.

- messageFromJenkinsCommonLib(message)
- getRoute53_ZoneId(aws_hosted_zone)
- getRoute53_Record(aws_zone_id, record_name)
- deleteRoute53_Record(aws_zone_id, recordSet)
- checkDeleteRoute53_Record(aws_hosted_zone, record_name)
