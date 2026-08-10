# Derives refined and pristine imprint blank textures from art/substrate.png (the crude tier),
# keeping its silhouette. Crude itself is untouched.
#
# The three tiers have to be tellable apart in an inventory slot, which brightness alone does not
# achieve - measured at 8.6 apart on a 0-255 scale, i.e. indistinguishable. So each tier moves in hue
# AND carries a structural mark: crude is bare clay, refined is cooler ceramic with an etched amber
# trace, pristine is pearl with a rose inlay and a hard glint. Re-check after editing with:
#   python <skills>/minecraft-pixel-art/scripts/check_texture.py --compare <the three files>
# Run: powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/generate_substrate_tiers.ps1
Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$src  = Join-Path $root 'art/substrate.png'
$item = Join-Path $root 'src/main/resources/assets/simulacra/textures/item'

function LoadUnlocked($p) { $t=[System.Drawing.Bitmap]::FromFile($p); $b=New-Object System.Drawing.Bitmap $t; $t.Dispose(); return $b }
function Clamp([int]$v) { return [Math]::Max(0,[Math]::Min(255,$v)) }
function C([int]$r,[int]$g,[int]$b,[int]$a) { return [System.Drawing.Color]::FromArgb($a,(Clamp $r),(Clamp $g),(Clamp $b)) }
function IsFleck($p) { return ($p.A -gt 32) -and ($p.R -gt 150) -and ($p.G -gt ($p.B + 40)) }

$base = LoadUnlocked $src

# Refined: machine-finished, so it reads cooler and greyer than raw clay, and carries an etched
# circuit trace crude blanks do not have.
$traceDk = C 178 124 50 255
$traceHi = C 248 202 112 255
$out = New-Object System.Drawing.Bitmap 16,16
for ($y=0; $y -lt 16; $y++) { for ($x=0; $x -lt 16; $x++) {
  $p = $base.GetPixel($x,$y)
  if ($p.A -le 32) { $out.SetPixel($x,$y,$p); continue }
  if (IsFleck $p) { $out.SetPixel($x,$y,(C 240 190 90 $p.A)) }
  else {
    # Blend hard toward a cool grey-blue rather than nudging the multipliers. Crude is warm tan, so
    # a decisive move across the wheel is what separates them at 16px; a subtle shift measured as no
    # improvement at all over the original brightness-only version.
    $r = [int]($p.R + (138 - $p.R) * 0.55)
    $g = [int]($p.G + (162 - $p.G) * 0.55)
    $b = [int]($p.B + (198 - $p.B) * 0.62)
    $out.SetPixel($x,$y,(C $r $g $b $p.A))
  }
} }
# A bus across the middle with a long branch up and a short one down. Deliberately asymmetric: a
# centred cross reads as a crosshair, an offset one reads as circuitry.
foreach ($x in 4..10) { $out.SetPixel($x,8,$traceDk) }
foreach ($p in @(@(6,5),@(6,6),@(6,7),@(9,9),@(9,10))) { $out.SetPixel($p[0],$p[1],$traceDk) }
foreach ($p in @(@(4,8),@(6,5),@(9,10))) { $out.SetPixel($p[0],$p[1],$traceHi) }
$out.Save((Join-Path $item 'refined_imprint_blank.png'), [System.Drawing.Imaging.ImageFormat]::Png); $out.Dispose()
Write-Host "wrote refined_imprint_blank.png"

# Pristine: pearl ceramic carrying a rose inlay - the mark of the only tier that can hold a boss
# imprint - plus a hard glint so it reads as jewel-grade next to the other two.
$rosePr   = C 226 138 168 255
$roseCore = C 250 214 228 255
$glintW   = C 255 252 248 255
$out = New-Object System.Drawing.Bitmap 16,16
for ($y=0; $y -lt 16; $y++) { for ($x=0; $x -lt 16; $x++) {
  $p = $base.GetPixel($x,$y)
  if ($p.A -le 32) { $out.SetPixel($x,$y,$p); continue }
  if (IsFleck $p) { $out.SetPixel($x,$y,(C 236 148 176 $p.A)) }
  else {
    # blend strongly toward white, keeping the shading
    $r = [int]($p.R + (255 - $p.R) * 0.66); $g = [int]($p.G + (255 - $p.G) * 0.62); $b = [int]($p.B + (255 - $p.B) * 0.64)
    $out.SetPixel($x,$y,(C $r $g $b $p.A))
  }
} }
# Diamond outline, symmetric about the pebble's centre, with a lit core.
foreach ($p in @(@(7,6),@(8,6),@(6,7),@(9,7),@(5,8),@(10,8),@(6,9),@(9,9),@(7,10),@(8,10))) {
  $out.SetPixel($p[0],$p[1],$rosePr)
}
$out.SetPixel(7,8,$roseCore); $out.SetPixel(8,8,$roseCore)
$out.SetPixel(5,5,$glintW); $out.SetPixel(4,7,$glintW)
$out.Save((Join-Path $item 'pristine_imprint_blank.png'), [System.Drawing.Imaging.ImageFormat]::Png); $out.Dispose()
$base.Dispose()
Write-Host "wrote pristine_imprint_blank.png"

