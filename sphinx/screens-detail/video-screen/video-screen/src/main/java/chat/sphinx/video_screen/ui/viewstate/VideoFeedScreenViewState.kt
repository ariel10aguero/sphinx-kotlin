package chat.sphinx.video_screen.ui.viewstate

import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_feed.FeedItem
import chat.sphinx.wrapper_feed.FeedTitle
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class VideoFeedScreenViewState: ViewState<VideoFeedScreenViewState>() {

    object Idle: VideoFeedScreenViewState()

    class FeedLoaded(
        val title: FeedTitle,
        val imageToShow: PhotoUrl?,
        val items: List<FeedItem>
    ): VideoFeedScreenViewState()
}