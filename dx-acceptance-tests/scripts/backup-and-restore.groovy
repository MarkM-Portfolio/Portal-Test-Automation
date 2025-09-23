/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2022. All Rights Reserved.       *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

/*** Logic performing the backup and restore testing in DAM ***/

// Object holding the test data references
testData = [:]

/*
 * Function to connect to the target kubernetes instance
 * Configures kubectl to be able to talk to the remote instance
 */
def connectToKubeInstance(pipelineParameters) {
    // Install kubectl so we can use it for our testing
    dxKubectlWorkspaceInstall()
    switch(pipelineParameters.KUBE_FLAVOUR) {
        // Openshift login procedure
        case 'openshift':
            dxKubectlOpenshiftConfig()
            break
        // Azure (EKS) login procedure
        case 'aws':
            dxKubectlAwsConfig()
            break
        // Azure (AKS) login procedure
        case 'azure':
            dxKubectlAzureConfig()
            break
        // Google (GKE) login procedure
        case 'google':
            dxKubectlGoogleConfig()
            break
        // Assuming native kube as default
        default:
            // Extract hostname for SSH connection
            def sshHost = pipelineParameters.PORTAL_HOST
            // Remove the protocoll from the URL
            sshHost = sshHost.substring(sshHost.indexOf('://') + 3)
            // Detect the position of port or contextroot definition
            def portIndex = sshHost.indexOf(':')
            def slashIndex = sshHost.indexOf('/')
            // If there is a port defined, we will use the port as cutoff
            if (portIndex != -1) {
                sshHost = sshHost.substring(0, portIndex)
            // If there is no port defined, we will use the first slash as cutoff
            } else {
                sshHost = sshHost.substring(0, slashIndex)
            }
            // Configure kubectl to target the remote kubectl environment
            dxKubectlNativeKubeConfig(sshTarget: sshHost)
    }
}

/*
 * Prepare content in DAM to validate backup and restore
 */
def prepareContent(uniqueId, pipelineParameters) {
    // Run login on remote DX instance
    sh """
        curl -k --fail -X POST ${pipelineParameters.EXP_API}/auth/login \
        -H 'Content-Type: application/json' \
        -d '{"username":"${pipelineParameters.USERNAME}","password":"${pipelineParameters.PASSWORD}"}' \
        --cookie-jar cookie.txt
    """
    // Create a collection that will hold our data
    def collectionData = sh(script: """
        curl -k --fail -X POST ${pipelineParameters.DAM_API}/collections \
        -H 'Content-Type: application/json' \
        -d '{"name":"testing-collection-${uniqueId}","description":"Created during backup and restore testing."}' \
        -b cookie.txt
    """, returnStdout: true).trim()
    // Parse returned data to JSON
    def collectionObject = readJSON text: collectionData
    // Save the collection ID to the test data for reference
    testData.collectionId = collectionObject.id
    // Upload a sample image to DAM
    def assetData = sh(script: """
        curl -k --fail -F files=@${workspace}/dx-acceptance-tests/scripts/data/sample.png \
        -b cookie.txt \
        ${pipelineParameters.DAM_API}/collections/${collectionObject.id}/items
    """, returnStdout: true).trim()
    def assetObject = readJSON text: assetData
    // Save the asset ID to the test data for reference
    testData.assetId = assetObject.mediaInfo.id
    // print out data references
    println(testData)
}

/*
 * Validate that the binary that was uploaded before is still present and has the same checksum
 */
def validateContent(pipelineParameters) {
    // Run login on remote DX instance
    sh """
        curl -k --fail -X POST ${pipelineParameters.EXP_API}/auth/login \
        -H 'Content-Type: application/json' \
        -d '{"username":"${pipelineParameters.USERNAME}","password":"${pipelineParameters.PASSWORD}"}' \
        --cookie-jar cookie.txt
    """
    // Download binary
    sh """
        curl -k --fail -o asset.png \
        ${pipelineParameters.DAM_API}/collections/${testData.collectionId}/items/${testData.assetId}?binary=true \
        -b cookie.txt
    """
    // Checksum of original file
    def originalChecksum = sh(script: """
        sha256sum ${workspace}/dx-acceptance-tests/scripts/data/sample.png | awk '{print \$1}'
    """, returnStdout: true).trim()
    // Checksum of downloaded file
    def downloadChecksum = sh(script: """
        sha256sum asset.png | awk '{print \$1}'
    """, returnStdout: true).trim()
    println("Original sha256 checksum: ${originalChecksum}")
    println("Downloaded sha256 checksum ${downloadChecksum}")

    if (originalChecksum == downloadChecksum) {
        println('Checksums match, validation successful!')
        // Perform a clean up
        wipeContent(pipelineParameters)
    } else {
        error('Checksums do not match, validation failed!')
    }
}

/*
 * Backup procedure following official documentation
 */
