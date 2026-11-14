package cc.kesardev.scparchive.home

object PageInput {

    private val SCP_PAGE = Regex("^scp-(?:cn-)?\\d+(?:-[a-z0-9]+)*$")

    /**
     * Accepts 173, scp-173, CN-1000, scp-cn-1000 or a full wiki URL and returns
     * the canonical lower-case slug. Returns null for anything that is not a
     * numbered SCP page.
     */
    fun normalize(raw: String): String? {
        var value = asciiForm(raw).trim().lowercase()
        if (value.isEmpty()) {
            return null
        }
        value = value.substringBefore('#').substringBefore('?')
        value = value.trimEnd('/').substringAfterLast('/')
        value = value.removePrefix("scp-").removePrefix("scp:")
        if (value.matches(Regex("^cn-?\\d+.*"))) {
            value = "cn-" + value.removePrefix("cn-").removePrefix("cn")
        }
        val slug = "scp-" + value
        return if (SCP_PAGE.matches(slug)) slug else null
    }

    /**
     * Chinese input methods often produce full-width hyphens and digits. Map
     * them to ASCII before parsing so CN－1000 still opens scp-cn-1000.
     */
    private fun asciiForm(raw: String): String {
        val builder = StringBuilder(raw.length)
        for (character in raw) {
            val code = character.code
            when {
                code == 0x3000 -> builder.append(' ')
                code in 0xFF01..0xFF5E -> builder.append((code - 0xFEE0).toChar())
                character == '\u2013' || character == '\u2014' || character == '\u2212' ->
                    builder.append('-')
                else -> builder.append(character)
            }
        }
        return builder.toString()
    }
}
