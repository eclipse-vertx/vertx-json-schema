let fs = require('fs');
let ar = [];

fs.readdirSync("format").forEach(file => ar = ar.concat(JSON.parse(fs.readFileSync("format/" + file))));

console.log(JSON.stringify(ar))