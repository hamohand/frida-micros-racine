# =====================================================================
#   FRIDA - Installer PowerShell (Docker dans WSL Ubuntu)
#   Strategie : WSL Ubuntu + Docker apt (pas Docker Desktop)
#   Compose : docker-compose.local.yml (Composante 1 - local notaire)
# =====================================================================

$ErrorActionPreference = "Continue"
$OutputEncoding = [Console]::OutputEncoding = [System.Text.Encoding]::UTF8

function Write-Section($title) {
    Write-Host ""
    Write-Host "=====================================" -ForegroundColor Cyan
    Write-Host " $title" -ForegroundColor Cyan
    Write-Host "=====================================" -ForegroundColor Cyan
}

function Test-WslUbuntuReady {
    try {
        $test = wsl -d Ubuntu -e echo "ready" 2>&1
        return ($test -match "ready")
    } catch { return $false }
}

# ------------------------------------------------------------
#  Phase 1 : Verification / Installation de WSL Ubuntu
# ------------------------------------------------------------
Write-Section "[1/5] Verification de l'environnement Linux (WSL Ubuntu)"

if (-not (Test-WslUbuntuReady)) {
    # Ubuntu present mais pas initialise ?
    $list = wsl -l -q 2>&1
    $listStr = if ($list) { $list -join " " } else { "" }

    if ($listStr -match "Ubuntu") {
        Write-Host "Ubuntu est installe mais non initialise." -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Action requise :"
        Write-Host "  1. Ouvrez le menu Demarrer de Windows."
        Write-Host "  2. Cherchez 'Ubuntu' et lancez l'application."
        Write-Host "  3. Creez votre nom d'utilisateur UNIX et un mot de passe."
        Write-Host "  4. Fermez la fenetre Ubuntu et relancez ce script."
        pause
        exit
    } else {
        Write-Host "WSL Ubuntu n'est pas installe." -ForegroundColor Yellow
        Write-Host "Lancement de l'installation..."
        wsl --install -d Ubuntu
        Write-Host ""
        Write-Host "IMPORTANT :" -ForegroundColor Yellow
        Write-Host "  - Windows peut vous demander de REDEMARRER."
        Write-Host "  - Apres redemarrage, Ubuntu s'ouvrira pour creer votre profil UNIX."
        Write-Host "  - Une fois le profil cree, RELANCEZ ce script."
        pause
        exit
    }
}
Write-Host "WSL Ubuntu detecte et pret." -ForegroundColor Green

# ------------------------------------------------------------
#  Phase 2 : Extraction de frida-micros.zip
# ------------------------------------------------------------
Write-Section "[2/5] Extraction des sources FRIDA"

$zipPath = Join-Path -Path $PSScriptRoot -ChildPath "frida-micros.zip"
$targetPath = Join-Path -Path $env:USERPROFILE -ChildPath "Frida-Micros"

if (Test-Path (Join-Path $targetPath "docker-compose.local.yml")) {
    Write-Host "Sources FRIDA deja presentes dans $targetPath (extraction ignoree)."
} else {
    if (-not (Test-Path $zipPath)) {
        Write-Host "ERREUR : frida-micros.zip introuvable a cote du script." -ForegroundColor Red
        Write-Host "Verifiez que le zip est dans le meme dossier que Installer-Frida.bat."
        pause
        exit 1
    }
    Write-Host "Decompression de $zipPath vers $targetPath ..."
    Expand-Archive -Path $zipPath -DestinationPath $targetPath -Force
    Write-Host "Extraction terminee." -ForegroundColor Green
}

# Verification : le compose et le .env doivent etre presents apres extraction
$composeInTarget = Join-Path $targetPath "docker-compose.local.yml"
$envInTarget = Join-Path $targetPath ".env"
if (-not (Test-Path $composeInTarget)) {
    Write-Host "ERREUR : $composeInTarget introuvable dans le zip." -ForegroundColor Red
    Write-Host "Le zip doit contenir docker-compose.local.yml a la racine."
    pause
    exit 1
}
if (-not (Test-Path $envInTarget)) {
    $envLocal = Join-Path $targetPath ".env.local"
    if (Test-Path $envLocal) {
        Copy-Item $envLocal $envInTarget
        Write-Host "Fichier .env cree depuis .env.local."
    }
}

# Convertit le chemin Windows en chemin WSL (/mnt/c/...)
$linuxPath = "/mnt/c/Users/$env:USERNAME/Frida-Micros"

# ------------------------------------------------------------
#  Phase 3 : Installation de Docker dans WSL Ubuntu
# ------------------------------------------------------------
Write-Section "[3/5] Installation de Docker dans WSL Ubuntu"
Write-Host "(Cette etape peut prendre 3 a 5 minutes la premiere fois.)"

