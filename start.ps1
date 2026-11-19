# Lanceur standard FridaAI (WSL Ubuntu / Docker natif + OCR natif Windows + Agent NFC USB)
# Options : -Build, -Demo, -Stop, -Logs, -FixWsl, -Menu, -Help
param(
    [switch]$Build,
    [switch]$Stop,
    [switch]$Logs,
    [switch]$FixWsl,
    [switch]$Demo,
    [switch]$Menu,
    [switch]$Help,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$ExtraArgs
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
$ErrorActionPreference = "Stop"
Set-Location -LiteralPath $PSScriptRoot

if ($ExtraArgs) {
    foreach ($arg in $ExtraArgs) {
        switch -Regex ($arg.ToLowerInvariant()) {
            '^(--help|-h|/\?)$' { $Help = $true }
            '^--build$'         { $Build = $true }
            '^--stop$'          { $Stop = $true }
            '^--logs$'          { $Logs = $true }
            '^--fix-wsl$'       { $FixWsl = $true }
            '^--demo$'          { $Demo = $true }
            '^--menu$'          { $Menu = $true }
            default {
                Write-Warning "Option ignoree : $arg"
            }
        }
    }
}

$drive          = $PSScriptRoot.Substring(0, 1).ToLowerInvariant()
$restPath       = $PSScriptRoot.Substring(2).Replace('\', '/')
$fridaRootWsl   = "/mnt/$drive$restPath"
$envFile        = Join-Path $PSScriptRoot ".env"
$envExampleFile = Join-Path $PSScriptRoot ".env.example"
$usbAgentBat    = "C:\Users\hamoh\Documents\projets\dz-eid\dz-eid-agent\build\install\dz-eid-agent\bin\dz-eid-agent.bat"
$ocrAppDir      = "C:\Users\hamoh\Documents\projets\ocr\backend\app_ocr"
$ocrCoreLibDir  = "C:\Users\hamoh\Documents\projets\ocr\backend\core_lib"
$ocrPythonExe   = "C:\Users\hamoh\Documents\projets\ocr\.venv\Scripts\python.exe"

function Get-FridaPorts {
    $front = "4202"
    $back  = "8080"
    if (Test-Path -LiteralPath $envFile) {
        foreach ($line in Get-Content -LiteralPath $envFile) {
            if ($line -match '^FRONTEND_PORT=(.+)$') {
                $val = $Matches[1].Trim()
                if ($val) { $front = $val }
            } elseif ($line -match '^BACKEND_PORT=(.+)$') {
                $val = $Matches[1].Trim()
                if ($val) { $back = $val }
            }
        }
    }
    return @{ Front = $front; Back = $back }
}

function Test-WslUbuntuDocker {
    if (-not (Get-Command wsl -ErrorAction SilentlyContinue)) {
        return $false
    }
    $prevEap = $ErrorActionPreference
    $ErrorActionPreference = "SilentlyContinue"
    try {
        & wsl -d Ubuntu -e bash -c "command -v docker >/dev/null 2>&1" *> $null
        return ($LASTEXITCODE -eq 0)
    } catch {
        return $false
    } finally {
        $ErrorActionPreference = $prevEap
    }
}

function Ensure-WslUbuntuDocker {
    $prevEap = $ErrorActionPreference
    $ErrorActionPreference = "SilentlyContinue"
    try {
        & wsl -d Ubuntu -u root -e bash -c "docker info >/dev/null 2>&1 || /usr/sbin/service docker start" *> $null
    } finally {
        $ErrorActionPreference = $prevEap
    }
}

function Test-NativeDocker {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        return $false
    }
    $prevEap = $ErrorActionPreference
    $ErrorActionPreference = "SilentlyContinue"
    try {
        & docker info *> $null
        return ($LASTEXITCODE -eq 0)
    } catch {
        return $false
    } finally {
        $ErrorActionPreference = $prevEap
    }
}

function Ensure-UsbAgent {
    $listening = Get-NetTCPConnection -LocalPort 8989 -State Listen -ErrorAction SilentlyContinue
    if (-not $listening -and (Test-Path -LiteralPath $usbAgentBat)) {
        Write-Host "Demarrage de l'agent NFC USB local (dz-eid-agent :8989)..." -ForegroundColor Cyan
        Start-Process -FilePath "cmd.exe" -ArgumentList "/c", "`"`"$usbAgentBat`" --relay`"" -WindowStyle Minimized
    }
}

function Ensure-LocalOcrWindows {
    $listening = Get-NetTCPConnection -LocalPort 8082 -State Listen -ErrorAction SilentlyContinue
    if (-not $listening -and (Test-Path -LiteralPath $ocrAppDir) -and (Test-Path -LiteralPath $ocrPythonExe)) {
        Write-Host "Demarrage du service OCR natif Windows (:8082)..." -ForegroundColor Cyan
        Start-Process -FilePath "powershell.exe" -ArgumentList "-NoExit", "-Command", "`$env:PYTHONPATH = '$ocrCoreLibDir'; Set-Location -LiteralPath '$ocrAppDir'; & '$ocrPythonExe' run.py" -WindowStyle Normal
    }
}

function Ensure-EnvAndUploads {
    if (-not (Test-Path -LiteralPath $envFile) -and (Test-Path -LiteralPath $envExampleFile)) {
        Write-Host "Creation de .env a partir de .env.example..." -ForegroundColor Yellow
        Copy-Item -LiteralPath $envExampleFile -Destination $envFile
    }
    $uploadsDir = Join-Path $PSScriptRoot "backend\uploads"
    if (-not (Test-Path -LiteralPath $uploadsDir)) {
        New-Item -ItemType Directory -Force -Path $uploadsDir | Out-Null
    }
}

function Show-Help {
    $ports = Get-FridaPorts
    Write-Host ""
    Write-Host "FridaAI - Lanceur standard" -ForegroundColor Cyan
    Write-Host "Usage :"
    Write-Host "  .\start.bat                  Demarrer FridaAI (Agent NFC USB + Docker WSL/natif + OCR Windows)"
    Write-Host "  .\start.ps1 -Build           Reconstruire les images Docker puis demarrer (--build)"
    Write-Host "  .\start.ps1 -Demo            Demarrer le mode Demo locale (compose/dev.demo.yml, --demo)"
    Write-Host "  .\start.ps1 -Stop            Arreter les conteneurs Docker FridaAI (--stop)"
    Write-Host "  .\start.ps1 -Logs            Suivre les logs Docker en temps reel (--logs)"
    Write-Host "  .\start.ps1 -FixWsl          Redemarrer WSL et le daemon Docker Ubuntu (--fix-wsl)"
    Write-Host "  .\start.ps1 -Menu            Afficher le menu interactif (--menu)"
    Write-Host "  .\start.ps1 -Help            Afficher cette aide (alias : --help, -h, /?)"
    Write-Host ""
    Write-Host "URLs locales configurees :" -ForegroundColor Yellow
    Write-Host "  Frontend : http://localhost:$($ports.Front)"
    Write-Host "  Backend  : http://localhost:$($ports.Back) (Swagger : /swagger-ui.html)"
    Write-Host "  OCR API  : http://localhost:8082 (fenetre Windows)"
    Write-Host "  NFC USB  : http://127.0.0.1:8989"
    Write-Host ""
}

function Start-FridaMain([bool]$withBuild) {
    $ports = Get-FridaPorts
    Ensure-UsbAgent

    if (Test-WslUbuntuDocker) {
        Ensure-LocalOcrWindows
        Ensure-WslUbuntuDocker
        $scriptCmd = if ($withBuild) { "bash scripts/start.sh --build" } else { "bash scripts/start.sh" }
        Write-Host "Lancement de FridaAI via WSL Ubuntu (Frontend :$($ports.Front), Backend :$($ports.Back))..." -ForegroundColor Green
        & wsl -d Ubuntu -e bash -c "cd '$fridaRootWsl' && $scriptCmd"
        exit $LASTEXITCODE
    }

    if (Test-NativeDocker) {
        Write-Host "Docker natif detecte. Demarrage de FridaAI (Frontend :$($ports.Front), Backend :$($ports.Back))..." -ForegroundColor Green
        Ensure-EnvAndUploads
        if (Test-Path -LiteralPath $envFile) {
            $lines = Get-Content -LiteralPath $envFile
            $updated = $false
            $newLines = foreach ($line in $lines) {
                if ($line -match '^WINDOWS_HOST_IP=') {
                    $updated = $true
                    "WINDOWS_HOST_IP=host.docker.internal"
                } else {
                    $line
                }
            }
            if (-not $updated) {
                $newLines += "WINDOWS_HOST_IP=host.docker.internal"
            }
            [System.IO.File]::WriteAllLines($envFile, $newLines, [System.Text.UTF8Encoding]::new($false))
        }
        Ensure-LocalOcrWindows
        if ($withBuild) {
            & docker compose up -d --build
        } else {
            & docker compose up -d
        }
        exit $LASTEXITCODE
    }

    Write-Error "Docker introuvable (ni dans WSL Ubuntu, ni en natif Windows)."
    exit 1
}

function Stop-FridaDocker {
    Write-Host "Arret des conteneurs Docker FridaAI..." -ForegroundColor Yellow
    if (Test-WslUbuntuDocker) {
        Ensure-WslUbuntuDocker
        & wsl -d Ubuntu -e bash -c "cd '$fridaRootWsl' && docker compose down"
        exit $LASTEXITCODE
    }
    if (Test-NativeDocker) {
        & docker compose down
        exit $LASTEXITCODE
    }
    exit 0
}

function Show-FridaLogs {
    Write-Host "Logs Docker FridaAI (Ctrl+C pour quitter)..." -ForegroundColor Cyan
    if (Test-WslUbuntuDocker) {
        Ensure-WslUbuntuDocker
        & wsl -d Ubuntu -e bash -c "cd '$fridaRootWsl' && docker compose logs -f"
        exit $LASTEXITCODE
    }
    if (Test-NativeDocker) {
        & docker compose logs -f
        exit $LASTEXITCODE
    }
    exit 1
}

function Start-FridaDemo([bool]$withBuild) {
    Ensure-UsbAgent
    Write-Host "Demarrage du mode Demo locale (compose/dev.demo.yml)..." -ForegroundColor Green
    if (Test-WslUbuntuDocker) {
        Ensure-WslUbuntuDocker
        $upCmd = if ($withBuild) { "docker compose -f compose/dev.demo.yml up -d --build" } else { "docker compose -f compose/dev.demo.yml up -d" }
        & wsl -d Ubuntu -e bash -c "cd '$fridaRootWsl' && $upCmd"
        exit $LASTEXITCODE
    }
    if (Test-NativeDocker) {
        Ensure-EnvAndUploads
        if ($withBuild) {
            & docker compose -f compose/dev.demo.yml up -d --build
        } else {
            & docker compose -f compose/dev.demo.yml up -d
        }
        exit $LASTEXITCODE
    }
    Write-Error "Docker introuvable (ni dans WSL Ubuntu, ni en natif Windows)."
    exit 1
}

function Repair-WslDocker {
    Write-Host "Arret complet de WSL (wsl --shutdown)..." -ForegroundColor Yellow
    & wsl --shutdown
    Write-Host "Redemarrage du service Docker dans WSL Ubuntu..." -ForegroundColor Cyan
    & wsl -d Ubuntu -u root -e /usr/sbin/service docker start
    exit $LASTEXITCODE
}

if ($Help) {
    Show-Help
    exit 0
}

if ($Menu) {
    Write-Host ""
    Write-Host "=== FridaAI : Menu de demarrage ===" -ForegroundColor Cyan
    Write-Host "  1. Demarrer FridaAI (Agent NFC + Docker WSL/natif + OCR Windows)"
    Write-Host "  2. Reconstruire (--build) puis demarrer FridaAI"
    Write-Host "  3. Demarrer le mode Demo locale (compose/dev.demo.yml)"
    Write-Host "  4. Arreter les conteneurs Docker"
    Write-Host "  5. Suivre les logs Docker"
    Write-Host "  6. Reparer reseau/Docker WSL (wsl --shutdown + service docker start)"
    Write-Host ""
    $choice = Read-Host "Votre choix (1-6)"
    switch ($choice.Trim()) {
        "1" { Start-FridaMain $false }
        "2" { Start-FridaMain $true }
        "3" { Start-FridaDemo $Build }
        "4" { Stop-FridaDocker }
        "5" { Show-FridaLogs }
        "6" { Repair-WslDocker }
        default { Write-Host "Choix invalide." -ForegroundColor Red; exit 1 }
    }
}

if ($FixWsl) { Repair-WslDocker }
if ($Stop)   { Stop-FridaDocker }
if ($Logs)   { Show-FridaLogs }
if ($Demo)   { Start-FridaDemo $Build }

Start-FridaMain $Build
