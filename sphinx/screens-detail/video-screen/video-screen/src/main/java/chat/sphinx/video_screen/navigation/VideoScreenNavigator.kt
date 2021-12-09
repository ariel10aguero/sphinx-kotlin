package chat.sphinx.video_screen.navigation

import androidx.navigation.NavController
import chat.sphinx.video_fullscreen.navigation.ToFullScreenVideoActivity
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.message.MessageId
import io.matthewnelson.android_feature_navigation.requests.PopBackStack
import io.matthewnelson.concept_navigation.BaseNavigationDriver
import io.matthewnelson.concept_navigation.Navigator

abstract class VideoScreenNavigator(
    detailNavigationDriver: BaseNavigationDriver<NavController>
): Navigator<NavController>(detailNavigationDriver) {
    abstract suspend fun closeDetailScreen()

    suspend fun popBackStack() {
        navigationDriver.submitNavigationRequest(PopBackStack())
    }

    suspend fun toFullScreenVideoActivity(
        messageId: MessageId,
        videoFilepath: String?,
        feedId: FeedId? = null,
        currentTime: Int = 0
    ) {
        navigationDriver.submitNavigationRequest(
            ToFullScreenVideoActivity(
                messageId,
                videoFilepath,
                feedId,
                currentTime
            )
        )
    }
}


