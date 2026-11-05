package com.classtrack.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.classtrack.app.data.model.ArtistTheme
import com.classtrack.app.data.model.CustomThemeConfig
import com.classtrack.app.data.model.UiStyle

fun parseHexColor(hex: String, fallback: Color): Color {
    val clean = hex.trim().removePrefix("#").removePrefix("0x").removePrefix("0X")
    return runCatching {
        when (clean.length) {
            6 -> Color(("FF$clean").toLong(16))
            8 -> Color(clean.toLong(16))
            else -> fallback
        }
    }.getOrDefault(fallback)
}

@Immutable
data class ClassTrackVisuals(
    val artistTheme: ArtistTheme,
    val themeDisplayName: String,
    val artistDisplayName: String,
    val isDark: Boolean,
    val cardBorderColor: Color,
    val mutedTextColor: Color,
    val cardCornerRadius: Dp,
    val trackBackgroundPalette: List<Color>,
    val trackAccentPalette: List<Color>,
    val uiStyle: UiStyle = UiStyle.CLASSIC
) {
    val isGlass: Boolean get() = uiStyle == UiStyle.GLASS

    fun trackBackground(index: Int): Color =
        trackBackgroundPalette[index.mod(trackBackgroundPalette.size.coerceAtLeast(1))]

    fun trackAccent(index: Int): Color =
        trackAccentPalette[index.mod(trackAccentPalette.size.coerceAtLeast(1))]

    val glassCardBorder: BorderStroke
        get() = BorderStroke(
            width = 0.8.dp,
            brush = Brush.linearGradient(
                0.0f to Color.White.copy(alpha = if (isDark) 0.32f else 0.90f),
                0.5f to Color.White.copy(alpha = if (isDark) 0.10f else 0.45f),
                1.0f to Color.White.copy(alpha = if (isDark) 0.18f else 0.70f)
            )
        )

    fun glassAccentBorder(accent: Color, width: Dp = 1.1.dp): BorderStroke =
        BorderStroke(
            width = width,
            brush = Brush.linearGradient(
                0.0f to accent.copy(alpha = if (isDark) 0.75f else 0.80f),
                0.5f to Color.White.copy(alpha = if (isDark) 0.35f else 0.85f),
                1.0f to accent.copy(alpha = if (isDark) 0.50f else 0.60f)
            )
        )
}

val LocalClassTrackVisuals = staticCompositionLocalOf {
    ClassTrackVisuals(
        artistTheme = ArtistTheme.ZTMY,
        themeDisplayName = ArtistTheme.ZTMY.displayName,
        artistDisplayName = ArtistTheme.ZTMY.artistName,
        isDark = true,
        cardBorderColor = ZtmyBorder,
        mutedTextColor = ZtmyMutedText,
        cardCornerRadius = 12.dp,
        trackBackgroundPalette = ZtmyTrackPalette,
        trackAccentPalette = ZtmyTrackAccentPalette,
        uiStyle = UiStyle.CLASSIC
    )
}

private val ZtmyColorScheme = darkColorScheme(
    primary = ZtmyPrimary,
    onPrimary = Color(0xFF041E13),
    primaryContainer = ZtmySurfaceVariant,
    onPrimaryContainer = ZtmyPrimary,
    secondary = ZtmySecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF351527),
    onSecondaryContainer = Color(0xFFFFB3C8),
    tertiary = ZtmyTertiary,
    background = ZtmyBackground,
    onBackground = ZtmyOnBackground,
    surface = ZtmySurface,
    onSurface = ZtmyOnSurface,
    surfaceVariant = ZtmySurfaceVariant,
    onSurfaceVariant = ZtmyMutedText,
    outline = ZtmyBorder
)

private val YorushikaColorScheme = lightColorScheme(
    primary = YoruPrimary,
    onPrimary = Color.White,
    primaryContainer = YoruSurfaceVariant,
    onPrimaryContainer = YoruPrimary,
    secondary = YoruSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9F3EE),
    onSecondaryContainer = Color(0xFF0D4F48),
    tertiary = YoruTertiary,
    background = YoruBackground,
    onBackground = YoruOnBackground,
    surface = YoruSurface,
    onSurface = YoruOnSurface,
    surfaceVariant = YoruSurfaceVariant,
    onSurfaceVariant = YoruMutedText,
    outline = YoruBorder
)

