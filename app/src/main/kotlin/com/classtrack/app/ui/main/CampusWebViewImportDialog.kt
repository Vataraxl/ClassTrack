package com.classtrack.app.ui.main

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Message
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.classtrack.app.data.CourseImporter
import com.classtrack.app.data.model.DefaultPresets
import com.classtrack.app.data.model.UniversityPortalPreset
import com.classtrack.app.ui.theme.LocalClassTrackVisuals
import org.json.JSONArray
import org.json.JSONTokener

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

/**
 * 内置高校教务智能导课浏览器（支持 Cookie 持久化、穿透多层 iframe 自动抓取课表、桌面/移动 UA 切换及高校快捷导航）
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CampusWebViewImportDialog(
    initialUrl: String,
    universityPortals: List<UniversityPortalPreset> = DefaultPresets.universityPortals,
    onDismiss: () -> Unit,
    onExtractSuccess: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val visuals = LocalClassTrackVisuals.current
    var addressInput by remember { mutableStateOf(initialUrl) }
    var activeLoadedUrl by remember { mutableStateOf(initialUrl) }
    var pageTitle by remember { mutableStateOf("内置智能导课浏览器") }
    var loadProgress by remember { mutableIntStateOf(0) }
    fun isDesktopPreferred(url: String): Boolean {
        val lower = url.lowercase()
        // bkjw.uibe.edu.cn 属于新版移动端/响应式课表页面，默认走手机视口以完整展示当前学期课表卡片
        if (lower.contains("bkjw.uibe.edu.cn")) return false
        return lower.contains("uibe.edu.cn") ||
            lower.contains("jsxsd") ||
            lower.contains("zhjw") ||
            lower.contains("newjwxt") ||
            lower.contains("bjfu.edu.cn") ||
            lower.contains("jwxt")
    }

    var useDesktopUa by remember { mutableStateOf(if (initialUrl.isNotBlank()) isDesktopPreferred(initialUrl) else true) }
    var showSchoolSelector by remember { mutableStateOf(initialUrl.isBlank()) }
    var detectedCourseCount by remember { mutableIntStateOf(0) }
    var capturedRawJson by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val navigateToUrl: (String) -> Unit = { rawUrl ->
        val trimmed = rawUrl.trim().replace("newjwxt.bjfu.cn", "newjwxt.bjfu.edu.cn")
        if (trimmed.isNotEmpty()) {
            val normalized = when {
                trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
                trimmed.contains("newjwxt.bjfu.edu.cn") -> "http://$trimmed"
                else -> "https://$trimmed"
            }
            val preferDesktop = isDesktopPreferred(normalized)
            useDesktopUa = preferDesktop
            webViewRef?.settings?.userAgentString = if (preferDesktop) DESKTOP_USER_AGENT else null
            addressInput = normalized
            activeLoadedUrl = normalized
            showSchoolSelector = false
            capturedRawJson = null
            detectedCourseCount = 0
            webViewRef?.loadUrl(normalized)
        }
    }

    // 解码 evaluateJavascript 返回的转义 JSON 字符串
    fun unwrapEvaluateJsString(rawJsResult: String?): String {
        if (rawJsResult.isNullOrBlank() || rawJsResult == "null") return ""
        return runCatching {
            val parsed = JSONTokener(rawJsResult).nextValue()
            parsed?.toString() ?: ""
        }.getOrDefault(rawJsResult.removeSurrounding("\""))
    }

    // 自动探测当前页面（含嵌套 iframe）是否存在课表项
    val probePageTimetable: (WebView?) -> Unit = { wv ->
        wv?.evaluateJavascript(CourseImporter.eduSystemExtractJs) { rawResult ->
            val jsonStr = unwrapEvaluateJsString(rawResult)
            val count = runCatching {
                when {
                    com.classtrack.app.data.TimetableCellParser.looksLikeCellPayload(jsonStr) ->
                        com.classtrack.app.data.TimetableCellParser.parse(jsonStr).size
                    jsonStr.startsWith("[") -> JSONArray(jsonStr).length()
                    else -> 0
                }
            }.getOrDefault(0)
            detectedCourseCount = count
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. 顶部操作栏
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(0.5.dp, visuals.cardBorderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = onDismiss,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("✕ 关闭", fontWeight = FontWeight.Bold)
                            }

                            Text(
                                text = pageTitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 6.dp)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedButton(
                                    onClick = { showSchoolSelector = !showSchoolSelector },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("高校列表", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        useDesktopUa = !useDesktopUa
                                        webViewRef?.settings?.userAgentString = if (useDesktopUa) {
                                            DESKTOP_USER_AGENT
                                        } else {
                                            null
                                        }
                                        webViewRef?.reload()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text(
                                        text = if (useDesktopUa) "桌面版" else "手机版",
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // 地址栏 + 刷新/后退
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (webViewRef?.canGoBack() == true) {
                                        webViewRef?.goBack()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Text("←", fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    webViewRef?.reload()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Text("⟳", fontSize = 13.sp)
                            }

                            OutlinedTextField(
                                value = addressInput,
                                onValueChange = { addressInput = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                placeholder = { Text("输入学校教务网址或从高校列表中选择...", fontSize = 11.sp) },
                                textStyle = MaterialTheme.typography.bodyMedium
                            )

                            Button(
                                onClick = { navigateToUrl(addressInput) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Text("前往", fontSize = 12.sp)
                            }
                        }

                        if (loadProgress in 1..99) {
                            LinearProgressIndicator(
                                progress = loadProgress / 100f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                            )
                        }
                    }
                }

                if (showSchoolSelector) {
                    UniversityPickerSubPageDialog(
                        universityPortals = universityPortals,
                        selectedPortalId = universityPortals.firstOrNull {
                            addressInput.contains(
                                it.url.removePrefix("https://").removePrefix("http://").take(12)
                            )
                        }?.id ?: "",
                        onDismiss = { showSchoolSelector = false },
                        onSelectPortal = { preset ->
                            navigateToUrl(preset.url)
                        },
                        onOpenPortalNow = { preset ->
                            navigateToUrl(preset.url)
                        }
                    )
                }

                // 2. WebView 主体（未选择网址时展示高校引导面板，加载网址后展示 WebView 并严格裁剪边界）
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clipToBounds()
                ) {
                    if (activeLoadedUrl.isBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "请选择学校教务系统或输入网址",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "点击下方入口打开对应教务系统，登录后进入个人课表页，点击底部「导入当前课表」即可读取：",
                                style = MaterialTheme.typography.bodyMedium,
                                color = visuals.mutedTextColor
                            )
                            universityPortals.forEach { preset ->
                                Surface(
                                    onClick = { navigateToUrl(preset.url) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, visuals.cardBorderColor),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${preset.schoolName} · ${preset.portalLabel}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${preset.systemType} | ${preset.url}",
                                                fontSize = 11.sp,
                                                color = visuals.mutedTextColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = "前往",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        AndroidView(
                            modifier = Modifier
                                .fillMaxSize()
                                .clipToBounds(),
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    // 开启 Cookie 持久化，登录一次后无需重复登录
                                    val cookieManager = CookieManager.getInstance()
                                    cookieManager.setAcceptCookie(true)
                                    cookieManager.setAcceptThirdPartyCookies(this, true)

                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        databaseEnabled = true
                                        setSupportMultipleWindows(true)
                                        javaScriptCanOpenWindowsAutomatically = true
                                        useWideViewPort = true
                                        loadWithOverviewMode = true
                                        setSupportZoom(true)
                                        builtInZoomControls = true
                                        displayZoomControls = false
                                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                        cacheMode = WebSettings.LOAD_DEFAULT
                                        userAgentString = if (useDesktopUa) DESKTOP_USER_AGENT else null
                                    }

                                    // 注册 JS 桥接，用于捕获异步课表 JSON
                                    addJavascriptInterface(
                                        object {
                                            @JavascriptInterface
                                            fun onCaptureTimetableJson(jsonStr: String) {
                                                if (jsonStr.isNotBlank()) {
                                                    val courses = CourseImporter.parseGenericUniversityApiJson(jsonStr)
                                                    if (courses.isNotEmpty()) {
                                                        post {
                                                            detectedCourseCount = courses.size
                                                            capturedRawJson = jsonStr
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        "ClassTrackBridge"
                                    )

                                    webChromeClient = object : WebChromeClient() {
                                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                            loadProgress = newProgress
                                            if (newProgress == 100) {
                                                probePageTimetable(view)
                                            }
                                        }

                                        override fun onReceivedTitle(view: WebView?, title: String?) {
                                            if (!title.isNullOrBlank()) {
                                                pageTitle = title
                                            }
                                        }

                                        // 彻底修复强智/UIBE 进入课表时 window.open('/jsxsd/xskb/xskb_list.do') 导致的白屏：
                                        // 拦截子窗口创建请求，并将目标 URL 强制重定向至当前宿主 WebView 中加载
                                        override fun onCreateWindow(
                                            view: WebView?,
                                            isDialog: Boolean,
                                            isUserGesture: Boolean,
                                            resultMsg: Message?
                                        ): Boolean {
                                            if (resultMsg == null || view == null) return false
                                            val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
                                            val tempWebView = WebView(view.context).apply {
                                                webViewClient = object : WebViewClient() {
                                                    override fun shouldOverrideUrlLoading(v: WebView?, request: WebResourceRequest?): Boolean {
                                                        val target = request?.url?.toString() ?: return false
                                                        view.loadUrl(target)
                                                        return true
                                                    }

                                                    @Deprecated("Deprecated in Java")
                                                    override fun shouldOverrideUrlLoading(v: WebView?, url: String?): Boolean {
                                                        if (!url.isNullOrBlank()) {
                                                            view.loadUrl(url)
                                                        }
                                                        return true
                                                    }
                                                }
                                            }
                                            transport.webView = tempWebView
                                            resultMsg.sendToTarget()
                                            return true
                                        }
                                    }

                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(
                                            view: WebView?,
                                            request: WebResourceRequest?
                                        ): Boolean {
                                            val target = request?.url?.toString() ?: return false
                                            if (target.startsWith("http://") || target.startsWith("https://")) {
                                                addressInput = target
                                                return false
                                            }
                                            return true
                                        }

                                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                            if (!url.isNullOrBlank()) {
                                                addressInput = url
                                                activeLoadedUrl = url
                                            }
                                            // 页面启动即注入 AJAX/Fetch 拦截 Hook
                                            view?.evaluateJavascript(CourseImporter.eduSystemAjaxHookJs, null)
                                            detectedCourseCount = 0
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            CookieManager.getInstance().flush()
                                            view?.evaluateJavascript(CourseImporter.eduSystemAjaxHookJs, null)
                                            probePageTimetable(view)
                                            // 兼容 SPA/Vue 异步渲染的现代教务课表（如 bkjw.uibe.edu.cn），延迟复探一次
                                            view?.postDelayed({ probePageTimetable(view) }, 1500L)
                                        }

                                        @SuppressLint("WebViewClientOnReceivedSslError")
                                        override fun onReceivedSslError(
                                            view: WebView?,
                                            handler: SslErrorHandler?,
                                            error: SslError?
                                        ) {
                                            // 兼容部分高校内网/教务系统自签名证书
                                            handler?.proceed()
                                        }
                                    }

                                    webViewRef = this
                                    loadUrl(activeLoadedUrl)
                                }
                            }
                        )
                    }
                }

                // 3. 底部一键抓取脚本栏
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, visuals.cardBorderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (detectedCourseCount > 0) {
                                    if (capturedRawJson != null) {
                                        "已捕获原生教务接口数据 ($detectedCourseCount 门)"
                                    } else {
                                        "已识别到 $detectedCourseCount 门课程"
                                    }
                                } else {
                                    "请登录教务系统并打开「我的课表」页面"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (detectedCourseCount > 0) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Text(
                                text = if (activeLoadedUrl.contains("uibe.edu.cn") || activeLoadedUrl.contains("jsxsd")) {
                                    "UIBE/强智教务已适配：若显示异常可点击顶栏⟳刷新"
                                } else {
                                    "进入个人课表页后点击右侧按钮导入"
                                },
                                fontSize = 10.sp,
                                color = visuals.mutedTextColor
                            )
                        }

                        Button(
                            onClick = {
                                val captured = capturedRawJson
                                if (!captured.isNullOrBlank()) {
                                    onExtractSuccess(captured)
                                    return@Button
                                }
                                val wv = webViewRef
                                if (wv == null) {
                                    onShowMessage("浏览器尚未就绪")
                                    return@Button
                                }
                                // 第 1 步：执行结构化多 frame 表格提取脚本
                                wv.evaluateJavascript(CourseImporter.eduSystemExtractJs) { rawJson ->
                                    val unwrappedJson = unwrapEvaluateJsString(rawJson)
                                    val hasStructuredCourses = runCatching {
                                        when {
                                            com.classtrack.app.data.TimetableCellParser.looksLikeCellPayload(unwrappedJson) ->
                                                com.classtrack.app.data.TimetableCellParser.parse(unwrappedJson).isNotEmpty()
                                            unwrappedJson.startsWith("[") -> JSONArray(unwrappedJson).length() > 0
                                            else -> false
                                        }
                                    }.getOrDefault(false)

                                    if (hasStructuredCourses) {
                                        onExtractSuccess(unwrappedJson)
                                    } else {
                                        // 第 2 步：若页面结构特殊，抓取所有 frame 的完整文本与 HTML 交由 Kotlin 解析器处理
                                        val fallbackJs = """
                                            (function() {
                                                var texts = [];
                                                function dumpDoc(d) {
                                                    if (!d) return;
                                                    try {
                                                        texts.push(d.body ? (d.body.innerText || '') : '');
                                                        var fs = d.querySelectorAll('iframe, frame');
                                                        for (var i = 0; i < fs.length; i++) {
                                                            try {
                                                                var sub = fs[i].contentDocument || (fs[i].contentWindow && fs[i].contentWindow.document);
                                                                if (sub) dumpDoc(sub);
                                                            } catch (e1) {}
                                                        }
                                                    } catch (e) {}
                                                }
                                                dumpDoc(document);
                                                return texts.join('\n');
                                            })();
                                        """.trimIndent()
                                        wv.evaluateJavascript(fallbackJs) { rawFallback ->
                                            val fallbackText = unwrapEvaluateJsString(rawFallback)
                                            if (fallbackText.isNotBlank()) {
                                                onExtractSuccess(fallbackText)
                                            } else {
                                                onShowMessage("当前页面未检测到课表数据，请确认已打开课表查询页")
                                            }
                                        }
                                    }
                                }
                            }
                        ) {
                            Text(
                                text = if (detectedCourseCount > 0) {
                                    "⚡ 一键导入 ($detectedCourseCount 门)"
                                } else {
                                    "⚡ 一键抓取本页课表"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            CookieManager.getInstance().flush()
        }
    }
}

/**
 * 独立高校目录选择子页面（不依赖横向滑动，支持搜索过滤、设为当前默认高校或立即进入教务系统）
 */
