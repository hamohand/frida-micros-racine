#!/usr/bin/env bash

# Test Script - Vérifier l'organisation des fichiers

echo "═══════════════════════════════════════════════════════"
echo "🧪 TEST - Vérification de l'organisation"
echo "═══════════════════════════════════════════════════════"
echo ""

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

test_count=0
pass_count=0

# Fonction pour tester l'existence d'un fichier
test_file() {
    test_count=$((test_count + 1))
    if [ -f "$1" ]; then
        echo -e "${GREEN}✅${NC} $1"
        pass_count=$((pass_count + 1))
    else
        echo -e "${RED}❌${NC} $1 (NOT FOUND)"
    fi
}

# Fonction pour tester l'existence d'un répertoire
test_dir() {
    test_count=$((test_count + 1))
    if [ -d "$1" ]; then
        echo -e "${GREEN}✅${NC} $1"
        pass_count=$((pass_count + 1))
    else
        echo -e "${RED}❌${NC} $1 (NOT FOUND)"
    fi
}

echo -e "${YELLOW}Backend Files:${NC}"
test_file "backend/pom.xml"
test_file "backend/Dockerfile"
test_file "backend/README.md"
test_dir "backend/src/main/java/com/muhend/backendai"
test_dir "backend/src/main/resources"
test_dir "backend/src/test/java"

echo ""
echo -e "${YELLOW}Frontend Files:${NC}"
test_file "frontend/package.json"
test_file "frontend/angular.json"
test_file "frontend/tsconfig.json"
test_file "frontend/Dockerfile"
test_file "frontend/nginx.conf"
test_file "frontend/default.conf"
test_file "frontend/README.md"
test_dir "frontend/src/app"
test_dir "frontend/src/styles"

echo ""
echo -e "${YELLOW}Root Files (Configuration):${NC}"
test_file "docker-compose.yml"
test_file ".env"
test_file ".dockerignore"
test_file "Makefile"
test_file "ORGANISATION.md"

echo ""
echo "═══════════════════════════════════════════════════════"
echo -e "${GREEN}Résultats: $pass_count/$test_count tests passés${NC}"
echo "═══════════════════════════════════════════════════════"
echo ""

if [ $pass_count -eq $test_count ]; then
    echo -e "${GREEN}✅ Tous les tests sont passés!${NC}"
    echo ""
    echo "Vous pouvez maintenant:"
    echo "  docker-compose build"
    echo "  docker-compose up -d"
    exit 0
else
    echo -e "${RED}⚠️ Certains fichiers sont manquants.${NC}"
    echo "Vérifiez l'organisation et relancez le script."
    exit 1
fi
