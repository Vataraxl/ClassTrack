package com.classtrack.app.ui.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.classtrack.app.data.model.ArtistTheme
import com.classtrack.app.data.model.CourseItem
import com.classtrack.app.data.model.LyricQuote
import com.classtrack.app.data.model.TodayTrackUiModel
import com.classtrack.app.data.model.TrackPlayState
import com.classtrack.app.ui.theme.ClassTrackCard
import com.classtrack.app.ui.theme.LocalClassTrackVisuals

private val DayLabels = listOf(
    1 to "MON · 周一",
    2 to "TUE · 周二",
    3 to "WED · 周三",
    4 to "THU · 周四",
    5 to "FRI · 周五",
    6 to "SAT · 周六",
    7 to "SUN · 周日"
)

@Composable
fun TracklistSection(
    artistTheme: ArtistTheme,
    currentWeek: Int,
    selectedDayOfWeek: Int,
    tracks: List<TodayTrackUiModel>,
    lyricQuote: LyricQuote,
    onSelectDay: (Int) -> Unit,
    onNextLyric: () -> Unit,
    onEditCourse: (CourseItem) -> Unit,
    onAddCourse: () -> Unit,
    onPushFluidCloud: () -> Unit,
    onAddCourseToSystemCalendar: (CourseItem) -> Unit
) {
    val visuals = LocalClassTrackVisuals.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. 概念专辑黑胶/磁带日签横幅 (含歌词彩蛋)
        item {
            AlbumLyricBannerCard(
                artistTheme = artistTheme,
                lyricQuote = lyricQuote,
                trackCount = tracks.size,
                onNextLyric = onNextLyric,
                onPushFluidCloud = onPushFluidCloud
            )
        }

        // 2. 星期胶片选择器 (Side A: Mon - Sun)
        item {
            DaySelectorStrip(
                selectedDay = selectedDayOfWeek,
                onSelectDay = onSelectDay
            )
        }

        // 3. 曲目单列表标题栏
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SIDE A · 第 $currentWeek 周 · ${tracks.size} Tracks",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onAddCourse) {
                    Text(
                        text = "+ 新课轨",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // 4. 若当天无课，展示 Interlude 自由假日卡片
        if (tracks.isEmpty()) {
            item {
                InterludeEmptyDayCard(
                    artistTheme = artistTheme,
                    lyricQuote = lyricQuote,
                    onAddCourse = onAddCourse
                )
            }
        } else {
            items(tracks, key = { it.course.id.toString() + "_" + it.trackNumber }) { track ->
                TrackCardItem(
                    track = track,
                    cardCornerRadius = visuals.cardCornerRadius,
                    trackBgColor = visuals.trackBackground(track.course.trackColorIndex),
                    trackAccentColor = visuals.trackAccent(track.course.trackColorIndex),
                    onClick = { onEditCourse(track.course) },
                    onExportToCalendar = { onAddCourseToSystemCalendar(track.course) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun AlbumLyricBannerCard(
    artistTheme: ArtistTheme,
    lyricQuote: LyricQuote,
    trackCount: Int,
    onNextLyric: () -> Unit,
    onPushFluidCloud: () -> Unit
) {
    val visuals = LocalClassTrackVisuals.current
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinyl_rotation"
    )

    val bannerInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isBannerPressed by bannerInteractionSource.collectIsPressedAsState()
    val bannerScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isBannerPressed) 0.985f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "BannerScale"
    )

    ClassTrackCard(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = bannerScale
                scaleY = bannerScale
            }
            .clickable(
                interactionSource = bannerInteractionSource,
                indication = null,
                onClick = onNextLyric
            ),
        shape = RoundedCornerShape(visuals.cardCornerRadius)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 旋转黑胶唱盘 / 磁带轴徽章
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .rotate(rotation),
                    contentAlignment = Alignment.Center
                ) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val secondaryColor = MaterialTheme.colorScheme.secondary
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(color = Color(0xFF1A1822))
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.45f),
                            radius = size.minDimension * 0.42f,
                            style = Stroke(width = 3f)
                        )
                        drawCircle(
                            color = secondaryColor.copy(alpha = 0.7f),
                            radius = size.minDimension * 0.28f,
                            style = Stroke(width = 4f)
                        )
                        drawCircle(
                            color = primaryColor,
                            radius = size.minDimension * 0.14f
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = artistTheme.badgeText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = lyricQuote.moodTag,
                            style = MaterialTheme.typography.labelMedium,
                            color = visuals.mutedTextColor
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "《${lyricQuote.songTitle}》· ${artistTheme.artistName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 双语歌词签展示区
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "「${lyricQuote.japaneseLyric}」",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = lyricQuote.chineseTranslation,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = visuals.mutedTextColor
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "轻触切换歌词 · 今日 $trackCount 节课",
                    style = MaterialTheme.typography.labelMedium,
                    color = visuals.mutedTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                OutlinedButton(
                    onClick = onPushFluidCloud,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = "☁️ 流体云",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun DaySelectorStrip(
    selectedDay: Int,
    onSelectDay: (Int) -> Unit
) {
    val visuals = LocalClassTrackVisuals.current
    val scrollState = rememberScrollState()

    // 选中的星期自动滚动到可视区域（例如周六/周日默认在屏幕外）
    LaunchedEffect(selectedDay, scrollState.maxValue) {
        if (scrollState.maxValue > 0) {
            val fraction = (selectedDay - 1) / 6f
            scrollState.animateScrollTo((scrollState.maxValue * fraction).toInt())
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DayLabels.forEach { (dayInt, label) ->
            val isSelected = dayInt == selectedDay
            Surface(
                modifier = Modifier.clickable { onSelectDay(dayInt) },
                shape = RoundedCornerShape(visuals.cardCornerRadius),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else visuals.cardBorderColor
                )
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun TrackCardItem(
    track: TodayTrackUiModel,
    cardCornerRadius: androidx.compose.ui.unit.Dp,
    trackBgColor: Color,
    trackAccentColor: Color,
    onClick: () -> Unit,
    onExportToCalendar: () -> Unit
) {
    val visuals = LocalClassTrackVisuals.current
    val course = track.course

    val itemInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isItemPressed by itemInteractionSource.collectIsPressedAsState()
    val itemScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isItemPressed) 0.98f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "TrackCardScale"
    )

    ClassTrackCard(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = itemScale
                scaleY = itemScale
            }
            .clickable(
                interactionSource = itemInteractionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(cardCornerRadius),
        tintColor = if (visuals.isGlass) trackBgColor else androidx.compose.ui.graphics.Color.Unspecified,
        border = BorderStroke(
            width = if (track.playState == TrackPlayState.NOW_PLAYING) 1.8.dp else 1.dp,
            color = if (track.playState == TrackPlayState.NOW_PLAYING) trackAccentColor else visuals.cardBorderColor.copy(alpha = 0.65f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 第一行：TRACK 编号 + 时间节次 + 实时状态标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = trackAccentColor,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "TRACK %02d".format(track.trackNumber),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = "${track.startTime} - ${track.endTime} (${course.sectionRangeText()})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val statusText = when (track.playState) {
                    TrackPlayState.NOW_PLAYING -> "▶ 正在播放 · 剩 ${track.minutesUntilStartOrEnd}m"
                    TrackPlayState.UPCOMING -> "⏳ ${track.minutesUntilStartOrEnd}m 后开课"
                    TrackPlayState.COMPLETED -> "✓ 已播放完毕"
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = trackAccentColor
                )
            }

            // 第二行：课程（曲目）主标题
            Text(
                text = course.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 第三行：教室、任课教师与周次属性
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = course.room,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (course.teacher.isNotBlank()) {
                    Text(
                        text = course.teacher,
                        style = MaterialTheme.typography.bodyMedium,
                        color = visuals.mutedTextColor
                    )
                }
                Text(
                    text = course.weekRangeText(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = visuals.mutedTextColor
                )
            }

            // 若有 B-Side 备注
            if (course.note.isNotBlank()) {
                Text(
                    text = "备注：${course.note}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = visuals.mutedTextColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 若正在上课，展示音频进度条
            if (track.playState == TrackPlayState.NOW_PLAYING) {
                LinearProgressIndicator(
                    progress = { track.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = trackAccentColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // 底部快捷操作栏：写入日历 / 编辑课程
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onExportToCalendar,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = "写入系统日历",
                        fontSize = 11.sp,
                        color = trackAccentColor
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                TextButton(
                    onClick = onClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = "编辑课程",
                        fontSize = 11.sp,
                        color = visuals.mutedTextColor
                    )
                }
            }
        }
    }
}

@Composable
private fun InterludeEmptyDayCard(
    artistTheme: ArtistTheme,
    lyricQuote: LyricQuote,
    onAddCourse: () -> Unit
) {
    val visuals = LocalClassTrackVisuals.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(visuals.cardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, visuals.cardBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "今日无课 · 自由间奏",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "今天没有课程安排，听听音乐放松一下吧。",
                style = MaterialTheme.typography.bodyMedium,
                color = visuals.mutedTextColor
            )
            OutlinedButton(
                onClick = onAddCourse,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("+ 添加今日课程或自习")
            }
        }
    }
}
