const P = require(require("path").join(__dirname, "../lib/pixel.js"));
const ref = P.load("ref-idle.png");
// flood from the edges only (the off-white cloth is close to the background, but it is closed by
// black outlines, so a flood fill cannot leak into it); tol kept low for the same reason
const nb = P.removeBg(ref, { tol: 22, erode: 1 });
P.save(nb, "nobg-idle.png");
console.log(JSON.stringify(P.bounds(nb)));
P.save(P.sheet([{ buf: ref, label: "REFERENSI" }, { buf: nb, label: "LATAR DIHAPUS" }], { fit: 420, gap: 16 }), "preview-cut.png");
