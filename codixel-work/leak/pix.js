const fs = require("fs");
const P = require(require("path").join(__dirname, "../lib/pixel.js"));
const HEX = fs.readFileSync("../../docs/art/palette-girah.hex", "utf8").trim().split(/\s+/);
const C = (i) => P.col([0, 2, 4].map((k) => parseInt(HEX[i].slice(k, k + 2), 16)));
const src = P.load("q-idle.png");
const b = P.bounds(src);
const S = 50 / b.h;                                   // whole Leak (hair to mist tip) about 50 px tall
const w = Math.round(src.w * S), h = Math.round(src.h * S);
const small = P.smoothEdge(P.cleanup(P.resize(src, w, h, { mode: true }), { tol: 20 }));
let sx = 0, n = 0, bottom = 0;
for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) if (small.get(x, y) >>> 24) { sx += x; n++; bottom = Math.max(bottom, y); }
const ox = Math.round(48 - sx / n), oy = 60 - bottom;
const canvas = new P.Buf(96, 64);
canvas.sprite(small, ox, oy);
// tidy: (1) light specks on the outer edge (hair gaps) become transparent,
//       (2) skin/cloth tones take the majority of their 3x3 neighbourhood (removes speckle)
const PALE = [15, 16, 17, 37, 38].map(C), LIGHT = [17, 37, 38].map(C);
{
  const src = new P.Buf(96, 64); src.px.set(canvas.px);
  for (let y = 1; y < 63; y++) for (let x = 1; x < 95; x++) {
    const v = src.get(x, y);
    if (!(v >>> 24)) continue;
    const n4 = [src.get(x - 1, y), src.get(x + 1, y), src.get(x, y - 1), src.get(x, y + 1)];
    if (LIGHT.includes(v) && n4.filter((u) => !(u >>> 24)).length >= 2) { canvas.pset(x, y, 0); continue; }
    if (!PALE.includes(v)) continue;
    const votes = new Map();
    for (let dy = -1; dy <= 1; dy++) for (let dx = -1; dx <= 1; dx++) { const u = src.get(x + dx, y + dy); if (PALE.includes(u)) votes.set(u, (votes.get(u) || 0) + 1); }
    let best = 0, col = v; for (const [k, n] of votes) if (n > best) (best = n), (col = k);
    canvas.pset(x, y, col);
  }
}
const eyes = JSON.parse(fs.readFileSync("eyes.json", "utf8")).eyes.map(([x, y]) => [ox + Math.floor(x * S), oy + Math.floor(y * S)]);
// glowing eyes: white-hot core, violet glow on the outer side, dark socket above
const [eL, eR] = eyes[0][0] < eyes[1][0] ? eyes : [eyes[1], eyes[0]];
for (const [[x, y], side] of [[eL, -1], [eR, 1]]) {
  canvas.pset(x, y, C(39)); canvas.pset(x + side, y, C(56)); canvas.pset(x, y - 1, C(0)); canvas.pset(x + side, y - 1, C(0));
}
P.save(canvas, "pix-idle.png");
fs.writeFileSync("eyes-sprite.json", JSON.stringify(eyes));
const cc = P.colorCount(canvas);
console.log("scale", S.toFixed(4), "size", w + "x" + h, "eyes", JSON.stringify(eyes), "colours", cc.length, JSON.stringify(P.edgeCheck(canvas)));
const pend = P.load("../pendekar/anim/idle_0.png");
P.save(P.sheet([
  { buf: P.load("nobg-idle.png"), label: "REFERENSI" },
  { buf: P.crop(canvas, 18, 4, 60, 58), label: "LEAK PIXEL", scale: 6 },
  { buf: P.crop(pend, 18, 4, 60, 58), label: "PENDEKAR (UKURAN)", scale: 6 },
  { buf: canvas, label: "2X", scale: 2 },
], { fit: 348, gap: 16 }), "preview.png");
