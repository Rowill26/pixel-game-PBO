// Pixelate the action poses at exactly the same scale as the idle sprite, mirror them to face left,
// and anchor them by the feet so the character does not jump between animations.
const fs = require("fs");
const P = require(require("path").join(__dirname, "../../lib/pixel.js"));
const ALL = fs.readFileSync("../../docs/art/palette-girah.hex", "utf8").trim().split(/\s+/)
  .map((h) => [parseInt(h.slice(0, 2), 16), parseInt(h.slice(2, 4), 16), parseInt(h.slice(4, 6), 16)]);
const use = [0,1,2,3, 13,14,15,16,17, 29,30,31,32,33, 37,38,39, 40,41,42,43,44, 45,46,47,48, 49,50,51,52];
const PAL = use.map((i) => ALL[i]);
const C = (i) => P.col(ALL[i]);
const S = 46 / 2051;                                   // same scale as the idle sprite
const EYES = {                                         // read off the reference faces (ref pixels)
  ancang: [[992, 527], [1172, 532]], tebas: [[992, 527], [1172, 532]], jurus: [[992, 527], [1172, 532]],
  terluka: [[992, 540], [1172, 540]], gugur: [[995, 548], [1175, 548]],
};
const CW = 96, CH = 64;

function feetCenter(b) {                               // centre of the lowest 3 opaque rows
  let bottom = -1;
  for (let y = b.h - 1; y >= 0 && bottom < 0; y--) for (let x = 0; x < b.w; x++) if (b.get(x, y) >>> 24) { bottom = y; break; }
  let sx = 0, n = 0;
  for (let y = bottom - 2; y <= bottom; y++) for (let x = 0; x < b.w; x++) if (b.get(x, y) >>> 24) { sx += x; n++; }
  return { x: sx / n, bottom };
}
const idle = P.load("idle-pix.png");
const idleFeet = feetCenter(idle);
const idleOffset = idleFeet.x - idle.w / 2;            // keep the same feet offset from the canvas centre
console.log("idle feet", idleFeet, "offset", idleOffset.toFixed(1));

const out = {};
for (const n of Object.keys(EYES)) {
  const src = P.load(`poses/q-${n}.png`);          // already mapped to the palette (material rules)
  const w = Math.round(src.w * S), h = Math.round(src.h * S);
  const q = src;
  let small = P.smoothEdge(P.cleanup(P.resize(q, w, h, { mode: true }), { tol: 20 }));
  for (const [ex, ey] of EYES[n]) { const x = Math.floor(ex * S), y = Math.floor(ey * S); small.pset(x, y, C(0)); }
  const mir = new P.Buf(w, h);                         // face left
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) { const c = small.get(x, y); if (c >>> 24) mir.pset(w - 1 - x, y, c); }
  const f = feetCenter(mir);
  const canvas = new P.Buf(CW, CH);
  canvas.sprite(mir, Math.round(CW / 2 + idleOffset - f.x), (CH - 4) - f.bottom);
  P.save(canvas, `poses/pix-${n}.png`);
  const e = P.edgeCheck(canvas);
  console.log(n, "scaled", w + "x" + h, "edges", e.nubs.length + e.strips.length + e.holes.length);
  out[n] = canvas;
}
const idle96 = new P.Buf(CW, CH); idle96.sprite(idle, (CW - 64) / 2, 0);
P.save(P.sheet([
  { buf: idle96, label: "DIAM", scale: 5 },
  ...Object.entries(out).map(([n, b]) => ({ buf: b, label: n.toUpperCase(), scale: 5 })),
], { cols: 3, gap: 14 }), "poses/preview.png");
