#!/bin/sh
if [ "$#" -ne 2 ]; then
  echo "Usage: reclaim.sh <peer_ap> <disk_space_to_reclaim>"
  exit 1
fi

java -cp out/production/Project1 test.TestApp "$1" RECLAIM "$2"