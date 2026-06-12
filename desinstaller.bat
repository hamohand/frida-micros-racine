@echo off
chcp 65001 >nul
title FRIDA — Désinstallation

echo.
echo ╔══════════════════════════════════════════════════╗
echo ║         FRIDA — Désinstallation                  ║
echo ╚══════════════════════════════════════════════════╝
echo.

echo Cette opération va arrêter Frida et supprimer les conteneurs Docker.
echo.
set /p KEEP_DATA="Voulez-vous CONSERVER vos données (dossiers, base de données) ? (O/N) : "
echo.

echo Arrêt des services...
docker compose -f docker-compose.local.yml down
echo    Services arrêtés.

if /i "%KEEP_DATA%"=="N" (
    echo.
    echo Suppression des données...
    if exist "data" rmdir /s /q "data"
    echo    Données supprimées.
) else (
    echo.
    echo    Vos données ont été conservées dans le dossier "data\".
)

echo.
echo ╔══════════════════════════════════════════════════╗
echo ║   Frida a été désinstallé.                       ║
echo ║                                                  ║
echo ║   Pour réinstaller, relancez installer.bat       ║
echo ╚══════════════════════════════════════════════════╝
echo.
pause
