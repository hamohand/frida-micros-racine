#!/bin/bash

# Script de démarrage de FridaAI
# Utilisation: ./start.sh

set -e

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
if ! command -v docker-compose &> /dev/null; then
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
docker-compose --version
echo ""

# Build des images
echo "🔨 Construction des images Docker..."
docker-compose build

# Démarrer les services
echo ""
echo "⚡ Démarrage des services..."
docker-compose up -d

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
