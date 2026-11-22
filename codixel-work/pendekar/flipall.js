// Mirror the whole original sprite so head AND body face left (towards the enemies).
const P = require(require("path").join(__dirname, "../lib/pixel.js"));
const src = P.load("idle-pix-sebelum.png");
const out = new P.Buf(64, 64);
for (let y = 0; y < 64; y++) for (let x = 0; x < 64; x++) { const c = src.get(x, y); if (c >>> 24) out.pset(63 - x, y, c); }
P.save(out, "idle-pix.png");
const prev = P.load("idle-pix-kepala-dibalik.png");
P.save(P.sheet([
  { buf: P.crop(src, 10, 12, 44, 52), label: "ASLI (KANAN)", scale: 6 },
  { buf: P.crop(prev, 10, 12, 44, 52), label: "KEPALA SAJA", scale: 6 },
  { buf: P.crop(out, 10, 12, 44, 52), label: "SEMUA DIBALIK", scale: 6 },
  { buf: out, label: "2X", scale: 2 }], { gap: 16 }), "preview-balik.png");
console.log("edges", JSON.stringify(P.edgeCheck(out)));
