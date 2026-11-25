// All battle effects except Tebasan Keris Pusaka (tebasan.js), in the same style:
// bright core -> role colour -> dark edge, flat bands with dither only where two bands meet,
// colours from the game palette (prototype-2.5d/tools/palette2.py). Transparent frames.
// Run: node effects.js [name ...]   (no name = all). Output: <name>/<name>_<i>.png, .gif, preview.png
const fs = require("fs");
const P = require(require("path").join(__dirname, "../lib/pixel.js"));

const rgbOf = (h) => [0, 2, 4].map((i) => parseInt(h.slice(i, i + 2), 16));
const ramp = (list) => list.map((h) => P.rgb(...rgbOf(h)));
const withAlpha = (c, a) => (((Math.round(a * 255) & 0xff) << 24) | (c & 0xffffff)) >>> 0;
const put = (b, x, y, v, R, strip) => { if (v > 0) b.pset(x, y, R[P.band(Math.min(1, v), R.length, x, y, strip)]); };
// see-through band colour: v in 0..1 -> one of the ramp colours with its own alpha
const putA = (b, x, y, v, R, A) => {
  if (v <= 0) return;
  const k = P.band(Math.min(1, v), R.length, x, y);
  b.pset(x, y, withAlpha(R[k], A[k]));
};
const smooth = (e0, e1, x) => { const t = P.clamp((x - e0) / (e1 - e0)); return t * t * (3 - 2 * t); };

// ramps (dark edge -> bright core)
const R = {
  putih: ramp(["474c50", "878d87", "c9c1ad", "e3dccb", "f4efe2"]),
  emas: ramp(["5e4a24", "8a6d33", "b8954a", "dcc07a", "f6e3a8", "f4efe2"]),
  hijau: ramp(["28382b", "3a4c34", "52653f", "74ad7e", "919a62", "f6e3a8"]),
  biru: ramp(["232b3d", "34405a", "4e5f78", "75879a", "a3b2b8", "f4efe2"]),
  merah: ramp(["3f171b", "642127", "8c2f2e", "b0473a", "e8b25e", "f4efe2"]),
  girah: ramp(["33294a", "4f416c", "73639a", "9f92c2"]),
};

// ---------- shared shapes ----------
// a straight slash from (x0,y0) to (x1,y1): the tip travels along it, the trail thins and fades
function lineSlash(b, seg, t, R, maxW) {
  const { x0, y0, x1, y1, start, dur } = seg;
  const lt = (t - start) / dur;                       // 0..1 while drawing, >1 while fading
  if (lt <= 0) return;
  const head = Math.min(1, lt / 0.55), fade = lt < 0.55 ? 1 : Math.max(0, 1 - (lt - 0.55) / 0.45);
  if (fade <= 0) return;
  const L = Math.hypot(x1 - x0, y1 - y0), ux = (x1 - x0) / L, uy = (y1 - y0) / L;
  const xa = Math.floor(Math.min(x0, x1) - maxW), xb = Math.ceil(Math.max(x0, x1) + maxW);
  const ya = Math.floor(Math.min(y0, y1) - maxW), yb = Math.ceil(Math.max(y0, y1) + maxW);
  for (let y = ya; y <= yb; y++) for (let x = xa; x <= xb; x++) {
    const px = x - x0, py = y - y0, s = (px * ux + py * uy) / L, d = Math.abs(-px * uy + py * ux);
    if (s < 0 || s > head) continue;
    const behind = head - s, k = 1 - Math.min(1, behind / 0.7);
    const w = maxW * Math.sin(Math.PI * s) * (0.3 + 0.7 * k) * (0.3 + 0.7 * Math.min(1, behind / 0.06)) + 0.6;
    const v = (1 - d / w) * (0.4 + 0.6 * k) * fade;
    if (d <= w && v > 0.1 + (1 - fade) * 0.25) put(b, x, y, v, R);
  }
}

