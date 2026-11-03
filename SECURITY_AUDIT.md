# Security Audit

Last reviewed: 2026-11-04

## Scope

- Android source, merged manifest, and signed `v1.1.0` release APK.
- Gradle wrapper, dependencies, GitHub Actions, and GitHub repository protections.

## Verified Properties

- Release APK has no declared Android permissions, Services, Receivers, or Providers.
- `IntentTestActivity` is the only exported component. It accepts only two allowlisted Vivo actions.
- Incoming extras, data URIs, ClipData, and flags are not forwarded. Google Wallet target is explicit.
- Release APK is signed with APK Signature Scheme v2 and its published SHA-256 checksum matches.
- The app has no network permission, tracker, analytics SDK, background service, AccessibilityService, root, or Shizuku integration.

## Accepted Android Risks

- The exported Activity is required for Android Resolver. Any app can send an allowlisted action and bring Google Wallet forward. A 750 ms persisted monotonic debounce limits repeated launches.
- Google Wallet package identity is not certificate-pinned. Do not add a digest until it is verified from a trusted Google source and tested on supported devices.
- `NEW_TASK`, `CLEAR_TOP`, and `SINGLE_TOP` remain because they are device-validated Recents behavior. Test Wallet enrollment/auth flows before changing them.

## Release Requirements Before Public Promotion

- Protect `main` and tag pattern `v*` with GitHub rulesets.
- Use a protected `release` environment with required approval when account plan supports it.
- Restrict tag creation to trusted maintainers.
- Require signed tags only after publishing and managing trusted public keys. `git verify-tag` alone is not publisher identity policy.
- Pin GitHub Actions by full commit SHA and keep Gradle Wrapper checksum verification enabled.
- Add Gradle dependency verification metadata when dependencies change.
- Publish release APK signer fingerprint and build provenance.

## Residual Risks

No software release is absolutely secure. Vendor shortcut behavior, Google Wallet lock-screen policy, OEM task behavior, and GitHub account security remain external trust boundaries.
