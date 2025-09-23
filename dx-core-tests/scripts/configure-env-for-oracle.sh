#!/bin/sh
#
 ####################################################################
 # Licensed Materials - Property of HCL                              #
 #                                                                  #
 # Copyright HCL Technologies Ltd. 2001, 2023. All Rights Reserved. #
 #                                                                  #
 # Note to US Government Users Restricted Rights:                   #
 #                                                                  #
 # Use, duplication or disclosure restricted by GSA ADP Schedule    #
 ####################################################################
 #

if [ XXX$1 != "XXX" ]; then
    DB_INSTANCE_IDENTIFIER=$1
    SNAPSHOT_ID=$2
    DB_INSTANCE_CLASS=$3
    DB_SUBNET_GROUP_ID=$4
    DB_AVAILABILITY_ZONE=$5
    VPC_SECURITY_GROUP=$6
    echo "DB Instance Identifier : "$DB_INSTANCE_IDENTIFIER
    echo "Snapshot ID : "$SNAPSHOT_ID
    echo "DB Instance Class : "$DB_INSTANCE_CLASS
    echo "DB Subnet Group ID : "$DB_SUBNET_GROUP_ID
    echo "DB Availability Zone : "$DB_AVAILABILITY_ZONE
    echo "VPC Security Group : "$VPC_SECURITY_GROUP
fi

# Create new Oracle instance in AWS RDS from an existing snapshot
aws rds restore-db-instance-from-db-snapshot --db-instance-identifier ${DB_INSTANCE_IDENTIFIER} --db-snapshot-identifier ${SNAPSHOT_ID} --db-instance-class ${DB_INSTANCE_CLASS} --db-subnet-group-name ${DB_SUBNET_GROUP_ID} --availability-zone ${DB_AVAILABILITY_ZONE} --vpc-security-group-ids ${VPC_SECURITY_GROUP}

#Wait for the DB instance to be ready
aws rds wait db-instance-available --db-instance-identifier ${DB_INSTANCE_IDENTIFIER}

#Modify properties files for new DB instance
sed -i "s/database-1/${DB_INSTANCE_IDENTIFIER}/g" /opt/IBM/WebSphere/wp_profile/ConfigEngine/properties/wkplc_dbdomain.properties

#Connect to the new DB instance
/opt/IBM/WebSphere/wp_profile/ConfigEngine/ConfigEngine.sh connect-database

# Start Portal so that DB changes can be picked up
/opt/IBM/WebSphere/wp_profile/bin/startServer.sh WebSphere_Portal

# Stop Portal so that CF upgrade can proceed...
/opt/IBM/WebSphere/wp_profile/bin/stopServer.sh WebSphere_Portal -username wpsadmin -password wpsadmin
