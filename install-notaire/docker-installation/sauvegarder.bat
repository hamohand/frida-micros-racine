@echo off
chcp 65001 >nul
title FRIDA — Sauvegarde
setlocal enabledelayedexpansion

echo.
echo ╔══════════════════════════════════════════════════╗
echo ║          FRIDA — Sauvegarde manuelle             ║
echo ╚══════════════════════════════════════════════════╝
echo.

REM ---- Dossier d'installation (les donnees ne sont PAS a cote de ce script) ----
set "FRIDA_DIR=%USERPROFILE%\Frida-Micros"
if not exist "%FRIDA_DIR%\docker-compose.local.yml" (
    echo    ERREUR : installation introuvable dans "%FRIDA_DIR%".
    echo    Lancez d'abord Installer-Frida.bat.
    pause
    exit /b 1
)

REM ---- Chemin equivalent vu depuis WSL (Docker tourne dans WSL, pas sous Windows) ----
set "LINUX_DIR="
for /f "delims=" %%p in ('wsl -d Ubuntu -e wslpath -a "%FRIDA_DIR%" 2^>nul') do set "LINUX_DIR=%%p"
if not defined LINUX_DIR (
    echo    ERREUR : WSL Ubuntu ne repond pas.
    echo    Demarrez FRIDA ^(raccourci "Demarrer-Frida" sur le Bureau^) puis reessayez.
    pause
    exit /b 1
)

REM ---- Identifiants de la base, lus depuis le .env ----
set "DB_USER=frida"
set "DB_NAME=frida_db"
for /f "usebackq tokens=1,2 delims==" %%a in ("%FRIDA_DIR%\.env") do (
    if /i "%%a"=="DB_USER" set "DB_USER=%%b"
    if /i "%%a"=="DB_NAME" set "DB_NAME=%%b"
)

REM ---- Nom horodate de la sauvegarde ----
for /f "tokens=1-3 delims=/ " %%a in ("%date%") do set "DATESTAMP=%%c%%b%%a"
for /f "tokens=1-2 delims=:." %%a in ("%time: =0%") do set "TIMESTAMP=%%a%%b"
set "BACKUP_NAME=frida_backup_%DATESTAMP%_%TIMESTAMP%"
set "BACKUP_DIR=%FRIDA_DIR%\data\backups\%BACKUP_NAME%"

if not exist "%FRIDA_DIR%\data\backups" mkdir "%FRIDA_DIR%\data\backups"
mkdir "%BACKUP_DIR%"

echo [1/2] Sauvegarde de la base de données...
wsl -u root -d Ubuntu -e bash -c "docker exec frida-db pg_dump -U '%DB_USER%' '%DB_NAME%' > '%LINUX_DIR%/data/backups/%BACKUP_NAME%/database.sql'"
if errorlevel 1 (
    echo    ERREUR : impossible de sauvegarder la base de données.
    echo    Vérifiez que FRIDA est démarré ^(raccourci "Demarrer-Frida"^).
    pause
    exit /b 1
)
echo    Base de données sauvegardée.

echo [2/2] Sauvegarde des documents...
if exist "%FRIDA_DIR%\data\uploads" (
    xcopy "%FRIDA_DIR%\data\uploads" "%BACKUP_DIR%\uploads\" /E /I /Q >nul
    echo    Documents sauvegardés.
) else (
    echo    Aucun document à sauvegarder.
)

echo.
echo ╔══════════════════════════════════════════════════╗
echo ║   Sauvegarde terminée !                          ║
echo ╚══════════════════════════════════════════════════╝
echo.
echo   Emplacement : %BACKUP_DIR%
echo.
echo   Conseil : copiez ce dossier sur un disque externe
echo   ou une clé USB pour plus de sécurité.
echo.
pause
