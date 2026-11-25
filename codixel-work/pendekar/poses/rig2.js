// Rig for the v2 idle pose (96x64 canvas, faces left, keris in the hand on the right of the picture)
const P = require(require("path").join(__dirname, "../../lib/pixel.js"));
module.exports = (spr) => new P.Rig(spr)
  .bone("legs", null, 44, 54, 44, 61)
  .bone("body", "legs", 44, 53, 44, 29)
  .bone("head", "body", 45, 28, 45, 16)
  .bone("armK", "body", 54, 33, 58, 47)
  .bone("armL", "body", 37, 33, 36, 45)
  .bind({
    joint: 1,
    paint: {
      head: [[37, 13], [57, 13], [57, 28], [37, 28]],
      armK: [[51, 31], [67, 31], [67, 54], [54, 54], [54, 42], [51, 42]],
      armL: [[32, 32], [40, 32], [40, 41], [38, 41], [38, 48], [32, 48]],
      legs: [[35, 54], [57, 54], [57, 63], [35, 63]],
    },
  });
