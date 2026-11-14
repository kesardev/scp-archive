package cc.kesardev.scparchive.catalog

data class SeriesInfo(
    val id: String,
    val name: String,
    val path: String
)

object SeriesCatalog {

    val all: List<SeriesInfo> = buildList {
        for (index in 1..10) {
            val suffix = if (index == 1) "" else "-" + index
            add(
                SeriesInfo(
                    id = "scp-series" + suffix,
                    name = "SCP 系列 " + roman(index),
                    path = "scp-series" + suffix
                )
            )
        }
        for (index in 1..5) {
            val suffix = if (index == 1) "" else "-" + index
            add(
                SeriesInfo(
                    id = "scp-series-cn" + suffix,
                    name = "SCP-CN 系列 " + roman(index),
                    path = "scp-series-cn" + suffix
                )
            )
        }
    }

    private fun roman(value: Int): String {
        return when (value) {
            1 -> "I"
            2 -> "II"
            3 -> "III"
            4 -> "IV"
            5 -> "V"
            6 -> "VI"
            7 -> "VII"
            8 -> "VIII"
            9 -> "IX"
            10 -> "X"
            else -> value.toString()
        }
    }
}
