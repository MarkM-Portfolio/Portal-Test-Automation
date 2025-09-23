#!/bin/bash
#
#********************************************************************
# Licensed Materials - Property of HCL                              *
#                                                                   *
#  Copyright HCL Technologies Ltd. 2001, 2020. All Rights Reserved. *
#                                                                   *
#  Note to US Government Users Restricted Rights:                   *
#                                                                   *
#  Use, duplication or disclosure restricted by GSA ADP Schedule    *
# *******************************************************************


echo "Generating users and changing wpsadmin password."
pw1=$(< /dev/urandom tr -dc _A-Z-a-z-0-9 | head -c${1:-10};echo;)  
pw2=$(< /dev/urandom tr -dc _A-Z-a-z-0-9 | head -c${1:-10};echo;)  
pw3=$(< /dev/urandom tr -dc _A-Z-a-z-0-9 | head -c${1:-10};echo;)  
pw4=$(< /dev/urandom tr -dc _A-Z-a-z-0-9 | head -c${1:-10};echo;)  
pw5=$(< /dev/urandom tr -dc _A-Z-a-z-0-9 | head -c${1:-10};echo;)  
pwadmin=$(< /dev/urandom tr -dc _A-Z-a-z-0-9 | head -c${1:-10};echo;)  
echo "====== USERS ======"
echo "user0001:$pw1"
echo "user0002:$pw2"
echo "user0003:$pw3"
echo "user0004:$pw4"
echo "user0005:$pw5"
echo "wpsadmin:$pwadmin"
echo "====== USERS ======"
sed -i "s/rndpw1/$pw1/g" users.py
sed -i "s/rndpw2/$pw2/g" users.py
sed -i "s/rndpw3/$pw3/g" users.py
sed -i "s/rndpw4/$pw4/g" users.py
sed -i "s/rndpw5/$pw5/g" users.py
sed -i "s/wpsadminrandpw/$pwadmin/" users.py
