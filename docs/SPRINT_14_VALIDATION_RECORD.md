# Sprint 14 — Real-World Validation Record

**Product:** NetPulse
**Date:** 2026-08-17
**Scope:** Build, static code-path, and real-world validation record
**Result:** Build and static audit complete; physical-device validation pending.

## Evidence integrity

This record deliberately separates verified code/build evidence from physical observations. No Wi-Fi measurements, access points, screenshots, speed-test results, or roaming transitions are claimed unless collected on an Android device.

## Build evidence

| Check | Result | Evidence |
|---|---|---|
| `:app:assembleDebug --no-daemon --offline` | Passed | `BUILD SUCCESSFUL in 15s`; 39 tasks up to date |
| Debug APK | Produced | `app/build/outputs/apk/debug/app-debug.apk` |
| `git diff --check` | Passed | No whitespace errors reported |

Build environment emitted one non-blocking SDK XML compatibility warning. It did not prevent APK assembly.

## Device and environment

| Item | Status |
|---|---|
| ADB device | Not available in this validation session |
| Android device model / Android version | Not recorded |
| Wi-Fi environment | Not observed |
| AP count / SSIDs / BSSIDs | Not observed |
| Screenshot or video evidence | Not collected |

## Static implementation audit

| Technical chain | Code-path finding | Status |
|---|---|---|
| Real Wi-Fi data | `WifiManagerHelper.scanNearbyNetworks()` maps `WifiManager.scanResults`; connected data is obtained by `getConnectedWifiInfo()` | Present |
| Scanner grouping | UI groups observed scan results by SSID while retaining individual BSSID records inside each group | Present |
| Connected state | Repository emits the connected network or `emptyList()` when none is available | Present |
| RSSI history | `RoamingPredictionService` retains a rolling sample collection and exposes sample count | Present |
| Trend analysis | Service classifies improving, stable, and degrading trends from observed samples | Present |
| Candidate ranking | Service ranks viable observed access points and uses the result for recommendations | Present |
| Three-state advisory | `STAY_CONNECTED`, `PREPARE_ROAMING`, and `ROAM_NOW` are supported; no automatic handoff is claimed | Present |
| Multi-factor analyzer | `AnalyzerService` provides health, congestion, security, and band analysis from nearby networks | Present |
| Real speed measurement | `SpeedTestService` performs HTTP-based ping/download/upload measurements; results are persisted through Room | Present |
| Explainability | `NetworkDecisionExplanation` is attached to generated recommendations and rendered only when evidence exists | Present |
| Stale-state clearing | ViewModel clears analyzer state without a connected network or scan results; recommendation engine returns an empty list without a connection | Present |

## Physical test matrix — pending

| Validation area | Required evidence | Status |
|---|---|---|
| Scanner | Multiple APs, duplicate SSID/BSSID preservation, sorting, favorite, rescan | Pending device test |
| Connected network | Wi-Fi off/on, connect/disconnect, SSID/BSSID/RSSI/band state | Pending device test |
| Roaming | Movement between APs with same SSID, changing RSSI/trend/candidate/recommendation | Pending device test |
| Analyzer | Results from multiple locations, congestion and recommended-channel comparison | Pending device test |
| Speed test | Three real tests across location/network, Room history persistence and failure state | Pending device test |
| Recommendations | Each explanation matches actual scanner/analyzer/prediction/speed inputs | Pending device test |
| Edge cases | Permissions, Location Services, rotation, background/resume, repeated actions | Pending device test |
| Patent screenshots/video | All twelve requested observed states | Pending device test |

## Known platform limitations to capture during device testing

- Android controls scan-result freshness and throttling; NetPulse may not match the Android Settings AP count exactly.
- Android can redact the connected SSID or BSSID depending on OS version, granted permissions, and Location Services state.
- Roaming guidance is advisory only. The app does not execute automatic Wi-Fi handoff.
- HTTP speed-test outcomes vary with the network, external endpoint availability, and device conditions.

## Patent alignment status

The implementation statically aligns with the stated technical sequence: real Wi-Fi observations → rolling RSSI history → trend and multi-factor assessment → candidate AP ranking → three-state advisory → explainable recommendation. This is an implementation observation, not a statement of patent approval or legal validity.

Sprint 13's light, Material 3 NetPulse UI and shared components remain present. No design changes were made as part of this Sprint 14 validation record.

## Next physical-validation procedure

1. Connect an Android test device with USB debugging enabled and verify `adb devices -l` reports it as `device`.
2. Install `app/build/outputs/apk/debug/app-debug.apk`.
3. Execute the pending matrix above using real nearby Wi-Fi environments.
4. Add dated screenshots/video and measured observations to this record; do not add inferred values.
