package com.classtrack.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.classtrack.app.MainActivity
import com.classtrack.app.R
import com.classtrack.app.data.DataRepository
import com.classtrack.app.data.model.ArtistTheme
import com.classtrack.app.data.model.DefaultPresets
import com.classtrack.app.data.model.TrackPlayState
import java.time.LocalDate

/**
 * 《课轨》桌面黑胶/磁带小组件 Provider：
 * 在 ColorOS 主屏幕展示下一轨课程、教室倒计时与三大日音音乐人双语歌词签
 */
class ClassTrackWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            updateSingleWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_NEXT_WIDGET_LYRIC) {
            val prefs = context.getSharedPreferences("classtrack_prefs", Context.MODE_PRIVATE)
            val currentIdx = prefs.getInt("widget_lyric_idx", 0)
            prefs.edit().putInt("widget_lyric_idx", currentIdx + 1).apply()
            refreshAllWidgets(context)
        }
    }

    companion object {
        const val ACTION_NEXT_WIDGET_LYRIC = "com.classtrack.app.ACTION_NEXT_WIDGET_LYRIC"

        fun refreshAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, ClassTrackWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(componentName)
            ids.forEach { id ->
                updateSingleWidget(context, manager, id)
            }
        }

        private fun updateSingleWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val repo = DataRepository(context)
            val theme = repo.artistTheme.value
            val currentWeek = repo.currentWeek.value
            val todayDay = LocalDate.now().dayOfWeek.value
            val todayTracks = repo.buildTracksForDay(todayDay, currentWeek)

            val focusTrack = todayTracks.firstOrNull { it.playState == TrackPlayState.NOW_PLAYING }
                ?: todayTracks.firstOrNull { it.playState == TrackPlayState.UPCOMING }
                ?: todayTracks.firstOrNull()

            val prefs = context.getSharedPreferences("classtrack_prefs", Context.MODE_PRIVATE)
            val lyricIdx = prefs.getInt("widget_lyric_idx", 0)
            val themeLyrics = DefaultPresets.defaultLyrics.filter { it.artistTheme == theme }
                .ifEmpty { DefaultPresets.defaultLyrics }
            val lyric = themeLyrics[lyricIdx.mod(themeLyrics.size)]

            val views = RemoteViews(context.packageName, R.layout.widget_classtrack_vinyl)

            // 根据主题切换高亮色
            val accentColor = when (theme) {
                ArtistTheme.ZTMY -> 0xFF00F5A0.toInt()
                ArtistTheme.YORUSHIKA -> 0xFF38BDF8.toInt()
                ArtistTheme.FUJII_KAZE -> 0xFFFFB703.toInt()
                ArtistTheme.JAY_CHOU -> 0xFFD97706.toInt()
                ArtistTheme.DAVID_TAO -> 0xFFFF6B1A.toInt()
                ArtistTheme.TAYLOR_SWIFT -> 0xFFF472B6.toInt()
                ArtistTheme.AESPA -> 0xFFC084FC.toInt()
                ArtistTheme.CUSTOM_JSON -> 0xFF38BDF8.toInt()
            }

            views.setTextColor(R.id.widget_badge, accentColor)
            views.setTextViewText(
                R.id.widget_badge,
                "${theme.displayName} · 第${currentWeek}周"
            )

            if (focusTrack != null) {
                val trackCode = "Track %02d".format(focusTrack.trackNumber)
                views.setTextViewText(
                    R.id.widget_course_title,
                    "$trackCode · ${focusTrack.course.name}"
                )
                views.setTextViewText(
                    R.id.widget_course_meta,
                    "${focusTrack.course.room} | ${focusTrack.startTime}-${focusTrack.endTime} (${focusTrack.course.sectionRangeText()})"
                )
            } else {
                views.setTextViewText(
                    R.id.widget_course_title,
                    "今日无课 · 自由间奏"
                )
                views.setTextViewText(
                    R.id.widget_course_meta,
                    "享受 ${theme.artistName} 的音乐时光"
                )
            }

            views.setTextViewText(
                R.id.widget_lyric_jp,
                "「${lyric.japaneseLyric}」— 《${lyric.songTitle}》"
            )
            views.setTextViewText(
                R.id.widget_lyric_cn,
                lyric.chineseTranslation
            )

            // 点击整个卡片打开 App
            val openAppIntent = Intent(context, MainActivity::class.java)
            val openPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openPendingIntent)

            // 点击右上角“换词”切换歌词签
            val nextLyricIntent = Intent(context, ClassTrackWidgetProvider::class.java).apply {
                action = ACTION_NEXT_WIDGET_LYRIC
            }
            val nextLyricPending = PendingIntent.getBroadcast(
                context,
                1,
                nextLyricIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_next_lyric_btn, nextLyricPending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
