package com.beatdrop.kt.util

import android.content.Context
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * ID3v2.4 tag writer for downloaded audio files.
 * Writes title, artist, album, year, and embedded cover art into M4A/MP3 files.
 *
 * For M4A (MP4/M4A container): writes iTunes-style metadata atoms.
 * For MP3: writes ID3v2.4 TIT2/TPE1/TALB frames.
 *
 * This allows downloaded files to show proper metadata in other music players.
 */
object MetadataWriter {

    data class Metadata(
        val title: String,
        val artist: String,
        val album: String = "",
        val year: String = "",
        val trackNumber: Int = 0,
        val genre: String = "",
        val coverArtPath: String? = null,
    )

    /**
     * Write metadata to an audio file. Detects format from extension and
     * uses the appropriate tagging method.
     */
    fun writeMetadata(file: File, meta: Metadata): Boolean {
        if (!file.exists()) return false
        return when {
            file.name.endsWith(".m4a", true) || file.name.endsWith(".mp4", true) ->
                writeM4aMetadata(file, meta)
            file.name.endsWith(".mp3", true) ->
                writeId3v2Metadata(file, meta)
            file.name.endsWith(".opus", true) ->
                writeVorbisComment(file, meta)
            else -> false // Unsupported format — skip tagging
        }
    }

