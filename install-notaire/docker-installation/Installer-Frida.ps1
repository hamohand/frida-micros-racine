# =====================================================================
#   FRIDA - Installer PowerShell (Docker dans WSL Ubuntu)
#   Strategie : WSL Ubuntu + Docker apt (pas Docker Desktop)
#   Compose : docker-compose.local.yml (Composante 1 - local notaire)
# =====================================================================

$ErrorActionPreference = "Continue"
$OutputEncoding = [Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# wsl.exe emet ses propres messages en UTF-16LE : PowerShell 5.1 les lit alors comme
# du texte entrecoupe de NUL et tout -match echoue silencieusement. WSL_UTF8 corrige
# cela (WSL 0.64+). Le filtre Clean-WslOutput couvre les versions plus anciennes.
$env:WSL_UTF8 = "1"

function Show-Popup($message, $title) {
    # La fenetre Ubuntu ouverte par "wsl --install" recouvre la console : sans popup
    # systeme, l'utilisateur ne voit jamais la consigne et croit l'install terminee.
    try {
        $wshell = New-Object -ComObject WScript.Shell
        # 0x1000 = MB_SYSTEMMODAL : la boite reste au premier plan
        $wshell.Popup($message, 0, $title, 0x1000 + 64) | Out-Null
    } catch { }
}

function Clean-WslOutput($raw) {
    if (-not $raw) { return "" }
    return (($raw -join " ") -replace "`0", "").Trim()
}

function Write-Section($title) {
    Write-Host ""
    Write-Host "=====================================" -ForegroundColor Cyan
    Write-Host " $title" -ForegroundColor Cyan
    Write-Host "=====================================" -ForegroundColor Cyan
}

function Test-WslUbuntuReady {
    try {
        $test = Clean-WslOutput (wsl -d Ubuntu -e echo "ready" 2>&1)
        return ($test -match "ready")
    } catch { return $false }
}

# ------------------------------------------------------------
#  Phase 1 : Verification / Installation de WSL Ubuntu
# ------------------------------------------------------------
Write-Section "[1/5] Verification de l'environnement Linux (WSL Ubuntu)"

if (-not (Test-WslUbuntuReady)) {
    # Ubuntu present mais pas initialise ?
    $listStr = Clean-WslOutput (wsl -l -q 2>&1)

    if ($listStr -match "Ubuntu") {
        Write-Host "Ubuntu est installe mais non initialise." -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Action requise :"
        Write-Host "  1. Ouvrez le menu Demarrer de Windows."
        Write-Host "  2. Cherchez 'Ubuntu' et lancez l'application."
        Write-Host "  3. Creez votre nom d'utilisateur UNIX et un mot de passe."
        Write-Host "  4. Fermez la fenetre Ubuntu et relancez ce script."
        Show-Popup "Ubuntu doit d'abord etre initialise.`n`n1. Ouvrez 'Ubuntu' depuis le menu Demarrer`n2. Creez votre nom d'utilisateur UNIX et un mot de passe`n3. Fermez la fenetre Ubuntu`n4. Double-cliquez a nouveau sur Installer-Frida.bat`n`nL'installation de FRIDA n'est PAS terminee." "FRIDA - Action requise (etape 1 sur 5)"
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
        Show-Popup "WSL Ubuntu vient d'etre installe.`n`n1. Si Windows le demande, REDEMARREZ le PC`n2. Dans la fenetre Ubuntu qui s'ouvre, creez votre nom d'utilisateur UNIX et un mot de passe`n3. Fermez la fenetre Ubuntu`n4. Double-cliquez a nouveau sur Installer-Frida.bat`n`nL'installation de FRIDA n'est PAS terminee : il reste Docker et le build (etapes 3 a 5). Le navigateur ne s'ouvrira qu'a la fin." "FRIDA - Relancez l'installation (etape 1 sur 5)"
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

# Copie les scripts de maintenance a cote de l'installation (les donnees sont ici,
# pas dans le dossier d'installation d'origine)
foreach ($tool in @("sauvegarder.bat", "restaurer.bat", "desinstaller.bat")) {
    $src = Join-Path $PSScriptRoot $tool
    if (Test-Path $src) { Copy-Item $src (Join-Path $targetPath $tool) -Force }
}

# Convertit le chemin Windows en chemin WSL, sans supposer que le dossier de profil
# porte le nom du compte (faux avec un compte Microsoft ou en domaine)
$linuxPath = (wsl -d Ubuntu -e wslpath -a "$targetPath" 2>$null | Select-Object -First 1)
if (-not $linuxPath) {
    Write-Host "ERREUR : impossible de convertir $targetPath en chemin WSL." -ForegroundColor Red
    Write-Host "Verifiez que WSL Ubuntu fonctionne (wsl -d Ubuntu -e echo test)."
    pause
    exit 1
}
$linuxPath = $linuxPath.Trim()

# Identifiants applicatifs : un mot de passe unique par poste, genere une seule
# fois et conserve dans le .env. Aucun mot de passe par defaut n'est livre.
$envContent = Get-Content $envInTarget -Raw
if ($envContent -notmatch '(?m)^\s*ADMIN_PASSWORD\s*=\s*\S') {
    $bytes = New-Object byte[] 9
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    $generated = [Convert]::ToBase64String($bytes).Replace('+','').Replace('/','').Replace('=','')
    if ($envContent -match '(?m)^\s*ADMIN_PASSWORD\s*=') {
        $envContent = $envContent -replace '(?m)^\s*ADMIN_PASSWORD\s*=.*$', "ADMIN_PASSWORD=$generated"
    } else {
        $envContent = $envContent.TrimEnd() + "`r`nADMIN_PASSWORD=$generated`r`n"
    }
    Set-Content -Path $envInTarget -Value $envContent -Encoding ASCII
    Write-Host "Mot de passe applicatif genere pour ce poste." -ForegroundColor Green
}

# Relecture des identifiants effectifs
$adminUser = "maitre"
$adminPass = ""
$uLine = Select-String -Path $envInTarget -Pattern '^\s*ADMIN_USERNAME\s*=\s*(\S+)' -ErrorAction SilentlyContinue | Select-Object -First 1
if ($uLine) { $adminUser = $uLine.Matches[0].Groups[1].Value }
$pLine = Select-String -Path $envInTarget -Pattern '^\s*ADMIN_PASSWORD\s*=\s*(\S+)' -ErrorAction SilentlyContinue | Select-Object -First 1
if ($pLine) { $adminPass = $pLine.Matches[0].Groups[1].Value }

# Port web choisi par l'utilisateur dans .env (80 par defaut)
$portWeb = "80"
$portLine = Select-String -Path $envInTarget -Pattern '^\s*PORT_WEB\s*=\s*(\d+)' -ErrorAction SilentlyContinue | Select-Object -First 1
if ($portLine) { $portWeb = $portLine.Matches[0].Groups[1].Value }
$appUrl = if ($portWeb -eq "80") { "http://localhost" } else { "http://localhost:$portWeb" }

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
Write-Section "[5/5] Creation des raccourcis et du demarrage automatique"

$desktopPath = [Environment]::GetFolderPath("Desktop")

# ------------------------------------------------------------------
# frida-service.ps1 : pilote unique du demarrage / arret.
#
# WSL2 eteint sa machine virtuelle des qu'aucun processus n'y tourne. Un
# "docker compose up -d" rend la main immediatement : quelques secondes plus
# tard WSL s'arrete, les conteneurs avec lui, et Windows n'a plus rien a
# rediriger sur le port 80 (constate en test terrain le 2026-09-09).
# On pose donc un processus d'ancrage qui maintient la VM en vie.
# ------------------------------------------------------------------
$servicePath = Join-Path $targetPath "frida-service.ps1"
$serviceContent = @"
param([ValidateSet("start","stop")][string]`$Action = "start")

`$linuxPath = '$linuxPath'
`$marker    = 'frida-wsl-anchor'

function Get-Anchor {
    Get-CimInstance Win32_Process -Filter "Name='wsl.exe'" -ErrorAction SilentlyContinue |
        Where-Object { `$_.CommandLine -like "*`$marker*" }
}

if (`$Action -eq "start") {
    if (-not (Get-Anchor)) {
        # PowerShell 5.1 ne quote pas les elements de -ArgumentList contenant des
        # espaces : sans guillemets explicites, sh recevait "-c sleep" et l'ancre
        # sortait aussitot. Marqueur ecrit en dur : entre apostrophes, une variable
        # ne serait pas developpee et Get-Anchor ne retrouverait pas l'ancre.
        Start-Process wsl -ArgumentList '-d','Ubuntu','-u','root','-e','sh','-c','"sleep infinity # frida-wsl-anchor"' -WindowStyle Hidden
        Start-Sleep -Seconds 3
    }
    wsl -u root -d Ubuntu -e bash -c "cd '`$linuxPath' && docker compose -f docker-compose.local.yml --env-file .env up -d"
} else {
    wsl -u root -d Ubuntu -e bash -c "cd '`$linuxPath' && docker compose -f docker-compose.local.yml --env-file .env stop"
    Get-Anchor | ForEach-Object { Stop-Process -Id `$_.ProcessId -Force -ErrorAction SilentlyContinue }
}
"@
Set-Content -Path $servicePath -Value $serviceContent -Encoding UTF8

# ---- Raccourci de demarrage sur le Bureau ----
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
powershell -NoProfile -ExecutionPolicy Bypass -File "$servicePath" -Action start
echo.
echo Attente du demarrage complet (30 secondes)...
timeout /t 30 /nobreak >nul
echo.
echo Ouverture du navigateur...
start $appUrl
echo.
echo FRIDA est pret a l'usage.
echo Fermez cette fenetre quand vous voulez : FRIDA continue de tourner.
pause
"@
Set-Content -Path $launcherPath -Value $launcherContent -Encoding ASCII
Write-Host "Raccourci 'Demarrer-Frida.bat' cree sur le Bureau." -ForegroundColor Green

# ---- Raccourci d'arret sur le Bureau ----
$stopPath = Join-Path -Path $desktopPath -ChildPath "Arreter-Frida.bat"
$stopContent = @"
@echo off
title FRIDA - Arret
chcp 65001 >nul
echo.
echo ============================================
echo          Arret de FRIDA
echo ============================================
echo.
echo Vos donnees sont conservees. Pour redemarrer,
echo double-cliquez sur "Demarrer-Frida".
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "$servicePath" -Action stop
echo.
echo FRIDA est arrete.
pause
"@
Set-Content -Path $stopPath -Value $stopContent -Encoding ASCII
Write-Host "Raccourci 'Arreter-Frida.bat' cree sur le Bureau." -ForegroundColor Green

# ---- Demarrage automatique a l'ouverture de session ----
# Sans cela, FRIDA est eteint apres chaque redemarrage du PC et le notaire doit
# penser a cliquer sur un raccourci avant de pouvoir travailler.
$startupDir = [Environment]::GetFolderPath("Startup")
$autoStart  = Join-Path $startupDir "FRIDA-Demarrage.vbs"
$autoContent = @"
' Demarre FRIDA en arriere-plan a l'ouverture de session, sans fenetre.
CreateObject("WScript.Shell").Run "powershell -NoProfile -ExecutionPolicy Bypass -File ""$servicePath"" -Action start", 0, False
"@
Set-Content -Path $autoStart -Value $autoContent -Encoding ASCII
Write-Host "Demarrage automatique installe (FRIDA-Demarrage.vbs)." -ForegroundColor Green

# Pose l'ancre tout de suite : la stack vient d'etre demarree par la phase 4,
# sans ancrage elle s'arreterait a la fin de ce script.
# Arguments en une seule chaine, chemin quote : un dossier profil contenant un
# espace (ex. Jean Dupont) casserait sinon le -File.
Start-Process powershell -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$servicePath`" -Action start" -WindowStyle Hidden

# ------------------------------------------------------------
#  Fin
# ------------------------------------------------------------
Write-Host ""
Write-Host "=============================================" -ForegroundColor Green
Write-Host "  INSTALLATION TERMINEE AVEC SUCCES !" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green
Write-Host ""
Write-Host "  - Ouvrez votre navigateur : $appUrl"
Write-Host "  - Pour redemarrer FRIDA plus tard : double-cliquez sur 'Demarrer-Frida' sur le Bureau."
Write-Host "  - Pour arreter FRIDA (liberer la RAM) : 'Arreter-Frida' sur le Bureau."
Write-Host ""
# Fiche d'identifiants deposee a cote de l'installation : le notaire ne peut pas
# deviner le mot de passe, et il n'est affiche qu'une fois a la creation du compte.
$credPath = Join-Path $targetPath "IDENTIFIANTS.txt"
$credContent = @"
FRIDA - Identifiants de connexion
=================================

Adresse    : $appUrl
Utilisateur: $adminUser
Mot de passe: $adminPass

Ce mot de passe est propre a ce poste. Conservez ce fichier en lieu sur
et ne le diffusez pas : il donne acces a tous les dossiers de succession.

Il est egalement stocke dans le fichier .env de ce dossier.
"@
Set-Content -Path $credPath -Value $credContent -Encoding UTF8

Write-Host ""
Write-Host "  CONNEXION A FRIDA" -ForegroundColor Yellow
Write-Host "    Utilisateur  : $adminUser"
Write-Host "    Mot de passe : $adminPass"
Write-Host "    (egalement dans $credPath)"
Write-Host ""
Write-Host "  Vos donnees sont dans : $targetPath\data\"
Write-Host "  Sauvegarde / restauration / desinstallation : sauvegarder.bat,"
Write-Host "  restaurer.bat et desinstaller.bat"
Write-Host "  dans $targetPath"
Write-Host ""
Show-Popup "Installation terminee.`n`nConnectez-vous a FRIDA avec :`n`n   Utilisateur  : $adminUser`n   Mot de passe : $adminPass`n`nCes identifiants sont aussi dans le fichier :`n$credPath`n`nConservez-les : le mot de passe est propre a ce poste." "FRIDA - Vos identifiants de connexion"

Start-Sleep -Seconds 3
Start-Process $appUrl
