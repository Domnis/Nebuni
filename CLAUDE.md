# Nebuni — notes for agents

## What this repository is

Nebuni is a Compose Multiplatform app (Android, iOS, desktop) that fetches Unistellar's science ephemerides
and stores them locally so they can be used offline.

## Stack

- Kotlin Multiplatform + Compose Multiplatform, single module `:composeApp`
  - targets: `androidTarget`, `iosX64` / `iosArm64` / `iosSimulatorArm64`, `jvm("desktop")`
  - shared code in `composeApp/src/commonMain`, platform code in `androidMain`, `iosMain`, `desktopMain`
- Networking: Ktor (engine per platform: OkHttp, Darwin, CIO) with kotlinx.serialization
- Persistence: Room (KMP) with the bundled SQLite driver, KSP for the Room compiler
- DI: Koin (BOM), navigation: `org.jetbrains.androidx.navigation`, Material 3 adaptive
- Versions live in `gradle/libs.versions.toml` only: never hard-code a version in a build file.
- iOS host app: `iosApp/` (Xcode project embedding the `ComposeApp` framework).

## Commands

- Full verification (run by the orchestrator, do not run it yourself):
  `./gradlew :composeApp:desktopTest :composeApp:iosSimulatorArm64Test :composeApp:testDebugUnitTest :composeApp:lintDebug`
- Fast loop while working: `./gradlew :composeApp:desktopTest --tests '<TestClass>' -q`

## Conventions

- Shared tests go in `composeApp/src/commonTest` (kotlin.test).
- User-facing strings are Compose resources (`commonMain/composeResources`), never hard-coded in
  composables.
- Code comments and identifiers in English.

## Boundaries — never do this

- Never edit signing settings, provisioning, secrets or CI configuration.
- Never add a dependency unless the task asks for it.
- Never call production services in tests: use fakes (for HTTP, Ktor's `MockEngine`).
