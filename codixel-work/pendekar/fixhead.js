// Turn only the head to face left (towards the enemies): mirror the head pixels in place.
const P = require(require("path").join(__dirname, "../lib/pixel.js"));
const src = P.load("idle-pix-sebelum.png");
const X0 = 25, X1 = 47, Y0 = 14, Y1 = 28;            // head box = the "head" bone paint area
let minX = 99, maxX = -1;
for (let y = Y0; y <= Y1; y++) for (let x = X0; x <= X1; x++) if (src.get(x, y) >>> 24) { minX = Math.min(minX, x); maxX = Math.max(maxX, x); }
const out = new P.Buf(64, 64); out.px.set(src.px);
for (let y = Y0; y <= Y1; y++) for (let x = X0; x <= X1; x++) out.pset(x, y, 0);
for (let y = Y0; y <= Y1; y++) for (let x = minX; x <= maxX; x++) {
  const c = src.get(x, y);
  if (c >>> 24) out.pset(minX + maxX - x, y, c);
}
// keep whatever of the neck/shoulders sat inside the box but outside the head span
for (let y = Y0; y <= Y1; y++) for (let x = X0; x <= X1; x++) if ((x < minX || x > maxX) && (src.get(x, y) >>> 24)) out.pset(x, y, src.get(x, y));
const clean = P.smoothEdge(out);
console.log("head span", minX, maxX, "edges", JSON.stringify(P.edgeCheck(clean)));
P.save(clean, "idle-pix.png");
P.save(P.sheet([{ buf: P.crop(src, 10, 12, 44, 52), label: "SEBELUM", scale: 6 }, { buf: P.crop(clean, 10, 12, 44, 52), label: "SESUDAH", scale: 6 },
  { buf: clean, label: "1X", scale: 1 }, { buf: clean, label: "2X", scale: 2 }], { gap: 16 }), "preview.png");
