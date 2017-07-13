#!/bin/bash
if [ $# -ne 1 ]; then
	echo "build_cmd SERVER_IP"
	exit
fi
if [ -z $JAVA_HOME ]; then
	export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_131.jdk/Contents/Home
fi
export WEBSOCKET_SERVER_IP=$1
echo "Setting Web Socket Address to $WEBSOCKET_SERVER_IP"
./gradlew assembleRelease

echo copying FROM  ../neo-service/app/build/outputs/aar/app-release.aar TO ./neo-service/
cp ./app/build/outputs/aar/app-release.aar ../neo-proto/neo-service/
