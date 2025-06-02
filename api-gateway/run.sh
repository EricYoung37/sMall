#!/bin/bash

COMMON_MODULE_DIR="../common"
COMMON_TARGET_JAR="$COMMON_MODULE_DIR/target/common-0.0.1-SNAPSHOT.jar"
COMMON_M2_JAR="$HOME/.m2/repository/com/sMall/backend/common/0.0.1-SNAPSHOT/common-0.0.1-SNAPSHOT.jar"

# Check if common module is installed
if [ ! -f "$COMMON_M2_JAR" ]; then
  echo "ERROR: common module is not installed."
  echo "Please run 'mvn clean install' inside the 'sMall/common/' directory first."
  read -p "Press enter to close..."
  exit 1
fi

# Check if target jar exists and is newer than the installed one
if [ ! -f "$COMMON_TARGET_JAR" ] || ! cmp -s "$COMMON_TARGET_JAR" "$COMMON_M2_JAR"; then
  echo "WARNING: Installed common module may be outdated."
  echo "Please run 'mvn clean install' inside the 'sMall/common/' directory to update it."
  read -p "Press enter to close..."
  exit 1
fi

# Load environment variables
export $(cat ../.env | xargs)

# Run the service
mvn spring-boot:run