private val FujiiKazeColorScheme = lightColorScheme(
    primary = KazePrimary,
    onPrimary = Color.White,
    primaryContainer = KazeSurfaceVariant,
    onPrimaryContainer = KazePrimary,
    secondary = KazeSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCEEE4),
    onSecondaryContainer = Color(0xFF17402D),
    tertiary = KazeTertiary,
    background = KazeBackground,
    onBackground = KazeOnBackground,
    surface = KazeSurface,
    onSurface = KazeOnSurface,
    surfaceVariant = KazeSurfaceVariant,
    onSurfaceVariant = KazeMutedText,
    outline = KazeBorder
)

private val JayChouColorScheme = lightColorScheme(
    primary = JayPrimary,
    onPrimary = Color.White,
    primaryContainer = JaySurfaceVariant,
    onPrimaryContainer = JayPrimary,
    secondary = JaySecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDECE6),
    onSecondaryContainer = Color(0xFF15382E),
    tertiary = JayTertiary,
    background = JayBackground,
    onBackground = JayOnBackground,
    surface = JaySurface,
    onSurface = JayOnSurface,
    surfaceVariant = JaySurfaceVariant,
    onSurfaceVariant = JayMutedText,
    outline = JayBorder
)

private val DavidTaoColorScheme = darkColorScheme(
    primary = TaoPrimary,
    onPrimary = Color(0xFF1F0A00),
    primaryContainer = TaoSurfaceVariant,
    onPrimaryContainer = TaoPrimary,
    secondary = TaoSecondary,
    onSecondary = Color(0xFF1E1300),
    secondaryContainer = Color(0xFF33240C),
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = TaoTertiary,
    background = TaoBackground,
    onBackground = TaoOnBackground,
    surface = TaoSurface,
    onSurface = TaoOnSurface,
    surfaceVariant = TaoSurfaceVariant,
    onSurfaceVariant = TaoMutedText,
    outline = TaoBorder
)

private val TaylorSwiftColorScheme = darkColorScheme(
    primary = SwiftPrimary,
    onPrimary = Color(0xFF280619),
    primaryContainer = SwiftSurfaceVariant,
    onPrimaryContainer = SwiftPrimary,
    secondary = SwiftSecondary,
    onSecondary = Color(0xFF1B0B3B),
    secondaryContainer = Color(0xFF2B2152),
    onSecondaryContainer = Color(0xFFDDD6FE),
    tertiary = SwiftTertiary,
    background = SwiftBackground,
    onBackground = SwiftOnBackground,
    surface = SwiftSurface,
    onSurface = SwiftOnSurface,
    surfaceVariant = SwiftSurfaceVariant,
    onSurfaceVariant = SwiftMutedText,
    outline = SwiftBorder
)

private val AespaColorScheme = darkColorScheme(
    primary = AespaPrimary,
    onPrimary = Color(0xFF21073B),
    primaryContainer = AespaSurfaceVariant,
    onPrimaryContainer = AespaPrimary,
    secondary = AespaSecondary,
    onSecondary = Color(0xFF04262E),
    secondaryContainer = Color(0xFF10313D),
    onSecondaryContainer = Color(0xFFA5F3FC),
    tertiary = AespaTertiary,
    background = AespaBackground,
    onBackground = AespaOnBackground,
    surface = AespaSurface,
    onSurface = AespaOnSurface,
    surfaceVariant = AespaSurfaceVariant,
    onSurfaceVariant = AespaMutedText,
    outline = AespaBorder
)

fun themePreviewSwatches(
    theme: ArtistTheme,
    customThemeConfig: CustomThemeConfig = CustomThemeConfig()
): List<Color> = when (theme) {
    ArtistTheme.ZTMY -> listOf(ZtmyBackground, ZtmyPrimary, ZtmySecondary, ZtmyTertiary)
    ArtistTheme.YORUSHIKA -> listOf(YoruBackground, YoruPrimary, YoruSecondary, YoruTertiary)
    ArtistTheme.FUJII_KAZE -> listOf(KazeBackground, KazePrimary, KazeSecondary, KazeTertiary)
    ArtistTheme.JAY_CHOU -> listOf(JayBackground, JayPrimary, JaySecondary, JayTertiary)
    ArtistTheme.DAVID_TAO -> listOf(TaoBackground, TaoPrimary, TaoSecondary, TaoTertiary)
    ArtistTheme.TAYLOR_SWIFT -> listOf(SwiftBackground, SwiftPrimary, SwiftSecondary, SwiftTertiary)
    ArtistTheme.AESPA -> listOf(AespaBackground, AespaPrimary, AespaSecondary, AespaTertiary)
    ArtistTheme.CUSTOM_JSON -> listOf(
        parseHexColor(customThemeConfig.backgroundHex, ZtmyBackground),
        parseHexColor(customThemeConfig.primaryHex, ZtmyPrimary),
        parseHexColor(customThemeConfig.secondaryHex, ZtmySecondary),
        parseHexColor(customThemeConfig.tertiaryHex, ZtmyTertiary)
    )
}

