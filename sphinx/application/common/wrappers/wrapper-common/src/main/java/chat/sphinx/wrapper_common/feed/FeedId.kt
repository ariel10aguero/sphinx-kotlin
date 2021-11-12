package chat.sphinx.wrapper_common.feed

@Suppress("NOTHING_TO_INLINE")
inline fun String.toFeedId(): FeedId? =
    try {
        FeedId(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class FeedId(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "FeedId cannot be empty"
        }
    }
}