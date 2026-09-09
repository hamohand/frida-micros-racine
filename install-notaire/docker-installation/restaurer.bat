@echo off
chcp 65001 >nul
title FRIDA — Restauration
setlocal enabledelayedexpansion

echo.
echo ╔══════════════════════════════════════════════════╗
echo ║        FRIDA — Restauration d'une sauvegarde     ║
echo ╚══════════════════════════════════════════════════╝
echo.

set "FRIDA_DIR=%USERPROFILE%\Frida-Micros"
if not exist "%FRIDA_DIR%\docker-compose.local.yml" (
    echo    ERREUR : installation introuvable dans "%FRIDA_DIR%".
    pause
    exit /b 1
)

set "LINUX_DIR="
for /f "delims=" %%p in ('wsl -d Ubuntu -e wslpath -a "%FRIDA_DIR%" 2^>nul') do set "LINUX_DIR=%%p"
if not defined LINUX_DIR (
    echo    ERREUR : WSL Ubuntu ne répond pas. Démarrez FRIDA puis réessayez.
    pause
    exit /b 1
)

echo Sauvegardes disponibles dans data\backups :
echo.
set "COUNT=0"
for /d %%d in ("%FRIDA_DIR%\data\backups\*") do (
    if exist "%%d\database.sql" (
        set /a COUNT+=1
        echo    %%~nxd
    )
)
if "%COUNT%"=="0" (
    echo    Aucune sauvegarde trouvée.
    pause
    exit /b 1
)

echo.
set "CHOIX="
set /p CHOIX="Nom exact du dossier à restaurer : "
set "SRC=%FRIDA_DIR%\data\backups\%CHOIX%"
if not exist "%SRC%\database.sql" (
    echo    ERREUR : "%SRC%\database.sql" introuvable.
    pause
    exit /b 1
)

echo.
echo   ATTENTION : la base actuelle sera REMPLACÉE par cette sauvegarde.
set "CONFIRM="
set /p CONFIRM="Confirmez-vous ? (O/N) : "
if /i not "%CONFIRM%"=="O" (
    echo    Restauration annulée.
    pause
    exit /b 0
)

set "DB_USER=frida"
set "DB_NAME=frida_db"
for /f "usebackq tokens=1,2 delims==" %%a in ("%FRIDA_DIR%\.env") do (
    if /i "%%a"=="DB_USER" set "DB_USER=%%b"
    if /i "%%a"=="DB_NAME" set "DB_NAME=%%b"
)

echo.
echo [1/3] Démarrage de la base...
wsl -u root -d Ubuntu -e bash -c "cd '%LINUX_DIR%' && docker compose -f docker-compose.local.yml --env-file .env up -d db"
if errorlevel 1 (
    echo    ERREUR : impossible de démarrer la base.
    pause
    exit /b 1
)

echo [2/3] Restauration de la base de données...
wsl -u root -d Ubuntu -e bash -c "docker exec -i frida-db psql -U '%DB_USER%' -d postgres -c \"DROP DATABASE IF EXISTS %DB_NAME%;\" && docker exec -i frida-db psql -U '%DB_USER%' -d postgres -c \"CREATE DATABASE %DB_NAME%;\" && docker exec -i frida-db psql -U '%DB_USER%' -d '%DB_NAME%' < '%LINUX_DIR%/data/backups/%CHOIX%/database.sql'"
if errorlevel 1 (
    echo    ERREUR : la restauration a échoué.
    pause
    exit /b 1
)
echo    Base restaurée.

echo [3/3] Restauration des documents...
if exist "%SRC%\uploads" (
    xcopy "%SRC%\uploads" "%FRIDA_DIR%\data\uploads\" /E /I /Q /Y >nul
    echo    Documents restaurés.
) else (
    echo    Aucun document dans cette sauvegarde.
)

echo.
echo ╔══════════════════════════════════════════════════╗
echo ║   Restauration terminée.                         ║
echo ║   Relancez "Demarrer-Frida" sur le Bureau.       ║
echo ╚══════════════════════════════════════════════════╝
echo.
pause
