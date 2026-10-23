# Implémentation de la Démo Publique Bridée (`DEMO_BRIDEE=true`)

> [!IMPORTANT]
> **Règle d'or respectée** :
> - **Poste notaire réel** (`DEMO_BRIDEE=false`) : toutes les fonctionnalités locales sont actives, aucune donnée ne sort du poste.
> - **Serveur de démonstration en ligne** (`https://frida.enclume-numerique.com`, `DEMO_BRIDEE=true`) : accès libre, **0 upload de fichier personnel du visiteur**, construction pas-à-pas d'une fiche Frida exactement comme sur la version locale mais en puisant **exclusivement** dans le dossier en ligne `famille` (15 actes fictifs validés par empreinte SHA-256 côté serveur), administration masquée et bloquée, quotas journaliers (3/IP/jour et 30/total/jour), filigrane `SPECIMEN` sur Word/PDF et purge automatique nocturne.

---

## 1. Dossier en ligne `famille` (15 actes fictifs avec QR Code)

Les documents fictifs (famille **KACEM Ahmed** / **MANSOURI Fatima**) générés par [`scripts/generer_actes_naissance.py`](../../scripts/generer_actes_naissance.py) et [`scripts/generer_actes_deces.py`](../../scripts/generer_actes_deces.py) sont embarqués dans [`backend/src/main/resources/demo-specimens/famille/`](../../backend/src/main/resources/demo-specimens/famille/) :

- **2 actes de décès fictifs** avec QR Code (`adc_01`) :
  - `epoux_deces.png` (Acte de décès — Époux : KACEM Ahmed)
  - `epouse_deces.png` (Acte de décès — Épouse : MANSOURI Fatima)
- **13 extraits de naissance fictifs** avec QR Code (`en_01_qrcode_01`) :
  - `epoux.png`, `epouse.png`
  - `fils_1.png`, `fils_2.png`, `fils_3.png`
  - `fille_1.png`, `fille_2.png`
  - `pere.png`, `mere.png`
  - `frere.png`, `soeur.png`
  - `temoin_1.png`, `temoin_2.png`

---

## 2. Sécurité & Restrictions Backend (`DEMO_BRIDEE=true`)

- **[`application.properties`](../../backend/src/main/resources/application.properties)** :
  - `app.demo-bridee=${DEMO_BRIDEE:false}`
  - `app.demo-bridee.max-dossiers-total=${DEMO_MAX_DOSSIERS_TOTAL:30}`
  - `app.demo-bridee.max-dossiers-par-ip=${DEMO_MAX_DOSSIERS_PAR_IP:3}`
- **[`DemoFamilleService.java`](../../backend/src/main/java/com/muhend/backendai/service/demo/DemoFamilleService.java)** & **[`DemoController.java`](../../backend/src/main/java/com/muhend/backendai/controller/DemoController.java)** :
  - `GET /api/demo/famille` : liste les 15 actes fictifs du dossier `famille` en ligne.
  - `GET /api/demo/famille/{nomFichier}` : sert le fichier PNG correspondant.
  - Calcul au démarrage des empreintes **SHA-256** des 15 fichiers autorisés.
- **Vérification stricte SHA-256 sur les endpoints d'upload** :
  - [`FileController.java`](../../backend/src/main/java/com/muhend/backendai/controller/FileController.java) (`POST /api/files/upload`) et [`OcrProcessingController.java`](../../backend/src/main/java/com/muhend/backendai/controller/OcrProcessingController.java) (`POST /api/pdfs/extraire-noms-qr`) rejettent (`HTTP 403`) tout fichier dont le hash SHA-256 ne correspond pas exactement à l'un des 15 fichiers du dossier `famille`. Aucun document personnel ne peut donc être envoyé sur le VPS.
- **[`DemoBrideeGuardFilter.java`](../../backend/src/main/java/com/muhend/backendai/config/security/DemoBrideeGuardFilter.java)** :
  - Bloque (`HTTP 403`) : `POST /api/pdfs/ocr-cni-front`, `POST /api/pdfs/mobile-mrz`, `/api/backups/**`, `/api/archives/**`, `/api/telechargements/**`, `/api/license/**`, `/api/nfc-session/**`, `/api/auth/users/**`, `/api/auth/register`, `PUT /api/parametres`, toutes les requêtes `DELETE` sur `/api/frida/**`, `/api/brouillons/**`, `/api/folders/**`, ainsi que Swagger/OpenAPI.
  - Quotas (`HTTP 429`) : **3 créations/IP/jour** et **30 créations/jour au total** sur `POST /api/folders/create` et `POST /api/demo/creer-dossier-exemple`.
