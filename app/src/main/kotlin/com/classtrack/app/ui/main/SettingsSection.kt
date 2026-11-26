package com.classtrack.app.ui.main

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classtrack.app.data.model.NotificationCardStyle
import com.classtrack.app.data.model.TimeSlot
import com.classtrack.app.data.model.UiStyle
import com.classtrack.app.system.ColorOsFluidCloudHelper
import com.classtrack.app.system.CourseAlarmScheduler
import com.classtrack.app.system.LiveCapsuleOsFamily
import com.classtrack.app.ui.theme.ClassTrackCard
import com.classtrack.app.ui.theme.LocalClassTrackVisuals
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun SettingsSection(
    uiStyle: UiStyle,
    currentWeek: Int,
    totalWeeks: Int,
    maxSections: Int,
    timeSlots: List<TimeSlot>,
    fluidCloudEnabled: Boolean,
    watchBridgeEnabled: Boolean,
    leadMinutes: Int,
    notificationCardStyle: NotificationCardStyle,
    alarmClockPriorityEnabled: Boolean,
    nextReminderInfo: String,
    onSelectUiStyle: (UiStyle) -> Unit,
    onChangeCurrentWeek: (Int) -> Unit,
    onChangeTotalWeeks: (Int) -> Unit,
    onChangeMaxSections: (Int) -> Unit,
    onUpdateTimeSlot: (section: Int, startTime: String, endTime: String) -> Unit,
    onUpdateAllTimeSlots: (List<TimeSlot>) -> Unit = {},
    onResetTimeSlots: () -> Unit,
    onToggleFluidCloud: (Boolean) -> Unit,
    onToggleWatchBridge: (Boolean) -> Unit,
    onChangeLeadMinutes: (Int) -> Unit,
    onSelectNotificationCardStyle: (NotificationCardStyle) -> Unit,
    onToggleAlarmClockPriority: (Boolean) -> Unit,
    onTriggerFluidCloudPreview: (LiveCapsuleOsFamily) -> Unit,
    onTestWatchBridge: () -> Unit,
    onRefreshAutomaticAlarms: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val visuals = LocalClassTrackVisuals.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollState = rememberScrollState()

    val detectedOs = remember { ColorOsFluidCloudHelper.detectDeviceOsFamily() }
    val oemGuidance = remember(detectedOs) {
        resolveOemGuidance(
            manufacturer = Build.MANUFACTURER.orEmpty(),
            brand = Build.BRAND.orEmpty(),
            detectedOs = detectedOs
        )
    }
    var selectedOsFamily by remember { mutableStateOf(LiveCapsuleOsFamily.AUTO) }

    var showTimeSlotsEditor by remember { mutableStateOf(false) }
    var editingSlot by remember { mutableStateOf<TimeSlot?>(null) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showDiagnosticsDetail by remember { mutableStateOf(false) }

    var promotedGranted by remember {
        mutableStateOf(ColorOsFluidCloudHelper.checkPromotedPermission(context))
    }
    var alarmReadiness by remember {
        mutableStateOf(CourseAlarmScheduler.inspectReadiness(context))
    }

    val livePreviewState by ColorOsFluidCloudHelper.islandPreviewState.collectAsStateWithLifecycle()
    val appVersionName = remember(context) { readAppVersionName(context) }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                promotedGranted = ColorOsFluidCloudHelper.checkPromotedPermission(context)
                alarmReadiness = CourseAlarmScheduler.inspectReadiness(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationAndLiveOk = alarmReadiness.areNotificationsEnabled &&
        (notificationCardStyle == NotificationCardStyle.DOT_CARD || promotedGranted)
    val verifiedCheckCount = listOf(
        alarmReadiness.canScheduleExactAlarms,
        alarmReadiness.isIgnoringBatteryOptimizations,
        notificationAndLiveOk
    ).count { it }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ====================================================================
        // 1. 界面风格（纯净双卡选择，去除重复顶标与孤字折行）
        // ====================================================================
        ClassTrackCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "界面风格",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "选择纯净网格排版或半透光学玻璃",
                        fontSize = 11.5.sp,
                        color = visuals.mutedTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    UiStyleOptionCard(
                        style = UiStyle.CLASSIC,
                        isSelected = uiStyle == UiStyle.CLASSIC,
                        badgeText = "EDITORIAL",
                        title = "经典纯净",
                        description = "清晰网格 · 高对比度",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { onSelectUiStyle(UiStyle.CLASSIC) }
                    )

                    UiStyleOptionCard(
                        style = UiStyle.GLASS,
                        isSelected = uiStyle == UiStyle.GLASS,
                        badgeText = "LIQUID GLASS",
                        title = "液态玻璃",
                        description = "弥散极光 · 通透磨砂",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { onSelectUiStyle(UiStyle.GLASS) }
                    )
                }
            }
        }

        // ====================================================================
        // 2. 学期与作息（高危重置收入编辑区并加弹窗确认，支持点选顺延改时）
        // ====================================================================
        ClassTrackCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "学期与作息",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "校准教学周进度与每节上下课时刻",
                        fontSize = 11.5.sp,
                        color = visuals.mutedTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                SettingStepperRow(
                    title = "当前学期周次",
                    subtitle = "课表定位与提醒基准周",
                    valueText = "第 $currentWeek 周",
                    onDecrement = { onChangeCurrentWeek((currentWeek - 1).coerceAtLeast(1)) },
                    onIncrement = { onChangeCurrentWeek((currentWeek + 1).coerceAtMost(totalWeeks)) }
                )

                SectionHairlineDivider(visuals.cardBorderColor)

                SettingStepperRow(
                    title = "学期总周数",
                    subtitle = "最大周次范围（10–25 周）",
                    valueText = "$totalWeeks 周",
                    onDecrement = { onChangeTotalWeeks((totalWeeks - 1).coerceAtLeast(10)) },
                    onIncrement = { onChangeTotalWeeks((totalWeeks + 1).coerceAtMost(25)) }
                )

                SectionHairlineDivider(visuals.cardBorderColor)

                SettingStepperRow(
                    title = "每日课程节数",
                    subtitle = "纵向时间轴节数（6–16 节）",
                    valueText = "$maxSections 节",
                    onDecrement = { onChangeMaxSections((maxSections - 1).coerceAtLeast(6)) },
                    onIncrement = { onChangeMaxSections((maxSections + 1).coerceAtMost(16)) }
                )

                SectionHairlineDivider(visuals.cardBorderColor)

                val firstSlotStart = timeSlots.firstOrNull()?.startTime ?: "08:00"
                val displayedSlots = timeSlots.take(maxSections)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (showTimeSlotsEditor) {
                                MaterialTheme.colorScheme.primary.copy(alpha = if (visuals.isDark) 0.12f else 0.08f)
                            } else {
                                Color.Transparent
                            }
                        )
                        .clickable { showTimeSlotsEditor = !showTimeSlotsEditor }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "每节课上下课时间",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${firstSlotStart} 起 · 共 $maxSections 节，点击${if (showTimeSlotsEditor) "收起" else "查看与调整"}",
                            fontSize = 11.sp,
                            color = visuals.mutedTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = if (showTimeSlotsEditor) "收起 ▴" else "展开 ▾",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                AnimatedVisibility(
                    visible = showTimeSlotsEditor,
                    enter = expandVertically(spring(dampingRatio = 0.85f, stiffness = 500f)) + fadeIn(tween(150)),
                    exit = shrinkVertically(tween(140)) + fadeOut(tween(110))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        displayedSlots.forEachIndexed { index, slot ->
                            val startMin = parseHhMmToMinutes(slot.startTime)
                            val endMin = parseHhMmToMinutes(slot.endTime)
                            val duration = if (startMin != null && endMin != null && endMin > startMin) {
                                "${endMin - startMin}分"
                            } else {
                                null
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { editingSlot = slot }
                                    .padding(horizontal = 8.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "第 ${slot.section} 节",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.width(64.dp)
                                )

                                Text(
                                    text = "${slot.startTime} — ${slot.endTime}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (duration != null) {
                                        Text(
                                            text = duration,
                                            fontSize = 11.sp,
                                            color = visuals.mutedTextColor
                                        )
                                    }
                                    Text(
                                        text = "修改 ›",
                                        fontSize = 11.5.sp,
                                        color = visuals.mutedTextColor
                                    )
                                }
                            }

                            if (index < displayedSlots.lastIndex) {
                                HorizontalDivider(
                                    thickness = 0.4.dp,
                                    color = visuals.cardBorderColor.copy(alpha = 0.28f),
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, start = 8.dp, end = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "点击任意节次即可调整时刻",
                                fontSize = 11.sp,
                                color = visuals.mutedTextColor
                            )
                            TextButton(
                                onClick = { showResetConfirmDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(28.dp),
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("恢复默认作息", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // ====================================================================
        // 3. 上课实况提醒（开关与选项优先，效果预览随开关联动并匹配主题）
        // ====================================================================
        ClassTrackCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "上课实况提醒",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "课前自动点亮卡片，上课切换倒计时",
                            fontSize = 11.5.sp,
                            color = visuals.mutedTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Switch(
                        checked = fluidCloudEnabled,
                        onCheckedChange = onToggleFluidCloud
                    )
                }

                AnimatedVisibility(
                    visible = fluidCloudEnabled,
                    enter = expandVertically(spring(dampingRatio = 0.85f, stiffness = 500f)) + fadeIn(tween(150)),
                    exit = shrinkVertically(tween(140)) + fadeOut(tween(110))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        SectionHairlineDivider(visuals.cardBorderColor)

                        // 课前提醒提前量
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "课前提醒提前量",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = if (visuals.isDark) 0.42f else 0.72f
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf(10, 15, 20, 30).forEach { mins ->
                                        val isSelected = leadMinutes == mins
                                        val chipBg = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            Color.Transparent
                                        }
                                        val chipText = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(34.dp)
                                                .clip(RoundedCornerShape(9.dp))
                                                .background(chipBg)
                                                .clickable { onChangeLeadMinutes(mins) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${mins}分钟",
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = chipText,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 通知卡片呈现样式
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "通知呈现样式",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                NotificationCardStyle.entries.forEach { styleOption ->
                                    val selected = notificationCardStyle == styleOption
                                    val border = if (selected) {
                                        BorderStroke(1.4.dp, MaterialTheme.colorScheme.primary)
                                    } else {
                                        BorderStroke(0.7.dp, visuals.cardBorderColor.copy(alpha = 0.55f))
                                    }
                                    val bg = if (selected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = if (visuals.isDark) 0.14f else 0.08f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
                                    }
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onSelectNotificationCardStyle(styleOption) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = bg,
                                        border = border
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = styleOption.displayName,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                if (selected) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(7.dp)
                                                            .clip(CircleShape)
                                                            .background(MaterialTheme.colorScheme.primary)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = styleOption.description,
                                                fontSize = 10.5.sp,
                                                color = visuals.mutedTextColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 选择状态栏实况胶囊时，展示机型兼容协议与实况权限入口
                        if (notificationCardStyle == NotificationCardStyle.LIVE_CAPSULE) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "实况胶囊协议适配",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "识别为 ${oemGuidance.osDisplayLabel}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    LiveCapsuleOsFamily.entries.forEach { family ->
                                        FilterChip(
                                            selected = selectedOsFamily == family,
                                            onClick = { selectedOsFamily = family },
                                            label = {
                                                Text(
                                                    text = if (family == LiveCapsuleOsFamily.AUTO) {
                                                        "自动识别"
                                                    } else {
                                                        "${family.displayName} · ${family.featureName.substringBefore(" ")}"
                                                    },
                                                    fontSize = 11.sp
                                                )
                                            }
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (promotedGranted) "系统实况胶囊权限已就绪" else "需开启系统实况通知权限",
                                        fontSize = 11.5.sp,
                                        color = if (promotedGranted) visuals.mutedTextColor else MaterialTheme.colorScheme.secondary
                                    )
                                    TextButton(
                                        onClick = { ColorOsFluidCloudHelper.openSystemFluidCloudSettings(context) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text(
                                            text = if (promotedGranted) "查看权限" else "去开启 ›",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // 高优先级准时唤醒
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "高优先级准时唤醒",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "息屏省电时优先触发，避免系统延迟",
                                    fontSize = 11.sp,
                                    color = visuals.mutedTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Switch(
                                checked = alarmClockPriorityEnabled,
                                onCheckedChange = onToggleAlarmClockPriority
                            )
                        }

                        SectionHairlineDivider(visuals.cardBorderColor)

                        // 实时效果预览（紧贴配置项之后，测试按钮弱化为次级工具按钮）
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (livePreviewState.isActive) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    visuals.mutedTextColor.copy(alpha = 0.65f)
                                                }
                                            )
                                    )
                                    Text(
                                        text = if (livePreviewState.isActive) "当前实况运行中" else "卡片效果预览",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            ColorOsFluidCloudHelper.cancelFluidCloudNotification(context)
                                            onShowMessage("已收起当前提醒")
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("收起", fontSize = 11.5.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            promotedGranted = ColorOsFluidCloudHelper.checkPromotedPermission(context)
                                            onTriggerFluidCloudPreview(selectedOsFamily)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("发送测试", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            NotificationIslandCarouselPreviewCard(
                                cardStyle = notificationCardStyle,
                                onConfirmArrivalClick = { isLive ->
                                    if (isLive) {
                                        ColorOsFluidCloudHelper.cancelFluidCloudNotification(context)
                                        onShowMessage("已模拟「已到教室」并收起当前通知")
                                    } else {
                                        onShowMessage("已体验「已到教室」交互（真实通知下点击将收起提醒）")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // ====================================================================
        // 4. 后台准时自检与穿戴同步（默认折叠明细 + 诚实校验 + 全品牌自适应）
        // ====================================================================
        ClassTrackCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showDiagnosticsDetail = !showDiagnosticsDetail }
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "后台运行自检",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val allReady = verifiedCheckCount == 3
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (allReady) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                } else {
                                    Color(0xFFF59E0B).copy(alpha = 0.16f)
                                }
                            ) {
                                Text(
                                    text = if (allReady) "3/3 已就绪" else "$verifiedCheckCount/3 项就绪",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (allReady) MaterialTheme.colorScheme.primary else Color(0xFFF59E0B),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = nextReminderInfo,
                            fontSize = 11.5.sp,
                            color = if (fluidCloudEnabled) MaterialTheme.colorScheme.primary else visuals.mutedTextColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = if (showDiagnosticsDetail) "收起 ▴" else "明细 ▾",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                AnimatedVisibility(
                    visible = showDiagnosticsDetail,
                    enter = expandVertically(spring(dampingRatio = 0.85f, stiffness = 500f)) + fadeIn(tween(150)),
                    exit = shrinkVertically(tween(140)) + fadeOut(tween(110))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionHairlineDivider(visuals.cardBorderColor)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "检测环境：${oemGuidance.osDisplayLabel}（点击任意项可复查）",
                                fontSize = 11.sp,
                                color = visuals.mutedTextColor,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    alarmReadiness = CourseAlarmScheduler.inspectReadiness(context)
                                    promotedGranted = ColorOsFluidCloudHelper.checkPromotedPermission(context)
                                    onRefreshAutomaticAlarms()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("刷新排程", fontSize = 11.sp)
                            }
                        }

                        BackgroundReadinessRow(
                            title = "1. 精确闹钟权限",
                            subtitle = "用于课前、上课与下课准点切换",
                            badgeText = if (alarmReadiness.canScheduleExactAlarms) "已授权" else "未开启",
                            isVerifiedOk = alarmReadiness.canScheduleExactAlarms,
                            onClick = { CourseAlarmScheduler.openExactAlarmPermissionSettings(context) }
                        )

                        BackgroundReadinessRow(
                            title = "2. 电池优化白名单",
                            subtitle = oemGuidance.batteryTip,
                            badgeText = if (alarmReadiness.isIgnoringBatteryOptimizations) "已放行" else "去放行",
                            isVerifiedOk = alarmReadiness.isIgnoringBatteryOptimizations,
                            onClick = { CourseAlarmScheduler.requestIgnoreBatteryOptimizations(context) }
                        )

                        BackgroundReadinessRow(
                            title = if (notificationCardStyle == NotificationCardStyle.LIVE_CAPSULE) {
                                "3. 系统通知与实况权限"
                            } else {
                                "3. 系统通知权限"
                            },
                            subtitle = if (notificationCardStyle == NotificationCardStyle.LIVE_CAPSULE) {
                                "控制通知显示与顶部实况胶囊弹出"
                            } else {
                                "用于发送上课卡片与震动提醒"
                            },
                            badgeText = if (notificationAndLiveOk) "已开启" else "去开启",
                            isVerifiedOk = notificationAndLiveOk,
                            onClick = { ColorOsFluidCloudHelper.openSystemFluidCloudSettings(context) }
                        )

                        // 第 4 项诚实提示：系统不向第三方 App 暴露自启动开关结果
                        BackgroundReadinessRow(
                            title = "4. 开机与跨天自启动",
                            subtitle = "${oemGuidance.autoStartTip}（系统无法自动检测）",
                            badgeText = "前往确认",
                            isVerifiedOk = null,
                            onClick = { CourseAlarmScheduler.openOemAutoStartSettings(context) }
                        )
                    }
                }

                SectionHairlineDivider(visuals.cardBorderColor)

                // 手表与手环同步
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "手表与手环同步",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "开课前手腕震动提醒并推送课程通知",
                            fontSize = 11.sp,
                            color = visuals.mutedTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Switch(
                        checked = watchBridgeEnabled,
                        onCheckedChange = onToggleWatchBridge
                    )
                }

                AnimatedVisibility(
                    visible = watchBridgeEnabled,
                    enter = expandVertically(spring(dampingRatio = 0.85f, stiffness = 500f)) + fadeIn(tween(150)),
                    exit = shrinkVertically(tween(140)) + fadeOut(tween(110))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = oemGuidance.watchCompanionTip,
                            fontSize = 11.sp,
                            color = visuals.mutedTextColor,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )
                        OutlinedButton(
                            onClick = onTestWatchBridge,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("测试震动", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 底部安静收口信息
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "课轨 ClassTrack · v$appVersionName",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = visuals.mutedTextColor.copy(alpha = 0.85f)
            )
            Text(
                text = "课表与主题设置仅保存在本地设备",
                fontSize = 10.5.sp,
                color = visuals.mutedTextColor.copy(alpha = 0.6f)
            )
        }
    }

    // 恢复默认作息二次确认弹窗
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = {
                Text(
                    text = "恢复默认作息时间？",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("将把每日总节数还原为 12 节，并覆盖你手动修改过的所有上下课时间。已有课程不会受影响。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirmDialog = false
                        onResetTimeSlots()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("确认恢复", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 单节作息修改弹窗（支持点击选时、快捷课长、同步顺延后续节次）
    val currentEditing = editingSlot
    if (currentEditing != null) {
        TimeSlotEditDialog(
            slot = currentEditing,
            maxSections = maxSections,
            onDismiss = { editingSlot = null },
            onSave = { startStr, endStr, cascade ->
                if (cascade) {
                    val updated = applyTimeSlotUpdate(
                        slots = timeSlots,
                        section = currentEditing.section,
                        newStart = startStr,
                        newEnd = endStr,
                        cascadeSubsequent = true
                    )
                    onUpdateAllTimeSlots(updated)
                } else {
                    onUpdateTimeSlot(currentEditing.section, startStr, endStr)
                }
                editingSlot = null
            }
        )
    }
}

@Composable
private fun SectionHairlineDivider(borderColor: Color) {
    HorizontalDivider(
        thickness = 0.5.dp,
        color = borderColor.copy(alpha = 0.42f)
    )
}

@Composable
private fun SettingStepperRow(
    title: String,
    subtitle: String,
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    val visuals = LocalClassTrackVisuals.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = visuals.mutedTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (visuals.isGlass) 0.55f else 0.85f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDecrement),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "−",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = valueText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    modifier = Modifier.width(58.dp),
                    textAlign = TextAlign.Center
                )

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onIncrement),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * 权限自检行：所有条目始终可点击前往系统设置复查；第 4 项诚实标识为「前往确认」
 */
@Composable
private fun BackgroundReadinessRow(
    title: String,
    subtitle: String,
    badgeText: String,
    isVerifiedOk: Boolean?,
    onClick: () -> Unit
) {
    val visuals = LocalClassTrackVisuals.current
    val badgeBg = when (isVerifiedOk) {
        true -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        false -> Color(0xFFF59E0B).copy(alpha = 0.16f)
        null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (visuals.isDark) 0.55f else 0.85f)
    }
    val badgeColor = when (isVerifiedOk) {
        true -> MaterialTheme.colorScheme.primary
        false -> Color(0xFFF59E0B)
        null -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 10.5.sp,
                color = visuals.mutedTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = badgeBg
        ) {
            Text(
                text = if (isVerifiedOk == true) "✓ $badgeText ›" else "$badgeText ›",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = badgeColor,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * 单节作息时间调节弹窗：步进调时 + 快捷课长 + 后续节次顺延
 */
@Composable
private fun TimeSlotEditDialog(
    slot: TimeSlot,
    maxSections: Int,
    onDismiss: () -> Unit,
    onSave: (startTime: String, endTime: String, cascadeSubsequent: Boolean) -> Unit
) {
    val initialStart = remember(slot) { parseHhMmToMinutes(slot.startTime) ?: (8 * 60) }
    val initialEnd = remember(slot) {
        (parseHhMmToMinutes(slot.endTime) ?: (initialStart + 45)).coerceAtLeast(initialStart + 5)
    }

    var startMinutes by remember(slot) { mutableIntStateOf(initialStart) }
    var endMinutes by remember(slot) { mutableIntStateOf(initialEnd) }
    var editingStart by remember(slot) { mutableStateOf(true) }
    var cascadeSubsequent by remember(slot) { mutableStateOf(false) }

    val startStr = formatMinutesToHhMm(startMinutes)
    val endStr = formatMinutesToHhMm(endMinutes)
    val isValid = endMinutes > startMinutes
    val duration = (endMinutes - startMinutes).coerceAtLeast(0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "第 ${slot.section} 节作息时间",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isValid) "${duration}分钟" else "时间无效",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimeFieldSelectorBox(
                        label = "上课时刻",
                        value = startStr,
                        selected = editingStart,
                        modifier = Modifier.weight(1f),
                        onClick = { editingStart = true }
                    )
                    TimeFieldSelectorBox(
                        label = "下课时刻",
                        value = endStr,
                        selected = !editingStart,
                        modifier = Modifier.weight(1f),
                        onClick = { editingStart = false }
                    )
                }

                val targetMin = if (editingStart) startMinutes else endMinutes
                val adjustMinutes: (Int) -> Unit = { delta ->
                    val nextVal = (targetMin + delta).coerceIn(0, 23 * 60 + 59)
                    if (editingStart) {
                        val oldDur = (endMinutes - startMinutes).coerceAtLeast(15)
                        startMinutes = nextVal
                        if (endMinutes <= startMinutes) {
                            endMinutes = (startMinutes + oldDur).coerceAtMost(23 * 60 + 59)
                        }
                    } else {
                        endMinutes = nextVal
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (editingStart) "微调上课时刻" else "微调下课时刻",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(-60 to "-1时", -5 to "-5分", -1 to "-1分", 1 to "+1分", 5 to "+5分", 60 to "+1时")
                            .forEach { (delta, label) ->
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { adjustMinutes(delta) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 7.dp)
                                    )
                                }
                            }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "按单节时长设置下课",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(40, 45, 50, 90).forEach { preset ->
                            val active = duration == preset
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        endMinutes = (startMinutes + preset).coerceAtMost(23 * 60 + 59)
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = if (active) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                },
                                border = if (active) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Text(
                                    text = "${preset}分",
                                    fontSize = 11.5.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                if (!isValid) {
                    Text(
                        text = "下课时刻必须晚于上课时刻",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (slot.section < maxSections) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { cascadeSubsequent = !cascadeSubsequent }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "同步顺延第 ${slot.section + 1}–$maxSections 节",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "保持后续节次与课间间隔不变",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = cascadeSubsequent,
                            onCheckedChange = { cascadeSubsequent = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(startStr, endStr, cascadeSubsequent) },
                enabled = isValid
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun TimeFieldSelectorBox(
    label: String,
    value: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
        border = if (selected) BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                fontSize = 10.5.sp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * 独立重组作用域的通知预览组件：
 * - 配色自动同步当前主题强调色（保证暗底可辨识度）
 * - 右下角「已到教室」真正可交互
 * - 静态示例态显示「示例」标识与真实进度演练，不再出现死点阵或假按钮
 */
@Composable
private fun NotificationIslandCarouselPreviewCard(
    cardStyle: NotificationCardStyle,
    onConfirmArrivalClick: (isLiveActive: Boolean) -> Unit
) {
    val visuals = LocalClassTrackVisuals.current
    val livePreviewState by ColorOsFluidCloudHelper.islandPreviewState.collectAsStateWithLifecycle()
    var isExpanded by remember { mutableStateOf(false) }
    var demoArrived by remember { mutableStateOf(false) }

    val themePrimary = MaterialTheme.colorScheme.primary
    val previewAccent = remember(themePrimary) { computePreviewAccentColor(themePrimary) }

    var demoStep by remember { mutableIntStateOf(0) }
    LaunchedEffect(livePreviewState.isActive, cardStyle) {
        if (!livePreviewState.isActive) {
            while (true) {
                delay(3500L)
                demoStep = (demoStep + 1) % 3
                if (demoStep == 0) demoArrived = false
            }
        }
    }

    if (cardStyle == NotificationCardStyle.DOT_CARD) {
        val isActiveLive = livePreviewState.isActive
        val headline = if (isActiveLive) {
            val validRoom = livePreviewState.roomName.takeIf { it.isNotBlank() && it != "待定教室" }
            if (validRoom != null) "${livePreviewState.timeRange}  $validRoom" else livePreviewState.timeRange
        } else {
            "18:30-20:05  二教208"
        }
        val courseLine = if (isActiveLive && livePreviewState.fullCourseName.isNotBlank()) {
            livePreviewState.fullCourseName
        } else {
            "无机及分析化学 · 第10-11节"
        }
        val statusText = when {
            isActiveLive && livePreviewState.progressPercent > 0 ->
                livePreviewState.fullMarqueeText.substringAfterLast("· ").ifBlank { "正在上课" }
            isActiveLive -> "即将上课"
            demoArrived -> "已到达教室 · 祝听课顺利"
            demoStep == 0 -> "6分钟后上课"
            demoStep == 1 -> "78分钟后下课"
            else -> "36分钟后下课"
        }
        val progressRatio = when {
            isActiveLive && livePreviewState.progressPercent > 0 ->
                (livePreviewState.progressPercent / 100f).coerceIn(0f, 1f)
            isActiveLive -> 0.04f
            demoStep == 0 -> 0.08f
            demoStep == 1 -> 0.28f
            else -> 0.68f
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (visuals.isDark) Color(0xFF171B26) else Color(0xFF1F1D24),
            border = BorderStroke(0.8.dp, previewAccent.copy(alpha = 0.24f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = headline,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF8FAFC),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (!isActiveLive) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "示例",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFCBD5E1),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF242A38),
                        border = BorderStroke(0.6.dp, previewAccent.copy(alpha = 0.45f)),
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0F172A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(previewAccent)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = courseLine,
                    fontSize = 12.sp,
                    color = Color(0xFFA6B0C0),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val dotCount = 28
                val litDots = if (progressRatio <= 0f) 0 else (progressRatio * dotCount).toInt().coerceIn(1, dotCount)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .height(10.dp)
                ) {
                    val radius = 2.6.dp.toPx()
                    val startX = radius + 1f
                    val usableW = (size.width - startX * 2f).coerceAtLeast(1f)
                    val stepX = usableW / (dotCount - 1)
                    val cy = size.height / 2f
                    for (i in 0 until dotCount) {
                        val cx = startX + i * stepX
                        if (i < litDots) {
                            if (i == litDots - 1) {
                                drawCircle(
                                    color = previewAccent.copy(alpha = 0.35f),
                                    radius = radius * 1.55f,
                                    center = Offset(cx, cy)
                                )
                            }
                            drawCircle(
                                color = previewAccent,
                                radius = radius,
                                center = Offset(cx, cy)
                            )
                        } else {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.20f),
                                radius = radius * 0.9f,
                                center = Offset(cx, cy)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedContent(
                        targetState = statusText,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                        label = "DotCardStatusAnim"
                    ) { label ->
                        Text(
                            text = label,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = previewAccent,
                            maxLines = 1
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (demoArrived && !isActiveLive) {
                            previewAccent.copy(alpha = 0.22f)
                        } else {
                            Color.White.copy(alpha = 0.13f)
                        },
                        border = BorderStroke(
                            0.6.dp,
                            if (demoArrived && !isActiveLive) previewAccent.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.18f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                if (!isActiveLive) {
                                    demoArrived = !demoArrived
                                }
                                onConfirmArrivalClick(isActiveLive)
                            }
                    ) {
                        Text(
                            text = if (demoArrived && !isActiveLive) "✓ 已确认" else "已到教室",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE2E8F0),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
        return
    }

    val demoUnit = when (demoStep % 3) {
        0 -> "高等数学"
        1 -> "二教209"
        else -> "6m上课"
    }

    val displayCapsuleText = if (livePreviewState.isActive && livePreviewState.islandCapsuleText.isNotBlank()) {
        livePreviewState.islandCapsuleText
    } else {
        demoUnit
    }

    val cardHeadline = if (livePreviewState.isActive) {
        val validRoom = livePreviewState.roomName.takeIf { it.isNotBlank() && it != "待定教室" }
        if (validRoom != null) "${livePreviewState.timeRange} · $validRoom" else livePreviewState.timeRange
    } else {
        "18:30-20:05 · 二教208"
    }

    val cardCourseSub = if (livePreviewState.isActive && livePreviewState.fullCourseName.isNotBlank()) {
        livePreviewState.fullCourseName
    } else {
        "无机及分析化学"
    }

    val cardStatusBadge = if (livePreviewState.isActive && livePreviewState.capsuleRightValue.isNotBlank()) {
        livePreviewState.capsuleRightValue
    } else {
        "6m上课"
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (visuals.isDark) Color(0xFF171B26) else Color(0xFF1F1D24),
        border = BorderStroke(0.8.dp, previewAccent.copy(alpha = 0.24f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (livePreviewState.isActive) previewAccent else Color.White.copy(alpha = 0.45f))
                    )
                    Text(
                        text = if (livePreviewState.isActive) "状态栏胶囊已挂载" else "顶部状态栏胶囊示例",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (livePreviewState.isActive) previewAccent else Color(0xFFCBD5E1)
                    )
                }

                Text(
                    text = if (isExpanded) "收起详情" else "展开详情",
                    fontSize = 11.sp,
                    color = previewAccent,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { isExpanded = !isExpanded }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            val pillInteractionSource = remember { MutableInteractionSource() }
            val isPillPressed by pillInteractionSource.collectIsPressedAsState()
            val pillScale by animateFloatAsState(
                targetValue = if (isPillPressed) 0.95f else 1f,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 600f),
                label = "IslandPillScale"
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0A0E17),
                border = BorderStroke(1.dp, previewAccent.copy(alpha = 0.65f)),
                modifier = Modifier
                    .width(156.dp)
                    .height(32.dp)
                    .graphicsLayer {
                        scaleX = pillScale
                        scaleY = pillScale
                    }
                    .clickable(
                        interactionSource = pillInteractionSource,
                        indication = null,
                        onClick = { isExpanded = !isExpanded }
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(previewAccent)
                    )

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = displayCapsuleText,
                            transitionSpec = {
                                fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                            },
                            label = "CapsuleUnitSwitch"
                        ) { unitText ->
                            Text(
                                text = unitText,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Text(
                        text = if (isExpanded) "▴" else "▾",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(spring(dampingRatio = 0.85f, stiffness = 500f)) + fadeIn(tween(150)),
                exit = shrinkVertically(tween(140)) + fadeOut(tween(110))
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(0.8.dp, previewAccent.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = cardHeadline,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                            Text(
                                text = cardStatusBadge,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = previewAccent
                            )
                        }
                        Text(
                            text = cardCourseSub,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.78f),
                            maxLines = 1
                        )
                        LinearProgressIndicator(
                            progress = { if (livePreviewState.isActive) livePreviewState.progressPercent / 100f else 0.28f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = previewAccent,
                            trackColor = Color.White.copy(alpha = 0.15f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UiStyleOptionCard(
    style: UiStyle,
    isSelected: Boolean,
    badgeText: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val visuals = LocalClassTrackVisuals.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 600f),
        label = "UiStyleCardScale"
    )

    val accentColor = MaterialTheme.colorScheme.primary
    val borderStroke = if (isSelected) {
        BorderStroke(1.5.dp, accentColor)
    } else {
        BorderStroke(0.7.dp, visuals.cardBorderColor.copy(alpha = 0.6f))
    }

    val containerBg = if (isSelected) {
        accentColor.copy(alpha = if (visuals.isDark) 0.14f else 0.08f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    }

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(14.dp),
        color = containerBg,
        border = borderStroke
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = badgeText,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    color = if (isSelected) accentColor else visuals.mutedTextColor
                )

                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.2.dp,
                            color = if (isSelected) accentColor else visuals.mutedTextColor.copy(alpha = 0.5f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                    }
                }
            }

            // 极简风格微缩示意图（一眼分辨纯色网格 vs 光学玻璃）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                val sampleTint = if (style == UiStyle.GLASS) {
                    accentColor.copy(alpha = 0.28f)
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(if (style == UiStyle.GLASS) 5.dp else 2.dp))
                        .background(sampleTint)
                )
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(if (style == UiStyle.GLASS) 5.dp else 2.dp))
                        .background(accentColor.copy(alpha = if (style == UiStyle.GLASS) 0.55f else 0.72f))
                )
            }

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            Text(
                text = description,
                fontSize = 10.5.sp,
                color = visuals.mutedTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

internal data class OemGuidance(
    val osDisplayLabel: String,
    val batteryTip: String,
    val autoStartTip: String,
    val watchCompanionTip: String
)

internal fun resolveOemGuidance(
    manufacturer: String,
    brand: String,
    detectedOs: LiveCapsuleOsFamily
): OemGuidance {
    val combined = "$manufacturer $brand".lowercase(Locale.ROOT)
    val isOplus = combined.contains("oppo") || combined.contains("oneplus") || combined.contains("realme")
    val isXiaomi = combined.contains("xiaomi") || combined.contains("redmi") || combined.contains("poco") ||
        detectedOs == LiveCapsuleOsFamily.HYPEROS
    val isVivo = combined.contains("vivo") || combined.contains("iqoo") ||
        detectedOs == LiveCapsuleOsFamily.ORIGINOS
    val isHonorOrHuawei = combined.contains("honor") || combined.contains("huawei") ||
        detectedOs == LiveCapsuleOsFamily.MAGICOS

    return when {
        isOplus -> OemGuidance(
            osDisplayLabel = "ColorOS · OPPO / 一加",
            batteryTip = "防止系统后台冻结休眠",
            autoStartTip = "在「手机管家」允许课轨自启动",
            watchCompanionTip = "请在「欢太健康」或手表通知设置中勾选「课轨」"
        )
        isXiaomi -> OemGuidance(
            osDisplayLabel = "HyperOS · 小米 / Redmi",
            batteryTip = "建议将省电策略设为无限制",
            autoStartTip = "在「手机管家」开启课轨自启动",
            watchCompanionTip = "请在「小米运动健康」消息通知中勾选「课轨」"
        )
        isVivo -> OemGuidance(
            osDisplayLabel = "OriginOS · vivo / iQOO",
            batteryTip = "允许后台高耗电防清理",
            autoStartTip = "在「i 管家」开启课轨自启动",
            watchCompanionTip = "请在「vivo 健康」消息通知中勾选「课轨」"
        )
        isHonorOrHuawei -> OemGuidance(
            osDisplayLabel = "MagicOS / 华为",
            batteryTip = "应用启动管理建议设为允许后台",
            autoStartTip = "在「手机管家」允许课轨自启动",
            watchCompanionTip = "请在「运动健康」消息通知中勾选「课轨」"
        )
        else -> OemGuidance(
            osDisplayLabel = "Android 设备",
            batteryTip = "防止省电模式休眠后台闹钟",
            autoStartTip = "请在系统应用设置中允许后台运行",
            watchCompanionTip = "请在手表 / Wear OS 配套应用通知中勾选「课轨」"
        )
    }
}

internal fun parseHhMmToMinutes(time: String): Int? {
    val parts = time.trim().split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}

internal fun formatMinutesToHhMm(totalMinutes: Int): String {
    val clamped = totalMinutes.coerceIn(0, 23 * 60 + 59)
    val h = clamped / 60
    val m = clamped % 60
    return String.format(Locale.US, "%02d:%02d", h, m)
}

internal fun applyTimeSlotUpdate(
    slots: List<TimeSlot>,
    section: Int,
    newStart: String,
    newEnd: String,
    cascadeSubsequent: Boolean
): List<TimeSlot> {
    val targetSlot = slots.firstOrNull { it.section == section } ?: return slots
    val oldEndMin = parseHhMmToMinutes(targetSlot.endTime)
    val newEndMin = parseHhMmToMinutes(newEnd)
    val deltaMinutes = if (cascadeSubsequent && oldEndMin != null && newEndMin != null) {
        newEndMin - oldEndMin
    } else {
        0
    }

    return slots.map { slot ->
        when {
            slot.section == section -> slot.copy(startTime = newStart, endTime = newEnd)
            cascadeSubsequent && slot.section > section && deltaMinutes != 0 -> {
                val origStart = parseHhMmToMinutes(slot.startTime)
                val origEnd = parseHhMmToMinutes(slot.endTime)
                if (origStart != null && origEnd != null) {
                    val shiftedStart = (origStart + deltaMinutes).coerceIn(0, 23 * 60 + 58)
                    val dur = (origEnd - origStart).coerceAtLeast(1)
                    val shiftedEnd = (shiftedStart + dur).coerceAtMost(23 * 60 + 59)
                    slot.copy(
                        startTime = formatMinutesToHhMm(shiftedStart),
                        endTime = formatMinutesToHhMm(shiftedEnd)
                    )
                } else {
                    slot
                }
            }
            else -> slot
        }
    }
}

private fun computePreviewAccentColor(themePrimary: Color): Color {
    return if (themePrimary.luminance() < 0.28f) {
        lerp(themePrimary, Color.White, 0.28f)
    } else {
        themePrimary
    }
}

private fun readAppVersionName(context: Context): String = runCatching {
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.PackageInfoFlags.of(0)
        )
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    info.versionName.orEmpty().ifBlank { "1.0.0" }
}.getOrDefault("1.0.0")
