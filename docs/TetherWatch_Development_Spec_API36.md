# TetherWatch 개발 정의서 v1.0

> **문서 목적**  
> 본 문서는 Android 16/API 36 이상 삼성 갤럭시 장비를 대상으로 하는 네트워크·테더링 모니터링 앱 **TetherWatch**의 제품 정의, 기능 정의, 기술 구조, 데이터 모델, 권한 설계, 화면 설계, 개발 착수 기준을 정리한 개발 기준 문서이다.

---

## 0. 최종 결정 사항 요약

| 항목 | 결정 |
|---|---|
| 앱 이름 | **TetherWatch** |
| 대상 기기 | **Samsung Galaxy 전용** |
| 대상 OS | **Android 16 이상** |
| API 기준 | **API 36** |
| 개발 방식 | **Android Native** |
| 주 언어 | **Kotlin** |
| UI 프레임워크 | **Jetpack Compose** |
| 핵심 API | `TetheringManager`, `ConnectivityManager`, `TrafficStats`, `NetworkStatsManager`, `TelephonyManager` |
| 배포 형태 | 초기 APK/내부 배포, 이후 Play Store 또는 Enterprise 배포 검토 |
| 핵심 차별점 | Galaxy 테더링 상태, 네트워크 품질, 지연시간, 트래픽, 모바일 신호를 하나의 대시보드에서 통합 감시 |

---

## 1. 제품 정의

### 1.1 제품명

**TetherWatch**

### 1.2 제품 한 줄 정의

**TetherWatch는 삼성 갤럭시 기기를 테더링 허브 또는 모바일 라우터처럼 사용하는 사용자를 위해, 테더링 상태·모바일 네트워크 품질·지연시간·트래픽 사용량을 실시간으로 보여주는 Galaxy 전용 네트워크 모니터링 앱이다.**

### 1.3 해결하려는 문제

사용자가 Android 테더링을 자주 사용할 때 다음 문제가 발생한다.

1. 지금 테더링이 정상적으로 동작하는지 바로 알기 어렵다.
2. 인터넷이 느린 원인이 통신사망인지, DNS인지, 테더링 상태인지 구분하기 어렵다.
3. 핫스팟에 연결된 장비가 데이터를 과도하게 사용하는지 확인하기 어렵다.
4. 현재 업로드/다운로드 속도와 데이터 사용량을 한눈에 보기 어렵다.
5. 데이터가 순간적으로 몰렸는지, 시간대별로 안정적으로 흘렀는지 보기 어렵다.
6. 1.1.1.1, 8.8.8.8 같은 주요 인터넷 대상과 OpenAI/Claude/GitHub/Cloudflare 상태 페이지까지 실제 도달성과 응답성이 어떤지 지속적으로 확인하기 어렵다.
7. 일반 네트워크 속도 측정 앱은 테더링 상태, 트래픽, 모바일 신호 상태를 통합적으로 제공하지 않는다.

### 1.4 핵심 사용자

| 사용자 유형 | 설명 |
|---|---|
| 테더링 파워 유저 | 노트북/태블릿을 갤럭시 핫스팟에 자주 연결하는 사용자 |
| 현장 엔지니어 | 고객사/현장에서 모바일 네트워크를 임시 인터넷 회선으로 사용하는 사용자 |
| 기술영업/프리세일즈 | 데모, PoC, 외부 미팅에서 안정적인 모바일 인터넷을 확인해야 하는 사용자 |
| IT 관리자 | 지급 단말의 네트워크 상태와 테더링 사용 상태를 확인해야 하는 관리자 |
| 개발자/네트워크 담당자 | DNS, 지연시간, 테더링 상태를 빠르게 보고 싶은 사용자 |

---

## 2. 제품 목표

### 2.1 사용자가 앱을 열었을 때 바로 답해야 하는 질문

TetherWatch는 다음 질문에 즉시 답할 수 있어야 한다.

```text
1. 지금 인터넷은 정상인가?
2. 지금 테더링은 켜져 있고 정상 동작 중인가?
3. 1.1.1.1, 8.8.8.8, 사용자 지정 IP까지 실제 ping 응답시간은 얼마인가?
4. 패킷 손실률과 지터는 어떤가?
5. 현재 다운로드/업로드 속도는 얼마인가?
6. 오늘/이번 세션/이번 달 데이터 사용량은 얼마이고, 언제 많이 사용했는가?
7. 모바일 신호 상태가 나쁜가?
8. 지금 어느 네트워크 대상 또는 서비스 상태 페이지에서 문제가 보이는가?
9. 연결된 장비 수를 추정할 수 있는가?
10. 문제가 발생하면 어떤 원인이 가장 가능성이 높은가?
```

### 2.2 제품 방향

