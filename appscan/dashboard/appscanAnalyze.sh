#!/bin/sh
# *****************************************************************
# This script is capable to analyze AppScan Source scan results for
# DX Core.
#
# Syntax: see help function
#
#
# HCL Technologies Confidential
#
# (C) Copyright HCL Technologies 2023
#
# Output at: https://dxcore-test-results-secure.team-q-dev.com/DX-95-TestReports/s3mount/appscan-source/
#
# *****************************************************************

#
# Global variables
#

quiet="false"
verbose="false"
scanResultUrl="https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/appscan/reports"
scanResultFolder=""
scanBaselineStamp=""
scanBaselineCompare=""
action=""
infokey=""
report=""
outformat=""
ozasmtXML=""
ozasmtFileset=""
assessmentRun=""
applicationName=""
applicationPath=""
baselineDirectory=""
FilePool=("noFile for index 0")


#
# Subroutines
#

# Command line help.

showHelp() {
   echo "appscanAnalyze is a tool to analyze ozamt scan result files from AppScan source."
   echo "The report includes total of scanned files, total findings, and total excluded findings."
   echo "In addition it reports information depending on the requested info passed in as infoKey."
   echo " "
   echo "Syntax: appscanAnalyze.sh -i | -b | -n | -r | -a ozamtXml [-k infoKey] [-h]"
   echo "        -i, --info        - get info from given ozamt file or even a set of files"
   echo "                            i.e. *.ozasmt or *base*.ozasmt"
   echo "        -a, --analyze     - analyze findings from given ozamt file or set of files"
   echo "                            compared to baseline (syntax same as for info)"
   echo "        -b, --baseline    - set baseline directory"
   echo "        -r, --report      - pass in report folder in Artifactory"
   echo "        -s, --stamp       - pass in stamp for use as baseline metadata"
   echo "        -k, --key         - optional key which findings to report"
   echo "                            this is a comma separated list, default is h,m,l"
   echo "                            possible keys to pass in:"
   echo "                              h    : sum of all high rated findings"
   echo "                              ha   : report on all high rated findings (same as h,hh,hm,hl)"
   echo "                              hh   : number of all high-high rated findings"
   echo "                              hm   : number of all high-medium rated findingsv"
   echo "                              hl   : number of all high-low rated findings"
   echo "                              m    : sum of all medium rated findings"
   echo "                              ma   : report on all med rated findings (same as m,mh,mm,ml)"
   echo "                              mh   : number of all medium-high rated findings"
   echo "                              mm   : number of all medium-medium rated findingsv"
   echo "                              ml   : number of all medium-low rated findings"
   echo "                              l    : sum of all low rated findings"
   echo "                              la   : report on all low rated findings (same as l,lh,lm,ll)"
   echo "                              lh   : number of all low-high rated findings"
   echo "                              lm   : number of all low-medium rated findingsv"
   echo "                              ll   : number of all low-low rated findings"
   echo "                              full : full report (same as h,hh,hm,hl,m,mh,mm,ml,l,lh,lm,ll)"
   echo "        -t, --type        - spefifies type of report (short or long)"
   echo "                            default for --info  : long"
   echo "                            default for --files : short"
   echo "        -o, --outformat   - set output format"
   echo "        -u, --url         - resets the value of scanResultUrl"
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
   local GMV_files=""
   local GMV_param

   while [ $# -gt 0 ]; do
      GMV_param=$1
      if [[ "$GMV_param" == "-"* ]]; then
         break
      fi
      GMV_files="$GMV_files $1"
      shift 1
   done
   echo $GMV_files
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
                         action="--info"
                         if [ "$gp_shift" != "1" ]; then
                            ozasmtFileset=$ozasmtXML
                         fi
                         shift $gp_shift
                         ;;
        -a | --anylyze)  test2ndParm "$gp_flag" "$2"
                         shift 1
                         ozasmtXML=$(getMultipleValues $@)
                         gp_shift=$(echo "$ozasmtXML" | wc -w)
                         action="--analyze"
                         if [ "$gp_shift" != "1" ]; then
                            ozasmtFileset=$ozasmtXML
                         fi
                         shift $gp_shift
                         ;;
        -b | --baseline) test2ndParm "$gp_flag" "$2"
                         baselineDirectory="$2"
                         shift 2
                         ;;
        -r | --report)   test2ndParm "$gp_flag" "$2"
                         scanResultFolder="$2"
                         shift 2
                         ;;
        -s | --stamp)    test2ndParm "$gp_flag" "$2"
                         scanBaselineStamp="$2"
                         shift 2
                         ;;
        -k | --key)      test2ndParm "$gp_flag" "$2"
                         infokey="$2"
                         shift 2
                         ;;
        -t | --type)     test2ndParm "$gp_flag" "$2"
                         report="$2"
                         shift 2
                         ;;
        -o | --outformat) test2ndParm "$gp_flag" "$2"
                         outformat="$2"
                         shift 2
                         ;;
        -u | --url)      test2ndParm "$gp_flag" "$2"
                         scanResultUrl="$2"
                         shift 2
                         ;;
        -h | --help)     showHelp
                         ;;
        *)               echo "Unknown option $gp_flag"
                         showHelp
                         ;;
      esac
   done
}

