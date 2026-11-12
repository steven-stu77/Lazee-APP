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
