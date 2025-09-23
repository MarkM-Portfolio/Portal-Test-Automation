#!/bin/bash

# We add a cronjob for wpbuild, since it will need to clean up its docker images
cp /home/centos/setupscripts/helpers/cleanup_containers.sh /home/wpbuild/cleanup_containers.sh
chown wpbuild:wpbuild /home/wpbuild/cleanup_containers.sh
chmod u+x /home/wpbuild/cleanup_containers.sh
cp /home/centos/setupscripts/helpers/cleanup-cron /home/wpbuild/cleanup-cron
chown wpbuild:wpbuild /home/wpbuild/cleanup-cron
chmod u+x /home/wpbuild/cleanup-cron


# Execute setup cron for wpbuild
su - wpbuild -c "/usr/bin/crontab /home/wpbuild/cleanup-cron"