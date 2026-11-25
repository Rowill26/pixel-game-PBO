const fs = require("fs");
const P = require(require("path").join(__dirname, "../lib/pixel.js"));
const spr = P.load("idle-pix.png");
const rig = require("./rig.js")(spr);
// Breathing: body sinks 1 px, head follows one frame later, keris arm lags a little (weight)
const poses = [
  { body: { a: 0, dy: 0 }, head: { a: 0, dy: 0 }, armL: { a: 0, dy: 0 }, armR: { a: 0, dy: 0 } },
  { body: { a: 0, dy: 1 }, head: { a: 0, dy: -1 }, armL: { a: 0, dy: -1 }, armR: { a: 0, dy: 0 } },
  { body: { a: 0, dy: 1 }, head: { a: 0, dy: 0 }, armL: { a: 0, dy: 0 }, armR: { a: 0, dy: 0 } },
  { body: { a: 0, dy: 0 }, head: { a: 0, dy: 1 }, armL: { a: 0, dy: 1 }, armR: { a: 0, dy: 0 } },
];
const frames = poses.map((pose) => { const b = new P.Buf(64, 64); rig.draw(b, 0, 0, pose); return b; });
const diff = (a, b) => { let n = 0; for (let i = 0; i < a.px.length; i++) if (a.px[i] !== b.px[i]) n++; return n; };
console.log("changed px per step:", frames.map((f, i) => diff(f, frames[(i + 1) % frames.length])).join(" "),
  "| edges:", frames.map((f) => { const e = P.edgeCheck(f); return e.nubs.length + e.strips.length + e.holes.length; }).join(" "));
fs.mkdirSync("frames", { recursive: true });
frames.forEach((f, i) => P.save(f, `frames/idle_${i}.png`));
fs.writeFileSync("idle.gif", P.gif(frames, { delay: 200, scale: 4, loop: 0 }));
P.save(P.sheet([
  ...frames.map((f, i) => ({ buf: P.crop(f, 10, 12, 44, 52), label: "FRAME " + i, scale: 6 })),
  { buf: P.crop(frames[3], 10, 12, 44, 52), label: "AKHIR", scale: 6 },
  { buf: P.crop(frames[0], 10, 12, 44, 52), label: "AWAL", scale: 6 },
], { cols: 4, gap: 14 }), "preview.png");