- **[`WordGenerationService.java`](../../backend/src/main/java/com/muhend/backendai/service/document/WordGenerationService.java)** :
  - Filigrane **`SPECIMEN`** diagonal ajouté automatiquement aux documents Word (`.docx`) et PDF (`.pdf`) lorsque `app.demo-bridee=true`.

---

## 3. Parcours Utilisateur Identique au Local (`FamillePickerModalComponent`)

- **[`famille-picker-modal.component.ts`](../../frontend/src/app/shared/famille-picker-modal/famille-picker-modal.component.ts)** :
  - En mode `DEMO_BRIDEE=true`, tout clic sur un bouton de sélection de fichier ouvre l'explorateur du dossier en ligne **`famille`** au lieu de l'explorateur de disque local du navigateur.
- **[`create-person.component.ts`](../../frontend/src/app/components/dossier/create-person/create-person.component.ts)**, **[`file-upload.component.ts`](../../frontend/src/app/components/dossier/file-upload/file-upload.component.ts)** & **[`upload-windows.component.ts`](../../frontend/src/app/components/dossier/upload-windows/upload-windows.component.ts)** :
  - Le visiteur suit **exactement** le même parcours étape par étape que sur la version locale :
    1. Création du dossier (saisie manuelle ou bouton *« Remplir via QR Code (Acte de décès) »* en choisissant `epoux_deces.png` ou `epouse_deces.png` dans `famille`).
    2. Carrousel de dépôt des pièces (Défunt, Conjoint, Fils, Filles, Père, Mère, Frères/Sœurs, Témoins) en sélectionnant un ou plusieurs actes depuis `famille`.
    3. Lecture IA / QR Code (`/api/pdfs/lireai-ecrirebd`), vérification de la fiche, calcul des parts successorales et génération Word / PDF (`SPECIMEN`).
- **[`auth.service.ts`](../../frontend/src/app/services/auth.service.ts)**, **[`auth.guard.ts`](../../frontend/src/app/services/auth.guard.ts)** & **[`admin.component.ts`](../../frontend/src/app/components/admin/admin.component.ts)** :
  - Bandeau permanent bilingue d'avertissement en haut de l'écran.
  - Menu **Administration**, bouton **Déconnexion** et boutons de suppression masqués.
  - Garde `nonDemoBrideeGuard` bloquant `/users`, `/parametres`, `/backups`, `/license`, `/mobile-scanner/:sessionId`.

---

## 4. Service OCR Léger (QR Code Décès & Naissance)

- **[`ocr_engine_v2.py`](../../ocr-api/app_ocr/app/services/ocr_engine_v2.py)** & **[`ocr_engine.py`](../../ocr-api/app_ocr/app/services/ocr_engine.py)** :
  - Détection automatique du format QR Code **Acte de Décès** (`adc_01`) vs **Extrait de Naissance** (`en_01_qrcode_01`).
- **[`adc_01.json`](../../ocr-api/app_ocr/entities/adc_01.json)** & **[`en_01_qrcode_01.json`](../../ocr-api/app_ocr/entities/en_01_qrcode_01.json)** :
  - Entités QR Code embarquées dans `ocr-api/app_ocr/entities/` et montées dans [`compose/vps.demo.yml`](../../compose/vps.demo.yml).

---

## 5. Déploiement & Exploitation sur `srv816669` (`https://frida.enclume-numerique.com`)

### Mise à jour sur le VPS `srv816669`
```bash
cd ~/frida/frida-micros-racine
git pull origin main
docker compose -f compose/vps.demo.yml up -d --build
```

### Réinitialisation nocturne automatique (cron à 03h00)
Le script [`scripts/demo-reset.sh`](../../scripts/demo-reset.sh) vide chaque nuit les tables de dossiers/brouillons (`frida`, `defunt`, `heritier`, `temoin`, `calcul`, `identites`, `brouillon`) et nettoie `/app/uploads/*` :
```bash
chmod +x /root/frida/frida-micros-racine/scripts/demo-reset.sh
/root/frida/frida-micros-racine/scripts/demo-reset.sh

# Planifier tous les soirs à 03h00 :
(crontab -l 2>/dev/null; echo "0 3 * * * /root/frida/frida-micros-racine/scripts/demo-reset.sh >> /var/log/frida-demo-reset.log 2>&1") | crontab -
```
