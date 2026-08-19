# NetPulse

[![Android](https://img.shields.io/badge/Android-API%2024%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Min SDK](https://img.shields.io/badge/minSdk-24-orange)](https://developer.android.com/about/versions/nougat)
[![Target SDK](https://img.shields.io/badge/targetSdk-36-blue)](https://developer.android.com/about/versions/15)
[![Status](https://img.shields.io/badge/status-active%20development-yellow)](#validation-status)

NetPulse is a local-first Android Wi-Fi diagnostics application. It discovers nearby access points, evaluates the active connection, measures internet performance, predicts when roaming may be useful, and explains recommendations using observed network data.

NetPulse is advisory software. It does not automatically switch Wi-Fi networks, perform seamless handoff, or claim guaranteed performance improvements.

## Problem and core vision

Android exposes Wi-Fi information through APIs that are permission-gated, privacy-limited, cached, and sometimes redacted. NetPulse turns the information that is actually available into an understandable chain:

```text
Real Wi-Fi observations
        ↓
RSSI history and trend
        ↓
Multi-factor network analysis
        ↓
Candidate access-point ranking
        ↓
Advisory roaming state
        ↓
Rule-based recommendation
        ↓
Explainable local evidence
        ↓
Historical journey
```

No demo networks, random metrics, fabricated coordinates, or seeded optimization events are used.

## Implemented capabilities

### Wi-Fi Scanner

- Reads nearby access points from `WifiManager.scanResults`.
- Preserves SSID, BSSID, RSSI, frequency, channel, security capabilities, and band information.
- Groups duplicate SSIDs while retaining individual BSSID records.
- Supports strongest, alphabetical, band-first, and connected-first presentation sorting.
- Provides connected-state indication only when Android exposes enough identity information to match an AP safely.
- Includes expandable access-point details, signal bars, favorites backed by Room, loading, empty, and error states.

Android controls scan freshness and throttling, so the Scanner may not show the same AP count as Android Settings.

### Network Analyzer

`AnalyzerService` computes values from observed connected and nearby networks:

- Network health score.
- RSSI classification.
- Security protocol and rating.
- 2.4 GHz, 5 GHz, and 6 GHz distribution.
- Per-band channel congestion.
- Lowest-observed-congestion channel recommendation.
- Explainable score contributions.

Analyzer state clears when the connected network or required nearby observations disappear.

### Roaming Prediction

`RoamingPredictionService` maintains up to 20 in-memory RSSI samples for the active network and calculates:

- Improving, stable, or degrading trend.
- Current signal risk.
- Same-SSID and neighboring candidate access points.
- Security-compatible candidate ranking.
- Band and congestion preferences.
- `STAY_CONNECTED`, `PREPARE_ROAMING`, or `ROAM_NOW` advisory output.

Prediction requires a real connected BSSID and a minimum of two samples. Redacted identity or insufficient observations produces an empty prediction rather than an invented result.

### Recommendations and explainability

`RecommendationEngine` combines Scanner, Analyzer, Prediction, and completed Speed Test outputs with deterministic rules. Recommendations include category, priority, severity, confidence, action, expected benefit, timestamp, and a `NetworkDecisionExplanation` containing available evidence such as RSSI, trend, candidate AP, analyzer contributions, and speed-test measurements.

Recommendations are advisory and disappear when their required inputs disappear.

### Speed Test

`SpeedTestService` performs real HTTP-based measurements:

- Ping and jitter samples.
- Download throughput.
- Upload throughput.
- Duration and phase progress.
- Current connected network association.
- Error and cancellation handling.

Completed results are persisted in Room and shown in local history. The app does not use simulated speeds.

### Network Journey

`NetworkJourneyService` records meaningful transitions rather than every recomposition or polling cycle:

- Connected and disconnected.
- Material RSSI improvement or degradation.
- Material analyzer health changes.
- Roaming-state and candidate transitions.
- Generated recommendations.
- Completed speed tests.

Events are stored in the existing Room database and displayed chronologically in the Network Journey screen.

### Signal Map

`HeatmapService` stores observations only when Android provides both a real connected Wi-Fi observation and an available last-known latitude/longitude with location permission and Location Services enabled.

The Signal Map supports observed SSID/BSSID filtering, signal-point visualization, strongest/weakest/average RSSI, observation count, and last-observation metadata. It is explicitly an observation map, not a fabricated indoor floor plan.

### Analytics

Analytics uses persisted and live values for ping history, speed-test count, analyzer health, current roaming state, Network Journey event count, and location-backed signal observation count. Empty histories remain empty; charts are not populated with placeholder values.

### Background observation

The existing WorkManager integration schedules an optional 15-minute constrained observation job when enabled in Settings. It requires a connected network, reads the current connection and already-available last-known location, and persists a valid observation when possible.

It does not call unrestricted background Wi-Fi scans, run a continuous loop, modify router settings, or change the device's Wi-Fi connection.

## Architecture and data flow

```text
Android Wi-Fi / connectivity / location APIs
        ↓
WifiManagerHelper
        ↓
WifiRepositoryImpl
        ↓
Room + domain services
        ↓
WifiWiseViewModel
        ↓
Compose screens and shared NetPulse design system
```

The project uses MVVM, a repository boundary, domain services, and a ServiceLocator-based dependency setup. Business calculations are kept out of Compose screens.

## Persistence

Room database: `wifi_wise_db`, schema version 3.

Persisted tables include saved/favorite networks, scan history, completed speed-test history, Network Journey events, and location-backed heatmap observations.

The database includes explicit migrations from version 1 to 2 and 2 to 3. Destructive fallback migration is not enabled, so unexpected schema changes fail visibly rather than silently deleting user history.

## Navigation and UI

Primary navigation contains Dashboard, Scanner, Analyzer, Recommendations, and Settings. Secondary destinations keep Speed Test, Analytics, History, Network Journey, and Signal Map reachable without overcrowding primary navigation.

The UI uses the existing light-first Material 3 NetPulse design system with shared cards, buttons, badges, loading/empty/error states, rounded geometry, semantic colors, and responsive Compose layouts.

## Android APIs and permissions

The app uses `WifiManager.scanResults`, `ConnectivityManager`, `NetworkCapabilities`, `WifiInfo`, `ScanResult`, `LocationManager.getLastKnownLocation`, WorkManager, Room, and OkHttp/HTTP requests.

Manifest and runtime access includes `ACCESS_WIFI_STATE`, `ACCESS_NETWORK_STATE`, `ACCESS_FINE_LOCATION`, `NEARBY_WIFI_DEVICES` on Android 13+, and `INTERNET` for speed testing. Android versions may redact SSID/BSSID values, and scan results require appropriate permission and system conditions. Location Services may also be required for usable SSID/BSSID and location-backed observations.

## Project structure

```text
app/src/main/java/com/example/
├── data/       Room, DAO, repository, Android Wi-Fi helper
├── domain/     Models, repository contract, domain services
├── ui/         Compose screens, navigation, ViewModel, design system and theme
└── worker/     Constrained background observation scheduling and worker
```

Additional validation notes are maintained in [docs/SPRINT_14_VALIDATION_RECORD.md](docs/SPRINT_14_VALIDATION_RECORD.md).

## Build and run

```bash
./gradlew :app:assembleDebug
```

Windows PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
```

Grant requested Wi-Fi/location permissions and enable Location Services when required by Android. Use a physical device for meaningful scan and speed-test validation.

## Validation status

- `:app:assembleDebug --no-daemon --offline`: passed.
- `:app:compileDebugKotlin --no-daemon --offline`: passed.
- `git diff --check`: passed after the final fixes.
- Physical ADB/Wi-Fi validation: pending; no Android device was available in the validation session.

Remaining work is real-device testing, bug fixes discovered during that testing, documentation, and patent preparation. This README makes no claim that physical measurements have been collected.
