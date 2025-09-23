artifactoryRootDirectory="appscan/dx-reports"
artifactoryReportDirectory=""
appscanDirectory="."
baselineMetafile="baseline.metadata"
scriptName=$(basename $0)
quiet="no"

# Command line help.

showHelp() {
   echo "$scriptName is a tool to create a baseline metafile."
   echo " "
   echo "Syntax: $scriptName -r report [-m root] [-d directory] [-h]"
   echo "        -r, --report    - defines the report folder in Artifactory"
   echo "        -m, --master    - defines the root folder in Artifactory"
   echo "        -d, --directory - specifies the download directory"
   echo "                          (default: current directory)"
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
                         artifactoryReportDirectory="$2"
                         shift 2
                         ;;
        -m | --master)   test2ndParm "$gp_flag" "$2"
                         artifactoryRootDirectory="$2"
                         shift 2
                         ;;
        -d | --directory) test2ndParm "$gp_flag" "$2"
                         appscanDirectory="$2"
                         shift 2
                         ;;
        -q | --quiet)    quiet="yes"
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

if [ "$artifactoryReportDirectory" == "" ]; then
   echo "Missing report to set as metadata."
   showHelp
fi

if [ "$quiet" == "no" ]; then
   echo "$appscanDirectory/$baselineMetafile"
fi
echo "<metadata>" > "$appscanDirectory/$baselineMetafile"
echo "   <artifactoryRootDirectory>" >> "$appscanDirectory/$baselineMetafile"
echo "      $artifactoryRootDirectory" >> "$appscanDirectory/$baselineMetafile"
echo "   </artifactoryRootDirectory>" >> "$appscanDirectory/$baselineMetafile"
echo "   <artifactoryReportDirectory>" >> "$appscanDirectory/$baselineMetafile"
echo "      $artifactoryReportDirectory" >> "$appscanDirectory/$baselineMetafile"
echo "   </artifactoryReportDirectory>" >> "$appscanDirectory/$baselineMetafile"
echo "</metadata>" >> "$appscanDirectory/$baselineMetafile"
