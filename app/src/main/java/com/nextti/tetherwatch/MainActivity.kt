package com.nextti.tetherwatch

import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart

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
        setContent {
            TetherWatchTheme {
                val viewModel: DashboardViewModel = viewModel()
                val state by viewModel.state.collectAsState()
                TetherDashboardScreen(
                    state = state,
                    onProbeIntervalSelected = viewModel::setProbeIntervalSeconds
                )
            }
        }
    }
}

@Composable
private fun TetherWatchTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colorScheme = if (Build.VERSION.SDK_INT >= 31 && !LocalInspectionMode.current) {
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
    MaterialTheme(colorScheme = colorScheme, content = content)
}

@Composable
private fun TetherDashboardScreen(
    state: DashboardState,
    onProbeIntervalSelected: (Int) -> Unit
) {
    val visual = DashboardVisualModelBuilder.build(state)
    var showProbeSettings by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TopStatusHeader(visual)
            HeroPanel(visual.hero, state.score, state.traffic.samples)
            StatusTileGrid(visual.tiles)
            ProbePanel(
                state = state,
                onClick = { showProbeSettings = true }
            )
            AlertStrip(visual.alerts)
            Spacer(Modifier.height(4.dp))
        }
    }
    if (showProbeSettings) {
        ProbeSettingsDialog(
            settings = state.probeSettings,
            onSelect = {
                onProbeIntervalSelected(it)
                showProbeSettings = false
            },
            onDismiss = { showProbeSettings = false }
        )
    }
}

@Composable
private fun TopStatusHeader(visual: DashboardVisualModel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
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
                    "${visual.header.tetheringLabel} ${visual.header.clientCountLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun HeroPanel(hero: HeroVisualModel, score: DashboardScore, samples: List<TrafficSamplePoint>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScoreRing(score.value, score.health)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(hero.healthLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            hero.countryLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.74f)
                        )
                    }
                    TrafficLegend(hero)
                }
                DualTrafficSparkline(
                    samples = samples,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                )
            }
        }
    }
}

@Composable
private fun TrafficLegend(hero: HeroVisualModel) {
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        LegendMetric("Down", hero.downloadValue, Color(0xFF1F7A5C))
        LegendMetric("Up", hero.uploadValue, Color(0xFF2F6DF6))
    }
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
private fun ScoreRing(score: Int, health: DashboardHealth) {
    val ringColor = health.tone().accent
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(74.dp)) {
        Canvas(modifier = Modifier.size(74.dp)) {
            val stroke = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = Color.White.copy(alpha = 0.45f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()),
                style = stroke
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * (score.coerceIn(0, 100) / 100f),
                useCenter = false,
                topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()),
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
            .fillMaxWidth(0.48f)
            .heightIn(min = 82.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = tone.container)
    ) {
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
private fun ProbePanel(state: DashboardState, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                Text("Probe response", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "${ProbeIntervalOptions.label(state.probeSettings.intervalSeconds)} · Loss ${state.probe.summary.packetLossPercent}% / jitter ${state.probe.summary.jitterMs ?: 0} ms",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ProbeTargetStrip(state.probe.targets)
            ProbeLatencyChart(state.probe.history, chartHeight = 148.dp)
        }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
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

@Composable
private fun ProbeSettingsDialog(
    settings: ProbeSettings,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Probe interval") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ProbeIntervalOptions.seconds.forEach { seconds ->
                    TextButton(onClick = { onSelect(seconds) }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (settings.intervalSeconds == seconds) {
                                "${ProbeIntervalOptions.label(seconds)} · selected"
                            } else {
                                ProbeIntervalOptions.label(seconds)
                            },
                            maxLines = 1
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun AlertStrip(alerts: List<DashboardAlertVisualModel>) {
    if (alerts.isEmpty()) return
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        alerts.take(3).forEach { alert ->
            val tone = alert.tone.colors()
            Surface(color = tone.container, contentColor = tone.accent, shape = RoundedCornerShape(12.dp)) {
                Text(
                    alert.message,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
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
    DualLineChart(
        downValues = samples.map { it.rxBps },
        upValues = samples.map { it.txBps },
        modifier = modifier,
        downColor = Color(0xFF1F7A5C),
        upColor = Color(0xFF2F6DF6)
    )
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
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(chartModel.series) {
        if (chartModel.series.isNotEmpty()) {
            modelProducer.runTransaction {
                lineModel {
                    chartModel.series.forEach { series ->
                        series(series.x, series.y)
                    }
                }
            }
        }
    }

    if (chartModel.series.isEmpty()) {
        EmptyChart(if (points.any { !it.success }) "Failing" else "Collecting")
    } else {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(),
                startAxis = VerticalAxis.rememberStart(),
                bottomAxis = HorizontalAxis.rememberBottom(),
            ),
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
        )
    }
    ProbeFailureStrip(chartModel.failures)
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
private fun ProbeFailureStrip(failures: List<Boolean>) {
    if (failures.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        failures.forEach { failed ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .background(
                        if (failed) Color(0xFFD94A3A) else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(2.dp)
                    )
            )
        }
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

private fun DashboardTileIcon.glyph(): String = when (this) {
    DashboardTileIcon.SIM -> "SIM"
    DashboardTileIcon.NETWORK -> "NW"
    DashboardTileIcon.DNS -> "DNS"
    DashboardTileIcon.TRAFFIC -> "TX"
    DashboardTileIcon.PROBE -> "P"
    DashboardTileIcon.TETHER -> "TH"
}
