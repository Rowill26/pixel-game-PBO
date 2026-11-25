const fs = require("fs");
const P = require(require("path").join(__dirname, "../../lib/pixel.js"));
const spr = P.load("poses/v2-idle.png");
const rig = require("./rig2.js")(spr);
const own = P.ruler(P.crop(rig.ownerMap(), 0, 0, 96, 64), 8, { step: 4 });
rig.drawBones(own, 0, 0, {}, { scale: 8 });
P.save(P.sheet([{ buf: own, label: "PEMBAGIAN" }, { buf: rig.legend(), label: "WARNA" }], { gap: 12 }), "poses/owners.png");
const poses = [
  { body: { a: 0, dy: 0 }, head: { a: 0, dy: 0 }, armK: { a: 0, dy: 0 }, armL: { a: 0, dy: 0 } },
  { body: { a: 0, dy: 1 }, head: { a: 0, dy: -1 }, armK: { a: 0, dy: -1 }, armL: { a: 0, dy: 0 } },
  { body: { a: 0, dy: 1 }, head: { a: 0, dy: 0 }, armK: { a: 0, dy: 0 }, armL: { a: 0, dy: 0 } },
  { body: { a: 0, dy: 0 }, head: { a: 0, dy: 1 }, armK: { a: 0, dy: 1 }, armL: { a: 0, dy: 0 } },
];
fs.mkdirSync("anim", { recursive: true });
poses.forEach((pose, i) => { const b = new P.Buf(96, 64); rig.draw(b, 0, 0, pose); P.save(P.smoothEdge(b), `anim/idle_${i}.png`); });
console.log("ok");
