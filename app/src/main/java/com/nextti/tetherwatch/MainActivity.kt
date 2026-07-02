package com.nextti.tetherwatch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )
        super.onCreate(savedInstanceState)
        startMonitorForegroundService()
        setContent {
            val viewModel: DashboardViewModel = viewModel()
            val state by viewModel.state.collectAsState()
            TetherWatchTheme(themeMode = state.appSettings.themeMode) {
                val activity = this@MainActivity
                KeepScreenOnEffect(state.appSettings.keepScreenOn)
                var showMonitoringControls by remember { mutableStateOf(false) }
                var selectedPermissionAction by remember { mutableStateOf<PermissionRecoveryAction?>(null) }
                var requestedPermission by remember { mutableStateOf<String?>(null) }
                var deniedPermissionAttempts by remember { mutableStateOf(setOf<String>()) }
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    requestedPermission?.let { permission ->
                        if (!granted) deniedPermissionAttempts = deniedPermissionAttempts + permission
                    }
                    requestedPermission = null
                    viewModel.refreshNow()
                }
                TetherDashboardScreen(
                    state = state,
                    onPingSettingsSaved = viewModel::setPingSettings,
                    onEndpointSettingsSaved = viewModel::setEndpointSettings,
                    onAppSettingsSaved = viewModel::setAppSettings,
                    onPermissionActionSelected = { selectedPermissionAction = it },
                    onBackPressed = {
                        if (state.appSettings.autoBackgroundMonitoring) {
                            activity.moveTaskToBack(true)
                        } else {
                            showMonitoringControls = true
                        }
                    }
                )
                if (showMonitoringControls) {
                    MonitoringControlsDialog(
                        onKeepInBackground = {
                            showMonitoringControls = false
                            activity.moveTaskToBack(true)
                        },
                        onStopMonitoring = {
                            showMonitoringControls = false
                            activity.stopMonitorForegroundService()
                            activity.finishAndRemoveTask()
                        },
                        onDismiss = { showMonitoringControls = false }
                    )
                }
                selectedPermissionAction?.let { action ->
                    val permissionDenied = ContextCompat.checkSelfPermission(
                        activity,
                        action.permission
                    ) != PackageManager.PERMISSION_GRANTED
                    val needsSettings = deniedPermissionAttempts.contains(action.permission) &&
                        permissionDenied &&
                        !activity.shouldShowRequestPermissionRationale(action.permission)
                    PermissionRationaleDialog(
                        action = action,
                        primaryLabel = if (needsSettings) "Open settings" else action.actionLabel,
                        onPrimaryClick = {
                            selectedPermissionAction = null
                            if (needsSettings) {
                                activity.openAppSettings()
                            } else if (action.permission == Manifest.permission.POST_NOTIFICATIONS &&
                                Build.VERSION.SDK_INT < 33
                            ) {
                                viewModel.refreshNow()
                            } else {
                                requestedPermission = action.permission
                                permissionLauncher.launch(action.permission)
                            }
                        },
                        onDismiss = { selectedPermissionAction = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun startMonitorForegroundService() {
        runCatching {
            ContextCompat.startForegroundService(
                this,
                Intent(this, MonitorForegroundService::class.java)
            )
        }.onFailure { error ->
            Log.w(TAG, "Unable to start foreground monitoring service.", error)
        }
    }

    private fun stopMonitorForegroundService() {
        startService(
            Intent(this, MonitorForegroundService::class.java).apply {
                action = MonitoringIntents.ACTION_STOP_MONITORING
            }
        )
    }

    private companion object {
        const val TAG = "TetherWatchMain"
    }
}

private fun Intent?.isMonitoringOpenIntent(): Boolean {
    return this?.action == MonitoringIntents.ACTION_OPEN_MONITORING
}

@Composable
private fun TetherWatchTheme(themeMode: AppThemeMode, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val darkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF7DDFA8),
            secondary = Color(0xFFB4CCBA),
            tertiary = Color(0xFFA4CDDD),
            background = Color(0xFF050906),
            surface = Color(0xFF101510),
            surfaceVariant = Color(0xFF3E493F),
            error = Color(0xFFFFB4AB)
        )
    } else if (Build.VERSION.SDK_INT >= 31 && !LocalInspectionMode.current) {
        dynamicLightColorScheme(context)
    } else {
        lightColorScheme(
            primary = Color(0xFF006D3D),
            secondary = Color(0xFF4D6357),
            tertiary = Color(0xFF3D6473),
            background = Color(0xFFF7FAF7),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFE1E9E1),
            error = Color(0xFFBA1A1A)
        )
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = TetherWatchTypography,
        content = content
    )
}

@Composable
private fun KeepScreenOnEffect(enabled: Boolean) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    DisposableEffect(enabled, activity) {
        if (enabled) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

private val PretendardFontFamily = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_bold, FontWeight.Bold)
)

private val BaseTypography = Typography()

private val TetherWatchTypography = Typography(
    displayLarge = BaseTypography.displayLarge.withPretendard(FontWeight.Bold),
    displayMedium = BaseTypography.displayMedium.withPretendard(FontWeight.Bold),
    displaySmall = BaseTypography.displaySmall.withPretendard(FontWeight.Bold),
    headlineLarge = BaseTypography.headlineLarge.withPretendard(FontWeight.Bold),
    headlineMedium = BaseTypography.headlineMedium.withPretendard(FontWeight.Bold),
    headlineSmall = BaseTypography.headlineSmall.withPretendard(FontWeight.Bold),
    titleLarge = BaseTypography.titleLarge.withPretendard(FontWeight.Bold),
    titleMedium = BaseTypography.titleMedium.withPretendard(FontWeight.Bold),
    titleSmall = BaseTypography.titleSmall.withPretendard(FontWeight.Bold),
    bodyLarge = BaseTypography.bodyLarge.withPretendard(FontWeight.Normal),
    bodyMedium = BaseTypography.bodyMedium.withPretendard(FontWeight.Normal),
    bodySmall = BaseTypography.bodySmall.withPretendard(FontWeight.Normal),
    labelLarge = BaseTypography.labelLarge.withPretendard(FontWeight.Bold),
    labelMedium = BaseTypography.labelMedium.withPretendard(FontWeight.Normal),
    labelSmall = BaseTypography.labelSmall.withPretendard(FontWeight.Normal)
)

