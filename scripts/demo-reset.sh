#!/usr/bin/env bash
# ==============================================================================
# FRIDA — Script de réinitialisation quotidienne de la démo publique (VPS)
# À planifier dans le crontab de srv816669 (ex: tous les soirs à 03h00) :
#   0 3 * * * /root/frida/frida-micros-racine/scripts/demo-reset.sh >> /var/log/frida-demo-reset.log 2>&1
# ==============================================================================

set -euo pipefail

echo "[$(date -Iseconds)] Début de la réinitialisation de la démo publique Frida..."

# 0. Attendre que frida-backend ait terminé son démarrage (et que Hibernate ait créé les tables)
echo "Attente de la disponibilité de frida-backend..."
for i in $(seq 1 30); do
  if docker exec frida-backend curl -sf http://localhost:8080/api/auth/status >/dev/null 2>&1; then
    echo "frida-backend est prêt."
    break
  fi
  sleep 2
done

# 1. Vider les tables de dossiers et brouillons existantes (conserve parametres et utilisateur)
docker exec frida-db sh -c 'psql -U "${POSTGRES_USER:-muhend}" -d "${POSTGRES_DB:-frida_db}" -c "
DO \$\$
DECLARE
    tbl text;
BEGIN
    FOR tbl IN
        SELECT tablename FROM pg_tables
        WHERE schemaname = '\''public'\''
          AND tablename IN ('\''frida'\'', '\''defunt'\'', '\''heritier'\'', '\''temoin'\'', '\''calcul'\'', '\''identites'\'', '\''brouillon'\'')
    LOOP
        EXECUTE format('\''TRUNCATE TABLE %I RESTART IDENTITY CASCADE'\'', tbl);
    END LOOP;
END \$\$;
"'

# 2. Nettoyer les fichiers déposés dans le volume /app/uploads
docker exec frida-backend sh -c "rm -rf /app/uploads/*"

# 3. Pré-créer un brouillon d'exemple (Famille KACEM Ahmed — 15 actes fictifs)
docker exec frida-backend curl -sf -X POST http://localhost:8080/api/demo/creer-dossier-exemple >/dev/null \
  && echo "Dossier d'exemple (Famille KACEM Ahmed) pré-chargé." \
  || echo "Attention : impossible de pré-charger le dossier d'exemple (vérifiez docker logs frida-backend)."

echo "[$(date -Iseconds)] Réinitialisation terminée avec succès."
