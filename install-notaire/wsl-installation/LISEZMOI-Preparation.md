# Préparation du package d'installation pour le Notaire

Puisque le dépôt Github est privé, l'installation se fera via une archive `.zip`.
Voici la procédure exacte pour préparer le fichier à envoyer au notaire.

## Étape 1 : Créer l'archive `.zip` du projet
1. Allez dans le répertoire racine de votre projet (`frida-micros-racine`).
2. Sélectionnez les dossiers `backend` et `frontend`.
3. Compressez-les dans un fichier `.zip`.
4. Renommez impérativement ce fichier en : **`frida-micros.zip`**

*(Note : Ne mettez pas le dossier racine dans le zip, sélectionnez directement `backend` et `frontend` avant de zipper).*

## Étape 2 : Préparer le dossier d'installation
1. Récupérez les deux fichiers du dossier `install-notaire/wsl-installation` :
   - `Installer-WSL.bat`
   - `Installer-WSL.ps1`
2. Placez ces deux fichiers **dans le même dossier** que `frida-micros.zip`.

Vous devriez avoir un dossier à envoyer contenant :
- `Installer-WSL.bat`
- `Installer-WSL.ps1`
- `frida-micros.zip`

## Étape 3 : Déploiement chez le notaire
1. Envoyez ce dossier (par exemple sur clé USB ou lien de téléchargement) au notaire.
2. Demandez-lui de copier ce dossier sur son Bureau (ou dans Mes Documents).
3. Il lui suffit de double-cliquer sur **`Installer-WSL.bat`**.

Le script s'occupera automatiquement :
- D'installer WSL (avec redémarrage si nécessaire).
- De configurer l'environnement (Java, Node.js).
- D'extraire le projet dans `C:\Users\SonNom\Frida-Micros`.
- De placer un raccourci **`Demarrer-Frida.bat`** sur son Bureau pour le lancement quotidien de la plateforme.
