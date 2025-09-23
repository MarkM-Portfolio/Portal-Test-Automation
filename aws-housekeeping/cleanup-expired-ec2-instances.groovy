/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2021. All Rights Reserved.       *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */
import groovy.transform.Field
import groovy.time.TimeCategory
import java.time.LocalDateTime 
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Field def defaultExpireTime
@Field def currentTimeAsDate
//global map to share instance information across stages of pipeline
/*
termFlagNo: all instances which cannot be terminated
termFlagY: all instances that will be imediately terminated
noExpireSet: These will get a default expire date set
notExpireSoon: These instances are properly tagged but not within the warning period
expireSoon: These instances are properly tagged and within the warning period
expired: These instances get tagged with termFlagY
*/
def reportMap = [termFlagNo: [], termFlagY: [], noExpireSet: [], notExpireSoon: [], expireSoon: [], expired: [], brokenHousekeeping: [], popoTagEmpty: [], popoTagNA: [], popoTagTerminate: []]

 pipeline { 
    agent {
        label 'build_infra'
    }

    stages {
        stage("Prepare settings") {
            steps {
                script {
                    // We normally don't delete the entries
                    if (!env.DELETE_ENTRIES) {
                      env.DELETE_ENTRIES = "false"
                    }
                    // Sending a teams notification per default
                    if (!env.SEND_TEAMS) {
                      env.SEND_TEAMS = "true"
                    }
                    // Teams channel default webhook endpoint
                    // Points to the DevOps Community per default
                    if (!env.WEBHOOK_URL) {
                      env.WEBHOOK_URL = "https://hclo365.webhook.office.com/webhookb2/8a6712b0-0629-4fbb-9e35-641ae6c7f577@189de737-c93a-4f5a-8b68-6f4ca9941912/IncomingWebhook/306939d536234a119edcd66ad98f6cd9/02dcf8e7-5b47-496e-91d0-fc764f400ea0"
                      
                      //debug channel
                      //env.WEBHOOK_URL="https://hclo365.webhook.office.com/webhookb2/8a6712b0-0629-4fbb-9e35-641ae6c7f577@189de737-c93a-4f5a-8b68-6f4ca9941912/IncomingWebhook/49239c00bcac49e38ae9a27664ba889a/09eac2b9-2e83-489f-8e71-93c26c691407"

                    }
                    // if an instance expires in less than the specified time we note it in the message
                    if (!env.WARNING_PERIOD_HOURS) {
                      env.WARNING_PERIOD_HOURS = 24
                    }
                    // Expire date we set for instances without proper expire date
                    if (!env.GRACE_PERIOD_HOURS) {
                      env.GRACE_PERIOD_HOURS = 48
                    }
                    // if an instance in Test area is older than specified time we flag it for termination without POPO schedule being set
                    if (!env.TEST_AREA_MAX_DAYS) {
                      env.TEST_AREA_MAX_DAYS = 7
                    }

                    // if an instance in Test area is younger than the specified time we ignore it
                    if (!env.TEST_AREA_MIN_HOURS) {
                      env.TEST_AREA_MIN_HOURS = 8
                    }
                }
            }
        }
      stage('Get AWS instances with their tags') {
            steps {
                withAWS(credentials: 'aws_credentials', region: 'us-east-1') {
                    script {
                        def currentTime = System.currentTimeMillis()
                        currentTimeAsDate = new Date(currentTime)
                        def warningTime = currentTime + (env.WARNING_PERIOD_HOURS.toLong() * 60 * 60 * 1000)
                        defaultExpireTime = currentTime + (env.GRACE_PERIOD_HOURS.toLong() * 60 * 60 * 1000)
                        def brokenHousekeepingTime = currentTime - (env.WARNING_PERIOD_HOURS.toLong() * 60 * 60 * 1000)
                        def testAreaMinAge = ZonedDateTime.now().minusHours(env.TEST_AREA_MIN_HOURS.toLong());
                        def testAreaMaxAge = ZonedDateTime.now().minusHours(env.TEST_AREA_MAX_DAYS.toLong() * 24);
                      
                      
                        echo "current time: ${currentTime}"
                        echo "warning time: ${warningTime}"
                        // loop through all instances
                        def awsResult = sh(script: "aws ec2 describe-instances " + 
                            " --query 'Reservations[].Instances[].{Instance: InstanceId,LaunchTime:LaunchTime,Expires: Tags[?Key==`expires`].Value,Name: Tags[?Key==`Name`].Value,Owner: Tags[?Key==`Owner`].Value,TermFlag: Tags[?Key==`termFlag`].Value,Area: Tags[?Key==`Area`].Value,POPOSchedule: Tags[?Key==`POPOSchedule`].Value}'", 
                            returnStdout: true).trim()
                        awsResult = awsResult.replace("\n","")
                        def jsonAwsResult = readJSON text: awsResult 
                        jsonAwsResult.each {
                            //fix for empty expire via UI
                            if(it['Expires'][0] == "") {
                              it['Expires'][0] = "0"
                            }

                            // Check instances in the test area
                            if(it['Area'][0] != null && it['Area'][0].toLowerCase() == 'test') {
                              def launchTimeStamp = ZonedDateTime.parse(it['LaunchTime'], DateTimeFormatter.ISO_OFFSET_DATE_TIME)

                              // Check if instance is older than min age, to exclude transient test machines
                              if (launchTimeStamp.isBefore(testAreaMinAge)) {
                                echo "${it['Instance']} is older than minimum age - checking POPO Schedule"

                                if(it['POPOSchedule'][0] == null ) {
                                  // POPO tags are empty
                                  reportMap['popoTagEmpty'].push(it)
                                } else if (it['POPOSchedule'][0] != null && it['POPOSchedule'][0] == "") {
                                  // POPO tags are empty
                                  reportMap['popoTagEmpty'].push(it)
                                } else if (it['POPOSchedule'][0] != null && it['POPOSchedule'][0].toLowerCase() == "n/a") {
                                  // POPO tags are set to NA
                                  reportMap['popoTagNA'].push(it)
                                }
                              }

                              // Build list of old test machines to clean up

                              if (launchTimeStamp.isBefore(testAreaMaxAge)) {
                                echo "${it['Instance']} is older than max age - checking POPO Schedule"

                                if(it['POPOSchedule'][0] == null || it['POPOSchedule'][0] == "") {
                                  // POPO tags are empty
                                  reportMap['popoTagTerminate'].push(it)
                                } else if (it['POPOSchedule'][0] != null && it['POPOSchedule'][0].toLowerCase() == "n/a") {
                                  // POPO tags are set to NA
                                  reportMap['popoTagTerminate'].push(it)
                                }
                              }

                            }

                            echo "checking ${it['Instance']} for Expiries"
                            if(it['Area'][0] != null && it['Area'][0].toLowerCase() == 'support' ) {
                              reportMap['termFlagNo'].push(it)
                            } else if (it['TermFlag'][0] != null && it['TermFlag'][0].toLowerCase() == 'n') {
                              // check if we have an expire date with termFlag N that is long in the past
                              if (it['Expires'][0] != null && it['Expires'][0].toLong() < brokenHousekeepingTime && it['Expires'][0].toLong() > 1) {
                                reportMap['brokenHousekeeping'].push(it)
                              } else {
                                reportMap['termFlagNo'].push(it)
                              }
                            } else if (it['TermFlag'][0] != null && it['TermFlag'][0].toLowerCase() == 'y') {
                              reportMap['termFlagY'].push(it)
                            } else {
                              // Termflag not specified - check expire date
                              if (it['Expires'][0] == null || it['Expires'][0].toLong() < 1) {
                                // it['Expires'][0] = "${defaultExpireTime}"
                                reportMap['noExpireSet'].push(it)
                              } else if (it['Expires'][0].toLong() < currentTime) {
                                reportMap['expired'].push(it)
                              } else if (it['Expires'][0].toLong() < warningTime) {
                                reportMap['expireSoon'].push(it)
                              } else {
                                reportMap['notExpireSoon'].push(it)
                              }
                            }

                        }
                    }
                }
            }
      }

      stage('Fix tagging of instances') {
        steps {
            withAWS(credentials: 'aws_credentials', region: 'us-east-1') {
                script {
                    reportMap['noExpireSet'].each {
                        echo "fix tagging for instance ${it}"
	                    sh(script: "aws ec2 create-tags --resources ${it['Instance']} --tags Key=expires,Value=${defaultExpireTime}" , returnStdout: true)
                    }
                }
            }
        }
      }

      stage('set termFlag to expired instances') {
        steps {
            // Set TERMFLAG for expired instances
            withAWS(credentials: 'aws_credentials', region: 'us-east-1') {
                script {
                    reportMap['expired'].each {
                      echo "set termFlag=Y for ${it}"
	                    sh(script: "aws ec2 create-tags --resources ${it['Instance']} --tags Key=termFlag,Value=Y" , returnStdout: true)
                    }
                }
            }

            // Set TERMFLAG for old TEST instances with no POPO tags
            withAWS(credentials: 'aws_credentials', region: 'us-east-1') {
                script {
                    reportMap['popoTagTerminate'].each {
                      echo "set termFlag=Y for ${it}"
	                    sh(script: "aws ec2 create-tags --resources ${it['Instance']} --tags Key=termFlag,Value=Y" , returnStdout: true)
                    }
                }
            }
        }
      }
      
      stage('Check disableApiTermination attribute') {
        steps {
            script {
                def instancesToCheck = reportMap['termFlagY'].toList()
                instancesToCheck.each {
                    try {
                        echo "Checking disableApiTermination attribute for instance ${it}"

                        // Check if termination is disabled for the instance
                        def describeInstanceOutput = sh(script: "aws ec2 describe-instance-attribute --instance-id ${it['Instance']} --attribute disableApiTermination", returnStdout: true).trim()
                        def disableApiTermination = describeInstanceOutput.contains("true")

                        if (disableApiTermination) {
                            echo "disableApiTermination attribute is true for instance ${it}. Removing from cleanup list."
                            reportMap['termFlagY'].remove(it)                                            
                        } else {
                            echo "disableApiTermination attribute is false for instance ${it}."
                        }
                    } catch (error) {
                        echo "Error checking disableApiTermination attribute for instance ${it}."
                        echo "${error}"
                    }
                }
            }
        }
    }
    
      stage('terminate instance with termFlag=Y') {
        steps {
            withAWS(credentials: 'aws_credentials', region: 'us-east-1') {
                script {
                    reportMap['termFlagY'].each {
		        try {
                            echo "terminate instance ${it}"
    	                    sh(script: "aws ec2 terminate-instances --instance-ids ${it['Instance']}" , returnStdout: true)
			} catch (error) {
                            echo "Error terminating instance ${it}."
                            echo "${error}"
			}
                    }
                }
            }
        }
      }

      stage('Report to MS Teams') {
        steps {
          echo "Generate Teams Report"
          script {
            def invalidTaggingMessage = ""
            def expireSoonMessage = ""
            def expiredMessage = ""
            def terminatedMessage = ""
            def brokenHousekeepingMessage = ""
            if (reportMap['termFlagY'].size() > 0) {
              terminatedMessage = "The following instances were flagged with termFlag=Y and are therefore terminated."
              terminatedMessage = "${terminatedMessage}${prettyPrintInstanceDetails(reportMap['termFlagY'], false)}"
            } else {
              terminatedMessage = "No instance was flagged with termFlag=Y"
            }

            if (reportMap['noExpireSet'].size() > 0) {
              invalidTaggingMessage = "The following instances were not correctly tagged and therefore were set with an expire date of ${env.GRACE_PERIOD_HOURS} hours   \nPlease note the [current tagging requirements](https://pages.git.cwp.pnp-hcl.com/Team-Q/development-doc/docs/enable/portal-build/AWS-tagging-volumes#tagging)"
              invalidTaggingMessage = "${invalidTaggingMessage}${prettyPrintInstanceDetails(reportMap['noExpireSet'], true)}"
            } else {
              invalidTaggingMessage = "All instances were correctly tagged. Nice job!"
            }

            if (reportMap['expireSoon'].size() > 0) {
              expireSoonMessage = "The following instances will expire in less than ${env.WARNING_PERIOD_HOURS} hours. If you do still need an instance please adjust the expire timestamp or termFlag"
              expireSoonMessage = "${expireSoonMessage}${prettyPrintInstanceDetails(reportMap['expireSoon'], true)}"
            } else {
              expireSoonMessage = "No instances will expire soon."
            }

            if (reportMap['expired'].size() > 0) {
              expiredMessage = "The following instances expired and are therefore tagged with termFlag=Y. If you do not adjust the instance soon it will get terminated."
              expiredMessage = "${expiredMessage}${prettyPrintInstanceDetails(reportMap['expired'], false)}"
            } else {
              expiredMessage = "No instance has expired."
            }

            if (reportMap['brokenHousekeeping'].size() > 0) {
              brokenHousekeepingMessage = "The following instances expired more then ${env.WARNING_PERIOD_HOURS}h ago but are set with termFlag=N. This seems like a bug somewhere in housekeeping logic!"
              brokenHousekeepingMessage = "${brokenHousekeepingMessage}${prettyPrintInstanceDetails(reportMap['brokenHousekeeping'], true)}"
            } else {
              brokenHousekeepingMessage = "All housekeeping jobs seem to work as expected."
            }

            if (reportMap['popoTagEmpty'].size() > 0) {
              nopopoMessage = "The following instances are in TEST area but have no POPOSchedule set. Seems like they should?"
              nopopoMessage = "${nopopoMessage}${prettyPrintInstanceDetails(reportMap['popoTagEmpty'], true)}"
            } else {
              nopopoMessage = "Wow, POPO is set for all TEST instances."
            }

            if (reportMap['popoTagNA'].size() > 0) {
              naPopoMessage = "The following instances are in TEST area but have POPOSchedule set to N/A. Is that expected?"
              naPopoMessage = "${naPopoMessage}${prettyPrintInstanceDetails(reportMap['popoTagNA'], true)}"
            } else {
              naPopoMessage = "Wow, POPO is set for all TEST instances."
            }

            if (reportMap['popoTagTerminate'].size() > 0) {
              popoTerminateMessage = "The following instances are in TEST area are older than ${env.TEST_AREA_MAX_DAYS} days and have null or n/a POPOSchedule. TERMFLAG has been set to Y and these machines will be automatically terminated when this script next runs."
              popoTerminateMessage = "${popoTerminateMessage}${prettyPrintInstanceDetails(reportMap['popoTagTerminate'], true)}"
            } else {
              popoTerminateMessage = "No anomalous POPO tags on old instances."
            }



            def payload = """
              {
                  "@type": "MessageCard",
                  "@context": "http://schema.org/extensions",
                  "themeColor": "0076D7",
                  "summary": "Summary of AWS EC2 instance cleanup in us-east-1",
                  "title": "Summary of AWS EC2 instance cleanup in us-east-1",
                  "text": "Summary of AWS EC2 instance cleanup in [us-east-1](https://console.aws.amazon.com/ec2/v2/home?region=us-east-1)   \n",
                  "sections": [
                    {
                      "activityTitle": "## Broken housekeeping",
                      "text": "${brokenHousekeepingMessage}"
                    },
                    {
                      "activityTitle": "## Machines to be terminated due to missing POPO tags",
                      "text": "${popoTerminateMessage}"
                    },
                    {
                      "activityTitle": "## Absent POPO Schedule",
                      "text": "${nopopoMessage}"
                    },
                    {
                      "activityTitle": "## N/A POPO Schedule",
                      "text": "${naPopoMessage}"
                    },
                    {
                      "activityTitle": "## Terminated instances",
                      "text": "${terminatedMessage}"
                    },
                    {
                      "activityTitle": "## Expired instances",
                      "text": "${expiredMessage}"
                    },
                    {
                      "activityTitle": "## Expiring instances",
                      "text": "${expireSoonMessage}"
                    },
                    {
                      "activityTitle": "## Wrongly tagged instances",
                      "text": "${invalidTaggingMessage}"
                    }                    
                  ]
              }
            """
            echo "${payload}"
            if(env.SEND_TEAMS == "true") {
              def response = httpRequest acceptType: 'APPLICATION_JSON', contentType: 'APPLICATION_JSON', httpMode: 'POST', requestBody: payload, url: "${env.WEBHOOK_URL}", validResponseCodes: "100:599"
              println('Status: '+response.status)
              println('Response: '+response.content)  
            } else {
              echo "Teams message disabled."
            }

          }       
        }
      }
    }
    
    post {
        cleanup {
            /* Cleanup workspace */
            dir("${workspace}") {
                deleteDir()
            }

            /* Cleanup workspace@tmp */
            dir("${workspace}@tmp") {
                deleteDir()
            }
        }
    }
    
}

