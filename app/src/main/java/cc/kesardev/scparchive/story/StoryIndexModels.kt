package cc.kesardev.scparchive.story

data class StoryEntry(
    val slug: String,
    val title: String
)

data class StoryGroup(
    val label: String,
    val entries: List<StoryEntry>
)