TetherWatch는 단순한 속도 측정 앱이 아니라 **상시 네트워크 계기판**이다.

```text
Speed Test App        = 순간 속도 측정
TetherWatch           = 지속적인 네트워크 상태 감시
```

---

## 3. 플랫폼 및 SDK 정의

### 3.1 Android 기준

```kotlin
android {
    namespace = "com.nextti.tetherwatch"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nextti.tetherwatch"
        minSdk = 36
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }
}
```

### 3.2 Android 16/API 36으로 확정한 이유

TetherWatch의 핵심은 **테더링 상태 감시**이다. Android 15/API 35까지 지원하면 설치 가능 범위는 넓어지지만, 핵심 기능인 `TetheringManager` 기반 구현에서 제약이 생길 수 있다.

따라서 다음 기준으로 확정한다.

```text
minSdk = 36
compileSdk = 36
targetSdk = 36
```

### 3.3 대상 장비 정보 기준

현재 기준 테스트 장비:

```text
Device: Samsung Galaxy
Android Version: Android 16
API Level: 36
Kernel: 5.15.189
Kernel lineage string: android13-3-33470412 계열
```

주의: 커널 문자열에 `android13`이 들어간다고 해서 OS가 Android 13이라는 뜻은 아니다. 앱 개발 기준은 **Android OS 버전/API level**이다.

---

## 4. 기술 스택

| 영역 | 기술 |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM. Clean Architecture는 코드가 커질 때만 검토 |
| Reactive State | Kotlin Flow / StateFlow |
| Async | Kotlin Coroutines |
| Local DB | MVP 제외. Room은 장기 히스토리가 필요할 때만 검토 |
| Settings | SharedPreferences 기반 로컬 설정. cloud sync/backup 없음 |
| Background | Foreground Service |
| Notification | Android Notification Channel |
| Network State | ConnectivityManager / NetworkCapabilities / LinkProperties |
| Tethering | TetheringManager |
| Traffic | TrafficStats / NetworkStatsManager |
| Mobile Signal | TelephonyManager / SignalStrength / CellInfo |
| Flow/Port Analysis | 제외 |
| Export | 제외. 앱은 설치 후 바로 쓰는 로컬 모니터링 도구로 유지 |
| Enterprise 확장 | Samsung Knox SDK 검토 |

---

## 5. 전체 시스템 구조

```text
TetherWatch
├─ app
│  ├─ MainActivity
│  ├─ TetherWatchApplication
│  └─ Dependency Injection
│
├─ presentation
│  ├─ dashboard
│  ├─ latency
│  ├─ traffic
│  ├─ tethering
│  ├─ devices
│  ├─ signal
│  ├─ alerts
│  └─ settings
│
├─ domain
│  ├─ model
│  ├─ usecase
│  ├─ analyzer
│  └─ scoring
│
├─ data
│  ├─ repository
│  ├─ datastore
│  └─ session
│
├─ platform
│  ├─ connectivity
│  ├─ tethering
│  ├─ traffic
│  ├─ telephony
│  ├─ probe
│  ├─ foreground
│  └─ notification
│
└─ common
   ├─ permission
   ├─ logging
   ├─ error
   ├─ time
   └─ privacy
```

---

## 6. 앱 모드 정의

### 6.1 Standard Mode

일반 모드이다. VPN 권한이나 계정 없이 동작한다.

제공 기능:

```text
- 현재 네트워크 상태
- 테더링 상태
- 다중 대상 Ping 상태 확인
- 주요 서비스 Statuspage API 도달성 및 상태 확인
- 전체 업로드/다운로드 속도
- 세션/일간/월간 데이터 사용량
- 모바일 신호 상태
- 알림
```

제약:

```text
- 포트/세션 상세 분석 제외
- 연결 장비별 트래픽은 정확하지 않을 수 있음
- 테더링 클라이언트별 사용량은 추정값으로 표시해야 함
```

### 6.2 VPN Mode 제외

TetherWatch는 `VpnService` 기반 트래픽 가로채기, 패킷 파싱, 포트/세션 상세 분석을 제품 목표에서 제외한다.

제외 이유:

```text
- 사용자가 설치 후 바로 쓰는 단순한 Galaxy 모니터링 앱으로 유지
- VPN 권한 설명, 다른 VPN 앱과의 충돌, Play Store 심사 부담 제거
- payload/payload-like 데이터 처리 위험 제거
```

대신 제공하는 범위:

```text
- ConnectivityManager 기반 현재 네트워크 상태
- TrafficStats/NetworkStatsManager 기반 전체 트래픽
- Ping 기반 대상별 latency/availability
- Statuspage JSON 기반 서비스 상태 확인
- TetheringManager 기반 테더링 상태
```

