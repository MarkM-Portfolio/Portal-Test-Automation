print "Stopping nodeAgents."
nodeagents = AdminControl.queryNames('type=NodeAgent,*').splitlines()
for nodeagent in nodeagents:
    print nodeagent
    AdminControl.invoke(nodeagent,'stopNode')

print "ActionDone: Nodeagents are stopped."
