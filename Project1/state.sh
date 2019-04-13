#!/bin/sh
if [ "$#" -ne 1 ]; then
  echo "Usage: state.sh <peer_ap>"
  exit 1
fi

java -cp out/production/Project1 test.TestApp "$1" STATE