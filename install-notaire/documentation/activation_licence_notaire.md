# Activation de Licence - Client (Chez le Notaire)

Ce guide détaille l'activation du logiciel Frida sur les PC des clients (Notaires) à l'aide du système de licences.

---

## Installation Client (Chez le Notaire)

Cette partie concerne le déploiement physique du logiciel Frida sur les PC des clients.

### 1. Préparation de la machine
1. Le notaire doit disposer d'un environnement Java/Python fonctionnel (selon votre méthode d'installation actuelle : exécutable, script batch, etc.).
2. L'ordinateur **doit avoir accès à Internet** au moins lors de la première activation et à chaque démarrage pour valider la clé.

### 2. Déploiement du Code Local
Assurez-vous que le code que vous installez correspond à la dernière version de la branche `main` (contenant les intercepteurs de licence et la page d'activation).
- Compilez le Backend (Spring Boot) : `mvn clean package`
- Compilez le Frontend (Angular) : `npm run build`

### 3. Premier Lancement
1. Lancez le Backend Spring Boot local.
2. Lancez le Frontend Angular local (ou servez-le via Nginx local).
3. Le notaire ouvre Frida dans son navigateur (ex: `http://localhost:4200`).
4. **Comportement attendu** : L'accès à l'application est bloqué. Le système redirige automatiquement le notaire vers la page `/license`.

### 4. Activation de la Licence
1. Le notaire saisit la clé que vous lui avez fournie (ex: `FRIDA-XXXX-YYYY`).
2. Le système local génère l'identifiant matériel (`Hardware ID`) de la machine.
3. Il envoie la requête à votre VPS (`licences.frida.enclume-numerique.com`).
4. Si la clé est valide et n'est pas déjà liée à un autre PC, elle est activée et **définitivement liée** à cet ordinateur.
5. Une copie locale (`uploads/license.key`) est sauvegardée pour les prochains démarrages.

> [!TIP]
> Si le notaire tente de copier le dossier d'installation sur le PC de son collègue, le système générera un nouveau `Hardware ID` différent. Le VPS rejettera alors la validation, empêchant le piratage. S'il souhaite utiliser Frida sur un 2ème poste, vous devrez lui générer une 2ème clé.

### 5. En cas de Révocation ou d'Expiration
Si vous révoquez la clé depuis votre Dashboard, ou si la durée de validité est dépassée, le Frida local affichera de nouveau la page d'erreur au prochain démarrage ou appel d'API, bloquant ainsi l'accès.
