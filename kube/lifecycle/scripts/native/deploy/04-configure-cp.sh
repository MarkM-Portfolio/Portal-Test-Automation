#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2024. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

# Set the "internal" host name in the hosts file
sudo -- sh -c -e "echo '$1  $HOSTNAME'>>/etc/hosts";

# Init cluster
sudo kubeadm init --pod-network-cidr=192.168.0.0/16

# Copy the config file
mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config

# Configure reverse DNS
IFS=. read groupA groupB groupC groupD <<< $1
kubectl get configmap coredns -n kube-system -o yaml | sed "s'        forward '        template IN PTR in-addr.arpa {\n           match ^(?P<d>$groupD)[.](?P<c>$groupC)[.](?P<b>$groupB)[.](?P<a>$groupA)[.]in-addr[.]arpa[.]$\n           answer \"{{ .Name }} 60 IN PTR $2.\"\n        }\n        forward 'g" | sed "s/cache 30/cache 2/g" | kubectl replace -f -
kubectl scale --replicas=0 deployment -n kube-system coredns
kubectl scale --replicas=1 deployment -n kube-system coredns

# Install Helm
HELMFILE="helm-$2-linux-amd64.tar.gz"
curl "$3/${HELMFILE}" -o helmsetup.tgz
tar zvxf helmsetup.tgz -C .
rm -f helmsetup.tgz
sudo cp -f ./linux-amd64/helm /usr/local/bin/helm
rm -rf ./linux-amd64