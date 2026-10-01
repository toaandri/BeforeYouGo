# BeforeYouGo

**La petite checklist qui vérifie que vous n’oubliez rien avant de partir.**

BeforeYouGo est une application Android native, simple et entièrement hors ligne. Elle permet d’enregistrer ses objets indispensables, d’ajouter des rappels pour une date précise et de consulter une liste à cocher avant de quitter la maison.

## Fonctionnalités

- Créer des **essentiels quotidiens** : clés, portefeuille, lunettes, etc.
- Ajouter des objets **ponctuels** à ne pas oublier à une date donnée.
- Afficher une liste du jour réunissant les essentiels et les rappels en retard ou prévus aujourd’hui.
- Cocher les objets déjà préparés.
- Modifier ou supprimer les objets enregistrés.
- Créer plusieurs routines : nom, heure, jours de la semaine, mode **Sonnerie** ou **Notification**, vibration et report de 5/10/15 minutes.
- Associer une checklist à chaque départ, passer la prochaine occurrence et réactiver une routine.
- Parcourir 36 objets du quotidien avec catégories, recherche et icônes conservées dans les listes.
- Choisir une apparence claire, sombre ou synchronisée avec le système.
- Conserver toutes les données localement, sans compte, serveur ni connexion Internet.

## Utilisation

1. Dans **Objets**, utiliser **Du quotidien** pour ajouter des idées, ou le **+** pour un objet personnel. Choisir son icône et, si besoin, une date avec le calendrier.
2. Dans **Aujourd’hui**, sélectionner **Créer une alarme** : nommer le départ, toucher l’heure et choisir les jours, le mode d’alerte et les objets concernés. « Sonnerie » lance une alarme audible ; « Notification » affiche seulement un rappel discret avec les actions Ouvrir et Reporter.
3. Dans **Réglages**, autoriser les notifications et les alarmes exactes Android. Les alarmes en attente d’autorisation sont signalées et ne sont pas présentées comme opérationnelles.
4. Cocher les objets une fois prêts. Les objets ponctuels restent décochables le jour même, puis sont archivés au changement de jour s’ils ont été cochés.
5. À la sonnerie, utiliser **Arrêter** ou **Reporter** dans la notification ou l’application. Une sonnerie dure au maximum deux minutes ; un essai dure dix secondes. Les notifications discrètes ne lancent pas de sonnerie et se contrôlent depuis la barre de notifications.

Chaque alarme se répète les jours choisis en heure locale. Les changements de fuseau et d’heure, les mises à jour et le redémarrage reprogramment les alarmes. Un téléphone éteint ne peut pas sonner ; après un arrêt forcé Android de l’application, la rouvrir. Le volume Alarme, les autorisations et les restrictions du fabricant restent applicables. Aucune permission plein écran n’est demandée.

## Prérequis

- Android Studio récent
- JDK 11 ou plus récent
- Un émulateur ou un appareil Android avec Android 8.0 (API 26) minimum

## Lancer le projet

1. Cloner le dépôt et ouvrir son dossier dans Android Studio.
2. Attendre la synchronisation Gradle.
3. Sélectionner la configuration `app` puis un appareil de test.
4. Lancer l’application.

Pour compiler depuis Windows :

```powershell
.\gradlew.bat assembleDebug
```

L’APK est alors disponible dans `app/build/outputs/apk/debug/app-debug.apk`.

Pour exécuter les tests unitaires :

```powershell
.\gradlew.bat testDebugUnitTest
```

## Choix techniques

- Kotlin et Jetpack Compose avec Material 3
- Stockage local dans `SharedPreferences` au format JSON, adapté au petit volume de données du MVP
- `AlarmManager` et canaux de notification Android pour le rappel quotidien
- Règles de checklist isolées et couvertes par un test unitaire

## Vie privée

BeforeYouGo ne crée aucun compte et n’envoie aucune donnée : les listes et préférences restent sur l’appareil.

## Limites actuelles

Pas de synchronisation, widget ou détection automatique de la présence d’un objet. Les checklists partagent l’état « prêt » d’un objet pour la journée. La validation de publication Google Play et la matrice complète d’appareils restent à réaliser (voir `docs/PLAY_STORE_RELEASE.md`).

## Tests sur un téléphone personnel

Éviter `connectedDebugAndroidTest` sur un appareil contenant des données à conserver : selon le lanceur Gradle, l’application peut être désinstallée avant les tests. Compiler avec `assembleDebugAndroidTest`, puis installer les deux APK avec `adb install -r` (et `-t` pour l’APK de test), ouvrir l’application et utiliser `adb shell am instrument -w com.toaandri.beforeyougo.test/androidx.test.runner.AndroidJUnitRunner`. Ne pas utiliser l’option `-g` sur les appareils qui bloquent l’octroi automatique des permissions. Les tests de stockage utilisent des préférences isolées ; l’essai sonore temporaire restaure la liste des alarmes dans un bloc `finally`.

## Licence

Aucune licence open source n’a encore été choisie. Les contenus de ce dépôt ne sont donc pas automatiquement autorisés à la réutilisation.
