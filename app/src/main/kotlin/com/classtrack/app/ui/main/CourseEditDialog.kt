package com.classtrack.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.classtrack.app.data.model.CourseItem
import com.classtrack.app.ui.theme.LocalClassTrackVisuals

private val DialogWeekDays = listOf(
    1 to "周一", 2 to "周二", 3 to "周三", 4 to "周四", 5 to "周五", 6 to "周六", 7 to "周日"
)

@Composable
fun CourseEditDialog(
    initialCourse: CourseItem,
    onDismiss: () -> Unit,
    onSave: (CourseItem) -> Unit,
    onDelete: (Long) -> Unit
) {
    val visuals = LocalClassTrackVisuals.current
    val isEditingExisting = initialCourse.id > 0L

    var name by remember(initialCourse) { mutableStateOf(initialCourse.name) }
    var room by remember(initialCourse) { mutableStateOf(initialCourse.room) }
    var teacher by remember(initialCourse) { mutableStateOf(initialCourse.teacher) }
    var dayOfWeek by remember(initialCourse) { mutableIntStateOf(initialCourse.dayOfWeek) }
    var startSection by remember(initialCourse) { mutableIntStateOf(initialCourse.startSection) }
    var endSection by remember(initialCourse) { mutableIntStateOf(initialCourse.endSection) }
    var startWeek by remember(initialCourse) { mutableIntStateOf(initialCourse.startWeek) }
    var endWeek by remember(initialCourse) { mutableIntStateOf(initialCourse.endWeek) }
    var weekParity by remember(initialCourse) { mutableStateOf(initialCourse.weekParity) }
    var weeksSpec by remember(initialCourse) { mutableStateOf(initialCourse.weeksSpec) }
    var colorIndex by remember(initialCourse) { mutableIntStateOf(initialCourse.trackColorIndex) }
    var note by remember(initialCourse) { mutableStateOf(initialCourse.note) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = if (isEditingExisting) "编辑课程" else "添加课程",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    label = { Text("课程名称 (必填)") }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        label = { Text("上课教室") }
                    )
                    OutlinedTextField(
                        value = teacher,
                        onValueChange = { teacher = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        label = { Text("任课教师") }
                    )
                }

                // 星期选择
                Text(
                    text = "上课星期",
                    style = MaterialTheme.typography.labelMedium,
                    color = visuals.mutedTextColor
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DialogWeekDays.forEach { (d, label) ->
                        FilterChip(
                            selected = dayOfWeek == d,
                            onClick = { dayOfWeek = d },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                // 节次范围一体式胶囊调整
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "第 %02d - %02d 节".format(startSection, endSection),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(
                                onClick = {
                                    startSection = (startSection - 1).coerceAtLeast(1)
                                    if (endSection < startSection) endSection = startSection
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) { Text("起−", fontSize = 12.sp) }
                            TextButton(
                                onClick = {
                                    startSection = (startSection + 1).coerceAtMost(16)
                                    if (endSection < startSection) endSection = startSection
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) { Text("起+", fontSize = 12.sp) }
                            TextButton(
                                onClick = {
                                    endSection = (endSection - 1).coerceAtLeast(startSection)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) { Text("止−", fontSize = 12.sp) }
                            TextButton(
                                onClick = {
                                    endSection = (endSection + 1).coerceAtMost(16)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) { Text("止+", fontSize = 12.sp) }
                        }
                    }
                }

                OutlinedTextField(
                    value = weeksSpec,
                    onValueChange = { weeksSpec = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    label = { Text("上课周次 (如: 1-16 或 2,6,8-9,12-18)") }
                )

                Text(
                    text = "课程色标",
                    style = MaterialTheme.typography.labelMedium,
                    color = visuals.mutedTextColor
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (idx in 0 until 6) {
                        val accent = visuals.trackAccent(idx)
                        val isChosen = idx == colorIndex
                        Box(
                            modifier = Modifier
                                .size(if (isChosen) 28.dp else 22.dp)
                                .clip(CircleShape)
                                .background(accent)
                                .border(
                                    width = if (isChosen) 2.5.dp else 0.dp,
                                    color = if (isChosen) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { colorIndex = idx }
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    label = { Text("课程备注 (选填)") }
                )
            }
        },
        confirmButton = {
            Button(
                shape = RoundedCornerShape(12.dp),
                onClick = {
                    onSave(
                        initialCourse.copy(
                            name = name.trim(),
                            room = room.trim(),
                            teacher = teacher.trim(),
                            dayOfWeek = dayOfWeek,
                            startSection = startSection,
                            endSection = endSection.coerceAtLeast(startSection),
                            startWeek = startWeek,
                            endWeek = endWeek.coerceAtLeast(startWeek),
                            weekParity = weekParity,
                            weeksSpec = weeksSpec.trim(),
                            trackColorIndex = colorIndex,
                            note = note.trim()
                        )
                    )
                }
            ) {
                Text("保存", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isEditingExisting) {
                    TextButton(onClick = { onDelete(initialCourse.id) }) {
                        Text("删除", color = MaterialTheme.colorScheme.secondary)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )
}