def String prettyPrintInstanceDetails(instances, printDate) {
  def ret = ""
  // first we sort by owner
  instanceOwner = [:]
  instances.each {
    if (instanceOwner[it['Owner'][0]] == null) {
      instanceOwner[it['Owner'][0]] = []
    }
      instanceOwner[it['Owner'][0]].push(it)
  }
  for (owner in instanceOwner.keySet().sort()) {
    ret = "${ret}   \n **${owner}:**"
    instanceOwner[owner].each {
      def potentialTimeString = ""
      if (printDate) {
        def ts
        if (it['Expires'][0] == null || it['Expires'][0].toLong() < 1) {
          ts = new Date(defaultExpireTime)
        } else {
          ts = new Date(it['Expires'][0].toLong())
        }
        def td = TimeCategory.minus( ts, currentTimeAsDate )
        if(td.toMilliseconds() < 0) {
          // switch direction format
          td = TimeCategory.minus( currentTimeAsDate, ts )
          potentialTimeString = "- ${prettyPrintTimeFormat(td)}|"
        } else {
          potentialTimeString = "${prettyPrintTimeFormat(td)}|"
        }
      }
      ret = "${ret}   \n - ${it['Name'][0]} (${potentialTimeString}${it['Instance']})"
    }
  }
  return ret
}

def String prettyPrintTimeFormat(duration) {
  def ret = ""
  if (duration.days > 0) {
    ret = "${duration.days}d"
  }
  if (duration.hours > 0 || ret.length() > 0) {
    if (ret.length() > 0) {
      // add whitespace between time sections
      ret = "${ret} "
    }
    ret = "${ret}${duration.hours}h"
  }

  if (duration.minutes > 0 || ret.length() > 0) {
    if (ret.length() > 0) {
      // add whitespace between time sections
      ret = "${ret} "
    }
    ret = "${ret}${duration.minutes}m"
  }
  return ret
}
