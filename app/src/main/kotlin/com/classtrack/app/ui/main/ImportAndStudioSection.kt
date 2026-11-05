package com.classtrack.app.ui.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.classtrack.app.data.CourseImporter
import com.classtrack.app.data.DataRepository
import com.classtrack.app.data.model.ArtistTheme
import com.classtrack.app.data.model.CustomThemeConfig
import com.classtrack.app.data.model.DefaultPresets
import com.classtrack.app.data.model.MusicGenre
import com.classtrack.app.data.model.TimeSlot
import com.classtrack.app.system.ColorOsFluidCloudHelper
import com.classtrack.app.system.LiveCapsuleOsFamily
import com.classtrack.app.ui.theme.ClassTrackCard
import com.classtrack.app.ui.theme.ClassTrackSubSurface
import com.classtrack.app.ui.theme.LocalClassTrackVisuals
import com.classtrack.app.ui.theme.ZtmyPrimary
import com.classtrack.app.ui.theme.ZtmySecondary
import com.classtrack.app.ui.theme.ZtmyTertiary
import com.classtrack.app.ui.theme.parseHexColor
import com.classtrack.app.ui.theme.themePreviewSwatches
import java.io.File

@Composable
fun ImportCenterSection(
    selectedPortalId: String,
    recentPortalIds: List<String>,
    onSelectPortal: (String) -> Unit,
    onImportRawText: (String) -> Unit,
    onApplyImportResult: (CourseImporter.ImportResult) -> Unit,
    onExportBackupJson: () -> String,
    onExportShareToken: () -> String,
    onBuildDeepSeekSchedulePrompt: () -> String,
    onClearAllCourses: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val visuals = LocalClassTrackVisuals.current
    val context = LocalContext.current

    val portals = DefaultPresets.universityPortals
    val primaryPortal = remember(selectedPortalId, portals) {
        portals.firstOrNull { it.id == selectedPortalId } ?: portals.first()
    }

    var eduUrl by remember(primaryPortal.url) { mutableStateOf("") }
    var showUniversitySubPage by remember { mutableStateOf(false) }
    var showWebViewDialog by remember { mutableStateOf(false) }
    var webViewTargetUrl by remember { mutableStateOf("") }
    var showManualPasteBox by remember { mutableStateOf(false) }
    var rawTextInput by remember { mutableStateOf("") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // 保留 2 个固定快捷高校按钮（优先展示最近使用或代表高校，不依赖横向滚动）
    val quickPortals = remember(recentPortalIds, primaryPortal.id, portals) {
        val recentList = recentPortalIds.mapNotNull { id -> portals.firstOrNull { it.id == id } }
        (recentList + portals).distinctBy { it.id }.take(2)
    }

    val scrollState = rememberScrollState()

    val openInAppWebView: (String) -> Unit = { targetUrl ->
        val trimmed = targetUrl.trim().replace("newjwxt.bjfu.cn", "newjwxt.bjfu.edu.cn")
        val normalized = when {
            trimmed.isEmpty() -> ""
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.contains("newjwxt.bjfu.edu.cn") -> "http://$trimmed"
            else -> "https://$trimmed"
        }
        webViewTargetUrl = normalized
        showWebViewDialog = true
    }

    if (showUniversitySubPage) {
        UniversityPickerSubPageDialog(
            universityPortals = portals,
            selectedPortalId = primaryPortal.id,
            onDismiss = { showUniversitySubPage = false },
            onSelectPortal = { preset ->
                onSelectPortal(preset.id)
                eduUrl = preset.url
                onShowMessage("已选择「${preset.schoolName}」")
            },
            onOpenPortalNow = { preset ->
                onSelectPortal(preset.id)
                eduUrl = preset.url
                openInAppWebView(preset.url)
            }
        )
    }

    if (showWebViewDialog) {
        CampusWebViewImportDialog(
            initialUrl = webViewTargetUrl,
            universityPortals = portals,
            onDismiss = { showWebViewDialog = false },
            onExtractSuccess = { extractedTextOrJson ->
                showWebViewDialog = false
                onImportRawText(extractedTextOrJson)
            },
            onShowMessage = onShowMessage
        )
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val content = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                    ?: ""
                if (content.isNotBlank()) {
                    onImportRawText(content)
                } else {
                    onShowMessage("所选文件内容为空")
                }
            }.onFailure {
                onShowMessage("读取文件失败，请重试")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. 教务系统在线导课
        ClassTrackCard(
            modifier = Modifier.fillMaxWidth()
        ) {
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
                        text = "教务系统直连导入",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = { showUniversitySubPage = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("切换高校 ›", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Text(
                    text = "登录教务系统进入个人课表页后，点击底部「导入当前课表」即可自动解析并合并连堂课。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = visuals.mutedTextColor
                )

                // 主展示当前选中的高校卡片
                ClassTrackSubSurface(
                    shape = RoundedCornerShape(14.dp),
                    classicColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    isHighlighted = true,
                    onClick = {
                        onSelectPortal(primaryPortal.id)
                        eduUrl = primaryPortal.url
                        openInAppWebView(primaryPortal.url)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = "当前高校",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = primaryPortal.schoolName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "${primaryPortal.portalLabel} · ${primaryPortal.systemType}",
                                fontSize = 11.5.sp,
                                color = visuals.mutedTextColor
                            )
                        }

                        Button(
                            onClick = {
                                onSelectPortal(primaryPortal.id)
                                eduUrl = primaryPortal.url
                                openInAppWebView(primaryPortal.url)
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("进入教务", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 常用高校快捷切换条
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    quickPortals.forEach { preset ->
                        val isSelected = preset.id == primaryPortal.id
                        ClassTrackSubSurface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            isHighlighted = isSelected,
                            onClick = {
                                onSelectPortal(preset.id)
                                eduUrl = preset.url
                                openInAppWebView(preset.url)
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = preset.schoolName,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    ClassTrackSubSurface(
                        shape = RoundedCornerShape(10.dp),
                        onClick = { showUniversitySubPage = true }
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "全部高校",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = eduUrl,
                    onValueChange = { eduUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("输入自定义教务网址...", fontSize = 11.5.sp) },
                    label = { Text("自定义教务网址 (留空则使用当前高校)") }
                )

                Button(
                    onClick = {
                        val target = eduUrl.ifBlank { primaryPortal.url }
                        openInAppWebView(target)
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("打开内置导课浏览器", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. 课表文件与备份
        ClassTrackCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "课表文件与备份",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "支持导入课表 JSON 文件、分享口令，或将格式模板发给 DeepSeek 等 AI 整理后直接导入。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = visuals.mutedTextColor
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("选择课表文件", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val clipboard =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipText = clipboard.primaryClip
                                ?.takeIf { it.itemCount > 0 }
                                ?.getItemAt(0)
                                ?.coerceToText(context)
                                ?.toString()
                                ?: ""
                            if (clipText.isNotBlank()) {
                                onImportRawText(clipText)
                            } else {
                                onShowMessage("剪贴板为空，请先复制课表内容")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("从剪贴板导入", fontSize = 12.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val prompt = onBuildDeepSeekSchedulePrompt()
                            val clipboard =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("DeepSeekSchedulePrompt", prompt))
                            onShowMessage("已复制 AI 导课模板，可发送给 DeepSeek 转换课表")
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("复制 AI 导课模板", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val jsonBackup = onExportBackupJson()
                            val clipboard =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("ClassTrackJson", jsonBackup))
                            onShowMessage("已复制完整课表 JSON 备份")
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("导出 JSON 备份", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val token = onExportShareToken()
                            val clipboard =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("ClassTrackToken", token))
                            onShowMessage("已复制分享口令")
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("分享口令", fontSize = 11.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showManualPasteBox = !showManualPasteBox },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (showManualPasteBox) "收起文本粘贴框" else "手动粘贴课表文本 / 口令",
                            fontSize = 11.sp
                        )
                    }

                    TextButton(
                        onClick = { showClearConfirmDialog = true },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("清空当前课表", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                }

                if (showClearConfirmDialog) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showClearConfirmDialog = false },
                        title = { Text("清空课表确认") },
                        text = { Text("确定要清空本地所有已添加的课程吗？清空后课表将恢复为空白状态。") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showClearConfirmDialog = false
                                    onClearAllCourses()
                                    onShowMessage("已清空当前课表")
                                }
                            ) {
                                Text("确认清空")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClearConfirmDialog = false }) {
                                Text("取消")
                            }
                        }
                    )
                }

                AnimatedVisibility(
                    visible = showManualPasteBox,
                    enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(200)),
                    exit = shrinkVertically(tween(180)) + fadeOut(tween(140))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = rawTextInput,
                            onValueChange = { rawTextInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp),
                            label = { Text("粘贴课表 JSON、分享口令或 CSV 文本") },
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = { onImportRawText(rawTextInput) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("确认导入")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * 将「空值主题 JSON 模板文件 (`classtrack_theme_template.json`)」+ 字段规范提示词通过 FileProvider 分享至 DeepSeek
 */
private fun shareEmptyThemeTemplateToDeepSeek(
    context: Context,
    styleDescription: String,
    buildPrompt: (String) -> String,
    onShowMessage: (String) -> Unit
) {
    val promptText = buildPrompt(styleDescription)
    val emptyTemplateJson = CourseImporter.buildEmptyThemeTemplateJson()

    // 1. 将字段规范提示词写入剪贴板
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("DeepSeekThemePrompt", promptText))

    // 2. 在缓存目录生成空值 JSON 模板文件
    val sharedDir = File(context.cacheDir, "shared_templates").apply { mkdirs() }
    val templateFile = File(sharedDir, "classtrack_theme_template.json")
    templateFile.writeText(emptyTemplateJson, Charsets.UTF_8)

    val fileUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        templateFile
    )

    // 3. 构造包含 JSON 模板文件与文本提示词的 Intent
    val baseSendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, fileUri)
        putExtra(Intent.EXTRA_TEXT, promptText)
        putExtra(Intent.EXTRA_SUBJECT, "课轨主题配置模板.json")
        clipData = ClipData.newRawUri("classtrack_theme_template.json", fileUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    val deepSeekSendIntent = Intent(baseSendIntent).apply {
        setPackage("com.deepseek.chat")
    }

    val pm = context.packageManager
    val launchedDirectShare = runCatching {
        if (deepSeekSendIntent.resolveActivity(pm) != null) {
            context.startActivity(deepSeekSendIntent)
            onShowMessage("已打开 DeepSeek，提示词已复制到剪贴板")
            true
        } else {
            false
        }
    }.getOrDefault(false)

    if (!launchedDirectShare) {
        val wildcardDeepSeekIntent = Intent(baseSendIntent).apply {
            type = "*/*"
            setPackage("com.deepseek.chat")
        }
        val launchedWildcard = runCatching {
            if (wildcardDeepSeekIntent.resolveActivity(pm) != null) {
                context.startActivity(wildcardDeepSeekIntent)
                onShowMessage("已打开 DeepSeek，提示词已复制到剪贴板")
                true
            } else {
                false
            }
        }.getOrDefault(false)

        if (!launchedWildcard) {
            runCatching {
                val chooser = Intent.createChooser(
                    baseSendIntent,
                    "选择 AI 助手应用（提示词已同步复制）"
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(chooser)
                onShowMessage("模板文件已生成，请选择应用发送")
            }.onFailure {
                onShowMessage("提示词与模板内容已复制到剪贴板")
            }
        }
    }
}

/**
 * 拟物黑胶唱片圆盘绘制组件（支持静态与旋转动画）：
 * 包含黑胶外圈、同心声槽纹理、斜向高光折射、主题色中心盘芯与轴孔。
 */
@Composable
fun VinylRecordDisc(
    primaryColor: Color,
    secondaryColor: Color,
    size: Dp = 48.dp,
    spinning: Boolean = false,
    modifier: Modifier = Modifier
) {
    val rotationDeg = if (spinning) {
        val infiniteTransition = rememberInfiniteTransition(label = "VinylSpin")
        val angle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 9000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "VinylSpinAngle"
        )
        angle
    } else {
        0f
    }

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer { rotationZ = rotationDeg }
    ) {
        val r = this.size.minDimension / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)

        // 1. 黑胶盘体基底
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF232733), Color(0xFF0E1017), Color(0xFF191D28)),
                center = center,
                radius = r
            ),
            radius = r,
            center = center
        )

        // 2. 同心音轨声槽
        val grooveRadii = listOf(0.86f, 0.74f, 0.62f, 0.50f)
        grooveRadii.forEachIndexed { idx, ratio ->
            drawCircle(
                color = if (idx % 2 == 0) {
                    Color.White.copy(alpha = 0.09f)
                } else {
                    primaryColor.copy(alpha = 0.14f)
                },
                radius = r * ratio,
                center = center,
                style = Stroke(width = (r * 0.025f).coerceAtLeast(1f))
            )
        }

        // 3. 斜向高光点缀（让旋转肉眼可见）
        drawCircle(
            color = Color.White.copy(alpha = 0.22f),
            radius = r * 0.07f,
            center = Offset(center.x + r * 0.46f, center.y - r * 0.42f)
        )

        // 4. 专辑中心彩标 (Center Label)
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(primaryColor, secondaryColor),
                start = Offset(center.x - r * 0.35f, center.y - r * 0.35f),
                end = Offset(center.x + r * 0.35f, center.y + r * 0.35f)
            ),
            radius = r * 0.35f,
            center = center
        )

        // 5. 中心唱针孔
        drawCircle(
            color = Color(0xFF0B0D13),
            radius = r * 0.095f,
            center = center
        )
    }
}

