# 🧪 Guide des Tests - Projet Frida (Backend)

Ce document centralise les bonnes pratiques et les commandes utiles pour garantir la fiabilité du code grâce aux tests automatisés.

## 1. Philosophie & Quand tester ?

Dans l'écosystème Frida, nous appliquons une logique pragmatique quant à l'écriture des tests :

- **Règles Métiers ("Business Logic")** : À tester **impérativement**. Par exemple, `HeirPartCalculatorService` calcule l'héritage. Une erreur ici entraîne des conséquences graves.
- **Réduction de dettes & Réfusinage (Refactoring)** : Si vous devez revoir l'architecture d'un service (ex: l'ancien `EcrireBdService`), écrivez ou maintenez un test "Filet de sécurité" avant de casser le code de production.
- **Non-régression des bugs** : Chaque fois qu'un bug critique est remonté et localisé, il faut écrire un test unitaire qui *reproduit* l'anomalie, puis la corriger pour que le test passe.
- **NE PAS TESTER le Framework** : Inutile de tester si Spring Boot injecte bien un Bean, ou si la sauvegarde JPA (save) écrit bien dans la DB. L'équipe Spring l'a déjà testé.

---

## 2. L'Anatomie d'un Test (Pattern A-A-A)

Un test propre et lisible respecte la structure `Arrange`, `Act`, `Assert` (3A).

```java
@Test
void calculerCoefficient_ShouldCalculateCorrectPercentage() {
    // 1. ARRANGE (Préparer : Initialiser les données et les mocks)
    HeirPartCalculatorService calculatorService = new HeirPartCalculatorService();
    int numerateur = 5;
    int denominateur = 40;

    // 2. ACT (Agir : Ce qu'on veut réellement tester)
    float resultat = calculatorService.calculerCoefficient(numerateur, denominateur);

    // 3. ASSERT (Vérifier : S'assurer que le résultat correspond aux attentes)
    assertEquals(0.13f, resultat); // On vérifie l'arrondi (ex: 5/40 = 0.125 arrondi à 0.13)
}
```

---

## 3. Outils Autorisés (Stack technique de test)

1. **JUnit 5** (`@Test`, `@BeforeEach`, `assertEquals`, `assertThrows`) : Pour structurer l'exécution des fonctions tests.
2. **Mockito** (`@Mock`, `@InjectMocks`, `when().thenReturn()`) : Essentiel pour isoler un composant. (Si l'on teste `ServiceA` qui appelle `ServiceB`, on "Mock" `ServiceB` pour ne pas tester deux choses à la fois).

---

## 4. Exécuter les tests : Commandes Utiles

### A. Depuis l'IDE (VSCode, IntelliJ, Eclipse)
- Ouvrez n'importe quel fichier sous `backend/src/test/java/...`
- Cliquez sur le lien `Run` ou `Play` (▶️) affiché par votre IDE juste au-dessus du nom de la méthode ou de la classe.
- *Avantage* : Extrêmement rapide pour déboguer une méthode directement.

### B. Depuis le Terminal (via Maven local)
Si Java et Maven sont installés sur votre machine (en dehors de Docker).
```bash
# Se placer dans le bon répertoire
cd backend

# Lancer la totalité de la suite de tests
mvn clean test

# Lancer une classe spécifique
mvn test -Dtest="HeirPartCalculatorServiceTest"
```

### C. Depuis Docker (La méthode infaillible)
Si vous ne voulez pas polluer votre machine ou que vous voulez les strictes versions de l'environnement de production.
```bash
# À la racine du projet, lance un conteneur temporaire qui joue la suite de tests
docker-compose run --rm backend mvn test
```
*Note: Le flag `--rm` garantit que le conteneur est détruit une fois le test terminé.*

---

## 5. Bonnes Pratiques de Nommage

La convention de nommage des méthodes de test est celle du "Should":
`nomDeLaMethode_ConditionInitiale_ShouldResultatAttendu()`

Exemples corrects : 
- ✅ `genererIdentifiant_WithNullDate_ShouldOnlyReturnTimestamp()`
- ✅ `calculerParts_WhenApiThrowsException_ShouldWrapAndRethrow()`

Ceci permet, lors de l'échec de 50 tests sur un rapport en Intégration Continue (CI), de comprendre immédiatement ce qui a planté sans devoir ouvrir le code source.