private fun buildCustomColorScheme(cfg: CustomThemeConfig): ColorScheme {
    val bg = parseHexColor(cfg.backgroundHex, ZtmyBackground)
    val surface = parseHexColor(cfg.surfaceHex, ZtmySurface)
    val surfaceVar = parseHexColor(cfg.surfaceVariantHex, ZtmySurfaceVariant)
    val primary = parseHexColor(cfg.primaryHex, ZtmyPrimary)
    val secondary = parseHexColor(cfg.secondaryHex, ZtmySecondary)
    val tertiary = parseHexColor(cfg.tertiaryHex, ZtmyTertiary)
    val onBg = parseHexColor(cfg.onBackgroundHex, if (cfg.isDark) Color.White else Color.Black)
    val onSurface = parseHexColor(cfg.onSurfaceHex, if (cfg.isDark) Color.White else Color.Black)
    val muted = parseHexColor(cfg.mutedTextHex, ZtmyMutedText)
    val border = parseHexColor(cfg.borderHex, ZtmyBorder)

    return if (cfg.isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color(0xFF0A0F18),
            primaryContainer = surfaceVar,
            onPrimaryContainer = primary,
            secondary = secondary,
            onSecondary = Color.White,
            tertiary = tertiary,
            background = bg,
            onBackground = onBg,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVar,
            onSurfaceVariant = muted,
            outline = border
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = surfaceVar,
            onPrimaryContainer = primary,
            secondary = secondary,
            onSecondary = Color.White,
            tertiary = tertiary,
            background = bg,
            onBackground = onBg,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVar,
            onSurfaceVariant = muted,
            outline = border
        )
    }
}

