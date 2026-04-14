# Guide d'Installation de l'Application Frida

Ce guide décrit la liste complète des dépendances et outils nécessaires ainsi que la procédure étape par étape pour déployer l'architecture de **Frida OCR** sur une nouvelle machine.

L'application est découpée en deux environnements d'exécution principaux :
1. **L'environnement Docker :** Il gère la base de données PostgreSQL, l'API de gestion (Backend), l'API des calculs successoraux, et l'interface Web (Frontend Angular).
2. **L'environnement Local Python :** Il gère le service d'extraction et d'analyse OCR de texte (EasyTess). Ce choix a été fait pour des questions de rapidité et d'efficacité liées au traitement d'image et à l'accès natif aux librairies Tesseract.

---

## 🛠️ Outils Nécessaires : Prérequis

Avant de télécharger l'application, voici les outils à installer sur votre ordinateur :

### 1. Requis pour le Socle Principal (Docker)
*   **Docker Desktop** (ou Docker Engine) : Obligatoire pour lancer les différents services et la BDD.
*   **Docker Compose** : Inclus généralement avec Docker Desktop, c'est l'orchestrateur.
*   **Git** : Pour pouvoir télécharger le code source officiel depuis le dépôt.

### 2. Requis pour le Moteur OCR
*   **Python 3.8 à 3.11** : Le service web OCR local fonctionne avec le framework `Flask` et python.
*   **Le binaire Tesseract-OCR** : Essentiel pour faire correspondre le texte des images.
    *   *Windows* : Installer l'exécutable natif `tesseract-ocr-setup.exe` (s'assurer de l'ajouter aux variables d'environnement `PATH`).
    *   *Linux (Ubuntu)* : `sudo apt-get install tesseract-ocr tesseract-ocr-ara tesseract-ocr-fra`
    *   *Mac* : `brew install tesseract`

### 3. Optionnel : Accélération de développement
*   **Node.js (18+)** et `npm` : Uniquement si vous souhaitez développer/compiler le Frontend Angular en dehors de Docker.
*   **Java 21** et **Maven** : Uniquement pour débugger le Backend Spring Boot localement sans Docker.

---

## ⚙️ Procédure Complète d'Installation

### Étape 1 : Récupération du Projet Principal

Commencez par récupérer le code sur votre nouvelle machine et initialisez l'environnement.

```bash
# 1. Cloner le projet entier depuis le système de contrôle de version
git clone <url-de-votre-repo-github/gitlab>
cd frida-micros

# 2. Entrez dans le dossier racine principal contenant les conteneurs
cd frida-micros-racine

# 3. Préparez la configuration d'environnement (variables secrètes, ports)
cp .env.example .env
```
*(Si le fichier `.env.example` n'existe pas, copiez-le depuis vos archives sécurisées ou configurez directement `.env` à la main.)*

---

### Étape 2 : Lancement du Service Tesseract OCR (Local)

Le service OCR doit tourner de façon native sur la machine physique pour éviter les surcharges de virtualisation, il fonctionnera sur le port **8082**.

```bash
# 1. Depuis la racine, entrez dans l'espace dédié au moteur EasyTess
cd ../easytess_ocr_api/backend

# 2. Lier la librairie de fondation (core_lib) commune au projet
cd core_lib
pip install -e .

# 3. Installer les dépendances du serveur OCR et le démarrer
cd ../app_ocr
pip install -r requirements.txt

# Optionnel sous unix : pip install opencv-python pypdfium2 etc... si erreur
python run.py
```
*Garder ce terminal ouvert, le moteur OCR doit répondre sur* `http://localhost:8082`.

---

### Étape 3 : Démarrage des Services Applicatifs (Docker)

L'application principale est totalement orchestrée par Docker. Vous n'avez pas besoin de gérer manuellement le paramétrage Java, Nginx ou PostgreSQL.

Ouvrez **un second terminal** et exécutez la construction complète :

```bash
# 1. Naviguez dans le dossier racine de l'application connectée
cd frida-micros/frida-micros-racine

# 2. Lancez le script de démarrage officiel générant l'orchestration 
#    (Linux/Mac/GitBash)
./start.sh

# Alternative si vous êtes sur un cmd Windows :
# docker-compose up -d --build
```

**Que fait cette étape en arrière plan ?**
* Crée le dossier `backend/uploads` s'il n'existe pas.
* Télécharge et installe PostgreSQL 16 (Port 5432) en créant une base via `init-db.sql`.
* Prépare et déploie l'API Spring Boot de calculs sur le port `8081`.
* Compile le backend Spring Boot principal et le lance sur le port `8080`.
* Compile l'interface frontend Angular 18 dans un serveur Nginx sur le port `4200`.

---

### Étape 4 : Validation du Système

Après quelques dizaines de secondes, les serveurs répondront présents de cette façon :

*   ✅ **Interface Utilisateur (Naviguer pour tester) :** `http://localhost:4200`
*   ✅ **Base de Données Postgres (Accès pour outil) :** `localhost:5432` *(identifiants dans `.env`)*
*   ✅ **API d'Administration Interactive :** `http://localhost:8080/swagger-ui.html`
*   ✅ **API de Calcul (Microservice Interne) :** `http://localhost:8081`
*   ✅ **Plateforme d'Extraction d'Images (OCR) :** `http://localhost:8082`

**En cas de besoin de dépannage :**
*   Vérifier que les données circulent (backend) : `docker-compose logs -f backend`
*   Accès urgence la BD : `make shell-db` ou `docker-compose exec postgres psql -U postgres -d fridaaidb`
*   Éteindre la plateforme correctement : `./stop.sh` ou `docker-compose down`.
