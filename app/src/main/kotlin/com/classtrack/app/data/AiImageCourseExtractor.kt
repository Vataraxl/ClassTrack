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
                "parity": "ALL"
              }
            ]
            说明：
            - dayOfWeek：1代表周一，2代表周二……7代表周日。
            - startSection / endSection：第几节到第几节（1~12）。
            - parity：每周填 "ALL"，单周填 "ODD"，双周填 "EVEN"。
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
