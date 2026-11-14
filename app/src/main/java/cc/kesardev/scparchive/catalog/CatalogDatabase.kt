package cc.kesardev.scparchive.catalog

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class CatalogDatabase(context: Context) : SQLiteOpenHelper(context, "catalog.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE entries (" +
                "id TEXT PRIMARY KEY," +
                "title TEXT NOT NULL," +
                "series_id TEXT NOT NULL," +
                "number INTEGER NOT NULL)"
        )
        db.execSQL("CREATE INDEX idx_entries_number ON entries(number)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS entries")
        onCreate(db)
    }

    fun upsert(entries: List<CatalogEntry>) {
        if (entries.isEmpty()) {
            return
        }
        val db = writableDatabase
        db.beginTransaction()
        try {
            val statement = db.compileStatement(
                "INSERT OR REPLACE INTO entries(id, title, series_id, number) VALUES(?, ?, ?, ?)"
            )
            for (entry in entries) {
                statement.clearBindings()
                statement.bindString(1, entry.id)
                statement.bindString(2, entry.title)
                statement.bindString(3, entry.seriesId)
                statement.bindLong(4, number(entry.id).toLong())
                statement.executeInsert()
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun count(): Int {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM entries", null).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    fun random(): CatalogEntry? {
        readableDatabase.rawQuery(
            "SELECT id, title, series_id FROM entries ORDER BY RANDOM() LIMIT 1",
            null
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                return null
            }
            return CatalogEntry(
                id = cursor.getString(0),
                title = cursor.getString(1),
                seriesId = cursor.getString(2)
            )
        }
    }

    fun search(query: String, limit: Int = 200): List<CatalogEntry> {
        val text = query.trim()
        if (text.isEmpty()) {
            return emptyList()
        }
        val like = "%" + text + "%"
        val cursor = readableDatabase.rawQuery(
            "SELECT id, title, series_id FROM entries " +
                "WHERE id LIKE ? OR title LIKE ? " +
                "ORDER BY CASE WHEN id = ? THEN 0 ELSE 1 END, number LIMIT ?",
            arrayOf(like, like, text.lowercase(), limit.toString())
        )
        val entries = ArrayList<CatalogEntry>(cursor.count)
        cursor.use {
            while (it.moveToNext()) {
                entries.add(
                    CatalogEntry(
                        id = it.getString(0),
                        title = it.getString(1),
                        seriesId = it.getString(2)
                    )
                )
            }
        }
        return entries
    }

    private fun number(id: String): Int {
        return id.substringAfterLast('-').toIntOrNull() ?: Int.MAX_VALUE
    }
}
