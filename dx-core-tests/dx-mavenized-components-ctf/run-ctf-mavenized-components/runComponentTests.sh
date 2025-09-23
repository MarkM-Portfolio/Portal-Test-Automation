#!/bin/bash
#/*
#********************************************************************
#* Licenses Materials - Property of HCL                             *
#*                                                                  *
#* Copyright HCL Technologies Ltd. 2022. All Rights Reserved.       *
#*                                                                  *
#* Note to US Government Users Restricted Rights:                   *
#*                                                                  *
#* Use, duplication or disclosure restricted by GSA ADP Schedule    *
#********************************************************************
#*/

# Run CTF tests for the provided mavenized dxcore component/s
# Setup initial variables
MVNIZED_COMP_JARS_LOCATION="$2" 
COMP_JSON_LIST="$3" 
CTF_MASTER_RPRT_URL=$4
LATEST_REPORTS_DIR=./reports
DXCORE_SERVER_LOCATION=$WEBSPHERE_ROOT/PortalServer
CTF_REPORTS_DIR=/shared/care/ctf/build/reports/base


# Validate mavenized dxcore components jars location/naming in the instance
validateMavenizedComponentJars(){
MVNIZED_COMP_JARS_LOCATION="$1"
componentName="$2"
if [ -d "$MVNIZED_COMP_JARS_LOCATION" ]; then
    echo "$MVNIZED_COMP_JARS_LOCATION does exist."
    cd $MVNIZED_COMP_JARS_LOCATION
    for name in *.jar; do 
    if [[ $name =~ [-0-9] ]];then
      echo "Renaming the File $name jar to be without hypen(-) characters"
      newname=${name%%-*}.jar
      mv -v "$name" "$newname"
   fi
done
  else  
    echo "ERROR: $MVNIZED_COMP_JARS_LOCATION directory doesn't exist and failed to run CTF tests for $componentName component"
    exit
fi
}

# Jq is required to support json parsing
validateJQ(){
if ! command -v jq
then
    echo "jq is not available in the system and will be installed"
    echo "Installing jq ..."
    yes | yum install epel-release
    yum install jq -y
fi
echo $?
}

# 
runCTFTest(){
    echo Executing Care setup process...
    givenComponent="$1"
    componentCtfTests="$2"
    
    cd /shared/run-ctf-mavenized-components
    chmod 777 /shared/care/rt/ref/copysetup.sh
    cd /shared/care/rt/ref 
    . ./copysetup.sh
    echo Executing Care setupenv process....
    chmod 777 /shared/care/setupenv.sh
    cd /shared/care
    . ./setupenv.sh
    cd /shared/care/rt
    ant -Ddeploy=true care.deploy.ctf
    echo CARE setup started......
    ant care.prepare.setup  
    echo Starting the DX Server......
    /opt/IBM/WebSphere/wp_profile/bin/startServer.sh WebSphere_Portal
    echo Executing the CTF tests for given compnent $givenComponent:$componentCtfTests
    cd /shared/care/rt/
    ant ctf.run -Dctf.input=$componentCtfTests/regression.tsf 
    echo CTF Tests for $givenComponent component executed successfully
    echo $?
}

# Input Component 
givenComponent="$1"
echo Provided Component is: $givenComponent
echo DXCORE_SERVER_LOCATION is: $DXCORE_SERVER_LOCATION
# validate download artifactory jars and jars file name
 validateMavenizedComponentJars "${MVNIZED_COMP_JARS_LOCATION}" "${givenComponent}"

# Identify jq support and install if not
validateJQ


