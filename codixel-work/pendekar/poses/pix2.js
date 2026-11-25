// Shrink the material-coloured poses at one shared scale, mirror to face left, align by the feet,
// then redraw each face by hand (eyes, brows, mouth) so the expression reads at sprite size.
const fs = require("fs");
const P = require(require("path").join(__dirname, "../../lib/pixel.js"));
const HEX = fs.readFileSync("../../docs/art/palette-girah.hex", "utf8").trim().split(/\s+/);
const C = (i) => P.col([0, 2, 4].map((k) => parseInt(HEX[i].slice(k, k + 2), 16)));
const S = 46 / 2051, CW = 96, CH = 64, FEET_X = 44, FEET_Y = 60;
const INK = C(0), SKIN = C(42), SKIN_D = C(41), MOUTH = C(45), TEETH = C(38);
// face landmarks read off the references (reference pixels) + expression per pose
const FACE = {
  idle:    { eyes: [[1020, 540], [1180, 540]], mouth: [1080, 700], expr: "tegas" },
  ancang:  { eyes: [[992, 527], [1172, 532]], mouth: [1080, 695], expr: "tegas" },
  tebas:   { eyes: [[992, 527], [1172, 532]], mouth: [1080, 695], expr: "garang" },
  jurus:   { eyes: [[992, 527], [1172, 532]], mouth: [1080, 695], expr: "teriak" },
  terluka: { eyes: [[992, 540], [1172, 540]], mouth: [1080, 700], expr: "sakit" },
  gugur:   { eyes: [[995, 548], [1175, 548]], mouth: [1085, 700], expr: "pingsan" },
};

function feet(b) {
  let bottom = -1;
  for (let y = b.h - 1; y >= 0 && bottom < 0; y--) for (let x = 0; x < b.w; x++) if (b.get(x, y) >>> 24) { bottom = y; break; }
  let sx = 0, n = 0;
  for (let y = bottom - 2; y <= bottom; y++) for (let x = 0; x < b.w; x++) if (b.get(x, y) >>> 24) { sx += x; n++; }
  return { x: sx / n, bottom };
}

// Black cartoon lines inside the body turn into dark specks at sprite size. Keep the outline and the
// hair mass (ink touching transparency or other ink); turn lone inner ink into the darkest tone of
// the material around it.
const DARKEST = new Map([[41, 41], [42, 41], [43, 41], [45, 45], [46, 45], [47, 45], [50, 50], [51, 50], [52, 50], [14, 14], [16, 14], [17, 14]]
  .map(([k, v]) => [C(k), C(v)]));
function cleanInk(c) {
  const D2 = C(1), isInk = (v) => v === INK || v === D2;
  const src = new P.Buf(c.w, c.h); src.px.set(c.px);
  for (let y = 1; y < c.h - 1; y++) for (let x = 1; x < c.w - 1; x++) {
    if (!isInk(src.get(x, y))) continue;
    const n4 = [src.get(x - 1, y), src.get(x + 1, y), src.get(x, y - 1), src.get(x, y + 1)];
    if (n4.some((v) => !(v >>> 24))) continue;                 // silhouette outline
    if (n4.filter(isInk).length >= 2) continue;                // hair, thick outline
    const votes = new Map();
    for (const v of n4) if (DARKEST.has(v)) votes.set(DARKEST.get(v), (votes.get(DARKEST.get(v)) || 0) + 1);
    let best = 0, col = 0; for (const [k, v] of votes) if (v > best) (best = v), (col = k);
    if (col) c.pset(x, y, col);
  }
}

const SKIN_TONES = [C(41), C(42), C(43)];
function smoothSkin(c) {
  const src = new P.Buf(c.w, c.h); src.px.set(c.px);
  for (let y = 1; y < c.h - 1; y++) for (let x = 1; x < c.w - 1; x++) {
    const v = src.get(x, y); if (!SKIN_TONES.includes(v)) continue;
    const votes = new Map();
    for (let dy = -1; dy <= 1; dy++) for (let dx = -1; dx <= 1; dx++) { const u = src.get(x + dx, y + dy); if (SKIN_TONES.includes(u)) votes.set(u, (votes.get(u) || 0) + 1); }
    let best = 0, col = v; for (const [k, n] of votes) if (n > best) (best = n), (col = k);
    c.pset(x, y, col);
  }
}

