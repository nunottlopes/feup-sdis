#!/bin/sh
if [ "$#" -ne 2 ]; then
  echo "Usage: restore.sh <peer_ap> <file_path>"
  exit 1
fi

java -cp out/production/Project1 test.TestApp "$1" RESTORE "$2"