@Composable
fun ClassTrackTheme(
    artistTheme: ArtistTheme = ArtistTheme.ZTMY,
    customThemeConfig: CustomThemeConfig = CustomThemeConfig(),
    uiStyle: UiStyle = UiStyle.CLASSIC,
    content: @Composable () -> Unit
) {
    val colorScheme = when (artistTheme) {
        ArtistTheme.ZTMY -> ZtmyColorScheme
        ArtistTheme.YORUSHIKA -> YorushikaColorScheme
        ArtistTheme.FUJII_KAZE -> FujiiKazeColorScheme
        ArtistTheme.JAY_CHOU -> JayChouColorScheme
        ArtistTheme.DAVID_TAO -> DavidTaoColorScheme
        ArtistTheme.TAYLOR_SWIFT -> TaylorSwiftColorScheme
        ArtistTheme.AESPA -> AespaColorScheme
        ArtistTheme.CUSTOM_JSON -> buildCustomColorScheme(customThemeConfig)
    }

    val visuals = when (artistTheme) {
        ArtistTheme.ZTMY -> ClassTrackVisuals(
            artistTheme = ArtistTheme.ZTMY,
            themeDisplayName = ArtistTheme.ZTMY.displayName,
            artistDisplayName = ArtistTheme.ZTMY.artistName,
            isDark = true,
            cardBorderColor = ZtmyBorder,
            mutedTextColor = ZtmyMutedText,
            cardCornerRadius = if (uiStyle == UiStyle.GLASS) 20.dp else 14.dp,
            trackBackgroundPalette = ZtmyTrackPalette,
            trackAccentPalette = ZtmyTrackAccentPalette,
            uiStyle = uiStyle
        )
        ArtistTheme.YORUSHIKA -> ClassTrackVisuals(
            artistTheme = ArtistTheme.YORUSHIKA,
            themeDisplayName = ArtistTheme.YORUSHIKA.displayName,
            artistDisplayName = ArtistTheme.YORUSHIKA.artistName,
            isDark = false,
            cardBorderColor = YoruBorder,
            mutedTextColor = YoruMutedText,
            cardCornerRadius = if (uiStyle == UiStyle.GLASS) 20.dp else 14.dp,
            trackBackgroundPalette = YoruTrackPalette,
            trackAccentPalette = YoruTrackAccentPalette,
            uiStyle = uiStyle
        )
        ArtistTheme.FUJII_KAZE -> ClassTrackVisuals(
            artistTheme = ArtistTheme.FUJII_KAZE,
            themeDisplayName = ArtistTheme.FUJII_KAZE.displayName,
            artistDisplayName = ArtistTheme.FUJII_KAZE.artistName,
            isDark = false,
            cardBorderColor = KazeBorder,
            mutedTextColor = KazeMutedText,
            cardCornerRadius = if (uiStyle == UiStyle.GLASS) 20.dp else 16.dp,
            trackBackgroundPalette = KazeTrackPalette,
            trackAccentPalette = KazeTrackAccentPalette,
            uiStyle = uiStyle
        )
        ArtistTheme.JAY_CHOU -> ClassTrackVisuals(
            artistTheme = ArtistTheme.JAY_CHOU,
            themeDisplayName = ArtistTheme.JAY_CHOU.displayName,
            artistDisplayName = ArtistTheme.JAY_CHOU.artistName,
            isDark = false,
            cardBorderColor = JayBorder,
            mutedTextColor = JayMutedText,
            cardCornerRadius = if (uiStyle == UiStyle.GLASS) 20.dp else 16.dp,
            trackBackgroundPalette = JayTrackPalette,
            trackAccentPalette = JayTrackAccentPalette,
            uiStyle = uiStyle
        )
        ArtistTheme.DAVID_TAO -> ClassTrackVisuals(
            artistTheme = ArtistTheme.DAVID_TAO,
            themeDisplayName = ArtistTheme.DAVID_TAO.displayName,
            artistDisplayName = ArtistTheme.DAVID_TAO.artistName,
            isDark = true,
            cardBorderColor = TaoBorder,
            mutedTextColor = TaoMutedText,
            cardCornerRadius = if (uiStyle == UiStyle.GLASS) 20.dp else 14.dp,
            trackBackgroundPalette = TaoTrackPalette,
            trackAccentPalette = TaoTrackAccentPalette,
            uiStyle = uiStyle
        )
        ArtistTheme.TAYLOR_SWIFT -> ClassTrackVisuals(
            artistTheme = ArtistTheme.TAYLOR_SWIFT,
            themeDisplayName = ArtistTheme.TAYLOR_SWIFT.displayName,
            artistDisplayName = ArtistTheme.TAYLOR_SWIFT.artistName,
            isDark = true,
            cardBorderColor = SwiftBorder,
            mutedTextColor = SwiftMutedText,
            cardCornerRadius = if (uiStyle == UiStyle.GLASS) 20.dp else 16.dp,
            trackBackgroundPalette = SwiftTrackPalette,
            trackAccentPalette = SwiftTrackAccentPalette,
            uiStyle = uiStyle
        )
        ArtistTheme.AESPA -> ClassTrackVisuals(
            artistTheme = ArtistTheme.AESPA,
            themeDisplayName = ArtistTheme.AESPA.displayName,
            artistDisplayName = ArtistTheme.AESPA.artistName,
            isDark = true,
            cardBorderColor = AespaBorder,
            mutedTextColor = AespaMutedText,
            cardCornerRadius = if (uiStyle == UiStyle.GLASS) 20.dp else 14.dp,
            trackBackgroundPalette = AespaTrackPalette,
            trackAccentPalette = AespaTrackAccentPalette,
            uiStyle = uiStyle
        )
        ArtistTheme.CUSTOM_JSON -> {
            val trackBgs = customThemeConfig.trackBackgroundHexList
                .map { parseHexColor(it, ZtmySurfaceVariant) }
                .ifEmpty { ZtmyTrackPalette }
            val trackAccents = customThemeConfig.trackAccentHexList
                .map { parseHexColor(it, ZtmyPrimary) }
                .ifEmpty { ZtmyTrackAccentPalette }

            ClassTrackVisuals(
                artistTheme = ArtistTheme.CUSTOM_JSON,
                themeDisplayName = customThemeConfig.themeName.ifBlank { "自定义主题" },
                artistDisplayName = customThemeConfig.artistName.ifBlank { "Custom Artist" },
                isDark = customThemeConfig.isDark,
                cardBorderColor = parseHexColor(customThemeConfig.borderHex, ZtmyBorder),
                mutedTextColor = parseHexColor(customThemeConfig.mutedTextHex, ZtmyMutedText),
                cardCornerRadius = if (uiStyle == UiStyle.GLASS) 20.dp else customThemeConfig.cardCornerDp.coerceIn(8, 24).dp,
                trackBackgroundPalette = trackBgs,
                trackAccentPalette = trackAccents,
                uiStyle = uiStyle
            )
        }
    }

    CompositionLocalProvider(LocalClassTrackVisuals provides visuals) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildArtistTypography(artistTheme, customThemeConfig),
            content = content
        )
    }
}

