# Guide de Configuration et Déploiement : Système de Licences Frida (VPS)

Ce guide détaille les étapes nécessaires pour déployer le système de licences sur votre infrastructure (SaaS/VPS).

---

## Déploiement Côté Serveur (VPS SaaS)

Cette partie concerne l'infrastructure centrale qui génère et valide les licences.

### 1. Configuration DNS et Sécurité
1. Assurez-vous d'avoir créé un enregistrement DNS (type `A` ou `CNAME`) pour **`licences.frida.enclume-numerique.com`** pointant vers l'adresse IP de votre VPS.
2. Patientez jusqu'à la propagation complète (1h à 24h).

### 2. Configuration Keycloak
Puisque le dashboard des licences utilise votre Keycloak pour l'authentification :
1. Connectez-vous à la console d'administration de Keycloak (`auth.hscode.enclume-numerique.com`).
2. Allez dans le royaume **`frida-realm`**.
3. **Créez un Client** (Clients -> Create client) :
   - Client ID : `frida-license-dashboard`
   - Client authentication : `Off` (Client public)
   - Standard flow : `On` (Activé)
   - Valid redirect URIs : `https://licences.frida.enclume-numerique.com/*`
   - Web origins : `https://licences.frida.enclume-numerique.com`
4. Assurez-vous d'avoir au moins un utilisateur créé dans ce royaume pour pouvoir vous connecter au dashboard.

> [!IMPORTANT]
> Si vous souhaitez restreindre l'accès au Dashboard uniquement à certains administrateurs, vous pouvez configurer des Rôles Keycloak. (Par défaut, n'importe quel utilisateur valide du royaume `frida-realm` pourra se connecter).

### 3. Récupération du Code sur le VPS
Connectez-vous en SSH à votre VPS et placez-vous dans le dossier de votre projet Frida :
```bash
cd /chemin/vers/frida-micros-racine
git pull origin main
```

### 4. Lancement des Services
Démarrez les conteneurs liés aux licences (Base de données isolée, API, et Dashboard) :
```bash
docker compose -f docker-compose.licences.yml up -d
```

> [!NOTE]
> Au premier lancement, Traefik va interroger Let's Encrypt pour obtenir le certificat SSL. Si vous obtenez une erreur de connexion non privée, attendez 2 minutes et redémarrez avec `docker compose -f docker-compose.licences.yml restart`.

### 5. Utilisation du Dashboard
1. Rendez-vous sur `https://licences.frida.enclume-numerique.com`
2. Connectez-vous via Keycloak.
3. Saisissez le nom du notaire et la durée de validité (en mois) pour générer une clé.
4. Communiquez cette clé (ex: `FRIDA-XXXX-YYYY`) à votre client.
