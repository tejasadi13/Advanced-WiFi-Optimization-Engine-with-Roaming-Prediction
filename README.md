# NetPulse

NetPulse is an Android Wi-Fi companion for scanning nearby access points, monitoring the active connection, analyzing Wi-Fi conditions, and predicting when roaming may be useful.

## Current status

The app is actively developed and the following features are implemented:

- Live connected-network details: SSID, RSSI, channel, frequency, and link speed.
- Nearby Wi-Fi scanning using Android `WifiManager.scanResults`.
- Scanner grouping, connected-network indication, signal quality, loading/error states, and Room-backed favorites.
- Wi-Fi Analyzer with health score, signal classification, security assessment, congestion analysis, channel recommendations, and band distribution.
- Roaming Prediction Engine with a rolling 20-sample RSSI history, trend detection, real scan-result candidate selection, and Stay Connected / Prepare Roaming / Roam Now recommendations.
- Material 3 NetPulse UI with system light/dark theme support.

Predictions are computed only from real connected-network and nearby-scan data. When Android redacts the active BSSID or sufficient data is unavailable, NetPulse exposes an empty prediction instead of fabricated values.

## Requirements

- Android Studio
- An Android device or emulator with Wi-Fi support
- Wi-Fi and location permissions granted at runtime where required by Android

## Build

Open the project in Android Studio and run it on a device, or build from the project root:

```bash
./gradlew :app:compileDebugKotlin
```

On Windows:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

## Architecture

The app follows MVVM and a repository-based clean architecture:

`WifiManagerHelper → WifiRepository → WifiWiseViewModel → Compose UI`

Domain services contain Wi-Fi analysis and roaming-prediction calculations. Room is used for saved networks, favorites, and scan history.
