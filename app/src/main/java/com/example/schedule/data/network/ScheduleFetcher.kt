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
class TeacherNotFoundException(message: String) : Exception(message)
class ServerErrorException(message: String) : Exception(message)

/**
 * Класс для загрузки HTML со страницы расписания
 */
class ScheduleFetcher {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    
    suspend fun fetchScheduleHtml(group: String): String = withContext(Dispatchers.IO) {
        try {
            val url = "https://mgkct.minskedu.gov.by/personnel/for-students/weekly-timetable?group=$group"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                when (response.code) {
                    404 -> throw GroupNotFoundException("Группа $group не найдена")
                    500, 502, 503, 504 -> throw ServerErrorException("Сервер временно недоступен")
                    else -> throw ServerErrorException("Ошибка сервера (код ${response.code})")
                }
            }
            
            response.body?.string() ?: throw ServerErrorException("Пустой ответ от сервера")
            
        } catch (e: UnknownHostException) {
            throw NoInternetException("Нет подключения к интернету")
        } catch (e: SocketTimeoutException) {
            throw NoInternetException("Превышено время ожидания. Проверьте подключение к интернету")
        } catch (e: IOException) {
            if (e is NoInternetException || e is GroupNotFoundException || e is ServerErrorException) throw e
            throw NoInternetException("Ошибка сети. Проверьте подключение к интернету")
        }
    }

    suspend fun fetchTeacherScheduleHtml(teacherName: String): String = withContext(Dispatchers.IO) {
        try {
            val url = "https://mgkct.minskedu.gov.by/personnel/for-teachers/weekly-timetable?teacher=$teacherName"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                when (response.code) {
                    404 -> throw TeacherNotFoundException("Преподаватель $teacherName не найден")
                    500, 502, 503, 504 -> throw ServerErrorException("Сервер временно недоступен")
                    else -> throw ServerErrorException("Ошибка сервера (код ${response.code})")
                }
            }
            
            response.body?.string() ?: throw ServerErrorException("Пустой ответ от сервера")
            
        } catch (e: UnknownHostException) {
            throw NoInternetException("Нет подключения к интернету")
        } catch (e: SocketTimeoutException) {
            throw NoInternetException("Превышено время ожидания. Проверьте подключение к интернету")
        } catch (e: IOException) {
            if (e is NoInternetException || e is TeacherNotFoundException || e is ServerErrorException) throw e
            throw NoInternetException("Ошибка сети. Проверьте подключение к интернету")
        }
    }
}