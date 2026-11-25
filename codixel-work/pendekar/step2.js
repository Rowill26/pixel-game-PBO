const fs = require("fs");
const P = require(require("path").join(__dirname, "../lib/pixel.js"));
const PAL = fs.readFileSync("../../docs/art/palette-girah.hex", "utf8").trim().split(/\s+/)
  .map((h) => [parseInt(h.slice(0, 2), 16), parseInt(h.slice(2, 4), 16), parseInt(h.slice(4, 6), 16)]);
const cut = P.load("idle-cut.png");
const H = 44, W = Math.round((cut.w * H) / cut.h);

// A: shrink first, then map to the palette with dither strips
const a0 = P.resize(cut, W, H);
const A = P.smoothEdge(P.cleanup(P.quantize(a0, 64, { palette: PAL, dither: true }).buf, { tol: 20 }));
// B: map to the palette at full size, then shrink keeping the most common colour per cell
const bq = P.quantize(cut, 64, { palette: PAL }).buf;
const B = P.smoothEdge(P.cleanup(P.resize(bq, W, H, { mode: true }), { tol: 20 }));

function onCanvas(spr) {               // 64x64, centred, feet 3 px above the bottom edge
  const c = new P.Buf(64, 64);
  c.sprite(spr, Math.round((64 - spr.w) / 2), 64 - 3 - spr.h);
  return c;
}
const ca = onCanvas(A), cb = onCanvas(B);
P.save(ca, "idle-A.png"); P.save(cb, "idle-B.png");
for (const [n, b] of [["A", A], ["B", B]]) {
  const cc = P.colorCount(b);
  console.log(n, "size", b.w + "x" + b.h, "colours", cc.length, "top", cc.slice(0, 4).map(c => (c.share * 100).toFixed(0) + "%").join(" "), "edges", JSON.stringify(P.edgeCheck(b)).slice(0, 120));
}
const old = P.load("../../assets/sprites/pendekar.png");
P.save(P.sheet([
  { buf: cut, label: "REFERENSI" },
  { buf: ca, label: "A: KECILKAN LALU PALET", scale: 6 },
  { buf: cb, label: "B: PALET LALU KECILKAN", scale: 6 },
  { buf: old, label: "SPRITE SEMENTARA LAMA", scale: 6 },
], { fit: 384, gap: 20 }), "compare.png");
