@echo off
setlocal

:: Aller à la racine du projet
cd /d "%~dp0\.."

if "%~1"=="" (
    echo Utilisation : scripts\restore_db.bat chemin\vers\fichier_backup.dump
    exit /b 1
)

set BACKUP_FILE=%~1

if not exist "%BACKUP_FILE%" (
    echo Le fichier %BACKUP_FILE% n'existe pas.
    exit /b 1
)

echo Restauration de la base de donnees a partir de %BACKUP_FILE%...
echo ATTENTION : Cette operation va effacer les donnees actuelles de la base.
pause

:: Restauration avec pg_restore. -c nettoie les anciens objets, -1 fait la restauration dans une seule transaction.
docker compose exec -T db sh -c "pg_restore -U $POSTGRES_USER -d $POSTGRES_DB -c -1" < "%BACKUP_FILE%"

if %ERRORLEVEL% equ 0 (
    echo.
    echo Restauration reussie !
) else (
    echo.
    echo Echec de la restauration !
)
endlocal
