# Generates placeholder 16x16 textures for Simulacra, leaning on Create's palette:
# wood + brass + stone, with RED for indicator lights. No teal / off-palette colours.
# The Neural Node front is an 8-frame animated drive-activity strip (16x128) + .mcmeta.
# Run:  pwsh -File tools/generate_placeholder_textures.ps1
Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$dir = Join-Path $root "src\main\resources\assets\simulacra\textures"
New-Item -ItemType Directory -Force -Path (Join-Path $dir "block") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $dir "item")  | Out-Null

function C($a,$r,$g,$b){ [System.Drawing.Color]::FromArgb($a,$r,$g,$b) }
function NewBmp($w,$h){ New-Object System.Drawing.Bitmap $w,$h }
function FillRect($bmp,$x0,$y0,$x1,$y1,$col){
    for($x=$x0;$x -le $x1;$x++){ for($y=$y0;$y -le $y1;$y++){ $bmp.SetPixel($x,$y,$col) } }
}
function Save($bmp,$path){
    $bmp.Save($path,[System.Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose()
    Write-Host "wrote $path"
}

# --- Create palette ---
$wood    = C 255 110 80 46
$woodHi  = C 255 134 99 60
$woodSh  = C 255 80 57 32
$brass   = C 255 196 158 84
$brassHi = C 255 224 192 122
$brassDk = C 255 138 104 46
$stone   = C 255 134 134 138
$stoneSh = C 255 96 96 100
$slot    = C 255 28 24 20
$redOn   = C 255 226 50 40
$redHi   = C 255 255 120 90
$redOff  = C 255 92 24 18
$amber   = C 255 236 196 72
$amberDim= C 255 110 86 28

# brass-framed wood panel, drawn into a 16x16 region at vertical offset $oy
function DrawPanel($b,$oy){
    FillRect $b 0 (0+$oy) 15 (15+$oy) $wood
    FillRect $b 2 (4+$oy) 13 (4+$oy) $woodSh
    FillRect $b 2 (11+$oy) 13 (11+$oy) $woodSh
    for($i=0;$i -le 15;$i++){
        $b.SetPixel($i,(0+$oy),$brassDk); $b.SetPixel(0,($i+$oy),$brassDk)
        $b.SetPixel($i,(15+$oy),$brassDk); $b.SetPixel(15,($i+$oy),$brassDk)
    }
    foreach($p in @(@(2,2),@(13,2),@(2,13),@(13,13))){ $b.SetPixel($p[0],($p[1]+$oy),$brass) }
}

# one frame of the Neural Node front: a mounted drive module (opaque; sits proud on the casing,
# which shows as a border around it). Stacked bays with blinking red activity LEDs.
function DrawNodeFront($b,$oy,$f){
    # Transparent base so the casing shows through. Only drive bays (dark slots) and the animated
    # red + amber activity lights are drawn; no opaque backing.
    $bayYs = @(3,7,11)
    for($bi=0;$bi -lt 3;$bi++){
        $by = $bayYs[$bi]
        FillRect $b 2 ($by+$oy) 13 ($by+1+$oy) $slot           # drive bay (dark slot)
        $r = ((($f + $bi) % 3) -ne 0)
        $rcol = if($r){$redOn}else{$redOff}
        $b.SetPixel(12,($by+$oy),$rcol)                        # red activity LED
        $a = ((($f + $bi*2) % 2) -eq 0)
        $acol = if($a){$amber}else{$amberDim}
        $b.SetPixel(10,($by+$oy),$acol)                        # amber activity LED
        $hx = 4 + ((($f*2)+($bi*3)) % 6)                       # read indicator sweeping the bay
        $sweep = if(($f % 2) -eq 0){$redOn}else{$amber}
        $b.SetPixel($hx,($by+1+$oy),$sweep)
    }
}

# === Neural Node front: animated 8-frame strip ===
$frames = 8
$strip = NewBmp 16 (16*$frames)
for($f=0;$f -lt $frames;$f++){ DrawNodeFront $strip ($f*16) $f }
Save $strip (Join-Path $dir "block\neural_node_front.png")

# === Neural Node side / top (rack casing) ===
$b = NewBmp 16 16; DrawPanel $b 0
foreach($y in 6,9){ FillRect $b 4 $y 11 $y $stoneSh }
Save $b (Join-Path $dir "block\neural_node_side.png")

# === Neural Node back: recess floor with shaft socket ===
$b = NewBmp 16 16
FillRect $b 0 0 15 15 $stone
for($i=0;$i -le 15;$i++){ $b.SetPixel($i,0,$stoneSh); $b.SetPixel(0,$i,$stoneSh); $b.SetPixel($i,15,$stoneSh); $b.SetPixel(15,$i,$stoneSh) }
FillRect $b 4 4 11 11 $brassDk
FillRect $b 5 5 10 10 (C 255 40 36 30)
foreach($p in @(@(4,4),@(11,4),@(4,11),@(11,11))){ $b.SetPixel($p[0],$p[1],$brass) }
Save $b (Join-Path $dir "block\neural_node_back.png")

# === Mainframe Controller front overlay: just the screen + red readouts (transparent base) ===
$b = NewBmp 16 16
FillRect $b 3 3 12 12 $slot               # screen
FillRect $b 4 4 11 4  $redOn
FillRect $b 4 6 10 6  $redOff
FillRect $b 4 8 8 8   $redOff
FillRect $b 4 10 11 10 $redOff
$b.SetPixel(11,4,$redHi)
Save $b (Join-Path $dir "block\mainframe_controller_front.png")

# === Simulation Chamber front overlay: just the viewport + red core (transparent base) ===
$b = NewBmp 16 16
FillRect $b 3 3 12 12 $slot               # viewport
FillRect $b 6 6 9 9 $redOff
FillRect $b 7 7 8 8 $redOn
$b.SetPixel(7,7,$redHi)
foreach($p in @(@(4,4),@(11,4),@(4,11),@(11,11))){ $b.SetPixel($p[0],$p[1],$redOff) }
Save $b (Join-Path $dir "block\simulation_chamber_front.png")

# === Mainframe Controller side: rack units with red status LEDs ===
$b = NewBmp 16 16; DrawPanel $b 0
foreach($y in 3,6,9,12){ FillRect $b 3 $y 12 $y $stoneSh; $b.SetPixel(11,$y,$redOn) }
Save $b (Join-Path $dir "block\mainframe_controller_side.png")

# === Mainframe Controller top: vent grille ===
$b = NewBmp 16 16; DrawPanel $b 0
foreach($x in 4,6,8,10,12){ FillRect $b $x 4 $x 11 $stoneSh }
Save $b (Join-Path $dir "block\mainframe_controller_top.png")

# === Data Cable: brass pipe ===
$b = NewBmp 16 16
FillRect $b 0 0 15 15 $brass
FillRect $b 0 0 15 1 $brassHi
FillRect $b 0 14 15 15 $brassDk
foreach($y in 4,8,12){ FillRect $b 0 $y 15 $y $brassDk }
Save $b (Join-Path $dir "block\data_cable.png")

# === items ===
$b = NewBmp 16 16
FillRect $b 3 2 12 13 $woodSh
FillRect $b 4 3 11 12 $stone
FillRect $b 4 3 11 3  $brass
$d = $redOn
foreach($x in 5,8,11){ foreach($y in 6,9){ $b.SetPixel($x,$y,$d) } }
Save $b (Join-Path $dir "item\blank_data_matrix.png")

$b = NewBmp 16 16
FillRect $b 3 3 12 12 $wood
for($i=3;$i -le 12;$i++){ $b.SetPixel($i,3,$brassDk); $b.SetPixel($i,12,$brassDk); $b.SetPixel(3,$i,$brassDk); $b.SetPixel(12,$i,$brassDk) }
$clear = C 0 0 0 0
$b.SetPixel(3,3,$clear); $b.SetPixel(12,3,$clear); $b.SetPixel(3,12,$clear); $b.SetPixel(12,12,$clear)
FillRect $b 5 5 6 6 $woodHi
$b.SetPixel(9,8,$woodSh); $b.SetPixel(7,10,$woodSh); $b.SetPixel(10,5,$brass)
Save $b (Join-Path $dir "item\crude_imprint_blank.png")

Write-Host "done."
