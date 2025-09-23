appscanReport=""
appscanBaseline="baseline"
appscanDirectory="appscan"
appscanXml="appscanResult"
appscanS3Root="s3://dx-testarea/appscan-source"
appscanUpload="no"
scriptName=$(basename $0)

# Command line help.

showHelp() {
   echo "$scriptName is a tool to create a new appscan Dashboard XML files from a given scan result."
   echo " "
   echo "Syntax: $scriptName -r report [-h]"
   echo "        -r, --report    - defines the report folder in Artifactory"
   echo "        -b, --baseline  - defines a folder in Artifactory to use as baseline"
   echo "                          (default: baseline)"
   echo "        -h, --help      - showing this help"
   echo " "
   exit
}

# This function is testing a command line parameter which needs a value.
# The script stops if the parameter value is missing.

test2ndParm() {
   if [[ "$2" == "" || "$2" == "-"* ]]; then
      echo "Wrong usage of parameter $1"
      echo "Missing parameter value."
      echo "Must be:  $1 value"
      echo "Just got: $1 $2"
      exit 1
   fi
}

# This function provides a general mechanism to get command line parameters.
# It gets all parameters passed on the command line, analyse them, sets global variables accordingly.
# The way it works is to loop through all parameters, put the first parameter into a local variable
# which is used to determine the parameter option, sets the appropriate global variables, and finally
# removes the first element before starting the next cycle (shift 1).
# In case an option needs to have a value the second parameter is first validated by test2ndParm()
# and then assigned to a global variable. To keep the parameter list in sync for the next cycle the
# parameter list needs an additional first element removal.
# The cycle ends if no parameter is left in the list. With this mechanism no parameter order is forced.

getParams() {
   local gp_flag
   while [ $# -gt 0 ]; do
      gp_flag=$1
      case "${gp_flag}" in
        -r | --report)   test2ndParm "$gp_flag" "$2"
                         appscanReport="$2"
                         shift 2
                         ;;
        -b | --baseline) test2ndParm "$gp_flag" "$2"
                         appscanBaseline="$2"
                         shift 2
                         ;;
        --upload)        appscanUpload="yes"
                         shift 1
                         ;;
        -h | --help)     showHelp
                         ;;
        *)               echo "Unknown option $gp_flag"
                         showHelp
                         ;;
      esac
   done
}

# Get and validate command line parameters
getParams $@

echo "Download appscan results from $appscanReport"
./appscanDwnlOzasmt.sh -r $appscanReport -d $appscanDirectory -q

if [ "$appscanReport" == "baseline" ]; then
   appscanXml="appscanBaseline"
   echo "Create dashboard baseline report"
   ./appscanAnalyze.sh -o xml -i appscan/*.ozasmt > ${appscanXml}.xml
   echo "Create dashboard baseline details report"
   ./appscanAnalyze.sh -t long -o xml -r $appscanReport -i appscan/*.ozasmt > ${appscanXml}Details.xml
else
   echo "Download baseline to compare with from $appscanBaseline"
   ./appscanDwnlOzasmt.sh -r $appscanBaseline -d baseline --baseline -q
   echo "Create dashboard scan report"
   ./appscanAnalyze.sh -o xml -b baseline -r $appscanReport -a appscan/*.ozasmt > ${appscanXml}.pre.xml
   echo "Create dashboard scan details report"
   ./appscanAnalyze.sh -t long -o xml -b baseline -r $appscanReport -a appscan/*.ozasmt > ${appscanXml}Details.xml
   if [ "$appscanUpload" == "yes" ]; then
      checkFresh=$(aws s3 ls ${appscanS3Root}/ |grep "${appscanXml}.xml")
      if [ "$checkFresh" == "" ]; then
         mv ${appscanXml}.pre.xml ${appscanXml}.xml
      else
         echo "Download existing ${appscanXml}.xml from S3"
         aws s3 cp ${appscanS3Root}/${appscanXml}.xml ${appscanXml}.tmp.xml
         echo "Add new report to ${appscanXml}.xml"
         echo "<results>" > ${appscanXml}.tmp.xml
         cat ${appscanXml}.pre.xml | grep -v "<results>" |grep -v "</results>" >>${appscanXml}.xml
         cat ${appscanXml}.tmp.xml | grep -v "<results>" >>${appscanXml}.xml
      fi
      appscanReport="scan-results/${appscanReport//\//_}"
   fi
fi

if [ "$appscanUpload" == "yes" ]; then
   echo "Upload dashboard reports to S3"
   aws s3 cp ${appscanXml}.xml ${appscanS3Root}/${appscanXml}.xml
   aws s3 cp ${appscanXml}Details.xml ${appscanS3Root}/${appscanReport}/${appscanXml}Details.xml
   aws s3 cp ${appscanXml}Details.html ${appscanS3Root}/${appscanReport}/${appscanXml}Details.html
fi

