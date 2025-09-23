appscanReport=""
appscanDirectory="."
appscanRegex="_Normal_CLI_Assessment_"
appscanListUrl="https://artifactory.cwp.pnp-hcl.com/artifactory/list/quintana-generic/appscan/reports"
appscanUrl=${appscanListUrl//\/list\//\/}
scriptName=$(basename $0)
nameAsBaseline="no"
cleandir="yes"
quiet="no"

# Command line help.

showHelp() {
   echo "$scriptName is a tool to download appscan report files from Artifactory."
   echo "If no directory specified the download will go into the current directory."
   echo "The download directory will be cleaned up before the new download starts."
   echo " "
   echo "Syntax: $scriptName -r report [-x regex] [-d directory] [-h]"
   echo "        -r, --report    - defines the report folder in Artifactory"
   echo "        -x, --regex     - specifies a file filter for the download"
   echo "                          (default: _Normal_CLI_Assessment_)"
   echo "        -d, --directory - specifies the download directory"
   echo "                          (default: current directory)"
   echo "        -b, --baseline  - rename downloaded files as baseline"
   echo "        -q, --quiet     - no output during execution"
   echo "        -a, --append    - add download to existing directory"
   echo "        -u, --url       - resets the value of appscanListUrl"
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
        -x | --regex)    test2ndParm "$gp_flag" "$2"
                         appscanRegex="$2"
                         shift 2
                         ;;
        -d | --directory) test2ndParm "$gp_flag" "$2"
                         appscanDirectory="$2"
                         shift 2
                         ;;
        -b | --baseline) nameAsBaseline="yes"
                         shift 1
                         ;;
        -a | --append)   cleandir="no"
                         shift 1
                         ;;
        -u | --url)      test2ndParm "$gp_flag" "$2"
                         appscanListUrl="$2"
                         appscanUrl=${appscanListUrl//\/list\//\/}
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

if [ "$appscanReport" == "" ]; then
   echo "Missing report to download."
   showHelp
fi

appscanFiles=$(curl --silent $appscanListUrl/$appscanReport/ | grep $appscanRegex | grep -v "_debug" |tr -s '[:blank:]' ',' | cut -d ',' -f 2 | sed 's/.*">//' | sed 's/\/.*>//' | sed 's/<//')

if [ "$cleandir" == "yes" ]; then
   if [ -d ${appscanDirectory} ]; then
      rm -fR ${appscanDirectory}/*
   else
      mkdir ${appscanDirectory}
   fi
fi

for file in $appscanFiles; do
   if [ "$quiet" == "no" ]; then
      echo "$appscanUrl/$appscanReport/$file"
   fi
   if [ "$nameAsBaseline" == "yes" ]; then
      fname=${file%%${appscanRegex}*}
      fname="${fname}_baseline.ozasmt"
   else
      fname=$file
   fi
   curl --silent -o ${appscanDirectory}/$fname "$appscanUrl/$appscanReport/$file"
done
