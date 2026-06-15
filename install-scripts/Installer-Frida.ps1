# Script PowerShell pour installer et lancer Frida-Micros via WSL

Write-Host "========================================="
Write-Host "   Installation de Frida-Micros (WSL)    "
Write-Host "========================================="
Write-Host ""

# 1. Vérification de WSL
Write-Host "[1/4] Vérification de WSL..."
try {
    $wslStatus = wsl --status 2>&1
    if ($LASTEXITCODE -ne 0 -or $wslStatus -match "n'a aucune distribution installée" -or $wslStatus -match "has no installed distributions") {
        Write-Host "WSL n'est pas installé ou aucune distribution Linux n'est présente."
        Write-Host "Lancement de l'installation de WSL avec Ubuntu..."
        wsl --install -d Ubuntu
        Write-Host ""
        Write-Host "IMPORTANT : Windows va vous demander de redémarrer votre ordinateur."
        Write-Host "Après le redémarrage, une fenêtre noire (Ubuntu) s'ouvrira pour créer un nom d'utilisateur et un mot de passe."
        Write-Host "Une fois cela terminé, RELANCEZ le fichier Installer-Frida.bat pour continuer l'installation."
        pause
        exit
    }
} catch {
    Write-Host "Lancement de l'installation de WSL avec Ubuntu..."
    wsl --install -d Ubuntu
    Write-Host "Veuillez redémarrer puis relancer ce script."
    pause
    exit
}

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

# Installation
$wslCmd = "sudo apt update && sudo apt install -y openjdk-17-jdk maven curl && curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash - && sudo apt-get install -y nodejs"
wsl -d Ubuntu -e bash -c $wslCmd

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
start http://localhost:4200

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
