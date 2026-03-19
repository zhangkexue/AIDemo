# Tech Stack

## Languages & Runtime
- Kotlin 2.0.0
- JVM target: Java 11

## Android
- AGP 8.4.2
- compileSdk / targetSdk: 35
- minSdk: 21

## Key Libraries
- AndroidX Core KTX 1.13.1
- AppCompat 1.7.0
- Material 1.12.0
- ConstraintLayout 2.1.4

## Testing
- JUnit 4.13.2 (unit tests)
- Kotest 5.9.1 — `kotest-runner-junit5` + `kotest-property` for property-based testing
- Espresso 3.6.1 (instrumented tests)
- Tests use JUnit Platform: `useJUnitPlatform()`

## Static Analysis
- **Detekt** 1.23.6 — config at `detekt.yml`, zero-issue tolerance (`maxIssues: 0`)
- **ktlint** — registered as Gradle tasks (`ktlintCheck`, `ktlintFormat`)

## Build System
- Gradle with Kotlin DSL (`.kts`)
- Version catalog: `gradle/libs.versions.toml`

## Common Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run detekt static analysis
./gradlew detekt

# Run ktlint check
./gradlew ktlintCheck

# Auto-format with ktlint
./gradlew ktlintFormat

# Full check (lint + tests)
./gradlew check
```
