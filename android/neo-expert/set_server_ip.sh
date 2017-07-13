#!/bin/bash

SERVER_IP="192.168.1.129"

echo adb shell setprop log.tag.my_ip $SERVER_IP
adb shell setprop log.tag.my_ip \"$SERVER_IP\"
