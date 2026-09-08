@echo off
chcp 65001 >nul
title FRIDA — Désinstallation
setlocal enabledelayedexpansion

echo.
echo ╔══════════════════════════════════════════════════╗
echo ║         FRIDA — Désinstallation                  ║
echo ╚══════════════════════════════════════════════════╝
echo.
echo   ATTENTION : ceci DÉSINSTALLE FRIDA.
echo   Pour simplement arrêter FRIDA, fermez cette fenêtre et
echo   utilisez le raccourci "Arreter-Frida" sur le Bureau.
echo.

set "FRIDA_DIR=%USERPROFILE%\Frida-Micros"
if not exist "%FRIDA_DIR%\docker-compose.local.yml" (
    echo    Aucune installation trouvée dans "%FRIDA_DIR%". Rien à faire.
    pause
    exit /b 0
)

set "LINUX_DIR="
for /f "delims=" %%p in ('wsl -d Ubuntu -e wslpath -a "%FRIDA_DIR%" 2^>nul') do set "LINUX_DIR=%%p"
if not defined LINUX_DIR (
    echo    ERREUR : WSL Ubuntu ne répond pas. Impossible d'arrêter les services.
    pause
    exit /b 1
)

set "CONFIRM="
set /p CONFIRM="Confirmez-vous la désinstallation ? (O/N) : "
if /i not "%CONFIRM%"=="O" (
    echo.
    echo    Désinstallation annulée. Rien n'a été modifié.
    pause
    exit /b 0
)

set "KEEP_DATA="
echo.
set /p KEEP_DATA="Voulez-vous CONSERVER vos données (dossiers, base de données) ? (O/N) : "
echo.

echo Arrêt des services...
wsl -u root -d Ubuntu -e bash -c "cd '%LINUX_DIR%' && docker compose -f docker-compose.local.yml --env-file .env down"
if errorlevel 1 (
    echo    AVERTISSEMENT : l'arrêt des conteneurs a échoué ^(déjà arrêtés ?^).
) else (
    echo    Services arrêtés.
)

if /i "%KEEP_DATA%"=="N" (
    echo.
    echo Suppression des données...
    wsl -u root -d Ubuntu -e bash -c "rm -rf '%LINUX_DIR%/data'"
    if exist "%FRIDA_DIR%\data" rmdir /s /q "%FRIDA_DIR%\data"
    echo    Données supprimées.
) else (
    echo.
    echo    Vos données ont été conservées dans "%FRIDA_DIR%\data\".
)

echo.
echo ╔══════════════════════════════════════════════════╗
echo ║   FRIDA a été désinstallé.                       ║
echo ║                                                  ║
echo ║   Pour réinstaller, relancez Installer-Frida.bat ║
echo ╚══════════════════════════════════════════════════╝
echo.
pause
