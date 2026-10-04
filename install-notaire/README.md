# Dossier d'Installation FRIDA

Ce dossier regroupe tous les éléments nécessaires au déploiement de l'application chez les notaires.

## Organisation

- \windows/\ : Contient l'installateur automatisé pour Windows (scripts PowerShell/Batch) et l'archive du code pour une installation locale. Voir \LISEZMOI.txt\ à l'intérieur.
- \linux/\ : Contient les scripts et la configuration Docker pour un déploiement sur un serveur Linux natif. Voir \INSTALLATION_LINUX.md\ à l'intérieur.
- \mobile/\ : Contient l'application Android (\rida_mobile_nfc.apk\) à installer sur les téléphones des notaires pour la lecture NFC.
- \documentation/\ : Contient la documentation utilisateur ou technique de l'application.

## Mode opératoire
1. Déterminez le système cible (Windows ou Linux).
2. Utilisez le sous-dossier correspondant.
3. Installez l'APK du dossier \mobile/\ sur les téléphones.
