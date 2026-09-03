# Préparation du package d'installation — Composante 1 (local notaire)

Ce document décrit comment **préparer** l'archive à envoyer au notaire. Il s'adresse au développeur qui prépare le livrable, pas au notaire lui-même (voir `install-notaire/docker-installation/LISEZMOI.txt` pour le notaire).

---

## Vue d'ensemble

Le notaire reçoit un dossier contenant 4 fichiers :

```
├── Installer-Frida.bat         ← script de lancement (demande droits admin)
├── Installer-Frida.ps1         ← script principal PowerShell
├── frida-micros.zip            ← sources FRIDA (backend + frontend + ocr-api + compose + .env)
└── LISEZMOI.txt                ← notice utilisateur
```

Ces 4 fichiers sont dans `install-notaire/docker-installation/` du dépôt.

## Stratégie technique

- **Docker natif dans WSL Ubuntu** (pas Docker Desktop) — voir `docs/architecture/architecture_saas_frida.md` et la note dans `CLAUDE.md`
- Le script d'install : (1) installe WSL Ubuntu si absent, (2) installe Docker via `get.docker.com` dans WSL, (3) extrait le zip, (4) lance `docker compose up -d --build`, (5) crée un lanceur `Demarrer-Frida.bat` sur le Bureau.

## Régénérer `frida-micros.zip` après une évolution du code

Depuis la racine du dépôt, en PowerShell :

```powershell
$stage = Join-Path $env:TEMP "frida-zip-stage"
if (Test-Path $stage) { Remove-Item $stage -Recurse -Force }
New-Item -ItemType Directory -Path $stage -Force | Out-Null

$proj = (Get-Location).Path

robocopy "$proj\backend"  "$stage\backend"  /E /XD target uploads /NFL /NDL /NJH /NJS /NC /NS /NP | Out-Null
robocopy "$proj\frontend" "$stage\frontend" /E /XD node_modules dist .angular /NFL /NDL /NJH /NJS /NC /NS /NP | Out-Null
robocopy "$proj\ocr-api"  "$stage\ocr-api"  /E /NFL /NDL /NJH /NJS /NC /NS /NP | Out-Null
Copy-Item "$proj\install-notaire\docker-installation\docker-compose.local.yml" $stage
Copy-Item "$proj\install-notaire\docker-installation\.env.local" $stage

$zip = "$proj\install-notaire\docker-installation\frida-micros.zip"
if (Test-Path $zip) { Remove-Item $zip -Force }
Compress-Archive -Path "$stage\*" -DestinationPath $zip -CompressionLevel Optimal
Remove-Item $stage -Recurse -Force
```

Le zip fait ~0,6 Mo (compressé) pour ~2 Mo de sources.

## Ce qu'il faut avant d'envoyer chez un client

1. `git pull` puis régénérer le zip (le zip est daté et gèle un état du code).
2. Vérifier que `.env.local` a des valeurs non compromises (le mot de passe DB dedans finira sur la machine du client).
3. Zipper les 4 fichiers du dossier `install-notaire/docker-installation/` (sauf `desinstaller.bat` et `sauvegarder.bat` qui doivent rester dans le dossier extrait — ils ne sont pas dans le zip mais accessibles au notaire pour la maintenance).
4. Tester l'installation complète sur une VM Windows 10/11 vierge **avant** l'envoi.

## Prérequis chez le notaire

- Windows 10/11 (64 bits)
- 8 Go de RAM minimum (16 Go recommandé)
- 10 Go d'espace disque
- Droits administrateur (pour installer WSL)
- Connexion Internet (installation initiale uniquement)

## URLs après installation

- Application FRIDA : http://localhost (port 80 par défaut, ajustable via `.env`)
- Backend Swagger : http://localhost:8080/swagger-ui.html (accès direct sans passer par le proxy Nginx)
