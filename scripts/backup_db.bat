@echo off
setlocal

:: Aller à la racine du projet
cd /d "%~dp0\.."

set BACKUP_DIR=db_backups
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

:: Obtenir la date et l'heure actuelles (format: AAAA-MM-JJ_HH-mm-ss)
for /f %%I in ('powershell -Command "Get-Date -format 'yyyy-MM-dd_HH-mm-ss'"') do set datetime=%%I
set BACKUP_FILE=%BACKUP_DIR%\backup_%datetime%.dump

echo Demarrage de la sauvegarde de la base de donnees...
:: Utilisation de pg_dump au format custom (-F c)
docker compose exec -T db sh -c "pg_dump -U $POSTGRES_USER -d $POSTGRES_DB -F c" > "%BACKUP_FILE%"

if %ERRORLEVEL% equ 0 (
    echo.
    echo Sauvegarde reussie : %BACKUP_FILE%
    echo Vous pouvez utiliser scripts\restore_db.bat pour la restaurer.
) else (
    echo.
    echo Echec de la sauvegarde !
    if exist "%BACKUP_FILE%" del "%BACKUP_FILE%"
)
endlocal
