# Decisions

One line per judgement call made during development.

- Package / applicationId is `io.github.steven_stu77.lazee`: the owner name `steven-stu77` contains a hyphen, which is not allowed in a Java package, so it becomes an underscore.
- Toolchain pinned to the set used by Google's Now in Android sample (AGP 9.3.2, Kotlin 2.3.0, KSP 2.3.4, Hilt 2.59, Room 2.8.3, Gradle 9.7.1), because Google Maven was not reachable from the dev container to check newer versions. With AGP 9 Kotlin is built in, so the `kotlin-android` plugin is not applied.
- compileSdk 37, targetSdk 36 (Google Play's requirement for new apps and updates from Aug 2026).
- Noto Sans SC is **not** bundled: even three static weights add ~25 MB, over the APK size target. Android's system CJK font is already Noto Sans CJK, so Chinese text looks the same. Only Inter (variable font, ~0.9 MB) is bundled.
- Charts (spending pace line, 6-month bars, category detail bars) are drawn with Compose `Canvas` instead of Vico: they need dashed lines and a budget line in the prototype's exact style, and Vico 3's API could not be compile-checked offline. No extra dependency.
- `material-icons-extended` is not used (it bloats debug APKs); the few line icons are copied from the prototype's SVG paths into `core/ui/components/LazeeIcons.kt`.
- The `Transaction` entity is stored in a table named `Txn` so it never clashes with Room's `@Transaction` annotation or the SQL keyword.
- `CategoryBudget.limit` is named `amountLimit` because `LIMIT` is an SQL keyword.
- The CI signing secrets cannot be set from the dev container (no access to the secrets API). The workflow falls back to the default debug key and says so in the Release notes; README explains how to add the secrets once.
- Category reordering uses "Move earlier / Move later" in the long-press menu instead of drag-and-drop: dragging inside a wrapping grid is fiddly on small phones and easy to trigger by accident.
- Screenshot tests (`app/src/test/.../android/ScreenshotTest.kt`) run the real app under Robolectric with an in-memory Room database (SQLCipher's native library cannot load on the JVM) and write PNGs to `app/build/screenshots`. They double as an end-to-end smoke test of Hilt + navigation. On sandboxed machines that cannot download Robolectric's Android jars, set `ROBOLECTRIC_DEPS_DIR` to a folder holding them.
- Loan principal is recorded as a TRANSFER with no target account (positive = money leaves the account, negative = money comes in), so it never counts as income or spending; interest is a normal INCOME/EXPENSE.
- Parser rules support one extra direction, `PERSON_TRANSFER` (DuitNow / transfer to a person's name): it is an expense whose category is never guessed (spec §6 step 8), so the parser has to tell it apart from a shop payment.
- `OWN_TRANSFER` rules carry an `incoming` flag in their JSON (a TNG reload = money in, a MAE "transfer to your TNG" = money out), so both halves can be paired into one TRANSFER. A lone half becomes a pending transfer whose other side defaults to another account.
- The bundled TNG eWallet and MAE rules are written from made-up samples and marked `"preview": true`. The app list shows them as "即将支持 / Coming soon" but still lets the user switch them on to try, instead of hiding them completely; replace them with rules from real masked samples.
- The auto-log pipeline is tested end to end in `AutoLogFlowTest` (Robolectric + Hilt, in-memory DB): dedupe, OTP drop, 4th familiar visit auto-logged and undone, MAE→TNG transfer merge, bill match, possible duplicate.
- `androidx.documentfile` is used to list, create and delete backup files inside the folder picked with the Storage Access Framework (keep-newest-10 rule). It is an offline AndroidX library.
- App lock uses `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` on Android 11+, and `BIOMETRIC_WEAK or DEVICE_CREDENTIAL` on Android 8–10, where AndroidX does not allow the strong + credential combination. Turning the lock on requires one successful unlock first, so nobody locks themselves out.
- Restore first writes an encrypted copy of the current data to the app's private files (`before-restore.lgb`, same password) and, when a backup folder is set, also a normal backup there.
- The auto-log page shows a privacy-safe status: whether the listener is connected and the last supported-app alert's time and outcome (never its text). Only apps that have a parser rule are looked at at all. The listener asks to be re-bound when the system disconnects it (common on Xiaomi HyperOS), and a Reconnect button toggles the component to force a re-bind. TNG eWallet is no longer marked preview: a real DuitNow payment sample is now in the tests.