/**
 * 旗舰级纯净光学玻璃绘制修饰符（drawWithCache 零重绘分配）：
 * 摒弃杂乱脏暗色块与圆角裁切溢出的顶边横线，采用克制通透的半透基底 + 顶部柔和漫射光泽。
 */
fun Modifier.liquidGlassSurface(
    isDark: Boolean,
    accentColor: Color,
    tintColor: Color = Color.Unspecified,
    elevated: Boolean = true
): Modifier = this.drawWithCache {
    val w = size.width
    val h = size.height

    val baseTopLeft = if (isDark) {
        Color(0xFF1E2538).copy(alpha = if (elevated) 0.72f else 0.52f)
    } else {
        Color.White.copy(alpha = if (elevated) 0.88f else 0.74f)
    }
    val baseBottomRight = if (tintColor != Color.Unspecified) {
        tintColor.copy(alpha = if (isDark) 0.20f else 0.14f)
    } else if (isDark) {
        Color(0xFF131826).copy(alpha = if (elevated) 0.68f else 0.48f)
    } else {
        Color(0xFFF4F8FD).copy(alpha = if (elevated) 0.78f else 0.62f)
    }

    val bodyBrush = Brush.linearGradient(
        colors = listOf(baseTopLeft, baseBottomRight),
        start = Offset(0f, 0f),
        end = Offset(w, h)
    )

    val topSheenBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark) 0.07f else 0.42f),
            Color.Transparent
        ),
        startY = 0f,
        endY = (h * 0.45f).coerceAtLeast(1f)
    )

    val ambientGlowBrush = Brush.radialGradient(
        colors = listOf(
            accentColor.copy(alpha = if (isDark) 0.08f else 0.06f),
            Color.Transparent
        ),
        center = Offset(w * 0.9f, h * 0.1f),
        radius = (w * 0.7f).coerceAtLeast(24f)
    )

    onDrawBehind {
        drawRect(brush = bodyBrush)
        drawRect(brush = ambientGlowBrush)
        drawRect(
            brush = topSheenBrush,
            size = Size(w, h * 0.45f)
        )
    }
}

/**
 * 跨界面统一一级分组容器（零阴影重分配开销，极致清爽）：
 * - CLASSIC: 纯净卡片底色 + 0.6.dp 极细微对比描边
 * - GLASS: 光学半透磨砂 + 0.8.dp 柔光折射线框
 */
@Composable
fun ClassTrackCard(
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    border: BorderStroke? = null,
    tintColor: Color = Color.Unspecified,
    content: @Composable ColumnScope.() -> Unit
) {
    val visuals = LocalClassTrackVisuals.current
    val effectiveShape = shape ?: RoundedCornerShape(visuals.cardCornerRadius)
    val accent = MaterialTheme.colorScheme.primary

    if (visuals.isGlass) {
        Box(
            modifier = modifier
                .clip(effectiveShape)
                .liquidGlassSurface(
                    isDark = visuals.isDark,
                    accentColor = accent,
                    tintColor = tintColor,
                    elevated = true
                )
                .border(border ?: visuals.glassCardBorder, effectiveShape)
        ) {
            Column(content = content)
        }
    } else {
        Surface(
            modifier = modifier,
            shape = effectiveShape,
            color = MaterialTheme.colorScheme.surface,
            border = border ?: BorderStroke(0.7.dp, visuals.cardBorderColor.copy(alpha = 0.75f))
        ) {
            Column(content = content)
        }
    }
}

/**
 * 二级内嵌轻量子表面：
 * 克制使用边框（仅在高亮选中或必要分隔时强调），消除“盒中盒”的视觉拥挤感。
 */
