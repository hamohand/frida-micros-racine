@echo off
:: ============================================================
::   FRIDA - Installation locale (Docker dans WSL Ubuntu)
::   Double-cliquez pour lancer. Elevation Administrateur requise.
:: ============================================================

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
echo Lancement de l'installation de FRIDA...
PowerShell -NoProfile -ExecutionPolicy Bypass -File "%~dp0Installer-Frida.ps1"
pause
