package chat.sphinx.test_network_query

import chat.sphinx.concept_network_client.NetworkClient
import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.concept_network_query_contact.NetworkQueryContact
import chat.sphinx.concept_network_query_invite.NetworkQueryInvite
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_network_query_message.NetworkQueryMessage
import chat.sphinx.concept_network_query_subscription.NetworkQuerySubscription
import chat.sphinx.concept_relay.RelayDataHandler
import chat.sphinx.feature_network_client.NetworkClientImpl
import chat.sphinx.feature_network_query_chat.NetworkQueryChatImpl
import chat.sphinx.feature_network_query_contact.NetworkQueryContactImpl
import chat.sphinx.feature_network_query_invite.NetworkQueryInviteImpl
import chat.sphinx.feature_network_query_lightning.NetworkQueryLightningImpl
import chat.sphinx.feature_network_query_message.NetworkQueryMessageImpl
import chat.sphinx.feature_network_query_subscription.NetworkQuerySubscriptionImpl
import chat.sphinx.feature_relay.RelayDataHandlerImpl
import chat.sphinx.wrapper_relay.JavaWebToken
import chat.sphinx.wrapper_relay.RelayUrl
import com.squareup.moshi.Moshi
import io.matthewnelson.build_config.BuildConfigDebug
import io.matthewnelson.k_openssl_common.clazzes.Password
import io.matthewnelson.test_feature_authentication_core.AuthenticationCoreDefaultsTestHelper
import io.matthewnelson.test_feature_authentication_core.TestEncryptionKeyHandler
import kotlinx.coroutines.test.runBlockingTest
import okio.base64.decodeBase64ToArray
import org.cryptonode.jncryptor.AES256JNCryptor
import org.junit.Before
import org.junit.BeforeClass

/**
 * This class uses a test account setup on SphinxRelay to help ensure API compatibility.
 *
 * It is important that all tests related to use of this class **not** fail
 * if environment variables are not set.
 *
 * Wrapping tests in [getCredentials]?.let { credentials -> // my test } ensures
 * that test will simply notify that environment variables should be set with
 * their own test account credentials.
 * */
abstract class NetworkQueryTestHelper: AuthenticationCoreDefaultsTestHelper() {

    companion object {
        protected var privKey: String? = null
        protected var pubKey: String? = null
        protected var relayUrl: RelayUrl? = null
        protected var javaWebToken: JavaWebToken? = null

        @BeforeClass
        @JvmStatic
        fun setupClassNetworkQueryTestHelper() {
            System.getenv("SPHINX_CHAT_KEY_EXPORT")?.let { export ->
                System.getenv("SPHINX_CHAT_EXPORT_PASS")?.toCharArray()?.let { pass ->
                    setProperties(export, pass)
                    return
                }
            }

            println("\n\n***********************************************")
            println("          SPHINX_CHAT_KEY_EXPORT")
            println("                   and")
            println("          SPHINX_CHAT_EXPORT_PASS\n")
            println("    System environment variables are not set\n")
            println("        Network Tests will not be run!!!")
            println("***********************************************\n\n")
        }

        fun setProperties(keyExport: String, password: CharArray) {
            keyExport
                .decodeBase64ToArray()
                ?.toString(charset("UTF-8"))
                ?.split("::")
                ?.let { decodedSplit ->
                    if (decodedSplit.elementAtOrNull(0) != "keys") {
                        return
                    }

                    decodedSplit.elementAt(1).decodeBase64ToArray()?.let { toDecrypt ->
                        val decryptedSplit = AES256JNCryptor()
                            .decryptData(toDecrypt, password)
                            .toString(charset("UTF-8"))
                            .split("::")

                        if (decryptedSplit.size != 4) {
                            return
                        }

                        privKey = decryptedSplit[0]
                        pubKey = decryptedSplit[1]
                        relayUrl = RelayUrl(decryptedSplit[2])
                        javaWebToken = JavaWebToken(decryptedSplit[3])
                    }
                }
        }
    }

    protected data class Credentials(
        val privKey: String,
        val pubKey: String,
        val relayUrl: RelayUrl,
        val jwt: JavaWebToken,
    )

    /**
     * Will return null if the SystemProperties for:
     *  - SPHINX_CHAT_KEY_EXPORT
     *  - SPHINX_CHAT_EXPORT_PASS
     *
     * are not set, allowing for a soft failure of the tests.
     * */
    protected fun getCredentials(): Credentials? =
        privKey?.let { nnPrivKey ->
            pubKey?.let { nnPubKey ->
                relayUrl?.let { nnRelayUrl ->
                    javaWebToken?.let { nnJwt ->
                        Credentials(
                            nnPrivKey,
                            nnPubKey,
                            nnRelayUrl,
                            nnJwt
                        )
                    }
                }
            }
        }

    protected open val moshi: Moshi by lazy {
        Moshi.Builder().build()
    }

    /**
     * Override this and set to `true` to use Logging Interceptors during the test
     * */
    open val useLoggingInterceptors: Boolean = false

    protected open val networkClient: NetworkClient by lazy {
        NetworkClientImpl(
            // true will add interceptors to the OkHttpClient
            BuildConfigDebug(useLoggingInterceptors)
        )
    }

    protected open val relayDataHandler: RelayDataHandler by lazy {
        RelayDataHandlerImpl(
            testStorage,
            testCoreManager,
            dispatchers,
            testHandler
        )
    }

    protected open val nqChat: NetworkQueryChat by lazy {
        NetworkQueryChatImpl(
            dispatchers,
            moshi,
            networkClient,
            relayDataHandler
        )
    }

    protected open val nqContact: NetworkQueryContact by lazy {
        NetworkQueryContactImpl(
            dispatchers,
            moshi,
            networkClient,
            relayDataHandler
        )
    }

    protected open val nqInvite: NetworkQueryInvite by lazy {
        NetworkQueryInviteImpl(
            dispatchers,
            moshi,
            networkClient,
            relayDataHandler
        )
    }

    protected open val nqMessage: NetworkQueryMessage by lazy {
        NetworkQueryMessageImpl(
            dispatchers,
            moshi,
            networkClient,
            relayDataHandler
        )
    }

    protected open val nqSubscription: NetworkQuerySubscription by lazy {
        NetworkQuerySubscriptionImpl(
            dispatchers,
            moshi,
            networkClient,
            relayDataHandler
        )
    }

    protected open val nqLightning: NetworkQueryLightning by lazy {
        NetworkQueryLightningImpl(
            dispatchers,
            moshi,
            networkClient,
            relayDataHandler
        )
    }

    @Before
    fun setupNetworkQueryTestHelper() = testDispatcher.runBlockingTest {
        getCredentials()?.let { creds ->
            // Set our raw private/public keys in the test handler so when we login
            // for the first time the generated keys will be these
            testHandler.keysToRestore = TestEncryptionKeyHandler.RestoreKeyHolder(
                Password(creds.privKey.toCharArray()),
                Password(creds.pubKey.toCharArray())
            )

            // login for the first time to setup the authentication library with
            // a pin of 000000
            login()

            // persist our relay url and java web token to test storage
            relayDataHandler.persistJavaWebToken(creds.jwt)
            relayDataHandler.persistRelayUrl(creds.relayUrl)
        }

        // if null, do nothing.
    }
}