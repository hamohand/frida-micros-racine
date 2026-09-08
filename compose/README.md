# Composes Docker de FridaAI

Ce dossier regroupe les fichiers `docker-compose` **spécialisés par déploiement**.
Le compose par défaut de développement reste à la racine du dépôt (`docker-compose.yml`).

## Correspondance fichier ↔ composante

| Fichier | Composante | Rôle | Où ça tourne |
|---|---|---|---|
| `../docker-compose.yml` | Dev par défaut | Stack complète pour développer | Poste dev |
| `dev.demo.yml` | Dev en mode démo | Backend en `APP_DEMO_MODE=true`, OCR pointe sur `Medell-i7.local:8082` | Poste dev |
| `vps.demo.yml` | **Composante 2 — Démo en ligne** | Vitrine commerciale `frida.enclume-numerique.com`, OCR léger (QR uniquement), aucune donnée client | VPS Hostinger |
| `vps.licences.yml` | Infrastructure licences (transverse) | Serveur de licences `licences.frida.enclume-numerique.com` — appelé par le backend chez chaque notaire | VPS Hostinger |
| `../install-notaire/docker-installation/docker-compose.local.yml` | **Composante 1 — Local notaire** | Livré au notaire dans le zip d'installation | Poste notaire |
| `vps.calc.yml` | **Composante 3 — Calculs SaaS** | Backend seul en profil `calc-only`, stateless, uniquement `/api/calculs/*` | VPS Hostinger |

## Lancement (depuis la racine du dépôt)

```bash
# Dev local par défaut
docker compose up -d

# Dev local en mode démo
docker compose -f compose/dev.demo.yml up -d
# ou
./scripts/start.demo.sh

# VPS — vitrine démo (projet existant : frida-micros-racine)
docker compose -p frida-micros-racine --env-file .env -f compose/vps.demo.yml up -d

# VPS — infra licences (même projet, pour retrouver le volume license_db_data)
docker compose -p frida-micros-racine --env-file .env -f compose/vps.licences.yml up -d

# VPS — Calculs SaaS (stateless, calc.frida.enclume-numerique.com)
docker compose -f compose/vps.calc.yml up -d
```

> [!WARNING]
> Sur le VPS, **le `-p frida-micros-racine` n'est pas optionnel** pour la démo et les licences.
> Ces deux stacks ont été lancées à l'origine depuis un unique `docker-compose.licences.yml`
> (fichier supprimé depuis) et portent donc le nom de projet `frida-micros-racine`.
> Sans `-p`, Docker Compose déduit le nom de projet du dossier du fichier (`compose`) :
> il crée alors des volumes **vides** (`compose_postgres_data`, `compose_license_db_data`)
> et échoue sur les `container_name` déjà pris. Le `--env-file .env` est nécessaire pour la
> même raison : les fichiers de `compose/` ne chargent pas le `.env` de la racine.

## État réel du VPS (constaté le 2026-09-08)

| Stack | Nom de projet Docker | Conteneurs | Volumes |
|---|---|---|---|
| Démo (Composante 2) | `frida-micros-racine` | `frida-db`, `frida-ocr-api`, `frida-backend`, `frida-frontend` | `frida-micros-racine_postgres_data`, `frida-micros-racine_frida_uploads` |
| Licences | `frida-micros-racine` | `frida-license-db`, `frida-license-api`, `frida-license-dashboard` | `frida-micros-racine_license_db_data` |
| Calculs (Composante 3) | `compose` | `frida-calc-api` | aucun (stateless) |

Démo et licences partageant le même nom de projet, chaque commande `compose` sur l'une
signale l'autre en `orphan containers`. **C'est sans conséquence** : ne jamais passer
`--remove-orphans`, cela supprimerait la stack voisine. Les séparer proprement
imposerait de migrer le volume `license_db_data`, opération à risque pour un gain
purement cosmétique — non fait volontairement.

### Redéployer un seul service (ex. le frontend après un changement d'UI)

```bash
cd /root/frida/frida-micros-racine
git pull --ff-only origin main
docker compose -p frida-micros-racine -f compose/vps.demo.yml build frontend
docker compose -p frida-micros-racine -f compose/vps.demo.yml up -d --no-deps frontend
```

## Notes

- Tous les composes du dossier `compose/` référencent les contextes de build via `../` (racine du dépôt).
- Les stacks VPS (`vps.demo.yml`, `vps.licences.yml`) supposent que le réseau externe `webproxy` de Traefik existe déjà. Voir `docs/architecture/deploiement_vps.md`.
- La Composante 3 (Calculs SaaS) réutilise l'image `frida-backend` avec `SPRING_PROFILES_ACTIVE=calc-only` — voir `backend/src/main/resources/application-calc-only.properties`. Les endpoints hors `/api/calculs/*` sont désactivés via `@Profile("!calc-only")`.
