#!/bin/bash

# Script de démarrage de FridaAI en MODE DEMO
# Utilisation: ./start.demo.sh

set -e

echo "======================================"
echo "🚀 FridaAI - Démarrage Application (MODE DEMO)"
echo "======================================"
echo ""

# Vérifier si Docker est installé
if ! command -v docker &> /dev/null; then
    echo "❌ Docker n'est pas installé. Merci d'installer Docker."
    exit 1
fi

# Vérifier si Docker Compose est installé
if ! command -v docker compose &> /dev/null; then
    echo "❌ Docker Compose n'est pas installé. Merci d'installer Docker Compose."
    exit 1
fi

# Créer .env s'il n'existe pas
if [ ! -f .env ]; then
    echo "⚠️  Le fichier .env n'existe pas."
    echo "📝 Création de .env à partir de .env.example..."
    cp .env.example .env
    echo "✅ Fichier .env créé. Merci de modifier les valeurs sensibles si nécessaire."
fi

# Créer le dossier uploads s'il n'existe pas
if [ ! -d "backend/uploads" ]; then
    echo "📁 Création du dossier backend/uploads..."
    mkdir -p backend/uploads
fi

# Afficher les versions
echo "📋 Vérifications des versions:"
docker --version
docker compose --version
echo ""

# ----- HYBRID ENVIRONMENT (WSL to Windows) -----
if grep -qi microsoft /proc/version; then
    echo "🐧 Environnement WSL détecté. Configuration de la connexion avec l'hôte Windows..."
    
    # 1. Obtenir l'IP de l'hôte Windows depuis WSL
    WIN_IP=$(ip route show default | awk '{print $3}')
    echo "🌐 IP de l'hôte Windows détectée : $WIN_IP"
    
    # 2. Mettre à jour ou ajouter WINDOWS_HOST_IP dans .env
    if grep -q "WINDOWS_HOST_IP=" .env; then
        sed -i "s/^WINDOWS_HOST_IP=.*/WINDOWS_HOST_IP=$WIN_IP/" .env
    else
        echo "WINDOWS_HOST_IP=$WIN_IP" >> .env
    fi
    echo "✅ IP $WIN_IP configurée dans .env"
    
    # 3. Lancer l'OCR côté Windows via powershell.exe
    echo "🐍 Démarrage du micro-service OCR natif (Python) sur Windows..."
    powershell.exe -Command "Start-Process powershell -ArgumentList '-NoExit', '-Command', 'cd ..\easytess_ocr_api\backend\app_ocr; & ''..\..\.venv\Scripts\python.exe'' run.py' -WindowStyle Normal"
    echo "✅ Fenêtre OCR lancée sous Windows."
    echo ""
fi
# -----------------------------------------------

# Build des images (uniquement si --build est passé)
if [ "$1" = "--build" ]; then
    echo "🔨 Construction des images Docker (Mode Demo)..."
    docker compose -f docker-compose.demo.yml build
else
    echo "⏭️  Saut de la construction des images (utilisez ./start.demo.sh --build pour forcer le build)"
fi

# Démarrer les services
echo ""
echo "⚡ Démarrage des services (Mode Demo)..."
docker compose -f docker-compose.demo.yml up -d

# Attendre que le backend soit prêt
echo ""
echo "⏳ Attente de la disponibilité du backend..."
sleep 10

BACKEND_HEALTH=$(curl -s http://localhost:8080/actuator/health | grep -q '"status":"UP"' && echo "OK" || echo "FAILED")

if [ "$BACKEND_HEALTH" = "OK" ]; then
    echo "✅ Backend est prêt"
else
    echo "⚠️  Backend peut prendre un peu plus de temps à démarrer..."
fi

# Afficher les informations
echo ""
echo "======================================"
echo "✅ FridaAI est démarré!"
echo "======================================"
echo ""
echo "🌐 URLs d'accès:"
echo "   Frontend:    http://localhost:4200"
echo "   Backend:     http://localhost:8080"
echo "   Swagger UI:  http://localhost:8080/swagger-ui.html"
echo "   PostgreSQL:  localhost:5432"
echo ""
echo "📝 Commandes utiles:"
echo "   make logs          - Voir tous les logs"
echo "   make logs-backend  - Logs du backend"
echo "   make logs-frontend - Logs du frontend"
echo "   make shell-db      - Accès PostgreSQL"
echo "   make down          - Arrêter les services"
echo ""
