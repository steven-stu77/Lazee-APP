package io.github.steven_stu77.lazee.core.logic

/** Category name limits (spec §5.12): Chinese up to 4 characters, otherwise up to 12 letters. */
object CategoryRules {
    private fun hasCjk(s: String) = s.any { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.HAN }

    fun maxLength(name: String): Int = if (hasCjk(name)) 4 else 12

    fun clamp(name: String): String = name.take(maxLength(name))

    /** Case-insensitive clash with any existing display name of the same kind. */
    fun isDuplicate(name: String, existing: Collection<String>): Boolean =
        name.trim().lowercase().let { n -> existing.any { it.trim().lowercase() == n } }
}
