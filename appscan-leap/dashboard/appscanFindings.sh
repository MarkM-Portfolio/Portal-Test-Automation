#!/bin/bash
# *****************************************************************
# This script is capable to analyze AppScan Source finding results.
#
# Syntax: see help function
#
#
# HCL Technologies Confidential
#
# (C) Copyright HCL Technologies 2023
#
# *****************************************************************

#
# Global variables
#

action=""
ozasmtXML=""
ozasmtFileset=""
excludeTag="excluded"
confidenceFilter="none"
baselineDirectory=""
poolConfidence=("none" "definitive" "suspect" "scan coverage")
poolSeverity=("high" "medium" "low")
poolFindingData=""
poolSites=""
poolFiles=""
poolString=""
assessmentFindings=""
scriptName=$(basename $0)
quietMode="false"

#
# Subroutines
#

# Command line help.

showHelp() {
   echo "$scriptName is a tool to analyze AppScan Source findings in more details."
   echo " "
   echo "Syntax: $scriptName -i | -a | -s ozamtXml [-h]"
   echo "        -i, --info        - get info from given ozamt file or even a set of files"
   echo "                            i.e. *.ozasmt or *base*.ozasmt"
   echo "                            output is a detailed information about each finding"
   echo "                            i.e. [lineNo:findingType:fileName:codeSnippet]"
   echo "        -s, --summary     - summerize findings from the given ozamt file or set of files"
   echo "                            compared to baseline (syntax same as for info)"
   echo "        -a, --analyze     - analyze findings from given ozamt file or set of files"
   echo "                            compared to baseline (syntax same as for info)"
   echo "        -b, --baseline    - set baseline directory"
   echo "        -f, --filter      - set confidence filter"
   echo "        -q, --quiet       - print only findings"
   echo "        -h, --help        - showing this help"
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

# Return multiple values followed after a single option as string
#
getMultipleValues () {
   local GMV_values=""
   local GMV_param

   while [ $# -gt 0 ]; do
      GMV_param=$1
      if [[ "$GMV_param" == "-"* ]] || [[ "$GMV_param" == "" ]]; then
         break
      fi
      GMV_values="$GMV_values $1"
      shift 1
   done
   echo $GMV_values
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
   local gp_shift
   while [ $# -gt 0 ]; do
      gp_flag=$1
      case "${gp_flag}" in
        -i | --info)     test2ndParm "$gp_flag" "$2"
                         shift 1
                         ozasmtXML=$(getMultipleValues $@)
                         gp_shift=$(echo "$ozasmtXML" | wc -w)
                         if [ "$gp_shift" == "1" ]; then
                            ozasmtXML=$(find $(dirname $ozasmtXML) -type f -name $(basename $ozasmtXML))
                         fi
                         ozasmtFileset=$ozasmtXML
                         action="--info"
                         shift $gp_shift
                         ;;
        -s | --summary)  test2ndParm "$gp_flag" "$2"
                         shift 1
                         ozasmtXML=$(getMultipleValues $@)
                         gp_shift=$(echo "$ozasmtXML" | wc -w)
                         if [ "$gp_shift" == "1" ]; then
                            ozasmtXML=$(find $(dirname $ozasmtXML) -type f -name $(basename $ozasmtXML))
                         fi
                         ozasmtFileset=$ozasmtXML
                         action="--summary"
                         shift $gp_shift
                         ;;
        -a | --anylyze)  test2ndParm "$gp_flag" "$2"
                         shift 1
                         ozasmtXML=$(getMultipleValues $@)
                         gp_shift=$(echo "$ozasmtXML" | wc -w)
                         if [ "$gp_shift" == "1" ]; then
                            ozasmtXML=$(find $(dirname $ozasmtXML) -type f -name $(basename $ozasmtXML))
                         fi
                         ozasmtFileset=$ozasmtXML
                         action="--analyze"
                         shift $gp_shift
                         ;;
        -b | --baseline) test2ndParm "$gp_flag" "$2"
                         baselineDirectory="$2"
                         shift 2
                         ;;
        -f | --filter)   test2ndParm "$gp_flag" "$2"
                         confidenceFilter="$2"
                         shift 2
                         ;;
        -q | --quiet)    quietMode="true"
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

