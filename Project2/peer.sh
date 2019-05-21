#!/bin/sh
if [ "$#" -ne 3 ]; then
  echo "Usage: peer.sh <version> <peer_num> <peer_access_point>"
  exit 1
fi

java -cp out/production/Project2 peer.Peer "$1" "$2" "$3" 8001 8002 8003 1234