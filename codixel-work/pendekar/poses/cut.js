const P = require(require("path").join(__dirname, "../../lib/pixel.js"));
for (const n of ["idle", "ancang", "tebas", "jurus", "terluka", "gugur"]) {
  const ref = P.load(`poses/ref-${n}.png`);
  const nb = P.removeBg(ref, { tol: 40, erode: 1, flood: false });
  P.save(nb, `poses/nobg-${n}.png`);           // keep full canvas so all poses share one coordinate frame
  const b = P.bounds(nb);
  console.log(n, JSON.stringify(b));
}
