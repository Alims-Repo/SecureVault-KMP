# Changelog

All notable changes to **SecureVault KMP** are documented here.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-05-26
### Added
- Initial public API:
  - `SecureVault` interface — `put` / `get` / `remove` / `contains` /
    `clear` / `keys`, all `suspend`.
  - `SecureVaultFactory` `expect class` per platform.
  - `VaultConfig` + `Accessibility` enum.
  - `VaultException` sealed hierarchy
    (`InvalidKey`, `CryptoFailure`, `Tampered`, `StorageUnavailable`).
- Android backend powered by
  [`androidx.security:security-crypto`](https://developer.android.com/jetpack/androidx/releases/security)
  (AES-256 SIV keys + AES-256 GCM values via Android Keystore).
- iOS backend powered by Keychain Services with `kSecClassGenericPassword`
  and per-namespace `kSecAttrService` isolation.
- `commonTest` behavioural contract (`SecureVaultContractTest`) + in-memory
  `FakeSecureVault`.
- Maven Central publishing pipeline (vanniktech 0.30, Dokka HTML javadoc jar,
  signed) and binary-compatibility-validator-enforced public ABI.

### Known limitations
- Android Robolectric tests are deferred until AGP's
  `com.android.kotlin.multiplatform.library` plugin exposes a stable
  host-test surface.
- `Accessibility.WhenUnlocked` on Android currently maps to the same
  EncryptedSharedPreferences scheme as `AfterFirstUnlock`; full user-presence
  enforcement (biometric prompt) is tracked for v0.2.
- No JVM/Desktop target yet; planned for v0.2.

[Unreleased]: https://github.com/Alims-Repo/SecureVault-KMP/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/Alims-Repo/SecureVault-KMP/releases/tag/v0.1.0

