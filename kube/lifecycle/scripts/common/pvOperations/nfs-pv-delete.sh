#!/bin/bash
VERBOSE_MODE=$4
echo "remove folder $1 from $2 entry from /etc/exports"
cd $2 && sudo rm -rf $1
pv=$(printf '%s\n' "$1" | sed -e 's/[\/&]/\\&/g')
folder=$(printf '%s\n' "$2" | sed -e 's/[\/&]/\\&/g')
if [[ $3 == "google" ]]; then
    sudo sed -i ":g /$folder\/$pv \*(rw,sync,no_subtree_check)/d" /etc/exports
elif [[ $3 == "azure" ]]; then
    sudo sed -i ":g /$folder\/$pv \*(rw,sync,no_subtree_check)/d" /etc/exports
elif [[ $3 == "openshift" ]]; then
    sudo sed -i ":g /$folder\/$pv \*(rw,sync,insecure,root_squash,no_subtree_check)/d" /etc/exports
elif [[ $3 == "aws" ]]; then
    sudo sed -i ":g /$folder\/$pv \*(rw,sync,insecure,root_squash)/d" /etc/exports
else
    sudo sed -i ":g /$folder\/$pv \*(rw,sync,insecure,root_squash)/d" /etc/exports
fi
sudo exportfs -r
if [ "$VERBOSE_MODE" != "" ]; then
   echo "exportfs after remove folder $1, after remove entry /etc/exports,after refresh exportfs"
   sudo exportfs
fi
