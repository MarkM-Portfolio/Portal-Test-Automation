#!/bin/bash

#
 ####################################################################
 # Licensed Materials - Property of HCL                             #
 #                                                                  #
 # Copyright HCL Technologies Ltd. 2001, 2021. All Rights Reserved. #
 #                                                                  #
 # Note to US Government Users Restricted Rights:                   #
 #                                                                  #
 # Use, duplication or disclosure restricted by GSA ADP Schedule    #
 ####################################################################
 #

 # Ensure that the WAS JRE security settings for TLS are correctly set
 # to allow WTF's ServerAgent to function via SOAP.

security_file="/opt/IBM/WebSphere/AppServer/java/8.0/jre/lib/security/java.security"
temp_security_file="/opt/IBM/WebSphere/AppServer/java/8.0/jre/lib/security/temp.security"
key_setting="jdk.tls.disabledAlgorithms="

cd /
# verify that the JRE security file is there
if [ -f "$security_file" ]
then
    # make a backup copy of JRE security file
    cp "$security_file" "$security_file"-backup

    # test to see if a setting for the disabledAlgorithms already exists
    if grep -Fq "$key_setting" $security_file
    then
        echo "A disabledAlgorithms entry exists in the JRE security file, updating for WTF"
        echo "reading each line from JRE security file ---------"
        while IFS= read -r line
        do
            case $line in 
                *"$key_setting"*)
                    # comment out the existing disabledAlgorithms settings
                    echo "#$line"
                ;;
                *)
                    # keep all other lines as is
                    echo "$line"
                ;;
            esac        
        done < "$security_file"  > "$temp_security_file"

        # insert the setting that we want that does not disable TLSv1, TLSv1.1
        echo "jdk.tls.disabledAlgorithms=SSLv3, RC4, DES, MD5withRSA, DH keySize < 1024, DESede, \ EC keySize < 224, 3DES_EDE_CBC, anon, NULL, DES_CBC" >> "$temp_security_file"
        # replace the security file with our version
        mv "$temp_security_file" "$security_file"
        echo "JRE security file updated"
    else
        echo "No disabledAlgorithms entry was found in the JRE security file, no update needed for WTF"
    fi
fi