### 6.3 Galaxy Enterprise Mode

Samsung Knox, Device Owner, Knox Platform for Enterprise 등을 사용하는 기업용 확장 모드이다.

제공 가능성 검토 기능:

```text
- Knox Network Platform Analytics 연동
- 관리형 단말 네트워크 메타데이터 분석
- 정책 기반 네트워크 제어
- 방화벽 정책 연동
- 핫스팟/테더링 정책 제어
- 기업 단말 배포 및 관리
```

---

## 7. 핵심 기능 정의

---

# 7.1 Dashboard

## 목적

사용자가 앱을 열자마자 전체 네트워크 상태를 파악하게 한다.

## 표시 항목

```text
- TetherWatch Score
- 전체 상태: Excellent / Good / Unstable / Poor / Offline
- 현재 네트워크 타입: 5G / LTE / Wi-Fi / Ethernet / Tethering
- 인터넷 검증 상태
- 테더링 상태
- 현재 다운로드 속도
- 현재 업로드 속도
- 평균 latency
- packet loss
- jitter
- 모바일 신호 레벨
- 세션 데이터 사용량
- 오늘 데이터 사용량
- 연결 장비 수: 가능 시 표시
- 최근 알림/장애 이벤트
```

## 상태 색상 예시

| 상태 | 의미 |
|---|---|
| Excellent | 매우 안정적 |
| Good | 정상 |
| Unstable | 불안정 |
| Poor | 사용 어려움 |
| Offline | 인터넷 불가 |

## 상태 판정 기준 초안

```text
Excellent
- validated network
- average latency < 50ms
- packet loss < 1%
- jitter < 20ms

Good
- validated network
- average latency < 100ms
- packet loss < 3%

Unstable
- average latency 100~250ms
- packet loss 3~10%
- DNS 실패 간헐 발생

Poor
- average latency > 250ms
- packet loss > 10%
- probe 연속 실패

Offline
- active network 없음
- internet validation 실패
- 모든 probe 실패
```

---

# 7.2 Network State Monitor

## 목적

Android 시스템이 인식하는 현재 네트워크 상태를 실시간으로 감시한다.

## 사용 API

```kotlin
ConnectivityManager
NetworkCallback
NetworkCapabilities
LinkProperties
```

## 수집 데이터

```kotlin
data class NetworkStateSnapshot(
    val timestamp: Long,
    val isConnected: Boolean,
    val hasInternet: Boolean,
    val isValidated: Boolean,
    val isMetered: Boolean,
    val isRoaming: Boolean,
    val transports: Set<TransportType>,
    val interfaceName: String?,
    val ipv4Addresses: List<String>,
    val ipv6Addresses: List<String>,
    val dnsServers: List<String>,
    val gateway: String?
)
```

## 이벤트 정의

```text
NETWORK_AVAILABLE
NETWORK_LOST
NETWORK_CAPABILITIES_CHANGED
LINK_PROPERTIES_CHANGED
VALIDATION_CHANGED
TRANSPORT_CHANGED
DNS_CHANGED
METERED_CHANGED
```

---

# 7.3 Tethering Monitor

## 목적

Android 16/API 36의 `TetheringManager`를 핵심으로 사용해 테더링 상태와 이벤트를 추적한다.

## 사용 API

```kotlin
TetheringManager
TetheringManager.TetheringEventCallback
TetheringManager.TetheringRequest
TetheringManager.StartTetheringCallback
ConnectivityManager
LinkProperties
```

## 필수 기능

```text
- 테더링 활성 여부 확인
- Wi-Fi hotspot / USB tethering / Bluetooth tethering 구분
- tethering start/stop 이벤트 기록
- tethering interface 변경 감시
- upstream network 변경 감시
- downstream interface 변경 감시
- tethering error code 표시
- 테더링 세션 시작/종료 시간 기록
- 테더링 세션 누적 트래픽 표시
```

## 테더링 상태 모델

```kotlin
data class TetheringState(
    val timestamp: Long,
    val isTetheringActive: Boolean,
    val tetheringTypes: Set<TetheringType>,
    val upstreamInterface: String?,
    val downstreamInterfaces: List<String>,
    val tetheredInterfaces: List<String>,
    val lastError: TetheringError?,
    val sessionId: String?
)

enum class TetheringType {
    WIFI_HOTSPOT,
    USB,
    BLUETOOTH,
    ETHERNET,
    UNKNOWN
}
```

## UI 표시 예시

```text
Hotspot Status: Active
Type: Wi-Fi Hotspot
Upstream: Cellular 5G
Downstream: wlan0
Session: 01:24:35
Traffic: 3.2 GB down / 820 MB up
Last Event: Tethered interface changed
```

## 주의 사항

