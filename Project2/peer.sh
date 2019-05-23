#!/bin/sh
if [[ "$#" -eq 4 ]]
then
  java -cp out/production/Project2 peer.Peer "$1" "$2" "$3" 29501 "$4"
elif [[ "$#" -eq 6 ]]
then
    java -cp out/production/Project2 peer.Peer "$1" "$2" "$3" 29501 "$4" "$5" "$6"
else
    echo "Usage: peer.sh <version> <peer_num> <peer_access_point> <chord_port> [<ConnectionPeer address> <ConnectionPeer port>]"
    exit 1
fi


#java -cp out/production/Project2 peer.Peer 1.0 2 peer2 8004 1235 localhost 1234