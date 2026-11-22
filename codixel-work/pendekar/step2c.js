const fs = require("fs");
const P = require(require("path").join(__dirname, "../lib/pixel.js"));
const PAL = fs.readFileSync("../../docs/art/palette-girah.hex", "utf8").trim().split(/\s+/)
  .map((h) => [parseInt(h.slice(0, 2), 16), parseInt(h.slice(2, 4), 16), parseInt(h.slice(4, 6), 16)]);
const ref = P.load("ref-game2.png");
const cut = P.trim(P.removeBg(ref, { tol: 40, erode: 1 }), 4);
P.save(cut, "game2-cut.png");
function onCanvas(spr) { const c = new P.Buf(64, 64); c.sprite(spr, Math.round((64 - spr.w) / 2), 64 - 3 - spr.h); return c; }
const H = 46, W = Math.round((cut.w * H) / cut.h);
const avg = P.smoothEdge(P.cleanup(P.quantize(P.resize(cut, W, H), 64, { palette: PAL, dither: true }).buf, { tol: 20 }));
const q = P.quantize(cut, 64, { palette: PAL }).buf;
const mode = P.smoothEdge(P.cleanup(P.resize(q, W, H, { mode: true }), { tol: 20 }));
const A = onCanvas(avg), M = onCanvas(mode);
P.save(A, "game2-A.png"); P.save(M, "game2-M.png");
console.log("size", W + "x" + H, "A", JSON.stringify(P.edgeCheck(A)), "M", JSON.stringify(P.edgeCheck(M)));
const oldB = P.load("idle-B.png");
P.save(P.sheet([
  { buf: cut, label: "REFERENSI BARU" },
  { buf: oldB, label: "REFERENSI LAMA 44PX", scale: 6 },
  { buf: A, label: "BARU: RATA-RATA", scale: 6 },
  { buf: M, label: "BARU: WARNA DOMINAN", scale: 6 },
  { buf: A, label: "1X", scale: 1 }, { buf: M, label: "1X", scale: 1 },
], { fit: 384, gap: 20 }), "compare.png");
