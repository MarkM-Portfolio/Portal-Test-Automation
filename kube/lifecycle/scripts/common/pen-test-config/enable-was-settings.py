#/*
#********************************************************************
#* Licensed Materials - Property of HCL                             *
#*                                                                  *
#* Copyright HCL Technologies Ltd. 2024. All Rights Reserved.       *
#*                                                                  *
#* Note to US Government Users Restricted Rights:                   *
#*                                                                  *
#* Use, duplication or disclosure restricted by GSA ADP Schedule    *
#********************************************************************
#*/

# Enable WAS settings for penetration testing
# This script will enable the following settings:
# - Set the session cookie to be secure and httpOnly
# - Configure single sign on
# - Add custom properties to the resource environment provider
# - Save the changes

# Set the session cookie to be secure and httpOnly
# Get server object
#server = AdminConfig.getid('/Server:WebSphere_Portal/')
server = AdminConfig.getid('/Cell:dockerCell/Node:dockerNode/Server:WebSphere_Portal/')
print ("Server ID: " + server)

# Get web container object
webContainer = AdminConfig.list('WebContainer', server)
print ("Web Container: " + webContainer)

# Get session manager object
sessionManager = AdminConfig.list('SessionManager', webContainer)
print ("Session Manager: " + sessionManager)

# Get default cookie settings
cookieParams = AdminConfig.showAttribute(sessionManager, 'defaultCookieSettings')
print ("Cookies settings: " + cookieParams)

# set the cookie settings
print ("Changing cookie settings to: [maximumAge '-1'] [name 'JSESSIONID'] [useContextRootAsPath 'false'] [domain ''] [path '/'] [secure 'true'] [httpOnly 'true']")
print AdminConfig.modify(cookieParams, '[[maximumAge "-1"] [name "JSESSIONID"] [useContextRootAsPath "false"] [domain ""] [path "/"] [secure "true"] [httpOnly "true"]]')

# Configure single sign on
print ("Configure single sign on to: [enable 'true'] [domainName ''] [requiresSSL 'true'] [interoperable 'false'] [attributePropagation 'true']")
print AdminTask.configureSingleSignon(['-enable', 'true', '-domainName', '', '-requiresSSL', 'true', '-interoperable', 'false', '-attributePropagation', 'true'])

# Adding custom properties to the resource environment provider
# get resource environment provider
newrep = AdminConfig.getid('/Cell:dockerCell/Node:dockerNode/Server:WebSphere_Portal/ResourceEnvironmentProvider:WP ConfigService/')

def addCustomProperty(propSet, name, value, type):
    # Check if property exists
    if name not in AdminConfig.list('J2EEResourceProperty', newrep):
        print ("Creating new property: " + name)
        print AdminConfig.create('J2EEResourceProperty', propSet, [['name', name], ['value', value], ['type', type]])
    else:
        print ("Property already exists: " + name)

# Check if resource environment provider exists
propSet = AdminConfig.showAttribute(newrep, 'propertySet')
if (propSet == ""):
    print ("Creating new property set")
    newPropSet = AdminConfig.create('J2EEResourcePropertySet', newrep, [])
    print ("New property set created: " + newPropSet)
    addCustomProperty(newPropSet, 'csp-enabled', 'true', 'java.lang.Boolean')
    addCustomProperty(newPropSet, 'csp-header', "default-src 'self'; script-src 'self' 'nonce-default'; img-src 'self' data:; style-src 'self' 'nonce-default';", 'java.lang.String')
else:
    print ("Property set already exists: " + propSet)
    addCustomProperty(propSet, 'csp-enabled', 'true', 'java.lang.Boolean')
    addCustomProperty(propSet, 'csp-header', "default-src 'self'; script-src 'self' 'nonce-default'; img-src 'self' data:; style-src 'self' 'nonce-default';", 'java.lang.String')
        
# Save changes
print AdminConfig.save()
