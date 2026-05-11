#!/bin/bash

# Aller à la racine du projet
cd "$(dirname "$0")/.." || exit

BACKUP_DIR="db_backups"
mkdir -p "$BACKUP_DIR"

DATETIME=$(date +"%Y-%m-%d_%H-%M-%S")
BACKUP_FILE="$BACKUP_DIR/backup_$DATETIME.dump"

echo "Démarrage de la sauvegarde de la base de données..."
# Utilisation de pg_dump au format custom (-F c)
docker compose exec -T db sh -c 'pg_dump -U $POSTGRES_USER -d $POSTGRES_DB -F c' > "$BACKUP_FILE"

if [ $? -eq 0 ]; then
    echo ""
    echo "Sauvegarde réussie : $BACKUP_FILE"
    echo "Vous pouvez utiliser scripts/restore_db.sh pour la restaurer."
else
    echo ""
    echo "Échec de la sauvegarde !"
    rm -f "$BACKUP_FILE"
    exit 1
fi

# Optionnel : Décommentez pour conserver seulement les X dernières sauvegardes (ex: 7 jours)
# find "$BACKUP_DIR" -name "backup_*.dump" -type f -mtime +7 -delete
# echo "Anciennes sauvegardes nettoyées."
