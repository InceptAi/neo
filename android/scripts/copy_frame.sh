#!/bin/bash


adb shell "run-as com.inceptai.neoproto chmod 777 /data/data/com.inceptai.neoproto/files/frame-01.png"

adb shell cp /data/data/com.inceptai.neoproto/files/frame-01.png /sdcard/frame-01.png

adb pull /sdcard/frame-01.png



