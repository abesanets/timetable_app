package com.example.schedule.features.staff.utils

import com.example.schedule.features.staff.data.StaffData
import com.example.schedule.features.staff.data.StaffMember

object StaffUtils {

    /**
     * Parses a room string (e.g. "2-304", "415", "Сп.з") and returns a descriptive string.
     */
    fun getRoomDescription(room: String): String {
        val trimmedRoom = room.trim()
        
        if (trimmedRoom.isBlank() || trimmedRoom == "-") return "Кабинет не указан"
        
        // Check for "gym" or non-numeric special rooms
        if (trimmedRoom.contains("Сп.з", ignoreCase = true) || trimmedRoom.contains("Спорт", ignoreCase = true)) {
            return "Спортивный зал"
        }
        if (trimmedRoom.contains("Акт", ignoreCase = true)) {
            return "Актовый зал"
        }

        // Regex for "Corpus-Room" format (e.g. 2-304)
        val corpusRegex = Regex("""^(\d+)[-](\d+)$""")
        val corpusMatch = corpusRegex.find(trimmedRoom)
        
        if (corpusMatch != null) {
            val (corpus, roomNum) = corpusMatch.destructured
            val floor = roomNum.firstOrNull() ?: '?'
            return "Корпус $corpus, этаж $floor, кабинет $roomNum"
        }

        // Regex for "Room" only format (assumed Corpus 1) (e.g. 304, 703)
        // Usually 3 digits, first is floor.
        val simpleRoomRegex = Regex("""^(\d{3,})$""")
        if (simpleRoomRegex.matches(trimmedRoom)) {
            val floor = trimmedRoom.first()
            return "Корпус 1, этаж $floor, кабинет $trimmedRoom"
        }

        return "Кабинет $trimmedRoom"
    }

    /**
     * Tries to find a StaffMember based on a short name from the schedule (e.g. "Козел Г.В.").
     */
    fun findStaffByShortName(shortName: String): StaffMember? {
        val cleanShortName = shortName.replace(".", "").trim()
        // e.g. "Козел ГВ"
        
        val parts = cleanShortName.split(" ").filter { it.isNotBlank() }
        if (parts.isEmpty()) return null
        
        val surname = parts[0]
        
        // Helper to normalize strings for comparison
        fun normalize(s: String) = s.lowercase().replace("ё", "е")

        // Search in all lists
        val allStaff = StaffData.administration + StaffData.teachers + StaffData.employees
        
        return allStaff.find { member ->
            val memberParts = member.fullName.split(" ")
            val memberSurname = memberParts.getOrNull(0) ?: ""
            
            // Check surname match
            if (normalize(memberSurname) == normalize(surname)) {
                // If initials are present in short name, try to match them
                if (parts.size > 1) {
                    val initials = parts.drop(1).joinToString("")
                    val memberInitials = memberParts.drop(1).mapNotNull { it.firstOrNull() }.joinToString("")
                    
                    // Simple check: if the provided initials match the start of the member's initials
                    // e.g. Short: "ГВ", Member: "Георгий Владимирович" -> "ГВ" -> Match
                    memberInitials.startsWith(initials, ignoreCase = true)
                } else {
                    // Only surname provided, return true (risky if duplicates, but best effort)
                    true
                }
            } else {
                false
            }
        }
    }
}