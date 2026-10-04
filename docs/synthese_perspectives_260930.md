# 🗺️ Synthèse & Perspectives — FRIDA V1 (Version Notaire)
## Document de suivi du 30/09/2026

> Objectif : constituer une version complète et fiable de Frida qu'on pourra installer chez les notaires.

---

## État des Lieux (après audit du 30/09/2026)

| Domaine | Avancement | Verdict |
|---|:---:|---|
| Moteur de calcul successoral (Mawarith) | ✅ 95% | Complet et robuste (Fard, Asaba, Hajb, Aoul, Radd, Wasiyya) |
| Liens de parenté (HeirCategory) | ✅ 90% | 12 catégories, terminologie arabe contextuelle |
| Acquisition NFC / OCR / QR Code | ✅ 85% | Mobile (Flutter/JMRTD) + USB (Python/pyscard) |
| Authentification JWT (Maître / Clerc) | ✅ 80% | JWT, rôles, BCrypt, mode démo |
| Gestion des dossiers (Recherche / Brouillons) | ✅ 80% | Recherche, tri, onglets validés/brouillons |
| Sauvegarde / Restauration | ✅ 95% | Sauvegardes auto, restauration sécurisée avec annulation |
| Impression (window.print) | ⚠️ 50% | Fonctionnel mais basique |
| Validation des données | ⚠️ 50% | Doublons NIN bloqués, mais champs obligatoires non vérifiés |
| Document Frida final (HTML) | ⚠️ 55% | Bon pour cas simples, incomplet pour cas complexes |
| Génération Word (.docx) / PDF | ❌ 0% | Inexistant |
| Paramètres de l'étude notariale | ❌ 20% | Seule l'IP locale est configurable |
| Export CSV / Excel | ❌ 0% | Inexistant |

---

## Travaux Réalisés (session du 30/09/2026)

### Standardisation des champs NFC
- **Problème** : Les 3 sources de données (Python USB, Flutter Mobile, OCR) utilisaient des noms de champs différents pour les mêmes informations.
- **Solution** : Standardisation sur la convention de la base de données :
  - `nom` / `prenom` → noms arabes (puce DG11)
  - `latines` / `prenomLatines` → noms latins (MRZ)
- **Fichiers modifiés** : `OcrMappingService.java`, `NfcSessionController.java`, `emrtd.py`

### Correction du sexe (genre) depuis le Mobile NFC
- **Problème** : L'app Flutter envoyait `"gender": "MALE"`, le backend attendait `"sexe"`. Résultat : tous les hommes étaient enregistrés comme femmes.
- **Solution** : Le backend lit désormais les deux clés (`sexe` et `gender`).

### Extraction des prénoms arabes depuis la puce NFC (Mobile)
- **Problème** : La librairie JMRTD (Java) décodait l'arabe ISO-8859-6 en UTF-8, produisant du charabia. Le backend compensait avec l'OCR qui lisait l'en-tête de la carte ("الجمهورية الجزائرية...") au lieu du prénom.
- **Solution** :
  1. L'app Flutter extrait les octets bruts (hex) du fichier DG11 de la puce
  2. Le backend implémente un parseur TLV (Tag-Length-Value) qui décode chaque champ individuellement avec l'encodage ISO-8859-6
  3. Suppression de l'injection OCR parasite dans `NfcSessionController`

### Stabilisation du scanner MRZ (Mobile)
- Ajout d'un cadre visuel (overlay avec coins verts) pour guider le positionnement
- Correction du bug MLKit qui tronquait la 1ère ligne de la MRZ (padding agressif des `<`)
- Exigence stricte de 3 lignes complètes avant validation

---

## Fonctionnalités à Développer — Classées par Priorité

### 🔴 Priorité 1 — BLOQUANTES (sans ça, pas de distribution)

#### 1.1 Date de décès du défunt
> C'est la date d'ouverture de la succession. Sans elle, l'acte est juridiquement nul.

- Ajouter le champ `dateDeces` dans `DefuntEntity`
- L'afficher dans le formulaire de saisie (upload-windows ou heir-review)
- L'intégrer dans le texte officiel de la Frida
- **Effort estimé : ~1h**

#### 1.2 Paramètres de l'étude notariale
> Chaque notaire doit voir son propre nom, adresse et cachet sur l'acte.

- Nom complet du notaire (arabe + latin)
- Intitulé officiel de l'étude
- Adresse, wilaya, commune
- Numéro de répertoire / chambre des notaires
- Logo ou cachet (optionnel pour la V1)

