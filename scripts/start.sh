#!/bin/bash

# Script de démarrage de FridaAI
# Utilisation: ./start.sh

set -e

# Se placer à la racine du projet (un niveau au-dessus du dossier scripts)
cd "$(dirname "$0")/.."

echo "======================================"
echo "🚀 FridaAI - Démarrage Application"
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

# Récupérer les ports depuis .env ou utiliser les valeurs par défaut
FRONT_PORT=$(grep -E '^FRONTEND_PORT=' .env | cut -d '=' -f2 | tr -d '\r' || echo "4202")
BACK_PORT=$(grep -E '^BACKEND_PORT=' .env | cut -d '=' -f2 | tr -d '\r' || echo "8080")
if [ -z "$FRONT_PORT" ]; then FRONT_PORT=4202; fi
if [ -z "$BACK_PORT" ]; then BACK_PORT=8080; fi

echo "🛑 [1/3] Arrêt des conteneurs existants..."
docker compose down

echo ""
echo "🧹 [2/3] Nettoyage des processus fantômes (Ghosting) sur les ports $FRONT_PORT, $BACK_PORT, 8082..."

if grep -qi microsoft /proc/version; then
    # On est sous WSL, on utilise PowerShell pour tuer les processus Windows (wslrelay, node...)
    powershell.exe -Command "& {
        foreach (\$p in @($FRONT_PORT, $BACK_PORT, 8082)) {
            \$conns = Get-NetTCPConnection -LocalPort \$p -State Listen -ErrorAction SilentlyContinue
            if (\$conns) {
                foreach (\$c in \$conns) {
                    Write-Host \"⚠️ Processus fantôme Windows détecté sur le port \$p (PID: \$(\$c.OwningProcess)). Fermeture...\"
                    Stop-Process -Id \$c.OwningProcess -Force -ErrorAction SilentlyContinue
                }
            }
        }
    }"
else
    # Linux natif ou macOS
    for p in $FRONT_PORT $BACK_PORT 8082; do
        if command -v lsof &> /dev/null; then
            PID=$(lsof -t -i:$p 2>/dev/null || true)
            if [ ! -z "$PID" ]; then
                echo "⚠️ Processus fantôme détecté sur le port $p (PID: $PID). Fermeture..."
                kill -9 $PID 2>/dev/null || true
            fi
        fi
    done
fi

echo ""
echo "⚡ [3/3] Démarrage des services Docker..."

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
    echo "🔨 Construction des images Docker..."
    docker compose build
else
    echo "⏭️  Saut de la construction des images (utilisez ./start.sh --build pour forcer le build)"
fi

# Démarrer les services
echo ""
echo "Démarrage en arrière-plan..."
docker compose up -d

# Attendre que le backend soit prêt
echo ""
echo "⏳ Attente de la disponibilité du backend..."
sleep 10

BACKEND_HEALTH=$(curl -s http://localhost:$BACK_PORT/actuator/health | grep -q '"status":"UP"' && echo "OK" || echo "FAILED")

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
echo "   Frontend:    http://localhost:$FRONT_PORT"
echo "   Backend:     http://localhost:$BACK_PORT"
echo "   Swagger UI:  http://localhost:$BACK_PORT/swagger-ui.html"
echo "   PostgreSQL:  localhost:5432"
echo ""
echo "📝 Commandes utiles:"
echo "   make logs          - Voir tous les logs"
echo "   make logs-backend  - Logs du backend"
echo "   make logs-frontend - Logs du frontend"
echo "   make shell-db      - Accès PostgreSQL"
echo "   make down          - Arrêter les services"
echo ""