@Composable
fun ClassTrackSubSurface(
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    classicColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    classicBorder: BorderStroke? = null,
    glassTintColor: Color = Color.Unspecified,
    isHighlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val visuals = LocalClassTrackVisuals.current
    val accent = MaterialTheme.colorScheme.primary
    val effectiveShape = shape ?: RoundedCornerShape(if (visuals.isGlass) 14.dp else 12.dp)

    if (visuals.isGlass) {
        val borderStroke = if (isHighlighted) {
            visuals.glassAccentBorder(accent, width = 1.2.dp)
        } else {
            BorderStroke(
                width = 0.6.dp,
                color = Color.White.copy(alpha = if (visuals.isDark) 0.12f else 0.55f)
            )
        }
        Box(
            modifier = modifier
                .clip(effectiveShape)
                .liquidGlassSurface(
                    isDark = visuals.isDark,
                    accentColor = accent,
                    tintColor = glassTintColor,
                    elevated = isHighlighted
                )
                .border(borderStroke, effectiveShape)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            content = content
        )
    } else {
        val effectiveBorder = classicBorder ?: if (isHighlighted) {
            BorderStroke(1.2.dp, accent.copy(alpha = 0.8f))
        } else {
            null
        }
        Surface(
            modifier = modifier
                .clip(effectiveShape)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            shape = effectiveShape,
            color = classicColor,
            border = effectiveBorder
        ) {
            Box(content = content)
        }
    }
}

/**
 * 零重绘开销全屏流体环境背景（drawWithCache 缓存全部 Brush 对象）：
 * 仅在屏幕尺寸或主题色改变时构建一次渐变对象，日常滑动与页面切换 0 CPU/GPU 分配开销。
 */
@Composable
fun LiquidGlassBackdrop(
    modifier: Modifier = Modifier
) {
    val visuals = LocalClassTrackVisuals.current
    val colorScheme = MaterialTheme.colorScheme

    val primary = colorScheme.primary
    val secondary = colorScheme.secondary
    val tertiary = colorScheme.tertiary
    val baseBg = colorScheme.background
    val isDark = visuals.isDark

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer()
            .drawWithCache {
                val w = size.width
                val h = size.height

                val bgBrush = Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(
                            baseBg,
                            Color(0xFF0E1320),
                            Color(0xFF090C14)
                        )
                    } else {
                        listOf(
                            baseBg,
                            Color(0xFFEDF3FA),
                            Color(0xFFF4EFF8)
                        )
                    }
                )

                val orb1Center = Offset(w * 0.85f, h * 0.12f)
                val orb1Radius = (w * 0.85f).coerceAtLeast(10f)
                val orb1Brush = Brush.radialGradient(
                    colors = listOf(
                        primary.copy(alpha = if (isDark) 0.22f else 0.15f),
                        primary.copy(alpha = if (isDark) 0.06f else 0.04f),
                        Color.Transparent
                    ),
                    center = orb1Center,
                    radius = orb1Radius
                )

                val orb2Center = Offset(w * 0.12f, h * 0.48f)
                val orb2Radius = (w * 0.75f).coerceAtLeast(10f)
                val orb2Brush = Brush.radialGradient(
                    colors = listOf(
                        secondary.copy(alpha = if (isDark) 0.16f else 0.12f),
                        secondary.copy(alpha = if (isDark) 0.04f else 0.03f),
                        Color.Transparent
                    ),
                    center = orb2Center,
                    radius = orb2Radius
                )

                val orb3Center = Offset(w * 0.78f, h * 0.86f)
                val orb3Radius = (w * 0.80f).coerceAtLeast(10f)
                val orb3Brush = Brush.radialGradient(
                    colors = listOf(
                        tertiary.copy(alpha = if (isDark) 0.18f else 0.12f),
                        Color.Transparent
                    ),
                    center = orb3Center,
                    radius = orb3Radius
                )

                onDrawBehind {
                    drawRect(brush = bgBrush)
                    drawCircle(brush = orb1Brush, radius = orb1Radius, center = orb1Center)
                    drawCircle(brush = orb2Brush, radius = orb2Radius, center = orb2Center)
                    drawCircle(brush = orb3Brush, radius = orb3Radius, center = orb3Center)
                }
            }
    )
}

@Composable
fun Theme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    ClassTrackTheme(
        artistTheme = if (darkTheme) ArtistTheme.ZTMY else ArtistTheme.YORUSHIKA,
        content = content
    )
}
