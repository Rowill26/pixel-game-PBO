// Build the action animations from the approved v2 key poses (frames are 96x64, feet at x=44, y=60).
const fs = require("fs");
const P = require(require("path").join(__dirname, "../../lib/pixel.js"));
const pose = (n) => P.load(`poses/v2-${n}.png`);
const shift = (b, dx, dy) => { const o = new P.Buf(b.w, b.h); o.sprite(b, dx, dy); return o; };
const K = { ancang: pose("ancang"), tebas: pose("tebas"), jurus: pose("jurus"), terluka: pose("terluka"), gugur: pose("gugur") };
const ANIMS = {
  // forward = towards the enemies = negative x
  serang:  { delay: 90,  frames: [K.ancang, K.tebas, shift(K.tebas, -1, 0), K.ancang] },
  jurus:   { delay: 100, frames: [K.ancang, K.jurus, shift(K.jurus, 0, -1), K.jurus, K.tebas, K.ancang] },
  terluka: { delay: 100, frames: [shift(K.terluka, 2, 0), shift(K.terluka, 1, 0)] },
  gugur:   { delay: 140, frames: [K.terluka, shift(K.gugur, 0, -2), shift(K.gugur, 0, -1), K.gugur] },
};
fs.mkdirSync("anim", { recursive: true });
const rows = [];
for (const [name, a] of Object.entries(ANIMS)) {
  a.frames.forEach((f, i) => P.save(f, `anim/${name}_${i}.png`));
  fs.writeFileSync(`anim/${name}.gif`, P.gif(a.frames.map((f) => { const b = new P.Buf(96, 64); b.rect(0, 0, 96, 64, P.col([30, 27, 39])); b.sprite(f, 0, 0); return b; }),
    { delay: a.delay, scale: 4, loop: 0 }));
  a.frames.forEach((f, i) => rows.push({ buf: P.crop(f, 18, 4, 56, 58), label: `${name.toUpperCase()} ${i}`, scale: 4 }));
  console.log(name, a.frames.length, "frames @", a.delay, "ms");
}
const idle = [0, 1, 2, 3].map((i) => P.load(`anim/idle_${i}.png`));
fs.writeFileSync("anim/idle.gif", P.gif(idle.map((f) => { const b = new P.Buf(96, 64); b.rect(0, 0, 96, 64, P.col([30, 27, 39])); b.sprite(f, 0, 0); return b; }), { delay: 200, scale: 4, loop: 0 }));
P.save(P.sheet([...idle.map((f, i) => ({ buf: P.crop(f, 18, 4, 56, 58), label: `DIAM ${i}`, scale: 4 })), ...rows], { cols: 6, gap: 10 }), "anim/preview.png");
