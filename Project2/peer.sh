#!/bin/sh
if [[ "$#" -eq 3 ]]
then
  java -cp out/production/Project2 peer.Peer "$1" "$2" 29501 "$3"
elif [[ "$#" -eq 5 ]]
then
    java -cp out/production/Project2 peer.Peer "$1" "$2" 29501 "$3" "$4" "$5"
else
    echo "Usage: peer.sh <peer_num> <peer_access_point> <chord_port> [<ConnectionPeer address> <ConnectionPeer port>]"
    exit 1
fi
