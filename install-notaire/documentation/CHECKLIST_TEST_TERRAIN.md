# Checklist de test terrain — Installation Composante 1 chez un notaire

Cette checklist accompagne le déploiement de la Composante 1 (local notaire, OCR lourd) chez un client. À imprimer ou avoir en fenêtre séparée pendant l'installation.

> Contexte : voir `INSTALLATION_PROCEDURE.md` (même dossier) pour la préparation du package et `../docker-installation/LISEZMOI.txt` pour la notice utilisateur remise au notaire.

---

## AVANT le test (à préparer)

- [ ] **Version Windows exacte** de la machine cliente : `winver` (10 21H2 ? 11 22H2 ? 11 24H2 ?)
- [ ] **RAM totale** : Task Manager → Performance → Memory
- [ ] **Espace disque C: libre** : minimum 15 Go recommandé
- [ ] **Antivirus actif** : Bitdefender/Kaspersky/Sophos peuvent bloquer WSL ou Docker — note le nom
- [ ] **Poste d'entreprise ?** : politique de groupe, proxy réseau, restrictions admin
- [ ] **Docker Desktop déjà installé ?** (`docker --version` en cmd Windows) — si oui, potentiel conflit avec Docker-in-WSL
- [ ] **WSL déjà présent ?** : `wsl -l -v` — note le résultat exact
- [ ] **Support de secours** : clé USB avec le zip + une VM portable si possible

## Phase 1 — Lancement Installer-Frida.bat

- [ ] **UAC apparaît ?** (bandeau bleu droits admin) → clique Oui
- [ ] Message « Droits administrateur confirmés »
- [ ] Si le script demande un **redémarrage** pour WSL → note l'heure, redémarre, chronométrage jusqu'à Ubuntu prêt
- [ ] Si Ubuntu demande de créer un user UNIX → **note le nom que tu crées** (utile pour debug)
- [ ] Après création user, RE-double-clic sur Installer-Frida.bat

## Phase 2 — Installation Docker dans WSL (⏱ 3–5 min)

**Le point le plus fragile — connectivité + apt + curl**

- [ ] Chronométrer la phase `[3/5] Installation de Docker`
- [ ] Regarder si `apt-get update` renvoie des erreurs (proxy, DNS)
- [ ] Regarder si `curl -fsSL https://get.docker.com | sh` télécharge sans warning
- [ ] Si échec : `wsl -d Ubuntu -e bash -c "curl -v https://get.docker.com"` pour capturer l'erreur exacte
- [ ] Après install : vérifier `wsl -u root -d Ubuntu -e docker --version` → doit renvoyer une version

## Phase 3 — Extraction zip

- [ ] Vérifier création `C:\Users\<nom>\Frida-Micros\` avec `backend/`, `frontend/`, `ocr-api/`, `docker-compose.local.yml`, `.env.local`
- [ ] Taille du dossier extrait : ~2 Mo

## Phase 4 — Build docker compose (⏱ 10–20 min — critique)

**Le vrai marathon. À surveiller de près.**

- [ ] Chronométrer et **note le temps total du build**
- [ ] Errors Maven côté backend (téléchargement dépendances) → si oui, connexion Internet stable ?
- [ ] Errors npm install côté frontend → même question
- [ ] Errors pip install côté ocr-api
- [ ] RAM au pic du build : `wsl -d Ubuntu -e bash -c "free -h"` — dépasse-t-on 6 Go ?
- [ ] Le PC swappe-t-il ? (disque à 100% en continu)

## Phase 5 — Démarrage des containers

- [ ] `wsl -u root -d Ubuntu -e bash -c "docker ps"` → **4 containers doivent être Up** : `frida-db`, `frida-ocr-api`, `frida-backend`, `frida-frontend`
- [ ] Après ~30 s : http://localhost s'ouvre-t-il ?
- [ ] Si erreur : quel container est en erreur ? `docker logs frida-backend` (ou autre)
- [ ] http://localhost:8080/swagger-ui.html accessible ?

## Phase 6 — Test fonctionnel de base

- [ ] Écran d'accueil rendu correctement (police, styles, icônes Material)
- [ ] Créer un nouveau dossier de test
- [ ] **Uploader un vrai extrait de naissance algérien** (celui du notaire) — c'est **le** vrai test OCR complet
- [ ] L'OCR extrait-il le NIN, la date, les noms correctement ?
- [ ] Le calcul de parts fonctionne-t-il sur une composition simple (conjoint + 2 enfants) ?
- [ ] Note les erreurs remontées à l'écran (screenshot)

## Phase 7 — Persistance et redémarrage

- [ ] `sauvegarder.bat` : produit-il un `data\backups\<date>\database.sql` valide ?
- [ ] **Redémarrer le PC** complètement
- [ ] Après redémarrage, double-clic sur `Demarrer-Frida` (bureau) → chronométrer jusqu'à écran d'accueil
- [ ] Le dossier de test créé avant est-il toujours là ?

## Phase 8 — Empreinte RAM en régime

Une fois tout démarré, note :

- [ ] `wsl -d Ubuntu -e bash -c "docker stats --no-stream --format 'table {{.Name}}\t{{.MemUsage}}'"`
- [ ] Task Manager Windows : consommation totale WSL (processus `vmmemWSL`)

**Cible réaliste** : 2–3 Go RAM totale (Docker + WSL + navigateur), pas plus.

## À ne pas oublier de rapporter

- [ ] **Screenshots** de chaque écran clé (accueil, upload, résultat calcul, erreurs)
- [ ] **Timing total** de l'install (de double-clic à navigateur ouvert)
- [ ] **Un test « démonter/remonter »** : `desinstaller.bat` (conservation données) + `Demarrer-Frida.bat` — les données sont bien préservées ?
- [ ] Sentiment du notaire — vitesse ressentie, ergonomie, ce qu'il trouve bizarre

## Points où on sait déjà que ça peut coincer

- **Réseau proxy d'entreprise** : `get.docker.com`, `deb.nodesource.com`, apt mirrors peuvent tous être bloqués. Prépare un plan B (installer Docker via un paquet `.deb` offline, ou proposer d'utiliser sa 4G mobile en hotspot temporaire pour l'install).
- **RAM < 8 Go** : le build risque de swap et prendre 30+ min.
- **Windows 10 < build 19041** : WSL 2 pas disponible. Vérifier au préalable.
- **Notaire pressé** : ne pas se laisser embarquer dans « c'est plus vite comme ça », suivre le script.

---

## Après le test — modèle de rapport

Compléter en revenant :

- **Machine cliente** : Windows _____, RAM _____ Go, disque libre _____ Go, antivirus _____
- **Réseau** : type de connexion, proxy éventuel, débit estimé
- **Timing** : install WSL _____ min, install Docker _____ min, build _____ min, TOTAL _____ min
- **Empreinte RAM au repos** : _____ Mo (docker stats)
- **Ce qui a marché du premier coup** : _____
- **Ce qui a coincé** : _____
- **Actions correctives à prendre au retour** : _____
- **Sentiment client** : _____
