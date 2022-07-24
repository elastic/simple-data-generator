#!/bin/bash 

if [ -z "$3" ];then
  echo "Error: Not enough arguments"
  echo "Usage: build_keystore.ksh <keystore_password> <es_host> <es_port>"
  exit 1;
fi

if [ -z "$1" ];then
  echo -n "No Password for Keystore. What's the password? "; read PASSWORD
else 
  PASSWORD=$1
fi

HOST=$2
PORT=$3

openssl s_client -connect ${HOST}:${PORT} </dev/null \
    | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > es_cloud.cert

keytool -genkey -alias elastic_servers \
    -keyalg RSA -keystore ./keystore.jks \
    -dname "CN=AJ Pahl, OU=PahlSoft, O=Pahl L=Atlanta, S=Georgia, C=US" \
    -storepass ${PASSWORD} -keypass ${PASSWORD}

keytool -import -noprompt -trustcacerts -alias pahl_cloud -file es_cloud.cert \
    -keystore ./keystore.jks -storepass ${PASSWORD}

echo -n "Overwriting existing JKS in scripts directory"
cp keystore.jks scripts/.
