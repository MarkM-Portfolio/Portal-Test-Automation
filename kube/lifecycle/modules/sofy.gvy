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

/* Loading other modules for usage */
commonConfig = load "${configDirectory}/common.gvy"
gkeConfig = load "${configDirectory}/google.gvy"
commonModule = load "${moduleDirectory}/common.gvy"

/*
 * create the gke environment
 */

env.TARGET_IMAGE_REPOSITORY_CREDENTIALS = "sofy_gcr_login"
env.SOFY_HARBOR_CREDENTIALS = "sofy-harbor-universal"

def doesNamespaceExist(NAMESPACE) {
    namespaceExist = sh(script: """
        if [ \$(kubectl get namespace -A 2>&1 | grep -c '${NAMESPACE}') -eq 0 ]; then
            echo false
        else 
            echo true
        fi
    """, returnStdout: true)
    return namespaceExist;
}

def createEnvironmentSofy(SOLUTION_ID, NAMESPACE, TOKEN, IMAGE_REPO, RELEASE_TYPE) {
    /*Download helm charts from SoFy solution and deploy it on GKE environment.*/
    loginGKE();
    withEnv(["PATH=$PATH:${workspace}/gke-client","KUBECONFIG=${workspace}/gke-${NAMESPACE}-kubeconfig.yaml"]){
        withCredentials([
            usernamePassword(credentialsId: "${env.SOFY_HARBOR_CREDENTIALS}", passwordVariable: 'SOFY_PASSWORD', usernameVariable: 'SOFY_USERNAME'),
            file(credentialsId: "${env.TARGET_IMAGE_REPOSITORY_CREDENTIALS}", variable: 'LOGIN_KEY')
            ]) {
                script {
                    SOLUTION_URL="https://sol.products.pnpsofy.com/solutions/${SOLUTION_ID}/versions/1.0.0/chart"
                    sh(script: """
                        mkdir -p helm && cd helm
                        curl "${COMMON_HELM_ARTIFACTORY_URL}/helm-${COMMON_HELM_VERSION}-linux-amd64.tar.gz" -o helmsetup.tgz && tar zvxf helmsetup.tgz -C . && rm -f helmsetup.tgz && cd ..
                    """)
                    certManager = doesNamespaceExist('cert-manager')
                    rwxPersitenceVolume = doesNamespaceExist("rwx-pv")
                    echo "cert manager already exist: ${certManager}"
                    echo "rwx pv already exist:  ${rwxPersitenceVolume}"
                    if(!certManager.toBoolean()) {
                        /* Installing cert-manager */
                        sh(script: """
                            export PATH="$PATH":${workspace}/helm/linux-amd64/
                            curl -o cert-manager-setup.sh https://hclcr.io/files/sofy/scripts/cert-manager-setup.sh
                            ls -lah
                            sh cert-manager-setup.sh
                            sleep 60
                        """)
                    }
                    if(!rwxPersitenceVolume.toBoolean()) {
                        /* Installing rwx volume */
                        sh(script: """
                            export PATH="$PATH":${workspace}/helm/linux-amd64/
                            helm repo add kvaps https://kvaps.github.io/charts
                            helm install my-nfs-server-provisioner kvaps/nfs-server-provisioner --version 1.4.0
                            kubectl create namespace rwx-pv
                            helm install -n rwx-pv rwx kvaps/nfs-server-provisioner --version 1.3.1 -f ${workspace}/kube/lifecycle/scripts/sofy/pv.yaml
                        """)
                    }
                    sh(script: """
                        mkdir -p helmChart && cd helmChart
                        curl -o chart.tgz --location --request GET ${SOLUTION_URL} \
                             --header 'Accept: application/json, text/plain, */*' \
                             --header 'Authorization: Bearer ${TOKEN}'
                        cd ..
                        kubectl create namespace ${NAMESPACE}
                    """)
                    if(IMAGE_REPO == 'harbor') {
                        if(RELEASE_TYPE == 'preview') {
                            IMAGE_REGISTRY = 'hclcr.io/sofy-hcl-users'
                        } else {
                            IMAGE_REGISTRY = 'hclcr.io/sofy'
                        }
                        sh(script: """
                            kubectl create secret docker-registry regcred --docker-server=hclcr.io --docker-username=${SOFY_USERNAME} --docker-password=${SOFY_PASSWORD} -n ${NAMESPACE}
                        """)
                    } else {
                        IMAGE_REGISTRY = 'gcr.io/blackjack-209019'
                        sh(script: """
                            kubectl create secret docker-registry regcred --docker-server=gcr.io --docker-username=_json_key --docker-password="\$(cat $LOGIN_KEY)" -n ${NAMESPACE}
                        """)
                    }
                    sh(script: """
                        export PATH="$PATH":${workspace}/helm/linux-amd64/
                        helm version
                        helm install ${NAMESPACE} helmChart/chart.tgz -n ${NAMESPACE} --set global.persistence.rwxStorageClassV3=nfs-client-ac --set global.hclImageRegistry=${IMAGE_REGISTRY} --set global.hclImagePullSecret=regcred --set global.hclPreviewImageRegistry=${IMAGE_REGISTRY}  --dependency-update  
                        ${workspace}/kube/lifecycle/scripts/common/start-dx-core-ready.sh ${NAMESPACE}-core-0 ${NAMESPACE} ${commonConfig.DX_FRESH_PROBE_RETRIES} ${commonConfig.DX_START_VERBOSE_MODE} 
                    """)
                    EXTERNAL_IP= sh (script: """(kubectl get services -n ${NAMESPACE} | grep  LoadBalancer | awk '{print \$4}' )""" ,   returnStdout: true).trim();
                    echo "EXTERNAL_IP: ${EXTERNAL_IP}"
                }
                if(!("${commonConfig.COMMON_SKIP_ACCEPTANCE_TESTS}".toBoolean())){
                    commonModule.isAcceptanceTestsSuccess("develop", "dx.${EXTERNAL_IP}.nip.io", true)
                }
            
        }
    }
}

