#!/bin/bash

COMMON_MODULE_DIR="../common"
COMMON_TARGET_JAR="$COMMON_MODULE_DIR/target/common-0.0.1-SNAPSHOT.jar"
COMMON_M2_JAR="$HOME/.m2/repository/com/sMall/backend/common/0.0.1-SNAPSHOT/common-0.0.1-SNAPSHOT.jar"

# Check if common module is installed
if [ ! -f "$COMMON_M2_JAR" ]; then
  echo "ERROR: common module is not installed."
  echo "Please run 'mvn clean install' inside the 'sMall/common/' directory first."
  exit 1
fi

# Check if common module is up-to-date
if [ "$COMMON_TARGET_JAR" -nt "$COMMON_M2_JAR" ]; then
  echo "WARNING: Installed common module is outdated."
  echo "Please run 'mvn clean install' inside the 'sMall/common/' directory to update it."
  exit 1
fi

# Load environment variables
export $(cat ../.env | xargs)

# Run the service
mvn spring-boot:run
