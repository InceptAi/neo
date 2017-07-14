#!/bin/bash
if [ -z $WEBSOCKET_SERVER_IP ]; then
	if [ $# -ne 1 ]; then
		echo "build_cmd SERVER_IP"
		exit
	fi
fi
if [ -z $JAVA_HOME ]; then
	export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_131.jdk/Contents/Home
fi
if [ ! -z $1 ]; then
	export WEBSOCKET_SERVER_IP=$1
fi
echo "Setting Web Socket Address to $WEBSOCKET_SERVER_IP"
./gradlew assembleRelease

echo copying FROM  ./app/build/outputs/aar/app-release.aar TO ../neo-proto/neo-service/
cp ./app/build/outputs/aar/app-release.aar ../neo-proto/neo-service/
