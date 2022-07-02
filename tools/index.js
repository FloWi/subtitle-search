const dirTree = require("directory-tree");
const tree = dirTree(".", {
    attributes: ["size", "type", "extension"],
    exclude: /node_modules|\.DS_Store|listing\.json/

});

console.log(JSON.stringify(tree, null, 2));