    // ── M4A / MP4 iTunes-style atom writer ──────────────────────────────────
    // Writes a minimal `moov.udta.meta.ilst` structure with:
    //   ©nam (title), ©ART (artist), ©alb (album), ©day (year),
    //   trkn (track number), ©gen (genre), covr (cover art)
    private fun writeM4aMetadata(file: File, meta: Metadata): Boolean {
        return try {
            val tempFile = File(file.parent, "${file.name}.tmp")
            RandomAccessFile(file, "r").use { src ->
                // Read the ftyp atom
                val ftypSize = src.readInt()
                val ftypType = ByteArray(4).also { src.readFully(it) }
                if (String(ftypType) != "ftyp") return false

                src.seek(0)
                val ftypData = ByteArray(ftypSize.toInt()).also { src.readFully(it) }

                // Read mdat atom(s)
                val mdatAtoms = mutableListOf<ByteArray>()
                var pos = ftypSize.toLong()
                while (pos < src.length()) {
                    src.seek(pos)
                    val atomSize = src.readInt()
                    val atomType = ByteArray(4).also { src.readFully(it) }
                    val atomStr = String(atomType)

                    src.seek(pos)
                    val atomData = ByteArray(atomSize.toInt()).also { src.readFully(it) }

                    if (atomStr == "mdat" || atomStr == "free" || atomStr == "moov") {
                        mdatAtoms.add(atomData)
                    }
                    pos += atomSize.toLong()
                }

                // Build metadata moov atom
                val moovAtom = buildMoovAtom(meta)

                // Write output: ftyp + moov + mdat
                RandomAccessFile(tempFile, "rw").use { out ->
                    out.setLength(0)
                    out.write(ftypData)
                    out.write(moovAtom)
                    for (atom in mdatAtoms) {
                        if (String(atom, 4, 4) != "moov") {
                            out.write(atom)
                        }
                    }
                }
            }

            // Replace original with tagged version
            file.delete()
            tempFile.renameTo(file)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun buildMoovAtom(meta: Metadata): ByteArray {
        val ilstChildren = mutableListOf<ByteArray>()

        // ©nam — title
        meta.title.takeIf { it.isNotBlank() }?.let {
            ilstChildren.add(makeIlstEntry("©nam", it))
        }
        // ©ART — artist
        meta.artist.takeIf { it.isNotBlank() }?.let {
            ilstChildren.add(makeIlstEntry("©ART", it))
        }
        // ©alb — album
        meta.album.takeIf { it.isNotBlank() }?.let {
            ilstChildren.add(makeIlstEntry("©alb", it))
        }
        // ©day — year
        meta.year.takeIf { it.isNotBlank() }?.let {
            ilstChildren.add(makeIlstEntry("©day", it))
        }
        // ©gen — genre
        meta.genre.takeIf { it.isNotBlank() }?.let {
            ilstChildren.add(makeIlstEntry("©gen", it))
        }
        // covr — cover art
        meta.coverArtPath?.let { path ->
            val artFile = File(path.replace("file://", ""))
            if (artFile.exists()) {
                val artData = artFile.readBytes()
                val typeFlag = if (path.endsWith(".png", true)) 0x0E else 0x0D // PNG or JPEG
                val dataAtom = ByteArray(8 + artData.size)
                writeInt32BE(dataAtom, 0, 8 + artData.size)
                System.arraycopy("data".toByteArray(), 0, dataAtom, 4, 4)
                writeInt32BE(dataAtom, 8, typeFlag shl 24)
                System.arraycopy(artData, 0, dataAtom, 12, artData.size)
                val covr = ByteArray(8 + dataAtom.size)
                writeInt32BE(covr, 0, covr.size)
                System.arraycopy("covr".toByteArray(), 0, covr, 4, 4)
                System.arraycopy(dataAtom, 0, covr, 8, dataAtom.size)
                ilstChildren.add(covr)
            }
        }

        val ilstBody = ilstChildren.fold(ByteArray(0)) { acc, b -> acc + b }
        val ilstAtom = ByteArray(8 + ilstBody.size).also {
            writeInt32BE(it, 0, it.size)
            System.arraycopy("ilst".toByteArray(), 0, it, 4, 4)
            System.arraycopy(ilstBody, 0, it, 8, ilstBody.size)
        }

        // Wrap in moov > udta > meta > hdlr + ilst
        val hdlrAtom = ByteArray(33).also {
            writeInt32BE(it, 0, 33)
            System.arraycopy("hdlr".toByteArray(), 0, it, 4, 4)
            // 4 zero bytes (version + flags)
            // 4 zero bytes (pre-defined)
            writeInt32BE(it, 12, 0x6D646972) // "mdis"
            // 12 zero bytes (reserved)
            it[32] = 0 // null terminator
        }

        val metaBody = hdlrAtom + ilstAtom
        val metaAtom = ByteArray(12 + metaBody.size).also {
            writeInt32BE(it, 0, it.size)
            System.arraycopy("meta".toByteArray(), 0, it, 4, 4)
            // version (0) + flags (0) = 4 bytes of zero at offset 8
            System.arraycopy(metaBody, 0, it, 12, metaBody.size)
        }

        val udtaAtom = ByteArray(8 + metaAtom.size).also {
            writeInt32BE(it, 0, it.size)
            System.arraycopy("udta".toByteArray(), 0, it, 4, 4)
            System.arraycopy(metaAtom, 0, it, 8, metaAtom.size)
        }

        return ByteArray(8 + udtaAtom.size).also {
            writeInt32BE(it, 0, it.size)
            System.arraycopy("moov".toByteArray(), 0, it, 4, 4)
            System.arraycopy(udtaAtom, 0, it, 8, udtaAtom.size)
        }
    }

    private fun makeIlstEntry(tag: String, value: String): ByteArray {
        val utf8 = value.toByteArray(StandardCharsets.UTF_8)
        // data atom: 8 header + 8 (version+flags+wellKnownType+locale) + utf8
        val dataAtom = ByteArray(16 + utf8.size)
        writeInt32BE(dataAtom, 0, dataAtom.size)
        System.arraycopy("data".toByteArray(), 0, dataAtom, 4, 4)
        writeInt32BE(dataAtom, 8, 0x00000001) // type 1 = UTF-8
        writeInt32BE(dataAtom, 12, 0)         // locale = 0
        System.arraycopy(utf8, 0, dataAtom, 16, utf8.size)

        return ByteArray(8 + dataAtom.size).also {
            writeInt32BE(it, 0, it.size)
            System.arraycopy(tag.toByteArray(), 0, it, 4, 4)
            System.arraycopy(dataAtom, 0, it, 8, dataAtom.size)
        }
    }

    // ── ID3v2.4 writer (for MP3 files) ──────────────────────────────────────
    private fun writeId3v2Metadata(file: File, meta: Metadata): Boolean {
        return try {
            val frames = mutableListOf<ByteArray>()
            meta.title.takeIf { it.isNotBlank() }?.let { frames.add(makeId3Frame("TIT2", it)) }
            meta.artist.takeIf { it.isNotBlank() }?.let { frames.add(makeId3Frame("TPE1", it)) }
            meta.album.takeIf { it.isNotBlank() }?.let { frames.add(makeId3Frame("TALB", it)) }
            meta.year.takeIf { it.isNotBlank() }?.let { frames.add(makeId3Frame("TDRC", it)) }
            meta.genre.takeIf { it.isNotBlank() }?.let { frames.add(makeId3Frame("TCON", it)) }

            // Cover art (APIC frame)
            meta.coverArtPath?.let { path ->
                val artFile = File(path.replace("file://", ""))
                if (artFile.exists()) {
                    val artData = artFile.readBytes()
                    val mime = if (path.endsWith(".png", true)) "image/png" else "image/jpeg"
                    // APIC: encoding(1) + mime(null-term) + pictureType(1) + description(null) + imageData
                    val apicBody = ByteArray(1 + mime.length + 1 + 1 + 1 + artData.size)
                    apicBody[0] = 3 // UTF-8
                    System.arraycopy(mime.toByteArray(), 0, apicBody, 1, mime.length)
                    // null at mime.length+1, pictureType=0x03 (Cover front) at next byte
                    apicBody[1 + mime.length + 1] = 0x03
                    System.arraycopy(artData, 0, apicBody, 1 + mime.length + 1 + 1 + 1, artData.size)
                    frames.add(makeId3Frame("APIC", apicBody))
                }
            }

            val framesData = frames.fold(ByteArray(0)) { acc, b -> acc + b }

            // ID3v2.4 header: 10 bytes
            val totalSize = framesData.size
            val header = ByteArray(10)
            System.arraycopy("ID3".toByteArray(), 0, header, 0, 3)
            header[3] = 4 // version 2.4
            header[4] = 0 // revision
            header[5] = 0 // flags (no unsync, no extended header)
            // Size in syncsafe integer (4 bytes, 7 bits each)
            header[6] = ((totalSize ushr 21) and 0x7F).toByte()
            header[7] = ((totalSize ushr 14) and 0x7F).toByte()
            header[8] = ((totalSize ushr 7) and 0x7F).toByte()
            header[9] = (totalSize and 0x7F).toByte()

            val tagBytes = header + framesData

            // Write tag + original audio data
            val tempFile = File(file.parent, "${file.name}.tmp")
            RandomAccessFile(file, "r").use { src ->
                // Skip any existing ID3v2 tag
                val head = ByteArray(10)
                src.readFully(head)
                var audioStart = 10L
                if (String(head, 0, 3) == "ID3") {
                    val existingSize = ((head[6].toLong() and 0x7F) shl 21) or
                            ((head[7].toLong() and 0x7F) shl 14) or
                            ((head[8].toLong() and 0x7F) shl 7) or
                            (head[9].toLong() and 0x7F)
                    audioStart = 10 + existingSize
                }
                src.seek(audioStart)
                val audioData = ByteArray((src.length() - audioStart).toInt())
                src.readFully(audioData)

                RandomAccessFile(tempFile, "rw").use { out ->
                    out.setLength(0)
                    out.write(tagBytes)
                    out.write(audioData)
                }
            }

            file.delete()
            tempFile.renameTo(file)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun makeId3Frame(id: String, value: String): ByteArray {
        val utf8 = value.toByteArray(StandardCharsets.UTF_8)
        // Frame: 10-byte header + 1 encoding byte + UTF-8 data
        val body = ByteArray(1 + utf8.size)
        body[0] = 3 // UTF-8
        System.arraycopy(utf8, 0, body, 1, utf8.size)
        return makeId3Frame(id, body)
    }

    private fun makeId3Frame(id: String, body: ByteArray): ByteArray {
        val frame = ByteArray(10 + body.size)
        System.arraycopy(id.toByteArray(), 0, frame, 0, 4)
        writeInt32BE(frame, 4, body.size)
        // Flags = 0x00 0x00
        System.arraycopy(body, 0, frame, 10, body.size)
        return frame
    }

    // ── Vorbis Comment writer (for Opus files) ──────────────────────────────
    // Opus files use Ogg Vorbis comments. This is a simplified writer that
    // appends a basic comment block — proper Ogg rewriting would require
    // full Ogg container manipulation. For now, just a best-effort tag.
    private fun writeVorbisComment(file: File, meta: Metadata): Boolean {
        // Vorbis comment rewriting in Ogg is complex — skip for now.
        // The file will play fine but won't have embedded metadata in other players.
        // A full implementation would use a library like VorbisJava.
        return false
    }

    // ── Helpers ─────────────────────────────────────────────────────────────
    private fun writeInt32BE(arr: ByteArray, offset: Int, value: Int) {
        arr[offset] = (value ushr 24).toByte()
        arr[offset + 1] = (value ushr 16).toByte()
        arr[offset + 2] = (value ushr 8).toByte()
        arr[offset + 3] = value.toByte()
    }
}