$dockerCheck = wsl -u root -d Ubuntu -e bash -c "command -v docker >/dev/null && echo installed || echo missing" 2>&1
if ($dockerCheck -match "installed") {
    Write-Host "Docker deja installe dans WSL." -ForegroundColor Green
} else {
    Write-Host "Installation de Docker via le script officiel get.docker.com ..."
    # Le script officiel installe docker-ce + le plugin compose v2
    wsl -u root -d Ubuntu -e bash -c "apt-get update -qq && apt-get install -y -qq curl ca-certificates && curl -fsSL https://get.docker.com | sh" 2>&1 | Out-Host

    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "ERREUR : L'installation de Docker a echoue." -ForegroundColor Red
        Write-Host "Verifiez votre connexion Internet et relancez ce script."
        pause
        exit 1
    }
    Write-Host "Docker installe." -ForegroundColor Green

    # Active systemd dans WSL pour que le service docker demarre automatiquement
    Write-Host "Activation de systemd dans WSL..."
    wsl -u root -d Ubuntu -e bash -c "printf '[boot]\nsystemd=true\n' > /etc/wsl.conf" 2>&1 | Out-Null

    # Redemarre WSL pour prendre en compte systemd
    Write-Host "Redemarrage de WSL pour appliquer la configuration..."
    wsl --shutdown
    Start-Sleep -Seconds 3
    # Force un redemarrage explicite en lancant une commande
    wsl -d Ubuntu -e echo "restarted" 2>&1 | Out-Null
}

# Verifie que docker daemon repond ; sinon tente un demarrage manuel (au cas ou systemd desactive)
$daemonCheck = wsl -u root -d Ubuntu -e bash -c "docker info >/dev/null 2>&1 && echo up || echo down" 2>&1
if ($daemonCheck -match "down") {
    Write-Host "Le daemon Docker ne repond pas, tentative de demarrage manuel..."
    wsl -u root -d Ubuntu -e bash -c "service docker start" 2>&1 | Out-Null
    Start-Sleep -Seconds 3
}

# ------------------------------------------------------------
#  Phase 4 : Build et lancement de la stack FRIDA
# ------------------------------------------------------------
Write-Section "[4/5] Build et lancement de FRIDA (Composante 1)"
Write-Host "(Le premier build peut prendre 5 a 15 minutes.)"

$composeCmd = "cd '$linuxPath' && docker compose -f docker-compose.local.yml --env-file .env up -d --build"
wsl -u root -d Ubuntu -e bash -c $composeCmd 2>&1 | Out-Host

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "ERREUR : Le build ou le demarrage a echoue." -ForegroundColor Red
    Write-Host "Consultez les logs ci-dessus pour identifier le probleme."
    pause
    exit 1
}
Write-Host "FRIDA est en cours d'execution." -ForegroundColor Green

# ------------------------------------------------------------
#  Phase 5 : Creation du raccourci Demarrer-Frida sur le Bureau
# ------------------------------------------------------------
Write-Section "[5/5] Creation du raccourci de demarrage"

$desktopPath = [Environment]::GetFolderPath("Desktop")
$launcherPath = Join-Path -Path $desktopPath -ChildPath "Demarrer-Frida.bat"

$launcherContent = @"
@echo off
title FRIDA - Demarrage
chcp 65001 >nul
echo.
echo ============================================
echo         Demarrage de FRIDA
echo ============================================
echo.
echo Reveil des services Docker dans WSL...
wsl -u root -d Ubuntu -e bash -c "cd '$linuxPath' && docker compose -f docker-compose.local.yml --env-file .env up -d"
echo.
echo Attente du demarrage complet (30 secondes)...
timeout /t 30 /nobreak >nul
echo.
echo Ouverture du navigateur...
start http://localhost
echo.
echo FRIDA est pret a l'usage.
echo Fermez cette fenetre quand vous voulez.
pause
"@

Set-Content -Path $launcherPath -Value $launcherContent -Encoding ASCII
Write-Host "Raccourci 'Demarrer-Frida.bat' cree sur le Bureau." -ForegroundColor Green

# ------------------------------------------------------------
#  Fin
# ------------------------------------------------------------
Write-Host ""
Write-Host "=============================================" -ForegroundColor Green
Write-Host "  INSTALLATION TERMINEE AVEC SUCCES !" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green
Write-Host ""
Write-Host "  - Ouvrez votre navigateur : http://localhost"
Write-Host "  - Pour redemarrer FRIDA plus tard : double-cliquez sur 'Demarrer-Frida' sur le Bureau."
Write-Host ""
Write-Host "  Vos donnees sont dans : $targetPath\data\"
Write-Host ""
Start-Sleep -Seconds 3
Start-Process "http://localhost"
