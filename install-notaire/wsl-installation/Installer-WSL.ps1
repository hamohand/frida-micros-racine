# Script PowerShell pour installer et lancer Frida-Micros via WSL

Write-Host "========================================="
Write-Host "   Installation de Frida-Micros (WSL)    "
Write-Host "========================================="
Write-Host ""

$OutputEncoding = [Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# 1. Vérification de WSL
Write-Host "[1/4] Vérification de l'environnement Linux..."

$ubuntuReady = $false
try {
    $testCmd = wsl -d Ubuntu -e echo "ready" 2>&1
    if ($testCmd -match "ready") {
        $ubuntuReady = $true
    }
} catch {}

if (-not $ubuntuReady) {
    Write-Host "Ubuntu n'est pas prêt ou n'est pas installé."
    
    # On vérifie s'il est au moins dans la liste (non initialisé)
    # wsl -l -q sort de l'UTF16 sous Windows, on convertit
    $list = wsl -l -q 2>&1
    $listStr = ""
    if ($list) {
        $listStr = $list -join " "
    }
    
    if ($listStr -match "Ubuntu") {
        Write-Host ""
        Write-Host "ATTENTION : Ubuntu est installé mais son initialisation n'est pas terminée !"
        Write-Host "Action requise pour le notaire :"
        Write-Host "1. Ouvrez le menu Démarrer de Windows."
        Write-Host "2. Cherchez l'application 'Ubuntu' et lancez-la."
        Write-Host "3. Attendez la fin de l'installation et créez votre nom d'utilisateur UNIX."
        Write-Host "4. Une fois terminé, fermez la fenêtre Ubuntu et relancez ce script."
        Write-Host ""
        pause
        exit
    } else {
        Write-Host "Lancement de l'installation de WSL avec Ubuntu..."
        wsl --install -d Ubuntu
        Write-Host ""
        Write-Host "IMPORTANT : L'installation a démarré."
        Write-Host "Windows peut vous demander de redémarrer votre ordinateur."
        Write-Host "Après le redémarrage, une fenêtre noire (Ubuntu) s'ouvrira pour créer votre profil."
        Write-Host "Une fois l'utilisateur créé, fermez la fenêtre Ubuntu et RELANCEZ ce script."
        Write-Host ""
        pause
        exit
    }
}
Write-Host "Environnement Linux (Ubuntu) détecté et prêt !"

# 2. Décompression de l'archive
Write-Host "[2/4] Extraction des fichiers..."
$zipPath = Join-Path -Path $PSScriptRoot -ChildPath "frida-micros.zip"
$targetPath = Join-Path -Path $env:USERPROFILE -ChildPath "Frida-Micros"

if (Test-Path $targetPath) {
    Write-Host "Dossier Frida-Micros déjà existant dans votre dossier Utilisateur."
} else {
    if (Test-Path $zipPath) {
        Write-Host "Décompression de frida-micros.zip vers $targetPath ..."
        Expand-Archive -Path $zipPath -DestinationPath $targetPath -Force
    } else {
        Write-Host "ERREUR : Fichier frida-micros.zip introuvable !"
        Write-Host "Veuillez placer l'archive frida-micros.zip dans le même dossier que ce script."
        pause
        exit
    }
}

# 3. Installation des dépendances Linux (Java, Node, Maven)
Write-Host "[3/4] Configuration de l'environnement Linux (Ceci peut prendre quelques minutes)..."
Write-Host "Il se peut qu'Ubuntu vous demande votre mot de passe (celui créé lors de la première installation de WSL)."

# Pour accéder au dossier Windows depuis WSL : /mnt/c/Users/NomUtilisateur/Frida-Micros
$linuxPath = "/mnt/c/Users/$env:USERNAME/Frida-Micros".Replace('\','/')

# Installation en tant que root pour éviter les demandes de mot de passe sudo qui bloquent le script
$wslCmd = "apt update && apt install -y openjdk-17-jdk maven curl && curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && apt-get install -y nodejs"
wsl -u root -d Ubuntu -e bash -c $wslCmd

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "=========================================================="
    Write-Host " ERREUR CRITIQUE LORS DU TELECHARGEMENT DES COMPOSANTS !"
    Write-Host "=========================================================="
    Write-Host "L'installation a été bloquée car Linux n'a pas pu télécharger Java ou Node.js."
    Write-Host "Cela arrive souvent si votre connexion Internet est instable, ou si vous êtes"
    Write-Host "sur un réseau public/4G qui demande de vous connecter (Portail Captif)."
    Write-Host "Veuillez vérifier votre connexion Internet et relancer l'installation."
    pause
    exit
}

Write-Host "Installation des dépendances Node.js du frontend..."
$wslNpmCmd = "cd '$linuxPath/frontend' && npm install"
wsl -d Ubuntu -e bash -c $wslNpmCmd

# 4. Création du lanceur sur le bureau
Write-Host "[4/4] Création du raccourci sur le bureau..."
$desktopPath = [Environment]::GetFolderPath("Desktop")
$launcherPath = Join-Path -Path $desktopPath -ChildPath "Demarrer-Frida.bat"

$launcherContent = @"
@echo off
echo ==============================================
echo        Demarrage de Frida-Micros...
echo ==============================================
echo.
echo Veuillez patienter pendant que les serveurs demarrent (environ 20 secondes)...

:: Lancement du backend (Java/Spring Boot) en arriere-plan
start /b wsl -d Ubuntu -e bash -c "cd '$linuxPath/backend' && mvn spring-boot:run"

:: Lancement du frontend (Angular) en arriere-plan
start /b wsl -d Ubuntu -e bash -c "cd '$linuxPath/frontend' && npm start"

:: Attente que le serveur frontend soit pret
timeout /t 20 /nobreak >nul

:: Ouverture du navigateur
start http://localhost:4201

echo Application lancee dans votre navigateur !
echo Laissez cette fenetre ouverte pour que l'application continue de fonctionner.
"@

Set-Content -Path $launcherPath -Value $launcherContent
Write-Host "Raccourci 'Demarrer-Frida.bat' créé sur le bureau."

Write-Host ""
Write-Host "========================================="
Write-Host " INSTALLATION TERMINEE AVEC SUCCES !"
Write-Host "========================================="
Write-Host "Vous pouvez maintenant double-cliquer sur 'Demarrer-Frida' sur votre Bureau."
pause
