// Peramu (AI pixel version) animations. Frames 96x64, feet centred at x=48, row 60, facing left.
const fs = require("fs");
const P = require(require("path").join(__dirname, "../../lib/pixel.js"));
const K = Object.fromEntries(["idle", "ancang", "pukul", "jurus", "terluka", "gugur"].map((n) => [n, P.load(`${n}.png`)]));
const shift = (b, dx, dy) => { const o = new P.Buf(b.w, b.h); o.sprite(b, dx, dy); return o; };
const bounds = (b) => { let t = 999, bo = 0; for (let y = 0; y < b.h; y++) for (let x = 0; x < b.w; x++) if (b.get(x, y) >>> 24) { t = Math.min(t, y); bo = Math.max(bo, y); } return [t, bo]; };

// breathing without bones: the body (not the staff column) sinks 1 px, the head follows a frame later.
function breathe(src, bodyDy, headDy, x0, x1) {
  const [top, bot] = bounds(src), h = bot - top;
  const neck = top + Math.round(h * 0.27), hip = top + Math.round(h * 0.6);
  const out = new P.Buf(src.w, src.h); out.px.set(src.px);
  const layer = (y0, y1, dy) => {
    if (!dy) return;
    for (let y = y0; y < y1; y++) for (let x = x0; x < x1; x++) { const c = src.get(x, y); if (c >>> 24) out.pset(x, y + dy, c); }
  };
  layer(neck, hip, bodyDy);
  layer(top, neck, headDy);
  return out;
}
// no staff: the whole width breathes
const B = (b, h) => breathe(K.idle, b, h, 0, 96);
const ANIMS = {
  idle:    { delay: 200, frames: [K.idle, B(1, 0), B(1, 1), B(0, 1)] },
  serang:  { delay: 90,  frames: [K.ancang, K.pukul, shift(K.pukul, -1, 0), K.ancang] },
  jurus:   { delay: 100, frames: [K.idle, K.jurus, shift(K.jurus, 0, -1), shift(K.jurus, 0, -1), K.jurus, K.idle] },
  terluka: { delay: 100, frames: [shift(K.terluka, 2, 0), shift(K.terluka, 1, 0)] },
  gugur:   { delay: 140, frames: [K.terluka, shift(K.gugur, 0, -2), shift(K.gugur, 0, -1), K.gugur] },
};
fs.mkdirSync("anim", { recursive: true });
const items = [];
for (const [name, a] of Object.entries(ANIMS)) {
  a.frames.forEach((f, i) => { P.save(f, `anim/${name}_${i}.png`); items.push({ buf: P.crop(f, 14, 4, 68, 58), label: `${name.toUpperCase()} ${i}`, scale: 4 }); });
  fs.writeFileSync(`anim/${name}.gif`, P.gif(a.frames.map((f) => { const b = new P.Buf(96, 64); b.rect(0, 0, 96, 64, P.col([30, 27, 39])); b.sprite(f, 0, 0); return b; }), { delay: a.delay, scale: 4, loop: 0 }));
}
P.save(P.sheet(items, { cols: 6, gap: 10 }), "anim/preview.png");
console.log("ok");
