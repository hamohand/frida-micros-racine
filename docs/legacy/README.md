# Documentation legacy

Ce dossier contient des documents **conservés pour historique** mais qui ne reflètent plus la réalité du projet. Ne pas s'y référer pour des décisions actives.

## Contenu

### `STRATEGIE_PRODUCTION.md`
Note de vision produit rédigée avant la refonte de l'installation notaire et de l'architecture SaaS. Devenue obsolète parce que :

- **§3 « Monolithe hybride »** : déjà réalisé — le module `calculs/` est intégré au backend (commit `513bd1f`).
- **§6 « Setup.exe via Inno Setup + Docker Desktop »** : abandonné au profit de **WSL Ubuntu + Docker natif + zip/.bat/.ps1** (voir `install-notaire/documentation/INSTALLATION_PROCEDURE.md`).
- Ignore les 3 composantes actuelles (local notaire, démo VPS, calc SaaS).

Ce qui reste éventuellement pertinent :
- **§4 Traitement par lots nocturne** : idée d'évolution jamais implémentée, peut inspirer un futur mode asynchrone.
- **§5 Stratégie de sauvegarde** : partiellement couverte aujourd'hui par `sauvegarder.bat` livré au notaire.

### `DEPLOYMENT.md`
Guide de déploiement générique (Blue-Green, K8s, registry Azure, hostnames `frida.example.com`). Rédigé avant que la vraie stratégie ne soit fixée. Ne correspond ni au déploiement VPS actuel (Hostinger + Traefik mutualisé + 3 composantes) ni à l'installation notaire.

## Pour la documentation à jour

| Sujet | Doc active |
|---|---|
| Déploiement VPS (Composantes 2, 3, licences) | `docs/architecture/deploiement_vps.md` |
| Infra licences | `docs/architecture/configuration_licences_vps.md` |
| Installation chez un notaire (Composante 1) | `install-notaire/documentation/README.md` |
| Architecture SaaS / 3 composantes | `docs/architecture/architecture_saas_frida.md` |
| Correspondance compose ↔ composante | `compose/README.md` |
