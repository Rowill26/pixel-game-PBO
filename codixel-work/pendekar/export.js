// Export the approved idle frames into the 2.5D prototype (one PNG per frame, 64x64)
const P = require(require("path").join(__dirname, "../lib/pixel.js"));
const OUT = "../../prototype-2.5d/assets/sprites/";
for (let i = 0; i < 4; i++) {
  const f = P.smoothEdge(P.load(`frames/idle_${i}.png`));
  const e = P.edgeCheck(f);
  P.save(f, `${OUT}pendekar_idle_${i}.png`);
  console.log(`pendekar_idle_${i}.png`, f.w + "x" + f.h, "edges", e.nubs.length + e.strips.length + e.holes.length);
}
