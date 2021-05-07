package chat.sphinx.feature_network_tor

import android.app.Application
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import chat.sphinx.concept_network_tor.SocksProxyAddress
import chat.sphinx.concept_network_tor.TorManager
import chat.sphinx.concept_network_tor.TorServiceState
import chat.sphinx.concept_network_tor.TorState as KTorState
import chat.sphinx.concept_network_tor.TorNetworkState as KTorNetworkState
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.d
import chat.sphinx.logger.e
import chat.sphinx.logger.i
import io.matthewnelson.build_config.BuildConfigDebug
import io.matthewnelson.build_config.BuildConfigVersionCode
import io.matthewnelson.topl_service.TorServiceController
import io.matthewnelson.topl_service.lifecycle.BackgroundManager
import io.matthewnelson.topl_service.notification.ServiceNotification
import io.matthewnelson.topl_service_base.BaseServiceConsts
import io.matthewnelson.topl_service_base.ServiceExecutionHooks
import io.matthewnelson.topl_service_base.TorPortInfo
import io.matthewnelson.topl_service_base.TorServiceEventBroadcaster
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect

class TorManagerAndroid(
    application: Application,
    buildConfigDebug: BuildConfigDebug,
    buildConfigVersionCode: BuildConfigVersionCode,
    private val LOG: SphinxLogger,
): TorManager {

    companion object {
        const val TAG = "TorManagerAndroid"
    }

    private inner class SphinxBroadcaster: TorServiceEventBroadcaster() {

        @Suppress("RemoveExplicitTypeArguments", "PropertyName")
        val _torServiceStateFlow: MutableStateFlow<TorServiceState> by lazy {
            MutableStateFlow<TorServiceState>(TorServiceState.OnDestroy(-1))
        }

        override fun broadcastServiceLifecycleEvent(event: String, hashCode: Int) {
            when (event) {
                BaseServiceConsts.ServiceLifecycleEvent.CREATED -> {
                    TorServiceState.OnCreate(hashCode)
                }
                BaseServiceConsts.ServiceLifecycleEvent.DESTROYED -> {
                    TorServiceState.OnDestroy(hashCode)
                }
                BaseServiceConsts.ServiceLifecycleEvent.ON_BIND -> {
                    TorServiceState.OnBind(hashCode)
                }
                BaseServiceConsts.ServiceLifecycleEvent.ON_UNBIND -> {
                    TorServiceState.OnUnbind(hashCode)
                }
                BaseServiceConsts.ServiceLifecycleEvent.TASK_REMOVED -> {
                    TorServiceState.OnTaskRemoved(hashCode)
                }
                else -> {
                    null
                }
            }?.let { state ->
                _torServiceStateFlow.value = state
            }
        }

        override fun broadcastBandwidth(bytesRead: String, bytesWritten: String) {}

        override fun broadcastDebug(msg: String) {
            LOG.d(TAG, msg)
        }

        override fun broadcastException(msg: String?, e: Exception) {
            LOG.e(TAG, msg ?: "", e)
        }

        override fun broadcastLogMessage(logMessage: String?) {}

        override fun broadcastNotice(msg: String) {
            LOG.i(TAG, msg)
        }

        @Suppress("RemoveExplicitTypeArguments", "PropertyName")
        val _socksProxyAddressStateFlow: MutableStateFlow<SocksProxyAddress?> by lazy {
            MutableStateFlow<SocksProxyAddress?>(null)
        }

        override fun broadcastPortInformation(torPortInfo: TorPortInfo) {
            _socksProxyAddressStateFlow.value = torPortInfo.socksPort?.let { SocksProxyAddress(it) }
        }

        @Suppress("RemoveExplicitTypeArguments", "PropertyName")
        val _torStateFlow: MutableStateFlow<KTorState> by lazy {
            MutableStateFlow<KTorState>(KTorState.Off)
        }

        @Suppress("RemoveExplicitTypeArguments", "PropertyName")
        val _torNetworkStateFlow: MutableStateFlow<KTorNetworkState> by lazy {
            MutableStateFlow<KTorNetworkState>(KTorNetworkState.Disabled)
        }

        override fun broadcastTorState(state: String, networkState: String) {
            _torStateFlow.value = if (state == TorState.ON) {
                KTorState.On
            } else {
                KTorState.Off
            }

            _torNetworkStateFlow.value = if (networkState == TorNetworkState.ENABLED) {
                KTorNetworkState.Enabled
            } else {
                KTorNetworkState.Disabled
            }
        }
    }

    private val broadcaster = SphinxBroadcaster()

    override val socksProxyAddressStateFlow: StateFlow<SocksProxyAddress?>
        get() = broadcaster._socksProxyAddressStateFlow.asStateFlow()

    override val torStateFlow: StateFlow<KTorState>
        get() = broadcaster._torStateFlow.asStateFlow()
    override val torNetworkStateFlow: StateFlow<KTorNetworkState>
        get() = broadcaster._torNetworkStateFlow.asStateFlow()

    override val torServiceStateFlow: StateFlow<TorServiceState>
        get() = broadcaster._torServiceStateFlow.asStateFlow()

    override fun startTor() {
        TorServiceController.startTor()
    }

    override fun stopTor() {
        TorServiceController.stopTor()
    }

    override fun restartTor() {
        TorServiceController.restartTor()
    }

    override fun newIdentity() {
        TorServiceController.newIdentity()
    }

    init {
        TorServiceController.Builder(
            application,

            torServiceNotificationBuilder = ServiceNotification.Builder(
                channelName = TorManager.NOTIFICATION_CHANNEL_NAME,
                channelID = TorManager.NOTIFICATION_CHANNEL_ID,
                channelDescription = TorManager.NOTIFICATION_CHANNEL_DESCRIPTION,
                notificationID = TorManager.NOTIFICATION_ID
            )
//                .setImageTorNetworkingEnabled()
//                .setImageTorNetworkingDisabled()
//                .setImageTorDataTransfer()
//                .setImageTorErrors()
//                .setCustomColor()
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .enableTorRestartButton(enable = false)
                .enableTorStopButton(enable = false)
                .showNotification(show = true)
                .also { builder ->
                    application.packageManager
                        ?.getLaunchIntentForPackage(application.packageName)
                        ?.let { intent ->
                            builder.setContentIntent(
                                PendingIntent.getActivity(application, 0, intent, 0)
                            )
                        }
                },

            backgroundManagerPolicy = BackgroundManager.Builder()
                .runServiceInForeground(killAppIfTaskIsRemoved = true),

            buildConfigVersionCode = buildConfigVersionCode.value,
            defaultTorSettings = SphinxTorSettings(),
            geoipAssetPath = "common/geoip",
            geoip6AssetPath = "common/geoip6",
        )

            // ServiceController Builder Options

            .addTimeToDisableNetworkDelay(milliseconds = 4_000)
//            .addTimeToRestartTorDelay()
//            .addTimeToStopServiceDelay()
//            .disableStopServiceOnTaskRemoved()
            .setBuildConfigDebug(buildConfigDebug = buildConfigDebug.value)
            .setEventBroadcaster(eventBroadcaster = broadcaster)
//            .setServiceExecutionHooks()
//            .useCustomTorConfigFiles()
            .build()

        LOG.d(TAG, "TorOnionProxyLibrary-Android initialized")
    }
}
