package cc.kesardev.scparchive.home

data class DailyPick(
    val slug: String,
    val name: String,
    val tagline: String
)

/**
 * A small curated rotation keeps the daily card readable and offline-safe on a
 * fresh install, before any catalog sync has happened.
 */
object DailyPicks {

    val all: List<DailyPick> = listOf(
        DailyPick("scp-173", "SCP-173", "最初的收容物。眨眼之间，它就会移动。"),
        DailyPick("scp-682", "SCP-682", "无法摧毁的憎恶，对一切生命怀有敌意。"),
        DailyPick("scp-049", "SCP-049", "自称疫医，试图治愈它眼中的瘟疫。"),
        DailyPick("scp-096", "SCP-096", "看见它的脸，就再也逃不掉了。"),
        DailyPick("scp-106", "SCP-106", "从腐蚀的墙后伸出手的老人。"),
        DailyPick("scp-087", "SCP-087", "没有尽头的楼梯，和哭声。"),
        DailyPick("scp-999", "SCP-999", "会让人感到快乐的黏液。"),
        DailyPick("scp-3008", "SCP-3008", "走不出去的家具商场。"),
        DailyPick("scp-5000", "SCP-5000", "如果基金会本身才是异常。"),
        DailyPick("scp-2317", "SCP-2317", "门后之物，与七道锁。"),
        DailyPick("scp-2935", "SCP-2935", "一个所有人都已死去的世界。"),
        DailyPick("scp-cn-1000", "SCP-CN-1000", "收容失效之后，谁还值得信任。")
    )

    fun forDay(epochDay: Long): DailyPick {
        val size = all.size
        val index = (((epochDay % size) + size) % size).toInt()
        return all[index]
    }

    fun currentEpochDay(): Long {
        val calendar = java.util.Calendar.getInstance()
        val offset = calendar.get(java.util.Calendar.ZONE_OFFSET) +
            calendar.get(java.util.Calendar.DST_OFFSET)
        return (System.currentTimeMillis() + offset) / 86_400_000L
    }
}
