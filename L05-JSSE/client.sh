#!/bin/sh

OUT_DIR="out/"

SSLClient="SSLClient"
Host="localhost"
Port=12345
CypherSuite=("TLS_RSA_WITH_AES_128_CBC_SHA" "TLS_DHE_RSA_WITH_AES_128_CBC_SHA")

KeyStore="client.keys"
KeyStore_PW="123456"
TrustStore="truststore"
TrustStore_PW="123456"

if [[ "$#" -ne 2 && "$#" -ne 3 ]]; then
  echo "Usage: client.sh <oper> <opnd>*"
  exit 1
fi

if [[ "$#" -eq 2 ]]; then
    java -Djavax.net.ssl.keyStore="$KeyStore" -Djavax.net.ssl.keyStorePassword="$KeyStore_PW" \
        -Djavax.net.ssl.trustStore="$TrustStore" -Djavax.net.ssl.trustStorePassword="$TrustStore_PW" \
        -cp "$OUT_DIR" "$SSLClient" "$Host" "$Port" "$1" "$2" "${CypherSuite[@]}"
elif [[ "$#" -eq 3 ]]; then
    java -Djavax.net.ssl.keyStore="$KeyStore" -Djavax.net.ssl.keyStorePassword="$KeyStore_PW" \
        -Djavax.net.ssl.trustStore="$TrustStore" -Djavax.net.ssl.trustStorePassword="$TrustStore_PW" \
        -cp "$OUT_DIR" "$SSLClient" "$Host" "$Port" "$1" "$2" "$3" "${CypherSuite[@]}"
fi
