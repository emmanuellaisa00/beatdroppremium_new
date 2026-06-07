package com.beatdrop.kt.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Channel / artist subscription manager.
 * Users can subscribe to YouTube channels and get their latest uploads.
 * Stored as a simple JSON file.
 */
object Subscriptions {

    data class Channel(
        val channelId: String,
        val name: String,
        val thumbnailUrl: String? = null,
        val subscriberCount: String? = null,
        val platform: String = "YouTube",
        val subscribedAt: Long = System.currentTimeMillis(),
        val lastCheckedAt: Long = 0,
        val latestVideoId: String? = null,
        val latestVideoTitle: String? = null,
    )

    private val gson = Gson()
    private val channels = ConcurrentHashMap<String, Channel>()
    private var subsFile: File? = null

    fun init(context: Context) {
        subsFile = File(context.filesDir, "subscriptions.json")
        load()
    }

    private fun load() {
        val file = subsFile ?: return
        if (!file.exists()) return
        try {
            val json = file.readText()
            val type = object : TypeToken<Map<String, Channel>>() {}.type
            val map: Map<String, Channel> = gson.fromJson(json, type)
            channels.putAll(map)
        } catch (_: Exception) { }
    }

    private fun persist() {
        val file = subsFile ?: return
        try {
            file.writeText(gson.toJson(channels.toMap()))
        } catch (_: Exception) { }
    }

    fun subscribe(channel: Channel) {
        channels[channel.channelId] = channel
        persist()
    }

    fun unsubscribe(channelId: String) {
        channels.remove(channelId)
        persist()
    }

    fun isSubscribed(channelId: String): Boolean = channels.containsKey(channelId)

    fun getAll(): List<Channel> = channels.values.sortedBy { it.name.lowercase() }

    fun updateLatest(channelId: String, videoId: String, title: String) {
        channels[channelId]?.let {
            channels[channelId] = it.copy(
                lastCheckedAt = System.currentTimeMillis(),
                latestVideoId = videoId,
                latestVideoTitle = title,
            )
            persist()
        }
    }

    fun count(): Int = channels.size
}
