package chat.sphinx.feature_service_media_player_android.service

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.concept_service_media.UserAction
import chat.sphinx.feature_service_media_player_android.MediaPlayerServiceControllerImpl
import chat.sphinx.feature_service_media_player_android.util.toServiceActionPlay
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

internal abstract class MediaPlayerService: Service() {

    protected abstract val mediaServiceController: MediaPlayerServiceControllerImpl
    protected abstract val dispatchers: CoroutineDispatchers

    inner class MediaPlayerServiceBinder: Binder() {
        fun getCurrentState(): MediaPlayerServiceState.ServiceActive {
            // TODO: Implement
            return MediaPlayerServiceState.ServiceActive.ServiceLoading
        }

        fun processUserAction(userAction: UserAction) {
            this@MediaPlayerService.processUserAction(userAction)
        }
    }

    private val binder: MediaPlayerServiceBinder by lazy {
        MediaPlayerServiceBinder()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    private var mediaPlayer: MediaPlayer? = null

    private fun processUserAction(userAction: UserAction) {
        @Exhaustive
        when (userAction) {
            is UserAction.AdjustSpeed -> {

            }
            is UserAction.ServiceAction.Pause -> {

            }
            is UserAction.ServiceAction.Play -> {

            }
            is UserAction.ServiceAction.Seek -> {

            }
        }
    }

    private val supervisor = SupervisorJob()
    protected val serviceLifecycleScope = CoroutineScope(supervisor)

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.toServiceActionPlay()?.let { processUserAction(it) }
        // TODO: If Null, stop service and don't post notification

        return START_NOT_STICKY
//        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        mediaServiceController.unbindService()
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        // TODO: Clear notification
        supervisor.cancel()
    }
}
