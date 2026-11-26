package com.example.schedule.features.staff.data

import com.example.schedule.data.preferences.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

class StaffRepository(private val preferencesManager: PreferencesManager) {

    companion object {
        private const val BASE_URL = "https://mgkct.minskedu.gov.by"
        private const val TEACHERS_URL = "$BASE_URL/about/collective/teachers"
        private const val ADMINISTRATION_URL = "$BASE_URL/about/collective/administration"
        private const val CACHE_TTL_MS = 7L * 24 * 60 * 60 * 1000 // 7 days
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val mutex = Mutex()

    @Volatile
    private var memoryCache: List<StaffMember>? = null

    /**
     * Returns cached staff list immediately if fresh, otherwise fetches from the college website.
     * Falls back to stale cache if network fetch fails.
     */
    suspend fun getStaff(forceRefresh: Boolean = false): List<StaffMember> = mutex.withLock {
        if (!forceRefresh) {
            memoryCache?.let { return it }
        }

        val cached = preferencesManager.cachedStaff.first()
        val now = System.currentTimeMillis()

        if (!forceRefresh && cached != null && (now - cached.timestamp) < CACHE_TTL_MS && cached.members.isNotEmpty()) {
            memoryCache = cached.members
            return cached.members
        }

        return try {
            val fetched = fetchAllStaff()
            if (fetched.isNotEmpty()) {
                val sorted = fetched.distinctBy { it.fullName }.sortedBy { it.fullName }
                preferencesManager.saveCachedStaff(CachedStaffData(now, sorted))
                memoryCache = sorted
                sorted
            } else {
                cached?.members?.also { memoryCache = it } ?: emptyList()
            }
        } catch (e: Exception) {
            cached?.members?.also { memoryCache = it } ?: emptyList()
        }
    }

    /**
     * Returns whatever is currently in memory or DataStore cache without hitting the network.
     */
    suspend fun getCachedOnly(): List<StaffMember> {
        memoryCache?.let { return it }
        return preferencesManager.cachedStaff.first()?.members?.also { memoryCache = it } ?: emptyList()
    }

    private suspend fun fetchAllStaff(): List<StaffMember> = coroutineScope {
        val teachersDeferred = async(Dispatchers.IO) { fetchAndParsePage(TEACHERS_URL, "Преподаватели") }
        val adminDeferred = async(Dispatchers.IO) { fetchAndParsePage(ADMINISTRATION_URL, "Администрация") }
        teachersDeferred.await() + adminDeferred.await()
    }

    private fun fetchAndParsePage(url: String, category: String): List<StaffMember> {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .build()

        val html = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            response.body?.string() ?: return emptyList()
        }

        val doc = Jsoup.parse(html, BASE_URL)
        return doc.select(".card.employee-card").mapNotNull { card ->
            val fullName = card.selectFirst(".employee-card-title")?.text()?.trim()
            if (fullName.isNullOrBlank()) return@mapNotNull null

            val position = card.selectFirst("strong.text-primary")?.text()?.trim().orEmpty()

            var qualification: String? = null
            var education = ""

            card.select("p").forEach { p ->
                val text = p.text().trim()
                when {
                    text.startsWith("Квалификация:", ignoreCase = true) -> {
                        qualification = text.removePrefix("Квалификация:").trim().ifBlank { null }
                    }
                    text.startsWith("Образование:", ignoreCase = true) -> {
                        education = text.removePrefix("Образование:").trim()
                    }
                }
            }

            val rawImgSrc = card.selectFirst("img")?.attr("src")?.trim()
            val photoUrl = when {
                rawImgSrc.isNullOrBlank() -> null
                rawImgSrc.startsWith("http") -> rawImgSrc
                rawImgSrc.startsWith("/") -> "$BASE_URL$rawImgSrc"
                else -> "$BASE_URL/$rawImgSrc"
            }

            StaffMember(
                fullName = fullName,
                position = position.ifBlank { category },
                qualification = qualification,
                education = education,
                category = category,
                photoUrl = photoUrl
            )
        }
    }
}
