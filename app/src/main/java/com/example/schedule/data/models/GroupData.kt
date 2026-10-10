package com.example.schedule.data.models

data class CollegeGroup(
    val number: String,
    val fullName: String,
    val specialty: String,
    val course: Int
)

object GroupData {
    val allGroups = listOf(
        // 1 курс
        CollegeGroup(number = "104", fullName = "104РТК", specialty = "РТК", course = 1),
        CollegeGroup(number = "105", fullName = "105ТП", specialty = "ТП", course = 1),
        CollegeGroup(number = "106", fullName = "106МНЭ", specialty = "МНЭ", course = 1),
        CollegeGroup(number = "102", fullName = "102М", specialty = "М", course = 1),
        CollegeGroup(number = "103", fullName = "103М", specialty = "М", course = 1),
        CollegeGroup(number = "107", fullName = "107ТЭ", specialty = "ТЭ", course = 1),
        CollegeGroup(number = "108", fullName = "108ТЭ", specialty = "ТЭ", course = 1),
        CollegeGroup(number = "109", fullName = "109ТП", specialty = "ТП", course = 1),

        // 2 курс
        CollegeGroup(number = "96", fullName = "96РТК", specialty = "РТК", course = 2),
        CollegeGroup(number = "97", fullName = "97РТК", specialty = "РТК", course = 2),
        CollegeGroup(number = "98", fullName = "98ТП", specialty = "ТП", course = 2),
        CollegeGroup(number = "99", fullName = "99ТП", specialty = "ТП", course = 2),
        CollegeGroup(number = "100", fullName = "100МНЭ", specialty = "МНЭ", course = 2),
        CollegeGroup(number = "95", fullName = "95М", specialty = "М", course = 2),
        CollegeGroup(number = "101", fullName = "101ТП", specialty = "ТП", course = 2),

        // 3 курс
        CollegeGroup(number = "86", fullName = "86РТК", specialty = "РТК", course = 3),
        CollegeGroup(number = "87", fullName = "87ТП", specialty = "ТП", course = 3),
        CollegeGroup(number = "88", fullName = "88ТП", specialty = "ТП", course = 3),
        CollegeGroup(number = "89", fullName = "89ТП", specialty = "ТП", course = 3),
        CollegeGroup(number = "90", fullName = "90МНЭ", specialty = "МНЭ", course = 3),
        CollegeGroup(number = "91", fullName = "91МНЭ", specialty = "МНЭ", course = 3),
        CollegeGroup(number = "92", fullName = "92МНЭ", specialty = "МНЭ", course = 3),
        CollegeGroup(number = "85", fullName = "85М", specialty = "М", course = 3),
        CollegeGroup(number = "9", fullName = "9СР", specialty = "СР", course = 3),
        CollegeGroup(number = "93", fullName = "93МНЭ", specialty = "МНЭ", course = 3),
        CollegeGroup(number = "94", fullName = "94ТП", specialty = "ТП", course = 3),
        CollegeGroup(number = "169", fullName = "169ТП", specialty = "ТП", course = 3),

        // 4 курс
        CollegeGroup(number = "80", fullName = "80РКТ", specialty = "РКТ", course = 4),
        CollegeGroup(number = "81", fullName = "81ТП", specialty = "ТП", course = 4),
        CollegeGroup(number = "82", fullName = "82ТП", specialty = "ТП", course = 4),
        CollegeGroup(number = "83", fullName = "83МНЭ", specialty = "МНЭ", course = 4),
        CollegeGroup(number = "84", fullName = "84МНЭ", specialty = "МНЭ", course = 4),
        CollegeGroup(number = "79", fullName = "79ТЭ", specialty = "ТЭ", course = 4),
        CollegeGroup(number = "167", fullName = "167ТП", specialty = "ТП", course = 4),
        CollegeGroup(number = "168", fullName = "168МНЭ", specialty = "МНЭ", course = 4)
    )

    /**
     * Finds groups matching the given query by number, full name or specialty.
     */
    fun findGroups(query: String): List<CollegeGroup> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        return allGroups.filter { group ->
            group.number.startsWith(trimmed) ||
            group.fullName.contains(trimmed, ignoreCase = true) ||
            group.specialty.contains(trimmed, ignoreCase = true)
        }.sortedWith(
            compareBy<CollegeGroup> {
                // Exact number match or prefix has highest priority
                if (it.number == trimmed) 0
                else if (it.number.startsWith(trimmed)) 1
                else 2
            }.thenBy { it.course }.thenBy { it.number.toIntOrNull() ?: 999 }
        )
    }

    /**
     * Normalizes query input:
     * - If query contains digits, extracts the group number (e.g. "105ТП" -> "105", "№ 88" -> "88").
     * - Otherwise returns trimmed text (for teacher names).
     */
    fun normalizeQuery(query: String): String {
        val trimmed = query.trim()
        val digitsMatch = Regex("""\d+""").find(trimmed)
        return digitsMatch?.value ?: trimmed
    }
}
