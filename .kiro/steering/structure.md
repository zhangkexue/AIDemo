# Project Structure

```
/
├── build.gradle.kts          # Root build script (plugin declarations)
├── settings.gradle.kts       # Project settings
├── gradle.properties         # Gradle/JVM properties
├── detekt.yml                # Detekt static analysis config
├── gradle/
│   └── libs.versions.toml    # Centralized version catalog
└── app/                      # Single application module
    ├── build.gradle.kts      # App-level build config
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/zkx/aidemo/   # Kotlin source files
        │   └── res/
        │       ├── layout/            # XML layouts
        │       └── values/            # strings.xml, themes.xml
        └── test/
            └── java/com/zkx/aidemo/  # Unit & property-based tests
```

## Conventions
- Single-module project — all app code lives under `app/`
- Package name: `com.zkx.aidemo`
- All dependencies declared in `gradle/libs.versions.toml` and referenced via `libs.*` aliases
- Property-based tests use Kotest `StringSpec` style and live alongside unit tests in `app/src/test/`
- Layouts use `ConstraintLayout` as the root view
- String resources go in `res/values/strings.xml` — no hardcoded strings in layouts or code
