# Animates ONLY the red screen elements of the user's simulation_chamber_front composite:
# the central core pulses, the four corner brackets light in a rotating sequence. The brass border
# and dark screen are left untouched. Produces the 16x128 animated strip + dim 16x16 off frame.
# Run: powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/generate_chamber_animation.ps1
Add-Type -AssemblyName System.Drawing

$block = "C:\Users\epete\OneDrive\Documents\Simulacra\src\main\resources\assets\simulacra\textures\block"
$src = Join-Path $block 'simulation_chamber_front.png'

function LoadUnlocked($p) { $t=[System.Drawing.Bitmap]::FromFile($p); $b=New-Object System.Drawing.Bitmap $t; $t.Dispose(); return $b }
function C([int]$r,[int]$g,[int]$b,[int]$a) {
  $r=[Math]::Max(0,[Math]::Min(255,$r)); $g=[Math]::Max(0,[Math]::Min(255,$g)); $b=[Math]::Max(0,[Math]::Min(255,$b))
  return [System.Drawing.Color]::FromArgb($a,$r,$g,$b)
}
function Scale($col,[double]$f) { return C ([int]($col.R*$f)) ([int]($col.G*$f)) ([int]($col.B*$f)) $col.A }
# Red screen pixel: strong red channel, weak green/blue (excludes brass which has high green).
function IsRed($p) { return ($p.A -gt 32) -and ($p.R -gt 70) -and ($p.G -lt $p.R*0.55) -and ($p.B -lt $p.R*0.75) }

$base = LoadUnlocked $src

# Post-process on top of the source art (kept out of the .bbmodel so it is one edit to revert):
# rivets at the wood-ring corners so the panel reads as bolted on.
$rivet = C 56 42 28 255
foreach ($rp in @(@(2,2),@(13,2),@(2,13),@(13,13))) { $base.SetPixel($rp[0],$rp[1],$rivet) }

# Only the central core is a light; the red corner brackets are part of the brass frame and must
# stay untouched in every frame (and in the off state).
$strip = New-Object System.Drawing.Bitmap 16,128
for ($f=0; $f -lt 8; $f++) {
  $pulse = 0.50 + 0.65 * ((([Math]::Sin(2*[Math]::PI*$f/8)) * 0.5) + 0.5)
  for ($y=0; $y -lt 16; $y++) {
    for ($x=0; $x -lt 16; $x++) {
      $p = $base.GetPixel($x,$y)
      if ($x -ge 6 -and $x -le 9 -and $y -ge 6 -and $y -le 9 -and (IsRed $p)) {
        $p = Scale $p $pulse
      }
      $strip.SetPixel($x, $f*16+$y, $p)
    }
  }
}
$strip.Save($src, [System.Drawing.Imaging.ImageFormat]::Png); $strip.Dispose()
Write-Host "wrote chamber front strip (8 frames)"

$off = New-Object System.Drawing.Bitmap 16,16
for ($y=0; $y -lt 16; $y++) { for ($x=0; $x -lt 16; $x++) {
  $p = $base.GetPixel($x,$y)
  if ($x -ge 6 -and $x -le 9 -and $y -ge 6 -and $y -le 9 -and (IsRed $p)) { $p = Scale $p 0.30 }
  $off.SetPixel($x,$y,$p)
} }
$off.Save((Join-Path $block 'simulation_chamber_front_off.png'), [System.Drawing.Imaging.ImageFormat]::Png); $off.Dispose()
$base.Dispose()
Write-Host "wrote chamber front off"