#!/bin/sh
if [ "$#" -ne 2 ]; then
  echo "Usage: delete.sh <peer_ap> <file_path>"
  exit 1
fi

java -cp out/production/Project2 test.TestApp "$1" DELETE "$2"