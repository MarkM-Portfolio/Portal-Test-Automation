# db2 kube

This project allows to run DB2 inside Kubernetes.

## Image transfer

To get the DB2 up and running, you can use the Jenkinsfile and run it with default parameters.
It will transfer the v11.5 image to all remote cloud repositories.

See the `parameters.yaml` for all possible configuration parameters.

## Deploy DB2 to Kube

Replace the `DB2IMAGE` placeholder inside the yaml file with your container image reference for the target repository/cluster.

If running on Openshift, execute `kubectl apply -f restricted-db2-scc.yaml -n YOUR_NAMESPACE`, 