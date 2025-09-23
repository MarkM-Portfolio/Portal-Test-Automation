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
   echo "Syntax: $SCRIPT_NAME -d DOMAIN_NAME [--cert-home CERT_HOME] [--days N] [--staging] [--debug] [--verbose]"
   echo "                            [--access-key AWS_ACCESS_KEY_ID] [--secret AWS_SECRET_ACCESS_KEY]"
   echo "        DOMAIN_NAME - Name of system (instance name) to get certificate for"
   echo "        CERT_HOME - Set directory where to put certificate (default is $HOME/.acme.sh)"
   echo "        days N - Specifies the days to renew the cert (default is 60 days)"
   echo "        AWS_ACCESS_KEY_ID - AWS access key ID (may also be set as environment variable)"
   echo "        AWS_SECRET_ACCESS_KEY - AWS secret access key (may also be set as environment variable)"
   echo "        staging - Specifies to run script against staging system (for testing purposes)"
   echo "        debug - Switch on debug mode of acme.sh (for testing purposes)"
   exit 1
}

# Set defaults
MAIN_PARAMS="--issue --dns dns_aws"
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
       --days)
         MAIN_PARAMS="$MAIN_PARAMS --days $2"
         shift # get next value
         ;;
      --staging)
         SERVER="letsencrypt_test"
         ;;
      --verbose)
         VERBOSE="yes"
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

# If DOMAIN is missing the * as wildcard add it 
if [[ "$DOMAIN" != "*."* ]]; then
   DOMAIN="*.$DOMAIN"
fi

# Compute CERTDIR, if CERTHOME not starts with / it is used as a relativ path to $HOME/.scme.sh
CERTNAME=${DOMAIN#\*\.}
if [ "$CERTHOME" == "" ]; then
   CERTDIR=$HOME/.acme.sh/$CERTNAME
else
   if [[ "$CERTHOME" == "/"* ]]; then
      CERTDIR="$CERTHOME/$CERTNAME"
   else
      CERTDIR="$HOME/.acme.sh/$CERTHOME/$CERTNAME"
   fi
fi

if [ "$VERBOSE" == "yes" ]; then
   echo "CERTDIR = $CERTDIR"
fi

# Backup old CERTDIR
if [ -e $CERTDIR ]; then
   if [ "$VERBOSE" == "yes" ]; then
      echo "Backup old CERTDIR to ${CERTDIR}.bkp"
   fi
   mv $CERTDIR ${CERTDIR}.bkp
fi

# Get new certificate
if [ "$VERBOSE" == "yes" ]; then
   echo "$HOME/.acme.sh/acme.sh $MAIN_PARAMS --server $SERVER -d \"$DOMAIN\" $DEBUG"
fi
a=$($HOME/.acme.sh/acme.sh $MAIN_PARAMS --server $SERVER -d "$DOMAIN" $DEBUG 2>&1)

if [ "$VERBOSE" == "yes" ]; then
   echo "$a"
fi

# In case of an invalid domain restore old CERTDIR and quit
if [[ "$a" != *"Cert success."* ]]; then
   # do not echo twice
   if [ "$VERBOSE" != "yes" ]; then
      echo "$a"
   fi
   if [ -e $HOME/.acme.sh/$DOMAIN ]; then
      rm -fR $HOME/.acme.sh/$DOMAIN
   fi
   if [ -e ${CERTDIR}.bkp ]; then
      mv ${CERTDIR}.bkp $CERTDIR
   fi
   exit 1
fi

CERT_DOWNLOAD=${a#*Your cert key is in: }
CERT_DOWNLOAD=${CERT_DOWNLOAD%%/\*\.*}

if [ "$VERBOSE" == "yes" ]; then
   echo "-------------"
   echo "CERT_DOWNLOAD = $CERT_DOWNLOAD"
fi

# Remove *. from wildcard certificate names
mv $CERT_DOWNLOAD/$DOMAIN $CERT_DOWNLOAD/$CERTNAME
mv $CERT_DOWNLOAD/$CERTNAME/$DOMAIN.cer $CERT_DOWNLOAD/$CERTNAME/$CERTNAME.cer
mv $CERT_DOWNLOAD/$CERTNAME/$DOMAIN.conf $CERT_DOWNLOAD/$CERTNAME/$CERTNAME.conf
mv $CERT_DOWNLOAD/$CERTNAME/$DOMAIN.csr $CERT_DOWNLOAD/$CERTNAME/$CERTNAME.csr
mv $CERT_DOWNLOAD/$CERTNAME/$DOMAIN.csr.conf $CERT_DOWNLOAD/$CERTNAME/$CERTNAME.csr.conf
mv $CERT_DOWNLOAD/$CERTNAME/$DOMAIN.key $CERT_DOWNLOAD/$CERTNAME/$CERTNAME.key

# If CERTHOME was set move cert directory there
if [ "$CERTHOME" != "" ]; then
   if [ "$CERT_DOWNLOAD/$CERTNAME" != "$CERTDIR" ]; then
      mkdir -p $CERTHOME
      mv $CERT_DOWNLOAD/$CERTNAME $CERTDIR
   fi
fi

# Delete existing backup
if [ -e ${CERTDIR}.bkp ]; then
   rm -fR ${CERTDIR}.bkp
fi

echo "Certificate downloaded:"
echo "  The cert is in $CERTDIR/$CERTNAME.cer"
echo "  The cert key is in $CERTDIR/$CERTNAME.key"
echo "  The CA cert is in $CERTDIR/ca.cer"
echo "  The full chain certs is in $CERTDIR/fullchain.cer"
