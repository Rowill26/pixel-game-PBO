const P = require(require("path").join(__dirname, "../lib/pixel.js"));
const spr = P.load("idle-pix.png");
P.save(P.ruler(spr, 12, { step: 4 }), "ruler.png");
console.log(JSON.stringify(P.bounds(spr)));
