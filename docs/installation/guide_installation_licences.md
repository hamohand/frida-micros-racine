# Guide d'Installation et de Déploiement : Système de Licences Frida

Ce guide détaille les étapes nécessaires pour déployer le système de licences sur votre infrastructure (SaaS/VPS) et l'installer chez les clients (Notaires).

---

## Partie 1 : Déploiement Côté Serveur (VPS SaaS)

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

---

## Partie 2 : Installation Client (Chez le Notaire)

Cette partie concerne le déploiement physique du logiciel Frida sur les PC des clients.

### 1. Préparation de la machine
1. Le notaire doit disposer d'un environnement Java/Python fonctionnel (selon votre méthode d'installation actuelle : exécutable, script batch, etc.).
2. L'ordinateur **doit avoir accès à Internet** au moins lors de la première activation et à chaque démarrage pour valider la clé.

### 2. Déploiement du Code Local
Assurez-vous que le code que vous installez correspond à la dernière version de la branche `main` (contenant les intercepteurs de licence et la page d'activation).
- Compilez le Backend (Spring Boot) : `mvn clean package`
- Compilez le Frontend (Angular) : `npm run build`

### 3. Premier Lancement
1. Lancez le Backend Spring Boot local.
2. Lancez le Frontend Angular local (ou servez-le via Nginx local).
3. Le notaire ouvre Frida dans son navigateur (ex: `http://localhost:4200`).
4. **Comportement attendu** : L'accès à l'application est bloqué. Le système redirige automatiquement le notaire vers la page `/license`.

### 4. Activation de la Licence
1. Le notaire saisit la clé que vous lui avez fournie (ex: `FRIDA-XXXX-YYYY`).
2. Le système local génère l'identifiant matériel (`Hardware ID`) de la machine.
3. Il envoie la requête à votre VPS (`licences.frida.enclume-numerique.com`).
4. Si la clé est valide et n'est pas déjà liée à un autre PC, elle est activée et **définitivement liée** à cet ordinateur.
5. Une copie locale (`uploads/license.key`) est sauvegardée pour les prochains démarrages.

> [!TIP]
> Si le notaire tente de copier le dossier d'installation sur le PC de son collègue, le système générera un nouveau `Hardware ID` différent. Le VPS rejettera alors la validation, empêchant le piratage. S'il souhaite utiliser Frida sur un 2ème poste, vous devrez lui générer une 2ème clé.

### 5. En cas de Révocation ou d'Expiration
Si vous révoquez la clé depuis votre Dashboard, ou si la durée de validité est dépassée, le Frida local affichera de nouveau la page d'erreur au prochain démarrage ou appel d'API, bloquant ainsi l'accès.
