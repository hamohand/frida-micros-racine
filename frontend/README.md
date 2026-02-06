# Frontend - FridaAI

Application Angular pour la gestion des fiches de succession.

## 📋 Structure

```
frontend/
├── src/
│   ├── app/
│   │   ├── app.component.*              # Root component
│   │   ├── app.config.ts                # Configuration app
│   │   ├── app.routes.ts                # Routing
│   │   ├── components/
│   │   │   ├── accueil/                 # Pages d'accueil
│   │   │   │   ├── home/
│   │   │   │   └── about/
│   │   │   ├── admin/                   # Interface admin
│   │   │   ├── dossier/                 # Gestion fichiers
│   │   │   ├── frida/                   # Détail fiche
│   │   │   ├── frida-list/              # Liste fiches
│   │   │   └── search/                  # Recherche
│   │   ├── services/                    # Services HTTP
│   │   │   ├── frida.service.ts
│   │   │   ├── file-upload.service.ts
│   │   │   └── ...
│   │   └── shared/                      # Code partagé
│   ├── styles/                          # Styles CSS globaux
│   ├── main.ts                          # Entry point
│   ├── index.html                       # Template HTML
│   └── assets/                          # Ressources (images, etc)
├── public/                              # Assets publiques
├── dist/                                # Build production
├── package.json                         # Dépendances NPM
├── angular.json                         # Configuration Angular
├── tsconfig.json                        # Configuration TypeScript
├── tsconfig.app.json                    # Config app TS
├── Dockerfile                           # Build Docker
├── nginx.conf                           # Config Nginx global
├── default.conf                         # Config site Nginx
└── README.md                            # Ce fichier
```

## 🚀 Démarrage

### Local (dev mode)

```bash
# Installation
npm install

# Démarrage dev server
npm start
# ou
ng serve --open

# Accessible sur http://localhost:4200
```

### Build Production

```bash
npm run build
# ou
ng build --configuration production
```

### Docker

```bash
# Build
docker build -t frida-ai-frontend .

# Run
docker run -d -p 4200:80 frida-ai-frontend
```

### Docker Compose

```bash
# Depuis la racine du projet
docker-compose up frontend
```

## 🛠️ Commandes NPM

```bash
npm install              # Installer dépendances
npm start               # Dev server
npm run build           # Build production
npm test                # Tests
npm run lint            # Linting
npm run serve:ssr       # Server-side rendering
```

## 🔌 Communication API

Requests vers le backend (port 8080):

```typescript
// Dans les services
private apiUrl = 'http://localhost:8080/api';

// Ou avec variable d'environnement
import { environment } from '../environments/environment';
private apiUrl = environment.apiUrl;
```

## 🎨 Styles

Styles CSS globaux dans `src/styles/`:
- `variables.css` - Variables CSS
- `layout.css` - Layout global
- `buttons.css` - Composants boutons
- `navigation.css` - Navigation

## 🧪 Tests

```bash
npm test                # Tests unitaires
npm run test:coverage   # Coverage report
```

## 🌐 Nginx Configuration

- `nginx.conf` - Configuration serveur
- `default.conf` - Configuration site
  - SPA routing
  - Proxy API vers backend
  - Cache des assets
  - Compression Gzip

## 🐳 Build Docker

```bash
# Build local
docker build -t frida-ai-frontend:latest .

# Build avec tag
docker build -t myregistry/frida-ai-frontend:1.0.0 .

# Push
docker push myregistry/frida-ai-frontend:1.0.0
```

## 📚 Documentation

- [Angular Docs](https://angular.io/)
- [Angular Material](https://material.angular.io/)
- [TypeScript](https://www.typescriptlang.org/)
- [RxJS](https://rxjs.dev/)

## 🚀 Production Deployment

```bash
# Build production
npm run build

# Résultat en dist/angular-app/
# Servir avec Nginx (voir docker-compose.yml)
```

## 📝 Logs & Debug

```bash
# Dev server
npm start

# Avec source maps pour debugging
ng serve --source-map

# Logs console (F12 dans le navigateur)
```

## ⚠️ Notes Importantes

1. Vérifier que le backend est accessible avant de faire des requêtes
2. CORS doit être configuré côté backend
3. En production, utiliser HTTPS
4. Le proxy Nginx gère les requêtes `/api/*`

## 🔗 Intégration avec Backend

Le frontend proxie les requêtes `/api/` vers le backend via Nginx:

```nginx
location /api/ {
    proxy_pass http://backend:8080;
}
```

## 📖 Voir aussi

- [README.md](../README.md) - Documentation principale
- [ARCHITECTURE.md](../ARCHITECTURE.md) - Architecture du projet
- [docker-compose.yml](../docker-compose.yml) - Orchestration services
