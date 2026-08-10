# Generates 16x16 textures for the Data Cable (belt-inspired, dark grey) and Blank Data Matrix.
# Clean-room art (never copies Create PNGs); dark grey + brass + amber palette.
# NOTE: crude_imprint_blank.png is Ethan's own art (from art/substrate.png) - do NOT generate it here.
#
# These outputs are scaffolding. Once a texture has been touched up by hand, this script must not be
# allowed to quietly stamp over it - that happened to the Data Cable, whose hand-drawn connector ends
# were replaced by the plain generated ring. Existing files are therefore left alone unless -Force is
# passed, matching how make_ponder_structures.py guards its own output.
#
# Run: powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/generate_item_textures.ps1
#      ... -File tools/generate_item_textures.ps1 -Force     # overwrite, losing any hand edits
param([switch]$Force)
Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$blockDir = Join-Path $root 'src/main/resources/assets/simulacra/textures/block'
$itemDir  = Join-Path $root 'src/main/resources/assets/simulacra/textures/item'

function New-Img { return New-Object System.Drawing.Bitmap 16,16 }
function C([int]$r,[int]$g,[int]$b,[int]$a=255) { return [System.Drawing.Color]::FromArgb($a,$r,$g,$b) }
function Save($bmp,$path) {
    if ((Test-Path $path) -and -not $Force) {
        Write-Host "kept existing $(Split-Path -Leaf $path) (pass -Force to overwrite)"
        $bmp.Dispose()
        return
    }
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "wrote $path"
}

# ---------- Data Cable: mechanical-belt read - dark brownish gray, stitched segments ----------
# Mostly flat dark brown-gray body with very low-contrast mottle; a darker seam ring every ~6px
# with dim thread stitches crossing it, like a belt's stitched-together sections. No bright rim.
$body    = C 52 46 40
$mottle  = C 58 51 44
$seam    = C 40 35 30
$stitch  = C 96 81 63
$edge    = C 45 39 34
$seams = @(2, 8, 14)
$bmp = New-Img
for ($y=0; $y -lt 16; $y++) {
  for ($x=0; $x -lt 16; $x++) {
    $col = $body
    if (((($x * 3) + ($y * 7)) % 11) -eq 0) { $col = $mottle }   # sparse, subtle mottle
    if ($seams -contains $x) { $col = $seam }                    # segment seam rings
    if ($y -eq 0 -or $y -eq 15) { $col = $edge }                 # faintly darker edge rows
    $bmp.SetPixel($x,$y,$col)
  }
}
# thread stitches crossing each seam, placed inside the band the pipe model actually shows
foreach ($sx in $seams) {
  $bmp.SetPixel($sx, 6, $stitch)
  $bmp.SetPixel($sx, 9, $stitch)
  $bmp.SetPixel($sx, 12, $stitch)
  $bmp.SetPixel($sx, 3, $stitch)
}
Save $bmp (Join-Path $blockDir 'data_cable.png')

# ---------- Data Cable item sprite: a coiled length of cable ----------
$coilHi  = C 66 58 50
$coilDk  = C 40 35 30
$clear2  = [System.Drawing.Color]::FromArgb(0,0,0,0)
$bmp = New-Img
for ($y=0; $y -lt 16; $y++) { for ($x=0; $x -lt 16; $x++) { $bmp.SetPixel($x,$y,$clear2) } }
# ring: coil of cable, thickness ~2.5px
for ($y=0; $y -lt 16; $y++) {
  for ($x=0; $x -lt 16; $x++) {
    $dx = $x - 7.5; $dy = $y - 7.5
    $d = [math]::Sqrt($dx*$dx + $dy*$dy)
    if ($d -ge 4.0 -and $d -le 6.6) {
      $col = $body
      if ($dx + $dy -lt -4.5) { $col = $coilHi }     # top-left catches light
      if ($dx + $dy -gt 5.0) { $col = $coilDk }      # bottom-right in shadow
      $bmp.SetPixel($x,$y,$col)
    }
  }
}
# seam stitches on the coil (quarters)
foreach ($p in @(@(7,2),@(8,3),@(13,7),@(12,8),@(7,13),@(8,12),@(2,7),@(3,8))) {
  $bmp.SetPixel($p[0],$p[1],$stitch)
}
# loose tail exiting bottom-right, with a darker end cap
foreach ($p in @(@(12,12),@(13,13),@(14,13),@(14,14))) { $bmp.SetPixel($p[0],$p[1],$body) }
$bmp.SetPixel(15,14,$seam); $bmp.SetPixel(15,15,$seam)
Save $bmp (Join-Path $itemDir 'data_cable.png')

