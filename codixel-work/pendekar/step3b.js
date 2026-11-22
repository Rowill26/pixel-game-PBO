const P = require(require("path").join(__dirname, "../lib/pixel.js"));
const spr = P.load("idle-pix.png");
const rig = require("./rig.js")(spr);
const bones = P.ruler(spr, 8, { step: 4 });
rig.drawBones(bones, 0, 0, {}, { scale: 8 });
const own = P.ruler(rig.ownerMap(), 8, { step: 4 });
rig.drawBones(own, 0, 0, {}, { scale: 8 });
P.save(P.sheet([{ buf: bones, label: "TULANG" }, { buf: own, label: "PEMBAGIAN BAGIAN" }, { buf: rig.legend(), label: "WARNA" }], { gap: 16 }), "preview.png");
