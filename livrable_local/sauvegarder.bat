@echo off
chcp 65001 >nul
title FRIDA — Sauvegarde

echo.
echo ╔══════════════════════════════════════════════════╗
echo ║          FRIDA — Sauvegarde manuelle             ║
echo ╚══════════════════════════════════════════════════╝
echo.

REM ---- Générer le nom du fichier de sauvegarde ----
for /f "tokens=1-3 delims=/ " %%a in ("%date%") do set DATESTAMP=%%c%%b%%a
for /f "tokens=1-2 delims=:." %%a in ("%time: =0%") do set TIMESTAMP=%%a%%b
set BACKUP_NAME=frida_backup_%DATESTAMP%_%TIMESTAMP%

REM ---- Créer le dossier de sauvegarde ----
if not exist "data\backups" mkdir "data\backups"
set BACKUP_DIR=data\backups\%BACKUP_NAME%
mkdir "%BACKUP_DIR%"

echo [1/2] Sauvegarde de la base de données...
docker exec frida-db pg_dump -U frida frida_db > "%BACKUP_DIR%\database.sql" 2>nul
if %errorlevel% neq 0 (
    echo    ERREUR : Impossible de sauvegarder la base de données.
    echo    Vérifiez que Frida est en cours d'exécution.
    pause
    exit /b 1
)
echo    Base de données sauvegardée.

echo [2/2] Sauvegarde des documents...
if exist "data\uploads" (
    xcopy "data\uploads" "%BACKUP_DIR%\uploads\" /E /I /Q >nul
    echo    Documents sauvegardés.
) else (
    echo    Aucun document à sauvegarder.
)

echo.
echo ╔══════════════════════════════════════════════════╗
echo ║   Sauvegarde terminée !                          ║
echo ║                                                  ║
echo ║   Emplacement : %BACKUP_DIR%
echo ║                                                  ║
echo ║   Conseil : Copiez ce dossier sur un disque      ║
echo ║   externe ou une clé USB pour plus de sécurité.  ║
echo ╚══════════════════════════════════════════════════╝
echo.
pause