echo "********* Execution start ************"
echo Portal path $WEBSPHERE_ROOT
# Find the given component present in component list (component_list.json)
componentDetails=$(jq -r '.Components[]|select(.name=='\"$givenComponent\"')' $COMP_JSON_LIST)

if [ -z "$componentDetails" ]
then
      echo "Given Component $givenComponent is not existed in the dxcore component list (component_list.json)"
      echo "CTF Test run for this mavenized $componentName component is Failed"
      exit
else
      echo "Given Component $givenComponent is found in the dxcore component list (component_list.json)"
      validCTFTest=false
      componentName=$(echo $componentDetails | jq '.name')
      componentJarName=$(echo $componentDetails | jq '.name_of_JAR')
      componentCorePath=$(echo $componentDetails | jq '.location_of_JAR')
      componentCtfTests=$(echo $componentDetails | jq -r '.ctf_tests')
      echo $givenComponent component details are: Component Name:$componentName, Jar file name:$componentJarName, dxcore component path:$componentCorePath
      componentCorePath=$(echo $componentDetails | jq '.location_of_JAR | @sh' | tr -d \"\')
      componentCorePath="$componentCorePath"
      # for multiple places of component jar in dxcore
      for coreComponentPath in ${componentCorePath[@]}
      do
        echo "coreComponentPath:: $coreComponentPath "
        # Identifying given component exists in mavenized Component jars location
        componentJarName="${componentJarName//\"}" 

        if [ -e $MVNIZED_COMP_JARS_LOCATION/$componentJarName ]
            then
                echo "Mavenized jar for $componentName exists in location $MVNIZED_COMP_JARS_LOCATION"

                # Validating the component jar location in DXCore path
                echo $DXCORE_SERVER_LOCATION$coreComponentPath
                if [ -e $DXCORE_SERVER_LOCATION$coreComponentPath ];  
                    then
                        echo "$componentName component jar file exists in the dxcore path $DXCORE_SERVER_LOCATION$coreComponentPath"
                        # Replace the core component jar in dxcore with the mavenized jar
                        componentCorePathDir=${coreComponentPath%/*}
                         echo "componentCorePathDir:: $componentCorePathDir "
                        # Copying mavenized jar to the dxcore component jar location
                        cp -fr $MVNIZED_COMP_JARS_LOCATION/$componentJarName $DXCORE_SERVER_LOCATION$componentCorePath
                        # ready for CTF tests to run for this component
                        validCTFTest=true
                    else
                        echo "$componentName component jar file does not exist in the dxcore path $DXCORE_SERVER_LOCATION$componentCorePath"
                        echo "CTF Test run for this mavenized $componentName component is Failed"
                        exit
                fi   
            else
                echo "Mavenized $componentName component jar not found in the location $MVNIZED_COMP_JARS_LOCATION"
                echo "CTF Test run for this mavenized $componentName component is Failed"
                exit
        fi
      done  
      # After copying the jar to different dxcore locations, run CTF setup
      if [ "$validCTFTest" = true ] ; then
            echo Executing Care setup process...
            cd /shared/run-ctf-mavenized-components
            chmod 777 /shared/care/rt/ref/copysetup.sh
            cd /shared/care/rt/ref 
            . ./copysetup.sh
            chmod 777 /shared/care/setupenv.sh
            cd /shared/care
            . ./setupenv.sh
            cd /shared/care/rt
            ant -Ddeploy=true care.deploy.ctf
            echo CARE deploy started......
            ant care.prepare.setup  
            echo Starting the DX Server......
            $PROFILE_PATH/bin/startServer.sh WebSphere_Portal
            echo Executing the CTF tests for given component $givenComponent:$componentCtfTests
            cd /shared/care/rt/
            ant ctf.run -Dctf.input=$componentCtfTests/regression.tsf 
            echo CTF Tests for $givenComponent component executed successfully
            echo $?
            cd /shared/run-ctf-mavenized-components
            if [[ ! -e $LATEST_REPORTS_DIR ]]; then
                mkdir $LATEST_REPORTS_DIR
            fi
            echo "Copy generated CTF reports"
            cp -fr $CTF_REPORTS_DIR/$componentCtfTests/ ./$LATEST_REPORTS_DIR/$componentCtfTests
            echo "Retrieving CTF master reports from Instance s3/108"
            mkdir ./$LATEST_REPORTS_DIR/old-$componentCtfTests
            cd ./$LATEST_REPORTS_DIR/old-$componentCtfTests && curl -Ok $CTF_MASTER_RPRT_URL/$componentCtfTests/Summary_regression_tsf_000000.html
            cd /shared/run-ctf-mavenized-components/$LATEST_REPORTS_DIR
            echo "Please check the $componentName component report here: $LATEST_REPORTS_DIR" 
        else
            echo "This Mavenized $componentName component jar not found in dxcore location and cannot run CTF tests"
            echo "CTF Test run for this mavenized $componentName component is Failed"
            exit
      fi
      
fi
echo "********* Execution end ************"
