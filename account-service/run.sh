#!/bin/bash

COMMON_JAR="$HOME/.m2/repository/com/sMall/backend/common/0.0.1-SNAPSHOT/common-0.0.1-SNAPSHOT.jar"

if [ ! -f "$COMMON_JAR" ]; then
  echo "ERROR: common module is not installed."
  echo "Please run 'mvn clean install' inside the 'sMall/common/' directory first."
  exit 1
fi

export $(cat ../common/common.env | xargs)

mvn spring-boot:run