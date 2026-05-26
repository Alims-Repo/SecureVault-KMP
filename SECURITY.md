# Security Policy

## Supported versions

| Version | Supported |
|---------|-----------|
| 0.1.x   | ✅        |

## Reporting a vulnerability

Please do **not** open a public GitHub issue for security reports.

Instead, email **sourav.0.alim@gmail.com** with:

- A description of the issue and its impact.
- Reproduction steps or a proof-of-concept.
- The affected version(s).

You should receive an acknowledgement within **72 hours**. A fix or
mitigation will be coordinated privately, followed by a patch release and
a public disclosure crediting the reporter (unless anonymity is requested).

## Threat model & non-goals

SecureVault wraps the platform's native secure storage:

- **Android** — `EncryptedSharedPreferences` backed by the Android Keystore.
- **iOS** — Keychain Services (`kSecClassGenericPassword`, `*ThisDeviceOnly`).

It therefore inherits the OS guarantees and limitations. In particular,
SecureVault does **not** protect against:

- A rooted/jailbroken device, or any attacker with code execution inside
  the host process.
- Backups: stored values are excluded from device backups by the underlying
  schemes, but users can still snapshot their device with developer tools.
- Memory inspection while a value is in flight (`String` is decrypted in
  user space).

If your threat model requires hardware-backed user-presence enforcement,
set `Accessibility.WhenUnlocked` and (on Android, in a future release)
opt into biometric prompts.

