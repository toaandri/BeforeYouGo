# BeforeYouGo

**Un petit rituel avant de partir. La suite est à toi.**

BeforeYouGo est une application Android qui transforme la préparation du départ en un moment simple et agréable. Choisis les objets qui t’accompagnent, touche-les ou glisse-les dans ton sac, puis pars à ton rythme.

Un carnet personnel pour les journées de travail, les cours, le sport ou les escapades. Les notifications proposent un rendez-vous avec ton sac ; tu gardes le choix de l’ouvrir, de le reporter ou de passer ton tour.

**Disponibilité :** application en développement, destinée à une publication sur **Google Play**. Ce dépôt contient le code source et la documentation de préparation au lancement. Il ne constitue pas une version déjà publiée sur le Store.

## L’expérience

### Un sac à composer

L’écran **Mon départ** rassemble les objets prévus pour la journée ou pour un départ particulier. Un toucher confirme un objet ; un second toucher annule ce choix. Un appui long permet aussi de le glisser dans le sac illustré. Pour une longue liste, maintenir l’objet près du bord supérieur fait défiler l’écran vers le sac.

Le bouton **Je file !** reste disponible même quand tout n’est pas coché. Il ne valide aucun objet à ta place. Il n’y a ni score, ni série quotidienne à maintenir, ni pénalité.

### Une collection personnelle

Dans **Mes objets**, crée tes propres objets ou pioche parmi **36 idées illustrées** : clés, gourde, lunettes, carnet, tenue de sport, documents… La recherche et les catégories facilitent le choix.

Un objet peut revenir chaque jour ou apparaître à partir d’une date. Les objets ponctuels non confirmés restent disponibles ; ceux confirmés sont archivés au changement de journée. Tu peux revenir sur une confirmation pendant la journée.

### Des invitations à ton rythme

Chaque départ possède un nom, une heure, des jours de la semaine et son propre choix d’objets.

| Option | Comportement |
| --- | --- |
| **Un mot discret** | Notification silencieuse, proposée par défaut pour les nouveaux départs. |
| **Une sonnerie** | Son Android et vibration facultative, avec arrêt et report. |
| **Dans quelques minutes** | Report de 5, 10 ou 15 minutes, choisi dans les réglages du départ. |
| **Pas aujourd’hui** | Ferme l’invitation affichée, sans cocher les objets ni désactiver les prochains rendez-vous. |
| **Passer le prochain rendez-vous** | Ignore une seule occurrence et conserve la récurrence. |

Exemple de notification : **« On fait le tour ? »** — *« Mon départ · Un petit tour et tu files. »*

Le mode discret ne déclenche ni lecture audio ni vibration par l’application. Une sonnerie s’arrête automatiquement après deux minutes ; un essai dure dix secondes. Les anciens départs conservent leur mode de notification lors d’une mise à jour.

## Une application chaleureuse, à ta façon

La direction **carnet vivant** associe papier crème, terre cuite, sauge et titres typographiques inspirés d’un carnet. Les objets réagissent aux gestes ; les transitions restent discrètes. L’apparence peut suivre le téléphone ou utiliser le thème clair ou sombre.

Le ton est complice et direct. BeforeYouGo accompagne un départ sans réduire la personne à ses oublis. L’application ne contient pas d’IA générative et ne prétend pas détecter les objets physiques : un objet dans le sac représente la confirmation de l’utilisateur.

## Premiers pas

1. Ouvre **Mes objets** et choisis **Piocher des idées**, ou utilise **+** pour ajouter un objet personnel.
2. Reviens dans **Mon départ** pour composer ton sac avec un toucher ou un glisser.
3. Pour recevoir une invitation, choisis **Imaginer un départ** sous le sac, puis configure son horaire, ses jours et ses objets.
4. Dans **À ma façon**, règle l’apparence et, si tu souhaites recevoir les invitations programmées, les autorisations Android.
5. Quand tu veux partir, touche **Je file !**.

Le sac reste utilisable sans autorisation de notification. Les validations d’un même objet sont partagées entre les sacs de la journée.

## Confidentialité

La version présente dans ce dépôt fonctionne **hors ligne, sans compte, sans publicité et sans outil de suivi**. Elle n’utilise aucun serveur et ne demande pas la permission d’accès à Internet.

Les objets, départs, préférences et confirmations sont conservés sur le téléphone. Les préférences sont exclues de la sauvegarde cloud Android et du transfert de sauvegarde. La désinstallation ou l’effacement des données de l’application supprime ces informations.

| Autorisation Android | Utilisation |
| --- | --- |
| Notifications | Afficher les invitations et les commandes de sonnerie. |
| Alarmes exactes | Programmer les départs aux horaires choisis, sous réserve des autorisations et restrictions Android. |
| Redémarrage | Reprogrammer les départs après un redémarrage du téléphone. |
| Vibration | Faire vibrer le téléphone lorsque cette option est activée en mode sonnerie. |
| Service de premier plan et lecture audio | Maintenir la sonnerie et ses commandes pendant sa courte durée de lecture. |
| Verrou de réveil | Permettre la lecture de la sonnerie lorsque l’écran est éteint. |

