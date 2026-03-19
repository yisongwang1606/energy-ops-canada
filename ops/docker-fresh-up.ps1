$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$projectRoot = Split-Path -Parent $PSScriptRoot
$wslConfigPath = Join-Path $env:USERPROFILE ".wslconfig"
$wslBackupPath = Join-Path $env:USERPROFILE ".wslconfig.energy-ops.backup"
$wslConfig = @"
[wsl2]
memory=8GB
swap=0
defaultVhdSize=8GB

[experimental]
autoMemoryReclaim=gradual
sparseVhd=true
"@

if ((Test-Path $wslConfigPath) -and -not (Test-Path $wslBackupPath)) {
    Copy-Item $wslConfigPath $wslBackupPath
}

Set-Content -Path $wslConfigPath -Value $wslConfig -Encoding ascii

Write-Host "WSL Docker limits overwritten at $wslConfigPath"
Write-Host "memory=8GB, defaultVhdSize=8GB, sparseVhd=true"
Write-Host "Docker startup for this project is destructive by design: containers, images, and build cache are rebuilt each run."

wsl --shutdown

Push-Location $projectRoot
try {
    docker compose down --volumes --remove-orphans --rmi all
    docker builder prune --all --force
    docker system prune --all --force --volumes
    docker compose build --pull --no-cache
    docker compose up --detach --force-recreate --renew-anon-volumes
    docker compose ps
} finally {
    Pop-Location
}
