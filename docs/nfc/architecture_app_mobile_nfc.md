# Architecture : Application Mobile Frida NFC

Ce document détaille l'architecture et les choix techniques validés pour la création de l'application mobile compagnon "Frida NFC" (Phase 3 des lectures biométriques). L'objectif est de permettre aux clercs de notaire de lire les puces NFC des pièces d'identité directement depuis un smartphone professionnel.

## 1. Choix Technologiques Validés

- **Technologie (Frontend Mobile)** : **Flutter**. Ce choix s'appuie sur son excellent écosystème cross-platform et sa robustesse pour l'intégration de fonctionnalités matérielles bas niveau (Caméra, NFC).
- **Public Cible** : **Usage interne (Clercs de notaire)**. L'application ne sera pas distribuée au grand public. Cela simplifie les contraintes de sécurité (l'API backend peut être protégée par un simple token VPN/Intranet ou une authentification clerc) et l'interface utilisateur peut être optimisée pour la productivité (mode "scan à la chaîne").

## 2. Le Flux de Travail (Workflow Mobile)

Le processus de bout en bout pour numériser un héritier ou le défunt se déroulera en 5 étapes rapides :

1. **Scan MRZ (Caméra)** : 
   - L'application ouvre la caméra et détecte automatiquement la zone MRZ au dos de la CNI ou au bas du passeport.
   - Utilisation de *Google ML Kit Text Recognition* (via les packages Flutter) pour extraire instantanément le texte.
2. **Génération de la Clé BAC** : 
   - Le code mobile parse les 3 lignes de la MRZ pour isoler : le Numéro de document, la Date de naissance et la Date d'expiration.
   - Ces trois données constituent la clé BAC (Basic Access Control).
3. **Lecture NFC (Puce)** : 
   - L'écran invite le clerc à plaquer la pièce d'identité au dos de son smartphone.
   - Utilisation d'un package comme `flutter_nfc_kit` ou de l'implémentation native des standards ICAO pour négocier la sécurité BAC avec la puce.
4. **Extraction des Data Groups** : 
   - Une fois la puce déverrouillée, le téléphone télécharge le **DG1** (Identité textuelle complète et exacte, y compris le NIN) et le **DG2** (Photo biométrique HD).
5. **Transmission au Backend** : 
   - L'application compile un payload JSON contenant l'identité parfaite et encode la photo en Base64 (ou l'envoie via `multipart/form-data`).
   - Requête HTTP vers une nouvelle route sécurisée du backend Spring Boot (ex: `POST /api/frida/mobile-nfc-upload`).

## 3. Impact sur le Backend Actuel

Le backend Frida actuel devra être légèrement adapté pour accepter les flux de données provenant du mobile :

- **Nouvelle API d'ingestion** : Actuellement, le backend attend des images (via le dossier de scan) qu'il transmet au Python OCR. Il faudra une API qui accepte un objet de type `IdentitesEntity` "pré-rempli" et 100% fiable, qui contourne l'OCR Python et s'insère directement dans la base de données.
- **Liaison avec la Frida en cours** : Le clerc devra indiquer sur l'application mobile à quelle Frida (ou quel dossier) il est en train d'ajouter un héritier, ou scanner un QR code généré par l'application web pour lier son téléphone à la session en cours.

## 4. Prochaines Étapes Techniques (Proof of Concept)

Avant d'intégrer le système de bout en bout, la création d'un projet Flutter de type "POC" (Proof of Concept) est recommandée avec les jalons suivants :
- [ ] Initialiser un projet Flutter vierge.
- [ ] Intégrer l'appareil photo et lire avec succès les caractères de la MRZ d'une carte de test.
- [ ] Déclencher le capteur NFC du téléphone et réussir l'authentification BAC (Handshake).
- [ ] Récupérer et afficher la photo JPEG2000 contenue dans la puce sur l'écran du smartphone.