// small particles that fly and fade (sparks, motes, drops)
function particles(list, b, R) {
  for (const p of list) {
    if (p.age >= 0 && p.age < p.life) {
      const k = p.age / p.life;
      const c = R[Math.max(0, Math.min(R.length - 1, Math.round((1 - k) * (R.length - 1))))];
      b.pset(Math.round(p.x), Math.round(p.y), p.alpha ? withAlpha(c, p.alpha * (1 - k * 0.6)) : c);
      if (p.cross && k < 0.5) { b.pset(Math.round(p.x) + 1, Math.round(p.y), c); b.pset(Math.round(p.x) - 1, Math.round(p.y), c); b.pset(Math.round(p.x), Math.round(p.y) - 1, c); b.pset(Math.round(p.x), Math.round(p.y) + 1, c); }
    }
    if (p.age >= 0) { p.x += p.vx; p.y += p.vy; p.vy += p.g || 0; p.vx *= p.drag || 1; }
    p.age++;
  }
}

// ellipse ring: value along an ellipse outline of thickness th
function ellRing(x, y, cx, cy, rx, ry, th) {
  if (rx <= 0 || ry <= 0) return 0;
  const d = Math.hypot((x - cx) / rx, (y - cy) / ry);
  return 1 - Math.abs(d - 1) * Math.min(rx, ry * 2.5) / th;
}

// ---------- the effects ----------
const FX = {};

// 1. ordinary hit for every pengelana: a quick white slash from upper right to lower left
FX.sabetan = { W: 48, H: 48, N: 7, delay: 45, make() {
  const out = [];
  for (let i = 0; i < this.N; i++) {
    const b = new P.Buf(this.W, this.H), t = i / (this.N - 1);
    lineSlash(b, { x0: 42, y0: 6, x1: 8, y1: 42, start: 0, dur: 1 }, t, R.putih, 4.5);
    out.push(b);
  }
  return out;
} };

// 2. Langkah Bayangan (Pengintai): two dark-blue slashes that cross, then a few shadow motes
FX.langkah_bayangan = { W: 64, H: 64, N: 10, delay: 50, make() {
  const out = [], motes = [], rnd = P.rng(5);
  for (let i = 0; i < 12; i++) motes.push({ x: 32 + (rnd() - 0.5) * 20, y: 32 + (rnd() - 0.5) * 20, vx: (rnd() - 0.5) * 1.2, vy: -0.4 - rnd() * 0.6, age: -4 - Math.floor(rnd() * 3), life: 5, alpha: 0.8 });
  for (let i = 0; i < this.N; i++) {
    const b = new P.Buf(this.W, this.H), t = i / (this.N - 1);
    lineSlash(b, { x0: 56, y0: 8, x1: 8, y1: 56, start: 0, dur: 0.6 }, t, R.biru, 4);
    lineSlash(b, { x0: 56, y0: 56, x1: 8, y1: 8, start: 0.2, dur: 0.6 }, t, R.biru, 4);
    particles(motes, b, R.biru.slice(0, 4));
    out.push(b);
  }
  return out;
} };

// 3. Cakaran (Calon Arang ritual form): three red claw streaks from upper left to lower right
FX.cakar = { W: 64, H: 64, N: 8, delay: 45, make() {
  const out = [];
  for (let i = 0; i < this.N; i++) {
    const b = new P.Buf(this.W, this.H), t = i / (this.N - 1);
    for (let k = 0; k < 3; k++) {
      const o = (k - 1) * 9;
      lineSlash(b, { x0: 10 + o, y0: 8 - o * 0.4, x1: 50 + o, y1: 54 - o * 0.4, start: k * 0.08, dur: 0.75 }, t, R.merah, 3);
    }
    out.push(b);
  }
  return out;
} };

