# Stratégie de Production et Évolutions Architectures Futures

Ce document fait la synthèse des réflexions techniques pour préparer la commercialisation et le déploiement de l'application chez les clients finaux (version On-Premise).

## 1. Environnement de Production (Le Produit Fini)

Lorsqu'on livrera le client, la procédure devra fortement différer de l'environnement de développement :

*   **Le Code est Invisible :** On n'utilise plus l'instruction `build: context: ./` dans le fichier compose. Le poste du client va télécharger via internet des "boîtes noires" pré-compilées estampillées depuis un registre privé (ex: `image: mon-entreprise/frida-backend:1.0.0`).
*   **Protection et Robustesse :** Le client final n'aura pas accès au code source intellectuel (vos algorithmes de calculs).
*   **Démarrage Éclair :** L'application s'allumera instantanément (en quelques secondes) sans devoir re-compiler toutes les librairies Java ou Node.js.
*   *(L'exemple complet du script de production se trouve dans `frida-micros-racine/docker-compose.prod.yml.example`)*

---

## 2. Configuration Matérielle Estimée Réaliste

L'application fait intervenir une base de données, l'API de gestion, l'API mathématique, l'interface web, et surtout, un lourd microservice Python avec `PyTorch` pour l'OCR.

*   **Configuration Minimale Stricte :**
    *   **Processeur :** Multi-coeur de base (Intel i3 récent ou équivalent AMD).
    *   **RAM :** 8 Go.
    *   **Disque Dur :** SSD Obligatoire (20 Go libres).
    *   **OS :** Windows 10/11 - 64 bit (pour supporter Virtualisation/WSL2).
*   **Configuration Recommandée (Pour fluidité maximale de l'employé) :**
    *   **Processeur :** Modèle milieu de gamme (Intel i5 récent ou équivalent AMD).
    *   **RAM :** 16 Go (Optimal pour pré-charger les modèles d'intelligence artificielle en mémoire sans figer les autres fenêtres et le navigateur web du client).

---

## 3. Transformation en "Monolithe Hybride" ? (Optimisation)

Pour alléger l'impact de l'application sur la machine du client, il a été envisagé de réduire la fragmentation "microservices" de l'architecture.

**✅ Ce qui est très facile à faire et rentabilisé : "Le Monolithe Java"**
*   Fusionner les deux serveurs existants (`backend` et `calculs-api`) en un seul programme Java en appelant directement le code mathématique.
*   Absorber les fichiers compilés du Frontend (Angular) directement pour que Spring Boot génère la vue Web, évitant d'avoir un composant spécial Nginx.
*   *Gain direct : Le logiciel passe de 3 serveurs web à un seul. On économise 1 Go de RAM environ.*

**❌ Ce qui est difficile et fortement déconseillé : L'Intégration Totale Python**
*   L'IA `EasyTess` doit rester dans un composant `Python` externe (un deuxième pilier isolé). Essayer de lancer à la brute le code Python depuis du langage Java, ou réécrire l'intelligence neuronale en langage Java, poserait des graves soucis de stabilité et de performance.

---

## 4. Idée d'Évolution Phare : Le "Traitement Par Lots" (Mode Nuit)

Afin de pallier la lourdeur du traitement logiciel des cartes d'identité ou livrets de la famille chez un client possédant une petite machine :

*   **L'approche :** Différer le moteur de l'OCR pour qu'il soit asynchrone (l'utilisateur prépare ses dizaines de pièces jointes le jour, le logiciel les lira pendant la nuit).
*   **Architecturalement :** L'employé met en "File d'attente". Le backend Java planifie (via une annotation `@Scheduled`) un réveil nocturne (ex: 2h00 du matin), contacte le microservice Python, fait analyser les dizaines d'images, et met à jour PostgreSQL.
*   **Le Bénéfice :** Aucune baisse de vitesse de l'ordinateur durant les heures de travail bureautique ; Un écran unique de type "Bilan analytique" à l'arrivée de l'employé le lendemain matin (ex: "45 traités dont 2 échecs à finir à la main").

---

## 5. La Stratégie de Sauvegarde (Le Backup)

L'application possédant toutes les données sensibles (légales, photos d'identité), la perte de données n'est pas permise en cas de panne de la machine du client.

*   **La Base de données Unique :** Le client utilise une seule base de données (PostgreSQL) qui s'enrichit avec le temps. L'objectif est d'en "photographier" le contenu régulièrement.
*   **L'Assistant de Backup Dockerisé :** Un conteneur autonome (ex: `postgres-backup-local`) est greffé au module de production.
    *   **Le Rôle :** Tous les soirs à l'heure prévue (ex: minuit), le conteneur interroge la base, génère un fichier archive `.sql.gz` contenant toutes les données historiques, puis efface automatiquement les sauvegardes vieilles de plus de 7 jours (concept de "Fenêtre de rétention" évitant de saturer le disque dur).
*   **Le Principe des Zones Miroirs (Volumes) :** L'écriture d'une telle sauvegarde dans le conteneur est inutile si elle n'est pas accessible. En utilisant la formulation de montage `C:\FridaSauvegardes:/backups`, on "téléporte" physiquement les archives SQL hors de la bulle Docker pour les poser directement sur le disque (vrai bureau) du client.
*   *Note cruciale:* Le dossier regroupant les fichiers d'images scannées (photos CNI téléchargées, etc.) doit lui-aussi faire l'objet de sauvegardes par le client car la base de données SQL n'en conserve qu'un chemin symbolique.

---

## 6. Politique de Commercialisation : Le rôle exclusif du "Setup.exe"

Déployer un stack technique avancé via du personnel de vente sur le terrain peut vite être intimidant s'il n'est pas maitrisé.

*   **Les responsabilités du Vendeur (Commercial) :**
    *   Ce qu'il DOIT faire : Diagnostiquer si le PC est apte, activer la virtualisation processeur si nécessaire (Intel VT-x), double-cliquer sur le fichier livreur `Setup.exe`, montrer au client son nouveau raccourci de mise en route, et faire concrètement le lien pour que le dossier des sauvegardes automatiques atterrisse sur une clé USB / DDE / Clé Google Cloud.
    *   Ce qu'il NE DOIT PAS faire : Taper des lignes de commande, gérer ou configurer manuellement Docker ni éditer le code des scripts.
*   **La création du "Setup.exe" :**
    Il sera généré par des outils fiables comme **Inno Setup**. Et comportera tout le socle technologique pour automatiser les pré-requis : installer de façon silencieuse et aveuglément le WSL 2 (Noyau Linux Windows), Docker Desktop, importer les bons fichiers `docker-compose.prod.yml`, initier ou importer les images pré-existantes du registre ("load docker"), puis générer le grand raccourci localisant l'accès à `http://localhost:4200/`.
*   **Installation On-line vs Off-line :**
    *   *Option Connectée :* L'exécutable (`Setup.exe`) est léger (ex: < 500Mo) et tire en arrière-plan les Go de la base Docker directement à partir du "Docker Hub" privé ou une source lointaine. Dépendant de la connexion locale.
    *   *Option Air-gapped (Déconnectée):* Privilégiée face aux cabinets administratifs mal équipés. L'installeur porte localement les +3 Go d'images sur sa clé dans un conteneur compressé. Durée d'installation drastiquement rapide face aux limitations internet.
