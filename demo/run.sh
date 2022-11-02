#!/bin/sh
# -Dagent=true 
mvn -Pnative clean package -DskipTests && target/demo