@Composable
private fun GenreFilterBar(
    selectedGenre: MusicGenre,
    customCount: Int,
    onSelectGenre: (MusicGenre) -> Unit
) {
    val visuals = LocalClassTrackVisuals.current
    val horizontalState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(horizontalState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MusicGenre.entries.forEach { genre ->
            val isSelected = selectedGenre == genre
            val count = when (genre) {
                MusicGenre.ALL -> ArtistTheme.builtInThemes.size + customCount
                MusicGenre.CUSTOM -> customCount
                else -> ArtistTheme.builtInThemes.count { it.genre == genre }
            }
            val bg by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (visuals.isGlass) 0.55f else 0.85f)
                },
                animationSpec = tween(160),
                label = "GenreChipBg"
            )
            val contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            }

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onSelectGenre(genre) },
                shape = RoundedCornerShape(18.dp),
                color = bg
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = genre.displayName,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor
                    )
                    Text(
                        text = "$count",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor.copy(alpha = 0.78f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BuiltInAlbumThemeCard(
    theme: ArtistTheme,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val visuals = LocalClassTrackVisuals.current
    val swatches = themePreviewSwatches(theme)
    val primarySwatch = swatches.getOrElse(1) { MaterialTheme.colorScheme.primary }
    val secondarySwatch = swatches.getOrElse(2) { MaterialTheme.colorScheme.secondary }

    val cardBg by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(180),
        label = "BuiltInThemeCardBg"
    )
    val cardBorder by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            visuals.cardBorderColor
        },
        animationSpec = tween(180),
        label = "BuiltInThemeCardBorder"
    )

    ClassTrackSubSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (visuals.isGlass) 16.dp else 12.dp),
        classicColor = cardBg,
        classicBorder = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = cardBorder
        ),
        isHighlighted = isSelected,
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                VinylRecordDisc(
                    primaryColor = primarySwatch,
                    secondaryColor = secondarySwatch,
                    size = 40.dp,
                    spinning = isSelected
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = primarySwatch.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = theme.genre.displayName,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = primarySwatch,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                            )
                        }
                        Text(
                            text = "${theme.displayName} · ${theme.artistName}",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = theme.subtitle,
                        fontSize = 11.sp,
                        color = visuals.mutedTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                swatches.drop(1).forEach { c ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomAlbumThemeCard(
    item: CustomThemeConfig,
    isSelected: Boolean,
    canDelete: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val visuals = LocalClassTrackVisuals.current
    val primaryColor = parseHexColor(item.primaryHex, ZtmyPrimary)
    val secondaryColor = parseHexColor(item.secondaryHex, ZtmySecondary)
    val tertiaryColor = parseHexColor(item.tertiaryHex, ZtmyTertiary)
    val swatches = listOf(primaryColor, secondaryColor, tertiaryColor)

    val cardBg by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(180),
        label = "CustomThemeCardBg"
    )
    val cardBorder by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            visuals.cardBorderColor
        },
        animationSpec = tween(180),
        label = "CustomThemeCardBorder"
    )

    ClassTrackSubSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (visuals.isGlass) 16.dp else 12.dp),
        classicColor = cardBg,
        classicBorder = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = cardBorder
        ),
        isHighlighted = isSelected,
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                VinylRecordDisc(
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    size = 40.dp,
                    spinning = isSelected
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = primaryColor.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "自定义",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                            )
                        }
                        Text(
                            text = if (item.artistName.isNotBlank()) {
                                "${item.themeName} · ${item.artistName}"
                            } else {
                                item.themeName
                            },
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = item.subtitle.ifBlank {
                            "${if (item.isDark) "深色" else "浅色"} · 圆角 ${item.cardCornerDp}dp · ${item.fontStyle}"
                        },
                        fontSize = 11.sp,
                        color = visuals.mutedTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                swatches.forEach { c ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                    )
                }
                if (canDelete) {
                    TextButton(
                        onClick = onDelete,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("删除", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

/**
 * 全局交互式「黑胶唱片柜」主题选择抽屉组件（ModalBottomSheet）：
 * 可从顶栏主题胶囊、课表底部歌词条、或主题工作台唱机卡片随时呼出，
 * 支持按「全部 / 华语 / 欧美 / K-Pop / 日音 / 自定义」流派筛选并一键换碟。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VinylThemeCabinetBottomSheet(
    currentTheme: ArtistTheme,
    customThemeConfig: CustomThemeConfig,
    customThemes: List<CustomThemeConfig>,
    onSelectTheme: (ArtistTheme) -> Unit,
    onSelectCustomTheme: (String) -> Unit,
    onDeleteCustomTheme: (String) -> Unit,
    onNavigateToThemeStudio: () -> Unit,
    onDismiss: () -> Unit
) {
    val visuals = LocalClassTrackVisuals.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedGenre by remember { mutableStateOf(MusicGenre.ALL) }
    val listScrollState = rememberScrollState()

    val filteredBuiltIn = remember(selectedGenre) {
        when (selectedGenre) {
            MusicGenre.ALL -> ArtistTheme.builtInThemes
            MusicGenre.CUSTOM -> emptyList()
            else -> ArtistTheme.builtInThemes.filter { it.genre == selectedGenre }
        }
    }
    val showCustomSection = selectedGenre == MusicGenre.ALL || selectedGenre == MusicGenre.CUSTOM

    val activeTitle = if (currentTheme == ArtistTheme.CUSTOM_JSON) {
        customThemeConfig.themeName
    } else {
        currentTheme.displayName
    }
    val activeArtist = if (currentTheme == ArtistTheme.CUSTOM_JSON) {
        customThemeConfig.artistName.ifBlank { "Custom Studio" }
    } else {
        currentTheme.artistName
    }
    val activeSubtitle = if (currentTheme == ArtistTheme.CUSTOM_JSON) {
        customThemeConfig.subtitle.ifBlank { "自定义专辑配色方案" }
    } else {
        currentTheme.subtitle
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. 顶部正在播放的黑胶唱盘状态横幅
            ClassTrackSubSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                classicColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                classicBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                isHighlighted = true
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    VinylRecordDisc(
                        primaryColor = MaterialTheme.colorScheme.primary,
                        secondaryColor = MaterialTheme.colorScheme.secondary,
                        size = 54.dp,
                        spinning = true
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "NOW SPINNING · ${currentTheme.genre.displayName}",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "$activeTitle · $activeArtist",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = activeSubtitle,
                            fontSize = 11.sp,
                            color = visuals.mutedTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 2. 流派分类导航胶囊栏（全部 / 华语 / 欧美 / K-Pop / 日音 / 自定义）
            GenreFilterBar(
                selectedGenre = selectedGenre,
                customCount = customThemes.size,
                onSelectGenre = { selectedGenre = it }
            )

            // 3. 唱片列表滚动区
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 370.dp)
                    .verticalScroll(listScrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filteredBuiltIn.forEach { theme ->
                    BuiltInAlbumThemeCard(
                        theme = theme,
                        isSelected = currentTheme == theme,
                        onSelect = { onSelectTheme(theme) }
                    )
                }

                if (showCustomSection && customThemes.isNotEmpty()) {
                    if (filteredBuiltIn.isNotEmpty()) {
                        Text(
                            text = "自定义专辑主题",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = visuals.mutedTextColor,
                            modifier = Modifier.padding(top = 4.dp, start = 2.dp)
                        )
                    }
                    customThemes.forEach { item ->
                        val isSelected = currentTheme == ArtistTheme.CUSTOM_JSON && customThemeConfig.id == item.id
                        val canDelete = customThemes.size > 1 || item.id != DataRepository.SAMPLE_CUSTOM_THEME_ID
                        CustomAlbumThemeCard(
                            item = item,
                            isSelected = isSelected,
                            canDelete = canDelete,
                            onSelect = { onSelectCustomTheme(item.id) },
                            onDelete = { onDeleteCustomTheme(item.id) }
                        )
                    }
                }
            }

            // 4. 底部操作栏：前往主题工作台定制 / 关闭抽屉
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onNavigateToThemeStudio()
                    },
                    modifier = Modifier.weight(1.3f)
                ) {
                    Text("AI 定制 / 导入新专辑", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(0.9f)
                ) {
                    Text("完成选碟", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ThemeStudioSection(
    currentTheme: ArtistTheme,
    customThemeConfig: CustomThemeConfig,
    customThemes: List<CustomThemeConfig>,
    onSelectTheme: (ArtistTheme) -> Unit,
    onSelectCustomTheme: (String) -> Unit,
    onDeleteCustomTheme: (String) -> Unit,
    onImportThemeJson: (String) -> Boolean,
    onBuildDeepSeekThemePrompt: (String) -> String,
    onOpenThemeCabinet: () -> Unit = {},
    onShowMessage: (String) -> Unit
) {
    val visuals = LocalClassTrackVisuals.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var selectedGenre by remember { mutableStateOf(MusicGenre.ALL) }
    var aiStyleInput by remember { mutableStateOf("") }
    var themeJsonInput by remember { mutableStateOf("") }
    var showThemeJsonEditor by remember { mutableStateOf(false) }

    val filteredBuiltIn = remember(selectedGenre) {
        when (selectedGenre) {
            MusicGenre.ALL -> ArtistTheme.builtInThemes
            MusicGenre.CUSTOM -> emptyList()
            else -> ArtistTheme.builtInThemes.filter { it.genre == selectedGenre }
        }
    }
    val showCustomSection = selectedGenre == MusicGenre.ALL || selectedGenre == MusicGenre.CUSTOM

    val themeFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val content = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                    ?: ""
                if (content.isNotBlank()) {
                    themeJsonInput = content
                    onImportThemeJson(content)
                } else {
                    onShowMessage("所选文件为空")
                }
            }.onFailure {
                onShowMessage("读取文件失败")
            }
        }
    }

    val saveEmptyTemplateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(CourseImporter.buildEmptyThemeTemplateJson().toByteArray(Charsets.UTF_8))
                }
                onShowMessage("已保存模板文件 classtrack_theme_template.json")
            }.onFailure {
                onShowMessage("保存模板失败")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ====================================================================
        // 1. 交互式黑胶唱机展台卡片（点击随时呼出底部黑胶唱片柜抽屉）
        // ====================================================================
        ClassTrackCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        VinylRecordDisc(
                            primaryColor = MaterialTheme.colorScheme.primary,
                            secondaryColor = MaterialTheme.colorScheme.secondary,
                            size = 52.dp,
                            spinning = true,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onOpenThemeCabinet() }
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "当前播放专辑 · ${currentTheme.genre.displayName}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${visuals.themeDisplayName} · ${visuals.artistDisplayName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "共收录 ${ArtistTheme.builtInThemes.size} 款内置音乐专辑与 ${customThemes.size} 款自定义主题",
                                fontSize = 11.sp,
                                color = visuals.mutedTextColor
                            )
                        }
                    }
                }

                Button(
                    onClick = onOpenThemeCabinet,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "打开黑胶唱片柜 · 按流派快速选碟 ▾",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ====================================================================
        // 2. 主题库列表（支持华语 / 欧美 / K-Pop / 日音 / 自定义流派筛选）
        // ====================================================================
        ClassTrackCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "黑胶专辑主题库",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${ArtistTheme.builtInThemes.size + customThemes.size} 张唱片",
                        fontSize = 11.sp,
                        color = visuals.mutedTextColor
                    )
                }

                GenreFilterBar(
                    selectedGenre = selectedGenre,
                    customCount = customThemes.size,
                    onSelectGenre = { selectedGenre = it }
                )

                // 按流派展示内置音乐人主题
                filteredBuiltIn.forEach { theme ->
                    BuiltInAlbumThemeCard(
                        theme = theme,
                        isSelected = currentTheme == theme,
                        onSelect = { onSelectTheme(theme) }
                    )
                }

                // 用户新建的自定义主题列表
                if (showCustomSection && customThemes.isNotEmpty()) {
                    Text(
                        text = "自定义专辑主题：",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    customThemes.forEach { item ->
                        val isSelected = currentTheme == ArtistTheme.CUSTOM_JSON && customThemeConfig.id == item.id
                        val canDelete = customThemes.size > 1 || item.id != DataRepository.SAMPLE_CUSTOM_THEME_ID
                        CustomAlbumThemeCard(
                            item = item,
                            isSelected = isSelected,
                            canDelete = canDelete,
                            onSelect = { onSelectCustomTheme(item.id) },
                            onDelete = { onDeleteCustomTheme(item.id) }
                        )
                    }
                }

                // AI 辅助创建主题工作区
                ClassTrackSubSurface(
                    shape = RoundedCornerShape(if (visuals.isGlass) 16.dp else 12.dp),
                    classicColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    classicBorder = BorderStroke(1.dp, visuals.cardBorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "AI 辅助创建主题",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "输入期望的主题风格后发送至 DeepSeek，应用将生成包含字段规范的空值模板文件：",
                            fontSize = 11.sp,
                            color = visuals.mutedTextColor
                        )

                        OutlinedTextField(
                            value = aiStyleInput,
                            onValueChange = { aiStyleInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("主题风格描述（选填）") },
                            placeholder = { Text("例如：黑白极简风格 / 赛博朋克深色", fontSize = 11.sp) }
                        )

                        Button(
                            onClick = {
                                shareEmptyThemeTemplateToDeepSeek(
                                    context = context,
                                    styleDescription = aiStyleInput,
                                    buildPrompt = onBuildDeepSeekThemePrompt,
                                    onShowMessage = onShowMessage
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "发送模板至 DeepSeek",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val promptText = onBuildDeepSeekThemePrompt(aiStyleInput)
                                    val clipboard =
                                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("DeepSeekThemePrompt", promptText))
                                    onShowMessage("已复制设计提示词与模板")
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("复制提示词与模板", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    saveEmptyTemplateLauncher.launch("classtrack_theme_template.json")
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("保存模板文件", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // 导入主题操作区
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val clipboard =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipText = clipboard.primaryClip
                                ?.takeIf { it.itemCount > 0 }
                                ?.getItemAt(0)
                                ?.coerceToText(context)
                                ?.toString()
                                ?: ""
                            if (clipText.isNotBlank()) {
                                themeJsonInput = clipText
                                onImportThemeJson(clipText)
                            } else {
                                onShowMessage("剪贴板为空，请先复制主题 JSON")
                            }
                        },
                        modifier = Modifier.weight(1.25f)
                    ) {
                        Text("从剪贴板创建主题", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { themeFilePickerLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.weight(0.95f)
                    ) {
                        Text("导入 JSON 文件", fontSize = 12.sp)
                    }
                }

                TextButton(
                    onClick = {
                        if (!showThemeJsonEditor && themeJsonInput.isBlank()) {
                            themeJsonInput = CourseImporter.buildEmptyThemeTemplateJson()
                        }
                        showThemeJsonEditor = !showThemeJsonEditor
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = if (showThemeJsonEditor) "收起文本编辑框" else "手动粘贴 JSON 创建主题",
                        fontSize = 11.sp
                    )
                }

                AnimatedVisibility(
                    visible = showThemeJsonEditor,
                    enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(200)),
                    exit = shrinkVertically(tween(180)) + fadeOut(tween(140))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = themeJsonInput,
                            onValueChange = { themeJsonInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            label = { Text("粘贴主题 JSON 内容") },
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = { onImportThemeJson(themeJsonInput) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("添加并启用主题")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
