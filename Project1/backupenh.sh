#!/bin/sh
if [ "$#" -ne 3 ]; then
  echo "Usage: backupenh.sh <peer_ap> <file_path> <replication_degree>"
  exit 1
fi

java -cp out/production/Project1 test.TestApp "$1" BACKUPENH "$2" "$3"