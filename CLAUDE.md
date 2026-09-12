# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

FridaAI is a full-stack web application for managing succession records (*fiches*) with OCR document processing. It uses a **hybrid architecture**: Docker-hosted services plus a native Windows/Python OCR service.

### The 3 Composantes

FridaAI is delivered in **three distinct composantes**, each with its own compose file and target audience. Understanding this split is essential — it drives Spring profiles, compose file selection, and deployment decisions.

| Composante | Compose file | Target | Backend profile |
|---|---|---|---|
| **1 — Local notaire** | `install-notaire/docker-installation/docker-compose.local.yml` | Poste du notaire (installation via zip WSL) | `docker` / `prod` |
| **2 — Démo en ligne** | `compose/vps.demo.yml` | Vitrine commerciale `frida.enclume-numerique.com` + `simul-frida.enclume-numerique.com` | `prod` + `APP_DEMO_MODE=true` |
| **3 — Calculs SaaS** | `compose/vps.calc.yml` | API JSON pure `calc.frida.enclume-numerique.com` pour appelants machine (Tarif-Cloak, mobile NFC, intégrations) | `calc-only` |

The `calc-only` profile guards all controllers/services except `CalculController` and its dependencies via `@Profile("!calc-only")`. See `backend/src/main/resources/application-calc-only.properties` for the autoconfigure exclusions (DataSource, JPA, Security).

Transverse infrastructure: `compose/vps.licences.yml` (license-db + license-api + license-dashboard on `licences.frida.enclume-numerique.com`).

The default `docker-compose.yml` at repo root is for local dev with native Windows OCR. See `compose/README.md` for the full map.

## Commands

### Docker (full stack, dev par défaut)
```bash
make up              # Start all services (uses root docker-compose.yml)
make down            # Stop all services
make build           # Rebuild all images (no-cache)
make restart         # Restart all services
make logs-backend    # Tail backend logs
make logs-frontend   # Tail frontend logs
make clean           # Stop + remove containers, volumes, prune system
make ps              # Show container status
make shell-backend   # Shell into backend container
make shell-db        # psql into PostgreSQL container
make db-backup       # Dump database to SQL file
```

### Docker par composante
```bash
# Sur le VPS, démo et licences tournent sous le projet `frida-micros-racine` :
# le -p et le --env-file sont obligatoires (sinon volumes vides). Voir compose/README.md.
docker compose -p frida-micros-racine --env-file .env -f compose/vps.demo.yml up -d      # Composante 2 (VPS)
docker compose -p frida-micros-racine --env-file .env -f compose/vps.licences.yml up -d  # Infra licences (VPS)
docker compose -f compose/vps.calc.yml up -d       # Composante 3 (VPS, projet `compose`)
docker compose -f compose/dev.demo.yml up -d       # Dev local en mode démo
```

> **Note WSL**: sur Windows, Docker tourne dans WSL Ubuntu (pas Docker Desktop). Invoquer les commandes via `wsl -d Ubuntu -e bash -c "cd <projet> && docker ..."`.

### Backend (Spring Boot, Java 21)
```bash
cd backend && mvn spring-boot:run                                        # Dev mode (port 8080)
cd backend && SPRING_PROFILES_ACTIVE=calc-only mvn spring-boot:run       # Composante 3 en local
cd backend && mvn clean package -DskipTests                              # Build JAR
cd backend && mvn test                                                   # Run tests
cd backend && mvn test -Dtest=ClassName                                  # Run a single test class
```

### Frontend (Angular 18)
```bash
cd frontend && npm start     # Dev server (port 4200)
cd frontend && npm run build # Production build
cd frontend && npm test      # Run tests
```

### Service URLs (dev local)
| Service        | URL                                        |
|----------------|--------------------------------------------|
| Frontend       | http://localhost:4200                      |
| Backend API    | http://localhost:8080                      |
| Swagger UI     | http://localhost:8080/swagger-ui.html      |
| OCR API        | http://localhost:8082 (native, not Docker) |

### Service URLs (production VPS)
| Composante | URL                                             |
|------------|-------------------------------------------------|
| Démo (2)   | https://frida.enclume-numerique.com             |
| Démo (2)   | https://simul-frida.enclume-numerique.com       |
| Calc (3)   | https://calc.frida.enclume-numerique.com        |
| Licences   | https://licences.frida.enclume-numerique.com    |

## Architecture

