// Pengintai (AI pixel version) animations. Frames 96x64, feet centred at x=48, row 60, facing left.
const fs = require("fs");
const P = require(require("path").join(__dirname, "../../lib/pixel.js"));
const K = Object.fromEntries(["idle", "ancang", "tusuk", "jurus", "terluka", "gugur"].map((n) => [n, P.load(`${n}.png`)]));
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

// Langkah Bayangan: see-through dark-blue copies of the dash pose trail behind him (to the right,
// since he dashes left toward the enemies). Pixel colour 0xAABBGGRR as in pixel.js.
function trail(src, offsets) {
  const out = new P.Buf(src.w, src.h);
  offsets.forEach(([dx, alpha]) => {
    const col = (alpha << 24) | (0x5a << 16) | (0x40 << 8) | 0x34;   // Langit 2 (#34405a)
    for (let y = 0; y < src.h; y++) for (let x = 0; x < src.w; x++) {
      if ((src.get(x, y) >>> 24) && x + dx < src.w && !(out.get(x + dx, y) >>> 24)) out.pset(x + dx, y, col);
    }
  });
  for (let y = 0; y < src.h; y++) for (let x = 0; x < src.w; x++) { const c = src.get(x, y); if (c >>> 24) out.pset(x, y, c); }
  return out;
}

const ANIMS = {
  idle:    { delay: 200, frames: [K.idle, B(1, 0), B(1, 1), B(0, 1)] },
  serang:  { delay: 90,  frames: [K.ancang, K.tusuk, shift(K.tusuk, -1, 0), K.ancang] },
  jurus:   { delay: 100, frames: [K.ancang, trail(K.jurus, [[14, 70], [7, 130]]), trail(K.jurus, [[7, 110]]), K.jurus, shift(K.jurus, -1, 0), K.ancang] },
  terluka: { delay: 100, frames: [shift(K.terluka, 2, 0), shift(K.terluka, 1, 0)] },
  gugur:   { delay: 140, frames: [K.terluka, shift(K.gugur, 0, -2), shift(K.gugur, 0, -1), K.gugur] },
};
fs.mkdirSync("anim", { recursive: true });
const items = [];
for (const [name, a] of Object.entries(ANIMS)) {
  a.frames.forEach((f, i) => { P.save(f, `anim/${name}_${i}.png`); items.push({ buf: P.crop(f, 10, 4, 76, 58), label: `${name.toUpperCase()} ${i}`, scale: 4 }); });
  fs.writeFileSync(`anim/${name}.gif`, P.gif(a.frames.map((f) => { const b = new P.Buf(96, 64); b.rect(0, 0, 96, 64, P.col([30, 27, 39])); b.sprite(f, 0, 0); return b; }), { delay: a.delay, scale: 4, loop: 0 }));
}
P.save(P.sheet(items, { cols: 6, gap: 10 }), "anim/preview.png");
console.log("ok");