일반 앱 권한만으로 모든 핫스팟 클라이언트의 상세 정보와 장비별 트래픽을 항상 정확하게 가져올 수 있다고 가정하면 안 된다. 정확도는 Android 버전, 삼성 One UI 구현, 권한, Knox/Enterprise 상태에 따라 달라질 수 있다.

---

# 7.4 Ping and Status Monitoring

## 목적

사용자가 지정한 IP에 대해 Android의 실제 `/system/bin/ping` 결과를 콘솔처럼 표시하고, 주요 서비스의 `status.json` 엔드포인트를 주기적으로 조회해 서비스 도달성과 상태를 함께 보여준다.

## Ping 기본 대상

| Label | Target | 기본 Method |
|---|---|---|
| Cloudflare | 1.1.1.1 | `/system/bin/ping` |
| Google | 8.8.8.8 | `/system/bin/ping` |
| User Target | 사용자 입력 IPv4 | `/system/bin/ping` |

## Status 기본 대상

```text
https://status.openai.com/api/v2/status.json
https://status.claude.com/api/v2/status.json
https://www.githubstatus.com/api/v2/status.json
https://www.cloudflarestatus.com/api/v2/status.json
```

Status 응답은 Statuspage JSON의 `status.indicator`와 `status.description`을 우선 표시한다.

```text
none      = 정상, 초록색
minor     = 주의, 노란색
major     = 장애, 빨간색
critical  = 심각, 빨간색
network error / DNS / TLS / timeout / connection refused = 실패, 빨간색
```

## 사용자 설정

```kotlin
data class PingTarget(
    val ip: String,
    val intervalSeconds: Int,
    val enabled: Boolean
)

data class StatusTarget(
    val url: String,
    val intervalSeconds: Int,
    val timeoutSeconds: Int,
    val enabled: Boolean
)
```

## 측정 주기

Ping과 Status 대상은 각 row마다 개별 interval을 가진다. Ping interval은 1~255초 정수만 허용하고, Status timeout은 최대 5초로 제한한다. Status는 redirect를 따르지 않는다.

## 결과 데이터

```kotlin
data class ProbeResult(
    val id: Long,
    val target: String,
    val timestamp: Long,
    val success: Boolean,
    val latencyMs: Long?,
    val errorType: String?,
    val networkType: NetworkType,
    val transport: TransportType
)
```

## 계산 지표

```text
- Ping console: 최근 N줄만 유지
- Ping jitter
- Ping slow threshold: 기본 1000ms, 100~3000ms, 100ms step
- Status indicator / description
- DNS/TLS/timeout/connection refused 같은 네트워크 오류 타입
```

## Jitter 계산 초안

```text
jitter = average(abs(latency[n] - latency[n-1]))
```

---

# 7.5 Traffic Monitor

## 목적

현재 기기의 전체 업로드/다운로드 속도와 사용량을 실시간으로 표시한다.

## 사용 API

```kotlin
TrafficStats
NetworkStatsManager
```

## 필수 표시 항목

```text
- 현재 다운로드 Mbps
- 현재 업로드 Mbps
- 현재 총 throughput Mbps
- 세션 다운로드 사용량
- 세션 업로드 사용량
- 오늘 다운로드 사용량
- 오늘 업로드 사용량
- 오늘 시간대별 사용량
- 세션 분 단위 사용량
- throughput stability: Stable / Bursty / Dropping / Idle
- spike/drop 구간 표시
- 이번 달 사용량
- 모바일 데이터 사용량
- Wi-Fi 사용량
```

## 실시간 속도 계산

```text
rxBps = (currentRxBytes - previousRxBytes) / intervalSeconds
txBps = (currentTxBytes - previousTxBytes) / intervalSeconds

downloadMbps = rxBps * 8 / 1_000_000
uploadMbps = txBps * 8 / 1_000_000
```

## 시간대별 사용량

```text
- 세션 중 1분 단위 rx/tx bucket을 메모리에 유지한다.
- 오늘/이번 달 총량과 시간대별 bucket은 가능하면 NetworkStatsManager에서 조회한다.
- 별도 백업, 서버 전송, Room 저장 없이 화면 표시용으로 계산한다.
```

## 안정성 판단

```text
Stable
- 최근 5분 동안 throughput 변동이 작고 drop 구간이 없음

Bursty
- 짧은 시간에 사용량 spike가 반복됨

Dropping
- active traffic 중 0에 가까운 구간이 반복되거나 급격히 떨어짐

Idle
- 의미 있는 traffic이 거의 없음
```

## 데이터 모델

