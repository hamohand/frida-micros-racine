# Projets de Numérisation pour la Gestion Notariale

*Inspirés de l'architecture et des principes de FRIDA*

---

## Rappel : Ce que fait FRIDA

FRIDA est une application qui **numérise la gestion des dossiers de succession** en combinant :
- **OCR intelligent** → extraction automatique de données depuis des documents scannés (extraits de naissance, CNI, passeports)
- **Modèle de données structuré** → défunt, héritiers, témoins, calculs de parts
- **Calcul automatisé** → répartition des parts successorales
- **Workflow digitalisé** → upload → OCR → vérification/correction → calcul → archivage

Ce modèle peut être **répliqué** pour de nombreuses procédures notariales.

---

## 🏗️ Projets Proposés

### 1. 📜 VENTA — Gestion des Ventes Immobilières

| Aspect | Détail |
|--------|--------|
| **Problème** | La vente immobilière implique de nombreux documents (titres fonciers, certificats d'urbanisme, diagnostics, actes de propriété) traités manuellement |
| **Solution** | Application OCR + workflow pour digitaliser tout le cycle de vente |
| **Architecture** | Identique à FRIDA : Angular + Spring Boot + OCR API |

**Modèle de données :**
```
VenteEntity
  ├── 1:1 → VendeurEntity → 1:1 → IdentitesEntity
  ├── 1:1 → AcquéreurEntity → 1:1 → IdentitesEntity
  ├── 1:N → BienImmobilierEntity (parcelles, lots)
  ├── 1:1 → PrixEntity (prix, frais, taxes)
  ├── 1:N → DocumentEntity (titre foncier, diagnostic, etc.)
  └── 1:N → TemoinEntity → 1:1 → IdentitesEntity
```

**Documents OCR à traiter :**
- Titre foncier / certificat de propriété
- Certificat d'urbanisme
- Diagnostics techniques (amiante, plomb, DPE)
- Attestations notariées antérieures

**Valeur ajoutée :** Calcul automatique des droits d'enregistrement, frais de notaire, plus-values.

---

### 2. 💍 NIKAH — Gestion des Contrats de Mariage

| Aspect | Détail |
|--------|--------|
| **Problème** | Les dossiers de mariage nécessitent la collecte et vérification de nombreuses pièces d'identité et certificats |
| **Solution** | Digitalisation du processus de constitution du dossier de mariage |

**Modèle de données :**
```
MariageEntity
  ├── 1:1 → EpouxEntity → 1:1 → IdentitesEntity
  ├── 1:1 → EpouseEntity → 1:1 → IdentitesEntity
  ├── 1:N → TemoinEntity → 1:1 → IdentitesEntity
  ├── 1:1 → RegimeMatrimonialEntity
  ├── 1:N → DotEntity (biens dotaux)
  └── 1:1 → ContratEntity (clauses, conditions)
```

**Documents OCR :**
- Extraits de naissance des époux
- Certificats de célibat / non-remariage
- CNI / Passeports
- Certificats médicaux
- Autorisations parentales (si mineurs)

**Valeur ajoutée :** Vérification automatique de la cohérence des documents, détection d'anomalies (âge minimum, liens de parenté).

---

### 3. 🏠 WAKF — Gestion des Donations / Legs (Waqf & Libéralités)

| Aspect | Détail |
|--------|--------|
| **Problème** | Les actes de donation et les constitutions de waqf nécessitent des vérifications complexes sur les biens et les bénéficiaires |
| **Solution** | Plateforme de gestion des donations avec vérification documentaire automatisée |

**Modèle de données :**
```
DonationEntity
  ├── 1:1 → DonateurEntity → 1:1 → IdentitesEntity
  ├── 1:N → DonataireEntity → 1:1 → IdentitesEntity
  ├── 1:N → BienDonneEntity (immobilier, mobilier)
  ├── 1:1 → ConditionsEntity (charges, réserves)
  └── 1:N → DocumentEntity
```

**Valeur ajoutée :** Calcul des droits de donation, vérification de la quotité disponible, alertes sur les réserves héréditaires.

---

### 4. 📋 TAWKIL — Gestion des Procurations

| Aspect | Détail |
|--------|--------|
| **Problème** | Les procurations sont fréquentes et leur gestion (rédaction, vérification d'identité, suivi de validité) est chronophage |
| **Solution** | Système de création, suivi et révocation des procurations |

**Modèle de données :**
```
ProcurationEntity
  ├── 1:1 → MandantEntity → 1:1 → IdentitesEntity
  ├── 1:N → MandataireEntity → 1:1 → IdentitesEntity
  ├── 1:1 → ObjetEntity (pouvoirs accordés)
  ├── 1:1 → ValiditeEntity (dates, conditions)
  └── 1:1 → RevocationEntity (si applicable)
```

**Valeur ajoutée :** Alertes d'expiration, génération automatique de l'acte à partir des données OCR, registre numérique consultable.

---

### 5. 📊 IQRAR — Gestion des Attestations et Certificats

| Aspect | Détail |
|--------|--------|
| **Problème** | Le notaire produit de nombreuses attestations (notoriété, hérédité, conformité) avec un processus très manuel |
| **Solution** | Génération automatisée d'attestations à partir de modèles et de données OCR |

**Types d'attestations :**
- Attestation de notoriété (après décès)
- Certificat d'hérédité
- Attestation de propriété immobilière
- Certificat de conformité
- Attestation de non-opposition

**Valeur ajoutée :** Templates intelligents, pré-remplissage automatique depuis la base de données, numérotation séquentielle, archivage numérique certifié.

---

### 6. 🏛️ SIJIL — Registre Notarial Numérique (Répertoire)

| Aspect | Détail |
|--------|--------|
| **Problème** | Le répertoire des actes notariés est souvent tenu manuellement (registre papier) |
| **Solution** | Répertoire numérique centralisé avec recherche avancée et statistiques |

**Fonctionnalités :**
- Enregistrement automatique de chaque acte
- Recherche par type d'acte, date, parties, bien
- Statistiques d'activité (nombre d'actes par mois, types les plus fréquents)
- Export réglementaire pour les autorités de tutelle
- Traçabilité et horodatage certifié

---

### 7. 💰 HISAB — Gestion de la Comptabilité Notariale

| Aspect | Détail |
|--------|--------|
| **Problème** | La comptabilité notariale (compte client, consignations, débours) est spécifique et complexe |
| **Solution** | Module comptable intégré aux actes notariés |

**Fonctionnalités :**
- Compte séquestre par dossier
- Suivi des encaissements et décaissements
- Calcul automatique des émoluments (barème officiel)
- Rapprochement bancaire
- Déclarations fiscales automatisées

---

## 🔗 Vision d'Ensemble : Écosystème Intégré

```
                    ┌──────────────────┐
                    │   🔍 OCR Engine  │
                    │ Service partagé  │
                    └──┬──┬──┬──┬──┬──┘
                       │  │  │  │  │
        ┌──────────────┘  │  │  │  └──────────────┐
        │          ┌──────┘  │  └──────┐           │
        ▼          ▼         ▼         ▼           ▼
   ┌─────────┐ ┌────────┐ ┌───────┐ ┌────────┐ ┌────────┐
   │ 🏠 FRIDA│ │📜 VENTA│ │💍NIKAH│ │🏠 WAKF │ │📋TAWKIL│
   │Succes.  │ │Ventes  │ │Mariage│ │Donat.  │ │Procur. │
   └──┬──────┘ └──┬─────┘ └──┬────┘ └──┬─────┘ └──┬─────┘
      │           │          │         │           │
      └─────┬─────┴────┬─────┴────┬────┴───────────┘
            │          │          │
            ▼          ▼          ▼
      ┌──────────┐ ┌────────┐ ┌────────┐
      │📊 IQRAR  │ │🏛️SIJIL │ │💰HISAB │
      │Attestat. │ │Répert. │ │Compta. │
      └──────────┘ └────────┘ └────────┘
```

> **💡 Astuce :** Tous ces projets peuvent **réutiliser le moteur OCR de FRIDA** et le composant `IdentitesEntity` déjà existant. L'investissement initial est largement amorti.

---

## 📐 Stratégie de Réalisation

### Phase 1 — Fondations (déjà fait avec FRIDA ✅)
- Moteur OCR opérationnel
- Architecture microservices validée
- Modèle d'identités unifié

### Phase 2 — Extensions prioritaires
| Priorité | Projet | Justification |
|----------|--------|---------------|
| 🔴 Haute | **VENTA** (Ventes) | Volume élevé d'actes, fort impact business |
| 🔴 Haute | **IQRAR** (Attestations) | Complémentaire à FRIDA (attestation de notoriété post-succession) |
| 🟡 Moyenne | **NIKAH** (Mariages) | Réutilise fortement l'OCR existant |
| 🟡 Moyenne | **TAWKIL** (Procurations) | Acte fréquent, workflow simple |

### Phase 3 — Écosystème complet
| Priorité | Projet | Justification |
|----------|--------|---------------|
| 🟢 Standard | **WAKF** (Donations) | Moins fréquent mais à forte valeur ajoutée |
| 🟢 Standard | **SIJIL** (Répertoire) | Agrège toutes les applications |
| 🟢 Standard | **HISAB** (Comptabilité) | Nécessite les autres modules en amont |

---

## 🛠️ Avantages Techniques de cette Approche

| Avantage | Description |
|----------|-------------|
| **Réutilisation du code** | IdentitesEntity, OCR API, frontend components partagés |
| **Architecture éprouvée** | Stack Angular + Spring Boot + PostgreSQL déjà validée |
| **Montée en compétence** | L'équipe connaît déjà la stack |
| **Déploiement uniforme** | Docker + même infrastructure pour tous les projets |
| **Données interconnectées** | Un client identifié dans FRIDA est retrouvé dans VENTA, NIKAH, etc. |

> **⚠️ Important :** Le point clé est la **mutualisation du moteur OCR** et du **modèle d'identités**. Chaque nouveau projet ne nécessite que la modélisation du métier spécifique (vente, mariage, donation…), le socle technique étant déjà en place.
