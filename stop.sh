#!/bin/bash

# Script d'arrêt de FridaAI
# Utilisation: ./stop.sh [--clean]

set -e

echo "======================================"
echo "⛔ FridaAI - Arrêt des services"
echo "======================================"
echo ""

if [ "$1" = "--clean" ]; then
    echo "🧹 Nettoyage complet (suppression des volumes)..."
    docker compose down -v
    echo "✅ Suppression des containers et volumes effectuée."
else
    echo "Arrêt des services..."
    docker compose down
    echo "✅ Services arrêtés."
    echo ""
    echo "💡 Pour supprimer aussi les données (volumes), utiliser: ./stop.sh --clean"
fi

if grep -qi microsoft /proc/version; then
    echo ""
    echo "⚠️  N'oubliez pas de fermer manuellement la fenêtre PowerShell de l'OCR Python !"
fi

echo ""