// 4. Mantra Lingkar Suci (Juru Mantra): a white-gold ring spreads on the ground under the enemies,
//    runes flare on it and thin pillars of light rise, then everything fades
FX.lingkar_suci = { W: 200, H: 96, N: 14, delay: 70, make() {
  const out = [], cx = 100, cy = 70, RX = 88, RY = 22, motes = [], rnd = P.rng(11);
  for (let i = 0; i < 26; i++) {
    const a = rnd() * Math.PI * 2;
    motes.push({ x: cx + Math.cos(a) * RX * (0.3 + rnd() * 0.7), y: cy + Math.sin(a) * RY * (0.3 + rnd() * 0.7), vx: 0, vy: -0.8 - rnd() * 1.2, age: -3 - Math.floor(rnd() * 6), life: 6 + Math.floor(rnd() * 4), cross: rnd() < 0.3 });
  }
  const runes = 12;
  for (let i = 0; i < this.N; i++) {
    const b = new P.Buf(this.W, this.H), t = i / (this.N - 1);
    const grow = P.ease.outCubic ? P.ease.outCubic(P.clamp(t / 0.3)) : 1 - Math.pow(1 - P.clamp(t / 0.3), 3);
    const fade = t < 0.7 ? 1 : Math.max(0, 1 - (t - 0.7) / 0.3);
    for (let y = 0; y < this.H; y++) for (let x = 0; x < this.W; x++) {
      let v = ellRing(x, y, cx, cy, RX * grow, RY * grow, 2.2) * fade;
      v = Math.max(v, ellRing(x, y, cx, cy, RX * grow * 0.78, RY * grow * 0.78, 1.2) * 0.55 * fade * smooth(0.15, 0.35, t));
      if (v > 0.15) put(b, x, y, v, R.emas);
    }
    // runes: short bright marks on the outer ring, lighting up one after another
    for (let r = 0; r < runes; r++) {
      const a = (r / runes) * Math.PI * 2, on = smooth(0.25 + r * 0.02, 0.35 + r * 0.02, t) * fade;
      if (on <= 0.1) continue;
      const rx = Math.round(cx + Math.cos(a) * RX * 0.89), ry = Math.round(cy + Math.sin(a) * RY * 0.89);
      for (let d = -1; d <= 1; d++) put(b, rx + d, ry, on, R.emas);
      put(b, rx, ry - 1, on, R.emas);
    }
    // pillars of light rising from the ring
    if (t > 0.3) {
      const pt = P.clamp((t - 0.3) / 0.4);
      for (let r = 0; r < 8; r++) {
        const a = (r / 8) * Math.PI * 2 + 0.2, px = Math.round(cx + Math.cos(a) * RX), base = Math.round(cy + Math.sin(a) * RY);
        const h = Math.round(46 * Math.sin(Math.PI * pt) * (0.7 + 0.3 * ((r * 7) % 3) / 2));
        for (let y = 0; y < h; y++) {
          const v = (1 - y / h) * fade;
          put(b, px, base - y, v, R.emas);
          if (y < h * 0.5) put(b, px + 1, base - y, v * 0.6, R.emas);
        }
      }
    }
    particles(motes, b, R.emas.slice(2));
    out.push(b);
  }
  return out;
} };

// 5. Ramuan Penawar (Peramu): green drops fall onto the ally, then healing sparkles rise
FX.ramuan_penawar = { W: 48, H: 64, N: 12, delay: 70, make() {
  const out = [], rnd = P.rng(3), drops = [], rise = [];
  [[18, 0], [25, 1], [31, 2]].forEach(([x, d]) => drops.push({ x, y: 2, vx: 0, vy: 2.2, g: 0.9, age: -d, life: 4 }));
  for (let i = 0; i < 18; i++) rise.push({ x: 10 + rnd() * 28, y: 58 - rnd() * 6, vx: (rnd() - 0.5) * 0.4, vy: -1.2 - rnd() * 1.4, age: -4 - Math.floor(rnd() * 5), life: 6 + Math.floor(rnd() * 3), cross: rnd() < 0.35 });
  for (let i = 0; i < this.N; i++) {
    const b = new P.Buf(this.W, this.H), t = i / (this.N - 1);
    // soft green glow on the ground once the drops land
    const g = smooth(0.25, 0.4, t) * (1 - smooth(0.7, 1, t));
    for (let y = 0; y < this.H; y++) for (let x = 0; x < this.W; x++) {
      const v = ellRing(x, y, 24, 58, 17, 4.5, 1.6) * g;
      if (v > 0.15) put(b, x, y, v * 0.85, R.hijau);
    }
    // drops: 2 px tall, bright core
    for (const p of drops) {
      if (p.age >= 0 && p.age < p.life) { put(b, p.x, Math.round(p.y), 1, R.hijau); put(b, p.x, Math.round(p.y) - 1, 0.7, R.hijau); put(b, p.x, Math.round(p.y) - 2, 0.4, R.hijau); }
      if (p.age >= 0) { p.y += p.vy; p.vy += p.g; }
      p.age++;
    }
    particles(rise, b, R.hijau.slice(2));
    out.push(b);
  }
  return out;
} };

