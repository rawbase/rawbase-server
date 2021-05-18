#!/bin/bash

#############################
#
# Run script for R&Wbase 0.1
#
############################

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

port=3030

if [ ! -z "$1" ]
then
    port="$1"
fi

echo "[R&Wbase] Starting server on port $port"

java -jar $DIR/target/rawbase-0.1-server.jar --home=$DIR  --config=$DIR/config-rawbase.ttl --port=$port
