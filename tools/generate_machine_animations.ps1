# Derives animated front strips + dim "off" frames for the Mainframe Controller and Simulation
# Chamber from the existing single-frame panels, and installs the substrate item art.
# Run: powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/generate_machine_animations.ps1
Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$block = Join-Path $root 'src/main/resources/assets/simulacra/textures/block'
$item  = Join-Path $root 'src/main/resources/assets/simulacra/textures/item'

function C([int]$r,[int]$g,[int]$b,[int]$a) {
  $r=[Math]::Max(0,[Math]::Min(255,$r)); $g=[Math]::Max(0,[Math]::Min(255,$g)); $b=[Math]::Max(0,[Math]::Min(255,$b))
  return [System.Drawing.Color]::FromArgb($a,$r,$g,$b)
}
function Scale($col,[double]$f) { return C ([int]($col.R*$f)) ([int]($col.G*$f)) ([int]($col.B*$f)) $col.A }
function IsLit($col) { return ($col.A -gt 32) -and ([Math]::Max($col.R,[Math]::Max($col.G,$col.B)) -gt 80) }
function LoadUnlocked($p) { $t=[System.Drawing.Bitmap]::FromFile($p); $b=New-Object System.Drawing.Bitmap $t; $t.Dispose(); return $b }

# ---------- substrate -> Crude Imprint Blank item ----------
Copy-Item (Join-Path $root 'art/substrate.png') (Join-Path $item 'crude_imprint_blank.png') -Force
Write-Host "installed substrate.png as crude_imprint_blank.png"

# ---------- Mainframe Controller: equalizer bars grow/shrink ----------
$base = LoadUnlocked (Join-Path $block 'mainframe_controller_front.png')
$bgDark = $base.GetPixel(5,5)
$lengths = @{
  '4'  = @(8,7,6,5,4,5,6,7)
  '6'  = @(5,6,7,8,7,6,5,4)
  '8'  = @(3,4,5,6,5,4,3,4)
  '10' = @(7,8,7,6,5,6,7,8)
}
$strip = New-Object System.Drawing.Bitmap 16,128
for ($f=0; $f -lt 8; $f++) {
  for ($y=0; $y -lt 16; $y++) {
    for ($x=0; $x -lt 16; $x++) {
      $col = $base.GetPixel($x,$y)
      if (($y -eq 4 -or $y -eq 6 -or $y -eq 8 -or $y -eq 10) -and $x -ge 4 -and $x -le 11) {
        $L = $lengths["$y"][$f]
        if ($x -lt (4 + $L)) { $col = $base.GetPixel(4,$y) } else { $col = $bgDark }
      }
      $strip.SetPixel($x, $f*16+$y, $col)
    }
  }
}
$strip.Save((Join-Path $block 'mainframe_controller_front.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$strip.Dispose()
$off = New-Object System.Drawing.Bitmap 16,16
for ($y=0; $y -lt 16; $y++) { for ($x=0; $x -lt 16; $x++) {
  $col = $base.GetPixel($x,$y); if (IsLit $col) { $col = Scale $col 0.30 }; $off.SetPixel($x,$y,$col)
} }
$off.Save((Join-Path $block 'mainframe_controller_front_off.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$off.Dispose(); $base.Dispose()
Write-Host "wrote controller front (8 frames) + off"

# ---------- Simulation Chamber: pulsing core + rotating corner indicators ----------
$base = LoadUnlocked (Join-Path $block 'simulation_chamber_front.png')
$cornerX = @(4,11,4,11); $cornerY = @(4,4,11,11)
$strip = New-Object System.Drawing.Bitmap 16,128
for ($f=0; $f -lt 8; $f++) {
  $b = 0.55 + 0.55 * (([Math]::Sin(2*[Math]::PI*$f/8) * 0.5) + 0.5)
  $lit = [Math]::Floor($f/2) % 4
  for ($y=0; $y -lt 16; $y++) {
    for ($x=0; $x -lt 16; $x++) {
      $col = $base.GetPixel($x,$y)
      if ($x -ge 6 -and $x -le 9 -and $y -ge 6 -and $y -le 9 -and (IsLit $col)) { $col = Scale $col $b }
      for ($i=0; $i -lt 4; $i++) {
        if ($x -eq $cornerX[$i] -and $y -eq $cornerY[$i] -and (IsLit $col)) {
          $col = Scale $col $(if ($i -eq $lit) {1.5} else {0.45})
        }
      }
      $strip.SetPixel($x, $f*16+$y, $col)
    }
  }
}
$strip.Save((Join-Path $block 'simulation_chamber_front.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$strip.Dispose()
$off = New-Object System.Drawing.Bitmap 16,16
for ($y=0; $y -lt 16; $y++) { for ($x=0; $x -lt 16; $x++) {
  $col = $base.GetPixel($x,$y); if (IsLit $col) { $col = Scale $col 0.30 }; $off.SetPixel($x,$y,$col)
} }
$off.Save((Join-Path $block 'simulation_chamber_front_off.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$off.Dispose(); $base.Dispose()
Write-Host "wrote chamber front (8 frames) + off"
Write-Host "done"
