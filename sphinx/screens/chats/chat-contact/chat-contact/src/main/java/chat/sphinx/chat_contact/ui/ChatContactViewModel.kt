package chat.sphinx.chat_contact.ui

import android.app.Application
import android.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.chat_common.ui.ChatViewModel
import chat.sphinx.chat_common.ui.viewstate.InitialHolderViewState
import chat.sphinx.concept_network_query_lightning.NetworkQueryLightning
import chat.sphinx.concept_network_query_lightning.model.route.RouteSuccessProbabilityDto
import chat.sphinx.concept_network_query_lightning.model.route.isRouteAvailable
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_repository_message.SendMessage
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.resources.getRandomHexCode
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_chat.ChatName
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.util.getInitials
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_contact.ContactAlias
import chat.sphinx.wrapper_contact.getColorKey
import chat.sphinx.wrapper_message.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

internal inline val ChatContactFragmentArgs.chatId: ChatId?
    get() = if (argChatId == ChatId.NULL_CHAT_ID.toLong()) {
        null
    } else {
        ChatId(argChatId)
    }

internal inline val ChatContactFragmentArgs.contactId: ContactId
    get() = ContactId(argContactId)

@HiltViewModel
internal class ChatContactViewModel @Inject constructor(
    app: Application,
    dispatchers: CoroutineDispatchers,
    chatRepository: ChatRepository,
    contactRepository: ContactRepository,
    messageRepository: MessageRepository,
    networkQueryLightning: NetworkQueryLightning,
    savedStateHandle: SavedStateHandle,
    userColorsHelper: UserColorsHelper,
): ChatViewModel<ChatContactFragmentArgs>(
    app,
    dispatchers,
    chatRepository,
    contactRepository,
    messageRepository,
    networkQueryLightning,
    savedStateHandle,
    userColorsHelper
) {
    override val args: ChatContactFragmentArgs by savedStateHandle.navArgs()
    private var chatId: ChatId? = args.chatId

    private val contactSharedFlow: SharedFlow<Contact?> = flow {
        emitAll(contactRepository.getContactById(args.contactId))
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1,
    )

    override val chatSharedFlow: SharedFlow<Chat?> = flow {
        chatId?.let { chatId ->
            emitAll(chatRepository.getChatById(chatId))
        } ?: chatRepository.getConversationByContactId(args.contactId).collect { chat ->
            chatId = chat?.id
            emit(chat)
        }
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1
    )

    override val headerInitialHolderSharedFlow: SharedFlow<InitialHolderViewState> = flow {
        contactSharedFlow.collect { contact ->
            if (contact != null) {
                contact.photoUrl?.let { photoUrl ->
                    emit(
                        InitialHolderViewState.Url(photoUrl)
                    )
                } ?: contact.alias?.let { alias ->
                    val contactColor = Color.parseColor(
                        userColorsHelper.getHexCodeForKey(
                            contact.getColorKey(),
                            app.getRandomHexCode()
                        )
                    )

                    emit(
                        InitialHolderViewState.Initials(
                            alias.value.getInitials(),
                            contactColor
                        )
                    )
                } ?: emit(
                    InitialHolderViewState.None
                )
            }
        }
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        replay = 1
    )

    override suspend fun getChatNameIfNull(): ChatName? {
        contactSharedFlow.replayCache.firstOrNull()?.let { contact ->
            return contact.alias?.value?.let { ChatName(it) }
        } ?: contactSharedFlow.firstOrNull()?.let { contact ->
            return contact.alias?.value?.let { ChatName(it) }
        } ?: let {
            var alias: ContactAlias? = null

            try {
                contactSharedFlow.collect { contact ->
                    if (contact != null) {
                        alias = contact.alias
                        throw Exception()
                    }
                }
            } catch (e: Exception) {}
            delay(25L)

            return alias?.value?.let { ChatName(it) }
        }
    }

    override suspend fun getInitialHolderViewStateForReceivedMessage(message: Message): InitialHolderViewState {
        headerInitialHolderSharedFlow.replayCache.firstOrNull()?.let { initialHolder ->
            if (initialHolder !is InitialHolderViewState.None) {
                return initialHolder
            }
        }

        headerInitialHolderSharedFlow.firstOrNull()?.let { initialHolder ->
            if (initialHolder !is InitialHolderViewState.None) {
                return initialHolder
            }
        }

        var initialHolder: InitialHolderViewState? = null

        try {
            headerInitialHolderSharedFlow.collect {
                initialHolder = it
                throw Exception()
            }
        } catch (e: Exception) {}
        delay(25L)

        return initialHolder ?: InitialHolderViewState.None
    }

    override val checkRoute: Flow<LoadResponse<Boolean, ResponseError>> = flow {
        emit(LoadResponse.Loading)

        val networkFlow: Flow<LoadResponse<RouteSuccessProbabilityDto, ResponseError>>? = let {
            emit(LoadResponse.Loading)

            var contact: Contact? = contactSharedFlow.replayCache.firstOrNull()
                ?: contactSharedFlow.firstOrNull()

            if (contact == null) {
                try {
                    contactSharedFlow.collect {
                        if (contact != null) {
                            contact = it
                            throw Exception()
                        }
                    }
                } catch (e: Exception) {}
                delay(25L)
            }

            contact?.let { nnContact ->
                nnContact.nodePubKey?.let { pubKey ->

                    nnContact.routeHint?.let { hint ->

                        networkQueryLightning.checkRoute(pubKey, hint)

                    } ?: networkQueryLightning.checkRoute(pubKey)

                }
            }
        }

        networkFlow?.let { flow ->
            flow.collect { response ->
                @Exhaustive
                when (response) {
                    LoadResponse.Loading -> {}
                    is Response.Error -> {
                        emit(response)
                    }
                    is Response.Success -> {
                        emit(
                            Response.Success(response.value.isRouteAvailable)
                        )
                    }
                }
            }
        } ?: emit(Response.Error(
            ResponseError("Contact and chatId were null, unable to check route")
        ))
    }

    override fun readMessages() {
        val idResolved: ChatId? = args.chatId ?: chatSharedFlow.replayCache.firstOrNull()?.id
        if (idResolved != null) {
            viewModelScope.launch(mainImmediate) {
                messageRepository.readMessages(idResolved)
            }
        }
    }

    override fun sendMessage(builder: SendMessage.Builder): SendMessage? {
        builder.setContactId(args.contactId)
        builder.setChatId(chatId)
        return super.sendMessage(builder)
    }
}
