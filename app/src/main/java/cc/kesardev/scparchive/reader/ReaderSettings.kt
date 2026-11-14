package cc.kesardev.scparchive.reader

import org.json.JSONObject

data class ReaderSettings(
    val fontSize: Float = 17f,
    val lineHeight: Float = 1.75f,
    val showImages: Boolean = true,
    val margin: Int = 16
) {
    fun toJson(): String {
        return JSONObject()
            .put("fontSize", fontSize.toDouble())
            .put("lineHeight", lineHeight.toDouble())
            .put("showImages", showImages)
            .put("margin", margin)
            .toString()
    }
}
