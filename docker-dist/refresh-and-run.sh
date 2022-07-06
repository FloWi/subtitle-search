#!/usr/bin/env bash

trap 'exit 0' SIGTERM

set -e

if [ ! -d "webapp/assets" ]; then
  ###  Control will jump here if $DIR does NOT exists ###
  echo "Error: Directory assets not found. Please mount directory into webapp/assets"
  exit 1
fi

(cd webapp/assets; tree -J -s -f --noreport . > listing.json)

#echo "generate directory listing"
#(cd webapp || exit; node ../generate-listing.js > listing.json)

echo "starting server"

# exec is important for listening to SIGTERM
exec /miniserve-linux --port 3000 --index index.html webapp
