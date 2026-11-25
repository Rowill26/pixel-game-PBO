const fs = require("fs");
const P = require(require("path").join(__dirname, "../lib/pixel.js"));
const ALL = fs.readFileSync("../../docs/art/palette-girah.hex", "utf8").trim().split(/\s+/)
  .map((h) => [parseInt(h.slice(0, 2), 16), parseInt(h.slice(2, 4), 16), parseInt(h.slice(4, 6), 16)]);
// only the ramps this character is made of: shadow, stone (keris), earth (handle), white, skin, red, gold
const use = [0,1,2,3, 13,14,15,16,17, 29,30,31,32,33, 37,38,39, 40,41,42,43,44, 45,46,47,48, 49,50,51,52];
const PAL = use.map((i) => ALL[i]);
const C = (i) => P.col(ALL[i]);

const ref = P.load("ref-game2.png");
const noBg = P.removeBg(ref, { tol: 40, erode: 1 });
const b = P.bounds(noBg), offX = b.x - 4, offY = b.y - 4;      // P.trim(…, 4) origin in ref coords
const cut = P.load("game2-cut-keris.png");
const H = 46, W = Math.round((cut.w * H) / cut.h), s = H / cut.h;
const q = P.quantize(cut, PAL.length, { palette: PAL }).buf;
const spr = P.smoothEdge(P.cleanup(P.resize(q, W, H, { mode: true }), { tol: 20 }));

const canvas = new P.Buf(64, 64);
const cx = Math.round((64 - spr.w) / 2), cy = 64 - 3 - spr.h;
canvas.sprite(spr, cx, cy);
const at = (rx, ry) => [cx + Math.floor((rx - offX) * s), cy + Math.floor((ry - offY) * s)];
// eyes and brows sampled from the reference face (ref pixels), stamped as single dark pixels
const eyeL = at(1015, 532), eyeR = at(1185, 532), mouth = at(1092, 698);
console.log("eyes", eyeL, eyeR, "mouth", mouth, "offset", offX, offY, "sprite", W + "x" + H);
for (const [x, y] of [eyeL, eyeR]) { canvas.pset(x, y, C(0)); canvas.pset(x, y - 1, C(1)); }
canvas.pset(mouth[0], mouth[1], C(41));
P.save(canvas, "idle-pix.png");

const prev = P.load("game2-M.png");
const cc = P.colorCount(canvas);
console.log("colours", cc.length, "top", cc.slice(0, 5).map((c) => (c.share * 100).toFixed(0) + "%").join(" "), JSON.stringify(P.edgeCheck(canvas)));
P.save(P.sheet([
  { buf: P.load("game2-cut.png"), label: "REFERENSI" },
  { buf: prev, label: "UJI CEPAT SEBELUMNYA", scale: 6 },
  { buf: canvas, label: "TAHAP 2 DIRAPIKAN", scale: 6 },
  { buf: canvas, label: "1X", scale: 1 },
  { buf: canvas, label: "2X (DI GAME)", scale: 2 },
], { fit: 384, gap: 20 }), "preview.png");
