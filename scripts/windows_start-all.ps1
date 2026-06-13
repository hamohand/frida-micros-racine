$ErrorActionPreference = "Stop"

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host "  🚀 FridaAI - Démarrage Complet (Docker + OCR Natif)" -ForegroundColor Cyan
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host ""

# Vérifier si le fichier .env existe, sinon le créer
if (-Not (Test-Path ".env")) {
    Write-Host "⚠️ Le fichier .env n'existe pas." -ForegroundColor Yellow
    Write-Host "📝 Création de .env à partir de .env.example..." -ForegroundColor Green
    Copy-Item ".env.example" -Destination ".env"
    Write-Host "✅ Fichier .env créé. Merci de vérifier/modifier les valeurs sensibles si nécessaire." -ForegroundColor Green
    Write-Host ""
}

# Vérifier et créer le dossier d'uploads si nécessaire
if (-Not (Test-Path "backend\uploads")) {
    Write-Host "📁 Création du dossier backend\uploads..." -ForegroundColor Green
    New-Item -ItemType Directory -Force -Path "backend\uploads" | Out-Null
}

Write-Host "[1/2] Démarrage des services Docker..." -ForegroundColor Cyan
Write-Host "(Base de données, Backend Spring, Frontend Angular, Calculs API)" -ForegroundColor Gray
docker-compose up -d

Write-Host ""
Write-Host "[2/2] Démarrage du micro-service OCR en local..." -ForegroundColor Cyan
Write-Host "(S'exécute dans une nouvelle fenêtre car l'OCR nécessite un accès natif Windows)" -ForegroundColor Gray

# On ouvre une nouvelle fenêtre PowerShell pour exécuter l'environnement virtuel Python
$ocrCmd = "cd ..\easytess_ocr_api\backend\app_ocr; & '..\..\.venv\Scripts\python.exe' run.py"
Start-Process powershell -ArgumentList "-NoExit", "-Command", $ocrCmd -WindowStyle Normal

Write-Host ""
Write-Host "==============================================================" -ForegroundColor Green
Write-Host "  ✅ Application en cours de démarrage !" -ForegroundColor Green
Write-Host "==============================================================" -ForegroundColor Green
Write-Host "Le backend peut prendre quelques instants pour être complètement disponible."
Write-Host ""
Write-Host "🌐 URLs d'accès :" -ForegroundColor Yellow
Write-Host "   Frontend Angular : http://localhost:4200"
Write-Host "   Backend Spring   : http://localhost:8080"
Write-Host "   Swagger UI       : http://localhost:8080/swagger-ui.html"
Write-Host "   Service OCR      : http://localhost:8082 (Dans l'autre fenêtre)"
Write-Host "   Service Calculs  : http://localhost:8081"
Write-Host ""
Write-Host "📝 Commandes utiles :" -ForegroundColor Yellow
Write-Host "   Arrêter Docker   : docker-compose down"
Write-Host "   Arrêter l'OCR    : Fermer la fenêtre de commande OCR"
Write-Host "   Voir les logs    : docker-compose logs -f"
Write-Host "==============================================================" -ForegroundColor Cyan

Read-Host "Appuyez sur Entrée pour quitter..."
