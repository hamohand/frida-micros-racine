#!/usr/bin/env bash

# FridaAI - Quick Start Guide
# Ce script affiche un guide interactif pour démarrer l'application

echo "╔═══════════════════════════════════════════════════════════╗"
echo "║                    🚀 FridaAI Quick Start                 ║"
echo "╚═══════════════════════════════════════════════════════════╝"
echo ""

echo "📋 Vérification des pré-requis..."
echo ""

# Vérifier Docker
if command -v docker &> /dev/null; then
    DOCKER_VERSION=$(docker --version)
    echo "✅ Docker installé: $DOCKER_VERSION"
else
    echo "❌ Docker n'est pas installé"
    echo "   Installer: https://www.docker.com/products/docker-desktop"
    exit 1
fi

# Vérifier Docker Compose
if command -v docker-compose &> /dev/null; then
    COMPOSE_VERSION=$(docker-compose --version)
    echo "✅ Docker Compose installé: $COMPOSE_VERSION"
else
    echo "❌ Docker Compose n'est pas installé"
    echo "   Installer: https://docs.docker.com/compose/install/"
    exit 1
fi

# Vérifier Make (optionnel)
if command -v make &> /dev/null; then
    echo "✅ Make installé"
    HAS_MAKE=true
else
    echo "⚠️  Make n'est pas installé (optionnel)"
    echo "   Sur Windows: chocolatey install make"
    echo "   Sur Mac: brew install make"
    echo "   Sur Linux: apt-get install make"
    HAS_MAKE=false
fi

echo ""
echo "═══════════════════════════════════════════════════════════"
echo ""
echo "🔧 Configuration"
echo ""

# Vérifier .env
if [ ! -f .env ]; then
    echo "⚠️  Fichier .env non trouvé"
    echo "📝 Création de .env à partir de .env.example..."
    if [ -f .env.example ]; then
        cp .env.example .env
        echo "✅ Fichier .env créé"
        echo "⚠️  ⚠️  À FAIRE: Modifier les values sensibles dans .env"
    else
        echo "❌ .env.example non trouvé"
        exit 1
    fi
else
    echo "✅ Fichier .env trouvé"
fi

echo ""
echo "═══════════════════════════════════════════════════════════"
echo ""
echo "🚀 Démarrage de l'application"
echo ""

# Proposer les différentes façons de démarrer
echo "Choisissez la méthode de démarrage:"
echo ""
echo "  1) Make (Recommandé - si installé)"
echo "  2) Docker Compose"
echo "  3) Script shell"
echo "  4) Afficher uniquement le résumé"
echo "  5) Quitter"
echo ""

read -p "Votre choix (1-5): " choice

case $choice in
    1)
        if [ "$HAS_MAKE" = true ]; then
            echo ""
            echo "🔨 Construction et démarrage avec Make..."
            echo ""
            make build
            make up
            echo ""
            echo "✅ Services démarrés avec Make!"
        else
            echo "❌ Make n'est pas installé. Utiliser l'option 2 ou 3."
        fi
        ;;
    2)
        echo ""
        echo "🐳 Construction et démarrage avec Docker Compose..."
        echo ""
        docker-compose build
        docker-compose up -d
        echo ""
        echo "✅ Services démarrés avec Docker Compose!"
        ;;
    3)
        echo ""
        echo "🏃 Utilisation du script shell..."
        echo ""
        chmod +x start.sh
        ./start.sh
        ;;
    4)
        # Afficher le résumé et quitter
        ;;
    5)
        echo "Au revoir! 👋"
        exit 0
        ;;
    *)
        echo "❌ Choix invalide"
        exit 1
        ;;
esac

# Afficher le résumé
echo ""
echo "═══════════════════════════════════════════════════════════"
echo ""
echo "📊 Résumé - FridaAI"
echo ""
echo "Structure:"
echo "  📂 backend/        → Spring Boot API (port 8080)"
echo "  📂 frontend/       → Angular App (port 4200)"
echo "  🗄️  PostgreSQL      → Database (port 5432)"
echo ""
echo "URLs d'accès:"
echo "  🌐 Frontend:       http://localhost:4200"
echo "  🔌 API:            http://localhost:8080"
echo "  📚 Swagger:        http://localhost:8080/swagger-ui.html"
echo "  🗄️  PostgreSQL:    localhost:5432"
echo ""
echo "Commandes utiles:"
echo ""
if [ "$HAS_MAKE" = true ]; then
    echo "  make help           → Afficher toutes les commandes"
    echo "  make logs           → Voir les logs"
    echo "  make down           → Arrêter les services"
    echo "  make clean          → Supprimer tout"
    echo ""
fi
echo "  docker-compose ps              → État des services"
echo "  docker-compose logs -f         → Logs en temps réel"
echo "  docker-compose exec backend bash    → Shell backend"
echo "  docker-compose exec postgres psql   → Shell DB"
echo ""
echo "Documentation:"
echo "  📖 README.md        → Guide complet"
echo "  🏗️  ARCHITECTURE.md  → Architecture du projet"
echo "  🚀 DEPLOYMENT.md    → Guide de déploiement"
echo "  ✅ RESTRUCTURATION.md → Changements effectués"
echo ""
echo "═══════════════════════════════════════════════════════════"
echo ""
echo "💡 Conseil: Consultez README.md pour plus de détails"
echo ""
