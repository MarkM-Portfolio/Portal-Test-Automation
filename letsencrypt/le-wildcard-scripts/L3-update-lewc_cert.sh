#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2021. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

function show_help {
   echo "Syntax: $SCRIPT_NAME -d DOMAIN_NAME [--cert-home CERT_HOME] [--staging] [--debug]"
   echo "                            [--access-key AWS_ACCESS_KEY_ID] [--secret AWS_SECRET_ACCESS_KEY]"
   echo "        DOMAIN_NAME - Name of system (instance name) to get certificate for"
   echo "        CERT_HOME - Set directory where to put certificate (default is $HOME/.acme.sh)"
   echo "        AWS_ACCESS_KEY_ID - AWS access key ID (may also be set as environment variable)"
   echo "        AWS_SECRET_ACCESS_KEY - AWS secret access key (may also be set as environment variable)"
   echo "        staging - Specifies to run script against staging system (for testing purposes)"
   echo "        debug - Switch on debug mode of acme.sh (for testing purposes)"
   exit 1
}

# Set defaults
MAIN_PARAMS="--renew --dns dns_aws --force"
SERVER="letsencrypt"

# Get command line parmeters
SCRIPT_NAME=$(basename "${BASH_SOURCE[0]}")
while [[ $# -gt 0 ]]; do
   case "$1" in
      -d|--domain)
         DOMAIN=$2
         shift # get next value
         ;;
      --cert-home)
         CERTHOME=$2
         shift # get next value
         ;;
      --access-key)
         AWS_ACCESS_KEY_ID=$2
         shift # get next value
         ;;
      --secret)
         AWS_SECRET_ACCESS_KEY=$2
         shift # get next value
         ;;
      --staging)
         SERVER="letsencrypt_test"
         ;;
      --debug)
         DEBUG="--debug"
         if [[ "$2" != "-"* ]]; then
             DEBUG="--debug $2"
             shift # get next value
         fi
         ;;
      -h|--help)
         show_help
         ;;
      *)
         # unknown option
         ;;
   esac
   shift
done

# Check if mandatory parameters DOMAIN, AWS_ACCESS_KEY_ID, and AWS_SECRET_ACCESS_KEY are available
if [ "$DOMAIN" == "" ]; then
   echo "ERROR: Domain missing!"
   show_help
fi
if [ "$AWS_ACCESS_KEY_ID" == "" ]; then
   echo "ERROR: AWS_ACCESS_KEY_ID missing!"
   show_help
fi
if [ "$AWS_SECRET_ACCESS_KEY" == "" ]; then
   echo "ERROR: AWS_SECRET_ACCESS_KEY missing!"
   show_help
fi


export AWS_ACCESS_KEY_ID
export AWS_SECRET_ACCESS_KEY

# Compute CERTDIR and CERT_ROOT, if CERTHOME not starts with / it is used as a relativ path to $HOME/.acme.sh
CERTNAME=$DOMAIN
if [ "$CERTHOME" == "" ]; then
   CERT_ROOT=$HOME/.acme.sh
else
   if [[ "$CERTHOME" == "/"* ]]; then
      CERT_ROOT=$CERTHOME
   else
      CERT_ROOT=$HOME/.acme.sh/$CERTHOME
   fi
fi
CERTDIR="$CERT_ROOT/$CERTNAME"

# Renew certificate
if [ "$DEBUG" != "" ]; then
   echo "$HOME/.acme.sh/acme.sh $MAIN_PARAMS --server $SERVER -d "$DOMAIN" --home $CERT_ROOT $DEBUG"
fi
a=$($HOME/.acme.sh/acme.sh $MAIN_PARAMS --server $SERVER -d "$DOMAIN" --home $CERT_ROOT $DEBUG 2>&1)

# In case of failure don't update and quit
if [[ "$a" != *"Cert success."* ]]; then
   echo "$a"
   exit 1
fi

echo "Certificate downloaded:"
echo "  The cert is in $CERTDIR/$CERTNAME.cer"
echo "  The cert key is in $CERTDIR/$CERTNAME.key"
echo "  The CA cert is in $CERTDIR/ca.cer"
echo "  The full chain certs is in $CERTDIR/fullchain.cer"
