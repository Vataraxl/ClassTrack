package com.classtrack.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.classtrack.app.data.model.CourseItem
import com.classtrack.app.data.model.DefaultPresets
import com.classtrack.app.data.model.WeekParity

/**
 * 原生 SQLite 离线数据库引擎，负责持久化存储课程表与离散周次数据
 */
class ClassTrackDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_COURSES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                room TEXT NOT NULL,
                teacher TEXT NOT NULL,
                day_of_week INTEGER NOT NULL,
                start_section INTEGER NOT NULL,
                end_section INTEGER NOT NULL,
                start_week INTEGER NOT NULL,
                end_week INTEGER NOT NULL,
                week_parity TEXT NOT NULL,
                weeks_spec TEXT NOT NULL DEFAULT '',
                track_color_index INTEGER NOT NULL,
                note TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_COURSES")
        onCreate(db)
    }

    private fun insertCourseInternal(db: SQLiteDatabase, course: CourseItem): Long {
        val values = ContentValues().apply {
            if (course.id > 0L) put("id", course.id)
            put("name", course.name)
            put("room", course.room)
            put("teacher", course.teacher)
            put("day_of_week", course.dayOfWeek.coerceIn(1, 7))
            put("start_section", course.startSection.coerceIn(1, 12))
            put("end_section", course.endSection.coerceAtLeast(course.startSection).coerceIn(1, 12))
            put("start_week", course.startWeek.coerceAtLeast(1))
            put("end_week", course.endWeek.coerceAtLeast(course.startWeek))
            put("week_parity", course.weekParity.name)
            put("weeks_spec", course.weeksSpec)
            put("track_color_index", course.trackColorIndex)
            put("note", course.note)
        }
        return db.insertWithOnConflict(TABLE_COURSES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getAllCourses(): List<CourseItem> {
        val result = mutableListOf<CourseItem>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_COURSES,
            null,
            null,
            null,
            null,
            null,
            "day_of_week ASC, start_section ASC"
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                val parityStr = c.getString(c.getColumnIndexOrThrow("week_parity"))
                val parity = runCatching { WeekParity.valueOf(parityStr) }.getOrDefault(WeekParity.ALL)
                val weeksSpecCol = c.getColumnIndex("weeks_spec")
                val weeksSpec = if (weeksSpecCol >= 0) c.getString(weeksSpecCol) ?: "" else ""
                result.add(
                    CourseItem(
                        id = c.getLong(c.getColumnIndexOrThrow("id")),
                        name = c.getString(c.getColumnIndexOrThrow("name")),
                        room = c.getString(c.getColumnIndexOrThrow("room")),
                        teacher = c.getString(c.getColumnIndexOrThrow("teacher")),
                        dayOfWeek = c.getInt(c.getColumnIndexOrThrow("day_of_week")),
                        startSection = c.getInt(c.getColumnIndexOrThrow("start_section")),
                        endSection = c.getInt(c.getColumnIndexOrThrow("end_section")),
                        startWeek = c.getInt(c.getColumnIndexOrThrow("start_week")),
                        endWeek = c.getInt(c.getColumnIndexOrThrow("end_week")),
                        weekParity = parity,
                        weeksSpec = weeksSpec,
                        trackColorIndex = c.getInt(c.getColumnIndexOrThrow("track_color_index")),
                        note = c.getString(c.getColumnIndexOrThrow("note"))
                    )
                )
            }
        }
        return result
    }

    fun upsertCourse(course: CourseItem): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            if (course.id > 0L) put("id", course.id)
            put("name", course.name)
            put("room", course.room)
            put("teacher", course.teacher)
            put("day_of_week", course.dayOfWeek.coerceIn(1, 7))
            put("start_section", course.startSection.coerceIn(1, 12))
            put("end_section", course.endSection.coerceAtLeast(course.startSection).coerceIn(1, 12))
            put("start_week", course.startWeek.coerceAtLeast(1))
            put("end_week", course.endWeek.coerceAtLeast(course.startWeek))
            put("week_parity", course.weekParity.name)
            put("weeks_spec", course.weeksSpec)
            put("track_color_index", course.trackColorIndex)
            put("note", course.note)
        }
        return if (course.id > 0L) {
            val updated = db.update(TABLE_COURSES, values, "id = ?", arrayOf(course.id.toString()))
            if (updated == 0) db.insert(TABLE_COURSES, null, values) else course.id
        } else {
            db.insert(TABLE_COURSES, null, values)
        }
    }

    fun deleteCourse(id: Long) {
        writableDatabase.delete(TABLE_COURSES, "id = ?", arrayOf(id.toString()))
    }

    fun replaceAllCourses(courses: List<CourseItem>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_COURSES, null, null)
            courses.forEach { course ->
                insertCourseInternal(db, course.copy(id = 0L))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun clearAllCourses() {
        val db = writableDatabase
        db.delete(TABLE_COURSES, null, null)
    }

    companion object {
        private const val DATABASE_NAME = "classtrack_album.db"
        private const val DATABASE_VERSION = 3
        private const val TABLE_COURSES = "courses"
    }
}
