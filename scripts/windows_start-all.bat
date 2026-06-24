@echo off
chcp 65001 >nul
echo ==============================================================
echo   🚀 FridaAI - Démarrage Complet (Docker + OCR Natif)
echo ==============================================================
echo.

:: Se placer à la racine du projet
cd /d "%~dp0\.."


:: Vérifier si le fichier .env existe, sinon le créer
if not exist .env (
    echo ⚠️ Le fichier .env n'existe pas.
    echo 📝 Création de .env a partir de .env.example...
    copy .env.example .env
    echo ✅ Fichier .env cree. Merci de verifier/modifier les valeurs sensibles si necessaire.
    echo.
)

:: Charger les ports depuis le fichier .env
set FRONT_PORT=4202
set BACK_PORT=8080
FOR /F "tokens=1,2 delims==" %%A IN (.env) DO (
    if "%%A"=="FRONTEND_PORT" set FRONT_PORT=%%B
    if "%%A"=="BACKEND_PORT" set BACK_PORT=%%B
)

:: Vérifier et créer le dossier d'uploads si nécessaire
if not exist backend\uploads (
    echo 📁 Création du dossier backend\uploads...
    mkdir backend\uploads
)

echo [1/3] Arret des conteneurs existants...
docker-compose down

echo.
echo [2/3] Nettoyage des processus fantomes (Ghosting) sur les ports %FRONT_PORT%, %BACK_PORT%, 8082...
FOR %%p IN (%FRONT_PORT% %BACK_PORT% 8082) DO (
    FOR /F "tokens=5" %%a IN ('netstat -aon ^| findstr ":%%p " ^| findstr "LISTENING"') DO (
        if "%%a" neq "0" (
            echo ⚠️  Processus fantome (ex: wslrelay.exe ou node.exe) detecte sur le port %%p ^(PID: %%a^). Fermeture forcee...
            taskkill /F /PID %%a >nul 2>&1
        )
    )
)

echo.
echo [3/3] Démarrage des services Docker...
echo (Base de donnees, Backend Spring, Frontend Angular, Calculs API)
docker-compose up -d

echo.
echo Démarrage du micro-service OCR en local...
echo (S'exécute dans une nouvelle fenêtre car l'OCR nécessite un accès natif Windows)
:: On ouvre une nouvelle fenêtre pour exécuter l'environnement virtuel Python
start "Frida OCR Service (Port 8082)" cmd /k "cd ..\easytess_ocr_api\backend\app_ocr && ..\..\.venv\Scripts\python.exe run.py"

echo.
echo ==============================================================
echo   ✅ Application en cours de démarrage !
echo ==============================================================
echo Le backend peut prendre quelques instants pour etre completement disponible.
echo.
echo 🌐 URLs d'acces :
echo    Frontend Angular : http://localhost:%FRONT_PORT%
echo    Backend Spring   : http://localhost:%BACK_PORT%
echo    Swagger UI       : http://localhost:%BACK_PORT%/swagger-ui.html
echo    Service OCR      : http://localhost:8082 (Dans l'autre fenetre)
echo.
echo 📝 Commandes utiles :
echo    Arreter Docker   : docker-compose down
echo    Arreter l'OCR    : Fermer la fenetre de commande OCR
echo    Voir les logs    : docker-compose logs -f
echo ==============================================================
pause
