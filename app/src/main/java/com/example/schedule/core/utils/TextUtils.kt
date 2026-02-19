package com.example.schedule.core.utils

object TextUtils {
        fun getPairsWord(count: Int): String {
            return when {
                count % 10 == 1 && count % 100 != 11 -> "пара"
                count % 10 in 2..4 && (count % 100 < 10 || count % 100 >= 20) -> "пары"
                else -> "пар"
            }
        }
    
        fun toShortName(fullName: String): String {
            val parts = fullName.trim().split(Regex("\\s+"))
            if (parts.size < 3) return fullName
            
            val lastName = parts[0]
            val firstName = parts[1]
            val middleName = parts[2]
            
            return "$lastName ${firstName.take(1)}. ${middleName.take(1)}."
        }
    }
    