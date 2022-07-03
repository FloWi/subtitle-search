echo "generate directory listing"
(cd webapp/assets_non_bundled/ && node ../../tools/index.js > listing.json)
