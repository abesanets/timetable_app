package com.example.schedule.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Исключения для различных типов ошибок
 */
class NoInternetException(message: String) : Exception(message)
class GroupNotFoundException(message: String) : Exception(message)
class ServerErrorException(message: String) : Exception(message)

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
     * @throws NoInternetException если нет подключения к интернету
     * @throws GroupNotFoundException если группа не найдена
     * @throws ServerErrorException если сервер вернул ошибку
     */
    suspend fun fetchScheduleHtml(group: String): String = withContext(Dispatchers.IO) {
        try {
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
                when (response.code) {
                    404 -> throw GroupNotFoundException("Группа $group не найдена")
                    500, 502, 503, 504 -> throw ServerErrorException("Сервер временно недоступен")
                    else -> throw ServerErrorException("Ошибка сервера (код ${response.code})")
                }
            }
            
            // Возвращаем HTML
            response.body?.string() ?: throw ServerErrorException("Пустой ответ от сервера")
            
        } catch (e: UnknownHostException) {
            throw NoInternetException("Нет подключения к интернету")
        } catch (e: SocketTimeoutException) {
            throw NoInternetException("Превышено время ожидания. Проверьте подключение к интернету")
        } catch (e: IOException) {
            throw NoInternetException("Ошибка сети. Проверьте подключение к интернету")
        } catch (e: NoInternetException) {
            throw e
        } catch (e: GroupNotFoundException) {
            throw e
        } catch (e: ServerErrorException) {
            throw e
        }
    }
}