.PHONY: help build up down logs logs-backend logs-frontend logs-db restart clean migrate test

help:
	@echo "FridaAI - Docker Commands"
	@echo "=========================="
	@echo "make build              - Construire les images Docker"
	@echo "make up                 - Démarrer tous les services"
	@echo "make down               - Arrêter tous les services"
	@echo "make restart            - Redémarrer tous les services"
	@echo "make logs               - Afficher les logs de tous les services"
	@echo "make logs-backend       - Afficher les logs du backend"
	@echo "make logs-frontend      - Afficher les logs du frontend"
	@echo "make logs-db            - Afficher les logs de la base de données"
	@echo "make clean              - Arrêter et supprimer les containers et volumes"
	@echo "make shell-backend      - Accès shell au container backend"
	@echo "make shell-db           - Accès shell au container PostgreSQL"
	@echo "make test               - Lancer les tests"
	@echo "make ps                 - Afficher l'état des containers"

build:
	docker-compose build --no-cache

up:
	docker-compose up -d
	@echo ""
	@echo "✅ Services started!"
	@echo "Frontend: http://localhost:4200"
	@echo "Backend API: http://localhost:8080"
	@echo "Swagger UI: http://localhost:8080/swagger-ui.html"

down:
	docker-compose down

restart:
	docker-compose restart

logs:
	docker-compose logs -f

logs-backend:
	docker-compose logs -f backend

logs-frontend:
	docker-compose logs -f frontend

logs-db:
	docker-compose logs -f postgres

clean:
	docker-compose down -v
	docker system prune -f

ps:
	docker-compose ps

shell-backend:
	docker-compose exec backend sh

shell-db:
	docker-compose exec postgres psql -U postgres -d fridaaidb

# Commandes de développement
dev-backend:
	cd backend && mvn spring-boot:run

dev-frontend:
	cd frontend && npm start

# Build et run
build-frontend:
	cd frontend && npm ci && npm run build

build-backend:
	cd backend && mvn clean package -DskipTests

# Tests
test:
	cd backend && mvn test

test-frontend:
	cd frontend && npm test

# Docker push to registry (si nécessaire)
push:
	docker-compose push

# Inspection
inspect-backend:
	docker-compose exec backend java -version

db-backup:
	docker-compose exec postgres pg_dump -U postgres fridaaidb > backup_$(shell date +%Y%m%d_%H%M%S).sql

db-restore:
	@echo "Usage: make db-restore FILE=backup_xxxxxxx.sql"
ifdef FILE
	docker-compose exec -T postgres psql -U postgres fridaaidb < $(FILE)
endif
