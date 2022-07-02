echo "generate directory listing"
(cd webapp/assets/ && node ../../tools/index.js > listing.json)
