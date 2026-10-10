# Development notes

- Run targeted checks with `./gradlew test lint assembleDebug` when the Android toolchain is available.
- Do not commit generated APKs, signing configuration, keystores, device/session data, or build output.
- Preserve the existing package name and local authentication flow unless a requested change requires otherwise.
