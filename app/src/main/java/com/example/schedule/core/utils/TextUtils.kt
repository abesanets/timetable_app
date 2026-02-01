package com.example.schedule.core.utils

object TextUtils {
    fun getPairsWord(count: Int): String {
        return when {
            count % 10 == 1 && count % 100 != 11 -> "пара"
            count % 10 in 2..4 && (count % 100 < 10 || count % 100 >= 20) -> "пары"
            else -> "пар"
        }
    }
}