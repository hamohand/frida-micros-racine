#!/usr/bin/env bash
# ==============================================================================
# FRIDA — Script de réinitialisation quotidienne de la démo publique (VPS)
# À planifier dans le crontab de srv816669 (ex: tous les soirs à 03h00) :
#   0 3 * * * /root/frida/frida-micros-racine/scripts/demo-reset.sh >> /var/log/frida-demo-reset.log 2>&1
# ==============================================================================

set -euo pipefail

echo "[$(date -Iseconds)] Début de la réinitialisation de la démo publique Frida..."

# 1. Vider les tables de dossiers et brouillons (conserve parametres et utilisateur)
docker exec frida-db psql -U "${DB_USER:-muhend}" -d "${DB_NAME:-frida_db}" -c \
  "TRUNCATE TABLE frida, defunt, heritier, temoin, calcul, identites, brouillon RESTART IDENTITY CASCADE;"

# 2. Nettoyer les fichiers déposés dans le volume /app/uploads
docker exec frida-backend sh -c "rm -rf /app/uploads/*"

# 3. Pré-créer un brouillon d'exemple (Famille KACEM Ahmed — 15 actes fictifs)
docker exec frida-backend curl -sf -X POST http://localhost:8080/api/demo/creer-dossier-exemple >/dev/null || true

echo "[$(date -Iseconds)] Réinitialisation terminée avec succès."