// 6. Doa Ratna: soft white motes rise around the party with a faint glow
FX.doa_ratna = { W: 48, H: 64, N: 12, delay: 80, make() {
  const out = [], rnd = P.rng(21), motes = [];
  for (let i = 0; i < 24; i++) motes.push({ x: 6 + rnd() * 36, y: 62 - rnd() * 14, vx: (rnd() - 0.5) * 0.3, vy: -1.2 - rnd() * 1.2, age: -Math.floor(rnd() * 7), life: 6 + Math.floor(rnd() * 4), cross: rnd() < 0.25 });
  for (let i = 0; i < this.N; i++) {
    const b = new P.Buf(this.W, this.H);
    // a short fading trail under every mote, so they read as rising light, not dust
    for (const p of motes) if (p.age >= 0 && p.age < p.life) for (let k = 1; k <= 3; k++) {
      const v = (1 - p.age / p.life) * (1 - k / 4);
      if (v > 0.15) b.pset(Math.round(p.x), Math.round(p.y) + k, withAlpha(R.putih[2 + Math.min(2, Math.round(v * 2))], 0.35 + v * 0.4));
    }
    particles(motes, b, R.putih.slice(1));
    out.push(b);
  }
  return out;
} };

// 7. Lingkar Perlindungan (Mpu Bharadah): a golden dome rises over the party, shimmers, then fades
FX.lingkar_perlindungan = { W: 176, H: 112, N: 16, delay: 70, make() {
  const out = [], cx = 88, cy = 100, RX = 80, RY = 86, rnd = P.rng(13), motes = [];
  for (let i = 0; i < 20; i++) motes.push({ x: cx + (rnd() - 0.5) * 140, y: cy - rnd() * 10, vx: 0, vy: -1 - rnd() * 1.5, age: -4 - Math.floor(rnd() * 8), life: 7, cross: rnd() < 0.3 });
  for (let i = 0; i < this.N; i++) {
    const b = new P.Buf(this.W, this.H), t = i / (this.N - 1);
    const up = 1 - Math.pow(1 - P.clamp(t / 0.35), 3), fade = t < 0.75 ? 1 : Math.max(0, 1 - (t - 0.75) / 0.25);
    const ry = RY * up;
    for (let y = 0; y < this.H; y++) for (let x = 0; x < this.W; x++) {
      // ground ring (full ellipse) + dome outline (upper half only)
      let v = ellRing(x, y, cx, cy, RX, 12, 1.6) * fade;
      if (y <= cy) {
        const d = Math.hypot((x - cx) / RX, (y - cy) / Math.max(1, ry));
        v = Math.max(v, (1 - Math.abs(d - 1) * 40) * fade);
        // see-through inside with slow shimmering bands
        if (d < 1 && ry > 4 && v < 0.2) {
          // see-through shell, brighter only in a thin band just inside the rim
          const inside = Math.pow(d, 14) * 0.85 * fade * (0.8 + 0.2 * Math.sin(t * 12 + x * 0.1));
          if (inside > 0.2) putA(b, x, y, inside, R.emas.slice(2, 5), [0.18, 0.3, 0.45]);
        }
      }
      if (v > 0.2) put(b, x, y, v, R.emas);
    }
    particles(motes, b, R.emas.slice(2));
    out.push(b);
  }
  return out;
} };

