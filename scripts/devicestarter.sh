#!/bin/bash
#######################################################
# Description : Shell script to start Possum Service
#######################################################

# function zone start

function CheckConnectivity() {
	deviceVIDPID=$1
	notConnected=`lsusb | grep $deviceVIDPID`
	if [ -z "$notConnected" ]; then return 0
	else return 1
	fi
}

function SpringProfile() {
  regType=$(echo "$PRODUCTNAME" | cut -d- -f1)
  if [ "$domenv" == "labs" ]
  then
    echo "Lab environment"; echo "Setting spring profile to dev"
    export SPRING_PROFILES_ACTIVE=dev
  elif [ "$domenv" == "stores" ]
  then
    echo "Stores environment"; echo "Setting spring profile to prod"
    export SPRING_PROFILES_ACTIVE=prod
  else
    echo "Setting spring profile to local"
    export SPRING_PROFILES_ACTIVE=local
  fi
}

function cleanStartDevices() {
    # restart NCRRetail service
    systemctl restart NCRRetail
    sleep 5
}

# function zone end

# Restart supporting services(if required) example NCRRetail
cleanStartDevices

echo ""
echo "################################"
echo "Verify Environment Settings..."
echo "################################"
echo ""

# Setting Spring Profile
SpringProfile


export PATH=$PATH:$JAVA_HOME/bin
# change it to dev for ATM

# Define all class path here
CP=$CLASSPATH
#========== Datalogic ==========#
CP=$CP:/usr/local/Datalogic/JavaPOS/*
CP=$CP:/usr/local/Datalogic/JavaPOS/SupportJars/*
#========== NCR PSL ============#
CP=$CP:/usr/local/ncr/platform
CP=$CP:/usr/local/ncr/platform/jpos/NcrJavaPosControls.jar
CP=$CP:/usr/local/ncr/platform/jpos/NcrJavaPosServices.jar
CP=$CP:/usr/local/ncr/platform/jpos/javapos.jar
CP=$CP:/usr/local/ncr/platform/jpos/json-simple-1.1.jar
CP=$CP:/usr/local/ncr/platform/jpos/xerces.jar
CP=$CP:/usr/local/ncr/platform/jpos/NCRLogger.jar
CP=$CP:/usr/local/ncr/platform/ncrdeviceassistant/NcrDeviceAssistantConsole.jar
CP=$CP:/usr/local/ncr/platform/PlatformRuntime.jar
CP=$CP:/usr/local/ncr/platform_utilities/NcrAuthentication.jar
CP=$CP:/usr/local/ncr/platform_utilities/TransactionTool.jar
CP=$CP:/usr/local/ncr/platform_utilities/DeviceConfigTool.jar
CP=$CP:/usr/local/ncr/platform_utilities/LogCollectorLib.jar
CP=$CP:/usr/local/ncr/platform_utilities/LogCollector.jar
#========== Honeywell ==========#
CP=$CP:/usr/local/Honeywell/HWHydraSO.jar
CP=$CP:/usr/local/Honeywell/honeywelljpos.jar
CP=$CP:/usr/local/Honeywell/jpos113.jar
CP=$CP:/usr/local/Honeywell/JAI.jar
CP=$CP:/usr/local/Honeywell/jcl.jar
CP=$CP:/usr/local/Honeywell/jpos113-controls.jar
CP=$CP:/usr/local/Honeywell/JavaPOSSuite.jar
CP=$CP:/usr/local/Honeywell/jpos111.jar
CP=$CP:/usr/local/Honeywell/log4j-api-2.17.1.jar
CP=$CP:/usr/local/Honeywell/log4j-core-2.17.1.jar
CP=$CP:/usr/local/Honeywell/log4j-1.2-api-2.17.1.jar
CP=$CP:/usr/local/Honeywell/RXTXcomm.jar
CP=$CP:/usr/local/Honeywell/xerces.jar
#========== NCR RPSL ==========#
CP=$CP:/usr/local/NCRRetail
CP=$CP:/usr/local/NCRRetail/jar/NCRRetailCommon.jar
CP=$CP:/usr/local/NCRRetail/jar/xerces.jar
CP=$CP:/usr/local/NCRRetail/jar/NCRConfigManager.jar
CP=$CP:/usr/local/NCRRetail/jar/jpos.jar
CP=$CP:/usr/local/NCRRetail/jar/NCRJavaPOS.jar
CP=$CP:/usr/local/NCRRetail/jar/NCRJavaPOS316.jar
#========== ZEBRA ==========#
CP=$CP:/usr/lib/zebra-scanner/javapos/jpos/javaPOS114.jar
CP=$CP:/usr/share/zebra-scanner/javapos/xml
CP=$CP:/usr/share/zebra-scanner/javapos/config
CP=$CP:/usr/lib/zebra-scanner/javapos/jpos/JposLogger.jar
CP=$CP:/usr/lib/zebra-scanner/javapos/jpos/JposServiceJniScale.jar
CP=$CP:/usr/lib/zebra-scanner/javapos/jpos/JposServiceJniScanner.jar
CP=$CP:/usr/lib/zebra-scanner/javapos/jpos/JposServiceOnScale.jar
CP=$CP:/usr/lib/zebra-scanner/javapos/jpos/JposServiceOnScanner.jar
CP=$CP:/usr/lib/zebra-scanner/javapos/jpos/JposServiceScanner.jar
CP=$CP:/usr/lib/zebra-scanner/javapos/jpos/JposServiceScale.jar
#========== ELO ==========#
CP=$CP:/usr/local/ELO/jar/eloJPosService114.jar
#========== Possum ==========#
CP=$CP:/opt/target/possum/$(find PossumDeviceManager*)
CP=$CP:.

#Define all LIB path here
LIB_PATH=$LD_LIBRARY_PATH
LIB_PATH=$LIB_PATH:/usr/lib/
LIB_PATH=$LIB_PATH:/lib64/
LIB_PATH=$LIB_PATH:/usr/local/NCRRetail/lib/
LIB_PATH=$LIB_PATH:/usr/local/ncr/aero:/usr/local/ncr/platform
LIB_PATH=$LIB_PATH:/usr/local/Datalogic/JavaPOS/SupportJars
LIB_PATH=$LIB_PATH:/usr/local/Datalogic/JavaPOS/
LIB_PATH=$LIB_PATH:/usr/local/Honeywell/
LIB_PATH=$LIB_PATH:/usr/local/Honeywell/externalLib
LIB_PATH=$LIB_PATH:/usr/lib/zebra-scanner/javapos/jni


echo "DeviceManager log file path " $POSSUM_LOG_PATH
if [ -z "$POSSUM_LOG_PATH" ]; then
  echo "Setting default path for log"
  POSSUM_LOG_PATH="/var/log/target/possum"
fi
mkdir -p $POSSUM_LOG_PATH/CrashLog


echo ""
echo "################################"
echo "Starting Possum..."
echo "################################"
echo ""

cd /opt/target/possum

# TODO Need to check if NCRRetail form init.d and NCRLoader is running.
java -cp $CP -Djava.library.path=$LIB_PATH -XX:ErrorFile=$POSSUM_LOG_PATH/CrashLog/hs_err_pid%p.log -XX:ReplayDataFile=$POSSUM_LOG_PATH/CrashLog/replay_pid%p.log -Dlog4j2.formatMsgNoLookups=true -Dloader.main=com.target.devicemanager.DeviceMain org.springframework.boot.loader.PropertiesLauncher