### Services
- **backend** (Spring Boot 3.3.4, Java 21) — main REST API, JPA/PostgreSQL, Swagger docs. **Le module de calculs (`calculs/` package) est intégré au backend** — plus de microservice séparé. En profil `calc-only`, seul `CalculController` est actif.
- **frontend** (Angular 18, Nginx) — SPA proxying `/api/*` to backend
- **postgres** (PostgreSQL 16) — primary database (absent en Composante 3)
- **ocr-api** (Python Flask + pyzbar) — **lecture de QR codes uniquement**. L'image Docker n'embarque ni Tesseract ni EasyOCR : `ocr-api/Dockerfile` ignore volontairement `app_ocr/requirements.txt`. Le code d'OCR texte (`ocr_engine_v2.py`, flags `TESSERACT_DISPONIBLE` / `EASYOCR_DISPONIBLE` / `PADDLEOCR_DISPONIBLE` sous `try/except ImportError`) est dormant et dégrade proprement. Il expose aussi `/api/translitteration/verifier` (comparaison phonétique nom arabe / nom latin), appelée seulement si l'option « Vérification phonétique des noms » est cochée dans la page Paramètres (désactivée par défaut, table `parametres`). Entité par défaut des extraits de naissance : `en_01_qrcode_01`.
- **license-api / license-dashboard** — infra transverse pour la validation des licences chez les notaires

The `start.sh` detects WSL and automatically sets `WINDOWS_HOST_IP` so the backend can reach the native OCR service. `MAX_PARALLEL_FOLDERS` (default 2) controls OCR concurrency.

### Data Model
```
FridaEntity (succession record)
  ├── 1:1 → DefuntEntity (deceased)   → 1:1 → IdentitesEntity
  ├── 1:N → HeritierEntity (heirs)    → 1:1 → IdentitesEntity
  ├── 1:N → TemoinEntity (witnesses)  → 1:1 → IdentitesEntity
  └── 1:1 → CalculEntity (inheritance calculations)
```

`IdentitesEntity` is a single identity table that stores data extracted from multiple document types (birth certificate, CNI, passport, etc.).

### OCR Processing Pipeline (Composantes 1 & 2 uniquement)
1. Frontend uploads documents → Backend (`FileController`, `OcrProcessingController`)
2. Backend calls OCR API (Python) → receives structured JSON
3. Backend maps OCR JSON to JPA entities → persists to PostgreSQL
4. Backend calcule les parts via le module interne `calculs/`, après contrôle des doublons : `DoublonsHeritiersValidator`, appelé en tête de `HeirPartCalculatorService.executerCalcul` (point de passage de tous les calculs), refuse le calcul si un même NIN normalisé figure deux fois dans le dossier, défunt compris. Les NIN absents ou invalides sont ignorés. Réponse 400 avec message, validée aussi côté écran de revue.

### Folder Naming Convention
Uploaded files land in `dossiers/<fiche>/{code}_{documentType}[_{entityName}]/<horodatage>_<nom>` (ex. `03_en_en_01_qrcode_01`, `02_cni_cni_01`, verso dans `02_cni_cni_01_verso`).
- Codes (`HeirCategory`) : 00=Témoin, 01=Défunt, 02=Conjoint, 03=Enfant, 04=Parent, 05=Fratrie, 06=Oncle paternel, 07=Cousin paternel, 08=Grand-père paternel, 09=Petit-fils, 10=Petite-fille, 11=Grand-mère paternelle. `3_en` et `03_en` désignent la même catégorie.
- **Plusieurs personnes partagent un même sous-dossier** : les fenêtres « Fils » et « Filles » envoient toutes deux en `03`, et le sexe est lu dans le QR code. `DossierProcessingService.regrouperParPersonne` traite donc chaque recto comme une personne. Ne jamais regrouper les fichiers par code de catégorie (régression du 2026-07-02 : un seul enfant traité, aucune fille ; corrigée le 2026-09-10).
- Verso de CNI : fichier suffixé `_verso`, nommé selon son recto par le frontend. Rattaché au recto de même nom de base, sinon au recto unique de la catégorie, sinon laissé non rattaché. Dump NFC (`nfc_dump_<numéro>.json`) : même règle, et traité comme une personne à part entière lorsque rien ne le rattache.

### Backend Package Layout (`backend/src/main/java/`)
- `calculs/` — module de calcul islamique successoral (model, service, validator). Auto-porteur : aucune dépendance JPA/OCR, seul actif en profil `calc-only`.
- `controller/` — REST endpoints. Tous annotés `@Profile("!calc-only")` sauf `CalculController` et `CalcInfoController` (page `/` en calc-only).
- `service/` — business logic, OCR orchestration (guardé `@Profile("!calc-only")`)
- `entities/` — JPA models
- `repository/` — Spring Data repositories
- `client/` — HTTP clients to OCR API
- `config/` — CORS (`WebConfig`), sécurité, `LicenseInterceptor`
- `enums/` — `DocumentType`, `HeirCategory`
- `dto/` — request/response objects

