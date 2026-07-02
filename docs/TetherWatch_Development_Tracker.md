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
- Built-in local preferences only for app settings
```

## Product Purpose

```text
TetherWatch is a Galaxy-only local network dashboard for people who use a phone as a mobile internet hub.
It keeps tethering state, active network type, DNS resolver IPs, live traffic, real ping output, service status checks, and background monitoring visible without VPN capture, accounts, cloud sync, or long-term history.
```

## 2026-07-02 Closeout Summary

```text
[x] Applied teddy-bear-watch launcher icon and Pretendard app typography
[x] Built foreground background monitoring with Down/Up notification and stop action
[x] Reworked Back behavior into background-monitoring controls and auto-background setting
[x] Replaced Probe response with terminal-style Ping console using real Android ping output
[x] Added Settings screen for Ping targets, Status targets, console display, app behavior, and theme
[x] Added service Status checks for OpenAI, Claude, GitHub, and Cloudflare status.json endpoints
[x] Refined first screen: larger hero traffic graph, rounded Wi-Fi/5G header, compact DNS row, no Tether card
[x] Confirmed Galaxy SM-A546E install/launch/screenshot smokes through the day
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
[x] Add app top navigation with Tether bold + Watch regular brand and gear Settings entry
[x] Slim top navigation typography, padding, and settings icon size
[x] Keep scroll content unmasked so text does not fade or clip while moving under header/footer bounds
[x] Add safe bottom breathing room instead of overlaying alpha masks on dashboard content
[x] Add subtle animated ambient background behind dashboard and Settings cards
[x] Increase ambient background motion with layered dynamic sweeps
```

## Dashboard Health Score

```text
[x] Calculate weighted health score from link validation, ping latency, loss, jitter, and traffic flow
[x] Expose readable score factors as Net, Ping, Loss, Jitter, and Flow
[x] Replace clipped factor bars and numeric fractions with compact colored status lamps
[x] Move score factor lamps to a horizontal strip at the bottom of the hero card
[x] Replace flat score factor lamps with tiny donut indicators that show each factor's weighted fill
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
[x] Add clearer spike/drop markers in UI
[x] Add compact traffic sparkline to first-screen chart band
[x] Add second-level traffic sample history for hero and tile graphs
[x] Show dual down/up traffic sparkline in hero panel
[-] Show mini traffic graph inside Traffic tile
[x] Move traffic graph to hero and remove redundant Traffic tile
[x] Add visible Down/Up legend to the hero traffic graph
[x] Add compact Mbps axis labels and old/now index to the hero traffic graph
[x] Reduce hero traffic graph line thickness for dense first-screen readability
[x] Enlarge the top hero panel for better score, SIM, and traffic graph readability
[x] Keep hero traffic values based on device-wide TrafficStats counters while hiding the extra scope label in the compact UI
[x] Show both instant traffic rate and sustained usage totals in the hero panel
[x] Add a faint cumulative traffic layer behind the instant Down/Up traffic graph
[x] Shorten hero traffic labels to DN/UP for compact scanability
[x] Keep the hero traffic graph unit fixed to Mbps so the axis label does not flicker between Kbps and Mbps
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
[x] Prefer TelephonyDisplayInfo override labels so NSA 5G can show as 5G instead of raw LTE
[x] Prefer Wi-Fi over mobile SIM labels in the compact top header when Wi-Fi is active
[x] Render compact top header as a rounded inset surface
[x] Show DNS addresses instead of resolver/provider labels in the compact DNS tile
[x] Prefer IPv4 DNS resolver in the compact DNS tile so the primary value fits on one line
[x] Show two DNS resolver IPs in one thin bold row while keeping the DNS icon
[x] Remove duplicate DNS text label from the DNS row because the icon already labels it
[x] Show active network device IP in the hero panel instead of SIM slot label
[-] Show MAC address in the hero panel; avoided because modern Android does not expose a stable useful device MAC for this local dashboard
```

## Permissions

```text
[x] Document denied-permission UI policy
[x] Show "Permission denied. Data was not collected." for runtime permission-gated data
[x] Make denied-permission UI clickable to request permission or open settings
[x] Show an in-app rationale dialog before re-requesting denied permissions
[x] Add notification permission request flow
```

## App Identity

```text
[x] Generate teddy-bear-watch 1024x1024 icon concept
[x] Apply teddy-bear-watch icon to launcher adaptive icon resources
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
[x] Remove the separate Tether summary card; tethering state remains in the compact top header
[x] Hide unknown tethering client count instead of showing a confusing question mark
[x] Show tethering On/Off in the compact top header and suppress the Off client count
[x] Remove developer-facing client-list unavailable warning from the dashboard
[x] Verify hotspot ON screen shows Active / Wi-Fi / swlan0 on Galaxy SM-A546E
[ ] Re-run ADB inspection with hotspot on and one connected client
```

## Probe Engine

```text
[x] Add default targets: 1.1.1.1 and 8.8.8.8
[-] Add TCP connect probe; replaced by actual Android ping command
[-] Add HTTPS HEAD fallback probe; replaced by raw ping error output
[x] Run actual /system/bin/ping for Ping console results
[x] Calculate latency, packet loss, jitter
[x] Show target status list
[x] Show probe latency/failure bar graph
[x] Replace probe bar graph with Vico latency line chart and failure strip
[x] Keep probe latency/failure visible in first-screen chart band
[x] Show scrolling probe target chips with IP address and latest response value
[x] Remove redundant Probe tile and make Probe response the primary probe surface
[x] Double Probe response chart height for easier latency reading
[x] Add color dots that map probe IP chips to chart series
[x] Add tappable Probe response interval setting
[x] Fit Probe response chart to the card width using latest samples only
[x] Remove unexplained probe failure marker strip from the bottom of the chart
[x] Keep Probe response grid and old/now + ms axis labels while fitting the chart
[x] Rename Probe response surface to Ping
[x] Replace probe chart surface with black terminal-style Ping console
[x] Keep enough recent Ping console lines for the largest visible console setting
[x] Use a 6sp single-line Ping console font so ttl/time stays visible on Galaxy width
[x] Use Pretendard Regular/Bold as the app typography family while keeping Ping output monospace
[-] Add tappable Ping settings for IP and interval fields; replaced by the dedicated Settings screen
[-] Restyle Ping settings as a rounded modern dialog with row cards and a primary Save action; replaced by the dedicated Settings screen
[x] Add Ping console font size slider in Ping settings
[x] Save Ping console font size locally with target settings
[x] Remove extra Ping settings subtitle, Target heading, row badges, and row grouping chrome
[x] Validate Ping settings before save: IPv4 target only, integer interval only, 1-255 seconds
[x] Schedule each Ping target using its own interval seconds
[x] Save Ping targets and per-target intervals in local app preferences
[-] Show Ping target list on the dashboard; removed because the console output is enough
[x] Move Ping target editing out of the Ping panel and into the Settings screen
[x] Add Settings screen Ping target rows with IP, interval, add, delete, reset, and validation
[x] Add Ping console line-count setting with 6-30 visible lines
[x] Snap Ping console line-count setting in 3-line steps: 6, 9, 12, 15, 18, 21, 24, 27, 30
[x] Size Ping and Status console panels from selected font size and console line count
[x] Prefix Ping and Status console lines with two-digit HH:mm:ss timestamps
[x] Add slow Ping threshold setting with 1000 ms default, 100-3000 ms range, and 100 ms steps
[x] Color Ping console lines: normal green, slow response yellow, error/unreachable red
[x] Remove visible Ping loss text from the dashboard while keeping loss available for internal health scoring
[x] Add compact dashboard toggles to pause/resume Ping and Status monitoring independently
[x] Gray out Ping and Status console surfaces when their dashboard toggle is off
[x] Exclude Ping, Loss, and Jitter from health score and gray their hero lamps when Ping monitoring is off
[x] Rename Endpoint console card to Status
[x] Use HEAD for Endpoint checks and fallback to GET when HEAD is 405 or returns a non-normal HTTP status
[x] Use GET directly for OpenAI and Claude status API endpoints
[x] Treat any HTTP response as Endpoint reachability success, including API auth responses
[x] Classify HTTP 401/403 as AUTH_FAILED while keeping it green/yellow for network reachability
[x] Add default status endpoints for Claude, OpenAI, GitHub, and Cloudflare status pages
[x] Parse Statuspage JSON status.indicator and status.description into the Endpoint console when present
[x] Show status Endpoint output as indicator / description before HTTP status details
[x] Accept only status.json URLs in Status monitoring settings
[x] Derive Status monitoring display names from status URL domains
[x] Color Status monitoring lines: none green, minor yellow, major/critical/error red
[x] Color Status major and critical indicators red
[x] Cap Status timeout at 5 seconds and disable Status redirects
[x] Move Font size and Console lines into a shared Console display settings group for Ping and Status
[-] Keep Google DNS endpoint as the only DNS-style default target; removed after switching defaults to provider status APIs
[-] Show Endpoint latency as green below the shared slow threshold and yellow at/above the threshold; superseded because slow response threshold is Ping-only
[x] Show TLS, DNS, timeout, connection refused, redirect limit, and generic network error types as Endpoint failures
[x] Limit Endpoint redirects using the Settings redirect limit
```

## Foreground Monitoring

```text
[x] Add MonitorForegroundService
[x] Add notification channel
[x] Start MonitorForegroundService when the dashboard launches
[x] Show live Down/Up traffic rate in the foreground notification
[x] Keep foreground service alive when Back sends app to background
[x] Add Back button monitoring controls: keep in background, stop monitoring, cancel
[x] Add notification tap action that returns to the singleTop monitoring screen
[x] Keep notification taps modal-free; show Monitoring controls only from Back
[x] Add explicit stop-monitoring service action that removes the foreground notification
[x] Add Stop monitoring action directly inside the foreground notification
[x] Run Ping from MonitorForegroundService so background monitoring keeps probing after the UI is backgrounded
[x] Run Endpoint checks from MonitorForegroundService while keeping notification text traffic-only
[x] Keep foreground notification focused on Down/Up traffic only; Ping runs silently in the background
[x] Confirm Android 13+ provides a system Active apps stop affordance for foreground services
[~] Keep traffic/network sampling alive while screen is off
[x] Replace connectedDevice foreground service type with dataSync for Galaxy Android 16
[x] Verify Monitor button no longer crashes on Galaxy Android 16
[x] Restyle in-app Monitoring controls as a rounded modern dialog
[x] Simplify Monitoring controls copy and actions: Background monitoring, explanation, keep background monitoring, stop and close, cancel
[x] Add persisted app behavior settings for keep screen on and auto background monitoring
[x] Auto-send Back to background when auto background monitoring is enabled
```

## Settings

```text
[x] Add dedicated Settings screen instead of Ping panel popup settings
[x] Add Android-style add/delete Ping target controls
[x] Remove explicit Settings Save button and make "< Back" validate, save, and close
[x] Keep Settings open when Ping or Endpoint values are invalid
[x] Place Add target on the left and Reset on the right inside target settings groups
[x] Require confirmation before resetting Ping or Endpoint targets to default values
[x] Show Ping target delete as compact red trash icon with confirmation dialog
[x] Widen Ping IP field and keep Interval label on one line
[x] Add app behavior toggles: keep screen on and auto background monitoring
[x] Add theme mode setting: follow device, light, black
[x] Add Endpoint target settings with URL, interval, timeout, and redirect limit
[-] Rename slow Ping threshold setting to shared Slow response threshold; superseded because slow response threshold is Ping-only
[x] Order Settings groups as Ping targets, Status targets, then Console display
[x] Share the animated ambient background with Settings without covering card surfaces
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
[x] 2026-07-02 Probe-focused dashboard ./gradlew :app:testDebugUnitTest
[x] 2026-07-02 Probe-focused dashboard ./gradlew :app:assembleDebug
[x] 2026-07-02 Galaxy SM-A546E screenshot smoke: Probe tile removed, bottom actions removed, enlarged Probe response visible with series color dots
[x] 2026-07-02 Galaxy SM-A546E Probe interval smoke: tapping Probe response opens interval dialog and selecting 10 sec updates the panel label
[x] 2026-07-02 Permission rationale + launcher icon ./gradlew testDebugUnitTest assembleDebug
[x] 2026-07-02 Galaxy SM-A546E permission rationale install/launch/dialog smoke
[x] 2026-07-02 Foreground monitoring startup ./gradlew testDebugUnitTest assembleDebug
[x] 2026-07-02 Galaxy SM-A546E HOME smoke: MonitorForegroundService remains isForeground=true
[x] 2026-07-02 Live notification traffic + fitted Probe chart ./gradlew testDebugUnitTest assembleDebug
[x] 2026-07-02 Galaxy SM-A546E notification dump: android.text shows Down/Up Mbps
[x] 2026-07-02 Galaxy SM-A546E screenshot smoke: Probe chart fits card width and failure marker strip is gone
[x] 2026-07-02 Galaxy SM-A546E screenshot smoke: Probe chart keeps grid plus old/now and ms axis labels
[x] 2026-07-02 Hero traffic chart axis ./gradlew testDebugUnitTest assembleDebug
[x] 2026-07-02 Galaxy SM-A546E screenshot smoke: hero traffic chart shows Mbps scale, old/now index, thin Down/Up lines
[x] 2026-07-02 Ping console settings/parser/buffer ./gradlew :app:testDebugUnitTest
[x] 2026-07-02 Ping console final ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 Galaxy SM-A546E screenshot smoke: Ping console shows actual ping lines, default 1.1.1.1/8.8.8.8 targets, settings dialog, and wrapped time values
[x] 2026-07-02 Back/notification monitoring controls ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 Galaxy SM-A546E BACK smoke: Monitoring controls dialog appears instead of closing immediately
[x] 2026-07-02 Galaxy SM-A546E background smoke: Keep in background moves app home and MonitorForegroundService remains isForeground=true
[-] 2026-07-02 Galaxy SM-A546E notification action smoke: OPEN_MONITORING previously showed Monitoring controls; superseded by no-modal notification tap behavior
[x] 2026-07-02 Galaxy SM-A546E stop smoke: Stop monitoring removes MonitorForegroundService; relaunch starts it again as isForeground=true
[x] 2026-07-02 Hero IP/size update ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 Galaxy SM-A546E screenshot smoke: enlarged hero shows active network IP 10.4.192.248 instead of Indonesia
[x] 2026-07-02 Ping per-target interval + compact console ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 Galaxy SM-A546E screenshot smoke: Ping target list hidden, console shows 10 compact rows with ttl/time
[x] 2026-07-02 Galaxy SM-A546E screenshot smoke: Ping settings dialog shows separate IP and Interval fields per row
[x] 2026-07-02 Ping console 6sp font ./gradlew :app:testDebugUnitTest :app:assembleDebug; Galaxy SM-A546E screenshot smoke saved at /private/tmp/tetherwatch-ping-6sp-console.png
[x] 2026-07-02 Pretendard Regular/Bold + modern Ping settings dialog ./gradlew :app:testDebugUnitTest :app:assembleDebug; Galaxy SM-A546E screenshot smoke saved at /private/tmp/tetherwatch-pretendard-ping-dialog.png
[x] 2026-07-02 Ping console font size slider ./gradlew :app:testDebugUnitTest :app:assembleDebug; Galaxy SM-A546E screenshot smoke saved at /private/tmp/tetherwatch-ping-font-slider.png
[x] 2026-07-02 Ping settings validation, 15-line console, notification stop action, hero IP, modern monitoring dialog ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 Foreground service background Ping notification + simplified Monitoring controls ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 Settings navigation/app behavior/theme update ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 Galaxy SM-A546E Settings smoke: top nav opens Settings, Ping target add/delete rows, app behavior toggles, and theme options visible
[x] 2026-07-02 Galaxy SM-A546E notification dump: foreground notification text shows Down/Up only and no Ping result
[x] 2026-07-02 Ping console line count + slow/error color rules ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 Galaxy SM-A546E screenshot smoke: Settings shows Console lines and Slow ping threshold; Ping console marks slow responses yellow
[x] 2026-07-02 Settings icon/delete polish ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 Galaxy SM-A546E screenshot smoke: Tether bold/Watch regular, gear Settings, Save icon button, trash delete buttons, and delete confirmation dialog
[x] 2026-07-02 5G display label + Ping loss visible text removal + Settings compact polish ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 Galaxy SM-A546E screenshot smoke: header shows 5G via TelephonyDisplayInfo NR_NSA, Ping loss text removed, Settings has outline Save icon and compact trash icon
[x] 2026-07-02 Endpoint HEAD/GET fallback, HTTP status console, shared slow threshold, timeout, redirect settings ./gradlew :app:testDebugUnitTest
[x] 2026-07-02 Settings auto-save Back navigation ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 Settings target action alignment and reset confirmation ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 Subtle animated ambient background ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 LLM provider reachability defaults and AUTH_FAILED success classification ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 Dynamic ambient background sweeps ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 OpenAI/Claude status endpoint defaults and status.indicator parsing ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 GitHub status endpoint and indicator/description console format ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 Status monitoring rename, status.json validation, and indicator color rules ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 Status title, major/critical red, no redirects, shared Console display settings ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 Rounded top connection header, Wi-Fi header preference, and notification tap without modal ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 Galaxy SM-A546E screenshot smoke: Wi-Fi header shows Wi-Fi / wlan0 inside rounded inset surface
[x] 2026-07-02 Galaxy SM-A546E notification action smoke: OPEN_MONITORING returns to dashboard without Monitoring controls
[x] 2026-07-02 Galaxy SM-A546E Back smoke: Monitoring controls dialog appears only when Back is pressed
[x] 2026-07-02 Slim top navigation, Cloudflare status default, Ping-only slow threshold, 6-30 console line steps, dynamic console height, and Settings order ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 Galaxy SM-A546E screenshot smoke: slim top navigation, Cloudflare status line, shorter font/line-driven console panels, and Settings order captured
[x] 2026-07-02 Scroll edge alpha fades for top nav and bottom clipping ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 Galaxy SM-A546E screenshot smoke: scroll body fades under top nav and near bottom edge
[x] 2026-07-02 Tether tile TH removal, compact DNS IPv4 preference, and scroll fade alpha mask ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 Galaxy SM-A546E screenshot smoke: Tether tile has no TH badge, DNS primary value is one-line IPv4, and scroll fade no longer draws a bright footer/header overlay
[x] 2026-07-02 Full-width DNS card with icon and two bold resolver IPs, separate Tether card removed ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 Galaxy SM-A546E screenshot smoke: DNS card shows icon plus two bold IPs on one line and no Tether card
[x] 2026-07-02 Thin one-line DNS resolver row and fully unmasked scroll body ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 DNS row duplicate text label removed; icon now labels the resolver row ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 Galaxy SM-A546E screenshot smoke: DNS row shows only the DNS icon plus resolver IP text, and scroll content is not alpha-masked; saved at /private/tmp/tetherwatch-dns-icon-only-unmasked-scroll.png
[x] 2026-07-02 Hide unknown tether client count, remove client-list unavailable warning, and keep hero traffic values based on device-wide counters ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 Galaxy SM-A546E screenshot smoke: Tethering header no longer shows unknown client count, no client-list unavailable warning is visible, and hero graph values remain device-wide; saved at /private/tmp/tetherwatch-device-traffic-no-client-warning.png
[x] 2026-07-02 Instant and cumulative traffic display: hero shows current Mbps plus 60s/session usage, and graph overlays cumulative traffic shape ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 Galaxy SM-A546E screenshot smoke: hero traffic shows Down/Up Mbps, 60s total, Session total, and cumulative traffic layer without text clipping; saved at /private/tmp/tetherwatch-traffic-rate-and-usage.png
[x] 2026-07-02 Score breakdown status lamps and DN/UP traffic labels ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 Galaxy SM-A546E screenshot smoke: score ring shows readable Net/Ping/Loss/Jitter/Flow labels with colored status lamps and no clipped numeric fractions; saved at /private/tmp/tetherwatch-score-status-lamps-dn-up.png
[x] 2026-07-02 Hero card layout: top row split into info, DN/UP rate, and 60s/session usage columns; score ring aligned to the first third and traffic graph spans the second and third columns; score lamps along the bottom ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 Hero card typography: normalize top three-column text to the same compact metric size for cleaner alignment
[x] 2026-07-02 Galaxy SM-A546E screenshot smoke: hero card shows equal top columns, the score ring aligned to the first third, the graph spanning the second and third columns, and normalized top typography; saved at /private/tmp/tetherwatch-hero-grid-typography.png
[x] 2026-07-02 Ping and Status console timestamps: prefix both console streams with two-digit HH:mm:ss while preserving line color classification ./gradlew :app:testDebugUnitTest :app:assembleDebug
[-] 2026-07-02 Hero usage column: shorten Session label to Sess so the session total fits in the 3-column hero grid; superseded by restored Session label after the 60-second graph rebuild
[x] 2026-07-02 Flow-first traffic graph: EWMA flow lines, drop markers, compact flow/drop label, preserved DN/UP and 60s/session hero metrics ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 Galaxy SM-A546E screenshot smoke: flow-first hero graph shows DN/UP, 60s total, Session total, compact flow label, and no text clipping; saved at /private/tmp/tetherwatch-flow-first-traffic.png
[x] 2026-07-02 Score factor indicators: replace flat colored dots with tiny donut arcs showing each factor's weighted fill ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 Galaxy SM-A546E screenshot smoke: score factor indicators show tiny tinted donut rings instead of flat color dots; saved at /private/tmp/tetherwatch-score-mini-donuts-tinted.png
[x] 2026-07-02 Traffic graph 60-second window: replace last-12-sample timeline with fixed 60s x-axis, active/idle label, minimum 0.25 Mbps axis, and no cumulative relative layer ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 Galaxy SM-A546E screenshot smoke: hero traffic graph shows 60s->now axis, active seconds label, readable 60s/Session totals, and no cumulative layer; saved at /private/tmp/tetherwatch-traffic-60s-window-session.png
[x] 2026-07-02 Tethering header On/Off color and Off count suppression ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 Ping/Status compact dashboard toggles with foreground-service pause support ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 Galaxy SM-A546E screenshot smoke: Ping and Status panels show compact green toggles beside Jitter/status.json; saved at /private/tmp/tetherwatch-ping-status-toggles.png
[x] 2026-07-02 Ping/Status toggle-off console gray state ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 Ping-off score behavior: exclude Ping/Loss/Jitter inputs and gray disabled hero lamps ./gradlew :app:testDebugUnitTest :app:assembleDebug
[x] 2026-07-02 Galaxy SM-A546E screenshot smoke: Ping Off disables Ping/Loss/Jitter lamps and grays the Ping console; saved at /private/tmp/tetherwatch-ping-off-score-actual.png
```
