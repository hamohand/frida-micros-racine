# Guide d'Installation (Serveur Linux)

Ce guide permet de déployer l'application Frida OCR sur un serveur Linux (Ubuntu, Debian, CentOS...) destiné aux tests chez les notaires.

## 1. Prérequis

La machine cible doit avoir installé :
- **Git** (pour récupérer le code complet du projet)
- **Docker** et **Docker Compose** (V2)

## 2. Récupération du code

Déplacez l'ensemble du projet sur le serveur cible (en clonant le dépôt ou en copiant le dossier).

## 3. Configuration

Le fichier .env situé dans le dossier install-notaire contient la configuration par défaut. Vous pouvez modifier les ports si nécessaire :
- FRONTEND_PORT=4202
- BACKEND_PORT=8080

Assurez-vous que le serveur Linux autorise ces ports dans son pare-feu.

## 4. Lancement de l'installation

Exécutez les commandes suivantes depuis le dossier racine du projet pour lancer le déploiement automatisé :

\\\ash
cd install-notaire/linux
chmod +x deploy-linux.sh
./deploy-linux.sh
\\\

Le script va :
- Créer le dossier de stockage persistant rida-storage.
- Construire les images Docker à partir du code source (Base de données, Backend Java, Frontend Angular, Service OCR Python).
- Lancer tous les conteneurs en arrière-plan.

## 5. Accès à l'application

Une fois les conteneurs lancés, l'application est accessible depuis n'importe quel navigateur sur le réseau :
http://<IP_DU_SERVEUR>:4202

## 6. Application Mobile (APK)

Le fichier d'installation de l'application mobile est disponible dans ce même dossier : rida_mobile_nfc.apk.
Transférez-le sur le téléphone Android des testeurs. Assurez-vous que le téléphone est connecté au **même réseau Wi-Fi** que le serveur Linux pour que le flux local fonctionne.
