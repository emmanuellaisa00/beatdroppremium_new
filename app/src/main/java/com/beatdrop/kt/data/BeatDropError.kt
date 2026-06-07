package com.beatdrop.kt.data

/**
 * Typed error hierarchy for BeatDrop.
 * Every failure path maps to a specific subclass.
 */
sealed class BeatDropError {
    abstract val message: String
    abstract val cause: Throwable?

    // ── Network ──
    data class NetworkUnavailable(
        override val message: String = "No internet connection",
        override val cause: Throwable? = null,
    ) : BeatDropError()

    data class NetworkTimeout(
        override val message: String = "Request timed out",
        override val cause: Throwable? = null,
    ) : BeatDropError()

    data class HttpError(
        val code: Int,
        override val message: String = "Server error ($code)",
        override val cause: Throwable? = null,
    ) : BeatDropError()

    // ── Media ──
    data class PlaybackFailed(
        override val message: String = "Unable to play track",
        val trackId: String? = null,
        override val cause: Throwable? = null,
    ) : BeatDropError()

    data class DownloadFailed(
        override val message: String = "Download failed",
        val trackId: String? = null,
        override val cause: Throwable? = null,
    ) : BeatDropError()

    data class FileNotFound(
        override val message: String = "File not found on device",
        val path: String? = null,
        override val cause: Throwable? = null,
    ) : BeatDropError()

    data class StorageFull(
        override val message: String = "Not enough storage space",
        override val cause: Throwable? = null,
    ) : BeatDropError()

    data class CodecError(
        override val message: String = "Unsupported audio format",
        override val cause: Throwable? = null,
    ) : BeatDropError()

    // ── Data ──
    data class DataLoadFailed(
        override val message: String = "Failed to load data",
        override val cause: Throwable? = null,
    ) : BeatDropError()

    data class DatabaseError(
        override val message: String = "Database error",
        override val cause: Throwable? = null,
    ) : BeatDropError()

    data class ParseError(
        override val message: String = "Failed to parse response",
        override val cause: Throwable? = null,
    ) : BeatDropError()

    // ── Link resolver ──
    data class InvalidUrl(
        override val message: String = "Invalid link format",
        val url: String? = null,
        override val cause: Throwable? = null,
    ) : BeatDropError()

    data class LinkUnresolvable(
        override val message: String = "Could not resolve link",
        val url: String? = null,
        override val cause: Throwable? = null,
    ) : BeatDropError()

    // ── Auth / Legal ──
    data class TermsRequired(
        override val message: String = "Please accept terms to continue",
        override val cause: Throwable? = null,
    ) : BeatDropError()

    // ── Unknown ──
    data class Unknown(
        override val message: String = "Something went wrong",
        override val cause: Throwable? = null,
    ) : BeatDropError()
}

/**
 * Generic result wrapper — replaces try/catch at call sites.
 */
sealed class BeatDropResult<out T> {
    data class Success<T>(val data: T) : BeatDropResult<T>()
    data class Error(val error: BeatDropError) : BeatDropResult<Nothing>()
}

/**
 * Helper to wrap a throwing block into a BeatDropResult.
 */
inline fun <T> safeCall(block: () -> T): BeatDropResult<T> {
    return try {
        BeatDropResult.Success(block())
    } catch (e: Exception) {
        BeatDropResult.Error(e.toBeatDropError())
    }
}

/**
 * Map common exceptions to typed errors.
 */
fun Throwable.toBeatDropError(): BeatDropError = when (this) {
    is java.net.UnknownHostException -> BeatDropError.NetworkUnavailable(cause = this)
    is java.net.SocketTimeoutException -> BeatDropError.NetworkTimeout(cause = this)
    is java.io.FileNotFoundException -> BeatDropError.FileNotFound(cause = this)
    is java.io.IOException -> BeatDropError.NetworkUnavailable(message = message ?: "I/O error", cause = this)
    is IllegalStateException -> BeatDropError.ParseError(message = message ?: "Invalid state", cause = this)
    is NumberFormatException -> BeatDropError.ParseError(message = message ?: "Invalid number", cause = this)
    is NoSuchElementException -> BeatDropError.DataLoadFailed(message = message ?: "Not found", cause = this)
    else -> BeatDropError.Unknown(message = message ?: "Unexpected error", cause = this)
}

/**
 * Map error to user-friendly display properties.
 */
val BeatDropError.icon: String
    get() = when (this) {
        is BeatDropError.NetworkUnavailable, is BeatDropError.NetworkTimeout -> "📡"
        is BeatDropError.HttpError -> "🔌"
        is BeatDropError.PlaybackFailed -> "🎵"
        is BeatDropError.DownloadFailed -> "⬇️"
        is BeatDropError.FileNotFound -> "📂"
        is BeatDropError.StorageFull -> "💾"
        is BeatDropError.CodecError -> "🎚️"
        is BeatDropError.DataLoadFailed, is BeatDropError.DatabaseError -> "🗄️"
        is BeatDropError.ParseError -> "🔧"
        is BeatDropError.InvalidUrl, is BeatDropError.LinkUnresolvable -> "🔗"
        is BeatDropError.TermsRequired -> "📜"
        is BeatDropError.Unknown -> "⚠️"
    }

val BeatDropError.retryable: Boolean
    get() = when (this) {
        is BeatDropError.NetworkUnavailable,
        is BeatDropError.NetworkTimeout,
        is BeatDropError.HttpError,
        is BeatDropError.DataLoadFailed,
        is BeatDropError.DownloadFailed,
        -> true
        else -> false
    }
