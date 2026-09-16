# BeforeYouGo

**Remember what matters before you leave.**

BeforeYouGo is an offline-first native Android app that helps people remember everyday essentials and one-off items. Capture a thought in seconds, receive a configurable morning reminder, and check your list before heading out.

> Project status: early development. Features below describe the product roadmap, not functionality already shipped.

## The idea

- **Everyday essentials:** Choose items such as keys, wallet, glasses, or anything personal. No mandatory categories.
- **Don't forget tomorrow:** Quickly add an item for tomorrow or another date.
- **Today's checklist:** See recurring essentials and date-specific reminders together; confirm items individually.
- **Morning reminder:** Set your preferred time and active days; open today's checklist from the notification.
- **Offline by default:** No account or server required for the core experience.

The app records user confirmations; it cannot automatically verify that an object is physically present.

## MVP scope

1. First-run onboarding: select and create essentials; optionally set reminder time and request notification permission at the appropriate moment.
2. Add, edit, archive, and delete essentials and one-off items.
3. Generate today's checklist from recurrence rules and dated items without creating endless duplicate records.
4. Persist per-day check states and preserve pending one-off items until the user completes, postpones, or archives them.
5. Configure a morning reminder, handle permission denial, and provide a snooze action.
6. Work offline, survive app restarts, and provide accessible Compose UI.

**Not in the MVP:** AI, accounts, cloud sync, geofencing, mandatory trip/work/exam templates, ads, or subscriptions.

## Tech stack

- Kotlin, Android Studio, Gradle Kotlin DSL
- Jetpack Compose and Material 3
- Room for app data; DataStore for preferences
- Coroutines and Flow; ViewModel and repository pattern
- WorkManager for deferrable reminders; evaluate AlarmManager only if exact alarms become a justified product requirement
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
- [ ] Establish README, specification, and project conventions
- [ ] Build Today screen and essentials management
- [ ] Implement Room persistence and daily checklist rules
- [ ] Add one-off reminders and quick capture
- [ ] Add configurable notifications and snooze
- [ ] Test recurrence, date changes, restarts, and accessibility
- [ ] Add optional widget, backup/export, and beta testing
- [ ] Prepare Play Store listing, privacy disclosures, signing, and release

## Privacy and accessibility

The intended first release keeps personal lists on-device and requires no account. Do not add analytics or third-party SDKs without documenting their data practices. Support TalkBack, scalable text, adequate touch targets, and clear notification controls. Review the current Google Play requirements and Android target API requirements when preparing the release; these can change.

## Contributing

This project is in its initial development phase. Issues and pull requests can be discussed once contribution guidelines are published.

## License

No open-source license has been selected yet. Until one is added, the repository's contents are not automatically licensed for reuse.
