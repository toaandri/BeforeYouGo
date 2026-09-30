# BeforeYouGo

**Remember what matters before you leave.**

BeforeYouGo is an offline-first native Android app that helps people remember everyday essentials and one-off items. Capture a thought in seconds, receive a configurable morning reminder, and check your list before heading out.

> Project status: MVP implemented. The interface intentionally stays simple while the core daily-list workflow is usable offline.

## The idea

- **Everyday essentials:** Choose items such as keys, wallet, glasses, or anything personal. No mandatory categories.
- **Don't forget tomorrow:** Quickly add an item for tomorrow or another date.
- **Today's checklist:** See recurring essentials and date-specific reminders together; confirm items individually.
- **Morning reminder:** Set your preferred time and active days; open today's checklist from the notification.
- **Offline by default:** No account or server required for the core experience.

The app records user confirmations; it cannot automatically verify that an object is physically present.

## What works today

1. Add, edit, and delete everyday essentials and dated one-off reminders.
2. See a daily checklist that combines essentials with due and overdue one-off items.
3. Keep daily check states and items on the device across app restarts.
4. Complete a one-off item directly from today's list; it is then removed.
5. Configure or disable a daily Android notification reminder (Android 13+ asks for notification permission).
6. Work entirely offline with a small, accessible Compose UI.

**Not in the MVP:** AI, accounts, cloud sync, geofencing, mandatory trip/work/exam templates, ads, or subscriptions.

## Tech stack

- Kotlin, Android Studio, Gradle Kotlin DSL
- Jetpack Compose and Material 3
- SharedPreferences with JSON for the intentionally small, local MVP data set
- Android AlarmManager and notification channels for the daily reminder
- JUnit and Android/Compose instrumentation tests

Implementation details and dependencies will be added as development progresses. Notification delivery timing depends on Android permissions, battery restrictions, and scheduling APIs.

## Getting started

1. Clone this repository.
2. Open its root folder in a recent stable Android Studio version.
3. Let Gradle sync; use the project's Gradle wrapper.
4. Run the `app` configuration on an emulator or Android device.

On Windows, a command-line debug build can be run from the repository root:

```powershell
.\gradlew.bat assembleDebug
```

Do not commit `local.properties`, signing keys, passwords, or API secrets.

## Proposed architecture

```text
app/src/main/java/com/toaandri/beforeyougo/
  core/          # database, notifications, preferences, shared UI
  data/          # entities, DAOs, repository implementations
  domain/        # models, recurrence and checklist rules, use cases
  feature/       # today, essentials, quick-add, settings, onboarding
  MainActivity.kt
```

Start with one Gradle module and split modules only if complexity justifies it. Keep business logic independent of Compose for straightforward unit testing.

## Roadmap

- [x] Initialize Android Studio project and Git repository
- [x] Establish README and project conventions
- [x] Build Today screen and essentials management
- [x] Implement local persistence and daily checklist rules
- [x] Add one-off reminders and quick capture
- [x] Add configurable daily notifications
- [x] Add unit coverage for checklist recurrence rules
- [ ] Add optional widget, backup/export, and beta testing
- [ ] Prepare Play Store listing, privacy disclosures, signing, and release

## Privacy and accessibility

The intended first release keeps personal lists on-device and requires no account. Do not add analytics or third-party SDKs without documenting their data practices. Support TalkBack, scalable text, adequate touch targets, and clear notification controls. Review the current Google Play requirements and Android target API requirements when preparing the release; these can change.

## Contributing

This project is in its initial development phase. Issues and pull requests can be discussed once contribution guidelines are published.

## License

No open-source license has been selected yet. Until one is added, the repository's contents are not automatically licensed for reuse.