def destroySofyEnvironment(NAMESPACE) {
    loginGKE();
    withEnv(["PATH=$PATH:${workspace}/gke-client","KUBECONFIG=${workspace}/gke-${NAMESPACE}-kubeconfig.yaml"]){
        withCredentials([file(credentialsId: "${env.TARGET_IMAGE_REPOSITORY_CREDENTIALS}", variable: 'LOGIN_KEY')]) {
            script {
                sh(script: """
                    export PATH="$PATH":${workspace}/helm/linux-amd64/
                    helm delete ${NAMESPACE} -n ${NAMESPACE}
                    kubectl delete svc,hpa,po,sts,pvc,pods,jobs -n ${NAMESPACE} --all --force --grace-period=0
                    kubectl delete namespaces ${NAMESPACE}
                """)
            }
        }
    }
}


/*
 * login to gke environment
 */
def loginGKE() {
    echo "Configure gke env,Check connection to GKE"
    /***
    once the activate-service-account is created enable below section.
    withCredentials([file(credentialsId: 'gke_credentials', variable: 'GOOGLE_SERVICE_ACCOUNT_KEY')]) {
        gcloud auth activate-service-account --key-file=${GOOGLE_SERVICE_ACCOUNT_KEY}
    }
    **/
    sh """
        mkdir -p gke-client && cd gke-client
        curl -LO https://storage.googleapis.com/kubernetes-release/release/v${gkeConfig.COMMON_KUBE_VERSION}/bin/linux/amd64/kubectl > /dev/null 2>&1
        export PATH="$PATH:${workspace}/gke-client"
        cd ${workspace}
        chmod -R 755 ./
        KUBECONFIG="gke-${NAMESPACE}-kubeconfig.yaml" gcloud container clusters get-credentials ${gkeConfig.CLUSTER_NAME} --region ${gkeConfig.CLUSTER_REGION} --project hcl-gcp-l2com-sofy
        export KUBECONFIG="gke-${NAMESPACE}-kubeconfig.yaml"
        kubectl config current-context
    """
}

/* Mandatory return statement on EOF */
return this