private fun TextStyle.withPretendard(defaultWeight: FontWeight): TextStyle {
    return copy(
        fontFamily = PretendardFontFamily,
        fontWeight = fontWeight ?: defaultWeight
    )
}

@Composable
private fun TopNavigationBar(
    showingSettings: Boolean,
    onSettingsClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 0.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showingSettings) {
                TextButton(onClick = onBackClick) {
                    Text("< Back")
                }
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("Tether", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Watch", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Normal)
                }
            }
        }
        if (showingSettings) {
            Spacer(Modifier.width(48.dp))
        } else {
            IconButton(onClick = onSettingsClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings_24),
                    contentDescription = "Settings",
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun TetherDashboardScreen(
    state: DashboardState,
    onPingSettingsSaved: (ProbeSettings) -> Unit,
    onEndpointSettingsSaved: (EndpointSettings) -> Unit,
    onAppSettingsSaved: (AppSettings) -> Unit,
    onPermissionActionSelected: (PermissionRecoveryAction) -> Unit,
    onBackPressed: () -> Unit
) {
    val visual = DashboardVisualModelBuilder.build(state)
    val scrollState = rememberScrollState()
    var showingSettings by remember { mutableStateOf(false) }
    var saveSettingsAction by remember { mutableStateOf<(() -> Boolean)?>(null) }

    BackHandler {
        if (showingSettings) {
            if (tryCloseSettings(saveSettingsAction)) {
                showingSettings = false
            }
        } else {
            onBackPressed()
        }
    }
    LaunchedEffect(showingSettings) {
        if (!showingSettings) saveSettingsAction = null
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedAmbientBackground(Modifier.fillMaxSize())
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TopNavigationBar(
                    showingSettings = showingSettings,
                    onSettingsClick = { showingSettings = true },
                    onBackClick = {
                        if (tryCloseSettings(saveSettingsAction)) {
                            showingSettings = false
                        }
                    }
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showingSettings) {
                        SettingsScreen(
                            probeSettings = state.probeSettings,
                            endpointSettings = state.endpointSettings,
                            appSettings = state.appSettings,
                            onPingSettingsSaved = onPingSettingsSaved,
                            onEndpointSettingsSaved = onEndpointSettingsSaved,
                            onAppSettingsSaved = onAppSettingsSaved,
                            onSaveActionChanged = { action -> saveSettingsAction = action }
                        )
                    } else {
                        TopStatusHeader(visual)
                        HeroPanel(visual.hero, state.score, state.traffic)
                        StatusTileGrid(visual.tiles)
                        ProbePanel(state = state)
                        EndpointPanel(state = state)
                        AlertStrip(visual.alerts, onPermissionActionSelected)
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun AnimatedAmbientBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "ambientBackground")
    val slowSweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7_500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "slowSweep"
    )
    val counterSweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10_500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counterSweep"
    )
    val highlightSweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12_500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "highlightSweep"
    )
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val secondary = MaterialTheme.colorScheme.secondary

    Canvas(modifier = modifier) {
        val longestSide = max(size.width, size.height)
        drawAmbientSweep(
            progress = slowSweep,
            radius = longestSide * 1.06f,
            xAmplitude = 0.46f,
            yAmplitude = 0.28f,
            phase = 0.08f,
            colors = listOf(
                primary.copy(alpha = 0.115f),
                primary.copy(alpha = 0.044f),
                Color.Transparent
            )
        )
        drawAmbientSweep(
            progress = counterSweep,
            radius = longestSide * 0.94f,
            xAmplitude = 0.38f,
            yAmplitude = 0.42f,
            phase = 0.54f,
            colors = listOf(
                tertiary.copy(alpha = 0.095f),
                secondary.copy(alpha = 0.034f),
                Color.Transparent
            )
        )
        drawAmbientSweep(
            progress = highlightSweep,
            radius = longestSide * 0.86f,
            xAmplitude = 0.52f,
            yAmplitude = 0.34f,
            phase = 0.72f,
            colors = listOf(
                secondary.copy(alpha = 0.075f),
                tertiary.copy(alpha = 0.028f),
                Color.Transparent
            )
        )
        drawAmbientLinearSweep(
            progress = highlightSweep,
            colors = listOf(
                Color.Transparent,
                primary.copy(alpha = 0.036f),
                tertiary.copy(alpha = 0.040f),
                Color.Transparent
            )
        )
    }
}

private fun DrawScope.drawAmbientLinearSweep(progress: Float, colors: List<Color>) {
    val travel = size.width + size.height
    val shift = (progress * travel * 2f) - travel
    drawRect(
        brush = Brush.linearGradient(
            colors = colors,
            start = Offset(shift - size.width * 0.35f, size.height),
            end = Offset(shift + size.width * 0.85f, 0f)
        )
    )
}

private fun DrawScope.drawAmbientSweep(
    progress: Float,
    radius: Float,
    xAmplitude: Float,
    yAmplitude: Float,
    phase: Float,
    colors: List<Color>
) {
    val angle = (progress + phase) * 2f * PI.toFloat()
    val center = Offset(
        x = size.width * (0.5f + cos(angle) * xAmplitude),
        y = size.height * (0.5f + sin(angle * 0.78f) * yAmplitude)
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = colors,
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

private fun tryCloseSettings(saveSettingsAction: (() -> Boolean)?): Boolean {
    return saveSettingsAction?.invoke() ?: true
}

@Composable
private fun MonitoringControlsDialog(
    onKeepInBackground: () -> Unit,
    onStopMonitoring: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.54f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 42.dp, height = 4.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp))
                )
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        "Background monitoring",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "TetherWatch can keep traffic and ping monitoring visible in the notification while the app stays in the background.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onKeepInBackground,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Keep background monitoring")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = onStopMonitoring,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Stop and close")
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRationaleDialog(
    action: PermissionRecoveryAction,
    primaryLabel: String,
    onPrimaryClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(action.title) },
        text = {
            Text(
                action.explanation,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onPrimaryClick) {
                Text(primaryLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not now")
            }
        }
    )
}