# ---------- Blank Data Matrix: brass-framed slate card, legible node graph ----------
$fr    = C 181 124 62
$frHi  = C 208 155 88
$frDk  = C 130 88 42
$rivet = C 92 62 30
$slate = C 24 28 38
$grid  = C 42 50 64
$wire  = C 82 92 110
$node  = C 216 168 62   # amber nodes, dim-ish (blank matrix); trained variant can brighten later
$nodeC = C 240 196 96
$bmp = New-Img
for ($y=0; $y -lt 16; $y++) {
  for ($x=0; $x -lt 16; $x++) {
    if ($x -eq 0 -or $x -eq 15 -or $y -eq 0 -or $y -eq 15) { $bmp.SetPixel($x,$y,$frDk) }
    elseif ($x -eq 1 -or $x -eq 14 -or $y -eq 1 -or $y -eq 14) {
      # top/left of inner frame catches light
      if ($y -eq 1 -or $x -eq 1) { $bmp.SetPixel($x,$y,$frHi) } else { $bmp.SetPixel($x,$y,$fr) }
    }
    else {
      $col = $slate
      if ((($x % 3) -eq 0) -and (($y % 3) -eq 0)) { $col = $grid }     # blueprint grid dots
      $bmp.SetPixel($x,$y,$col)
    }
  }
}
# corner rivets on the frame
foreach ($p in @(@(1,1),@(14,1),@(1,14),@(14,14))) { $bmp.SetPixel($p[0],$p[1],$rivet) }
# node graph: 4 nodes in a diamond, connected by wires
$nodes = @(@(7,3),@(3,8),@(12,7),@(8,12))
$pairs = @(@(0,1),@(0,2),@(1,3),@(2,3),@(1,2))
foreach ($pr in $pairs) {
  $a = $nodes[$pr[0]]; $b = $nodes[$pr[1]]
  $steps = 10
  for ($i=1; $i -lt $steps; $i++) {
    $px = [int][math]::Round($a[0] + ($b[0]-$a[0]) * $i / $steps)
    $py = [int][math]::Round($a[1] + ($b[1]-$a[1]) * $i / $steps)
    if ($px -ge 2 -and $px -le 13 -and $py -ge 2 -and $py -le 13) { $bmp.SetPixel($px,$py,$wire) }
  }
}
foreach ($n in $nodes) {
  $bmp.SetPixel($n[0],$n[1],$nodeC)
  foreach ($d in @(@(1,0),@(-1,0),@(0,1),@(0,-1))) {
    $px = $n[0]+$d[0]; $py = $n[1]+$d[1]
    if ($px -ge 2 -and $px -le 13 -and $py -ge 2 -and $py -le 13) { $bmp.SetPixel($px,$py,$node) }
  }
}
Save $bmp (Join-Path $itemDir 'blank_data_matrix.png')

# ---------- Incomplete Data Matrix (Sequenced Assembly transitional): wires laid, sockets dark ----------
$socket = C 52 58 72
$bmp = New-Img
for ($y=0; $y -lt 16; $y++) {
  for ($x=0; $x -lt 16; $x++) {
    if ($x -eq 0 -or $x -eq 15 -or $y -eq 0 -or $y -eq 15) { $bmp.SetPixel($x,$y,$frDk) }
    elseif ($x -eq 1 -or $x -eq 14 -or $y -eq 1 -or $y -eq 14) { $bmp.SetPixel($x,$y,$fr) }  # no highlight yet
    else {
      $col = $slate
      if ((($x % 3) -eq 0) -and (($y % 3) -eq 0)) { $col = $grid }
      $bmp.SetPixel($x,$y,$col)
    }
  }
}
# Only the first half of the lattice is strung. Drawing every wire and merely dimming the nodes
# made this near-identical to the finished matrix in an inventory - it has to read as a different
# stage of construction, not a darker version of the same object.
$half = [int][math]::Ceiling($pairs.Count / 2)
for ($pi = 0; $pi -lt $half; $pi++) {
  $pr = $pairs[$pi]
  $a = $nodes[$pr[0]]; $b = $nodes[$pr[1]]
  $steps = 10
  for ($i=1; $i -lt $steps; $i++) {
    $px = [int][math]::Round($a[0] + ($b[0]-$a[0]) * $i / $steps)
    $py = [int][math]::Round($a[1] + ($b[1]-$a[1]) * $i / $steps)
    if ($px -ge 2 -and $px -le 13 -and $py -ge 2 -and $py -le 13) { $bmp.SetPixel($px,$py,$wire) }
  }
}
foreach ($n in $nodes) {
  $bmp.SetPixel($n[0],$n[1],$socket)
  foreach ($d in @(@(1,0),@(-1,0),@(0,1),@(0,-1))) {
    $px = $n[0]+$d[0]; $py = $n[1]+$d[1]
    if ($px -ge 2 -and $px -le 13 -and $py -ge 2 -and $py -le 13) { $bmp.SetPixel($px,$py,$socket) }
  }
}
# Break the frame open at the bottom-right. A silhouette difference is the only cue that survives
# being shrunk to a hotbar slot, so the unfinished item is literally not closed yet.
$gap = [System.Drawing.Color]::FromArgb(0,0,0,0)
foreach ($x in 10..15) { $bmp.SetPixel($x,15,$gap); $bmp.SetPixel($x,14,$gap) }
foreach ($y in 10..13) { $bmp.SetPixel(15,$y,$gap); $bmp.SetPixel(14,$y,$gap) }
# Two loose wire ends reaching into the gap, as if waiting to be soldered.
$bmp.SetPixel(12,12,$wire); $bmp.SetPixel(13,13,$wire); $bmp.SetPixel(11,13,$wire)
Save $bmp (Join-Path $itemDir 'incomplete_data_matrix.png')

Write-Host "done"
