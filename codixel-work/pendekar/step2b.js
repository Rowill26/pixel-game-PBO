const fs = require("fs");
const P = require(require("path").join(__dirname, "../lib/pixel.js"));
const PAL = fs.readFileSync("../../docs/art/palette-girah.hex", "utf8").trim().split(/\s+/)
  .map((h) => [parseInt(h.slice(0, 2), 16), parseInt(h.slice(2, 4), 16), parseInt(h.slice(4, 6), 16)]);
function pix(src, H) {
  const W = Math.round((src.w * H) / src.h);
  const q = P.quantize(src, 64, { palette: PAL }).buf;
  return P.smoothEdge(P.cleanup(P.resize(q, W, H, { mode: true }), { tol: 20 }));
}
function onCanvas(spr) {
  const c = new P.Buf(64, 64);
  c.sprite(spr, Math.round((64 - spr.w) / 2), 64 - 3 - spr.h);
  return c;
}
const cut = P.load("idle-cut.png"), chunky = P.load("idle-chunky.png");
const C = onCanvas(pix(cut, 56)), D = onCanvas(pix(chunky, 56));
P.save(C, "idle-C.png"); P.save(D, "idle-D.png");
const prev = P.load("idle-B.png");
for (const [n, b] of [["C", C], ["D", D]]) console.log(n, JSON.stringify(P.edgeCheck(b)), P.colorCount(b).length, "colours");
P.save(P.sheet([
  { buf: cut, label: "REFERENSI" },
  { buf: prev, label: "B: 44 PX (SEBELUMNYA)", scale: 6 },
  { buf: C, label: "C: 56 PX", scale: 6 },
  { buf: D, label: "D: 56 PX KEPALA+KERIS", scale: 6 },
  { buf: D, label: "D UKURAN ASLI", scale: 1 },
], { fit: 384, gap: 20 }), "compare.png");
