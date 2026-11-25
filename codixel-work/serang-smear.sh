#!/usr/bin/env bash
# Animasi serang semua tokoh dengan smear frame + waktu per frame (skill game-feel).
# Jalankan dari folder prototype-2.5d:   bash ../codixel-work/serang-smear.sh [--install ../assets/sprites]
# Pendekar memakai skrip sendiri: codixel-work/pendekar/ai/smear.py
set -e
T="python -I tools/smear_attack.py"
W=../codixel-work
I="$@"

# pengelana (menghadap kiri, menyerang ke kiri)
$T $W/juru-mantra/ai juru_mantra --ancang ancang.png --strike ayun.png --kind arc \
   --pivot 52,42 --from 68,21 --to 29,65 --sweep -1 --colors wood --follow -1 --start 0.3 $I
$T $W/peramu/ai peramu --ancang ancang.png --strike pukul.png --kind arc \
   --pivot 47,31 --from 54,16 --to 25,49 --sweep -1 --colors wood --follow -1 --start 0.3 $I
$T $W/pengintai/ai pengintai --ancang ancang.png --strike tusuk.png --kind thrust \
   --tip 20,31 --dir-x -1 --length 18 --colors steel --follow -1 --durations 0.14,0.04,0.12,0.08,0.10 $I
$T $W/ratna/ai ratna --ancang idle.png --strike berseru.png --kind none --follow -1 \
   --durations 0.14,0.10,0.18,0.10,0.12 $I
$T $W/bharadah/ai bharadah --ancang idle.png --strike sabda.png --kind none --follow -1 \
   --durations 0.14,0.10,0.18,0.10,0.12 $I

# musuh (menghadap kanan, menyerang ke kanan)
$T $W/pengikut/ai pengikut --ancang ancang.png --strike ayun.png --kind arc \
   --pivot 47,30 --from 29,32 --to 75,37 --sweep -1 --colors copper --follow 1 --start 0.15 $I
$T $W/pemimpin/ai pemimpin_pengikut --ancang ancang.png --strike ayun.png --kind arc \
   --pivot 52,26 --from 25,26 --to 82,31 --sweep -1 --colors copper --follow 1 --start 0.15 $I
$T $W/leak/poses leak --ancang idle.png --strike serang.png --kind claw \
   --pivot 50,30 --from 66,16 --to 70,38 --sweep 1 --colors ghost --width 1.5 --follow 1 $I
$T $W/calon-arang/ai calon_arang --ancang ancang.png --strike kutuk.png --kind thrust \
   --tip 73,25 --dir-x 1 --length 20 --colors magic --follow 1 --durations 0.18,0.05,0.14,0.08,0.12 $I
$T $W/calon-arang-ritual/ai calon_arang_ritual --ancang ancang.png --strike cakar.png --kind claw \
   --pivot 75,40 --from 95,24 --to 100,50 --sweep 1 --colors claw --follow 2 --durations 0.20,0.06,0.16,0.10,0.12 $I
