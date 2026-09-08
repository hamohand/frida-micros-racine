# Stratégie de Lecture des Pièces d'Identité (Lectures Biométriques)

Dans le cadre de l'application Frida, l'extraction fiable et sans erreur des données des pièces d'identité (Défunt, Héritiers, Témoins) est critique pour garantir la validité juridique de la Fréda et le calcul exact des parts.

Ce document récapitule les différentes technologies de lecture, ce qui a été fait, et la feuille de route recommandée pour l'avenir.

## Phase 1 : OCR Standard + Validation Croisée MRZ (Déployé)

**Statut : 🟢 Actif (Court Terme)**

La première phase s'appuie sur une lecture OCR classique de la carte couplée à une lecture de la zone MRZ (Machine Readable Zone).

### Principe de fonctionnement
1. **OCR Visuel** : Lecture des données textuelles de la carte (Noms en arabe et en latin, Date de Naissance, NIN, Sexe).
2. **Lecture MRZ** : Analyse des 3 lignes au dos de la carte (CNI) ou des 2 lignes (Passeport) pour extraire les données sécurisées.
3. **Validation Croisée** : 
   - La MRZ sert de "Source de Vérité" pour valider l'OCR.
   - Les dates de naissance sont confirmées via les algorithmes de checksum (ICAO 9303).
   - Les noms latins sont extraits et une translittération est testée avec le nom arabe lu par l'OCR.

> [!NOTE]
> **Limitations actuelles de la MRZ**
> Sur le format TD1 (CNI Algérienne), la MRZ ne dispose que de 15 caractères pour les données optionnelles, ce qui est insuffisant pour contenir le Numéro d'Identification National (NIN) algérien complet (18 chiffres). Le NIN extrait via MRZ est donc ignoré s'il est tronqué, et la priorité est redonnée à l'OCR pour ce champ précis.

---

## Phase 2 : Lecteur NFC USB en Cabinet Notarial

**Statut : 🟡 En Projet (Moyen Terme)**

La seconde phase vise à éliminer totalement les erreurs liées à la qualité de l'image (flou, reflets, hologrammes) en lisant directement la puce RFID/NFC intégrée aux passeports et cartes d'identité biométriques.

### Scénario d'usage
- Un lecteur NFC USB standard (ex: **ACR122U**, coût d'environ 30 à 40€) est branché sur le PC du clerc de notaire ou de la secrétaire.
- Au moment de saisir un héritier, le clerc pose la CNI sur le lecteur.
- Les données (y compris la photo HD, l'identité complète, et le NIN exact) sont transférées instantanément et de manière 100% fiable vers l'application Frida.

### Avantages
- **Zéro faute d'OCR** : Les données sont numériques et infalsifiables.
- **Zéro champ suspect** : Disparition de l'étape de correction manuelle dans l'interface.
- **Productivité** : Lecture instantanée (quelques secondes par pièce).
- **Technologie mature** : Utilisation de bibliothèques Java open-source comme `JMRTD` (Java Machine Readable Travel Document).

> [!TIP]
> **Pourquoi le NFC est parfait pour Frida**
> L'accès à la puce NFC nécessite une clé cryptographique dérivée de la MRZ (via le protocole BAC - Basic Access Control ou PACE). Ainsi, l'intégration consistera à scanner rapidement la MRZ à la webcam ou au scanner classique, puis à lire la puce avec le lecteur NFC pour déverrouiller la donnée parfaite.

---

## Phase 3 : Application Mobile NFC sur le Terrain

**Statut : ⚪ Vision (Long Terme)**

La dernière phase décentralise la collecte des pièces d'identité directement vers le client ou le notaire en déplacement.

### Scénario d'usage
- Développement d'une application mobile compagnon "Frida Mobile".
- Le notaire (ou même le client lui-même depuis son domicile) utilise son smartphone équipé du NFC pour scanner sa propre pièce d'identité.
- L'application mobile lit la puce et envoie directement le dossier sécurisé au backend Frida dans le cloud via une API sécurisée.

### Avantages
- Plus besoin d'infrastructure matérielle dédiée au bureau.
- Amélioration de l'expérience client (démarches à distance possibles).
- Architecture moderne et distribuée.

---

## Synthèse et Recommandation

Pour un cabinet notarial cherchant à optimiser la saisie des Frédas, la **Phase 2** offre le meilleur rapport Qualité / Coût / Effort. 
L'ajout d'un petit lecteur physique NFC USB au bureau transformera l'expérience logicielle en supprimant définitivement les frictions inhérentes à la reconnaissance optique de caractères (OCR). L'OCR MRZ actuel (Phase 1) servira alors uniquement de clé d'accès (BAC) pour lire la puce NFC.