// 8. Asap Wabah / Sebar Wabah (Pengikut, Calon Arang): a see-through purple plague cloud rolls to the
//    right (toward the party), grows and thins out
FX.asap_wabah = { W: 176, H: 80, N: 16, delay: 70, make() {
  const out = [], A = [0.22, 0.38, 0.55, 0.72];
  for (let i = 0; i < this.N; i++) {
    const b = new P.Buf(this.W, this.H), t = i / (this.N - 1);
    const front = 20 + t * 130, rad = 14 + t * 18, fade = t < 0.6 ? 1 : Math.max(0, 1 - (t - 0.6) / 0.4);
    for (let y = 0; y < this.H; y++) for (let x = 0; x < this.W; x++) {
      // body: a row of overlapping puffs behind the front, low to the ground
      let dens = 0;
      for (let k = 0; k < 4; k++) {
        const px = front - k * rad * 0.9, py = 52 - k * 2, rr = rad * (1 - k * 0.15);
        dens = Math.max(dens, 1 - Math.hypot((x - px) / (rr * 1.3), (y - py) / rr));
      }
      if (dens <= 0) continue;
      const n = P.fbm(x * 0.09 - t * 3, y * 0.12, 3, 4);
      const v = Math.pow(dens, 0.6) * (0.55 + (n - 0.5) * 1.1) * fade;
      if (v > 0.12) putA(b, x, y, Math.min(0.85, v), R.girah, A);
    }
    out.push(b);
  }
  return out;
} };

// 9. Jerat Kabut (Pemimpin Pengikut): three see-through fog tendrils spiral up around the target
FX.jerat_kabut = { W: 64, H: 80, N: 14, delay: 70, make() {
  const out = [], A = [0.3, 0.5, 0.7, 0.85];
  for (let i = 0; i < this.N; i++) {
    const b = new P.Buf(this.W, this.H), t = i / (this.N - 1);
    const grow = smooth(0, 0.5, t), fade = t < 0.75 ? 1 : Math.max(0, 1 - (t - 0.75) / 0.25);
    for (let k = 0; k < 3; k++) {
      const ph = k * 2.1 + t * (3 + k), top = [1, 0.7, 0.85][k];   // tendrils end at different heights
      for (let s = 0; s <= grow * top; s += 0.004) {
        const y = 76 - s * 60, rad = 16 * (1 - s * 0.3) * (0.7 + 0.6 * P.noise(s * 5 + k * 3, t * 2)), a = s * Math.PI * (1.6 + k * 0.5) + ph;
        const x = 32 + Math.cos(a) * rad, front = Math.sin(a) > 0;     // in front of the target or behind it
        const w = 3.4 * (1 - s * 0.55) * (0.5 + 0.5 * Math.min(1, (grow - s) / 0.12));
        for (let dy = -2; dy <= 2; dy++) for (let dx = -3; dx <= 3; dx++) {
          const d = Math.hypot(dx, dy) / Math.max(0.6, w);
          if (d > 1) continue;
          const v = (1 - d) * fade * (front ? 1 : 0.55);
          const px = Math.round(x + dx), py = Math.round(y + dy);
          const old = b.get(px, py) >>> 24;
          if (old && old > A[P.band(v, 4, px, py)] * 255) continue;
          putA(b, px, py, v, R.girah, A);
        }
      }
    }
    // fog pooling around the feet
    for (let y = 64; y < this.H; y++) for (let x = 0; x < this.W; x++) {
      const dens = 1 - Math.hypot((x - 32) / 26, (y - 76) / 7);
      if (dens <= 0) continue;
      const v = dens * (0.6 + (P.fbm(x * 0.15 + t * 2, y * 0.2, 2, 8) - 0.5)) * grow * fade;
      if (v > 0.12 && !(b.get(x, y) >>> 24)) putA(b, x, y, v, R.girah, A);
    }
    out.push(b);
  }
  return out;
} };

