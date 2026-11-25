// Calon Arang, RITUAL form (AI pixel version) animations. Frames 128x96, feet centred at x=64, row 92, facing RIGHT (enemy, not mirrored).
const fs = require("fs");
const P = require(require("path").join(__dirname, "../../lib/pixel.js"));
const K = Object.fromEntries(["idle", "ancang", "cakar", "jurus", "terluka", "gugur"].map((n) => [n, P.load(`${n}.png`)]));
const shift = (b, dx, dy) => { const o = new P.Buf(b.w, b.h); o.sprite(b, dx, dy); return o; };
const bounds = (b) => { let t = 999, bo = 0; for (let y = 0; y < b.h; y++) for (let x = 0; x < b.w; x++) if (b.get(x, y) >>> 24) { t = Math.min(t, y); bo = Math.max(bo, y); } return [t, bo]; };

function breathe(src, bodyDy, headDy) {
  const [top, bot] = bounds(src), h = bot - top;
  const neck = top + Math.round(h * 0.27), hip = top + Math.round(h * 0.6);
  const out = new P.Buf(src.w, src.h); out.px.set(src.px);
  const layer = (y0, y1, dy) => {
    if (!dy) return;
    for (let y = y0; y < y1; y++) for (let x = 0; x < src.w; x++) { const c = src.get(x, y); if (c >>> 24) out.pset(x, y + dy, c); }
  };
  layer(neck, hip, bodyDy);
  layer(top, neck, headDy);
  return out;
}
const B = (b, h) => breathe(K.idle, b, h);

const ANIMS = {
  idle:    { delay: 200, frames: [K.idle, B(1, 0), B(1, 1), B(0, 1)] },
  serang:  { delay: 100, frames: [K.ancang, K.cakar, shift(K.cakar, 2, 0), K.ancang] },
  // Kutukan: arms raised to the sky; the curse effect is drawn by the game
  jurus:   { delay: 110, frames: [K.ancang, K.jurus, shift(K.jurus, 0, -1), shift(K.jurus, 0, -2), shift(K.jurus, 0, -1), K.idle] },
  terluka: { delay: 110, frames: [shift(K.terluka, -3, 0), shift(K.terluka, -1, 0)] },
  gugur:   { delay: 180, frames: [K.terluka, shift(K.gugur, 0, -3), shift(K.gugur, 0, -1), K.gugur] },
};
fs.mkdirSync("anim", { recursive: true });
const items = [];
for (const [name, a] of Object.entries(ANIMS)) {
  a.frames.forEach((f, i) => { P.save(f, `anim/${name}_${i}.png`); items.push({ buf: P.crop(f, 8, 4, 112, 92), label: `${name.toUpperCase()} ${i}`, scale: 4 }); });
  fs.writeFileSync(`anim/${name}.gif`, P.gif(a.frames.map((f) => { const b = new P.Buf(128, 96); b.rect(0, 0, 128, 96, P.col([30, 27, 39])); b.sprite(f, 0, 0); return b; }), { delay: a.delay, scale: 4, loop: 0 }));
}
P.save(P.sheet(items, { cols: 6, gap: 10 }), "anim/preview.png");
console.log("ok");