# Return value of a single xml line for given key
#
getXmlLineValue () {
   local GXLV_line=$1
   local GXLV_key=$2
   local GXLV_value

   GXLV_key="* ${GXLV_key}=\""
   GXLV_value=${GXLV_line#$GXLV_key}
   GXLV_value=${GXLV_value%%\"*}
   echo "$GXLV_value"
}

# Return value of a single xml line for given key
#
getXmlFileValue () {
   local GXFV_file=$1
   local GXFV_linekey=$2
   local GXFV_valuekey=$3
   local GXFV_line
   local GXFV_value

   GXFV_line=$(grep --max-count=1 "$GXFV_linekey" $GXFV_file)
   GXFV_value=$(getXmlLineValue "$GXFV_line" "$GXFV_valuekey")
   echo "$GXFV_value"
}

# Return baseline XML filename of a given XML file
#
getBaselineXml () {
   local GBX_file=$1
   local GBX_baseline=$2
   local GBX_regex="_Normal_CLI_Assessment_"
   local GBX_baselineXml

   GBX_baselineXml=${GBX_file%%${GBX_regex}*}
   GBX_baselineXml="${GBX_baselineXml}_baseline.ozasmt"
   if [ "$GBX_baseline" != "" ]; then
      GBX_baselineXml=$(basename $GBX_baselineXml)
      GBX_baselineXml="$GBX_baseline/$GBX_baselineXml"
   fi
   echo "$GBX_baselineXml"
}

# Get section data
# from <FindingDataPool>, <SitePool>, <FilePool>, and <StringPool>
#
getSectionDataFromXML () {
   local GSDFX_ozasmtXML=$1
   
   poolFindingData=$(cat $GSDFX_ozasmtXML | grep "<FindingData ")
   poolSites=$(cat $GSDFX_ozasmtXML | grep "<Site ")
   poolFiles=$(cat $GSDFX_ozasmtXML | grep "<File ")
   poolStrings=$(cat $GSDFX_ozasmtXML | grep "<String ")
}

# Return finding details from finding mentioned in assessment section
#
getDetailsFromFinding () {
   local GDFF_idNo=$1
   local GDFF_xmlLine
   local GDFF_lineNo
   local GDFF_confidence
   local GDFF_severity
   local GDFF_code
   local GDFF_scanFile

   # Get site id from finding
   GDFF_xmlLine=$(echo "$poolFindingData" |grep " id=\"$GDFF_idNo\"")
   GDFF_idNo=$(getXmlLineValue "$GDFF_xmlLine" "site_id")
   
   # Get confidence of finding
   GDFF_confidence=$(getXmlLineValue "$GDFF_xmlLine" "conf")
   
   # Get severity of finding
   GDFF_severity=$(getXmlLineValue "$GDFF_xmlLine" "sev")

   # Get file id from site pool
   GDFF_xmlLine=$(echo "$poolSites" |grep " id=\"$GDFF_idNo\"")
   GDFF_idNo=$(getXmlLineValue "$GDFF_xmlLine" "file_id")
   
   # Get line number of finding
   GDFF_lineNo=$(getXmlLineValue "$GDFF_xmlLine" "ln")
   
   # Get code of finding
   GDFF_code=$(getXmlLineValue "$GDFF_xmlLine" "cxt")
   GDFF_xmlLine=$(echo "$poolStrings" |grep " id=\"$GDFF_code\"")
   GDFF_code=$(getXmlLineValue "$GDFF_xmlLine" "value")

   # Get filename of finding
   GDFF_xmlLine=$(echo "$poolFiles" |grep " id=\"$GDFF_idNo\"")
   GDFF_scanFile=$(getXmlLineValue "$GDFF_xmlLine" "value")
   GDFF_scanFile=${GDFF_scanFile#/Apps/}

   echo "[$GDFF_lineNo:$GDFF_severity:${poolConfidence[$GDFF_confidence]}:$GDFF_scanFile:$GDFF_code]"
}

# Return all finding from assessment section with or without excluded
#
getAssessmentFindings () {
   local GAF_ozasmtFile=$1
   local GAF_excludeTags=$2
   local GAF_searchTag="<Finding "
   local GAF_findings

   if [ "$GAF_excludeTag" == "" ]; then
      GAF_findings=$(cat $GAF_ozasmtFile | grep "$GAF_searchTag")
   else
      GAF_findings=$(cat $GAF_ozasmtFile | grep "$GAF_searchTag" | grep -v "$GAF_excludeTag")
   fi

   echo $GAF_findings
}

getDetailsFromSingleRun () {
   local l_ozasmtXML=$1
   local l_idNo=""
   local l_findings=""
   local l_findingDetails=""
   
   # Get all findings from the assessments
   #
   l_findings=$(getAssessmentFindings $l_ozasmtXML "$excludeTag")

   for f in $l_findings; do
      if [[ "$f" == *"data_id"* ]]; then
         l_idNo=$(getXmlLineValue "<Finding $f" "data_id")
         l_findingDetails=$(getDetailsFromFinding $l_idNo)
         if [ "$action" == "--analyze" ]; then
            l_findingDetails=${l_findingDetails// /_._}
         fi
         echo $l_findingDetails
      fi
   done
}

getSummaryFromSingleRun () {
   local l_ozasmtXML=$1
   local l_no=""
   local l_findings=""
   local l_files=""
   
   l_files=$(cat $l_ozasmtXML |grep "<AsmntFile file_id=" | wc -l)
   echo "Total files scanned: $l_files"
   l_findings=$(cat $l_ozasmtXML | grep "<Finding data_id=" | wc -l)
   echo "Total findings: $l_findings"
   l_no=$(cat $l_ozasmtXML | grep "<Finding data_id=" | grep "excluded=" | wc -l)
   echo "  Total excluded findings: $l_no"
   l_no=$(cat $l_ozasmtXML | grep "<Finding data_id=" | grep -v "excluded=" | wc -l)
   echo "  Total vital findings: $l_no"
}

consoleLog () {
   local CL_message=$1

   if [ "$quietMode" != "true" ]; then
      echo "$CL_message"
   fi
}

#
# Main program
#

# Get and validate command line parameters
getParams $@

if [ "$ozasmtXML" == "" ]; then
   echo "No file(s) to analyze."
   showHelp
fi

for ozasmtXML in $ozasmtFileset; do
   assessmentRun=$(getXmlFileValue "$ozasmtXML" "<AssessmentRun" "name")
   if [ "$baselineDirectory" != "" ]; then
      baselineXML=$(getBaselineXml $ozasmtXML $baselineDirectory)
      baselineRun=$(getXmlFileValue "$baselineXML" "<AssessmentRun" "name")
   fi
   getSectionDataFromXML "$ozasmtXML"
   if [ "$action" == "--info" ]; then
      consoleLog "Findings scan $assessmentRun ..."
      getDetailsFromSingleRun "$ozasmtXML"
      if [ "$baselineDirectory" != "" ]; then
         consoleLog "Findings baseline $baselineRun ..."
         getDetailsFromSingleRun "$baselineXML"
      fi
   elif [ "$action" == "--summary" ]; then
      if [ "$baselineDirectory" != "" ]; then
         consoleLog "Summary baseline $baselineRun ..."
         getSummaryFromSingleRun "$baselineXML"
      else
         consoleLog "Summary scan $assessmentRun ..."
         getSummaryFromSingleRun "$ozasmtXML"
      fi
   elif [ "$action" == "--analyze" ]; then
      consoleLog "Get appscan findings $assessmentRun ..."
      appscanDetails=$(getDetailsFromSingleRun "$ozasmtXML")
      consoleLog "Get baseline findings $baselineRun ..."
      baselineDetails=$(getDetailsFromSingleRun "$baselineXML")
      consoleLog "Get new findings ..."
      for line in $appscanDetails; do
         if [[ "$baselineDetails" != *"$line"* ]]; then
            if [ "$confidenceFilter" == "none" ] || [[ "$line" == *":${confidenceFilter}:"* ]]; then
               line=${line//_._/ }
               echo $line
            fi
         fi
      done
   fi
done
 
