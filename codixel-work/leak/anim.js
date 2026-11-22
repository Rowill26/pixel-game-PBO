// Leak animations from the normalised key poses (96x64, head-aligned). Enemy faces right: forward = +x.
const fs = require("fs");
const P = require(require("path").join(__dirname, "../lib/pixel.js"));
const K = Object.fromEntries(["idle", "serang", "jurus", "terluka", "gugur"].map((n) => [n, P.load(`poses/${n}.png`)]));
const shift = (b, dx, dy) => { const o = new P.Buf(b.w, b.h); o.sprite(b, dx, dy); return o; };
// fade into smoke: remove pixels in an ordered-dither pattern, more from the bottom up
function dissolve(b, amount, seed) {
  const o = new P.Buf(b.w, b.h); o.px.set(b.px);
  const r = P.rng(seed);
  let top = b.h, bot = 0;
  for (let y = 0; y < b.h; y++) for (let x = 0; x < b.w; x++) if (b.get(x, y) >>> 24) { top = Math.min(top, y); bot = Math.max(bot, y); }
  for (let y = top; y <= bot; y++) for (let x = 0; x < b.w; x++) {
    if (!(b.get(x, y) >>> 24)) continue;
    const fromBottom = (bot - y) / Math.max(1, bot - top);           // 0 at the bottom, 1 at the top
    if (r() < amount * (1.4 - fromBottom)) o.pset(x, y, 0);
  }
  return o;
}
const ANIMS = {
  serang:  { delay: 90,  frames: [K.idle, K.serang, shift(K.serang, 1, 0), K.idle] },
  jurus:   { delay: 100, frames: [K.idle, K.jurus, shift(K.jurus, 0, -1), K.jurus, K.serang, K.idle] },
  terluka: { delay: 100, frames: [shift(K.terluka, -2, 0), shift(K.terluka, -1, 0)] },
  gugur:   { delay: 140, frames: [K.terluka, K.gugur, dissolve(K.gugur, 0.35, 3), dissolve(K.gugur, 0.7, 5)] },
};
fs.mkdirSync("anim", { recursive: true });
const items = [];
for (const [name, a] of Object.entries(ANIMS)) {
  a.frames.forEach((f, i) => { P.save(f, `anim/${name}_${i}.png`); items.push({ buf: P.crop(f, 14, 4, 68, 58), label: `${name.toUpperCase()} ${i}`, scale: 4 }); });
  fs.writeFileSync(`anim/${name}.gif`, P.gif(a.frames.map((f) => { const b = new P.Buf(96, 64); b.rect(0, 0, 96, 64, P.col([30, 27, 39])); b.sprite(f, 0, 0); return b; }), { delay: a.delay, scale: 4, loop: 0 }));
}
P.save(P.sheet([{ buf: P.crop(K.idle, 14, 4, 68, 58), label: "DIAM", scale: 4 }, ...items], { cols: 6, gap: 10 }), "anim/preview.png");
console.log("ok");
