package com.beatdrop.kt.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore

/**
 * Port of RN MediaStoreModule + MediaLibraryService — reads the on-device
 * audio library via MediaStore (the user's own files, no network).
 */
class MediaRepository(private val context: Context) {

    fun loadTracks(): List<Track> {
        val out = ArrayList<Track>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
        )
        // Mirror RN filter: real music, >= 20s to skip notification blips.
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND " +
            "${MediaStore.Audio.Media.DURATION} >= 20000"
        val sort = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        context.contentResolver.query(collection, projection, selection, null, sort)?.use { c ->
            val idC = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val tC = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val arC = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val alC = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val alIdC = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dC = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataC = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val daC = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val seen = mutableSetOf<String>()
            while (c.moveToNext()) {
                val id = c.getLong(idC)
                val title = c.getString(tC) ?: "Unknown"
                val artist = c.getString(arC) ?: "Unknown artist"
                val album = c.getString(alC) ?: ""
                val duration = c.getLong(dC)
                val data = c.getString(dataC)
                // De-dup by file path (preferred) or title+artist+duration
                val key = if (!data.isNullOrBlank()) data else "$title|$artist|$duration"
                if (key in seen) continue
                seen.add(key)
                out.add(
                    Track(
                        id = id.toString(),
                        uri = ContentUris.withAppendedId(collection, id),
                        title = title,
                        artist = artist,
                        album = album,
                        albumId = c.getLong(alIdC),
                        durationMs = duration,
                        data = data,
                        dateAdded = c.getLong(daC),
                    )
                )
            }
        }
        return out
    }

    /**
     * Streaming load: invokes [onBatch] with the growing track list every
     * [batchSize] rows, so the UI can render the first songs instantly while the
     * rest of the (potentially large) library keeps loading.
     */
    fun loadTracksStreaming(batchSize: Int = 60, onBatch: (List<Track>) -> Unit) {
        val out = ArrayList<Track>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND " +
            "${MediaStore.Audio.Media.DURATION} >= 20000"
        val sort = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        context.contentResolver.query(collection, projection, selection, null, sort)?.use { c ->
            val idC = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val tC = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val arC = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val alC = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val alIdC = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dC = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataC = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val daC = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val seen = mutableSetOf<String>()
            var sinceEmit = 0
            while (c.moveToNext()) {
                val id = c.getLong(idC)
                val title = c.getString(tC) ?: "Unknown"
                val artist = c.getString(arC) ?: "Unknown artist"
                val album = c.getString(alC) ?: ""
                val duration = c.getLong(dC)
                val data = c.getString(dataC)
                // De-dup key: file path preferred, fallback to title+artist+duration
                val key = if (!data.isNullOrBlank()) data else "$title|$artist|$duration"
                if (key in seen) continue
                seen.add(key)
                out.add(
                    Track(
                        id = id.toString(),
                        uri = ContentUris.withAppendedId(collection, id),
                        title = title,
                        artist = artist,
                        album = album,
                        albumId = c.getLong(alIdC),
                        durationMs = duration,
                        data = data,
                        dateAdded = c.getLong(daC),
                    )
                )
                sinceEmit++
                if (sinceEmit >= batchSize) { onBatch(ArrayList(out)); sinceEmit = 0 }
            }
        }
        onBatch(out) // final
    }

    fun groupAlbums(tracks: List<Track>): List<AlbumGroup> =
        tracks.groupBy { it.album to it.artist }
            .map { (k, v) -> AlbumGroup(k.first, k.second, v.first().artworkUri, v) }
            .sortedBy { it.album.lowercase() }

    fun groupArtists(tracks: List<Track>): List<ArtistGroup> =
        tracks.groupBy { it.artist }
            .map { (artist, v) -> ArtistGroup(artist, v.size, v) }
            .sortedBy { it.artist.lowercase() }

    fun sort(tracks: List<Track>, mode: SortMode): List<Track> = when (mode) {
        SortMode.TITLE_ASC -> tracks.sortedBy { it.title.lowercase() }
        SortMode.TITLE_DESC -> tracks.sortedByDescending { it.title.lowercase() }
        SortMode.ARTIST -> tracks.sortedBy { it.artist.lowercase() }
        SortMode.RECENT -> tracks.sortedByDescending { it.dateAdded }
    }
}
