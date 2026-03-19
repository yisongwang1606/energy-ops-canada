$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$projectRoot = Split-Path -Parent $PSScriptRoot

Push-Location $projectRoot
try {
    docker compose down --volumes --remove-orphans --rmi all
    docker builder prune --all --force
    docker system prune --all --force --volumes
} finally {
    Pop-Location
}

wsl --shutdown