# Resonant Catalyst: a rose crystal shard with a dark echoing core and a pale glint.
$rose   = C 214 116 150 255
$roseHi = C 240 168 192 255
$roseDk = C 156 74 108 255
$core   = C 46 24 46 255
$glint  = C 252 232 240 255
$clear  = [System.Drawing.Color]::FromArgb(0,0,0,0)
$out = New-Object System.Drawing.Bitmap 16,16
for ($y=0; $y -lt 16; $y++) { for ($x=0; $x -lt 16; $x++) { $out.SetPixel($x,$y,$clear) } }
# main shard: tall rhombus from (8,1) tip to (8,14) base tip, widest ~rows 7-9
$widths = @(0,1,1,2,2,3,3,4,4,4,3,3,2,2,1,0)
for ($y=1; $y -le 14; $y++) {
  $w = $widths[$y]
  if ($w -le 0) { continue }
  for ($dx=-$w+1; $dx -le $w-1; $dx++) {
    $x = 8 + $dx
    $col = $rose
    if ($dx -le -($w-1)/2 - 0) { $col = $roseHi }      # left facet catches light
    if ($dx -ge ($w-1)) { $col = $roseDk }             # right edge in shadow
    $out.SetPixel($x,$y,$col)
  }
}
# dark resonant core down the middle
foreach ($y in @(6,7,8,9)) { $out.SetPixel(8,$y,$core) }
$out.SetPixel(8,7,$core); $out.SetPixel(7,8,$core)
# glints
$out.SetPixel(7,3,$glint); $out.SetPixel(6,6,$glint)
# small companion shard bottom-left
foreach ($p in @(@(4,11),@(4,12),@(5,12),@(4,13))) { $out.SetPixel($p[0],$p[1],$rose) }
$out.SetPixel(3,12,$roseHi); $out.SetPixel(5,13,$roseDk)
$out.Save((Join-Path $item 'resonant_catalyst.png'), [System.Drawing.Imaging.ImageFormat]::Png); $out.Dispose()
Write-Host "wrote resonant_catalyst.png"

# Corrupted Imprint: the substrate silhouette gone wrong - dark violet, dead flecks, glitch pixels.
$src2 = LoadUnlocked $src
$glitch1 = C 214 60 92 255
$glitch2 = C 132 84 196 255
$out = New-Object System.Drawing.Bitmap 16,16
for ($y=0; $y -lt 16; $y++) { for ($x=0; $x -lt 16; $x++) {
  $p = $src2.GetPixel($x,$y)
  if ($p.A -le 32) { $out.SetPixel($x,$y,$p); continue }
  if (IsFleck $p) { $out.SetPixel($x,$y,(C 74 52 88 $p.A)) }   # flecks gone dead
  else {
    # darken hard and shift violet, keeping the shading
    $out.SetPixel($x,$y,(C ([int]($p.R*0.32)) ([int]($p.G*0.26)) ([int]($p.B*0.42)) $p.A))
  }
} }
# deterministic glitch pixels (fixed positions so reruns are stable)
foreach ($p in @(@(6,5),@(10,7),@(5,9),@(9,11))) { $out.SetPixel($p[0],$p[1],$glitch1) }
foreach ($p in @(@(8,4),@(4,7),@(11,10))) { $out.SetPixel($p[0],$p[1],$glitch2) }
$out.Save((Join-Path $item 'corrupted_imprint.png'), [System.Drawing.Imaging.ImageFormat]::Png); $out.Dispose()
$src2.Dispose()
Write-Host "wrote corrupted_imprint.png"