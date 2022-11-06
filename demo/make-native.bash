#!/usr/bin/env bash

# https://stackoverflow.com/questions/71146331/graalvm-native-image-build-fails-to-find-log4j-appender-class

rm -rf target/native
mkdir -p target/native
cd target/native
jar -xvf ../demo-12.0.0-SNAPSHOT.jar >/dev/null 2>&1
cp -R META-INF BOOT-INF/classes
native-image -H:Name=demo -cp BOOT-INF/classes:`find BOOT-INF/lib | tr '\n' ':'` -H:ConfigurationFileDirectories=../graalvm-reachability-metadata/*
mv demo ../
