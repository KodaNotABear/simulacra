# One-shot pipeline for the machine front panels. Run this after saving edits in Blockbench:
#   1) extracts the 16x16 front composites from the saved .bbmodel files (north-face texture)
#   2) runs the animation generators, which add the bevel/rivet post-process and write the
#      16x128 animated strips + dim off frames into the mod assets.
# Run: powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/update_machine_fronts.ps1
$root = Split-Path -Parent $PSScriptRoot
$block = Join-Path $root 'src/main/resources/assets/simulacra/textures/block'

$jobs = @(
  @{ bbm = Join-Path $root 'art/reference/chain_drive/simulation_chamber.bbmodel'; out = Join-Path $block 'simulation_chamber_front.png' },
  @{ bbm = Join-Path $root 'art/reference/chain_drive/controller.bbmodel';         out = Join-Path $block 'mainframe_controller_front.png' }
)
foreach ($j in $jobs) {
  $json = Get-Content $j.bbm -Raw | ConvertFrom-Json
  $northTex = $json.elements[0].faces.north.texture
  $b64 = ($json.textures[$northTex].source) -replace '^data:image/png;base64,',''
  [IO.File]::WriteAllBytes($j.out, [Convert]::FromBase64String($b64))
  Write-Host "extracted $($j.out) (texture index $northTex)"
}

& powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'generate_chamber_animation.ps1')
& powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'generate_controller_animation.ps1')
