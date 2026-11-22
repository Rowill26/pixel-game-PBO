// Tebasan Keris Pusaka (Pendekar's jurus): a crescent slash that sweeps under the target from right to
// left (the party stands on the right), then sparks and fades. 96x96, drawn over the target.
const fs = require("fs");
const P = require(require("path").join(__dirname, "../lib/pixel.js"));

// palette (palette2.py): dark edge -> role colour (Merah/Api) -> bright core (Api/Putih)
const RAMP = ["642127", "8c2f2e", "b0473a", "c9773c", "e8b25e", "f6e3a8", "f4efe2"].map((h) => P.rgb(...[0, 2, 4].map((i) => parseInt(h.slice(i, i + 2), 16))));
const W = 96, H = 96, N = 12;
const C = { x: 54, y: 30 }, R = 36;
const A0 = -15 * Math.PI / 180, A1 = 170 * Math.PI / 180;    // right -> through the bottom -> left
const rnd = P.rng(7);
const sparks = [];

function frame(i) {
  const b = new P.Buf(W, H);
  const t = i / (N - 1);
  const head = P.clamp(0.08 + t / 0.45);                 // the blade tip reaches the end of the arc at half time
  const fade = t < 0.5 ? 1 : Math.pow(1 - (t - 0.5) / 0.5, 1.3);  // then the whole arc fades out
  const trail = 0.6;
  for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
    const dx = x - C.x, dy = y - C.y;
    const r = Math.hypot(dx, dy);
    let a = Math.atan2(dy, dx); if (a < A0 - 0.5) a += Math.PI * 2;
    const s = (a - A0) / (A1 - A0);
    if (s < 0 || s > head) continue;
    const behind = head - s;                       // 0 at the tip, grows along the trail
    if (behind > trail) continue;
    const k = 1 - behind / trail;                  // 1 at the tip
    const tip = Math.min(1, behind / 0.07);         // the blade tip tapers to a point
    const w = (1.5 + 6.5 * Math.sin(Math.PI * Math.min(1, s * 1.15)) * (0.35 + 0.65 * k)) * (0.25 + 0.75 * tip);
    const n = P.noise(x * 0.18 + i * 0.7, y * 0.18) * 0.5 + 0.5;
    const d = Math.abs(r - R) / w;
    let v = (1 - d) * (0.45 + 0.55 * k) * fade + (n - 0.5) * 0.35;
    if (d > 1 || v < 0.12 + (1 - fade) * 0.25) continue;   // fading arc thins out instead of leaving blotches
    b.pset(x, y, RAMP[P.band(Math.min(1, v * 1.1), RAMP.length, x, y)]);
  }
  // impact glint where the arc passes under the target (a 4-point star, two frames)
  const hitT = (0.5 - 0.08) * 0.45;
  if (t >= hitT && t < hitT + 2 / (N - 1)) {
    const gx = C.x + Math.cos(Math.PI / 2) * R, gy = C.y + R;
    const big = t < hitT + 1 / (N - 1) ? 7 : 4;
    for (let d = -big; d <= big; d++) {
      const c = Math.abs(d) < 2 ? RAMP[6] : Math.abs(d) < big * 0.6 ? RAMP[5] : RAMP[4];
      b.pset(gx + d, gy, c); b.pset(gx, gy + d, c);
    }
    b.pset(gx + 1, gy + 1, RAMP[5]); b.pset(gx - 1, gy - 1, RAMP[5]); b.pset(gx + 1, gy - 1, RAMP[5]); b.pset(gx - 1, gy + 1, RAMP[5]);
  }
  // sparks thrown off the blade tip while it moves
  if (t <= 0.5) {
    const a = A0 + head * (A1 - A0);
    const tx = C.x + Math.cos(a) * R, ty = C.y + Math.sin(a) * R;
    for (let k = 0; k < 3; k++) sparks.push({ x: tx, y: ty, vx: (rnd() - 0.5) * 4 - Math.sin(a) * 1.5, vy: (rnd() - 0.8) * 3, life: 3 + rnd() * 3 });
  }
  for (const s of sparks) {
    if (s.life <= 0) continue;
    b.pset(Math.round(s.x), Math.round(s.y), s.life > 2.5 ? RAMP[6] : s.life > 1.2 ? RAMP[5] : RAMP[3]);
    s.x += s.vx; s.y += s.vy; s.vy += 0.5; s.life -= 1;
  }
  return b;
}

const frames = Array.from({ length: N }, (_, i) => frame(i));
fs.mkdirSync("tebasan", { recursive: true });
frames.forEach((f, i) => P.save(f, `tebasan/tebasan_${i}.png`));
const onBg = (f) => { const b = new P.Buf(W, H); b.rect(0, 0, W, H, P.col([30, 27, 39])); b.sprite(f, 0, 0); return b; };
fs.writeFileSync("tebasan/tebasan.gif", P.gif(frames.map(onBg), { delay: 50, scale: 4, loop: 0 }));
P.save(P.sheet(frames.map((f, i) => ({ buf: onBg(f), label: `${i}`, scale: 3 })), { cols: 6, gap: 8 }), "tebasan/preview.png");
console.log("ok");
