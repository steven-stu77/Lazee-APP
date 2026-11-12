# Lazee (懒记) — rules for Claude Code sessions

Full spec: `docs/SPEC.md`. UI reference: `docs/prototype.html`. Decisions log: `docs/DECISIONS.md`.

## Development guide (spec §0)

1. **The prototype is the design reference** for layout, copy, interaction and colour. If the prototype and the spec disagree, the spec wins. Screens marked "原型外" reuse the same card / list / switch / segmented-control style.
2. Read the whole spec first, then follow the milestone order in §13.
3. **Use libraries** from §2 instead of rewriting them. Before adding a new one, make sure it needs no network, and log the reason in `docs/DECISIONS.md`.
4. **Readable code**: code, comments and commit messages in English; files under ~300 lines; plain names; a short comment for the "why" of anything tricky. Every user-visible string lives in `strings.xml` (English in `values/`, Chinese in `values-zh/`) — never hard-coded.
5. **Never add `INTERNET`** (or `ACCESS_NETWORK_STATE`), and no analytics, ads or crash-reporting SDKs. The app is fully offline; CI fails if the merged manifest contains a network permission.
6. **Verification**: this environment may have no Android SDK. GitHub Actions is the source of truth — push each milestone and fix until green. With an SDK, run `./gradlew testDebugUnitTest assembleDebug` first.
7. When unsure, pick the simplest safe option, keep going, and add one line to `docs/DECISIONS.md`.

## Architecture rules (spec §3)

- Single `app` module, organised by feature: `core/` (data, repo, prefs, security, money, calc, ui) and `feature/<name>/`, plus `work/` for WorkManager workers.
- **Two layers per screen**: `XxxScreen.kt` (UI only: takes state, emits events) and `XxxViewModel.kt` (logic, exposes `StateFlow<XxxUiState>`).
- **One data direction**: Room DAO `Flow` → Repository → ViewModel builds UiState → Screen. Screens never touch DAOs.
- **Money is `Long` cents** (RM 6.50 = 650). Never Float/Double for stored money; format only for display (`Money.format`).
- **Pure logic stays Android-free** (parser, classifier, dedupe/matching, budget and stats maths, calculator, backup crypto, CSV) so it is unit-testable.
- **Config in JSON, not code**: `app/src/main/assets/merchants.json`, `keywords.json`, `parsers/*.json`.
- Text in `strings.xml`; colours and sizes in `core/ui/theme`.
- **Database starts at version 1.** Any schema change needs a real `Migration`; `fallbackToDestructiveMigration` is forbidden (it wipes user data).

## Privacy hard rules (spec §6, §8)

- Raw notification text lives only in memory: never written to the DB, files or Logcat.
- Only store amount, merchant, time, source app and card last-4.
- OTP / verification-code notifications are dropped before parsing.
