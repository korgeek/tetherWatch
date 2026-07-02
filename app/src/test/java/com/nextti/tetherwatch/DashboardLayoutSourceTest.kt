package com.nextti.tetherwatch

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DashboardLayoutSourceTest {
    @Test
    fun heroPanelUsesLargerTopSurfaceDimensions() {
        val mainActivity = File("src/main/java/com/nextti/tetherwatch/MainActivity.kt").readText()

        assertTrue(mainActivity.contains("HeroPanel(hero: HeroVisualModel"))
        assertTrue(mainActivity.contains(".padding(16.dp)"))
        assertTrue(mainActivity.contains("ScoreRing(score.value, score.health, modifier = Modifier.size(88.dp))"))
        assertTrue(mainActivity.contains(".height(82.dp)"))
    }

    @Test
    fun pingPanelDoesNotShowTargetListAndUsesCompactConsoleRows() {
        val mainActivity = File("src/main/java/com/nextti/tetherwatch/MainActivity.kt").readText()

        assertTrue(mainActivity.contains("lines.takeLast(settings.consoleLineCount)"))
        assertTrue(mainActivity.contains("fontSize = settings.consoleFontSizeSp.sp"))
        assertTrue(mainActivity.contains("lineHeight = (settings.consoleFontSizeSp + 1).sp"))
        assertTrue(mainActivity.contains("pingConsoleLineColor(line, settings.slowResponseThresholdMs)"))
        assertTrue(mainActivity.contains(".height(consolePanelHeight(settings.consoleFontSizeSp, settings.consoleLineCount))"))
        assertTrue(mainActivity.contains(".height(consolePanelHeight(state.probeSettings.consoleFontSizeSp, state.probeSettings.consoleLineCount))"))
        assertTrue(mainActivity.contains("private fun consolePanelHeight(fontSizeSp: Int, lineCount: Int): Dp"))
        assertTrue(!mainActivity.contains(".heightIn(min = 116.dp)"))
        assertTrue(!mainActivity.contains("\"Target and console\""))
        assertTrue(!mainActivity.contains("state.probeSettings.targets.joinToString(\"  \")"))
        assertTrue(!mainActivity.contains("PingSettingsDialog("))
        assertTrue(!mainActivity.contains(".clickable(onClick = onClick)"))
        assertTrue(!mainActivity.contains("\"Loss \${state.probe.summary.packetLossPercent}%"))
    }

    @Test
    fun statusPanelUsesConsoleAndCommonSlowThreshold() {
        val mainActivity = File("src/main/java/com/nextti/tetherwatch/MainActivity.kt").readText()
        val viewModel = File("src/main/java/com/nextti/tetherwatch/DashboardViewModel.kt").readText()

        assertTrue(mainActivity.contains("EndpointPanel(state = state)"))
        assertTrue(mainActivity.contains("Text(\"Status\""))
        assertTrue(!mainActivity.contains("Text(\"Status monitoring\""))
        assertTrue(mainActivity.contains("\"status.json\""))
        assertTrue(mainActivity.contains("EndpointConsolePanel("))
        assertTrue(mainActivity.contains("EndpointConsoleLineClassifier.tone(line)"))
        assertTrue(!mainActivity.contains("endpointConsoleLineColor(line, state.probeSettings.slowResponseThresholdMs)"))
        assertTrue(viewModel.contains("EndpointHttpChecker"))
        assertTrue(viewModel.contains("runDueEndpointChecks()"))
    }

    @Test
    fun appUsesPretendardTypographyButPingConsoleKeepsMonospace() {
        val mainActivity = File("src/main/java/com/nextti/tetherwatch/MainActivity.kt").readText()

        assertTrue(mainActivity.contains("PretendardFontFamily"))
        assertTrue(mainActivity.contains("Font(R.font.pretendard_regular, FontWeight.Normal)"))
        assertTrue(mainActivity.contains("Font(R.font.pretendard_bold, FontWeight.Bold)"))
        assertTrue(mainActivity.contains("typography = TetherWatchTypography"))
        assertTrue(mainActivity.contains("fontFamily = FontFamily.Monospace"))
    }

    @Test
    fun topNavigationShowsBrandAndSettingsEntry() {
        val mainActivity = File("src/main/java/com/nextti/tetherwatch/MainActivity.kt").readText()

        assertTrue(mainActivity.contains("TopNavigationBar("))
        assertTrue(mainActivity.contains("Text(\"Tether\""))
        assertTrue(mainActivity.contains("Text(\"Watch\""))
        assertTrue(mainActivity.contains(".padding(top = 0.dp, bottom = 2.dp)"))
        assertTrue(mainActivity.contains("Text(\"Tether\", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)"))
        assertTrue(mainActivity.contains("Text(\"Watch\", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Normal)"))
        assertTrue(mainActivity.contains("IconButton(onClick = onSettingsClick, modifier = Modifier.size(40.dp))"))
        assertTrue(mainActivity.contains("modifier = Modifier.size(22.dp)"))
        assertTrue(mainActivity.contains("R.drawable.ic_settings_24"))
        assertTrue(mainActivity.contains("Text(\"< Back\")"))
        assertTrue(!mainActivity.contains("R.drawable.ic_save_24"))
        assertTrue(!mainActivity.contains("onSaveSettingsClick"))
        assertTrue(!mainActivity.contains("contentDescription = \"Save settings\""))
    }

    @Test
    fun settingsBackSavesValidValuesAndBlocksInvalidValues() {
        val mainActivity = File("src/main/java/com/nextti/tetherwatch/MainActivity.kt").readText()

        assertTrue(mainActivity.contains("var saveSettingsAction by remember { mutableStateOf<(() -> Boolean)?>(null) }"))
        assertTrue(mainActivity.contains("private fun tryCloseSettings("))
        assertTrue(mainActivity.contains("if (tryCloseSettings(saveSettingsAction))"))
        assertTrue(mainActivity.contains("onSaveActionChanged: (((() -> Boolean)?) -> Unit)"))
        assertTrue(mainActivity.contains("return false"))
        assertTrue(mainActivity.contains("return true"))
    }

    @Test
    fun topNavigationStaysOutsideScrollableBody() {
        val mainActivity = File("src/main/java/com/nextti/tetherwatch/MainActivity.kt").readText()
        val screenStart = mainActivity.indexOf("private fun TetherDashboardScreen(")
        val navigationCall = mainActivity.indexOf("TopNavigationBar(", screenStart)
        val scrollBody = mainActivity.indexOf(".verticalScroll(scrollState)", screenStart)

        assertTrue(screenStart >= 0)
        assertTrue(navigationCall >= 0)
        assertTrue(scrollBody >= 0)
        assertTrue(navigationCall < scrollBody)
    }

    @Test
    fun scrollBodyKeepsDashboardContentUnmasked() {
        val mainActivity = File("src/main/java/com/nextti/tetherwatch/MainActivity.kt").readText()
        val screenStart = mainActivity.indexOf("private fun TetherDashboardScreen(")
        val screenEnd = mainActivity.indexOf("private fun AnimatedAmbientBackground(", screenStart)
        val screenSource = mainActivity.substring(screenStart, screenEnd)

        assertTrue(screenSource.contains("val scrollState = rememberScrollState()"))
        assertTrue(screenSource.contains(".verticalScroll(scrollState)"))
        assertTrue(screenSource.contains("Spacer(Modifier.height(16.dp))"))
        assertTrue(!screenSource.contains("ScrollEdgeScrim("))
        assertTrue(!mainActivity.contains("private fun ScrollEdgeScrim("))
        assertTrue(!mainActivity.contains(".scrollEdgeFadeMask()"))
        assertTrue(!mainActivity.contains("BlendMode.DstIn"))
    }

    @Test
    fun compactDnsTileKeepsIconAndShowsResolversInOneThinRow() {
        val mainActivity = File("src/main/java/com/nextti/tetherwatch/MainActivity.kt").readText()
        val tileStart = mainActivity.indexOf("private fun StatusTile(")
        val tileEnd = mainActivity.indexOf("@Composable", tileStart + 1)
        val tileSource = mainActivity.substring(tileStart, tileEnd)

        assertTrue(tileSource.contains(".fillMaxWidth(if (tile.icon == DashboardTileIcon.DNS) 1f else 0.48f)"))
        assertTrue(tileSource.contains(".heightIn(min = if (tile.icon == DashboardTileIcon.DNS) 52.dp else 82.dp)"))
        assertTrue(tileSource.contains("if (tile.icon == DashboardTileIcon.DNS)"))
        assertTrue(tileSource.contains("Text(tile.icon.glyph()"))
        assertTrue(tileSource.contains("tile.value,"))
        assertTrue(tileSource.contains("MaterialTheme.typography.titleSmall"))
        val dnsBranch = tileSource.substring(
            tileSource.indexOf("if (tile.icon == DashboardTileIcon.DNS)"),
            tileSource.indexOf("} else {")
        )
        assertTrue(!dnsBranch.contains("Text(tile.label"))
        assertTrue(tileSource.contains("if (tile.detail.isNotBlank())"))
        assertTrue(!tileSource.contains("if (tile.icon != DashboardTileIcon.TETHER)"))
    }

    @Test
    fun topStatusHeaderUsesRoundedInsetSurface() {
        val mainActivity = File("src/main/java/com/nextti/tetherwatch/MainActivity.kt").readText()
        val headerStart = mainActivity.indexOf("private fun TopStatusHeader(")
        val headerEnd = mainActivity.indexOf("@Composable", headerStart + 1)
        val headerSource = mainActivity.substring(headerStart, headerEnd)

        assertTrue(headerSource.contains("shape = RoundedCornerShape(14.dp)"))
        assertTrue(headerSource.contains("tonalElevation = 1.dp"))
        assertTrue(headerSource.contains(".padding(horizontal = 12.dp, vertical = 8.dp)"))
        assertTrue(headerSource.contains(".padding(horizontal = 8.dp, vertical = 4.dp)"))
    }

    @Test
    fun dashboardAndSettingsShareSubtleAnimatedAmbientBackground() {
        val mainActivity = File("src/main/java/com/nextti/tetherwatch/MainActivity.kt").readText()

        assertTrue(mainActivity.contains("containerColor = Color.Transparent"))
        assertTrue(mainActivity.contains("AnimatedAmbientBackground("))
        assertTrue(mainActivity.contains("rememberInfiniteTransition(label = \"ambientBackground\")"))
        assertTrue(mainActivity.contains("Brush.radialGradient"))
        assertTrue(mainActivity.contains("highlightSweep"))
        assertTrue(mainActivity.contains("drawAmbientLinearSweep("))
        assertTrue(mainActivity.contains("primary.copy(alpha = 0.115f)"))
        assertTrue(mainActivity.contains("tertiary.copy(alpha = 0.095f)"))
        assertTrue(mainActivity.contains("secondary.copy(alpha = 0.075f)"))
        assertTrue(mainActivity.contains("background(MaterialTheme.colorScheme.background)"))
    }

    @Test
    fun settingsScreenManagesPingTargetsAndAppOptions() {
        val mainActivity = File("src/main/java/com/nextti/tetherwatch/MainActivity.kt").readText()

        assertTrue(mainActivity.contains("SettingsScreen("))
        assertTrue(mainActivity.contains("Ping targets"))
        assertTrue(mainActivity.contains("label = { Text(\"IP\") }"))
        assertTrue(mainActivity.contains("label = { Text(\"Interval\", maxLines = 1, overflow = TextOverflow.Clip) }"))
        assertTrue(mainActivity.contains("Add target"))
        assertTrue(mainActivity.contains("R.drawable.ic_delete_24"))
        assertTrue(mainActivity.contains("modifier = Modifier.weight(1.5f)"))
        assertTrue(mainActivity.contains("modifier = Modifier.weight(0.62f)"))
        assertTrue(mainActivity.contains("horizontalArrangement = Arrangement.spacedBy(4.dp)"))
        assertTrue(!mainActivity.contains("OutlinedButton("))
        assertTrue(mainActivity.contains("Delete ping target?"))
        assertTrue(mainActivity.contains("TargetActionRow("))
        assertTrue(mainActivity.contains("horizontalArrangement = Arrangement.SpaceBetween"))
        assertTrue(mainActivity.contains("pendingPingResetConfirm = true"))
        assertTrue(mainActivity.contains("pendingEndpointResetConfirm = true"))
        assertTrue(mainActivity.contains("Reset ping targets?"))
        assertTrue(mainActivity.contains("Reset status targets?"))
        assertTrue(mainActivity.contains("This will restore Ping targets to the default values."))
        assertTrue(mainActivity.contains("This will restore Status targets to the default values."))
        assertTrue(mainActivity.contains("Confirm"))
        assertTrue(mainActivity.contains("Console display"))
        assertTrue(mainActivity.contains("Font size"))
        assertTrue(mainActivity.contains("Console lines"))
        assertTrue(mainActivity.contains("Ping slow response threshold"))
        assertTrue(mainActivity.contains("Status targets"))
        val pingTargetsIndex = mainActivity.indexOf("SettingsGroup(title = \"Ping targets\")")
        val statusTargetsIndex = mainActivity.indexOf("SettingsGroup(title = \"Status targets\")")
        val consoleDisplayIndex = mainActivity.indexOf("SettingsGroup(title = \"Console display\")")
        val pingSlowIndex = mainActivity.indexOf("Ping slow response threshold")
        assertTrue(pingTargetsIndex >= 0)
        assertTrue(statusTargetsIndex > pingTargetsIndex)
        assertTrue(consoleDisplayIndex > statusTargetsIndex)
        assertTrue(pingSlowIndex > pingTargetsIndex)
        assertTrue(pingSlowIndex < statusTargetsIndex)
        assertTrue(mainActivity.contains("label = { Text(\"URL\") }"))
        assertTrue(mainActivity.contains("Timeout"))
        assertTrue(!mainActivity.contains("Redirect limit"))
        assertTrue(mainActivity.contains("EndpointSettingsValidator.validate"))
        assertTrue(mainActivity.contains("Slider("))
        assertTrue(mainActivity.contains("PingSettingsValidator.validate"))
        assertTrue(mainActivity.contains("Keep screen on"))
        assertTrue(mainActivity.contains("Auto background monitoring"))
        assertTrue(mainActivity.contains("Theme"))
        assertTrue(mainActivity.contains("Use device theme"))
        assertTrue(mainActivity.contains("Black"))
        assertTrue(mainActivity.contains("errorMessage"))
        assertTrue(mainActivity.contains("KeyboardOptions(keyboardType = KeyboardType.Number)"))
    }

    @Test
    fun dashboardUsesTelephonyDisplayInfoForStatusBarLikeFiveGLabels() {
        val viewModel = File("src/main/java/com/nextti/tetherwatch/DashboardViewModel.kt").readText()

        assertTrue(viewModel.contains("TelephonyCallback.DisplayInfoListener"))
        assertTrue(viewModel.contains("ensureTelephonyDisplayCallback(activeDataSubId)"))
        assertTrue(viewModel.contains("telephony.createForSubscriptionId(it)"))
        assertTrue(viewModel.contains("NetworkTypeDisplayName.label(networkType, displayNetworkTypeOverride)"))
    }
}
