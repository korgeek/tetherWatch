# TetherWatch Development Tracker

> Purpose: Track implementation until the app is done. Keep this file short and update it whenever work starts or finishes.

## Status Rule

```text
[ ] TODO
[~] IN PROGRESS
[x] DONE
[-] SKIPPED
```

## Current Direction

```text
- Galaxy Android 16/API 36 only
- Local-only app: no account, no server sync, no remote backup
- No VPN service
- No Room until long-term history is needed
- DataStore only for local settings when settings are implemented
```

## Build Foundation

```text
[x] Set package/applicationId to com.nextti.tetherwatch
[x] Add minimal launcher Activity
[x] Disable Android backup
[x] Add basic network permissions
[x] Verify unit tests pass
[x] Verify debug APK builds
[x] Replace placeholder UI with Compose
[x] Add MVVM DashboardViewModel with StateFlow
[x] Replace vertical card list with Material 3 cockpit dashboard
[x] Apply Material safe-area layout for status/navigation bars
[x] Set transparent system bars with readable dark status/navigation icons
```

## Traffic Monitor

```text
[x] Read current device rx/tx bytes with TrafficStats
[x] Show current download/upload Mbps
[x] Show session download/upload usage
[x] Add 1-minute traffic bucket tracker
[x] Show recent minute-level usage timeline
[x] Calculate Stable / Bursty / Dropping / Idle
[x] Add unit tests for bucket rollup and drop detection
[-] Add NetworkStatsManager today/month usage lookup
[x] Add traffic bar graph for minute-level throughput visibility
[ ] Add clearer spike/drop markers in UI
[x] Add compact traffic sparkline to first-screen chart band
[x] Add second-level traffic sample history for hero and tile graphs
[x] Show dual down/up traffic sparkline in hero panel
[-] Show mini traffic graph inside Traffic tile
[x] Move traffic graph to hero and remove redundant Traffic tile
[x] Add visible Down/Up legend to the hero traffic graph
```

## Network State

```text
[x] Show active transport summary
[x] Show validated / not validated state
[x] Show metered / unmetered state
[x] Add NetworkCallback for live network changes
[x] Show DNS servers and interface name
[x] Replace verbose network text with compact status badges
[x] Add active dual-SIM data slot snapshot
[x] Classify DNS as ISP/private/public resolver without GeoIP calls
[x] Infer network country from mobile network country before SIM fallback
[x] Show first DNS resolver IP in compact DNS tile
[x] Show compact top header as network type, active SIM/provider, and 2-letter country code
[x] Show DNS addresses instead of resolver/provider labels in the compact DNS tile
```

## Permissions

```text
[x] Document denied-permission UI policy
[x] Show "Permission denied. Data was not collected." for runtime permission-gated data
[x] Make denied-permission UI clickable to request permission or open settings
[x] Add notification permission request flow
```

## Tethering

```text
[x] Build TetheringManager availability PoC
[x] Inspect Galaxy SM-A546E tethering state and API limits with ADB
[x] Show active tethering state from public TetheringManager callback
[x] Show tethering type when available
[x] Show upstream/downstream interface when available
[x] Show tethering error when available
[-] Show full tethered client device list through public app API
[x] Show tethering state and available client count in the compact top header
[x] Verify hotspot ON screen shows Active / Wi-Fi / swlan0 on Galaxy SM-A546E
[ ] Re-run ADB inspection with hotspot on and one connected client
```

## Probe Engine

```text
[x] Add default targets: 1.1.1.1 and 8.8.8.8
[x] Add TCP connect probe
[x] Add HTTPS HEAD fallback probe
[x] Calculate latency, packet loss, jitter
[x] Show target status list
[x] Show probe latency/failure bar graph
[x] Replace probe bar graph with Vico latency line chart and failure strip
[x] Keep probe latency/failure visible in first-screen chart band
[x] Show scrolling probe target chips with IP address and latest response value
```

## Foreground Monitoring

```text
[x] Add MonitorForegroundService
[x] Add notification channel
[-] Keep traffic/network sampling alive while screen is off
[x] Replace connectedDevice foreground service type with dataSync for Galaxy Android 16
[x] Verify Monitor button no longer crashes on Galaxy Android 16
```

## Mobile Signal

```text
[x] Build TelephonyManager PoC
[x] Show carrier/network type
[-] Show signal level/dbm when permission allows
[x] Apply denied-permission UI when permission is refused
```

## Verification Log

```text
[x] 2026-07-01 ./gradlew :app:testDebugUnitTest
[x] 2026-07-01 ./gradlew :app:assembleDebug
[x] 2026-07-01 Compose/MVVM migration ./gradlew :app:testDebugUnitTest
[x] 2026-07-01 Compose/MVVM migration ./gradlew :app:assembleDebug
[x] 2026-07-02 Galaxy SM-A546E API 36 adb install -r app-debug.apk
[x] 2026-07-02 Galaxy SM-A546E app launch smoke: process alive, no recent fatal logcat
[x] 2026-07-02 Probe/traffic graph update ./gradlew :app:testDebugUnitTest
[x] 2026-07-02 Probe/traffic graph update ./gradlew :app:assembleDebug
[x] 2026-07-02 Probe/traffic graph update Galaxy SM-A546E install/launch smoke
[x] 2026-07-02 Vico probe chart ./gradlew :app:testDebugUnitTest
[x] 2026-07-02 Vico probe chart ./gradlew :app:assembleDebug
[x] 2026-07-02 Network badge UI ./gradlew :app:testDebugUnitTest
[x] 2026-07-02 Network badge UI ./gradlew :app:assembleDebug
[x] 2026-07-02 Material 3 cockpit dashboard ./gradlew :app:testDebugUnitTest
[x] 2026-07-02 Material 3 cockpit dashboard ./gradlew :app:assembleDebug
[x] 2026-07-02 Galaxy SM-A546E tethering device-info inspection via ADB and API 36.1 android.jar
[x] 2026-07-02 Tethering dashboard callback model ./gradlew :app:testDebugUnitTest
[x] 2026-07-02 Tethering dashboard callback model ./gradlew :app:assembleDebug
[x] 2026-07-02 Galaxy SM-A546E install/launch smoke: Tether tile Active / Wi-Fi / swlan0 with hotspot ON
[-] 2026-07-02 Galaxy SM-A546E connected-client smoke: hotspot ON but mCurrentConnectionCount=0 and ip neigh empty
[x] 2026-07-02 Safe-area graph-first dashboard ./gradlew :app:testDebugUnitTest
[x] 2026-07-02 Safe-area graph-first dashboard ./gradlew :app:assembleDebug
[x] 2026-07-02 Galaxy SM-A546E safe-area screenshot smoke: top row below status bar, bottom buttons above navigation bar, hero/traffic graphs and DNS IP visible
[x] 2026-07-02 Info-refined dashboard ./gradlew :app:testDebugUnitTest
[x] 2026-07-02 Info-refined dashboard ./gradlew :app:assembleDebug
[x] 2026-07-02 Galaxy SM-A546E screenshot smoke: LTE/SIM/provider/ID header, Off 0 tether status, DNS address list, probe IP response chips, Down/Up legend visible
[x] 2026-07-02 Galaxy SM-A546E Monitor button smoke: tap produced no AndroidRuntime fatal log and app PID stayed alive
```
