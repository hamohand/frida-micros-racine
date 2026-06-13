#!/bin/bash
echo "========================================="
echo "⚡ Passage en mode DEVELOPPEMENT Frontend"
echo "========================================="
echo ""
echo "1) Arrêt du frontend dans Docker pour libérer le port 4200..."
docker compose stop frontend
echo ""
echo "2) Lancement du serveur de développement Angular (Hot Reload)..."
cd frontend
npm start
