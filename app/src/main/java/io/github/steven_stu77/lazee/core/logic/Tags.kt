package io.github.steven_stu77.lazee.core.logic

/** "#旅行 dinner #team" → ["旅行", "team"]. Tags are letters/digits/underscore after '#'. */
object Tags {
    private val pattern = Regex("#([\\p{L}\\p{N}_]+)")

    fun parse(note: String?): List<String> =
        if (note.isNullOrBlank()) emptyList()
        else pattern.findAll(note).map { it.groupValues[1] }.distinct().toList()
}
