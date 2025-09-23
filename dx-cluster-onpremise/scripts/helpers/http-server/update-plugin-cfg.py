import re
import sys

dmgrHost =  sys.argv[0] 
isCluster =  sys.argv[1] 
print 'dmgrHost =>'+ dmgrHost  

def setPluginProperty(pluginProperty, name, value, description='""'):
    print '------------------------------------------------------------------------------'
    print 'processing ' + pluginProperty
    existingProperties = AdminConfig.showAttribute(
        pluginProperty, 'properties').split()
    create = 'true'
    for existingProperty in existingProperties:

        existingProperty = re.sub('^\[', '', existingProperty)
        existingProperty = re.sub('\]$', '', existingProperty)
        if re.search(name, existingProperty):
            create = 'false'
            matchingProperty = existingProperty
    if create == 'true':
        print 'creating new plugin custom property ' + name + ' with value ' + value
        result = AdminConfig.create('Property', pluginProperty, [['name', name], [
                                    'value', value], ['description', description], ['required', 'false']])
    else:
        print 'modifying existing plugin custom property ' + name + ' with value ' + value
        result = AdminConfig.modify(matchingProperty, [['name', name], ['value', value], [
                                    'description', description], ['required', 'false']])


serverId = AdminConfig.getid('/Node:' + dmgrHost + '/Server:webserver1')
print serverId
pluginId = AdminConfig.list("PluginProperties",serverId)
print pluginId
AdminConfig.showall(serverId)
AdminConfig.modify(pluginId, '[["LogFilename" "/opt/IBM/WebSphere/ihsPlugins/logs/webserver1/http_plugin.log"]]')
AdminConfig.modify(pluginId, '[["PluginInstallRoot" "/opt/IBM/WebSphere/ihsPlugins"]]')
AdminConfig.modify(pluginId, '[["RemoteConfigFilename" "/opt/IBM/WebSphere/ihsPlugins/config/webserver1/plugin-cfg.xml"]]')
AdminConfig.modify(pluginId, '[["RemoteKeyRingFilename" "/opt/IBM/WebSphere/ihsPlugins/config/webserver1/plugin-key.kdb"]]')

pluginProperties = AdminConfig.list('PluginProperties').splitlines()
for pluginProperty in pluginProperties:
    setPluginProperty(pluginProperty, 'UseInsecure', 'true',
                      'Specifies that if you want to allow the plug-in to create unsecured connections when secure connections are defined, as was done in previous versions of Websphere Application Server, you need to create the custom property UseInsecure=true.')
    setPluginProperty(pluginProperty, 'StrictSecurity', 'true',
                      'Indicates that you want to allow the plug-in to enable security compatible with the application server FIPS SP800-131 and TLSv1.2 handshake protocol settings.')

print '+++ saving configuration +++'
result = AdminConfig.save()


if isCluster == 'true' :
    print 'synchronizing nodes'
    dmgr = AdminControl.completeObjectName('type=DeploymentManager,*')
    result = AdminControl.invoke(dmgr, 'syncActiveNodes', '[true]')
    profileConfigPath = '/opt/IBM/WebSphere/AppServer/profiles/dmgr01/config'
else:
    profileConfigPath = '/opt/IBM/WebSphere/wp_profile/config'
print 'Profile config path ' + profileConfigPath

cell = AdminControl.getCell()
nodeNames = AdminTask.listNodes().splitlines()
for nodeName in nodeNames:
  webservers = AdminTask.listServers('[-serverType WEB_SERVER -nodeName ' + nodeName + ']').splitlines()
  for webserver in webservers:
    webserverName = AdminConfig.showAttribute(webserver, 'name')
    generator = AdminControl.completeObjectName('type=PluginCfgGenerator,*')
    print "Generating plugin-cfg.xml for " + webserverName + " on " + nodeName
    result = AdminControl.invoke(generator, 'generate', profileConfigPath + ' ' + cell + ' ' + nodeName + ' ' + webserverName + ' false')
    print "Propagating plugin-cfg.xml for " + webserverName + " on " + nodeName
    result = AdminControl.invoke(generator, 'propagate', profileConfigPath + ' ' + cell + ' ' + nodeName + ' ' + webserverName)
    print "Propagating keyring for " + webserverName + " on " + nodeName
    result = AdminControl.invoke(generator, 'propagateKeyring', profileConfigPath + ' ' + cell + ' ' + nodeName + ' ' + webserverName)
    webserverCON = AdminControl.completeObjectName('type=WebServer,*')
    if isCluster == 'true' :
        print "Stopping " + webserverName + " on " + nodeName + " " + cell 
        result = AdminControl.invoke(webserverCON, 'stop', '[' + cell + ' ' + nodeName + ' ' + webserverName + ']')   
        print "Starting " + webserverName + " on " + nodeName
        result = AdminControl.invoke(webserverCON, 'start', '[' + cell + ' ' + nodeName + ' ' + webserverName + ']')