// 10. Kutukan (Calon Arang ritual form): a red rune circle burns into the ground under the target,
//     then red flames erupt from it and die down
FX.kutukan = { W: 96, H: 112, N: 16, delay: 70, make() {
  const out = [], cx = 48, cy = 98, RX = 34, RY = 9;
  const cols = ["3f171b", "642127", "8c2f2e", "b0473a", "c9773c", "e8b25e", "f6e3a8"].map(rgbOf);
  const flames = [-16, 0, 16].map((dx, k) => ({ dx, f: new P.Flame({ cols, height: 70 - Math.abs(dx) * 1.2, width: 11, seed: 3 + k, bits: 0.3 }) }));
  for (let i = 0; i < this.N; i++) {
    const b = new P.Buf(this.W, this.H), t = i / (this.N - 1);
    const draw = smooth(0, 0.3, t), fade = t < 0.75 ? 1 : Math.max(0, 1 - (t - 0.75) / 0.25);
    // rune circle: outer + inner ring drawn around clockwise, 8 radial marks
    for (let y = 0; y < this.H; y++) for (let x = 0; x < this.W; x++) {
      let a = Math.atan2((y - cy) / RY, (x - cx) / RX) + Math.PI;
      if (a / (Math.PI * 2) > draw) continue;
      let v = Math.max(ellRing(x, y, cx, cy, RX, RY, 1.4), ellRing(x, y, cx, cy, RX * 0.72, RY * 0.72, 1.1) * 0.8);
      for (let r = 0; r < 8; r++) {
        const ra = (r / 8) * Math.PI * 2, mx = cx + Math.cos(ra) * RX * 0.86, my = cy + Math.sin(ra) * RY * 0.86;
        if (Math.abs(x - mx) < 1 && Math.abs(y - my) < 2.2) v = Math.max(v, 0.9);
      }
      v *= fade * (0.75 + 0.25 * Math.sin(t * 20));
      if (v > 0.2) put(b, x, y, v, R.merah);
    }
    // flames rise once the circle is closed
    const up = smooth(0.3, 0.45, t) * (1 - smooth(0.7, 0.95, t));
    if (up > 0.05) {
      const tmp = new P.Buf(this.W, this.H);
      for (const { dx, f } of flames) f.draw(tmp, cx + dx, cy, i * 0.12);
      // squash the flames by "up" so they grow from the ground and sink again
      for (let y = 0; y < this.H; y++) for (let x = 0; x < this.W; x++) {
        const sy = Math.round(cy - (cy - y) / up);
        if (sy < 0 || sy >= this.H) continue;
        const c = tmp.get(x, sy);
        if (c >>> 24) b.pset(x, y, c);
      }
    }
    out.push(b);
  }
  return out;
} };

// ---------- output ----------
const onBg = (f) => { const b = new P.Buf(f.w, f.h); b.rect(0, 0, f.w, f.h, P.col([30, 27, 39])); b.sprite(f, 0, 0); return b; };
const want = process.argv.slice(2);
for (const [name, fx] of Object.entries(FX)) {
  if (want.length && !want.includes(name)) continue;
  let frames = fx.make();
  const empty = (f) => { for (let i = 0; i < f.px.length; i++) if (f.px[i] >>> 24) return false; return true; };
  while (frames.length > 1 && empty(frames[0])) frames.shift();
  while (frames.length > 1 && empty(frames[frames.length - 1])) frames.pop();
  for (const old of fs.existsSync(name) ? fs.readdirSync(name) : []) if (/_\d+\.png$/.test(old)) fs.unlinkSync(`${name}/${old}`);
  fs.mkdirSync(name, { recursive: true });
  frames.forEach((f, i) => P.save(f, `${name}/${name}_${i}.png`));
  fs.writeFileSync(`${name}/${name}.gif`, P.gif(frames.map(onBg), { delay: fx.delay, scale: fx.W > 100 ? 3 : 4, loop: 0 }));
  P.save(P.sheet(frames.map((f, i) => ({ buf: onBg(f), label: `${i}`, scale: fx.W > 100 ? 2 : 3 })), { cols: fx.W > 100 ? 4 : 6, gap: 8 }), `${name}/preview.png`);
  console.log(name, frames.length, "frames");
}
