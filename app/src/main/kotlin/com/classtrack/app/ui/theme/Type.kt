package com.classtrack.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.classtrack.app.data.model.ArtistTheme
import com.classtrack.app.data.model.CustomThemeConfig

/**
 * 现代瑞士网格与原生旗舰排版系统：
 * 统一采用高可读性无衬线字体族（避免中文环境下系统 Serif/Monospace 导致的字重断裂与生硬感），
 * 通过精准的字重梯度（Bold / SemiBold / Medium / Normal）与紧凑字距构建清晰的信息层级。
 */
fun buildArtistTypography(
    theme: ArtistTheme,
    customTheme: CustomThemeConfig? = null
): Typography {
    val preferredFamily = when (theme) {
        ArtistTheme.CUSTOM_JSON -> when (customTheme?.fontStyle?.uppercase()) {
            "SERIF" -> FontFamily.Serif
            "MONO" -> FontFamily.Monospace
            else -> FontFamily.SansSerif
        }
        else -> FontFamily.SansSerif
    }

    return Typography(
        headlineMedium = TextStyle(
            fontFamily = preferredFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = (-0.4).sp
        ),
        titleLarge = TextStyle(
            fontFamily = preferredFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            letterSpacing = (-0.3).sp
        ),
        titleMedium = TextStyle(
            fontFamily = preferredFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            letterSpacing = (-0.15).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = preferredFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = preferredFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.sp
        ),
        labelMedium = TextStyle(
            fontFamily = preferredFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            letterSpacing = 0.1.sp
        )
    )
}
