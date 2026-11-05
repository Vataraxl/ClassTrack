package com.classtrack.app.ui.main

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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classtrack.app.data.model.TimeSlot
import com.classtrack.app.data.model.UiStyle
import com.classtrack.app.system.ColorOsFluidCloudHelper
import com.classtrack.app.system.LiveCapsuleOsFamily
import com.classtrack.app.ui.theme.ClassTrackCard
import com.classtrack.app.ui.theme.ClassTrackSubSurface
import com.classtrack.app.ui.theme.LocalClassTrackVisuals
import kotlinx.coroutines.delay

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
    nextReminderInfo: String,
    onSelectUiStyle: (UiStyle) -> Unit,
    onChangeCurrentWeek: (Int) -> Unit,
    onChangeTotalWeeks: (Int) -> Unit,
    onChangeMaxSections: (Int) -> Unit,
    onUpdateTimeSlot: (section: Int, startTime: String, endTime: String) -> Unit,
    onResetTimeSlots: () -> Unit,
    onToggleFluidCloud: (Boolean) -> Unit,
    onToggleWatchBridge: (Boolean) -> Unit,
    onChangeLeadMinutes: (Int) -> Unit,
    onTriggerFluidCloudPreview: (LiveCapsuleOsFamily) -> Unit,
    onTestWatchBridge: () -> Unit,
    onRefreshAutomaticAlarms: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val visuals = LocalClassTrackVisuals.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val detectedOs = remember { ColorOsFluidCloudHelper.detectDeviceOsFamily() }
    var selectedOsFamily by remember { mutableStateOf(LiveCapsuleOsFamily.AUTO) }
    var showTimeSlotsEditor by remember { mutableStateOf(false) }

    var promotedGranted by remember {
        mutableStateOf(ColorOsFluidCloudHelper.checkPromotedPermission(context))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ====================================================================
        // 1. 视觉设计语言切换（经典纯净 vs 液态玻璃）
        // ====================================================================
        ClassTrackCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "视觉设计语言",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "切换瑞士现代纯净网格与光学半透磨砂玻璃",
                            fontSize = 11.5.sp,
                            color = visuals.mutedTextColor
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = if (uiStyle == UiStyle.GLASS) "LIQUID GLASS" else "EDITORIAL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    UiStyleOptionCard(
                        style = UiStyle.CLASSIC,
                        isSelected = uiStyle == UiStyle.CLASSIC,
                        badgeText = "EDITORIAL",
                        title = "经典纯净",
                        description = "瑞士网格排版 · 柔和色块 · 零视觉噪点",
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectUiStyle(UiStyle.CLASSIC) }
                    )

                    UiStyleOptionCard(
                        style = UiStyle.GLASS,
                        isSelected = uiStyle == UiStyle.GLASS,
                        badgeText = "LIQUID GLASS",
                        title = "液态玻璃",
                        description = "环境漫射极光 · 通透磨砂 · 柔光折射",
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectUiStyle(UiStyle.GLASS) }
                    )
                }
            }
        }

        // ====================================================================
        // 2. 课表周次与每日作息时间（扁平分组列表，消除嵌套盒中盒）
        // ====================================================================
        ClassTrackCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "学期周次与作息",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = onResetTimeSlots,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("恢复默认作息", fontSize = 11.5.sp)
                    }
                }

                // 行 1：当前学期周次
                SettingStepperRow(
                    title = "当前学期周次",
                    subtitle = "主页默认定位与课前提醒基准周",
                    valueText = "第 $currentWeek 周",
                    onDecrement = { onChangeCurrentWeek((currentWeek - 1).coerceAtLeast(1)) },
                    onIncrement = { onChangeCurrentWeek((currentWeek + 1).coerceAtMost(totalWeeks)) }
                )

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = visuals.cardBorderColor.copy(alpha = 0.45f)
                )

                // 行 2：学期总周数
                SettingStepperRow(
                    title = "学期总周数",
                    subtitle = "本学期课表横滑最大周数（10 ~ 25 周）",
                    valueText = "$totalWeeks 周",
                    onDecrement = { onChangeTotalWeeks((totalWeeks - 1).coerceAtLeast(10)) },
                    onIncrement = { onChangeTotalWeeks((totalWeeks + 1).coerceAtMost(25)) }
                )

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = visuals.cardBorderColor.copy(alpha = 0.45f)
                )

                // 行 3：每日最大节数
                SettingStepperRow(
                    title = "每日课程总节数",
                    subtitle = "课表纵向时间轴节数（6 ~ 16 节）",
                    valueText = "$maxSections 节",
                    onDecrement = { onChangeMaxSections((maxSections - 1).coerceAtLeast(6)) },
                    onIncrement = { onChangeMaxSections((maxSections + 1).coerceAtMost(16)) }
                )

                // 作息时间折叠展开按钮
                ClassTrackSubSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    onClick = { showTimeSlotsEditor = !showTimeSlotsEditor }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showTimeSlotsEditor) "收起每节上下课时间表" else "自定义每节课上下课时间",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (showTimeSlotsEditor) "▴" else "▾",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showTimeSlotsEditor,
                    enter = expandVertically(spring(dampingRatio = 0.85f, stiffness = 500f)) + fadeIn(tween(150)),
                    exit = shrinkVertically(tween(140)) + fadeOut(tween(110))
                ) {
                    Column(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        timeSlots.take(maxSections).forEach { slot ->
                            var startInput by remember(slot.startTime) { mutableStateOf(slot.startTime) }
                            var endInput by remember(slot.endTime) { mutableStateOf(slot.endTime) }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "第${slot.section}节",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.width(44.dp)
                                )
                                OutlinedTextField(
                                    value = startInput,
                                    onValueChange = { startInput = it },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    label = { Text("上课", fontSize = 10.sp) }
                                )
                                OutlinedTextField(
                                    value = endInput,
                                    onValueChange = { endInput = it },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    label = { Text("下课", fontSize = 10.sp) }
                                )
                                Button(
                                    onClick = {
                                        onUpdateTimeSlot(slot.section, startInput, endInput)
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("保存", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ====================================================================
        // 3. 流体云与状态栏实况胶囊设置（独立局部作用域轮播，绝不触发整页重组）
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
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "流体云与状态栏实况胶囊",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "课前状态栏定宽胶囊常驻，轮流展示时间、课程与教室",
                            fontSize = 11.5.sp,
                            color = visuals.mutedTextColor
                        )
                    }
                    Switch(
                        checked = fluidCloudEnabled,
                        onCheckedChange = onToggleFluidCloud
                    )
                }

                // 独立状态作用域的状态栏胶囊预览组件
                NotificationIslandCarouselPreviewCard()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            promotedGranted = ColorOsFluidCloudHelper.checkPromotedPermission(context)
                            onTriggerFluidCloudPreview(selectedOsFamily)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text("发送实况预览", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            ColorOsFluidCloudHelper.cancelFluidCloudNotification(context)
                            onShowMessage("已收起实况胶囊")
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text("收起提醒", fontSize = 12.5.sp)
                    }
                }

                AnimatedVisibility(
                    visible = fluidCloudEnabled,
                    enter = expandVertically(spring(dampingRatio = 0.85f, stiffness = 500f)) + fadeIn(tween(150)),
                    exit = shrinkVertically(tween(140)) + fadeOut(tween(110))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = visuals.cardBorderColor.copy(alpha = 0.45f)
                        )

                        // 提前提醒时间（四等分对称胶囊选择器，严防第4项被挤压变形）
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "课前提醒提前量",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            ClassTrackSubSurface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
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

                        // 全自动后台排程状态
                        ClassTrackSubSurface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "RTC 硬件级后台自动唤醒",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    TextButton(
                                        onClick = onRefreshAutomaticAlarms,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Text("刷新排程", fontSize = 11.sp)
                                    }
                                }
                                Text(
                                    text = nextReminderInfo,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "无需后台常驻，到点自动唤醒系统状态栏弹出实况胶囊。",
                                    fontSize = 10.5.sp,
                                    color = visuals.mutedTextColor
                                )
                            }
                        }

                        // 智能手表端适配
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "智能手表同步 (OPPO Watch X2 / Wear OS)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "手腕脉冲震动提醒与抬腕课程卡片同步",
                                    fontSize = 11.sp,
                                    color = visuals.mutedTextColor
                                )
                            }
                            Switch(
                                checked = watchBridgeEnabled,
                                onCheckedChange = onToggleWatchBridge
                            )
                        }

                        if (watchBridgeEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "请在「欢太健康」➔ 消息通知 中勾选「课轨」",
                                    fontSize = 11.sp,
                                    color = visuals.mutedTextColor,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    onClick = onTestWatchBridge,
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("测试震动", fontSize = 11.sp)
                                }
                            }
                        }

                        // 实况胶囊协议选择
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "系统胶囊协议",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "当前设备: ${detectedOs.displayName}",
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
                                                    "${family.displayName} ${family.featureName.substringBefore(" ")}"
                                                },
                                                fontSize = 11.sp
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        // 系统权限快捷入口
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
                                    text = if (promotedGranted) "权限设置" else "去开启",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

