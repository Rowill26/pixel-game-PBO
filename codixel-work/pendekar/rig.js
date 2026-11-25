// Rig for the Pendekar idle sprite (coordinates are sprite pixels inside the 64x64 canvas)
const P = require(require("path").join(__dirname, "../lib/pixel.js"));
// The sprite is now mirrored (faces left), so every x coordinate below is mirrored: x -> 63 - x
const m = (x) => 63 - x;
const poly = (pts) => pts.map(([x, y]) => [m(x), y]);
module.exports = (spr) => new P.Rig(spr)
  .bone("legs", null, m(36), 52, m(36), 61)
  .bone("body", "legs", m(36), 51, m(36), 29)
  .bone("head", "body", m(37), 28, m(37), 16)
  .bone("armL", "body", m(30), 31, m(26), 46)     // arm holding the keris (right of the picture after mirroring)
  .bone("armR", "body", m(45), 32, m(47), 45)
  .bind({
    joint: 1,
    paint: {
      head: poly([[25, 14], [47, 14], [47, 28], [25, 28]]),
      armL: poly([[12, 32], [30, 32], [30, 49], [24, 56], [12, 56]]),
      armR: poly([[44, 31], [53, 31], [53, 48], [44, 48]]),
      legs: poly([[27, 54], [49, 54], [49, 63], [27, 63]]),
    },
  });
