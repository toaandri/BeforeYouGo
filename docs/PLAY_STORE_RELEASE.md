# Préparation de la publication Google Play

## État technique — prêt dans le dépôt

- `targetSdk` et `compileSdk` : API 37.
- Compatibilité : Android 8.0+ (API 26).
- Notifications demandées lors de l’activation d’une alarme ; accès spécial `SCHEDULE_EXACT_ALARM` à autoriser par l’utilisateur dans Android. Aucune utilisation de `USE_EXACT_ALARM` ni de fenêtre plein écran imposée.
- Sonnerie bornée (2 minutes), service de premier plan `mediaPlayback`, vibration et verrou de réveil pendant la lecture. Le volume Alarme et les restrictions du téléphone s’appliquent.
- Répétitions par jours ISO, report et restauration après redémarrage, mise à jour, changement d’heure ou de fuseau.
- Aucune collecte, compte, publicité, SDK d’analyse, réseau ou partage de données.
- Données confinées au stockage local ; la sauvegarde Android est désactivée afin de ne pas les envoyer vers un compte cloud.
- Tests unitaires de la règle métier de sélection de la checklist.

## À faire depuis le compte Play Console

Ces éléments ne peuvent pas être validés uniquement dans le code :

1. Créer une clé de signature de production et configurer la signature release dans le coffre-fort CI/Android Studio. Ne jamais committer cette clé.
2. Générer un Android App Bundle signé : `./gradlew bundleRelease`.
3. Créer la fiche Store : nom, description courte, description complète, icône 512 × 512, capture(s) d’écran téléphone et bannière 1024 × 500.
4. Héberger la politique de confidentialité ci-dessous à une URL publique et la renseigner dans Play Console.
5. Déclarer « aucune donnée collectée ou partagée » dans Data safety, puis confirmer l’utilisation des notifications pour le rappel configuré par l’utilisateur.
6. Remplir le questionnaire de classification du contenu ; l’application est normalement « Tous publics ».
7. Exécuter le test fermé Play sur plusieurs appareils, en particulier Android 13+ (permission notification) et Android 8–12 (rappel quotidien).
8. Définir un e-mail de contact, le pays de distribution et le prix (gratuit conseillé).
9. Déclarer l’usage du service de premier plan `mediaPlayback` dans Play Console et fournir la démonstration requise pour la sonnerie. Refaire la revue des autorisations et des règles Play en vigueur avant soumission.

## Texte de politique de confidentialité

**BeforeYouGo — Politique de confidentialité**

Dernière mise à jour : 2 octobre 2026.

BeforeYouGo fonctionne entièrement hors ligne. L’application ne crée pas de compte et ne collecte, ne transmet ni ne vend aucune donnée personnelle. Les objets de votre checklist et le réglage de rappel sont enregistrés uniquement dans le stockage local de votre appareil.

Si vous activez une routine, l’application demande les notifications et l’accès spécial aux alarmes exactes. Ces autorisations servent à déclencher la sonnerie ou la notification discrète choisie les jours et à l’heure indiqués. Le mode Notification n’active aucun son ni vibration par l’application. Le mode Sonnerie utilise un service temporaire qui lit le son et peut faire vibrer le téléphone pendant au maximum deux minutes. Les routines sont reprogrammées au redémarrage ; vous pouvez les désactiver à tout moment. Le choix de la sonnerie utilise le sélecteur Android et aucune donnée n’est transmise.

L’application n’utilise ni publicité, ni analytics, ni cookie, ni service tiers. Pour toute question, contactez l’éditeur à l’adresse e-mail affichée dans la fiche Google Play.

## Crédit de direction artistique

La palette encre/jade/sable est conçue à partir de la méthode de composition de [Coolors](https://coolors.co/). Les icônes sont les vecteurs Material inclus dans Android : ils restent nets à toutes les densités et évitent l’obligation d’attribution des ressources Icons8 gratuites. Les photos Pexels ne sont volontairement pas embarquées : aucune image décorative ne doit alourdir une checklist utilisable hors ligne.
