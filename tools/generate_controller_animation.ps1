# Animates ONLY the bar pixels of the user's mainframe_controller_front composite: each of the four
# bars grows/shrinks like an equalizer. The brass border and dark screen are left untouched.
# Produces the 16x128 animated strip + a dim 16x16 off frame.
# Run: powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/generate_controller_animation.ps1
Add-Type -AssemblyName System.Drawing

$block = "C:\Users\epete\OneDrive\Documents\Simulacra\src\main\resources\assets\simulacra\textures\block"
$src = Join-Path $block 'mainframe_controller_front.png'

function LoadUnlocked($p) { $t=[System.Drawing.Bitmap]::FromFile($p); $b=New-Object System.Drawing.Bitmap $t; $t.Dispose(); return $b }
function C([int]$r,[int]$g,[int]$b,[int]$a) {
  $r=[Math]::Max(0,[Math]::Min(255,$r)); $g=[Math]::Max(0,[Math]::Min(255,$g)); $b=[Math]::Max(0,[Math]::Min(255,$b))
  return [System.Drawing.Color]::FromArgb($a,$r,$g,$b)
}
function Scale($col,[double]$f) { return C ([int]($col.R*$f)) ([int]($col.G*$f)) ([int]($col.B*$f)) $col.A }
function IsRed($p) { return ($p.A -gt 32) -and ($p.R -gt 70) -and ($p.G -lt $p.R*0.55) -and ($p.B -lt $p.R*0.75) }

$base = LoadUnlocked $src

# Post-process on top of the source art (kept out of the .bbmodel so it is one edit to revert):
# rivets at the wood-ring corners so the panel reads as bolted on.
$rivet = C 56 42 28 255
foreach ($rp in @(@(2,2),@(13,2),@(2,13),@(13,13))) { $base.SetPixel($rp[0],$rp[1],$rivet) }

$bgScreen = $base.GetPixel(5,5)
$lengths = @{
  '4'  = @(8,7,6,5,4,5,6,7)
  '6'  = @(5,6,7,8,7,6,5,4)
  '8'  = @(6,5,4,3,4,5,6,7)
  '10' = @(7,8,7,6,5,6,7,8)
}
$strip = New-Object System.Drawing.Bitmap 16,128
for ($f=0; $f -lt 8; $f++) {
  for ($y=0; $y -lt 16; $y++) {
    for ($x=0; $x -lt 16; $x++) {
      $p = $base.GetPixel($x,$y)
      if (($y -eq 4 -or $y -eq 6 -or $y -eq 8 -or $y -eq 10) -and $x -ge 4 -and $x -le 11) {
        $L = $lengths["$y"][$f]
        if ($x -lt (4 + $L)) { $p = $base.GetPixel(4,$y) } else { $p = $bgScreen }
      }
      $strip.SetPixel($x, $f*16+$y, $p)
    }
  }
}
$strip.Save($src, [System.Drawing.Imaging.ImageFormat]::Png); $strip.Dispose()
Write-Host "wrote controller front strip (8 frames)"

$off = New-Object System.Drawing.Bitmap 16,16
for ($y=0; $y -lt 16; $y++) { for ($x=0; $x -lt 16; $x++) {
  $p = $base.GetPixel($x,$y); if (IsRed $p) { $p = Scale $p 0.30 }; $off.SetPixel($x,$y,$p)
} }
$off.Save((Join-Path $block 'mainframe_controller_front_off.png'), [System.Drawing.Imaging.ImageFormat]::Png); $off.Dispose()
$base.Dispose()
Write-Host "wrote controller front off"