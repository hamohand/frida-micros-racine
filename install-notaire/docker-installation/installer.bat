@echo off
chcp 65001 >nul
title FRIDA — Installation

echo.
echo ╔══════════════════════════════════════════════════╗
echo ║          FRIDA — Installation locale             ║
echo ║     Gestion de successions notariales            ║
echo ╚══════════════════════════════════════════════════╝
echo.

REM ---- Vérification de Docker ----
echo [1/4] Vérification de Docker...
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo ╔══════════════════════════════════════════════════╗
    echo ║  ERREUR : Docker n'est pas installé.            ║
    echo ║                                                  ║
    echo ║  Veuillez installer Docker Desktop :             ║
    echo ║  https://www.docker.com/products/docker-desktop/ ║
    echo ║                                                  ║
    echo ║  Puis relancez ce script.                        ║
    echo ╚══════════════════════════════════════════════════╝
    echo.
    pause
    exit /b 1
)

docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo ╔══════════════════════════════════════════════════╗
    echo ║  ERREUR : Docker n'est pas démarré.             ║
    echo ║                                                  ║
    echo ║  Veuillez lancer Docker Desktop et attendre      ║
    echo ║  que l'icône soit stable (baleine verte),        ║
    echo ║  puis relancez ce script.                        ║
    echo ╚══════════════════════════════════════════════════╝
    echo.
    pause
    exit /b 1
)
echo    Docker OK.

REM ---- Vérification de la mémoire ----
echo [2/4] Vérification de la configuration système...
for /f "tokens=2 delims==" %%a in ('wmic os get TotalVisibleMemorySize /value') do set RAM_KB=%%a
set /a RAM_GB=%RAM_KB% / 1048576
if %RAM_GB% lss 7 (
    echo.
    echo ╔══════════════════════════════════════════════════╗
    echo ║  ATTENTION : Votre machine a %RAM_GB% Go de RAM.    ║
    echo ║  Frida nécessite au minimum 8 Go de RAM.        ║
    echo ║  L'application risque d'être très lente.        ║
    echo ╚══════════════════════════════════════════════════╝
    echo.
    set /p CONTINUE="Voulez-vous continuer quand même ? (O/N) : "
    if /i not "%CONTINUE%"=="O" exit /b 1
) else (
    echo    RAM : %RAM_GB% Go — OK.
)

REM ---- Copie du fichier .env ----
echo [3/4] Préparation de la configuration...
if not exist ".env" (
    copy ".env.local" ".env" >nul
    echo    Fichier de configuration créé.
) else (
    echo    Configuration existante conservée.
)

REM ---- Création des dossiers de données ----
if not exist "data\uploads" mkdir "data\uploads"
if not exist "data\postgres" mkdir "data\postgres"
if not exist "data\backups" mkdir "data\backups"
echo    Dossiers de données créés.

REM ---- Construction et lancement ----
echo [4/4] Construction et démarrage de Frida...
echo    (Cette étape peut prendre 5 à 15 minutes la première fois)
echo.

docker compose -f docker-compose.local.yml --env-file .env build
if %errorlevel% neq 0 (
    echo.
    echo ERREUR lors de la construction. Consultez les messages ci-dessus.
    pause
    exit /b 1
)

docker compose -f docker-compose.local.yml --env-file .env up -d
if %errorlevel% neq 0 (
    echo.
    echo ERREUR lors du démarrage. Consultez les messages ci-dessus.
    pause
    exit /b 1
)

echo.
echo ╔══════════════════════════════════════════════════╗
echo ║                                                  ║
echo ║   FRIDA est installé et en cours d'exécution !   ║
echo ║                                                  ║
echo ║   Ouvrez votre navigateur à l'adresse :          ║
echo ║   http://localhost                                ║
echo ║                                                  ║
echo ║   (Attendez ~30 secondes le temps que tous       ║
echo ║    les services démarrent)                        ║
echo ║                                                  ║
echo ╚══════════════════════════════════════════════════╝
echo.

REM ---- Ouvrir le navigateur automatiquement après 10 secondes ----
echo Ouverture du navigateur dans 10 secondes...
timeout /t 10 /nobreak >nul
start http://localhost

pause
