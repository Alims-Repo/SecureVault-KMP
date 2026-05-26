# SecureVault KMP

[![Maven Central](https://img.shields.io/maven-central/v/io.github.alims-repo/secure-vault?style=flat-square)](https://central.sonatype.com/artifact/io.github.alims-repo/secure-vault)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue?style=flat-square)](LICENSE)
[![Kotlin](https://img.shields.io/badge/kotlin-2.3-blueviolet?style=flat-square&logo=kotlin)](https://kotlinlang.org)
[![Targets](https://img.shields.io/badge/targets-Android%20%7C%20iOS-success?style=flat-square)](#supported-targets)

A small, coroutine-first Kotlin Multiplatform library for storing secrets on
**Android** (`EncryptedSharedPreferences` over the Android Keystore) and
**iOS** (Keychain Services). One API, two native backends, no hand-rolled
cryptography.

```kotlin
val vault = SecureVault("com.acme.auth")

vault.put("session", "eyJhbGciOiJIUzI1NiJ9…")
val token: String? = vault.get("session")
```

---

## Install

`secure-vault` is published to Maven Central. The Compose Multiplatform
integration ships separately as `secure-vault-compose`.

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.alims-repo:secure-vault:0.2.0")
    // Optional — only if you use Compose Multiplatform:
    implementation("io.github.alims-repo:secure-vault-compose:0.1.0")
}
```

KMP source set wiring:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.alims-repo:secure-vault:0.2.0")
            implementation("io.github.alims-repo:secure-vault-compose:0.1.0")
        }
    }
}
```

## Supported targets

| Target               | Backend                           |
|----------------------|-----------------------------------|
| `android`            | `EncryptedSharedPreferences`      |
| `iosArm64`           | Keychain Services                 |
| `iosSimulatorArm64`  | Keychain Services                 |

Both artifacts (`secure-vault` and `secure-vault-compose`) ship the same
target matrix. Common-side consumers depend on the artefact name; the matching
platform variant is selected by Gradle metadata automatically.

## Usage

### Common code

```kotlin
class AuthRepository(private val vault: SecureVault) {

    suspend fun saveSession(token: String) = vault.put("session", token)

    suspend fun session(): String? = vault.get("session")

    suspend fun logout() = vault.remove("session")
}
```

### Android entry point

No `Context` plumbing — the library captures the application Context via
[`androidx.startup`](https://developer.android.com/topic/libraries/app-startup)
before `Application.onCreate()` returns.

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Anywhere from here on:
        val vault = SecureVault("com.acme.auth")
    }
}
```

If you have explicitly disabled `androidx.startup` for SecureVault's
initializer, supply the context manually:

```kotlin
SecureVault.initialize(applicationContext)
```

### iOS entry point (Swift)

```swift
let vault = SecureVaultsKt.SecureVault(
    namespace: "com.acme.auth",
    accessibility: .afterFirstUnlock
)
```

## Error handling

Every storage method throws a single sealed type — `VaultException`:

```kotlin
try {
    vault.put("token", value)
} catch (e: VaultException.InvalidKey)         { /* programmer error */ }
  catch (e: VaultException.Tampered)           { /* nuke the namespace */ }
  catch (e: VaultException.CryptoFailure)      { /* report */ }
  catch (e: VaultException.StorageUnavailable) { /* retry / surface */ }
```

## Using with Compose Multiplatform

Add the optional `secure-vault-compose` dependency for a small, opinionated
Compose layer: a lifecycle-aware `rememberSecureVault(...)` factory, a
`VaultState` sealed type so you render `Initializing`/`Ready`/`Failed`
explicitly, and a `LocalSecureVault` `CompositionLocal`.

```kotlin
@Composable
fun AppRoot() {
    val state by rememberSecureVault("com.acme.auth")
    when (val s = state) {
        VaultState.Initializing -> SplashScreen()
        is VaultState.Failed    -> ErrorScreen(s.reason)
        is VaultState.Ready     -> ProvideSecureVault(s.vault) { HomeScreen() }
    }
}

@Composable
fun HomeScreen() {
    val vault = LocalSecureVault.current   // available everywhere below ProvideSecureVault
    // ... vault.put(...), vault.get(...)
}
```

The underlying host is process-wide and keyed by `VaultConfig.namespace`, so
calling `rememberSecureVault` from multiple composables with the same namespace
returns the same vault and the master-key pre-warm runs **exactly once**.

## Threading

All suspending methods dispatch onto an I/O-appropriate dispatcher
(`Dispatchers.IO` on Android, `Dispatchers.Default` on iOS); callers do not
need to switch context. Instances are safe to share across coroutines.

## Security model

See [SECURITY.md](SECURITY.md) for guarantees, non-goals, and the
responsible-disclosure policy.

## Building locally

```bash
./gradlew :secure-vault:build
./gradlew :secure-vault:apiDump        # after intentional API changes
./gradlew :secure-vault:publishToMavenLocal
```

Publication requires `~/.gradle/gradle.properties` to declare
`mavenCentralUsername`, `mavenCentralPassword`, and the in-memory
`signingInMemoryKey` / `signingInMemoryKeyPassword` properties consumed by
the [vanniktech maven-publish](https://vanniktech.github.io/gradle-maven-publish-plugin/)
plugin.

## License

```
Copyright 2026 Alim Sourav

Licensed under the Apache License, Version 2.0.
You may obtain a copy of the License at
    https://www.apache.org/licenses/LICENSE-2.0
```
