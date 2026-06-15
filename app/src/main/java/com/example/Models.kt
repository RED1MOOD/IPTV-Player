package com.example

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import androidx.room.Entity
import androidx.room.PrimaryKey

// --- Xtream API Models ---

@JsonClass(generateAdapter = true)
data class UserInfoWrapper(
    @Json(name = "user_info") val userInfo: UserInfo?,
    @Json(name = "server_info") val serverInfo: ServerInfo?
)

@JsonClass(generateAdapter = true)
data class UserInfo(
    @Json(name = "username") val username: String?,
    @Json(name = "password") val password: String?,
    @Json(name = "auth") val auth: Int?,
    @Json(name = "status") val status: String?,
    @Json(name = "exp_date") val expDate: String?,
    @Json(name = "is_trial") val isTrial: String?,
    @Json(name = "active_cons") val activeCons: String?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "max_connections") val maxConnections: String?,
    @Json(name = "allowed_output_formats") val allowedOutputFormats: List<String>?
)

@JsonClass(generateAdapter = true)
data class ServerInfo(
    @Json(name = "url") val url: String?,
    @Json(name = "port") val port: String?,
    @Json(name = "https_port") val httpsPort: String?,
    @Json(name = "server_tmp") val serverTmp: String?,
    @Json(name = "timezone") val timezone: String?
)

@JsonClass(generateAdapter = true)
data class ChannelCategory(
    @Json(name = "category_id") val categoryId: String,
    @Json(name = "category_name") val categoryName: String,
    @Json(name = "parent_id") val parentId: Int?
)

@JsonClass(generateAdapter = true)
data class LiveStream(
    @Json(name = "num") val num: Int?,
    @Json(name = "name") val name: String?,
    @Json(name = "stream_id") val streamId: Int,
    @Json(name = "stream_icon") val streamIcon: String?,
    @Json(name = "live_class") val liveClass: String?,
    @Json(name = "category_id") val categoryId: String?,
    @Json(name = "custom_sid") val customSid: String?,
    @Json(name = "epg_channel_id") val epgChannelId: String?,
    @Json(name = "tv_archive") val tvArchive: Int?,
    @Json(name = "direct_source") val directSource: String?,
    @Json(name = "tv_archive_duration") val tvArchiveDuration: Int?
)

@JsonClass(generateAdapter = true)
data class ShortEpg(
    @Json(name = "epg_listings") val epgListings: List<EpgListing>?
)

@JsonClass(generateAdapter = true)
data class EpgListing(
    @Json(name = "id") val id: String?,
    @Json(name = "epg_id") val epgId: String?,
    @Json(name = "title") val title: String?, // Base64 encoded
    @Json(name = "lang") val lang: String?,
    @Json(name = "start") val start: String?,
    @Json(name = "end") val end: String?,
    @Json(name = "description") val description: String?, // Base64 encoded
    @Json(name = "start_timestamp") val startTimestamp: String?,
    @Json(name = "stop_timestamp") val stopTimestamp: String?
)

// --- Local Room Entities ---

@Entity(tableName = "favorite_channels")
data class FavoriteChannel(
    @PrimaryKey val streamId: Int,
    val name: String,
    val streamIcon: String?,
    val categoryId: String?,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_channels")
data class RecentChannel(
    @PrimaryKey val streamId: Int,
    val name: String,
    val streamIcon: String?,
    val categoryId: String?,
    val watchedAt: Long = System.currentTimeMillis()
)

// Helper utility for safe Base64 decoding (safeguarding Arabic/multi-language characters)
object Base64Decoder {
    fun decode(input: String?): String {
        if (input.isNullOrBlank()) return ""
        return try {
            val cleaned = input.trim().replace("\n", "").replace("\r", "")
            val decodedBytes = android.util.Base64.decode(cleaned, android.util.Base64.DEFAULT)
            String(decodedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            input // fallback to original on failure
        }
    }
}
