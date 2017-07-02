#!/bin/bash

export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_131.jdk/Contents/Home
./gradlew assembleRelease

echo copying FROM  ../neo-service/app/build/outputs/aar/app-release.aar TO ./neo-service/
cp ./app/build/outputs/aar/app-release.aar ../neo-proto/neo-service/
