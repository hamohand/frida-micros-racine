#!/bin/bash

# Aller à la racine du projet
cd "$(dirname "$0")/.." || exit

if [ -z "$1" ]; then
    echo "Utilisation : $0 chemin/vers/fichier_backup.dump"
    exit 1
fi

BACKUP_FILE="$1"

if [ ! -f "$BACKUP_FILE" ]; then
    echo "Le fichier $BACKUP_FILE n'existe pas."
    exit 1
fi

echo "Restauration de la base de données à partir de $BACKUP_FILE..."
echo "ATTENTION : Cette opération va écraser les données actuelles de la base."
read -p "Voulez-vous continuer ? (o/N) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Oo]$ ]]
then
    echo "Restauration annulée."
    exit 1
fi

docker compose exec -T db sh -c 'pg_restore -U $POSTGRES_USER -d $POSTGRES_DB -c -1' < "$BACKUP_FILE"

if [ $? -eq 0 ]; then
    echo ""
    echo "Restauration réussie !"
else
    echo ""
    echo "Échec de la restauration !"
    exit 1
fi
