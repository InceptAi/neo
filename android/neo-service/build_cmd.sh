#!/bin/bash
if [ -z $JAVA_HOME ]; then
	export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_131.jdk/Contents/Home
fi
./gradlew assembleRelease

echo copying FROM  ./app/build/outputs/aar/app-release.aar TO ../neo-proto/neo-service/
cp ./app/build/outputs/aar/app-release.aar ../neo-proto/neo-service/
