const P = require(require("path").join(__dirname, "../lib/pixel.js"));
const ref = P.load("ref-idle.png");
const cut = P.trim(P.removeBg(ref, { tol: 40, erode: 1 }), 4);
P.save(cut, "idle-cut.png");
const b = P.bounds(cut);
console.log("cut size", cut.w, cut.h, "bounds", JSON.stringify(b));
P.save(P.sheet([{ buf: ref, label: "REFERENSI" }, { buf: cut, label: "LATAR DIHAPUS" }], { fit: 600, gap: 24 }), "preview.png");
