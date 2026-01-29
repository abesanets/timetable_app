package com.example.schedule

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Класс для загрузки HTML со страницы расписания
 */
class ScheduleFetcher {
    
    // HTTP-клиент с таймаутами
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    
    /**
     * Загружает HTML страницы расписания для указанной группы
     * @param group номер группы (например "88")
     * @return HTML-код страницы
     * @throws Exception если произошла ошибка загрузки
     */
    suspend fun fetchScheduleHtml(group: String): String = withContext(Dispatchers.IO) {
        // Формируем URL
        val url = "https://mgkct.minskedu.gov.by/personnel/for-students/weekly-timetable?group=$group"
        
        // Создаём запрос с User-Agent (чтобы сайт не блокировал)
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .build()
        
        // Выполняем запрос
        val response = client.newCall(request).execute()
        
        // Проверяем успешность
        if (!response.isSuccessful) {
            throw Exception("Ошибка HTTP: ${response.code}")
        }
        
        // Возвращаем HTML
        response.body?.string() ?: throw Exception("Пустой ответ от сервера")
    }
}
