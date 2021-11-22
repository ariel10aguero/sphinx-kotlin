package chat.sphinx.concept_repository_media

import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_chat.ChatMetaData
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_feed.Feed
import chat.sphinx.wrapper_feed.FeedItem
import chat.sphinx.wrapper_message.Message
import chat.sphinx.wrapper_podcast.PodcastDestination
import kotlinx.coroutines.flow.Flow

interface RepositoryMedia {
    fun getChatById(chatId: ChatId): Flow<Chat?>
    fun updateChatMetaData(chatId: ChatId, metaData: ChatMetaData, shouldSync: Boolean = true)

    fun downloadMediaIfApplicable(
        message: Message,
        sent: Boolean,
    )

    fun streamPodcastPayments(
        chatId: ChatId,
        metaData: ChatMetaData,
        podcastId: String,
        episodeId: String,
        destinations: List<PodcastDestination>
    )

    fun getAllFeedItemsFromFeedId(feedId: FeedId): Flow<List<FeedItem>>
    fun getFeedByFeedId(feedId: FeedId): Flow<Feed?>

    fun getFeedItemById(feedItemId: FeedId): Flow<FeedItem?>
}