#### 1.3 Compléter le document Frida final
> Vérifier que le texte de l'acte couvre bien tous les cas de figure.

**Toutes les catégories d'héritiers sont présentes** dans le carousel de saisie et dans le moteur de calcul (Grand-père, Grand-mère, Oncles, Cousins, Petits-fils/Petites-filles Wasiyya Wajiba). Vérifier que la section textuelle القسمة du document final les affiche correctement.

**Ajouts nécessaires :**
- Mention de la date de décès dans le corps de l'acte
- Mention du lieu de décès (si disponible)
- Numérotation du répertoire notarial

#### 1.4 Contrôles de validation avant génération
> Empêcher le notaire de produire un acte incomplet ou invalide.

- ❌ Bloquer si moins de 2 témoins
- ❌ Bloquer si le défunt n'a pas de nom, prénom ou date de naissance
- ❌ Bloquer si la date de décès est absente
- ⚠️ Avertir si un héritier a des champs vides (NIN, filiation)

---

### 🟠 Priorité 2 — IMPORTANTES (confort et crédibilité professionnelle)

#### 2.1 Amélioration de la mise en page d'impression
- Règles CSS `break-inside: avoid` sur le tableau des héritiers
- Gestion `@page` avec marges officielles
- En-tête/pied de page sur chaque feuille imprimée

#### 2.2 Génération PDF côté serveur
> Archivage numérique, envoi par email, impression fidèle.

- Approche recommandée : OpenHTMLtoPDF (HTML → PDF avec police Amiri)
- Réutilise le template HTML existant de `frida.component.html`
- Endpoint `GET /api/fridas/{id}/pdf` → télécharge le PDF
- Gestion propre des sauts de page pour les grandes familles

#### 2.3 Changement de mot de passe
- Formulaire accessible depuis le profil utilisateur
- Vérification de l'ancien mot de passe

---

### 🟡 Priorité 3 — SOUHAITABLES (V1.1 ou version ultérieure)

> Cette phase peut être reportée à une version ultérieure du projet.

#### 3.1 Export CSV / Excel du registre
- Exporter la liste des Fridas validées depuis `/search`
- Colonnes : N° Frida, Nom défunt, Date décès, Date création, Nb héritiers, Notaire

#### 3.2 Statistiques / Tableau de bord
- Nombre de Fridas ce mois-ci
- Répartition par catégorie d'héritiers
- Dossiers en attente de correction

#### 3.3 Multi-notaires (Multi-tenancy)
- Cloisonnement des dossiers par étude
- Chaque notaire ne voit que ses propres dossiers
- Administration centralisée (si hébergement mutualisé)

#### 3.4 Génération Word (.docx)
> Permettre au notaire de retoucher l'acte dans Word avant impression.

- Approche recommandée : Apache POI côté backend
- Créer un template `.docx` avec des balises `${nomDefunt}`, `${dateDecès}`, etc.
- Endpoint `GET /api/fridas/{id}/docx` → télécharge le .docx pré-rempli

#### 3.5 Distinctions fines dans le calcul
- Grand-mère maternelle vs paternelle
- Frères utérins vs consanguins (cas rares)

---

## Idée Future — Module NFC Indépendant

Discussion ouverte sur l'extraction du module de lecture NFC (Mobile + USB) en **outil indépendant réutilisable** pour d'autres projets nécessitant l'authentification par carte d'identité algérienne (banques, assurances, hôtels, KYC).

Trois approches envisagées :
- **A.** Serveur local universel (WebSocket) pour applications web de bureau
- **B.** SDK Flutter (Package) pour applications mobiles
- **C.** Application "Fournisseur d'Identité" (style FranceConnect)

> À concrétiser après la stabilisation de Frida V1.

---

## Ordre de Développement Suggéré

```
Phase 1 (MVP distribuable) :
  1.1 Date de décès → 1.2 Paramètres étude → 1.3 Compléter le document → 1.4 Validations

Phase 2 (Confort professionnel) :
  2.1 Mise en page → 2.2 Génération PDF → 2.3 Mot de passe

Phase 3 (V1.1 — reportable à une version ultérieure) :
  3.1 Export CSV → 3.2 Stats → 3.3 Multi-notaires → 3.4 Génération Word → 3.5 Calculs fins
```

> **Estimation globale Phase 1 :** 2 à 3 sessions de travail intensives.
> **Estimation globale Phase 2 :** 2 à 3 sessions supplémentaires.
