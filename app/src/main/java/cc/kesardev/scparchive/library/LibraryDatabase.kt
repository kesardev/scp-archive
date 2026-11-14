package cc.kesardev.scparchive.library

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class LibraryEntry(
    val slug: String,
    val title: String,
    val timestamp: Long,
    val progress: Float
)

class LibraryDatabase(context: Context) : SQLiteOpenHelper(context, "library.db", null, 1) {

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
                "progress REAL NOT NULL)"
        )
        db.execSQL("CREATE INDEX idx_favorites_added ON favorites(added_at)")
        db.execSQL("CREATE INDEX idx_history_read ON history(last_read_at)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS favorites")
        db.execSQL("DROP TABLE IF EXISTS history")
        onCreate(db)
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

    fun recordHistory(slug: String, title: String, progress: Float) {
        writableDatabase.execSQL(
            "INSERT OR REPLACE INTO history(slug, title, last_read_at, progress) VALUES(?, ?, ?, ?)",
            arrayOf(slug, title, System.currentTimeMillis(), progress.coerceIn(0f, 1f))
        )
        writableDatabase.execSQL(
            "DELETE FROM history WHERE slug NOT IN (" +
                "SELECT slug FROM history ORDER BY last_read_at DESC LIMIT 500)"
        )
    }

    fun history(): List<LibraryEntry> {
        val result = ArrayList<LibraryEntry>()
        readableDatabase.rawQuery(
            "SELECT slug, title, last_read_at, progress FROM history ORDER BY last_read_at DESC",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    LibraryEntry(
                        slug = cursor.getString(0),
                        title = cursor.getString(1),
                        timestamp = cursor.getLong(2),
                        progress = cursor.getFloat(3)
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