```kotlin
data class TrafficSample(
    val timestamp: Long,
    val rxBytesTotal: Long,
    val txBytesTotal: Long,
    val rxBytesMobile: Long,
    val txBytesMobile: Long,
    val rxBps: Double,
    val txBps: Double,
    val networkType: NetworkType
)

data class TrafficBucket(
    val startTimestamp: Long,
    val bucketSizeMs: Long,
    val rxBytes: Long,
    val txBytes: Long,
    val averageRxBps: Double,
    val averageTxBps: Double,
    val peakRxBps: Double,
    val peakTxBps: Double
)
```

---

# 7.6 Mobile Signal Monitor

## 목적

네트워크 품질 저하의 원인이 모바일 신호인지 판단할 수 있게 한다.

## 사용 API

```kotlin
TelephonyManager
SignalStrength
CellInfo
ServiceState
```

## 표시 항목

```text
- 통신사명
- SIM 상태
- 데이터망 타입: 5G / LTE / 3G 등
- 5G NSA / SA 가능 시 표시
- signal level
- dBm
- roaming 여부
- data connection state
- voice/data registration state
- cell info: 권한 허용 시
```

## 데이터 모델

```kotlin
data class SignalSample(
    val timestamp: Long,
    val carrierName: String?,
    val networkType: String?,
    val signalLevel: Int?,
    val dbm: Int?,
    val isRoaming: Boolean,
    val simState: String?
)
```

---

# 7.7 Connected Device Monitor

## 목적

테더링에 연결된 장비와 장비별 트래픽 사용량을 가능한 범위에서 표시한다.

## 기능 목표

```text
- 연결 장비 수 표시
- 장비 IP 표시
- 장비 MAC 표시 가능 시 표시
- 장비 hostname 표시 가능 시 표시
- 장비별 현재 트래픽 추정
- 장비별 세션 사용량 추정
- 새 장비 연결 알림
- 과도한 사용량 알림
```

## 구현 전략

| 방식 | 설명 | 정확도 |
|---|---|---|
| TetheringManager/LinkProperties 기반 | tethered interface, subnet, route 정보 기반 추정 | 낮음~중간 |
| Knox Enterprise/NPA | 관리형 단말의 네트워크 메타데이터 활용 | 높음 가능성 |

## UI 표기 원칙

정확도가 낮거나 추정값인 경우 반드시 표시한다.

```text
Device 1
IP: 192.168.43.22
Traffic: 1.2 GB estimated
Confidence: Medium
```

---

# 7.8 Port & Flow Monitor

## 목적

제품 목표에서 제외한다.

## 구현 방식

구현하지 않는다.

## 제외 범위

```text
- VpnService
- packet parser
- port/session dashboard
- app별 flow 분석
- payload 또는 packet metadata 저장
```

## UI 표기 원칙

```text
- 화면에 Ports & Flows 메뉴를 만들지 않는다.
- 포트/세션 단위 장애 원인 추정 문구를 표시하지 않는다.
```

---

# 7.9 Alert Engine

## 목적

네트워크 이상 징후를 사용자에게 즉시 알려준다.

## 알림 조건

```text
- 인터넷 끊김
- active network lost
- 3회 이상 연속 probe 실패
- latency 200ms 이상 30초 지속
- packet loss 10% 이상
- jitter 급증
- DNS resolve 실패 반복
- upload spike 발생
- download spike 발생
- 새 장비 연결 감지
- 모바일 데이터 사용량 한도 임박
- 테더링 종료
```

## 알림 예시

```text
TetherWatch Alert
8.8.8.8 latency is unstable.
Average: 240ms
Packet loss: 18%
Network: 5G
```

---

## 8. 화면 정의

---

# 8.1 Home Dashboard

## 목적

전체 네트워크 상태를 카드형 대시보드로 표시한다.

## 구성

```text
Top Section
- TetherWatch Score
- Overall Status
- Current Network

Cards
- Internet Status
- Tethering Status
- Latency Summary
- Traffic Summary
- Mobile Signal
- Connected Devices
- Recent Alerts
```

---

# 8.2 Latency 화면

## 구성

```text
- 실시간 line chart
- 대상별 current latency
- 평균/p95/p99 latency
- packet loss
- jitter
- method filter: ICMP / TCP / DNS / HTTPS
- target add/edit/delete
- failure log
```

---

# 8.3 Traffic 화면

## 구성

```text
- 실시간 download/upload chart
- session usage
- today usage
- minute/hour usage timeline
- traffic stability state
- spike/drop markers
- monthly usage
- mobile/Wi-Fi 구분
- app usage: 권한 가능 시
```

---

# 8.4 Tethering 화면

## 구성

```text
- hotspot status
- tethering type
- upstream network
- downstream interface
- tethered interface list
- session duration
- session traffic
- error log
- connected devices estimate
```

---

# 8.5 Devices 화면

## 구성

