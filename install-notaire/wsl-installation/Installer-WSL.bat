@echo off
:: Verifie les droits d'administration
NET SESSION >nul 2>&1
if %errorLevel% == 0 (
    echo Droits administrateur confirmes.
) else (
    echo Ce script necessite les droits d'administration pour installer WSL.
    echo Relance en tant qu'administrateur...
    PowerShell -Command "Start-Process '%~dpnx0' -Verb RunAs"
    exit /b
)

:: Lancement du script PowerShell principal
echo Lancement de l'installation de Frida-Micros...
PowerShell -NoProfile -ExecutionPolicy Bypass -File "%~dp0Installer-WSL.ps1"
pause