@Composable
private fun TopStatusHeader(visual: DashboardVisualModel) {
    val tetheringText = listOf(
        visual.header.tetheringLabel,
        visual.header.clientCountLabel
    ).filter { it.isNotBlank() }.joinToString(" ")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${visual.header.networkTypeLabel} · ${visual.header.simProviderLabel} · ${visual.header.countryCode}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(
                            if (visual.header.tetheringActive) Color(0xFF1F7A5C) else MaterialTheme.colorScheme.outline,
                            CircleShape
                        )
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    tetheringText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun HeroPanel(hero: HeroVisualModel, score: DashboardScore, traffic: TrafficDashboard) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                HeroStatusText(hero, modifier = Modifier.weight(1f))
                TrafficRateLegend(hero, modifier = Modifier.weight(1f))
                TrafficUsageLegend(traffic, modifier = Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    ScoreRing(score.value, score.health, modifier = Modifier.size(98.dp))
                }
                DualTrafficSparkline(
                    samples = traffic.samples,
                    modifier = Modifier
                        .weight(2f)
                        .height(108.dp)
                )
            }
            ScoreBreakdownLampStrip(score.breakdown)
        }
    }
}

@Composable
private fun HeroStatusText(hero: HeroVisualModel, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            hero.healthLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            hero.activeSimLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TrafficRateLegend(hero: HeroVisualModel, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        LegendMetric("DN", hero.downloadValue, Color(0xFF1F7A5C))
        LegendMetric("UP", hero.uploadValue, Color(0xFF2F6DF6))
    }
}

@Composable
private fun TrafficUsageLegend(traffic: TrafficDashboard, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        TrafficUsageRow("60s", traffic.recentTotal)
        TrafficUsageRow("Session", traffic.sessionTotal)
    }
}

@Composable
private fun ScoreBreakdownLampStrip(items: List<ScoreBreakdown>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            ScoreBreakdownLampItem(item)
        }
    }
}

@Composable
private fun ScoreBreakdownLampItem(item: ScoreBreakdown) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ScoreBreakdownMiniDonut(item)
        Text(
            item.displayLabel(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
            maxLines = 1
        )
    }
}

