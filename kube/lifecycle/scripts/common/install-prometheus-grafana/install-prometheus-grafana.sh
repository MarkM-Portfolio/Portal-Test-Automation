#!/bin/bash
#********************************************************************
#* Licensed Materials - Property of HCL                             *
#*                                                                  *
#* Copyright HCL Technologies Ltd. 2023. All Rights Reserved.       *
#*                                                                  *
#* Note to US Government Users Restricted Rights:                   *
#*                                                                  *
#* Use, duplication or disclosure restricted by GSA ADP Schedule    *
#********************************************************************

# Install Prometheus and Grafana in Kube instance

# Set current dir - having prom and grafana yaml files and script
cd /home/centos/native-kube/install-prometheus-grafana

# Prometheus
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm install prometheus prometheus-community/prometheus -n prom -f prom-values.yaml --create-namespace

PROMETHEUS_CLUSTER_IP=$(kubectl get -o jsonpath="{.spec.clusterIP}" services prometheus-server -n prom)
PROMETHEUS_CLUSTER_PORT=$(kubectl get -o jsonpath="{.spec.ports[0].port}" services prometheus-server -n prom)

echo "PROMETHEUS_CLUSTER_IP :- $PROMETHEUS_CLUSTER_IP"
echo "PROMETHEUS_CLUSTER_PORT :- $PROMETHEUS_CLUSTER_PORT"

PROMETHEUS_NODEPORT=$(kubectl get -o jsonpath="{.spec.ports[0].nodePort}" services prometheus-server -n prom)
echo "Prometheus Port:- $PROMETHEUS_NODEPORT"

# Kube Instance IP
NODE_IP=$(kubectl get nodes --namespace prom -o jsonpath="{.items[0].status.addresses[0].address}")
echo "IP :- $NODE_IP"

# Grafana
# update prometheus url in grafana-values.yaml
sed -i.bck "s'PROMETHEUS_URL'http://$PROMETHEUS_CLUSTER_IP:$PROMETHEUS_CLUSTER_PORT'g" grafana-values.yaml
helm repo add grafana https://grafana.github.io/helm-charts
helm install grafana -n prom grafana/grafana -f grafana-values.yaml --create-namespace

GRAFANA_NODEPORT=$(kubectl get -o jsonpath="{.spec.ports[0].nodePort}" services grafana -n prom)
echo "Grafana Port:- $GRAFANA_NODEPORT"

echo "Grafana User:- admin"
echo "Grafana password:-"
kubectl get secret --namespace prom grafana -o jsonpath="{.data.admin-password}" | base64 --decode ; echo

PROMETHEUS_URL="http://$NODE_IP:$PROMETHEUS_NODEPORT"
GRAFANA_URL="http://$NODE_IP:$GRAFANA_NODEPORT"

echo "Prometheus URL -: $PROMETHEUS_URL "
echo "Grafana URL -: $GRAFANA_URL"
echo "Prometheus and Grafana installed successfully."