# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

FridaAI is a full-stack web application for managing succession records (*fiches*) with OCR document processing. It uses a **hybrid architecture**: Docker-hosted services plus a native Windows/Python OCR service.

## Commands

### Docker (full stack)
```bash
make up              # Start all services
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

### Backend (Spring Boot, Java 21)
```bash
cd backend && mvn spring-boot:run        # Dev mode (port 8080)
cd backend && mvn clean package -DskipTests  # Build JAR
cd backend && mvn test                   # Run tests
cd backend && mvn test -Dtest=ClassName  # Run a single test class
```

### Frontend (Angular 18)
```bash
cd frontend && npm start     # Dev server (port 4200)
cd frontend && npm run build # Production build
cd frontend && npm test      # Run tests
```

### Service URLs
| Service        | URL                                        |
|----------------|--------------------------------------------|
| Frontend       | http://localhost:4200                      |
| Backend API    | http://localhost:8080                      |
| Swagger UI     | http://localhost:8080/swagger-ui.html      |
| Calculs API    | http://localhost:8081                      |
| OCR API        | http://localhost:8082 (native, not Docker) |

## Architecture

### Services
- **backend** (Spring Boot 3.3.4, Java 21) — main REST API, JPA/PostgreSQL, Swagger docs
- **frontend** (Angular 18, Nginx) — SPA proxying `/api/*` to backend
- **postgres** (PostgreSQL 16) — primary database
- **calculs-api** (Spring Boot, port 8081) — inheritance share calculations microservice
- **OCR API** (Python Flask + EasyTess, port 8082) — **runs natively on Windows, never in Docker**

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

### OCR Processing Pipeline
1. Frontend uploads documents → Backend (`FileController`, `OcrProcessingController`)
2. Backend calls OCR API (Python) → receives structured JSON
3. Backend maps OCR JSON to JPA entities → persists to PostgreSQL
4. Optionally calls Calculs API for share calculations

### Folder Naming Convention
Uploaded folders follow `{code}_{documentType}`:
- `1_en` = Defunt birth certificate, `2_cni` = Conjoint CNI, `3_en` = Enfant birth certificate
- Person codes: 1=Défunt, 2=Conjoint, 3=Enfant, 4=Parent, 5=Fratrie, 11=Témoin

### Backend Package Layout (`backend/src/main/java/`)
- `controller/` — REST endpoints (Frida, File, Folder, OcrProcessing)
- `service/` — business logic, OCR orchestration
- `entities/` — JPA models
- `repository/` — Spring Data repositories
- `client/` — HTTP clients to Calculs API and OCR API
- `config/` — CORS, RestTemplate
- `enums/` — `DocumentType`, `HeirCategory`
- `dto/` — request/response objects

### Frontend Structure (`frontend/src/app/`)
- `components/` — pages: accueil, dossier, frida, frida-list, search, admin, aibd, frida-edit
- `services/` — Angular HTTP services
- Nginx proxies `/api/*` → backend; all other routes handled by Angular SPA routing

## Configuration

Copy `.env.example` to `.env` and set values. Key variables:
- `DB_PASSWORD` — PostgreSQL password
- `SPRING_PROFILES_ACTIVE` — `docker` | `development` | `production`
- `SPRING_JPA_HIBERNATE_DDL_AUTO` — `update` for dev, `validate` for prod
- `CORS_ORIGINS` — comma-separated allowed origins
- `MAX_PARALLEL_FOLDERS` — OCR parallelism (default 2)
- `ROOT_PATH` — host path mounted as `/frida-storage/` in containers

## Additional Documentation

Detailed docs are in `docs/`:
- `ARCHITECTURE.md` — full system design
- `DEPLOYMENT.md` — production deployment
- `TESTS.md` — testing strategy
- `INSTALLATION_PROCEDURE.md` — setup instructions