# Read content of component.xml and return as string
#
readOzasmtXML () {
   local ROX_filename=$1
   local ROX_filter=$2
   local ROX_content

   if [ "$ROX_filter" == "" ]; then
      ROX_content=$(cat $ROX_filename)
   else
      ROX_content=$(grep "<File id=" $ROX_filename)
   fi
   echo "$ROX_content"
}

# Return value of a single xml line for given key
#
getXmlLine () {
   local GXL_file=$1
   local GXL_linekey=$2
   local GXL_line=""

   if [ -f $GXL_file ]; then
      GXL_line=$(grep --max-count=1 "$GXL_linekey" $GXL_file)
   fi
   echo "$GXL_line"
}

# Return value of a single xml line for given key
#
getXmlLineValue () {
   local GXLV_line=$1
   local GXLV_key=$2
   local GXLV_value

   GXLV_key="*${GXLV_key}=\""
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
   local GXFV_value=""
   local GXFV_line

   if [ -f $GXFV_file ]; then
      GXFV_line=$(grep --max-count=1 "$GXFV_linekey" $GXFV_file)
      GXFV_value=$(getXmlLineValue "$GXFV_line" "$GXFV_valuekey")
   fi
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

# Return value of a single xml line for given key
#
readFilePool () {
   local RFP_file=$1
   local RFP_pool
   local RFP_fname

   RFP_pool=$(readOzasmtXML "$RFP_file" "<File id=")
   for line in $RFP_pool; do
      if [[ "$line" == *"value"* ]]; then
         RFP_fname=$(getXmlLineValue "$line" "value")
         RFP_fname=${RFP_fname//\\/\/}
         RFP_fname=${RFP_fname//${applicationPath}/}
         FilePool+=("$RFP_fname")
      fi
   done
}

# Return value of a single xml line for given key
#
getScanInfo () {
   local GSI_xmlLine=$1
   local GSI_keyin=$2
   local GSI_report=$3
   local GSI_key="h,m,l"
   local GSI_result=""
   local GSI_xmlValue

   if [ "$GSI_keyin" != "" ]; then
       if [ "$GSI_keyin" == "short" ]; then
          GSI_report="short"
       elif [ "$GSI_keyin" == "full" ]; then
          GSI_key="ha,ma,la"
       else
          GSI_key=$GSI_keyin
       fi
   fi
   GSI_xmlValue=$(getXmlLineValue "$GSI_xmlLine" "total_files")
   if [ "$GSI_report" == "short" ]; then
      GSI_result="$GSI_xmlValue"
   else
      echo "Total files scanned: $GSI_xmlValue"
   fi
   GSI_xmlValue=$(getXmlLineValue "$GSI_xmlLine" "total_findings")
   if [ "$GSI_report" == "short" ]; then
      GSI_result="$GSI_result $GSI_xmlValue"
   else
      echo "Total findings: $GSI_xmlValue"
   fi
   if [[ ",$GSI_key," == *",h,"* ]] || [[ ",$GSI_key," == *",ha,"* ]]; then
      GSI_xmlValue=$(getXmlLineValue "$GSI_xmlLine" "total_high_finding")
      if [ "$GSI_report" == "short" ]; then
         GSI_result="$GSI_result $GSI_xmlValue"
      else
         echo "  High findings: $GSI_xmlValue"
      fi
   fi 
   if [[ ",$GSI_key," == *",hh,"* ]] || [[ ",$GSI_key," == *",ha,"* ]]; then
      GSI_xmlValue=$(getXmlLineValue "$GSI_xmlLine" "total_high_high_finding")
      if [ "$GSI_report" == "short" ]; then
         GSI_result="$GSI_result $GSI_xmlValue"
      else
         echo "      High findings: $GSI_xmlValue"
      fi
   fi 
   if [[ ",$GSI_key," == *",hm,"* ]] || [[ ",$GSI_key," == *",ha,"* ]]; then
      GSI_xmlValue=$(getXmlLineValue "$GSI_xmlLine" "total_high_med_finding")
      if [ "$GSI_report" == "short" ]; then
         GSI_result="$GSI_result $GSI_xmlValue"
      else
         echo "      Med findings: $GSI_xmlValue"
      fi
   fi 
   if [[ ",$GSI_key," == *",hl,"* ]] || [[ ",$GSI_key," == *",ha,"* ]]; then
      GSI_xmlValue=$(getXmlLineValue "$GSI_xmlLine" "total_high_low_finding")
      if [ "$GSI_report" == "short" ]; then
         GSI_result="$GSI_result $GSI_xmlValue"
      else
         echo "      Low findings: $GSI_xmlValue"
      fi
   fi 
   if [[ ",$GSI_key," == *",m,"* ]] || [[ ",$GSI_key," == *",ma,"* ]]; then
      GSI_xmlValue=$(getXmlLineValue "$GSI_xmlLine" "total_med_finding")
      if [ "$GSI_report" == "short" ]; then
         GSI_result="$GSI_result $GSI_xmlValue"
      else
         echo "  Med findings: $GSI_xmlValue"
      fi
   fi 
   if [[ ",$GSI_key," == *",mh,"* ]] || [[ ",$GSI_key," == *",ma,"* ]]; then
      GSI_xmlValue=$(getXmlLineValue "$GSI_xmlLine" "total_med_high_finding")
      if [ "$GSI_report" == "short" ]; then
         GSI_result="$GSI_result $GSI_xmlValue"
      else
         echo "      High findings: $GSI_xmlValue"
      fi
   fi 
   if [[ ",$GSI_key," == *",mm,"* ]] || [[ ",$GSI_key," == *",ma,"* ]]; then
      GSI_xmlValue=$(getXmlLineValue "$GSI_xmlLine" "total_med_med_finding")
      if [ "$GSI_report" == "short" ]; then
         GSI_result="$GSI_result $GSI_xmlValue"
      else
         echo "      Med findings: $GSI_xmlValue"
      fi
   fi 
   if [[ ",$GSI_key," == *",ml,"* ]] || [[ ",$GSI_key," == *",ma,"* ]]; then
      GSI_xmlValue=$(getXmlLineValue "$GSI_xmlLine" "total_med_low_finding")
      if [ "$GSI_report" == "short" ]; then
         GSI_result="$GSI_result $GSI_xmlValue"
      else
         echo "      Low findings: $GSI_xmlValue"
      fi
   fi 
   if [[ ",$GSI_key," == *",l,"* ]] || [[ ",$GSI_key," == *",la,"* ]]; then
      GSI_xmlValue=$(getXmlLineValue "$GSI_xmlLine" "total_low_finding")
      if [ "$GSI_report" == "short" ]; then
         GSI_result="$GSI_result $GSI_xmlValue"
      else
         echo "  Low findings: $GSI_xmlValue"
      fi
   fi 
   if [[ ",$GSI_key," == *",lh,"* ]] || [[ ",$GSI_key," == *",la,"* ]]; then
      GSI_xmlValue=$(getXmlLineValue "$GSI_xmlLine" "total_low_high_finding")
      if [ "$GSI_report" == "short" ]; then
         GSI_result="$GSI_result $GSI_xmlValue"
      else
         echo "      High findings: $GSI_xmlValue"
      fi
   fi 
   if [[ ",$GSI_key," == *",lm,"* ]] || [[ ",$GSI_key," == *",la,"* ]]; then
      GSI_xmlValue=$(getXmlLineValue "$GSI_xmlLine" "total_low_med_finding")
      if [ "$GSI_report" == "short" ]; then
         GSI_result="$GSI_result $GSI_xmlValue"
      else
         echo "      Med findings: $GSI_xmlValue"
      fi
   fi 
   if [[ ",$GSI_key," == *",ll,"* ]] || [[ ",$GSI_key," == *",la,"* ]]; then
      GSI_xmlValue=$(getXmlLineValue "$GSI_xmlLine" "total_low_low_finding")
      if [ "$GSI_report" == "short" ]; then
         GSI_result="$GSI_result $GSI_xmlValue"
      else
         echo "      Low findings: $GSI_xmlValue"
      fi
   fi 
   GSI_xmlValue=$(getXmlLineValue "$GSI_xmlLine" "total_excluded_findings")
   if [ "$GSI_report" == "short" ]; then
      GSI_result="$GSI_result $GSI_xmlValue"
      echo "$GSI_result"
   else
      echo "Total excluded findings: $GSI_xmlValue"
   fi
}

# Convert long scan output to short
#
convertLong2Short () {
   local CLS_in=$1
   local CLS_flag=$2
   local CLS_tmp
   local CLS_out=${CLS_in%% *}

   if [[ "$CLS_in" != *"Total files scanned:"* ]]; then
      CLS_out="$CLS_in"
   else
      if [ "$CLS_flag" == "baseline" ]; then
         CLS_out="Baseline"
      fi
      if [[ "$CLS_in" == *" {{long}} "* ]]; then
         CLS_out="${CLS_out} {{long}}"
      fi
      if [[ "$CLS_in" == *" {{short}} "* ]]; then
         CLS_out="${CLS_out} {{short}}"
      fi
      if [[ "$CLS_in" == *" {{link}} "* ]]; then
         CLS_out="${CLS_out} {{link}}"
      fi
      CLS_tmp=${CLS_in#*Total files scanned: }
      CLS_tmp=${CLS_tmp%% *}
      CLS_out="${CLS_out} ${CLS_tmp}"
      CLS_tmp=${CLS_in#*Total findings: }
      CLS_tmp=${CLS_tmp%% *}
      CLS_out="${CLS_out} ${CLS_tmp}"
      CLS_tmp=${CLS_in#*High findings: }
      CLS_tmp=${CLS_tmp%% *}
      CLS_out="${CLS_out} ${CLS_tmp}"
      CLS_tmp=${CLS_in#*Med findings: }
      CLS_tmp=${CLS_tmp%% *}
      CLS_out="${CLS_out} ${CLS_tmp}"
      CLS_tmp=${CLS_in#*Low findings: }
      CLS_tmp=${CLS_tmp%% *}
      CLS_out="${CLS_out} ${CLS_tmp}"
      CLS_tmp=${CLS_in#*Total excluded findings: }
      CLS_tmp=${CLS_tmp%% *}
      CLS_out="${CLS_out} ${CLS_tmp}"
   fi

   echo "$CLS_out"
}

# Return value of a single compare
#
compareFindings () {
   local CF_file=$1
   local CF_resultdir=$(dirname "$1")
   local CF_baseline="$CF_resultdir/"$2"_baseline.ozasmt"
   local CF_report="$infokey"
   local CF_baselinerun
   local CF_xmlline

   if [ "$outformat" == "xml" ]; then
      CF_report="short"
   fi
   CF_xmlline=$(getXmlLine "$CF_file" "<AssessmentStats")
   getScanInfo "$CF_xmlline" "$CF_report"
   if [ "$CF_report" != "short" ]; then
      echo "-----------------------------------------------"
   fi
   
   CF_baselinerun=$(getXmlFileValue "$CF_baseline" "<AssessmentRun" "name")
   if [ "$CF_baselinerun" != "" ]; then
      echo "Compared baseline results for: $CF_baselinerun"
      CF_xmlline=$(getXmlLine "$CF_baseline" "<AssessmentStats")
      getScanInfo "$CF_xmlline" "$CF_report"
   else
      if [ "$outformat" != "xml" ]; then
         echo "$CF_baseline not found."
      fi
   fi
}

# Format new XML block
#
addXmlBlock () {   
   local AXB_intput=$1
   local AXB_flag=$2
   local AXB_baselineLink="baseline/appscanBaselineDetails.html"
   local AXB_projectsLine="0"
   local AXB_htmlmain=""
   local AXB_root=""
   local AXB_outval=()

   if [[ "${AXB_flag}" == "single"* ]]; then
      if [ "${AXB_flag}" == "singlelong" ]; then
         local AXB_htmlmain="false"
      else
         local AXB_htmlmain="true"
      fi
   fi
   i=0
   for out in $AXB_intput; do
      if [ "$out" == "{{short}}" ]; then
         AXB_projectsLine="1"
         continue
      fi
      if [ "$out" == "{{long}}" ]; then
         AXB_projectsLine="0"
         continue
      fi
      if [ "$out" == "{{link}}" ]; then
         if [ "${AXB_outval[0]}" == "Baseline" ]; then
            out=$AXB_baselineLink
         else
            if [[ "${AXB_outval[0]}" == "Develop"* ]] || [[ "${AXB_outval[0]}" == "Release"* ]] || [[ "${AXB_htmlmain}" == "true"  ]]; then
               out="scan-results/${scanResultFolder//\//_}/appscanResultDetails.html"
            else
               out="$scanResultUrl/$scanResultFolder/${AXB_outval[0]}.pdf"
            fi
         fi
      fi
      AXB_outval[((i++))]=$out
   done
   # Change scan entry if single scan
   #   AXB_htmlmain = true for main xml
   #   AXB_htmlmain = false for detailed xml
   if [ "${AXB_htmlmain}" == "true" ]; then
      AXB_outval[0]="SingleScan_${AXB_outval[0]}_$(date +%s)"
   fi
   if [ "${AXB_htmlmain}" == "false" ]; then
      AXB_outval[0]=${scanResultFolder//\//_}
   fi
   i=2
   echo "   <appscan_run>"
   echo "      <scan>${AXB_outval[0]}</scan>"
   echo "      <link>${AXB_outval[1]}</link>"
   if [ "$AXB_projectsLine" == "1" ]; then
      echo "      <projects>${AXB_outval[((i++))]}</projects>"
   fi
   echo "      <files>${AXB_outval[((i++))]}</files><findings>${AXB_outval[((i++))]}</findings><high>${AXB_outval[((i++))]}</high><med>${AXB_outval[((i++))]}</med><low>${AXB_outval[((i++))]}</low><skipped>${AXB_outval[((i++))]}</skipped><inbaseline>${AXB_outval[((i++))]}</inbaseline>"
   if [ "$scanBaselineCompare" != "" ]; then
      echo "      <baseline>${scanBaselineCompare}</baseline>"
   fi
   echo "   </appscan_run>"
}

# Analyzing single result
#
singleAnalyze () {
   local l_xmlLine
   local l_scanInfo
   
   assessmentRun=$(getXmlFileValue "$ozasmtXML" "<AssessmentRun" "name")
   applicationName=$(getXmlFileValue "$ozasmtXML" "<Application name=" "name")
   applicationPath=$(getXmlFileValue "$ozasmtXML" "<Application name=" "path")
   applicationPath=${applicationPath/${applicationName}.paf/}
   applicationPath=${applicationPath//\\/\/}

   readFilePool "$ozasmtXML"

   if [ "$outformat" != "xml" ]; then
       echo "Analyzing scan results for: $assessmentRun"
   fi

   if [ "$action" == "--info" ]; then
       l_xmlLine=$(getXmlLine "$ozasmtXML" "<AssessmentStats")
       getScanInfo "$l_xmlLine" "$infokey" "$report"
   elif [ "$action" == "--analyze" ]; then
       l_scanInfo=$(compareFindings "$ozasmtXML" "$applicationName")
       if [ "$outformat" == "xml" ]; then
           echo "<results>"
           echo "   <meta>"
           if [ "$report" != "short" ]; then
               echo "      <stamp>${scanResultFolder//\//_}</stamp>"
           fi
           echo "   </meta>"
           addXmlBlock "${applicationName} {{link}} $l_scanInfo {{long}}" "single${report}"
           echo "</results>"
       else
           echo "$l_scanInfo"
       fi
   else
       if [ "$outformat" !== "xml" ]; then
           echo "Nothing to do"
       fi
   fi
}

# Analyzing set of scan results
#
multiAnalyze () {
   local l_infoReport=$report
   local l_scanResults=()
   local l_baseIndex=0
   local l_projectInBase=""
   local l_baselineResult=""
   local l_baselineProjects
   local l_xmlLine
   local l_result
   local l_baselineXml
   
   if [ "$action" == "--analyze" ]; then
      l_infoReport="short"
   fi
   applicationName="Baseline"
   if [ "$report" == "short" ]; then
      l_scanResults[0]=$(echo $ozasmtFileset | wc -w)
      if [ "$action" == "--analyze" ]; then
         l_baselineProjects=$(find $baselineDirectory -type f -name "*.ozasmt" | wc -w)
         l_scanResults[0]=$(( ${l_scanResults[0]} - $l_baselineProjects ))
      fi
      l_baseIndex=1
   fi
   for ozasmtXML in $ozasmtFileset; do
      l_projectInBase="1"
      if [ "$l_infoReport" == "long" ]; then
         assessmentRun=$(getXmlFileValue "$ozasmtXML" "<AssessmentRun" "name")
         echo "Analyzing scan results for: $assessmentRun"
      fi
      l_xmlLine=$(getXmlLine "$ozasmtXML" "<AssessmentStats")
      l_result=$(getScanInfo "$l_xmlLine" "$infokey" "$l_infoReport")
      if [ "$action" == "--analyze" ]; then   
         l_baselineXml=$(getBaselineXml $ozasmtXML $baselineDirectory)
         if [ -e "$l_baselineXml" ]; then
            l_xmlLine=$(getXmlLine "$l_baselineXml" "<AssessmentStats")
            l_baselineResult=$(getScanInfo "$l_xmlLine" "$infokey" "short")
         else
            l_projectInBase="0"
         fi
      fi
      if [ "$report" == "short" ]; then
         i=$l_baseIndex
         for r in $l_result; do
            l_scanResults[$i]=$(( ${l_scanResults[$i]} + $r ))
            i=$(( i + 1 ))
         done
         if [ "$action" == "--analyze" ]; then
            i=$l_baseIndex
            for r in $l_baselineResult; do
               l_scanResults[$i]=$(( ${l_scanResults[$i]} - $r ))
               i=$(( i + 1 ))
            done
         fi
      else
         i=$l_baseIndex
         for r in $l_result; do
            l_scanResults[$i]=$r
            i=$(( i + 1 ))
         done
         if [ "$action" == "--analyze" ]; then
            i=$l_baseIndex
            for r in $l_baselineResult; do
               l_scanResults[$i]=$(( ${l_scanResults[$i]} - $r ))
               i=$(( i + 1 ))
            done
         fi
         if [ "$outformat" == "xml" ]; then
            applicationName=$(getXmlFileValue "$ozasmtXML" "<Application name=" "name")
            echo ",$applicationName {{long}} {{link}} ${l_scanResults[@]} $l_projectInBase;"
         else
            echo "${l_scanResults[@]} $l_projectInBase"
         fi
      fi
   done
   if [ "$report" == "short" ]; then
      if [ "$outformat" == "xml" ]; then
         if [ "$scanResultFolder" != "" ]; then
            applicationName=${scanResultFolder//\//_}
            applicationName=${applicationName//_full/}
            applicationName=${applicationName^}
         fi
         echo ",$applicationName {{short}} {{link}} ${l_scanResults[@]};"
      else
         echo "${l_scanResults[@]}"
      fi
   fi
}

#
# Main program
#

# Get and validate command line parameters
getParams $@

if [ "$ozasmtFileset" != "" ]; then
    if [ "$report" == "" ]; then
       report="short"
    fi
    output=$(multiAnalyze)
    if [ "$outformat" == "html" ]; then
       for out in $output; do
          echo "<td class=\"center-text\">$out</td>"
       done
    elif [ "$outformat" == "xml" ]; then
       if [ "$report" == "short" ] && [ "$action" == "--analyze" ] ; then
          scanBaselineCompare=$(cat appscanBaseline.xml |grep "<stamp>")
          scanBaselineCompare=${scanBaselineCompare#*<stamp>}
          scanBaselineCompare=${scanBaselineCompare%</stamp>*}
       fi
       echo "<results>"
       echo "   <meta>"
       if [ "$scanBaselineStamp" != "" ]; then
          echo "      <stamp>$scanBaselineStamp</stamp>"
       else
          if [ "$report" != "short" ]; then
             echo "      <stamp>${scanResultFolder//\//_}</stamp>"
          fi
       fi
       echo "   </meta>"
       while [ "$output" != "" ]; do
          rawData=${output#*,}
          rawData=${rawData%%;*}
          if [ "$scanResultFolder" == "baseline" ]; then
             rawData=$(convertLong2Short "$rawData")
          fi
          output=${output#*;}
          addXmlBlock "$rawData"
       done
       echo "</results>"
    else
       echo "$output"
    fi
else
    if [ "$report" == "" ]; then
       report="short"
    fi
    singleAnalyze
fi
