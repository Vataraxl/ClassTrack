package com.classtrack.app.ui.main

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classtrack.app.data.model.ArtistTheme
import com.classtrack.app.system.LiveCapsuleOsFamily
import com.classtrack.app.ui.theme.ClassTrackTheme
import com.classtrack.app.ui.theme.LiquidGlassBackdrop
import com.classtrack.app.ui.theme.LocalClassTrackVisuals
import com.classtrack.app.ui.theme.UiStyleOpticalTransitionContainer
import com.classtrack.app.ui.theme.liquidGlassSurface
import kotlin.math.roundToInt

@Composable
fun MainScreen(
    viewModel: MainScreenViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val artistTheme by viewModel.artistTheme.collectAsStateWithLifecycle()
    val customThemeConfig by viewModel.customThemeConfig.collectAsStateWithLifecycle()
    val customThemes by viewModel.customThemes.collectAsStateWithLifecycle()
    val selectedPortalId by viewModel.selectedPortalId.collectAsStateWithLifecycle()
    val recentPortalIds by viewModel.recentPortalIds.collectAsStateWithLifecycle()
    val viewingWeek by viewModel.viewingWeek.collectAsStateWithLifecycle()
    val currentWeek by viewModel.currentWeek.collectAsStateWithLifecycle()
    val totalWeeks by viewModel.totalWeeks.collectAsStateWithLifecycle()
    val maxSections by viewModel.maxSections.collectAsStateWithLifecycle()
    val selectedDayOfWeek by viewModel.selectedDayOfWeek.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val timeSlots by viewModel.timeSlots.collectAsStateWithLifecycle()
    val currentLyric by viewModel.currentLyric.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val showCourseDialog by viewModel.showCourseDialog.collectAsStateWithLifecycle()
    val showThemeCabinetSheet by viewModel.showThemeCabinetSheet.collectAsStateWithLifecycle()
    val editingCourse by viewModel.editingCourse.collectAsStateWithLifecycle()
    val fluidCloudEnabled by viewModel.fluidCloudEnabled.collectAsStateWithLifecycle()
    val watchBridgeEnabled by viewModel.watchBridgeEnabled.collectAsStateWithLifecycle()
    val leadMinutes by viewModel.leadMinutes.collectAsStateWithLifecycle()
    val uiStyle by viewModel.uiStyle.collectAsStateWithLifecycle()
    val uiStyleTransition by viewModel.uiStyleTransitionTrigger.collectAsStateWithLifecycle()
    val nextReminderInfo by viewModel.nextReminderInfo.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showWeekStrip by remember { mutableStateOf(false) }

    // 真正意义上的常驻内存保活容器：
    // 访问过的 Tab 保留在 Composition 树中，隐藏时通过 layout 不放置节点（0 绘制、0 点击拦截），
    // 彻底解决 AnimatedVisibility 在 exit 后销毁视图树导致的切换卡顿！
    val renderedTabs = remember { mutableStateOf(setOf(activeTab)) }
    if (!renderedTabs.value.contains(activeTab)) {
        renderedTabs.value = renderedTabs.value + activeTab
    }

    LaunchedEffect(activeTab) {
        if (activeTab != MainTab.WEEKLY_GRID) {
            showWeekStrip = false
        }
    }

    LaunchedEffect(toastMessage) {
        val msg = toastMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToastMessage()
        }
    }

    var pendingOsFamily by remember { mutableStateOf(LiveCapsuleOsFamily.AUTO) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.triggerFluidCloudLivePreview(context, preferredOsFamily = pendingOsFamily)
    }

    val handleTriggerFluidCloud: (LiveCapsuleOsFamily) -> Unit = { osFamily ->
        pendingOsFamily = osFamily
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                viewModel.triggerFluidCloudLivePreview(context, preferredOsFamily = osFamily)
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            viewModel.triggerFluidCloudLivePreview(context, preferredOsFamily = osFamily)
        }
    }

    UiStyleOpticalTransitionContainer(
        targetUiStyle = uiStyle,
        event = uiStyleTransition,
        onTransitionFinished = { viewModel.clearUiStyleTransition() },
        modifier = modifier.fillMaxSize()
    ) { displayedUiStyle ->
        ClassTrackTheme(
            artistTheme = artistTheme,
            customThemeConfig = customThemeConfig,
            uiStyle = displayedUiStyle
        ) {
            val visuals = LocalClassTrackVisuals.current

            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as? Activity)?.window ?: return@SideEffect
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !visuals.isDark
                        isAppearanceLightNavigationBars = !visuals.isDark
                    }
                }
            }

            val glassBackdropAlpha by animateFloatAsState(
                targetValue = if (visuals.isGlass) 1f else 0f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                label = "GlassBackdropAlpha"
            )
            val scaffoldBgColor by animateColorAsState(
                targetValue = if (visuals.isGlass) Color.Transparent else MaterialTheme.colorScheme.background,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                label = "ScaffoldBgTransition"
            )

            Box(modifier = Modifier.fillMaxSize()) {
                if (glassBackdropAlpha > 0.005f) {
                    LiquidGlassBackdrop(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = glassBackdropAlpha }
                    )
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = scaffoldBgColor,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    topBar = {
                        ClassTrackMinimalTopBar(
                            activeTab = activeTab,
                            viewingWeek = viewingWeek,
                            currentWeek = currentWeek,
                            totalWeeks = totalWeeks,
                            showWeekStrip = showWeekStrip,
                            artistTheme = artistTheme,
                            themeDisplayName = visuals.themeDisplayName,
                            onToggleWeekStrip = { showWeekStrip = !showWeekStrip },
                            onPrevWeek = { viewModel.changeWeek(-1) },
                            onNextWeek = { viewModel.changeWeek(1) },
                            onSelectWeek = {
                                viewModel.selectViewingWeek(it)
                                showWeekStrip = false
                            },
                            onReturnToCurrentWeek = {
                                viewModel.returnToCurrentWeek()
                                showWeekStrip = false
                            },
                            onCycleTheme = { viewModel.openThemeCabinetSheet() },
                            onAddCourse = { viewModel.openAddCourseDialog() }
                        )
                    },
                    bottomBar = {
                        ClassTrackMinimalBottomBar(
                            activeTab = activeTab,
                            onSelectTab = { targetTab ->
                                if (targetTab != activeTab) {
                                    runCatching {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    }
                                    viewModel.selectTab(targetTab)
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        MainTab.entries.forEach { tab ->
                            if (renderedTabs.value.contains(tab)) {
                                KeepAliveTabLayer(
                                    isActive = tab == activeTab,
                                    tabIndex = tab.ordinal,
                                    activeIndex = activeTab.ordinal
                                ) {
                                    when (tab) {
                                        MainTab.WEEKLY_GRID -> {
                                            WeeklyGridSection(
                                                viewingWeek = viewingWeek,
                                                currentWeek = currentWeek,
                                                totalWeeks = totalWeeks,
                                                maxSections = maxSections,
                                                selectedDayOfWeek = selectedDayOfWeek,
                                                courses = courses,
                                                timeSlots = timeSlots,
                                                lyricQuote = currentLyric,
                                                onNextLyric = { viewModel.nextLyricQuote() },
                                                onOpenThemeCabinet = { viewModel.openThemeCabinetSheet() },
                                                onChangeWeek = { viewModel.changeWeek(it) },
                                                onSelectWeek = { viewModel.selectViewingWeek(it) },
                                                onReturnToCurrentWeek = { viewModel.returnToCurrentWeek() },
                                                onSetAsCurrentWeek = { viewModel.setActualCurrentWeek(it) },
                                                onSelectDay = { viewModel.selectDayOfWeek(it) },
                                                onEditCourse = { viewModel.openEditCourseDialog(it) },
                                                onAddCourseAt = { day, sec ->
                                                    viewModel.openAddCourseDialog(
                                                        defaultDay = day,
                                                        defaultStartSection = sec
                                                    )
                                                }
                                            )
                                        }

                                        MainTab.IMPORT_CENTER -> {
                                            ImportCenterSection(
                                                selectedPortalId = selectedPortalId,
                                                recentPortalIds = recentPortalIds,
                                                onSelectPortal = { viewModel.selectPortal(it) },
                                                onImportRawText = { viewModel.importScheduleFromText(it) },
                                                onApplyImportResult = { viewModel.applyImportResult(it) },
                                                onExportBackupJson = { viewModel.exportScheduleBackupJson() },
                                                onExportShareToken = { viewModel.exportScheduleShareToken() },
                                                onBuildDeepSeekSchedulePrompt = { viewModel.buildDeepSeekScheduleJsonPrompt() },
                                                onClearAllCourses = { viewModel.clearAllCourses() },
                                                onShowMessage = { viewModel.showMessage(it) }
                                            )
                                        }

                                        MainTab.THEME_STUDIO -> {
                                            ThemeStudioSection(
                                                currentTheme = artistTheme,
                                                customThemeConfig = customThemeConfig,
                                                customThemes = customThemes,
                                                onSelectTheme = { viewModel.setArtistTheme(it) },
                                                onSelectCustomTheme = { viewModel.selectCustomTheme(it) },
                                                onDeleteCustomTheme = { viewModel.deleteCustomTheme(it) },
                                                onImportThemeJson = { viewModel.importThemeFromJson(it) },
                                                onBuildDeepSeekThemePrompt = { viewModel.buildDeepSeekThemePrompt(it) },
                                                onOpenThemeCabinet = { viewModel.openThemeCabinetSheet() },
                                                onShowMessage = { viewModel.showMessage(it) }
                                            )
                                        }

                                        MainTab.SETTINGS -> {
                                            SettingsSection(
                                                uiStyle = uiStyle,
                                                currentWeek = currentWeek,
                                                totalWeeks = totalWeeks,
                                                maxSections = maxSections,
                                                timeSlots = timeSlots,
                                                fluidCloudEnabled = fluidCloudEnabled,
                                                watchBridgeEnabled = watchBridgeEnabled,
                                                leadMinutes = leadMinutes,
                                                nextReminderInfo = nextReminderInfo,
                                                onSelectUiStyle = { viewModel.setUiStyle(it) },
                                                onChangeCurrentWeek = { viewModel.setActualCurrentWeek(it) },
                                                onChangeTotalWeeks = { viewModel.setTotalWeeks(it) },
                                                onChangeMaxSections = { viewModel.setMaxSections(it) },
                                                onUpdateTimeSlot = { sec, start, end ->
                                                    viewModel.updateSingleTimeSlot(sec, start, end)
                                                },
                                                onResetTimeSlots = { viewModel.resetTimeSlotsToDefault() },
                                                onToggleFluidCloud = { viewModel.setFluidCloudEnabled(it) },
                                                onToggleWatchBridge = { viewModel.setWatchBridgeEnabled(it) },
                                                onChangeLeadMinutes = { viewModel.setLeadMinutes(it) },
                                                onTriggerFluidCloudPreview = handleTriggerFluidCloud,
                                                onTestWatchBridge = { viewModel.testWatchBridge(context) },
                                                onRefreshAutomaticAlarms = { viewModel.refreshAutomaticAlarms(context) },
                                                onShowMessage = { viewModel.showMessage(it) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (showCourseDialog && editingCourse != null) {
                    CourseEditDialog(
                        initialCourse = editingCourse!!,
                        onDismiss = { viewModel.dismissCourseDialog() },
                        onSave = { viewModel.saveCourse(it) },
                        onDelete = { viewModel.deleteCourse(it) }
                    )
                }

                if (showThemeCabinetSheet) {
                    VinylThemeCabinetBottomSheet(
                        currentTheme = artistTheme,
                        customThemeConfig = customThemeConfig,
                        customThemes = customThemes,
                        onSelectTheme = { viewModel.setArtistTheme(it) },
                        onSelectCustomTheme = { viewModel.selectCustomTheme(it) },
                        onDeleteCustomTheme = { viewModel.deleteCustomTheme(it) },
                        onNavigateToThemeStudio = { viewModel.selectTab(MainTab.THEME_STUDIO) },
                        onDismiss = { viewModel.dismissThemeCabinetSheet() }
                    )
                }
            }
        }
    }
}

/**
 * 120Hz 零延迟保活图层容器：
 * 保持已访问 Tab 的 Composition 节点常驻内存，隐藏时不执行 place（不绘制、不响应触摸），
 * 切换瞬间在 RenderThread 上以高刚度可打断弹簧（Stiffness = 680f）瞬时平滑过渡。
 */
@Composable
private fun KeepAliveTabLayer(
    isActive: Boolean,
    tabIndex: Int,
    activeIndex: Int,
    content: @Composable () -> Unit
) {
    val progress by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.90f,
            stiffness = 680f
        ),
        label = "TabLayerProgress"
    )
    val slideSign = when {
        tabIndex > activeIndex -> 1f
        tabIndex < activeIndex -> -1f
        else -> 0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(if (isActive) 1f else 0f)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    if (isActive || progress > 0.004f) {
                        val maxSlidePx = 14.dp.toPx()
                        val offsetX = (1f - progress) * maxSlidePx * slideSign
                        placeable.placeWithLayer(0, 0) {
                            alpha = progress.coerceIn(0f, 1f)
                            translationX = offsetX
                            val s = 0.988f + 0.012f * progress
                            scaleX = s
                            scaleY = s
                        }
                    }
                }
            }
    ) {
        content()
    }
}

/**
 * 瑞士现代网格排版顶栏：
 * 去除繁琐的双重边框与廉价胶囊堆砌，以呼吸感留白、大字重标题与高响应微缩按钮构建旗舰视觉。
 */
@Composable
private fun ClassTrackMinimalTopBar(
    activeTab: MainTab,
    viewingWeek: Int,
    currentWeek: Int,
    totalWeeks: Int,
    showWeekStrip: Boolean,
    artistTheme: ArtistTheme,
    themeDisplayName: String,
    onToggleWeekStrip: () -> Unit,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onSelectWeek: (Int) -> Unit,
    onReturnToCurrentWeek: () -> Unit,
    onCycleTheme: () -> Unit,
    onAddCourse: () -> Unit
) {
    val visuals = LocalClassTrackVisuals.current
    val isCurrentWeek = viewingWeek == currentWeek
    val isWeeklyGridTab = activeTab == MainTab.WEEKLY_GRID

    val topBarBgModifier = if (visuals.isGlass) {
        Modifier
            .fillMaxWidth()
            .liquidGlassSurface(
                isDark = visuals.isDark,
                accentColor = MaterialTheme.colorScheme.primary,
                elevated = false
            )
    } else {
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    }

    Column(modifier = topBarBgModifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧主标题与周次控制区
            if (isWeeklyGridTab) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 点击展开周次滑条的大标题
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onToggleWeekStrip() }
                            .padding(vertical = 2.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            text = "第 $viewingWeek 周",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.4).sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (showWeekStrip) "▴" else "▾",
                            fontSize = 13.sp,
                            color = visuals.mutedTextColor
                        )
                    }

                    // 紧凑型左右切周与状态胶囊
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isCurrentWeek) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (visuals.isGlass) 0.55f else 0.85f)
                        } else {
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "‹",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrentWeek) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onPrevWeek() }
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )

                            Text(
                                text = if (isCurrentWeek) "本周" else "非本周 · 回本周",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isCurrentWeek) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.secondary
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        if (!isCurrentWeek) onReturnToCurrentWeek() else onToggleWeekStrip()
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )

                            Text(
                                text = "›",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrentWeek) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onNextWeek() }
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(
                        text = when (activeTab) {
                            MainTab.IMPORT_CENTER -> "导入与备份"
                            MainTab.THEME_STUDIO -> "主题工作台"
                            MainTab.SETTINGS -> "系统设置"
                            else -> activeTab.title
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.4).sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "课轨",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // 右侧操作区（极简主题切换胶囊 + 加课按钮）
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val themeInteraction = remember { MutableInteractionSource() }
                val isThemePressed by themeInteraction.collectIsPressedAsState()
                val themeScale by animateFloatAsState(
                    targetValue = if (isThemePressed) 0.93f else 1f,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 600f),
                    label = "ThemeBtnScale"
                )

                Surface(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = themeScale
                            scaleY = themeScale
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(
                            interactionSource = themeInteraction,
                            indication = null,
                            onClick = onCycleTheme
                        ),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (visuals.isGlass) 0.55f else 0.9f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        VinylRecordDisc(
                            primaryColor = MaterialTheme.colorScheme.primary,
                            secondaryColor = MaterialTheme.colorScheme.secondary,
                            size = 16.dp,
                            spinning = false
                        )
                        Text(
                            text = themeDisplayName.take(6),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "▾",
                            fontSize = 10.sp,
                            color = visuals.mutedTextColor
                        )
                    }
                }

                if (isWeeklyGridTab) {
                    val addInteraction = remember { MutableInteractionSource() }
                    val isAddPressed by addInteraction.collectIsPressedAsState()
                    val addScale by animateFloatAsState(
                        targetValue = if (isAddPressed) 0.92f else 1f,
                        animationSpec = spring(dampingRatio = 0.75f, stiffness = 600f),
                        label = "AddBtnScale"
                    )

                    Surface(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = addScale
                                scaleY = addScale
                            }
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(
                                interactionSource = addInteraction,
                                indication = null,
                                onClick = onAddCourse
                            ),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "+ 加课",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // 可展开的周次选择滑条
        val weekListState = rememberLazyListState(
            initialFirstVisibleItemIndex = (viewingWeek - 3).coerceAtLeast(0)
        )
        LaunchedEffect(showWeekStrip, viewingWeek) {
            if (showWeekStrip) {
                weekListState.animateScrollToItem((viewingWeek - 3).coerceAtLeast(0))
            }
        }

        AnimatedVisibility(
            visible = isWeeklyGridTab && showWeekStrip,
            enter = expandVertically(animationSpec = spring(dampingRatio = 0.85f, stiffness = 500f)) +
                fadeIn(tween(150)),
            exit = shrinkVertically(tween(150)) + fadeOut(tween(120))
        ) {
            LazyRow(
                state = weekListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(totalWeeks, key = { it + 1 }) { index ->
                    val w = index + 1
                    val isActualCurrent = w == currentWeek
                    val isViewing = w == viewingWeek
                    val chipBg = when {
                        isViewing -> MaterialTheme.colorScheme.primary
                        isActualCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    }
                    val chipTextColor = when {
                        isViewing -> MaterialTheme.colorScheme.onPrimary
                        isActualCurrent -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectWeek(w) },
                        shape = RoundedCornerShape(12.dp),
                        color = chipBg
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "第${w}周",
                                fontSize = 11.5.sp,
                                fontWeight = if (isViewing || isActualCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = chipTextColor
                            )
                            Text(
                                text = if (isActualCurrent) "本周" else "·",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isViewing) {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                } else if (isActualCurrent) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Transparent
                                }
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(
            thickness = 0.5.dp,
            color = visuals.cardBorderColor.copy(alpha = if (visuals.isGlass) 0.35f else 0.6f)
        )
    }
}

/**
 * 旗舰级物理滑动胶囊底部导航栏：
 * 配备精细绘制的几何矢量图标与高帧率跟随滑动的指示胶囊（Sliding Pill Indicator），
 * 点击或快速连按任意 Tab 均呈现跟手物理弹性移动。
 */
@Composable
private fun ClassTrackMinimalBottomBar(
    activeTab: MainTab,
    onSelectTab: (MainTab) -> Unit
) {
    val visuals = LocalClassTrackVisuals.current
    val tabs = MainTab.entries
    val selectedIndex = activeTab.ordinal

    val animatedTabIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = 540f
        ),
        label = "SlidingPillIndex"
    )

    if (visuals.isGlass) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .liquidGlassSurface(
                        isDark = visuals.isDark,
                        accentColor = MaterialTheme.colorScheme.primary,
                        elevated = true
                    )
                    .border(visuals.glassCardBorder, RoundedCornerShape(28.dp))
                    .padding(4.dp)
            ) {
                val tabWidth = maxWidth / tabs.size

                // 物理跟随滑动的高亮胶囊底块
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (tabWidth.toPx() * animatedTabIndex).roundToInt(),
                                y = 0
                            )
                        }
                        .width(tabWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            if (visuals.isDark) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            } else {
                                Color.White.copy(alpha = 0.92f)
                            }
                        )
                        .border(
                            width = 0.8.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(24.dp)
                        )
                )

                Row(modifier = Modifier.fillMaxSize()) {
                    tabs.forEach { tab ->
                        BottomBarTabItem(
                            tab = tab,
                            isSelected = tab == activeTab,
                            onClick = { onSelectTab(tab) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = visuals.cardBorderColor.copy(alpha = 0.7f)
            )
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .height(50.dp)
            ) {
                val tabWidth = maxWidth / tabs.size

                // 物理跟随滑动的柔和胶囊指示器
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (tabWidth.toPx() * animatedTabIndex).roundToInt(),
                                y = 0
                            )
                        }
                        .width(tabWidth)
                        .fillMaxHeight()
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                )

                Row(modifier = Modifier.fillMaxSize()) {
                    tabs.forEach { tab ->
                        BottomBarTabItem(
                            tab = tab,
                            isSelected = tab == activeTab,
                            onClick = { onSelectTab(tab) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomBarTabItem(
    tab: MainTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visuals = LocalClassTrackVisuals.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val itemScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 650f),
        label = "BottomNavItemScale"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            visuals.mutedTextColor
        },
        animationSpec = tween(150),
        label = "BottomNavItemColor"
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = itemScale
                scaleY = itemScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TabVectorIcon(
            tab = tab,
            color = contentColor,
            isSelected = isSelected,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = tab.title,
            fontSize = 10.5.sp,
            lineHeight = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/**
 * 极简瑞士几何矢量图标（Canvas 原生绘制，零图片资源开销，适配任意主题色）
 */
@Composable
private fun TabVectorIcon(
    tab: MainTab,
    color: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = if (isSelected) 2.0.dp.toPx() else 1.6.dp.toPx()
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)

        when (tab) {
            MainTab.WEEKLY_GRID -> {
                // 2x2 现代圆角日程网格
                val gap = w * 0.12f
                val cell = (w - gap) * 0.44f
                val offset = (w - (cell * 2 + gap)) / 2f
                val r = CornerRadius(cell * 0.28f, cell * 0.28f)

                drawRoundRect(
                    color = color,
                    topLeft = Offset(offset, offset),
                    size = Size(cell, cell),
                    cornerRadius = r,
                    style = if (isSelected) androidx.compose.ui.graphics.drawscope.Fill else stroke
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(offset + cell + gap, offset),
                    size = Size(cell, cell),
                    cornerRadius = r,
                    style = stroke
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(offset, offset + cell + gap),
                    size = Size(cell, cell),
                    cornerRadius = r,
                    style = stroke
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(offset + cell + gap, offset + cell + gap),
                    size = Size(cell, cell),
                    cornerRadius = r,
                    style = if (isSelected) androidx.compose.ui.graphics.drawscope.Fill else stroke
                )
            }

            MainTab.IMPORT_CENTER -> {
                // 导入托盘与向下箭头
                val cx = w * 0.5f
                drawLine(
                    color = color,
                    start = Offset(cx, h * 0.14f),
                    end = Offset(cx, h * 0.64f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(cx - w * 0.22f, h * 0.44f),
                    end = Offset(cx, h * 0.66f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(cx + w * 0.22f, h * 0.44f),
                    end = Offset(cx, h * 0.66f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.18f, h * 0.84f),
                    end = Offset(w * 0.82f, h * 0.84f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            MainTab.THEME_STUDIO -> {
                // 唱片/调色盘同心圆与高光点
                drawCircle(
                    color = color,
                    radius = w * 0.40f,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = stroke
                )
                drawCircle(
                    color = color,
                    radius = if (isSelected) w * 0.16f else w * 0.12f,
                    center = Offset(w * 0.5f, h * 0.5f)
                )
            }

            MainTab.SETTINGS -> {
                // 双轨均衡器滑块
                val y1 = h * 0.32f
                val y2 = h * 0.68f
                drawLine(
                    color = color,
                    start = Offset(w * 0.15f, y1),
                    end = Offset(w * 0.85f, y1),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = color,
                    radius = w * 0.13f,
                    center = Offset(w * 0.64f, y1)
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.15f, y2),
                    end = Offset(w * 0.85f, y2),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = color,
                    radius = w * 0.13f,
                    center = Offset(w * 0.36f, y2)
                )
            }
        }
    }
}