/**
 * 极简一体式胶囊步进控制器行（替代原先生硬杂乱的多个 OutlinedButton）
 */
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
            modifier = Modifier.weight(1f),
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
                color = visuals.mutedTextColor
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
                    modifier = Modifier.width(56.dp),
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
 * 独立重组作用域的状态栏实况胶囊预览组件：
 * 与系统状态栏胶囊 1:1 真实对齐（去除底部虚假跑马灯，采用 4.0s 定宽等长短词轮换）
 */
@Composable
private fun NotificationIslandCarouselPreviewCard() {
    val visuals = LocalClassTrackVisuals.current
    val livePreviewState by ColorOsFluidCloudHelper.islandPreviewState.collectAsStateWithLifecycle()
    var isExpanded by remember { mutableStateOf(false) }

    var demoStep by remember { mutableIntStateOf(0) }
    LaunchedEffect(livePreviewState.isActive) {
        if (!livePreviewState.isActive) {
            while (true) {
                delay(4000L)
                demoStep = (demoStep + 1) % 3
            }
        }
    }

    val demoUnit = when (demoStep % 3) {
        0 -> "高等数学"
        1 -> "二教209"
        else -> "08:00开课"
    }

    val displayCapsuleText = if (livePreviewState.isActive && livePreviewState.islandCapsuleText.isNotBlank()) {
        livePreviewState.islandCapsuleText
    } else {
        demoUnit
    }

    val cardCourseTitle = if (livePreviewState.isActive && livePreviewState.fullCourseName.isNotBlank()) {
        livePreviewState.fullCourseName
    } else {
        "高等数学"
    }

    val cardSubtitle = if (livePreviewState.isActive) {
        val validRoom = livePreviewState.roomName.takeIf { it.isNotBlank() && it != "待定教室" }
        if (validRoom != null) {
            "$validRoom · ${livePreviewState.timeRange}"
        } else {
            livePreviewState.timeRange
        }
    } else {
        "二教 209 · 08:00 - 09:35"
    }

    val cardStatusBadge = if (livePreviewState.isActive && livePreviewState.capsuleRightValue.isNotBlank()) {
        livePreviewState.capsuleRightValue
    } else {
        "15m后上课"
    }

    ClassTrackSubSurface(
        shape = RoundedCornerShape(14.dp),
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
                            .background(
                                if (livePreviewState.isActive) MaterialTheme.colorScheme.primary
                                else visuals.mutedTextColor.copy(alpha = 0.6f)
                            )
                    )
                    Text(
                        text = if (livePreviewState.isActive) "状态栏胶囊已挂载" else "状态栏实况胶囊样式",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (livePreviewState.isActive) MaterialTheme.colorScheme.primary else visuals.mutedTextColor
                    )
                }

                Text(
                    text = if (isExpanded) "收起卡片" else "展开通知卡片",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { isExpanded = !isExpanded }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // 1:1 还原系统状态栏定宽黑色胶囊 (156.dp)
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
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)),
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
                            .background(MaterialTheme.colorScheme.primary)
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
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
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
                                text = cardCourseTitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                            Text(
                                text = cardStatusBadge,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = cardSubtitle,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.78f),
                            maxLines = 1
                        )
                        LinearProgressIndicator(
                            progress = { if (livePreviewState.isActive) livePreviewState.progressPercent / 100f else 0.35f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.White.copy(alpha = 0.15f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 现代极简视觉风格选择卡（去除俗气的彩虹渐变描边与 Emoji，采用几何微缩示意与克制高光）
 */
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
        accentColor.copy(alpha = if (visuals.isDark) 0.12f else 0.08f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
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
                .fillMaxWidth()
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

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = description,
                fontSize = 10.5.sp,
                color = visuals.mutedTextColor,
                lineHeight = 14.sp
            )
        }
    }
}
