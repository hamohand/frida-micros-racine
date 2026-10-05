#!/bin/bash
echo '============================================'
echo '  Installation de Frida OCR (Mode Linux)'
echo '============================================'

# Vérification de Docker
if ! command -v docker &> /dev/null; then
    echo "Docker n'est pas installé. Veuillez l'installer en premier."
    exit 1
fi

# Création du dossier de stockage si inexistant
mkdir -p ./frida-storage
chmod 777 ./frida-storage

# Création d'un fichier credentials dummy si inexistant
if [ ! -f .env ]; then
    cp .env.linux .env
fi

if [ ! -f ./dummy-google-credentials.json ]; then
    echo '{}' > ./dummy-google-credentials.json
fi

# Arrêt des anciens conteneurs si existants
echo 'Arrêt des anciens services...'
docker compose -f docker-compose.linux.yml down

# Construction et lancement
echo 'Construction et lancement des conteneurs...'
docker compose -f docker-compose.linux.yml up -d --build

echo '============================================'
echo 'Installation terminée !'
echo 'Le frontend est accessible sur le port 4202.'
echo 'Le backend est accessible sur le port 8080.'
echo '============================================'