### Frontend Structure (`frontend/src/app/`)
- `components/` — pages: accueil, dossier, frida, frida-list, search, admin, aibd, frida-edit
- `services/` — Angular HTTP services
- Nginx proxies `/api/*` → backend; all other routes handled by Angular SPA routing

## Configuration

Copy `.env.example` to `.env` and set values. Key variables:
- `DB_PASSWORD` — PostgreSQL password
- `SPRING_PROFILES_ACTIVE` — seul `calc-only` a un effet (voir `application-calc-only.properties`). Les autres valeurs (`docker`, `prod`, …) n'activent aucune configuration : il n'existe pas de fichier `application-<profil>.properties` correspondant.
- `SPRING_JPA_HIBERNATE_DDL_AUTO` — `update` for dev, `validate` for prod
- `CORS_ORIGINS` — comma-separated allowed origins (ou `*` en profil calc-only)
- `CORS_ALLOW_CREDENTIALS` — `true` par défaut ; forcer `false` en calc-only avec `CORS_ORIGINS=*`
- `MAX_PARALLEL_FOLDERS` — OCR parallelism (default 2)
- `ROOT_PATH` — host path mounted as `/frida-storage/` in containers
- `APP_DEMO_MODE` — `false` par défaut : en mode démo, toute requête sans jeton est authentifiée en Maître. `docker-compose.yml` (dev), `compose/dev.demo.yml` et `compose/vps.demo.yml` le mettent à `true`.
- `SAUVEGARDES_LECTURE_SEULE` — `true` sur la démo publique : sauvegardes et archives consultables, mais ni créées, ni restaurées, ni supprimées, ni téléchargées.
- `BACKUP_PATH`, `BACKUP_AUTO_ENABLED`, `BACKUP_AUTO_KEEP`, `ARCHIVE_AUTO_ENABLED` — une sauvegarde est un dossier `<nom>/database.sql` + `<nom>/uploads`, le format de `sauvegarder.bat` / `restaurer.bat`. Sauvegarde automatique si la dernière a plus de 24 h ; archivage automatique désactivé par défaut. Restaurer, supprimer, télécharger et archiver sont réservés au rôle Maître ; le téléchargement passe par un lien à usage unique (`/api/telechargements/<jeton>`, 60 s).
- Le backend tourne à l'heure `Europe/Paris` (`ENV TZ` dans `backend/Dockerfile`).
- `JWT_SECRET` — clé de signature des jetons de connexion, 32 caractères minimum. Générée par poste par `Installer-Frida.ps1` dans le `.env` notaire. Absente : clé aléatoire à chaque démarrage (connexions perdues au redémarrage) ; l'ancienne valeur par défaut, publiée dans le dépôt, est refusée. Une requête sans jeton valide reçoit 401 (403 = droits insuffisants), et le frontend renvoie alors à la page de connexion.

> **Note compose subdirectory**: les fichiers de `compose/` ne chargent PAS automatiquement le `.env` de la racine. Utiliser `docker compose --env-file .env -f compose/xxx.yml ...` si nécessaire.

## Additional Documentation

Detailed docs are in `docs/`:
- `ARCHITECTURE.md` — full system design
- `TESTS.md` — testing strategy
- `legacy/` — anciens documents (STRATEGIE_PRODUCTION.md, DEPLOYMENT.md) conservés pour historique — voir `legacy/README.md`
- `architecture/architecture_saas_frida.md` — architecture SaaS et les 3 composantes
- `architecture/configuration_licences_vps.md` — déploiement infra licences
- `architecture/deploiement_vps.md` — déploiement général VPS
- `compose/README.md` — table des correspondances fichier compose ↔ composante

Documentation NFC & lectures biométriques regroupée dans `docs/nfc/` :
- `README.md` — index
- `lectures_biometriques.md` — stratégie en 3 phases (OCR+MRZ, lecteur USB, mobile)
- `etude_lecteur_nfc.md` — étude Phase 2 (lecteur NFC USB de bureau)
- `architecture_app_mobile_nfc.md` — architecture Phase 3 (app Flutter, code dans `frida_mobile_nfc/`)

Documentation d'installation notaire (Composante 1) regroupée dans `install-notaire/documentation/` :
- `README.md` — index des documents d'installation
- `INSTALLATION_PROCEDURE.md` — préparation du package (dev)
- `CHECKLIST_TEST_TERRAIN.md` — checklist de test à emporter chez le notaire
- `activation_licence_notaire.md` — activation licence (actuellement suspendue)
- `../docker-installation/LISEZMOI.txt` — notice remise au notaire