@Composable
fun UniversityPickerSubPageDialog(
    universityPortals: List<UniversityPortalPreset> = DefaultPresets.universityPortals,
    selectedPortalId: String,
    onDismiss: () -> Unit,
    onSelectPortal: (UniversityPortalPreset) -> Unit,
    onOpenPortalNow: (UniversityPortalPreset) -> Unit
) {
    val visuals = LocalClassTrackVisuals.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredList = remember(searchQuery, universityPortals) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            universityPortals
        } else {
            universityPortals.filter {
                it.schoolName.lowercase().contains(q) ||
                    it.portalLabel.lowercase().contains(q) ||
                    it.url.lowercase().contains(q) ||
                    it.systemType.lowercase().contains(q)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 顶部子页面导航栏
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(0.5.dp, visuals.cardBorderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("← 返回", fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "选择高校教务系统",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "共 ${universityPortals.size} 所",
                            fontSize = 11.sp,
                            color = visuals.mutedTextColor
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("搜索学校名称、简称或教务网址") },
                        placeholder = { Text("例如：北京林业大学 / newjwxt / 强智...", fontSize = 12.sp) }
                    )

                    Text(
                        text = "点击「选定此高校」可将其设为导课页主展示学校；点击「立即导课」直接打开对应教务系统：",
                        fontSize = 11.sp,
                        color = visuals.mutedTextColor
                    )

                    filteredList.forEach { preset ->
                        val isCurrent = preset.id == selectedPortalId
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                width = if (isCurrent) 1.8.dp else 1.dp,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else visuals.cardBorderColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectPortal(preset)
                                    onDismiss()
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = preset.schoolName,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isCurrent) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                                ) {
                                                    Text(
                                                        text = "当前已选",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${preset.portalLabel} · ${preset.systemType}",
                                            fontSize = 12.sp,
                                            color = visuals.mutedTextColor
                                        )
                                        Text(
                                            text = preset.url,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            onSelectPortal(preset)
                                            onDismiss()
                                        },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(
                                            text = if (isCurrent) "✓ 已选为主展示" else "选定此高校",
                                            fontSize = 11.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            onSelectPortal(preset)
                                            onDismiss()
                                            onOpenPortalNow(preset)
                                        },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("立即导课 →", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
