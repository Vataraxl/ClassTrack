package com.classtrack.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * AI 课表截图 / 照片多模态识别导课引擎
 * 支持从手机相册选择纸质课表照片、Excel 截图或教务系统截图，自动提取结构化课程列表
 */
object AiImageCourseExtractor {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun extractCoursesFromImage(
        context: Context,
        imageUri: Uri,
        apiKey: String
    ): CourseImporter.ImportResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext CourseImporter.ImportResult(
                success = false,
                sourceType = "AI 截图识别",
                courses = emptyList(),
                message = "请先填写 Gemini API Key 以启用 AI 课表图片识别"
            )
        }

        val base64Jpeg = runCatching { encodeUriToBase64Jpeg(context, imageUri) }.getOrNull()
            ?: return@withContext CourseImporter.ImportResult(
                success = false,
                sourceType = "AI 截图识别",
                courses = emptyList(),
                message = "无法读取所选图片，请重新选择清晰的课表截图"
            )

        val prompt = """
            你是一个专业的高校课表识别助手。请仔细识别这张课表图片中的所有课程，并严格只输出一个 JSON 数组（不要输出 Markdown 代码块标记，只输出纯 JSON 数组）。
            数组中每个对象的字段格式如下：
            [
              {
                "name": "课程名称",
                "room": "教室地点（无则填待定教室）",
                "teacher": "教师姓名（无则填空字符串）",
                "dayOfWeek": 1,
                "startSection": 1,
                "endSection": 2,
                "startWeek": 1,
                "endWeek": 16,
                "weeksSpec": "",
                "parity": "ALL"
              }
            ]
            严格识别规则（务必逐条遵守）：
            1. 星期列严禁错位与跨列猜测：
               - 每张课程卡片必须严格归属于其正上方表头所在的星期列（星期一=1，星期二=2……星期五=5，星期六=6，星期日=7）。
               - 若截图仅显示星期一至星期五，绝不可将底部的课程卡片或被裁切的卡片臆测放入星期六(6)或星期日(7)，也不可把同一张卡片重复识别到另一列！
               - 若最底部的卡片跨过截图标注区域被部分裁切但上方仍可看到其列位置（如星期一/四/五的第10节及以后行），若无法确认节次则根据其纵向位置对应第10节以后（如 10~11 节），切勿放到第 1~4 节或周六/周日；若完全缺失时间信息则可略过或标为对应列晚间节次。
            2. 课程名清洗：
               - 去掉课程名末尾的英文数字课程代码和班号（例如 "思想道德与法治[ETH105] 19" 应输出 "思想道德与法治"，"数学分析（一）[STA102] 1" 应输出 "数学分析(一)"）。
               - 忽略 "选课人数：99"、"学分" 等统计噪声文本。
            3. 紧凑多合一排课行拆解（重点适配如 bkjw.uibe.edu.cn 等格式）：
               - 卡片中常见紧贴写法如 "4-16周李怀征 1节-2节 求103"、"4-12周,15-16周陈俊彰 6节-7节 求319"、"4-16周吴超 8节-9节 田径场"。
               - 周次在 "周" 前面（如 4-16），若存在不连续周次如 "4-12周,15-16周"，除填 startWeek=4、endWeek=16 外，必须在 weeksSpec 填写 "4-12,15-16"；连续周次 weeksSpec 留空 ""。
               - 紧跟在 "周" 后的中文姓名是 teacher（例如 "李怀征"、"叶文锐"、"单萧语"），注意姓氏 "单" 不是单双周！
               - "X节-Y节"（如 1节-2节、3节-5节、6节-7节、8节-9节）对应 startSection=X, endSection=Y（支持 1~18）。
               - 跟在节次后面的短字符+数字或场地名（如 求103、求305、求319、求321、求323、求507、求217、宁726、宁331、博305、田径场、体育馆 等）全部是 room 教室地点！绝不可忽略这些短教室代号而填成 "待定教室"！
            4. 单卡多时段/多教师拆分：
               - 若一张卡片内部有两条不同周次或不同教师的排课（如 "5-16周单萧语 6节-7节 求321" 与 "4周刘菁 6节-7节 求321"），请拆分为两条独立的 JSON 对象分别输出。
            5. parity：每周填 "ALL"，明确标注单周填 "ODD"，明确标注双周填 "EVEN"。
        """.trimIndent()

        val requestBody = buildJsonObject {
            put(
                "contents",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put(
                                "parts",
                                buildJsonArray {
                                    add(buildJsonObject { put("text", prompt) })
                                    add(
                                        buildJsonObject {
                                            put(
                                                "inline_data",
                                                buildJsonObject {
                                                    put("mime_type", "image/jpeg")
                                                    put("data", base64Jpeg)
                                                }
                                            )
                                        }
                                    )
                                }
                            )
                        }
                    )
                }
            )
            put(
                "generationConfig",
                buildJsonObject {
                    put("response_mime_type", "application/json")
                    put("temperature", 0.1)
                }
            )
        }.toString()

        runCatching {
            val endpoint =
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${apiKey.trim()}"
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20000
                readTimeout = 30000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }

            conn.outputStream.use { os ->
                os.write(requestBody.toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            val responseText = if (code in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                val errText = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: "HTTP $code"
                throw IllegalStateException("API 请求失败 ($code): ${errText.take(120)}")
            }

            val rootObj = json.parseToJsonElement(responseText).jsonObject
            val textPart = rootObj["candidates"]
                ?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("content")
                ?.jsonObject?.get("parts")
                ?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("text")
                ?.jsonPrimitive?.content
                ?.trim()
                ?.removePrefix("```json")
                ?.removePrefix("```")
                ?.removeSuffix("```")
                ?.trim()
                ?: "[]"

            val parsedResult = CourseImporter.parseAuto(textPart)
            if (parsedResult.success) {
                parsedResult.copy(
                    sourceType = "AI 课表截图识别",
                    message = "✨ AI 成功从截图中识别出 ${parsedResult.courses.size} 门课轨"
                )
            } else {
                CourseImporter.ImportResult(
                    success = false,
                    sourceType = "AI 课表截图识别",
                    courses = emptyList(),
                    message = "未能从图中识别出有效排课，请确保截图包含清晰的课程与节次网格"
                )
            }
        }.getOrElse { e ->
            CourseImporter.ImportResult(
                success = false,
                sourceType = "AI 课表截图识别",
                courses = emptyList(),
                message = "AI 识别出错：${e.message}"
            )
        }
    }

    private fun encodeUriToBase64Jpeg(context: Context, uri: Uri): String {
        val original = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: throw IllegalArgumentException("无法解码图片")

        // 限制最大边长为 1600px，兼顾清晰度与上传速度
        val maxSide = 1600
        val ratio = minOf(
            maxSide.toFloat() / original.width.coerceAtLeast(1),
            maxSide.toFloat() / original.height.coerceAtLeast(1),
            1f
        )
        val scaled = if (ratio < 1f) {
            Bitmap.createScaledBitmap(
                original,
                (original.width * ratio).toInt().coerceAtLeast(1),
                (original.height * ratio).toInt().coerceAtLeast(1),
                true
            )
        } else {
            original
        }

        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
}
