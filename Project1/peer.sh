#!/bin/sh
if [ "$#" -ne 3 ]; then
  echo "Usage: peer.sh <version> <peer_num> <peer_access_point>"
  exit 1
fi

java -cp out/production/Project1 peer.Peer "$1" "$2" "$3" 224.0.0.15 8001 224.0.0.16 8002 224.0.0.17 8003