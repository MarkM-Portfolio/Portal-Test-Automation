# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2022. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

# This script generates a small HTML report of important commands
# that can be used to monitor and maintain this NFS Server
# The report will be put into a directory from which an HTTP Server can host it

# Target directory for report generation
EXPORTS_REPORT=/home/centos/nginx/exports_report.html

# Create file
rm -f $EXPORTS_REPORT || true
touch $EXPORTS_REPORT

# Writing a copyright header to the HTML file
writeCopyRightHeader() {
  YEAR=$(date +'%Y')
  echo "
  <!--
    ********************************************************************
    * Licensed Materials - Property of HCL                             *
    *                                                                  *
    * Copyright HCL Technologies Ltd. $YEAR. All Rights Reserved.       *
    *                                                                  *
    * Note to US Government Users Restricted Rights:                   *
    *                                                                  *
    * Use, duplication or disclosure restricted by GSA ADP Schedule    *
    ********************************************************************
  -->
  " >> $EXPORTS_REPORT
}

# Write output block function
# Write the output of a command in a "pretty" fashion in a formatted div
# Parameter $1: The heading of your output block
# Parameter $2: The actual command that should be run
writeOutputBlock() {
  local heading=$1
  local command=$2
  echo "<h2>$heading</h2><br>" >> $EXPORTS_REPORT
  echo "<div style='white-space: pre; font-family:monospace; color:#fff; background: #000'>" >> $EXPORTS_REPORT
  printf "$command\r\n\r\n" >> $EXPORTS_REPORT
  $command >> $EXPORTS_REPORT
  echo "" >> $EXPORTS_REPORT
  echo "</div>" >> $EXPORTS_REPORT
  echo "<br><br>" >> $EXPORTS_REPORT
}

# Print html top to file
writeCopyRightHeader
printf "<html>\r\n<title>NFS Report</title>\r\n<body>\r\n" >> $EXPORTS_REPORT

# Print report generation date
echo "<h1>CREATION DATE: $(date)</h1><br>" >> $EXPORTS_REPORT

# Print exports file
writeOutputBlock "/etc/exports" "cat /etc/exports"

# Print all files in exports.d
for fileName in /etc/exports.d/*.exports; do
  writeOutputBlock "$fileName" "cat $fileName"
done

# Print storage usage report
writeOutputBlock "Storage usage" "df -lh"

# Print inode usage report
writeOutputBlock "Inode usage" "df -i"

# Print storage directory listing
writeOutputBlock "Storage directory listing" "ls -lah /mnt/storage"

# Print html bottom to file
printf "</body>\n\r<</html>" >> $EXPORTS_REPORT