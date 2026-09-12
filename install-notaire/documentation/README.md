# Documentation — Installation notaire (Composante 1)

Tous les documents nécessaires à l'installation de FRIDA sur le poste d'un notaire (Composante 1 : local notaire, Docker natif dans WSL Ubuntu).

## Index

| Document | Audience | Objet |
|---|---|---|
| [INSTALLATION_PROCEDURE.md](INSTALLATION_PROCEDURE.md) | Développeur | Préparation du package livrable (régénération du zip, prérequis, ce qu'il faut vérifier avant envoi) |
| [construire_package.ps1](construire_package.ps1) | Développeur | Script : régénère `frida-micros.zip` et assemble `FRIDA-Installation.zip` en un passage |
| [CHECKLIST_TEST_TERRAIN.md](CHECKLIST_TEST_TERRAIN.md) | Développeur sur place | Checklist à dérouler pendant l'installation chez le notaire (phases, timings, points de rupture connus, modèle de rapport) |
| [activation_licence_notaire.md](activation_licence_notaire.md) | Développeur / client | Flux d'activation de licence (**actuellement suspendu** — voir l'avertissement en tête du fichier) |
| [`../docker-installation/LISEZMOI.txt`](../docker-installation/LISEZMOI.txt) | Notaire | Notice pas-à-pas remise au notaire, incluse dans le livrable |

## Livrable envoyé au notaire

Contenu du dossier `install-notaire/docker-installation/` :

```
├── Installer-Frida.bat         ← lancement (droits admin)
├── Installer-Frida.ps1         ← script principal PowerShell
├── frida-micros.zip            ← sources FRIDA (backend + frontend + ocr-api + compose + .env)
├── LISEZMOI.txt                ← notice utilisateur
├── desinstaller.bat            ← maintenance
└── sauvegarder.bat             ← maintenance
```

Construit par `construire_package.ps1` (voir `INSTALLATION_PROCEDURE.md`) après une évolution du code — ne pas compresser tout le dossier `docker-installation/` : `docker-compose.local.yml` et `.env.local` sont déjà repris à l'intérieur de `frida-micros.zip`.

## Contexte

- Architecture globale et les 3 composantes : `../../docs/architecture/architecture_saas_frida.md`
- Notes générales projet : `../../CLAUDE.md`
