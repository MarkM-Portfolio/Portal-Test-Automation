#/*
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2022. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
# */

pipeline {
   agent {
       label 'build_infra'
   }

   stages {
      stage('Add admin') {
         steps {
            script {
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId:"openshift_credentials", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                    sh """
                        mkdir -p oc-client
                        curl -L https://github.com/openshift/origin/releases/download/v3.11.0/openshift-origin-client-tools-v3.11.0-0cbc58b-linux-64bit.tar.gz > ./oc-client/openshift-oc.tar.gz
                        cd oc-client && tar -xvf openshift-oc.tar.gz --strip=1 > /dev/null 2>&1
                        curl -LO https://storage.googleapis.com/kubernetes-release/release/v1.19.0/bin/linux/amd64/kubectl > /dev/null 2>&1
                        export PATH="$PATH:${workspace}/oc-client"
                        cd ${workspace}
                        chmod -R 755 ./
                        oc login -u=$USERNAME -p=$PASSWORD --server=https://api.hcl-dxdev.hcl-dx-dev.net:6443 --insecure-skip-tls-verify
                        oc status
                    """
                }
                withEnv(["PATH=$PATH:${workspace}/oc-client"]){
                    sh """
                        oc policy add-role-to-user admin ${OS_USER} -n ${NAMESPACE}
                    """
                }
            }
         }
      }
   }
}
