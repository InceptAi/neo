!#/bin/bash

SERVER_IP="192.168.1.128"

echo adb shell setprop com.inceptai.server_ip $SERVER_IP
adb shell setprop com.inceptai.server_ip \"$SERVER_IP\"
