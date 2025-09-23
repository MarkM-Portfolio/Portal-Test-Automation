/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2024. All Rights Reserved.       *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

 // Import the shared library with the name configured in Jenkins
@Library("dx-shared-library") _

// Map for pipeline parameters
def pipelineParameters = [:]

pipeline { 
    agent {
        label 'build_infra'
    }

    stages {
        // Load the pipeline parameters into object
        stage('Load parameters') {
            steps {
                script {
                    dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/kube/rosa/parameters.yaml")
                }
            }
        }

        // Install ROSA CLI in our workspace
        stage("Install ROSA CLI") {
            steps {
                script {
                    echo "Configure ROSA env, Check connection to ROSA."
                    withCredentials([
                        string(credentialsId: 'openshift_rosa_api_token', variable: 'ROSA_API_TOKEN')
                    ]) {       
                        sh """
                            cd ${workspace}
                            mkdir -p rosa-client
                            (curl -L ${pipelineParameters.ROSA_CLI_URL} > ./rosa-client/rosa-linux.tar.gz) > /dev/null 2>&1
                            cd rosa-client && tar -xvf rosa-linux.tar.gz > /dev/null 2>&1
                            export PATH="$PATH:${workspace}/rosa-client"
                            chmod -R 750 ./
                            rosa login --token="${ROSA_API_TOKEN}" --region="${pipelineParameters.CLUSTER_REGION}"
                        """

                        // Adjust the env variable globally, so that the rosa client is easily accessible
                        env.PATH = "$PATH:${workspace}/rosa-client"

                        // Perform a quick test
                        sh 'rosa version'
                    }
                }
            }
        }

        // Create ROSA Cluster via ROSA CLI
        stage("Create ROSA Cluster") {
            when {
                expression { pipelineParameters.CLUSTER_MANAGER_MODE.equalsIgnoreCase("Create") }
            }
            steps {
                script {
                    withCredentials([[
                        $class: 'UsernamePasswordMultiBinding',
                        credentialsId: "openshift_rosa_credentials",
                        usernameVariable: 'USERNAME', 
                        passwordVariable: 'PASSWORD'
                    ]]) {
                        // Discourge cluster name longer than 15 characters
                        if (pipelineParameters.CLUSTER_NAME.length() > 15) {
                            error("Cluster name longer than 15 characters is not recommended: ${pipelineParameters.CLUSTER_NAME}")
                        }

                        sh """
                            export PATH="$PATH:${workspace}/rosa-client"

                            echo "Checking if cluster ${pipelineParameters.CLUSTER_NAME} already exists..."
                            if rosa describe cluster --region="${pipelineParameters.CLUSTER_REGION}" --cluster="${pipelineParameters.CLUSTER_NAME}"; then
                                echo "Cluster ${pipelineParameters.CLUSTER_NAME} already exists. Skipping creation."
                            else
                                set +vx
                                echo "Verifying ROSA prerequisites..."
                                echo "Check if AWS CLI is installed."; aws --version
                                echo "Check if ROSA CLI is installed."; rosa version
                                echo "Check if ELB service role is enabled."
                                if ! aws iam get-role --role-name "AWSServiceRoleForElasticLoadBalancing" --no-cli-pager; then
                                    aws iam create-service-linked-role --aws-service-name "elasticloadbalancing.amazonaws.com"
                                fi
                                rosa whoami --region="${pipelineParameters.CLUSTER_REGION}"
                                rosa verify quota --region="${pipelineParameters.CLUSTER_REGION}"

                                echo "Installing ROSA cluster ${pipelineParameters.CLUSTER_NAME}"

                                ROSA_CMD="rosa create cluster \
                                --region="${pipelineParameters.CLUSTER_REGION}" \
                                --sts \
                                --cluster-name ${pipelineParameters.CLUSTER_NAME} \
                                --version ${pipelineParameters.CLUSTER_VERSION} \
                                --cluster-admin-password $PASSWORD \
                                --role-arn arn:aws:iam::${pipelineParameters.ROSA_AWS_ACCOUNT}:role/ManagedOpenShift-Installer-Role \
                                --support-role-arn arn:aws:iam::${pipelineParameters.ROSA_AWS_ACCOUNT}:role/ManagedOpenShift-Support-Role \
                                --controlplane-iam-role arn:aws:iam::${pipelineParameters.ROSA_AWS_ACCOUNT}:role/ManagedOpenShift-ControlPlane-Role \
                                --worker-iam-role arn:aws:iam::${pipelineParameters.ROSA_AWS_ACCOUNT}:role/ManagedOpenShift-Worker-Role \
                                --operator-roles-prefix ${pipelineParameters.CLUSTER_NAME} \
                                --tags "Area:${pipelineParameters.RESOURCE_TAG_AREA},Owner:${pipelineParameters.RESOURCE_TAG_OWNER},termFlag:${pipelineParameters.RESOURCE_TAG_TERM_FLAG},Savings:${pipelineParameters.RESOURCE_TAG_SAVINGS}" \
                                --region ${pipelineParameters.CLUSTER_REGION} \
                                --ec2-metadata-http-tokens optional \
                                --enable-autoscaling \
                                --min-replicas 2 \
                                --max-replicas ${pipelineParameters.ROSA_WORKER_MAX_REPLICAS} \
                                --compute-machine-type ${pipelineParameters.ROSA_WORKER_TYPE} \
                                --machine-cidr ${pipelineParameters.ROSA_MACHINE_CIDR} \
                                --service-cidr ${pipelineParameters.ROSA_SERVICE_CIDR} \
                                --pod-cidr ${pipelineParameters.ROSA_POD_CIDR} \
                                --host-prefix ${pipelineParameters.ROSA_HOST_PREFIX} \
                                --subnet-ids ${pipelineParameters.ROSA_SUBNET_IDS} \
                                --worker-disk-size ${pipelineParameters.ROSA_WORKER_DISK_SIZE} \
                                --autoscaler-ignore-daemonsets-utilization \
                                --autoscaler-max-nodes-total ${pipelineParameters.ROSA_AUTOSCALER_MAX_NODES_TOTAL} \
                                --autoscaler-scale-down-enabled \
                                --autoscaler-scale-down-utilization-threshold ${pipelineParameters.ROSA_AUTOSCALER_SCALE_DOWN_THRESHOLD} \
                                --additional-compute-security-group-ids ${pipelineParameters.ROSA_ADDITIONAL_WORKER_SECURITY_GROUPS} \
                                --additional-infra-security-group-ids ${pipelineParameters.ROSA_ADDITIONAL_INFRA_SECURITY_GROUPS} \
                                --additional-control-plane-security-group-ids ${pipelineParameters.ROSA_ADDITIONAL_CONTROLPLANE_SECURITY_GROUPS}"

                                echo \$ROSA_CMD
                                \$ROSA_CMD

                                if [ \$? -ne 0 ]; then
                                    echo "Failed to create the cluster ${pipelineParameters.CLUSTER_NAME}"
                                    exit 1
                                fi
                                
                                echo "Creating operator roles..."
                                rosa create operator-roles --cluster "${pipelineParameters.CLUSTER_NAME}" --mode auto --yes --region="${pipelineParameters.CLUSTER_REGION}"

                                echo "Creating OIDC provider..."
                                rosa create oidc-provider --cluster "${pipelineParameters.CLUSTER_NAME}" --mode auto --yes --region="${pipelineParameters.CLUSTER_REGION}"

                            fi
                        """
                    }
                }
            }
        }

        // Check the new ROSA cluster
        stage("Check Cluster Creation") {
            when {
                expression { pipelineParameters.CLUSTER_MANAGER_MODE.equalsIgnoreCase("Create") }
            }         
            steps {
                script {
                    sh """
                        # Number of tries to check the cluster readiness
                        clusterReadinessTries=${pipelineParameters.ROSA_CLUSTER_READINESS_TRIES}
                        # Time between tries in seconds
                        lookupInterval=30
                        countNotCreated=0
                        clusterReadinessCheckStatus=false

                        set +vx
                        echo "Waiting for the cluster ${pipelineParameters.CLUSTER_NAME} to be ready..."
                        while [[ \$clusterReadinessCheckStatus != true && \$clusterReadinessTries -gt \$countNotCreated ]]; do
                            echo "Consequent tries of cluster creation: \$countNotCreated \$clusterReadinessTries"
                            set -x

                            describeClusterResult=\$(rosa logs install --region="${pipelineParameters.CLUSTER_REGION}" --cluster="${pipelineParameters.CLUSTER_NAME}" 2>&1)
                            echo "\$describeClusterResult"

                            if [[ \$describeClusterResult == *"successfully installed"* ]]; then
                                echo "Cluster ${pipelineParameters.CLUSTER_NAME} is created."
                                clusterReadinessCheckStatus=true
                            else
                                echo "Waiting for > \$lookupInterval < before next iteration"
                                sleep \$lookupInterval
                                countNotCreated=\$((countNotCreated+1))
                            fi
                        done

                        if [[ \$clusterReadinessCheckStatus == true ]]; then
                            echo "Add the AmazonEC2ContainerRegistryReadOnly policy to the worker IAM role to access ECR"
                            aws iam attach-role-policy \
                            --role-name ManagedOpenShift-Worker-Role \
                            --policy-arn "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
                        else
                            echo "Cluster ${pipelineParameters.CLUSTER_NAME} is not ready after \$clusterReadinessTries readiness retries."
                            exit 1
                        fi
                    """
                }
            }
        }

        // Note: The following stage uses AWS CLI to update Route53 records. This is a workaround for the issue observed where records disappear  the
        // when using the Terraform equivalent and Terraform backend config needs to be reset between each dxTerraformCreateRoute53Entry.
        stage("Update Route53 records") {
            when {
                expression { pipelineParameters.CLUSTER_MANAGER_MODE.equalsIgnoreCase("Create") && pipelineParameters.UPDATE_ROUTE53_RECORD }
            }            
            steps {
                script {
                    sh """
                        set +vx

                        create_route53_alias_record() {
                            local recordHostedZoneId="\$1"
                            local recordName="\$2"
                            local apiRoute="\$3"
                            local hostedZoneId="\$4"
                            local updateRecord=false

                            # Check if the record exists and if so has the same value, encode * in recordName if needed before searching
                            recordNameEncoded=\$(echo "\$recordName" | sed 's/*/\\\\052/g')
                            existingRecordValue=\$(aws route53 list-resource-record-sets --hosted-zone-id "\${recordHostedZoneId}" \
                                --query "ResourceRecordSets[?Name == '\${recordNameEncoded}'].AliasTarget.DNSName" --output text)

                            if [[ -z "\$existingRecordValue" ]]; then
                                echo "Record \${recordName} does not exist. Creating..."
                                updateRecord=true
                            elif [[ "\$existingRecordValue" == "\$apiRoute" ]]; then
                                echo "Record \${recordName} already exists with the same value \${apiRoute}. Skipping..."
                                return
                            else
                                echo "Record \${recordName} exists with a different value \${existingRecordValue}. Updating..."
                                updateRecord=true
                            fi

                            if [[ \$updateRecord ]]; then
                                echo "Update the record \${recordName} to \${apiRoute} in the hosted zone \${recordHostedZoneId} ******"
                                
                                # Execute AWS Route 53 command
                                aws route53 change-resource-record-sets --hosted-zone-id "\${recordHostedZoneId}" --change-batch "{
                                    \\"Changes\\": [
                                        {
                                            \\"Action\\": \\"UPSERT\\",
                                            \\"ResourceRecordSet\\": {
                                                \\"Name\\": \\"\${recordName}\\",
                                                \\"Type\\": \\"A\\",
                                                \\"AliasTarget\\": {
                                                    \\"DNSName\\": \\"\${apiRoute}\\",
                                                    \\"EvaluateTargetHealth\\": false,
                                                    \\"HostedZoneId\\": \\"\${hostedZoneId}\\"
                                                }
                                            }
                                        }
                                    ]
                                }"
                            fi 
                        }

                        create_route53_cname_record() {
                            local recordHostedZoneId="\$1"
                            local recordName="\$2"
                            local apiRoute="\$3"
                            local updateRecord=false

                            # Check if the record exists and has the same value, encode * in recordName if needed before searching
                            recordNameEncoded=\$(echo "\$recordName" | sed 's/*/\\\\052/g')
                            existingRecordValue=\$(aws route53 list-resource-record-sets --hosted-zone-id "\${recordHostedZoneId}" \
                                --query "ResourceRecordSets[?Name == '\${recordNameEncoded}'].ResourceRecords[0].Value" --output text)

                            if [[ -z "\$existingRecordValue" ]]; then
                                echo "Record \${recordName} does not exist. Creating..."
                                updateRecord=true
                            elif [[ "\$existingRecordValue" == "\$apiRoute" ]]; then
                                echo "Record \${recordName} already exists with the same value \${apiRoute}. Skipping..."
                                return
                            else
                                echo "Record \${recordName} exists with a different value \${existingRecordValue}. Updating..."
                                updateRecord=true
                            fi

                            if [[ \$updateRecord ]]; then
                                echo "Update the record \${recordName} to \${apiRoute} in the hosted zone \${recordHostedZoneId} ******"

                                # Execute AWS Route 53 command
                                aws route53 change-resource-record-sets --hosted-zone-id "\${recordHostedZoneId}" --change-batch "{
                                    \\"Changes\\": [
                                        {
                                            \\"Action\\": \\"UPSERT\\",
                                            \\"ResourceRecordSet\\": {
                                                \\"Name\\": \\"\${recordName}\\",
                                                \\"Type\\": \\"CNAME\\",
                                                \\"TTL\\": 300,
                                                \\"ResourceRecords\\": [
                                                    {
                                                        \\"Value\\": \\"\${apiRoute}\\"
                                                    }
                                                ]
                                            }
                                        }
                                    ]
                                }"
                            fi
                        }

                        # Update private hosted zone alias records to the ELBs.

                        echo "Get the DNS of the ROSA cluster"
                        rosaHostname=\$(rosa describe cluster --cluster ${pipelineParameters.CLUSTER_NAME} --region="${pipelineParameters.CLUSTER_REGION}" | awk '/DNS:/ {print \$2}')
                        # check if rosaHostname contains openshiftapps.com, if not, something is wrong and we should not update Route53.
                        if [[ \$rosaHostname != *".openshiftapps.com" ]]; then
                            echo "Hosted zone ${pipelineParameters.CLUSTER_NAME} is not available.  Aborting..."
                            exit 1
                        fi
                        echo "DNS = \$rosaHostname"

                        echo "\nGet the private hosted zone id of the ROSA domain"
                        rosaHostedZoneId=\$(aws route53 list-hosted-zones  --query "HostedZones[?Name == '\$rosaHostname.'] | [0].Id" --output text)              
                        echo "ROSA private hosted zone ID = \$rosaHostedZoneId"

                        echo "\nGet the value of the api record in the ROSA private hosted zone"
                        rosaApiRoute=\$(aws route53 list-resource-record-sets --hosted-zone-id \$rosaHostedZoneId --query "ResourceRecordSets[?Name == 'api.\$rosaHostname.'].AliasTarget.DNSName" --output text)
                        echo "ROSA private API route = \$rosaApiRoute"

                        echo "\nGet the private hosted zone id of the DX domain ${pipelineParameters.DX_PRIVATE_HOSTED_ZONE}"
                        dxHostedZoneId=\$(aws route53 list-hosted-zones  --query "HostedZones[?Name == '${pipelineParameters.DX_PRIVATE_HOSTED_ZONE}.'] | [0].Id" --output text)
                        if [[ \$dxHostedZoneId == None ]]; then
                            echo "Hosted zone ${pipelineParameters.DX_PRIVATE_HOSTED_ZONE} is not available.  Aborting..."
                            exit 1
                        fi
                        echo "DX private hosted zone ID = \$dxHostedZoneId"

                        echo "\nUpdate api.${pipelineParameters.DX_PRIVATE_HOSTED_ZONE} in the DX private domain ${pipelineParameters.DX_PRIVATE_HOSTED_ZONE} to \$rosaApiRoute"
                        create_route53_alias_record "\$dxHostedZoneId" "api.${pipelineParameters.DX_PRIVATE_HOSTED_ZONE}." "\$rosaApiRoute" "${pipelineParameters.NETWORK_ELB_HOSTED_ZONE}"

                        echo "\nUpdate api-int.${pipelineParameters.DX_PRIVATE_HOSTED_ZONE} in the DX private domain ${pipelineParameters.DX_PRIVATE_HOSTED_ZONE} to \$rosaApiRoute"
                        create_route53_alias_record "\$dxHostedZoneId" "api-int.${pipelineParameters.DX_PRIVATE_HOSTED_ZONE}." "\$rosaApiRoute" "${pipelineParameters.NETWORK_ELB_HOSTED_ZONE}"

                        echo "\nGet the value of the apps record in the ROSA private hosted zone"
                        # Need to need to encode the * otherwise the record will not be found.
                        rosaAppsRoute=\$(aws route53 list-resource-record-sets --hosted-zone-id \$rosaHostedZoneId --query "ResourceRecordSets[?Name == '\\052.apps.\$rosaHostname.'].AliasTarget.DNSName" --output text)
                        echo "ROSA private APPS Route = \$rosaAppsRoute"

                        echo "\nUpdate *.apps.${pipelineParameters.DX_PRIVATE_HOSTED_ZONE} in the DX domain ${pipelineParameters.DX_PRIVATE_HOSTED_ZONE} to \$rosaAppsRoute"
                        # No need to encode here
                        create_route53_alias_record "\$dxHostedZoneId" "*.apps.${pipelineParameters.DX_PRIVATE_HOSTED_ZONE}." "\$rosaAppsRoute" "${pipelineParameters.NETWORK_ELB_HOSTED_ZONE}"

                        # Update public hosted zone alias records to the ELBs.

                        echo "\nGet the ROSA public hosted zone name from \$rosaHostname"
                        rosaPublicHostedZoneName=\$(echo \$rosaHostname | sed "s/${pipelineParameters.CLUSTER_NAME}.//") 
                        echo "ROSA public hosted zone name = \$rosaPublicHostedZoneName"

                        echo "\nGet the public hosted zone id of the ROSA domain"
                        rosaPublicHostedZoneId=\$(aws route53 list-hosted-zones  --query "HostedZones[?Name == '\$rosaPublicHostedZoneName.'] | [0].Id" --output text)
                        echo "ROSA public hosted zone ID = \$rosaPublicHostedZoneId"

                        echo "\nGet the value of the api record in the ROSA public hosted zone"
                        rosaPublicApiRoute=\$(aws route53 list-resource-record-sets --hosted-zone-id \$rosaPublicHostedZoneId --query "ResourceRecordSets[?Name == 'api.\$rosaHostname.'].AliasTarget.DNSName" --output text)
                        echo "ROSA public API route = \$rosaPublicApiRoute"                    

                        echo "\nGet the public hosted zone id of the DX domain ${pipelineParameters.DX_PUBLIC_HOSTED_ZONE}"
                        dxPublicHostedZoneId=\$(aws route53 list-hosted-zones  --query "HostedZones[?Name == '${pipelineParameters.DX_PUBLIC_HOSTED_ZONE}.'] | [0].Id" --output text)
                        if [[ \$dxPublicHostedZoneId == None ]]; then
                            echo "Cluster ${pipelineParameters.DX_PUBLIC_HOSTED_ZONE} is not available.  Aborting..."
                            exit 1
                        fi                        
                        echo "DX public hosted zone ID = \$dxPublicHostedZoneId"

                        # Note: the record name is the same as the private hosted zone (eg. api.dx-cluster-dev.hcl-dx-dev.net)
                        echo "\nUpdate api.${pipelineParameters.DX_PRIVATE_HOSTED_ZONE} in the DX public domain ${pipelineParameters.DX_PUBLIC_HOSTED_ZONE} to \$rosaPublicApiRoute"
                        create_route53_alias_record "\$dxPublicHostedZoneId" "api.${pipelineParameters.DX_PRIVATE_HOSTED_ZONE}." "\$rosaPublicApiRoute" "${pipelineParameters.NETWORK_ELB_HOSTED_ZONE}"

                        # Update public hosted zone CNAME records to ROSA router.
                        echo "\nUpdate *.apps.${pipelineParameters.DX_PRIVATE_HOSTED_ZONE} in the DX public domain ${pipelineParameters.DX_PUBLIC_HOSTED_ZONE} to the ROSA cluster router-default.apps.\$rosaHostname"
                        # No need to encode here
                        create_route53_cname_record "\$dxPublicHostedZoneId" "*.apps.${pipelineParameters.DX_PRIVATE_HOSTED_ZONE}." "router-default.apps.\$rosaHostname"

                    """
                }
            }
        }

        // Delete ROSA Cluster via ROSA CLI
        stage("Destroy ROSA Cluster") {
            when {
                expression { pipelineParameters.CLUSTER_MANAGER_MODE.equalsIgnoreCase("Destroy") }
            }
            steps {
                script {
                    withCredentials([[
                        $class: 'UsernamePasswordMultiBinding',
                        credentialsId: "openshift_rosa_credentials",
                        usernameVariable: 'USERNAME', 
                        passwordVariable: 'PASSWORD'
                    ]]) {
                        sh """
                            export PATH="$PATH:${workspace}/rosa-client"

                            deleteROSARoles() {
                                local policy="\$1"

                                # Reduce role name to 64 characters
                                local role=\$(echo "\$2" | cut -c 1-64)

                                # Detach and delete the role
                                echo "\nDetach role \${role} from policy \${policy}"
                                aws iam detach-role-policy \
                                    --policy-arn arn:aws:iam::"${pipelineParameters.ROSA_AWS_ACCOUNT}":policy/"\${policy}" \
                                    --role-name "\${role}"

                                echo "\nDelete role \${role}"
                                aws iam delete-role \
                                    --role-name "\${role}"
                            }

                            echo "Describing the cluster ${pipelineParameters.CLUSTER_NAME}..."
                            set +evx
                            cluster_desc=\$(rosa describe cluster --region="${pipelineParameters.CLUSTER_REGION}" --cluster="${pipelineParameters.CLUSTER_NAME}" 2>&1)
                            exit_status=\$?
                            echo "\$cluster_desc"
                            set -ex

                            echo "Checking if cluster ${pipelineParameters.CLUSTER_NAME} exists..."
                            if [ \$exit_status -ne 0 ]; then
                                echo "Cluster ${pipelineParameters.CLUSTER_NAME} does not exist. Skipping deletion."
                             else
                                echo "Get the cluster ID before deleting the cluster"
                                cluster_id=\$(echo "\$cluster_desc" | awk '/^ID:/ {print \$2}')
                                echo "Cluster ID = \$cluster_id"

                                # If cluster_id is empty, something is wrong and we should not delete the cluster because the subsequent commands will fail.
                                if [ -z "\$cluster_id" ]; then
                                    echo "Cannot find cluster ID from the rosa describe cluster command.  Aborting..."
                                    exit 1
                                fi

                                echo "Deleting ROSA cluster ${pipelineParameters.CLUSTER_NAME}"
                                rosa delete cluster --region="${pipelineParameters.CLUSTER_REGION}" --cluster="${pipelineParameters.CLUSTER_NAME}" --yes

                                echo "Deleting operator roles for ID \$cluster_id ..."
                                # Note: could've used the "rosa delete operator-roles" command, but the account-wide IAM roles might be used by other ROSA clusters 
                                # in the same AWS account so delete the cluster specific roles only.
                                # rosa delete operator-roles -c <cluster_id> --mode auto
                                
                                set +v
                                policy="ManagedOpenShift-openshift-ingress-operator-cloud-credentials"
                                role="${pipelineParameters.CLUSTER_NAME}-openshift-ingress-operator-cloud-credentials"
                                deleteROSARoles "\$policy" "\$role"

                                policy="ManagedOpenShift-openshift-cluster-csi-drivers-ebs-cloud-credent"
                                role="${pipelineParameters.CLUSTER_NAME}-openshift-cluster-csi-drivers-ebs-cloud-credentials"
                                deleteROSARoles "\$policy" "\$role"

                                policy="ManagedOpenShift-openshift-cloud-network-config-controller-cloud"
                                role="${pipelineParameters.CLUSTER_NAME}-openshift-cloud-network-config-controller-cloud-credentials"
                                deleteROSARoles "\$policy" "\$role"

                                policy="ManagedOpenShift-openshift-machine-api-aws-cloud-credentials"
                                role="${pipelineParameters.CLUSTER_NAME}-openshift-machine-api-aws-cloud-credentials"
                                deleteROSARoles "\$policy" "\$role"

                                policy="ManagedOpenShift-openshift-cloud-credential-operator-cloud-crede"
                                role="${pipelineParameters.CLUSTER_NAME}-openshift-cloud-credential-operator-cloud-credential-operator"
                                deleteROSARoles "\$policy" "\$role"

                                policy="ManagedOpenShift-openshift-image-registry-installer-cloud-creden"
                                role="${pipelineParameters.CLUSTER_NAME}-openshift-image-registry-installer-cloud-credentials"
                                deleteROSARoles "\$policy" "\$role"


                                echo "Deleting OIDC provider for ID \$cluster_id ..."
                                # Note: could've used the "rosa delete oidc-provider" command, but want to be explicit in case it deletes more than it should.
                                # rosa delete oidc-provider -c <cluster_id> --mode auto
                                aws iam delete-open-id-connect-provider \
                                    --open-id-connect-provider-arn arn:aws:iam::"${pipelineParameters.ROSA_AWS_ACCOUNT}":oidc-provider/oidc.op1.openshiftapps.com/\$cluster_id
                                
                                set -v
                            fi
                        """
                    }
                }
            }
        } 
    }

    post {
        cleanup {
            dxWorkspaceDirectoriesCleanup()
        }
    }
}
