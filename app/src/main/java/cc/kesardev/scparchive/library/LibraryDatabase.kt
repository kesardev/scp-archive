package cc.kesardev.scparchive.library

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class LibraryEntry(
    val slug: String,
    val title: String,
    val timestamp: Long,
    val progress: Float,
    val readCount: Int = 0
)

class LibraryDatabase(context: Context) : SQLiteOpenHelper(context, "library.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE favorites (" +
                "slug TEXT PRIMARY KEY," +
                "title TEXT NOT NULL," +
                "added_at INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE history (" +
                "slug TEXT PRIMARY KEY," +
                "title TEXT NOT NULL," +
                "last_read_at INTEGER NOT NULL," +
                "progress REAL NOT NULL," +
                "read_count INTEGER NOT NULL DEFAULT 0)"
        )
        db.execSQL("CREATE INDEX idx_favorites_added ON favorites(added_at)")
        db.execSQL("CREATE INDEX idx_history_read ON history(last_read_at)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(
                "ALTER TABLE history ADD COLUMN read_count INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    fun isFavorite(slug: String): Boolean {
        readableDatabase.rawQuery(
            "SELECT 1 FROM favorites WHERE slug = ? LIMIT 1",
            arrayOf(slug)
        ).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    fun toggleFavorite(slug: String, title: String): Boolean {
        if (isFavorite(slug)) {
            removeFavorite(slug)
            return false
        }
        writableDatabase.execSQL(
            "INSERT OR REPLACE INTO favorites(slug, title, added_at) VALUES(?, ?, ?)",
            arrayOf(slug, title, System.currentTimeMillis())
        )
        return true
    }

    fun removeFavorite(slug: String) {
        writableDatabase.execSQL("DELETE FROM favorites WHERE slug = ?", arrayOf(slug))
    }

    fun favorites(): List<LibraryEntry> {
        val result = ArrayList<LibraryEntry>()
        readableDatabase.rawQuery(
            "SELECT slug, title, added_at FROM favorites ORDER BY added_at DESC",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    LibraryEntry(
                        slug = cursor.getString(0),
                        title = cursor.getString(1),
                        timestamp = cursor.getLong(2),
                        progress = 0f
                    )
                )
            }
        }
        return result
    }

    fun recordHistory(
        slug: String,
        title: String,
        progress: Float,
        incrementRead: Boolean = false
    ) {
        val db = writableDatabase
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("title", title)
            put("last_read_at", now)
            put("progress", progress.coerceIn(0f, 1f))
        }
        db.beginTransaction()
        try {
            val updated = db.update("history", values, "slug = ?", arrayOf(slug))
            if (updated == 0) {
                values.put("slug", slug)
                values.put("read_count", if (incrementRead) 1 else 0)
                db.insertOrThrow("history", null, values)
            } else if (incrementRead) {
                db.execSQL(
                    "UPDATE history SET read_count = read_count + 1 WHERE slug = ?",
                    arrayOf(slug)
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        db.execSQL(
            "DELETE FROM history WHERE slug NOT IN (" +
                "SELECT slug FROM history ORDER BY last_read_at DESC LIMIT 500)"
        )
    }

    fun history(): List<LibraryEntry> {
        val result = ArrayList<LibraryEntry>()
        readableDatabase.rawQuery(
            "SELECT slug, title, last_read_at, progress, read_count " +
                "FROM history ORDER BY last_read_at DESC",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    LibraryEntry(
                        slug = cursor.getString(0),
                        title = cursor.getString(1),
                        timestamp = cursor.getLong(2),
                        progress = cursor.getFloat(3),
                        readCount = cursor.getInt(4)
                    )
                )
            }
        }
        return result
    }

    fun removeHistory(slug: String) {
        writableDatabase.execSQL("DELETE FROM history WHERE slug = ?", arrayOf(slug))
    }

    fun clearHistory() {
        writableDatabase.execSQL("DELETE FROM history")
    }
}