La politique publique de confidentialité et son accès depuis l’application font partie de la préparation à la publication, décrite dans le [guide Google Play](docs/PLAY_STORE_RELEASE.md).

## Projet Android

| Élément | Configuration actuelle |
| --- | --- |
| Interface | Kotlin, Jetpack Compose, Material 3 |
| Compatibilité | Android 8.0 et versions ultérieures — API 26 minimum |
| SDK de compilation et cible | API 37 |
| Identifiant de l’application | `com.toaandri.beforeyougo` |
| Stockage | JSON dans `SharedPreferences`, sur l’appareil |
| Planification | `AlarmManager.setAlarmClock()`, avec contrôle de l’accès aux alarmes exactes |
| Restauration | Redémarrage, mise à jour, changement d’heure ou de fuseau et changement d’accès aux alarmes exactes |
| Langue de l’interface | Français |

### Construire et installer une version de développement

Prérequis : Android Studio compatible avec le projet, Android SDK 37, et le **JDK 25** configuré pour le daemon Gradle dans `gradle/gradle-daemon-jvm.properties`. Le wrapper utilise Gradle 9.6.0 ; la cible de compilation Java 11 du module ne correspond pas au JDK nécessaire pour exécuter le build. La première synchronisation peut télécharger les dépendances et le JDK configuré.

Configure le chemin de ton SDK avec Android Studio ou dans `local.properties`, qui reste propre à chaque machine.

Sous Windows :

```powershell
.\gradlew.bat assembleDebug
```

Sous macOS ou Linux, utilise `./gradlew assembleDebug`.

L’APK de développement est généré dans `app/build/outputs/apk/debug/app-debug.apk`. Avec un appareil de test connecté et le débogage USB activé :

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.toaandri.beforeyougo/.MainActivity
```

Cet APK sert aux essais locaux. La distribution Google Play se prépare avec un **Android App Bundle signé** ; voir le [guide de publication](docs/PLAY_STORE_RELEASE.md).

### Vérifier le projet

```powershell
.\gradlew.bat testDebugUnitTest lintDebug
.\gradlew.bat assembleDebug assembleDebugAndroidTest
```

Les tests unitaires couvrent notamment la sélection des objets, les récurrences, les reports d’occurrence, les changements d’heure et le mode silencieux par défaut. Les tests instrumentés couvrent aussi les migrations, la sonnerie, la fermeture d’une invitation sans modification des données, les confirmations annulables et le dépôt d’un objet dans le sac.

**Vérification de la refonte du 8 octobre 2026 :** compilation de l’application et des tests instrumentés réussie, 10 tests unitaires réussis, analyse Lint sans erreur bloquante. Les tests instrumentés de cette refonte n’ont pas été exécutés sur appareil ; compiler leur APK ne les exécute pas.

Exécute les tests instrumentés sur un émulateur ou un appareil de test dédié. Certains runners peuvent réinstaller l’application et effacer ses données ; évite un appareil personnel contenant des données à conserver.

### Organisation du code

```text
app/src/main/java/com/toaandri/beforeyougo/
├── MainActivity.kt          Navigation et composition des écrans
├── DepartureRitual.kt       Sac, gestes et confirmations
├── ObjectsScreen.kt        Collection, catalogue et édition des objets
├── ObjectCatalog.kt        Suggestions illustrées
├── AlarmEditor.kt          Personnalisation des départs
├── DepartureAlarm.kt       Modèle des départs et règles de récurrence
├── ChecklistRules.kt        Sélection des objets applicables
├── AppStore.kt             Persistance locale et migrations
├── ReminderReceiver.kt     Programmation et notifications discrètes
├── AlarmRingingService.kt  Sonnerie bornée et commandes
└── ui/theme/               Couleurs, typographie et thèmes
```

## Publication sur Google Play

Le [guide de préparation Google Play](docs/PLAY_STORE_RELEASE.md) rassemble la procédure de signature, les essais avant lancement, les éléments de la fiche Store, un projet de description et les déclarations à préparer dans Play Console.

La publication publique interviendra après validation sur appareils, préparation des éléments de confidentialité, signature de la version de distribution et examen par Google Play. Aucune date de sortie ni approbation du Store n’est annoncée ici.

## Périmètre actuel

La synchronisation entre appareils, les listes partagées, les widgets, l’export et la détection automatique d’objets ne sont pas disponibles. Les anciennes captures conservées dans `docs/media` précèdent la refonte et ne représentent pas son interface actuelle.

Pour signaler un problème reproductible, ouvre une [issue GitHub](https://github.com/toaandri/BeforeYouGo/issues) en indiquant la version Android, le modèle de téléphone et les étapes utiles, sans joindre de données personnelles.

## Licence

Aucune licence de réutilisation du code n’a été choisie à ce stade. Le caractère public du dépôt ne constitue pas une autorisation de réutilisation. Les bibliothèques et icônes intégrées conservent leurs licences respectives.
