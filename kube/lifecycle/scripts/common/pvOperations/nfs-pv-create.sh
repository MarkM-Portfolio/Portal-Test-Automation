#!/bin/bash
VERBOSE_MODE=$4
echo "create folder $1 inside $2 add entry to /etc/exports"
if [[ $3 == "google" ]]; then
    mkdir -p $2
    cd $2 && mkdir -p $1
    sudo chown -R nobody:nogroup $1
    sudo chmod -R 777 $1
    echo  "$2/$1 *(rw,sync,no_subtree_check)" | sudo  tee -a /etc/exports
elif [[ $3 == "azure" ]]; then
    mkdir -p $2
    cd $2 && mkdir -p $1
    sudo chown -R nobody:nogroup $1
    sudo chmod -R 777 $1
    echo  "$2/$1 *(rw,sync,no_subtree_check)" | sudo  tee -a /etc/exports
elif [[ $3 == "openshift" ]]; then
    mkdir -p $2
    cd $2 && mkdir -p $1
    sudo chmod -R 777 $1
    echo  "$2/$1 *(rw,sync,insecure,root_squash,no_subtree_check)" | sudo  tee -a /etc/exports
elif [[ $3 == "aws" ]]; then
    sudo mkdir -p $2/$1
    sudo chown -R nfsnobody:nfsnobody $2/$1
    sudo chmod -R 777 $2/$1
    echo  "$2/$1 *(rw,sync,insecure,root_squash)" | sudo  tee -a /etc/exports
else
    echo  "$2/$1 *(rw,sync,insecure,root_squash)" | sudo  tee -a /etc/exports
fi
sudo exportfs -r
if [ "$VERBOSE_MODE" != "" ]; then
   echo "exportfs after adding folder $1 inside $2 after adding entry /etc/exports ,after refresh exportfs"
   sudo exportfs
fi