```text
- connected device list
- IP/MAC/hostname 가능 범위 표시
- traffic estimate
- confidence label
- first seen / last seen
- device alias
```

---

# 8.6 Ports & Flows 화면

## 구성

```text
- 화면을 만들지 않는다.
- VPN/packet/port 기반 분석은 제품 범위에서 제외한다.
```

---

# 8.7 Signal 화면

## 구성

```text
- carrier
- data network type
- signal level
- dBm
- roaming
- SIM status
- signal history chart
```

---

# 8.8 Settings 화면

## 구성

```text
Probe Settings
- targets
- interval
- timeout
- method

Monitoring Settings
- foreground monitoring
- battery mode
- data retention

Alert Settings
- latency threshold
- packet loss threshold
- traffic spike threshold

Privacy
- local only mode
- delete all data
```

---

## 9. Foreground Service 정의

### 9.1 목적

TetherWatch는 화면이 꺼져도 모니터링을 지속해야 하므로 Foreground Service를 사용한다.

### 9.2 서비스명

```kotlin
MonitorForegroundService
```

### 9.3 역할

```text
- NetworkCallback 유지
- ProbeScheduler 실행
- TrafficSampler 실행
- TetheringEventCallback 유지
- 알림 상태 업데이트
- 화면 표시용 세션 상태 유지
```

### 9.4 Manifest 초안

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />

<service
    android:name=".platform.foreground.MonitorForegroundService"
    android:exported="false"
    android:foregroundServiceType="connectedDevice" />
```

실제 foreground service type은 최종 기능과 Play Store 정책 검토 후 조정한다.

---

## 10. 권한 정의

### 10.1 필수 권한

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

### 10.2 기능별 권한

```xml
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" />
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" />
```

### 10.3 권한 거부 UI 정책

권한이 거부된 기능은 실패나 빈 값처럼 숨기지 않고 화면에 명확히 표시한다.

```text
Permission denied. Data was not collected.
```

권한 거부 안내 영역은 클릭 가능해야 하며, 클릭 시 해당 Android permission request 또는 앱 설정 화면으로 다시 이동한다.

### 10.4 VPN Service 제외

`VpnService`는 선언하지 않는다.

---

## 11. 로컬 데이터 정책

TetherWatch는 설치 후 바로 쓰는 로컬 모니터링 앱이다.

```text
- cloud sync 없음
- remote backup 없음
- 계정 로그인 없음
- 서버 전송 없음
- 기본 동작은 화면 표시용 세션 데이터 중심
- SharedPreferences는 사용자 설정값만 로컬 저장
- Room은 장기 히스토리 저장 요구가 생길 때만 검토
```

Room은 Android의 SQLite 래퍼 라이브러리이다. SQL 테이블을 Kotlin 코드에서 타입 안전하게 다루게 해주는 도구이며, 서버 DB나 백업 기능이 아니다.

### 11.1 향후 Room 검토 시 테이블 후보

```text
ping_targets
ping_results
status_targets
status_results
traffic_samples
network_events
tethering_events
signal_samples
device_records
alert_rules
alert_events
sessions
settings
```

### 11.2 ping_targets

```sql
CREATE TABLE ping_targets (
    id TEXT PRIMARY KEY,
    ip TEXT NOT NULL,
    interval_seconds INTEGER NOT NULL,
    enabled INTEGER NOT NULL,
    created_at INTEGER NOT NULL
);
```

### 11.3 ping_results

```sql
CREATE TABLE ping_results (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    target_id TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    success INTEGER NOT NULL,
    latency_ms INTEGER,
    error_type TEXT,
    resolved_ip TEXT,
    network_type TEXT,
    transport TEXT
);
```

### 11.4 status_targets

```sql
CREATE TABLE status_targets (
    id TEXT PRIMARY KEY,
    url TEXT NOT NULL,
    interval_seconds INTEGER NOT NULL,
    timeout_seconds INTEGER NOT NULL,
    enabled INTEGER NOT NULL,
    created_at INTEGER NOT NULL
);
```

### 11.5 traffic_samples

```sql
CREATE TABLE traffic_samples (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp INTEGER NOT NULL,
    rx_bytes_total INTEGER NOT NULL,
    tx_bytes_total INTEGER NOT NULL,
    rx_bytes_mobile INTEGER,
    tx_bytes_mobile INTEGER,
    rx_bps REAL NOT NULL,
    tx_bps REAL NOT NULL,
    network_type TEXT NOT NULL
);
```

### 11.5 tethering_events

```sql
CREATE TABLE tethering_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp INTEGER NOT NULL,
    event_type TEXT NOT NULL,
    tethering_type TEXT,
    upstream_interface TEXT,
    downstream_interfaces TEXT,
    error_code TEXT,
    description TEXT
);
```

### 11.6 device_records

```sql
CREATE TABLE device_records (
    id TEXT PRIMARY KEY,
    first_seen INTEGER NOT NULL,
    last_seen INTEGER NOT NULL,
    ip_address TEXT,
    mac_address TEXT,
    hostname TEXT,
    label TEXT,
    vendor TEXT,
    confidence TEXT
);
```

---

## 12. 데이터 보관 정책

MVP 기본값은 장기 저장 없음이다.

| 데이터 | 기본 보관 기간 |
|---|---:|
| 화면 표시용 live samples | 앱 실행 세션 동안 |
| 사용자 설정값 | 사용자가 삭제할 때까지 로컬 SharedPreferences에만 저장 |
| 장기 Ping/Status history | MVP 제외 |
| traffic raw samples | MVP 제외 |
| flow metadata | 수집하지 않음 |
| alert events | MVP 제외 |

### 12.1 데이터 삭제 기능

```text
- 로컬 설정 초기화
- 현재 세션 데이터 초기화
```

---

## 13. TetherWatch Score

### 13.1 목적

복잡한 네트워크 상태를 0~100점으로 요약한다.

### 13.2 점수 공식 초안

```text
Score =
  35% Connectivity