def performBackup(pipelineParameters) {
    // Exporting DAM references from DX Core
    sh """
        kubectl -n ${pipelineParameters.KUBERNETES_NAMESPACE} exec pod/${pipelineParameters.KUBERNETES_DEPLOYMENT_NAME}-core-0 -c core -- /bin/bash -c \
        "/opt/HCL/PortalServer/bin/xmlaccess.sh \
        -user ${pipelineParameters.USERNAME} \
        -password ${pipelineParameters.PASSWORD} \
        -url http://localhost:10039/wps/config \
        -in /opt/HCL/PortalServer/doc/xml-samples/ExportAllDAMCollections.xml \
        -out /tmp/damExport.xml"
    """
    // Copy the exported references into our current directory
    sh """
        kubectl cp -c core \
        ${pipelineParameters.KUBERNETES_NAMESPACE}/${pipelineParameters.KUBERNETES_DEPLOYMENT_NAME}-core-0:/tmp/damExport.xml \
        damExport.xml
    """
    // Determine primary persistence node, asking node 0 about it
        def primaryPersistenceNode = sh(script: """
            kubectl -n ${pipelineParameters.KUBERNETES_NAMESPACE} exec -c persistence-node pod/${pipelineParameters.KUBERNETES_DEPLOYMENT_NAME}-persistence-node-0 -- /bin/bash -c \
            'repmgr cluster show --compact --terse 2>/dev/null | grep primary | awk "{split(\\\$0,a,\\\"|\\\"); print a[2]}" | xargs'
        """, returnStdout: true).trim()
    // Perform the database dump on the found primary node
    sh """
        kubectl -n ${pipelineParameters.KUBERNETES_NAMESPACE} exec pod/${primaryPersistenceNode} \
        -c persistence-node -- /bin/bash -c "pg_dump dxmediadb > /tmp/dxmediadb.dmp"
    """
    // Copy the exported DB dump into our current directory
    sh """
        kubectl cp -c persistence-node \
        ${pipelineParameters.KUBERNETES_NAMESPACE}/${primaryPersistenceNode}:/tmp/dxmediadb.dmp \
        dxmediadb.dmp
    """
    // Compress DAM binaries inside the container before performing a backup
    sh """
        kubectl -n ${pipelineParameters.KUBERNETES_NAMESPACE} exec pod/${pipelineParameters.KUBERNETES_DEPLOYMENT_NAME}-digital-asset-management-0 \
        -- /bin/bash -c "tar -cvpzf /tmp/backupml.tar.gz --one-file-system --directory /opt/app/upload ."
    """
    // Copy the compressed binaries into our current directory
    sh """
        kubectl cp ${pipelineParameters.KUBERNETES_NAMESPACE}/${pipelineParameters.KUBERNETES_DEPLOYMENT_NAME}-digital-asset-management-0:/tmp/backupml.tar.gz \
        backupml.tar.gz
    """
    // List directory content for easy validation in log files
    println('Printing out workspace directory content.')
    sh "ls -lah ."
}

/*
 * Perform the restore from the files we backed up before
 */
