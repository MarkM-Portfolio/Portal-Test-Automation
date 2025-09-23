import sys

enabled =  sys.argv[0] 
synchInterval =  sys.argv[1]

nodeagents = AdminTask.listServers('[-serverType NODE_AGENT]').splitlines()
for nodeagent in nodeagents:
    configSyncSvc = AdminConfig.list('ConfigSynchronizationService', nodeagent) 
    AdminConfig.modify(configSyncSvc, '[[enable ' + enabled + '][synchInterval ' + synchInterval + '] [autoSynchEnabled '+ enabled + ']]')

AdminConfig.save()

if enabled == 'true':
    print "ActionDone: autoSynchEnabled set to true."
else:
    print "ActionDone: autoSynchEnabled set to false."
    
AdminNodeManagement.syncActiveNodes()
