# Tethering Device Info Inspection - 2026-07-02

## Scope

- Device: Galaxy SM-A546E
- ADB serial: RRCX2048S5F
- Goal: decide how much tethering client/device information TetherWatch can show without root, hidden APIs, VPN, cloud lookup, or privileged/system permissions.
- App principle: local-only, no fake client list.

## Current App Baseline

| Area | Current implementation | Decision |
| --- | --- | --- |
| Dashboard tether tile | Shows service availability text through `DashboardViewModel.readTethering()` | Baseline only |
| Active tethering state | Not implemented | Needs implementation |
| Tethering type/interface | Not implemented | Public API candidate |
| Connected client list | Not implemented | Restricted unless proven through public API |

Current code only checks whether the Android tethering service exists:

```text
Tethering service available. Waiting for Galaxy callback validation.
```

## ADB Evidence

### Device

```text
RRCX2048S5F device usb:0-1 product:a54xnsxx model:SM_A546E device:a54x transport_id:2
```

### Current tethering state

`adb shell dumpsys tethering` currently reports:

```text
swlan0 - AvailableState - lastError = 0
Upstream wanted: false
Current upstream interface(s): null
mCurrentConnectionCount=0, mLastMaxConnectionCount=0
Client Information:
```

`adb shell ip addr` currently reports:

```text
rmnet15 UP with 10.4.192.248/24
rmnet16 UP with IPv6
wlan0 DORMANT
swlan0 DOWN
```

`adb shell ip neigh` returned no rows.

Interpretation: during this inspection run, Wi-Fi hotspot/tethering was not actively serving clients. The acceptance item that requires one connected client cannot be completed until the hotspot is turned on and a test client joins.

### Historical tethering session evidence

`dumpsys tethering` retained earlier Wi-Fi tethering events:

```text
2026-07-02T10:57:08 startTethering: TetheringRequest[ TETHERING_WIFI ... softApConfig=ssid = "US" ...]
OBSERVED iface=swlan0 state=2 error=0
OBSERVED LinkProperties update iface=swlan0 state=TETHERED
LinkAddresses: [ 10.71.247.26/24 ]
Routes: [ 10.71.247.0/24 -> 0.0.0.0 swlan0 ]
Found upstream interface(s): [rmnet15]
SET DNS forwarders: network=124 dnsServers=[114.5.5.5, 114.5.5.77]
2026-07-02T11:17:08 stopTethering: 0
OBSERVED iface=swlan0 state=1 error=0
```

`dumpsys wifi` also retained Soft AP history:

```text
CMD_SET_AP 1 ... SoftApManagers:1
MaximumSupportedClientNumber=10
mSoftApCountryCode: ID
mCurrentSoftApConfiguration: ssid = "US"
mCurrentSoftApInfoMap {}
Wifi Soft AP state is: false
Soft AP state is: false
```

`dumpsys netstats` retained interface stats:

```text
All wifi interfaces: swlan0 wlan0
All mobile interfaces: rmnet0 rmnet15 rmnet16
ifaceName=swlan0
swlan0 rxBytes=54518028 txBytes=218223569
```

Interpretation: Galaxy SM-A546E uses `swlan0` as the Wi-Fi hotspot downstream interface and `rmnet15` as the mobile upstream interface in the observed session. Netstats can show historical interface traffic, but not tethered client identity.

## Public API Feasibility

The installed compile SDK is:

```text
/Users/jinik/Library/Android/sdk/platforms/android-36.1/android.jar
```

`javap` on the public SDK shows:

```text
android.net.TetheringManager
  registerTetheringEventCallback(Executor, TetheringEventCallback)
  startTethering(TetheringRequest, Executor, StartTetheringCallback)
  stopTethering(TetheringRequest, Executor, StopTetheringCallback)
  unregisterTetheringEventCallback(TetheringEventCallback)

android.net.TetheringManager$TetheringEventCallback
  onTetheredInterfacesChanged(Set<TetheringInterface>)

android.net.TetheringInterface
  getInterface()
  getType()
  getSoftApConfiguration()
```

No public `WifiClient`, `SoftApInfo`, or Soft AP client callback class is present in this API 36.1 `android.jar`.

## Capability Matrix

| Data | Public app API | ADB shell | Decision |
| --- | --- | --- | --- |
| Tethering service available | Yes | Yes | Implement |
| Active tethered interface | Yes, via `TetheringEventCallback.onTetheredInterfacesChanged` | Yes | Implement |
| Tethering type | Yes, via `TetheringInterface.getType()` | Yes | Implement |
| Downstream interface name | Yes, via `TetheringInterface.getInterface()` | Yes | Implement |
| Soft AP SSID/config | Possibly via `getSoftApConfiguration()` | Yes | Optional, avoid exposing sensitive passphrase |
| Last error | Start/stop callback errors only; current callback does not expose current error | Yes | Limited |
| Upstream interface | Not directly exposed by `TetheringManager` callback | Yes | Derive from active `ConnectivityManager` network, label as inferred |
| Connected client count | Not exposed in public SDK inspected here | Shell shows BPF count only when available | Restricted |
| Client MAC/IP/hostname list | Not exposed in public SDK inspected here | Not observed in current inactive state | Restricted |
| Historical traffic on hotspot interface | TrafficStats/netstats style interface counters possible, but app access may be limited by API/permission | Yes | Consider aggregate only |

## Resulting Decision

Next implementation scope should be **restricted status plus public interface details**, not a full client/device list.

Implementable model direction:

```kotlin
data class TetheringDashboard(
    val state: TetheringState,
    val types: Set<TetheringType>,
    val downstreamInterfaces: List<String>,
    val upstreamInterface: String?,
    val lastError: String?,
    val clientVisibility: TetheringClientVisibility
)
```

Recommended UI copy when clients are requested but not available:

```text
Clients: restricted
Device list unavailable on this Android build
```

## Open Follow-Up

### 2026-07-02 implementation smoke update

After implementing the public `TetheringManager` callback model, the debug APK was installed on Galaxy SM-A546E and launched. With hotspot enabled, the app screen showed:

```text
Tether
Active
Wi-Fi / swlan0
Device list unavailable on this Android build.
```

ADB agreed on the active tethering state:

```text
swlan0 - TetheredState - lastError = 0
Upstream wanted: true
Current upstream interface(s): [rmnet15]
mCurrentConnectionCount=0, mLastMaxConnectionCount=0
Client Information:
```

The active-client acceptance item is still pending because no client was connected during this smoke run.

To complete the active-client acceptance criteria, rerun inspection while:

1. Mobile data is on.
2. Galaxy hotspot is on.
3. At least one client device is connected.

Then capture:

```text
adb shell dumpsys tethering
adb shell dumpsys wifi
adb shell ip neigh
adb shell dumpsys netstats
```

If ADB exposes client identifiers only during an active session, keep them as QA evidence only unless a public app API exposes the same data.