+ 25% Latency
+ 20% Packet Loss
+ 10% Jitter
+ 10% Signal/Traffic Anomaly
```

### 13.3 점수 구간

| Score | 상태 |
|---:|---|
| 90~100 | Excellent |
| 75~89 | Good |
| 50~74 | Unstable |
| 1~49 | Poor |
| 0 | Offline |

---

## 14. 개발 단계

Galaxy 전용 로컬 모니터링 앱을 목표로 하며, 개발 순서는 다음과 같이 잡는다.

### Phase 1. Foundation

```text
- 프로젝트 생성
- Gradle/API 36 설정
- Compose 기본 구조
- 최소 MVVM 구조
- SharedPreferences는 로컬 설정 저장에만 사용
- Room 제외
- 권한 관리 구조
- 공통 로깅/에러 처리
```

### Phase 2. Core Network

```text
- ConnectivityManager 연동
- NetworkCallback 구현
- NetworkCapabilities 파싱
- LinkProperties 파싱
- NetworkStateRepository 구현
```

### Phase 3. Ping and Status Engine

```text
- PingTarget 관리
- 실제 /system/bin/ping 실행 및 최근 콘솔 라인 유지
- StatusTarget 관리
- status.json GET 조회 및 indicator/description 파싱
- 대상별 interval scheduler
- Ping jitter 계산
- Status/Ping console 구현
```

### Phase 4. Traffic Monitor

```text
- TrafficStats sampler
- NetworkStatsManager query
- 실시간 rx/tx bps 계산
- 세션 1분 bucket 계산
- 오늘 시간대별 사용량 조회
- traffic stability 계산
- 세션 사용량 계산
- traffic chart 구현
```

### Phase 5. Tethering Monitor

```text
- TetheringManager 연동
- TetheringEventCallback 구현
- tethering interface 상태 추적
- tethering session 기록
- tethering error 표시
- tethering dashboard 구현
```

### Phase 6. Mobile Signal

```text
- TelephonyManager 연동
- SignalStrength 수집
- carrier/network type 표시
- dBm/signal level 그래프 구현
```

### Phase 7. Connected Device Estimation

```text
- tethering subnet 추정
- client IP grouping
- confidence model 구현
- device list UI 구현
```

### Phase 8. Alerts

```text
- AlertRule 구현
- Notification 구현
```

### Phase 9. Galaxy Enterprise 검토

```text
- Knox SDK 연동 가능성 검토
- Enterprise flavor 생성
- Device Owner/Profile Owner 상태 감지
- Knox NPA/Firewall/WifiPolicy 검토
```

---

## 15. 수용 기준

### 15.1 Dashboard

```text
- 앱 실행 후 2초 이내 현재 네트워크 상태 표시
- 현재 transport 구분 가능
- 인터넷 validated 여부 표시
- 전체 상태 점수 표시
```

### 15.2 Probe

```text
- 1.1.1.1, 8.8.8.8 기본 target 제공
- 사용자 target 추가/삭제 가능
- 1초/5초 interval 선택 가능
- latency graph 표시
- packet loss/jitter 계산 가능
```

### 15.3 Traffic

```text
- 현재 다운로드/업로드 속도 표시
- 1초 단위 갱신 가능
- 세션 사용량 표시
- 일간 사용량 표시
- 분/시간 단위 사용량 timeline 표시
- Stable / Bursty / Dropping / Idle 상태 표시
```

### 15.4 Tethering

```text
- 테더링 활성 여부 표시
- tethering interface 변경 이벤트 기록
- tethering session duration 표시
- tethering error 발생 시 사용자에게 표시
```

### 15.5 권한 거부 UI

```text
- 권한이 거부된 기능은 "Permission denied. Data was not collected." 표시
- 권한 안내 영역 클릭 시 권한 요청 또는 앱 설정 화면으로 이동
- 권한이 없는 데이터는 추정값처럼 표시하지 않음
```

---

## 16. QA 계획

### 16.1 대상 단말

```text
- Galaxy S Ultra 계열
- Galaxy S/S+ 계열
- Galaxy Z Fold 계열
- Galaxy Z Flip 계열
- Galaxy Tab: 선택 테스트
```

### 16.2 테스트 환경

```text
- 5G 단독
- LTE 단독
- Wi-Fi 연결
- 모바일 핫스팟 ON
- USB tethering
- Bluetooth tethering
- 화면 꺼짐
- 배터리 절약 모드
- 데이터 절약 모드
- 통신 불안정 환경
- DNS 장애 환경
```

### 16.3 성능 기준

```text
- 1초 Ping interval에서도 UI freeze 없어야 함
- 1시간 연속 모니터링 시 크래시 없어야 함
- foreground notification 정상 유지
- 장기 저장 데이터가 기본 생성되지 않음
- 배터리 사용량 측정 및 표시
```

---

## 17. 개인정보 및 보안 정책

### 17.1 기본 원칙

```text
- 모든 동작은 기본 로컬
- 계정 로그인 없음
- cloud sync 없음
- remote backup 없음
- 서버 전송 없음
- VpnService 사용 안 함
- 패킷 payload 저장 안 함
- 사용자가 로컬 설정과 현재 세션 데이터를 초기화 가능
```

---

## 18. 주요 리스크와 대응

| 리스크 | 설명 | 대응 |
|---|---|---|
| 테더링 장비별 사용량 정확도 | 일반 앱 권한으로 정확한 client별 사용량 확보 제한 | 추정값 표시, Knox 확장 검토 |
| Foreground Service 정책 | Android 14+ 서비스 타입 요구 | manifest/service type 엄격 적용 |
| 배터리 소모 | 짧은 Ping interval은 배터리 사용 증가 | 대상별 interval 설정과 foreground notification으로 상태 명확화 |
| Play Store 기기 제한 | Galaxy 전용 공개 배포는 기기 카탈로그/배포 정책 검토 필요 | 초기 APK/내부 배포 우선 |
| 제조사별 차이 | 타 제조사는 지원 범위 밖 | Galaxy 실기기 다종 테스트 |

---

## 19. 개발자 착수 체크리스트

```text
[ ] Android Studio 최신 버전 설치
[ ] Android 16/API 36 SDK 설치
[ ] Kotlin/Compose 프로젝트 생성
[ ] minSdk/targetSdk/compileSdk 36 설정
[ ] 패키지명 com.nextti.tetherwatch 확정
[ ] SharedPreferences 로컬 설정 구조
[ ] ConnectivityManager PoC
[ ] TetheringManager PoC
[ ] TrafficStats PoC
[ ] Ping/Status scheduler PoC
[ ] Foreground Service PoC
[ ] Galaxy Android 16 실기기 테스트
```

---

## 20. 참고 공식 문서

- [Android Developers - TetheringManager](https://developer.android.com/reference/android/net/TetheringManager)
- [Android Developers - TetheringManager.TetheringEventCallback](https://developer.android.com/reference/android/net/TetheringManager.TetheringEventCallback)
- [Android Developers - TetheringManager.StartTetheringCallback](https://developer.android.com/reference/android/net/TetheringManager.StartTetheringCallback)
- [Android Developers - ConnectivityManager](https://developer.android.com/reference/android/net/ConnectivityManager)
- [Android Developers - NetworkStatsManager](https://developer.android.com/reference/android/app/usage/NetworkStatsManager)
- [Android Developers - TrafficStats](https://developer.android.com/reference/android/net/TrafficStats)
- [Android Developers - Foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types)

---

## 21. 최종 개발 방향 문장

> **TetherWatch는 Samsung Galaxy Android 16/API 36 이상을 대상으로 하는 Kotlin Native 네트워크 모니터링 앱이다. `TetheringManager`를 핵심 API로 사용해 테더링 상태와 이벤트를 추적하고, `ConnectivityManager`, `TrafficStats`, `NetworkStatsManager`, `TelephonyManager`를 결합해 지연시간, 트래픽, 모바일 신호를 실시간 대시보드로 제공한다. 앱은 계정, 서버 전송, cloud sync, remote backup 없이 로컬에서 바로 사용할 수 있어야 한다.**
