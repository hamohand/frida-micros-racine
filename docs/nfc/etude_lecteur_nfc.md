# Étude Technique : Intégration d'un Lecteur NFC USB (Phase 2)

L'intégration d'un lecteur NFC USB de bureau (comme le célèbre **ACR122U**) pour lire les puces des pièces d'identité biométriques est une excellente évolution pour Frida. Cependant, cela soulève plusieurs défis techniques intéressants, notamment en termes d'architecture web et de sécurité.

Voici l'étude complète de faisabilité et d'implémentation.

---

## 1. Le Matériel (Hardware)
- **Le Lecteur** : Tout lecteur répondant à la norme **PC/SC** (Personal Computer/Smart Card) fonctionne. Le modèle de référence mondial, peu coûteux (~30€) et ultra-fiable est l'**ACS ACR122U**.
- **Les Puces** : Les passeports et CNI biométriques algériennes respectent le standard international **ICAO 9303** (eMRTD - Electronic Machine Readable Travel Document).

---

## 2. La Sécurité : Pourquoi la MRZ reste indispensable
Contrairement à une carte bancaire de paiement sans contact, on ne peut pas simplement "poser" une carte d'identité sur le lecteur pour lire son contenu. C'est une sécurité contre le piratage à distance (dans le métro par exemple).

Pour que la puce accepte de livrer ses données (photo HD, identité, NIN), le lecteur doit prouver qu'il a l'autorisation du porteur. 
- Cette preuve s'appelle le **BAC (Basic Access Control)** ou le **PACE**.
- Pour générer la clé BAC, le logiciel a **obligatoirement besoin de 3 informations issues de la MRZ** :
  1. Le numéro du document
  2. La date de naissance
  3. La date d'expiration

**Conséquence UX (Expérience Utilisateur) :**
Le clerc de notaire ne pourra pas juste poser la carte sur le lecteur. Il faudra d'abord que le système lise la MRZ (soit via une webcam, soit via un scan rapide, soit par saisie manuelle), *puis* pose la carte sur le lecteur NFC.

---

## 3. Le Défi de l'Architecture Web
L'application Frida est une application Web (Angular + Spring Boot). 
**Problème majeur** : Pour des raisons évidentes de sécurité, un navigateur web (Chrome, Edge, Firefox) n'a **pas le droit** de communiquer directement avec les ports USB ou les lecteurs de cartes à puce de l'ordinateur de l'utilisateur. (L'API *WebNFC* existe mais ne fonctionne pratiquement que sur Android, pas sur les PC de bureau).

### La Solution : Le "Frida Local Agent"
Pour qu'un site web puisse parler à un lecteur USB branché sur le PC, il faut installer un petit logiciel pont sur le PC du notaire.

**Architecture proposée :**
1. **Frida Local Agent** : Un tout petit programme autonome (en Java ou Python) installé sur le PC Windows du clerc de notaire. Il tourne discrètement en arrière-plan (systray) et expose un mini-serveur web local sur le port `http://localhost:8088`.
2. **Bibliothèque de lecture** : Cet agent utilise la bibliothèque open-source **JMRTD** (Java Machine Readable Travel Document) pour parler au lecteur PC/SC.
3. **Le Flux (Workflow)** :
   - Sur l'interface web Frida, le clerc clique sur "Scanner la carte via NFC".
   - Le frontend Angular envoie une requête locale à `http://localhost:8088/api/read-nfc` en passant les clés BAC (issues de l'OCR préalable de la MRZ).
   - L'agent local fait clignoter le lecteur USB. Le clerc pose la carte.
   - L'agent déverrouille la puce, extrait la photo et les données textuelles, et les renvoie au navigateur web au format JSON.
   - Le navigateur web envoie ces données parfaites au serveur backend Frida normal.

---

## 4. Stack Technique Recommandée
Si nous devons développer ce "Local Agent", la meilleure approche est :
- **Langage** : Java (car c'est l'écosystème où le standard ICAO est le mieux implémenté).
- **Communication USB** : `javax.smartcardio` (intégré à Java, gère le standard PC/SC nativement sous Windows).
- **Extraction ICAO** : La librairie open-source **[JMRTD](https://jmrtd.org/)**. C'est la référence absolue. Elle sait générer les clés BAC/PACE, lire les Data Groups (DG1 pour le texte, DG2 pour la photo) et décoder les images (Souvent encodées en JPEG2000).
- **Serveur local** : Un mini serveur HTTP embarqué (comme Javalin ou un minuscule Spring Boot).

## 5. Bilan
**Est-ce réalisable ?** Oui, à 100%. C'est une architecture classique pour les lecteurs de cartes (c'est exactement ainsi que fonctionnent les lecteurs de cartes Vitale chez les médecins en France, ou les lecteurs de cartes d'identité eID en Belgique).

**Est-ce que ça vaut le coup ?** Absolument. Le gain de temps et l'éradication totale des erreurs (NIN, Noms) justifie largement le développement de ce petit agent local et l'achat de lecteurs à 30€.
