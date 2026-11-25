const fs = require("fs");
const P = require(require("path").join(__dirname, "../lib/pixel.js"));
const HEX = fs.readFileSync("../../docs/art/palette-girah.hex", "utf8").trim().split(/\s+/);
const RGB = (i) => [0, 2, 4].map((k) => parseInt(HEX[i].slice(k, k + 2), 16));
const PAL = [0, 1, 2, 3, 15, 16, 17, 37, 38, 39, 53, 54, 55, 56].map(RGB);   // hair, pale skin, cloth, mist, glow
const nat = P.trim(P.removeBg(P.load("ai-native.png"), { tol: 18 }), 0);
const natQ = P.quantize(nat, PAL.length, { palette: PAL }).buf;
const H = 50, W = Math.round((natQ.w * H) / natQ.h);
const scaled = P.smoothEdge(P.resize(natQ, W, H, { mode: true }));
const place = (b) => { const c = new P.Buf(96, 64); c.sprite(b, Math.round((96 - b.w) / 2), 61 - b.h); return c; };
console.log("AI native character", nat.w + "x" + nat.h, "-> normalised", W + "x" + H);
P.save(place(scaled), "ai-norm.png");
P.save(P.sheet([
  { buf: P.load("ai-native.png"), label: "AI ASLI (GRID 90)", scale: 4 },
  { buf: natQ, label: "AI: PALET, UKURAN ASLI", scale: 5 },
  { buf: P.crop(place(scaled), 18, 4, 60, 58), label: "AI: DISAMAKAN 50PX", scale: 6 },
  { buf: P.crop(P.load("pix-idle.png"), 18, 4, 60, 58), label: "CODIXEL DARI ILUSTRASI", scale: 6 },
], { gap: 16 }), "compare-ai.png");
