package com.example.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.LegalPortalCatalog
import com.example.model.LegalPortalSource
import com.example.model.PortalHeadline
import com.example.model.PortalSyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private val Context.legalSyncDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mrgiang_law_portal_sync"
)

data class PersistedSyncSnapshot(
    val autoSyncEnabled: Boolean,
    val lastSyncMillis: Long,
    val statuses: List<PortalSyncStatus>
)

data class LegalSyncBatchResult(
    val syncedAtMillis: Long,
    val statuses: List<PortalSyncStatus>,
    val newlyDetectedCount: Int
)

class LegalSyncRepository(private val context: Context) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun observeSnapshot(): Flow<PersistedSyncSnapshot> {
        return context.legalSyncDataStore.data.map { prefs ->
            val autoEnabled = prefs[KEY_AUTO_SYNC_ENABLED] ?: true
            val lastSync = prefs[KEY_LAST_SYNC_MILLIS] ?: 0L
            val rawJson = prefs[KEY_SYNC_SNAPSHOT_JSON].orEmpty()
            val statuses = deserializeStatuses(rawJson)
            PersistedSyncSnapshot(
                autoSyncEnabled = autoEnabled,
                lastSyncMillis = lastSync,
                statuses = statuses
            )
        }
    }

    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        context.legalSyncDataStore.edit { prefs ->
            prefs[KEY_AUTO_SYNC_ENABLED] = enabled
        }
    }

    suspend fun syncAllPortals(previousHeadlines: Set<String> = emptySet()): LegalSyncBatchResult =
        withContext(Dispatchers.IO) {
            val statuses = coroutineScope {
                LegalPortalCatalog.portals.map { portal ->
                    async { fetchPortalStatus(portal) }
                }.awaitAll()
            }
            val now = System.currentTimeMillis()
            val currentTitles = statuses.flatMap { it.headlines }.map { it.title }.toSet()
            val newlyDetected = if (previousHeadlines.isEmpty()) {
                0
            } else {
                currentTitles.count { it !in previousHeadlines }
            }

            context.legalSyncDataStore.edit { prefs ->
                prefs[KEY_LAST_SYNC_MILLIS] = now
                prefs[KEY_SYNC_SNAPSHOT_JSON] = serializeStatuses(statuses)
            }

            LegalSyncBatchResult(
                syncedAtMillis = now,
                statuses = statuses,
                newlyDetectedCount = newlyDetected
            )
        }

    private fun fetchPortalStatus(portal: LegalPortalSource): PortalSyncStatus {
        val startedNs = System.nanoTime()
        return try {
            val request = Request.Builder()
                .url(portal.baseUrl)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                )
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val latencyMs = ((System.nanoTime() - startedNs) / 1_000_000L).coerceAtLeast(1L)
                val code = response.code
                val bodyString = response.body?.string().orEmpty().take(350_000)
                val baseHttpUrl = response.request.url
                val pageTitle = extractHtmlTitle(bodyString)
                val headlines = if (response.isSuccessful && bodyString.isNotBlank()) {
                    extractLegalHeadlines(
                        portal = portal,
                        html = bodyString,
                        resolvedBaseUrl = baseHttpUrl.toString()
                    )
                } else {
                    emptyList()
                }
                val note = when {
                    response.isSuccessful -> ""
                    code == 403 || code == 429 -> "Cổng bảo vệ truy cập tự động (HTTP $code) — nhấn Mở cổng để tra cứu trực tiếp"
                    else -> "Cổng phản hồi mã HTTP $code"
                }

                PortalSyncStatus(
                    portalId = portal.id,
                    portalName = portal.name,
                    domain = portal.domain,
                    group = portal.group,
                    isReachable = response.isSuccessful,
                    httpCode = code,
                    latencyMs = latencyMs,
                    pageTitle = pageTitle,
                    headlines = headlines,
                    syncedAtMillis = System.currentTimeMillis(),
                    errorNote = note
                )
            }
        } catch (e: Exception) {
            val latencyMs = ((System.nanoTime() - startedNs) / 1_000_000L).coerceAtLeast(1L)
            Log.w(TAG, "Portal sync failed for ${portal.domain}: ${e.message}")
            PortalSyncStatus(
                portalId = portal.id,
                portalName = portal.name,
                domain = portal.domain,
                group = portal.group,
                isReachable = false,
                httpCode = null,
                latencyMs = latencyMs,
                pageTitle = "",
                headlines = emptyList(),
                syncedAtMillis = System.currentTimeMillis(),
                errorNote = "Không thể kết nối tự động (${e.localizedMessage ?: "Timeout"})"
            )
        }
    }

    internal fun extractHtmlTitle(html: String): String {
        val match = TITLE_REGEX.find(html) ?: return ""
        return cleanHtmlText(match.groupValues[1]).take(120)
    }

    internal fun extractLegalHeadlines(
        portal: LegalPortalSource,
        html: String,
        resolvedBaseUrl: String
    ): List<PortalHeadline> {
        val baseUrl = resolvedBaseUrl.toHttpUrlOrNull() ?: portal.baseUrl.toHttpUrlOrNull()
        val seenTitles = HashSet<String>()
        val collected = ArrayList<PortalHeadline>()

        ANCHOR_REGEX.findAll(html).forEach { match ->
            if (collected.size >= MAX_HEADLINES_PER_PORTAL) return@forEach
            val rawHref = match.groupValues[1].trim()
            val rawText = cleanHtmlText(match.groupValues[2])

            if (rawHref.isEmpty() || rawHref.startsWith("#") || rawHref.startsWith("javascript:", ignoreCase = true)) {
                return@forEach
            }
            if (rawText.length < 22 || rawText.length > 220) {
                return@forEach
            }
            val lower = rawText.lowercase()
            if (!LEGAL_KEYWORDS.any { lower.contains(it) }) {
                return@forEach
            }
            if (seenTitles.add(lower)) {
                val resolvedUrl = baseUrl?.resolve(rawHref)?.toString() ?: portal.baseUrl
                collected.add(
                    PortalHeadline(
                        portalId = portal.id,
                        portalDomain = portal.domain,
                        title = rawText,
                        url = resolvedUrl
                    )
                )
            }
        }
        return collected
    }

    private fun cleanHtmlText(raw: String): String {
        return raw
            .replace(HTML_TAG_REGEX, " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(WHITESPACE_REGEX, " ")
            .trim()
    }

    private fun serializeStatuses(statuses: List<PortalSyncStatus>): String {
        val array = JSONArray()
        statuses.forEach { status ->
            val obj = JSONObject().apply {
                put("portalId", status.portalId)
                put("isReachable", status.isReachable)
                put("httpCode", status.httpCode ?: -1)
                put("latencyMs", status.latencyMs)
                put("pageTitle", status.pageTitle)
                put("syncedAtMillis", status.syncedAtMillis)
                put("errorNote", status.errorNote)
                val headlinesArr = JSONArray()
                status.headlines.forEach { headline ->
                    headlinesArr.put(
                        JSONObject().apply {
                            put("title", headline.title)
                            put("url", headline.url)
                        }
                    )
                }
                put("headlines", headlinesArr)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeStatuses(rawJson: String): List<PortalSyncStatus> {
        val portalMap = LegalPortalCatalog.portals.associateBy { it.id }
        if (rawJson.isBlank()) {
            return LegalPortalCatalog.portals.map { portal ->
                PortalSyncStatus(
                    portalId = portal.id,
                    portalName = portal.name,
                    domain = portal.domain,
                    group = portal.group
                )
            }
        }
        return try {
            val array = JSONArray(rawJson)
            val parsedById = HashMap<String, PortalSyncStatus>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val portalId = obj.optString("portalId")
                val portal = portalMap[portalId] ?: continue
                val codeRaw = obj.optInt("httpCode", -1)
                val headlinesArr = obj.optJSONArray("headlines") ?: JSONArray()
                val headlines = ArrayList<PortalHeadline>()
                for (j in 0 until headlinesArr.length()) {
                    val hObj = headlinesArr.getJSONObject(j)
                    headlines.add(
                        PortalHeadline(
                            portalId = portal.id,
                            portalDomain = portal.domain,
                            title = hObj.optString("title"),
                            url = hObj.optString("url", portal.baseUrl)
                        )
                    )
                }
                parsedById[portalId] = PortalSyncStatus(
                    portalId = portal.id,
                    portalName = portal.name,
                    domain = portal.domain,
                    group = portal.group,
                    isReachable = obj.optBoolean("isReachable", false),
                    httpCode = if (codeRaw > 0) codeRaw else null,
                    latencyMs = obj.optLong("latencyMs", 0L),
                    pageTitle = obj.optString("pageTitle", ""),
                    headlines = headlines,
                    syncedAtMillis = obj.optLong("syncedAtMillis", 0L),
                    errorNote = obj.optString("errorNote", "")
                )
            }
            LegalPortalCatalog.portals.map { portal ->
                parsedById[portal.id] ?: PortalSyncStatus(
                    portalId = portal.id,
                    portalName = portal.name,
                    domain = portal.domain,
                    group = portal.group
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse persisted portal sync snapshot", e)
            LegalPortalCatalog.portals.map { portal ->
                PortalSyncStatus(
                    portalId = portal.id,
                    portalName = portal.name,
                    domain = portal.domain,
                    group = portal.group
                )
            }
        }
    }

    private companion object {
        const val TAG = "LegalSyncRepository"
        const val MAX_HEADLINES_PER_PORTAL = 4

        val KEY_AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
        val KEY_LAST_SYNC_MILLIS = longPreferencesKey("last_sync_millis")
        val KEY_SYNC_SNAPSHOT_JSON = stringPreferencesKey("sync_snapshot_json")

        val TITLE_REGEX = Regex("(?is)<title[^>]*>(.*?)</title>")
        val ANCHOR_REGEX = Regex("(?is)<a\\s+[^>]*href\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>(.*?)</a>")
        val HTML_TAG_REGEX = Regex("(?is)<[^>]+>")
        val WHITESPACE_REGEX = Regex("\\s+")

        val LEGAL_KEYWORDS = listOf(
            "luật", "nghị định", "thông tư", "quyết định", "nghị quyết",
            "công báo", "chỉ thị", "pháp lệnh", "hợp nhất", "bộ luật",
            "thi hành án", "đất đai", "hôn nhân", "dân sự", "tương trợ tư pháp"
        )
    }
}
