# Préparation du package d'installation — Composante 1 (local notaire)

Ce document décrit comment **préparer** l'archive à envoyer au notaire. Il s'adresse au développeur qui prépare le livrable, pas au notaire lui-même (voir `install-notaire/docker-installation/LISEZMOI.txt` pour le notaire).

---

## Vue d'ensemble

Le notaire reçoit un dossier contenant 6 fichiers :

```
├── Installer-Frida.bat         ← script de lancement (demande droits admin)
├── Installer-Frida.ps1         ← script principal PowerShell
├── frida-micros.zip            ← sources FRIDA (backend + frontend + ocr-api + compose + .env)
├── sauvegarder.bat             ← sauvegarde manuelle (copié par l'installeur dans l'install)
├── desinstaller.bat            ← désinstallation (copié par l'installeur dans l'install)
└── LISEZMOI.txt                ← notice utilisateur
```

Ces 6 fichiers sont dans `install-notaire/docker-installation/` du dépôt.

`sauvegarder.bat` et `desinstaller.bat` agissent sur l'installation (`%USERPROFILE%\Frida-Micros`),
pas sur le dossier d'où ils sont lancés : l'installeur les y recopie en phase 2 pour que le notaire
les trouve à côté de ses données. Ils pilotent Docker **via WSL** (`wsl -u root -d Ubuntu -e bash -c ...`),
puisque Docker n'est pas installé côté Windows.

## Stratégie technique

- **Docker natif dans WSL Ubuntu** (pas Docker Desktop) — voir `docs/architecture/architecture_saas_frida.md` et la note dans `CLAUDE.md`
- Le script d'install : (1) installe WSL Ubuntu si absent, (2) extrait le zip et recopie les scripts de maintenance, (3) installe Docker via `get.docker.com` dans WSL, (4) lance `docker compose up -d --build`, (5) crée les lanceurs `Demarrer-Frida.bat` et `Arreter-Frida.bat` sur le Bureau.
- Le chemin WSL de l'installation est obtenu avec `wslpath` sur `$env:USERPROFILE`, et non reconstruit à partir de `$env:USERNAME` : le dossier de profil ne porte pas toujours le nom du compte (compte Microsoft, poste en domaine).
- Le port web est relu depuis `.env` (`PORT_WEB`) pour que les raccourcis et l'ouverture du navigateur pointent au bon endroit.

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
3. Livrer les 6 fichiers du dossier `install-notaire/docker-installation/`. `sauvegarder.bat` et `desinstaller.bat` ne sont **pas** dans `frida-micros.zip` : ils voyagent à côté et c'est l'installeur qui les recopie dans l'installation.
4. Vérifier l'adresse de support en fin de `LISEZMOI.txt` (actuellement `mohhamroun@gmail.com`).
5. Tester l'installation complète sur une VM Windows 10/11 vierge **avant** l'envoi (voir `CHECKLIST_TEST_TERRAIN.md`).

## Prérequis chez le notaire

- Windows 10/11 (64 bits)
- 8 Go de RAM minimum (16 Go recommandé)
- 10 Go d'espace disque
- Droits administrateur (pour installer WSL)
- Connexion Internet (installation initiale uniquement)

## URLs après installation

- Application FRIDA : http://localhost (port 80 par défaut, ajustable via `PORT_WEB` dans `.env`)
- Backend Swagger : http://localhost/swagger-ui.html — le backend n'expose **aucun port** sur l'hôte ; nginx proxifie `/swagger-ui/`, `/swagger-ui.html` et `/v3/api-docs/` vers `backend:8080` (voir `frontend/default.conf`). Si `PORT_WEB` a été changé, adapter le port.
