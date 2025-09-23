/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2021. All Rights Reserved.       *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 *                                                                  *
 ********************************************************************
 */

/*
 * This is a library of common Jenkins funtions usable all over the *
 * Portal-Test-Automation. To load the library add the following    *
 * definitions before the pipeline declaration.                     *
 *                                                                  *
 * def commonJenkinsLib                                             *
 * def commonLibFile = "./jenkins_common/common-lib.gvy"            *
 *                                                                  *
 * The load will then be done by adding the following line into a   *
 * preparation stage before first usage.                            *
 *                                                                  *
 * commonJenkinsLib = load "${commonLibFile}"                       *
 */


/*
 * Just a test function to check if the library has been loaded
 */
def messageFromJenkinsCommonLib(message) {
    println message
}


/*
 * Get AWS Route53 zone id from given hosted zone name.
 * Return either the id if found or empty if not.
 */
def getRoute53_ZoneId(aws_hosted_zone) {
    def jsonAwsResult
    def aws_zone_id = ""
    
    // Get id for hosted zone
    def awsResult = sh(script: "aws route53 list-hosted-zones-by-name --dns-name ${aws_hosted_zone} --max-items 1", returnStdout: true).trim()
    awsResult = awsResult.replace("\n","")
    jsonAwsResult = readJSON text: awsResult
    if (jsonAwsResult.HostedZones.Name.toString() == "[${aws_hosted_zone}.]") {
       aws_zone_id = jsonAwsResult.HostedZones.Id.toString()
       aws_zone_id = aws_zone_id.replace("[/hostedzone/","")
       aws_zone_id = aws_zone_id.replace("]","")
    }
    return aws_zone_id
}


/*
 * Get AWS Route53 A-record for given hosted zone id.
 * Return either the recordSet found or empty if not.
 */
def getRoute53_Record(aws_zone_id, record_name) {
    // Add period at the end of record_name if missing
    if (!record_name.endsWith('.')) {
        record_name += "."
    }
    
    // Get the record for the given record_name
    def awsResult = sh(script: "aws route53 list-resource-record-sets --hosted-zone-id ${aws_zone_id} --query 'ResourceRecordSets[?Name == `${record_name}`]'", returnStdout: true).trim()
    awsResult = awsResult.replace("\n","")
    
    // If awsResult is not empty it just has one record which will be checked to be an A-record
    if (awsResult != "[]") {
        def jsonAwsResult = readJSON text: awsResult
        def recordSet = jsonAwsResult[0]
        if (recordSet.Type == "A") {
            return recordSet
        }
    }
    return ""
}


/*
 * Delete a provided recordset in AWS Route53 in the given zone id.
 * First create a json file with the record to delete.
 * Then use the aws client to delete that record.
 */
def deleteRoute53_Record(aws_zone_id, recordSet) {
    def deleteJSON = "delete_job.json"
    sh """
       set +x
       cat <<EOF > ./${deleteJSON}
{
    "Comment": "Deleting ${recordSet.Name}",
    "Changes": [
        {
            "Action": "DELETE",
            "ResourceRecordSet": {
                "Name": "${recordSet.Name}",
                "Type": "${recordSet.Type}",
                "TTL": ${recordSet.TTL},
                "ResourceRecords": [
                    {
                        "Value": "${recordSet.ResourceRecords[0].Value}"
                    }
                ]                
            }
        }
    ]
}
EOF
       aws route53 change-resource-record-sets --hosted-zone-id=${aws_zone_id} --change-batch file://${deleteJSON}
       rm -f ${deleteJSON}
    """
}


/*
 * Check and delete an existing A-Record for the given EC2 instance in a hosted zone.
 * By default aws_hosted_zone is treated as the name of the AWS hosted zone.
 * If prefixed by "ID:" or "NAME:" the following value is treated depending on the prefix.
 * First we need to get the zone id if aws_hosted_zone is past as name.
 * With the zone id we get the A record which then can be deleted.
 */
def checkDeleteRoute53_Record(aws_hosted_zone, record_name) {
    def resultMsg = "No A-Record found for instance ${record_name} in hosted zone ${aws_hosted_zone}"
    def aws_zone_id = ""
    
    // If aws_hosted_zone has a correct prefix change aws_hosted_zone to real name or set aws_zone_id
    if (aws_hosted_zone.toUpperCase().startsWith("NAME:") || aws_hosted_zone.toUpperCase().startsWith("ID:")) {
        def (prefix, value) = aws_hosted_zone.split(':')
        if (prefix.toUpperCase() == "ID") {
            aws_zone_id = value
        } else {
            aws_hosted_zone = value
        }
    }
    
    // Get id for hosted zone if past as name
    if (aws_zone_id == "") {
        aws_zone_id = getRoute53_ZoneId(aws_hosted_zone)
    }
    
    // Get A-record for instance
    if (aws_zone_id != "") {
        def recordSet = getRoute53_Record(aws_zone_id, record_name)
        
        // Delete A-record
        if (recordSet != "") {
            deleteRoute53_Record(aws_zone_id, recordSet)
            recordSet = getRoute53_Record(aws_zone_id, record_name)
            if (recordSet == "") {
                resultMsg = "Successfully deleted A-Record found for instance ${record_name} in hosted zone ${aws_hosted_zone}"
            } else {
                resultMsg = "Could not delete A-Record found for instance ${record_name} in hosted zone ${aws_hosted_zone}"
            }
        }
    }
    println resultMsg
}

/* Mandatory return statement on EOF */
return this
