package chat.sphinx.feature_service_media_player_android.di

import android.app.Application
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.concept_service_media.MediaPlayerServiceController
import chat.sphinx.feature_service_media_player_android.MediaPlayerServiceControllerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object MediaServiceModule {

    @Singleton
    @Provides
    fun provideMediaServiceControllerImpl(
        app: Application,
        dispatchers: CoroutineDispatchers,
        repositoryMedia: RepositoryMedia,
    ): MediaPlayerServiceControllerImpl =
        MediaPlayerServiceControllerImpl(
            app,
            dispatchers,
            repositoryMedia,
        )

    @Provides
    fun provideMediaServiceController(
        mediaServiceControllerImpl: MediaPlayerServiceControllerImpl
    ): MediaPlayerServiceController =
        mediaServiceControllerImpl
}
