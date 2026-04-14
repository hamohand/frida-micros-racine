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