@Composable
private fun ScoreBreakdownMiniDonut(item: ScoreBreakdown) {
    val color = item.scoreBreakdownLampColor()
    val progress = item.scoreBreakdownFraction()
    Canvas(modifier = Modifier.size(13.dp)) {
        val stroke = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
        val inset = 2.dp.toPx()
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        drawArc(
            color = color.copy(alpha = 0.26f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = stroke
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = stroke
        )
    }
}

@Composable
private fun TrafficUsageRow(label: String, value: String) {
    HeroMetricRow(label, value, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun HeroMetricRow(label: String, value: String, modifier: Modifier = Modifier) {
    Text(
        "$label $value",
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.70f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun LegendMetric(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
        Text(
            "$label $value",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f),
            maxLines = 1
        )
    }
}

@Composable
private fun ScoreRing(score: Int, health: DashboardHealth, modifier: Modifier = Modifier.size(74.dp)) {
    val ringColor = health.tone().accent
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = Color.White.copy(alpha = 0.45f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(5.dp.toPx(), 5.dp.toPx()),
                size = Size(size.width - 10.dp.toPx(), size.height - 10.dp.toPx()),
                style = stroke
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * (score.coerceIn(0, 100) / 100f),
                useCenter = false,
                topLeft = Offset(5.dp.toPx(), 5.dp.toPx()),
                size = Size(size.width - 10.dp.toPx(), size.height - 10.dp.toPx()),
                style = stroke
            )
        }
        Text("$score", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatusTileGrid(tiles: List<DashboardTileVisualModel>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tiles.forEach { tile ->
            StatusTile(tile)
        }
    }
}

@Composable
private fun StatusTile(tile: DashboardTileVisualModel) {
    val tone = tile.tone.colors()
    Card(
        modifier = Modifier
            .fillMaxWidth(if (tile.icon == DashboardTileIcon.DNS) 1f else 0.48f)
            .heightIn(min = if (tile.icon == DashboardTileIcon.DNS) 52.dp else 82.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = tone.container)
    ) {
        if (tile.icon == DashboardTileIcon.DNS) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(tone.accent, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(tile.icon.glyph(), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    tile.value,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(tone.accent, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(tile.icon.glyph(), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(7.dp))
                    Text(tile.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    tile.value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (tile.detail.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            tile.detail,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        tile.meterFraction?.let { MiniMeter(it, tone.accent) }
                    }
                }
                if (tile.downSeries.size >= 2 || tile.upSeries.size >= 2) {
                    DualLineChart(
                        downValues = tile.downSeries,
                        upValues = tile.upSeries,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                        downColor = Color(0xFF1F7A5C),
                        upColor = Color(0xFF2F6DF6)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniMeter(fraction: Float, color: Color) {
    Box(
        modifier = Modifier
            .size(width = 36.dp, height = 6.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0.05f, 1f))
                .height(6.dp)
                .background(color, RoundedCornerShape(8.dp))
        )
    }
}

@Composable
private fun ProbePanel(state: DashboardState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ping", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Jitter ${state.probe.summary.jitterMs ?: 0} ms",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            PingConsolePanel(
                lines = state.probe.consoleLines,
                settings = state.probeSettings
            )
        }
    }
}

@Composable
private fun EndpointPanel(state: DashboardState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Status", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "status.json",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            EndpointConsolePanel(
                lines = state.endpoint.consoleLines,
                state = state
            )
        }
    }
}

@Composable
private fun SettingsScreen(
    probeSettings: ProbeSettings,
    endpointSettings: EndpointSettings,
    appSettings: AppSettings,
    onPingSettingsSaved: (ProbeSettings) -> Unit,
    onEndpointSettingsSaved: (EndpointSettings) -> Unit,
    onAppSettingsSaved: (AppSettings) -> Unit,
    onSaveActionChanged: (((() -> Boolean)?) -> Unit)
) {
    var rows by remember(probeSettings) {
        mutableStateOf(probeSettings.targetSettings.map { PingTargetEditorRow(it.target, it.intervalSeconds.toString()) })
    }
    var endpointRows by remember(endpointSettings) {
        mutableStateOf(endpointSettings.targetSettings.map { EndpointTargetEditorRow(it.url, it.intervalSeconds.toString()) })
    }
    var fontSizeSp by remember(probeSettings) { mutableStateOf(probeSettings.consoleFontSizeSp) }
    var lineCount by remember(probeSettings) { mutableStateOf(probeSettings.consoleLineCount) }
    var slowThresholdMs by remember(probeSettings) { mutableStateOf(probeSettings.slowResponseThresholdMs) }
    var endpointTimeoutMs by remember(endpointSettings) { mutableStateOf(endpointSettings.timeoutMs) }
    var endpointRedirectLimit by remember(endpointSettings) { mutableStateOf(endpointSettings.redirectLimit) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingDeleteIndex by remember { mutableStateOf<Int?>(null) }
    var pendingEndpointDeleteIndex by remember { mutableStateOf<Int?>(null) }
    var pendingPingResetConfirm by remember { mutableStateOf(false) }
    var pendingEndpointResetConfirm by remember { mutableStateOf(false) }
    val savePingSettings = remember(
        rows,
        endpointRows,
        fontSizeSp,
        lineCount,
        slowThresholdMs,
        endpointTimeoutMs,
        endpointRedirectLimit
    ) {
        fun(): Boolean {
            val result = PingSettingsValidator.validate(
                targetInputs = rows.map { row -> PingTargetInput(row.ip, row.intervalSeconds) },
                consoleFontSizeSp = fontSizeSp,
                consoleLineCount = lineCount,
                slowResponseThresholdMs = slowThresholdMs
            )
            if (result.errorMessage != null) {
                errorMessage = result.errorMessage
                return false
            } else {
                val endpointResult = EndpointSettingsValidator.validate(
                    targetInputs = endpointRows.map { row -> EndpointTargetInput(row.url, row.intervalSeconds) },
                    timeoutMs = endpointTimeoutMs,
                    redirectLimit = endpointRedirectLimit
                )
                if (endpointResult.errorMessage != null) {
                    errorMessage = endpointResult.errorMessage
                    return false
                } else {
                    result.settings?.let(onPingSettingsSaved)
                    endpointResult.settings?.let(onEndpointSettingsSaved)
                    errorMessage = null
                    return true
                }
            }
        }
    }
    SideEffect {
        onSaveActionChanged(savePingSettings)
    }
    DisposableEffect(Unit) {
        onDispose { onSaveActionChanged(null) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        errorMessage?.let { message ->
            Text(
                message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
        SettingsGroup(title = "Ping targets") {
            rows.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = row.ip,
                        onValueChange = { value ->
                            errorMessage = null
                            rows = rows.updatePingTargetRow(index) { copy(ip = value.trim()) }
                        },
                        modifier = Modifier.weight(1.5f),
                        label = { Text("IP") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = row.intervalSeconds,
                        onValueChange = { value ->
                            errorMessage = null
                            rows = rows.updatePingTargetRow(index) { copy(intervalSeconds = value.take(3)) }
                        },
                        modifier = Modifier.weight(0.62f),
                        label = { Text("Interval", maxLines = 1, overflow = TextOverflow.Clip) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    IconButton(
                        onClick = {
                            if (rows.size > 1) pendingDeleteIndex = index
                        },
                        enabled = rows.size > 1,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete_24),
                            contentDescription = "Delete ping target",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            TargetActionRow(
                addLabel = "Add target",
                onAddClick = {
                    if (rows.size < 6) {
                        rows = rows + PingTargetEditorRow("", ProbeIntervalOptions.DEFAULT_SECONDS.toString())
                    }
                },
                onResetClick = { pendingPingResetConfirm = true }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ping slow response threshold", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text("${slowThresholdMs}ms", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = slowThresholdMs.toFloat(),
                onValueChange = { value -> slowThresholdMs = PingSlowResponseOptions.normalize(value.roundToInt()) },
                valueRange = PingSlowResponseOptions.MIN_MS.toFloat()..PingSlowResponseOptions.MAX_MS.toFloat(),
                steps = (PingSlowResponseOptions.MAX_MS - PingSlowResponseOptions.MIN_MS) /
                    PingSlowResponseOptions.STEP_MS - 1
            )
        }

        SettingsGroup(title = "Status targets") {
            endpointRows.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = row.url,
                        onValueChange = { value ->
                            errorMessage = null
                            endpointRows = endpointRows.updateEndpointTargetRow(index) { copy(url = value.trim()) }
                        },
                        modifier = Modifier.weight(1.65f),
                        label = { Text("URL") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = row.intervalSeconds,
                        onValueChange = { value ->
                            errorMessage = null
                            endpointRows = endpointRows.updateEndpointTargetRow(index) { copy(intervalSeconds = value.take(3)) }
                        },
                        modifier = Modifier.weight(0.58f),
                        label = { Text("Interval", maxLines = 1, overflow = TextOverflow.Clip) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    IconButton(
                        onClick = {
                            if (endpointRows.size > 1) pendingEndpointDeleteIndex = index
                        },
                        enabled = endpointRows.size > 1,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete_24),
                            contentDescription = "Delete status target",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            TargetActionRow(
                addLabel = "Add status",
                onAddClick = {
                    if (endpointRows.size < 6) {
                        endpointRows = endpointRows + EndpointTargetEditorRow("", "30")
                    }
                },
                onResetClick = { pendingEndpointResetConfirm = true }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Timeout", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text("${endpointTimeoutMs}ms", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = endpointTimeoutMs.toFloat(),
                onValueChange = { value -> endpointTimeoutMs = EndpointTimeoutOptions.normalize(value.roundToInt()) },
                valueRange = EndpointTimeoutOptions.MIN_MS.toFloat()..EndpointTimeoutOptions.MAX_MS.toFloat(),
                steps = (EndpointTimeoutOptions.MAX_MS - EndpointTimeoutOptions.MIN_MS) /
                    EndpointTimeoutOptions.STEP_MS - 1
            )
        }

        SettingsGroup(title = "Console display") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Font size", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text("${fontSizeSp}sp", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = fontSizeSp.toFloat(),
                onValueChange = { value -> fontSizeSp = PingConsoleFontOptions.normalize(value.roundToInt()) },
                valueRange = PingConsoleFontOptions.MIN_SP.toFloat()..PingConsoleFontOptions.MAX_SP.toFloat(),
                steps = PingConsoleFontOptions.MAX_SP - PingConsoleFontOptions.MIN_SP - 1
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Console lines", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text("$lineCount", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = lineCount.toFloat(),
                onValueChange = { value -> lineCount = PingConsoleLineOptions.normalize(value.roundToInt()) },
                valueRange = PingConsoleLineOptions.MIN_COUNT.toFloat()..PingConsoleLineOptions.MAX_COUNT.toFloat(),
                steps = (PingConsoleLineOptions.MAX_COUNT - PingConsoleLineOptions.MIN_COUNT) /
                    PingConsoleLineOptions.STEP_COUNT - 1
            )
        }

        SettingsGroup(title = "App behavior") {
            SettingsSwitchRow(
                title = "Keep screen on",
                description = "Prevent the screen from sleeping while TetherWatch is open.",
                checked = appSettings.keepScreenOn,
                onCheckedChange = { checked -> onAppSettingsSaved(appSettings.copy(keepScreenOn = checked)) }
            )
            SettingsSwitchRow(
                title = "Auto background monitoring",
                description = "Back sends TetherWatch to the background without asking.",
                checked = appSettings.autoBackgroundMonitoring,
                onCheckedChange = { checked -> onAppSettingsSaved(appSettings.copy(autoBackgroundMonitoring = checked)) }
            )
        }

        SettingsGroup(title = "Theme") {
            ThemeModeRow("Use device theme", AppThemeMode.SYSTEM, appSettings, onAppSettingsSaved)
            ThemeModeRow("Light", AppThemeMode.LIGHT, appSettings, onAppSettingsSaved)
            ThemeModeRow("Black", AppThemeMode.DARK, appSettings, onAppSettingsSaved)
        }
    }
    pendingDeleteIndex?.let { index ->
        AlertDialog(
            onDismissRequest = { pendingDeleteIndex = null },
            title = { Text("Delete ping target?") },
            text = { Text("This IP and interval will be removed from Ping targets.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        errorMessage = null
                        rows = rows.filterIndexed { rowIndex, _ -> rowIndex != index }
                        pendingDeleteIndex = null
                    }
                ) {
                    Text("Confirm", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteIndex = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    pendingEndpointDeleteIndex?.let { index ->
        AlertDialog(
            onDismissRequest = { pendingEndpointDeleteIndex = null },
            title = { Text("Delete status target?") },
            text = { Text("This status URL and interval will be removed from Status targets.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        errorMessage = null
                        endpointRows = endpointRows.filterIndexed { rowIndex, _ -> rowIndex != index }
                        pendingEndpointDeleteIndex = null
                    }
                ) {
                    Text("Confirm", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingEndpointDeleteIndex = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    if (pendingPingResetConfirm) {
        AlertDialog(
            onDismissRequest = { pendingPingResetConfirm = false },
            title = { Text("Reset ping targets?") },
            text = { Text("This will restore Ping targets to the default values.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        errorMessage = null
                        rows = PingTargetOptions.DEFAULT_TARGET_SETTINGS.map {
                            PingTargetEditorRow(it.target, it.intervalSeconds.toString())
                        }
                        pendingPingResetConfirm = false
                    }
                ) {
                    Text("Confirm", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPingResetConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    if (pendingEndpointResetConfirm) {
        AlertDialog(
            onDismissRequest = { pendingEndpointResetConfirm = false },
            title = { Text("Reset status targets?") },
            text = { Text("This will restore Status targets to the default values.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        errorMessage = null
                        endpointRows = EndpointTargetOptions.DEFAULT_TARGET_SETTINGS.map {
                            EndpointTargetEditorRow(it.url, it.intervalSeconds.toString())
                        }
                        pendingEndpointResetConfirm = false
                    }
                ) {
                    Text("Confirm", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingEndpointResetConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TargetActionRow(
    addLabel: String,
    onAddClick: () -> Unit,
    onResetClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onAddClick) {
            Text(addLabel)
        }
        TextButton(onClick = onResetClick) {
            Text("Reset")
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ThemeModeRow(
    label: String,
    mode: AppThemeMode,
    settings: AppSettings,
    onAppSettingsSaved: (AppSettings) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAppSettingsSaved(settings.copy(themeMode = mode)) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        RadioButton(
            selected = settings.themeMode == mode,
            onClick = { onAppSettingsSaved(settings.copy(themeMode = mode)) }
        )
    }
}

@Composable
private fun PingConsolePanel(lines: List<String>, settings: ProbeSettings) {
    val visibleLines = lines.takeLast(settings.consoleLineCount)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(consolePanelHeight(settings.consoleFontSizeSp, settings.consoleLineCount)),
        color = Color(0xFF050906),
        contentColor = Color(0xFF5DFF89),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (visibleLines.isEmpty()) {
                Text(
                    "waiting for reply...",
                    fontFamily = FontFamily.Monospace,
                    fontSize = settings.consoleFontSizeSp.sp,
                    lineHeight = (settings.consoleFontSizeSp + 1).sp,
                    color = Color(0xFF5DFF89)
                )
            } else {
                visibleLines.forEach { line ->
                    Text(
                        line,
                        fontFamily = FontFamily.Monospace,
                        fontSize = settings.consoleFontSizeSp.sp,
                        lineHeight = (settings.consoleFontSizeSp + 1).sp,
                        color = pingConsoleLineColor(line, settings.slowResponseThresholdMs),
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        softWrap = false
                    )
                }
            }
        }
    }
}

private fun pingConsoleLineColor(line: String, slowResponseThresholdMs: Int): Color {
    return when (PingConsoleLineClassifier.tone(line, slowResponseThresholdMs)) {
        PingConsoleLineTone.NORMAL -> Color(0xFF5DFF89)
        PingConsoleLineTone.SLOW -> Color(0xFFFFD95A)
        PingConsoleLineTone.ERROR -> Color(0xFFFF6B6B)
    }
}

@Composable
private fun EndpointConsolePanel(lines: List<String>, state: DashboardState) {
    val visibleLines = lines.takeLast(state.probeSettings.consoleLineCount)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(consolePanelHeight(state.probeSettings.consoleFontSizeSp, state.probeSettings.consoleLineCount)),
        color = Color(0xFF050906),
        contentColor = Color(0xFF5DFF89),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (visibleLines.isEmpty()) {
                Text(
                    "waiting for status...",
                    fontFamily = FontFamily.Monospace,
                    fontSize = state.probeSettings.consoleFontSizeSp.sp,
                    lineHeight = (state.probeSettings.consoleFontSizeSp + 1).sp,
                    color = Color(0xFF5DFF89)
                )
            } else {
                visibleLines.forEach { line ->
                    Text(
                        line,
                        fontFamily = FontFamily.Monospace,
                        fontSize = state.probeSettings.consoleFontSizeSp.sp,
                        lineHeight = (state.probeSettings.consoleFontSizeSp + 1).sp,
                        color = endpointConsoleLineColor(line),
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        softWrap = false
                    )
                }
            }
        }
    }
}

private fun consolePanelHeight(fontSizeSp: Int, lineCount: Int): Dp {
    val normalizedFontSize = PingConsoleFontOptions.normalize(fontSizeSp)
    val normalizedLineCount = PingConsoleLineOptions.normalize(lineCount)
    return ((normalizedFontSize + 1) * normalizedLineCount + 18).dp
}

private fun endpointConsoleLineColor(line: String): Color {
    return when (EndpointConsoleLineClassifier.tone(line)) {
        ConsoleLineTone.NORMAL -> Color(0xFF5DFF89)
        ConsoleLineTone.SLOW -> Color(0xFFFFD95A)
        ConsoleLineTone.ERROR -> Color(0xFFFF6B6B)
    }
}

@Composable
private fun ProbeTargetStrip(targets: List<ProbeTargetState>) {
    if (targets.isEmpty()) {
        Text(
            "Collecting probe responses",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        targets.forEachIndexed { index, target ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(probeSeriesColor(index), CircleShape)
                    )
                    Text(target.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(target.status, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

private data class PingTargetEditorRow(
    val ip: String,
    val intervalSeconds: String
)

private data class EndpointTargetEditorRow(
    val url: String,
    val intervalSeconds: String
)

private fun List<PingTargetEditorRow>.updatePingTargetRow(
    index: Int,
    update: PingTargetEditorRow.() -> PingTargetEditorRow
): List<PingTargetEditorRow> {
    return mapIndexed { rowIndex, row -> if (rowIndex == index) row.update() else row }
}

private fun List<EndpointTargetEditorRow>.updateEndpointTargetRow(
    index: Int,
    update: EndpointTargetEditorRow.() -> EndpointTargetEditorRow
): List<EndpointTargetEditorRow> {
    return mapIndexed { rowIndex, row -> if (rowIndex == index) row.update() else row }
}

@Composable
private fun AlertStrip(
    alerts: List<DashboardAlertVisualModel>,
    onPermissionActionSelected: (PermissionRecoveryAction) -> Unit
) {
    if (alerts.isEmpty()) return
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        alerts.take(3).forEach { alert ->
            val tone = alert.tone.colors()
            Surface(
                modifier = Modifier.clickable(enabled = alert.action != null) {
                    alert.action?.let(onPermissionActionSelected)
                },
                color = tone.container,
                contentColor = tone.accent,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        alert.message,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (alert.action != null) {
                        Text(
                            "Review",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

private fun ComponentActivity.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
    )
}

@Composable
private fun DualTrafficSparkline(samples: List<TrafficSamplePoint>, modifier: Modifier = Modifier) {
    if (samples.size < 2) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.38f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Collecting", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.68f))
        }
        return
    }
    TrafficFitLineChart(TrafficChartModelBuilder.build(samples), modifier)
}

@Composable
private fun TrafficFitLineChart(chartModel: TrafficChartModel, modifier: Modifier) {
    val maxValue = chartModel.yAxisLabels.maxOfOrNull { it.value }?.takeIf { it > 0.0 } ?: 1.0
    val labelColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.70f)
    val gridColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f)
    Canvas(modifier = modifier) {
        val leftPadding = 36.dp.toPx()
        val rightPadding = 4.dp.toPx()
        val topPadding = 10.dp.toPx()
        val bottomPadding = 14.dp.toPx()
        val plotLeft = leftPadding
        val plotTop = topPadding
        val plotWidth = size.width - leftPadding - rightPadding
        val plotHeight = size.height - topPadding - bottomPadding
        val maxX = chartModel.xAxisLabels.maxOfOrNull { it.value }?.coerceAtLeast(1.0) ?: 1.0
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor.toArgb()
            textSize = 8.dp.toPx()
        }

        drawContext.canvas.nativeCanvas.drawText(
            chartModel.unitLabel,
            plotLeft,
            8.dp.toPx(),
            textPaint
        )
        val activityLabel = if (chartModel.dropCount > 0) {
            "${chartModel.activityLabel} · drops ${chartModel.dropCount}"
        } else {
            chartModel.activityLabel
        }
        val flowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (chartModel.dropCount > 0) Color(0xFFD94A3A).toArgb() else labelColor.toArgb()
            textSize = 8.dp.toPx()
        }
        val flowLabelWidth = flowPaint.measureText(activityLabel)
        drawContext.canvas.nativeCanvas.drawText(
            activityLabel,
            (plotLeft + plotWidth - flowLabelWidth).coerceAtLeast(plotLeft),
            8.dp.toPx(),
            flowPaint
        )
        chartModel.yAxisLabels.forEach { label ->
            val y = plotTop + plotHeight * (1f - (label.value / maxValue).toFloat())
            drawLine(
                color = gridColor,
                start = Offset(plotLeft, y),
                end = Offset(plotLeft + plotWidth, y),
                strokeWidth = 0.75.dp.toPx()
            )
            drawContext.canvas.nativeCanvas.drawText(
                label.label,
                0f,
                y + 3.dp.toPx(),
                textPaint
            )
        }
        chartModel.xAxisLabels.forEach { label ->
            val x = plotLeft + ((label.value / maxX).toFloat() * plotWidth)
            drawLine(
                color = gridColor.copy(alpha = 0.65f),
                start = Offset(x, plotTop),
                end = Offset(x, plotTop + plotHeight),
                strokeWidth = 0.75.dp.toPx()
            )
            val textWidth = textPaint.measureText(label.label)
            val textX = if (label.label == "now") x - textWidth else x
            drawContext.canvas.nativeCanvas.drawText(
                label.label,
                textX.coerceIn(0f, size.width - textWidth),
                size.height,
                textPaint
            )
        }

        drawTrafficSmoothedSeries(chartModel.smoothedDownSeries, maxX, maxValue, plotLeft, plotTop, plotWidth, plotHeight, Color(0xFF1F7A5C))
        drawTrafficSmoothedSeries(chartModel.smoothedUpSeries, maxX, maxValue, plotLeft, plotTop, plotWidth, plotHeight, Color(0xFF2F6DF6))
        drawTrafficRateSeries(chartModel.downSeries, maxX, maxValue, plotLeft, plotTop, plotWidth, plotHeight, Color(0xFF1F7A5C))
        drawTrafficRateSeries(chartModel.upSeries, maxX, maxValue, plotLeft, plotTop, plotWidth, plotHeight, Color(0xFF2F6DF6))
        drawTrafficDropMarkers(chartModel.dropMarkers, maxX, plotLeft, plotTop, plotWidth, plotHeight)
    }
}

private fun DrawScope.drawTrafficSmoothedSeries(
    series: TrafficRateSeries,
    maxX: Double,
    maxValue: Double,
    plotLeft: Float,
    plotTop: Float,
    plotWidth: Float,
    plotHeight: Float,
    color: Color
) {
    val points = series.x.zip(series.y)
    if (points.size < 2) return
    points.zipWithNext().forEach { (start, end) ->
        drawLine(
            color = color.copy(alpha = 0.82f),
            start = Offset(
                x = plotLeft + ((start.first / maxX).toFloat() * plotWidth),
                y = plotTop + plotHeight * (1f - (start.second / maxValue).toFloat().coerceIn(0f, 1f))
            ),
            end = Offset(
                x = plotLeft + ((end.first / maxX).toFloat() * plotWidth),
                y = plotTop + plotHeight * (1f - (end.second / maxValue).toFloat().coerceIn(0f, 1f))
            ),
            strokeWidth = 2.4.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawTrafficRateSeries(
    series: TrafficRateSeries,
    maxX: Double,
    maxValue: Double,
    plotLeft: Float,
    plotTop: Float,
    plotWidth: Float,
    plotHeight: Float,
    color: Color
) {
    val points = series.x.zip(series.y)
    if (points.size < 2) return
    points.zipWithNext().forEach { (start, end) ->
        drawLine(
            color = color.copy(alpha = 0.46f),
            start = Offset(
                x = plotLeft + ((start.first / maxX).toFloat() * plotWidth),
                y = plotTop + plotHeight * (1f - (start.second / maxValue).toFloat().coerceIn(0f, 1f))
            ),
            end = Offset(
                x = plotLeft + ((end.first / maxX).toFloat() * plotWidth),
                y = plotTop + plotHeight * (1f - (end.second / maxValue).toFloat().coerceIn(0f, 1f))
            ),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawTrafficDropMarkers(
    markers: List<TrafficDropMarker>,
    maxX: Double,
    plotLeft: Float,
    plotTop: Float,
    plotWidth: Float,
    plotHeight: Float
) {
    markers.forEach { marker ->
        val x = plotLeft + ((marker.x / maxX).toFloat().coerceIn(0f, 1f) * plotWidth)
        drawLine(
            color = Color(0xFFD94A3A).copy(alpha = 0.86f),
            start = Offset(x, plotTop + plotHeight * 0.18f),
            end = Offset(x, plotTop + plotHeight * 0.86f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun TrafficSparkline(buckets: List<TrafficBucket>) {
    val recent = buckets.takeLast(8)
    if (recent.isEmpty()) {
        EmptyChart("Collecting")
        return
    }
    SimpleLineChart(
        values = recent.map { (it.rxBytes + it.txBytes).toDouble() },
        color = Color(0xFF1F7A5C),
        height = 74.dp
    )
}

@Composable
private fun ProbeLatencyChart(points: List<ProbePoint>, chartHeight: Dp = 74.dp) {
    val chartModel = ProbeChartModelBuilder.build(points)
    if (chartModel.series.isEmpty()) {
        EmptyChart(if (points.any { !it.success }) "Failing" else "Collecting")
    } else {
        ProbeFitLineChart(chartModel, chartHeight)
    }
}

@Composable
private fun ProbeFitLineChart(chartModel: ProbeChartModel, chartHeight: Dp) {
    val series = chartModel.series
    val allValues = chartModel.yAxisLabels.map { it.value }.ifEmpty { series.flatMap { it.y } }
    val minValue = allValues.minOrNull() ?: 0.0
    val maxValue = allValues.maxOrNull() ?: minValue
    val range = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.74f)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(chartHeight)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f), RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        val leftPadding = 46.dp.toPx()
        val rightPadding = 8.dp.toPx()
        val topPadding = 10.dp.toPx()
        val bottomPadding = 22.dp.toPx()
        val plotLeft = leftPadding
        val plotTop = topPadding
        val plotWidth = size.width - leftPadding - rightPadding
        val plotHeight = size.height - topPadding - bottomPadding
        val maxX = chartModel.xAxisLabels.maxOfOrNull { it.value }?.coerceAtLeast(1.0)
            ?: series.flatMap { it.x }.maxOrNull()?.coerceAtLeast(1.0)
            ?: 1.0
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor.toArgb()
            textSize = 10.dp.toPx()
        }

        chartModel.yAxisLabels.forEach { label ->
            val y = plotTop + plotHeight * (1f - ((label.value - minValue) / range).toFloat())
            drawLine(
                color = gridColor,
                start = Offset(plotLeft, y),
                end = Offset(plotLeft + plotWidth, y),
                strokeWidth = 1.dp.toPx()
            )
            drawContext.canvas.nativeCanvas.drawText(
                label.label,
                0f,
                y + 4.dp.toPx(),
                textPaint
            )
        }
        chartModel.xAxisLabels.forEach { label ->
            val x = plotLeft + ((label.value / maxX).toFloat() * plotWidth)
            drawLine(
                color = gridColor.copy(alpha = 0.45f),
                start = Offset(x, plotTop),
                end = Offset(x, plotTop + plotHeight),
                strokeWidth = 1.dp.toPx()
            )
            val textWidth = textPaint.measureText(label.label)
            val textX = when (label.label) {
                "now" -> x - textWidth
                else -> x
            }.coerceIn(0f, size.width - textWidth)
            drawContext.canvas.nativeCanvas.drawText(
                label.label,
                textX,
                size.height - 2.dp.toPx(),
                textPaint
            )
        }
        series.forEachIndexed { index, probeSeries ->
            val points = probeSeries.x.zip(probeSeries.y)
            if (points.size < 2) return@forEachIndexed
            points.zipWithNext().forEach { (start, end) ->
                drawLine(
                    color = probeSeriesColor(index),
                    start = Offset(
                        x = plotLeft + ((start.first / maxX).toFloat()) * plotWidth,
                        y = plotTop + plotHeight * (1f - ((start.second - minValue) / range).toFloat())
                    ),
                    end = Offset(
                        x = plotLeft + ((end.first / maxX).toFloat()) * plotWidth,
                        y = plotTop + plotHeight * (1f - ((end.second - minValue) / range).toFloat())
                    ),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun SimpleLineChart(values: List<Double>, color: Color, height: Dp) {
    val fractions = GraphScale.fractions(values)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
    ) {
        if (fractions.size < 2) return@Canvas
        val step = size.width / (fractions.lastIndex)
        fractions.zipWithNext().forEachIndexed { index, (start, end) ->
            drawLine(
                color = color,
                start = Offset(index * step, size.height * (1f - start)),
                end = Offset((index + 1) * step, size.height * (1f - end)),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun DualLineChart(
    downValues: List<Double>,
    upValues: List<Double>,
    modifier: Modifier,
    downColor: Color,
    upColor: Color
) {
    val downFractions = GraphScale.fractions(downValues)
    val upFractions = GraphScale.fractions(upValues)
    Canvas(modifier = modifier) {
        drawSeries(downFractions, downColor, 3.dp.toPx())
        drawSeries(upFractions, upColor, 3.dp.toPx())
    }
}

private fun DrawScope.drawSeries(
    fractions: List<Float>,
    color: Color,
    strokeWidth: Float
) {
    if (fractions.size < 2) return
    val step = size.width / fractions.lastIndex
    fractions.zipWithNext().forEachIndexed { index, (start, end) ->
        drawLine(
            color = color,
            start = Offset(index * step, size.height * (1f - start)),
            end = Offset((index + 1) * step, size.height * (1f - end)),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun EmptyChart(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun probeSeriesColor(index: Int): Color {
    val colors = listOf(
        Color(0xFF2F6DF6),
        Color(0xFF1F7A5C),
        Color(0xFFE6A700),
        Color(0xFFD94A3A)
    )
    return colors[index % colors.size]
}

private data class TileColors(
    val container: Color,
    val accent: Color
)

@Composable
private fun DashboardTileTone.colors(): TileColors = when (this) {
    DashboardTileTone.GOOD -> TileColors(Color(0xFFEAF6F0), Color(0xFF1F7A5C))
    DashboardTileTone.WARN -> TileColors(Color(0xFFFFF4D6), Color(0xFFE6A700))
    DashboardTileTone.BAD -> TileColors(Color(0xFFFFE8E4), Color(0xFFD94A3A))
    DashboardTileTone.NEUTRAL -> TileColors(MaterialTheme.colorScheme.surfaceVariant, Color(0xFF627080))
}

private fun DashboardHealth.tone(): TileColors = when (this) {
    DashboardHealth.EXCELLENT -> TileColors(Color(0xFFEAF6F0), Color(0xFF1F7A5C))
    DashboardHealth.GOOD -> TileColors(Color(0xFFEAF6F0), Color(0xFF1F7A5C))
    DashboardHealth.UNSTABLE -> TileColors(Color(0xFFFFF4D6), Color(0xFFE6A700))
    DashboardHealth.POOR -> TileColors(Color(0xFFFFF4D6), Color(0xFFE6A700))
    DashboardHealth.OFFLINE -> TileColors(Color(0xFFFFE8E4), Color(0xFFD94A3A))
}

private fun ScoreBreakdown.scoreBreakdownLampColor(): Color {
    val fraction = scoreBreakdownFraction()
    return when {
        fraction >= 0.8f -> Color(0xFF1F7A5C)
        fraction >= 0.5f -> Color(0xFFE6A700)
        else -> Color(0xFFD94A3A)
    }
}

private fun ScoreBreakdown.scoreBreakdownFraction(): Float {
    return if (max <= 0) 0f else (value.toFloat() / max.toFloat()).coerceIn(0f, 1f)
}

private fun ScoreBreakdown.displayLabel(): String = when (label) {
    "LINK" -> "Net"
    "LAT" -> "Ping"
    "LOSS" -> "Loss"
    "JIT" -> "Jitter"
    "FLOW" -> "Flow"
    else -> label.lowercase().replaceFirstChar { it.uppercase() }
}

private fun DashboardTileIcon.glyph(): String = when (this) {
    DashboardTileIcon.SIM -> "SIM"
    DashboardTileIcon.NETWORK -> "NW"
    DashboardTileIcon.DNS -> "DNS"
    DashboardTileIcon.TRAFFIC -> "TX"
    DashboardTileIcon.PROBE -> "P"
}
