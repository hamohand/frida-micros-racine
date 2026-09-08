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

# VPS — vitrine démo
docker compose -f compose/vps.demo.yml up -d

# VPS — infra licences
docker compose -f compose/vps.licences.yml up -d

# VPS — Calculs SaaS (stateless, calc.frida.enclume-numerique.com)
docker compose -f compose/vps.calc.yml up -d
```

## Notes

- Tous les composes du dossier `compose/` référencent les contextes de build via `../` (racine du dépôt).
- Les stacks VPS (`vps.demo.yml`, `vps.licences.yml`) supposent que le réseau externe `webproxy` de Traefik existe déjà. Voir `docs/architecture/deploiement_vps.md`.
- La Composante 3 (Calculs SaaS) réutilise l'image `frida-backend` avec `SPRING_PROFILES_ACTIVE=calc-only` — voir `backend/src/main/resources/application-calc-only.properties`. Les endpoints hors `/api/calculs/*` sont désactivés via `@Profile("!calc-only")`.