def restoreBackup(pipelineParameters) {
    // Copy DAM collection dump to Core
    sh """
        kubectl cp -c core damExport.xml \
        ${pipelineParameters.KUBERNETES_NAMESPACE}/${pipelineParameters.KUBERNETES_DEPLOYMENT_NAME}-core-0:/tmp/damExport.xml
    """
    // Import DAM collection to core
    sh """
        kubectl -n ${pipelineParameters.KUBERNETES_NAMESPACE} exec pod/${pipelineParameters.KUBERNETES_DEPLOYMENT_NAME}-core-0 -c core -- /bin/bash -c \
        "/opt/HCL/PortalServer/bin/xmlaccess.sh \
        -user ${pipelineParameters.USERNAME} \
        -password ${pipelineParameters.PASSWORD} \
        -url http://localhost:10039/wps/config \
        -in /tmp/damExport.xml"
    """
    // Copy DAM binary archive to the DAM container
    sh """
        kubectl cp backupml.tar.gz \
        ${pipelineParameters.KUBERNETES_NAMESPACE}/${pipelineParameters.KUBERNETES_DEPLOYMENT_NAME}-digital-asset-management-0:/tmp/backupml.tar.gz \
    """
    // Unpack the DAM binary archive inside the DAM container
    sh """
        kubectl -n ${pipelineParameters.KUBERNETES_NAMESPACE} exec \
        pod/${pipelineParameters.KUBERNETES_DEPLOYMENT_NAME}-digital-asset-management-0 -- /bin/bash -c \
        "tar -mpxf /tmp/backupml.tar.gz --directory /opt/app/upload"
    """
    // Determine primary persistence node, asking node 0 about it
    def primaryPersistenceNode = sh(script: """
        kubectl -n ${pipelineParameters.KUBERNETES_NAMESPACE} exec -c persistence-node pod/${pipelineParameters.KUBERNETES_DEPLOYMENT_NAME}-persistence-node-0 -- /bin/bash -c \
        'repmgr cluster show --compact --terse 2>/dev/null | grep primary | awk "{split(\\\$0,a,\\\"|\\\"); print a[2]}" | xargs'
    """, returnStdout: true).trim()
    // Copy the database dump into the primary DB node
    sh """
        kubectl cp -c persistence-node dxmediadb.dmp ${pipelineParameters.KUBERNETES_NAMESPACE}/${primaryPersistenceNode}:/tmp/dxmediadb.dmp
    """
    // Set connection limit for DB to 0
    sh """
        kubectl -n ${pipelineParameters.KUBERNETES_NAMESPACE} exec pod/${primaryPersistenceNode} \
        -c persistence-node -- psql -c "ALTER DATABASE dxmediadb CONNECTION LIMIT 0;"
    """
    // Terminate existing connections
    sh """
        kubectl -n ${pipelineParameters.KUBERNETES_NAMESPACE} exec pod/${primaryPersistenceNode} \
        -c persistence-node -- psql -c "SELECT pg_terminate_backend(pg_stat_activity.pid) FROM pg_stat_activity WHERE pg_stat_activity.datname = 'dxmediadb' AND pid <> pg_backend_pid();"
    """
    // Drop old database
    sh """
        kubectl -n ${pipelineParameters.KUBERNETES_NAMESPACE} exec pod/${primaryPersistenceNode} \
        -c persistence-node -- dropdb dxmediadb
    """
    // Create the new DB
    sh """
        kubectl -n ${pipelineParameters.KUBERNETES_NAMESPACE} exec pod/${primaryPersistenceNode} \
        -c persistence-node -- /bin/bash -c "createdb -O dxuser dxmediadb"
    """
    // Restore DB from DB dump
    sh """
        kubectl -n ${pipelineParameters.KUBERNETES_NAMESPACE} exec pod/${primaryPersistenceNode} \
        -c persistence-node -- /bin/bash -c "psql dxmediadb < /tmp/dxmediadb.dmp"
    """
    // Restore the DB connection limits
    sh """
        kubectl -n ${pipelineParameters.KUBERNETES_NAMESPACE} exec pod/${primaryPersistenceNode} \
        -c persistence-node -- psql -c "ALTER DATABASE dxmediadb CONNECTION LIMIT 500;"
    """
    // Delete DAM Pod to force restart
    sh """
        kubectl delete pod ${pipelineParameters.KUBERNETES_DEPLOYMENT_NAME}-digital-asset-management-0 \
        -n ${pipelineParameters.KUBERNETES_NAMESPACE}
    """
    // Wait for all pods to become ready
    dxPodsCheckReadiness(namespace: pipelineParameters.KUBERNETES_NAMESPACE)
    // Print out success message for restore
    println('DB restore has been completed.')
}

/*
 * Wipe the created content from DAM, using the references we have created before
 */
def wipeContent(pipelineParameters) {
    // Run login on remote DX instance
    sh """
        curl -k --fail -X POST ${pipelineParameters.EXP_API}/auth/login \
        -H 'Content-Type: application/json' \
        -d '{"username":"${pipelineParameters.USERNAME}","password":"${pipelineParameters.PASSWORD}"}' \
        --cookie-jar cookie.txt
    """
    // Delete the uploaded media asset
    sh """
        curl -k --fail -X DELETE ${pipelineParameters.DAM_API}/collections/${testData.collectionId}/items/${testData.assetId} \
        -b cookie.txt
    """
    // Delete the created collection
    sh """
        curl -k --fail -X DELETE ${pipelineParameters.DAM_API}/collections/${testData.collectionId} \
        -b cookie.txt
    """
    // Verify that the content is not there anymore
    def returnCode = sh(script: """
        curl -k -s -o /dev/null -w "%{http_code}" ${pipelineParameters.DAM_API}/collections/${testData.collectionId}/items/${testData.assetId}
    """, returnStdout: true).trim()
    if (returnCode != '404') {
        error("Content was not wiped properly, received return code ${returnCode}")
    }
}

def performTest(pipelineParameters) {
    // Create a random number that will used to identify assets from this test
    def uniqueId = new Random().nextInt(100000)
    // Connect to the remote kubernetes instance
    connectToKubeInstance(pipelineParameters)
    // Prepare content in DAM
    prepareContent(uniqueId, pipelineParameters)
    // Perform actual backup procedure
    performBackup(pipelineParameters)
    // Remove assets again
    wipeContent(pipelineParameters)
    // Perform restore
    restoreBackup(pipelineParameters)
    // Validate content
    validateContent(pipelineParameters)
}

/* Mandatory return statement on EOF */
return this