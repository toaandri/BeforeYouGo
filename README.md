# BeforeYouGo

**La petite checklist qui vérifie que vous n’oubliez rien avant de partir.**

BeforeYouGo est une application Android native, simple et entièrement hors ligne. Elle permet d’enregistrer ses objets indispensables, d’ajouter des rappels pour une date précise et de consulter une liste à cocher avant de quitter la maison.

## Fonctionnalités

- Créer des **essentiels quotidiens** : clés, portefeuille, lunettes, etc.
- Ajouter des objets **ponctuels** à ne pas oublier à une date donnée.
- Afficher une liste du jour réunissant les essentiels et les rappels en retard ou prévus aujourd’hui.
- Cocher les objets déjà préparés.
- Modifier ou supprimer les objets enregistrés.
- Configurer ou désactiver une notification quotidienne à l’heure souhaitée.
- Conserver toutes les données localement, sans compte, serveur ni connexion Internet.

## Utilisation

1. Ouvrir l’onglet **Mes objets** puis sélectionner **Ajouter**.
2. Laisser l’option « Essentiel quotidien » cochée pour un objet à prendre chaque jour.
3. La décocher pour ajouter un rappel ponctuel et saisir sa date au format `AAAA-MM-JJ`.
4. Dans **Aujourd’hui**, cocher les objets une fois prêts. Les rappels ponctuels cochés sont retirés de la liste.
5. Dans **Réglages**, choisir une heure comme `08:00`, puis activer le rappel. Android demandera l’autorisation des notifications si nécessaire.

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

L’interface est volontairement rudimentaire. Le MVP ne comporte pas encore de synchronisation, widget, export/sauvegarde, report de rappel ni vérification automatique de la présence d’un objet.

## Licence

Aucune licence open source n’a encore été choisie. Les contenus de ce dépôt ne sont donc pas automatiquement autorisés à la réutilisation.