function drawFace(c, eyes, mouth, expr) {
  const [l, r] = eyes[0][0] < eyes[1][0] ? eyes : [eyes[1], eyes[0]];
  const y = Math.min(l[1], r[1]);
  // wipe old eye/mouth specks inside the face (only dark ink pixels), keep hair and outline
  for (let yy = y - 1; yy <= mouth[1] + 1; yy++)
    for (let xx = l[0] - 1; xx <= r[0] + 1; xx++) if (c.get(xx, yy) === INK) c.pset(xx, yy, SKIN);
  const brow = (e, outerDx, innerUp) => {             // outerDx: -1 for the left eye, +1 for the right eye
    const ex = e[0];
    c.pset(ex + outerDx, y - 2 - (innerUp ? 0 : 1), INK);
    c.pset(ex, y - 2, INK);
    c.pset(ex - outerDx, y - 2 - (innerUp ? 1 : 0), INK);
  };
  const [mx, my] = mouth;
  if (expr === "sakit") {                              // squeezed eyes, brows up in the middle, gritted teeth
    c.pset(l[0] - 1, y, INK); c.pset(l[0], y + 1, INK); c.pset(l[0] - 1, y + 1, INK);     // >  squeezed
    c.pset(r[0] + 1, y, INK); c.pset(r[0], y + 1, INK); c.pset(r[0] + 1, y + 1, INK);     // <
    c.pset(mx - 1, my, INK); c.pset(mx, my, TEETH); c.pset(mx + 1, my, TEETH); c.pset(mx + 2, my, INK);
    return;
  }
  if (expr === "pingsan") {                            // closed eyes, flat sad brows, small frown
    for (const e of [l, r]) { c.pset(e[0] - 1, y + 1, INK); c.pset(e[0], y + 1, INK); }
    c.pset(mx - 1, my + 1, SKIN_D); c.pset(mx, my, SKIN_D); c.pset(mx + 1, my + 1, SKIN_D);
    return;
  }
  for (const e of [l, r]) { c.pset(e[0], y, INK); c.pset(e[0], y + 1, INK); }   // open eyes (1x2)
  c.pset(l[0] + 1, y - 1, INK); c.pset(r[0] - 1, y - 1, INK);                   // frown: inner corners pulled down
  if (expr === "tegas") { for (let x = mx - 1; x <= mx + 1; x++) c.pset(x, my, SKIN_D); }
  if (expr === "garang") { c.pset(mx, my, MOUTH); c.pset(mx + 1, my, MOUTH); c.pset(mx - 1, my, SKIN_D); }
  if (expr === "teriak") { for (let x = mx - 1; x <= mx + 1; x++) { c.pset(x, my, MOUTH); c.pset(x, my + 1, MOUTH); } c.pset(mx, my, TEETH); }
}

const result = {};
for (const [n, f] of Object.entries(FACE)) {
  const src = P.load(`poses/q-${n}.png`);
  const w = Math.round(src.w * S), h = Math.round(src.h * S);
  const small = P.smoothEdge(P.cleanup(P.resize(src, w, h, { mode: true }), { tol: 20 }));
  const mir = new P.Buf(w, h);
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) { const c = small.get(x, y); if (c >>> 24) mir.pset(w - 1 - x, y, c); }
  const ft = feet(mir);
  const ox = Math.round(FEET_X - ft.x), oy = FEET_Y - ft.bottom;
  const canvas = new P.Buf(CW, CH);
  canvas.sprite(mir, ox, oy);
  const map = ([rx, ry]) => [ox + (w - 1 - Math.floor(rx * S)), oy + Math.floor(ry * S)];
  cleanInk(canvas);
  smoothSkin(canvas);
  drawFace(canvas, f.eyes.map(map), map(f.mouth), f.expr);
  P.save(canvas, `poses/v2-${n}.png`);
  result[n] = canvas;
  const cc = P.colorCount(canvas);
  console.log(n, "colours", cc.length, "edges", JSON.stringify(P.edgeCheck(canvas)).length > 40 ? JSON.stringify(P.edgeCheck(canvas)) : "ok");
}
const crop = (b) => P.crop(b, 22, 4, 52, 58);
P.save(P.sheet(Object.entries(result).map(([n, b]) => ({ buf: crop(b), label: n.toUpperCase(), scale: 6 })), { cols: 6, gap: 12 }), "poses/preview-v2.png");
const face = (b) => P.crop(b, 28, 6, 26, 24);
P.save(P.sheet(Object.entries(result).map(([n, b]) => ({ buf: face(b), label: n.toUpperCase(), scale: 12 })), { cols: 6, gap: 12 }), "poses/preview-wajah.png");
const old = ["ancang", "tebas", "jurus", "terluka", "gugur"].map((n) => ({ buf: crop(P.load(`poses/pix-${n}.png`)), label: "LAMA " + n.toUpperCase(), scale: 2 }));
const neu = ["ancang", "tebas", "jurus", "terluka", "gugur"].map((n) => ({ buf: crop(result[n]), label: "BARU " + n.toUpperCase(), scale: 2 }));
P.save(P.sheet([...old, ...neu], { cols: 5, gap: 10 }), "poses/preview-2x.png");
