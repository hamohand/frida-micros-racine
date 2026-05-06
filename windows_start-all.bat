@echo off
chcp 65001 >nul
echo ==============================================================
echo   🚀 FridaAI - Démarrage Complet (Docker + OCR Natif)
echo ==============================================================
echo.

:: Vérifier si le fichier .env existe, sinon le créer
if not exist .env (
    echo ⚠️ Le fichier .env n'existe pas.
    echo 📝 Création de .env a partir de .env.example...
    copy .env.example .env
    echo ✅ Fichier .env cree. Merci de verifier/modifier les valeurs sensibles si necessaire.
    echo.
)

:: Vérifier et créer le dossier d'uploads si nécessaire
if not exist backend\uploads (
    echo 📁 Création du dossier backend\uploads...
    mkdir backend\uploads
)

echo [1/2] Démarrage des services Docker...
echo (Base de donnees, Backend Spring, Frontend Angular, Calculs API)
docker-compose up -d

echo.
echo [2/2] Démarrage du micro-service OCR en local...
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
echo    Frontend Angular : http://localhost:4200
echo    Backend Spring   : http://localhost:8080
echo    Swagger UI       : http://localhost:8080/swagger-ui.html
echo    Service OCR      : http://localhost:8082 (Dans l'autre fenetre)
echo    Service Calculs  : http://localhost:8081
echo.
echo 📝 Commandes utiles :
echo    Arreter Docker   : docker-compose down
echo    Arreter l'OCR    : Fermer la fenetre de commande OCR
echo    Voir les logs    : docker-compose logs -f
echo ==============================================================
pause
